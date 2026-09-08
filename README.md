# cobol-lower

O primeiro slice `minimal-entry-goback@1` transforma fatos públicos SP 1.1.0 em `air-java::Publication`: uma Unit, Entry, Sequence e `Return([])`. O adapter de saída 2A publica essa Publication como AIR JSON canônico pelo codec compartilhado. Não reanalisa COBOL nem calcula CFG. O inventário alternativo permanece parcial.

## Usar e verificar

A porta pública `io.github.gustavo2358.lower.application.LowerInput` recebe `SpInput` e `LowerInput.Options`. `EntryGobackLowerer` implementa a mesma admissão usada por memória e arquivo. `LoweringResult` contém status, input observado/diagnósticos, Publication somente em sucesso, correlações tipadas, limitações e relatório integral do AirValidator.

No módulo adapters, `SpJsonDecoder` decodifica bytes e `SpFileInput` lê um Path sob limites explícitos. `FileLowering` compõe reader e porta: falha física não chama o lowerer; um resultado `Lowered` ainda exige examinar o status interno. Caminhos de provenance não são abertos. `CobolLower` compõe esse caminho e `AirFileOutput`, exclusivamente no módulo adapters.

Requisitos do harness: Java21, Maven, Python3 com [dependências](scripts/harness/requirements.txt), Git e gh autenticado para checks Git/PR. Use um diretório temporário isolado para dependências; bootstrap verifica SHA de air-java, digests dos jars de model e codec e testes upstream antes de cada uso posterior:

```sh
export LOWER_BUILD_ROOT="$(mktemp -d /tmp/cobol-lower-build.XXXXXX)"
python3 -m pip install -r scripts/harness/requirements.txt
python3 scripts/harness/run.py bootstrap
python3 scripts/harness/run.py semantic
python3 scripts/harness/run.py performance
python3 scripts/harness/run.py full
```

`full` exige a branch/PR autorizados; no CI publicado usa `--commit "$GITHUB_SHA"`, certificado/trailer e head exatos. Maven sozinho não substitui certificação nem confirmação remota. Os testes Java executam pelo exec-maven-plugin, com marcadores não zero, não por contagem vazia de Surefire. [Gates](docs/engineering/gates.md) explica execução e limites.

## CLI 2A: arquivo SP → arquivo AIR

Depois do bootstrap acima, com Java 21 selecionado, instale o reactor no mesmo repositório Maven isolado:

```sh
mvn -B -ntp "-Dmaven.repo.local=$LOWER_BUILD_ROOT/m2" install
```

Execute da raiz do cobol-lower (o nome lógico do comando é `cobol-lower <semantic-product.json> <air.json>`):

```sh
mvn -q -ntp -f adapters/pom.xml \
  "-Dmaven.repo.local=$LOWER_BUILD_ROOT/m2" \
  org.codehaus.mojo:exec-maven-plugin:3.5.0:java \
  -Dexec.mainClass=io.github.gustavo2358.lower.adapters.cli.CobolLower \
  -Dexec.args="adapters/src/test/resources/sp/cobol-semantic-product.json /tmp/goback.air.json"
```

O `main` chama `System.exit(run(...))`; o processo Maven preserva esse exit code.
Não é necessário fat/uber JAR. Para paths com espaços, também é possível iniciar a classe diretamente:

```sh
mvn -q -ntp -f adapters/pom.xml \
  "-Dmaven.repo.local=$LOWER_BUILD_ROOT/m2" \
  org.apache.maven.plugins:maven-dependency-plugin:3.8.1:build-classpath \
  -DincludeScope=runtime -Dmdep.outputFile=target/runtime-classpath.txt
java -cp "adapters/target/classes:$(cat adapters/target/runtime-classpath.txt)" \
  io.github.gustavo2358.lower.adapters.cli.CobolLower \
  "adapters/src/test/resources/sp/cobol-semantic-product.json" "/tmp/goback.air.json"
```

| Exit | Resultado |
| --- | --- |
| 0 | Publication codificada e arquivo publicado |
| 2 | Argumentos/path inválidos; usage em stderr |
| 3 | Falha física/JSON do SP; code, phase e location do decoder em stderr |
| 4 | Lowering não SUCCESS; status e diagnostics de admissão em stderr |
| 5 | AirJsonException; code e path do codec em stderr |
| 6 | Falha de I/O ao publicar AIR; mensagem em stderr |

Erros esperados não imprimem stack trace. Bugs inesperados propagam e não são classificados
como erro de input. Uma falha pode preservar um arquivo antigo: **o caller deve confiar no exit code**,
nunca interpretar apenas a existência do destino como resultado da execução atual.

