# Coverage, readiness e incerteza

## Três perguntas diferentes

Inventory responde o que foi observado/inventariado. Coverage responde como cada ocorrência foi representada. Readiness descreve suficiência relativa a uma capability upstream. Precisão AIR descreve garantias nas dimensões do target. Não existe conversão global `SUFFICIENT → EXACT`.

O mapeamento exige regra por dimensão, escopo e fatos concretos. Um `READY` publicado pelo frontend pode não satisfazer precondições da AIR, como os audits de CALL/IF já demonstraram. Um `BLOCKED` em effects não impede necessariamente o primeiro target de controle, mas também não autoriza anunciar efeitos vazios.

## Entry inventory

`PRIMARY_ONLY/PARTIAL` com `ALTERNATE_ENTRIES_NOT_PROJECTED` é esperado. A entry primária pode ter start/assinatura conhecidos enquanto o inventário de todas as entradas continua aberto. Não interpretar ausência de entries alternativas como prova de que não existem.

A política inicial é publicar cobertura conservadora da Unit/publicação e uma lacuna AIR correlacionada com esse gap, mantendo o fato positivo de entry/Return. O escopo deve mencionar o inventário não fechado, não degradar a referência conhecida de start como se ela fosse incerta. Se a API não oferecer granularidade específica de inventário, usar escopo mais amplo honestamente, sem claim falsa de completude.

## Dimensões no primeiro slice

| Informação | Garantia que pode ser expressa | O que deve continuar separado |
| --- | --- | --- |
| Start conhecido | Entry aponta para label do statement publicado | Inventário de entries alternativas |
| GOBACK tipado | Saída da ativação corrente, sem successor local | Runtime caller, contexto raiz/subprograma |
| Assinatura zero conhecida | Nenhum parâmetro nem valor explícito de retorno no perfil | Assinaturas parciais de outras entradas |
| Effects/dataflow BLOCKED | Nenhuma certificação de effects/dataflow herdada do frontend | Sem inventar no-op ou garantia física de memória |
| Provenance parcial | Correlacionar evidência disponível | Sem fabricar coordenadas |

Cada claim EXACT/CONSERVATIVE/OPEN/UNAVAILABLE/NOT_APPLICABLE da AIR precisa de rationale. NOT_APPLICABLE significa de fato não aplicável à observação, não “não implementamos”. Um relatório deve preservar as claims upstream usadas e as rejeitadas como insuficientes.

## Monotonicidade de conhecimento

Perder um fato não pode fortalecer uma claim; adicionar um gap não pode fechar inventário. Isso não implica que todo enriquecimento aceite automaticamente mais inputs: pode revelar uma contradição real. Metamorfismos devem declarar suas pré-condições.

Não misturar ausência JSON, null, coleção vazia, unknown e input missing. Tampouco string de gap é automaticamente um código normativo AIR: quando não houver código existente, use código qualificado de produtor, com origem/razão/dimensão/escopo válidos. Não reduzir todas as incertezas a uma mensagem de console.

## Relatório versus fatos AIR

O relatório de lowering preserva a rastreabilidade das decisões e diagnósticos, mas uma lacuna necessária à interpretação da AIR não pode existir **somente** no relatório. O consumidor que recebe apenas Publication precisa observar seus limites sem callback para o relatório ou frontend.
