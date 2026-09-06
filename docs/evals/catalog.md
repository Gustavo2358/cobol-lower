# Catálogo de evals

Evals são contratos de prova, não testes já executados. Cada caso informa positivo, adversarial e oracle. Paths de testes serão registrados no checkpoint que os implementar; nunca apontar para testes fictícios. Estados e índices estão em [catalog.json](catalog.json).

<a id="EVAL-LWR-001"></a>
## EVAL-LWR-001 — Integridade do harness

**Propriedade:** Links/IDs/lifecycle/routing coerentes e nenhuma implementação não autorizada.

**Positivo:** Checkout documental íntegro com proposal pronta, active vazio e referências existentes.

**Negativo/adversarial:** Link quebrado, ID inexistente, work item duplicado/ativo completed, must_read inexistente ou scope futuro sem planned.

**Oracle:** Checker estrutural independente de conteúdo de produção, mais revisão semântica de autoridade.

**Etapa/gates:** `docs` / docs.
**Invariantes:** INV-LWR-001, INV-LWR-024.
**Estado:** SPECIFIED_NOT_IMPLEMENTED; sem evidência de execução runtime.

<a id="EVAL-LWR-002"></a>
## EVAL-LWR-002 — Destino AIR manual

**Propriedade:** Shape Entry→Return é representável e checks AIR cobrem o alvo mínimo.

**Positivo:** Publication manual de 1 Unit/Entry/Sequence e Return vazio, sem lowerer/decoder.

**Negativo/adversarial:** Label pendente, assinatura incompatível ou biblioteca não fixada; falha deve ser tipada.

**Oracle:** AirValidator real + assertions manuais de shape; não certificado integral de tradução.

**Etapa/gates:** `first_slice` / semantic.
**Invariantes:** INV-LWR-001, INV-LWR-004, INV-LWR-019.
**Estado:** SPECIFIED_NOT_IMPLEMENTED; sem evidência de execução runtime.

<a id="EVAL-LWR-003"></a>
## EVAL-LWR-003 — Fronteiras arquiteturais

**Propriedade:** Dependências entram no núcleo; AIR compartilhada e sem internals frontend.

**Positivo:** Core só usa tipos próprios/air-java e JDK permitido.

**Negativo/adversarial:** Path/JsonNode/annotation JSON no core, dependência de ProLeap/CFG, API ou modelo AIR paralelo.

**Oracle:** Imports + API/signatures + bytecode/dependency graph, com contracasos adequados a cada mecanismo.

**Etapa/gates:** `bootstrap` / architecture.
**Invariantes:** INV-LWR-002, INV-LWR-003, INV-LWR-004, INV-LWR-025.
**Estado:** SPECIFIED_NOT_IMPLEMENTED; sem evidência de execução runtime.

<a id="EVAL-LWR-004"></a>
## EVAL-LWR-004 — Porta em memória

**Propriedade:** Mesmo caso de uso recebe input tipado sem codec.

**Positivo:** Input manual imutável chama a porta e obtém resultado esperado.

**Negativo/adversarial:** Caso de uso serializa internamente para JSON ou depende de arquivo temporário para executar.

**Oracle:** Teste sem adapter/carregamento JSON no classpath do core; assertions de resultado.

**Etapa/gates:** `first_slice` / architecture, semantic.
**Invariantes:** INV-LWR-002, INV-LWR-005.
**Estado:** SPECIFIED_NOT_IMPLEMENTED; sem evidência de execução runtime.

<a id="EVAL-LWR-005"></a>
## EVAL-LWR-005 — Fixture upstream genuína

**Propriedade:** Input positivo é output real do frontend fixado, não JSON inventado.

**Positivo:** Fixture capturada com SHA do produtor, configuração, fonte autorizada e digest.

**Negativo/adversarial:** Payload manual rotulado real; SHA divergente; regeneração modifica expected silenciosamente.

