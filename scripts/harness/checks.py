"""Offline documentary and certification predicates. Never infer execution from prose."""
import hashlib
import json
from collections import Counter, defaultdict
from pathlib import Path, PurePosixPath
import re
from urllib.parse import unquote, urlsplit

import jsonschema
import yaml


def digest(data):
    return hashlib.sha256(data).hexdigest()


def relative(path):
    return (isinstance(path, str) and bool(path) and not PurePosixPath(path).is_absolute()
            and ".." not in PurePosixPath(path).parts and "\\" not in path)


def unique_pairs(pairs):
    result = {}
    for key, value in pairs:
        if key in result:
            raise ValueError("duplicate key: " + str(key))
        result[key] = value
    return result


class StrictYaml(yaml.SafeLoader):
    pass


def yaml_mapping(loader, node):
    return unique_pairs([(loader.construct_object(k), loader.construct_object(v)) for k, v in node.value])


StrictYaml.add_constructor(yaml.resolver.BaseResolver.DEFAULT_MAPPING_TAG, yaml_mapping)


def read_data(path):
    text = path.read_text(encoding="utf-8")
    if path.suffix == ".json":
        return json.loads(text, object_pairs_hook=unique_pairs)
    return yaml.load(text, Loader=StrictYaml)


def anchors(text):
    found = set(re.findall(r'<a\s+(?:id|name)=["\']([^"\']+)', text))
    repeated = {}
    for title in re.findall(r"^#{1,6}\s+(.+?)\s*#*\s*$", text, re.M):
        key = re.sub(r"[^\w\- ]", "", re.sub(r"<[^>]+>", "", title).lower()).replace(" ", "-")
        number = repeated.get(key, 0)
        repeated[key] = number + 1
        found.add(key if number == 0 else f"{key}-{number}")
    return found


def historical_link(root, path, resolved, fragment):
    """Resolve unchanged archived evidence at its recorded Git head, never current routing."""
    from git_checks import git
    import subprocess
    if not resolved.is_relative_to(root.resolve()):
        return False
    try:
        for item in read_data(root / "docs/work/registry.json")["history"]:
            if not path.is_relative_to(root / "docs/quality" / item["id"]):
                continue
            sha = item.get("remote_observation", {}).get("head_sha", "")
            if not re.fullmatch(r"[0-9a-f]{40}", sha):
                continue
            if git(root, "show", sha + ":" + str(path.relative_to(root))) != path.read_bytes():
                continue
            original = git(root, "show", sha + ":" + str(resolved.relative_to(root.resolve())))
            return not fragment or unquote(fragment) in anchors(original.decode("utf-8"))
    except (OSError, KeyError, ValueError, subprocess.CalledProcessError):
        pass
    return False


def markdown_errors(root):
    errors = []
    paths = list((root / "docs").rglob("*.md")) + list(root.glob("*.md"))
    for path in paths:
        # Historical handoffs are evidence, not the current routing graph.
        if path.is_relative_to(root / "docs/sources/history"):
            continue
        text = re.sub(r"(?ms)^```.*?^```[^\n]*", "", path.read_text(encoding="utf-8"))
        for link in re.findall(r"\[[^\]\n]*\]\(([^\s)]+)(?:\s+[^)]*)?\)", text):
            target = urlsplit(link.strip("<>"))
            if target.scheme or target.netloc:
                continue
            resolved = (path.parent / unquote(target.path)).resolve() if target.path else path.resolve()
            if not resolved.is_relative_to(root.resolve()) or not resolved.exists():
                if not historical_link(root, path, resolved, target.fragment):
                    errors.append(f"LINK {path.relative_to(root)}: {link}")
            elif target.fragment and resolved.is_file() and resolved.suffix == ".md":
                if unquote(target.fragment) not in anchors(resolved.read_text(encoding="utf-8")):
                    errors.append(f"ANCHOR {path.relative_to(root)}: {link}")
    return errors


