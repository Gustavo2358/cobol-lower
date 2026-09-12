# BACKLOG-LOWER-007 — IF e controle condicional sustentado

**Estado:** `in_progress`. **Fase:** `capability`. **Autorização:** não concedida.

## Problema e objetivo observável

Preservar branches e junções com predicado/avaliação que satisfaçam AIR.

## Escopo e estratégia

Auditar facts de estrutura, pureza/totalidade e resultado booleano; decidir branch ou abstração permitida sem inventar predicate.

## Dependências

BACKLOG-LOWER-002

## Aceitação

Oracle de diamond/nested branches/saída sem continuação, reads preservados e rejeição quando falta garantia de avaliação.

## Evals e garantias

EVAL-LWR-008, EVAL-LWR-011, EVAL-LWR-014, EVAL-LWR-020, EVAL-LWR-023; ver [catálogo](../../evals/catalog.md). Aplicar os invariantes vinculados e o protocolo de fontes antes de algoritmo não trivial.

## Fora de escopo

Predicate parser, CFG builder e leitura de texto de condição.

## Checkpoints e handoff

Nenhum work item promovido. Ao promover, delimitar um resultado por checkpoint, escopo e testes. Não iniciar itens dependentes por terminar este. Documentar descoberta upstream na primeira fronteira afetada; não editar outro repo.

## Evidência atual

Planejamento documental somente. Sem commit de implementação, CI, benchmark ou aprovação remota atribuídos a este item.

CP6 W2B autorizado em [WORK-LOWER-011](../active/WORK-LOWER-011/work-item.yaml).
