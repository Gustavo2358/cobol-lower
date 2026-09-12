# Protocolo de sessão do agente

## Entrada

Identificar tarefa autorizada, checkpoint, branch/PR, source lock e working tree. Ler AGENTS e índice de trabalho, depois manifesto e state do item. Se não houver autorização de implementação, trabalhar apenas no escopo documental pedido. Um backlog pronto não basta.

Carregar somente contratos, ADRs, invariantes e evals da tarefa. Consultar fonte externa quando uma regra ou algoritmo não trivial precisar de fundamentação; registrar referência/revisão e evidência. A memória do modelo não é autoridade.

## Contexto de execução

Separar fatos confirmados, hipóteses, limites e decisões ainda pendentes. Antes de editar, verificar se o snapshot upstream mudou; não atualizar o lock automaticamente. Ler diff e estado existente para não repetir trabalho ou sobrescrever mudanças de outra sessão.

Um plano local curto identifica resultado observável, arquivos em escopo, teste focal e gates. Não transformar esse plano em uma segunda especificação. Se o escopo for maior que o checkpoint, registrar proposta de fatiamento e continuar apenas a parte autorizada que permaneça segura.

## Modos e autoridade

O padrão é um checkpoint por autorização/review boundary: certificar, commit/push quando autorizado e parar para review/autorização humana. A presença de CP0..CP5 no plano não autoriza executá-los. Somente autorização humana expressa de modo `multi-checkpoint` e lista finita permite dispensar reviews humanos intermediários. Registrar a autoridade no manifesto/state conforme [lifecycle](work-item-protocol.md); exemplos e schemas não concedem autorização.

## Transação de checkpoint

Executar sequencialmente; evidência usa o [template existente](../templates/checkpoint-evidence.json), em `docs/quality/<WORK-ID>/<CP-ID>.json`, roteado pelo state. Não criar engine ou depender de fornecedor/modelo. Git + documentos persistidos são a memória autoritativa, nunca apenas a janela de contexto.

