# BACKLOG-LOWER-009 — Fallback opaco com envelopes conservadores

**Estado:** `needs_discovery`. **Fase:** `capability`. **Autorização:** não concedida.

## Problema e objetivo observável

Permitir inventário maior sem perder construções observadas e sem transformar unknown em no-op.

## Escopo e estratégia

Definir escopos/envelopes de memória/controle/dependências, operand occurrences e policy de admissão para cada shape suportado.

## Dependências

BACKLOG-LOWER-001

## Aceitação

opaque válido com limites justificáveis e lacunas materiais; controle aberto não vira fallthrough e statements não desaparecem.

## Evals e garantias

EVAL-LWR-011, EVAL-LWR-012, EVAL-LWR-013, EVAL-LWR-014, EVAL-LWR-020, EVAL-LWR-023; ver [catálogo](../../evals/catalog.md). Aplicar os invariantes vinculados e o protocolo de fontes antes de algoritmo não trivial.

## Fora de escopo

Um catch-all que declara semântica precisa por observedKind; any_* sem argumento de abrangência/escopo.

## Checkpoints e handoff

Nenhum work item promovido. Ao promover, delimitar um resultado por checkpoint, escopo e testes. Não iniciar itens dependentes por terminar este. Documentar descoberta upstream na primeira fronteira afetada; não editar outro repo.

## Evidência atual

Planejamento documental somente. Sem commit de implementação, CI, benchmark ou aprovação remota atribuídos a este item.