def dependency_errors(root, ident, checkpoint, branch, pull_request):
    # Local Git is offline evidence; the remote receipt never selects its own expected SHA.
    from git_checks import resolve_checkpoint_commit
    import subprocess
    try:
        expected = resolve_checkpoint_commit(root, ident, checkpoint, branch, pull_request)
        certificate = read_data(root / f"docs/quality/{ident}/{checkpoint}.json")
        receipt = read_data(root / f"docs/quality/{ident}/{checkpoint}-remote.json")
        if receipt.get("pull_request") != pull_request:
            return ["DEPENDENCY receipt PR identity"]
        return ["DEPENDENCY " + e for e in remote_errors(certificate["frozen_contract"]["required_remote_checks"], expected, receipt)]
    except (OSError, ValueError, KeyError, subprocess.CalledProcessError) as ex:
        return [f"DEPENDENCY {ident}/{checkpoint}: {ex}"]


def history_errors(root, registry):
    """Validate local closure metadata, not final certification or live remote state."""
    errors, valid = [], set()
    all_ids = Counter(r.get("id") for kind in ("active", "proposals", "history") for r in registry[kind])
    backlogs = defaultdict(list)
    for backlog in registry["backlog"]:
        backlogs[backlog["id"]].append(backlog)
    schema = read_data(root / "docs/templates/work-item.schema.json")
    index = (root / "docs/work/index.md").read_text()
    for item in registry["history"]:
        ident = item.get("id")
        start = len(errors)
        try:
            if not isinstance(ident, str) or not re.fullmatch(r"WORK-[A-Z][A-Z0-9-]*-[0-9]{3}", ident):
                raise ValueError("identity")
            if all_ids[ident] != 1:
                raise ValueError("duplicate registration")
            pair = (item["review_status"], item["merge_status"])
            allowed = {(r, "open_not_merged") for r in ("pending_human", "changes_requested", "reviewed", "approved")}
            allowed |= {(r, "merged") for r in ("reviewed", "approved", "not_recorded")}
            if item["status"] != "completed" or pair not in allowed:
                raise ValueError("invalid historical review/merge state")
            if item["review_status"] in ("reviewed", "approved", "not_recorded"):
                observation = item["remote_observation"]
                if (observation["pull_request"] != item["pull_request"] or observation["branch"] != item["git_branch"]
                        or observation["base"] != "main" or observation["review_status"] != item["review_status"]
                        or observation["state"] != ("MERGED" if item["merge_status"] == "merged" else "OPEN")
                        or not observation["repository"] or not observation["observed_at"]
                        or not re.fullmatch(r"[0-9a-f]{40}", observation["head_sha"])
                        or (item["review_status"] == "not_recorded" and
                            (observation.get("reviews") != [] or observation.get("review_url") is not None
                             or observation.get("review_commit") is not None))
                        or (item["review_status"] != "not_recorded" and
                            (not observation["review_url"].startswith("https://")
                             or observation["review_commit"] != observation["head_sha"]))
                        or (item["merge_status"] == "merged" and not re.fullmatch(r"[0-9a-f]{40}", observation["merge_commit"] or ""))):
                    raise ValueError("historical remote observation incoherent")
            for key in ("path", "manifest", "evidence"):
                if not relative(item[key]) or not (root / item[key]).is_file():
                    raise ValueError("missing/unsafe " + key)
            if item["path"] != f"docs/work/history/{ident}.md":
                raise ValueError("history path")
            if any((root / f"docs/work/{kind}/{ident}").exists() for kind in ("active", "proposals")):
                raise ValueError("active/proposal residue")
            manifest = read_data(root / item["manifest"])
            if list(jsonschema.Draft202012Validator(schema).iter_errors(manifest)):
                raise ValueError("manifest schema/authority")
            auth = manifest["authorization"]
            final = manifest["checkpoints"][-1]["id"]
            if (manifest["id"] != ident or auth["state"] != "granted" or auth["current_checkpoint"] != final
                    or final not in auth["authorized_checkpoints"]
                    or item["authorized_checkpoints"] != auth["authorized_checkpoints"]):
                raise ValueError("final checkpoint authorization")
            if item["manifest"] != f"docs/quality/{ident}/{final}-manifest.yaml" or item["evidence"] != f"docs/quality/{ident}/{final}.json":
                raise ValueError("final evidence path")
            if (item["git_branch"] != manifest["git"]["branch"] or manifest["git"]["pull_request"] not in (None, item["pull_request"])
                    or item["backlog_ids"] != manifest["backlog_ids"]):
                raise ValueError("Git/backlog identity")
            record = read_data(root / item["evidence"])
            if (record["work_item"] != ident or record["checkpoint"] != final
                    or record["branch"] != item["git_branch"]
                    or (record["pull_request"] != item["pull_request"]
                        and not (record["checkpoint"] == "CP0" and record["pull_request"] is None and manifest["git"]["pull_request"] is None))):
                raise ValueError("evidence identity")
            refs = [r for r in record["frozen_contract"]["references"] if r["path"] == item["manifest"]]
            if len(refs) != 1 or refs[0]["sha256"] != digest((root / item["manifest"]).read_bytes()):
                raise ValueError("frozen manifest binding")
            for checkpoint in manifest["checkpoints"][:-1]:
                cp = checkpoint["id"]
                errors.extend("HISTORY " + e for e in dependency_errors(root, ident, cp, item["git_branch"], item["pull_request"]))
            remediation_ids = set()
            for remediation in item.get("remediations", []):
                rid = remediation["id"]
                if not re.fullmatch(r"R[1-9][0-9]*", rid) or rid in remediation_ids or remediation["status"] not in ("active", "ready_for_review", "reviewed"):
                    raise ValueError("remediation registration")
                remediation_ids.add(rid)
                for key, suffix in (("authorization", "-authorization.json"), ("evidence", ".json"), ("state", "-state.md")):
                    if remediation[key] != f"docs/quality/{ident}/{rid}{suffix}" or not (root / remediation[key]).is_file():
                        raise ValueError("remediation path " + key)
                authority = read_data(root / remediation["authorization"])
                if (authority["work_item"] != ident or authority["id"] != rid or authority["branch"] != item["git_branch"]
                        or authority["pull_request"] != item["pull_request"] or authority["kind"] != "human-review-remediation"
                        or authority["state"] != "granted" or not authority["authority"]):
                    raise ValueError("remediation authority identity")
                errors.extend("HISTORY " + e for e in dependency_errors(root, ident, final, item["git_branch"], item["pull_request"]))
            for backlink in item["backlog_ids"]:
                matches = backlogs[backlink]
                if len(matches) != 1 or matches[0]["status"] != "completed" or matches[0].get("work_item") != ident:
                    raise ValueError("backlog not closed")
            if ident not in index or item["path"].removeprefix("docs/work/") not in index:
                errors.append(f"HISTORY INDEX {ident}")
        except (KeyError, IndexError, TypeError, ValueError, OSError) as ex:
            errors.append(f"HISTORY DEPENDENCY/REGISTRATION {ident}: {ex}")
        if len(errors) == start:
            valid.add(ident)
    return errors, valid


