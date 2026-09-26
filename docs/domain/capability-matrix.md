# Matriz bilateral e rota de evolução

**Status:** Entry/GOBACK e DATA/MOVE escalar implementados; CALL W1C merged/closed; IF simples W2B implementado em WORK-LOWER-011, qualificado localmente e aguardando review humano. Demais capacidades são plano, não autorização. A matriz deve evoluir por evidência dos snapshots, não por expectativa de prazo. Cada linha exige simultaneamente fatos upstream e pré-condições target.

| Capability | Entrada indispensável | Target/objetivo | Limite atual / próximo pré-requisito |
| --- | --- | --- | --- |
| Entry/GOBACK mínimo | Primary/start/assinatura zero conhecidos e saída da invocação | Entry + Sequence + Return | Implementado/testado, review humano final pendente; inventário alternativo aberto |
| Sequenciamento explícito | Fatos executáveis, containment/continuation suficientes | Labels e terminadores coerentes | Não inferir por ProgramPoint ou ordem de arrays |
| DATA nominal | Identidade/visibilidade/provenance e desconhecimentos explícitos | Object com TypeRef/storage compatíveis | Não inventar célula independente por DataItemId |
| MOVE preciso | Valor/domínio, endereço/destino, conversão e cópia provados | assign/normalização explícita | LiteralKind sozinho não resolve padding, alias ou conversão |
| IF | Avaliação booleana/pura/total ou abstração admitida; destinos provados | branch ou fallback autorizado | ConditionSurface e reads nominais não certificam predicate |
| CALL | Interpretação de target, avaliação, assinatura, effects/outcomes | invoke ou fallback autorizado | Binding nominal não é target runtime; runtime unknown permanece |
| Outros observados | Existência, origem e envelopes conservadores justificáveis | opaque, nunca nop | Fora do primeiro slice; não adotar any_* sem avaliar escopo/semântica |
| PERFORM/controle local | Entrada, retorno local, limites e contexto publicados | Extensão AIR de controle local | Requer enrichment e oracles de retorno, não GOTO aproximado |
| EVALUATE/SEARCH | Seletores, prioridade e controle publicados | Testes ordenados/dispatch quando legal | Não perder sobreposição/prioridade ou inferir de texto |
| GO TO/terminais | Targets/saída explícitos por família | jump/return/halt conforme regra | STOP RUN e EXIT PROGRAM não herdam regra GOBACK |
| Storage preciso | Bases, extensões, aliases, codec e layout conhecidos | Células/regiões/views/premissas | REDEFINES/RENAMES/groups/ref-mod exigem fatos sustentados |
| Fatos de dependência | AIR e análises derivadas necessárias | Produto externo | Não pertence ao lowerer nem se deriva de strings por conveniência |

## Regra de promoção

Preencher: fonte e versão; formas aceitas; formas bloqueadas; observação do consumidor; mapeamento; incertezas; argumento de preservação; impacto na porta; fixtures e anti-exemplos. A promoção é por work item/review e atualiza [backlog](../work/backlog.md), invariantes e evals.

Uma nova variante deve ter tratamento explícito também no input adapter e no dispatch do core. Família desconhecida falha de modo claro. Não preservar semântica legada incorreta por compatibilidade de API.

## O que não foi decidido

Não foi congelado um planner universal de CFG, SSA, basic blocks máximos ou um lowering de uma Sequence por statement para toda a linguagem. Esses desenhos dependem dos fatos executáveis disponíveis e dos contratos de consumidores; devem ser analisados no slice correspondente.

## Checkpoint 4C

[scalar-text-move@1](scalar-text-move.md) acrescenta DATA escalar e MOVE FULL_IDENTITY
com cadeia explícita até GOBACK. Não habilita MOVE geral, IF ou CALL.

## CP6 W1C

[CALL literal/DATA](call-lowering.md) acrescenta Invoke neutro, continuação normal
explícita para GOBACK e consumo FITTED_TEXT publicado. Runtime target/name policy,
efeitos e saídas não normais permanecem abertos. Não promove IF, argumentos,
handlers, CFG ou resolução de dependências.

## CP6 W2B

[IF simples](simple-if-diamond.md) acrescenta diamond fechado/aberto com Unknown BOOL
e DisjointStorage da prova W2A. Predicado não avaliado; obrigações e PARTIAL mantidos.
Não habilita IF geral/nested, path pruning, CFG, solver/lattice ou W2D.

SP 2.0: [EVALUATE first slice](evaluate-first-slice.md) implements one simple DATA
subject, ordered text literals, OTHER and explicit completion. Generic Branch
uses Unknown BOOL; unsupported variants retain Opaque. No numeric/conversion or
path-pruning capability is claimed.

## Campanha Storage Semantics ST-W0–ST-W5

[Contrato regional](regional-storage.md): SP 2.7 → Region/View/bytes, com
validação de fechamento no core e preservação de leitores históricos. Tradução
de byte Assign, CopyBytes, HavocMust e CALL como Read de View implementada e
coberta por oracles de arquivo/memória; M1 ainda exige values/dependências e
o vertical group→child→CALL com seus gates cumulativos.

## R7-R4 assessment boundary

SP2.40/2.41/2.42 admission is retained. CICS_HANDLER and CICS_ABEND remain executable NOT_READY. [Local abstract state and dispatch assessment](cics-handler-state-r7-r4.md) are available through Admission.handlerState after factual validation. AIR and CFG are unchanged; runtime enclosing levels and executable dispatch are unmodeled.

R7-R7B: SP2.44 SEND_TERMINAL and structural LENGTH are admitted/validated. Executable SEND remains NOT_READY; independent bounded publication survives. See [contract](terminal-send-r7-r7b.md).
