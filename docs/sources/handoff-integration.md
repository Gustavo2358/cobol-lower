# Absorção do handoff e decisões da conversa

O original foi preservado sem alteração em [history](history/handoff-cobol-lower-first-slice.md). Esta tabela rastreia suas 23 seções; não é uma nova fonte de semântica.

| Seção | Assunto | Destino canônico | Tratamento |
| --- | --- | --- | --- |
| 1 | Fronteiras físicas | [documento](../architecture/boundaries.md) | Mantidas; JSON como integração inicial, sem prender a porta permanentemente a arquivos. |
| 2 | Estado SP e PR31 | [documento](../sources/upstream-state.md) | Confirmado snapshot mergeado; pins antigos preservados no original histórico. |
| 3 | EntryInventory e GOBACK | [documento](../domain/semantic-input-contract.md) | Campos conceituais versus shape real do writer explicitamente separados. |
| 4 | PRIMARY_ONLY/PARTIAL | [documento](../domain/coverage-readiness-uncertainty.md) | Inventário alternativo aberto permanece visível na AIR. |
| 5 | Início executável | [documento](../domain/first-slice-entry-goback.md) | Start explícito; proibidas escolhas por ordem/ID/root. |
| 6 | GOBACK e successor | [documento](../domain/first-slice-entry-goback.md) | Nenhum fallthrough; adversarial GOBACK seguido de statement. |
| 7 | Mapeamento AIR mínimo | [documento](../evals/first-slice-oracle.md) | Entry/Sequence/Return vazio no perfil conhecido. |
| 8 | Return versus Halt | [documento](../domain/first-slice-entry-goback.md) | Sem inferência de main/subprograma por filename/nesting. |
| 9 | Assinatura | [documento](../domain/validation-and-results.md) | Zero conhecido distinto de desconhecido; partial bloqueado. |
| 10 | Versão SP | [documento](../domain/semantic-input-contract.md) | 1.1.0 explicitamente negociada; não assumir qualquer 1.x. |
| 11 | Namespaces | [documento](../domain/identity-and-provenance.md) | Joins completos e teste cross-unit nas formas realmente representáveis. |
| 12 | Coverage/readiness | [documento](../domain/coverage-readiness-uncertainty.md) | Sem promoção enum→precisão global. |
| 13 | Outros terminais | [documento](../domain/capability-matrix.md) | Não recebem semântica de GOBACK. |
| 14 | Observed statements | [documento](../domain/first-slice-entry-goback.md) | Rejeição explícita no primeiro perfil; opaque posterior, sem omissão. |
| 15 | air-java e DTOs | [documento](../architecture/ports-and-adapters.md) | Input interno sem transporte; wire DTOs no adapter; AIR não duplicada. |
| 16 | Validação AIR | [documento](../domain/air-target-contract.md) | Reutilizar checker, conservar limites e não certificar tradução por green. |
| 17 | Fixture AIR independente | [documento](../evals/first-slice-oracle.md) | Esperado manual antecede integração com frontend. |
| 18 | TDD e adversariais | [documento](../evals/catalog.md) | Casos convertidos em evals estáveis com oracle/negativo explícitos. |
| 19 | Critério primeiro sucesso | [documento](../work/active/WORK-LOWER-001/spec.md) | JSON real→AIR válida; sem output codec exigido neste slice. |
| 20 | Fora de escopo | [documento](../work/active/WORK-LOWER-001/spec.md) | Demais capacidades no backlog, não autorizadas. |
| 21 | Transporte | [documento](../architecture/ports-and-adapters.md) | Core sem Jackson/Path; substituição arquivo/memória testável. |
| 22 | E2E futuro | [documento](../product/ecosystem.md) | Parallel tracks e rendezvous explícito com CFG. |
| 23 | Sem nova análise COBOL | [documento](../engineering/semantic-analysis-policy.md) | Regra central preservada e reforçada com fontes/contraexemplos. |

## Decisões adicionais explícitas da conversa

Repositórios separados e integração futura por módulos; air-java sem codec; output AIR em arquivo por adapter; domínio sem infraestrutura/DIP; código evolutivo para capacidades futuras; mesmo PR por trabalho; gates/falsificação; prompt curto roteado; meta de 15/09/2026 tratada como priorização e não promessa. Estão em arquitetura, engenharia, produto e ADRs.

## Refinamentos de planejamento, não fatos extraídos do handoff

O nome `minimal-entry-goback@1`, a admissão inicial com shape mínimo explícito, seis checkpoints, catálogos de estados/gates e a política de identidade por revisão são escolhas locais propostas por este harness. Devem ser verificadas/revisadas antes da implementação; não são campos oficiais SP/AIR.

Uma sugestão anterior de IDs baseados somente na unit foi refinada para exigir distinguir revisões. A proposta anterior de esperar aceitação do binding antes de qualquer codec foi refinada: experimentação autorizada contra draft fixado pode produzir a evidência de sua promoção. Essas decisões evitam colisão de publicações e dependência circular de aprovação.

A publicação AIR deve ser construída por regras de tradução sustentadas. Chamá-la de “puramente mecânica” não elimina a responsabilidade por normalização, precondições e preservação semântica.
