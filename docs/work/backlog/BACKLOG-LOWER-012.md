# BACKLOG-LOWER-012 — Outros terminais e transferências

**Estado:** `needs_discovery`. **Fase:** `capability`. **Autorização:** não concedida.

## Problema e objetivo observável

Explicitar requisitos de GO TO, EXIT PROGRAM, STOP RUN e outras saídas sem herdar a regra GOBACK.

## Escopo e estratégia

Discovery por família com semântica upstream e escopo AIR; separar subitens antes de implementar.

## Dependências

BACKLOG-LOWER-002

## Aceitação

Cada terminal conserva seu escopo e targets, com positivos/negativos de confusão Return/Halt/local return.

## Evals e garantias

EVAL-LWR-010, EVAL-LWR-011, EVAL-LWR-012, EVAL-LWR-020, EVAL-LWR-023; ver [catálogo](../../evals/catalog.md). Aplicar os invariantes vinculados e o protocolo de fontes antes de algoritmo não trivial.

## Fora de escopo

Normalizar todos os nomes de saída para Return por conveniência.

## Checkpoints e handoff

Nenhum work item promovido. Ao promover, delimitar um resultado por checkpoint, escopo e testes. Não iniciar itens dependentes por terminar este. Documentar descoberta upstream na primeira fronteira afetada; não editar outro repo.

## Evidência atual

Planejamento documental somente. Sem commit de implementação, CI, benchmark ou aprovação remota atribuídos a este item.
