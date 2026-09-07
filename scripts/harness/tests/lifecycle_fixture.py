"""Explicitly synthetic lifecycle fixtures; never alter the live work registry."""
import json
from pathlib import Path
import shutil

import yaml

ROOT = Path(__file__).resolve().parents[3]
IDENT = "WORK-LOWER-001"
ACTIVE = "docs/work/active/" + IDENT
HISTORY = "docs/work/history/" + IDENT + ".md"
MANIFEST = "docs/quality/" + IDENT + "/CP5-manifest.yaml"


def copy_checkout(root):
    for name in ("docs", "scripts", "core", "adapters", ".github"):
        shutil.copytree(ROOT / name, root / name, ignore=shutil.ignore_patterns("target", "__pycache__"))
    for name in ("AGENTS.md", "README.md", "ARCHITECTURE.md", "pom.xml", ".gitignore"):
        shutil.copy2(ROOT / name, root / name)
    attach_git(root)


def attach_git(root):
    """Isolated local object/ref snapshot for real offline dependency verification."""
    if not (root / ".git").exists():
        shutil.copytree(ROOT / ".git", root / ".git")


def registration(manifest):
    return dict(id=IDENT, status="completed", path=HISTORY, manifest=MANIFEST,
                evidence="docs/quality/" + IDENT + "/CP5.json",
                authorized_checkpoints=manifest["authorization"]["authorized_checkpoints"],
                git_branch=manifest["git"]["branch"], pull_request=manifest["git"]["pull_request"],
                backlog_ids=manifest["backlog_ids"], review_status="pending_human", merge_status="open_not_merged")


def close_fixture(root):
    """Independent expected registration for the CP5 local-closure protocol."""
    registry_path = root / "docs/work/registry.json"
    registry = json.loads(registry_path.read_text())
    if any(r["id"] == IDENT for r in registry["history"]):
        return
    manifest = yaml.safe_load((root / MANIFEST).read_text())
    registry["active"] = [r for r in registry["active"] if r["id"] != IDENT]
    registry["history"].append(registration(manifest))
    registry["backlog"][0]["status"] = "completed"
    registry_path.write_text(json.dumps(registry))
    active = root / ACTIVE
    for name in ("work-item.yaml", "spec.md", "plan.md", "eval.md", "state.md"):
        (active / name).unlink()
    active.rmdir()
    (root / HISTORY).write_text("# WORK-LOWER-001\nSynthetic local closure; pending human review, not merged.\n")
    (root / "docs/work/active/README.md").write_text("# Active\nNo active work in this synthetic closure.\n")
    for path in [root / "README.md", *list((root / "docs").rglob("*.md"))]:
        original = path.read_text()
        revised = original
        for name in ("spec", "plan", "eval", "state"):
            revised = revised.replace("active/WORK-LOWER-001/" + name + ".md", "history/WORK-LOWER-001.md")
        if path.name == "BACKLOG-LOWER-001.md":
            revised = revised.replace("`in_progress`", "`completed`")
        path.write_text(revised)


def activate_fixture(root):
    """Retain CP0's active-package counterexamples after the real package closes."""
    active = root / ACTIVE
    if active.exists():
        return
    manifest = yaml.safe_load((root / MANIFEST).read_text())
    active.mkdir(parents=True)
    (active / "work-item.yaml").write_text(yaml.safe_dump(manifest, sort_keys=False))
    for name in ("spec", "plan", "eval"):
        (active / (name + ".md")).write_text("# Synthetic active fixture\nNot a new authorized task.\n")
    (active / "state.md").write_text("CP5 multi-checkpoint synthetic active baseline\n")
    registry_path = root / "docs/work/registry.json"
    registry = json.loads(registry_path.read_text())
    registry["history"] = [r for r in registry["history"] if r["id"] != IDENT]
    item = registration(manifest)
    for key in ("manifest", "evidence", "review_status", "merge_status"):
        item.pop(key)
    item.update(status="active", path=ACTIVE)
    registry["active"].append(item)
    registry["backlog"][0]["status"] = "in_progress"
    registry_path.write_text(json.dumps(registry))
    backlog = root / "docs/work/backlog/BACKLOG-LOWER-001.md"
    backlog.write_text(backlog.read_text().replace("`completed`", "`in_progress`"))
    index = root / "docs/work/index.md"
    index.write_text(index.read_text() + "\n[WORK-LOWER-001 synthetic](active/WORK-LOWER-001/spec.md)\n")
