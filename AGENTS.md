# AGENTS.md

## Política de engenharia vigente

[LEAN HARNESS / GIT-IS-THE-RECORD](docs/engineering/lean-harness.md) governa o trabalho.
Git, commits, Pull Requests, testes e merge são a fonte de verdade do desenvolvimento.
Remote FAST only; full local/on-demand. Receipts e certificados CP não são requisitos.
História é READ_ONLY / BEST_EFFORT; registry/index servem à navegação.
Work items novos usam id/title/status/scope, com TODO/IN_PROGRESS/BLOCKED/DONE.
PR merged + required technical tests passed = DONE. Pins cross-repo permanecem estritos.
Mudanças semânticas importantes exigem revisão humana; metadata não exige cerimônia.
Execute `python3 -B scripts/harness/lean.py fast`; full local quando tecnicamente
necessário: `python3 -B scripts/harness/lean.py qualification-local`.
Preserve branches dedicadas, escopo, mudanças alheias e isolamento entre repositórios.
Não faça merge/auto-merge sem autorização.


## Missão e estado

Produzir AIR a partir do contrato público do COBOL Semantic Product, sem reanalisar COBOL. O núcleo usa `air-java::Publication`; `analysis-ir` governa seu significado. O repositório contém o slice `minimal-entry-goback@1`, build, adapters e harness executável. A autorização da sessão define o escopo; backlog não amplia capacidades por si mesmo.

## Regras universais

1. Consulte o índice de trabalho apenas para navegação e respeite o escopo autorizado.
2. Preserve a regra de dependência: domínio/aplicação não conhecem JSON, arquivos, CLI, rede, frameworks, frontend ou CFG. Interfaces pertencem ao lado interno que define a necessidade; adapters dependem delas.
3. Use somente fatos publicados. Nada de AST, resolver, source COBOL, regex semântica, nomes de exibição, ordem incidental ou default conveniente para preencher lacunas.
4. Para mudança semântica não trivial, registre regra, fontes primárias verificadas, premissas, algoritmo geral, limites, terminação, complexidade e oracle antes do código. Sem evidência: gap/discovery delimitado, não heurística.
5. `unknown`, `partial`, `unsupported` e input ausente não viram vazio, `nop`, fallthrough ou sucesso silencioso. A AIR exige precondições próprias; readiness upstream não é certificação downstream.
6. Slice limita capacidade declarada; nunca apague ocorrências para caber nele. O perfil inicial mínimo pode rejeitar uma publicação maior, mas não publicar um recorte como se fosse completo.
7. Não copie tipos AIR nem implemente validador AIR paralelo. Valide a entrada também na porta em memória e a saída com `AirValidator`; preserve os limites desse validador.
8. Teste por regra → classes → oracle independente → RED → implementação → GREEN; challenge/falsificação local sob demanda. Não altere esperado para acomodar implementação defeituosa.
9. Use branch/PR dedicados; preserve dirty state alheio. Sem reset/stash/discard/force-push silencioso.
10. Estado e testes devem ser verdadeiros. Gate não executado nunca é PASS; não amplie o escopo sozinho.

## Roteamento

| Decisão atual | Ler |
| --- | --- |
| Missão e primeiro resultado | [missão](docs/product/mission-and-slices.md) e [slice GOBACK](docs/domain/first-slice-entry-goback.md) |
| Interface, adapter, composição | [arquitetura](ARCHITECTURE.md) → [portas](docs/architecture/ports-and-adapters.md) |
| Regra/identidade/coverage | [domínio](docs/domain/index.md) e invariantes/evals citados |
| Nova capacidade / mudança upstream | [extensibilidade](docs/architecture/extensibility.md) e [mudanças de contrato](docs/engineering/change-control.md) |
| Algoritmo / pesquisa | [política semântica](docs/engineering/semantic-analysis-policy.md) e [evidência](docs/engineering/research-and-evidence.md) |
| Testes / desempenho | [testes](docs/engineering/testing.md), [falsificação](docs/engineering/falsification.md), [performance](docs/engineering/performance.md) |
| Abrir, revisar ou encerrar trabalho | [lifecycle](docs/engineering/work-item-protocol.md), [Git/review](docs/engineering/git-and-review.md), [gates](docs/engineering/gates.md) |
| Criar prompt / retomar sessão | [prompts](docs/engineering/prompt-authoring.md) e [sessão](docs/engineering/agent-session-protocol.md) |
| Divergência entre produtos | [autoridade](docs/sources/authority.md), [impacto](docs/engineering/downstream-impact.md), [baseline](docs/sources/upstream-state.md) |

Carregue contexto por dependência real. História não é instrução atual.

## Handoff

Informe diff, commit/PR, testes executados e resultados, limites e próximo passo.
Nenhum certificado, receipt ou reconciliação histórica é necessário.
