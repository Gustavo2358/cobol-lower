# CP1 — Self-review focal

Revisor/contexto: codex-root-WORK-LOWER-001-CP1, mesma sessão de implementação, portanto self-review. Candidato identificado exclusivamente pelo digest canônico em CP1.json. Base da revisão: último recovery CP0 1cc40112a7f57adb4a71861db8a101e6bec242dc. Review humano final do PR permanece obrigatório.

Escopo revisado: diff integral rastreado e arquivos novos de model/input, decoder/DTOs/materializer, suite, golden/intake, harness, manifesto/state/índices e evidência. Source lock e CP0 evidência/oracles não alterados. Sem frontend/parser/resolver/CFG no build ordinary/runtime; Jackson somente em adapters. SpInput é vocabulário de entrada próprio, não duplicação da AIR.

Findings resolvidos:

- Stub inicial rejeitava golden válido: RED esperado, depois materialização geral, sem regenerar expected.
- Scalar-to-String continuava aceito por Jackson apesar de ALLOW_COERCION_OF_SCALARS=false. Novos contracasos número/boolean em campo textual reproduziram falha semântica do decoder, exit 1. Configuração explícita Textual/Integer/Float/Boolean=Fail corrigiu a implementação, não o contrato. Mutação TryConvert novamente causou RED; restauração por hash e segundo GREEN.
- Verificador pós-commit antes apontava sempre CP0; agora resolve um único trailer seguro do próprio SHA. Stub CP0 fixo reprovou três testes pela causa esperada; positivos/negativos passaram após implementação. Check identidade/workflow/evento/producer/SHA permanece o mesmo.
- Com dois módulos testados, apenas uma contagem positiva podia ocultar decoder skip. Gate agora exige as duas suítes não zero após CP0. Mutante skip no adapter deixou Maven verde, mas gate reprovou com semantic tests absent/zero; restauração e segundo GREEN.
- Dois test doubles documentais dependiam do estado vivo do CP0. Agora constroem explicitamente current não autorizado e recibo ausente, preservando exatamente os expected negativos e a baseline positiva.

Falsificações: duplicatas ignoradas; null→zero; OBSERVED→GOBACK; adapter suite omitida; coerção textual reativada; sete desafios documentais/arquiteturais em cópia isolada. Todas com baseline GREEN, RED pelo oracle esperado, restauração integral e segundo GREEN. Logs adjacentes; nenhuma mutação de produção ou skip permanece.

Comparação contra writer: envelope completo e todas as cinco variantes possuem DTO físico estrito, opcionais anuláveis não viram missing; listas/records materializam snapshots. Golden integral mantém 2663 bytes e digest original. Testes adicionais exercitam DATA positivo no inventário, gaps localizados, branches, include chain, nomes Unicode, handles arbitrários e lifetime após remoção de arquivo. Nenhum desses sintéticos é rotulado golden ou sucesso de lowering.

Limites reais: CP1 não valida relações/contagens semanticamente nem admite publicação AIR. OtherStatement conserva header/variante/ocorrência para rejeição explícita; payloads MOVE/CALL/IF/OBSERVED são verificados fisicamente no adapter, sem claim de semântica consumida integralmente. EVAL-LWR-007 parcial: namespace materializado; validação cruzada é CP2. Custos/limites têm implementação operacional, prova N/2N continua CP4. Full não executado por não ser gate CP1. Certificação depende da regressão focal cumulativa e da checagem mecânica do candidato, não desta nota isolada.

Nenhuma mudança de must_not_change/capability, oracle enfraquecido, heurística por display name/ordem, callback a origem, modelo AIR paralelo ou atualização upstream encontrada. Registry/active/proposals/history/backlog/index revisados: um único active/branch/PR; nenhum próximo backlog iniciado.
