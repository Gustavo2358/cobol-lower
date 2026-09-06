# CP3 — FREEZE da regra Entry/GOBACK

Autoridade: pedido explícito do usuário em 2026-09-06, multi-checkpoint WORK-LOWER-001 CP0..CP5. Recovery CP2 c5546c9f4a318f31bcfb3c79a91a4b7d7d71149f e recibo adjacente reconfirmado às 22:52:51Z. Objetivo, evals, invariants, scopes e must_not_change: manifesto congelado CP3-manifest.yaml; transação canônica, sem CP4 antecipado.

Fontes primárias verificadas no source lock imutável: SRC-SP c8a891e0827ae1dc1140246f625fd16c2ac9bd97, docs/domain/cobol-semantic-product.md (Entry/GOBACK e provenance base1/base0 Unicode code points/fim inclusivo); SRC-AIR 122ce54e1b9ef9b00646f93ece409ca8b63bc933, especificacao/01-modelo-e-identidades.md §§1–4,9; 04-operacoes.md §8; 06-incompletude-e-proveniencia.md integral; SRC-AIR-JAVA 6a4091e5394fc22b3d2ada9abbdb530eb3572a58, Publication/Unit/Sequence/Ids/Origins/Evidence/Capabilities e ValidationResult/ValidationOptions/ValidationIssue. Contratos locais de target/identidade/coverage/resultados e oracle FIRST-LOWER governam o uso, não substituem as autoridades.

## Regra e algoritmo

A porta LowerInput recebe snapshot SP e limites explícitos. Chama EntryGobackAdmission em toda execução, também em memória. Falha de admissão conserva snapshot/diagnósticos, sem Publication. Somente ADMITTED permite mapear todos os statements admitidos por ID namespaced para label/Return; Entry.initialLabel vem do join do start publicado nessa tabela, nunca get(0), menor handle, nome ou ordem física. Usar exclusivamente modelos compartilhados; zero objetos/storage deriva de zero DATA; assinatura vazia fechada deriva de KNOWN/0/ABSENT.

Output: uma Publication AIR2, Unit AVAILABLE, Entry e Sequence, zero instruções comuns, um Return sem valores. Não Halt/Jump/CFG. Capabilities AIR vazias: não declarar perfil AIR inteiro; perfil local identificado no relatório. AirValidator real obrigatório na saída, resultado integral preservado. INVALID_IR → OUTPUT_INVALID; INCOMPLETE_VALIDATION → VALIDATION_INCOMPLETE; só STRUCTURALLY_VALID pode SUCCESS. Obrigações não são fatos provados. Falha não devolve Publication de sucesso nem apaga operações para reparar output.

## Identidade canônica sem hash

Função de revisão injetiva, não digest: serialização interna explícita dos fatos tipados admitidos, independente de JSON/reflection/toString/hashCode. Tokens de strings são comprimento decimal UTF-16 + ':' + quatro dígitos hex por code unit; números/enums/booleanos são tokens com representação decimal/name/true|false, listas levam comprimento e opcionais levam presença. Ordem dos campos é a ordem declarada em SpInput e records; listas mantêm ordem contratual publicada. Todos os campos do input admitido entram, inclusive policy/readiness/provenance/gaps/structure; famílias não admitidas nunca recebem identidade de publicação.

Prefixo de PublicationId identifica minimal-entry-goback@1, AIR2/SP1.1 e canonical-v1. Revisão, identidade da unit e política fonte entram nos tokens. Limites/telemetria de execução não entram. Sem compressão/hash: strings diferentes não colidem; custo O(B), tamanho O(B), sendo B caracteres/fatos visitados. Limite explícito de caracteres canônicos aborta com IMPLEMENTATION_LIMIT antes de retornar publicação; não usar hash alternativo. IDs locais AIR distinguem kind/regra e handle codificado, dentro do namespace completo. Uma única Unit pode ter localId fixo porque o namespace da publicação identifica revisão e unit integralmente. Correlações usam os tipos SP e AIR completos, não strings locais como chave global.

## Provenance e dimensões

