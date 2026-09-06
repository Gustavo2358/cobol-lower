# WORK-LOWER-001 — Especificação preparada

**Estado:** ready_for_authorization. Não iniciou implementação. A autorização atual cobre somente preparar o harness.

## Problema

Frontend já publica Entry/start/GOBACK; existe AIR normativa e modelo Java compartilhado. Falta provar a tradução por uma aplicação independente cujo núcleo não conheça os transportes.

## Objetivo

Consumir golden SP 1.1.0 real e produzir a Publication mínima Entry/Sequence/Return usando apenas contrato público e air-java. Provar o mesmo resultado por input tipado em memória. Detalhe canônico: [primeiro slice](../../../domain/first-slice-entry-goback.md).

## Domínio de entrada suportado

Perfil local minimal-entry-goback@1: unit selecionada, entry PRIMARY/start conhecidos, assinatura KNOWN/count0/ABSENT, GOBACK único tipado com saída da invocação e sem continuação, sem DATA/outros statements omitidos. Entry inventory PARTIAL legítimo e gap alternativo permanecem. Nome/arquivo/handles não restringem a regra à fixture AIR-FIRST.

## Classes semânticas

Positivo mínimo, versão não suportada, JSON/forma inválidos, input semanticamente contraditório, start ausente, assinatura parcial/desconhecida, variante/shape fora do slice, namespace inválido, output inválido e limite operacional. Expected não é derivado do lowerer.

## Premissas

SP/AIR/air-java nos SHAs do source lock; source da fixture disponível apenas para geração offline; contrato input fechado para a superfície usada; nenhum callback ao produtor. Convenções de provenance e identidade por revisão serão explicitadas e testadas, sem default inventado.

## Comportamento esperado

1 Publication, 1 Unit, 1 Entry, 1 Sequence, 0 instruções comuns e Return vazio; start→label coerente; origens e partial inventory preservados; AirValidator estruturalmente válido; relatório tipado preserva obrigações. Nenhum Halt/Jump arbitrário, nenhum arquivo obrigatório dentro do core.

## Incerteza

Lacunas relevantes atravessam em escopo/dimensões corretos. Incapacidade de cumprir o perfil gera resultado explícito, sem Publication de sucesso amputada. O primeiro slice não promete fallback opaco horizontal.

## Fora de escopo

DATA/MOVE/IF/CALL/storage/aliases/predicados/valores/CFG/opaque amplo; adapter AIR JSON, reader CFG, CLI rica, rede/cloud/banco; alterações nos repos upstream. Gate/projeto/testes só podem ser implementados após autorizar CP0.

## Regras, decisões e garantias

O manifesto referencia contratos, ADRs, invariantes e evals. [Plano](plan.md) define checkpoints; [eval](eval.md) define a prova. Não reproduzir todo o catálogo como código de produção.
