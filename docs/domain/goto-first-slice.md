# GO TO first slice

Input: SP 2.1 GoToFact, exact local procedure identity and published executable
entry with occurrence, reference, paragraph and entry provenance. Older decoders
remain unchanged; SP 2.0 rejects the new variant. Rule source is the producer's
`docs/domain/goto-semantic-product.md`, with IBM's unconditional transfer rule.
Target: pinned AIR 2.0, operations §jump and control §1–2: explicit local labels,
no implicit intersequence fallthrough, cycles allowed, no fabricated reconvergence.

Admission indexes statements once and verifies local target, entry membership and
origin agreement. Complete facts emit existing Jump solely to targetEntry. Partial
facts emit Opaque with a unit control frontier and no explicit continuation.
GO TO has no value production or known data effects. The fixed point is unchanged.

The primary DFS follows a precise target exactly as an explicit edge; active-node
cycles fail closed-primary qualification. Intrinsic BASIC bodies must remain
unreachable from ordinary control, including GO TO edges. Arm completion validation
checks normal continuations separately from nonlocal transfers. No source scanning,
name lookup or ProgramPoint-based destination inference occurs in the lower.

Time and memory: linear in admitted statements/edges; finite visited sets. Oracles:
G1–G5, reverse targets, multiplicity 1/2/5/40, partial control, historical decoding,
physical-order independence, and BASIC cycle/overlap counterexamples.
