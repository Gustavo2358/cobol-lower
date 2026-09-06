# BACKLOG-LOWER-006 — MOVE preciso e normalização de valores

**Estado:** `needs_discovery`. **Fase:** `capability`. **Autorização:** não concedida.

## Problema e objetivo observável

Preservar cópia/definição quando o frontend sustentar domínio, destino e conversão necessários.

## Escopo e estratégia

Identificar enrichment upstream para literal, endereço, conversão/padding/truncamento e condições de sameDomain; propor o menor slice demonstrável.

## Dependências

BACKLOG-LOWER-005

## Aceitação

assign ou normalização explícita semanticamente justificada; contracasos de kind desconhecido, conversão e storage; nenhum literal tipado por adivinhação.

## Evals e garantias

EVAL-LWR-014, EVAL-LWR-019, EVAL-LWR-020, EVAL-LWR-023; ver [catálogo](../../evals/catalog.md). Aplicar os invariantes vinculados e o protocolo de fontes antes de algoritmo não trivial.

## Fora de escopo

RD/PV ou reinterpretar sintaxe COBOL para compensar falta de contrato.

## Checkpoints e handoff

Nenhum work item promovido. Ao promover, delimitar um resultado por checkpoint, escopo e testes. Não iniciar itens dependentes por terminar este. Documentar descoberta upstream na primeira fronteira afetada; não editar outro repo.

## Evidência atual

Planejamento documental somente. Sem commit de implementação, CI, benchmark ou aprovação remota atribuídos a este item.
