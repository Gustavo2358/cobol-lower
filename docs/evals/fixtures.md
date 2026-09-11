# Aquisição e manutenção de fixtures

## Classes

`upstream_golden`: arquivo realmente produzido pelo frontend fixado. `synthetic_adversarial`: documento alterado para quebrar regra nomeada. `in_memory`: input tipado construído sem transporte. `air_oracle`: AIR manual, independente do lowerer.

O golden SP real foi capturado em CP1, conforme o [registro de aquisição](fixture-intake.json). AIR JSON permanece fora deste trabalho. Exemplos textuais não substituem prova de que um arquivo foi produzido.

## Capturar o positivo

Em ambiente isolado, usar checkout do proleap no SHA do lock e seu comando documentado para a fixture AIR-FIRST/GOBACK. Registrar fonte autorizada, formato/configuração/policy, comando, JDK/analyzer, output canônico e SHA-256. Não importar proleap como dependência do teste ordinário do lowerer; commitar a fixture de contrato e sua proveniência.

O golden deve ser validado estruturalmente e comparado com o writer/contrato fixado, não reduzido à mão aos poucos campos usados pelo primeiro caso. Remover campos para simplificar o reader mascara incompatibilidade.

## Adversariais

Partir de uma cópia do golden e documentar a mutação mínima: start inexistente, duplicate ID, count null, signature partial, variante não suportada etc. Manter representação física legal nos testes de semântica. Um JSON malformado pertence à classe de INPUT_ERROR, não ao contracaso de return.

## Expected

Escrever manualmente o alvo AIR e as relações relevantes. A comparação deve verificar entry/start/terminador/coverage/provenance/IDs, não só contagem. Nunca executar lowerer para regenerar expected sem review da regra. Frameworks de snapshot não são autoridade.

## Distribuição

Não depender de nome fixo de pasta irmã ou cwd oculto. Fixtures locais têm paths relativos de teste; checkout externo é explicitamente configurado. Não copiar corpus confidencial. Atualizar digest/manifesto/expected em commit explicado quando mudar baseline.

## CP6 W1C

`adapters/src/test/resources/sp/cp6/` contém fontes COBOL e SP1.3 capturados do
W1A fixo; `scripts/harness/w1c_e2e.py` verifica duas execuções byte-idênticas sem
injeção de CALL. CallInputs é a porta em memória, CallOracle é o oracle relacional
independente; CallIntegrationSuite cobre decoder, negativos, CLI, validator e
codec W1B. [Contrato e limites](../domain/call-lowering.md).
