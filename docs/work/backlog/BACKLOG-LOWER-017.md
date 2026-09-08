# BACKLOG-LOWER-017 — Scale AIR JSON transport beyond current 16 MiB operational boundary

**Estado:** `planned` / PLANNED / NOT STARTED. **Autorização:** NOT AUTHORIZED.
**work_item:** null. Não promovido; discovery e implementação não iniciados.

## Problema

AirJson default = 16 MiB/depth128. 4B/4C demonstram que milhares de Assigns podem exceder esse limite com algoritmo linear.

## Escopo futuro a investigar e decidir

Política de maximumDocumentBytes; configuração por consumer/profile; eventual streaming encode/decode; compatibilidade da API AirJson; atomic file output; taxonomy de limites; escala N/2N e peak heap; integração cobol-lower/analysis-cfg.

## Owner arquitetural

Principalmente air-java/air-json; impacto nos consumers cobol-lower e analysis-cfg.

## Trigger de prioridade

Tratar antes de afirmar suporte E2E a grandes programas que produzam AIR >16 MiB.

## Limite desta inscrição

Somente planejamento pedido no review do WORK-LOWER-006/PR7. Nenhum repositório
irmão modificado. D1 trata capacidade/teto operacional; D2 trata consumo de heap
e representações duplicadas. Não combinar automaticamente essas decisões.
NOT IMPLEMENTED IN 4C. NOT A CLAIM OF LARGE-PROGRAM E2E READINESS.

[Handoff e medições 4C](../../quality/WORK-LOWER-006/handoff.md).
