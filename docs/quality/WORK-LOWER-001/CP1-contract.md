# CP1 — FREEZE do contrato e decoder

Autoridade: pedido explícito do usuário em 2026-09-06, WORK-LOWER-001, modo multi-checkpoint, CP0..CP5. Aplica a transação canônica, sem protocolo alternativo. Recovery de CP0: 1cc40112a7f57adb4a71861db8a101e6bec242dc, check checkpoint/github-actions/push success no SHA, consulta em 2026-09-06T21:55:31.726660+00:00; recibo adjacente. Nenhum trabalho material de CP2 será iniciado.

Objetivo, evals EVAL-LWR-003/005/006/007/012/016, todos os invariants/scopes/must_not_change e gates docs/architecture/semantic/git constam do manifesto congelado. CP1 materializa o contrato e rejeita erros físicos; validação semântica/admissão é CP2, tradução é CP3. EVAL-LWR-007 aqui prova namespace materializado, não validação relacional ainda inexistente. Nenhuma AIR é produzida pelo decoder. Full/performance permanecem CP4/CP5.

Fontes primárias verificadas no SRC-SP c8a891e0827ae1dc1140246f625fd16c2ac9bd97: docs/domain/cobol-semantic-product.md integral; SemanticProductJsonWriter.java integral (document/DTOs/variantes); CobolSemanticProduct.java, enums e records de unit/entry/provenance/readiness e IDs; SemanticProductEntryGobackTest.java integral; README.md e pom.xml. O comando público Maven gerou o golden completo (2663 bytes, SHA-256 7ebce874bb98262598b908b176290368f738a21561c35a6ac342fc72e66d04ed), após 20 testes focais upstream PASS. Não há paths pessoais no payload; fonte pública sintética autorizada. Procedure disponível apenas offline. Source lock não muda.

Regra física: UTF-8 JSON completo, schema cobol-semantic-product e versão exata 1.1.0. JSON inválido/duplicatas/types/null ilegal/missing/unknown properties/unknown enum ou variante são INPUT_ERROR físico; schema/versão diferente são UNSUPPORTED_CONTRACT. Negociação não depende de defaults. Campos desconhecidos são rejeitados em todos os objetos, inclusive payloads de variantes conhecidas. Null é permitido somente onde o writer publica opcionais: picture, containment.parent, start.statement, signature.parameterCount, binding.selected, IF.continuation. Campos obrigatórios não podem faltar mesmo se anuláveis. Não há coerção string→número/boolean nem float→inteiro. Rejeitar trailing documento, prefixo/truncamento e JSON polimórfico por nome de classe.

Forma de todas as variantes conhecidas (MOVE/CALL/IF/OBSERVED/GOBACK) será verificada. Para variantes fora do perfil, conservar cada ocorrência/header/variant e diagnóstico explícito com ID; não promover a GOBACK por kind/shape/texto e não afirmar semântica validada de seus payloads. DATA permanece inventário positivo. O contrato interno poderá restringir payloads das famílias fora do slice à superfície necessária à rejeição, explicitamente rotulada; nunca filtrar a ocorrência. GOBACK conserva exit/NONE. As famílias não ganham interpretação nem output AIR em CP1.

Tipos internos próprios são materializados imutáveis: identidades incluem namespace da unit; listas e coleções aninhadas fazem cópia defensiva; count null é ausência, não zero; readiness, provenance/exact/includeChain, gaps, policy, roots/branches e coverage atravessam intactos na superfície consumida. DTOs, JSON, reflexão de forma física e diagnósticos de posição ficam no adapter. Core só JDK permitido e air-java. Nenhum acesso a arquivo de provenance, callback, parser COBOL, AST, resolver, CFG ou output AIR JSON.

Algoritmo: parse completo com detecção de duplicatas e limites explícitos → negociar envelope → desserializar DTOs fechados/validados fisicamente → materializar snapshots internos em uma travessia; gerar diagnósticos de variantes conhecidas fora do perfil sem apagar inventário. Não decidir start por ordem/menor handle. Termina por travessia finita da árvore limitada; custo O(B+N) tempo/espaço para bytes B e nós N, sem scans por referência nesta fase. Limites operacionais configuráveis de bytes/profundidade/nós não truncam: IMPLEMENTATION_LIMIT sem input parcial. Custos/escala serão provados no CP4, não claim antecipada.

Oracles independentes anteriores ao decoder: digest literal acima; tabela manual abaixo e relações de preservação. Nenhum expected virá da implementação. O shape inteiro será comparado ao writer, não reduzido para satisfazer o primeiro happy path.

| Classe | Resultado observado obrigatório |
| --- | --- |
| Golden completo | namespace SEMANTIC-PRODUCT-ENTRY-GOBACK.CBL/[0]/AIR-FIRST; entry:0 PRIMARY/KNOWN; start statement:0; assinatura KNOWN/0/ABSENT; um GOBACK CURRENT_PROGRAM_INVOCATION/NONE; roots statement:0; zero DATA/branches/gaps de statements |
| Metadados golden | inventory de statements COMPLETE, total/modelados 1/1, demais 0; inventory de entries PRIMARY_ONLY/PARTIAL, ALTERNATE_ENTRIES_NOT_PROJECTED; efeitos BLOCKED separado de lowering/cfg SUFFICIENT; provenance original entry início 3:7 fim 4:18 e GOBACK 4:11–4:16, exata, cadeia vazia; policy toda UNSPECIFIED |
| Schema errado/1.0.0/futuro | UNSUPPORTED_CONTRACT, nenhum input decodificado |
| Malformado/duplicata/missing/unknown/null ilegal/coerção/trailing | INPUT_ERROR com fase física e posição quando disponível; nenhum input |
| Count null físico legal | Optional vazio conservado, sem zero; contradição será diagnosticada por CP2 |
| MOVE/CALL/IF/OBSERVED legais | todas as ocorrências conservadas, identidade e variante no diagnóstico unsupported-slice; não confundir com versão/erro físico |
| OBSERVED cujo kind/shape é GOBACK | permanece OBSERVED, nunca GobackFact |
| Variante nova | INPUT_ERROR desconhecida, nunca ignorada |
| Propriedades JSON permutadas | mesmo snapshot tipado; arrays não permutados |
| Unit/handles/provenance variados | preservação exata de fatos, sem dependência de nome da fixture; mesmo handle em outra unit é identidade distinta |
| Lista externa alterada/arquivo fechado e removido | snapshot permanece igual; coleção exposta não mutável |
| Limite bytes/depth/nodes | IMPLEMENTATION_LIMIT, sem prefixo de sucesso |

RED inicial deve falhar pela tabela, não por compilação. Falsificações focais após GREEN: duplicatas ignoradas, count null convertido a zero e OBSERVED descartado/promovido devem causar RED pelo oracle esperado; restaurar bytes/configuração integralmente e segundo GREEN. Reexecutar desafios documentais/arquiteturais e suíte CP0 cumulativa. Test doubles de testes documentais estabelecem explicitamente current não autorizado e ausência de recibo, sem depender do checkpoint vivo.

Workflow manterá a identidade remota checkpoint/github-actions/push e validará a evidência indicada pelo trailer do SHA atual (não CP0 fixo); testes negativos devem rejeitar trailer ausente/inválido ou evidência de outro SHA. Limite remoto deste CP: 1200 segundos cumulativos desde seu primeiro push, sem reinício entre remediações. Review previsto: self-review integral desde CP0, sem claim de independência. Stop conditions canônicas preservadas.
