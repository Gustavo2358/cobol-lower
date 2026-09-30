# Estado upstream observado

## Stage 5 review branch — 2026-09-30

AIR `e0aef0e1928d88a74fe66b7a4d0af84556b84b19` (PR #23) transports existing
`control.local@1`. Frontend review pin `ea545f5a597731a6738bdc1ee9446a208e8da840` fixes fixed-format continuation and partial FILE route publication
(SP 2.62 unchanged). This lower branch adds proved body sharing and requires the matching
consumer in analysis-cfg PR #57. These are review pins, not integrated main.

[Current evidence and limits](../work/shared-routine-bodies.md). Later sections
retain historical context and do not override the current source lock.


## Estado vigente — valores e controle após W8 integrados

SP 2.62 e qualified-source-dependencies 1.6 estão integrados. Dependencies mantém
a versão 2.7. Os pins abaixo são SHAs reais de main, incluindo fechamento documental.

- proleap-poc: `4c00dea2a6bad1ba21076e55681f6038b80f8a47`.

[Integração e limites](../work/carddemo-values-control.md). Produção,
testes e contratos equivalentes aos heads qualificados; 560 execuções anteriores,
incluindo todos os 73 programas CardDemo e os 18 novos casos nos dois perfis,
permanecem evidência válida. Zero perdas de candidatos/supports/provenance; 43
adições cumulativas explicadas contra W8. Corpus/full não são reexecutados por
uma alteração documental ou pin equivalente. Gates finais de main e SHAs constam
no relatório de integração. O ponto 5 permanece em campanha separada. As seções
seguintes são históricas e não substituem estes pins.

## Checkpoints históricos

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


## POSITIVE_MEMORY_TOPOLOGY W1 — 2026-09-20

Isolated permanent campaign branch starts from lower #32
`f8e181f95929c650181c989318f8ba23d1e68a1a`; parent remains untouched and unmerged.
Current exact pins: SP `cfcf0abf06a3b6186e157957bb301077fcb6f0bc`, AIR norm
`b26465964fe75f944f6324df63330d69f33d77cd`, AIR Java
`00373f638039c580bb833b9949df12cb8218e2fb`. The workflow checkout matches the
AIR Java lock; source trees and consumed-file SHA-256 values were regenerated from
those commits. Source contracts retain their wire versions. The existing AIR Nop
kind gains codec support in the companion, without inventing a new instruction.

[W1 contract and limits](../domain/positive-memory-topology.md) governs this semantic
migration. `LOGICAL_ONLY` remains consumer default; physical execution requires
explicit experimental opt-in. Final integrated evidence/PR state is recorded in the
anchor analysis-cfg #45 campaign report. No W2/W3 or merge is started here.

AIR harness reconciliation: pin `26016f10460336f237a33b2ed126a6a1427f0207` replaces `00373f638039c580bb833b9949df12cb8218e2fb`. Git diff confirms no AIR production sources or POM changes; the fixed module-policy gate reads the active lock rather than a historical literal. Existing semantic evidence remains equivalent; lower FAST is rerun for exact pin resolution.

## R7 final source handler contract

Current additional input contract: SP2.45 from frontend fbbf61d1840eb92dca804c53d8e9b6e60538318a. The strict pin is recorded in sources.lock.json. Exceptional source events, conditional handler selection and entry deactivation are analysis-only; AIR2.0, CFG and dependency-policy pins remain unchanged. Historical SP profiles and fixture bytes remain supported. See ../domain/cics-exceptional-handlers.md.

## Storage boundary fixes — 2026-09-27

Producer `4da7d8b03941ee78acba0d001a7e6391c1f7d371` supplies negative relation fixes and complete logical RENAMES
proofs within SP 2.48.0. The lock records its tree and selected file hashes.
The lower preserves GLOBAL identities separately from allocation and admits
FILLER/nonallocating RENAMES views. Uncertain I/O retains MAY effects.
See [validation and limits](../work/storage-boundary-fixes.md). AIR/CFG pins unchanged.

## Incomplete native file effects follow-up (2026-09-27)

Current producer pin: `7775c0407f6e5b60d73517b687f9768c878e59eb` (SP 2.48 unchanged).
Description/control identities and observed operands survive incomplete input;
open read/write bounds describe unproved locations without removing known facts.
The existing lower admission and MAY lowering suffice. Three frozen compilations,
closed-omission negatives and seven new dependency adversaries cover the boundary.
See [incomplete file effects](../work/incomplete-file-effects.md).


## Structural DFH review — 2026-09-27

SP2.49 producer `15a37dac4592ce1387907ea97bb9b04640e8432a` (Draft PR #64). The lock records exact tree/blob hashes.
Admission and round-trip preserve explicit synthetic model assumptions; AIR pins remain unchanged.

Final Draft producer pin: `ec83262bd65752f785f7d4df37a1310732e23a7b`. Since 15a37da, only deterministic V1 key order, its test/oracle guard and qualification documentation changed. Final frontend FAST and all E2E/reuse evidence are recorded in the structural work item.


## CardDemo IBM catalogue — 2026-09-27

Producer `503e11f6b33daa504e6338cf84a11984acab82cf` extends the approved model authority to six MQ members and Db2 SQLCA.
SP 2.49 and nominal V2 contracts are unchanged. All 73 CardDemo variants crossed
this lower through the production CLI, preserving dependency evidence.
[Qualification and reuse](../work/carddemo-ibm-copybooks.md).

Final documentation-only producer pin: `f8170f513eef29adfff5a94f418e1c6481ee2365`. All selected producer blobs are identical to the FAST pin `503e11f6b33daa504e6338cf84a11984acab82cf`. The only intervening change corrects the runtime source label in the evidence report.