**Oracle:** Manifesto/procedimento auditáveis e digest verificado; produção do frontend isolada fora do teste ordinário.

**Etapa/gates:** `first_slice` / semantic.
**Invariantes:** INV-LWR-003.
**Estado:** SPECIFIED_NOT_IMPLEMENTED; sem evidência de execução runtime.

<a id="EVAL-LWR-006"></a>
## EVAL-LWR-006 — Negociação e forma física

**Propriedade:** Separar schema/version unsupported de JSON inválido.

**Positivo:** 1.1.0 reconhecido e todos os campos válidos do envelope.

**Negativo/adversarial:** 1.0.0, versão futura, schema errado, propriedade duplicada, null físico ilegal, campo obrigatório ausente.

**Oracle:** Assertions de diagnóstico tipado/phase; nunca default automático para variante/versão.

**Etapa/gates:** `first_slice` / semantic.
**Invariantes:** INV-LWR-006, INV-LWR-018.
**Estado:** SPECIFIED_NOT_IMPLEMENTED; sem evidência de execução runtime.

<a id="EVAL-LWR-007"></a>
## EVAL-LWR-007 — Namespaces e unicidade

**Propriedade:** IDs locais não colidem entre unidades/owners.

**Positivo:** Inputs separados com local handles iguais mantêm identidades independentes.

**Negativo/adversarial:** Dangling reference, ID duplicado na mesma unit e start tipado que aponta a outra unit.

**Oracle:** Testes da porta em memória e do decoder dentro das formas físicas reais, sem inventar schema cross-unit.

**Etapa/gates:** `first_slice` / semantic.
**Invariantes:** INV-LWR-007.
**Estado:** SPECIFIED_NOT_IMPLEMENTED; sem evidência de execução runtime.

<a id="EVAL-LWR-008"></a>
## EVAL-LWR-008 — Entry→start

**Propriedade:** A referência explícita governa a entrada.

**Positivo:** Renomear handle conhecido e todas as referências preserva Entry→Return.

**Negativo/adversarial:** Start ausente/inexistente/indisponível; fake tenta escolher root/menor ID/array[0].

**Oracle:** Oracle manual source-start→label; cenário maior fora do perfil deve rejeitar sem fabricar start.

**Etapa/gates:** `first_slice` / semantic.
**Invariantes:** INV-LWR-008.
**Estado:** SPECIFIED_NOT_IMPLEMENTED; sem evidência de execução runtime.

<a id="EVAL-LWR-009"></a>
## EVAL-LWR-009 — Assinatura zero versus desconhecida

**Propriedade:** Return vazio não nasce de null, UNKNOWN ou PARTIAL.

**Positivo:** KNOWN/count0/ABSENT admite o perfil.

**Negativo/adversarial:** UNAVAILABLE/countnull/UNKNOWN, PARTIAL/count1, RETURNING PRESENT.

**Oracle:** Tabela de decisão independente com diagnósticos e ausência de Publication de sucesso.

**Etapa/gates:** `first_slice` / semantic.
**Invariantes:** INV-LWR-009.
**Estado:** SPECIFIED_NOT_IMPLEMENTED; sem evidência de execução runtime.

<a id="EVAL-LWR-010"></a>
## EVAL-LWR-010 — Tradução GOBACK→Return

**Propriedade:** Controle local é preservado mesmo quando AIR alternativa é bem formada.

**Positivo:** GOBACK CURRENT_PROGRAM_INVOCATION/NONE gera Return vazio e nenhum Halt/Jump.

**Negativo/adversarial:** Mutante gera Halt NORMAL ou usa filename/nome/nesting para decidir.

**Oracle:** Assertion de espécie/escopo de saída independente de AirValidator; nomes variados.

**Etapa/gates:** `first_slice` / semantic.
**Invariantes:** INV-LWR-010, INV-LWR-019, INV-LWR-023.
**Estado:** SPECIFIED_NOT_IMPLEMENTED; sem evidência de execução runtime.

