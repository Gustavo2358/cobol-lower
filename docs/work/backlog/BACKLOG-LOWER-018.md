# BACKLOG-LOWER-018 — Reduce peak-memory amplification across SP and AIR transport

**Estado:** `planned` / PLANNED / NOT STARTED. **Autorização:** NOT AUTHORIZED.
**work_item:** null. Não promovido; discovery e implementação não iniciados.

## Problema

SP pode manter file bytes/byte[], JsonNode, Wire DTO e SpInput; AIR encode pode manter Publication, Json.Value e byte[]. São representações integrais O(N) coexistindo na heap, não evidência de O(N²).

## Escopo futuro a investigar e decidir

Medir peak heap e fatores de amplificação em corpora grandes; avaliar streaming físico e remoção de árvores intermediárias; manter duplicate/UTF-8/shape validation estrita, determinismo/canonical bytes, limites/taxonomy e desempenho.

## Owner arquitetural

Transporte SP em cobol-lower e AIR em air-java/air-json; coordenação de consumers quando houver descoberta autorizada.

## Trigger de prioridade

Avaliar antes de prometer capacidade de heap para corpora grandes; não pressupõe nem resolve o teto operacional de D1.

## Limite desta inscrição

Somente planejamento pedido no review do WORK-LOWER-006/PR7. Nenhum repositório
irmão modificado. D1 trata capacidade/teto operacional; D2 trata consumo de heap
e representações duplicadas. Não combinar automaticamente essas decisões.
NOT IMPLEMENTED IN 4C. NOT A CLAIM OF LARGE-PROGRAM E2E READINESS.

[Handoff e medições 4C](../../quality/WORK-LOWER-006/handoff.md).
