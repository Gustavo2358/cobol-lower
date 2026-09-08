# Oracles CP0 — XXH3-128

Preservar LoweringSuite.observe e VerticalSuite: Entry/Sequence/Return, PARTIAL, claims,
uncertainties, provenance, spans, links SP→AIR, arquivo/memória e invariância a JSON físico.
Substituir somente o esperado da identidade, agora 1718623a5d9fc4b5db0124dc15913f58 para a
fixture original. Oracle calculado por libxxhash 0.8.3, seed zero, sobre fatos canonical-v1 da
baseline com novo domínio. Vetores independentes vazio, abc e bytes 0..255 repetidos quatro
vezes verificam algoritmo, todos os 128 bits, padding e ordem high64/low64. Não são gerados
por hash4j ou pelo lowerer. Fixture SP original e golden semântico intactos.

CompactIdentitySuite: instâncias independentes, mesmo namespace em 73 ocorrências tipadas,
32 caracteres; limite 31 falha, 32 admite; campos/política/proveniência de LoweringSuite;
campos adjacentes, Unicode/surrogates, token inalterado. Proxy de teste observa putBytes do
stream privado em fato longo e rejeita buffers >256, conta todos os bytes canônicos e uma
finalização, exige estado interno limitado. Reflexão verifica ausência de texto retido;
self-review verifica ausência de materialização local intermediária. Sem API configurável
para hashing em produção, benchmark de heap ou alegação de suporte a programas maiores.

AirOutputSuite preserva codec/CLI e exige 15720 bytes = 398532 - 73*(5276-32). Comparação
local completa normaliza somente namespace e exige cada outro byte idêntico. Sem gerar CFG.
Falsificações omitem policy.version, alteram domínio/seed/framing e excedem buffer; GREEN
antes, RED pela causa correta, restauração e segundo GREEN. Full conserva challenges
anteriores e testa fronteira hash4j restrita; fixture de lifecycle fixa cenário unmerged
explicitamente, mantendo negativo de merged. Não alterar oracles após falha sem novo contrato.