def work_manifest(root, ident, for_execution=False):
    """Resolve an explicitly registered work; absence/ambiguity never falls back."""
    root = Path(root)
    registry = read_data(root / "docs/work/registry.json")
    matches = [(kind, r) for kind in ("active", "proposals", "history")
               for r in registry[kind] if r.get("id") == ident]
    if len(matches) != 1:
        raise ValueError("WORK_REGISTRATION missing/ambiguous " + ident)
    kind, item = matches[0]
    if kind == "history":
        if for_execution:
            raise ValueError("AUTHORIZATION historical work is read-only")
        errors, valid = history_errors(root, registry)
        if errors or ident not in valid:
            raise ValueError("WORK_REGISTRATION invalid closure: " + "; ".join(errors))
        path = item["manifest"]
    elif kind == "active" and item.get("path") == f"docs/work/active/{ident}":
        path = item["path"] + "/work-item.yaml"
    else:
        raise ValueError("WORK_REGISTRATION not active/closed " + ident)
    manifest = read_data(root / path)
    if manifest["id"] != ident or manifest["authorization"]["state"] != "granted":
        raise ValueError("WORK_REGISTRATION unauthorized " + ident)
    return manifest


def execution_authority(root, record):
    """A closed artifact is not authority; remediation needs its separate explicit grant."""
    if record.get("schema_version") != 2:
        raise ValueError("AUTHORIZATION new execution requires evidence v2; legacy is audit-only")
    if record.get("kind") != "human-review-remediation":
        return work_manifest(root, record["work_item"], for_execution=True)
    path = record["authorization_reference"]
    if not relative(path):
        raise ValueError("AUTHORIZATION unsafe path")
    authority = read_data(Path(root) / path)
    registry = read_data(Path(root) / "docs/work/registry.json")
    items = [r for r in registry["history"] if r["id"] == record["work_item"]]
    if len(items) != 1 or items[0]["merge_status"] != "open_not_merged":
        raise ValueError("AUTHORIZATION remediation requires unmerged historical work")
    remediations = [r for r in items[0].get("remediations", []) if r["id"] == record.get("remediation")]
    if (len(remediations) != 1 or remediations[0]["authorization"] != path
            or remediations[0]["status"] not in ("active", "ready_for_review")
            or authority["kind"] != record["kind"] or authority["state"] != "granted"
            or authority["id"] != record["remediation"] or authority["work_item"] != record["work_item"]
            or authority["branch"] != record["branch"] or authority["pull_request"] != record["pull_request"]
            or authority["base_commit"] != record["base_commit"] or not authority["authority"]):
        raise ValueError("AUTHORIZATION remediation mismatch")
    return authority


