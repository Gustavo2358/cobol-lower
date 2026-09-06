# Invariantes do cobol-lower

Cada definição tem ID estável, regra e evals. **Enforcement inicial: SPECIFIED_NOT_IMPLEMENTED em todos.** Não anunciar AUTOMATED antes de existir executor e contracaso que demonstre a proteção. O JSON adjacente indexa as definições; o texto da regra não ganha significado novo por ser serializado.

<a id="INV-LWR-001"></a>
## INV-LWR-001 — Autoridade bilateral

AIR/SP normativos governam; Java e corpus não criam semântica.

Regra detalhada: [fonte local](../engineering/semantic-analysis-policy.md). Evals: EVAL-LWR-001, EVAL-LWR-002.
Enforcement: `SPECIFIED_NOT_IMPLEMENTED`. Sem exceção silenciosa; limites legítimos devem ser explicitados no perfil/work item.

<a id="INV-LWR-002"></a>
## INV-LWR-002 — Dependency Inversion

Domínio/aplicação não dependem de tecnologia nem tipos de adapters; abstrações de necessidade ficam no lado interno.

Regra detalhada: [fonte local](ports-and-adapters.md). Evals: EVAL-LWR-003, EVAL-LWR-004.
Enforcement: `SPECIFIED_NOT_IMPLEMENTED`. Sem exceção silenciosa; limites legítimos devem ser explicitados no perfil/work item.

<a id="INV-LWR-003"></a>
## INV-LWR-003 — Boundary-only

Nenhuma tradução consulta source/AST/resolver/report/frontend/CFG para completar fatos.

Regra detalhada: [fonte local](../domain/semantic-input-contract.md). Evals: EVAL-LWR-003, EVAL-LWR-005.
Enforcement: `SPECIFIED_NOT_IMPLEMENTED`. Sem exceção silenciosa; limites legítimos devem ser explicitados no perfil/work item.

<a id="INV-LWR-004"></a>
## INV-LWR-004 — AIR única

O output usa Publication/tipos/validator de air-java; não existe IR concorrente local.

Regra detalhada: [fonte local](../domain/air-target-contract.md). Evals: EVAL-LWR-002, EVAL-LWR-003.
Enforcement: `SPECIFIED_NOT_IMPLEMENTED`. Sem exceção silenciosa; limites legítimos devem ser explicitados no perfil/work item.

<a id="INV-LWR-005"></a>
## INV-LWR-005 — Input em memória equivalente

Validação e regras são as mesmas independentemente de arquivo ou construção em memória.

Regra detalhada: [fonte local](ports-and-adapters.md). Evals: EVAL-LWR-004, EVAL-LWR-017.
Enforcement: `SPECIFIED_NOT_IMPLEMENTED`. Sem exceção silenciosa; limites legítimos devem ser explicitados no perfil/work item.

<a id="INV-LWR-006"></a>
## INV-LWR-006 — Versão explícita

A versão é negociada antes da interpretação das variantes; não aceitar 1.x indiscriminadamente.

Regra detalhada: [fonte local](../domain/semantic-input-contract.md). Evals: EVAL-LWR-006.
Enforcement: `SPECIFIED_NOT_IMPLEMENTED`. Sem exceção silenciosa; limites legítimos devem ser explicitados no perfil/work item.

<a id="INV-LWR-007"></a>
## INV-LWR-007 — Identidades completas

Joins usam namespace/owner completo; revisão e política integram a identidade da publicação.

Regra detalhada: [fonte local](../domain/identity-and-provenance.md). Evals: EVAL-LWR-007, EVAL-LWR-018.
Enforcement: `SPECIFIED_NOT_IMPLEMENTED`. Sem exceção silenciosa; limites legítimos devem ser explicitados no perfil/work item.

<a id="INV-LWR-008"></a>
## INV-LWR-008 — Entry start explícito

Label inicial deriva do start publicado e fechado, nunca de ordem física ou nome.

Regra detalhada: [fonte local](../domain/first-slice-entry-goback.md). Evals: EVAL-LWR-008.
Enforcement: `SPECIFIED_NOT_IMPLEMENTED`. Sem exceção silenciosa; limites legítimos devem ser explicitados no perfil/work item.

<a id="INV-LWR-009"></a>
## INV-LWR-009 — Assinatura não inventada