`AirFileOutput` obtém todos os bytes por `AirJson.encode(publication)` antes de criar o temporário
no diretório de destino; escreve os bytes exatos e tenta `ATOMIC_MOVE` com substituição.
Somente `AtomicMoveNotSupportedException` ativa fallback `REPLACE_EXISTING`, sem garantia de
atomicidade da plataforma. Não há garantia de durabilidade contra queda de energia nem fsync.
Falhas limpam o temporário; se o filesystem também impedir a limpeza, o erro de limpeza é
preservado como suppressed na exception de I/O, sem transformar a execução em sucesso.
Não cria diretórios pais. Não acrescenta newline, pretty-print, reparse ou fatos AIR.

Defaults operacionais explícitos, iguais aos usados nos testes existentes do slice:
SP 100.000 bytes, profundidade 64, 50.000 nós; admissão 100.000 entidades e 100 diagnostics;
identidades até 1.000.000 caracteres. `ValidationOptions.defaults()` do pin usa profundidade 128,
2.000.000 entidades e 10.000 issues. `AirJson` usa seus defaults: 16 MiB e profundidade 128,
com os mesmos defaults de validação. Limites não fazem parte da identidade, não mudam o perfil,
não elevam PARTIAL a COMPLETE e não convertem interrupção em sucesso. Não há flags neste checkpoint.

A suíte `AirOutputSuite`, chamada pelo Maven e obrigatória em `semantic/full/CI`, percorre a fixture
SP real, verifica bytes contra o codec, decode integral, repetição, falhas e publicação física.
Os oracles anteriores de Entry/Sequence/Return, PARTIAL, claims, origins, uncertainties e correlation
continuam independentes do round-trip. AIR JSON não inclui o relatório externo de lowering; links de
correlação continuam no `LoweringResult` em memória. Report JSON, 2B e E2E completo não foram iniciados.

## Garantias e limites

O positivo real tem [captura/hash fixados](docs/evals/fixture-intake.json); expected AIR manual e oracles de tradução são independentes do lowerer. CP4 executou 779 assertions semânticas, 95 adicionais de custo/limites e full/challenge. [Histórico e certificado final](docs/work/history/WORK-LOWER-001.md) distinguem conclusão local, remoto e review humano.

O [perfil](docs/domain/first-slice-entry-goback.md) exige uma entry primária/start/assinatura zero conhecidos e um GOBACK único. Inputs maiores são rejeitados integralmente, não filtrados. UNKNOWN/partial/input missing nunca viram zero, nop ou sucesso silencioso. Coverage parcial e dimensões não disponíveis atravessam em AIR. O checker estrutural não certifica a tradução nem perfil AIR completo.

PublicationId usa XXH3-128 completo incremental dos fatos canônicos, 32 hex minúsculos, com limite aplicado ao ID final. IDs locais derivados usam `local-xxh3-128-v1`, 32 hex e registro de colisões por publicação. SourceKeys ainda mantêm tokens integrais. JSON é materializado sob limite de bytes, não streaming; métricas estruturais não são SLA nem instrumentação de internals Jackson/JDK. Outras famílias só têm forma física/ocorrência comum reconhecidas: não há alegação de validação semântica integral de MOVE/IF/CALL/DATA. Coordenadas/include sites insuficientes geram limitações tipadas, sem inventar localização. [Identidade/provenance](docs/domain/identity-and-provenance.md), [resultados](docs/domain/validation-and-results.md).

## Navegar

[AGENTS.md](AGENTS.md) é a entrada para agentes; [arquitetura](ARCHITECTURE.md), [trabalho](docs/work/index.md), [índice](docs/index.md), [source lock](docs/sources/sources.lock.json) e [capacidades futuras](docs/domain/capability-matrix.md) orientam contexto. Domínio usa o contrato interno e AIR compartilhada; a aplicação também usa hash4j fixado para identidade. Adapters dependem das portas internas. `analysis-ir` governa a semântica, `air-java` fornece modelo/validator.

WORK-LOWER-001 foi autorizado em modo multi-checkpoint CP0..CP5. O PR termina em review humano, sem merge/auto-merge. WORK-LOWER-003/2A foi mergeado. O WORK-LOWER-004/PR5 foi mergeado. WORK-LOWER-005 autoriza IDs locais compactos; análise CFG e E2E cross-repo continuam fora deste trabalho.

IDs locais derivados usam `local-xxh3-128-v1` (XXH3-128 incremental, 32 hex), com
registro de colisões por publicação. SourceKeys ainda são integrais.
Política, limites e provas em [WORK-LOWER-005](docs/work/active/WORK-LOWER-005/spec.md).
