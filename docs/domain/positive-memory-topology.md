# Positive memory topology — W1

W2 continuation: the W1 contract remains authoritative. W2 propagates the same
positive publication rule to IF/EVALUATE/PERFORM/GO TO, Opaque, FILE and CICS
helpers. A source gap by itself no longer activates broad memory, control or
foreign effects. Known operands and real external input/read/return effects stay
executable. Unmaterialized declarations with no logical value domain or physical
view contribute coverage instead of an AllMemory alias; declarations required by
published captures, global data or FILE records keep their identity. The W2
checkpoint and integrated oracles are in the anchor W2 report. This extension
does not change AIR, the logical default or physical admission.

Status: IN_PROGRESS; campaign `POSITIVE_MEMORY_TOPOLOGY`, companion to analysis-cfg #45.
The accepted W0-R1 and W1 product policy supersede the older requirement that every
unimplemented source effect be approximated by havoc or a broad envelope.

## Rule and representation

The lower consumes published identities, source view ranges and logical family
coordinates. Distinct AIR StorageId values denote independent model bases. Shared
views and aliases still name the same base; no source disjointness matrix is needed.
The lower therefore no longer emits DisjointStorage premises for scalar or regional
storage. Supplied legacy SP assertions still undergo structural validation; absence
of that optional evidence no longer rejects a supported scalar IF.

Known text declarations and group/member projections continue using the existing
ScalarDataTranslator, RegionalDataTranslator and LogicalTextMove mechanisms. A
producer diagnostic for an omitted representation aspect does not instruct lower
to replace available logical/physical facts. Group copies use published character
or byte ranges and snapshots, including different source/destination partitions.
The lower never calculates a missing source offset, width or codec.

Coverage and Precision are descriptive publication metadata. ScalarEvidence.limited
continues reporting uncertified dimensions, without assigning those dimensions
executable meaning. Unknown expressions, explicit bounds and typed writes retain
their semantic meaning. Consumer migration is synchronized through campaign pins.

## Contributions changed

- CALL with a published normal continuation preserves target (literal, nominal
  Read or known alternatives), source site and continuation. Missing body/signature
  implementation contributes diagnostics, not foreign AllMemory reads/writes or
  additional nonlocal outcomes. Unknown runtime name policy remains distinct from
  unknown memory effects. Actual modeled arguments/results are unchanged; current
  SP CALL surface does not provide parameter/result value operands.
- The older MOVE MUST_UNKNOWN source effect and ConservativeMove fallback arise
  from unimplemented transformation/encoding, not modeled external input. They
  publish an abstract Nop with coverage and derived statement/source/target origins,
  not a substitute HavocMust. Supported fitted text, literal bytes, CopyBytes and
  logical family transfers retain their original implementations. Nop has no operand
  slots; no dangling synthetic operand correlations are created.
- Input/return effects explicitly modeled as Unknown or Havoc in AIR, FILE effects,
  and existing CICS controls are unchanged. This is a source producer correction,
  never a consumer filter that deletes typed semantic uncertainty.

## Limits and integrity

CALL without a materialized normal destination still has the existing open
control remainder. Its source completion projection is a W2 control issue; an empty
closed InvocationOutcomes is invalid AIR and is not emitted. The W1 two-CALL witness
has explicit normal continuations and no such remainder.

Wholly unrepresented declaration types/layouts outside the known-text W1 projection
retain the existing nominal publication path. In particular, this change does not
invent independent scalar cells within a positively shared family merely to hide an
unavailable layout. Completing that general projection remains explicitly pending.
No source gap suppresses identity/reference/type/range integrity checks.

Complexity is unchanged for actual typed transfers. Removing premise construction
removes the redundant storage-membership publication pass; no replacement negative
matrix is introduced. Copy and branch complexity follows existing supported ranges
and alternatives. No operational physical default is changed here.

## Oracles and contract deltas

`PositiveTopologySuite` (included in FAST) failed before the implementation with
`W1 unknown body does not manufacture reads`; it requires absent compensation reads,
writes and outcomes while preserving computed target Read and coverage. Existing
IfSuite now admits the same supported graph without negative storage evidence.
Existing CALL/IF oracles now require empty foreign effects/closed normal continuation;
existing regional and partial-MOVE tests require a diagnostic Nop for omitted
transformations. These are intentional contract deltas, not relaxed assertions.
FILE external-input tests still require their genuine strong/possible writes.

Adapter suites roundtrip the new Nop through the existing AIR instruction kind;
missing Nop codec support is repaired in the coordinated air-java companion.
The anchor report records final exact pins, integrated physical/logical witnesses,
checks and remaining mechanisms. No W2/W3 or merge is authorized.


## W1 local validation

