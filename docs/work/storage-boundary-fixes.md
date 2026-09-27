# Storage boundary fixes

- id: STORAGE-BOUNDARY-FIXES
- status: IN_PROGRESS
- scope: Nominal GLOBAL captures and admission of anonymous storage nodes / nonallocating RENAMES views.

## Rule and algorithm

Materialize required declaration identities even when storage proofs are unavailable, using UnknownBinding with bounded uncertainty. Capture them by the published source identity. Do not create a Cell without proof. FILLER retains physical node identity without a nominal data symbol. Count allocating nodes, excluding explicitly published RENAMES owners, when validating a complete physical chain. An executable I/O effect needs a location bound in addition to nominal identity. If no materialized bound exists, emit a visible-memory MAY effect; do not substitute an empty effect or a must kill.

Authority: existing storage/compilation contracts and AIR bindings. IBM Enterprise COBOL 6.4 [scope of names](https://www.ibm.com/docs/en/cobol-zos/6.4?topic=programs-scope-names) preserves identity across contained programs; [language reference](https://publibfp.dhe.ibm.com/epubs/pdf/igy6lr40.pdf) describes FILLER and nonallocating level-66 renaming. Negative input is retained with uncertainty, not promoted to valid COBOL.

Algorithms traverse finite published node/component sets and indexed parent relations. Capture materialization and admission use indexed linear scans; I/O fallback inspects the selected declaration binding, bounded by the existing declaration inventory. There is no source-text search or object-pair analysis. Existing cycle/identity validation remains authoritative.

## Oracle and validation

Before implementation, reproduce the failures. Assert that invalid relations retain unknown ranges and diagnostics; unaffected roots keep independent proofs; required captures alias their declared owner without invented storage; anonymous FILLER has no data object; RENAMES never enlarges physical allocation. Reject forged or incomplete exact-view chains. Check enumeration invariance, source provenance and AIR validation.

Run repository FAST and local qualification, the complete frontend fixture corpus, PERFORM, Chaos and CardDemo. Compare dependency relations and supports with the frozen baseline; keep existing PARTIAL and unrelated input limitations explicit. ALTER and incomplete file-effect findings F04/F07 are outside scope.

## Validation and pins (2026-09-27)

Producer pin: `4da7d8b03941ee78acba0d001a7e6391c1f7d371` (SP 2.48.0 unchanged). AIR Java remains
`d760b07b0fac42a106d09342ee9d5b8445ccf630`; AIR specification remains
`2c7f31f19efbe3211a2aea5bbda90173a9666fe2`. No AIR/CFG production change.

- Lower FAST, architecture, semantic and performance gates: PASS. Semantic gate:
  205,184 checks; performance gate: 39,207 additional capacity checks.
- `StorageBoundarySuite`: nine frozen real products, qualified captures and
  homonyms, provenance, permutations, FILLER, extra physical child rejection,
  and visible MAY writes for unproved GLOBAL records.
- Six new four-stage dependency oracles: PASS. Uncertain READ preserves the prior
  candidate and unknown remainder; proved MOVE kills it. Complete RENAMES aliases
  retain values written through the owning record. No expected target removed.
- All 11 discovery failures fixed; 307/310 complete, preserving 296 prior
  successes. Three previous incomplete file-effect inputs still reject:
  file-namespace-shadowing, nested-global-through-local-file, statements.
- PERFORM 39/39; Chaos 48/48; PERFORM adversaries 25/25; logical aliases 14/14.
- CardDemo 73/73 pipelines complete with existing PARTIAL status. Relations remain
  121 program / 271 file / 523 source; no semantic/support differences, and all
  20,548 referenced origins are unchanged. The final producer republishes all
  310 fixture and 73 CardDemo SPs byte-identically to the integrated execution.
  Downstream reuse requires exact input bytes and unchanged consumer hashes.
- Historical W2 product oracles remain 23/24 (known-open ACCEPT unchanged). The
  historical all-Cell runner remains 5/24; failed evidence is retained.

Frontend FAST passes 582 tests. Its full qualification retains six failures and
one skipped test; the same six failures reproduce on unchanged main. Stale
version/PERFORM/DLI oracles were not weakened. The historical copy-cycle source
oracle is unchanged (54/55 source oracles pass).

RED/GREEN logs, runtime hashes and corpus comparisons are retained in the local
E2E campaign `frontend-fixture-fixes-20260927`. Work remains IN_PROGRESS pending
human review and merge. No ALTER implementation.
