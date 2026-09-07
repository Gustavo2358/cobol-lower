# CP4 — FREEZE da prova vertical, custo e full

Autoridade: usuário, 2026-09-06, multi-checkpoint WORK-LOWER-001 CP0..CP5. Recovery CP3 d3867118e51a0654dcd995ae04e26ed68d124e99, certificado e recibo remoto adjacentes, check success confirmado 23:15:46.521797Z. Objetivo/evals/invariants/scopes/must_not_change do manifesto CP4 congelado. CP5 não iniciado. Mesma transação canônica, branch e PR; nenhum executor de sequenciamento novo.

Fontes primárias: mesmos SHAs do source lock e contratos SP/AIR do CP3. Superfície physical Wire e golden CP1 (2663 bytes, hash7ebce874bb98262598b908b176290368f738a21561c35a6ac342fc72e66d04ed), input/oracles SpFixtures/ManualAir, EntryGobackAdmission/LowerInput CP2/CP3 revisados. Políticas performance, observability/input safety, testing, falsification, gates, Git/recovery e catálogo de evals lidas; não reinterpretar COBOL.

## Regra vertical e limites

Driver FileLowering externo recebe Path e usa SpFileInput/SpJsonDecoder, chamando exatamente a mesma LowerInput uma vez por decode físico válido. Falha física não chama core; variantes conhecidas retidas não viram versão desconhecida. Comparar cada caminho com FIRST-LOWER/SpFixtures manual, além de igualdade entre os dois resultados. Nenhum frontend no classpath de runtime nem leitura do COBOL. Fechar/remover arquivo temporário de teste não muda Publication/correlação/relatório. Permutar recursivamente propriedades JSON, whitespace e filename do transporte conserva Publication; mudar fatos semânticos segue identidade CP3. Erros de start/assinatura e input maior têm mesmo resultado tipado/ausência de Publication; sem reparo no adapter.

Instrumentação de decoder é operacional, por execução, fora de SpInput/AIR/IDs. Nova observação medida pode envolver resultado existente sem alterar Decoded/Rejected. Contar bytes aceitos para processamento, nós JSON visitados na travessia e valores físicos não-null inspecionados, sem alegar instrumentação interna de Jackson ou SLA. decode comum usa a mesma execução; não existe decoder paralelo. Limites maxBytes/maxDepth/maxNodes continuam explícitos e sem truncamento de sucesso. A árvore inteira ainda é materializada dentro do limite de bytes; não alegar streaming.

## Família e oracle estrutural independente

Seed determinística: N GOBACKs de raiz, ids statement:0..N-1, programPoint0..N-1, roots completos, coverage COMPLETE/observed=modeled=N, mesmos fatos locais/readiness/provenance do golden, zero DATA/gaps de statements e uma entry PRIMARY conhecida cujo start aponta ao último statement. PRIMARY_ONLY/PARTIAL e gap alternativo preservados. N em 1/64/128/1024/2048; N=1 admite, N>1 UNSUPPORTED_SLICE sem prefixo AIR, snapshot integral. Geração sintética é teste, nunca rotulada captura real.

Oracles anteriores à instrumentação, derivados da estrutura contratual:

- JSON: 82 nós fixos +37N (36 nós por GOBACK, incluindo variant e parent null; 1 handle em roots). Golden N1=119. Conferência independente por walker de árvore de teste/contagem manual dos campos, não contador de produção.
- DTO physical não-null: 82+35N, retirando variant (discriminador, não componente record) e parent null por statement. Não contar property names como nós.
- Admissão CP2: visitas instrumentadas7N+9, referências consultadasN+1 (roots+start), componentes de provenance3N+3. Ledger: N*(statement+provenance3+checagem localizada+root+perfil)=7N; fixos unit/path2+coverage1+inventory gap1+entry/provenance4+perfil entry1=9. Esses contadores não medem todo ciclo de CPU/implementação JDK; review de loops/indexação completa a prova.
- Dobrar N conserva termo fixo e duplica termo variável. Byte count é comprimento real UTF-8, não count de caracteres. Medir tempo/heap somente como telemetria complementar identificada por JDK/OS/seed/tamanho; nunca como oracle ou fato AIR.

