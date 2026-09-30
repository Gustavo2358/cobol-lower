# STAGE5-ANONYMOUS-LOGICAL-ROOTS

Status: IN_PROGRESS

Scope: lower valid logical character families whose root has a structural NodeId
and no nominal DataId (01 FILLER). Keep named roots and physical admission
unchanged. Allocate an internal abstract text cell identified by the published
root, with source provenance; no forged COBOL name or DataLink. Preserve unknown
filler portions and update named views through the existing family operations.

Contracts: logical-text-w1/w2, StorageFacts NodeId vs DataId, no physical layout
proof from logical coordinates. Qualification includes actual producer SPs,
initialization and writes/aliases, two anonymous roots, unknown filler, profiles,
full pipeline dependencies, and the stage-5 560-case regression population.
RED: original real SP throws NoSuchElementException at LogicalTextMove.initial.
GREEN: logical storage suite and 25 complete producer/CFG/dependency cases PASS.
FAST: PASS (585.001 s); the final expanded anonymous-root suite also passes
against the same compiled production classes. The 560-case campaign comparison
is recorded by analysis-cfg PR #57; all 73 CardDemo cases have already passed
with byte-identical SP and no candidate/support/provenance delta. Physical-mode dependency test
uses --experimental-physical explicitly; logical-only mode does not promise byte
propagation. The new cell is abstract in every anonymous logical-root test.


## Gate scope and remaining limits

The original stage-5 full-local evidence remains recorded in
[shared-routine-bodies](shared-routine-bodies.md). It is not claimed as a fresh
full-wrapper run for this fix. New validation is fixed FAST (all existing scalar,
regional, initial-state and PERFORM suites), nine real-SP anonymous-root controls,
25 four-stage adversaries and the campaign replay. Only admitted logical GROUP
roots receive the new cell; physical allocation, synthetic-model confidence,
entry state, open remainders and kill proofs are unchanged. Unknown filler is
read from the root, never initialized to invented spaces.

Draft #52 carries this fix. Frontend #77 supplies normalization; consumer #57
records integrated qualification. No merge; status remains IN_PROGRESS until
review under repository policy.
