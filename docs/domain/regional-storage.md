# Storage Semantics — tradução regional ST-W3–ST-W5

Status: implementação autorizada pela campanha ST-20260913-01, ainda sem
qualificação M1. WORK-STORAGE-LOWER-001; revisão humana ao final de W5, sem merge.

## Autoridades fixadas e fronteira

SP 2.7 do frontend transporta fatos de StorageLayoutSemantics/StorageAccessSemantics
sob `ibm-enterprise-6.4-fixed-display-1047@1`. O contrato fonte está em
`proleap-poc/docs/domain/storage-semantics.md`; os commits e hashes consumidos estão
no source lock. W0 verificou as autoridades IBM Enterprise COBOL 6.4 e IANA CP1047
1.00; W1 qualificou AIR Region/View/ConstantRegionSlice/CopyBytes e o codec opcional
`text.ebcdic.ibm1047@1`. Norma AIR 2.0.0 e binding JSON 1.0.0 DRAFT permanecem.

O lower não calcula layout COBOL. SpInput recebe inventário físico, referências
regionais e MOVE com kind/bytes/gaps, preservando todas as versões históricas do
reader. Wire27 é fechado: chaves ausentes/desconhecidas, enum inválido, null
indevido e coerção numérica são erros físicos. Medidas usam decimal canônico em
string ou null com motivo; unknown não é zero. A API em memória recebe os mesmos
fatos e valida as mesmas invariantes sem passar pelo JSON.

## Regra e algoritmo

Indexar nós/bases/views/declarações e referências uma vez. Validar namespace,
duplicatas, closure, pais acíclicos, ordem de irmãos única, bounds, concordância
nominal, ambiente/codec e provas de separação. Não somar filhos nem reconstruir
PIC. A base de uma view é a base publicada, não o nome DATA. A validação literal
compara a sequência publicada ao resultado de MemoryCodecs da AIR: extensão
correta com texto/bytes contraditórios ainda é entrada inválida.

Traduzir uma base física para uma Region de lifetime PERSISTENT e visibilidade
PRIVATE quando a prova ordinária local de WORKING-STORAGE existe; não trocar a
semântica persistente legada por ativação. As vistas textuais provadas geram
objetos TEXT com binding View, offset/extent/codec explícitos. O mesmo componente
compartilha a Region. Não criar Cell por nome para simular precisão física.
Lacunas fora do perfil conservam diagnóstico. As relações disponíveis definem a
topologia positiva da abstração; uma lacuna não concede efeito de compensação.
A projeção geral de tipos sem representação permanece limite descrito na W1.

LITERAL_BYTES vira escrita BYTES em ConstantRegionSlice. COPY_BYTES vira
CopyBytes com captura anterior à escrita, extensões iguais e disjunção comprovada
no SP; a regra AIR de overlap não amplia a regra de MOVE COBOL. MUST_UNKNOWN,
quando descreve transformação não implementada do producer MOVE, vira Nop
diagnóstico, sem substituir o valor conhecido. Fitting escalar já provado continua traduzido;
readiness parcial e provenance de COPY não são substituídas por provas inventadas.

As operações recebem origins da declaração/base/view e dos operands/statement.
A identidade inclui os fatos físicos e é canônica para permutação dos inventários
não ordenados semanticamente. Retornar correlação DATA→objeto→base não alega alias
exato de interpretações: mesmo storage não é mesmo valor/codec. Bases distintas
são independentes no modelo, sem geração de DisjointStorage.

Terminação segue de inventários finitos com validação iterativa de pais; custo
O(n + referências), mais ordenação canônica O(n log n), sem matriz de pares.
AirValidator valida a saída. A validação AIR não substitui o oracle de tradução.

## Classes de oracle e limites

Antes de precisão: JSON real do produtor fixo e SP manual independente, bounds,
referência pendente, medida contraditória, bytes errados, ambiente ausente,
independência removida, cópia sobreposta, fonte desconhecida e permutação física.
Positivos: grupo→filho, FILLER intermediário, grupos aninhados, mesma seleção
qualificada, várias ocorrências 1/2/5/N e equivalência memória/arquivo.

