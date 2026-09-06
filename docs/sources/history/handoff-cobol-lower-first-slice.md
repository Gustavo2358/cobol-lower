# Handoff — Primeiro slice do `cobol-lower`

## Propósito

Este documento entrega o contexto mínimo necessário para iniciar o repositório/aplicação **`cobol-lower`** sem reabrir decisões já tomadas no frontend COBOL, na Analysis IR ou no CFG.

O primeiro objetivo do `cobol-lower` é extremamente estreito:

```text
cobol-semantic-product.json
        ↓
     cobol-lower
        ↓
air-java Publication
  └── Unit
      └── Entry
          └── Sequence
              └── Return
```

O primeiro slice deve consumir um Semantic Product equivalente ao programa:

```cobol
IDENTIFICATION DIVISION.
PROGRAM-ID. AIR-FIRST.
PROCEDURE DIVISION.
    GOBACK.
END PROGRAM AIR-FIRST.
```

e produzir uma `Publication` AIR 2.0.0 válida, usando **somente** o contrato público do Semantic Product.

---

# 1. Fronteiras físicas entre os repositórios

A arquitetura consolidada é:

```text
proleap-poc
  |
  | produz
  v
cobol-semantic-product.json
  |
  | consumido por
  v
cobol-lower
  |
  | depende de air-java
  | produz
  v
AIR 2.0.0 Publication
  |
  | consumida por
  v
analysis-cfg
  |
  v
CFG
```

## `proleap-poc`

É o frontend COBOL.

Responsabilidades:

- parsing;
- AST;
- símbolos;
- occurrences;
- binding/resolution;
- Semantic Product COBOL-specific;
- JSON determinístico do Semantic Product.

Sua fronteira pública termina no JSON.

O `cobol-lower` **não pode** consultar:

- AST;
- parser;
- symbol table;
- occurrences;
- resolution;
- source COBOL;
- `SourceMap`;
- HTML;
- reports do frontend;
- classes internas do `proleap-poc`.

Se um fato necessário não estiver no JSON, isso é um gap do contrato upstream. Não deve ser reconstruído por heurística no lowerer.

## `air-java`

É a implementação Java compartilhada da Analysis IR 2.0.0.

Snapshot já avaliado:

```text
repository: Gustavo2358/air-java
commit: 2108294d9dfeb89d0019ce75fab27172b15a75b9
library version: 0.1.0-SNAPSHOT
Analysis IR: 2.0.0
normative spec commit:
0b2fbce7046010b22b32efa8cbc3e75ccba09442
```

O `cobol-lower` deve depender de `air-java`.

Não crie cópias privadas de:

- `Publication`;
- `Unit`;
- `Entry`;
- `Sequence`;
- `Terminator`;
- `Return`;
- `TypeRef`;
- IDs AIR.

## `analysis-cfg`

É consumidor da AIR.

Não pertence ao escopo deste primeiro trabalho.

O `cobol-lower` deve produzir AIR correta independentemente de existir CFG.

---

# 2. Estado do Semantic Product relevante ao primeiro slice

O PR #31 do `proleap-poc` implementa o primeiro enriquecimento necessário.

Snapshot do PR avaliado:

```text
PR: Gustavo2358/proleap-poc#31
head:
02814fbfc75c60a88bb6236e89db2d960ca67d0d
```

Antes de iniciar implementação real, confirme que esse PR foi mergeado e fixe o commit de `main` efetivamente consumido.

O contrato público passa a ser:

```text
schema = cobol-semantic-product
contractVersion = 1.1.0
```

O arquivo canônico é:

```text
cobol-semantic-product.json
```

O frontend mantém temporariamente:

```text
semantic-product.json
```

como alias byte-idêntico de compatibilidade.

O `cobol-lower` deve considerar **`cobol-semantic-product.json`** o nome canônico.

---

# 3. Novos fatos disponíveis no JSON 1.1.0

