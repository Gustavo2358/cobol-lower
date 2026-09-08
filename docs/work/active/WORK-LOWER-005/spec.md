# CP0 — IDs locais limitados

Autorização explícita delegada em 2026-09-08 pela tarefa de origem
01a07ed2-f918-7cc3-8e22-b202ce75d9f4: um CP0 implementation completo, branch/PR,
certificação, commit/push/CI, checkout direto e entrega ao E2E sem esperar aprovação.
Sem merge/auto-merge, próximo checkpoint ou E2E cross-repo. Base main ff-only
1a456e7673df0e3357f157e0249d74aef12bdf63, PR5 merged com reviews=[].

Política `local-xxh3-128-v1`. Derivação de localId: XXH3-128 completo, hash4j 0.30.0
Java puro, seed0, secret padrão, 32 hex minúsculos high64/low64, zeros preservados.
Preimagem: ASCII literal `minimal-entry-goback@1/AIR2/local-xxh3-128-v1/`, seguido
por quatro strings (namespace, papel, proprietário, chave) nessa ordem. Cada string
é comprimento UTF-16 decimal + ':' + quatro hex minúsculos por code unit, emitidos
incrementalmente. Não normalizar Unicode nem substituir lone surrogates. Contagem
fixa de campos e framing tornam a entrada inequívoca, inclusive vazio e delimitadores.

| Namespace | Papel | Proprietário | Chave |
| --- | --- | --- | --- |
| label | goback-sequence | unit | handle statement |
| operation | goback-return | unit | handle statement |
| entry | primary-entry | unit | handle entry |
| origin | source/original/expanded (três papéis) | statement ou entry | handle |
| origin | sequence | unit | handle statement |
| artifact | original ou expanded | publication | filename lógico |
| uncertainty | unproved | localId de operation já registrado | nome da dimensão |

A publicação qualifica o universo da tabela; `unit` é a única unidade selecionada
admitida. O proprietário não é inferido de conteúdo. Handles de ocorrências diferentes
continuam diferentes; conteúdo GOBACK idêntico não é chave de operação. IDs constantes
unit, unit-origin e entry-inventory/alternate-not-projected podem permanecer constantes;
são disjuntos lexicalmente dos 32 hex. Todos os IDs derivados passam pelo registro.

PublicationId mantém os fatos canônicos existentes e acrescenta `local-xxh3-128-v1/`
ao domínio anterior: `minimal-entry-goback@1/AIR2/SP1.1/xxh3-128-v1/local-xxh3-128-v1/`.
Assim, mesma revisão SP em políticas locais distintas não reutiliza o namespace.
Não é garantida estabilidade longitudinal entre revisões. SourceKeys conservam token
integral e nomes/handles SP, incluindo seu custo residual proporcional ao texto.

Registro por publicação: digest → tupla original exata de quatro referências String.
Reuso de tupla igual é legítimo (inclusive ArtifactId); digest igual de tupla distinta
lança IllegalStateException com marcador LOCAL_ID_COLLISION antes de retornar Publication.
Não armazenar token, concatenação ou preimagem expandida no registro. Sem API pública
configurável de hashing: teste injeta colisão somente em estado privado por reflexão e
executa o mesmo caminho interno de tradução usado pela porta pública.

Preservação: atualização consistente de definições e referências tipadas, scopes,
claims, coverage, uncertainties, origins, artifacts, entries, labels e links SP→AIR.
Nenhuma mudança de semântica, spans, logicalName, SourceKeys, include chain, gaps ou
limites de admissão/AIR. Slice N>1 continua UNSUPPORTED_SLICE com inventário preservado.
Hash não criptográfico; colisão entre publicações distintas não tem registro global.
Não alegar impossibilidade matemática de colisão ou resistência a adversário criptográfico.

Algoritmo finito, O(B) por texto/framing hashado e O(K) registros para K identidades,
armazenando referências já recebidas e IDs de 32 caracteres. Comparação exata do reuso
pode examinar o texto original, sem cópia. Buffer de alimentação de 256 bytes e estado
hash4j limitado; memória transitória de cada hash O(1), sem token/preimagem integral.
Não há teto oculto de texto canônico. Custos do modelo, validator e SourceKeys são
separados; não se promete heap constante do lowering completo nem SLA de CPU.

Fontes primárias: reuso da fonte fixada SRC-HASH4J e verificação local de
Hashing.java (xxh3_128(long), equivalência XXH3_128bits_withSeed v0.8.3),
HashValues.java (toHexString: high64/low64, 32 caracteres) e XXH3Base.java (buffer fixo)
do sources jar 0.30.0 previamente verificado no WORK-LOWER-004. Revisão
cb49f4525ee965080a72d3dd9f61af4a31fdb31c; consulta remota indisponível nesta sessão,
bytes locais e source lock preservados. [API](https://github.com/dynatrace-oss/hash4j/tree/v0.30.0).
Isso sustenta reutilizar algoritmo/representação; framing/política acima são decisão local.
Rejeitados UTF-8 com substituição de surrogate, truncamento, hash do conteúdo GOBACK,
concatenação sem framing e registro de tokens expandidos. Sem novas dependências.

Vetores calculados antes do código por libxxhash nativa (apenas ferramenta de prova),
[registro independente](../../../quality/WORK-LOWER-005/reference-vectors.json).
Expected da revisão usa os fatos canonical-v1 publicados na baseline antiga, substituindo
somente o domínio; nunca é produzido pelo novo encoder. Golden AIR anterior congelado
em adapters/src/test/resources/air, comparação por bijeção independente de IDs.
