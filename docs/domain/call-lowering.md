# CP6 W1C — CALL para AIR Invoke

O contrato corrente é SP 1.3.0, W1A merge
`53d774026a1e4bcd969c7783a1d277aaa87b5f2f`. O modelo/codec é W1B merge
`2a37f5e980ba25fdc79614a66030a84d8bf5b8c9`, tree
`8d248f4ccf207eb7b609aa9ff0cdbcd512e526c8`. Os documentos primários
`proleap-poc/docs/domain/call-semantic-product.md`, `scalar-text-move.md`,
`air-java/docs/engineering/air-json.md` e `docs/quality/cp6-w1b-invoke.md`
foram consultados nesses snapshots. AIR normativa permanece no pin
`analysis-ir@51b4d9a8ae0364232bd97103cd73a77e1a34996c` (04 §7, 05 e 06).
[Regra congelada](../work/active/WORK-LOWER-010/spec.md),
[oracle](../work/active/WORK-LOWER-010/eval.md) e
[evidências](../quality/WORK-LOWER-010/CP0.json).

## Entrada e admissão

`Wire13` é fechado e materializa `CallFact`, `LiteralCallTarget` ou
`DataCallTarget`, preservando binding/candidatos/selected/wholeItemAccess,
provenance, surface, runtimeTarget UNKNOWN, effects UNKNOWN e outcomes OPEN.
`MoveFact` conserva `FITTED_TEXT` e `TextAdjustment` (rule, extent, result,
provenance). Nullable não equivale a ausente. Campos desconhecidos, duplicados,
tipos errados e enums inválidos falham no adapter. Memória e arquivo passam pela
mesma admissão; DTO/JSON não entra no core. Decoders 1.1/1.2 existentes permanecem
explícitos, sem negociação, downgrade ou projeção de compatibilidade.

`CallAdmission` admite entry primária conhecida com assinatura zero, cadeia root
linear de zero ou mais MOVEs suportados, exatamente um CALL e sua continuação
normal conhecida para GOBACK terminal. Um CALL delimita a capacidade semântica
desta slice; não é orçamento geral de tamanho. Todos os statements e DATA devem
ser reconciliados. A ordem de arrays/programPoint não escolhe controle.

DATA requer binding único e whole item escalar textual publicado. Literal requer
valor lógico publicado. USING, RETURNING/GIVING, handlers, ambiguidade,
unresolved, refmod, subscript, truncation e controle não linear são
`UNSUPPORTED_SLICE`. Input incompleto/continuação indisponível bloqueia com
`BLOCKED_LOWERING`; incoerências e referências pendentes são `INVALID_INPUT`.
Limites operacionais continuam `IMPLEMENTATION_LIMIT`.

## Tradução e incerteza

`ScalarDataTranslator` constrói objetos/células uma vez. `MoveHandler` consome
FULL_IDENTITY.source.logicalValue ou FITTED_TEXT.textAdjustment.result sem
interpretar PIC ou recalcular padding. X5 escreve `"PROGA"`; X8 escreve
`"PROGA   "`. A origem ajustada deriva de statement, literal, destino e prova
textAdjustment. O lower não afirma que o literal com espaços foi escrito.

`CallSequenceAssembler` produz `[Assign*; Invoke]` e `[Return]`. O label normal
vem do StatementId explícito do GOBACK. `InvokeHandler` usa action `call`,
category `program`, namespace fonte `cobol.program`. Literal conserva texto
lógico bruto; DATA produz `ComputedTarget(Read(ObjectPlace(selected DATA)))`,
mesmo com MOVE anterior ou sem MOVE algum. Não há resolução de nome runtime.

Arguments/results/effectOperands e inventários da assinatura externa são vazios,
com `NoRemainder`. `UnknownName` e `UnknownContract` preservam a falta de
autoridade externa. Reads/writes são `WithinMemory(AllMemory(publication,true))`,
mustOverwrite/perOutcome vazios. Outcomes contêm Normal conhecido e
`WithinControl(AllControl(publication))`; sucesso normal não fecha outras saídas.
Cinco uncertainties distinguem alvo runtime, política de nome, efeitos, saídas
e contrato. Claims abertos e coverage ABSTRACTED do Invoke coexistem com
Publication/Unit PARTIAL. `AirValidator` retorna STRUCTURALLY_VALID e conserva
I-56 como obrigação semântica, inclusive após AirJson round-trip.

StatementLink, OperandLink, EntryLink e DataLink preservam identidade da unit
SP e relações com operações/labels/origins AIR. Alvo, Read, ObjectPlace,
operação e continuação têm origens distintas. Não se extrai caller de UnitId.

## Identidade, custo e limites

`CanonicalRevision.call` tem domínio próprio e inclui target literal/DATA,
continuação, surface, knowledge e resultado fitted. Dados canônicos e cadeia
explícita tornam permutações físicas invariantes. IDs não dependem de relógio,
UUID aleatório, ordem de hash ou diretório temporário.

Índices DATA/statement são construídos uma vez; o percurso usa visited set e
termina após no máximo S statements. Custo amortizado O(D+S+R+P+B) e memória
O(D+S+R+P), com B bytes de fatos codificados para identidade. Lookup de DATA por
CALL é indexado. Não há scan de todos os statements/DATA por CALL nem limite
semântico de candidatos, sequências ou visitas. Probes N/2N medem contagens,
sem alegar SLA do validator/codec.

Testes reais W1A, porta em memória, CLI atômica, round-trip/canonical bytes,
negativos e mutações compiláveis protegem a regra. A invocação em instructions
é desafio de transporte semanticamente inválido (I-04), não erro de compilação.
O script `scripts/harness/w1c_e2e.py --lower` executa o produtor duas vezes e
compara os bytes da CLI com oracle relacional/validator/round-trip independente.

Não implementa FitText, argumentos/resultados, tracing do MOVE anterior,
PossibleValues, CFG ou produto de dependências. W1D/W2 permanecem não iniciados
e não autorizados. O codec mantém seu limite operacional de saída independente;
não há alegação de qualificação E2E geral de programas grandes.
