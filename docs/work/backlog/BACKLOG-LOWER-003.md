# BACKLOG-LOWER-003 — Adapter AIR JSON de saída e prova interoperável

**Estado:** `completed`. **Fase:** `transport`. **Autorização:** WORK-LOWER-003 CP0, somente 2A; mesmo codec compartilhado, sem prova cross-codec.

## Problema e objetivo observável

Gerar arquivo AIR por adapter externo sem mudar a porta/core ou criar schema proprietário.

## Escopo e estratégia

Fixar revisão do binding DRAFT/accepted, implementar writer e reader/oracle independente de teste; formalizar interoperabilidade com o CFG no trabalho apropriado.

## Dependências

BACKLOG-LOWER-001

## Aceitação

Round-trip semântico, determinismo contratado, leitura independente, versões/unknowns/provenance íntegros; contribuição de evidência para promoção do binding.

## Evals e garantias

EVAL-LWR-003, EVAL-LWR-017, EVAL-LWR-018, EVAL-LWR-025; ver [catálogo](../../evals/catalog.md). Aplicar os invariantes vinculados e o protocolo de fontes antes de algoritmo não trivial.

## Fora de escopo

JSON em air-java, annotations no domínio, alterar norma AIR ou exigir accepted antes dos experimentos autorizados.

## Checkpoints e handoff

[WORK-LOWER-003](../history/WORK-LOWER-003.md) promovido para 2A. Ao promover, delimitar um resultado por checkpoint, escopo e testes. Não iniciar itens dependentes por terminar este. Documentar descoberta upstream na primeira fronteira afetada; não editar outro repo.

## Evidência atual

2A implementado localmente em WORK-LOWER-003 CP0; evidência, certificação e PR/CI no state do item. Reader independente e integração CFG continuam futuros.

2A encerrado pelo merge real de PR #4; integração cross-repo permanece fora desta entrega.
