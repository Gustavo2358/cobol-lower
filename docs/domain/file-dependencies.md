# FILE-DEPENDENCIES — lowering composicional

H4 aprovado; core N+C autorizado em 2026-09-16. W0/W1 qualificados local; W2 em implementação; W10 não autorizado.
[Campanha, brief e waves](https://github.com/Gustavo2358/analysis-cfg/blob/feat/file-dependencies/docs/product/file-dependencies/README.md)
(workspace: `../analysis-cfg/docs/product/file-dependencies/README.md`).

O caminho corrente é `CobolLowerer` → `PartialProgramAdmission` →
`RegionalDataTranslator` → `PartialProgramAssembler` → `PartialProgramLowerer`.
Não acrescentar file-only lowerer nem usar README de CP3 como retrato do dispatch.

| Wave | Superfície local e obrigação |
| --- | --- |
| W0 | SpInput, SpJsonDecoder/admission; preservar assignment-name/external file name, sem inferir DD allocation/bindingMechanism; atualização SP coordenada, sem emitir I/O AIR |
| W1 | recursos/associações e consumer vertical; ResourceDeclaration neutra com owner/record/use; artifactRelations permanece vazio |
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

## FD-W0 — SP 2.21 / inventário declarativo

Execução core autorizada após H4; decoder lê `fileInventory@1.0.0` tipado e
obrigatório em SP2.21. Inventários históricos sem FILE permanecem UNAVAILABLE,
sem converter ausência em conjunto vazio. Novo FILE sob versão histórica é
rejeitado pelo shape fechado; nenhum dual writer ou inferência downstream.
`FileFacts` conserva owner, FD/SD, nome externo/sourceKind, record ownership,
chaves/status resolvidos, visibilidade e origens. Admission é comum a file/memory,
valida IDs/owners/refs/gaps e recusa SD com external name. Não emite FILE AIR em W0.

Oráculo `FileDeclarationSuite`: bytes reais frontend + expectativas manuais,
contracasos wire/admission (versão, inventário removido, enum, owner, record,
duplicação, SD, mecanismo externo indevido e disponibilidade incoerente).
Regressão focal fixa inclui os consumers CALL existentes. Pin final/evidência
bilateral são atualizados somente após qualificação do produtor.

## FD-W1 — slice estático SP2.22 / AIR resource.bindings@1

SP2.22/fileInventory1.1 acrescenta usos OPEN/READ/CLOSE N-LR tipados; o decoder
histórico SP2.21 preserva declarações com operações UNAVAILABLE. Admission comum
recusa refs ausentes, modos incoerentes, duplicações e disponibilidade inválida.
O lowering composicional emite invoke file com alvo literal quando provado e
associa owner, registros e usos à declaração. SD usa LocalResource, sem nome
externo inventado. Nome não provado conserva unknown. Efeitos e controle continuam
abertos até W3/W4, sem MUST. Nenhuma implementação ASSIGN DYNAMIC.

Registros com tipo ainda desconhecido têm identidade nominal independente do
índice usado pela tradução precisa de expressões; associá-los não promove seu
tipo nem altera regras de CALL. Inventário FILE participa da revisão canônica;
inputs históricos sem inventário conservam sua identidade.
Complexidade: indexação O(declarações + usos + registros), ordenação por ordinal
O(usos log usos) por statement; sem busca no fonte, fixpoint ou cutoff novo.
Oracle FileStaticSliceSuite: fixture real no pin SP, nomes/owner/record/ações
esperados manualmente, dez mutantes de admission, codec e determinismo. Suites
CALL/storage/effects existentes continuam no FAST fixo.

## FD-W2 — família nativa / SP2.23

Regra antes da produção: o SP2.23/fileInventory1.2 publica sete comandos N-LR,
operandos DATA por papel (RECORD/INTO/FROM/KEY/ADVANCING), opções por arquivo,
comparação START, delimitação e corpos dos handlers por StatementId. A autoridade
IBM SC27-8713-03, atualização 2026-04-28, está registrada no domínio do produtor;
o lower traduz esses fatos, sem interpretar fonte. WRITE/REWRITE usam o candidate
FILE publicado a partir do record owner; FROM nunca cria outro uso FILE.
DELETE_RECORD vira ação neutra `delete-record`, sem significar apagar dataset.

Admission bilateral fecha referências ao statement/operand/body, enums, papéis,
opções e disponibilidade. SP2.22 histórico conserva fatos estruturais UNAVAILABLE.
Invokes permanecem com efeitos/controle abertos até W3/W4; não se presume MUST
nem fluxo incondicional para handlers. Inventários CALL e FILE coexistem no mesmo
assembler. Identidade inclui fatos novos somente quando disponíveis, preservando
revisões históricas. Algoritmo: índices de operandos/statements e varredura de usos
O(statements + operandos + usos + handlers), sem cutoff; ordenação local por ordinal.
Oracle FileNativeOperationSuite: sete ações, FROM de outro FD sem leitura extra,
DELETE único, dois CALL condicionais sem duplicação, falhas de admission em file
port e memory port e codec/determinismo. Nenhuma mudança AIR é necessária.

Checkpoint lower W2: FileNativeOperationSuite PASS (sete ações, FROM isolado,
handlers, wire/memory negativos, codec e determinismo); FileStaticSliceSuite e
FileDeclarationSuite históricos PASS. FAST fixo PASS: 2340 testes core e todas
as suites adapter/CALL/storage/control, incluindo o novo oracle. Pin SP2.23
`b559292c97e004504fb867c4724298dc1637b6f2`; AIR/IR permanecem nos pins de W1.
B-SP native/delete-handlers byte a byte PASS. E-SELECTED registrado no harness
canônico após executar produtores imutáveis.
