# Estado — WORK-LOWER-001

## Onde estamos

Autorização explícita de 2026-09-06: `multi-checkpoint`, CP0..CP5 sequenciais, certificação/checks remotos antes de avançar, review humano final. `current_checkpoint: CP2`, implementação/falsificações concluídas, regressão/certificação em curso. CP3..CP5 não iniciados.

Branch `feat/first-entry-goback-slice`, [PR #2 Draft/open](https://github.com/Gustavo2358/cobol-lower/pull/2), base main `14aaafc5051eb287af6a8f126e8eaff78e30e365`. Working tree limpa na entrada do CP; alterações atuais pertencem a CP2.

## Último recovery plenamente certificado

CP1: `2e4c55afdaee3e31210f0e295a8e95b2d516ca00`, [certificação](../../../quality/WORK-LOWER-001/CP1.json), [recibo](../../../quality/WORK-LOWER-001/CP1-remote.json). Check checkpoint/github-actions/push success no SHA exato, run 34063640930/check 101568507628, consulta 2026-09-06T22:20:26.194146+00:00; dentro dos 1200 segundos do primeiro push.

CP0: `1cc40112a7f57adb4a71861db8a101e6bec242dc`, [evidência](../../../quality/WORK-LOWER-001/CP0.json)/[recibo](../../../quality/WORK-LOWER-001/CP0-remote.json). Inicial `98dbb60f204f42191fa1ee6f1a5b7caca89355ed` preservado; remediação CI no próprio CP0, sem rewrite.

## Verde cumulativo conhecido

CP1: docs/architecture/semantic/git/fast exit 0; 211 assertions (20 AIR + 191 decoder), 61 testes harness; 20 testes focais SP na captura offline, golden 2663 bytes intacto. Cinco falsificações focais e sete de docs/architecture restauradas com segundo GREEN. Coerção scalar→String encontrada/corrigida por contracaso. Self-review, sem revisor independente.

## CP2 e próximo passo não iniciado

Porta interna AdmitInput/EntryGobackAdmission e driver FileAdmission implementados; ambos caminhos usam a mesma validação. ADMITTED é somente admissão, sem Publication. 439 assertions cumulativas (238 core + 201 adapters), 61 testes harness; cinco mutações focais e sete desafios docs/architecture restaurados com segundo GREEN. [FREEZE](../../../quality/WORK-LOWER-001/CP2-contract.md), [evidência](../../../quality/WORK-LOWER-001/CP2.json), [self-review](../../../quality/WORK-LOWER-001/CP2-review.md).

Findings resolvidos: fixture IF precisava dos inventários THEN/ELSE conforme writer; negativo original preservado. Gap adicional de inventário indevidamente admitido foi bloqueado após contracaso. Uma execução durante wiring test-jar foi setup inválido, descartada como prova e reexecutada após estabilização. Golden/contratos/oracles não alterados.

Próximo passo: certificar candidato, commit/push e check remoto obrigatório no SHA exato. CP2 ainda não foi publicado; limite remoto será 1200 segundos desde primeiro push. CP3 (tradução) não iniciado. Full/performance são CP4/CP5, não executados neste CP.

Source lock, upstreams, AIR/modelo compartilhado e capabilities fora deste work item preservados. Sem merge/auto-merge. Nenhum backlog subsequente iniciado.
