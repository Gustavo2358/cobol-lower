# FILE-DEPENDENCIES — lowering composicional

H4 aprovado; core N+C autorizado em 2026-09-16. W0–W3 qualificadas local; W4 em implementação; W10 não autorizado.
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

## FD-W3 — contrato/algoritmo antes da produção

SP2.24/fileInventory1.3 acrescenta planos de memória: alvos DATA/view/ref canônicos,
leituras delimitadas, FROM antes de I/O e passos condicionados a SUCCESS/END/
INVALID_KEY/OTHER_ERROR. São efeitos **se** o outcome ocorre, não prova de que ele
é possível; Normal AIR continua distinto de status COBOL. Storage1.8 acrescenta
INDEPENDENT_LOCAL_STORAGE; a prova histórica WS é mantida somente para compatibilidade.

Decoder fechado novo + admission comum às portas devem verificar owners, views,
intervalos, alias/classe para COPY/FIT e MUST exato; retirar campo obrigatório,
injetar referência alheia ou elevar MAY sem prova deve falhar. Wire2.23 e anteriores
conservam effects UNAVAILABLE e a identidade histórica. Lower emite operações gerais
por fase/outcome, sem reanalisar COBOL; endereço INTO só é usado depois de READ.
Sem prova de endereço, manter efeito local ao bound publicado, sem MUST fictício.
Planos são percorridos e indexados por statement/ordinal/target; custo linear nos
fatos emitidos, sem solver FILE nem limite de ocorrências.

Oráculos antes da produção: FileMemoryEffectsSuite usa fixtures reais SP2.24,
expectativas manuais de fase/efeito e mutantes bilaterais. RED inicial rejeitou a
versão; contracasos posteriores detectaram view alheia, receiver omitido e salto
para fase interna. O1–O5 manuais em CFG exercitam validator/codec/CALL+FILE. W3 só fecha após
focais, FAST, Q-SHARED e E-SELECTED nos pins finais.

Lowering W3 usa operações AIR gerais: FROM antes do invoke, seleção desconhecida
de outcome após retorno, MAY regional/por base, MUST apenas para receptor exato
admitido e cópia/fit já provada. Nenhum result place de invoke antecipa endereço
INTO. As continuações ainda abertas alcançam entradas de statements do fonte e
saídas, nunca fases internas de memória; handlers/USE exatos são W4. O assembler
conserva instruções anteriores ao primeiro terminador e correla cada operação.
Planos antigos sem effects mantêm representação histórica; novos contratos não
podem omitir status/buffer/INTO/FROM e alegar bound fechado.

O bound provisório de controle compartilha o conjunto de entradas do fonte em
memória. Sua serialização repete esse conjunto por continuação FILE: limite
O(usos FILE × statements) no wire conservador W3. Não há cutoff; W4 deve qualificar
o controle específico de handlers/USE e medir a redução, sem inventar SLA.

FAST W3 core2340 + adapters PASS; qualification-local semântica244296 e
performance39215 PASS (contadores do harness, não precisão/recall). O gate de
saída tinha um fixture antigo que tratava PARTIAL como falha sem Publication,
contradizendo LoweringResult desde 69c78c5; corrigido o fixture, mantendo o teste
de CLI PARTIAL real em EvidencePreservingEntrySuite. Pin/E-SELECTED pendentes.

Revalidação final após FROM: FAST e qualification-local PASS (mesmos contadores),
leitura de FROM alias/não resolvido preservada e mutante sem leitura do record
de saída rejeitado. Logs `fast-3.log` e `qualification-local-3.log` em fd-w3.
Pin produtor SP2.24: `a9f8fbe4fb4f9e2097c01b1e8f6f992a5041ee5f`; E-SELECTED pendente no harness canônico.


## FD-W4 — contrato fechado de controle e redução conservadora

SP2.25/fileInventory1.4 publica DECLARATIVES e rotas de eventos; Wire225 é fechado,
sem modificar fixtures históricas. Admission comum verifica owner/body/entry,
FILE_HANDLER, correspondência com efeitos, precedência file/mode, referências,
continuação estrutural e possibilidade de erro crítico. Contradições falham também
na porta em memória. Retirar fatos nunca transforma input incompleto em sucesso.

A regra IBM e o oracle foram fixados no D-EFFECT canônico antes da produção.
Assembler usa branches para eventos, efeitos W3 ordenados, jumps aos handlers/USE
compartilhados e opaque de retorno com apenas as continuações dos sites invocadores.
Não clona CALL/I/O; não adiciona efeito global na entrada/retorno. União de resumes
é marcada LOCAL_RETURN_CONTEXT_NOT_PROVEN, sem alegar matching de pilha. Erro
crítico conserva saídas possíveis. Recursão é finita no inventário, sem cutoff.
A norma local.invoke existe, mas não é necessária para esta redução; a investigação
codec demonstrou lacuna de transporte/consumer, sem exigir extensão normativa.

Corpos/rotas indexados por identidade; custo proporcional aos fatos/alternativas,
com validação de seleção O(usos × declarações USE). O bound W3 com todas as entradas
de statements permanece somente para inputs históricos, não para o novo contrato.
FileControlSuite verifica seis fixtures do produtor, codec, ausência de duplicação,
resumes delimitados e mutantes de versão, seleção, status/efeitos, ownership,
completions e porta em memória. Gates finais/pins ainda pendentes na campanha.


W4 estabilizada: FAST fixo core2340 + adapters (incluindo FileControlSuite) PASS;
qualification-local semântica244296/performance39215 PASS. B-SP seis exports reais
byte a byte PASS; negativos de continuação/seleção/saída crítica/in-memory PASS.
Pin SP2.25 `1c21f21750aa3572e39fce71ccc156a357699d81`. AIR/IR conservam pins W1.
Logs `.harness-results/fd-w4/`; E-SELECTED imutável segue no harness CFG.
