# CardDemo values and remaining entry/control semantics

- id: CARDDEMO-VALUES-CONTROL
- status: IN_PROGRESS
- scope: the four capability families authorized after W8; separate review PRs, no merge authorization.

## Checkpoints

1. **VC0 — discovery and independent oracles.** Reproduce each family through
   frontend → SP → lower → AIR → CFG → dependencies, under both unspecified
   logical storage and the explicit IBM physical profile. Distinguish missing
   value facts, admission, control and source qualification. Successful process
   exits are not semantic PASS.
2. **VC1 — MOVE receiver sequences.** Preserve receiving order, sending operand
   evaluation, alias-family coherence, fitting and unaffected data. Cover
   literals, figuratives and data sources; retain explicit limits for conversions
   and overlap whose result the language does not define.
3. **VC2 — indexed text.** Publish source-proved table selection and initial text
   slices with identities and uncertainty. Unknown index is a set of possible
   in-range elements plus an open remainder, never arbitrary neighboring data.
4. **VC3 — expressions and special registers.** Separate pure text transforms
   from runtime inputs and implicit runtime state. Known receiver effects must
   not poison unrelated data. Do not invent CURRENT-DATE values or treat a
   compiler register as a user storage declaration.
5. **VC4 — external control and entries.** Model lexical SQL WHENEVER directives,
   dynamic SQL completion/effects, CICS condition dispatch and alternate entries
   at their canonical owners. An alternate entry is not an edge from the primary
   entry. Preserve handler/caller context and unavailable external effects.
6. **VC5 — qualification and review.** All 73 CardDemo programs, PERFORM 39,
   Chaos 48, aliases 14, PERFORM adversaries 25, frontend fixture corpus and CICS
   qualification; changed-repository FAST and impact-selected local full gates.
   Investigate each lost candidate/support using positive kill/exclusion evidence.

Each implementation checkpoint records its contract, tests, exact pins and known
limits. Commit checkpoints progressively. No shared routine bodies (point 5),
ALTER, UI changes, fabricated CFG edges, candidate injection or weakened oracle.

## Rule and ownership

IBM Enterprise COBOL 6.4 [MOVE](https://www.ibm.com/docs/en/cobol-zos/6.4.0?topic=statements-move-statement)
requires receiving order and evaluates sending subscripts/reference modification
and functions before the first receiving transfer. Receivers evaluate their own
selection immediately before their transfer. Overlap does not grant a defined
copy result. These are LANGUAGE_GUARANTEED premises, not corpus observations.

The frontend owns binding, source layout, expression classification and COBOL
control. SP transports typed canonical facts. Lowering composes existing generic
AIR operations where sufficient; CFG projects actual AIR terminators. Source
qualification may preserve possibilities but cannot manufacture executable flow.
Synthetic IBM declarations remain modelAssumed and never prove runtime values,
physical layout, branch exclusion or a strong kill.

## Validation design

Derive focal expected behavior from language rules before implementation. Assert
values, ordering, reachability and provenance rather than operation counts alone.
Challenge aliasing, unknown values, missing inputs, nesting and alternate routes.
Preserve raw RED outputs and use separate directories for GREEN runs. Reuse W8
baseline evidence only while its assumptions remain valid; mark reused evidence
explicitly. Whole-corpus counts are telemetry, not a replacement for the oracle.

Discovery records and raw products: workspace `.carddemo-values-control/evidence`.
VC0 starts from the merged W8 mains; the baseline manifest and runtime are frozen
in `.carddemo-values-control/baseline.json`.

## VC1 checkpoint — logical receiver sequences

The canonical storage analysis now prepares logical MOVE sequence facts. Data
sources require all receivers to have a proved logical family distinct from the
sending family. Literal text and SPACES may admit individual receivers. SPACES
normalizes to the space character and fits each receiving extent; the explicit
IBM1047 profile also receives its encoded bytes. LOW/HIGH and numeric conversion
are not inferred from this text rule.

Projection transports these facts and keeps the source logical identity on every
transfer. Lowering updates each root and all its alias views before the following
receiver. Operation IDs distinguish transfers. Undefined/unproved overlap keeps
its existing conservative boundary. There is no new CFG rule.

The proof pass is linear in the receiver count plus emitted fitted text. Lowering
is linear in receivers times affected alias views, with ordinary finite AIR text
expressions. The disjoint-family requirement is conservative: same-family data
copies remain outside this extension even if their individual ranges might be
disjoint.

New evidence: five frontend semantic tests (including physical SPACE encoding),
five real SP → AIR receiver/alias oracles integrated into LogicalTextStorageSuite,
and five four-stage probes with expected candidates. All passed. Existing frontend
FAST passed 648 tests before registering the new class; lower FAST including the
new oracles passed. Final campaign gates/corpus are pending. RED evidence is
`vc1-red-02.log`; GREEN focal evidence is `vc1-front-05.log`,
`vc1-probes-01/`, and `vc1-lower-fast-02.log` in the campaign evidence directory.

In the SPACES probe, prior program names disappear only after the now-executable
full receiving write. This is a positive kill, not exclusion based on missing
evidence. The campaign remains IN_PROGRESS; VC2–VC5 are not qualified yet.
