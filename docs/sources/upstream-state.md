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

## Baseline sincronizado pós-CP5 — WORK-LOWER-009

Input SP: `8722945cc4cd2052c6091533f6ee6989278aa2f8`; norma: `51b4d9a8ae0364232bd97103cd73a77e1a34996c`; biblioteca/codec: `3bafe3978f0f392e842038ad5628e85dfd91d00d` (PR8 mergeado). A [matriz](../quality/WORK-LOWER-009/upstream-deltas.json) prova ancestralidade. O writer SP, fixture e contrato escalar permanecem byte-identical; W5 já executou esse produtor. Especificação/conformidade/binding normativos inalterados. Air-java muda capacidade/taxonomia por PR7; PR8 só repina norma. Harness usa RESOURCE_LIMIT e budgets explícitos, requalificando 10k sob defaults aprovados. Produção lower/POM/fixtures não mudam. Referências históricas de inspiração/consumer context no lock permanecem históricas.

## CP6 W1C — autoridades em 2026-09-11

W1A merge fixo `53d774026a1e4bcd969c7783a1d277aaa87b5f2f`, SP1.3.0. W1B PR9
confirmado MERGED antes da branch: merge `2a37f5e980ba25fdc79614a66030a84d8bf5b8c9`,
tree `8d248f4ccf207eb7b609aa9ff0cdbcd512e526c8`, também main observada na consulta.
Norma AIR permanece `51b4d9a8ae0364232bd97103cd73a77e1a34996c`. O bootstrap
constrói model/codec desse merge em Maven repo isolado e vincula SHA/tree/digests
ao source lock. Produtor real é executado em checkout isolado do W1A.
[Contrato](../domain/call-lowering.md), [certificado](../quality/WORK-LOWER-010/CP0.json).

## CP6 W2B productive authorities — 2026-09-12

SP1.4 W2A product merge `4ffabded1aad39316b8a6f337f732976fdb3ca3e`, tree
`5880e174b33c85ba3f3cdbc70bd2d8dc7b1d567b`; AIR Java W2C product merge
`1d22068e9d9c1d100ecdef734e5b6995252e7ede`, tree
`ab3a55e13b781ec5424356e83bddbc6fde42dcb1`. Explicitly pinned product merges,
not later administrative closeouts (air-java `9aec1e9bce30466f4a53d2033bf2a2a2fedd56ec`).
Source lock includes SHA-256 for consumed contracts, writer, predicate/arm/completion/
IndependentStorageSet public model and fixtures. Historical SP1.3 decoder keeps
its exact meaning. AIR2 normative and JSON1.0 DRAFT pins remain unchanged.
