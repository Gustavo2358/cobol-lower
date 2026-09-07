# Validação e resultados

O vocabulário abaixo foi materializado em tipos de aplicação/adapters no primeiro slice, mantendo separados transporte, insuficiência semântica e defeitos do tradutor. `Admission.Status.ADMITTED` não é sucesso AIR e apenas libera a regra; `LoweringResult.SUCCESS` exige Publication e relatório estrutural válido. O adapter `FileLowering.Lowered` pode carregar falha semântica, não é sinônimo de SUCCESS.

| Resultado | Significado | Publicação de sucesso? |
| --- | --- | --- |
| SUCCESS | Perfil admitido, tradução observável correta e checks AIR requeridos aprovados | Sim, com relatório/obrigações |
| INPUT_ERROR | Falha do adapter: I/O, encoding, JSON ou forma física | Não |
| UNSUPPORTED_VERSION (adapter: UNSUPPORTED_CONTRACT) | Schema/versão fora do conjunto explicitamente negociado | Não |
| INVALID_INPUT | Contradição no contrato semântico lido: duplicatas, dangling start, estados incoerentes | Não |
| UNSUPPORTED_SLICE | Fatos reconhecidos, mas capacidade/shape fora do perfil implementado | Não |
| BLOCKED_LOWERING | Fato indispensável desconhecido/input missing; não há tradução admitida | Não |
| OUTPUT_INVALID | AIR produzida violou regra do target; possível bug do lowerer | Não |
| VALIDATION_INCOMPLETE | Checker não decidiu um requisito necessário, capacidade/limite explícito | Não no primeiro perfil estrito |
| IMPLEMENTATION_LIMIT | Limite operacional atingido sem truncar publicação | Não |

SUCCESS não é sinônimo de inventário completo ou ausência de gaps. Uma AIR estruturalmente válida pode representar cobertura parcial e carregar obrigações do produtor.

## Pipeline de validação

Adapter negocia envelope/versão e estrutura física. Core valida o input tipado mínimo necessário; verifica admissibilidade do perfil; só então aplica regras. A saída é validada com AirValidator e comparada com oracles nos testes. Consumidores futuros validam também seu ponto de entrada, inclusive em memória.

Os níveis não devem ser colapsados: documento de outra versão não é “COBOL inválido”; variante conhecida fora do slice não é “JSON malformado”; falha do target não deve ser reportada como culpa do frontend sem investigação.

## Atomicidade

Não produzir uma Publication de sucesso parcialmente montada depois de falhar. Resultados de bloqueio podem carregar inventário observado e diagnósticos tipados, mas não um `Publication` aceito como válido. Publicação parcial AIR legítima é uma capability distinta, com contrato de coverage/envelopes e autorização própria.

## Diagnósticos

Exigir código estável, fase, severidade operacional, regra violada, identidade namespaced/escopo, proveniência disponível e requisito que faltou. Mensagem humana é complementar. Preservar causa técnica de I/O fora dos fatos AIR. Ordem deve ser determinística; limites de diagnóstico precisam informar truncamento **de diagnósticos**, sem confundi-lo com inventário semântico completo.

`SEMANTIC_OBLIGATION` é categoria do validador, não outro status de sucesso nem fato provado. `isStructurallyValid()` não substitui exame das obrigações e do alcance dos checks implementados.

Não implementar recuperação por exclusão, best-effort por regex ou fallback genérico não provado. Para achados upstream, usar [classificação de impacto](../engineering/downstream-impact.md).
