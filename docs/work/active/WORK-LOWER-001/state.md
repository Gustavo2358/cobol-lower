# Estado — WORK-LOWER-001

## Onde estamos

Autorização explícita do usuário em 2026-09-06: `multi-checkpoint`, CP0..CP5, sequencialmente mediante certificação e checks remotos; review humano final. `current_checkpoint: CP1`, implementação e falsificação concluídas, regressão/certificação em curso. CP2..CP5 não iniciados.

Branch `feat/first-entry-goback-slice`, [PR #2 Draft/open](https://github.com/Gustavo2358/cobol-lower/pull/2), base main `14aaafc5051eb287af6a8f126e8eaff78e30e365`. Working tree limpa no início de CP1; alterações atuais pertencem a este checkpoint.

## Último recovery plenamente certificado

CP0: `1cc40112a7f57adb4a71861db8a101e6bec242dc`; [certificação](../../../quality/WORK-LOWER-001/CP0.json), [recibo remoto](../../../quality/WORK-LOWER-001/CP0-remote.json). Check `checkpoint`/github-actions/push concluído success no SHA exato, run 34062323376, check 101564913729, consulta 2026-09-06T21:55:31.726660+00:00. Dentro do prazo original até 22:05:08 UTC. CP0 estabeleceu a trusted execution boundary.

Commit inicial CP0 `98dbb60f204f42191fa1ee6f1a5b7caca89355ed` permanece preservado; workflow inválido foi corrigido e recertificado no mesmo checkpoint, sem rewrite.

## Verde conhecido

CP0: docs/architecture/semantic/git/fast exit 0; 58 testes do harness e 20 assertions AIR/boundary. Upstream air-java fixado: 172 checks. Oito falsificações com RED esperado, restauração e segundo GREEN. Review: self-review, sem revisor independente.

## CP1

Golden real capturado: 2663 bytes, sha256 7ebce874bb98262598b908b176290368f738a21561c35a6ac342fc72e66d04ed, 20 testes focais upstream PASS; [aquisição](../../../evals/fixture-intake.json). Snapshot imutável e DTO/decoder estrito implementados; 211 assertions cumulativas (20 AIR + 191 decoder), 61 testes harness. Fonte/provenance é dado, nunca arquivo a abrir pelo core.

[FREEZE e oracles](../../../quality/WORK-LOWER-001/CP1-contract.md) anteriores ao código; [evidência CP1](../../../quality/WORK-LOWER-001/CP1.json); [self-review](../../../quality/WORK-LOWER-001/CP1-review.md). Cinco falsificações focais mais sete documentais/arquiteturais, todas restauradas com segundo GREEN. Finding textual coercion corrigido após RED próprio; golden/contratos/oracles preservados. Nenhuma regra de lowering em CP1.

## Limitações e próximo passo não iniciado

CP1 ainda depende de certificação mecânica, commit/push e check remoto obrigatório do novo SHA. Limite remoto CP1: 1200 segundos cumulativos desde primeiro push; nenhum push CP1 ainda. CP2 (validação/admissão) não iniciado. Payloads de famílias não suportadas são verificados fisicamente, com ocorrência/header/variante preservados para rejeição; nenhuma claim de validação semântica integral. Source lock preservado, frontend/AIR/air-java/CFG sem alterações. Não houve merge/auto-merge nem trabalho fora de WORK-LOWER-001.
