# Referências históricas de controle

As máquinas JSON foram capturadas da produção de cobol-lower
`cb1b87250f133535ab26a3056be0d6da144d81bf`. São referências diferenciais, não uma
especificação COBOL. Não regenerar usando o candidato em teste.

Cada arquivo registra o fixture-fonte, SHA-256, variante da política e estados
da máquina observável. `ControlLanguageOracle` verifica a identidade e compara
linguagens com o intérprete independente `LocalControlOracle`, ocultando somente
Jump e operações administrativas de frames. Instruções, ocorrência-fonte e target
literal continuam observáveis. Verificações de valores, efeitos e dependências
pertencem às suites manuais e à qualificação integrada.

A captura utilizou os 156 arquivos de produção do commit de referência, conferidos
contra Git, e um helper de teste que exporta a máquina. O JAR dessa produção tem
SHA-256 `67aafa07148b2a116ecb92b07b3e2cab9f933e61ed1e0026b807b746098a78fe`.
O workspace preserva o helper, manifesto, comandos e logs em
`artefatos-e2e/lower-unification-20261003`. Fixtures de oito retornos que causam OOM
não são expandidas para capturar referências; são verificadas por escala e por
suas contrapartes menores.
