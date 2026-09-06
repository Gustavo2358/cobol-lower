# Classificação de impacto downstream

Adaptada da taxonomia do ProLeap, mantendo os oito nomes para colaboração entre repositórios. Fonte: SRC-HARNESS, `downstream-impact-classification.md`.

A pergunta é: **qual primeira fronteira demonstravelmente deixa de cumprir o contrato?** Não é severity, prioridade ou autorização de remediação. Registre uma classe primária; efeitos posteriores entram no rationale.

| Classe | Evidência necessária |
| --- | --- |
| BLOCKS_SEMANTIC_PRODUCT | Fato exigido pelo contrato SP está perdido/incorreto antes de sua fronteira |
| BLOCKS_IR | SP está correto e o lowerer/representação AIR falha no contrato |
| BLOCKS_CFG | AIR e entradas necessárias estão corretas; derivações de controle falham |
| BLOCKS_DATAFLOW | Produtos anteriores corretos; efeitos/storage/fluxo exigido não cumpre contrato |
| BLOCKS_DEPENDENCY_FACTS | Produtos requeridos corretos; extração final de fatos falha |
| REDUCES_PRECISION | Argumento/oracle sustenta soundness; perde-se apenas informação útil |
| UNASSESSED | Evidência insuficiente para localizar a primeira falha; declarar gatilho de revisão |
| NOT_APPLICABLE | Não há impacto semântico no escopo examinado |

“Não suportamos MOVE ainda” não é automaticamente bug SP; pode ser limitação intencional do perfil. “Parece quebrar CFG” sem input/contrato/resultado não sustenta BLOCKS_CFG. Não confundir afirmação falsa com redução de precisão.

Todo finding novo traz: classe, rationale não vazio, evidência reproduzível e `reassess_when` para UNASSESSED. Registrar fatos anteriores corretos, por que classes anteriores não se aplicam e a consequência ao usuário. Um erro apenas de ligação interna de documentação pode ser NOT_APPLICABLE; perda de provenance semântica não é meramente documental por padrão.

Uma limitação conhecida continua no backlog sem iniciar trabalho automaticamente. Reclassificação preserva a justificativa anterior no histórico e identifica a nova evidência.
