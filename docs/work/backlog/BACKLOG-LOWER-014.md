# BACKLOG-LOWER-014 — Hardening de escala e limites

**Estado:** `candidate`. **Fase:** `hardening`. **Autorização:** não concedida.

## Problema e objetivo observável

Ampliar evidência de throughput/memória conforme capacidades reais crescem.

## Escopo e estratégia

Perfis de carga sintéticos e inputs autorizados, contadores, medição e otimização geral com oracle preservado.

## Dependências

BACKLOG-LOWER-001

## Aceitação

Sem O(N²) oculto, sem truncamento, resultados determinísticos e diagnóstico de limites; números de máquina contextualizados.

## Evals e garantias

EVAL-LWR-016, EVAL-LWR-018, EVAL-LWR-021, EVAL-LWR-022; ver [catálogo](../../evals/catalog.md). Aplicar os invariantes vinculados e o protocolo de fontes antes de algoritmo não trivial.

## Fora de escopo

Otimização especulativa que reduz precisão ou metas de hardware sem medição.

## Checkpoints e handoff

Nenhum work item promovido. Ao promover, delimitar um resultado por checkpoint, escopo e testes. Não iniciar itens dependentes por terminar este. Documentar descoberta upstream na primeira fronteira afetada; não editar outro repo.

## Evidência atual

Planejamento documental somente. Sem commit de implementação, CI, benchmark ou aprovação remota atribuídos a este item.