1. **RECOVER:** ler manifesto, spec, plan, eval e state; confirmar autorização, branch/PR (ou criação inicial autorizada), último checkpoint certificado e SHA. Conferir commit, evidência e diff local/remoto, incluindo mudanças inesperadas. No primeiro CP, registrar base limpa em vez de inventar checkpoint anterior. Reconciliar state com Git antes de editar; após interrupção recomeçar por aqui.
2. **FREEZE:** fixar objetivo, evals, invariantes, gates/perfil, source lock, `source_scope`, `test_scope`, `must_not_change`, classes negativas e expected/oracles independentes. Registrar snapshots reproduzíveis do contrato na evidência conforme a seção FREEZE abaixo; incluir lista identificável de checks remotos obrigatórios e limite de espera para execução remota, quando aplicáveis. Esse é o contrato de sucesso da transação: fixture, expected, invariant, eval ou gate não podem ser enfraquecidos para acomodar implementação. Oracle comprovadamente errado exige mudança de contrato e avaliação das stop conditions, nunca ajuste silencioso.
3. **IMPLEMENT:** somente escopo do CP autorizado, por regra/oracle → RED pela causa esperada → implementação geral mínima → GREEN. Registrar descobertas adjacentes sem iniciar outra capability. Falha de compilação por API ausente não prova oracle semântico; classificar falha de input, teste, algoritmo, contrato, setup ou escopo antes de tentar novamente. Não repetir tentativas sem hipótese nova sustentada por evidência; aplicar a condição anti-spin abaixo.
4. **VERIFY:** executar testes e gates focais congelados, com comandos, ambiente, contagens, logs e exit codes reais. Afirmação do implementador não substitui feedback executável.
5. **FALSIFY:** buscar contraexemplo conforme [falsificação](falsification.md), por mutação, input adversarial, metamorfismo, oracle/validator independente conforme aplicável. Quebrar a propriedade, observar falha pelo oracle esperado, restaurar integralmente a mutação e conferir digest/diff. RED por setup não vale; registrar teste positivo da baseline também.
6. **REGRESS:** executar segundo GREEN focal, todos os gates anteriores baratos e toda a suíte semântica cumulativa disponível, incluindo garantias certificadas anteriores. No estágio pequeno atual, docs, architecture e git são baratos; não retirar testes antigos nem reduzir conjunto após falha. `full` e challenge são obrigatórios no fechamento e nos CPs de alto risco indicados pelo plano (WORK-LOWER-001: CP4/CP5). Gate anterior obrigatório indisponível bloqueia certificação; garantia de CP1 quebrada em CP3 impede certificar CP3.
7. **REVIEW:** revisar diff completo desde a base/último commit certificado, incluindo staged, unstaged e arquivos novos: escopo, arquivos inesperados, must_not_change, heurísticas, modelo duplicado, oracle enfraquecido, resíduos de mutação e contrato acidental. Registrar findings/resolução e identidade/contexto do revisor. Segunda passada do mesmo agente é `self-review`; review independente exige outro revisor/contexto e evidência vinculada ao candidato. Um verifier/falsifier independente é recomendável quando disponível, sem ser dependência do runtime.
8. **CERTIFY:** conferir mecanicamente a evidência contra o contrato congelado e os resultados executáveis. `certification.scope: pre_commit` identifica essa prova local. Todos os requisitos obrigatórios verificáveis antes do commit devem estar PASS; checks remotos do novo SHA são obrigações pós-push, sem dispensá-los ou declará-los PASS antecipadamente. Exigir revisão concluída sem finding bloqueante e mutações restauradas. `FAIL`, `NOT_RUN`, `UNKNOWN`, `BLOCKED`, `UNRESTORED`, campos obrigatórios ausentes ou executor apenas especificado impedem certificação. `NOT_APPLICABLE` só com razão contratual prévia para item não obrigatório. Registrar base, candidato, gates, falsificações, restauração, regressão, limitações, review e estado; “checkpoint done” não é certificado. O bootstrap deve estabelecer essa checagem documental com contracasos; este documento não é seu executor.
9. **COMMIT/PUSH:** estado certificado localmente produz commit focalizado na mesma branch/PR, seguido de push e atualização incremental do PR. Conferir conteúdo commitado e SHA remoto; depois observar checks remotos obrigatórios conforme gates. Até terminal PASS de todos no SHA publicado, o commit permanece aguardando CI e não substitui o último recovery point plenamente certificado. Sem checks remotos obrigatórios, registrar essa não aplicabilidade no FREEZE, nunca inferi-la de lista vazia retornada pela API. Preservar histórico certificado: sem squash, rebase destrutivo, amend antigo ou rewrite automático. Bug de CP3 descoberto em CP5 é corrigido no HEAD, revalidado e evidenciado em CP5. Falha de push impede avanço até reconciliação segura.
10. **ADVANCE:** exigir CP atual certificado, commit/push confirmados e todos os checks remotos obrigatórios em terminal PASS no SHA publicado, próximo CP explicitamente autorizado em modo multi-checkpoint e nenhuma stop condition. No padrão, parar para humano. Ao esgotar a lista autorizada, executar gates finais/full/challenge aplicáveis antes do último commit, confirmar também os checks remotos obrigatórios do último SHA, atualizar PR e parar para review humano, sem merge/auto-merge. Um range parcial exige congelar seus gates de handoff antes de começar; não autoriza checkpoints de fechamento fora do range.

## FREEZE reproduzível

Novas execuções/certificações exigem evidência v2; selecionar v1 não é uma opção para contornar imutabilidade. Verificação read-only de certificados antigos continua disponível.

