# Autoridade por contrato

Não existe uma fila linear em que o JSON ou a biblioteca Java redefinem tudo abaixo. Existem contratos com donos diferentes.

| Questão | Autoridade |
| --- | --- |
| O que o frontend afirma sobre COBOL | Contrato público SP do snapshot fixado |
| O que uma operação/valor/controle AIR significa | Especificação normativa analysis-ir |
| Como representar esse significado em Java | air-java, subordinado à AIR |
| Como codificar AIR em JSON | Binding versionado de analysis-ir, subordinado à AIR e atualmente DRAFT |
| Como traduzir SP para AIR neste produto | Regras bilaterais/ADRs locais, subordinadas aos dois contratos |
| Como derivar CFG | Contrato do analysis-cfg, sem alterar a AIR |
| Qual tarefa executar agora | Autorização humana + manifesto/checkpoint do work item |
| Qual estado foi executado/mergeado | Evidência verificável de arquivos/Git/CI/remoto |

Handoff e conversa fornecem objetivos e decisões. Snapshot/código esclarece implementação, mas não autoriza inventar norma. Literatura fundamenta algoritmos e métodos; não substitui esses contratos. Testes/corpus nunca definem sozinhos a semântica.

## Conflitos

Se SP correto não consegue sustentar operação AIR, registrar prerequisite/fallback autorizado, sem inferir fatos. Se air-java divergir da AIR, reportar ao runtime e manter a norma. Se binding divergir da AIR, reportar à especificação do binding. Se ordem de array parecer útil para inferir controle, rejeitar o atalho.

Não classificar toda ausência como bug: um fato intencionalmente fora de capability pode bloquear o próximo slice e permanecer uma limitação legítima. A classificação precisa de contrato/evidência.
