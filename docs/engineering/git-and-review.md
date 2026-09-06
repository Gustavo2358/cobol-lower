# Higiene Git e revisão humana

## Antes de editar

Conferir remotes, branch, upstream, `git status`, diff/staged e trabalho alheio. Buscar `origin` explicitamente. Em working tree limpa, atualizar a main com fast-forward (`git pull --ff-only` após selecionar a main correta); criar branch dedicada a partir dessa baseline. Se não houver remoto/main no repositório recém-criado, registrar a situação e seguir apenas a autorização de inicialização; não inventar upstream.

Não mudar branch nem aplicar sincronização que sobrescreva alterações locais. Dirty state não relacionado é preservado; usar worktree separado quando apropriado ou bloquear a operação insegura. Não fazer stash/reset/clean/discard silencioso.

## Trabalho incremental

Um work item usa uma branch e um PR. Checkpoints subsequentes e request changes continuam nesse par. Revalidar PR/head antes do push; não criar PR novo por checkpoint. Atualizar main durante o trabalho somente de forma não destrutiva, sem reescrever commits publicados por iniciativa própria. Conflitos resolvidos exigem review e gates novamente.

Commits são focalizados e incluem testes/documentação ligados à regra. Nunca commitar mutação temporária, credenciais, fixture confidencial ou output de diagnóstico não intencional. Rever o diff staged completo; não confiar só em `git status`.

## Gates antes do handoff

Executar checks aplicáveis de [gates](gates.md), `git diff --check`, confirmar restauração de falsificações e escopo. Registrar o que não executou e por quê. O agente pode abrir/atualizar/push no PR quando o prompt autorizar; não efetua merge nem auto-merge.

## Review

Revisor avalia diff e contexto mínimo normativo, não apenas a descrição do autor. Findings trazem arquivo/linha, entrada que manifesta o problema, resultado observado versus esperado, regra e impacto. P1/P2 não são classes de impacto downstream. Um approve é limitado ao SHA e à evidência examinados, não certificação universal do runtime.

Request changes são corrigidos sem ampliar escopo. Não mudar expected, invariante ou fonte lock para esconder um finding. Se o contrato estiver ambíguo, registrar evidência e encaminhar a decisão à autoridade.

## Fechamento

Seguir o [lifecycle](work-item-protocol.md), deixando main limpa de active concluído. Commit de fechamento não representa merge. A promoção de conhecimento é verificável pelo mapa de links/IDs e pelos testes duráveis, não por copiar a tasklist inteira para history.
