# WORK-LOWER-006 — estado

Modo single-checkpoint, mesmo Checkpoint 4C/CP0, branch feat/lower-scalar-move e
[PR7](https://github.com/Gustavo2358/cobol-lower/pull/7). Review humano de
24742514410d938063149a4f466e81a29801adac autorizou somente B1/B2 e backlog D1/D2.
B1: coerência literal SP1.2 verificada na boundary antes de Materialize.
B2: tetos de produção 32MiB/depth64/1500000nodes/250000visits medidos em SP real.
D1 BACKLOG-LOWER-017 e D2 BACKLOG-LOWER-018 planned/NOT STARTED, sem work item.

Full local PASS: 203103 assertions semânticas, 108 adicionais de performance,
141 testes do harness; 9 novos + 17 escalares + 27 históricos desafios,
restore exato e segundo GREEN. CP3 e fixture4C byte-idênticos. Probes reais
400/10k passam os limites SP/admission; 10k para no AirJson16MiB, atomicamente.
Self-review e certificação PASS. Commit identificado pelo trailer CP0; CI exact-head será
observado e recebido no PR após publicação. Review humano pendente; sem merge
ou auto-merge. Timeout de CI ajustado para 30min após full local de 11m35s,
sem reduzir gates. Não é claim de readiness E2E para programas grandes.

Base main original f9e74ec3404efe830992d9535becca847ace80e8. Base da remediação
24742514410d938063149a4f466e81a29801adac, árvore inicial limpa. Pins 4A/4B/AIR
normativa inalterados; todos os bytes rastreados nos siblings preservados.
HEAD de analysis-cfg avançou externamente para merge PR11 com árvore idêntica.
Nenhum sibling foi modificado por esta tarefa; 4D/4E não executados aqui.

[Handoff](../../../quality/WORK-LOWER-006/handoff.md),
[certificado](../../../quality/WORK-LOWER-006/CP0.json) e
[certificado anterior preservado](../../../quality/WORK-LOWER-006/reviewed-CP0.json).
Somente AIR JSON; CFG/dataflow/Possible Values/CALL permanecem fora do escopo.