<a id="EVAL-LWR-011"></a>
## EVAL-LWR-011 — Statement posterior e cardinalidade

**Propriedade:** Não criar fallthrough nem amputar input para manter sucesso.

**Positivo:** Fixture mínima satisfaz shape; propriedades de GOBACK preservadas.

**Negativo/adversarial:** GOBACK;CONTINUE ou vários statements recebem unsupported no perfil inicial, nunca sucesso filtrado.

**Oracle:** Admissão/inventário comparados com input conhecido; expansão plural futura possui oracle próprio.

**Etapa/gates:** `first_slice` / semantic.
**Invariantes:** INV-LWR-011, INV-LWR-012.
**Estado:** SPECIFIED_NOT_IMPLEMENTED; sem evidência de execução runtime.

<a id="EVAL-LWR-012"></a>
## EVAL-LWR-012 — Variante conhecida não suportada

**Propriedade:** Unsupported slice não é semântica inventada nem versão desconhecida.

**Positivo:** GOBACK tipado reconhecido; MOVE/IF/CALL/OBSERVED identificados como fora do slice.

**Negativo/adversarial:** OBSERVED com texto/shape GOBACK vira Return; variante nova ignorada por default.

**Oracle:** Diagnóstico estável com variante/ID e ausência de output de sucesso.

**Etapa/gates:** `first_slice` / semantic.
**Invariantes:** INV-LWR-012, INV-LWR-022.
**Estado:** SPECIFIED_NOT_IMPLEMENTED; sem evidência de execução runtime.

<a id="EVAL-LWR-013"></a>
## EVAL-LWR-013 — Entries alternativas abertas

**Propriedade:** PRIMARY_ONLY/PARTIAL não se converte em inventário global completo.

**Positivo:** Entry conhecida e gap ALTERNATE_ENTRIES_NOT_PROJECTED coexistem no output/relatório.

**Negativo/adversarial:** Mutante remove gap ou marca COMPLETE por existir apenas uma entry publicada.

**Oracle:** Oracle de cobertura/Uncertainty AIR independente do mapper.

**Etapa/gates:** `first_slice` / semantic.
**Invariantes:** INV-LWR-013, INV-LWR-014.
**Estado:** SPECIFIED_NOT_IMPLEMENTED; sem evidência de execução runtime.

<a id="EVAL-LWR-014"></a>
## EVAL-LWR-014 — Readiness e precisão por dimensão

**Propriedade:** Garantia local não promove effects/storage/values nem satisfaz AIR por enum.

**Positivo:** Controle local provado com limitações restantes explícitas.

**Negativo/adversarial:** Copiar SUFFICIENT→EXACT global; converter BLOCKED em NOT_APPLICABLE sem razão; gap só em log.

**Oracle:** Matriz normativa por dimensão/escopo revisada e assertions sobre Publication/relatório.

**Etapa/gates:** `first_slice` / semantic.
**Invariantes:** INV-LWR-013, INV-LWR-014.
**Estado:** SPECIFIED_NOT_IMPLEMENTED; sem evidência de execução runtime.

<a id="EVAL-LWR-015"></a>
## EVAL-LWR-015 — Provenance e convenções

**Propriedade:** Origem Entry/GOBACK e derivados não fabricam spans.

**Positivo:** Origem publicada correlacionada com Entry/Return; convenções confirmadas preservadas.

**Negativo/adversarial:** Usar path do JSON como fonte COBOL; inferir unidade/base/fim; inventar coordenadas ausentes.

**Oracle:** Mapeamento esperado manual + cenário de convenção não conhecida e limitação explícita.

**Etapa/gates:** `first_slice` / semantic.
**Invariantes:** INV-LWR-015.
**Estado:** SPECIFIED_NOT_IMPLEMENTED; sem evidência de execução runtime.

<a id="EVAL-LWR-016"></a>
## EVAL-LWR-016 — Imutabilidade e lifetime

