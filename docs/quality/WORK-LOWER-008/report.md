# WORK-LOWER-008 — initial CP0 merge audit

The first certified CP0 precedes PR creation and can contain `pull_request: null`.
The branch CI accepts that bootstrap, but the prior merge selector compared null
to the subsequently allocated PR number and rejected the otherwise proven merge.
PR8 exposed this as `CI merge/evidence identity mismatch` on main
937a81a23a7c908ed18124b3e8b7adc980ba0235, before lower gates ran. Its tree equals
the approved head 0c4cc7838deccc0323e3650669df350d263489c1.

The correction permits the initial null PR only for CP0 in historical selection.
The existing unique merged PR, exact merge SHA, repository/base, head ancestry,
branch and unchanged certificate checks remain. Explicit wrong PR IDs and null
outside CP0 reject. The certificate is not edited or normalized in memory; the
subsequent certificate/FREEZE/digest/trailer checks still run, and merged state
cannot authorize execution. Current gate and work-item policy now state this
initial-CP0 case explicitly.

## Proof

`test_ci_bootstrap.py` creates real synthetic Git histories: base, certified
feature head, merge, safe evidence trailer and committed JSON. Only API replies
are controlled. The original selector produced the intended AssertionError RED;
the same frozen test now selects the exact historical head and preserves bytes.
These synthetic records contain the fields used by the selector; full certificate
validation is separately exercised against the real PR8 certificate below.
Nineteen focal tests cover the positive bootstrap and matching explicit IDs,
later checkpoints, branch execution, wrong IDs/branches/repositories/base/SHA,
missing/ambiguous/unmerged PRs, changed evidence and unrelated heads. The unrelated
head has a valid trailer and byte-identical certificate in an orphan commit, so
the actual Git ancestry check is the discriminating oracle.

Six durable challenges restore the old comparison or bypass CP0-only, explicit
PR, branch, evidence or ancestry checks. Each must compile, reach its specific
AssertionError, restore exact source bytes and obtain second GREEN. They are
included in the existing full challenge composition, not a separately optional
command. No old oracle or expected value changed.

A separate read-only check used the actual PR8 API response and real Git graph.
It selected 0c4cc7838deccc0323e3650669df350d263489c1 from merge 937a81a and passed
the historical Git/certificate audit with the original record unchanged. This
proves the former failing boundary; it does not claim that the old GitHub run
was repaired or rerun successfully.

## Validation and scope

Final executed gates and their actual counts are in CP0.json. Raw logs include
the initial RED, focal GREEN, challenge receipts, actual merge audit, bootstrap
and full run. An initial docs check rejected a state missing the literal session
mode; that state was corrected and the failed result is retained as FAIL.

All 751 selected product/test/dependency/workflow/historical files remain
byte-identical to base; see preservation.json. No Java, POM, source lock, AIR
library, workflow, semantic contract or old test/evidence file changed. The AIR
snapshot is independently built from its existing ce530a7 lock in a separate
build directory. Runtime complexity, memory policy and capacities are unchanged.

The existing full-harness fixtures assume `.git` is a directory. Local full runs
therefore use a disposable standalone clone with identical candidate files;
the dedicated worktree and other checkouts remain independent. This pre-existing
portability limitation is not changed or used to skip checks.

WORK-LOWER-006/007 registry reconciliation remains a separate inherited lifecycle
limitation. Their frozen packages/certificates are preserved; this CP0 has a new
explicit authorization and does not reuse historical authority. This patch does
not silently relax history registration requirements or claim that reconciliation
has occurred. The old failed job remains historical evidence. Re-running that
old SHA would still execute its old selector.

The final commit is resolved by its Checkpoint-Evidence trailer. Exact published
SHA, PR URL and terminal CI receipt belong to the PR/handoff after push, without
self-inscribing or rewriting the certificate. Human review remains required;
merge and auto-merge are not authorized.
