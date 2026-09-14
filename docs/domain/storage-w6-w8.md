# Storage ST-W6–ST-W8

W6.1 traduz SP2.9/storage1.2: RENAMES é uma relação de faixa provada pelo
frontend (regra IBM em proleap-poc/docs/domain/storage-w6-w8.md). Lower verifica
closure, registro/base, endpoints, extensão e codec publicados; não resolve
nomes COBOL. Projeta ViewBinding sobre Region existente com origem da relação.
Inventário/ranges finitos; mapas e ancestry memoizada; sem tabela de pares.
Negativos: alvo ausente, range alterado, segunda base, gaps contraditórios, wire
incompleto/desconhecido. Oracle: uma região6, alias[0,6), tail[3,6), duas origens.

W6.2 traduz SP2.10: acesso com slice usa offsets absolutos explícitos e
RegionSlice da AIR, inclusive Read de target CALL. Admission prova bounds
contra a view declarada. Reader mantém contratos 2.7–2.9 intactos e recusa
coordenadas não canônicas antes de materializar. Não há aritmética COBOL no
lower. Oracle: write e CALL em [3,6), dentro de item [2,8); 5 negativos de
bounds, zero e wire lexical. Complexidade O(1) por acesso indexado.

W6.3 traduz SP2.11 e suas transferências ordenadas para Assign/FitText/Read,
CopyBytes ou HavocMust. Padding/truncation são fatos tipados explícitos; literal
ajustado conserva fonte lógica para validação. Admission valida cada par e usa
índices ordenados de intervalos para excluir alteração da origem por qualquer
receiver, O(n log n), sem teto de cardinalidade. IDs de operandos/operações e
proveniência são próprios por transferência. O encoding de identidade inclui
agora RENAMES, slices e transferências; regressões mostram mudança de ID quando
um slice ou a proveniência RENAMES muda. AIR passou a validar o fit regional
provadamente total e transportar fit_text existente.
