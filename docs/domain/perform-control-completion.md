# PERFORM control completion — design before implementation

Status: IN_PROGRESS; user authorized implementation and Draft PRs, no merge.

## Discovery and authority

The current SP topology is authoritative for control. Baseline product evidence in
`artefatos-e2e/carddemo-recall-20260926/historical-regression-current/perform`:
29 and 30 publish UNKNOWN_LOCAL for the EXIT; CALL exists but is unreachable.
37 rejects a resolved SECTION because topology target resolution accepts paragraphs
only. 38 rejects more than one VARYING level before creating a binding. For 37/38
closure therefore never inventories the continuation CALL into executable AIR.
AIR/CFG/dependencies preserve these missing control facts, without reconstructing
COBOL. The earlier paragraph-profile restrictions are separate from this route.

IBM Enterprise COBOL 6.4 Language Reference, printed pp. 344–345, 413–414,
420–423, read from the preserved official PDF/text on 2026-09-26:
https://publibfp.dhe.ibm.com/epubs/pdf/igy6lr40.pdf
EXIT PARAGRAPH transfers to paragraph completion. EXIT PERFORM transfers after
the lexically enclosing inline PERFORM, bypassing repetition; outside inline it
is ignored. CYCLE transfers to iteration completion. A SECTION invocation ends
at its own section boundary. VARYING initializes all controls, tests outer to
inner (BEFORE), increments the inner control and carries outward with inner
resets. AFTER executes the body before its tests and bypasses updates on exit.

## Representation and obligations

Extend existing grammar-owned topology, without adding an AIR capability. Typed
regional escape targets distinguish a scope exit from normal body completion.
The lower binds them to the correct enclosing activation; memory remains shared.
Section entry/boundaries come from AST ownership and resolved procedure symbols.
Loop phases carry a typed level, selecting the corresponding condition/control
payload. Numeric values stay explicitly open; exact footprints and predicate
reads remain local. No constant count unrolling, dependency-specific edge, source
reparse, ALTER change, or expected relaxation.

All graph walks terminate using visited sets; contexts retain existing recursion
refusal. For L controls, BEFORE reset phases use O(L²) space and AFTER uses O(L); activation expansion remains
output-sensitive as documented in compositional-perform.md. Unsupported operands
retain explicit partial effects/control and never become an ONCE invocation.

## Evidence plan

Independent source topology assertions RED first, then consumer AIR edge/effect
assertions and source adversaries: dead tails, nested exits, paragraph escape from
inline loops, repeated callers, section sentinels and mixed ranges, 2/3-level loops
with both TEST modes, reset/update ordering, body CALL and continuation CALL.
Run unchanged original 39 oracles; frontend/lower FAST and affected boundary
checks; CFG FAST when repinned. Real CardDemo corpus required because the topology
changes general dependency reachability; compare relations and source supports,
investigate every delta, preserve raw products and hashes. Existing PARTIAL
numeric/recursion and unrelated gaps stay explicit.

Legacy paragraph qualification diagnostics (including AFTER outside its historical
single-level profile) remain on the legacy fact surface. They do not override the
current topology. Unsupported integer operands keep partial memory effects; values
are open even when control and the write footprint are known.

Paragraph escape unwinds inline frames only. An out-of-line activation survives an
explicit GO TO outside its range; reaching the published endpoint later still
returns to its caller (IBM p. 414). A separate adversary reproduced loss of that
return before this distinction; both AIR and handler summaries now preserve it.

## Review tests

The fixed FAST profile includes 11 new SP snapshots with source fixtures, eight
malformed contract mutations, AIR phase/footprint assertions, codec roundtrip and
permutation checks. It also checks partial operands without duplicate identities
and a procedure endpoint reached after an external GO TO and EXIT PARAGRAPH.
Future-version rejection tests now use 2.49; they retain their original strict
UNSUPPORTED_CONTRACT expectation. Existing semantic fixture expected values remain
unchanged. Final gate/corpus results are reported in the Draft PR.
