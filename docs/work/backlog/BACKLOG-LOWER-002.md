# BACKLOG-LOWER-002 — Sequenciamento e inventários executáveis maiores

**Estado:** `needs_discovery`. **Fase:** `capability`. **Autorização:** não concedida.

## Problema e objetivo observável

Expandir além do shape mínimo sem usar ordem de statements como semântica executável nem apagar conteúdo não alcançável.

## Escopo e estratégia

Discovery bilateral dos fatos de controle/start/continuação publicados e dos terminadores AIR necessários; depois propor slice específico.

## Dependências

BACKLOG-LOWER-001

## Aceitação

Oracles com múltiplos statements, GOBACK sem successor, conteúdo posterior preservado e cardinalidade plural; ausência de fato vira prerequisite upstream.

## Evals e garantias

EVAL-LWR-008, EVAL-LWR-011, EVAL-LWR-012, EVAL-LWR-020, EVAL-LWR-021, EVAL-LWR-023; ver [catálogo](../../evals/catalog.md). Aplicar os invariantes vinculados e o protocolo de fontes antes de algoritmo não trivial.

## Fora de escopo

CFG builder, reachability calculada para eliminar fatos e inferência por ProgramPoint.

## Checkpoints e handoff

Nenhum work item promovido. Ao promover, delimitar um resultado por checkpoint, escopo e testes. Não iniciar itens dependentes por terminar este. Documentar descoberta upstream na primeira fronteira afetada; não editar outro repo.

## Evidência atual

Planejamento documental somente. Sem commit de implementação, CI, benchmark ou aprovação remota atribuídos a este item.
