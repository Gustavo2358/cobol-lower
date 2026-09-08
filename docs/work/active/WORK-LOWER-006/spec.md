# Checkpoint 4C — contrato congelado antes do código

Autoridade: SP 1.2.0 (proleap-poc merge 2815e805fd3a9ef4762a39ab9435260fc76da0e8),
AIR 2.0.0 (analysis-ir 122ce54e1b9ef9b00646f93ece409ca8b63bc933), modelo/codec
4B merge ce530a7e17ab12b23c48f29425f503ff920b09fb, coordenada 0.1.0-SNAPSHOT.
Fontes lidas: SP docs/domain/scalar-text-move.md e docs/evals/checkpoint-4a.md;
AIR 03 §§1–3, 04 §2/§8; air-java docs/engineering/air-json.md e
quality/air-json-scalar-assign.md. São fatos publicados, não inferências de COBOL.

Regra: scalarText(TEXT, extent positivo, WORKING_STORAGE, LOCAL) justifica
Object known(text), CellBinding e Cell PERSISTENT/PRIVATE por declaração.
Não há valor inicial nem modelo de bytes/Region/View. FULL_IDENTITY, literal
logicalValue TEXT, wholeItemAccess e selected RESOLVED iguais justificam Assign.
Validação de coerência pode comparar extensões tipadas, nunca tamanho da String/PIC.
Move handler gera instrução; Goback handler gera Return([]), sem Halt/Jump.

Admissão separada exige uma entry primária conhecida com assinatura zero/ABSENT,
DATA escalares e pelo menos um MOVE, uma cadeia explícita acíclica cobrindo todo
inventário e terminando em GOBACK. Índices de input únicos, percurso por start/next,
visitados limitados pelo inventário: termina ou rejeita ciclo/dangling/extra.
Não há filtragem de publicação. Fatos ausentes, incoerentes e fora do profile
rejeitam antes da publicação, inclusive porta em memória. Gaps independentes
compatíveis são preservados; desconhecimento de uma prova necessária bloqueia.
Readiness textual, nomes, PIC, lexemas, ordem estrutural e programPoint não decidem MOVE.

Compatibilidade: wire 1.1.0 estrito permanece; 1.2.0 tem shape próprio explícito,
não defaults que transformem shape antigo. CP3 preserva revisão e bytes.
Scalar profile usa novo domínio de PublicationId incremental cobrindo fatos
consumidos e identidades/provenance; ignora texto COBOL não consumido. LocalIds
compactos/versionados e registro de colisão existentes conservados, novos papéis
separados por domínio. Correlação DATA/Object/Cell, statement/op/label e operandos.
Publication/Unit PARTIAL, GOBACK com limitations dimensionais existentes.

Complexidade: indexing O(D+S+R+P), DATA O(D), traversal O(S), assembly O(D+S+P),
IDs O(B) nos bytes semânticos consumidos incrementalmente (O(D+S) para fatos de
largura fixa). Maps amortizados, sem scans por MOVE. Não alegar SLA/heap sem medir.

Saída: AirValidator antes de SUCCESS; adapter usa shared AirJson.encode bytes
no writer atômico existente. Defaults 16 MiB/depth128 preservados. CLI exit5
AIR codec IMPLEMENTATION_LIMIT quando encode exceder; antes de criar temp, sem
output parcial. Erro de I/O continua exit6 e cleanup. Sem API pública nova de limite.

## Blocker remediation — contrato autorizado antes da correção

Mesmo CP0/4C, WORK-LOWER-006, feat/lower-scalar-move e PR7; review humano do
head 24742514410d938063149a4f466e81a29801adac reportado pelo usuário. PR reviews=[]
na API não invalida a autoridade do review fornecido nesta sessão.
B1: boundary 1.2.0, depois de shape e antes de Materialize, rejeita INPUT_ERROR
quando logicalValue presente não tem kind ALPHANUMERIC, source.value igual ao
valor lógico, domínio TEXT ou extensão igual a codePointCount. Scalar extent >0;
logical extent >=0. Não reparar input, interpretar COBOL ou transportar legacy
value ao core. TextValue upstream aceita Unicode; teste suplementar fica no
contrato físico, sem ampliar o profile COBOL ASCII. Ausência permanece ausência.
O metamorfismo anterior que alterava somente source.value estava errado e sua
correção foi explicitamente autorizada; fixture legítima e política AIR intactas.
B2: limites internos centralizados, bounded, medidos no SP real 10k do merge4A;
bytes <=64MiB, depth64 preservado, nodes/entities com headroom documentado.
Caminho padrão arquivo→decoder→lower→writer deve ultrapassar o teto SP antigo
e alcançar sucesso (<16MiB AIR) ou o limite AirJson esperado no corpus10k.
Limites acima do novo teto continuam rejeitando atomicamente. Sem flags públicas,
novo parser/codec, streaming ou alterações em core/air-java. D1/D2 são backlogs
planejados não autorizados, sem work item, sem implementação/discovery.

Medição real upstream10k: 18809400 bytes, 1050154 JSON nodes, profundidade8 com
folhas, 190021 admission entity visits. Defaults escolhidos: 32MiB/64/1500000
nodes e250000visits; margens78.4%/42.8%/31.6%. Production success corpus400: SP
1071738bytes/56119nodes, ambos acima dos limites físicos anteriores.
