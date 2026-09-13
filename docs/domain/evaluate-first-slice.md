# EVALUATE first slice

SP 2.0 adds a closed EVALUATE family and EVALUATE_ARM containment. Wire20 is a
distinct decoder; 1.1–1.9 retain their existing wire and interpretation. The
source rule is [IBM Enterprise COBOL 6.4 EVALUATE](https://www.ibm.com/docs/en/cobol-zos/6.4.0?topic=statements-evaluate-statement),
published by the frontend as explicit ordered literal selections, DATA reference,
arm entries/members, OTHER presence and normal completion.

One generic AIR Branch per WHEN implements first-match order. Its true edge
targets the published arm entry; false targets the next decision, OTHER or the
published no-match continuation. Bodies keep their independent MOVE/CALL/IF/BASIC
PERFORM/GOBACK or conservative rules. Neither ProgramPoint nor array order supplies
an edge. No scalar conversion, comparison solver, path pruning or AIR change.

The existing Unknown BOOL abstraction preserves both outcomes. Literal and subject
reads are operands; unknown subject/storage retains a memory-read remainder.
Selection, subject, entry and completion origins feed the branch's derived origin.
Unknown values alone never suppress an arm or add write effects. Typed empty or
unavailable control falls back to Opaque; observed variants retain open control.

Admission indexes ownership once and checks distinct arms/operands, explicit
entries, member closure and continuation outside arms. Lowering visits each arm
once: linear work/storage in facts and members, with finite traversals. Physical
canonicalization remains independent of control. BASIC activation membership may
follow EVALUATE/IF entries in the proved primary region; target paragraph, linear
MOVE body and unique resume requirements stay intact.

A BASIC body can only be specialized when its target is disjoint from a closed
primary execution region. The primary worklist follows only proved IF/EVALUATE
entries and normal continuations; GOBACK closes a frontier. A missing successor,
unproved arm or cycle cannot prove a returning region. Completed shared joins
remain reusable. The iterative traversal uses linear work/storage in nodes and
published edges; no source order or new terminal semantics supplies closure.
The focal regression includes MAIN without GOBACK, open IF/EVALUATE arms and a
cycle, alongside the valid BASIC primary and EVALUATE cases.

Permanent multiplicity is 1/2/5/40; SP1.8 historical generators keep their original
families and EvaluateIntegrationSuite covers the SP2 family. Oracles include
three-way/no-match joins, strong updates, per-arm CALL sites, unknown subject,
partial body and IF/PERFORM composition. Contradictory ordinals and future fields
are rejected. Physical field/statement order and AIR codec round trips preserve
bytes. Runtime targets remain partial under existing source/interpretation/control
remainders; the known-subject fixture is intentionally not path-sensitive.