def document_errors(root):
    root = Path(root)
    errors = markdown_errors(root)
    for path in sorted(list((root / "docs").rglob("*")) + list((root / ".github").rglob("*"))):
        if path.suffix in (".json", ".yaml", ".yml"):
            try:
                read_data(path)
            except (ValueError, yaml.YAMLError) as ex:
                errors.append(f"PARSE {path.relative_to(root)}: {ex}")
    if any(e.startswith("PARSE") for e in errors):
        return errors
    registry = read_data(root / "docs/work/registry.json")
    evals = read_data(root / "docs/evals/catalog.json")["evals"]
    invariants = read_data(root / "docs/architecture/invariants.json")["records"]
    ids = {}
    for name, records in (("EVAL", evals), ("INVARIANT", invariants), ("BACKLOG", registry["backlog"])):
        ids[name] = {r["id"] for r in records}
        if len(ids[name]) != len(records):
            errors.append(f"DUPLICATE {name}")
    for e in evals:
        for ident in e["invariants"]:
            if ident not in ids["INVARIANT"]:
                errors.append(f"INVARIANT {e['id']}: {ident}")
        if e.get("test_path") and not (root / e["test_path"]).exists():
            errors.append(f"EVAL_PATH {e['id']}")
    for inv in invariants:
        if not (root / inv["canonical_rule"]).is_file():
            errors.append(f"INVARIANT_RULE {inv['id']}")
        for ident in inv["evals"]:
            if ident not in ids["EVAL"]:
                errors.append(f"EVAL {inv['id']}: {ident}")
    for kind, records, canonical in (("EVAL", evals, "docs/evals/catalog.md"),
                                     ("INVARIANT", invariants, "docs/architecture/invariants.md")):
        text = (root / canonical).read_text()
        for record in records:
            if len(re.findall(r'<a id="' + re.escape(record["id"]) + '"></a>', text)) != 1:
                errors.append(f"{kind}_ANCHOR {record['id']}")
    gates = read_data(root / "docs/engineering/gates.json")["gates"]
    gate_names = {g["name"] for g in gates}
    for gate in gates:
        if gate["implementation_status"] != "SPECIFIED_NOT_IMPLEMENTED":
            if not gate.get("entrypoint") or not (root / gate["entrypoint"].split()[0]).is_file():
                errors.append(f"GATE_EXECUTOR {gate['name']}")
    schema = read_data(root / "docs/templates/work-item.schema.json")
    discovered = {}
    for location in ("active", "proposals"):
        registered = {r["id"]: r for r in registry[location]}
        manifests = list((root / f"docs/work/{location}").glob("*/work-item.yaml"))
        observed = set()
        for path in manifests:
            work = read_data(path)
            ident = work["id"]
            observed.add(ident)
            if ident in discovered:
                errors.append(f"DUPLICATE {ident}")
            discovered[ident] = work
            for violation in jsonschema.Draft202012Validator(schema).iter_errors(work):
                errors.append(f"SCHEMA {ident}: {violation.message}")
            if {p.name for p in path.parent.iterdir()} != {"work-item.yaml", "spec.md", "plan.md", "eval.md", "state.md"}:
                errors.append(f"PACKAGE {ident}: expected exactly five files")
            if location == "active" and work["status"] not in ("active", "blocked"):
                errors.append(f"LIFECYCLE active {ident}: {work['status']}")
            if location == "proposals" and (work["status"] not in ("proposed", "ready_for_authorization")
                                              or work["authorization"]["state"] == "granted"):
                errors.append(f"LIFECYCLE proposal {ident}")
            registration = registered.get(ident, {})
            if registration.get("path") != str(path.parent.relative_to(root)) or registration.get("status") != work["status"]:
                errors.append(f"REGISTRY {ident}")
            if registration.get("git_branch") != work["git"]["branch"] or registration.get("pull_request") != work["git"]["pull_request"]:
                errors.append(f"REGISTRY_GIT {ident}")
            auth = work["authorization"]
            if registration.get("authorized_checkpoints") != auth["authorized_checkpoints"]:
                errors.append(f"REGISTRY_AUTHORIZATION {ident}")
            cps = {cp["id"]: cp for cp in work["checkpoints"]}
            if len(cps) != len(work["checkpoints"]):
                errors.append(f"DUPLICATE checkpoints {ident}")
            if not set(auth["authorized_checkpoints"]) <= set(cps) or (auth["state"] == "granted" and auth["current_checkpoint"] not in auth["authorized_checkpoints"]):
                errors.append(f"AUTHORIZATION {ident}")
            state = (path.parent / "state.md").read_text()
            if auth["state"] == "granted":
                if str(auth["current_checkpoint"]) not in state or auth.get("execution_mode", "single-checkpoint") not in state:
                    errors.append(f"STATE_AUTHORIZATION {ident}")
                current = cps.get(auth["current_checkpoint"], {})
                for previous in current.get("depends_on", []):
                    errors.extend(dependency_errors(root, ident, previous, work["git"]["branch"], work["git"]["pull_request"]))
            for key in ("must_read", "related_domain_rules"):
                for item in work[key]:
                    if not relative(item) or not (root / item).is_file():
                        errors.append(f"MUST_READ {ident}: {item}")
            for key in ("source_scope", "test_scope", "docs_scope"):
                for item in work[key]:
                    planned = item.startswith("planned:")
                    normalized = item.removeprefix("planned:")
                    if not relative(normalized) or (not planned and not (root / normalized).exists()):
                        errors.append(f"SCOPE {ident}: {item}")
            for key, catalogue in (("related_invariants", "INVARIANT"), ("evals", "EVAL"), ("backlog_ids", "BACKLOG")):
                for item in work[key]:
                    if item not in ids[catalogue]:
                        errors.append(f"{catalogue} {ident}: {item}")
            for adr in work["related_decisions"]:
                if not (root / f"docs/architecture/decisions/{adr.lower()}.md").is_file():
                    errors.append(f"ADR {ident}: {adr}")
            for cp in cps.values():
                if not set(cp["depends_on"]) <= set(cps) or not set(cp["evals"]) <= ids["EVAL"] or not set(cp["gates"]) <= gate_names:
                    errors.append(f"CHECKPOINT_REFERENCES {ident}: {cp['id']}")
        if observed != set(registered):
            errors.append(f"REGISTRY_LOCATION {location}")
    for backlog in registry["backlog"]:
        if not relative(backlog["path"]) or not (root / backlog["path"]).is_file():
            errors.append(f"BACKLOG_PATH {backlog['id']}")
        else:
            text = (root / backlog["path"]).read_text()
            if f"`{backlog['status']}`" not in text:
                errors.append(f"BACKLOG_STATUS {backlog['id']}")
        if backlog.get("work_item") in discovered and discovered[backlog["work_item"]]["status"] in ("active", "blocked") and backlog["status"] != "in_progress":
            errors.append(f"BACKLOG_LIFECYCLE {backlog['id']}")
    index = (root / "docs/work/index.md").read_text()
    for work in registry["active"]:
        if work["id"] not in index or work["path"].removeprefix("docs/work/") not in index:
            errors.append(f"INDEX {work['id']}")
    sources = read_data(root / "docs/sources/sources.lock.json")
    for source in sources["sources"]:
        if not re.fullmatch(r"[0-9a-f]{40}", source.get("commit", "")):
            errors.append(f"SOURCE_REVISION {source['id']}")
        if not source.get("paths") or any(not relative(p) for p in source["paths"]):
            errors.append(f"SOURCE_PATH {source['id']}")
    for source in sources["local_sources"]:
        if not relative(source["path"]) or not (root / source["path"]).is_file() or digest((root / source["path"]).read_bytes()) != source["sha256"]:
            errors.append(f"SOURCE_HASH {source['id']}")
    closure_errors, valid_history = history_errors(root, registry)
    errors.extend(closure_errors)
    if not any(w["authorization"]["state"] == "granted" for w in discovered.values()) and not valid_history:
        if (root / "pom.xml").exists() or (root / "scripts").exists() or list(root.glob("**/*.java")):
            errors.append("UNAUTHORIZED_IMPLEMENTATION docs-only")
    return errors


