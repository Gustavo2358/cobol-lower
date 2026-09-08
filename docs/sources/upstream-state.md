# Estado upstream observado

**Consulta:** 2026-09-06; apenas air-java revalidado em 2026-09-07. A baseline abaixo é fixa para preparar o trabalho; não garante que main não avançará. Atualizações posteriores seguem controle de mudanças.

| Produto | Commit observado | Estado relevante |
| --- | --- | --- |
| proleap-poc | `c8a891e0827ae1dc1140246f625fd16c2ac9bd97` | Merge de Entry/start/GOBACK; SP 1.1.0 e filename canônico |
| analysis-ir | `122ce54e1b9ef9b00646f93ece409ca8b63bc933` | AIR 2.0.0 reconciliada; binding JSON 1.0.0 DRAFT |
| air-java | `b78f4068d8a479f48eb048b8d76fa60a0997dc4a` | Merge PR #5 / 1A; air-model → air-java, air-json → codec compartilhado; Java 21 |
| analysis-cfg | `4685dca32bf4f31a0e699ec6a3927018ab1b1d84` | Contrato de porta Publication/BuildCfg e orientação docs-only na baseline consultada |

## Ajustes explícitos ao handoff

O handoff referia air-java `2108294...`, AIR `0b2fbce...` e PR #31 ainda a confirmar. Esses snapshots foram superados pelos acima. O original fica preservado em história; não foi reescrito para aparentar que já conhecia o merge.

A reconciliação AIR/Java removeu contracts top-level, ResourceTarget, SafetyAssertion e return.entryScope; definiu assinatura externa por invoke e outcomes separados. Não usar construtores antigos do ZIP air-java inicial como contrato atual.

A documentação CFG consultada ainda diz que o binding não existia no snapshot antigo. O binding existe na AIR fixada aqui e é DRAFT. Não presumir que o desenvolvimento em paralelo do CFG já foi concluído/mergeado; confirmar no checkpoint de integração, não editar o outro repo como efeito lateral.

## Evidência disponível e ausente

Foram consultados metadados de refs e os contratos/arquivos selecionados listados no lock, além do contexto de reviews anterior desta conversa. Não foi executado build do proleap/air-java/CFG para produzir este pacote. Não há release Maven ou binding accepted comprovados por esta entrega. Não existe golden SP capturado no ZIP.

O primeiro trabalho deve confirmar acessibilidade dos snapshots, resolver air-java reprodutivelmente e capturar a fixture real. Uma falha de acesso deve ser informada; não usar uma biblioteca homônima ou outro commit silenciosamente.

## Pin autorizado para os próximos checkpoints

O upstream air-java autorizado para os próximos checkpoints é `b78f4068d8a479f48eb048b8d76fa60a0997dc4a`. `pom.xml` é o parent `air-java-parent`; `air-model/` contém model/validator no artefato `air-java`, e `air-json/` contém o codec compartilhado `AirJson`, com cobertura 1A. A autoridade normativa permanece `analysis-ir@122ce54e1b9ef9b00646f93ece409ca8b63bc933`. O WORK-LOWER-002 atualizou apenas proveniência e foi mergeado. No 2A, `air-json:0.1.0-SNAPSHOT` passa a dependência de runtime exclusiva de adapters; model/validator continuam em core. 2B/CFG JSON/E2E completo não foram iniciados. Evidência em [WORK-LOWER-002](../work/history/WORK-LOWER-002.md). As observações do pacote original acima conservam seu contexto histórico.

## Baseline 4C

4A PR32 merge 2815e805fd3a9ef4762a39ab9435260fc76da0e8, SP1.2.0.
4B PR6 merge ce530a7e17ab12b23c48f29425f503ff920b09fb, Maven0.1.0-SNAPSHOT.
Ambos confirmados pela API; build isolado do merge, sem uso automático de head pré-merge.
Pin normativo 122ce54e1b9ef9b00646f93ece409ca8b63bc933 intacto.
