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
                         caller / adapter AIR JSON de saída (futuro)
```

A seta de dados pode apontar para fora; a dependência de código continua apontando para dentro. `LowerCobol` é a porta de entrada definida pela aplicação. Um port de saída só é necessário quando o caso de uso precisar acionar um efeito externo; retornar um resultado não exige um repository fictício.

## Dependências permitidas

```text
composition root → adapters → application/domain → air-java → java.base
                          application → domain
```

O projeto `analysis-ir` é fonte normativa, não dependência de runtime. O modelo AIR compartilhado é um vocabulário semântico neutro, não infraestrutura. Jackson/Gson, arquivos e CLI não são vocabulário de domínio.

Estrutura inicial recomendada: um módulo core, com packages de domain/application/ports, e um módulo de adapters. O módulo de adapters pode conter inicialmente apenas entrada JSON. O bootstrap fixa nomes/pacotes e registra as escolhas; não há Java ou POM nesta entrega. Separar dois módulos serve para provar dependências, não para introduzir dezenas de abstrações.

## Hoje e depois

Hoje, a aplicação recebe dados normalizados de um arquivo. Depois, um adapter de integração recebe o contrato público materializado do frontend e entrega os mesmos dados à porta. A entrada não será trocada por classes internas do ProLeap.

Na saída, hoje/futuro arquivo: `Publication → writer externo → AIR JSON → reader externo → Publication`. Na integração direta: o integrador entrega a mesma `Publication` ao CFG. Nenhum codec entra em `air-java` ou no core. O contrato das portas e o comportamento semântico devem permanecer equivalentes, ainda que o wiring/build mude.

## Extensão

`GOBACK` é a primeira regra, não o nome do framework nem do container. Novas variantes exigem fatos upstream, mapeamento bilateral e oracles próprios. Um registro de regras deve detectar ambiguidade/ausência de handler e não depender da ordem de registro. Não implementar tal registro genérico antes da necessidade; um dispatch tipado exaustivo e localizado pode atender o primeiro slice.

## Limites

Sem CFG builder, reaching definitions, possible values, resolução final de chamadas, reconstrução de PIC/source ou callbacks para completar fatos. Construir labels/terminadores da AIR a partir de controle provado é lowering; descobrir controle não publicado pela ordem de statements não é.
