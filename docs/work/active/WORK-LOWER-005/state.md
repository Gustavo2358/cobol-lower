# WORK-LOWER-005 — Estado

CP0 único single-checkpoint, autorizado em 2026-09-08 pela tarefa de origem
01a07ed2-f918-7cc3-8e22-b202ce75d9f4. Checkout direto, base main limpa/fetch/pull
ff-only 1a456e7673df0e3357f157e0249d74aef12bdf63. PR5 merged com reviews=[]
reconciliados, sem aprovação humana inventada; evidências anteriores intocadas.

Implementação pronta na branch perf/compact-local-ids: local-xxh3-128-v1,
hash4j0.30.0 XXH3-128 seed0, 32 hex high64/low64, campos framed com buffer256,
registro exato por publicação sem cópias expandidas. PublicationId inclui a versão
local: fixture a5fce8cae9328bc4007fc589e1989e37. AIR15720→13827 bytes; bijeção20 IDs,
163 ocorrências Java; todos os fatos não-ID iguais. SourceKeys e limites SP preservados.

RED/GREEN, sete falsificações/restaurações/segundo GREEN e full exit0: 139 testes
harness, 27 challenges históricos, 202819 assertions semânticas, 147 assertions de
saída + oracle bijetivo, 95 performance. Duas CLIs exit0/bytes iguais. Provas focais:
100000 identidades distintas, texto1000000 UTF16 (4000169 bytes alimentados), porta
com unit key/filename100000 e maior handle canônico. Nenhum programa de100k instruções,
benchmark de ganho heap/CPU ou E2E cross-repo alegado. SourceKeys continuam integrais.

Self-review em [review](../../../quality/WORK-LOWER-005/CP0-review.md), FREEZE v2/
certificação pre_commit em [CP0](../../../quality/WORK-LOWER-005/CP0.json), detalhes de
[escala](../../../quality/WORK-LOWER-005/scale-and-size.json). Finding de teste de handle
inválido resolvido/documentado sem alterar a porta. Nenhum finding bloqueante restante.

Fase ready_for_review local; primeiro commit certificado/PR serão publicados.
Certificação local não é CI nem aprovação humana. Resolver commit final pelo trailer
Checkpoint-Evidence, observar checkpoint no HEAD exato e registrar recibo no PR/handoff,
sem commit recursivo para autoinscrever SHA. Entrega permite E2E na tarefa de origem.
Manter branch/checkout limpos após commit; revisão humana e merge permanecem pendentes.
Não iniciar próximo CP, merge/auto-merge ou E2E cross-repo nesta tarefa.
