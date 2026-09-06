# BACKLOG-LOWER-010 — PERFORM e controle local

**Estado:** `needs_discovery`. **Fase:** `capability`. **Autorização:** não concedida.

## Problema e objetivo observável

Preparar tradução canônica do controle local COBOL quando os facts upstream existirem.

## Escopo e estratégia

Pesquisa de contrato AIR control.local, fatos PERFORM/retorno publicados e oracles de aninhamento/retorno por contexto.

## Dependências

BACKLOG-LOWER-002

## Aceitação

Mapeamento com entrada/resume/completion e fallback autorizado, sem substituir PERFORM por GOTO sem retorno.

## Evals e garantias

EVAL-LWR-011, EVAL-LWR-014, EVAL-LWR-020, EVAL-LWR-023; ver [catálogo](../../evals/catalog.md). Aplicar os invariantes vinculados e o protocolo de fontes antes de algoritmo não trivial.

## Fora de escopo

Implementar controle local ou stack do CFG antes da autorização de slice e do enrichment SP.

## Checkpoints e handoff

Nenhum work item promovido. Ao promover, delimitar um resultado por checkpoint, escopo e testes. Não iniciar itens dependentes por terminar este. Documentar descoberta upstream na primeira fronteira afetada; não editar outro repo.

## Evidência atual

Planejamento documental somente. Sem commit de implementação, CI, benchmark ou aprovação remota atribuídos a este item.