Em evidência v2, cada referência declara `usage`: `reference_only` conserva a referência histórica normativa; `candidate_immutable` exige também SHA-256 idêntico nos bytes do candidato e no commit certificado, mesmo com `git_revision` preenchida. Expected independente, golden, oracle, fixture normativa de falsificação, source lock e qualquer artefato explicitamente congelado usam `candidate_immutable`. Não deduzir a classificação pelo nome do arquivo, nem exigir imutabilidade de toda documentação histórica. V2 rejeita classificação ausente/desconhecida. Certificados v1 preservados são auditados sob seu contrato histórico, incluindo a proteção especial do source lock: não recebem retroativamente uma prova v2 que nunca declararam.

`frozen_contract.references` é a lista canônica de arquivos que fixam objetivo/escopos/autorização, regras, gates, evals, oracles e source lock. Cada entrada contém `path` relativo ao repo, `sha256` hexadecimal minúsculo dos bytes exatos do arquivo (SHA-256, sem normalizar JSON/YAML, encoding ou finais de linha) e `git_revision`: SHA completo do commit que preserva esses bytes, ou null para arquivo novo ainda não commitado. Não há digest agregado nem canonicalização implícita; paths são únicos, ordenados lexicograficamente, sem diretórios ou glob.

Na retomada, obter os bytes por `git show <git_revision>:<path>` e recalcular SHA-256. Quando git_revision for null, os bytes congelados devem sobreviver intactos no candidato e no commit de checkpoint; antes deste, usar o arquivo local e conferir o hash. Se não sobreviverem a uma interrupção, FREEZE está incompleto: não inferir o contrato do chat. Mudança autorizada de contrato exige novo FREEZE e revalidação; mudança de metadados de execução do manifesto não altera a versão normativa congelada, que continua recuperável pela revisão registrada. A lista inclui `docs/sources/sources.lock.json`; seus bytes fixam também os SHAs upstream. Expected novo deve ser congelado antes de IMPLEMENT e mantido no commit, nunca regenerado pelo código sob teste.

## Identidade da evidência e recovery

`candidate_diff_sha256` no top-level é a única identidade canônica do conteúdo candidato. REVIEW preenche `review.candidate_reference: "#/candidate_diff_sha256"` e usa o mesmo registro; não copia o hash nem mantém `review.reviewed_commit`. `reviewed_head` no top-level é somente o HEAD anterior ao commit, preenchido junto do digest antes da revisão; não identifica sozinho alterações staged. G-DOCS rejeita campos duplicados legados no review e referência diferente; G-GIT usa base_commit + digest canônico + reviewed_head, depois confere o commit publicado. Alterar o digest invalida o review anterior e exige nova revisão.

Evitar a autorreferência impossível de gravar o SHA de um commit dentro dele mesmo. Antes de certificar, registrar `reviewed_head` (HEAD anterior ao commit) e `candidate_diff_sha256`: SHA-256 de `git diff --cached --binary <base_commit> -- .` (registrar opções/configuração de diff e usar as mesmas na retomada) excluindo somente a própria evidência e o state do item, com exclusões explícitas em `candidate_exclusions`. Exigir worktree igual ao index e ausência de arquivos novos não incluídos no candidato antes da certificação. Stagear e revisar todo o candidato; os dois arquivos excluídos também exigem revisão documental final e não podem carregar código/contratos novos. Qualquer mudança no restante invalida o digest e exige rever/reexecutar checks afetados.

O commit inclui evidência/state e trailer `Checkpoint-Evidence: <path>`. `certified_commit` fica null na própria evidência; resolver o SHA pelo commit com esse trailer, conferir digest do diff base→commit com as mesmas exclusões e resultados, inclusive checks remotos obrigatórios em terminal PASS no SHA publicado, antes de aceitá-lo como recovery point. Persistir SHA literal e recibo pós-push no PR/handoff e no state do próximo CP: IDs/URLs dos runs/checks, SHA associado, conclusão terminal e instante da consulta confiável, incluindo falhas/pendências. O recibo completa a evidência pré-commit sem amend para autoinscrição ou commit apenas para gravar resultado do próprio CI. RECOVER reconsulta os checks do SHA, não confia apenas no recibo. Correção de CI gera novo commit no CP atual, nova validação e nova observação remota; nunca iniciar o próximo CP para corrigir o anterior. O state identifica último CP, caminho da evidência, SHA anterior e referência ao commit que contém a certificação atual. Se a sessão cair entre commit/push/state, RECOVER verifica Git e remoto; não presume push ou repete implementação. Commit sem certificação válida, inclusive diagnóstico de blocker, nunca é recovery point.

