# BACKLOG-LOWER-004 — Primeira integração com analysis-cfg

**Estado:** `candidate`. **Fase:** `integration`. **Autorização:** não concedida.

## Problema e objetivo observável

Provar o primeiro E2E de Entry/GOBACK usando Publication compartilhada e saída de invocação no CFG.

## Escopo e estratégia

Integrador de teste fora do core, snapshots compatíveis, input real SP e comparação contra oracle de controle independente.

## Dependências

BACKLOG-LOWER-001 Requer também disponibilidade do consumer/contrato correspondente no analysis-cfg, verificada no momento da autorização.

## Aceitação

Output do lowerer admitido pelo CFG com correlação Entry/Return/saída e gaps; nenhuma dependência de produção core→CFG.

## Evals e garantias

EVAL-LWR-010, EVAL-LWR-013, EVAL-LWR-017, EVAL-LWR-019, EVAL-LWR-026; ver [catálogo](../../evals/catalog.md). Aplicar os invariantes vinculados e o protocolo de fontes antes de algoritmo não trivial.

## Fora de escopo

Desenvolver o CFG neste repo; esperar codec para testar composição em memória; anunciar linguagem completa.

## Checkpoints e handoff

Nenhum work item promovido. Ao promover, delimitar um resultado por checkpoint, escopo e testes. Não iniciar itens dependentes por terminar este. Documentar descoberta upstream na primeira fronteira afetada; não editar outro repo.

## Evidência atual

Planejamento documental somente. Sem commit de implementação, CI, benchmark ou aprovação remota atribuídos a este item.
