# Blocker remediation — revisão do candidato

Self-review do implementador, não independente nem aprovação humana. A única
identidade do candidato é CP0.json#/candidate_diff_sha256. Base revisada:
24742514410d938063149a4f466e81a29801adac; mesmo WORK-LOWER-006/CP0/branch/PR7.

B1 foi confrontado com CobolSemanticProduct.java do merge4A: ScalarText>0,
TextValue.logicalExtent=codePointCount, LiteralSource impõe kind e igualdade de
valor quando logicalValue existe. O novo método em SpJsonDecoder é uma passagem
linear por DATA/statements e pelo texto lógico. Roda depois da validação física,
antes de Materialize, e lança a mesma PhysicalShape→INPUT_ERROR. Null legítimo é
preservado; nenhum valor é reparado ou enviado ao core por conveniência.
O metamorfismo de source.value isolado estava incorreto e foi substituído por
rejeição, por autorização expressa do review. PIC/readiness/programPoint seguem
sem uso semântico. Unicode suplementar é teste do tipo físico upstream, sem
ampliar o profile COBOL. CP3 e o fixture4C válido permanecem byte-idênticos.

B2 altera somente a composição padrão, usando ProductionLimits interno. Tetos
32MiB/1500000nodes/250000visits derivam do corpus real de 18809400bytes,
1050154nodes e190021visits; depth64 permanece. ProductionPathSuite usa arquivos
upstream reais congelados, a composição CLI padrão e o shared codec/validator.
400 DATA/MOVEs excedem bytes/nodes antigos e geram AIR íntegra; 10k sobre DATA
único atravessam SP/admission e param no codec16MiB com destino preservado.
Contracasos mantêm tetos de bytes/nodes/entities e rejeição atômica.

Produção em core, índices, IDs, claims, origins, Materialize, SpFileInput e
AirFileOutput não mudou. O guard de diff preserva esses bytes e rejeita classes
novas fora dos três arquivos de produção autorizados. Os desafios de escopo
compilam antes de avaliar a violação; não contam erro de compilação como RED.
As asserções externas do harness exigem execução dos probes, inclusive o probe
10k no performance; retornar INPUT como expected não satisfaz o oracle do arquivo.
Os desafios históricos e escalares continuam obrigatórios em full.

Diff integral revisado: código/novos arquivos, testes, snapshots/digests, scripts,
FREEZE e registro de autorização no mesmo CP, docs e backlog. BACKLOG-LOWER-017
(D1) e018(D2) são planned/NOT STARTED, NOT AUTHORIZED, work_item=null, sem discovery.
Não houve alteração do source lock, API/codec AirJson, flags públicas ou siblings.
Memória continua com representações integrais O(N) coexistentes; não há medição
nem claim de peak heap, e não se afirma readiness E2E para grandes programas.

Tentativas preservadas: Maven upstream exigiu download de dependência; primeiro
mutante AirJson tinha aridade errada (setup, não RED); primeiro full parou em
whitespace do backlog. Oracles de saída não foram relaxados para resolver falhas.
Logs brutos são preservados sem normalização, em gzip reversível com hashes.
Gates e CI só recebem PASS com a execução correspondente concluída.

Resultado local: full PASS, incluindo os nove novos desafios compiláveis e
restaurados. Após esse full, apenas o teto temporal do workflow foi elevado de
15 para 30 minutos (11m35s locais mais bootstrap remoto); policy/docs revalidados.
Todos os gates permanecem. Arquivos dos siblings são idênticos; o HEAD de
analysis-cfg avançou externamente para merge PR11 com a mesma árvore, registrado
no recibo read-only. Nenhum source pin mudou.

A execução CI34265242563 no commit2282308 encontrou uma dependência implícita de
cache apenas no executor novo: `mvn -pl adapters` exigia core/test-jar instalados.
Reproduzida a falha em m2 temporária sem nenhum artefato lower, mantendo somente
as dependências fixadas. A correção prepara o reactor core com install a partir
da cópia isolada atual antes dos desafios; não pula testes, não muda oracles,
produção ou dependências upstream. A nova certificação exige full nesse ambiente.

Full nesse ambiente inicialmente sem lower PASS: todos os gates, nove novos
desafios, 17 escalares e históricos, sem alteração dos oracles. O diff adicional
foi revisado: dez linhas do executor para preparação explícita e evidência/docs;
nenhum arquivo de produção mudou após2282308. Certificar e verificar novo commit
antes de push normal; o CI anterior falhado não é promovido a PASS.
