# Estado — WORK-LOWER-001

Autorização explícita do usuário em 2026-09-06: multi-checkpoint CP0..CP5 sequenciais mediante certificação e checks remotos; review humano final. Current checkpoint CP3, implementação/falsificação concluídas; regressão e certificação em curso. CP4/CP5 não iniciados.

Branch `feat/first-entry-goback-slice`, [PR #2 Draft/open](https://github.com/Gustavo2358/cobol-lower/pull/2), base main `14aaafc5051eb287af6a8f126e8eaff78e30e365`. Worktree limpa ao recuperar CP2; alterações atuais pertencem a CP3.

## Recovery plenamente certificado

CP2 `c5546c9f4a318f31bcfb3c79a91a4b7d7d71149f`: [certificado](../../../quality/WORK-LOWER-001/CP2.json), [recibo remoto](../../../quality/WORK-LOWER-001/CP2-remote.json). Check checkpoint/github-actions/push success no SHA exato, run 34064883612/check 101571789601; primeiro success observado 2026-09-06T22:46:04.528032+00:00, reconfirmado 22:52:51.215370+00:00, dentro de 1200 segundos desde push 22:44:10Z. 439 assertions (238 core + 201 adapters), 61 testes harness; docs/architecture/semantic/git/fast exit 0, cinco mutações focais e sete desafios restaurados com segundo GREEN.

CP1 `2e4c55afdaee3e31210f0e295a8e95b2d516ca00`: [certificado](../../../quality/WORK-LOWER-001/CP1.json)/[recibo](../../../quality/WORK-LOWER-001/CP1-remote.json), 211 assertions/61 testes harness; golden real 2663 bytes intacto.

CP0 `1cc40112a7f57adb4a71861db8a101e6bec242dc`: [certificado](../../../quality/WORK-LOWER-001/CP0.json)/[recibo](../../../quality/WORK-LOWER-001/CP0-remote.json). Commit inicial `98dbb60f204f42191fa1ee6f1a5b7caca89355ed` preservado; remediação de CI no mesmo CP0, sem rewrite.

## Próximo passo

CP3 produz Publication AIR2 Entry/Sequence/Return após a mesma admissão, com identidade canônica limitada, correlações e provenance, coverage parcial/gap alternativo e relatório integral do AirValidator. 741 assertions cumulativas/61 testes harness; cinco mutações focais e sete desafios docs/architecture restaurados com segundo GREEN. [FREEZE](../../../quality/WORK-LOWER-001/CP3-contract.md), [evidência](../../../quality/WORK-LOWER-001/CP3.json), [self-review](../../../quality/WORK-LOWER-001/CP3-review.md). Sem ajuste de esperado para acomodar implementação.

Próximo passo: terminar regressão/certificação, commit/push e remoto obrigatório do SHA exato. CP3 ainda não publicado; prazo remoto será 1200s desde primeiro push. CP4 ainda não iniciado. Full/performance não executados/implementados; obrigatórios em CP4/CP5. Limitações: IDs canônicos longos e limitados, provenance sem span/site inventado, checker estrutural não prova semântica completa, perfil mínimo não suporta inventário maior.

Source lock/upstreams/must_not_change preservados. Somente self-review até aqui. Sem merge/auto-merge ou trabalho fora de WORK-LOWER-001.
