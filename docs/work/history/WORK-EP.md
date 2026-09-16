# EP — Evidence-Preserving Partial Analysis

Status do work item: `completed`. **APPROVED / MERGED / CLOSED**.

PR [#29](https://github.com/Gustavo2358/cobol-lower/pull/29) mergeado por
merge commit `6bbde05515bd41e011388895a7b18ee39bed54ac`, com proteção pelo
source HEAD aprovado `476809819ffe0cf13ea3277575bcecb9c0628cc0`.
Fast CI #154 passou nesse source HEAD. A qualificação pertence ao snapshot
qualificado; este arquivamento é somente lifecycle/documentação.

O lower preserva evidência lógica sem fabricar storage, publica PARTIAL
explicitamente e mantém referências sem VALUE sem inventar candidates.
[W0](../ep-w0-authority.md), [W1](../ep-w1-qualification.md) e
[W5](../ep-w5-qualification.md) preservam os gates e limites.

Os cinco PRs da campanha foram aprovados e mergeados em 2026-09-16.
O finding F1 foi remediado no consumidor CFG e não alterou este lowering.
REAL CASE = NOT AVAILABLE; nenhuma alegação 4/4 ou de lowering COBOL completo.
