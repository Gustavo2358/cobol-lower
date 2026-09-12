# BACKLOG-LOWER-008 — CALL e contrato por site de interação

**Estado:** `completed`. **Fase:** `capability`. **Autorização:** implementação
da primeira slice CP6 W1C em [WORK-LOWER-010](../history/WORK-LOWER-010.md).

## Problema e objetivo observável

Representar interação sem confundir binding de variável com destino de programa ou assinatura desconhecida com vazia.

## Escopo e estratégia

Confrontar target interpretation, argumentos, signature, effects/outcomes e captura anterior exigidos pela AIR com capabilities SP.

## Dependências

BACKLOG-LOWER-005

## Aceitação

invoke ou fallback explicitamente sustentado; unknowns por site, retorno normal/exceção separados e conflitos não reparados.

## Evals e garantias

EVAL-LWR-007, EVAL-LWR-009, EVAL-LWR-014, EVAL-LWR-019, EVAL-LWR-020, EVAL-LWR-023; ver [catálogo](../../evals/catalog.md). Aplicar os invariantes vinculados e o protocolo de fontes antes de algoritmo não trivial.

## Fora de escopo

Resolver nomes dinâmicos, calcular resumo de efeitos ou inventar contrato de chamada.

## Checkpoints e handoff

WORK-LOWER-010/CP0 promove apenas CALL literal/DATA sem argumentos/results/handlers
com continuação normal para GOBACK. Não iniciar itens dependentes por terminar
este. W1D/W2 não autorizados; irmãos somente read-only.

## Evidência atual

[Evidência W1C](../../quality/WORK-LOWER-010/CP0.json) e
[guia de RED/GREEN/E2E](../../quality/WORK-LOWER-010/evidence-guide.md).
PR #11 mergeada; review formal não registrado na API. Este fechamento não certifica CALL geral,
resolução dinâmica ou contrato externo conhecido.
