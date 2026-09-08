# Checkpoint 4C — handoff

Baseline cobol-lower: `f9e74ec3404efe830992d9535becca847ace80e8`, main limpa,
fetch/switch/pull ff-only confirmado. Work item
[WORK-LOWER-006](../../work/active/WORK-LOWER-006/work-item.yaml), CP0 interno único.
Branch `feat/lower-scalar-move`; commit final resolve pelo trailer
`Checkpoint-Evidence: docs/quality/WORK-LOWER-006/CP0.json`. CI será consultado no
SHA publicado e recebido no PR/handoff externo; não há autoinscrição recursiva.

4A proleap-poc merge `2815e805fd3a9ef4762a39ab9435260fc76da0e8` (PR32).
4B air-java merge `ce530a7e17ab12b23c48f29425f503ff920b09fb` (PR6).
Merge e head pré-merge têm árvores iguais em ambos; o pin usa o merge real.
AIR normativa permanece `122ce54e1b9ef9b00646f93ece409ca8b63bc933` / 2.0.0.
Binding analysis-ir-json 1.0.0 DRAFT; Maven conjunta 0.1.0-SNAPSHOT.

SP real: `cobol-semantic-product` 1.2.0, 5177 bytes,
SHA-256 `468e3207f578e428ace89a311eadbd6e27c670331b739675b761479352adc7af`.
Fonte AIR-MOVE.cbl intacta do merge; captura em checkout isolado e
[comandos/digests](../../evals/fixture-intake.json), sem editar output.

[AIR produzida](actual.air.json): **32138 bytes**, SHA-256
`dd3bb4819282e609a97937ea01b7e202786e2c2d9ba9c8a1821a8b982eeb8788`.
PublicationId `77ae4cc3ed75332c64011efce81c431b`.
1 Unit, 1 Object, 1 Cell, 1 Entry, 1 Assign, 1 Sequence, 2 operações e 2 operandos.
Sequence=[Assign];Return([]). AirValidator issues=[]; shared encode/decode preserva
o model inteiro; duas execuções CLI têm bytes iguais. A tabela usa localId para
leitura; identidades completas incluem essa Publication e unit=`unit`, e operands
incluem o OperationOwner do Assign. O runtime preserva links tipados e sourceKeys.

| SP | AIR outputs |
| --- | --- |
| `data:0` | `object d277a147621da9e9d6d7e52986bbb8ee`; `storage 2eee2eced5889f75096c488505d6ba70` |
| `operand:0:0` | `operand b03927c6498457ea98dfb8539b197ce8` |
| `operand:0:1` | `operand 2365b2b2267995949d82bca22891396d` |
| `statement:0` | `operation e52226b01b4afc8f6c6214aafd63e5ab`; `label ebe036f3af2349d52967a6a63d32e97f` |
| `statement:1` | `operation 5ed7c27833651ab320a3b56d90ed923c`; `label ebe036f3af2349d52967a6a63d32e97f` |
| `entry:0` | `entry c4de8a797697cb8ddf3012f2248da01f` |

CP3: output 13827 bytes, SHA-256
`46919c1429db4aa310e66fc9df9374eeba53fd98e50a287fdd005c17622f33ad`,
PublicationId `a5fce8cae9328bc4007fc589e1989e37`, byte-identical ao aprovado no
WORK-LOWER-005. [Execuções CLI](cli.json); fixture 1.1.0 permanece intacta.

## Escala e complexidade

| Probe | DATA | MOVE | Objects/Cells | Assigns | Visitas | Referências | AIR bytes |
| --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| Model N | 1000 | 1000 | 1000/1000 | 1000 | 24016 | 7003 | não codificado neste probe |
| Model 2N | 2000 | 2000 | 2000/2000 | 2000 | 48016 | 14003 | não codificado neste probe |
| Shared model | 1 | 10000 | 1/1 | 10000 | 190021 | 70003 | não codificado neste probe |
| Codec N | 250 | 250 | 250/250 | 250 | — | — | 4766996 |
| Codec 2N | 500 | 500 | 500/500 | 500 | — | — | 9521746 |
| Shared codec | 1 | 1000 | 1/1 | 1000 | — | — | 11212321 |
| Physical 60012 lines | 1 | 1 | 1/1 | 1 | — | — | 32237 |

Observação inicial model N/2N/shared: 206/240/743 ms, aproximadamente;
codec N/2N incluindo lowering+round-trip: 348/590 ms. Não são SLA nem medidas
de pico de heap. Reexecuções variam; logs brutos conservam cada observação.
O SP físico tem 5339 bytes e mantém source spans acima da linha60000 sem IDs
proporcionais ao texto. Probes maiores de model são separados do transporte.