def certificate_errors(root, record):
    root = Path(root)
    errors = []
    def require(condition, label):
        if not condition:
            errors.append("CERTIFICATE " + label)
    for field in ("work_item", "checkpoint", "repository", "branch", "pull_request", "base_commit", "reviewed_head",
                  "execution_mode", "authorization_reference", "previous_certified_checkpoint", "certified_commit",
                  "candidate_diff_sha256", "candidate_exclusions", "frozen_contract", "executions", "not_executed",
                  "falsifications", "regressions", "review", "merge_status", "limitations", "blockers", "certification"):
        require(field in record, "missing " + field)
    require(record.get("template") is False, "not a template")
    require(record.get("certification", {}).get("status") == "PASS", "status")
    cert = record.get("certification", {})
    require(cert.get("scope") == "pre_commit", "pre_commit")
    for flag in ("all_mutations_restored", "required_results_pass"):
        require(cert.get(flag) is True, flag)
    require(cert.get("unresolved_findings") == [], "unresolved_findings")
    require(record.get("blockers") == [], "blockers")
    require(record.get("not_executed") == [], "not_executed")
    require(record.get("certified_commit") is None, "self SHA forbidden")
    for field in ("base_commit", "reviewed_head"):
        require(bool(re.fullmatch(r"[0-9a-f]{40}", record.get(field) or "")), field)
    require(bool(re.fullmatch(r"[0-9a-f]{64}", record.get("candidate_diff_sha256") or "")), "candidate digest")
    frozen = record.get("frozen_contract", {})
    references = frozen.get("references", [])
    paths = [r.get("path") for r in references]
    require(bool(paths) and all(isinstance(p, str) for p in paths), "FREEZE paths")
    if all(isinstance(p, str) for p in paths):
        require(paths == sorted(set(paths)), "FREEZE ordered unique paths")
    require("docs/sources/sources.lock.json" in paths, "FREEZE source lock")
    for reference in references:
        fields = {"path", "sha256", "git_revision"}
        if record.get("schema_version", 1) >= 2:
            fields.add("usage")
            require(reference.get("usage") in ("reference_only", "candidate_immutable"), "FREEZE usage")
            if reference.get("path") == "docs/sources/sources.lock.json":
                require(reference.get("usage") == "candidate_immutable", "FREEZE source lock immutable")
        require(set(reference) == fields, "FREEZE reference fields")
        require(relative(reference.get("path")), "FREEZE relative path")
        require(bool(re.fullmatch(r"[0-9a-f]{64}", reference.get("sha256") or "")), "FREEZE hash")
        require(reference.get("git_revision") is None or bool(re.fullmatch(r"[0-9a-f]{40}", reference.get("git_revision") or "")), "FREEZE revision")
    require(bool(frozen.get("required_gates")), "required gates")
    required = frozen.get("required_gates", [])
    remediation = record.get("kind") == "human-review-remediation"
    manifests = [p for p in paths if isinstance(p, str) and p.endswith("-manifest.yaml")]
    if remediation:
        require(record.get("schema_version") == 2 and record.get("checkpoint") is None, "remediation is not a checkpoint")
        authority_path = record.get("authorization_reference")
        if relative(authority_path) and (root / authority_path).is_file():
            authority = read_data(root / authority_path)
            require(authority.get("kind") == record["kind"] and authority.get("state") == "granted" and bool(authority.get("authority")), "remediation authority")
            require(bool(re.fullmatch(r"R[1-9][0-9]*", record.get("remediation") or "")), "remediation id")
            for key in ("work_item", "branch", "pull_request", "base_commit"):
                require(record.get(key) == authority.get(key), "remediation " + key)
            require(record.get("remediation") == authority.get("id") and required == authority.get("gates"), "remediation frozen gates/id")
            require(any(r.get("path") == authority_path and r.get("usage") == "candidate_immutable" for r in references), "remediation immutable grant")
            require(record.get("execution_mode") == "human-review-remediation", "remediation mode")
            normative = {"AGENTS.md", "docs/engineering/agent-session-protocol.md", "docs/engineering/work-item-protocol.md",
                         "docs/engineering/git-and-review.md", "docs/engineering/falsification.md", "docs/engineering/gates.md",
                         "docs/engineering/gates.json", "docs/architecture/invariants.md", "docs/evals/catalog.md"}
            require(normative <= set(paths), "FREEZE normative references incomplete")
            contract = f"docs/quality/{record.get('work_item')}/{record.get('remediation')}-contract.md"
            require(any(r.get("path") == contract and r.get("usage") == "candidate_immutable" for r in references), "remediation immutable contract")
            previous = record.get("previous_certified_checkpoint") or {}
            try:
                from git_checks import resolve_checkpoint_commit
                import subprocess
                expected = resolve_checkpoint_commit(root, record["work_item"], previous["checkpoint"], record["branch"], record["pull_request"])
                require(previous.get("commit") == record["base_commit"] == expected, "remediation recovery commit")
                prefix = f"docs/quality/{record['work_item']}/{previous['checkpoint']}"
                require(previous.get("evidence") == prefix + ".json" and previous.get("remote_receipt") == prefix + "-remote.json", "remediation recovery paths")
                require(not dependency_errors(root, record["work_item"], previous["checkpoint"], record["branch"], record["pull_request"]), "remediation recovery remote")
            except (KeyError, ValueError, OSError, subprocess.CalledProcessError):
                require(False, "remediation recovery missing/invalid")
        else:
            require(False, "remediation authority missing")
    else:
        require(len(manifests) == 1, "FREEZE manifest")
    if not remediation and len(manifests) == 1 and (root / manifests[0]).is_file():
        manifest = read_data(root / manifests[0])
        checkpoints = {c["id"]: c for c in manifest["checkpoints"]}
        current = checkpoints.get(record.get("checkpoint"), {})
        required_references = set(manifest["must_read"]) - {"docs/engineering/gates.md"}
        required_references |= {"docs/engineering/gates.md", "docs/engineering/gates.json",
            "docs/engineering/agent-session-protocol.md", "docs/engineering/falsification.md",
            "docs/architecture/invariants.md", "docs/evals/catalog.md"}
        required_references |= {f"docs/work/active/{record.get('work_item')}/{f}.md" for f in ("spec", "plan", "eval")}
        require(required_references <= set(paths), "FREEZE normative references incomplete")
        require(record.get("authorization_reference") in paths, "FREEZE authority reference")
        require(required == current.get("gates"), "frozen required gates cannot shrink")
        require(record.get("checkpoint") in manifest["authorization"]["authorized_checkpoints"], "frozen authorization")
        require(record.get("execution_mode") == manifest["authorization"].get("execution_mode", "single-checkpoint"), "frozen execution mode")
    elif not remediation:
        require(False, "FREEZE manifest unavailable")
    entries = record.get("executions", [])
    gate_records = {g["name"]: g for g in read_data(root / "docs/engineering/gates.json")["gates"]}
    for gate in required:
        require(gate_records.get(gate, {}).get("implementation_status") == "AUTOMATED_VERIFIED", "executor " + gate)
        runs = [e for e in entries if e.get("gate") == gate and e.get("required") is True]
        require(bool(runs), "missing gate " + gate)
        for execution in runs:
            require(execution.get("status") == "PASS" and execution.get("exit_code") == 0, "gate " + gate)
    for run in entries:
        for field in ("gate", "required", "command", "cwd", "environment", "status", "exit_code", "test_count", "log_reference"):
            require(field in run, "execution " + field)
        require(bool(run.get("command")) and bool(run.get("environment")), "execution provenance")
        log = run.get("log_reference")
        require(relative(log) and (root / log).is_file(), "execution log")
        if run.get("required"):
            require(run.get("status") == "PASS" and run.get("exit_code") == 0, "required result")
        if run.get("gate") == "semantic":
            require(isinstance(run.get("test_count"), int) and run["test_count"] > 0, "nonzero semantic tests")
    require(bool(record.get("falsifications")), "falsifications")
    for mutation in record.get("falsifications", []):
        for field in ("id", "mutation", "expected_oracle", "failure_reason", "green_command"):
            require(bool(mutation.get(field)), "falsification " + field)
        require(mutation.get("baseline_exit_code") == 0, "baseline GREEN")
        require(isinstance(mutation.get("observed_exit_code"), int) and mutation["observed_exit_code"] != 0, "mutation RED")
        require(mutation.get("green_exit_code") == 0, "second GREEN")
        require(mutation.get("restoration_status") == "RESTORED", "restoration")
        baseline = mutation.get("baseline_digest", "")
        require(bool(re.fullmatch(r"[0-9a-f]{64}", baseline)) and baseline == mutation.get("restored_digest"), "restored digest")
    require(bool(record.get("regressions")), "regressions")
    for regression in record.get("regressions", []):
        require(bool(regression.get("guarantees")) and bool(regression.get("execution_references")), "regression evidence")
        for number in regression.get("execution_references", []):
            require(isinstance(number, int) and 0 <= number < len(entries), "regression reference")
    covered = {entries[n].get("gate") for r in record.get("regressions", [])
               for n in r.get("execution_references", []) if isinstance(n, int) and 0 <= n < len(entries)
               and entries[n].get("status") == "PASS"}
    require(set(required) <= covered, "cumulative regression gates")
    review = record.get("review", {})
    require(review.get("status") == "PASS", "review status")
    require(review.get("candidate_reference") == "#/candidate_diff_sha256", "review candidate reference")
    require(not {"reviewed_commit", "candidate_diff_sha256", "reviewed_head"}.intersection(review), "duplicate review identity")
    require(review.get("kind") in ("self-review", "independent"), "review kind")
    require(bool(review.get("reviewer_context")) and bool(review.get("evidence")), "review evidence")
    if review.get("kind") == "independent":
        require(bool(review.get("implementer_context")) and review.get("implementer_context") != review.get("reviewer_context"), "independent reviewer context")
    for reference in review.get("evidence", []):
        require(isinstance(reference, str) and ((relative(reference) and (root / reference).is_file())
                or reference.startswith("https://")), "review evidence reference")
    require(isinstance(frozen.get("remote_wait_limit_seconds"), int) and frozen["remote_wait_limit_seconds"] > 0, "remote wait limit")
    if not frozen.get("required_remote_checks"):
        require(bool(frozen.get("remote_not_applicable_reason")), "remote applicability")
    for check in frozen.get("required_remote_checks", []):
        require(all(check.get(f) for f in ("name", "workflow", "app_slug", "event")), "remote check identity")
    return errors


def remote_errors(required, sha, receipt):
    errors = []
    if not sha or receipt.get("pushed_sha") != sha:
        errors.append("REMOTE push not confirmed")
    for requirement in required:
        matches = [c for c in receipt.get("checks", []) if all(c.get(k) == requirement[k]
                   for k in ("name", "workflow", "app_slug", "event")) and c.get("head_sha") == sha]
        # Any ambiguity remains blocked; the adapter must select the current run attempt explicitly.
        if len(matches) != 1:
            errors.append("REMOTE missing/ambiguous " + requirement["name"])
        elif matches[0].get("status") != "completed" or matches[0].get("conclusion") != "success":
            errors.append("REMOTE non-success " + requirement["name"])
        elif not all(matches[0].get(k) for k in ("id", "run_id", "url")):
            errors.append("REMOTE missing provenance " + requirement["name"])
    return errors
