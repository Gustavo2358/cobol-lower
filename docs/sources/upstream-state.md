# Estado upstream observado

**Consulta:** 2026-09-06. A baseline abaixo é fixa para preparar o trabalho; não garante que main não avançará. Atualizações posteriores seguem controle de mudanças.

| Produto | Commit observado | Estado relevante |
| --- | --- | --- |
| proleap-poc | `c8a891e0827ae1dc1140246f625fd16c2ac9bd97` | Merge de Entry/start/GOBACK; SP 1.1.0 e filename canônico |
| analysis-ir | `122ce54e1b9ef9b00646f93ece409ca8b63bc933` | AIR 2.0.0 reconciliada; binding JSON 1.0.0 DRAFT |
| air-java | `6a4091e5394fc22b3d2ada9abbdb530eb3572a58` | Merge da reconciliação; biblioteca 0.1.0-SNAPSHOT Java 21 |
| analysis-cfg | `4685dca32bf4f31a0e699ec6a3927018ab1b1d84` | Contrato de porta Publication/BuildCfg e orientação docs-only na baseline consultada |

## Ajustes explícitos ao handoff

O handoff referia air-java `2108294...`, AIR `0b2fbce...` e PR #31 ainda a confirmar. Esses snapshots foram superados pelos acima. O original fica preservado em história; não foi reescrito para aparentar que já conhecia o merge.

A reconciliação AIR/Java removeu contracts top-level, ResourceTarget, SafetyAssertion e return.entryScope; definiu assinatura externa por invoke e outcomes separados. Não usar construtores antigos do ZIP air-java inicial como contrato atual.

A documentação CFG consultada ainda diz que o binding não existia no snapshot antigo. O binding existe na AIR fixada aqui e é DRAFT. Não presumir que o desenvolvimento em paralelo do CFG já foi concluído/mergeado; confirmar no checkpoint de integração, não editar o outro repo como efeito lateral.

## Evidência disponível e ausente

Foram consultados metadados de refs e os contratos/arquivos selecionados listados no lock, além do contexto de reviews anterior desta conversa. Não foi executado build do proleap/air-java/CFG para produzir este pacote. Não há release Maven ou binding accepted comprovados por esta entrega. Não existe golden SP capturado no ZIP.

O primeiro trabalho deve confirmar acessibilidade dos snapshots, resolver air-java reprodutivelmente e capturar a fixture real. Uma falha de acesso deve ser informada; não usar uma biblioteca homônima ou outro commit silenciosamente.
