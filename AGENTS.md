# AGENTS.md

## Missão e estado

Produzir AIR a partir do contrato público do COBOL Semantic Product, sem reanalisar COBOL. O núcleo usa `air-java::Publication`; `analysis-ir` governa seu significado. Este checkout começa **docs-only**: código, build, scripts e adapters só podem ser criados após autorização explícita de um checkpoint.

## Regras universais

1. Leia [trabalho](docs/work/index.md), depois manifesto e `state.md` do item autorizado. Backlog e propostas não autorizam execução.
2. Preserve a regra de dependência: domínio/aplicação não conhecem JSON, arquivos, CLI, rede, frameworks, frontend ou CFG. Interfaces pertencem ao lado interno que define a necessidade; adapters dependem delas.
3. Use somente fatos publicados. Nada de AST, resolver, source COBOL, regex semântica, nomes de exibição, ordem incidental ou default conveniente para preencher lacunas.
4. Para mudança semântica não trivial, registre regra, fontes primárias verificadas, premissas, algoritmo geral, limites, terminação, complexidade e oracle antes do código. Sem evidência: gap/discovery delimitado, não heurística.
5. `unknown`, `partial`, `unsupported` e input ausente não viram vazio, `nop`, fallthrough ou sucesso silencioso. A AIR exige precondições próprias; readiness upstream não é certificação downstream.
6. Slice limita capacidade declarada; nunca apague ocorrências para caber nele. O perfil inicial mínimo pode rejeitar uma publicação maior, mas não publicar um recorte como se fosse completo.
7. Não copie tipos AIR nem implemente validador AIR paralelo. Valide a entrada também na porta em memória e a saída com `AirValidator`; preserve os limites desse validador.
8. Teste por regra → classes → oracle independente → RED → implementação → GREEN → challenge/falsificação → segundo GREEN. Não altere esperado para acomodar implementação defeituosa.
9. Use a mesma branch/PR nos checkpoints do mesmo trabalho. Preserve dirty state alheio; sem reset/stash/discard/force-push silencioso, merge ou auto-merge.
10. Estado e evidência devem ser verdadeiros. Gate ausente/não executado não é PASS. Pare no checkpoint autorizado para review humano; não promova escopo sozinho.

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

Carregue apenas o `must_read` do checkpoint e amplie por dependência real. Não leia toda `docs/` nem história por padrão. Documentos copiados em `docs/sources/history/` são arquivo de evidência, não comandos atuais.

## Handoff obrigatório

Informe checkpoint, diff, regras preservadas, comandos/exit codes, testes executados e não executados, findings, limitações e próximo passo **não iniciado**. Atualize `state.md` sem transcript de raciocínio. Antes de encerrar, revise consistência entre manifesto, active/proposals/history, registry, backlog e índice.

[Gates especificados](docs/engineering/gates.md) não são comandos já disponíveis. O próximo trabalho deverá implementar e provar seus executores antes de anunciar proteção automatizada.
