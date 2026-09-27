# CardDemo IBM catalogue admission

- id: CARDDEMO-IBM-COPYBOOKS-LOWER
- status: IN_PROGRESS
- scope: Pin the complete CardDemo IBM producer; retain the existing SP 2.49 model-assumption transport.

Frontend `503e11f6b33daa504e6338cf84a11984acab82cf` adds CMQGMOV, CMQMDV, CMQODV, CMQPMOV, CMQTML, CMQV and SQLCA
to DFHAID/DFHBMSCA. The wire contract and lower production code are unchanged.
The lock records the exact producer tree and selected blob hashes.

New production-CLI validation: all 73 CardDemo variants ran all four stages;
73/73 preserved program/file candidate sets, executable supports, conditional
evidence/assumptions and original operand provenance. Seven SPs changed; 24
missing includes now use structural models with explicit uncertainty.
21/21 structural adversaries passed, including seven MQ/SQLCA additions.
Frontend FAST passed 602 tests. PERFORM 39, Chaos 48, aliases 14 and PERFORM
adversaries 25 reran frontend with byte-identical SP and compilation products;
their downstream evidence is explicitly reused with identical binaries/inputs.

COTRTLIC/COTRTUPC conditional supports retain candidate evidence and assumptions
while replacing opaque SQLCA input with unavailable model inputs and localizing
the adjacent DCLTRTYP opaque declaration. No unexplained regression remains.

Evidence: frontend `docs/work/carddemo-ibm-validation.json` and local raw
`.synthetic-dfh/carddemo-expansion/`. Synthetic input remains PARTIAL, without
physical/initial-value proof. No new AIR/CFG edges and no merge.

## Expansion gate and pin

Lower FAST PASS, including 2481 core checks, admission/codec/integration and architecture.
Final producer pin: `f8170f513eef29adfff5a94f418e1c6481ee2365`. The gate used `503e11f6b33daa504e6338cf84a11984acab82cf`; the only intervening producer
change is the validation report. Every locked producer blob has the same SHA-256.
Final pin/docs checks PASS; no semantic test evidence was invalidated. The rebuilt
production classes equal the immutable lower runtime used in all 73 real runs.

## Review follow-up — SQLCA scope and operational metadata

Current producer pin: `e2d825b1551dd7a730ae79c4a1b7141586c23fc9`.
Synthetic SQLCA is now admitted only by EXEC SQL INCLUDE. Real COPY SQLCA keeps
normal library precedence; a missing COPY remains unresolved. The current lock
states SP 2.49 / NOMINAL_TEXT_SOURCE_V2 and Drafts #64/#39, replacing stale
SP 2.47/2.48 and merged-PR descriptions. Historical reference pins and wave
checkpoints are retained and labeled; upstream-state starts with the current
integration context.

Validation newly executed for the review: frontend focal 28/28, FAST 603/603,
package/docs; lower FAST PASS (core, contracts, integration, architecture).
The frontend reran all 73 CardDemo, 21 structural adversaries, 39 PERFORM,
48 Chaos, 14 aliases and 25 PERFORM adversaries. All 220 SP/compilation pairs
are byte-identical. Downstream corpus/suite evidence is reused with unchanged
inputs and immutable consumer binaries, including candidate supports/provenance.
The lower production code is unchanged; its rebuilt classes match that runtime.

The FAST used frontend implementation `3a4d9e9cbbce4d481eaf58db9e4ef8463e635914`.
The final producer adds only the follow-up validation documentation. All 109
locked frontend paths, tree, URLs and SHA-256 hashes were checked against the
final commit. Final docs/pin checks pass. Raw results, logs and compiled runtime
comparison: `.synthetic-dfh/carddemo-review-fixes/`; producer report:
`docs/work/carddemo-ibm-review-validation.json`. Full qualification and the
historical 310 matrix were not executed. Draft #39 remains open; no merge.

## Integration — merged producer pin

User authorized frontend #64 → lower #39 → CFG #49 integration.
Frontend #64 merged at `22d37233373b9db691ba170d898b7523bfea5746`. SRC-SP now pins this actual
merge commit on main; the entire tree and all 109 locked blobs equal the reviewed
producer `e2d825b1551dd7a730ae79c4a1b7141586c23fc9`. SP 2.49 / nominal V2 unchanged.
This commit changes consumer documentation/pins only; semantic/corpus evidence
remains valid. The final main gate and actual merge status are recorded by Git/CI
and the integration report. Earlier Draft/no-merge statements describe prior
qualification checkpoints; this section supersedes their integration state.