Indexing O(D+S+R+P), DATA translation O(D), statement traversal O(S), assembly
O(D+S+P). IDs O(B) nos bytes semânticos alimentados incrementalmente, O(D+S) para
largura fixa. Radix DATA tem quatro passes de domínio int; maps têm custo amortizado.
Referências medidas 7N+3; challenge scan→índice prova o oracle de custo estrutural.
Sem scans globais por MOVE/continuação, nomes como join ou índices/origins reconstruídos.

Identidade escalar versiona o domínio/preimage para SP1.2 e preserva local-xxh3-128-v1,
32 lowerhex, papéis separados e collision registry local. DATA/valores/provas/next/
provenance novos entram no hash; PIC, programPoint, legacy value e scopes de readiness
não. SourceKeys escalares usam namespace compacto. CP3 permanece byte-identical.

## Limites e verificação

A CLI mantém SP max100000 bytes/depth64/50000 nós. AirJson mantém **16 MiB/depth128**.
Uma AIR model-only válida com 1 DATA/2500 MOVEs excede o codec default: prova pelo
seam público existente da composição CLI retorna exit5/IMPLEMENTATION_LIMIT;
zero chamadas de temp, destino anterior intacto, output novo ausente e nenhum
resíduo. Nenhuma API/flag pública de limite ou codec de 512 MiB foi adicionada.
Falhas de I/O conservam cleanup CP2A. O writer não converte byte[] em String.

17 [challenges escalares](scalar-challenges.json) incluem todos os 16 exigidos e
scan quadrático. Todos requerem AssertionError semântico, compilação concluída,
restore byte a byte e segundo GREEN. Os desafios históricos também permanecem
parte de full. Primeiro RED wire: 1.2.0 UNSUPPORTED_CONTRACT. Primeiro RED model:
precise input recusado pelo perfil antigo. Erro inicial de accessor Java no teste
foi setup/compilação e não conta como RED semântico.

Gates locais/CI são registrados no [certificado](CP0.json); certificar não é
aprovação humana. O review é self-review explícito. Nenhum merge/auto-merge.
Publication/Unit PARTIAL, GOBACK com dimensões não provadas, INITIAL/value ausente,
sem alias/storage byte-level geral, IF/CALL/CFG/dataflow/Possible Values.
Nenhum repositório irmão é alterado. 4D e 4E não iniciados.

## Input contract for Checkpoint 4E

Executar no checkout cobol-lower do head deste PR, com Java21, Maven e build root
isolado. O bootstrap resolve o source pin; a instalação usa a mesma m2:

```sh
export LOWER_BUILD_ROOT="$(mktemp -d /tmp/lower-4e-consumer.XXXXXX)"
python3 scripts/harness/run.py bootstrap
mvn -B -ntp "-Dmaven.repo.local=$LOWER_BUILD_ROOT/m2" install
mvn -q -ntp -f adapters/pom.xml \
  "-Dmaven.repo.local=$LOWER_BUILD_ROOT/m2" \
  org.apache.maven.plugins:maven-dependency-plugin:3.8.1:build-classpath \
  -DincludeScope=runtime -Dmdep.outputFile=target/runtime-classpath.txt
java -cp "adapters/target/classes:$(cat adapters/target/runtime-classpath.txt)" \
  io.github.gustavo2358.lower.adapters.cli.CobolLower \
  /absolute/path/semantic-product.json /absolute/path/air.json
```

O comando java real foi executado nas duas fixtures deste checkpoint;
[recibo](cli.json) conserva cwd/classpath/exit/hash. Para reproduzir somente 4C,
substituir o input por `adapters/src/test/resources/sp/scalar-move-1.2.0.json`.
No 4E o SP deve vir do frontend real 1.2.0, não deste snapshot como substituto.

Oracles mínimos do 4E: exit0, arquivo inteiro decodificável pelo shared AirJson,
AirValidator issues=[], 1 Object/Cell known(text), PERSISTENT/PRIVATE, 1 Entry,
1 Sequence [Assign(ObjectPlace VALUE_WRITE, Literal TextValue(PROGA) VALUE_READ)];
Return([]), 2 operações/2 operandos, owners e referências fechados. Confrontar
SP handles pelos links/sourceKeys e origens, preservar PARTIAL/uncertainties;
repetição determinística. Hash/PublicationId acima só são expected se todos os
fatos semânticos e provenance do input forem idênticos ao fixture congelado.
Não extrapolar normalContinuation nem generalizar MOVE. Este handoff não executa 4E.
