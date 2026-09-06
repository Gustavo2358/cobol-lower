# Preparação do harness multi-checkpoint — evidência documental

Data: 2026-09-06. Escopo autorizado diretamente pelo pedido desta sessão: somente adaptação do harness, um commit/PR; nenhum checkpoint de WORK-LOWER-001. Base: `a429abdab3d9f9d57502749e0f34980edc032caf`; branch: `docs/long-running-checkpoint-protocol`. O commit que adiciona este relatório identifica o candidato final; SHA/PR publicados no handoff remoto. Este relatório não certifica CP0.

## Verificações reais

| Comando/verificação | Resultado |
| --- | --- |
| `git fetch origin`; `git pull --ff-only origin main` | Exit 0; main já atualizada, worktree inicial limpo. |
| `git ls-remote origin refs/heads/main`; `gh pr list --state open --json number,headRefName,baseRefName` | Exit 0; baseline acima, nenhum PR aberto antes desta adaptação. |
| `python3 /tmp/check-long-running.py` | Exit 0: schema Draft 2020-12 válido; três positivos, sete contracasos rejeitados e segundo GREEN; JSON/YAML, links relativos/anchors, must_read, referências invariantes/evals, registry e escopo documental íntegros. |
| `python3 /tmp/check-recovery.py` | Exit 0: digest staged equivale ao diff base→commit; mudança de conteúdo invalida digest; restauração retorna GREEN/worktree limpo. Repositório descartável removido. |
| `git diff --check` | Exit 0. |
| Revisão manual do diff completo e índice/backlog/registry/proposal/history | Self-review concluído; nenhuma mudança de produção, autorização ou promoção. Contratos semânticos/source lock preservados. |

Auditores pontuais em `/tmp`, externos ao repositório, usando Python 3, PyYAML, jsonschema e Git. Não são executores instalados do harness nem CI. Nenhum mutante alterou a proposta real: testes de schema usaram cópias em memória. Não houve revisão independente nem teste Java/Maven/AirValidator.

## Contracasos estruturais reproduzíveis

Usar uma cópia do manifesto real como positivo sem autorização. Para o positivo sintético multi-checkpoint, preencher authorization com state granted, execution_mode multi-checkpoint, lista CP0..CP5, current_checkpoint CP0 e authority textual explicitamente sintética. Para o positivo padrão, remover execution_mode e limitar lista a CP0. Validar com `jsonschema.Draft202012Validator` contra o schema local; nunca persistir essas autorizações sintéticas na proposta.

Sobre o positivo multi, cada alteração isolada deve gerar ValidationError: lista vazia; authority removida; execution_mode removido; execution_mode single-checkpoint; state not_granted; IDs duplicados; modo desconhecido. Os sete casos foram rejeitados. A lacuna anterior foi reproduzida: schema da base aceitava granted + lista CP0..CP5 sem modo/autoridade; o novo rejeita. Os três positivos passaram novamente após o challenge.

Recovery foi exercitado criando baseline e candidato Markdown em Git descartável: calcular SHA-256 do diff staged binário contra base, excluindo somente evidence/state; adicionar esses metadados e commitar; comparar com diff base→commit sob mesmas opções. Alterar o Markdown invalida digest; restaurá-lo recupera digest e worktree limpo. Isso verifica a vinculação por conteúdo, não implementa G-GIT ou certificação de software.

## Challenge de design — self-review

| Tentativa | Regra que impede / resultado da revisão |
| --- | --- |
| Prompt comum iniciar todo o plano | Modos/lifecycle exigem opt-in + lista + autoridade; ausência de modo mantém single. Schema rejeita lista múltipla no padrão. |
| Avançar após FAIL obrigatório | CERTIFY bloqueia; ADVANCE exige certificado, commit/push e ausência de stop condition. Correção local não autoriza pular gate. |
| Certificar com NOT_RUN | Obrigatório pendente impede certificado; registro em elaboração não é certificação. Enforcement completo depende de CP0. |
| Mutação sobreviver ao commit | FALSIFY exige restauração/digest e segundo GREEN; REVIEW/index conferem resíduos; UNRESTORED impede publicação. |
| CP3 quebrar garantia de CP1 | REGRESS exige gates baratos anteriores e suíte semântica cumulativa; falha impede CP3. Não promete detectar propriedade sem oracle: CP0/evals devem estabelecê-los. |
| Self-review virar independent review | REVIEW exige identidade/contexto e evidência de outro revisor para claim independente. Esta revisão é self-review. |
| Contrato bloqueado gerar fallback inventado | Stop conditions de contrato/upstream e fatos ausentes; preservar certificado e parar para humano. |
| Recovery depender do chat | Base, digest, evidência, trailer e SHA remoto; RECOVER confronta estado com Git. Teste descartável passou. |
| Um PR por checkpoint | Git e transação exigem um par branch/PR por work item. |
| Preparação autorizar implementação | Manifesto permanece not_granted, lista vazia, seis not_started; registry/proposal preservados, active vazio. |

Findings corrigidos: stop gates do plano eram ambíguos entre modos; schema não distinguia lista de autorização contínua; certificação precisava vincular candidato sem SHA autorreferente e evitar exigir resultado de push antes de existir commit. A regra geral agora separa candidato, certificação e confirmação pós-commit. Nenhum finding bloqueante remanescente nesta self-review.

## Não executado e próximo passo

G-DOCS, G-ARCH, G-GIT e demais gates catalogados continuam `SPECIFIED_NOT_IMPLEMENTED`, entrypoint null. Não foram executados como gates do repositório; auditoria pontual não muda esse estado. Não existem testes do harness instalados, runtime arquitetural ou suíte semântica cumulativa. Verificação arquitetural desta entrega limita-se à ausência de implementação e à revisão de preservação das fronteiras. Fontes upstream não foram reconsultadas: nenhum contrato/pin mudou.

Certificação mecânica completa (incluindo rejeição de evidência incompleta e confirmação remota) deverá ser implementada/provada no CP0 futuramente autorizado. O schema não prova autoridade humana real, conteúdo de logs ou revisão independente. Após commit/push/PR desta preparação, parar para review humano; futura autorização explícita usa o [modelo de prompt](../engineering/prompt-authoring.md). CP0..CP5 continuam não executados; sem merge/auto-merge.
