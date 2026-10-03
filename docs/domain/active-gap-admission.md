# Admissão de gaps atuais — SP 2.63

O frontend publica somente os gaps ativos. A versão 2.63.0 conserva os fatos de
2.62 e permite que a prova atual satisfaça uma obrigação antes representada por
um diagnóstico de perfil antigo. O decoder admite a versão exata; versões futuras
continuam rejeitadas. Os leitores históricos permanecem fechados.

`EntryGobackAdmission` aceita ausência de gap localizado somente com a capacidade
correspondente: NO_OP com controle local fechado, PERFORM com invocação e retomada
provadas, ou capacidade tipada suficiente com membership provado. INPUT_MISSING
continua exigindo diagnóstico. Containment UNKNOWN exige gap estrutural ou prova
atual de membership. As validações semânticas de cada fato continuam obrigatórias.

`DiagnosticEvidence` percorre as dependências transitivas das provas e rejeita
prova ausente, parcial ou mera possibilidade. O helper valida somente os fatos
publicados; não resolve COBOL nem usa contagem vazia como prova de execução.

A fixture `adapters/src/test/resources/sp/active-gaps/active-gaps.json` foi gerada
pelo frontend do PR #86 a partir do `.cbl` adjacente. `ActiveGapsSuite`, incluída
no FAST, verifica AIR válido, incertezas preservadas e seis mutações negativas.

A mudança afeta diagnósticos e identidades derivadas da publicação. Não acrescenta
instruções AIR, candidatos, transferências ou suporte de linguagem.

## Validação e pin

- Produtor real: `ed4830e41689e05001468fe8d4cf9ffcfb87207f` (frontend PR #86), com
  paths e SHA-256 registrados em `docs/sources/sources.lock.json`.
- `lean.py fast`: PASS, incluindo `ActiveGapsSuite`, contratos de versões
  anteriores, admissão negativa, arquitetura e 21 testes do harness.
- `lean.py qualification-local`: PASS nas suites semânticas, capacidade,
  desempenho e arquitetura. A restrição final para statements genéricos foi
  validada depois por FAST e todo o corpus.
- CardDemo: 73 fontes, 292 etapas, zero falhas. Todas as categorias de dependências,
  candidatos, suportes, origens, precisão e estados foram preservados.

[Relatório detalhado, normalizações e hashes](https://github.com/Gustavo2358/proleap-poc/blob/ed4830e41689e05001468fe8d4cf9ffcfb87207f/docs/validation/active-gaps.md).
O corpus usa os pins E2E registrados nesse relatório; o FAST usa o pin AIR do
source lock deste repositório. Não se confunde essa evidência com completude da
linguagem: 65 análises permanecem PARTIAL e oito COMPLETE no corpus.

## Fronteira das versões históricas

Até SP 2.62 inclusive, um statement não MODELED precisa conservar um gap
localizado; containment UNKNOWN precisa conservar um gap de STRUCTURE. Essas
obrigações do wire continuam valendo mesmo quando a topologia traz prova positiva.
A dispensa por prova atual pertence ao contrato 2.63.0.

O decoder preserva a versão recebida antes de normalizar documentos históricos.
Após materializar os fatos, valida as duas obrigações antigas para toda versão
admitida diferente de 2.63.0, antes de devolver SpInput. A aplicação e o domínio
continuam independentes da versão JSON. Nenhum fato ou diagnóstico é fabricado.
A verificação indexa os statements com gaps e os com STRUCTURE, com custo
O(statements + gaps) e memória O(statements); não percorre novamente as provas.

O oracle usa dois produtos reais do mesmo `historical-gaps.cbl`: frontend
`c43b1410ac9235d906ccc90392d5f3bf82399ac1` (2.62) e
`ed4830e41689e05001468fe8d4cf9ffcfb87207f` (2.63). Ambos devem ser admitidos.
Remover o gap de NO_OP, as restrições antigas de PERFORM, o gap único de MOVE
aninhado ou somente STRUCTURE de um NO_OP que ainda tem CAPABILITY deve ser
rejeitado em 2.62 e admitido em 2.63 com a prova preservada. Um gap de outra
categoria não substitui STRUCTURE no contrato histórico.

### Validação da correção de versão

O finding foi reproduzido no head `de71cbbf4edfaf53d28ee54afca51af3ee523c4b`:
as quatro mutações e a troca da tag 2.63 para 2.62 eram aceitas indevidamente.
`GapVersionBoundarySuite`, incluída no FAST, agora exige INPUT_ERROR no decoder
nesses cinco casos. Os dois produtos reais intactos e os quatro equivalentes
2.63 com prova positiva continuam gerando AIR válido. `ActiveGapsSuite` também
passou, preservando as seis rejeições anteriores de perda de prova/versão futura.

- FAST local após a correção: PASS, incluindo contratos, arquitetura e harness.
- CLI com NO_OP 2.62 sem gap: exit 3 / SP INPUT_ERROR, sem artefato publicado.
- E2E selecionado: CBACT01C, COSGN00C e COBSWAIT, cada um
  com o SP histórico e o atual; seis casos e 18 etapas, sem falhas. Os contratos
  históricos exercitados foram 2.58, 2.62 e 2.56; os três atuais são 2.63.
- Trinta produtos comparados: AIR, fonte qualificada, bundle, CFG e dependencies.
  Igualdade do JSON completo, excluindo apenas métricas operacionais de dependencies.
  Todos os estados, candidatos, suportes, origens, precisão e remainder permanecem.

Os logs, comandos exatos, hashes e outputs dessa revisão ficam preservados no
workspace em `.gap-reconciliation-20261003/active-evidence/historical-gap-review/`.
O fonte e os dois produtos mínimos estão versionados em
`adapters/src/test/resources/sp/active-gaps/historical-gaps*`.
A instrução CICS fica em um parágrafo separado para selecionar SP 2.62 no produtor
histórico sem tornar inexata a proveniência do PERFORM usado no contracaso.

A qualificação completa e o corpus de 73 fontes citados acima são evidência
anterior, não execuções desta correção. O novo código apenas rejeita wires
históricos que violam as duas obrigações flexibilizadas; não altera a tradução
nem os fatos de entradas válidas. SP 2.63 segue o mesmo caminho anterior. Por
isso foram reexecutados os contratos afetados, FAST e o E2E selecionado; full e
corpus integral não foram repetidos. O frontend e o pin do produtor não mudaram.
