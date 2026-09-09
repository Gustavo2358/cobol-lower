# Reproduction commands and environment

Base: 50cc57d78ac319d4a86f9d72c4b50ce9d3403297. Dedicated branch
fix/lower-remove-artificial-size-limits; original checkout remains clean.
Java21.0.12.1, Maven3.9.16, Python3.14, PyYAML6.0.3/jsonschema4.19.2.
Task build root: /tmp/lower-size-limits-build, own Maven repository and freshly
built locked AIR revision; MAVEN_OPTS=-Xmx3g for the harness runs.

Standard setup: export JAVA_HOME/PATH for Java21 and LOWER_BUILD_ROOT to an
isolated temporary directory; run `python3 scripts/harness/run.py bootstrap`.
Then `mvn -B -ntp -Dmaven.repo.local=$LOWER_BUILD_ROOT/m2 install`.

Focal: `mvn -B -ntp -Dmaven.repo.local=$LOWER_BUILD_ROOT/m2 -pl adapters test-compile exec:java -Dexec.mainClass=io.github.gustavo2358.lower.adapters.air.CapacitySuite -Dexec.classpathScope=test`.
The initial RED used the new frozen suite with all production sources still at
base. Baseline compile initially hit a deprecated test-generator method under
-Werror; it was corrected before the actual RED, with that setup failure retained.

Challenge: `python3 scripts/harness/capacity_challenge.py`. Its first run detected
that core mutation was not rebuilt for adapter execution; that non-killed mutation
is a failed experiment, not a RED. The executor now compiles/packages the mutated
core before the focal adapter suite (without running unrelated core oracles there);
full independently executes all required core tests. Mutations are in isolated copies.
The first standalone full then caught an assertion-order interaction: a new
valid/diagnostic-budget check ran before the existing cell-per-MOVE oracle. It
was moved after the established scalar oracles, without changing the historical
mutant or expected failure. That failed full log is retained too.

Full: `python3 scripts/harness/run.py full --evidence docs/quality/WORK-LOWER-007/CP0.json`.
The first attempt in the dedicated worktree reached five gates but failed 48
lifecycle tests because they copy .git as a directory. Full validation therefore
uses /tmp/lower-size-limits-validation, a local standalone clone with every tracked
and new candidate file copied byte-exactly (no target/cache files). Clone, candidate
branch and source base remain independent of the original checkout. The final
full verifies the final source/test/harness tree; evidence-only finalization then
runs docs/certify as the repository protocol requires. No failed attempt is PASS.

Canonical baseline: archive base with `git archive`, build in another isolated
directory with Maven verify -Dexec.skip=true (build only, not a semantic gate), then
run the actual CobolLower main in base and candidate using their own classes and
the same pinned runtime jars. Compare full output bytes for the unchanged CP3 and
scalar SP fixtures. The normal baseline install separately ran the original suites.

Memory: standalone CapacitySuite with Java -Xmx1536m under `/usr/bin/time -v`,
using candidate core/adapters/test classes and pinned jars. Raw log contains the
exact classpath/command. Its exit0, 17.22s and RSS are telemetry only, not a claim
of improving amplification. All probes generate/clean temporary files; no giant
fixture or build dependency is versioned.