O JSON agora publica uma superfície de entrada:

```text
entryInventory
```

e uma variante de statement:

```text
GOBACK
```

Conceitualmente:

```text
unit:
  canonicalProgramName: AIR-FIRST
  ...

entryInventory:
  scope: PRIMARY_ONLY
  status: PARTIAL
  entries:
    - id: entry:0
      role: PRIMARY
      availability: KNOWN
      start:
        availability: KNOWN
        statement: statement:...
      signature:
        availability: KNOWN
        parameterCount: 0
        returningClause: ABSENT
      provenance: ...
      coverage: MODELED
      readiness: ...
  gapCodes:
    - ALTERNATE_ENTRIES_NOT_PROJECTED

statements:
  - variant: GOBACK
    header:
      id: statement:...
      provenance: ...
      coverage: MODELED
      readiness: ...
    exit: CURRENT_PROGRAM_INVOCATION
    localContinuation: NONE
```

Os nomes exatos e campos devem ser lidos do JSON/schema real da versão fixada.

Não derive comportamento a partir deste exemplo se o contrato real divergir.

---

# 4. Semântica de `EntryInventory`

O primeiro slice cobre somente:

```text
EntryRole.PRIMARY
EntryInventoryScope.PRIMARY_ONLY
```

Portanto:

```text
entryInventory.status = PARTIAL
```

é esperado mesmo quando a entry primária está perfeitamente conhecida.

Isso **não significa** que a entry publicada seja imprecisa.

Significa apenas:

> o inventário de todas as entradas possíveis da ProgramUnit ainda não está fechado.

O gap:

```text
ALTERNATE_ENTRIES_NOT_PROJECTED
```

deve ser preservado como informação de cobertura.

Não trate:

```text
PRIMARY_ONLY
```

como prova de que não existem `ENTRY` statements alternativos.

---

# 5. Semântica do início executável

A entry publica:

```text
start.statement
```

quando o frontend conseguiu provar a entrada executável canônica.

O lowerer pode usar esse fato para estabelecer o label inicial da `Entry` AIR.

O lowerer **não deve** escolher:

- primeiro statement do array;
- menor ID;
- primeiro root;
- menor ProgramPoint;
- primeira linha do fonte.

Se `start.availability != KNOWN`, não fabrique entrada executável.

O fallback deve respeitar coverage/readiness/incompletude.

---

# 6. Semântica de `GOBACK`

O `GobackFact` publica:

```text
exit = CURRENT_PROGRAM_INVOCATION
localContinuation = NONE
```

A propriedade mais importante é:

```text
GOBACK
   X
   └── não existe successor local
```

`GOBACK` não é `nop`.

`GOBACK` não possui fallthrough.

A presença física de outro statement depois dele não cria aresta.

Exemplo upstream já testado:

```cobol
GOBACK.
CONTINUE.
```

Ambos permanecem no inventário, mas `GOBACK` não continua para `CONTINUE`.

---

# 7. Mapeamento mínimo esperado para AIR

Para o fixture mínimo:

```cobol
PROCEDURE DIVISION.
    GOBACK.
```

o resultado AIR deve ser conceitualmente:

```text
Publication
└── Unit
    └── Entry
        └── initialLabel = L0

Sequence L0
└── Return
```

A AIR 2.0.0 define:

```text
return(values: Expression[])
```

como retorno da ativação da unidade corrente ao invocador.

Em uma entry raiz, `return` termina normalmente essa invocação raiz.

Portanto, para o slice mínimo com:

```text
EntrySignature:
  availability = KNOWN
  parameterCount = 0
  returningClause = ABSENT
```

o candidato natural é:

```text
Return(values = [])
```

Não use `halt` apenas porque a ProgramUnit é top-level no arquivo.

O frontend explicitamente **não afirma** que uma unit top-level é necessariamente o programa principal de runtime.

---

# 8. Cuidado crítico: `return` vs `halt`

