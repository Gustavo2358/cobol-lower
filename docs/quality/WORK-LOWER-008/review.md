# Focused self-review

Reviewer: implementing Codex, same context. No independent/human approval claimed.
Candidate identity is CP0.json#/candidate_diff_sha256.

The behavioral change is restricted to the historical CI selector: null PR is
accepted only for CP0 after independent merged-PR association and ancestry have
already been established. Explicit contradictory IDs still fail. Branch,
repository/base, merge/event SHA and certificate equality checks are unchanged.
The returned record is untouched; validation of certificate/FREEZE/digest/trailer
remains mandatory. Execution mode still requires an open PR and its exact head.

The tests use actual Git objects and trailers, including a non-ancestor with
identical certificate bytes. They do not mock the Git predicate under test or
fetch network data. The original selector produced assertion RED. New test and
challenge sources were frozen before implementation; challenge restoration is
byte-checked in isolated copies. Historical oracles and production files are
unchanged. The actual failed merge also passed selection and historical Git
validation with the corrected code and original certificate.

Scope includes the new work package, current routing/policy, two new harness
files and minimal run.py changes. Existing registry limitations are disclosed;
no old work package, evidence, Java source, dependency or workflow was repurposed.
Full results, failed attempts, focal proofs and final candidate digest are in the
certificate; remote CI is reported only after terminal verification on the exact
published SHA. No merge/auto-merge or subsequent work item is initiated.
