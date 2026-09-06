# Gates: contratos e estado real

**CP0 implementa executores de docs, architecture, fast, semantic e git**, com positivos e contracasos; estado por gate em [gates.json](gates.json) e evidência de checkpoint em [CP0](../quality/WORK-LOWER-001/CP0.json). Enforcement automatizado desses mecanismos não certifica antecipadamente o checkpoint nem o slice completo. Performance/full/transport/integration continuam `SPECIFIED_NOT_IMPLEMENTED`. Execução, certificação local e resultado remoto são estados distintos.

Entrypoints: `python3 scripts/harness/run.py <gate>`. Java 21, Maven e dependências Python fixadas em `scripts/harness/requirements.txt` são necessários. Configurar `LOWER_BUILD_ROOT` para diretório temporário isolado e executar `bootstrap` para resolver/construir/testar air-java pelo SHA do lock. `semantic` executa o build e exige contagem de assertions não zero; `architecture` examina sources, API, bytecode/jdeps e árvore Maven. `harness-tests` exercita os contracasos; `python3 scripts/harness/challenge.py` demonstra restaurações. `certify` exige evidência pre_commit completa mais digest/index/FREEZE; `verify-commit --commit <SHA>` resolve trailer e diff publicado; `remote --commit <SHA>` confirma o único PR, push e checks do SHA.

G-DOCS verifica o grafo documental corrente; os handoffs em `docs/sources/history` são arquivo, com integridade de bytes assegurada pelo source lock, sem transformar seus links históricos em roteamento atual. G-ARCH automatiza dependências e nomes concorrentes explícitos; review continua necessário para identificar duplicação semântica sob outro nome. EVAL-LWR-004 em CP0 é somente o smoke em memória da validação AIR; decoder e porta de lowering permanecem nos CPs seguintes.

| Gate | Contrato | Ativação |
| --- | --- | --- |
| `docs` / G-DOCS | Links, anchors, IDs, JSON/YAML, registry/lifecycle, source lock e ausência de implementação não autorizada. | `docs` |
| `architecture` / G-ARCH | Source/imports, API, bytecode e dependências: core sem transportes/ProLeap/CFG/modelo AIR paralelo. | `bootstrap` |
| `fast` / G-FAST | Agrega docs e architecture quando este existir; perfil docs-only usa docs e declara arquitetura runtime indisponível. | `bootstrap` |
| `semantic` / G-SEMANTIC | Testes reais input/core/adapters, oracle independente, AIR checker, readiness, provenance e determinismo; contagem >0. | `first_slice` |
| `performance` / G-PERFORMANCE | Contadores de traversal/indexação e limites explícitos; telemetria não substitui propriedade. | `first_slice` |
| `transport` / G-TRANSPORT | Writer/reader independentes contra binding fixado; versão, round-trip e ausência de dados voláteis. Não faz parte do primeiro slice. | `transport_followup` |
| `integration` / G-INTEGRATION | Composição explicitamente autorizada preserva Publication/controle/diagnósticos. | `integration_followup` |
| `full` / G-FULL | Agrega todos os gates obrigatórios do perfil/work item e challenge final; especificar manifestamente o conjunto. | `first_slice` |
| `git` / G-GIT | Branch/base/head/working tree/PR e evidências de review/merge com fontes confiáveis; não consulta remota no G-DOCS. | `all` |

## Estado de execução e enforcement

Enforcement: SPECIFIED_NOT_IMPLEMENTED → IMPLEMENTED_UNVERIFIED → AUTOMATED_VERIFIED, somente com executor real e evidência positivo/negativo. Cada execução possui status PASS/FAIL/NOT_RUN/BLOCKED/NOT_APPLICABLE, SHA e escopo. NOT_APPLICABLE exige razão contratual e não pode mascarar gate obrigatório ausente.

O pacote foi verificado por um checker temporário de documentação durante sua criação; isso não instala G-DOCS no futuro checkout. O primeiro checkpoint deve implementar o gate local e refazer seus contracasos, não copiar um resultado PASS deste ZIP.

## Contrato de G-DOCS

Offline e sem inference de GitHub. Verificar links relativos/anchors, existência de must_read, unicidade/fechamento de IDs de ADR/invariantes/evals/backlog, referências entre catálogos, JSON/YAML parseáveis, status e localização de work items, ausência de completed em active, registry/index coerentes, propostas sem autorização automática e hashes/paths de fontes. Scopes planejados não equivalem a arquivos existentes. Rejeitar caminhos externos absolutos e scripts/POM/Java em entrega declarada docs-only.

