# WORK-LOWER-001 — Plano de checkpoints

Seis checkpoints autorizados explicitamente em 2026-09-06 no modo multi-checkpoint; autoridade no manifesto/state. Todos seguem a [transação de sessão](../../../engineering/agent-session-protocol.md): no padrão, certificação e commit/push autorizados terminam em parada humana; no modo multi-checkpoint explicitamente autorizado, certificar, commit/push na mesma branch/PR e avançar somente sem stop condition e com próximo CP autorizado. As revisões focais abaixo integram REVIEW em ambos os modos; CP5 mantém parada humana final. Regressão cumulativa em todos os CPs; full/challenge em CP4 e CP5. As dependências externas são fixadas por SHA. Alteração dos nomes de módulos no CP0 exige atualizar o manifesto, não criar arquivos fora do scope silenciosamente.

## CP0 — Bootstrap reproduzível e target AIR independente

**Objetivo:** Build Java21/Maven contra air-java fixado e fixture AIR manual com Return validada; gates documentais/arquiteturais reais.

**Dependência:** Autorização humana; harness adotado/revisado no repo.

**Artefatos:** módulos core/adapters e resolução rastreável de air-java; G-DOCS/G-ARCH executáveis com contracasos; fixture AIR manual e smoke da boundary sem lowerer.

**Evals:** EVAL-LWR-001, EVAL-LWR-002, EVAL-LWR-003, EVAL-LWR-004.
**Gates:** docs, architecture, semantic, git; executar somente depois de existir executor real, não simular PASS.

**Fora de escopo:** Decoder, regra GOBACK de produção, output AIR JSON ou CFG.

**Revisão focal / fronteira CP0:** Bootstrap e admissibilidade do target. CP0 é bootstrap da trusted execution boundary: executores previstos e certificação documental, oracles positivos/negativos, falsificações restauradas e segundo GREEN devem estar provados e commitados antes de CP1, conforme protocolo; gate apenas especificado bloqueia.

## CP1 — Contrato real de entrada e adapter JSON

**Objetivo:** Capturar golden SP 1.1.0 real e materializar contrato interno sem transporte.

**Dependência:** CP0

**Artefatos:** golden com produtor SHA/config/digest e procedimento de captura; wire DTOs no adapter e tipos internos imutáveis; decoder com versão/forma/variantes e diagnósticos físicos.

**Evals:** EVAL-LWR-003, EVAL-LWR-005, EVAL-LWR-006, EVAL-LWR-007, EVAL-LWR-012, EVAL-LWR-016.
**Gates:** docs, architecture, semantic, git; executar somente depois de existir executor real, não simular PASS.

**Fora de escopo:** Produzir AIR pelo lowerer ou interpretar outras famílias.

**Revisão focal:** Contrato/decoder e evidência de fixture real.

## CP2 — Validação semântica da entrada e admissão do perfil

**Objetivo:** Mesma validação na porta em memória/arquivo distingue shape admitido de lacuna, contradição ou capability não suportada.

**Dependência:** CP1

**Artefatos:** validação de namespace/start/contagens/estados usados; predicado de admissibilidade minimal-entry-goback@1; resultados tipados sem publicação de sucesso fabricada.

**Evals:** EVAL-LWR-007, EVAL-LWR-008, EVAL-LWR-009, EVAL-LWR-011, EVAL-LWR-012, EVAL-LWR-013, EVAL-LWR-014, EVAL-LWR-019.
**Gates:** docs, architecture, semantic, git; executar somente depois de existir executor real, não simular PASS.

**Fora de escopo:** Escolher primeiro statement, validar todo SP por afirmação ou ampliar slice.

**Revisão focal:** Tabela de decisão e contracasos.

## CP3 — Regra Entry/GOBACK → Publication/Return

**Objetivo:** Construir AIR válida pela regra explícita, preservando identidade por revisão, provenance e lacunas.

**Dependência:** CP2

**Artefatos:** regra de tradução e correlações; Publication/Unit/Entry/Sequence/Return reais de air-java; coverage/uncertainties/claims por escopo e relatório com AirValidator.

**Evals:** EVAL-LWR-002, EVAL-LWR-008, EVAL-LWR-009, EVAL-LWR-010, EVAL-LWR-013, EVAL-LWR-014, EVAL-LWR-015, EVAL-LWR-018, EVAL-LWR-019.
**Gates:** docs, architecture, semantic, git; executar somente depois de existir executor real, não simular PASS.

**Fora de escopo:** Halt por contexto runtime, copiar readiness global ou apagar gap para checker passar.

**Revisão focal:** Tradução contra os dois contratos.

## CP4 — Prova vertical, escala e CI

**Objetivo:** Arquivo real→porta→AIR e input em memória produzem a mesma observação, com limites/custo e CI reproduzíveis.

**Dependência:** CP3

**Artefatos:** teste vertical sem frontend em runtime; contraprovas de adapter/lifetime/determinismo e escala; CI/contagem não zero/dependency tree/evidência de falsificação.

**Evals:** EVAL-LWR-003, EVAL-LWR-004, EVAL-LWR-005, EVAL-LWR-006, EVAL-LWR-011, EVAL-LWR-012, EVAL-LWR-016, EVAL-LWR-017, EVAL-LWR-018, EVAL-LWR-019, EVAL-LWR-021, EVAL-LWR-022, EVAL-LWR-024.
**Gates:** docs, architecture, semantic, performance, full, git; executar somente depois de existir executor real, não simular PASS.

**Fora de escopo:** Reader/writer AIR, build de CFG ou aumentar perfil para fazer E2E parecer maior.

**Revisão focal:** Primeiro proof point e limites reais, com full/challenge. O CI obrigatório deve terminar PASS no SHA publicado de CP4 antes de CP5; push confirmado sozinho não libera avanço.

## CP5 — Challenge final e handoff de fechamento

**Objetivo:** Revisar todas as claims e fechar documentação do primeiro slice com evidência ligada ao head final.

**Dependência:** CP4

**Artefatos:** revisão adversarial + segunda passagem verde; modelo público/capability/known limits atualizados; histórico curto, promoção de conhecimento e remoção de active após review/autorização aplicável.

**Evals:** EVAL-LWR-001, EVAL-LWR-020, EVAL-LWR-024.
**Gates:** docs, architecture, semantic, performance, full, git; executar somente depois de existir executor real, não simular PASS.

**Fora de escopo:** Aprovação/merge presumidos, iniciar próximo backlog ou anunciar AIR-STRUCTURE completo.

**Stop gate:** Review humano do fechamento; sem merge/auto-merge.

## Superfície arquitetural provável

Core com domain/application/ports e módulo adapter de entrada. O input interno é imutável e sem biblioteca JSON. Infraestrutura da fixture e do CI é externa às regras. IDs/coverage/provenance podem ser componentes separados quando a responsabilidade concreta justificar; não criar interfaces vazias para todas as famílias futuras.

## Migrações requeridas

Atualizar baseline apenas após comparação; registrar resolução reproduzível do SNAPSHOT. Converter scopes planned em paths reais conforme criação. Promover proposal para active na autorização e retirar active no fechamento. Não mudar contrato SP/AIR para acomodar implementação.

## Artefatos não antecipados

AIR JSON e CFG E2E possuem backlog próprio. O handoff final dirá exatamente o que pode ser consumido pelo CFG e os gaps ainda presentes.
