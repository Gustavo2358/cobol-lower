# Primeiro slice — Entry primária e GOBACK

**Perfil local implementado:** `minimal-entry-goback@1`, autorizado em WORK-LOWER-001 CP0..CP5. [Evidência e review final](../work/history/WORK-LOWER-001.md). Derivação: SRC-HANDOFF §§3–14, 17–20; SRC-SP 1.1.0; SRC-AIR operações/controle. Não é um perfil oficial da AIR.

## Hipótese a provar

Usando somente o contrato público materializado do Semantic Product, conseguimos preservar a entrada executável e a saída da invocação corrente como AIR válida, sem inferir controle por ordem ou contexto de runtime.

```cobol
IDENTIFICATION DIVISION.
PROGRAM-ID. AIR-FIRST.
PROCEDURE DIVISION.
    GOBACK.
END PROGRAM AIR-FIRST.
```

O nome `AIR-FIRST` identifica a fixture, não faz parte do predicado de produção. Outros nomes, filenames, handles locais e proveniências válidas não podem mudar a regra de tradução.

## Admissão inicial explícita

| Fato | Condição para o caso positivo |
| --- | --- |
| Contrato | SP 1.1.0 reconhecido |
| Unit | Uma publicação de unit selecionada com identidade íntegra |
| Declarações | Nenhuma DATA a baixar neste perfil; não omitir DATA existente |
| Entry inventory | Scope PRIMARY_ONLY; inventário PARTIAL legítimo e gap de entries alternativas preservado |
| Entry | Uma PRIMARY publicada, availability KNOWN, coverage/readiness compatíveis com a capability |
| Start | KNOWN e referência fechada para o GOBACK da mesma unit |
| Assinatura | KNOWN, parameterCount=0, returningClause=ABSENT |
| Statements | Exatamente o shape mínimo: um GOBACK publicado, raiz com containment conhecido, sem outra ocorrência omitida |
| GOBACK | exit=CURRENT_PROGRAM_INVOCATION; localContinuation=NONE; coverage MODELED e readiness local suficiente |
| Input global | Sem input missing que invalide os fatos de entry/start/saída |

A restrição de shape é uma política inicial local, adicionada para tornar a primeira publicação fechada sem fallback horizontal. Não é alegação de que a linguagem só permite um GOBACK. Estruturas em memória devem usar inventários plurais. Um input maior recebe `UNSUPPORTED_SLICE`/`BLOCKED_LOWERING` com evidência, jamais uma publicação que contém só o primeiro statement.

## Tradução

Criar IDs AIR próprios, uma Unit, uma Entry com signature de inventários vazios **fechados** neste caso conhecido e um label inicial. Criar uma Sequence sem instruções comuns e terminador `Return(values=[])`. `return` encerra a ativação corrente; em uma invocação raiz encerra essa invocação normalmente. Não é `halt`, que tem outro escopo de término.

O mapeamento do start usa a referência publicada e uma tabela explícita source statement → label AIR. Não usa `get(0)`, menor ID, menor ordinal, filename, nesting ou ausência de caller para decidir semântica. Como o primeiro shape tem um statement, contracasos de renomeação de handle e validação de start são essenciais para não esconder esse atalho nos testes.

## Output positivo

Uma Publication, uma Unit, uma Entry, um label inicial, uma Sequence, zero instruções comuns e um Return sem valores. Nenhum Halt, Jump ou successor local inventado. `AirValidator` deve retornar `STRUCTURALLY_VALID` nos checks implementados, com obrigações preservadas no relatório. Isso não certifica um perfil AIR inteiro.

Coverage de inventário não vira COMPLETE global: `ALTERNATE_ENTRIES_NOT_PROJECTED` atravessa em incerteza/escopo apropriados. Precisão do controle local de Return não eleva effects/storage/values desconhecidos. Ver [mapeamento dimensional](coverage-readiness-uncertainty.md).

## Casos que bloqueiam ou rejeitam

Assinatura PARTIAL/UNAVAILABLE, parâmetro não zero ou retorno PRESENT não viram signature vazia. Start desconhecido não é escolhido por ordem. Referência ausente/cruzada é contrato inválido. OBSERVED cujo texto se parece com GOBACK não vira Return. STOP RUN, EXIT PROGRAM, CICS RETURN e outros terminais não recebem esta regra.

`GOBACK; CONTINUE` e múltiplos GOBACKs ficam fora do shape inicial, mas o diagnóstico não deve sugerir fallthrough do primeiro GOBACK. Quando houver suporte posterior a inventários maiores, as ocorrências precisam permanecer positivas, inclusive as não alcançáveis; não fazer DCE disfarçado de lowering.

## Duas provas independentes

Primeiro construir manualmente o shape AIR e validar. Depois consumir a fixture real SP e comparar a observação do resultado contra um oracle independente, sem chamar a implementação para gerar expected. Os testes negativos e metamórficos estão nos EVAL-LWR correspondentes em [catálogo](../evals/catalog.md).

## Fora de escopo

DATA, MOVE, IF, CALL, branches, storage/alias, resolução de target, opaque universal, CFG, RD/PV, writer AIR e CLI rica. O checkpoint posterior 2A compõe este mesmo slice com o codec compartilhado, writer físico e CLI mínima; o domínio e o perfil permanecem iguais. CFG JSON e E2E completo continuam futuros.
