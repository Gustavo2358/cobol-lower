# WORK-LOWER-002 — Estado

## Onde estamos

CP0, single-checkpoint, fase ready_for_review. Autorização: pedido explícito do usuário em 2026-09-07,
somente proveniência/pinning para 2A/2B, commit/push/PR próprios e parada para review humano, sem merge.
Baseline main limpa e atualizada: `e2488a362478057de7d59cdf9ae2b38b1f4040d3`; branch `chore/pin-air-java-1a`.

## Verde conhecido

[Review e evidência](../../../quality/WORK-LOWER-002/CP0-review.md): pin/paths/URLs e norma preservados;
bootstrap, full (semantic 779, performance 95, 126 testes do harness, 18 challenges), scope e diff revistos.
[Certificado pre_commit](../../../quality/WORK-LOWER-002/CP0.json); o commit correspondente é resolvido pelo trailer Checkpoint-Evidence.
O manifest do pacote original foi verificado em sua revisão histórica e preservado.

## Restante

[PR #3](https://github.com/Gustavo2358/cobol-lower/pull/3) aberto sem auto-merge.
Primeiro commit certificado/publicado: `e106a7f98905e2572d3d9a258cfe23b623fd961f`.
Número real do PR vinculado ao certificado no mesmo CP0; full novamente verde.
Publicar o delta certificado e confirmar CI do head final. O recibo remoto fica no PR/handoff,
sem presumir PASS remoto neste certificado local. Depois, apenas review humano; 2A/2B não iniciados.

## Descobertas que afetam o plano

Model/validator agora em air-model; codec compartilhado em air-json. O novo item expôs autorização
herdada por uma fixture de histórico: correção limitada às cópias sintéticas, mantendo o oracle e os checkers.
Nenhum POM, Java, teste de produto, contrato normativo ou upstream alterado.
