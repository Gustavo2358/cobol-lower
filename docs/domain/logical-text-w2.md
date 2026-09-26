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
