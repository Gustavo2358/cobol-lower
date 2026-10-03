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
