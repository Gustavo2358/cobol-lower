# Trabalho atual

## Item ativo

[WORK-LOWER-001](active/WORK-LOWER-001/spec.md): CP0 em revisão/certificação, UNTRUSTED BOOTSTRAP até commit/push e remoto green. Autorização explícita de 2026-09-06 para CP0..CP5 em modo multi-checkpoint; manifesto e [state](active/WORK-LOWER-001/state.md) registram o limite. Certificação local é registrada na evidência; nenhum recovery remoto é presumido.

## Próximo candidato

[BACKLOG-LOWER-001](backlog/BACKLOG-LOWER-001.md) está in_progress, vinculado ao único work item ativo. Demais candidatos permanecem sem autorização.

Promoção registrada conforme o [protocolo](../engineering/work-item-protocol.md), sem duplicata em proposals.

## Mapas

[Registry](registry.json), [backlog](backlog.md), [templates](../templates/README.md), [Git/review](../engineering/git-and-review.md), [gates](../engineering/gates.md).

## Estado remoto

Base confirmada: origin/main em `14aaafc5051eb287af6a8f126e8eaff78e30e365`, merge do PR de preparação #1. Branch `feat/first-entry-goback-slice`, [PR #2 Draft](https://github.com/Gustavo2358/cobol-lower/pull/2). CP0 teve certificação local e primeiro push; CI falhou na validação do workflow, remediação no mesmo checkpoint. Nenhum recovery remoto ainda.
