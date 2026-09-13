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
Lacunas fora do perfil conservam representação/evidência aberta e efeitos
conservadores; ausência de layout nunca autoriza independência.

LITERAL_BYTES vira escrita BYTES em ConstantRegionSlice. COPY_BYTES vira
CopyBytes com captura anterior à escrita, extensões iguais e disjunção comprovada
no SP; a regra AIR de overlap não amplia a regra de MOVE COBOL. MUST_UNKNOWN
vira havoc obrigatório sobre o footprint comprovado. UNAVAILABLE mantém efeito
conservador com gap, sem Nop. Fitting escalar já provado continua traduzido;
readiness parcial e provenance de COPY não são substituídas por provas inventadas.

As operações recebem origins da declaração/base/view e dos operands/statement.
A identidade inclui os fatos físicos e é canônica para permutação dos inventários
não ordenados semanticamente. Retornar correlação DATA→objeto→base não alega alias
exato de interpretações: mesmo storage não é mesmo valor/codec. DisjointStorage
só é publicado a partir da prova explícita de alocação, sem closure transitiva.

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