VALUE regional, OCCURS geral, RENAMES, ref-mod geral, CORRESPONDING e regras
numéricas de representação permanecem fora desta campanha. Overlays fonte são
introduzidos em W4; W3 não fabrica esse suporte. CALL lê o alvo antes de Invoke;
efeitos externos e destinos de runtime continuam desconhecidos. RD/values e
interpretação de nomes de programa pertencem a analysis-cfg.


## Checkpoint de reader e admissão — W3.2-A1

Implementados StorageFacts e campos aditivos de SpInput, Wire27 fechado,
materialização tipada e RegionalStorageAdmission na validação comum. O índice
pertence ao input exato e é preparado uma vez, sem caches globais por IDs.
Validação inclui bytes iguais em comprimento mas contraditórios ao texto, usando
o codec AIR independente. Decimal malformado retorna INPUT_ERROR; não escapa
como exceção. BaseId é opaco, sem derivação de NodeId pelo número do handle.

StorageIdentityFacts inclui todos os campos físicos na identidade e ordena os
inventários físicos por handles para neutralizar sua permutação; parent/order
continuam campos semânticos explícitos. Ausência das novas facts preserva a
codificação de identidade histórica. Uma mudança de extent da base altera a
publicação; permutação de nodes/views não a altera.

Quatro JSONs reais SP 2.7, com os respectivos COBOLs e hashes, estão em
`adapters/src/test/resources/sp/storage-27`. Foram capturados pela CLI no commit
frontend eeb5d55; o pin subsequente da5a658 corrige somente a validação de IDs
opacos, docs e teste, com writer idêntico. O source lock fixa os SHAs completos,
trees e digests. AIR Java 9906193 foi construído em Maven isolado contra a norma
a928791, sem reaproveitar um SNAPSHOT de outro checkout.

FAST após identidade: 2340 verificações de core, leitores históricos, 22 casos
regionais e guardas de arquitetura/harness, sem falhas. A suíte regional integra
o FAST fixo. A tradução física ainda não está implementada neste checkpoint:
aceitar SP 2.7 com fallback conservador não qualifica group→child nem M1.


## Tradução física e operações — ST-W3.2

RegionalDataTranslator recebe o índice preparado na admissão, sem reconstruí-lo.
Cada base tem no máximo uma representação física: Region para bytes provados ou
para extensão desconhecida com alocação ordinária provada; Cell abstrata apenas
para escalar legado isolado cuja representação física não foi publicada. A Cell
representa esse componente, sem uma Region duplicada. A admissão rejeita prova
escalar standalone em nó aninhado/grupo/componente compartilhado, comprimento
textual contraditório e prova de independência legada entre views da mesma base.
A topologia publicada usa identidades positivas de bases e relações compartilhadas;
o lower não transporta uma obrigação negativa de DisjointStorage.

A AIR não tem lifetime desconhecido. Uma base de extent desconhecido e alocação
UNPROVEN conserva gap de alocação, sem fabricar PERSISTENT/EXTERNAL. Base conhecida
sob o perfil ordinário ou alocação explícita prova persistência; visibilidade é
PRIVATE somente com prova local, UNKNOWN nos demais casos. Perfil UNSPECIFIED
conserva o caminho escalar antigo. Esses limites ficam na cobertura; não justificam efeito substituto.

Objetos textuais usam ViewBinding IBM1047, com provenance de DATA/nó/view/base.
FILLER tem cobertura física e origem sem objeto nominal. Extent/offset desconhecidos
conservam motivos com escopo da base representada; não geram View precisa. MOVE
literal produz RegionSlice IdentityBytes e BytesValue; cópia produz CopyBytes com
fallback conservador restrito às bases fonte/destino e continuação explícita.
MUST_UNKNOWN do MOVE omitido produz Nop diagnóstico, sem havoc local. Operações precisas
podem ser reutilizadas nas ativações PERFORM existentes, sem novo modelo de controle.

