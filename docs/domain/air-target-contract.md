# Target AIR e biblioteca compartilhada

**Autoridade:** SRC-AIR. **Representação:** SRC-AIR-JAVA. Versões e commits estão no [source lock](../sources/sources.lock.json). Este é um guia de uso, não uma especificação AIR alternativa.

## Modelo

Usar `io.github.gustavo2358.air.model.Publication` e os tipos reais da biblioteca compartilhada. Não criar um modelo AIR local para contornar API, validador ou versão. DTO de transporte da entrada não é AIR; wrapper de resultado também não pode mudar seu significado.

Na baseline reconciliada, Publication não possui contracts; `ContractRef` é valor rastreável e não lookup externo. Targets de interação são internal/literal/computed. Assinaturas preservam inventários independentes de parâmetros e resultados. `InvocationOutcomes` e `ControlEnvelope` não são intercambiáveis; `continue` não autoriza fallthrough em terminador. Não ressuscitar SafetyAssertion, ResourceTarget ou return.entryScope.

## Primeiro target

`Unit.BodyAvailability.AVAILABLE`, Entry com initialLabel fechado na mesma Unit e assinatura vazia fechada somente porque zero parâmetros e ausência de RETURNING são fatos. Sequence tem exatamente um terminador `Operations.Return`, sem valores. A ordem física entre sequences não implica execução.

Objetos e armazenamento vazios neste output derivam do perfil sem DATA admitido. Não usar os mesmos vazios quando um próximo slice receber declarações ainda não baixadas.

## Pré-condições para expansão

`assign` exige cópia sem conversão e `sameDomain`; padding/truncamento precisam de normalização explícita sustentada. `unknown_type` não é wildcard. `branch` exige predicado booleano com garantias compatíveis de avaliação; uma ConditionSurface nominal não basta. `invoke` exige target/argumentos/effects/outcomes/assinatura materializados de modo coerente. `ObservedStatement` não vira nop; fallback opaco exige envelopes válidos e checkpoint próprio.

Essas restrições guiam o backlog. Não implementar outro frontend no lowerer para satisfazê-las.

## Validação de saída

`AirValidator.validate(publication)` aplica checks estruturais do runtime fixado. Resultado INVALID_IR em uma publicação produzida pelo lowerer é erro de tradução/contrato a investigar, não motivo para apagar operações e tentar de novo. INCOMPLETE_VALIDATION é distinto de erro estrutural e de ausência de suporte ao slice. Conservar as obrigações semânticas; o checker não prova que COBOL foi traduzido corretamente.

Não duplicar TypeRef/sameDomain/closure checks do validador inteiro. A validação da correlação source → target e do perfil local continua sendo responsabilidade dos oracles do lowerer.

## Evolução do runtime

A mesma `0.1.0-SNAPSHOT` pode representar commits diferentes. Resolver a dependência pelo SHA documentado, build reproduzível e proveniência do artefato; sem JAR arbitrário ou vendoring. Mudança de source lock não deve ser automática por `main` móvel em cada build.
