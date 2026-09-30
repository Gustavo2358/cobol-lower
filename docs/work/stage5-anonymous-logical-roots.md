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
GREEN: logical storage suite and 22 complete producer/CFG/dependency cases PASS.
FAST and 560-case preservation replay in progress. Physical-mode dependency test
uses --experimental-physical explicitly; logical-only mode does not promise byte
propagation. The new cell is abstract in every anonymous logical-root test.
