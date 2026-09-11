# DATA + MOVE textual + GOBACK — scalar-text-move@1

Entrada SP 1.2.0 explícita, pinada no [source lock](../sources/sources.lock.json).
A [regra e oracle congelados](../work/active/WORK-LOWER-006/spec.md) fundamentam
esta implementação do Checkpoint 4C. Sem interpretação de COBOL, CFG ou dataflow.

`CobolLowerer` seleciona o profile escalar por fatos DATA/MOVE; o CP3 usa
`EntryGobackLowerer` com contrato e bytes preservados. `SpJsonDecoder` escolhe
DTOs 1.1/1.2 explicitamente; shapes enriquecidas são recusadas sob 1.1.0.
Os records comuns são compartilhados fisicamente, sem defaults para fatos novos.
GOBACK 1.2.0 permanece equivalente ao 1.1.0, cujo fixture é imutável.

`ScalarMoveAdmission` reaproveita a validação comum de inventário/identidades,
construindo os mapas DATA e Statement uma vez. Exige prova positiva scalarText,
logicalValue TEXT, wholeItemAccess, binding RESOLVED/selected coerente, FULL_IDENTITY
e normalContinuation KNOWN. Compara extensões lógicas publicadas para coerência;
nunca conta caracteres para descobrir padding/conversão. Readiness.scope, PIC,
nomes e programPoint não completam nem escolhem semântica. A boundary 1.2.0
valida source.value como fato normalizado redundante: quando logicalValue existe,
kind deve ser ALPHANUMERIC, os valores devem ser iguais e logicalExtent deve
corresponder a codePointCount. Scalar extent >0, logical extent >=0. Incoerência
é INPUT_ERROR antes de Materialize; nenhum campo é reparado ou passado ao core
para interpretar spelling. O TextValue físico admite Unicode; isso não amplia
o profile COBOL do 4A. logicalValue=null continua ausência admissível fisicamente.

Há uma entry conhecida/assinatura zero, uma ou mais declarações e MOVEs, e cadeia
explícita fechada em GOBACK. Todos os statements devem estar nessa cadeia; dados
admitidos, inclusive não referenciados, são preservados uma vez. Ciclos, destinos
pendentes, campos ausentes e variantes extras rejeitam a publicação inteira.
Não se exige uma declaração por MOVE. Dados se ordenam canonicamente pelo handle
numérico com quatro passes radix limitados; nomes nunca são chave. A estrutura
valida pertinência/containment; a ordem de roots/statements e programPoint não
é controle no novo profile.

`ScalarDataTranslator`: WORKING_STORAGE + LOCAL + scalarText é a regra explícita
para Cell PERSISTENT/PRIVATE e Object PRIVATE known(text)/CellBinding. O profile
upstream exclui layouts relevantes desconhecidos; não se generaliza storage COBOL.
Não existe inicialização inventada. Ambos têm origins derivadas da declaração real
com regras identificadas, sem spans novos. O Object tem precisão de storage local;
demais dimensões declarativas permanecem UNAVAILABLE.

`ScalarSequenceAssembler` consome o plano por Entry.start/normalContinuation;
`MoveHandler` emite Assign ObjectPlace VALUE_WRITE ← Literal TextValue VALUE_READ;
`GobackHandler` emite Return([]). Tudo ocupa uma Sequence, sem Jump intermediário.
Assign faz claims EXACT locais de controle/storage/efeitos/valor/dependências
porque FULL_IDENTITY prova a escrita inteira obrigatória sem outras operações
neste profile. Não é cálculo de Possible Values nem prova geral de alias.
GOBACK mantém CONTROL EXACT e quatro dimensões UNAVAILABLE. Publication/Unit
continuam PARTIAL; alternate inventory e gaps independentes são transportados
como uncertainties, sem fortalecer o inventário. Gaps gerais são conservadoramente
associados a todas as dimensões da unit; texto do código não dirige o lowering.

