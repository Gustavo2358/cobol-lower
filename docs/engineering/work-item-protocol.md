# Lifecycle de backlog, work items e checkpoints

## Endereços e fonte da verdade

`docs/work/registry.json` registra IDs e localização; `index.md` roteia estado corrente; `backlog.md` organiza candidatos. O manifesto do work item define escopo/autorização/gates; `state.md` registra execução factual. Nenhum registro isolado prova estado remoto do GitHub.

Um pacote de work item contém exatamente cinco arquivos: `work-item.yaml`, `spec.md`, `plan.md`, `eval.md` e `state.md`. Propostas preparadas ficam em `proposals/`; trabalho autorizado fica em `active/`. História curta fica em `history/<WORK-ID>.md`. README de diretório não é work item.

## Estados

Backlog: `candidate`, `needs_discovery`, `ready_for_authorization`, `in_progress`, `completed`, `deferred`. Work item: `proposed`, `ready_for_authorization`, `active`, `blocked`, `completed`. Em active só podem existir `active`/`blocked`; review_pending fica como fase no state. PR possui estado remoto separado.

IDs nunca são reutilizados. Uma proposta pronta não começa sozinha. Novo trabalho exige objetivo autorizado e checkpoint(s) explicitamente incluídos. Por padrão só um checkpoint está autorizado por prompt.

## Promoção

Confirmar main/upstream/dirty state e fontes; criar branch/PR ou continuar o par já designado. Preencher escopo de código/testes/docs, must_not_change, dependências, risco, referências e gates. Mover os cinco arquivos de proposals para active sem manter duplicata. Atualizar registry/index/backlog na mesma mudança. Registrar autorização humana e checkpoint atual no manifesto/state.

Campos mínimos do manifesto: schema_version, id, title, status, risk, goal, authorization, backlog_ids, source_scope, test_scope, docs_scope, must_read, related_domain_rules, related_decisions, related_invariants, evals, gates, must_not_change, checkpoints e git. Valores de autorização/PR desconhecidos são null/ausentes documentados, nunca números inventados.

Caminho futuro deve aparecer como `planned:<path>` somente em scope de produção/teste/artefato planejado. `must_read` sempre referencia arquivo existente. No bootstrap, pai ainda inexistente é legítimo se a criação do módulo estiver explicitamente no scope. Isso adapta o protocolo ProLeap a um repo sem src/POM; não simular que esses caminhos já existem. Converter planned em caminho real no checkpoint que o cria.

## Checkpoint

Cada checkpoint tem objetivo, dependências, artefato, oracle independente, classes negativas, gates aplicáveis e condição de parada. Concluir um checkpoint não conclui o work item inteiro. Evidência de execução aponta para commit/SHA e logs; modificar código depois da evidência exige nova execução dos checks afetados.

O mesmo PR acumula os commits de checkpoints e remediações. Review deve conseguir distinguir base aprovada e delta novo. Squash/rewrite não ocorre automaticamente. Uma revisão de contrato que extrapola o escopo pede autorização, não atualização unilateral da spec para acomodar código.

## Encerramento antes do merge

Após checkpoints e review técnico, preparar commit de fechamento no mesmo PR: promover conhecimento durável, preservar testes/oracles, registrar histórico factual, remover active e atualizar registry/index/backlog. Rodar gates de fechamento. O PR final precisa ser revisto se o fechamento introduzir mudanças materiais.

Histórico pode dizer `completed` localmente e `merge_status: not_verified/open`; jamais inferir merge. Aprovação humana e aprovação de código não autorizam o agente a fazer merge. Após merge confirmado por metadata confiável, apenas reconciliar o registro remoto, sem reabrir trabalho nem deixar active obsoleto.

## Duas camadas de verificação

G-DOCS é offline: links, IDs, diretórios, estados, escopo e referências. G-GIT usa metadata confiável para branch, PR, base/head, reviews e merge. O checker documental não consulta rede nem deduz merge por frases do state.

## Conteúdo dos cinco arquivos

Spec: problema, objetivo, domínio suportado, classes, premissas, comportamento normal/incerto, não objetivos e regras. Plan: checkpoints, dependências, superfície arquitetural, migrações e artefatos. Eval: corretude, positivos, negativos, ambíguos, adversariais, regressões, metamorfismo e escala. State: onde estamos, verde conhecido, restante, descobertas. Template em [templates](../templates/README.md).

## Mudar o próprio harness

Exigir work item ou escopo documental autorizado; demonstrar a lacuna com um contracaso do checker ou revisão. Atualização de status nunca substitui implementação do gate. Preservar rastreabilidade de decisões antigas sem duplicar tasklists em contexto padrão.
