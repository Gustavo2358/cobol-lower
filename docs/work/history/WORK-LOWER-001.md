# WORK-LOWER-001 — Primeiro slice Entry/GOBACK

## Human review remediation

Após o head CP5 `5a00a152d3c05793dad825c16226867c493ee31b`, review humano solicitou correções de receipts, FREEZE, lifecycle/CI e estado obsoleto de AGENTS. [R1](../../quality/WORK-LOWER-001/R1-state.md) registra autorização restrita, novos contracasos e revalidação no mesmo PR2, sem recriar active, CP6 ou ampliar `minimal-entry-goback@1`. Certificados e narrativa CP0..CP5 abaixo são preservados como evidência histórica, com os defeitos do harness explicitados pelo delta posterior. [CP5 remoto](../../quality/WORK-LOWER-001/CP5-remote.json) foi reconfirmado antes da remediação.

## Fechamento original em CP5

Estado local de fechamento: `completed`; review técnico self-review, review humano final pendente; merge_status `open_not_merged`. Fechamento preparado em CP5 conforme autorização explícita do usuário em2026-09-06, multi-checkpoint CP0..CP5. Não inferir certificação/CI de CP5 apenas deste estado: [evidência](../../quality/WORK-LOWER-001/CP5.json) e [estado](../../quality/WORK-LOWER-001/CP5-state.md) são factuais; head/recibo final no PR.

Branch `feat/first-entry-goback-slice`, [PR2](https://github.com/Gustavo2358/cobol-lower/pull/2), base main `14aaafc5051eb287af6a8f126e8eaff78e30e365`. Uma branch/PR, sem rewrite/merge/auto-merge. [Snapshot de autorização/scopes](../../quality/WORK-LOWER-001/CP5-manifest.yaml) é o manifesto congelado antes do fechamento, não novo item ativo. Spec/plan/eval anteriores permanecem reproduzíveis no Git pelas referências do FREEZE; o pacote ativo foi removido, sem copiar tasklist inteira para conhecimento canônico.

| CP | Commit plenamente certificado antes de CP5 | Evidência |
| --- | --- | --- |
| CP0 | 1cc40112a7f57adb4a71861db8a101e6bec242dc | [certificado](../../quality/WORK-LOWER-001/CP0.json), [remoto](../../quality/WORK-LOWER-001/CP0-remote.json) |
| CP1 | 2e4c55afdaee3e31210f0e295a8e95b2d516ca00 | [certificado](../../quality/WORK-LOWER-001/CP1.json), [remoto](../../quality/WORK-LOWER-001/CP1-remote.json) |
| CP2 | c5546c9f4a318f31bcfb3c79a91a4b7d7d71149f | [certificado](../../quality/WORK-LOWER-001/CP2.json), [remoto](../../quality/WORK-LOWER-001/CP2-remote.json) |
| CP3 | d3867118e51a0654dcd995ae04e26ed68d124e99 | [certificado](../../quality/WORK-LOWER-001/CP3.json), [remoto](../../quality/WORK-LOWER-001/CP3-remote.json) |
| CP4 | a487c52faded88d773c22333481f3a54a3327d47 | [certificado](../../quality/WORK-LOWER-001/CP4.json), [remoto](../../quality/WORK-LOWER-001/CP4-remote.json) |
| CP5 | SHA próprio no trailer/PR após publicação; sem autorreferência recursiva | [FREEZE](../../quality/WORK-LOWER-001/CP5-contract.md), [certificado](../../quality/WORK-LOWER-001/CP5.json) |

CP0 teve commit inicial98dbb60f204f42191fa1ee6f1a5b7caca89355ed com falha de contexto no workflow; remediação certificada no mesmo CP, sem rewrite. CP0 estabeleceu gates, checker de certificado, oracle AIR manual e contracasos antes do avanço. CP1 capturou SP real; CP2 provou admissão única; CP3 traduziu Entry/start/Return; CP4 provou vertical/custo/full; CP5 fecha lifecycle e revisa claims. Logs e mutações restauradas estão nos certificados individuais.

## Conhecimento promovido

Regra/limite: [primeiro slice](../../domain/first-slice-entry-goback.md) e [matriz](../../domain/capability-matrix.md). API/uso: [README](../../../README.md). Identidade/custo/provenance: [política canônica](../../domain/identity-and-provenance.md). Resultados: [contrato local](../../domain/validation-and-results.md). Evals/invariantes registram testes e escopo de enforcement, sem promover futuros codecs/CFG. Oracles/testes executáveis e todos os FREEZEs/certificados preservados.

Limites: shape mínimo; outras famílias sem validação semântica integral; inventário alternativo parcial; precisão CONTROL local não eleva outras dimensões; spans/include sites incompletos explicitados; IDs longos limitados; JSON em árvore limitada por bytes; contadores sem SLA; AirValidator não é certificação semântica integral. Sem writer AIR, CFG ou alterações upstream.

Próximo passo não iniciado: review humano do PR completo. Nenhum backlog dependente autorizado por este fechamento.
