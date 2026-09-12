"""Reviewed fixed focal Java entrypoints. No diff-based semantic test selection."""
import subprocess


def classpath(root, build):
    jars = ["io/github/gustavo2358/air-java/0.1.0-SNAPSHOT/air-java-0.1.0-SNAPSHOT.jar",
            "io/github/gustavo2358/air-json/0.1.0-SNAPSHOT/air-json-0.1.0-SNAPSHOT.jar",
            "com/dynatrace/hash4j/hash4j/0.30.0/hash4j-0.30.0.jar",
            "com/fasterxml/jackson/core/jackson-core/2.22.2/jackson-core-2.22.2.jar",
            "com/fasterxml/jackson/core/jackson-databind/2.22.2/jackson-databind-2.22.2.jar",
            "com/fasterxml/jackson/core/jackson-annotations/2.22/jackson-annotations-2.22.jar"]
    paths = [root / m / "target" / c for m in ("core", "adapters") for c in ("classes", "test-classes")]
    paths += [build / "m2" / j for j in jars]
    if not all(p.exists() for p in paths):
        raise RuntimeError("focal classpath incomplete")
    return ":".join(str(p) for p in paths)


def execute(root, build, profile):
    if profile != "fast":
        raise RuntimeError("unknown focal profile")
    cp = classpath(root, build)
    for main in ("io.github.gustavo2358.lower.testing.FastSuite",
                 "io.github.gustavo2358.lower.adapters.testing.FastAdapterSuite"):
        subprocess.run(["java", "-ea", "-Xmx1g", "-cp", cp, main], cwd=root, check=True)