**Propriedade:** Publicação fechada não depende de recursos/vistas vivas.

**Positivo:** Fechar/remover arquivo após decode não altera input/resultados; coleções não mutáveis.

**Negativo/adversarial:** Lista externa modifica snapshot ou callback abre fonte ao consultar referência.

**Oracle:** Testes de defensive copy/retention e architecture API gate.

**Etapa/gates:** `first_slice` / architecture, semantic.
**Invariantes:** INV-LWR-016.
**Estado:** SPECIFIED_NOT_IMPLEMENTED; sem evidência de execução runtime.

<a id="EVAL-LWR-017"></a>
## EVAL-LWR-017 — Substituição de adapter

**Propriedade:** Arquivo e memória chegam à mesma porta e preservam semântica.

**Positivo:** Fixture decodificada e input manual equivalente produzem mesma observação/correlação.

**Negativo/adversarial:** In-memory path exige JSON ou parser corrige semanticamente dados que core deixa passar.

**Oracle:** Comparador independente de observações, sem usar output de um caminho como única norma do outro.

**Etapa/gates:** `first_slice` / semantic.
**Invariantes:** INV-LWR-005, INV-LWR-016, INV-LWR-025.
**Estado:** SPECIFIED_NOT_IMPLEMENTED; sem evidência de execução runtime.

<a id="EVAL-LWR-018"></a>
## EVAL-LWR-018 — Determinismo e revisão

**Propriedade:** IDs/resultado não dependem de execução; revisões diferentes não se misturam.

**Positivo:** Duas construções independentes equivalentes; propriedades JSON permutadas.

**Negativo/adversarial:** Timestamp/UUID/identityHashCode; mesmo PublicationId para revisions distintas pelo nome da unit.

**Oracle:** Comparação semântica/ID conforme política fixada; não exigir estabilidade após edição.

**Etapa/gates:** `first_slice` / semantic.
**Invariantes:** INV-LWR-007, INV-LWR-017.
**Estado:** SPECIFIED_NOT_IMPLEMENTED; sem evidência de execução runtime.

<a id="EVAL-LWR-019"></a>
## EVAL-LWR-019 — Classes de falha e checker

**Propriedade:** Saída inválida/incompleta não é sucesso nem culpa automática do input.

**Positivo:** Checker real e relatório distinguem obrigações de erros.

**Negativo/adversarial:** Mutante ignora INVALID_IR/INCOMPLETE_VALIDATION ou repara removendo operação.

**Oracle:** Casos controlados e asserts do resultado/ausência de Publication de sucesso.

**Etapa/gates:** `first_slice` / semantic.
**Invariantes:** INV-LWR-018, INV-LWR-019.
**Estado:** SPECIFIED_NOT_IMPLEMENTED; sem evidência de execução runtime.

<a id="EVAL-LWR-020"></a>
## EVAL-LWR-020 — Regra geral e literatura

**Propriedade:** Escolha não trivial tem fonte, premissas e contracaso.

**Positivo:** Nota curta liga fonte verificada, algoritmo aplicável, complexidade e oracle.

**Negativo/adversarial:** Paper apenas nomeado, inferência de corpus como regra ou implementação gerando seu expected.

**Oracle:** Review humano/adversarial da nota e do teste; não simples busca por URL.

**Etapa/gates:** `all_semantic` / docs, semantic.
**Invariantes:** INV-LWR-020, INV-LWR-023.
**Estado:** SPECIFIED_NOT_IMPLEMENTED; sem evidência de execução runtime.

<a id="EVAL-LWR-021"></a>
## EVAL-LWR-021 — Custo proporcional

**Propriedade:** Índices/visitas não são repetidos quadraticamente.

**Positivo:** Input sintético N/2N; contadores dentro de limite derivado.

**Negativo/adversarial:** Scan integral por query, cópias profundas repetidas ou threshold de tempo como única prova.

