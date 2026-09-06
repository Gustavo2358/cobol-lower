# CP2 — Correção da preparação de um contracaso

A primeira execução após implementar o validador retornou INVALID_INPUT/STRUCTURE para o IF synthetic que pretendia testar UNSUPPORTED_SLICE. O test double omitia structure.branches. SemanticProductJsonWriter.document chama addBranch THEN e ELSE para cada IF, inclusive sem children; isso já estava no FREEZE de CP2. O input de teste era contraditório, não uma variante fisicamente/relacionalmente legal como exigia a premissa do oracle.

Preservado o expected UNSUPPORTED_SLICE da variante coerente; corrigida somente sua preparação com os dois inventários vazios publicados. O contracaso original não foi apagado: agora também verifica explicitamente INVALID_INPUT/STRUCTURE. Nenhum código do validador, golden, invariant, gate ou contrato foi enfraquecido. Essa falha não é contada como falsificação controlada válida.
