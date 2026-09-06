# <Work item / checkpoint — resultado observável>

## Escopo autorizado

Work item, checkpoint(s), branch existente e limites. Informar base de review e head. Checkpoint não autorizado permanece não iniciado.

## Mudança e evidência

| Regra/contrato | Alteração | Oracle e resultado real |
| --- | --- | --- |
| <fonte> | <delta> | <teste/comando + exit code ou não executado> |

Incluir fixtures reais/sintéticas e hashes pertinentes, revisões upstream, migrações de API e estratégia de resolução do SNAPSHOT quando afetada.

## Gates e falsificação

Registrar executores e resultados por gate; quantidade de testes efetivamente executados; mutação, oracle que a detectou, motivo do RED, restauração e segundo GREEN. Nada de PASS por ausência de runner ou compile quebrado por causa irrelevante.

## Fronteiras e limitações

Confirmar ausência de dependência proibida; preservar UNKNOWN/gaps/coverage. Relatar suporte não implementado, limitações do Validator e testes/integrações não executados.

## Estado final

Conhecimento promovido, work/state/index coerentes e próximo checkpoint não iniciado. Pedir review humano. Não efetuar merge ou auto-merge.
