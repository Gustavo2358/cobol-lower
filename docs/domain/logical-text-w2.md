# Logical storage W2

Continues PR #31 and branch feat/logical-text-w1. No physical profile, encoding
inference, physical Region, or new solver is involved. No automatic fallback.

## Representation and authority

The frontend publishes fixed textual logical views (node/root/start/length),
REDEFINES component relationships and proved RENAMES endpoints. Lower validates
root/parent containment, coverage, common source identity and endpoint bounds.
It does not parse PIC or recompute COBOL layout. Unsupported layouts and copies
within a shared family stay conservative.

One root TEXT Cell carries a family's sequence. Existing AIR Read, FitText,
SliceText, Binary(CONCAT) and Assign update that sequence, then adjacent assignments
project named views. A partial view write keeps prefix and suffix from the old
root. Existing entry values initialize a structural root without erasing known
child VALUE facts. FILLER occupies its original positions.

Group copy captures the source sequence before fitting the destination and
projecting its children: ABC|DEFGH -> ABCD|EFGH. Source and destination roots are
independent Cells; changing the source later cannot change the copied value.
REDEFINES shares a root; it is aliasing, not copying. RENAMES is the already proved
range between its endpoints. Overlapping group copies are refused in this wave.

Identical-range shared cells alone cannot handle different splits. Independent
segment cells would lose branch correlation at joins. Root composition permits
the existing downstream scalar solver to retain whole alternatives and partial
positions. No AIR schema/model variant was introduced; air-java PR #20 transports
the existing slice/concat forms and validates statically bounded slices.

## CALL outcome correction

Source E2E RED exposed arbitrary backward jumps from the old AllControl CALL
remainder: snapshots acquired ZZZD and a branch created impossible mixed names.
For the admitted CALL surface with absent handlers and a proved normal successor,
unknown outcomes now use UnitControl with labels=false. Normal return keeps the
published successor; normal/exceptional exit, halt, divergence, external control
and foreign memory effects remain open. Unknown/present handlers retain the wider
frontier. Generic AIR Opaque/control behavior is unchanged.

Authority: IBM Enterprise COBOL 6.4 [CALL](https://www.ibm.com/docs/en/cobol-zos/6.4.0?topic=statements-call-statement)
and [Language Reference, EXIT PROGRAM](https://publibfp.dhe.ibm.com/epubs/pdf/igy6lr40.pdf),
plus the published CALL surface/normalContinuation contract. This is source-fact
lowering, not dropping unknown paths in the solver.

## Qualification and limits

Local FAST PASS on 2026-09-18, including legacy wire, physical tests, CALL, IF,
PERFORM, EVALUATE, GOTO, FILE and CICS. W1 group test now checks root composition
and character projections through exact AIR round-trip; malformed inventory
rejection oracles remain. Source E2E fixtures and the runner live in analysis-cfg
under logical-text-w2 / scripts/project/e2e_logical_text.py. Synthetic W1 A-D,
group fitting/snapshot, overlays, RENAMES/FILLER, VALUE, composition/correlation and
open/unsupported cases passed the real CLI pipeline without logical/physical flags.

Pins: frontend 84845762c58ba0f64199bf01db9afce4c97a939b (see exact immutable SHA in sources.lock.json), air-java
646ca3ab1687d43f7d2063fc2a8f3837ab3cf9fa (exact immutable SHA in sources.lock.json). Runtime qualification uses these
producers. Physical engine remains EXPERIMENTAL / NOT PRODUCTION QUALIFIED.

Outside scope: general refmod, COMP/COMP-3/BINARY, NATIONAL/DBCS, dynamic extents,
OCCURS/ODO, codecs, arbitrary physical aliasing and overlapping group copies.
Corporate NOT REEXECUTED / NOT AN ACCEPTANCE GATE / NO NEW NINE-TARGET CLAIM.

Adversarial unequal-extent overlay: an original X(8) overlaid by a 4+8 group has
an unknown tail at [8,12). Initialization must retain the uncovered suffix of an
overlapping longer leaf; fitting only the original eight characters would invent
four spaces. The source fixture first exposed the false candidate EFGH (RED),
then returned an open value with no invented target (GREEN). The lower FAST suite
retains the independent full-initial-extent assertion and the real producer SP.

## Alias MOVE follow-up — rule and oracle

The current frontend still publishes a proved logical family for nested textual
REDEFINES (same root, start and extent). An elementary MOVE can additionally have
FULL_IDENTITY or POSSIBLE_TEXT. Those copy classifications do not supersede the
published family: writing a named view must update that family and project its
other views before continuing. IBM Enterprise COBOL 6.4, Language Reference,
REDEFINES considerations (printed pp225–227), says all descriptions of the area
remain in effect; MOVE through either name addresses the shared area.
Primary source checked: https://publibfp.dhe.ibm.com/epubs/pdf/igy6lr40.pdf.

Discovery: RegionalMoveHandler's scalar shortcuts run before LogicalTextMove,
so an equal-width literal is assigned only to a projection Cell. Separate view
Cells are intentional W2 representation; the missing root update and projections
are the defect. SP, AIR expressions and CFG already express the required facts.

Use the existing admitted logical-family translation before scalar shortcuts.
Its admission requires published whole-item coordinates and a literal or a source
in a different family. Preserve current refusal of overlapping DATA copies,
physical-profile behavior, multi-receiver admission and open remainders. Cost
remains one root expression and one projection per named view of the destination
family; finite published views bound translation. No source parsing or new alias
analysis belongs in lower.

RED oracle: current real-producer SPs nested-overlay and nested-overlay-perform
must assign PROGA001 to the root and both views in the MOVE sequence. Existing
AIR validation/codec roundtrip and malformed coordinate tests apply. Source E2E
must check forward/reverse alias writes, overwrite, copy capture, partial overlays,
PERFORM continuation, branch correlation and absence of dead/foreign candidates.

The literal-fitting adversary also exposed stale scalar-profile diagnostics in
PartialProgramAdmission: logical fitting was recognized as eligible but the
narrower scalar refusal prevented emission. Once published-fact validation passes,
positive logical-family admission discharges only those scalar capability
diagnostics, just as the existing regional translation does. Invalid facts still
reject in Phase A. Literal-fit retains its independent truncation oracle.


## Alias qualification closeout

The alias correction is reviewed in [PR #37](https://github.com/Gustavo2358/cobol-lower/pull/37)
and the consumer fixtures/pin in [CFG #48](https://github.com/Gustavo2358/analysis-cfg/pull/48).
Local and remote FAST passed. Source adversaries are 14/14; PERFORM stays 39/39
and its adversaries 25/25. CardDemo's 73 programs preserve all program/file/source
relations and dependency support provenance.

The subsequent chaos oracle audit gives 48/48 with unchanged product bytes:
a dead CALL need not have an executable site, but must remain in source inventory
without candidates. Six fixtures intentionally check current abstraction limits
(ALTER, unmodeled logical INITIALIZE, and loop predicate correlation). Historical
W2 product checks remain 23/24; the ACCEPT expectation and old all-Cell inventory
assertion are preexisting limits, not newly qualified behavior.
