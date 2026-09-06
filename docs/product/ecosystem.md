# Ecossistema e desenvolvimento paralelo

| Repositório | Responsabilidade | Relação com cobol-lower |
| --- | --- | --- |
| `proleap-poc` | Frontend e Semantic Product COBOL | Autoridade do contrato de entrada; fornecedor de fixtures versionadas |
| `analysis-ir` | Especificação semântica AIR e binding de transporte | Autoridade do target; sem código Java |
| `air-java` | Modelo imutável AIR e validador estático | Única biblioteca compartilhada AIR utilizada pelo core |
| `cobol-lower` | Tradução bilateral de fatos COBOL publicados para AIR | Dono de regras de lowering e oracles da tradução |
| `analysis-cfg` | Consequências de controle derivadas da AIR | Consumidor independente; nunca dependência do núcleo do lowerer |
| Integrador futuro | Wiring/CLI/batch/reactor Maven | Compõe módulos e adapters, sem mover regras para a main |

## Paralelismo seguro

Lowerer e CFG podem avançar separadamente usando o mesmo SHA de `air-java` e a mesma AIR normativa. Cada projeto mantém seu harness, branch, PR, estado e gates. Nenhum agente modifica o outro repo como efeito lateral.

O primeiro contrato de rendezvous é uma Publication construída independentemente com entry e Return. O lowerer prova a tradução; o CFG prova a interpretação do controle; um E2E posterior compõe os dois. Um teste AIR compartilhado ajuda, mas não pode ser o único oracle de ambos, sob pena de reproduzir a mesma premissa errada.

## Atualização coordenada

Uma API ou semântica upstream nova pede análise de compatibilidade, novo source lock e regressões nos produtores/consumidores. Um SNAPSHOT com o mesmo nome Maven não prova que ambos compilaram contra o mesmo conteúdo. Use SHA verificável e registro da estratégia de resolução. Não pressupor release remota nem diretórios irmãos fixos.

A [baseline](../sources/upstream-state.md) separa estado observado de planejamento. Declarações antigas do CFG sobre ausência do binding não prevalecem sobre a AIR fixada mais recente.
