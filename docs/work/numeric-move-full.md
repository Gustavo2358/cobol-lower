# Prioridade 2 — transferências canônicas do SP 2.66

> Fechamento do lower: DONE / MERGED no PR #58. [Integração e limites](priority2-integration.md). Os estados de revisão abaixo são históricos.

Implementação qualificada e pronta para revisão. O work item permanece
IN_PROGRESS até merge. Esta entrega fecha o escopo causal finito da auditoria
de produto de 2026-10-04; o checkpoint CP2.1 permanece como baseline histórico.

## Mudança entregue

ScalarNumber/NumericTransfer substituem a porta de inteiro DISPLAY.
O decoder e a porta em memória verificam identidade, tipos, intervalos,
certificados e campos obrigatórios. O lowering traduz os mesmos fatos canônicos
em atribuições/expressões AIR, sem reanálise COBOL ou PICTURE.

Conversões preservam leitura, ordem e efeito por receptor. Unknown com
expressão de origem conserva causalidade quando efeito, destino e pureza são
provados. Uma escrita exata continua podendo substituir a definição anterior
mesmo quando o valor escrito é desconhecido; alvos abertos continuam MAY.
Views compartilhadas conservam bounds e aliases, sem células independentes
fabricadas. Fitting/edição/fatias usam receitas comprimidas e preservam vizinhos.

TRUNC desconhecido afeta o valor recebido; não cria falha de controle por si só.
Uma representação textual/numérica de origem cuja validade não está provada
mantém guard e fronteira explícitos. O ramo inválido conserva a continuação
normal publicada, exceção/parada, divergência e controle externo; não inventa
GO TO, retorno de unidade nem uma saída para cada operação local.
Memória/recursos abertos desse ramo continuam declarados.

[Regras e oracles](../domain/numeric-move.md). Algoritmo linear nos receptores
e receitas, além das projeções de famílias materializadas; não enumera opções
TRUNC, valores ou combinações de aliases. O modelo AIR é o compartilhado,
validado por AirValidator e transportado pelo codec canônico.

## Qualificação final — 2026-10-04

O corpus congelado contém 73 fontes/variantes CardDemo. As 292 etapas de
frontend, lower, CFG e dependencies passaram. Depois do ajuste apenas no cache
interno de BDD, dependencies foi reexecutado nos 73 inputs: 73/73 semanticamente
idênticos, sendo 71 byte-idênticos e dois com diferenças somente em métricas de
execução. SP, AIR e CFG dessa reexecução foram reutilizados sem alteração.

| Inventário por ocorrência de MOVE | Quantidade |
| --- | ---: |
| Transferência precisa no modelo admitido | 5.450 |
| Transferência abstrata com causa preservada | 236 |
| Fronteira restante de modelagem | 1.329 |
| Código ausente identificado como causa direta | 0 |
| Total | 7.015 |

As 236 ocorrências abstratas produzem 463 Assigns com Unknown, considerando
receptores e contextos. Não resta MOVE_VALUE_NOT_MODELED na AIR deste corpus.
As 1.329 fronteiras se dividem em 1.173 de capacidade MOVE e 156 de controle ou
publicação executável. Esta classificação exige conferir a AIR por origem;
um fato positivo no SP sozinho não conta como transferência executável.

As três classes priorizadas somavam 10.803 diagnósticos. Restam 1.035
MOVE_IDENTITY_NOT_PROVEN e 429 SCALAR_WHOLE_ITEM_NOT_PROVEN;
LITERAL_KIND_NOT_PUBLISHED chegou a zero. São 1.464 diagnósticos, não 1.464
MOVEs. O total de gaps SP caiu de 23.960 após a prioridade 1 para 14.528.
Os resultados conservam 65 PARTIAL / 8 COMPLETE.

## Dependências e controles preservados

- Programas: 150 sites / 208 candidatos; arquivos: 391 / 378;
  fonte qualificada: 259 / 95. COPY/SQL INCLUDE, declarações de arquivo e
  dependências observadas permanecem iguais nos 73 fontes.
- Frente a CP2.1, uma exclusão foi investigada: COSGN00C no XCTL statement:323
  de COPAUS0C. A chamada do parágrafo grava COMEN01C antes do PERFORM; o fallback
  por SPACES/LOW-VALUES não cabe nesse caminho provado. Oracles independentes
  menu/spaces/unknown preservam o fallback nos casos em que ele é possível.
- Frente à wave10, nenhum candidato, site, suporte, qualificação ou remainder
  de valor mudou. Cinco sites passaram a explicitar openControlRemainder:
  MQPUT1/MQCLOSE/MQGET em COPAUA0C, COPAUS2C em COPAUS1C e MVSWAIT em COBSWAIT.
  A causa são fronteiras de representação numérica/textual inválida; os
  candidatos e seus supports permanecem presentes.
- As provas de fonte qualificada conservaram unidades, nós e derivações;
  65 bundles mudaram somente hashes de SP/AIR. Não se exige identidade de
  hash de produtos cujo conteúdo mudou.
- ConditionNames e ControlTopology são idênticos aos de CP2.1 nos 73 fontes.
  Permanecem 3.807 ocorrências de condições 88, incluindo 1.819 SETs.

## Limites explícitos

A conclusão segue o escopo causal finito definido na auditoria de produto de
2026-10-04. Não significa completar a semântica de todo MOVE COBOL. Os resíduos
incluem funções/LENGTH OF (81 ocorrências observadas), receptores ainda sem
prova suficiente (48), RETURN-CODE (9), grupos, representações e acessos sem
modelagem, além das fronteiras de controle. O inventário registra 397 MOVEs
com faceta de storage de catálogo não provado e 16 com referência DIBSTAT
não resolvida; essas facetas não são uma contagem adicional de gaps.

Os 585 COPY e 11 SQL INCLUDE do inventário estão resolvidos nominalmente.
Isso não concede layout físico aos modelos de catálogo nem prova input completo
para qualquer execução. TRUNC/CCSID ausentes não recebem defaults; não há
manifesto externo obrigatório. AP/exporter, slicing interprograma e predicados
da prioridade 3 permanecem fora desta entrega.

## Evidência de validação

FAST lower final PASS (283,656 s), incluindo contratos JSON/porta em memória,
NumericConversionSuite, NumericEditSuite, SharedNumericChecks, fatias,
receptores mistos, figurativos e controles vizinhos. FAST de frontend/AIR/CFG
PASS. O fixture uncertain-trunc atravessa frontend e lower reais; o probe de
StatementEffects/RD registra:

```text
unknown=3 reads=1 causalSource=1 mustOverwrite=1 neighborPreserved=1 inventedBranches=0
```

O corpus completo e os oracles de navegação conservam as dependências listadas
acima. Provas/fatos 88 e topologia são idênticos. A regressão de desempenho
COTRN02C foi fechada no consumidor: cache BDD fixo de 1 MiB, sem reduzir fatos.
Dependencies soma 216,852 s no corpus final, contra 223,611 s na wave10;
trata-se de uma rodada medida com três workers, não de garantia estatística.

Evidência bruta no workspace: `.gap-reconciliation-20261003/priority2-evidence`.
Relatório integrado: `artefatos-e2e/priority2-move-20261004/REPORT.md`.
Os SHAs executados estão em final-runtime-revisions.json e no runtime.json
da revalidação do cache; commits finais documentais preservam a produção.
PRs coordenados: analysis-ir #10 → AIR #27; frontend #86 → lower #58 → CFG #64.
Nenhum merge. Resíduos genuínos permanecem auditáveis, sem fallback Nop de
MOVE_VALUE_NOT_MODELED neste corpus.
