# WORK-LOWER-005 CP0 — self-review

Revisor: mesmo agente Codex implementador, segunda passagem de diff, sem aprovação
humana ou review independente. Identidade do candidato: campo único candidate_diff_sha256
na evidência CP0; digest final será recalculado após todos os artefatos de prova.

Escopo de produção: quatro classes application; LocalIds privado ao package; nenhuma
mudança de porta pública, frontend, AIR, CFG, codec, CLI, pins ou gates. hash4j continua
restrito a CanonicalRevision pela mesma regra arquitetural. token e sourceKey preservados.

Tupla explícita fixa namespace/papel/proprietário/chave e framing de UTF-16, seed0,
32 hex high64/low64. PublicationId incorpora POLICY. O registro é por publicação e
compara as strings originais, sem copiar tokens. Colisões são rejeitadas antes do retorno;
constantes são lexicalmente disjuntas. Repetir ArtifactId de mesmo papel/filename é válido.
Todas as criações variáveis e referências usam os IDs novos; nenhum prefixo variável
hex reversível permanece em localId. SP handles ainda identificam ocorrências, com
renomeação pública testada. Nenhuma seleção/filtragem/ordenação de fatos adicionada.

Oracles: golden antigo provém do PR5 e coincide byte a byte com baseline E2E somente
leitura. Comparador percorre todos os records, owners e referências; exige bijeção e
igualdade de todos os campos não-ID. Spans, nomes lógicos, include chain, coverage,
claims/gaps, Entry/Return e links são observados também pelas suítes cumulativas.
Vetores libxxhash independentes precedem código; testes não calculam expected por LocalIds.

Finding resolvido: prova inicial de handle longo na porta violava contrato kind:n/int.
[Correção registrada](oracle-correction.json) preserva teste/FREEZE anterior e prova
limites reais, sem mudar admissão. Uma prova adicional de renomeação pública fortaleceu
as quatro relações de ocorrência antes das falsificações; nenhum esperado foi afrouxado.
O runner temporário de desafios herdava filtro apenas AssertionError; para apagamento
de papel, ampliado somente à IllegalStateException LOCAL_ID_COLLISION prevista, emitida
pela produção antes de construir saída. Harness permanente e seus gates intactos.

Limites: hash não criptográfico; sem registro global entre publicações. SourceKeys
continuam integrais, incluindo crescimento de tempo/memória. 100k chaves distintas e
texto de 1M code units não certificam programa de 100k instruções. Sem ganho de heap/CPU
alegado. Invariantes estruturais de hash não são perfilador do lowering inteiro.

Revisão final conferiu full, sete falsificações restauradas, segundo GREEN, diff
com novos arquivos e docs/evidence/state antes da certificação. Nenhum finding
bloqueante de produção encontrado nesta passagem. PR/humano/CI não inferidos de testes locais.