`python3 -B scripts/harness/lean.py fast` passed on Java 21.0.12 with the exact
companion pins in sources.lock.json: 2,466 core assertions including 12 new positive
projection checks; the complete fixed adapter profile (including FILE, CICS,
source dependencies, regional groups/overlays/RENAMES, MOVE sequence and supported
control fixtures); architecture bytecode checks; and 21 harness tests. Gate elapsed
159.834 seconds. The adapter profile reports four focal entry cases in addition to
its named suites, not a total assertion count.

Two implementation defects were caught and repaired during RED/GREEN: baseline
AIR codec lacked existing Nop transport (companion repair), and multi-receiver
omitted MOVEs initially shared an operation ID (target operand identity now
participates). Identity validation remained enabled throughout. No corpus golden
was relaxed. The anchor's four new sources also passed frontend→lower under both
logical and IBM1047 producer profiles; the physical group-copy witness emitted two
eight-byte Regions and Assign→CopyBytes→Assign. Full consumer/solver observations
are owned by the anchor campaign report; these producer checks alone do not prove
physical execution or qualify operational use.

AIR harness reconciliation: pin `26016f10460336f237a33b2ed126a6a1427f0207` replaces `00373f638039c580bb833b9949df12cb8218e2fb`. Git diff confirms no AIR production sources or POM changes; the fixed module-policy gate reads the active lock rather than a historical literal. Existing semantic evidence remains equivalent; lower FAST is rerun for exact pin resolution.

## W5 — partial structural facts

SP 2.36 carries PERFORM_PROCEDURE `publicationKind: STRUCTURAL_FACTS` and optional
independent `targetEntry`. The decoder retains target, entry, membership, normal
completion frontiers, each activation's resume, provenance and diagnostics. In-memory
LEGACY_PROFILE and all old decoder versions preserve historical meaning. Old versions
reject the new fields. New facts contribute to deterministic identities.

This wave adds acceptance and consistency checks only. STRUCTURAL_FACTS does not
enter legacy executable range specialization; nested return composition remains W6.
Legacy range facts already consumed before W5 keep their existing admission (33-C1
is a compatibility control). No source scanning, broad fallback or AIR/CFG semantic
change is involved. PartialStructuralFactsSuite runs in FAST alongside historical
decoder suites; it checks wire round-trip, old-version rejection, diagnostic-only
mutation and invalid membership. New fixtures are producer-generated; historical
fixture bytes are unchanged. The SRC-SP pin records the W5 producer HEAD.

## W6 — compositional PERFORM lowering

[Control model and algorithm](compositional-perform.md): positive structural ONCE
facts now enter activation-specialized AIR control. A nested context retains its
parent; conditional completions traverse their own resume blocks. CALL coverage
keys are occurrence-specific. No AIR/CFG production or SP wire change is required.
The W5 acceptance-only boundary above is historical; its facts are now executable
under the documented W6 preconditions. Unsupported repetition remains explicit.
CompositionalPerformSuite joins FAST and preserves qualified/context/dead controls.

## W8 — independent MOVE receivers and CICS options

SP2.38 is selected only when a multi-receiver MOVE publishes independent logical
receiver values or a partial regional sequence. The decoder preserves SP2.37 and
older meanings, rejects the new field under an old version, and validates each
logical value against the declared receiver extent and the proved literal.
`RegionalMoveHandler` follows the published receiver order: a proved logical
receiver becomes an ObjectPlace Assign; an unavailable regional receiver remains
an Nop with uncertainty. The latter cannot erase a peer Assign. An explicit
REDEFINES/RENAMES identity is still shared, so a later write through an alias
replaces its peer value. DATA-source overlap still follows the prior capture
admission and may remain conservative.

For CICS Program Control, target materialization does not decide whether a known
COMMAREA read or RESP write exists. `Invoke.effectOperands` retain source option
places and `ForeignEffects` now carry separate local ObjectsMemory bounds for
proved read/write objects. The targetless Opaque path retains its exact known
operand IDs and NoMemory remainders. Neither path infers a write from a missing
target or from diagnostics. LINK normal continuation and XCTL no-return behavior
remain governed by their existing control contract. Scope precision is object
level when the AIR foreign bound cannot name a subobject interval.

For CICS FILE SYSID, a whole textual reference with declared logical extent four
may be read through its nominal ObjectPlace when no byte view is available.
Physical byte interpretation still requires its existing codec/view proof;
integer options do not acquire this textual rule. Opaque `unknownExposureBound`
is no longer mapped to `otherWrites`; only `unknownWriteBound` authorizes that
remainder. `unknownReadBound` remains separate. W8 tests cover the wire, target,
read/write and logical access mutations in addition to the integrated corpus.
