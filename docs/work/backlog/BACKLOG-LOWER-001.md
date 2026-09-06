# BACKLOG-LOWER-001 — Primeiro slice Entry/GOBACK → AIR Return

**Estado:** `ready_for_authorization`. **Fase:** `foundation`. **Autorização:** não concedida.

## Problema e objetivo observável

Criar o primeiro caminho SP JSON 1.1.0 → porta tipada → Publication AIR validada, sem JSON no núcleo.

## Escopo e estratégia

Bootstrap Java21/Maven reprodutível com air-java fixado, gates locais, fixture AIR manual, golden SP real, decoder, input validator, admissão e regra GOBACK.

## Dependências

Nenhuma dependência de implementação; ainda exige autorização explícita.

## Aceitação

Objeto AIR no shape mínimo, coverage parcial preservada, equivalência arquivo/memória, negativos e falsificações; primeiro full real com testes não zero.

## Evals e garantias

EVAL-LWR-001, EVAL-LWR-002, EVAL-LWR-003, EVAL-LWR-004, EVAL-LWR-005, EVAL-LWR-006, EVAL-LWR-007, EVAL-LWR-008, EVAL-LWR-009, EVAL-LWR-010, EVAL-LWR-011, EVAL-LWR-012, EVAL-LWR-013, EVAL-LWR-014, EVAL-LWR-015, EVAL-LWR-016, EVAL-LWR-017, EVAL-LWR-018, EVAL-LWR-019, EVAL-LWR-020, EVAL-LWR-021, EVAL-LWR-022, EVAL-LWR-024; ver [catálogo](../../evals/catalog.md). Aplicar os invariantes vinculados e o protocolo de fontes antes de algoritmo não trivial.

## Fora de escopo

MOVE/IF/CALL/DATA/storage/opaque amplo/CFG/writer AIR e qualquer atualização upstream.

## Checkpoints e handoff

[Proposta WORK-LOWER-001](../proposals/WORK-LOWER-001/spec.md) Ao promover, delimitar um resultado por checkpoint, escopo e testes. Não iniciar itens dependentes por terminar este. Documentar descoberta upstream na primeira fronteira afetada; não editar outro repo.

## Evidência atual

Planejamento documental somente. Sem commit de implementação, CI, benchmark ou aprovação remota atribuídos a este item.