Correlação em memória: DataLink(SP DATA, AIR Object, Cell, origem), StatementLink
(SP statement, operação, Label, origem) e OperandLink(ocorrência SP, AIR, origem).
MOVE/GOBACK compartilham Label; não há bijeção statement/label. Nenhum sidecar JSON.
CoverageItem/sourceKey mantém outputs para DATA, statement e operandos; Origins
preservam as declarações, literal, target, continuidade, entry e terminal.

PublicationId escalar: domínio `scalar-text-move@1/AIR2/SP1.2/xxh3-128-v2/` mais
`local-xxh3-128-v1`, encoder incremental existente. Inclui identidades, profile,
extensões lógicas, valor lógico, selected/whole, copy/next, nomes efetivamente
transportados para display, provenance, gaps e fatos Entry/GOBACK. Não inclui PIC,
legacy value, programPoint ou readiness textual sem efeito na AIR. DATA ordenada e
cadeia explícita tornam permutações físicas invariantes. Enums fechados não ganham
alternativas inventadas para testes. IDs locais usam namespaces/papéis novos na
política existente; registro por publicação detecta colisões de tuplas distintas.
SourceKeys escalares usam namespace compacto da publicação + espécie/handle,
sem repetir a codificação expandida da unit. CP3 mantém sua política anterior.

Indexação O(D+S+R+P), DATA O(D), percurso O(S), assembly O(D+S+P), IDs O(B) no
volume semântico incremental, O(D+S) para largura fixa. Maps com custo amortizado.
Não há scan de DATA por MOVE nem scan de statement por continuação. Custos de
codec/validator são do shared air-java e não estão cobertos por SLA do lower.

A entrada de produção não possui limites artificiais de bytes, nós ou visitas.
`ProductionLimits.SP_DEPTH=64` permanece como proteção do parser/validação de
estruturas patológicas; o tamanho de arrays válidos não consome profundidade.
O contador de visitas continua como telemetria para os ledgers de complexidade.
Os 100 diagnostics limitam relatos de inputs já inválidos. Strings de valores
não têm teto de capacidade no parser; overflow e validação de shape permanecem.

Historicamente, CP4C calibrou 32MiB/1500000 nós/250000 visitas no corpus real
de 1 DATA/10000 MOVE (18809400 bytes/1050154 nós/190021 visitas, depth8 com
folhas). A decisão atual e o RED/GREEN estão em
[WORK-LOWER-007](../work/active/WORK-LOWER-007/work-item.yaml); evidência CP4C
permanece imutável. Não há configuração pública nova. Shared AirJson permanece
no pin atual com 16MiB/depth128, uma limitação independente. Encode
ocorre antes de temp: exceder produz exit 5 `AIR codec IMPLEMENTATION_LIMIT`, não
trunca/cria prefixo e preserva eventual destino anterior. Falha de I/O é exit6
com cleanup do temp same-dir; bytes do codec vão diretamente ao writer.
Não foi criada opção pública de limite nem codec/validator paralelo.

Probes codificados 250/500 DATA+MOVE e 1 DATA/1000 MOVEs cabem no default. Probes
1000/2000 DATA+MOVE e 1 DATA/10000 MOVEs são do model; o limite operacional é
mostrado separadamente com 1 DATA/2500 MOVEs no default. O probe real de 1 DATA/10000 MOVE também atravessa arquivo/decoder/admission/lower
com defaults de produção e falha somente no codec AirJson conhecido. D1/D2
ficam em [BACKLOG-LOWER-017](../work/backlog/BACKLOG-LOWER-017.md) e
[BACKLOG-LOWER-018](../work/backlog/BACKLOG-LOWER-018.md), planned/NOT STARTED.
NOT IMPLEMENTED IN 4C. NOT A CLAIM OF LARGE-PROGRAM E2E READINESS.

## Extensão CP6 W1C

O contrato corrente SP1.3 acrescenta FITTED_TEXT com prova publicada. No
[profile CALL](call-lowering.md), MoveHandler consome o resultado ajustado bruto
e registra origem derivada. O profile scalar-text-move@1 continua FULL_IDENTITY;
a admissão CALL seleciona a capacidade adicional e a montagem de duas sequências.
A coerência copy/adjustment é validada também em memória.
