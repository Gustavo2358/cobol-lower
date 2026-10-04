# Fechamento do lower — gaps ativos, condições 88 e MOVE

Status: DONE / MERGED. [PR #58](https://github.com/Gustavo2358/cobol-lower/pull/58),
merge `b4551bec673957a73ca1d2533e830b0584f2088b`, aprovado em 2026-10-04.

O lower consome os fatos canônicos SP 2.66 e preserva leitura, efeito, armazenamento,
aliases e autoridade de controle separadamente. Valor desconhecido conserva o efeito
conhecido; representação inválida permanece uma fronteira própria.

## Validação

FAST local do repin passou (267,630 s); os checks remotos do head de integração
passaram antes do merge. O merge preserva a árvore testada. Os pins e os hashes
apontam para os commits integrados do frontend #86, IR #10 e AIR #27.
A correção normativa de DecimalPart.kind foi qualificada por teste literal independente
e mutações na AIR, sem alteração de produção nesta integração.

O corpus anterior de 73 fontes/292 etapas é reutilizado por igualdade dos fontes,
testes, recursos e POMs. A reexecução de cache posterior processou 73 dependencies,
reutilizando 219 produtos. Este fechamento documental executa apenas o gate docs.

## Resultado e limites

A Prioridade 2 conclui o escopo causal finito aprovado. Permanecem 14.528 gaps,
65 PARTIAL e 8 COMPLETE. Dos 7.015 MOVEs, 5.450 são precisos, 236 preservam efeito
causal abstrato e 1.329 ficam em fronteiras declaradas. As três famílias originais
caíram de 10.803 para 1.464 diagnósticos.

A exclusão comprovada de COSGN00C no caminho COPAUS0C e os cinco sites com
openControlRemainder ampliado continuam documentados na [qualificação](numeric-move-full.md).
Não se afirma identidade total de outputs nem suporte integral a COBOL.
Nenhuma modelagem adicional ou checkpoint 3 faz parte do fechamento.
