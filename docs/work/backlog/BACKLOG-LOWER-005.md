# BACKLOG-LOWER-005 — DATA nominal e tipos/storage explicitamente desconhecidos

**Estado:** `needs_discovery`. **Fase:** `capability`. **Autorização:** não concedida.

## Problema e objetivo observável

Representar declarações observadas sem afirmar identidade/independência física inexistentes.

## Escopo e estratégia

Confrontar declaração/visibilidade/TypeRef/storage desconhecido da AIR com fatos SP e atualizar input/rules sem criar segunda IR.

## Dependências

BACKLOG-LOWER-001

## Aceitação

Objetos inventariados com namespace, provenance e lacunas; ausência de layout não vira célula independente; AIR válida no perfil declarado.

## Evals e garantias

EVAL-LWR-007, EVAL-LWR-013, EVAL-LWR-014, EVAL-LWR-015, EVAL-LWR-020, EVAL-LWR-023; ver [catálogo](../../evals/catalog.md). Aplicar os invariantes vinculados e o protocolo de fontes antes de algoritmo não trivial.

## Fora de escopo

Interpretar PIC/rawLexeme, calcular aliases, values ou semântica completa de DATA.

## Checkpoints e handoff

Nenhum work item promovido. Ao promover, delimitar um resultado por checkpoint, escopo e testes. Não iniciar itens dependentes por terminar este. Documentar descoberta upstream na primeira fronteira afetada; não editar outro repo.

## Evidência atual

Planejamento documental somente. Sem commit de implementação, CI, benchmark ou aprovação remota atribuídos a este item.
