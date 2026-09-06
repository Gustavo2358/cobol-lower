# BACKLOG-LOWER-015 — Adapter de integração em memória e composição Maven

**Estado:** `candidate`. **Fase:** `integration`. **Autorização:** não concedida.

## Problema e objetivo observável

Substituir transporte sem mudar regras/porta, mantendo repositórios independentes.

## Escopo e estratégia

Adapter público upstream→SemanticProductInput e integrador Maven; JDK/API/artifact versions explícitos; saída entrega Publication diretamente.

## Dependências

BACKLOG-LOWER-001

## Aceitação

Equivalência com caminho em arquivo, core sem dependência de frontend/CFG, sem serialização intermediária forçada.

## Evals e garantias

EVAL-LWR-003, EVAL-LWR-004, EVAL-LWR-016, EVAL-LWR-017, EVAL-LWR-018, EVAL-LWR-026; ver [catálogo](../../evals/catalog.md). Aplicar os invariantes vinculados e o protocolo de fontes antes de algoritmo não trivial.

## Fora de escopo

Fusão obrigatória de repositórios, reflection framework ou uso de internals do proleap.

## Checkpoints e handoff

Nenhum work item promovido. Ao promover, delimitar um resultado por checkpoint, escopo e testes. Não iniciar itens dependentes por terminar este. Documentar descoberta upstream na primeira fronteira afetada; não editar outro repo.

## Evidência atual

Planejamento documental somente. Sem commit de implementação, CI, benchmark ou aprovação remota atribuídos a este item.
