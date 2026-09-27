# Incomplete native file effects

Status: IN_PROGRESS, awaiting review. Scope: F04/F07 follow-up to the storage fixes.

The producer previously discarded the entire file memory model without both FD
and SELECT, then published false/false unknown bounds. Validated admission refused
missing buffer or FROM effects. The producer now retains independent declarations
and explicit receivers and publishes uncertainty for observed unbound operations.

No lower production change is required. The existing SP 2.48 effects contract,
FileEffectAdmission and FileResourceLowering already preserve known steps plus
open MAY bounds. No empty-plan exception or validation relaxation is introduced.
No AIR/CFG schema or algorithm changes, no ALTER support.

`IncompleteFileEffectsSuite` freezes the three failing compilation envelopes.
It checks admission, retained partial status, known buffer/FROM identities,
MAY effects, deterministic unit permutation and AIR JSON roundtrip. Counterfeit
closed plans with buffer/FROM steps removed still reject. The existing
FileMemoryEffectsSuite checks strong-effect preconditions and outcome ordering.
Seven E2E additions independently assert old candidates and remainder after MAY,
new candidates and removal of old candidates after a proved MOVE, and preservation
of an unrelated source. The existing six storage adversaries remain unchanged.

The producer's conditional effect rules are normative; the lower does not inspect
COBOL text, choose owners, infer aliases or reconstruct receiver bounds. Source
provenance travels with each retained step through the existing SourceOrigins.

## Validation and pin

Producer pin: `4802305d17e7b1684d1f749618aa76fe8e091e0f`; SP 2.48 unchanged.
Producer FAST: 589 PASS. Lower focused incomplete-file and existing file-memory
suites PASS. Integrated matrix: 310/310 complete. PERFORM 39/39, Chaos 48/48,
PERFORM adversaries 25/25, aliases 14/14 and storage adversaries 13/13 PASS.

All 307 previous successes preserve candidates, supports and 12,454 dependency
provenance origins. Four old products have explained metadata/effect differences;
303 are unchanged. Eight replaced control-derived origins in nested-global-file
were not referenced by dependencies. CardDemo's 73 SPs are byte-identical to the
prior execution; unchanged consumers preserve 121 program / 271 file / 523 source
relations and 20,548 referenced origins. PARTIAL remains explicit.

The final producer adjustment preserves SORT/MERGE qualification with missing
control. Final frontend revalidation proves product equivalence before reusing
any downstream execution. Logs, binaries, source hashes and validation summaries
are retained under the local incomplete-file-effects-20260927 E2E report.
Repository FAST completion and remote CI are recorded in the review PR.

The FILE family has 113/115 tests passing; two old SP-version assertions fail as
on the untouched baseline. Unrelated full-qualification failures, W2 ACCEPT and
copy-cycle limits remain; this follow-up does not fix them. Previous lower
semantic/capacity gates are reused because lower production is unchanged.
