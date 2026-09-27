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

## Final gate and pin

Lower FAST PASS, including 2481 core checks, admission/codec/integration and architecture.
Final producer pin: `f8170f513eef29adfff5a94f418e1c6481ee2365`. The gate used `503e11f6b33daa504e6338cf84a11984acab82cf`; the only intervening producer
change is the validation report. Every locked producer blob has the same SHA-256.
Final pin/docs checks PASS; no semantic test evidence was invalidated. The rebuilt
production classes equal the immutable lower runtime used in all 73 real runs.
