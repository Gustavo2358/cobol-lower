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
2. **FREEZE:** fixar objetivo, evals, invariantes, gates/perfil, source lock, `source_scope`, `test_scope`, `must_not_change`, classes negativas e expected/oracles independentes. Registrar referências e digest do contrato na evidência. Esse é o contrato de sucesso da transação: fixture, expected, invariant, eval ou gate não podem ser enfraquecidos para acomodar implementação. Oracle comprovadamente errado exige mudança de contrato e avaliação das stop conditions, nunca ajuste silencioso.
3. **IMPLEMENT:** somente escopo do CP autorizado, por regra/oracle → RED pela causa esperada → implementação geral mínima → GREEN. Registrar descobertas adjacentes sem iniciar outra capability. Falha de compilação por API ausente não prova oracle semântico; classificar falha de input, teste, algoritmo, contrato, setup ou escopo antes de tentar novamente.
4. **VERIFY:** executar testes e gates focais congelados, com comandos, ambiente, contagens, logs e exit codes reais. Afirmação do implementador não substitui feedback executável.
5. **FALSIFY:** buscar contraexemplo conforme [falsificação](falsification.md), por mutação, input adversarial, metamorfismo, oracle/validator independente conforme aplicável. Quebrar a propriedade, observar falha pelo oracle esperado, restaurar integralmente a mutação e conferir digest/diff. RED por setup não vale; registrar teste positivo da baseline também.
6. **REGRESS:** executar segundo GREEN focal, todos os gates anteriores baratos e toda a suíte semântica cumulativa disponível, incluindo garantias certificadas anteriores. No estágio pequeno atual, docs, architecture e git são baratos; não retirar testes antigos nem reduzir conjunto após falha. `full` e challenge são obrigatórios no fechamento e nos CPs de alto risco indicados pelo plano (WORK-LOWER-001: CP4/CP5). Gate anterior obrigatório indisponível bloqueia certificação; garantia de CP1 quebrada em CP3 impede certificar CP3.
7. **REVIEW:** revisar diff completo desde a base/último commit certificado, incluindo staged, unstaged e arquivos novos: escopo, arquivos inesperados, must_not_change, heurísticas, modelo duplicado, oracle enfraquecido, resíduos de mutação e contrato acidental. Registrar findings/resolução e identidade/contexto do revisor. Segunda passada do mesmo agente é `self-review`; review independente exige outro revisor/contexto e evidência vinculada ao candidato. Um verifier/falsifier independente é recomendável quando disponível, sem ser dependência do runtime.
8. **CERTIFY:** conferir mecanicamente a evidência contra o contrato congelado e os resultados executáveis. Todos os requisitos obrigatórios devem estar PASS, revisão concluída sem finding bloqueante e mutações restauradas. `FAIL`, `NOT_RUN`, `UNKNOWN`, `BLOCKED`, `UNRESTORED`, campos obrigatórios ausentes ou executor apenas especificado impedem certificação. `NOT_APPLICABLE` só com razão contratual prévia para item não obrigatório. Registrar base, candidato, gates, falsificações, restauração, regressão, limitações, review e estado; “checkpoint done” não é certificado. O bootstrap deve estabelecer essa checagem documental com contracasos; este documento não é seu executor.
9. **COMMIT/PUSH:** apenas estado certificado vira recovery point, em commit focalizado na mesma branch/PR, seguido de push e atualização incremental do PR. Conferir conteúdo commitado e SHA remoto antes de avançar. Preservar histórico certificado: sem squash, rebase destrutivo, amend antigo ou rewrite automático. Bug de CP3 descoberto em CP5 é corrigido no HEAD, revalidado e evidenciado em CP5. Falha de push impede avanço até reconciliação segura.
10. **ADVANCE:** exigir CP atual certificado e commit/push confirmados, próximo CP explicitamente autorizado em modo multi-checkpoint e nenhuma stop condition. No padrão, parar para humano. Ao esgotar a lista autorizada, executar gates finais/full/challenge aplicáveis antes do último commit, atualizar PR e parar para review humano, sem merge/auto-merge. Um range parcial exige congelar seus gates de handoff antes de começar; não autoriza checkpoints de fechamento fora do range.