A certificação segue a [transação](agent-session-protocol.md). O bootstrap de G-DOCS deve conferir modo/lista/autoridade pelo schema e relações com plano/state (IDs existentes, current_checkpoint na lista autorizada e dependências anteriores certificadas antes de iniciar); evidência obrigatória preenchida, resultados PASS, digest do candidato, restauração, segundo GREEN, regressão cumulativa e review identificado. Rejeitar `NOT_RUN` obrigatório, mutação não restaurada e claim independente sem outro revisor/contexto e evidência. A checagem documental verifica registros e referências; não prova a verdade dos logs nem substitui execução dos oracles. G-GIT confere vínculo do commit/trailer/digest e confirmação remota.

Para evitar circularidade, distinguir validação de registro em elaboração de certificação final: G-DOCS pode validar forma/referências do candidato ainda pendente, mas não rotulá-lo certificado. Depois dos gates locais, a checagem de certificação pré-commit exige todos os resultados obrigatórios locais e rejeita pendências; a finalização apenas de evidência/state exige revisão documental final. G-GIT verifica branch/base/worktree/PR antes do commit; confirmação do SHA publicado e dos checks remotos obrigatórios em terminal PASS é pós-commit e bloqueia ADVANCE, sem exigir conhecer o SHA futuro antes de criá-lo. O bootstrap deve provar ambas as fases, inclusive rejeição de tentativa de avanço com push não confirmado ou CI obrigatório pending/missing/fail. Certificado pré-commit é local; só vira recovery point completo após essas obrigações remotas. G-DOCS rejeita identidade duplicada no review e referências FREEZE incompletas; G-GIT verifica bytes/hashes e o único digest canônico conforme protocolo de sessão.

No bootstrap autorizado, schema e executores verificam restrições estruturais e relacionais. Contracasos de CP0 incluem multi-checkpoint sem lista/autoridade, múltiplos CPs no modo padrão, certificação com gate ausente/falho, regressão omitida, digest divergente e self-review apresentado como independente. Revisão da prosa deve preservar default, stop conditions e ausência de autorização nas propostas; schema sozinho não compreende essas claims.

Falsificações mínimas: link quebrado; invariant inexistente em eval; active completed; required must_read inexistente. Checker precisa aceitar a baseline para ser considerado válido.

## Perfis

`docs-only`: verificação documental e higiene Git contextual; sem alegar runtime. `first-slice`: docs, architecture, semantic, performance, full e Git. `full` agrega os outros gates obrigatórios uma vez, não se chama recursivamente. O teste de entrada JSON pertence a semantic do primeiro slice. G-TRANSPORT se refere à saída AIR/reader interoperável, não é exigido para a primeira Publication em memória.

`air-transport` acrescenta G-TRANSPORT. `pipeline-integration` acrescenta G-INTEGRATION. O manifesto fixa perfil e conjunto antes de executar; não remover subgate depois de falha para anunciar green. Um gate novo ainda não implementado bloqueia a conclusão do checkpoint que o exige.

## CI e espera remota

O workflow do bootstrap parte de checkout limpo, resolve air-java no SHA, verifica número real de testes, executa gates e contracasos e confere o commit com a evidência. Logs/exit codes ficam no run remoto. Cache não substitui source lock. Node não é dependência de aplicação/build; ações do provedor podem usá-lo internamente. Gates de parser do ProLeap não pertencem ao lowerer.

Checks remotos obrigatórios devem ser enumerados no FREEZE com identidade verificável (workflow/job/contexto e produtor esperado) e `remote_wait_limit_seconds` positivo, medido desde o primeiro push do CP, sem reiniciar a espera por reruns; incluir os exigidos pelo plano/perfil e pelo PR. Não descobrir obrigatoriedade apenas pela lista de runs existentes, que pode estar vazia por workflow defeituoso. CP4 prova CI e exige essa espera antes de CP5; CP5 também espera os checks finais do seu SHA.

Após push, G-GIT consulta metadata confiável: cada check obrigatório deve terminar PASS/success para o SHA publicado. Pending, missing, fail, cancelled, skipped, neutral, timeout ou estado desconhecido impedem ADVANCE; resultado de SHA antigo não vale. Se o provedor testa merge sintético, exigir vínculo verificável ao head publicado e base testada, não equivalência presumida. Correção local cria novo SHA e invalida a observação anterior. Ausência de CI obrigatório só é NOT_APPLICABLE quando justificada pelo contrato congelado. No limite de espera sem prova, registrar blocker e parar conforme protocolo; não remover checks do conjunto.

O recibo remoto vive no PR/handoff e é roteado no state seguinte conforme recovery, evitando commit recursivo para registrar o CI do próprio commit. O bootstrap do verificador deve exercitar respostas controladas: success no SHA errado, pending, check ausente, falha e todos os obrigatórios em success no SHA correto. Isso não exige fornecedor específico nem implementa o verificador nesta adaptação.

## Evidência

Registrar comando, cwd lógico, SHA, ambiente, saída/contagem, exit code, subset/perfil, contracaso e restauração. FAIL por setup não mata um mutante semântico. PASS documental não prova semântica. Não anunciar branch protection configurada só porque o workflow existe.
