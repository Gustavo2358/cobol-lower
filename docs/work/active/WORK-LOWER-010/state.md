# WORK-LOWER-010 — CP6 W1C

CP0, single-checkpoint. IMPLEMENTED / AWAITING_HUMAN_REVIEW. Código e evidência
local concluídos; a revisão humana final permanece pendente. Nenhum checkpoint
seguinte foi autorizado ou iniciado.

Baseline `18016f16b4f63149eb1bb4ca13db7e12593d8909`, tree
`0139edc52e34c703b643ff484a33e70871ef091d`. Branch
`feat/cp6-w1c-call-lowering`. Certificado inicial: `docs/quality/WORK-LOWER-010/CP0.json`;
commit resolvido pelo trailer Checkpoint-Evidence. O número da Draft PR, SHA/tree
literal e recibo remoto pertencem ao PR/handoff pós-push, sem alterar o
certificado que precede a criação inicial da PR.

W1A `53d774026a1e4bcd969c7783a1d277aaa87b5f2f`, SP1.3.0. W1B PR9 MERGED confirmado
antes da implementação: `2a37f5e980ba25fdc79614a66030a84d8bf5b8c9`, tree
`8d248f4ccf207eb7b609aa9ff0cdbcd512e526c8`. Build isolado do merge e receipt dos JARs.

RED1 versão, RED2 CALL e RED3 FITTED_TEXT preservados. Decoder estrito, CallFact,
admissão, Invoke terminador, duas sequências, texto fitted bruto e revisão
canônica implementados. 23 fontes W1A executadas duas vezes; X8 e literal
atravessam CLI/AIR/AirJson duas vezes com bytes idênticos, STRUCTURALLY_VALID e I-56.
13 mutações compiláveis CALL e placement I-04; restauração exata e segundo GREEN.

Gates locais: bootstrap, Maven clean verify, fast, full, docs, architecture,
semantic, performance, git e 175 testes do harness PASS, conforme CP0.json.
Full inclui regressões e desafios anteriores. Gate dedicado transport/integration
continua SPECIFIED_NOT_IMPLEMENTED; testes reais estão em semantic e no script
E2E. Review foi do próprio implementador, sem alegação de independência.

Limites: um CALL sem USING/RETURNING/handlers; unknowns e PARTIAL explícitos.
Sem resolução dinâmica, tracing de MOVE anterior, trim de nomes, CFG,
PossibleValues ou dependency fact. Nenhum irmão produtivo alterado. Diretórios
untracked preexistentes em artefatos-e2e preservados. Sem merge/auto-merge.
W1D = NOT_STARTED / NOT_AUTHORIZED. W2 = NOT_STARTED / NOT_AUTHORIZED.
