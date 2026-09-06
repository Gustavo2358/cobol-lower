"""Falsify CP0 gates on isolated copies, restore bytes, then require a second GREEN."""
import hashlib
import json
from pathlib import Path
import shutil
import subprocess
import sys
import tempfile

from checks import document_errors
from architecture import source_errors, class_errors

ROOT = Path(__file__).resolve().parents[2]


def sha(data):
    return hashlib.sha256(data).hexdigest()


def mutate(root, path, replacement, predicate, expected, ident):
    path = root / path
    before = path.read_bytes()
    assert not predicate(), "challenge baseline must pass"
    try:
        path.write_bytes(replacement(before))
        errors = predicate()
        assert any(expected in e for e in errors), (ident, "wrong or absent failure", errors)
        print(json.dumps(dict(id=ident, baseline_exit_code=0, observed_exit_code=1,
                              failure_reason=errors, baseline_digest=sha(before))))
    finally:
        path.write_bytes(before)
    assert path.read_bytes() == before, "mutation restoration failed"
    assert not predicate(), "second GREEN failed"
    print(json.dumps(dict(id=ident, restoration_status="RESTORED", restored_digest=sha(path.read_bytes()),
                         green_exit_code=0)))


def main():
    with tempfile.TemporaryDirectory(prefix="lower-challenge-") as name:
        root = Path(name)
        for item in ("docs", "scripts", "core", "adapters", ".github"):
            shutil.copytree(ROOT / item, root / item, ignore=shutil.ignore_patterns("target", "__pycache__"))
        for item in ("AGENTS.md", "README.md", "ARCHITECTURE.md", "pom.xml", ".gitignore"):
            shutil.copy2(ROOT / item, root / item)
        mutate(root, "README.md", lambda b: b + b"\n[broken](missing.md)\n",
               lambda: document_errors(root), "LINK", "DOC-LINK")
        def unknown_invariant(data):
            record = json.loads(data)
            record["evals"][0]["invariants"].append("INV-LWR-999")
            return json.dumps(record).encode()
        mutate(root, "docs/evals/catalog.json", unknown_invariant,
               lambda: document_errors(root), "INVARIANT", "DOC-INVARIANT")
        import yaml
        def change_manifest(data, action):
            record = yaml.safe_load(data)
            action(record)
            return yaml.safe_dump(record).encode()
        path = "docs/work/active/WORK-LOWER-001/work-item.yaml"
        mutate(root, path, lambda b: change_manifest(b, lambda r: r.update(status="completed")),
               lambda: document_errors(root), "LIFECYCLE", "DOC-ACTIVE-COMPLETED")
        mutate(root, path, lambda b: change_manifest(b, lambda r: r["must_read"].append("docs/missing.md")),
               lambda: document_errors(root), "MUST_READ", "DOC-MUST-READ")
        probe = root / "Probe.java"
        probe.write_text("public class Probe { public String value() { return null; } }\n")
        def compile_check():
            subprocess.run(["javac", "--release", "21", str(probe)], check=True, capture_output=True)
            return class_errors(root / "Probe.class")
        mutate(root, "Probe.java", lambda b: b.replace(b"String", b"java.nio.file.Path"),
               compile_check, "ARCH_API", "ARCH-PATH-API-BYTECODE")
        mutate(root, "Probe.java", lambda b: b.replace(b"return null", b'return java.nio.file.Path.of("x").toString()'),
               compile_check, "ARCH_BYTECODE", "ARCH-PATH-INTERNAL")
        mutate(root, "Probe.java", lambda b: b.replace(b"String", b"java.nio.file.Path"),
               lambda: source_errors(probe.read_text()), "ARCH_SOURCE", "ARCH-PATH-SOURCE")
    print("PASS CHALLENGE: 7 restored falsifications and second GREEN")


if __name__ == "__main__":
    main()
