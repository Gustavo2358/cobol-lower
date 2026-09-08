# CP0 — implementação única do 4C

1. RECOVER/FREEZE baseline limpa, merges/pins, fixture real e oracles independentes.
2. RED: fixture 1.2.0 não suportada pela baseline; wire/core/admissão/handlers/indexes.
3. GREEN: model/codec/CLI, CP3 exato, negativos, metamorﬁsmo e escala estrutural.
4. FALSIFY: 16 mutações compiláveis, restauração exata e segundo GREEN.
5. REGRESS/REVIEW/CERTIFY: seis gates first-slice, diff integral; commit/push/PR,
CI checkpoint no head exato, limite de espera 3600s. Sem 4D/4E nem merge.

## Retomada B1/B2

RECOVER confirma head2474251 limpo/PR7 aberto; sem nova branch/work. Medir corpus
upstream em /tmp; congelar testes; RED→B1/B2→GREEN; 9 novos desafios, full com
probes anteriores, self-review/certificação, commit/push no mesmo PR e CI exato.
D1/D2 somente backlog. Certificado anterior preservado em reviewed-CP0.json e Git;
novo FREEZE autorizado cobre oracles corrigidos, sem enfraquecer fixtures legítimas.

CI execution budget only: the complete local B1/B2 full run took 11m35s before
remote bootstrap. Raise the bounded workflow timeout from 15 to 30 minutes to
accommodate all required challenges and bootstrap; no gate skipped or weakened.
The full run precedes this timeout-only adjustment; recheck workflow policy,
docs and Git afterward, and rerun full via CI on the exact published SHA.

CI run34265242563 reached the new production challenges after the other gates
and scalar challenges passed, then failed during baseline dependency resolution:
adapter-only Maven invocation assumed core/test-jar installed in the local m2.
Reproduced with a fresh temporary m2 excluding all cobol-lower artifacts. Fix
only challenge preparation: install current isolated core plus test-jar with
its reactor before focal adapter invocations. No oracle/product changes. Rerun
full using that fresh build root, preserve failed CI log, recertify and normal push.