## CP0: bootstrap da trusted execution boundary

CP0 começa em `UNTRUSTED BOOTSTRAP`. Não pode usar gates apenas especificados como prova. Antes de CP1, deve estabelecer os executores previstos pelo CP0, a checagem da certificação, oracles fundamentais e respectivos positivos/contracasos; demonstrar falsificações pela causa certa, restaurar todas as mutações, executar segundo GREEN e regressão, certificar e produzir commit saudável com push e checks remotos obrigatórios confirmados conforme gates. Só então começa o loop confiável CP1+. A autorização multi-checkpoint não dispensa essa fronteira. Esta adaptação documental não implementa nem executa CP0.

## Stop conditions e interrupção

Decisão local reversível ou pequena incerteza não exige interrupção humana. Corrigir falhas dentro do contrato e repetir verificação; não avançar enquanto falham. Suspender avanço automático diante de blocker material:

- Upstream/source lock real diverge da baseline e exige reinterpretar spec ou premissa fundamental.
- Gate obrigatório não pode ser implementado/executado no escopo autorizado.
- Correção exige alterar must_not_change, capability fora do CP ou work item não autorizado.
- Contraexemplo exige redesenhar contrato/invariant já certificado; oracle errado não é licença para enfraquecê-lo.
- Git e último certificado não podem ser reconciliados sem operação destrutiva ou hipótese insegura.
- Dependência externa obrigatória indisponível impede a evidência exigida.
- Continuar exigiria inventar fato que deveria ser publicado upstream.
- Tentativas de reparo não produzem progresso material: a mesma falha retorna sem nova evidência que diferencie a próxima hipótese. Não alternar patches/reruns equivalentes nem esquecer tentativas anteriores após perder contexto; registrar síntese factual das hipóteses e resultados no state, declarar blocker e parar para humano.
- Checks remotos obrigatórios não chegam a terminal PASS dentro do limite de espera congelado, ou sua indisponibilidade impede confirmar o SHA. Aguardar pending não autoriza reruns indefinidos; falha reparável segue IMPLEMENT e a condição anti-spin.

Preservar último checkpoint certificado; registrar blocker factual no state/evidência, pendências e próxima ação não iniciada. Restaurar mutações; se impossível, registrar `UNRESTORED` e impedir commit/push desse conteúdo. Pushar apenas mudanças coerentes já autorizadas e seguras, identificando diagnóstico não certificado quando for o caso; deixar PR recuperável e parar para humano. Nunca fabricar GREEN ou fallback heurístico para chegar ao último CP. State é memória curta, não transcript nem prova de CI/merge; certificação técnica não equivale à aceitação humana.

## Saída

Responder com objetivo atingido ou bloqueado, commit/PR, testes reais, findings, limitações e próximo checkpoint não iniciado. Atualizar índices conforme lifecycle. Não misturar “pronto para review”, “aprovado”, “concluído localmente” e “mergeado”.

## CP6 W2B qualification transaction

The explicit user-authorized [CI/local split](ci-qualification.md) supersedes the
pre-commit full timing above for W2B. Freeze → focused RED/GREEN → self-review →
local mutations → exact restoration/second GREEN → clean candidate commit →
LOCAL_QUALIFICATION once on exact HEAD/tree → push/Draft PR → remote FAST → human
review. No heavy remote/manual qualification. Documentation-only successors do
not inherit a full execution; protected-blob equality preserves the original receipt.
