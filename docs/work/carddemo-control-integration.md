# CardDemo — integração de FILE e W0–W8

Status: DONE — implementação de `cobol-lower` qualificada e mergeada em 28/09/2026, após autorização do usuário. Este fechamento documental não altera produção, testes, schemas ou oracles.

## Entrega

A campanha preserva dependências sob controle desconhecido e corrige composição FILE, comentários/aliases CICS, separação de controle e memória, áreas BMS, famílias CICS, SQL/DL/I e controle COBOL. W7 publica a política de reentrada indefinida e conserva possibilidades fonte condicionais; W8 qualifica preservação e corrige o anúncio da versão dependencies 2.7.

Os commits progressivos foram preservados por merges normais em `main`. Antes e depois de cada merge, a árvore completa foi comparada com o head qualificado do PR: igualdade em todos os merges deste repositório. Head final de revisão: `d41636e50e8c712d43e7d24f88a7a942442b657c`. Merge final de implementação: `fce18709b9fb33d45a6cab2356bc7d0333c2a80a`.

| PR | Commit de merge |
| --- | --- |
| [#41](https://github.com/Gustavo2358/cobol-lower/pull/41) | `9cecd43a837a30785870f8ab2dd8b13d6518f4f9` |
| [#42](https://github.com/Gustavo2358/cobol-lower/pull/42) | `85c3d31f5a4fd03d7ddd2dd3b7df6503ada6a78f` |
| [#43](https://github.com/Gustavo2358/cobol-lower/pull/43) | `36d6c3387ed0fdb0821d4066e85feec290cc09ea` |
| [#44](https://github.com/Gustavo2358/cobol-lower/pull/44) | `1a1fd469b390190518bff918c00f174664ae792f` |
| [#45](https://github.com/Gustavo2358/cobol-lower/pull/45) | `41f4859eb17ca6dd43d95418f89db92cf34e41e7` |
| [#46](https://github.com/Gustavo2358/cobol-lower/pull/46) | `41af1738eea2cf53fd8b99d967346917b78d6b21` |
| [#47](https://github.com/Gustavo2358/cobol-lower/pull/47) | `29bb7b9a846340f082429ec51cf4624f2b54d6b7` |
| [#48](https://github.com/Gustavo2358/cobol-lower/pull/48) | `fce18709b9fb33d45a6cab2356bc7d0333c2a80a` |

## Qualificação preservada

[Relatório W7/W8](carddemo-control-w7-w8.md) e [censo de todos os CardDemo](carddemo-control-w7-w8-carddemo.csv): 560/560 entradas nas quatro etapas, 73/73 CardDemo, PERFORM 39/39, Chaos 48/48, aliases 14/14 e PERFORM adversarial 25/25. W7 focal: 22 casos e 110 mutações de contrato. Zero perda de candidato ou support físico; 109 supports condicionais preservados; dois acréscimos condicionais investigados em fixtures de reentrada indefinida.

Frontend FAST 648 e full 1.222 testes (um skip opt-in preexistente); lower FAST/full e CFG FAST de 640 métodos passaram na qualificação W8. Estes são resultados reutilizados, não novas execuções desta alteração documental. Produção, recursos, testes, contratos e configuração de build permanecem iguais aos heads qualificados; somente documentação e pins equivalentes mudam no fechamento. Por isso o corpus/full não é repetido. Os gates de integração e os SHAs finais ficam registrados no PR documental, nos checks de `main` e no relatório E2E local `carddemo-control-integration-20260928/REPORT.md`.

## Autoridade e limites

SP 2.57, qualified-source-dependencies 1.2 e dependencies 2.7 permanecem os contratos vigentes. Pins downstream devem apontar para merges reais, na ordem frontend → lower → CFG. Os estados históricos dos relatórios não substituem esta integração.

- proleap-poc: `2a6cd9a43b26c04fada1f3f1cd4c8ccf01bc6b3d` (main mergeada).

- Cobertura continua PARTIAL; execução com exit zero não significa análise completa.
- Nenhuma aresta executável foi fabricada para conectar componentes. Todos os 73 grafos W7 são iguais aos W6 após normalizar somente o namespace da publicação.
- Reentrada ativa do mesmo PERFORM continua sem retorno executável definido sob IBM Enterprise COBOL 6.4. W7 preserva possibilidades condicionais fonte; não cumpre a premissa original de implementar recursão executável.
- Expansão de contextos AIR ainda pode ser exponencial. Compartilhar corpos com contexto de retorno preciso é uma campanha separada.
- Valores externos/desconhecidos e cinco targets indexados de menus continuam limites. Modelos IBM não concedem valor de runtime, layout físico ou kill forte.
- Full histórico do CFG, full AIR/IR e UI não foram executados nesta integração. AIR/IR não mudaram.

## Próximo trabalho

O usuário separou o encerramento desta campanha das próximas: os pontos 1–4 (MOVE com múltiplos destinos, targets indexados, funções/registradores e controle ainda não admitido) precisam de escopo e discovery próprios. O ponto 5 (representação compartilhada de rotinas e contexto) fica em outra campanha. Nenhuma dessas implementações faz parte deste fechamento. ALTER permanece fora do escopo.
