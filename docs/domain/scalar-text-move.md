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
legacy source.value, nomes e programPoint não completam nem escolhem semântica.

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

CLI mantém limite SP 100000 bytes, depth64/50000 nós e limites de admissão atuais.
Escala maior usa a porta em memória/decoder com limites explícitos de teste;
não amplia capacidade semântica. Shared AirJson mantém 16 MiB/depth128. Encode
ocorre antes de temp: exceder produz exit5 `AIR codec IMPLEMENTATION_LIMIT`, não
trunca/cria prefixo e preserva eventual destino anterior. Falha de I/O é exit6
com cleanup do temp same-dir; bytes do codec vão diretamente ao writer.
Não foi criada opção pública de limite nem codec/validator paralelo.

Probes codificados 250/500 DATA+MOVE e 1 DATA/1000 MOVEs cabem no default. Probes
1000/2000 DATA+MOVE e 1 DATA/10000 MOVEs são do model; o limite operacional é
mostrado separadamente com 1 DATA/2500 MOVEs no default. A composição CLI de teste
usa a porta existente para alcançar a fronteira de saída sem alterar o teto SP.
