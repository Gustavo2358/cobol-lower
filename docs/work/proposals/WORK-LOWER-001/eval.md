# WORK-LOWER-001 — Avaliação

## O que prova corretude

Expected AIR manual + fixture SP real + oracle de correlação Entry/start/Return, não apenas checker AIR. O [catálogo](../../../evals/catalog.md) define propriedades por ID e o plano seleciona por checkpoint.

## Positivos

SP1.1 conhecido, assinatura zero/sem retorno, GOBACK único, nomes/handles diferentes da fixture, memória/arquivo equivalentes e inventário alternativo parcial preservado.

## Negativos

Versão futura/1.0, propriedade duplicada, ID duplicado/dangling, start cruzado/ausente, assinatura partial/null/RETURNING, variant conhecida fora do slice, output AIR inválido e dependência arquitetural proibida.

## Ambíguos

Não selecionar candidato por ordem. Onde o primeiro perfil não admite ambiguidade/unknown, retornar blocked/unsupported com razão. Não criar cenário JSON inexistente para testar cross-unit; usar porta tipada quando necessário.

## Adversariais

GOBACK seguido de CONTINUE; OBSERVED cujo texto parece GOBACK; perda do gap alternativo; Return trocado por Halt; menor handle diferente do start; input maior filtrado para um statement; count null como zero.

## Regressões

Preservar shape/assinatura/readiness/namespace/provenance em todos os checkpoints. Nenhum teste upstream será alterado neste trabalho. Golden migrado exige justificativa/hash e não reescrita para satisfazer o decoder.

## Metamorfismo

Permutação de propriedades JSON, renomeação consistente de handles, variação de display name/filename e inputs construídos independentemente. Identidade de revisão diferente pode mudar; equivalência de controle não exige identidade longitudinal.

## Escala

Contadores para decoder/validador com inventários N/2N e limites explícitos. Inputs maiores podem ser fora do perfil de tradução e ainda servir à prova de custo/diagnóstico, sem promovê-los a support. Sem SLA ou limiar de máquina inventado.

## Falsificações e review

Ao menos Return→Halt, gap→COMPLETE e vazamento de transporte devem ser detectados pelo oracle/gate certo. Registrar causa da falha e restauração; um red por setup não serve. Segunda passagem verde depois do challenge. Logs podem ser resumidos, não fabricados.
