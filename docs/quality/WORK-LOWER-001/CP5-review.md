# CP5 — self-review de fechamento e PR completo

Review técnico pelo mesmo contexto implementador, passada adversarial separada; **self-review**, não independent review nem aprovação humana. Base focal CP4 a487c52faded88d773c22333481f3a54a3327d47; base do PR14aaafc5051eb287af6a8f126e8eaff78e30e365. Identidade exata do candidato permanece exclusivamente no campo candidate_diff_sha256 do certificado; esta nota não duplica SHA/digest próprio.

## Findings resolvidos

Gates assumiam manifesto em active mesmo após remoção exigida pelo lifecycle. Resolver agora usa registro único ativo ou histórico fechado explicitamente ligado ao snapshot autorizado; ausência/duplicata/path externo rejeita. Lista histórica não vazia antes podia dispensar autorização: novos contracasos geraram RED real e checagem de schema/autoridade/CP final/Git/evidência/FREEZE/dependências/backlog/index fecha essa lacuna. Falsificação de bypass gerou11 falhas esperadas; restauração integral/segundo GREEN. Revisão substituiu count/scan repetido dos registros por índices, mantendo regra/custo do FREEZE; desafio repetido após esse ajuste.

Fixture fechada inicial tinha link ativo residual e foi corrigida antes do RED válido; não conta como falsificação. Diretório vazio remanescente de apply_patch foi removido por rmdir explícito, sem dados adicionais. Leituras exploratórias de paths inexistentes não foram gates nem PASS. Documentação antiga dizia “não implementado”, CP2 ativo ou performance/full pendentes; corrigida por evidências CP0..4, sem afirmar remoto antecipado de CP5. Nome conceitual UNSUPPORTED_VERSION agora registra correspondência exata ao enum físico UNSUPPORTED_CONTRACT.

## Escopo e adversarial review

Diff integral CP4→candidato revisto: somente harness de fechamento, testes e documentação/evidência em scopes autorizados. Core/adapters, seus oracles, pom/workflow/source lock/golden têm diff vazio contra CP4. Nenhum must_not_change tocado; snapshots upstream isolados limpos. Catálogos comparados estruturalmente: regras, propriedades, positivos, negativos e oracles originais idênticos; somente status/path/evidência/limite de enforcement mudou. Evals023/025/026 e invariantes de capacidades futuras não viraram suporte. Review de fontes/algoritmos (EVAL020) é self-review, não busca por URL ou aprovação humana.

PR completo revisto por responsabilidade: SpInput imutável/plural, validação/admissão íntegra antes da construção; materialização mecânica e JSON/reflexão/Path somente adapters; índices explícitos e zero/null/readiness/gaps distintos; Return não Halt; lookup de start por ID inteiro; identidade canônica injetiva limitada sem hashCode/relógio; original/expanded e limitações preservadas; AirValidator compartilhado sem modelo/checker paralelo; resultados atômicos e sem reparo. Build/CI têm testes não zero, dependência rastreável e certificado/trailer/head remoto específicos. Oracles manuais/captura real e testes negativos/falsificações não geram expected pelo lowerer. Sem sequenciamento por nome/ordem, parser/resolver/frontend/CFG no runtime, capability além do perfil ou resíduo de mutante.

Lifecycle: pacote ativo removido, histórico compacto + registry/backlog/index coerentes; snapshot congelado preservado como autorização passada, não trabalho ativo. Estado final em CP5-state.md. Essa remoção não elimina evidência: spec/plan/eval estão no Git por SHA do FREEZE. Fechamento local completed é distinto de certificação local, push/check e review humano; sem merge/auto-merge/rewrite. Mesmo PR2, nenhuma task de backlog seguinte iniciada.

## Verificação e limites

Primeiro full exit0: 779 assertions semânticas, 95 adicionais de escala,89 testes harness; 10 desafios docs/architecture/lifecycle e3 semânticos/custo restaurados. Mutação adicional HISTORY-BYPASS exit1 pela causa esperada, restauração e testes exit0. Regressão final full/fast deve terminar antes da certificação; os comandos/logs/resultados finais são os do certificado, não inferidos desta nota.

Limites preservados: shape mínimo; superfície comum de variantes rejeitadas, não SP inteiro; coverage alternativa parcial; dimensões não disponíveis não promovidas; provenance sem site/coordenadas suficiente fica limitada; IDs potencialmente longos; árvore JSON sob limite de bytes, contadores sem SLA/internals JDK/Jackson; diagnóstico físico I/O sanitizado e genérico; AirValidator estrutural com obrigações não prova tradução/perfil integral. Harness não substitui review humano e o suporte de histórico fecha o estado pré-merge autorizado deste item, não automatiza futura gestão de review/merge. Nenhum finding sem correção exigida no escopo permaneceu aberto; limitações não são claims de PASS.

Próximo passo não iniciado: review humano adversarial do PR e decisão humana. CP5 só será recovery pleno após certificado, commit/push e check obrigatório success no SHA exato dentro de1200s.
