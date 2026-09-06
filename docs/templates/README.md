# Templates de trabalho e evidência

Copiar somente o template necessário para um destino autorizado. Placeholders nesta pasta não representam fatos, execução ou aprovação. Preencher dados por evidência e remover instruções exemplificativas antes de promover o documento.

| Uso | Template |
| --- | --- |
| Novo trabalho | [manifesto](work-item/work-item.yaml), [spec](work-item/spec.md), [plan](work-item/plan.md), [eval](work-item/eval.md), [state](work-item/state.md) |
| Decisão arquitetural | [ADR](adr.md) |
| Pesquisa de regra/algoritmo | [nota de pesquisa](research-note.md) |
| Candidato futuro | [backlog](backlog.md) |
| Bug/gap | [finding](finding.md) |
| Abrir ou atualizar PR | [pull request](pull-request.md) |
| Review humano/independente | [review](review.md) |
| Encerrar/retomar checkpoint | [handoff](handoff.md) e [evidência](checkpoint-evidence.json) |
| Encerrar work item | [closure](closure.md) |

O [protocolo](../engineering/work-item-protocol.md) governa estados e promoção. O [schema](work-item.schema.json) documenta forma do manifesto; integridade de paths/IDs/autorização exige também os checks relacionais de G-DOCS. Schema sozinho não verifica semântica nem GitHub.

Não copiar toda esta pasta para cada work item. O work item concreto contém exatamente os cinco arquivos do protocolo; evidências duráveis adicionais têm destino próprio roteado no eval/state.
