# WORK-LOWER-004 — Estado

CP0 único, single-checkpoint autorizado nesta sessão, atualizado explicitamente para XXH3-128.
Branch perf/compact-publication-id, checkout direto E2E cobol-lower. Main limpa/fetch/pull
ff-only em a12907d61dbad619bdda48dee16f8ae6a63460db, sem avanço. PR4/PR2 merged com
reviews=[] reconciliados; certificados históricos intocados, sem autorização herdada.

Implementação pronta: hash4j0.30.0, XXH3-128 seed0, 32 hex high64/low64, buffer256, limites31/32.
Fixture PublicationId 1718623a5d9fc4b5db0124dc15913f58. AIR398532→15720 bytes, somente
namespace alterado em 73 ocorrências. Token/IDs locais/SourceKeys/semântica/proveniência
preservados. Pins anteriores intactos; source lock apenas acrescenta hash4j.

RED, GREEN, cinco falsificações restauradas e segundo GREEN; full exit0 com 139 testes
do harness e 27 challenges anteriores. 2457 assertions semânticas (incluindo blocos),
148 AIR output e 95 performance. Build clean install e CLIs Java/Maven exit0; bytes iguais.
Self-review integral em [review](../../../quality/WORK-LOWER-004/CP0-review.md),
FREEZE v2/certificação pre_commit em [CP0](../../../quality/WORK-LOWER-004/CP0.json).
Limitações: hash não criptográfico, sem prova de unicidade matemática/CPU/heap ou grande escala;
nenhum CFG ou E2E cross-repo executado. Produção de adapters/CLI/binding/AIR/SP intocada.

Fase ready_for_review local. Primeiro commit certificado pre_commit/publicado:
`e0568298e94aa02efcd27731e70ffd16d92c4efc`.
[PR #5](https://github.com/Gustavo2358/cobol-lower/pull/5) aberto, autoMergeRequest null.
Número real agora vinculado ao manifesto/registry/evidência no mesmo CP0. Código/testes
inalterados neste ajuste documental; docs/git e certificação repetidos. O commit final
se resolve pelo trailer da evidência, sem autoinscrição recursiva.
Certificação técnica local não é aprovação humana ou CI remoto. Confirmar check checkpoint
no HEAD publicado e registrar recibo no PR/handoff, sem autoinscrição do próprio SHA.
Próximo passo externo: revisão humana solicitada do PR completo. Sem merge/auto-merge;
manter esta branch e working tree limpa para a sessão E2E. Nenhum próximo CP autorizado.
