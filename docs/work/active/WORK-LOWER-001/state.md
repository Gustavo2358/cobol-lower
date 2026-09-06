# Estado — WORK-LOWER-001

Autorização explícita do usuário em 2026-09-06: multi-checkpoint CP0..CP5 sequenciais mediante certificação e checks remotos; review humano final. Current checkpoint CP4, implementação/challenges e primeiro full concluídos; regressão/review/certificação em curso. CP5 não iniciado.

Branch `feat/first-entry-goback-slice`, [PR #2 Draft/open](https://github.com/Gustavo2358/cobol-lower/pull/2), base main `14aaafc5051eb287af6a8f126e8eaff78e30e365`. Worktree limpa ao recuperar CP3; alterações atuais pertencem a CP4.

## Recovery plenamente certificado

CP3 `d3867118e51a0654dcd995ae04e26ed68d124e99`: [certificado](../../../quality/WORK-LOWER-001/CP3.json), [recibo](../../../quality/WORK-LOWER-001/CP3-remote.json). Certify/verify-commit/push exit 0. Check checkpoint/github-actions/push completed success no SHA exato, run34066270348/check101575458574, consulta 2026-09-06T23:15:46.521797+00:00. Primeiro push23:13:18Z/deadline23:33:18Z, dentro de1200s. Pendências anteriores23:13:38Z/23:14:44Z não foram tratadas como PASS. 741 assertions/61 testes harness; cinco falsificações focais e sete desafios restaurados, segundo GREEN, docs/architecture/semantic/git/fast exit0. Último recovery plenamente certificado.

CP2 `c5546c9f4a318f31bcfb3c79a91a4b7d7d71149f`: [certificado](../../../quality/WORK-LOWER-001/CP2.json), [recibo remoto](../../../quality/WORK-LOWER-001/CP2-remote.json). Check checkpoint/github-actions/push success no SHA exato, run 34064883612/check 101571789601; primeiro success observado 2026-09-06T22:46:04.528032+00:00, reconfirmado 22:52:51.215370+00:00, dentro de 1200 segundos desde push 22:44:10Z. 439 assertions (238 core + 201 adapters), 61 testes harness; docs/architecture/semantic/git/fast exit 0, cinco mutações focais e sete desafios restaurados com segundo GREEN.

CP1 `2e4c55afdaee3e31210f0e295a8e95b2d516ca00`: [certificado](../../../quality/WORK-LOWER-001/CP1.json)/[recibo](../../../quality/WORK-LOWER-001/CP1-remote.json), 211 assertions/61 testes harness; golden real 2663 bytes intacto.

CP0 `1cc40112a7f57adb4a71861db8a101e6bec242dc`: [certificado](../../../quality/WORK-LOWER-001/CP0.json)/[recibo](../../../quality/WORK-LOWER-001/CP0-remote.json). Commit inicial `98dbb60f204f42191fa1ee6f1a5b7caca89355ed` preservado; remediação de CI no mesmo CP0, sem rewrite.

## Próximo passo

CP3 produz Publication AIR2 Entry/Sequence/Return após a mesma admissão, com identidade canônica limitada, correlações e provenance, coverage parcial/gap alternativo e relatório integral do AirValidator. 741 assertions cumulativas/61 testes harness; cinco mutações focais e sete desafios docs/architecture restaurados com segundo GREEN. [FREEZE](../../../quality/WORK-LOWER-001/CP3-contract.md), [evidência](../../../quality/WORK-LOWER-001/CP3.json), [self-review](../../../quality/WORK-LOWER-001/CP3-review.md). Sem ajuste de esperado para acomodar implementação.

CP4: driver arquivo→LowerInput, teste vertical independente/lifetime/determinismo, métricas N/2N e executores performance/full implementados e executados. 779 assertions semânticas, 95 de escala adicionais (874 no performance); suíte harness com 73 testes após finding final. Seis falsificações focais/custo/gates e sete desafios docs/architecture restaurados com segundo GREEN. [FREEZE](../../../quality/WORK-LOWER-001/CP4-contract.md), [evidência](../../../quality/WORK-LOWER-001/CP4.json), [self-review](../../../quality/WORK-LOWER-001/CP4-review.md).

Finding corrigido após primeiro full: modo Git publicado precisava conferir certificate_errors diretamente; RED reproduzido, correção e mutação reversa com restauração/segundo GREEN. Regredir full antes de certificar/publicar. Primeiro run isolado test_full falhou por setup/import, não contou como falsificação; RED real veio depois da correção do setup.

Próximo passo: regressão full/focais, certificação, commit/push e check remoto do SHA exato. CP4 ainda não publicado; prazo será1200s desde primeiro push. CP5 não iniciado. Limitações: sem SLA de throughput, contadores não instrumentam internals Jackson/JDK; árvore JSON é materializada sob limite de bytes. Limites CP3/shape/provenance/checker permanecem.

Source lock/upstreams/must_not_change preservados. Somente self-review até aqui. Sem merge/auto-merge ou trabalho fora de WORK-LOWER-001.
