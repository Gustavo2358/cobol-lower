# BACKLOG-LOWER-011 — EVALUATE/SEARCH e seleção

**Estado:** `needs_discovery`. **Fase:** `capability`. **Autorização:** não concedida.

## Problema e objetivo observável

Delimitar traduções de seleção sem perder prioridades, sobreposição e reads.

## Escopo e estratégia

Discovery de facts de seleção e legalidade de dispatch versus testes ordenados; dividir EVALUATE/SEARCH em work items quando houver contratos.

## Dependências

BACKLOG-LOWER-002

## Aceitação

Contraexemplos para predicados sobrepostos, OTHER/default e caminhos sem continuação; nenhuma regra por string.

## Evals e garantias

EVAL-LWR-011, EVAL-LWR-014, EVAL-LWR-020, EVAL-LWR-023; ver [catálogo](../../evals/catalog.md). Aplicar os invariantes vinculados e o protocolo de fontes antes de algoritmo não trivial.

## Fora de escopo

Supor que toda seleção é dispatch disjunto; implementações horizontais simultâneas.

## Checkpoints e handoff

Nenhum work item promovido. Ao promover, delimitar um resultado por checkpoint, escopo e testes. Não iniciar itens dependentes por terminar este. Documentar descoberta upstream na primeira fronteira afetada; não editar outro repo.

## Evidência atual

Planejamento documental somente. Sem commit de implementação, CI, benchmark ou aprovação remota atribuídos a este item.
