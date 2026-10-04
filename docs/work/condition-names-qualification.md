# Checkpoint 1 — qualificação de condições 88 e SET

Status: IN_PROGRESS, revisão nos PRs frontend #86, lower #58 e AIR #27; sem merge.

## Resultado por ocorrência

No CardDemo fixado em `59cc6c2fd7ebd7ef7925cad552a01a4b8b6e4d5e`, os 73 fontes
foram novamente analisados. [Inventário por fonte](https://github.com/Gustavo2358/proleap-poc/blob/b0a387c2bcecb0621a015a231543fb7de8dc65a4/docs/work/condition-names-corpus.csv). O inventário anterior foi unido aos fatos novos pelo
ID da referência AST, preservando o vínculo nominal da declaração 88.

| Medida | Resultado |
| --- | ---: |
| Usos no baseline / publicados | 3.807 / 3.807 |
| Leituras ligadas a TEST | 1.988 |
| Destinos ligados a SET ordenado | 1.819 |
| Usos ausentes / extras / sem consumo | 0 / 0 / 0 |
| Raízes de predicado publicadas | 1.438 |
| Gaps antes / depois | 28.050 / 23.960 |
| Redução líquida | 4.090 (14,58%) |

Os contextos de origem são 1.819 SET, 1.465 IF, 481 EVALUATE e 42 PERFORM.
Incluem 13 leituras com subscritos e um pai FILLER em LINKAGE. SET FALSE não ocorre
nesse corpus; seu valor declarado, ausência e formas opcionais têm testes sintéticos.

Os 80 gaps residuais `CONDITION_REFERENCE_KIND_NOT_PROJECTED` não coincidem
com a provenance de nenhum uso 88 publicado; o inventário por ocorrência fecha
a categoria sem remover esses diagnósticos independentes.

A redução é líquida: publicar EVALUATE parcial com seus seletores expõe gaps de
binding e controle que antes ficavam sob OBSERVED_STATEMENT_UNSUPPORTED. Os gaps
restantes de condições mistas preservam termos não 88 sem modelagem. Não se
converte input ausente ou storage desconhecido em prova positiva.

## Regressão integrada

A execução `pipeline-final` terminou frontend → lower → AIR JSON → CFG → dependencies
para os 73 fontes. A comparação por ocorrência/candidato preservou:

| Categoria | Sites | Candidatos |
| --- | ---: | ---: |
| Programas | 150 | 209 |
| Arquivos | 391 | 378 |
| Origem qualificada | 259 | 95 |

Zero sites perdidos, candidatos perdidos/adicionados ou mudanças de remainder
nas projeções comparadas. `sourceDependencies` e declarações de arquivos são
iguais após normalizar a identidade da publicação; `observed-dependencies` é
idêntico byte a byte nos 73 fontes. Os estados continuam **65 PARTIAL / 8 COMPLETE**.
Metadados de prova AIR podem mudar porque a semântica executável foi ampliada.

## Gates e evidência

- Frontend full local: PASS, 1.369 testes, zero falhas/erros; um teste de discovery
  opt-in exige `semantic.condition.required` e permanece ignorado. A regressão
  explícita dos 73 fontes foi executada separadamente.
- Lower suítes focais completas: PASS, incluindo oráculo de execução de SET,
  aliases, FALSE, WHEN NOT, intervalo numérico e contratos adversariais.
- AIR FAST e `mvn package`: PASS; CI do PR #27 verde.
- Frontend FAST final: PASS, 798 testes sem falhas, erros ou skips.
- Lower full local: PASS, incluindo semântica, capacidade/determinismo e arquitetura.
  O FAST com pins finais é registrado no PR consumidor #58.
- Repetição integrada final: 73/73 fontes, 292 etapas, zero falhas.

Baseline: frontend `ed4830e41689e05001468fe8d4cf9ffcfb87207f`, lower
`fdfddb714c8bf653ba471bdf901e1b2fe5dbc7a1`, AIR
`7d77330099f46117281fdcbb08304e20d68f5672`, CFG
`ae3b23d9e853f15fff64ebb49fa40be9391670ba`.
AIR atualizado: `a4c49bcf5e07000cb78349c2cc6357ca2dfe7acd`.

Evidência bruta local, preservada fora do Git do produto:
`/home/gustavo/workspace/teste-e2e/.gap-reconciliation-20261003/condition88-evidence/`.
Scripts: `run-frontend.py`, `audit-conditions.py`, `run-pipeline.py`,
`compare-dependencies.py`. Auditoria final: `conditions-final-audit.json`.
Execuções RED e correções intermediárias também permanecem nesse diretório.

A categoria está fechada para a **semântica de origem dos 3.807 usos observados**.
Persistem limitações independentes de acesso, índice, collation, input e controle,
com incerteza explícita. O resultado não declara completude geral de COBOL.

[Contagens e hashes de evidência](https://github.com/Gustavo2358/proleap-poc/blob/b0a387c2bcecb0621a015a231543fb7de8dc65a4/docs/work/condition-names-results.json).

## Pins de integração

- Produtor: `b0a387c2bcecb0621a015a231543fb7de8dc65a4` (SP 2.64).
- AIR: `a4c49bcf5e07000cb78349c2cc6357ca2dfe7acd`.
- CFG do corpus: `ae3b23d9e853f15fff64ebb49fa40be9391670ba`.

Disponibilizar AIR #27, lower #58 e então frontend #86. Sem merge nesta sessão.

A auditoria dos pins verificou 137 arquivos do produtor e 28 da AIR contra os
commits fixados. As 4.010 classes da execução integrada são idênticas byte a byte
aos builds finais: frontend 2.382, lower core 539, lower adapters 713, AIR model
341 e AIR JSON 35. A validação do corpus não depende de um binário anterior.

[Hashes, pins e equivalência de bytecode](condition-names-integration.json).
