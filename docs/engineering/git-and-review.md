# Higiene Git e revisão humana

## Antes de editar

Conferir remotes, branch, upstream, `git status`, diff/staged e trabalho alheio. Buscar `origin` explicitamente. Em working tree limpa, atualizar a main com fast-forward (`git pull --ff-only` após selecionar a main correta); criar branch dedicada a partir dessa baseline. Se não houver remoto/main no repositório recém-criado, registrar a situação e seguir apenas a autorização de inicialização; não inventar upstream.

Não mudar branch nem aplicar sincronização que sobrescreva alterações locais. Dirty state não relacionado é preservado; usar worktree separado quando apropriado ou bloquear a operação insegura. Não fazer stash/reset/clean/discard silencioso.

## Trabalho incremental

Um work item usa uma branch e um PR. Checkpoints subsequentes e request changes continuam nesse par. Revalidar PR/head antes do push; não criar PR novo por checkpoint. Atualizar main durante o trabalho somente de forma não destrutiva, sem reescrever commits publicados por iniciativa própria. Conflitos resolvidos exigem review e gates novamente.

Remediação pós-fechamento usa `Review-Remediation-Evidence: docs/quality/<WORK>/R<N>.json`, não `Checkpoint-Evidence`/CP6. Exige a mesma certificação pre_commit (gates, oracles, falsificação restaurada, regressão, review e digest) com `kind`/`execution_mode: human-review-remediation`, `checkpoint: null`, autorização própria congelada e vínculo Git independente à base certificada. As únicas exclusões do digest são a própria evidence e `R<N>-state.md`; nenhum deles pode carregar implementação/contrato novo. O trailer deve ser único e seguro. SHA literal e receipt de CI ficam no PR/handoff pós-push, sem amend para autoinscrição.

Dependências resolvem o SHA pelo WORK/CP/evidence esperado, commit alcançável, trailer, conteúdo commitado, certificado, parent/digest e FREEZE. Só depois comparam receipt, PR e checks ao SHA resolvido. Receipt jamais escolhe seu próprio SHA esperado. Auditoria `--mode historical` aceita PR OPEN/MERGED com identidade e ancestralidade provadas; não exige branch ainda existente e não concede execução. Execução corrente exige PR OPEN, branch/head corretos e autorização vigente, inclusive em CI.

No modo multi-checkpoint, seguir a [transação](agent-session-protocol.md): 1 work item → 1 branch → 1 PR → N commits certificados. Cada CP exige commit/push e terminal PASS dos checks remotos obrigatórios no SHA publicado antes de avançar, PR atualizado e recovery vinculado à evidência. O SHA publicado é parte da memória persistente; corrigir bugs antigos no HEAD, sem amend/rewrite de checkpoint certificado. Stop humano final permanece.

Commits são focalizados e incluem testes/documentação ligados à regra. Nunca commitar mutação temporária, credenciais, fixture confidencial ou output de diagnóstico não intencional. Rever o diff staged completo; não confiar só em `git status`.

## Gates antes do handoff

Executar checks aplicáveis de [gates](gates.md), `git diff --check`, confirmar restauração de falsificações e escopo. Registrar o que não executou e por quê. O agente pode abrir/atualizar/push no PR quando o prompt autorizar; não efetua merge nem auto-merge.

## Review

Revisor avalia diff e contexto mínimo normativo, não apenas a descrição do autor. Findings trazem arquivo/linha, entrada que manifesta o problema, resultado observado versus esperado, regra e impacto. P1/P2 não são classes de impacto downstream. Um approve é limitado ao SHA e à evidência examinados, não certificação universal do runtime.

Request changes são corrigidos sem ampliar escopo. Não mudar expected, invariante ou fonte lock para esconder um finding. Se o contrato estiver ambíguo, registrar evidência e encaminhar a decisão à autoridade.

## Fechamento

Seguir o [lifecycle](work-item-protocol.md), deixando main limpa de active concluído. Commit de fechamento não representa merge. A promoção de conhecimento é verificável pelo mapa de links/IDs e pelos testes duráveis, não por copiar a tasklist inteira para history.

## Documentary closeout audit

A final `Work-Item-Closeout: <WORK-ID>` commit retains the unique `Checkpoint-Evidence` trailer and byte-identical certificate of its direct parent. The parent must already be certified and its required exact-head CI successful before preparing closure. The closeout changes only the five active-file deletions, its short history, registry/index/backlog and optional data-only closeout receipt. Production, tests, source locks, contracts and certificate bytes cannot change. CI proves this Git delta and the actual PR/head/base binding, then runs only docs/FAST on the current checkout. Full qualification remains attached to the original qualified HEAD/tree; no heavy remote execution is permitted. Verify-commit verifies the same binding; remote still checks the actual published closeout SHA. This does not grant execution authority to history or permit recertifying new production under a closed item. Initial CP0 null PR stays immutable; the history and remote audit bind the real PR. Merge uses the exact validated closeout HEAD.

CP6 W2B uses the explicit post-commit [local qualification protocol](ci-qualification.md). Historical certificates are preserved; administrative reconciliation may occur in the next authorized branch in its own documentation-only commit.
