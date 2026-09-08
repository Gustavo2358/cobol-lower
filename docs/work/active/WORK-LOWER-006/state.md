# WORK-LOWER-006 — estado

Modo single-checkpoint, mesmo 4C/CP0, branch feat/lower-scalar-move e
[PR7](https://github.com/Gustavo2358/cobol-lower/pull/7). Review de
24742514410d938063149a4f466e81a29801adac autorizou somente B1/B2 e backlog D1/D2.
B1: coerência literal SP1.2 na boundary antes de Materialize. B2: tetos medidos
32MiB/depth64/1500000nodes/250000visits. D1 BACKLOG-LOWER-017 e D2
BACKLOG-LOWER-018 planned/NOT STARTED, sem work item nem implementação.

Full local PASS em m2 inicialmente sem artefatos lower: 203103 assertions
semânticas, 108 adicionais de performance, 141 testes do harness, 9 novos +17
escalares +27 históricos desafios, restore exato e segundo GREEN. CP3 e fixture4C
byte-idênticos. Probes reais400/10k atravessam SP/admission; 10k para no AirJson
16MiB atomicamente. Sem claim de readiness E2E para grandes programas.

CI34265242563 em2282308 falhou antes dos novos desafios por core/test-jar
não instalados; falha reproduzida. Executor agora prepara a cópia isolada atual,
sem alterar oracles ou produção; full nesse ambiente passou. Self-review PASS;
certificação e verify-commit antes de novo push. Novo head resolve pelo trailer
CP0, CI exact-head será recebido no PR após publicação. Review humano pendente,
sem merge/auto-merge. Nenhum PASS remoto é alegado para o run anterior falhado.

Base original main f9e74ec3404efe830992d9535becca847ace80e8; base cumulativa da
remediação2474251; parent de recuperação2282308. Pins 4A/4B/AIR inalterados.
Bytes rastreados dos siblings preservados; HEAD analysis-cfg avançou externamente
para merge PR11 com árvore idêntica. Nenhum sibling modificado por esta tarefa.
4D/4E/CFG/dataflow/Possible Values/CALL não executados aqui.

[Handoff](../../../quality/WORK-LOWER-006/handoff.md) e
[certificado](../../../quality/WORK-LOWER-006/CP0.json). Logs anteriores preservados.
