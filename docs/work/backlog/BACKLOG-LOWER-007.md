# BACKLOG-LOWER-007 — IF e controle condicional sustentado

**Estado:** `completed`. **Fase:** `capability`. **Autorização:** CP6 W2B simple IF only; product delivered, administrative archival blocked.

## Problema e objetivo observável

Preservar branches e junções com predicado/avaliação que satisfaçam AIR.

## Escopo e estratégia

Auditar facts de estrutura, pureza/totalidade e resultado booleano; decidir branch ou abstração permitida sem inventar predicate.

## Dependências

BACKLOG-LOWER-002

## Aceitação

Oracle de diamond/nested branches/saída sem continuação, reads preservados e rejeição quando falta garantia de avaliação.

## Evals e garantias

EVAL-LWR-008, EVAL-LWR-011, EVAL-LWR-014, EVAL-LWR-020, EVAL-LWR-023; ver [catálogo](../../evals/catalog.md). Aplicar os invariantes vinculados e o protocolo de fontes antes de algoritmo não trivial.

## Fora de escopo

Predicate parser, CFG builder e leitura de texto de condição.

## Checkpoints e handoff

WORK-LOWER-011 implemented and qualified the explicitly bounded simple IF slice. PR12 is merged; the legacy historical checker blocks documentary archival. Do not initiate dependent work or edit sibling repositories.

## Evidência atual

Product merge `2b7fa3a5cee865eef5007d6d870618e032047e1e` is frozen. Local qualification belongs only to `f588704c5a958af93562fe10e5e6646f5cb6a17e`; remote merge FAST passed. Status remains `completed` solely because WORK-LOWER-011 administrative archival is blocked. General/nested control is not delivered. [Closeout and blocker](../../quality/WORK-LOWER-011/closeout.md).

CP6 W2B autorizado em [WORK-LOWER-011](../history/WORK-LOWER-011/work-item.yaml).
