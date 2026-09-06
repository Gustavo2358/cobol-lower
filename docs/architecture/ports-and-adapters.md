# Portas, adapters e arquivo → memória

## Porta de entrada estável

Contrato conceitual, a concretizar somente no checkpoint autorizado:

```text
LowerCobol.lower(SemanticProductInput, LoweringOptions) → LoweringResult
```

`LowerCobol` é interface de aplicação. `SemanticProductInput` é um snapshot tipado, imutável e sem Jackson; preserva identidades completas, policy, entries, statements, relações, gaps, coverage e provenance exigidos pelo perfil. Não é um DTO privado de infraestrutura vazando para o núcleo. Não é cópia da AIR nem obrigação de reproduzir todas as classes do frontend.

`LoweringOptions` contém perfil e escolhas semânticas explicitamente versionadas. Caminho, encoding, logger, destino de arquivo, HTTP e stream não fazem parte desse contrato. Limites operacionais podem ser recebidos separadamente de política semântica; atingir um limite não muda o significado do input.

## Driving adapter versus port acionado pela aplicação

Um adapter de entrada pode ler o arquivo e chamar `LowerCobol`; não precisa fingir que implementa o próprio caso de uso. Se a aplicação precisar buscar a publicação de uma fonte, define um port de leitura semântico com resposta `SemanticProductInput`; o adapter de arquivo o implementa. Não introduzir as duas alternativas redundantes no primeiro slice.

Na saída, retornar `LoweringResult` com `Publication` basta para compor aplicações. Quando o caso de uso realmente acionar publicação externa, uma interface interna como `PublishAir.publish(Publication)` será implementada pelo adapter. Não obrigar filesystem para executar a regra e não criar uma interface para cada classe apenas por estilo.

## Caminho inicial

```text
File adapter [Path/UTF-8/JSON DTO] → SemanticProductInput → LowerCobol
                                                          ↓
                                                   LoweringResult
                                                          ↓
                     caller → AIR JSON writer externo (checkpoint posterior)
```

## Caminho integrado

```text
Frontend public snapshot → adapter de integração → SemanticProductInput
                                                    ↓ mesma porta
                                               LowerCobol
                                                    ↓
                                            air-java::Publication
                                                    ↓
                                          analysis-cfg::BuildCfg
```

O integrador, não o lowerer, conhece o CFG. O contrato do Semantic Product tem fronteira pública em memória e um transporte inicial em JSON; “consome arquivo hoje” não significa “domínio depende de arquivo para sempre”. A frase do handoff sobre terminar no JSON descreve a integração inicial, não elimina a API pública materializada.

## Lifetime

Após o adapter retornar, arquivo/conexão pode ser fechado. O snapshot não contém streams, fornecedores lazy de conteúdo, ObjectMapper, AST, source reader ou callbacks para resolver IDs. A publicação AIR e o relatório são imutáveis. Índices auxiliares vivem por execução e não modificam o snapshot.

Retenção deliberada de valores imutáveis pode evitar cópias profundas; isso não permite reter o modelo mutável do frontend. O adapter deve fechar a fronteira antes de chamar o core.

## Prova de substituição

Mesmo input semântico e opções, construídos por teste em memória ou pelo decoder JSON, devem produzir resultados equivalentes: conteúdo AIR, diagnósticos, IDs sob a política escolhida, origens, lacunas e cobertura. Testar separadamente falhas de I/O e falhas semânticas. O teste em memória não pode serializar e desserializar internamente para aparentar independência.

Não é prometido que toda futura integração não terá nenhum trabalho: wiring, compatibilidade de JDK/API e adapter upstream ainda precisam ser construídos. O que não deve mudar por motivo de transporte são as regras e contratos internos.
