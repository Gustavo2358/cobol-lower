# Condições 88 e SET — checkpoint 1

Checkpoint 1 integrado pelo PR #58, consumidor inicial do SP 2.64 do frontend #86.
O checkpoint 3 em revisão amplia as árvores no SP 2.67, conforme
[predicados compartilhados](checkpoint3-predicates.md).

O frontend publica vínculo nominal com a variável, valores/intervalos, usos,
árvores de predicados e atribuições SET na ordem escrita. O lower apenas traduz
estes fatos. As regras são as da IBM para [condition-name conditions](https://www.ibm.com/docs/en/cobol-zos/6.3.0?topic=expressions-condition-name-condition)
e [SET formato 4](https://www.ibm.com/docs/en/cobol-zos/6.4.0?topic=statement-format-4-set-condition-names).

SET TRUE escreve o primeiro valor; FALSE requer valor falso declarado. Não
inventar booleano para um 88, storage para LINKAGE, collation ou codec ausentes.
Reutilizar a escrita de células/vistas lógicas, preservando aliases; cada destino
vê o resultado anterior. Predicados mantêm NOT/AND/OR e intervalos fechados sem
enumerar domínios ou distribuir combinações. Árvores de união equilibradas têm
O(n) nós. Custo de emissão é proporcional às ocorrências e valores declarados,
mais as vistas de armazenamento efetivamente atualizadas.

Oracle: atribuição observada no pai, primeiro valor de listas, endpoints de
intervalos, FALSE explícito/ausente, destinos que compartilham pai, aliases,
seleção ordenada EVALUATE, UNTIL e negativos de contrato. Validar com AirValidator
e comparar dependências dos 73 fontes CardDemo ao baseline imutável.

## Tradução e fechamento do contrato

O decoder admite `conditionNames` em SP 2.64. A porta em memória valida as mesmas
identidades e relações: pai publicado, provenance do pai, papéis READ/WRITE,
statement proprietário, ordinais e valores de SET. O campo não relaxa requisitos
de versões anteriores. Os testes adversariais cobrem remoção e contradição de
provas, além de papel inventado e árvore associada a outro tipo de statement.

TEST vira pertinência ao conjunto declarado; um intervalo numérico vira GE/LE.
NOT/AND/OR preservam a árvore e uniões são equilibradas. IF, EVALUATE TRUE/FALSE
(inclusive WHEN NOT) e UNTIL usam esses mesmos fatos. SET usa o primeiro valor
verdadeiro ou o valor falso explícito; a escrita textual reutiliza a atualização
canônica de células e vistas de MOVE, mantendo aliases e a ordem dos destinos.

A AIR já especifica LT/LE/GT/GE. O PR de apoio
[air-java #27](https://github.com/Gustavo2358/air-java/pull/27) transporta esses
operadores no codec, sem alterar o modelo ou a especificação AIR.

## Incerteza e custo

Sem localização/tipo suficiente, SET emite HavocMay limitado ao objeto nominal
quando disponível; caso contrário, mantém o limite AllMemory explícito. Não cria
uma localização para LINKAGE. Sem acesso, índice ou collation provados, a condição
mantém expressão Unknown com motivo localizado. A modelagem de origem do 88 não
é uma prova da representação física ou do valor de runtime.

Definições, usos, predicados, statements e famílias de vistas são indexados uma
vez. A tradução visita a árvore e os intervalos publicados sem enumerar valores,
sem DNF e sem produto cartesiano. Atualizar aliases custa o número de vistas
realmente afetadas. O teste do intervalo até 999999999 verifica endpoints sem
produzir uma expressão por valor.

A qualificação usa os 73 fontes CardDemo e o comparador por site/candidato das
categorias programas, arquivos e dependências qualificadas de origem. Mantém os
65 PARTIAL e 8 COMPLETE, sem tratar ausência de perda no corpus como completude
geral. [Resultado do checkpoint](../work/condition-names-qualification.md).