Cada entry/statement tem Written original e Written expanded com artefatos lógicos separados por papel; Derived correlaciona ambos com regra explícita. Labels/Unit usam Derived da ocorrência apropriada, sem span novo. Span disponível usa números publicados, lineBase1/columnBase0/UNICODE_SCALAR/endExclusive=false. Zero de linha ou intervalo não representável não vira precisão: localização AIR ausente, exact=false e limitação tipada; snapshot retém números brutos. Isso não reclassifica controle conhecido. Include frames mantêm incluindo/incluído/requestedName; apenas includeLine não permite inventar coluna, portanto site ausente e limitação explícita, linha bruta no relatório. Nunca abrir file de provenance ou fabricar digest de artefato.

Coverage de Unit e Publication PARTIAL com razão qualificada cobol-lower:ALTERNATE_ENTRIES_NOT_PROJECTED em escopo amplo honesto. Items MODELED correlacionam entry e statement; nenhum gap necessário fica só no relatório. Return CONTROL EXACT (saída da ativação corrente); STORAGE/EFFECTS/VALUES/DEPENDENCIES UNAVAILABLE com razões distintas no escopo da operação: este perfil não certifica essas dimensões a partir do readiness de controle. Mesmo values=[] conhecido não afirma valores globais exatos. Sem NOT_APPLICABLE para esconder não implementado. Raw input/readiness integral retido no relatório. Ausência de relações/recursos não é claim global de ausência sob coverage parcial.

## Oracles independentes anteriores ao código

| Classe | Observação esperada |
| --- | --- |
| SP manual equivalente ao golden | SUCCESS, shape FIRST-LOWER, correlação Entry/start/Return exata, dois papéis de provenance, PARTIAL e gap AIR |
| Rename coerente de handles/unit/files | Mesma espécie Return; namespace de revisão distinto; referências fechadas |
| Construção independente, limites operacionais diferentes suficientes | Publication igual; nenhuma identidade de relógio/objeto/execução |
| Mudar policy, provenance, readiness scope ou ordinal sem invalidar fatos | PublicationId diferente, controle equivalente |
| Tokens ambíguos por concatenação, Unicode suplementar e delimitadores | Identidades distintas e determinísticas; sem colisão por separador |
| Input null/contraditório/fora do shape/desconhecimento | Mesmo status de admissão sem Publication, snapshot e diagnósticos preservados |
| Origem com linha zero/reversed, exact false, include chain | Sem span fictício; números/linha include preservados no relatório, limite explícito |
| AIR inválida manual independente | OUTPUT_INVALID e issues integrais; nenhuma Publication de sucesso |
| Limite real de AirValidator | VALIDATION_INCOMPLETE e VALIDATION_LIMIT integral; não SUCCESS |
| Limite da identidade um abaixo/tamanho exato | IMPLEMENTATION_LIMIT sem output / sucesso igual ao baseline |
| Trocar Return por Halt | AirValidator pode aceitar, oracle de correlação/término deve RED |
| PARTIAL convertido COMPLETE; gap removido | Oracle de coverage/gap deve RED, mesmo com checker estrutural verde |

Expected não vem de execução do lowerer. ManualAir CP0 e SpFixtures CP2 permanecem independentes e inalterados. Testes adicionais fazem inspeção direta de classes/referências/claims/coordinates e resultados do checker; não implementam outro validador AIR de produção. Falsificações após GREEN incluem Return→Halt, gap→COMPLETE, ignorar resultado inválido/incompleto, identidade ignorando revisão, além dos sete desafios documentais/arquiteturais. Exigir causa esperada, restauração por digest e segundo GREEN.

Terminação: admissão finita CP2 + percurso finito de snapshot, criação linear de artefatos/frames e índices; O(N+R+P+B) esperado, sem caminhos/CFG, sem recursão de input arbitrário. Limites interrompem sem publicar prefixo. Complexidade do checker é a do runtime fixado e seus limites explícitos, não claim de prova completa.

Gates obrigatórios CP3: docs/architecture/semantic/git; reexecutar fast/harness-tests e suíte cumulativa CP0..CP2. Review self-review integral desde recovery, sem independent review presumido. Remoto obrigatório checkpoint/.github/workflows/checkpoint.yml/github-actions/push/SHA exato, prazo cumulativo 1200s desde primeiro push deste CP. Full/performance e E2E arquivo→AIR permanecem CP4.
