# WORK-LOWER-008 — state

CP0 authorized in single-checkpoint mode by the 2026-09-09 user request to apply
the investigated patch and open a new PR. Phase ready_for_review; branch
fix/ci-initial-cp0-merge from 937a81a23a7c908ed18124b3e8b7adc980ba0235.

Initial assertion RED reproduced before implementation. Minimal historical CI
selector patch applied; 19 focal tests PASS and six mutations killed with exact
restore and second GREEN. Actual PR8 merge now selects its original certified
head and passes historical Git/certificate audit with no record rewrite.

Final full PASS: docs, architecture, 203099 semantic assertions, 93 performance
assertions, Git, 161 harness tests and 63 restored challenges. Sources/tests byte-identical to the
standalone validation clone. 751 product/test/pin/workflow/historical files
preserved. Initial docs failure for a missing session-mode token is retained.

[Report](../../../quality/WORK-LOWER-008/report.md),
[certificate](../../../quality/WORK-LOWER-008/CP0.json),
[self-review](../../../quality/WORK-LOWER-008/review.md). Commit resolves through
Checkpoint-Evidence; exact final SHA/PR/remote CI receipt belong in the PR/handoff
after push, without recursive certificate rewriting. Human review pending.

Old failed run and WORK-LOWER-006/007 registry reconciliation remain explicit
limitations. No production, dependency, workflow, sibling or old evidence change.
No merge/auto-merge; next work or historical reconciliation not initiated.
