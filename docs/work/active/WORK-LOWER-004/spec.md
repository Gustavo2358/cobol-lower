# CP0 — PublicationId compacto XXH3-128

Nova autorização explícita da sessão (2026-09-07 America/Sao_Paulo): implementation,
single-checkpoint CP0, branch/commits/push/um PR e parada para revisão humana. Atualização
explícita durante a execução substitui SHA-256 por XXH3-128, autoriza hash4j no core e
ajustes mínimos de lock/gates. Mesma branch/CP0/work item; fase SHA anterior não certificada
registrada em [observação](../../../quality/WORK-LOWER-004/superseded-sha256-observation.json).
Checkout direto E2E; main limpa/fetch/pull ff-only confirmaram baseline
`a12907d61dbad619bdda48dee16f8ae6a63460db`, sem avanço. Não herdar autorização anterior.

PublicationId = XXH3-128 completo, seed 0L, secret padrão da biblioteca, 32 hex minúsculos
sem prefixo, zeros iniciais preservados. Ordem textual: 16 hex de high64 seguidos dos
16 hex de low64 (ordem canônica xxHash). HashValues.toHexString(HashValue128) fornece isso;
toByteArray usa outra ordem e não integra o contrato. Nada de XXH64/XXH3-64 ou hash truncado.

Preimagem ASCII `minimal-entry-goback@1/AIR2/SP1.1/xxh3-128-v1/` seguido dos mesmos fatos
e ordem de canonical-v1: string = comprimento UTF-16 decimal, `:`, quatro hex por code unit;
números/enum/flags como strings; listas precedidas de contagem, optionals por presença.
Traversal tipado fixa a ordem dos campos, domínio/versões integram o hash. Sem normalização
Unicode, JSON bruto, dependência de ordem incidental, metadados ou opções operacionais.
A porta valida o mesmo SpInput vindo de memória ou arquivo antes de construir a identidade.

A política integral injetiva e a proposta SHA-256 são substituídas por identidade de conteúdo
não criptográfica. Assume-se boa dispersão e baixa probabilidade de colisões acidentais em
128 bits; não resistência criptográfica ou impossibilidade matemática de colisões.
maximumIdentityCharacters mede apenas os 32 caracteres finais: 31 falha com
IMPLEMENTATION_LIMIT/IDENTITY_LIMIT sem Publication; 32 admite sem teto sobre blob canônico.
Limites de admissão/validação continuam separados. token preserva resultados e limite anterior;
IDs locais/SourceKeys/labels/Return/claims/PARTIAL/uncertainties/origins/spans/links não mudam.

Algoritmo: Hashing.xxh3_128(0L).hashStream() por encode, buffer fixo de 256 bytes para emitir
fatos via putBytes(buffer, offset, length), get() uma vez e HashValues.toHexString. Tempo O(B),
memória auxiliar O(1) no hash, sem String/StringBuilder/byte[] proporcional à revisão.
Contagens finitas e traversal existente garantem terminação; strings pequenas de números são
limitadas por int. Nenhuma afirmação de memória constante do lowering inteiro ou CPU medida.

Dependência: com.dynatrace.hash4j:hash4j:0.30.0, tag v0.30.0 / commit
cb49f4525ee965080a72d3dd9f61af4a31fdb31c. Apache-2.0, Java puro, sem JNI ou dependências
transitivas no POM. Artefato multi-release, base Java11 e implementação Java21. Não compilar
biblioteca própria nem reimplementar xxHash. Pins AIR/SP/CFG existentes intocados.
G-ARCH admite só esse GAV e tipos Hashing/Hasher128/HashStream128/HashValue128/HashValues
para CanonicalRevision; proíbe exposição na API pública. Não liberar com.* nem java.security.

Fontes primárias verificadas em 2026-09-08 UTC:
[hash4j v0.30.0](https://github.com/dynatrace-oss/hash4j/tree/cb49f4525ee965080a72d3dd9f61af4a31fdb31c),
README, LICENSE, build.gradle (release 11/21), sources jar de Maven Central (Hashing,
HashStream128, HashValue128, HashValues, XXH3Base: buffer interno fixo 264 bytes), e
[POM publicado](https://repo.maven.apache.org/maven2/com/dynatrace/hash4j/hash4j/0.30.0/hash4j-0.30.0.pom).
API declara equivalência a [xxHash v0.8.3](https://github.com/Cyan4973/xxHash/tree/v0.8.3).
Vetores independentes executados via libxxhash 0.8.3 nativa somente em verificação local;
Java de produção/testes permanentes não depende de C/JNI. Sem medir throughput/heap.

Correção de fixture do harness: testes de autoridade de remediação precisam fixar sua
premissa unmerged em cópia temporária; registry real foi reconciliado como merged. Nenhum
predicado de autorização nem evidência histórica é afrouxado para conservar o positivo.
Sem benchmark novo, E2E cross-repo, alterações no pai ou plano amplo de refatoração.
