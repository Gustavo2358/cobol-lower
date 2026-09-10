# Lifecycle de backlog, work items e checkpoints

## Endereços e fonte da verdade

`docs/work/registry.json` registra IDs e localização; `index.md` roteia estado corrente; `backlog.md` organiza candidatos. O manifesto do work item define escopo/autorização/gates; `state.md` registra execução factual. Nenhum registro isolado prova estado remoto do GitHub.

Um pacote de work item contém exatamente cinco arquivos: `work-item.yaml`, `spec.md`, `plan.md`, `eval.md` e `state.md`. Propostas preparadas ficam em `proposals/`; trabalho autorizado fica em `active/`. História curta fica em `history/<WORK-ID>.md`. README de diretório não é work item.

## Estados

Backlog: `candidate`, `needs_discovery`, `ready_for_authorization`, `in_progress`, `completed`, `deferred`. Work item: `proposed`, `ready_for_authorization`, `active`, `blocked`, `completed`. Em active só podem existir `active`/`blocked`; review_pending fica como fase no state. PR possui estado remoto separado.

IDs nunca são reutilizados. Uma proposta pronta não começa sozinha. Novo trabalho exige objetivo autorizado e checkpoint(s) explicitamente incluídos. Por padrão só um checkpoint está autorizado por prompt. Ausência de `authorization.execution_mode` significa `single-checkpoint`. Modo `multi-checkpoint` exige autorização humana expressa, `state: granted`, lista finita não vazia em `authorized_checkpoints` e referência factual em `authorization.authority` (pedido/data ou link, registrada também no state). Todo `state: granted` exige lista não vazia e `current_checkpoint` não nulo. IDs seguem `^CP[0-9]+$`. Expandir ranges nos IDs existentes do plano; G-DOCS deve rejeitar IDs inexistentes (inclusive CP999 se ausente do plano), current_checkpoint fora da lista autorizada e início de CP sem dependências anteriores satisfeitas por evidência certificada e checks remotos aplicáveis; não inferir modo pela quantidade de IDs. Templates e plano não são autoridade.

## Promoção

Confirmar main/upstream/dirty state e fontes; criar branch/PR ou continuar o par já designado. Preencher escopo de código/testes/docs, must_not_change, dependências, risco, referências e gates. Mover os cinco arquivos de proposals para active sem manter duplicata. Atualizar registry/index/backlog na mesma mudança. Registrar autorização humana e checkpoint atual no manifesto/state.

Campos mínimos do manifesto: schema_version, id, title, status, risk, goal, authorization, backlog_ids, source_scope, test_scope, docs_scope, must_read, related_domain_rules, related_decisions, related_invariants, evals, gates, must_not_change, checkpoints e git. Valores de autorização/PR desconhecidos são null/ausentes documentados, nunca números inventados.

Caminho futuro deve aparecer como `planned:<path>` somente em scope de produção/teste/artefato planejado. `must_read` sempre referencia arquivo existente. No bootstrap, pai ainda inexistente é legítimo se a criação do módulo estiver explicitamente no scope. Isso adapta o protocolo ProLeap a um repo sem src/POM; não simular que esses caminhos já existem. Converter planned em caminho real no checkpoint que o cria.

## Checkpoint

A [transação de sessão](agent-session-protocol.md) governa certificação, regressão, recovery e avanço nos dois modos. Certificação técnica fica na evidência; não introduz aprovação humana: `ready_for_review` pode ter certificação técnica e permitir avanço apenas no modo explicitamente multi-checkpoint. State registra modo/autoridade, CP atual, último certificado e referência Git verificável; status completed do work item continua seguindo o fechamento.

Cada checkpoint tem objetivo, dependências, artefato, oracle independente, classes negativas, gates aplicáveis e condição de parada. Concluir um checkpoint não conclui o work item inteiro. Evidência de execução aponta para commit/SHA e logs; modificar código depois da evidência exige nova execução dos checks afetados.

O mesmo PR acumula os commits de checkpoints e remediações. Review deve conseguir distinguir base aprovada e delta novo. Squash/rewrite não ocorre automaticamente. Uma revisão de contrato que extrapola o escopo pede autorização, não atualização unilateral da spec para acomodar código.

## Encerramento antes do merge