A AIR distingue:

```text
return
```

de:

```text
halt
```

O frontend deliberadamente publica:

```text
CURRENT_PROGRAM_INVOCATION
```

e não:

```text
MAIN_PROGRAM_TERMINATION
SUBPROGRAM_RETURN
```

Isso é intencional.

Para o primeiro slice, a AIR permite representar o encerramento normal da ativação corrente com `return`, inclusive quando a entrada é raiz.

O lowerer não deve tentar descobrir pelo:

- filename;
- nesting;
- nome do programa;
- ausência de caller no corpus;

se deve produzir `halt`.

`halt` deve ser usado somente quando houver fato semântico suficiente de término de execução compatível com a AIR.

---

# 9. Cuidado crítico: assinatura da entry

Nunca interprete ausência de conhecimento como cardinalidade zero.

O Semantic Product diferencia:

```text
availability = KNOWN
parameterCount = 0
returningClause = ABSENT
```

de:

```text
availability = UNAVAILABLE | INPUT_MISSING
parameterCount = null
returningClause = UNKNOWN
```

e de:

```text
availability = PARTIAL
parameterCount = N
returningClause = PRESENT | ABSENT
```

## Primeiro slice suportado

O fixture norteador deve exigir:

```text
signature.availability = KNOWN
parameterCount = 0
returningClause = ABSENT
```

Nesse caso, um `Return([])` é representável sem inventar resultados.

## Assinatura parcial

Exemplo:

```cobol
PROCEDURE DIVISION USING ARG.
    GOBACK.
```

Hoje o frontend pode preservar `parameterCount=1`, mas marcar a assinatura como `PARTIAL`.

Isso não é autorização para fabricar:

- modos;
- tipos AIR;
- bindings;
- parâmetros completos.

Para o primeiro vertical slice, a recomendação é **não promover esse caso para lowering preciso**.

Preserve/report o gap ou use uma estratégia conservadora somente se a Analysis IR permitir a representação sem overclaim.

## RETURNING/GIVING

Se houver retorno explícito:

```cobol
PROCEDURE DIVISION RETURNING RESULT.
```

o mero `GobackFact` não basta para fabricar:

```text
Return([RESULT])
```

O lowerer precisa de informação suficiente sobre:

- posição de resultado;
- expressão/local correspondente;
- `TypeRef`;
- compatibilidade de domínio quando aplicável.

O primeiro slice deve evitar esse caso.

---

# 10. Negociação obrigatória de versão do Semantic Product

Antes de interpretar qualquer variant, valide:

```text
schema == "cobol-semantic-product"
```

e negocie:

```text
contractVersion
```

O primeiro consumer pode suportar explicitamente:

```text
1.1.0
```

Não assuma que qualquer `1.x` é automaticamente equivalente.

A mudança de 1.0.0 para 1.1.0 adicionou:

- `entryInventory`;
- variante `GOBACK`.

Consumers antigos que fechavam o conjunto de variants precisam reconhecer 1.1.0 ou rejeitar o documento.

## Regra recomendada

No primeiro slice:

```text
1.1.0 → supported
1.0.0 → unsupported for CFG-FIRST lowering
versão futura desconhecida → fail explicitly / unsupported version
```

Depois podemos introduzir negociação mais flexível se houver necessidade real.

---

# 11. IDs são namespaced

Handles JSON como:

```text
entry:0
statement:0
```

não são identidades globais isoladas.

A identidade é conceitualmente:

```text
(unit, localHandle)
```

O lowerer deve evitar joins apenas pela string do handle.

Uma entry de uma unit nunca pode iniciar em statement pertencente a outra unit.

O frontend já valida isso, mas o consumer deve continuar defensivo.

---

# 12. Coverage e readiness não são decorativos

Antes de escolher lowering preciso, consulte:

- availability;
- coverage;
- readiness;
- gaps.

Exemplo esperado para o fixture mínimo:

```text
Entry:
  lowering = SUFFICIENT
  cfg = SUFFICIENT para start
  effects/dataflow = BLOCKED

GOBACK:
  lowering = SUFFICIENT para saída da invocação
  cfg = SUFFICIENT para ausência de successor local
  effects/dataflow = BLOCKED
```

`SUFFICIENT` é sempre relativo à capability publicada.

Não interprete isso como:

```text
AIR-STRUCTURE@2 completo
```

nem como suporte a todos os terminais COBOL.

---

# 13. Outros terminais continuam fora

Não normalize para `Return` automaticamente:

```cobol
EXIT PROGRAM
STOP RUN
STOP literal
EXEC CICS RETURN
```

No Semantic Product atual, essas famílias continuam conservadoras/observadas.

Somente `GOBACK` possui a capability terminal precisa deste slice.

O lowerer não deve interpretar `observedKind`, source text ou nomes COBOL para recuperar a semântica que o frontend não publicou.

---

# 14. Statements observados

`ObservedStatement` não pode:

- desaparecer;
- virar `nop`;
- ganhar fallthrough por conveniência;
- ser reinterpretado pelo nome textual.

Para o primeiro fixture minimalista não deve haver `ObservedStatement`.

Em fixtures maiores, qualquer observed statement pode exigir `opaque` AIR com envelopes conservadores, mas isso está fora do primeiro slice.

Não implemente `opaque` horizontalmente só para ampliar escopo agora, a menos que seja estritamente necessário ao primeiro oracle.

---

# 15. Dependência no `air-java`

O `cobol-lower` deve usar o modelo real do `air-java`.

Não criar DTO de domínio paralelo.

É aceitável criar DTOs de **transporte do Semantic Product JSON**, porque o JSON pertence a outro contrato.

A separação esperada é:

```text
Semantic Product JSON DTOs
        ↓
semantic decoding / validation
        ↓
lowering rules
        ↓
air-java model
```

Não anote o modelo `air-java` com Jackson para adaptá-lo ao Semantic Product.

---

# 16. Validação AIR

Depois do lowering:

```java
Publication air = lower(...);
ValidationResult result = AirValidator.validate(air);
```

O primeiro oracle positivo só passa quando:

```text
result.isStructurallyValid() == true
```

Mas lembre:

```text
STRUCTURALLY_VALID
```

não significa conformidade completa com `AIR-STRUCTURE@2`.

É somente validação estrutural dos checks implementados pelo `air-java`.

---

# 17. Fixture AIR independente

Antes de integrar o Semantic Product real, é recomendável criar manualmente uma fixture AIR equivalente ao target:

```text
Publication
Unit
Entry
Sequence
Return
```

e validar com `AirValidator`.

Isso separa duas perguntas:

1. sabemos construir AIR válida desse shape?
2. sabemos traduzir o JSON real para esse shape?

Não use output do próprio lowerer como expected do oracle.

---

# 18. TDD mínimo recomendado

## Teste 1 — versão/schema

Input:

```text
schema=cobol-semantic-product
contractVersion=1.1.0
```

Expected:

```text
accepted
```

Versão incompatível deve falhar explicitamente.

---

## Teste 2 — fixture mínima

Input JSON derivado de:

```cobol
PROCEDURE DIVISION.
    GOBACK.
```

Expected AIR:

```text
1 Publication
1 Unit
1 Entry
1 initial label
1 Sequence
0 common operations
terminator = Return([])
```

---

## Teste 3 — vínculo Entry → GOBACK

Modificar fixture para:

```text
entry.start.statement = statement inexistente
```

Expected:

```text
rejeição
```

Não escolha outro statement.

---

## Teste 4 — sem fallthrough

Input Semantic Product equivalente a:

```cobol
GOBACK.
CONTINUE.
```

Mesmo que esse caso ainda não seja suportado como Publication AIR fechada no primeiro slice, teste pelo menos que:

