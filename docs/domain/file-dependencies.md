# FILE-DEPENDENCIES — lowering composicional

FD-H0–H4 prepara; produção ainda não autorizada.
[Campanha, brief e waves](https://github.com/Gustavo2358/analysis-cfg/blob/feat/file-dependencies/docs/product/file-dependencies/README.md)
(workspace: `../analysis-cfg/docs/product/file-dependencies/README.md`).

O caminho corrente é `CobolLowerer` → `PartialProgramAdmission` →
`RegionalDataTranslator` → `PartialProgramAssembler` → `PartialProgramLowerer`.
Não acrescentar file-only lowerer nem usar README de CP3 como retrato do dispatch.

| Wave | Superfície local e obrigação |
| --- | --- |
| W0 | SpInput, SpJsonDecoder/admission; preservar assignment-name/external file name, sem inferir DD allocation/bindingMechanism; atualização SP coordenada, sem emitir I/O AIR |
| W1 | recursos/associações e consumer vertical; Publication atualmente deixa resources/artifactRelations vazios |
| W2 | handlers composicionais para operações nativas; vínculo record-owner fornecido pelo SP |
| W3/W4 | tradução regional/efeitos, status, outcomes e handlers; não inferir MUST pelo verbo |
| W5/W6 | I/O implícito SORT/MERGE, procedimentos locais, I-O-CONTROL por perfil |
| W7/W8 | consultas gerais para CICS target no site, effects/condições; W8 literal não espera W7, computados exigem W7 |
| W9 | unidade selecionada/captures e agregação core, sem re-resolução nominal |
| W10 posterior | extensão D após core W11/autorização; ASSIGN DYNAMIC/captura OPEN, Report Writer e APIs; não bloqueia core N+C |

Reusar `LocalIds`, `SourceOrigins`, correlações statement/operand e storage.
Uma origem pode gerar vários usos com papéis; IDs só precisam de determinismo na
revisão, sem estabilidade longitudinal artificial. `ResourceId` não é Target.
Não abrir SP/COPY/fontes para completar a Publication no consumer.

[Provas core A1–A4/A6/O1–O5 e decisões](https://github.com/Gustavo2358/analysis-cfg/blob/feat/file-dependencies/docs/product/file-dependencies/contracts.md)
governam contrato AIR antes de W1 e efeitos antes de W3/W4. Semântica não provada
permanece localizada e conservadora, preservando nome conhecido e CALL independente.
A5/captura D só se torna obrigação na extensão W10.

Gates existentes: `python3 -B scripts/harness/lean.py docs` para H;
`fast` para produção estabilizada; `qualification-local` para checkpoint semântico.
`scripts/harness/focal.py` compõe `FastSuite` e `FastAdapterSuite` após bootstrap;
`CallSuite`, `IfSuite`, `InputSuite`, `ManualAir` e testes de adapters são bases de
oráculo. Não usar selector JUnit em suítes Java main deste repo.
`LOWER_BUILD_ROOT` isola o build; locks imutáveis continuam obrigatórios.
