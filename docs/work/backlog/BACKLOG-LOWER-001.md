# BACKLOG-LOWER-001 — Primeiro slice Entry/GOBACK → AIR Return

**Estado:** `in_progress`. **Fase:** `foundation`. **Autorização:** CP0..CP5, multi-checkpoint, pedido explícito do usuário em 2026-09-06 registrado no work item.

## Problema e objetivo observável

Criar o primeiro caminho SP JSON 1.1.0 → porta tipada → Publication AIR validada, sem JSON no núcleo.

## Escopo e estratégia

Bootstrap Java21/Maven reprodutível com air-java fixado, gates locais, fixture AIR manual, golden SP real, decoder, input validator, admissão e regra GOBACK.

## Dependências

Nenhuma dependência de implementação anterior; CP0 estabelece a fronteira confiável exigida para CP1..CP5.

## Aceitação

Objeto AIR no shape mínimo, coverage parcial preservada, equivalência arquivo/memória, negativos e falsificações; primeiro full real com testes não zero.

## Evals e garantias

EVAL-LWR-001, EVAL-LWR-002, EVAL-LWR-003, EVAL-LWR-004, EVAL-LWR-005, EVAL-LWR-006, EVAL-LWR-007, EVAL-LWR-008, EVAL-LWR-009, EVAL-LWR-010, EVAL-LWR-011, EVAL-LWR-012, EVAL-LWR-013, EVAL-LWR-014, EVAL-LWR-015, EVAL-LWR-016, EVAL-LWR-017, EVAL-LWR-018, EVAL-LWR-019, EVAL-LWR-020, EVAL-LWR-021, EVAL-LWR-022, EVAL-LWR-024; ver [catálogo](../../evals/catalog.md). Aplicar os invariantes vinculados e o protocolo de fontes antes de algoritmo não trivial.

## Fora de escopo

MOVE/IF/CALL/DATA/storage/opaque amplo/CFG/writer AIR e qualquer atualização upstream.

## Checkpoints e handoff

[WORK-LOWER-001 ativo](../active/WORK-LOWER-001/spec.md) delimita um resultado por checkpoint, escopo e testes. Não iniciar itens dependentes por terminar este. Documentar descoberta upstream na primeira fronteira afetada; não editar outro repo.

## Evidência atual

CP0 em revisão/certificação. Build, smoke AIR e gates do bootstrap têm execução local; nenhum commit certificado, CI, benchmark ou aprovação remota ainda. Estado factual no pacote ativo.
