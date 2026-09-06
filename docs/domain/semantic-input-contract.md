# Contrato de entrada — Semantic Product

**Fonte:** SRC-SP, documento de domínio e writer público fixados no source lock; SRC-HANDOFF, seções 2–5, 9–12, 15 e 21. Este texto especifica o consumo local, não redefine o formato upstream.

## Transporte inicial

Nome canônico: `cobol-semantic-product.json`. O alias `semantic-product.json` existe no frontend atual, mas o lowerer não procura nomes alternativos silenciosamente. Um caminho de arquivo é configuração do adapter e não identidade semântica.

O primeiro reader suporta exatamente `schema="cobol-semantic-product"` e `contractVersion="1.1.0"`. Versões 1.0.0 ou futuras são explicitamente não suportadas pelo primeiro perfil. Não reinterpretar documento futuro usando defaults de 1.1.0.

A publicação JSON atual pertence a uma unit selecionada. Seu namespace inclui `compilationUnitId`, `structuralPath` e `canonicalProgramName`; handles como `statement:0` são locais nesse contexto. Não agregar documentos usando somente esses handles.

## Informações que atravessam

O envelope contém `unit`, `policy`, `dataDeclarations`, `statements`, `structure`, `gaps`, `coverage` e `entryInventory`. O adapter deve conhecer a forma exata a partir do writer e de uma fixture produzida pela versão fixada; exemplos conceituais do handoff não substituem isso.

`entryInventory` conserva scope/status, entries com identidade, role, availability, start, signature, provenance, coverage, readiness e gaps; `gapCodes` registra limitações do inventário. `GOBACK` tem `header`, `exit` e `localContinuation`. MOVE/CALL/IF/OBSERVED são variantes conhecidas do formato, embora fora do primeiro perfil de lowering.

## Camada física versus semântica

O decoder rejeita JSON malformado, propriedades duplicadas, tipos físicos incompatíveis e campos obrigatórios ausentes. Política de campos desconhecidos deve ser explícita e testada para a versão fixada; a proposta inicial é rejeição estrita. Essa política não autoriza rejeitar campos válidos apenas porque não participam do primeiro slice.

O domínio valida identidade/referências, consistência de disponibilidade, contagens e fatos necessários ao perfil. A mesma validação vale para um input construído diretamente em memória. DTOs de Jackson ficam no adapter; o core não depende deles nem lê `JsonNode`.

Uma variante conhecida mas não suportada não é um documento com versão desconhecida. Deve permanecer distinguível em diagnóstico. Não é necessário implementar semântica completa de todos os payloads rejeitados; é necessário não ocultar sua existência nem alegar que o documento inteiro foi semanticamente validado quando só se validou a superfície lida.

## Fechamento mínimo defensivo

Verificar unicidade de IDs na unit; start referindo statement existente na própria unit; coerência de roots/containment no shape usado; contagens de coverage reconciliadas; null versus zero; gaps obrigatórios e status que motivam a decisão. A guarda de escopo ocorre antes da construção do output de sucesso.

O JSON usa handles locais, não um campo de unit em cada referência. Portanto um teste cross-unit do core deve construir dois inputs tipados ou um cenário de agregação explicitamente controlado; não inventar uma sintaxe JSON cross-unit inexistente só para fazer o teste. Um fixture inválido deve identificar exatamente qual regra do contrato ele viola.

## Fonte insuficiente

Se o shape físico não trouxer um fato necessário, registrar finding upstream. Não consultar AST, source, reports, HTML ou resolver durante execução. Leitura do código/documentação upstream durante discovery e geração offline de fixture é permitida; dependência de produção/teste ordinário no frontend não é.