## Identidade da evidência e recovery

Evitar a autorreferência impossível de gravar o SHA de um commit dentro dele mesmo. Antes de certificar, registrar `reviewed_head` (HEAD anterior ao commit) e `candidate_diff_sha256`: SHA-256 de `git diff --cached --binary <base_commit> -- .` (registrar opções/configuração de diff e usar as mesmas na retomada) excluindo somente a própria evidência e o state do item, com exclusões explícitas em `candidate_exclusions`. Exigir worktree igual ao index e ausência de arquivos novos não incluídos no candidato antes da certificação. Stagear e revisar todo o candidato; os dois arquivos excluídos também exigem revisão documental final e não podem carregar código/contratos novos. Qualquer mudança no restante invalida o digest e exige rever/reexecutar checks afetados.

O commit inclui evidência/state e trailer `Checkpoint-Evidence: <path>`. `certified_commit` fica null na própria evidência; resolver o SHA pelo commit com esse trailer, conferir digest do diff base→commit com as mesmas exclusões e resultados antes de aceitá-lo como recovery point. Persistir SHA literal no PR/handoff após commit/push e no state do próximo CP, sem amend para autoinscrição. O state identifica último CP, caminho da evidência, SHA anterior e referência ao commit que contém a certificação atual. Se a sessão cair entre commit/push/state, RECOVER verifica Git e remoto; não presume push ou repete implementação. Commit sem certificação válida, inclusive diagnóstico de blocker, nunca é recovery point.

## CP0: bootstrap da trusted execution boundary

CP0 começa em `UNTRUSTED BOOTSTRAP`. Não pode usar gates apenas especificados como prova. Antes de CP1, deve estabelecer os executores previstos pelo CP0, a checagem da certificação, oracles fundamentais e respectivos positivos/contracasos; demonstrar falsificações pela causa certa, restaurar todas as mutações, executar segundo GREEN e regressão, certificar e produzir commit saudável com push confirmado. Só então começa o loop confiável CP1+. A autorização multi-checkpoint não dispensa essa fronteira. Esta adaptação documental não implementa nem executa CP0.

## Stop conditions e interrupção

Decisão local reversível ou pequena incerteza não exige interrupção humana. Corrigir falhas dentro do contrato e repetir verificação; não avançar enquanto falham. Suspender avanço automático diante de blocker material:

- Upstream/source lock real diverge da baseline e exige reinterpretar spec ou premissa fundamental.
- Gate obrigatório não pode ser implementado/executado no escopo autorizado.
- Correção exige alterar must_not_change, capability fora do CP ou work item não autorizado.
- Contraexemplo exige redesenhar contrato/invariant já certificado; oracle errado não é licença para enfraquecê-lo.
- Git e último certificado não podem ser reconciliados sem operação destrutiva ou hipótese insegura.
- Dependência externa obrigatória indisponível impede a evidência exigida.
- Continuar exigiria inventar fato que deveria ser publicado upstream.

Preservar último checkpoint certificado; registrar blocker factual no state/evidência, pendências e próxima ação não iniciada. Restaurar mutações; se impossível, registrar `UNRESTORED` e impedir commit/push desse conteúdo. Pushar apenas mudanças coerentes já autorizadas e seguras, identificando diagnóstico não certificado quando for o caso; deixar PR recuperável e parar para humano. Nunca fabricar GREEN ou fallback heurístico para chegar ao último CP. State é memória curta, não transcript nem prova de CI/merge; certificação técnica não equivale à aceitação humana.

## Saída

Responder com objetivo atingido ou bloqueado, commit/PR, testes reais, findings, limitações e próximo checkpoint não iniciado. Atualizar índices conforme lifecycle. Não misturar “pronto para review”, “aprovado”, “concluído localmente” e “mergeado”.
