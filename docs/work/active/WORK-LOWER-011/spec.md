# CP6 W2B specification

Authority: explicit user request 2026-09-12. SP 1.4 W2A product merge
4ffabded1aad39316b8a6f337f732976fdb3ca3e / tree 5880e174b33c85ba3f3cdbc70bd2d8dc7b1d567b.
AIR Java W2C product merge 1d22068e9d9c1d100ecdef734e5b6995252e7ede / tree
ab3a55e13b781ec5424356e83bddbc6fde42dcb1. AIR 2.0.0 normative pin remains
51b4d9a8ae0364232bd97103cd73a77e1a34996c; JSON 1.0.0 DRAFT.

Admit only root explicitly terminated SIMPLE_TEXT_EQUALITY IF with complete
BOOLEAN/PURE/TOTAL predicate, UNKNOWN truth, complete resolved whole-item reads,
complete THEN/ELSE or absent ELSE, exact arm membership/entry/normal completion,
admitted MOVEs, one W1C CALL then GOBACK, and published KNOWN storage independence.
Translate published facts through indices. No COBOL source/AST/name analysis.

Emit Branch(Unknown BOOL, published Read dependencies, NoMemory remaining reads),
THEN and optional ELSE sequences ending with Jump to the same CALL merge label,
Invoke terminator and explicit Return sequence. Absent ELSE goes directly to CALL.
Assembly returns entryLabel explicitly. Preserve SP proof members/order/authority
and provenance as DisjointStorage premise; lower does not prove independence.
Preserve W1 fitting, CALL unknowns, and historical SP 1.3 interpretation.

O(statements + branch relations + data + proof members) traversal with indexed
joins; no pairwise disjointness. Physical sequence ordering never determines control.
Use LocalIds with owner/semantic role/source identity. AIR Validator traverses all
output; preserve semantic obligations. Every positive output round-trips through W2C.

Remote CI has one checkpoint check: fixed FAST suite or conservative DOCS_ONLY.
Executable orchestration guard must reject heavy direct/indirect remote calls,
including manual workflows. Full/performance/mutations/historical qualification
and repeated real-producer E2E stay local only. After focused RED/GREEN/mutations,
restore exact bytes, commit a clean candidate, then run full local qualification.
Receipts are outside the qualified tree. Later docs-only commits require protected
blob equality and docs/integrity checks, never retroactive FULL claims.

No W2D, CFG, solver/lattice, runtime condition evaluation, pruning or generic control.
