# Review — <PR/work/checkpoint> @ <head verificado>

## Veredito e alcance

APPROVE / REQUEST CHANGES / REVIEW INCOMPLETE, conforme evidência. Registrar os arquivos/contratos lidos, testes executados pelo revisor, evidência de CI apenas observada e limitações. Não chamar revisão incompleta de conformidade integral.

## Findings

Cada finding deve indicar arquivo/linhas, pré-condição/input, comportamento observado, esperado normativo e impacto. Separar bloqueantes de observações. Não criar findings apenas estéticos que não afetam o objetivo da revisão.

## Challenge dirigido

Verificar DIP e tipos de fronteira, dados conhecidos apagados, readiness promovida, callbacks, ID sem namespace/revisão, GOBACK→Halt/fallthrough, output fora do perfil e complexidade de scans repetidos. Para nova capacidade, verificar fatos exigidos dos dois lados.

## Evidência e decisão

Mapear os invariantes afetados a testes independentes. Um checker AIR green não prova GOBACK→Return. Um green do autor não substitui inspeção do diff. Aprovação vale para este head; merge continua ato separado.
