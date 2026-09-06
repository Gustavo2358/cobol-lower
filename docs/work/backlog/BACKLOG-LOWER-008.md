# BACKLOG-LOWER-008 — CALL e contrato por site de interação

**Estado:** `needs_discovery`. **Fase:** `capability`. **Autorização:** não concedida.

## Problema e objetivo observável

Representar interação sem confundir binding de variável com destino de programa ou assinatura desconhecida com vazia.

## Escopo e estratégia

Confrontar target interpretation, argumentos, signature, effects/outcomes e captura anterior exigidos pela AIR com capabilities SP.

## Dependências

BACKLOG-LOWER-005

## Aceitação

invoke ou fallback explicitamente sustentado; unknowns por site, retorno normal/exceção separados e conflitos não reparados.

## Evals e garantias

EVAL-LWR-007, EVAL-LWR-009, EVAL-LWR-014, EVAL-LWR-019, EVAL-LWR-020, EVAL-LWR-023; ver [catálogo](../../evals/catalog.md). Aplicar os invariantes vinculados e o protocolo de fontes antes de algoritmo não trivial.

## Fora de escopo

Resolver nomes dinâmicos, calcular resumo de efeitos ou inventar contrato de chamada.

## Checkpoints e handoff

Nenhum work item promovido. Ao promover, delimitar um resultado por checkpoint, escopo e testes. Não iniciar itens dependentes por terminar este. Documentar descoberta upstream na primeira fronteira afetada; não editar outro repo.

## Evidência atual

Planejamento documental somente. Sem commit de implementação, CI, benchmark ou aprovação remota atribuídos a este item.
