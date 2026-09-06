"""Architecture gate: source, public API, class constants, jdeps, Maven dependency tree."""
from pathlib import Path
import re
import subprocess

FORBIDDEN = re.compile(r"(?:java[./](?:io|nio|net|sql)[./]|javax[./]|jakarta[./]|com[./]|org[./]|"
                       r"io[./]github[./]gustavo2358[./](?:cobolexplorer|analysis[./]cfg)[./]|"
                       r"io[./]github[./]gustavo2358[./]lower[./]adapters[./]|"
                       r"java[./]lang[./](?:reflect[./]|Process\b|ProcessBuilder\b|Runtime\b))")


def source_errors(text):
    errors = []
    # Scan qualified names and imports; bytecode covers erased syntax and annotation descriptors.
    if FORBIDDEN.search(text):
        errors.append("ARCH_SOURCE forbidden dependency")
    if re.search(r"\b(?:class|record|interface|enum)\s+(?:Publication|Entry|Sequence|Return|LocalPublication|AirValidator)\b", text):
        errors.append("ARCH_MODEL parallel AIR type")
    if re.search(r"\bSystem\s*\.\s*(?:in|out|err|exit|load|loadLibrary)\b", text):
        errors.append("ARCH_SOURCE process/transport")
    return errors


def class_errors(path):
    result = subprocess.run(["javap", "-v", "-p", str(path)], text=True, capture_output=True)
    if result.returncode:
        return ["ARCH_SETUP javap: " + result.stderr]
    errors = []
    if FORBIDDEN.search(result.stdout):
        errors.append("ARCH_BYTECODE forbidden type/member/annotation")
    if "major version: 65" not in result.stdout:
        errors.append("ARCH_BYTECODE expected Java 21 class version 65")
    api = subprocess.run(["javap", "-public", "-s", str(path)], text=True, capture_output=True)
    if api.returncode:
        errors.append("ARCH_SETUP javap public")
    elif FORBIDDEN.search(api.stdout):
        errors.append("ARCH_API forbidden signature")
    return errors


def dependency_errors(tree):
    allowed = {("io.github.gustavo2358", "cobol-lower-core", "0.1.0-SNAPSHOT"),
               ("io.github.gustavo2358", "air-java", "0.1.0-SNAPSHOT")}
    errors = []
    def visit(node):
        key = (node.get("groupId"), node.get("artifactId"), node.get("version"))
        if key not in allowed:
            errors.append("ARCH_DEPENDENCY " + str(key))
        for child in node.get("children", []):
            visit(child)
    visit(tree)
    if not any(c.get("artifactId") == "air-java" for c in tree.get("children", [])):
        errors.append("ARCH_DEPENDENCY missing shared AIR")
    return errors


def architecture_errors(root, dependency_tree, air_jar):
    root = Path(root)
    errors = dependency_errors(dependency_tree)
    sources = list((root / "core/src/main/java").rglob("*.java"))
    classes = list((root / "core/target/classes").rglob("*.class"))
    if not sources or not classes:
        errors.append("ARCH_SETUP nonzero sources and compiled classes required")
    for path in sources:
        errors.extend(str(path.relative_to(root)) + ": " + e for e in source_errors(path.read_text()))
    for path in classes:
        errors.extend(str(path.relative_to(root)) + ": " + e for e in class_errors(path))
    result = subprocess.run(["jdeps", "--multi-release", "21", "-verbose:class", "-filter:none",
                             "--class-path", str(air_jar), str(root / "core/target/classes")],
                            text=True, capture_output=True)
    if result.returncode:
        errors.append("ARCH_SETUP jdeps: " + result.stderr)
    else:
        for line in result.stdout.splitlines():
            if "->" not in line:
                continue
            # jdeps also emits archive -> archive/module summary rows.
            # Maven's resolved graph checks those; these rows enumerate class references.
            owner = line.split("->", 1)[0].strip()
            if not owner.startswith("io.github.gustavo2358.lower."):
                continue
            target = line.split("->", 1)[1].strip().split()[0]
            if target == "not" or FORBIDDEN.search(target):
                errors.append("ARCH_JDEPS " + line.strip())
            elif "." in target and not target.startswith(("java.lang.", "java.util.", "java.math.",
                    "io.github.gustavo2358.lower.", "io.github.gustavo2358.air.")):
                errors.append("ARCH_JDEPS outside allowed vocabulary: " + target)
    return errors
