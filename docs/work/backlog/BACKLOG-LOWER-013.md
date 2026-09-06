# BACKLOG-LOWER-013 — Storage declarativo preciso e aliases

**Estado:** `needs_discovery`. **Fase:** `capability`. **Autorização:** não concedida.

## Problema e objetivo observável

Baixar fatos físicos publicados para AIR sem confundir nomes com memória.

## Escopo e estratégia

REDEFINES/RENAMES/groups/ref-mod, bases/extent/codec e disjoint_storage somente sob garantias explícitas do produtor.

## Dependências

BACKLOG-LOWER-005

## Aceitação

Overlaps, subintervalos e aliases preservados; premissas rastreáveis; ausência de layout continua incerta.

## Evals e garantias

EVAL-LWR-007, EVAL-LWR-014, EVAL-LWR-015, EVAL-LWR-020, EVAL-LWR-023; ver [catálogo](../../evals/catalog.md). Aplicar os invariantes vinculados e o protocolo de fontes antes de algoritmo não trivial.

## Fora de escopo

Calcular storage analysis a partir de source ou strong/weak updates de dataflow.

## Checkpoints e handoff

Nenhum work item promovido. Ao promover, delimitar um resultado por checkpoint, escopo e testes. Não iniciar itens dependentes por terminar este. Documentar descoberta upstream na primeira fronteira afetada; não editar outro repo.

## Evidência atual

Planejamento documental somente. Sem commit de implementação, CI, benchmark ou aprovação remota atribuídos a este item.
