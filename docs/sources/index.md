# Fontes e baselines

[Autoridade](authority.md) define quem governa cada contrato. [Source lock](sources.lock.json) fixa os snapshots consultados. [Estado upstream](upstream-state.md) separa fatos observados de expectativas. [Pesquisa externa](research.md) registra fontes primárias, o trecho consultado e os limites.

| ID | Papel |
| --- | --- |
| SRC-SP | Contrato do Semantic Product 1.1.0, writer público e testes Entry/GOBACK do proleap-poc |
| SRC-HARNESS | AGENTS, workflow, gates, política semântica/testes/impacto do proleap-poc |
| SRC-AIR | Especificação normativa da Analysis IR 2.0.0 |
| SRC-AIR-JAVA | Repositório com model/validator em air-model (artefato air-java) e codec compartilhado em air-json; implementação, não autoridade normativa |
| SRC-CFG | Contrato do consumer independente e sua porta em memória |
| SRC-HANDOFF | Handoff enviado pelo usuário; [original preservado](history/handoff-cobol-lower-first-slice.md) |
| SRC-PROMPTS | Boas práticas de prompts da conversa; [original preservado](history/handoff-boas-praticas-prompts-agenticos.txt) |
| SRC-USER | Decisões explícitas desta conversa, consolidadas nos ADRs e no mapa de absorção |

O [mapa de absorção](handoff-integration.md) demonstra o destino de cada seção do handoff. A [paridade](proleap-harness-parity.md) explica o que foi transferido do harness ProLeap e o que foi adaptado. [Questões de contratação](open-questions.md) permanecem explícitas.

Os arquivos históricos são evidência, não material de contexto obrigatório. Consultá-los só para investigação/migração; os SHAs antigos e instruções históricas não sobrepõem o source lock e o estado autorizado atual.
