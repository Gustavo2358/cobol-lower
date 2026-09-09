# WORK-LOWER-007 — state

CP0, single-checkpoint, authorized by the user's 2026-09-09 request; phase
ready_for_review. Dedicated branch fix/lower-remove-artificial-size-limits from
clean origin/main 50cc57d78ac319d4a86f9d72c4b50ce9d3403297. Original checkout
remains clean at 2329993ce61b33fd7105759e211a1861ca6cb217.

Removed artificial SP32MiB/nodes1500000/admission250000 gates and the duplicated
string ceiling. Depth64, validation and atomicity remain. Same 20k corpus: compiled
capacity RED before implementation, then full relational/validator GREEN twice,
Publication equality; 38179536 bytes, 2100154 nodes, 380021 visits. CP3/CP4C CLI
bytes equal base; 481 historical/fixture/pin files preserved.

Final full PASS in a byte-identical disposable standalone clone: docs, architecture,
semantic203099, performance93 plus capacity proofs, Git,142 harness tests and57
restored challenges. Earlier setup/focal/full failures are retained and classified
in the certificate; none is counted as PASS. No required frozen gate remains skipped.

[Report](../../../quality/WORK-LOWER-007/report.md),
[certificate](../../../quality/WORK-LOWER-007/CP0.json) and
[self-review](../../../quality/WORK-LOWER-007/review.md). Final commit resolves
through its Checkpoint-Evidence trailer; exact SHA and post-push CI receipt go in
the same PR. Human review pending; no merge/auto-merge.

AIR source lock unchanged; large in-memory proof stops before independent codec
16MiB boundary. BACKLOG-LOWER-017/018 separate; 018 not solved. Historical CP4C
registration and harness worktree portability notes remain explicit. No sibling
changes, CP5/W2, CFG/dataflow or cross-repo E2E requalification initiated.
Next action not initiated: human review and later separately authorized integration.