Índices existentes não serão trocados por scans nem o perfil será ampliado para a prova. Limite de visitas um abaixo do total retorna IMPLEMENTATION_LIMIT e retém snapshot completo. Limite de nós um abaixo retorna Rejected IMPLEMENTATION_LIMIT sem materialização parcial; tamanho exato passa o decoder. Byte limite um abaixo rejeita, exato passa. Depth deliberadamente insuficiente rejeita; diagnóstico limitado explicita truncamento de diagnósticos, não input.

Complexidade: decoder O(B+nodes+valores DTO) no trecho instrumentado + Jackson, admissão O(N+R+P+B) esperada como CP2, memória proporcional a input/índices + snapshots. Travessia JSON iterativa, DTO de shape finito/profundidade física limitada; sem caminhos/CFG. Limite de custo não é prova de semântica de famílias rejeitadas.

## Gates e CI

Performance terá executor real que roda suítes de escala core e adapter, exige dois marcadores de testes positivos não-zero e verifica todas as propriedades acima. Sem marker, marker0, falta de uma suíte ou exit não-zero bloqueiam PASS. Full exige exatamente o conjunto first-slice congelado docs/architecture/semantic/performance/full/git, exclui somente sua própria recursão e executa docs → semantic → performance → architecture → git → harness-tests → challenge; cada subgate uma vez. Semantic antecede architecture para haver classes no checkout limpo. Performance pode executar novamente os testes Maven como dependência da sua própria suíte; isso não duplica chamadas de full nem enfraquece regressão.

Full challenge inclui desafios documentais/arquiteturais existentes e mutações semânticas/custo em cópias temporárias: Return→Halt, PARTIAL→COMPLETE e consultas repetidas ao índice por statement. Cada caso exige baseline GREEN, RED pelo oracle esperado, restauração byte-for-byte/digest e segundo GREEN. Oracles/testes ficam executáveis. Positivos/contracasos do agregador provam ausência/falha de subgate e conjunto não encolhido; contagens de performance ausentes/zero bloqueiam. Outras mutações focais de CP4 podem ser executadas com o mesmo ciclo e evidência.

CI mantém job/workflow/check produtor/evento/SHA e source lock. Após bootstrap, executa full e verify-commit. G-GIT local mantém preflight de branch/escopo/PR. No modo publicado explícito (--commit SHA), checkout detached só é aceito com HEAD exatamente SHA, worktree limpa, certificado/trailer/diff/FREEZE verificados e PR/head correspondente, sem consultar o próprio check ainda pendente como se fosse completo. Não é fallback do modo local. Permissão adicional mínima pull-requests:read permite consulta do PR no CI; nenhum merge/write concedido.

| Contracaso independente | Resultado esperado |
| --- | --- |
| Memória e arquivo válidos/renomes/property order/lifetime | FIRST-LOWER correto e correlação/readiness/provenance/IDs iguais quando fatos iguais |
| Dangling start, assinatura incoerente, input maior | Mesma classe na porta; sem reparo/recorte/Publication |
| Versão futura, JSON inválido, limites físicos | PhysicalFailure tipada; zero chamadas da porta |
| N/2N, limites exato/um abaixo | Contadores conforme ledger, status e inventário integral; não SLA |
| Scan repetido por query | Contador de referências rompe N+1, RED específico |
| Full omitindo gate/falha de subgate, performance zero/ausente | FAIL; nunca executor inexistente como PASS |
| Published Git com SHA errado/dirty ou local detached | FAIL; não confundir CI detached comprovado com recuperação presumida |

Evals focais CP4 e gates docs/architecture/semantic/performance/full/git do manifesto. Regressão CP0..CP3 barata/cumulativa + fast/harness-tests; preservar todos os expected, a fixture real e regras/contratos já certificados. Review self-review integral desde CP3. Check remoto obrigatório checkpoint/.github/workflows/checkpoint.yml/github-actions/push no SHA publicado, prazo cumulativo1200s desde primeiro push de CP4. CP5 só após success terminal. Sem output AIR JSON, integração CFG, CLI rica ou nova capability.
