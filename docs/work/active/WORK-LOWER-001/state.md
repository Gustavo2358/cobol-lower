# Estado — WORK-LOWER-001

## Onde estamos

Autorização explícita do usuário nesta sessão em 2026-09-06: execution mode `multi-checkpoint`, CP0, CP1, CP2, CP3, CP4, CP5; avanço sequencial certificado, review humano final. Sem merge/auto-merge, mudanças upstream ou expansão de capability. `current_checkpoint: CP0`; CP1..CP5 não iniciados.

Branch `feat/first-entry-goback-slice`, [PR #2 Draft/open](https://github.com/Gustavo2358/cobol-lower/pull/2), base limpa `14aaafc5051eb287af6a8f126e8eaff78e30e365` confirmada em origin/main após merge do PR de preparação #1. Working tree inicial limpa e checkouts upstream preservados.

CP0 teve certificação local e commit/push `98dbb60f204f42191fa1ee6f1a5b7caca89355ed`. Run [34062009803](https://github.com/Gustavo2358/cobol-lower/actions/runs/34062009803) terminou failure antes de criar jobs/check `checkpoint`. Portanto último recovery plenamente certificado: **nenhum**; a base main acima continua o ponto anterior confirmado. UNTRUSTED BOOTSTRAP permanece até remoto green. Não reescrever o commit anterior.

Remediação de workflow no próprio CP0: runner.temp não está disponível em jobs.<job>.env; usar diretório em /tmp identificado por github.run_id/run_attempt. Fonte oficial e review em [CP0-review](../../../quality/WORK-LOWER-001/CP0-review.md). Prazo remoto original: primeiro push em **2026-09-06T21:45:08Z**, limite **1200 segundos**, deadline **2026-09-06T22:05:08Z**, sem reiniciar entre SHAs/reruns.

## Verde conhecido

172 checks upstream no SHA fixado com Temurin 21.0.12.1+1/Maven 3.9.16 em clone/cache isolados. Core: 20 assertions AIR manual/boundary em memória. Remediação reexecutou docs/architecture/semantic/git/fast, exit 0, e 58 testes do harness, exit 0. Oito falsificações com RED esperado, restauração e segundo GREEN: quatro documentais, três de transporte em source/API/bytecode e Return→Halt por input controlado de processo. Não há mutação no candidato.

Evidência atual: [CP0.json](../../../quality/WORK-LOWER-001/CP0.json); logs da remediação adjacentes. Novo candidato será certificado mecanicamente antes de commit/push. Referência Git da certificação resolve pelo trailer `Checkpoint-Evidence: docs/quality/WORK-LOWER-001/CP0.json`; certified_commit null na própria evidência conforme protocolo. Recibo pós-push ficará no PR/handoff e state seguinte.

## Findings e limites

[Self-review](../../../quality/WORK-LOWER-001/CP0-review.md) registra as correções de stub inválido, teste de obrigações, jdeps, referências FREEZE, vínculo da evidência ao commit e workflow. Não houve revisor independente. Erros de cwd upstream e DNS do sandbox foram setup, não falsificações semânticas. A [nota de obrigação](../../../quality/WORK-LOWER-001/CP0-obligation-oracle.md) preserva o oracle; nenhuma claim AIR-STRUCTURE foi atribuída ao produto.

Source lock preservado. SP/AIR/air-java remotos coincidem com ele; CFG avançou com bootstrap Java, fora da dependência/capability deste trabalho. AIR JSON permanece draft e fora do slice. G-DOCS verifica estrutura de registros, não verdade dos logs; G-ARCH é complementado por review semântico. AirValidator estrutural não certifica lowering. Adapters ainda vazio; EVAL-LWR-004 é somente o smoke CP0 em memória. Nenhum golden SP, decoder ou regra de lowering criado.

## Próximo passo não iniciado

Certificar e publicar a remediação de CP0 no mesmo PR; confirmar SHA e check obrigatório `checkpoint`/github-actions/push dentro do prazo original. Só depois iniciar CP1. Performance/full pertencem a CP4/CP5; transport/integration e demais backlog permanecem fora de escopo. Não houve merge/auto-merge nem trabalho fora de WORK-LOWER-001.
