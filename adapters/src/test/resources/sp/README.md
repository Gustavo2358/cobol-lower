# Golden SP 1.1.0

O arquivo `cobol-semantic-product.json` é uma cópia byte a byte do output canônico do produtor fixado, sem normalização, redução ou newline adicional. Registro de aquisição: [fixture-intake](../../../../../docs/evals/fixture-intake.json). Não regenerar pelo decoder.

4C: scalar-move-1.2.0.json e scalar-physical-1.2.0.json são outputs reais do merge
4A. AIR-MOVE.cbl é o source upstream intacto. Digests/comandos no
[registro de aquisição](../../../../../../docs/evals/fixture-intake.json).

## Production capacity — B1/B2 remediation

production-400.json.gz e production-shared-10000.json.gz preservam bytes exatos
de SP1.2.0 real do merge2815e805fd3a9ef4762a39ab9435260fc76da0e8, gzip lossless.
[Captura e medidas](../../../../../docs/quality/WORK-LOWER-006/production-fixtures.json).
SCALE-PRODUCTION.cbl é a fonte400DATA/MOVE; o corpus10k é emitido pelo teste
upstream ScalarMoveScaleTest.tenThousandTargetsShareOneDataDeclaration, sem edição
de output. Testes ordinários descomprimem snapshots, não executam frontend.