```text
GOBACK
```

não seja traduzido para `jump`/fallthrough ao statement seguinte.

---

## Teste 5 — assinatura desconhecida

Input:

```text
signature.availability = UNAVAILABLE
parameterCount = null
returningClause = UNKNOWN
```

Expected:

```text
não fabricar assinatura vazia/Return preciso
```

A ação concreta pode ser `unsupported slice`, `blocked lowering` ou outra representação conservadora definida explicitamente.

---

## Teste 6 — assinatura parcial

Input:

```text
availability = PARTIAL
parameterCount = 1
```

Expected:

```text
não promover automaticamente para o fixture preciso de zero parâmetros
```

---

## Teste 7 — terminal errado

Input variant:

```text
OBSERVED
```

com shape/text que pareça STOP/GOBACK.

Expected:

```text
não reinterpretar como Return
```

---

## Teste 8 — namespace

Entry de Unit A apontando para Statement de Unit B:

```text
rejeitar
```

mesmo se local handles forem iguais.

---

## Teste 9 — determinismo

Duas execuções equivalentes do lowering devem produzir Publications semanticamente equivalentes e IDs determinísticos conforme a política escolhida.

---

# 19. Primeiro critério de sucesso

O primeiro PR de implementação do `cobol-lower` deve provar:

```text
Semantic Product JSON 1.1.0
        ↓
decode
        ↓
validate input contract
        ↓
lower
        ↓
air-java Publication
        ↓
AirValidator
        ↓
STRUCTURALLY_VALID
```

para exatamente o fixture:

```cobol
IDENTIFICATION DIVISION.
PROGRAM-ID. AIR-FIRST.
PROCEDURE DIVISION.
    GOBACK.
END PROGRAM AIR-FIRST.
```

Nada mais é necessário para declarar o primeiro slice concluído.

---

# 20. Fora de escopo do primeiro PR

Não implementar ainda:

- MOVE;
- IF;
- branch;
- CALL;
- invoke;
- DATA lowering completo;
- storage;
- alias;
- `sameDomain` para MOVE;
- CALL literal;
- target dinâmico;
- GO TO;
- PERFORM;
- ALTER;
- SEARCH;
- `opaque` horizontal de todas as famílias;
- effects;
- RD;
- Possible Values;
- CFG;
- DOT/visualização;
- CLI rica;
- cloud;
- banco;
- integração com source COBOL;
- dependência no `proleap-poc` Java.

---

# 21. Transporte

O contrato físico de entrada é JSON.

O core do lowerer, porém, não precisa depender diretamente de Jackson.

Arquitetura recomendada:

```text
JSON adapter
   ↓
SemanticProductInput / DTO tipado
   ↓
CobolLower
   ↓
air-java Publication
```

O caso de uso deve ser testável em memória.

A existência do JSON não obriga o algoritmo de lowering a conhecer arquivos.

---

# 22. Futuro E2E

Depois que esse primeiro slice estiver mergeado:

```text
proleap-poc
→ cobol-semantic-product.json

cobol-lower
→ air-java Publication

analysis-cfg
→ CFG
```

O primeiro E2E completo esperado será:

```text
COBOL
  ↓
Semantic Product
  ↓
AIR
  ↓
CFG

ENTRY
  ↓
GOBACK / AIR Return
  ↓
EXIT
```

Esse será o primeiro proof point arquitetural da pipeline inteira.

---

# 23. Regra final

Quando faltar um fato no Semantic Product:

```text
não voltar ao source;
não abrir proleap internals;
não usar regex;
não inferir por nome;
não escolher default conveniente.
```

O lowerer pode:

- traduzir fatos publicados;
- normalizar estrutura;
- criar IDs/labels AIR;
- aplicar regras de tradução semanticamente válidas.

Ele não pode realizar uma nova análise COBOL escondida.

Essa separação é a principal propriedade arquitetural a preservar.
