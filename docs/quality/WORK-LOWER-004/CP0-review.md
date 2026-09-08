# Self-review CP0

Revisor: Codex, mesmo contexto do implementador; não é revisão independente nem humana.
Candidato identificado exclusivamente por `candidate_diff_sha256` na evidência CP0.
Revisão integral do diff base a12907d6 → index: produto, testes, docs, arquivos novos,
remoção do pacote ativo antigo e registry/history; evidence/state revisados separadamente.

PublicationId: XXH3-128 inteiro, hash4j 0.30.0, seed zero, domínio xxh3-128-v1, representação
high64/low64 fixa com padding. Vetores da referência nativa confirmam vazio/abc/1024 bytes
e fixture. Não se usa hashCode, clock, JSON bruto, 64 bits ou truncamento. Cada encode cria
stream próprio; não há estado global de identidade, JNI, nova configuração pública ou
implementação manual do algoritmo.

O diff mantém input/list/optional/unit/provenance/readiness/claim e traversal dos campos.
word emite comprimento decimal e hex UTF-16 diretamente ao buffer de 256 bytes; append
só recebe domínio, números e delimitadores pequenos. encode/input não chamam token nem
montam String/StringBuilder/byte[] da revisão. finish só materializa os 128 bits finais.
O único StringBuilder é o token local anterior, com seu limite integral preservado. A
biblioteca usa stream de estado limitado (fonte fixada XXH3Base buffer de 264 bytes).
Esta é prova estrutural + observação executável de streaming, sem medição de heap/CPU.

Limite final 31/32 e fato longo com limite 32 exercitados, sem truncamento. SourceKeys,
IDs locais, origins, links, Entry/Sequence/Return/PARTIAL/claims/uncertainties/spans continuam
cobertos por LoweringSuite e VerticalSuite. Fixture original preservada. AIR inteiro da
CLI permanece idêntico à baseline ao substituir apenas as 73 ocorrências do namespace:
398532 → 15720 bytes. Não há resultado CFG nem E2E cross-repo nesta revisão.

Hash4j: GAV exato no core e gate Maven; somente cinco tipos de hashing no owner
CanonicalRevision, sem API pública dependente da biblioteca. Probes de bytecode aceitam
uso interno e rejeitam exposição pública; tipos de outro package/biblioteca são rejeitados.
Source lock só acrescenta hash4j, preservando todas as entradas/pins anteriores. Codec,
AIR model, frontend, schema SP, CLI e todos os adapters de produção não mudaram.

Lifecycle: PR4 e PR2 MERGED com reviews=[] reconciliados como not_recorded. Históricos
CP0..CP5/R1 anteriores byte-idênticos; o pacote WORK-LOWER-003 está recuperável no Git
mergeado. WORK-LOWER-004 mantém nova autorização, um CP0 e um PR, sem merge/auto-merge.
A fixture de authority explicita unmerged em cópia temporária; o negativo merged continua
obrigatório. Nenhum predicate de execução foi enfraquecido.

Findings resolvidos: política antiga do README; dependências JDK/hashing excluídas pelo
gate; fixture de lifecycle acoplada ao registro remoto; ordem da nova assertion de tamanho
antecipava o oracle de newline. Este último ajuste apenas restaura prioridade do oracle
anterior, conserva expected exatos e renova o hash FREEZE do teste. O segundo full comprovou
todos os desafios anteriores novamente (exit0). Nenhum finding bloqueante remanescente.

Evidência da tentativa SHA-256 anterior é factual e marcada superseded, não certificada
como XXH3-128. Testes/oracles finais usam XXH3-128; hashes SHA-256 do harness são mantidos.
A revisão humana do PR completo é o próximo passo externo, ainda não concedida.

Revisão do delta de publicação após e0568298e94aa02efcd27731e70ffd16d92c4efc:
PR real #5 vinculado a manifesto corrente/registry/evidence/state; snapshot FREEZE anterior
ao PR permanece intocado (PR null permitido pelo lifecycle). Produção, testes, oracles,
full.log e contratos imutáveis idênticos ao primeiro commit. Docs/Git repetidos; nenhum
novo finding. CI do commit final é obrigação pós-push, sem antecipar seu resultado.
