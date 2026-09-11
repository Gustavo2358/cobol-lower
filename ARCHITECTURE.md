# Arquitetura do cobol-lower

**Decisão de projeto:** produtor AIR específico de COBOL, independente de transporte e de consumidores. Detalhes em [fronteiras](docs/architecture/boundaries.md), [portas](docs/architecture/ports-and-adapters.md) e [ADRs](docs/architecture/decisions/index.md).

## Fluxo de dados, não direção de imports

```text
arquivo Semantic Product → adapter de entrada → SemanticProductInput
                                                   ↓ porta LowerCobol
                                                aplicação
                                                   ↓
                                           regras de lowering
                                                   ↓
                                         air-java::Publication
                                                   ↓
                                 AirValidator + relatório de lowering
                                                   ↓
                         caller / AirFileOutput → shared AirJson → arquivo AIR JSON (2A)
```

A seta de dados pode apontar para fora; a dependência de código continua apontando para dentro. `LowerCobol` é a porta de entrada definida pela aplicação. Um port de saída só é necessário quando o caso de uso precisar acionar um efeito externo; retornar um resultado não exige um repository fictício.

## Dependências permitidas

```text
composition root (CLI em adapters) → adapters → application/domain → air-java → java.base
adapters → air-json → air-java
                          application → domain
```

O projeto `analysis-ir` é fonte normativa, não dependência de runtime. O modelo AIR compartilhado é um vocabulário semântico neutro, não infraestrutura. Jackson/Gson, arquivos e CLI não são vocabulário de domínio.

Estrutura inicial recomendada: um módulo core, com packages de domain/application/ports, e um módulo de adapters. O módulo de adapters contém entrada SP JSON, writer AIR e CLI mínima. O bootstrap fixou nomes/pacotes; core permanece sem transporte. Separar dois módulos serve para provar dependências, não para introduzir dezenas de abstrações.

## Hoje e depois

Hoje, a aplicação recebe dados normalizados de um arquivo. Depois, um adapter de integração recebe o contrato público materializado do frontend e entrega os mesmos dados à porta. A entrada não será trocada por classes internas do ProLeap.

Na saída, implementado em 2A: `Publication → AirFileOutput → shared AirJson → arquivo AIR JSON`. O reader do analysis-cfg e CFG JSON são futuros. Na integração direta: o integrador entrega a mesma `Publication` ao CFG. O módulo `air-json` upstream depende do modelo `air-java`; somente adapters consome o codec. Nenhum codec entra no core do lower. O contrato das portas e o comportamento semântico devem permanecer equivalentes, ainda que o wiring/build mude.

## Extensão

`GOBACK` é a primeira regra, não o nome do framework nem do container. Novas variantes exigem fatos upstream, mapeamento bilateral e oracles próprios. Um registro de regras deve detectar ambiguidade/ausência de handler e não depender da ordem de registro. Não implementar tal registro genérico antes da necessidade; um dispatch tipado exaustivo e localizado pode atender o primeiro slice.

## Limites

Sem CFG builder, reaching definitions, possible values, resolução final de chamadas, reconstrução de PIC/source ou callbacks para completar fatos. Construir labels/terminadores da AIR a partir de controle provado é lowering; descobrir controle não publicado pela ordem de statements não é.

A identidade de publicação e os IDs locais derivados usam hash4j 0.30.0 no core, exclusivamente em CanonicalRevision (reutilizado por LocalIds),
para XXH3-128 incremental. A dependência não atravessa a API pública; não acrescenta
transporte nem framework. Algoritmo/seed/formato em [identidade](docs/domain/identity-and-provenance.md).

## Checkpoint 4C

CobolLowerer faz dispatch de dois profiles. ScalarMoveAdmission produz índices/plano;
ScalarDataTranslator e ScalarSequenceAssembler compõem MoveHandler/GobackHandler,
com assembly e validação compartilhada antes do resultado em memória.
[Contrato escalar](docs/domain/scalar-text-move.md). CLI usa essa mesma porta.

## CP6 W1C

O dispatch tipado CALL precede o profile escalar. CallAdmission compartilha os índices
de entrada; CallLowerer reutiliza tradução de DATA, MOVE e origens. InvokeHandler
e CallSequenceAssembler produzem duas sequências com continuação SP explícita.
Wire13/Materialize continuam em adapters. Core conhece somente SpInput e modelo/
validator AIR; codec continua exclusivo de adapters. [Contrato CALL](docs/domain/call-lowering.md).
