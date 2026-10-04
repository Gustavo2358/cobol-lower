# Transferências MOVE canônicas — SP 2.66

O consumidor valida os fatos numéricos/textuais na porta JSON e em memória.
`ScalarNumber` e `NumericTransfer` substituem o caminho de inteiro DISPLAY;
expressões AIR preservam leituras, receptores, ordem, provenance e incerteza.
Os descritores de sinal, escala, USAGE, edição e TRUNC vêm do frontend. O lower
não interpreta COBOL, PICTURE, nomes ou offsets não publicados.

Acesso/tipo conhecido não cria Cell ou escrita MUST. Views compartilhadas mantêm
seus bounds; destinatários alternativos conservam MAY. Valor desconhecido usa
Assign(Unknown) com dependências quando pureza, destino e efeito estão provados.
TRUNC ausente não cria configuração implícita nem falha de controle fictícia.
Dados textuais/numéricos de representação não provada mantêm guards próprios.

A tradução é linear nos receptores e no tamanho das receitas tipadas. Índices são
construídos uma vez; não há enumeração de valores, opções ou combinações de aliases.
Oracles, pins e qualificação: [prioridade 2](../work/numeric-move-full.md).

## Receptores mistos

Os certificados numericTransfers também admitem receptores textuais tipados,
com edição explícita ou emissor inteiro. O valor literal certificado é o valor
numérico anterior à formatação; o consumidor verifica igualdade exata e aplica
o formatador já canônico. DATA conserva um prefixo ordenado de receptores
provados. Não há nova análise COBOL, leitura de PICTURE ou produto cartesiano.
A validação rejeita origem fracionária para texto comum e literal adulterado.

Fonte: IBM Enterprise COBOL 6.4 Language Reference, MOVE, p. 401–404,
https://publibfp.dhe.ibm.com/epubs/pdf/igy6lr40.pdf.
Oracle NumericEditSuite: -23 para 99, X(6), +9999 e 99 produz 23,
"0023  ", "-0023" e 23; a atualização dentro de L/edição/R mantém L e R.
O consumidor congelado anterior rejeita o novo certificado; a execução AIR
corrente é comparada com esses resultados escritos independentemente.

## Conversão de DATA textual

A origem textual tipada é distinta de número já tipado. A admissão exige
controle canônico publicado. O lowering emite um predicado is_digits, uma
alternativa de conversão/ajuste e uma alternativa explícita para comportamento
inválido. parse_integer possui fallback unknown(INT) com razão própria;
nenhuma string inválida vira zero. O ramo inválido preserva os operandos,
continuação possível e limites abertos de controle, memória e dependências.
As duas alternativas têm tamanho constante por MOVE, além das transferências
publicadas; não há enumeração dos valores possíveis do texto.

## Preenchimentos da sequência de ordenação

FIGURATIVE_LOW/HIGH de SP 2.66 exigem ausência de valor concreto no wire e na
porta em memória. A tradução usa fill_text com caractere Unknown(TEXT) ajustado
a uma posição, razão COLLATING_CHARACTER_NOT_SELECTED e precisão de valores
OPEN. A família textual compartilha a escrita, preservando prefixo e sufixo.
Nenhum byte ou CCSID é escolhido pelo lower. A dimensão espacial da escrita é
exata; a escolha do caractere permanece aberta.

FigurativeFillChecks executa as atribuições com dois pares de caracteres,
inclusive Unicode suplementar, e verifica seis objetos e vizinhos. O E2E
figurative-fill-calls preserva o candidato AB do campo vizinho e mantém sem
candidato concreto o CALL do campo preenchido; ambos conservam remainder.
O teste não afirma fechamento da propagação regional.

## Número em armazenamento compartilhado

A admissão distingue tipo lógico e célula independente. LOGICAL_NUMBER disponível
com bound materializável autoriza a conversão de MOVE; não autoriza uma nova Cell.
O tradutor mantém UnknownBinding no bound de armazenamento publicado. Para DATA,
receptores potencialmente sobrepostos ao emissor não recebem o certificado.
A leitura de um número por view compartilhada passa pelo mesmo controlador de
validade usado nas conversões textuais: o predicado de validade da representação
permanece desconhecido e o ramo inválido conserva efeitos/controle/dependências
abertos. Conversão numérica para texto também passa por esse controlador.

