# Questões a fechar antes de congelar APIs

Estas são decisões concretas do primeiro trabalho, não convite a reabrir todo o discovery. Nenhuma deve ser preenchida por default conveniente.

| ID | Pergunta / ação | Quando | Como concluir |
| --- | --- | --- | --- |
| Q-LWR-001 | Política de PublicationId distingue revisões e funciona também em memória? | CP0/CP3 | Contrato dos componentes + testes equivalência/revisão; não usar apenas nome/unit |
| Q-LWR-002 | Convenções de base/unidade/fim dos spans SP estão estabelecidas na baseline? | CP1/CP3 | Fonte verificável; se não, origem menos precisa e evidência preservada, nunca coordenada fabricada |
| Q-LWR-003 | Como obter air-java SNAPSHOT reproduzível sem release confirmada? | CP0 | Checkout SHA+build isolado ou artefato versionado verificável; nenhum JAR arbitrário |
| Q-LWR-004 | Shape exato do golden e todos os campos do envelope 1.1.0? | CP1 | Captura real e confronto com writer fixado; campos conceituais do handoff não bastam |
| Q-LWR-005 | Política de unknown fields e reconhecimento de variantes fora do slice? | CP1/CP2 | Testes distinguem forma inválida de capability não suportada |
| Q-LWR-006 | Granularidade AIR para entry inventory parcial e claims de effects bloqueados? | CP3 | Matriz por escopo/dimensão; checker e oracle independente sem COMPLETE/EXACT inventados |
| Q-LWR-007 | Convenção abstrata de posições versus base zero Java/JSON? | Antes de parâmetros não vazios | Confirmar normativamente; não bloqueia assinatura vazia do primeiro slice |
| Q-LWR-008 | Binding AIR draft e interoperabilidade de saída? | Trabalho de transporte | Pin do draft, writer e reader/oracle independente; promoção no analysis-ir |

Se uma questão impedir a tradução honesta do primeiro perfil, registrar BLOCKED com evidência e encaminhar à autoridade. Não modificar upstream a partir deste repo nem anunciar conclusão por contornar a lacuna.