Zero conhecido é distinto de null/UNKNOWN/PARTIAL; resultados vazios exigem ausência conhecida no perfil.

Regra detalhada: [fonte local](../domain/first-slice-entry-goback.md). Evals: EVAL-LWR-009.
Enforcement: `SPECIFIED_NOT_IMPLEMENTED`. Sem exceção silenciosa; limites legítimos devem ser explicitados no perfil/work item.

<a id="INV-LWR-010"></a>
## INV-LWR-010 — GOBACK não é Halt

Saída CURRENT_PROGRAM_INVOCATION vira Return no perfil, sem inferir término de processo.

Regra detalhada: [fonte local](../domain/first-slice-entry-goback.md). Evals: EVAL-LWR-010.
Enforcement: `SPECIFIED_NOT_IMPLEMENTED`. Sem exceção silenciosa; limites legítimos devem ser explicitados no perfil/work item.

<a id="INV-LWR-011"></a>
## INV-LWR-011 — Sem fallthrough inventado

GOBACK/terminadores não ganham successor por haver outro statement no array.

Regra detalhada: [fonte local](../domain/first-slice-entry-goback.md). Evals: EVAL-LWR-011.
Enforcement: `SPECIFIED_NOT_IMPLEMENTED`. Sem exceção silenciosa; limites legítimos devem ser explicitados no perfil/work item.

<a id="INV-LWR-012"></a>
## INV-LWR-012 — Inventário sem omissão

Occurrences fora de suporte são preservadas ou impedem sucesso; não filtrar para caber na fixture.

Regra detalhada: [fonte local](../domain/first-slice-entry-goback.md). Evals: EVAL-LWR-011, EVAL-LWR-012, EVAL-LWR-022.
Enforcement: `SPECIFIED_NOT_IMPLEMENTED`. Sem exceção silenciosa; limites legítimos devem ser explicitados no perfil/work item.

<a id="INV-LWR-013"></a>
## INV-LWR-013 — Coverage dimensional

Inventário, coverage, readiness e precisão não se colapsam; partial não é COMPLETE.

Regra detalhada: [fonte local](../domain/coverage-readiness-uncertainty.md). Evals: EVAL-LWR-013, EVAL-LWR-014.
Enforcement: `SPECIFIED_NOT_IMPLEMENTED`. Sem exceção silenciosa; limites legítimos devem ser explicitados no perfil/work item.

<a id="INV-LWR-014"></a>
## INV-LWR-014 — Incerteza materializada

Lacuna necessária ao consumidor atravessa na AIR e não somente em log/relatório.

Regra detalhada: [fonte local](../domain/coverage-readiness-uncertainty.md). Evals: EVAL-LWR-013, EVAL-LWR-014.
Enforcement: `SPECIFIED_NOT_IMPLEMENTED`. Sem exceção silenciosa; limites legítimos devem ser explicitados no perfil/work item.

<a id="INV-LWR-015"></a>
## INV-LWR-015 — Provenance honesta

Origem/coordinates/units/derivações não são fabricadas; preservar limite conhecido.

Regra detalhada: [fonte local](../domain/identity-and-provenance.md). Evals: EVAL-LWR-015.
Enforcement: `SPECIFIED_NOT_IMPLEMENTED`. Sem exceção silenciosa; limites legítimos devem ser explicitados no perfil/work item.

<a id="INV-LWR-016"></a>
## INV-LWR-016 — Snapshot fechado

Input/output imutáveis sem lazy callbacks, recursos abertos ou dependência de arquivo vivo.

Regra detalhada: [fonte local](ports-and-adapters.md). Evals: EVAL-LWR-016, EVAL-LWR-017.
Enforcement: `SPECIFIED_NOT_IMPLEMENTED`. Sem exceção silenciosa; limites legítimos devem ser explicitados no perfil/work item.

<a id="INV-LWR-017"></a>
## INV-LWR-017 — Determinismo por revisão

Mesmos fatos/contexto/opções produzem observações/IDs equivalentes; runtime metadata não interfere.

Regra detalhada: [fonte local](../domain/identity-and-provenance.md). Evals: EVAL-LWR-018.
Enforcement: `SPECIFIED_NOT_IMPLEMENTED`. Sem exceção silenciosa; limites legítimos devem ser explicitados no perfil/work item.

<a id="INV-LWR-018"></a>
## INV-LWR-018 — Validação sem reparo