**Oracle:** Contador estrutural independente; tempos são telemetria complementar.

**Etapa/gates:** `first_slice` / performance.
**Invariantes:** INV-LWR-021.
**Estado:** SPECIFIED_NOT_IMPLEMENTED; sem evidência de execução runtime.

<a id="EVAL-LWR-022"></a>
## EVAL-LWR-022 — Limites sem truncamento

**Propriedade:** Limite operacional impede sucesso parcial não declarado.

**Positivo:** Input dentro do limite e diagnóstico de limite fora dele.

**Negativo/adversarial:** Take(limit) apaga facts, overflow de contagem, memória/diagnóstico esconde restante.

**Oracle:** Inventário reconciliado e status IMPLEMENTATION_LIMIT, com seed/limites registrados.

**Etapa/gates:** `first_slice` / performance, semantic.
**Invariantes:** INV-LWR-012, INV-LWR-021.
**Estado:** SPECIFIED_NOT_IMPLEMENTED; sem evidência de execução runtime.

<a id="EVAL-LWR-023"></a>
## EVAL-LWR-023 — Extensão localizada

**Propriedade:** Nova família altera pontos de extensão explícitos, não a boundary de transporte.

**Positivo:** Contrato de evolução identifica adapter/input/rule/validator/eval afetados.

**Negativo/adversarial:** Registro escolhe primeiro handler ambiguamente ou faz catch-all silencioso.

**Oracle:** Dispatch exaustivo/contracaso de registro quando existir, mais review de dependências.

**Etapa/gates:** `expansion` / architecture, semantic.
**Invariantes:** INV-LWR-022.
**Estado:** SPECIFIED_NOT_IMPLEMENTED; sem evidência de execução runtime.

<a id="EVAL-LWR-024"></a>
## EVAL-LWR-024 — Falsificação e honestidade de evidência

**Propriedade:** Gates aceitam baseline e rejeitam defeito relevante; mutações não persistem.

**Positivo:** Baseline green → mutante red correto → restauração confirmada → green.

**Negativo/adversarial:** RED por setup, gate que sempre falha, teste pulado, evidência de outro SHA ou mutante commitado.

**Oracle:** Evidência/hash/diff/exit code e review; não autoassert narrativo.

**Etapa/gates:** `first_slice` / docs, semantic.
**Invariantes:** INV-LWR-023, INV-LWR-024.
**Estado:** SPECIFIED_NOT_IMPLEMENTED; sem evidência de execução runtime.

<a id="EVAL-LWR-025"></a>
## EVAL-LWR-025 — AIR JSON futuro

**Propriedade:** Codec externo preserva semântica/IDs/unknowns da Publication.

**Positivo:** Writer/reader independentes no binding fixado e equivalência com memória.

**Negativo/adversarial:** Nome de classe Java como schema, unknown perdido, tipo mudado ou draft anunciado accepted.

**Oracle:** Round-trip semântico + fixtures normativas + mutações de perda; não apenas writer-reader simétricos.

**Etapa/gates:** `transport_followup` / transport, architecture.
**Invariantes:** INV-LWR-025.
**Estado:** SPECIFIED_NOT_IMPLEMENTED; sem evidência de execução runtime.

<a id="EVAL-LWR-026"></a>
## EVAL-LWR-026 — Composição com CFG

**Propriedade:** Composição produz o controle esperado sem acoplamento dos cores.

**Positivo:** Publication Return do lowerer alimenta BuildCfg autorizado, preservando origem/gap/saída.

**Negativo/adversarial:** Lowerer importa CFG ou E2E green encobre erro de tradução local.

**Oracle:** Oracle E2E independente de produtores/consumidores; ambos mantêm suas suítes.

**Etapa/gates:** `integration_followup` / integration.
**Invariantes:** INV-LWR-026.
**Estado:** SPECIFIED_NOT_IMPLEMENTED; sem evidência de execução runtime.
