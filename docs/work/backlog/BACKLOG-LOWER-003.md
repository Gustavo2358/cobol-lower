# BACKLOG-LOWER-003 — Adapter AIR JSON de saída e prova interoperável

**Estado:** `candidate`. **Fase:** `transport`. **Autorização:** não concedida.

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

Nenhum work item promovido. Ao promover, delimitar um resultado por checkpoint, escopo e testes. Não iniciar itens dependentes por terminar este. Documentar descoberta upstream na primeira fronteira afetada; não editar outro repo.

## Evidência atual

Planejamento documental somente. Sem commit de implementação, CI, benchmark ou aprovação remota atribuídos a este item.