INPUT_ERROR, INVALID_INPUT, unsupported e falha AIR são distintos; não reparar por omissão.

Regra detalhada: [fonte local](../domain/validation-and-results.md). Evals: EVAL-LWR-006, EVAL-LWR-019.
Enforcement: `SPECIFIED_NOT_IMPLEMENTED`. Sem exceção silenciosa; limites legítimos devem ser explicitados no perfil/work item.

<a id="INV-LWR-019"></a>
## INV-LWR-019 — Sem overclaim do checker

STRUCTURALLY_VALID não certifica tradução, perfil completo ou verdade de premissas.

Regra detalhada: [fonte local](../domain/air-target-contract.md). Evals: EVAL-LWR-002, EVAL-LWR-010, EVAL-LWR-019.
Enforcement: `SPECIFIED_NOT_IMPLEMENTED`. Sem exceção silenciosa; limites legítimos devem ser explicitados no perfil/work item.

<a id="INV-LWR-020"></a>
## INV-LWR-020 — Regra geral pesquisada

Algoritmo não trivial exige premissas/fontes verificáveis/oracle; não heurística semântica de corpus.

Regra detalhada: [fonte local](../engineering/semantic-analysis-policy.md). Evals: EVAL-LWR-020.
Enforcement: `SPECIFIED_NOT_IMPLEMENTED`. Sem exceção silenciosa; limites legítimos devem ser explicitados no perfil/work item.

<a id="INV-LWR-021"></a>
## INV-LWR-021 — Custo verificável

Índices não são reconstruídos por query; limites não truncam facts nem passam silenciosamente.

Regra detalhada: [fonte local](../engineering/performance.md). Evals: EVAL-LWR-021, EVAL-LWR-022.
Enforcement: `SPECIFIED_NOT_IMPLEMENTED`. Sem exceção silenciosa; limites legítimos devem ser explicitados no perfil/work item.

<a id="INV-LWR-022"></a>
## INV-LWR-022 — Extensão explícita

Nova variante tem contrato/regra/eval, falha clara sem handler; sem classes genéricas que escondam loss.

Regra detalhada: [fonte local](extensibility.md). Evals: EVAL-LWR-012, EVAL-LWR-023.
Enforcement: `SPECIFIED_NOT_IMPLEMENTED`. Sem exceção silenciosa; limites legítimos devem ser explicitados no perfil/work item.

<a id="INV-LWR-023"></a>
## INV-LWR-023 — Oracle independente

Expected e falsificação não são derivados exclusivamente da implementação sob teste.

Regra detalhada: [fonte local](../engineering/testing.md). Evals: EVAL-LWR-010, EVAL-LWR-020, EVAL-LWR-024.
Enforcement: `SPECIFIED_NOT_IMPLEMENTED`. Sem exceção silenciosa; limites legítimos devem ser explicitados no perfil/work item.

<a id="INV-LWR-024"></a>
## INV-LWR-024 — Lifecycle e Git verdadeiros

Autorização, estado local, review e merge são distintos; mesma branch/PR por trabalho e sem mutações persistidas.

Regra detalhada: [fonte local](../engineering/work-item-protocol.md). Evals: EVAL-LWR-001, EVAL-LWR-024.
Enforcement: `SPECIFIED_NOT_IMPLEMENTED`. Sem exceção silenciosa; limites legítimos devem ser explicitados no perfil/work item.

<a id="INV-LWR-025"></a>
## INV-LWR-025 — Transporte fora do domínio

AIR JSON é binding externo fixado; writer/reader não alteram Publication nem adicionam framework ao core.

Regra detalhada: [fonte local](ports-and-adapters.md). Evals: EVAL-LWR-003, EVAL-LWR-017, EVAL-LWR-025.
Enforcement: `SPECIFIED_NOT_IMPLEMENTED`. Sem exceção silenciosa; limites legítimos devem ser explicitados no perfil/work item.

<a id="INV-LWR-026"></a>
## INV-LWR-026 — Integração independente

Lowerer e CFG validam suas responsabilidades sem importar os algoritmos do outro.

Regra detalhada: [fonte local](../product/ecosystem.md). Evals: EVAL-LWR-026.
Enforcement: `SPECIFIED_NOT_IMPLEMENTED`. Sem exceção silenciosa; limites legítimos devem ser explicitados no perfil/work item.
