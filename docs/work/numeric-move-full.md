# Prioridade 2 completa — integração SP 2.66

Status: IN_PROGRESS. O checkpoint 2.1 permanece como baseline, sem representar
fechamento desta prioridade.

A porta canônica substitui a prova inteira por `ScalarNumber` e
`NumericTransfer`. O decoder exige precisão, escala, sinal e representação
explícitos; campos primitivos ausentes não ganham defaults. O lowering reutiliza
o handler MOVE atual e atribuições INT/DECIMAL da AIR. Rejeita certificados
incoerentes tanto no JSON quanto na porta em memória.

A fonte das regras é [IBM MOVE](https://www.ibm.com/docs/en/cobol-zos/6.4.0?topic=moves-elementary-move-rules)
e [IBM TRUNC](https://www.ibm.com/docs/en/cobol-zos/6.4?topic=options-trunc).
O significado das expressões é definido pelo contrato AIR coordenado nesta mudança.
Sinal, escala, resto decimal e capacidade binária são explícitos. Na ausência de
TRUNC, apenas resultados comuns a STD/OPT/BIN são certificados para BINARY.

A admissão usa joins indexados por ocorrência e receptor. A tradução cria uma
quantidade constante de operandos por receptor. A checagem de literal com escala
extrema usa aritmética modular, sem expandir bilhões de zeros. As leituras dentro
dessas expressões continuam visíveis no CFG por `Operands.children`.

Os oráculos independentes executam a AIR produzida, incluindo truncamento de
texto e valores numéricos. Testes adversariais retiram campos, alteram tipos,
valores, limites, identidades, referências inteiras e a versão do contrato.
Fixtures numéricas são regeneradas pelo produtor atual para exercitar o contrato
novo; o consumidor não ganha uma segunda implementação inteira histórica.

A primeira medição nos 73 fontes passou nas 292 etapas e preservou todas as
projeções semânticas de dependências e os estados 65 PARTIAL / 8 COMPLETE.
A qualificação final e a preservação causal delimitada abaixo permanecem pendentes.

## TRUNC explícito

A opção CBL TRUNC é publicada no descritor numérico (`trunc`). STD usa a
PICTURE do receptor; BIN usa a palavra de 16/32/64 bits. OPT só recebe prova
quando o valor cabe na PICTURE, porque o resultado fora dela é imprevisível.
UNSPECIFIED conserva apenas a interseção de STD/OPT/BIN. TRUNC só se aplica a
BINARY; as demais representações exigem UNSPECIFIED no descritor.
O oracle independente usa 123451 para S99 COMP: STD = 51, BIN = -7621;
OPT e ausência da opção conservam resultado aberto quando não há resultado comum; a conversão continua reconhecida. A mesma regra vale para emissor DATA.
Fonte: https://www.ibm.com/docs/en/cobol-zos/6.4.0?topic=options-trunc

## Literal inteiro para texto

A AST conserva os dígitos escritos do literal inteiro separadamente do seu
valor numérico. O emissor textual usa a magnitude sem sinal; a largura inclui
os zeros escritos. `-00052` para X(6) resulta em '00052 ', enquanto o mesmo
emissor para S9(5) continua sendo -52. O literal 0 resulta em '0     '; o
figurativo ZERO continua preenchendo todo o receptor. O contrato verifica
a correspondência entre dígitos e magnitude numérica. Nenhum consumidor
reanálisa o fonte.
Fonte: IBM elementary MOVE rules e a regra de tamanho de literal numérico
em https://www.govinfo.gov/content/pkg/GOVPUB-C13-d0cd47d3539e1d225361316057506135/pdf/GOVPUB-C13-d0cd47d3539e1d225361316057506135.pdf

## Provenance de COPY e prova de valor

No SP 2.66, `exact=false` indica mapeamento físico aproximado, inclusive COPY
REPLACING. Não invalida uma prova tipada de acesso whole-item e fitting com
binding único e certificado completo. O lower continua verificando tipo,
extensão e resultado; contratos históricos conservam a obrigação anterior.
Oracle: o mesmo MOVE literal para PIC X, com provenance aproximada, produz
a mesma AIR; remover o acesso ou forjar a extensão continua sendo rejeitado.

## Formatação decimal e famílias lógicas

O mesmo FORMATTED_NUMBER atende inteiro para texto e número para texto editado.
O descritor scalarEdit contém segmentos, precisão, escala e extensão verificadas
na fronteira. A tradução usa format_decimal ou integer_digits e conserva a
leitura do emissor. Literais com expoente extremo são reduzidos a um representante
de mesma formatação e mesmo sinal, sem expandir zeros do expoente.

O caminho de escrita da família lógica aplica a formatação antes de atualizar a
raiz e reprojetar os campos. Oracle: editar o campo central de um grupo deve
transformar `L       R` em `L- 12.34R`, preservando ambos os vizinhos. O teste
executa a AIR produzida, além de verificar o transporte e rejeitar descritores
com contagens, precisão, escala ou símbolos incoerentes.

A medição frontend desta ampliação passou 73/73 fontes e reduziu os MOVEs
ainda abertos de 2.370 para 2.207. Ainda falta sua comparação integrada.
A rodada integrada anterior passou 292 etapas e preservou todas as dependências
semânticas das três categorias, 65 PARTIAL e 8 COMPLETE. A prioridade 2 continua
em desenvolvimento: acessos, conversões restantes e representação compartilhada
ainda estão em trabalho.

## Fatias textuais e células locais

SP2.66 publica logicalSlice(data,start,length) em caracteres, com origem zero,
sem wholeItemAccess/logicalWholeItem no mesmo acesso. A admissão verifica binding,
intervalo positivo e inclusão numa família textual ou célula local com prova.
O mesmo tradutor textual faz capture, fitting, atualização da raiz e projeções;
REDEFINES mantém a célula canônica existente. Não há expansão de possibilidades.
LogicalSliceSuite executa a AIR, compara caracteres vizinhos e aliases e rejeita
intervalos inválidos, DATA inexistente e payload histórico com a nova forma.

## Fitting simbólico e identidade

O ajuste textual publica apenas regra, extensão e provenance. O lower usa
fit_text também para literais e conserva os bytes escritos do emissor; não
constrói padding no frontend, no contrato ou na admissão. Certificados antigos
são verificados na entrada em tempo linear no JSON efetivamente fornecido,
sem alocar a extensão declarada. Todos entram no mesmo modelo simbólico.
A prova redundante de resultados literais dos receptores lógicos deixa de ser
produzida; os receptores usam o caminho comum de fitting e atualização da família.
Oracle: PIC X(1000000000) com MOVE 'A' publica uma receita de tamanho constante.
Os exemplos pequenos conservam os resultados escritos à mão e são executados
sobre a AIR. Regras, extensão e campos extras forjados continuam sendo rejeitados.

A identidade de publicação inclui TRUNC, descritores de edição e intervalos
lógicos parciais. O teste RED demonstrou que TRUNC era ignorado. Os testes GREEN
verificam diferenças de identidade para mudanças semânticas nesses três fatos,
incluindo dois produtos admitidos que escrevem em posições diferentes.

Medição wave10: 73 fontes / 292 etapas sem falha; 1.989 MOVEs com obrigações
abertas, contra 2.003 na wave9. A geometria de tabelas fechou mais 14 ocorrências.
A comparação manteve exatamente a diferença de COSGN00C já explicada na wave8;
nenhum novo delta de dependência. Permanecem 150 sites/208 candidatos de programa,
391/378 de arquivo, 259/95 qualificados, 65 PARTIAL/8 COMPLETE e 3.807 usos de
condições 88 / 1.819 SETs.

A regressão do fitting foi composta pela suíte frontend de 1.415 testes (uma
expectativa histórica de campo removido corrigida e reexecutada) e pela suíte
lower até ZeroFill, seguida da suíte ZeroFill corrigida e de todas as execuções
restantes do POM. Os valores esperados não mudaram; os testes passaram a executar
fit_text em vez de exigir Literal. Logs: frontend-full-wave11,
symbolic-fitting-compatibility-green, lower-full-wave39 e
symbolic-fitting-lower-remaining. FAST final continua pendente.

## Onda de views compartilhadas e TRUNC (em andamento)

MOVE passa a consumir descritores numéricos/textuais com limite de memória
materializável sem exigir LOCAL_CELL. O lower mantém UnknownBinding e não
transforma aliases em células independentes. Cópias DATA exigem limites
disjuntos quando faltam coordenadas comuns. Fatias usam o mesmo fitting e
concatenação da tradução lógica; testes independentes verificam vizinhos,
texto sobre número, limite compartilhado e rejeição de sobreposição forjada.

Leituras numéricas compartilhadas conservam guard e ramo opaco para
representação inválida. TRUNC incerto conserva o valor Unknown com sua causa. Esse ramo não inventa destinos locais de
GO TO; conserva continuidade, saída excepcional/externa, memória e recursos
abertos. Controles de PERFORM continuam exigindo a prova específica de célula.

Evidência selecionada: shared-text-trunc-coactupc e shared-text-trunc-copaua0c,
quatro etapas PASS em ambos. MOVE_IDENTITY caiu de 255 para 81 e de 35 para 29
comparado ao frontend wave11. A comparação integrada com wave10 não encontrou
delta de sites, candidatos, remainders, fontes ou declarações de arquivo. Os
dois continuam PARTIAL. Não há nova contagem global; a qualificação final dos
73 fontes permanece pendente. Os 110 MOVEs restantes desses dois fontes têm
72 ocorrências com INPUT_MODEL_STORAGE, além de grupos, acessos e três
conversões de texto editado. As causas se sobrepõem.

IR 61e13365f09bf3fff951ae6f0c63b0d64e2698e1 e AIR
01a466333fa198b8c5c6ad94971f9845db58c74e foram commitados localmente.
AIR FAST passou com o pin novo. Frontend FAST passou na onda compartilhada;
a mudança posterior de TRUNC tem RED/GREEN focal e E2E selecionado. O primeiro
FAST do lower usava AIR antiga; o segundo encontrou seis envelopes de
compilação com scalarInteger:null ainda não migrado. A migração troca apenas
esses 17 campos por scalarNumber:null; QualifiedSourceSuite passou (178 checks).
Os SHAs finais e todos os pins ainda precisam da qualificação de fechamento.

## Escopo de fechamento — orientação de 2026-10-04

Objetivo: conservar dependências e fatos de influência local para o futuro AP V1.
O fechamento não exige zero MOVE_IDENTITY_NOT_PROVEN nem calcular todos os valores.
Preservar funcionalidades corretas já construídas, tipos, reads de valor/endereço,
escrita por receptor, MUST/MAY, aliases/ranges, ordem, identidade e provenance.
Não conceder célula independente ou kill por conhecer apenas tipo ou valor.

Sequência restante finita:
1. Estabilizar o diff e resolver o finding do FAST em HandlerStateSuite com oracle
   de fonte/contrato; revisar a amplitude de NumericMoveControl sem presumir
   validade de representação ou configurar TRUNC implicitamente.
2. Auditar os fallbacks MOVE das famílias admitidas. Onde os fatos provam escrita
   e transformação pura, conservar a causa via Assign/Unknown.dependencies mesmo
   sem valor calculado. Onde faltam endereço, pureza ou efeito, explicitar a
   fronteira. Validar effects e RD focal, overwrite MUST/MAY, aliases/fatias,
   address/index reads e receptores sequenciais. Havoc isolado não prova causa.
3. Congelar e classificar cada MOVE dos mesmos 73 fontes: preciso, abstrato causal,
   input de código ausente ou fronteira residual; distinguir precisão de valor,
   configuração e ausência de acesso/efeito. Não criar novo framework de métricas.
4. FAST dos repos alterados após estabilização, corpus73/292 final comparável,
   deltas de dependências investigados, pins/SHAs consistentes e PRs revisáveis.

Não abrir famílias de edição, codecs ou runtime inválido por contagem de gaps.
Nova precisão exige witness de dependência/efeito/influência que a abstração não
atenda. AP/exporter, solver, controle do checkpoint3 e slicing interprograma
permanecem fora. Missing COPY/SQL INCLUDE/DCLGEN reais seguem explícitos; manifesto
externo de compilação não é requisito. Sem autorização de merge.

Autoridade: artefatos-e2e/move-scope-audit-20261004/REPORT.md e
analysis-product-discovery-20261004/REPORT.md, seções C–F, orientação do usuário.
Validação reutiliza oráculos válidos; execução nova e reutilizada serão separadas.
