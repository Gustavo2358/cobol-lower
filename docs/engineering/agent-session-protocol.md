# Protocolo de sessão do agente

## Entrada

Identificar tarefa autorizada, checkpoint, branch/PR, source lock e working tree. Ler AGENTS e índice de trabalho, depois manifesto e state do item. Se não houver autorização de implementação, trabalhar apenas no escopo documental pedido. Um backlog pronto não basta.

Carregar somente contratos, ADRs, invariantes e evals da tarefa. Consultar fonte externa quando uma regra ou algoritmo não trivial precisar de fundamentação; registrar referência/revisão e evidência. A memória do modelo não é autoridade.

## Contexto de execução

Separar fatos confirmados, hipóteses, limites e decisões ainda pendentes. Antes de editar, verificar se o snapshot upstream mudou; não atualizar o lock automaticamente. Ler diff e estado existente para não repetir trabalho ou sobrescrever mudanças de outra sessão.

Um plano local curto identifica resultado observável, arquivos em escopo, teste focal e gates. Não transformar esse plano em uma segunda especificação. Se o escopo for maior que o checkpoint, registrar proposta de fatiamento e continuar apenas a parte autorizada que permaneça segura.

## Loop de implementação

Regra/oracle → RED pelo motivo esperado → menor implementação geral → GREEN → revisão adversarial/falsificação → restauração → segundo GREEN. O loop não pede cadeia de pensamento privada; pede artefatos verificáveis: decisão, teste, diff, log, resultado e limites.

Não parar no primeiro teste feliz. Não repetir tentativas sem hipótese nova. Depois de uma falha, classificar se o defeito está no input, teste, algoritmo, contrato upstream, setup ou escopo. Uma falha de compilação pode ser RED para API ausente; não prova que o oracle mata uma implementação semanticamente errada.

## Interrupção e retomada

Antes de parar, state curto com o que está pronto, comandos, testes pendentes e próximos arquivos. Mutações temporárias devem ser restauradas; se não puder, declarar o estado e impedir commit/publicação. Não chamar branch suja de pronta.

Ao retomar, confrontar state com arquivos/Git/metadata confiável. O state é memória de execução, não prova de CI ou merge. O review humano decide a aceitação do checkpoint; autorização explícita pode permitir vários checkpoints sequenciais, mas o agente não supõe isso.

## Saída

Responder com objetivo atingido ou bloqueado, commit/PR, testes reais, findings, limitações e próximo checkpoint não iniciado. Atualizar índices conforme lifecycle. Não misturar “pronto para review”, “aprovado”, “concluído localmente” e “mergeado”.
