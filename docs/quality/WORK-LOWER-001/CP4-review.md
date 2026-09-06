# CP4 — Self-review focal

Resultado final da regressão após o finding: full e fast exit0, 779 assertions semânticas, 95 adicionais de escala (874 durante performance), 73 testes harness; todos os desafios restaurados. Evidência focal por componente usa o log integral do agregador, sem alegar comandos individuais não executados. Docs/certificação serão conferidos novamente sobre o candidato stageado.

Contexto codex-root-WORK-LOWER-001-CP4; segunda passagem do mesmo agente, não independent review. Diff integral desde d3867118e51a0654dcd995ae04e26ed68d124e99, candidato ligado somente a CP4.json#/candidate_diff_sha256.

Escopo: FileLowering e métricas operacionais do decoder; suítes vertical/escala core/adapters; agregação/gates/challenges/contracasos Git publicado; workflow; documentação de execução/FREEZE/evidência/recibo CP3/estado/índice. Nenhuma mudança de produção na admissão CP2, tradução/identidade/provenance CP3 ou AirValidator. SpFixtures/ManualAir/golden/source lock preservados. Nada de AIR writer, CLI rica, CFG, AST, resolução, contratos upstream ou capability nova.

FileLowering somente decodifica e chama LowerInput uma vez; erro físico não chega ao core. Cada caminho válido foi confrontado com oracle independente e depois comparado integralmente. Caso contraditório não é reparado. Property order/whitespace/arquivo removido não alteram payload semântico, identidade, correlação ou relatório. Limite de arquivo não aceita prefixo válido com resto oculto. Métricas não entram em SpInput/AIR/IDs, mantêm estado por chamada e distinguem bytes, nós JSON e valores physical.

Ledger congelado N/2N confirmado sem ajuste: visitas7N+9, referênciasN+1, provenance3N+3; JSON82+37N, physical82+35N; tamanhos1/64/128/1024/2048. Família plural permanece UNSUPPORTED_SLICE com snapshot integral. Limites exato/um abaixo e profundidade/diagnósticos preservam atomicidade; tempos/heap são telemetria, não SLA. Contadores instrumentam trabalho declarado, não todo o internals do parser/JDK; análise dos loops/índices continua necessária.

REDs válidos: driver sem chamada de porta, agregador vazio/contagens ausentes e métricas decoder zero. Primeira execução isolada de test_full teve ModuleNotFoundError de setup; corrigido sys.path conforme os demais testes antes do RED real, sem contar como falsificação. Expected/ledger não foram relaxados. Positivos passaram após implementação.

Falsificações restauradas: Return→Halt, PARTIAL→COMPLETE, índice→scan, full omitindo performance, limite aceitando prefixo e Git publicado omitindo certificação; mais sete desafios docs/architecture. Todas baseline0→RED1 esperado→restauração por bytes/digest→segundo GREEN0. Oracles/scripts permanecem executáveis; cópias temporárias descartadas somente após restauração, sem alterar recovery.

Finding resolvido após o primeiro full: Git publicado verificava candidate_errors, mas não certificate_errors diretamente, contrariando FREEZE. Contracaso novo reproduziu aceitação indevida de registro não certificado; checagem adicionada sem retirar gates/oracles. Mutação reversa detectada e restaurada; regressão full deve ser repetida antes de certificar. Modo local não foi enfraquecido; modo publicado exige SHA literal, HEAD/worktree, certificado e PR/head. Permissão CI adicional somente pull-requests:read.

Revisão de lifecycle: um único item active, mesma branch/PR, CP0..CP3 com recibos/recovery, CP4 em certificação; CP5 não iniciado. Índice/registry/backlog/proposals/history coerentes, sem antecipar fechamento humano. Nenhum finding bloqueante restante. Full/gates/CI locais e remotos são provas distintas; publicar e esperar o check exato ainda é obrigatório.
