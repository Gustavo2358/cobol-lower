import json
from pathlib import Path
import subprocess
import sys
import tempfile
import unittest

ROOT = Path(__file__).resolve().parents[3]
sys.path.insert(0, str(ROOT / "scripts/harness"))
from architecture import source_errors, class_errors, dependency_errors, identity_hash_dependency


class Architecture(unittest.TestCase):
    def test_identity_hash_vocabulary(self):
        owner = "io.github.gustavo2358.lower.application.CanonicalRevision"
        for suffix in ("Hashing", "Hasher128", "HashStream128", "HashValue128", "HashValues"):
            target = "com.dynatrace.hash4j.hashing." + suffix
            self.assertTrue(identity_hash_dependency(owner, target))
            self.assertFalse(identity_hash_dependency(owner + "Other", target))
        for target in ("java.security.MessageDigest", "java.security.SecureRandom", "com.dynatrace.hash4j.hashing.Hasher64",
                       "com.dynatrace.hash4j.hashing.HashingOther", "com.dynatrace.hash4j.random.PseudoRandomGenerator", "java.nio.file.Path"):
            self.assertFalse(identity_hash_dependency(owner, target))

    def test_hash4j_dependency_is_pinned(self):
        tree = dict(groupId="io.github.gustavo2358", artifactId="cobol-lower-core", version="0.1.0-SNAPSHOT",
                    children=[dict(groupId="io.github.gustavo2358", artifactId="air-java", version="0.1.0-SNAPSHOT"),
                              dict(groupId="com.dynatrace.hash4j", artifactId="hash4j", version="0.30.0")])
        self.assertEqual([], dependency_errors(tree))
        tree["children"][1]["version"] = "0.29.0"
        self.assertTrue(dependency_errors(tree))

    def test_shared_vocabulary(self):
        self.assertEqual([], source_errors("import io.github.gustavo2358.air.model.Publication; class Boundary {}"))

    def test_source_counterexamples(self):
        for source in ("import java.nio.file.Path; class Boundary {}",
                "class Boundary { java.nio.file.Path source; }", "import com.fasterxml.jackson.annotation.JsonProperty;",
                "import io.github.gustavo2358.cobolexplorer.Parser;", "import io.github.gustavo2358.analysis.cfg.BuildCfg;",
                "record Publication() {}", "class LocalPublication {}", "class Boundary { void f() { System.out.println(); } }"):
            with self.subTest(source=source):
                self.assertTrue(source_errors(source))

    def test_compiled_api_and_internal_use(self):
        with tempfile.TemporaryDirectory(prefix="lower-arch-oracle-") as name:
            root = Path(name)
            for body, expected in [("public String value() { return \"x\"; }", []),
                    ("public java.nio.file.Path value() { return null; }", ["ARCH_API", "ARCH_BYTECODE"]),
                    ("public String value() { return java.nio.file.Path.of(\"x\").toString(); }", ["ARCH_BYTECODE"])]:
                path = root / "Probe.java"
                path.write_text("public class Probe { " + body + " }")
                subprocess.run(["javac", "--release", "21", str(path)], check=True, capture_output=True)
                errors = class_errors(root / "Probe.class")
                if expected:
                    for code in expected:
                        self.assertTrue(any(code in e for e in errors), errors)
                else:
                    self.assertEqual([], errors)

    def test_class_retention_annotation(self):
        with tempfile.TemporaryDirectory(prefix="lower-annotation-oracle-") as name:
            root = Path(name)
            annotation = root / "com/fasterxml/jackson/annotation/JsonProperty.java"
            annotation.parent.mkdir(parents=True)
            annotation.write_text("package com.fasterxml.jackson.annotation; public @interface JsonProperty {}")
            path = root / "Probe.java"
            path.write_text("@com.fasterxml.jackson.annotation.JsonProperty public class Probe {}")
            subprocess.run(["javac", "--release", "21", "-d", str(root), str(annotation), str(path)], check=True, capture_output=True)
            self.assertTrue(any("ARCH_BYTECODE" in e for e in class_errors(root / "Probe.class")))

    def test_dependency_tree(self):
        tree = dict(groupId="io.github.gustavo2358", artifactId="cobol-lower-core", version="0.1.0-SNAPSHOT",
                    children=[dict(groupId="io.github.gustavo2358", artifactId="air-java", version="0.1.0-SNAPSHOT")])
        self.assertEqual([], dependency_errors(tree))
        tree["children"].append(dict(groupId="org.example", artifactId="transport", version="1"))
        self.assertTrue(dependency_errors(tree))


if __name__ == "__main__":
    unittest.main(verbosity=2)
