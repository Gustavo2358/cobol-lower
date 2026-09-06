# Controle de mudanças de contrato

## Fonte da mudança

Classificar separadamente: correção de implementação local; novo fato upstream; alteração semântica AIR; mudança representacional air-java; mudança de binding; troca de infraestrutura. Uma classe Java nova não altera norma AIR e uma versão minor do produtor não garante que este consumer a aceite.

## Procedimento

Comparar snapshots fixados; registrar diferença e primeiros contratos afetados; atualizar matriz bilateral e resultado esperado; decidir migração/versão/escopo; criar work item e testes negativos; só então mudar lock e implementação de maneira revisável. Não baixar sempre a main mais recente no build para esconder o problema de versão.

Use os códigos de [impacto](downstream-impact.md) e registro de finding. Quando a especificação e implementação discordarem, manter a autoridade normativa e reportar o drift ao repo apropriado. Não criar quarta variante local, campo mágico ou API deprecated que preserve semântica rejeitada.

## Binding DRAFT

O AIR JSON binding é uma candidata fixável por commit. Primeiro slice em memória não depende de sua promoção. Um trabalho posterior pode autorizar writer experimental e reader/oracle independente para produzir evidência. Só a autoridade `analysis-ir` promove o binding; não tratar merge de draft como accepted. Isso evita exigir um codec estabilizado antes do teste que o estabiliza.

## Mudança após review

Evidência vale para SHA específico. Alterar mapping, schema, profile, oracle, gate ou source lock após approve requer review do delta. Históricos podem explicar decisões anteriores; fontes atuais não devem ser alteradas apenas para fazer testes antigos passarem.
