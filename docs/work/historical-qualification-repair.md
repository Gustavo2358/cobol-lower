# Historical qualification repair

- id: HISTORICAL-QUALIFICATION-REPAIR
- status: IN_PROGRESS
- scope: classify and repair historical full-gate failures without weakening dependency candidates

## Real lower defect

The current source topology path replaced CALL with Opaque when all outcomes were
UNKNOWN_LOCAL. This erased the target before dependency analysis, although
PartialProgramAdmission and the existing InvokeHandler admit the target separately
from completion. A real CALL ON EXCEPTION fixture reproduced the defect.

TopologyProgramAssembler now reuses InvokeHandler at this frontier. Literal and
computed targets survive, while the known-outcome list and bounded local target
set remain empty. No handler, following GOBACK, normal edge, memory effect or kill
is inferred. The rest of the source inventory remains explicit and partial.
The rule and scope are in [CALL lowering](../domain/call-lowering.md#unknown-topology-completion).

TopologyEntryAdmissionSuite includes the frozen source-produced SP and COBOL,
literal-target mutation and statement-order permutation. The original assertion
failed before the production change; the new target/absence-of-successor assertions
pass after it. Final gates and corpus results are recorded in
[campaign qualification](cics-control-qualification.md).

A post-CALL boundary adversary first exposed duplicate invocation in the initial
repair. Payload preservation is now limited to a source CALL whose published
outcomes are all UNKNOWN_LOCAL. A modeled CALL followed by an unknown paragraph
boundary retains exactly one Invoke and an Opaque completion frontier. This
negative test prevents the repair from creating a new dependency site or call.

## Closure

Implementation and historical gate repair are complete and approved. Git/PR merge
and required checks establish DONE. Final results, exact reuse boundaries, the
remaining opt-in skip and corpus deltas are in
[campaign qualification](cics-control-qualification.md). Current consumer locks
identify the actual merged upstream revisions during integration.
