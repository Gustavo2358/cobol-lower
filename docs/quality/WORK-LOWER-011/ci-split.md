# CI split development evidence

14 orchestration countercases pass (0.251s), including required CI 1–10.
Exact pinned dependency compilation passed; initial skip-flag attempt failed
because upstream explicitly forbids skipped tests, preserved in raw log. FAST
now uses javac/jar and original Maven POM installation, without upstream edits.
Reactor compilation needed authorized Maven network for missing hash4j; retry
passed. Focal W1 development returned 1364 core assertions and 196 adapter
assertions. Final remote adapter profile is further restricted to literal and
computed W1 round-trip positives plus the forthcoming W2B focal cases; historical
placement challenge remains local. Architecture passed. No full qualification run.

RED1: both real W2A SP1.4 products were rejected by the current decoder at
UNSUPPORTED_CONTRACT /contractVersion, CLI exit 3, with no AIR file. This is
version evidence only, before any W2B product code. Other REDs pending.

These are development results, not LOCAL_QUALIFICATION or remote FAST_CI.

The first committed FAST invocation failed before Java at Git whitespace checking
because Maven raw logs contain trailing spaces. Logs are preserved byte-exact in
gzip with raw/compressed SHA-256; no evidence text was edited to pass. The log
packaging correction is documentation only and receives DOCS_ONLY CI.