Request changes após fechamento local e antes do merge não recria `active/`. O item permanece `completed` em history; `remediations` registra ID `R1`, `R2` etc., estado `active`/`ready_for_review`/`reviewed`, caminhos explícitos de autorização, evidence e state em `docs/quality/<WORK>/`. Essa fase de remediação não é status active do work item nem checkpoint novo. Cada autorização humana é separada, restrita ao review e vinculada à base certificada, branch e PR originais. Os certificados anteriores não são reescritos. Após validação, o delta recebe commit focalizado e novo review no mesmo PR. A remediação não sobrevive ao merge como autorização de código.

O registro histórico separa `review_status` (`pending_human`, `changes_requested`, `reviewed`, `approved`) e `merge_status` (`open_not_merged`, `merged`). Aberto permite os quatro estados de review; mergeado exige reviewed/approved, ou `not_recorded` quando a API retorna reviews vazios, com `remote_observation`: repository, PR, branch/base, head, review commit/URL, merge commit, estado e instante observado. G-DOCS valida sua coerência, não atesta GitHub. G-GIT `reconcile --commit <SHA> --mode historical` compara metadata remota e reviews do head, detectando registro aberto obsoleto após merge. Reconciliação documental autorizada permanece read-only quanto ao produto e não reabre trabalho. Um certificado histórico válido nunca substitui autorização vigente.

Após checkpoints e review técnico, preparar commit de fechamento no mesmo PR: promover conhecimento durável, preservar testes/oracles, registrar histórico factual, remover active e atualizar registry/index/backlog. Rodar gates de fechamento. O PR final precisa ser revisto se o fechamento introduzir mudanças materiais.

Histórico pode dizer `completed` localmente e `merge_status: not_verified/open`; jamais inferir merge. Aprovação humana e aprovação de código não autorizam o agente a fazer merge. Após merge confirmado por metadata confiável, apenas reconciliar o registro remoto, sem reabrir trabalho nem deixar active obsoleto.

## Duas camadas de verificação

G-DOCS é offline: links, IDs, diretórios, estados, escopo e referências. G-GIT usa metadata confiável para branch, PR, base/head, reviews e merge. O checker documental não consulta rede nem deduz merge por frases do state.

## Conteúdo dos cinco arquivos

Spec: problema, objetivo, domínio suportado, classes, premissas, comportamento normal/incerto, não objetivos e regras. Plan: checkpoints, dependências, superfície arquitetural, migrações e artefatos. Eval: corretude, positivos, negativos, ambíguos, adversariais, regressões, metamorfismo e escala. State: onde estamos, verde conhecido, restante, descobertas. Template em [templates](../templates/README.md).

## Mudar o próprio harness

Exigir work item ou escopo documental autorizado; demonstrar a lacuna com um contracaso do checker ou revisão. Atualização de status nunca substitui implementação do gate. Preservar rastreabilidade de decisões antigas sem duplicar tasklists em contexto padrão.

Para `not_recorded`, review_commit/review_url são null e reviews é uma lista vazia observada;
G-GIT reconsulta essa ausência e o head/merge. Isso não concede aprovação nem execução.
O manifesto e o certificado inicial CP0 anteriores à criação da PR podem ter PR null.
Certificados emitidos com PR já conhecida registram seu número real. Na seleção da
auditoria CI após merge, o CP0 inicial pode ser vinculado pela PR única comprovada
na API, SHA do merge, base/repositório, ancestralidade do head, branch e evidence
idêntica. PR explícita contraditória e null fora de CP0 continuam inválidos. Essa
exceção não altera o certificado histórico nem concede execução; também não faz
reconciliação automática do registry. O registro histórico continua exigindo o PR
real e os vínculos certificados próprios da reconciliação. Evidência histórica não é reescrita para
mover links: G-DOCS resolve links locais ausentes de documentos quality byte-idênticos ao
head histórico na mesma revisão Git, inclusive anchors. Links correntes continuam estritos.

## Documentary closeout audit

A final `Work-Item-Closeout: <WORK-ID>` commit retains the unique `Checkpoint-Evidence` trailer and byte-identical certificate of its direct parent. The parent must already be certified and its required exact-head CI successful before preparing closure. The closeout changes only the five active-file deletions, its short history, registry/index/backlog and optional data-only closeout receipt. Production, tests, source locks, contracts and certificate bytes cannot change. CI proves this Git delta and the actual PR/head/base binding, then runs full on the current checkout in historical audit mode against the original certified parent. Verify-commit verifies the same binding; remote still checks the actual published closeout SHA. This does not grant execution authority to history or permit recertifying new production under a closed item. Initial CP0 null PR stays immutable; the history and remote audit bind the real PR. Merge uses the exact validated closeout HEAD.
