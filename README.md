# cobol-lower

**Harness documental v1 — 6 de setembro de 2026.** Este pacote prepara o repositório do produtor COBOL da Analysis IR. Não contém Java, POM, scripts, workflows executáveis, codecs ou implementação de lowering. JSON/YAML neste pacote são registros e contratos do harness, não payloads de produção.

O objetivo do produto é transformar fatos públicos do COBOL Semantic Product em uma `Publication` AIR rastreável, determinística e conservadora quando necessário. A aplicação não é outro parser COBOL, não calcula CFG e não implementa dataflow.

```text
proleap-poc → Semantic Product → cobol-lower → air-java::Publication → analysis-cfg
                  arquivo hoje       │                 │
                  memória depois     └─ adapters externos de entrada/saída
```

## Começar

Para agentes, [AGENTS.md](AGENTS.md) é a entrada. Para humanos, leia [arquitetura](ARCHITECTURE.md), [primeiro slice](docs/domain/first-slice-entry-goback.md) e [trabalho](docs/work/index.md). O [índice](docs/index.md) organiza o restante por assunto; não é uma lista de leitura obrigatória.

O primeiro trabalho de implementação está **preparado, não autorizado**, em [WORK-LOWER-001](docs/work/proposals/WORK-LOWER-001/spec.md). Seus checkpoints têm objetivos, testes, gates e condições de parada. Criar este harness não autoriza executá-los. Nenhum work item de implementação está ativo.

## Primeiro resultado demonstrável

Para um Semantic Product 1.1.0 de entry primária conhecida, sem parâmetros/retorno explícito, cujo único statement suportado seja GOBACK, produzir `Publication → Unit → Entry → Sequence → Return([])`. A fixture deve ser real, produzida pelo frontend fixado, e o esperado AIR deve ser independente do lowerer.

Essa admissão estreita é um perfil inicial explícito, não um filtro que escolhe o primeiro statement e esquece os demais. O modelo será plural; entradas maiores ainda não suportadas serão diagnosticadas integralmente, não truncadas. O inventário de entries alternativas continua parcial.

## Decisões consolidadas

- `analysis-ir` contém somente a especificação normativa; `air-java` fornece o modelo Java e seu validador compartilhados.
- O núcleo recebe valores tipados próprios da sua porta de entrada e devolve AIR compartilhada. Não recebe arquivo, JSON, AST ou classes internas do frontend.
- Adapters decodificam/transportam; regras de lowering pertencem ao domínio. Interfaces são definidas de dentro para fora, não como wrappers de Jackson ou filesystem.
- O arquivo de entrada inicial é `cobol-semantic-product.json`. A saída AIR em arquivo é parte do roadmap, por adapter externo; o primeiro slice valida AIR em memória antes desse transporte.
- O binding AIR JSON existente é DRAFT. Uma implementação experimental poderá testá-lo em checkpoint próprio, antes da promoção normativa; não há dependência circular exigindo que seja aprovado sem testes.
- Os repositórios podem continuar separados mesmo quando um integrador futuro os compuser como módulos Maven. Agregação de build não exige fusão de histórico nem acoplamento dos núcleos.

## Garantias e limites desta entrega

Os [invariantes](docs/architecture/invariants.md) têm evals e gates correspondentes. Os [gates](docs/engineering/gates.md) estão especificados, **não implementados**. Seu estado inicial é `SPECIFIED_NOT_IMPLEMENTED`; ausência de executor nunca equivale a PASS. A checagem documental realizada sobre o ZIP está descrita em [validação do pacote](docs/quality/harness-validation.md), sem alegar testes de software inexistente.

Os snapshots e fontes consultados estão no [source lock](docs/sources/sources.lock.json). O [mapa de absorção](docs/sources/handoff-integration.md) distingue o que veio do handoff, das decisões da conversa e das escolhas deste harness. A [matriz de paridade](docs/sources/proleap-harness-parity.md) registra o que foi aproveitado do ProLeap e o que não se aplica a um lowerer.

## Colocar no repositório

Copie o conteúdo desta pasta para a raiz do checkout de `cobol-lower`. O nome externo da pasta é livre; os links são relativos. Use `AGENTS.md` com essa capitalização, sem criar um segundo `agents.md`. Confira arquivos existentes antes de sobrescrever, adote o harness em commit revisável e só depois autorize o primeiro checkpoint. Nenhum caminho local ou nome de diretório irmão é pressuposto.