SharedNumericChecks verifica as quatro views numéricas do mesmo armazenamento,
duas alternativas de validade, valores literais ajustados e rejeição de um
certificado forjado com emissor/receptor sobrepostos. Não certifica o valor de
MONTH após uma escrita em DATE-N sem uma interpretação de representação.

### Limite do controle para representação inválida

MOVE não declara um destino de desvio local. A alternativa inválida conserva a
continuação normal conhecida, exceção e parada próprias da operação,
além de divergência e controle externo abertos. Ela não acrescenta GO TO para cada
rótulo da publicação. A opção NUMCHECK documenta continuação com MSG e término
com ABD; NUMPROC/INVDATA documentam resultados distintos com dados inválidos.
Nenhuma dessas regras cria destinos locais de GO TO. Efeitos de memória e
dependências da alternativa inválida continuam abertos.

Fontes: [IBM NUMCHECK 6.4](https://www.ibm.com/docs/en/cobol-zos/6.4.0?topic=options-numcheck),
[IBM NUMPROC](https://www.ibm.com/docs/en/cobol-zos/6.3.0?topic=options-numproc),
[IBM migração com dados inválidos](https://www.ibm.com/docs/en/cobol-zos/6.4.0?topic=6-error-behavior-changes-incorrect-programs).
A regressão COACTUPC revelou que o antigo AllControl inventava reentrada em
PERFORM local. O teste agora rejeita esse excesso de bound; não remove a
alternativa inválida nem escolhe uma opção de compilação.

## Truncamento binário aberto

SP2.66 conserva a conversão quando TRUNC é UNSPECIFIED/OPT e o emissor pode
exceder a PICTURE. Literais têm value ausente exatamente nesses casos; fabricar
um resultado STD/BIN ou omitir um resultado comum é inválido. DATA conserva
a leitura em unknown INT/DECIMAL com BINARY_TRUNCATION_RESULT_UNKNOWN.
Resultado recebido incerto usa Assign(Unknown) com suas dependências.
Somente validade não provada do emissor exige o guard e a fronteira de falha.
Não há enumeração de opções ou produto de ramos por receptor. O caso 12 para
S99 COMP continua literal 12; 123451 fica aberto. NumericConversionSuite
verifica atribuições, leituras e certificados adversariais; TRUNC isolado
não cria ramos de controle.
Fonte: https://www.ibm.com/docs/en/cobol-zos/6.4?topic=options-trunc .


### Invalid-data exits remain local to the operation

An invalid numeric representation retains open memory/resource effects and may
continue, raise an arbitrary exception, halt, diverge or transfer outside.
MOVE has no program-return operation; unknown data does not synthesize GOBACK.
The first three possibilities use the AIR envelope's own explicit alternatives;
only divergence/external control remain scoped. A unit-wide exceptional-exit scope
would also select exits belonging to unrelated operations (including every local
PERFORM failure), introducing a dense graph without source evidence. This is a
change of representation of the same possible outcome classes, not a claim that
invalid data is valid or that its effects are local. No local label is invented.
Construction creates three alternatives per MOVE, independent of unit size.
Oracle: generated envelopes contain those alternatives, never a scope enumerating
other operations' exits; COTRN02C exercises repeated PERFORM context joins.


### Receiving TRUNC uncertainty is a value boundary

For valid numeric inputs, unknown STD/OPT/BIN selection affects the receiving
result, not the MOVE's source continuation or unrelated memory/resources. IBM
TRUNC describes truncation to the PICTURE or allocated binary width; its examples
include a literal exceeding the receiving PICTURE. Preserve this as an unknown
INT/DECIMAL result with the sending expression as a dependency. NUMCHECK(BIN)
checks sending fields, not the receiving overflow of a literal MOVE. No absent
compiler option selects a default or automatically synthesizes runtime checking.

This does not discharge representation validity for a shared numeric view or
text-to-number source. Those existing guards and invalid-source frontiers remain.
Algorithm: one unknown value per receiver; no configuration alternatives or new
control blocks. Oracle: literal and exact-cell DATA to S99 COMP without TRUNC
retain three unknown assignments, one sending read and the common exact 12;
no invented failure branch. Shared numeric/text invalid-source tests still apply.
Sources verified 2026-10-04: IBM 6.4 TRUNC and NUMCHECK:
https://www.ibm.com/docs/en/cobol-zos/6.4.0?topic=options-trunc
https://www.ibm.com/docs/en/cobol-zos/6.4.0?topic=options-numcheck