O fitting escalar continua uma Assign TEXT quando a prova legada é válida e o
resultado é codificável no codec declarado. Um resultado lógico contendo `€`, por
exemplo, não vira AIR inválida nem bytes da JVM: a transformação fica na cobertura. Isso
vale também para acesso apenas legado quando há fatos físicos explícitos. Leitura
escalar de cópia não é usada em View: CopyBytes exige a prova regional; ausência
dessa prova conserva fallback. CALL usa Read da view selecionada antes de Invoke,
sem inferir candidato nem política de nome. MEMORY_REGIONS e IBM1047 são required
somente quando presentes na saída. AirValidator e AirJson permanecem autoridades.

Oracles executáveis adicionais: quatro fixtures CLI, entradas independentes na
porta em memória com 1/2/5/40 filhos, permutação byte-idêntica de inventário físico,
FILLER/offset 6, escrita desconhecida somente em [6,14), fitting codificável e não
codificável, e grupo misturado com escalar INT abstrato. Positivos atravessam
encode/decode/encode AIR. Nenhum teste do lower afirma resolver CALL: M1 depende
do domínio regional de valores e da qualificação integrada em analysis-cfg.

Gate de tradução `w3-lower-translation-fast-01`: PASS, 106,874s, 2340 checks
core e suites históricas, 25 casos regionais de closure, oracles de tradução e
checks de arquitetura/harness. Mutantes da wave e vertical M1 ainda pendentes.

## Decisão W4.2: relações explícitas SP 2.8, sem reanálise de REDEFINES

O produtor fixo adiciona `storage/relations` em SP 2.8.0/storage 1.1.0.
Wire28 deve reutilizar os tipos Wire27 dos campos inalterados, com envelope
fechado próprio e inventário obrigatório de relações. Reader 2.7 continua
aceitando exatamente storage 1.0.0, sem permitir o campo novo silenciosamente.
A porta em memória recebe id/owner/target/status/provenance/gaps. Status PROVEN
significa início e base compartilhados; não significa mesma interpretação.
UNPROVEN conserva alvo ausente e motivo. Os fatos pertencem ao frontend;
nenhuma busca por nome, REDEFINES textual, PICTURE ou soma de footprints no lower.

A admissão verifica referências e coerência de parent/order/base/start. O alvo
provado deve ser irmão anterior, portanto ciclos não são admitidos. Não exige
igualdade de extent ou codec por compartilhar localização. AliasBinding não é
necessário: objetos continuam ViewBindings sobre o único representante da base.
Provenance individual da cláusula vira source origin e cobertura. A origem derivada
da base inclui as relações que explicam seu compartilhamento; cada objeto herda
a origem da própria declaração, nó, view e base, inclusive relações de FILLER.
Relação não provada recebe gap explícito, inclusive se a base não for representável.

Identidade canônica ordena relações por ID e inclui todos os seus campos;
inventário vazio não muda o encoding de publicações históricas 2.7. Custos são
O(n + relations), mais ordenação canônica; nenhum walk por objeto ou matriz de
pares. Oracles prévios: JSON real do frontend, base única em root/chain/FILLER,
leitura e escrita inversas, extent menor/maior, origem da relação alcançável,
JSON fechado/negativos em memória e round-trip AIR. Codec distinto sobre mesmos
bytes é contrato AIR-only e continua testado no consumidor language-agnostic.

Implementação W4.2: Wire28 compartilha exclusivamente os records imutáveis da
parte inalterada de Wire27; validação física precede projeção tipada/materialização.
Core valida relações e inclui todos os campos na identidade. Region e views
incluem origem derivada da alocação/relações; cobertura guarda cada cláusula,
inclusive FILLER. Unproved/unrepresented geram gaps próprios, sem perda quando
não existe Region AIR representável. A soma de tamanhos nunca entra no lower.

Challenge de consistência encontrou inventário com relação UNPROVEN e falsa
independência de alocação; também era possível alegar escalar standalone apesar
da relação aberta. As duas contradições agora são rejeitadas em memória, sem
assumir que o produtor é confiável. Cinco SPs CLI reais atravessaram AIR com
round-trip semântico e byte-exato; dez negativos JSON, onze negativos de memória,
permutação e identidade por alvo de relação passaram nos testes focais.
