# Evidências CP6 W1C

Baseline lower `18016f16b4f63149eb1bb4ca13db7e12593d8909`, tree
`0139edc52e34c703b643ff484a33e70871ef091d`; branch
`feat/cp6-w1c-call-lowering`. Work item WORK-LOWER-010, checkpoint interno CP0.
Commit certificado será resolvido pelo trailer `Checkpoint-Evidence: docs/quality/WORK-LOWER-010/CP0.json`.
O SHA literal, PR e CI pós-push são registrados no handoff/PR, sem autoinscrever
o SHA ou reescrever o certificado imutável.

## Autoridades e REDs

W1A fixo `53d774026a1e4bcd969c7783a1d277aaa87b5f2f`, SP1.3.0.
W1B real merge `2a37f5e980ba25fdc79614a66030a84d8bf5b8c9`, tree
`8d248f4ccf207eb7b609aa9ff0cdbcd512e526c8`. PR9 MERGED confirmado antes da
branch e reconsultado no recibo [remoto](w1b-remote-pr.json).
[Tree](w1b-remote-merge.json), [baseline remota](lower-main-remote.json),
[bootstrap](bootstrap.log) e [receipt dos JARs](air-provenance.json).
O build usa Maven repo isolado, nunca aceita apenas o nome SNAPSHOT como prova.

- RED1: [dinâmico](red1-dynamic-x8.log) e [literal](red1-literal.log), CLI baseline,
  `UNSUPPORTED_CONTRACT`, exit3, sem AIR. Código lower é a baseline exata;
  foi compilado com os JARs W1B isolados. A rejeição é anterior à construção AIR;
  esse experimento não alega uso do antigo pin Maven CP5.
- RED2: decoder 1.3 já materializa CallFact; lowering antigo retorna
  `UNSUPPORTED_SLICE`, sem Publication: [literal](red-decoder-only-literal.log).
- RED3: o mesmo estágio X8 recusa FITTED_TEXT por exigir FULL_IDENTITY e
  extensões idênticas: [log](red-decoder-only-dynamic-x8.log).
  [Probe](DecoderOnlyRed.java.txt), [patch do estágio](decoder-only.patch),
  [Wire13 novo daquele estágio](decoder-only-Wire13.java.txt),
  [hash do ScalarMoveAdmission baseline](baseline-scalar-sha256.txt).
  O probe retorna exit0 ao observar a falha esperada do produto.

`freeze.json` conserva hashes da regra/oracle/plan e das 23 fontes/fixtures
pré-implementação; todos foram rechecados byte a byte. `CP0-manifest.yaml`
conserva a autorização. Referências normativas do certificado usam a baseline;
o novo source lock e os contratos W1C são imutáveis no candidato.

## GREEN real

[Recibo E2E](real-e2e-receipt.json): 23 fontes executadas duas vezes pelo W1A.
Para X8 e literal, cada uma das duas execuções segue produtor real → SP bruto →
CLI lower → AIR JSON; uma execução independente em memória compara a Publication,
AirValidator e AirJson decode/re-encode. Nenhum CALL é injetado após o frontend.
SP1.3 e AIR são determinísticos. Os bytes da CLI coincidem com o oracle.

| Caso | SP SHA-256 | AIR JSON SHA-256 |
| --- | --- | --- |
| X8 | `e45c6fe191c6e9be4b35da793663fc908b0b600c848a236d797170e286614010` | `bef5d8979c0ffd3fb96272dd405b18d7b873e4e4d9244eeeb9ac98587fb973d2` |
| Literal | `e88bb1029fd184e3dabefd28bf37cdea99193630a9f5caeeebc1bd00e56870a1` | `d532b89c074d3a3fe8672090cef5432d9b664e137f3ce1bd37058af5fc59e67d` |

[Resumo de campos reais](real-output-summary.json). Outputs brutos e logs estão
em `real-e2e/dynamic-x8-{1,2}` e `real-e2e/literal-{1,2}`. X8 tem um WS-PGM Object,
Assign de `"PROGA   "`, Invoke `ComputedTarget(Read(ObjectPlace(WS-PGM)))`,
normal para a segunda Sequence com Return real. Literal tem
`LiteralTarget("PROGA")` e zero objetos artificiais. Em ambos: STRUCTURALLY_VALID,
I-56 presente, encode/decode semanticamente iguais e re-encode byte-idêntico.

## Negativas, mutações e limites

USING, RETURNING, handlers, ambiguous/unresolved, refmod/subscript, truncation,
IF/EVALUATE/GO TO/PERFORM → UNSUPPORTED_SLICE. Incomplete e continuação ausente →
BLOCKED_LOWERING. Incoerência copy/adjustment ou referências inválidas →
INVALID_INPUT. Físico inválido → INPUT_ERROR. Falhas não publicam AIR parcial.

[13 mutações compiláveis](call-challenge/receipt.json) incluem alvo literal
indevido, ObjectId errado, padding X8/X5, destino normal errado, efeitos puros,
outcomes fechados, contrato inventado, trim, ExactName indevido, USING/ambiguous
admitidos e revision ignorando alvo. Compilação deve passar; RED é assert semântica.
Cada mutação restaura bytes e exige segundo GREEN. Placement é um desafio
adicional: Invoke em instructions produz INVALID_IR/I-04; o original passa
novamente pelo round-trip. Logs intermediários com falha permanecem disponíveis.

Entry/GOBACK, scalar X5, CLI, atomicidade, capacidade e arquitetura anteriores
permanecem na suíte canônica. O status final de cada comando/gate está em
[CP0.json](CP0.json); nenhum gate ausente é declarado PASS.
[Self-review](review.md) explicita as correções e limites.

[Siblings](siblings-read-only.json): checkouts produtivos não alterados; clones
temporários usam merges exatos, mesmo quando o checkout local do irmão está no
antigo PR HEAD. Artefatos E2E conserva diretórios untracked preexistentes; não foi
editado, commitado ou sincronizado. Sem Git na raiz agregadora.

W1C termina AWAITING_HUMAN_REVIEW, PR Draft, sem merge/auto-merge. W1D e W2
permanecem NOT_STARTED / NOT_AUTHORIZED. Sem resolução dinâmica, tracing de
MOVE, trim de nomes, CFG, PossibleValues ou dependency fact.
