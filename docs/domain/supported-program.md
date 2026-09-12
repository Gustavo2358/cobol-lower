# SUPPORTED_CP6_PROGRAM

The public lower composes all admitted root MOVE, CALL, simple IF, at most one
isolated BASIC PERFORM, and final GOBACK from typed SP facts. SP1.7 uses the same
wire fields as SP1.6 and explicitly generalizes the PERFORM primary invariant.
The SP1.6 decoder preserves its earlier MOVE* / PERFORM / CALL / GOBACK restriction.

Authority: pinned SP1.7 contract, existing MOVE/IF/CALL/PERFORM contracts, and
analysis-ir 2.0.0 operations/control at the unchanged source lock. IBM Basic PERFORM
pp. 413–414 was rechecked 2026-09-12 in the official manual linked by perform-basic.md.
No COBOL, AST, display-name control, solver, new AIR operations or semantic defaults.

Admission follows the explicit Entry.start and normalContinuation relations,
checks root containment and cycles, validates each IF's existing predicate and
linear arms, and validates the single target's ordered MOVE body and unique resume.
Primary roots, IF arms and target must cover the whole observed statement inventory
exactly once. Unknown/unsupported/unaccounted facts refuse. No CALL/IF count limit.
Missing continuations block; dangling identities are invalid; other shapes refuse.

SupportedProgramAssembler holds root MOVE instructions until a terminator. Each
CALL emits one Invoke to the SP continuation and starts a new sequence. IF uses
IfSequenceAssembler's existing Branch/predicate/arm/Jump translation. PERFORM uses
PerformSequenceAssembler's existing target, isolated-return provenance and MOVE
translation, with an arbitrary supported root as resume. MoveHandler, InvokeHandler,
GobackHandler, ScalarDataTranslator and StoragePremise retain their semantics.
Output assessment remains AirValidator. Explicit Entry is independent of physical
sequence order; source links correlate every OperationId and its actual sequence.

The composed path uses SupportedProgramAdmission/Lowerer/Assembler. Earlier focused
admission APIs and assembly classes remain for compatibility and historical local
challenge tooling; they do not decide the composed public path. No new universal
statement framework is introduced. Identity hashes the canonical admitted primary,
arms/body and data once, then each typed control fact; it never repeats all data
per CALL. Work is finite O(statements + references + declarations), plus existing
canonical data ordering, with no path enumeration or backward target resolver.

Isolation still needs one PERFORM, one distinct paragraph target, MOVE-only body,
primary final GOBACK, no other modeled target entry. Multi-PERFORM, THRU, loop,
inline, nested PERFORM, GO TO, EVALUATE and alternate entry remain outside scope.
External CALL effects and open source/name-policy remainders are unchanged.

MultiCallIntegrationSuite checks all seven real SP snapshots through the memory
port, every MOVE/control edge, distinct operations, target identity, source closure,
SP permutation and refusals. The CallOracle now follows explicit Entry/Return
identities instead of assuming physical sequence positions. The separate CFG E2E
checks all site values/supports/remainders, global deduplication and A/B bytes.
