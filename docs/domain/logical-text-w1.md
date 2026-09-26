# Historical W1 property; continued by [logical storage W2](logical-text-w2.md)

# Logical text W1

id: logical-text-w1; status: IN_PROGRESS; scope: literal-to-group projection without physical profile.

The producer-owned SP 2.29/storage 1.9 extension supplies logical character coordinates, not byte layout. `LogicalTextIndex` validates identity, parent/root closure, complete partitions, exclusions and scalar lengths. `LogicalTextMove` folds fit-parent/slice-child for a literal source into existing AIR Assign/Literal/ObjectPlace contracts. No mutable source is reread, no observation intervenes in the sequence, and sibling knowledge is independent. A later child write updates its cell, not a cached whole-group string. VALUE uses existing entry-state authority. Source provenance and storage identity include the logical proof.

Only fixed PIC X/DISPLAY ordinary independent WORKING-STORAGE roots are eligible. FILLER contributes extent without creating a named cell. REDEFINES, RENAMES, numeric/representation-sensitive storage, dynamic extent and group-to-group copies are outside this rule. Unproven writes remain conservative effects. No encoding, Region or physical ViewBinding is fabricated. The proof cannot accompany a selected physical profile.

AIR wire/schema and air-java/analysis-ir are unchanged. The adapter retains legacy SP decoding and explicitly accepts the new extension; malformed coordinate proofs are rejected. The source-produced fixture tests projection and AIR JSON round-trip, with invalid proof mutations. Campaign design and empirical evidence live in artefatos-e2e/logical-text-w1-20260918.

W2 needs a separate capture/alias/correlation design; this rule does not authorize child-to-child group copying. The downstream W1 pivot selects logical-only by default and isolates physical propagation behind explicit experimental opt-in. Corporate execution is not a blocking W1 acceptance gate by user decision on 2026-09-18. Synthetic source E2E and boundary tests qualify the new property; corporate ON/OFF observations are user-reported, not a new measured run. Ready for human review; lifecycle remains IN_PROGRESS until merge. The synthetic 10,000-group serializer exhaustion is NOT_MEASURED_FOR_ANALYSIS, not a requirement to optimize serialization here. No merge performed.
