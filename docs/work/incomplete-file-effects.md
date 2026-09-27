# Incomplete native file effects

Status: DONE upon merge of [PR #38](https://github.com/Gustavo2358/cobol-lower/pull/38); required technical gates passed.
Scope: F04/F07 follow-up to the storage fixes.

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

Producer pin: `7775c0407f6e5b60d73517b687f9768c878e59eb`; SP 2.48 unchanged.
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

The producer FILE family now passes 115/115, without failures, errors or skips.
Its two old version expectations now require SP 2.47 for nominalValues and SP
2.48 for SECTION regions. All semantic assertions execute; producer FAST passes
589 tests. The three historical PERFORM inventory oracles, the DLI oracle and
one skipped discovery test remain pending. The full suite was not rerun and is
not claimed green. W2 ACCEPT and copy-cycle also remain outside this delivery.
Previous lower semantic/capacity gates are reused because lower production is
unchanged in this follow-up.


## Merge pin and closeout

Frontend PR #63 merged at `7775c0407f6e5b60d73517b687f9768c878e59eb`. The source lock records that immutable commit,
its tree and all 96 selected file hashes. Production, build inputs and source
fixtures equal `4802305d17e7b1684d1f749618aa76fe8e091e0f`; the producer test/version
and documentation corrections do not invalidate the integrated corpus evidence.
Frozen test manifests retain the producer that actually generated their bytes.
The user authorized merge of lower PR #38 after the producer. This final change
updates only the pin and documentation; lower production and test inputs remain
identical to `dbf590f83203b87ce4b1c0e89af01bad8a25c9fc`.
