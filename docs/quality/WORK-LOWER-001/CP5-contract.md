# CP5 — FREEZE de fechamento
Congelado antes de implementação em 2026-09-07 UTC, após RECOVER CP4 a487c52faded88d773c22333481f3a54a3327d47 e check obrigatório success observado23:54:48Z, reconfirmado00:09:18Z. Worktree limpa, mesma branch/PR2/main14aaafc. Autoridade factual: pedido explícito do usuário nesta sessão de 2026-09-06, WORK-LOWER-001, modo multi-checkpoint CP0..CP5 sem reviews humanos intermediários e review humano final, sem merge/auto-merge/rewrite/capability adicional.

Objetivo/evals/gates/scopes/must_not_change: snapshot [manifesto](CP5-manifest.yaml), CP5, EVAL-LWR-001/020/024, docs/architecture/semantic/performance/full/git. Regredir todas as garantias CP0..4, preservar source lock, golden e código/oracles semânticos. Gates transport/integration continuam fora de escopo, não executados nem PASS.

## Fechamento e regra executável
Fonte primária local verificada: work-item-protocol, Encerramento antes do merge; agent-session-protocol e Git/review, fixados por bytes/revisão no certificado. Preparar fechamento local após review técnico self-review autorizado; isso não afirma aprovação humana nem remoto futuro. Remover os cinco arquivos ativos e promover conhecimento durável; histórico curto + registro completed com merge_status open_not_merged/review_status pending_human. Manifesto congelado é autorização/scope histórico, não novo trabalho ativo. Estado compacto final em CP5-state.md, sem transcript; SHA próprio e recibo remoto final serão confirmados no PR/handoff, sem commit recursivo.

Os executores hoje exigem active/work-item.yaml. Ajuste mínimo de lifecycle dentro de scripts/harness: resolver exatamente um registro pelo ID; active usa manifesto ativo, history completed usa manifesto congelado registrado, nunca scan de arquivo “mais recente” nem fallback. Validar paths relativos, schema/autorização concedida, CP final explicitamente autorizado, Git/registro/backlog/index consistentes; ausência/duplicata/estado divergente rejeita. Histórico só autoriza código se pacote de fechamento válido, não pela existência de lista não vazia. Evidência final deve estar registrada; docs pode conferir estrutura/identidade enquanto execução é preparada, mas certify/verify-commit continuam exigindo PASS real de todos os gates, mutações e review. Dependências anteriores exigem certificados e recibos remotos. O diff continua usando exclusões canônicas exatas; nenhuma nova exclusão.

Algoritmo finito: indexar registros, validar cada histórico por referências explícitas e dependências finitas de seus checkpoints, resolver manifesto único. O(N+R+B) nos registros/referências/bytes, sem rede em docs; limite: gate documental não revalida remote ao vivo nem certifica semântica humana. SHA, FREEZE, trailer, candidato e remote continuam em verificadores próprios.

## Oracles independentes antes do código
| Classe | Oracle |
| --- | --- |
| Ativo válido e fechado válido | manifesto correto, docs vazio de erros, dois módulos semânticos preservados |
| Registro ausente/duplicado/path externo | rejeição explícita, sem fallback active/history |
| Histórico sem autorização, sem CP final autorizado, Git divergente | HISTORY/AUTHORIZATION, nunca proteção por lista não vazia |
| Histórico sem evidência/dependência/recibo, backlog/index divergente | HISTORY/DEPENDENCY/INDEX; não é encerramento comprovado |
| Histórico final com certificado FAIL | certify/verify-commit rejeitam; docs não inventa PASS |
| Ativo completed, must_read ausente e demais CP0 contracasos | mesmos predicados; fixture ativa sintética isolada preserva oracles após remoção do pacote real |
| Return→Halt, PARTIAL→COMPLETE, índice→scan, transporte no core | oracles semânticos/custo/arquitetura existentes, RED específico e restauração |
| Bypass de validação de histórico ou seleção ativa fixa | novos testes focais devem falhar e voltar GREEN após restauração |

Nenhum esperado semântico será derivado do lowerer nem alterado. Testes de lifecycle usam cópias temporárias declaradas sintéticas; novo baseline testa checkout fechado real além dos antigos contracasos ativos. RED por import/setup não vale como falsificação. Baseline GREEN→mutação plausível→RED por causa esperada→restauração bytes→segundo GREEN, depois full cumulativo e self-review do diff CP4→CP5 e PR completo.

Remote obrigatório: checkpoint, .github/workflows/checkpoint.yml, github-actions, push, SHA publicado exato, completed/success. Limite1200s desde primeiro push. Pending/missing/fail/neutral/skipped/oldSHA bloqueiam. CP5 termina em review humano, sem backlog seguinte.
