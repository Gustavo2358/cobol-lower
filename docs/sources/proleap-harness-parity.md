# Matriz de paridade com o harness ProLeap

Fonte examinada: SRC-HARNESS no source lock. Este pacote foi inspirado nas garantias, não copiou literalmente a implementação nem declarou que scripts existentes lá existem aqui. “Tudo que há lá” significa preservar mecanismos pertinentes e explicar exclusões, não transplantar parser/grammar/tests do frontend.

| Mecanismo observado | Aplicação no cobol-lower | Estado inicial |
| --- | --- | --- |
| AGENTS roteador e leitura progressiva | AGENTS + docs/index + must_read por checkpoint | Documentado |
| Arquitetura curta + contratos detalhados | ARCHITECTURE, boundaries, ports, dependency matrix | Documentado; DIP explicitado |
| ADRs e invariantes por ID | decisions e invariants com catálogos | Documentado |
| Cinco arquivos por work item | Manifesto/spec/plan/eval/state em propostas/active | Templates e proposta prontos |
| Backlog não autoriza execução | Registry/index e estados com autorização separada | Documentado |
| Promoção de conhecimento e remoção de active | Closure no mesmo PR e histórico curto | Documentado |
| Lifecycle local versus remoto | G-DOCS offline versus G-GIT contextual | Especificado; não executável no checkout |
| Gates docs/architecture/fast/semantic/performance/full | Mesmos papéis + transporte/integração por perfil | Especificados; executores futuros |
| Source/bytecode architecture gate | Imports, API e dependências; source sozinha não basta | Especificado |
| Regra→classes→oracle→implementação | Testing/evals/first-slice oracle | Especificado |
| Corpus como evidência, não spec | Golden genuíno, synthetic rotulado, expected independente | Documentado; golden não capturado |
| Metamorfismo e mutation focal | Catálogo, falsification e evidence template | Especificado |
| Performance por propriedade, não hardware | Contadores/índices/limites e diagnóstico de truncamento | Especificado |
| Política semântica e premissas | Fontes oficiais/papers; algoritmo geral/preservação | Documentado; sem heurística produtiva |
| Impacto downstream em oito classes | Mesmos nomes e earliest broken layer | Documentado |
| Provenance, ambiguity e unknowns | Contratos de IDs/coverage/lifetime/diagnósticos | Documentado |
| Observabilidade separada | Logs/metadata operacional fora de semântica | Documentado |
| Política de prompt pequeno | Autorização/delta/resultado + routing | Documentado |
| Grammar/ANTLR/parser/symbol/resolver gates | Não se aplicam: o lowerer é boundary-only | Exclusão deliberada |
| HTML/Node/source normalizer E2E | Não se aplicam ao primeiro produto do lowerer | Substituídos por JSON input→AIR e integração futura |
| Notices/licenças de gramática vendorizada | Nenhuma gramática/código externo vendorizado | Não copiar licença por analogia |

## Adaptações locais explícitas

O ProLeap admite heurística documentada como última opção em política genérica; o usuário pediu aqui vedação de heurística semântica de produção. O lowerer só aceita exato, conservador sustentado ou bloqueio/discovery.

O repo novo não tem src/POM. Paths `planned:` podem reservar criação de módulos no bootstrap, enquanto must_read continua apontando somente a documentos existentes. Propostas prontas ficam fora de active até autorização.

O harness é completo como **contrato documental**, não como enforcement instalado. Não anunciamos PIT, ArchUnit, scripts ou CI funcionando aqui porque funcionam em outro repo.
