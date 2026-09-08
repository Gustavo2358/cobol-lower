# Oracle independente e classes

Fixture real AIR-MOVE: 1 Unit/Object/Cell/Entry/Sequence, [Assign];Return([]),
ObjectPlace VALUE_WRITE ligado por DataId, Literal TextValue(PROGA) VALUE_READ;
Object/Cell known(text), PERSISTENT/PRIVATE. Origins literal/target distintos,
DataLink/OperandLink e ambos statements no mesmo Label; Publication PARTIAL.
Validator sem issues, shared codec round-trip igual, bytes repetidos iguais.

Negativos obrigatórios: scalarText/logicalValue/wholeItem ausentes, domínio não TEXT,
binding não RESOLVED, selected ausente/mismatch, não FULL_IDENTITY, continuação
UNAVAILABLE/dangling/ciclo, sem GOBACK/terminal diferente/statement extra/DATA ausente.
Metamorfismo: programPoint/arrays/nomes/lexemas não decidem ordem, valor ou joins.
Novos fatos consumidos alteram PublicationId; dados não semânticos não o alteram.

Escala: SP físico real de 60k linhas; N/2N DATA+MOVEs (1000/2000), 1 DATA/10000
MOVEs em memória, encode default limitado explicitamente e caso menor transportável.
Cardinalidades e contadores de índices/traversal, bytes/tempos observados.
16 challenges do pedido: successor por programPoint, ignorar FULL_IDENTITY,
wholeItem ausente, mismatch, Object por referência, Cell por MOVE, join por nome,
role, literal UNKNOWN, DataLink, literal/target origins, Halt, duas Sequences,
PublicationId sem fatos novos e local IDs expandidos. Compilação falha não conta.

## Oracles da remediação

B1: mismatch, forged extent, NUMERIC+logicalValue, extent negativo/zero DATA
→ INPUT_ERROR; A😀B extent3 aceita fisicamente, extent4 recusa; vazio extent0
aceita fisicamente; logicalValue=null materializa ausência e não emite Assign.
B2: corpus SP real >100KB passa no default CLI e preserva cardinalidades/shared
validator/round-trip. SP real1DATA/10k passa decoder/admission e retorna CODEC
IMPLEMENTATION_LIMIT, preservando arquivo e sem temp. Bytes/nodes/entities acima
dos novos limites rejeitam. Medir bytes/nodes/visits antes de escolher constantes.
Novos desafios: ignorar B1 equality/codepoints, regredir três limites antigos,
remover probe, aceitar INPUT como esperado, aumentar codec e novo codec/streaming
fora do escopo. Semântico requer compile RED, restore exato e segundo GREEN;
escopo usa guard de diff/arquitetura. Gates integrais anteriores preservados.
