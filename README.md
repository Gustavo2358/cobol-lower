# cobol-lower

O primeiro slice `minimal-entry-goback@1` transforma fatos públicos SP 1.1.0 em `air-java::Publication`: uma Unit, Entry, Sequence e `Return([])`. Não reanalisa COBOL, não calcula CFG e não publica AIR JSON. O inventário alternativo permanece parcial.

## Usar e verificar

A porta pública `io.github.gustavo2358.lower.application.LowerInput` recebe `SpInput` e `LowerInput.Options`. `EntryGobackLowerer` implementa a mesma admissão usada por memória e arquivo. `LoweringResult` contém status, input observado/diagnósticos, Publication somente em sucesso, correlações tipadas, limitações e relatório integral do AirValidator.

No módulo adapters, `SpJsonDecoder` decodifica bytes e `SpFileInput` lê um Path sob limites explícitos. `FileLowering` compõe reader e porta: falha física não chama o lowerer; um resultado `Lowered` ainda exige examinar o status interno. Caminhos de provenance não são abertos. Não há CLI de produto.

Requisitos do harness: Java21, Maven, Python3 com [dependências](scripts/harness/requirements.txt), Git e gh autenticado para checks Git/PR. Use um diretório temporário isolado para dependências; bootstrap verifica SHA de air-java, digest do jar e testes upstream antes de cada uso posterior:

```sh
export LOWER_BUILD_ROOT="$(mktemp -d /tmp/cobol-lower-build.XXXXXX)"
python3 -m pip install -r scripts/harness/requirements.txt
python3 scripts/harness/run.py bootstrap
python3 scripts/harness/run.py semantic
python3 scripts/harness/run.py performance
python3 scripts/harness/run.py full
```

`full` exige a branch/PR autorizados; no CI publicado usa `--commit "$GITHUB_SHA"`, certificado/trailer e head exatos. Maven sozinho não substitui certificação nem confirmação remota. Os testes Java executam pelo exec-maven-plugin, com marcadores não zero, não por contagem vazia de Surefire. [Gates](docs/engineering/gates.md) explica execução e limites.

## Garantias e limites

O positivo real tem [captura/hash fixados](docs/evals/fixture-intake.json); expected AIR manual e oracles de tradução são independentes do lowerer. CP4 executou 779 assertions semânticas, 95 adicionais de custo/limites e full/challenge. [Histórico e certificado final](docs/work/history/WORK-LOWER-001.md) distinguem conclusão local, remoto e review humano.

O [perfil](docs/domain/first-slice-entry-goback.md) exige uma entry primária/start/assinatura zero conhecidos e um GOBACK único. Inputs maiores são rejeitados integralmente, não filtrados. UNKNOWN/partial/input missing nunca viram zero, nop ou sucesso silencioso. Coverage parcial e dimensões não disponíveis atravessam em AIR. O checker estrutural não certifica a tradução nem perfil AIR completo.

IDs incluem revisão canônica completa, sem hash, com limite explícito de tamanho. JSON é materializado sob limite de bytes, não streaming; métricas estruturais não são SLA nem instrumentação de internals Jackson/JDK. Outras famílias só têm forma física/ocorrência comum reconhecidas: não há alegação de validação semântica integral de MOVE/IF/CALL/DATA. Coordenadas/include sites insuficientes geram limitações tipadas, sem inventar localização. [Identidade/provenance](docs/domain/identity-and-provenance.md), [resultados](docs/domain/validation-and-results.md).

## Navegar

[AGENTS.md](AGENTS.md) é a entrada para agentes; [arquitetura](ARCHITECTURE.md), [trabalho](docs/work/index.md), [índice](docs/index.md), [source lock](docs/sources/sources.lock.json) e [capacidades futuras](docs/domain/capability-matrix.md) orientam contexto. Domínio/aplicação dependem somente do contrato interno e AIR compartilhada; adapters dependem das portas internas. `analysis-ir` governa a semântica, `air-java` fornece modelo/validator.

WORK-LOWER-001 foi autorizado em modo multi-checkpoint CP0..CP5. O PR termina em review humano, sem merge/auto-merge. Nenhuma capability ou backlog subsequente foi iniciado.
