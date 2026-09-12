# Backlog do cobol-lower

Backlog não é autorização. A prioridade inicial é o primeiro proof point, sem ampliar para toda a linguagem. Demais linhas são candidatos; uma necessidade upstream/CFG pode reordená-las com evidência.

| ID | Objetivo | Estado |
| --- | --- | --- |
| [BACKLOG-LOWER-001](backlog/BACKLOG-LOWER-001.md) | Primeiro slice Entry/GOBACK → AIR Return | `completed` |
| [BACKLOG-LOWER-002](backlog/BACKLOG-LOWER-002.md) | Sequenciamento e inventários executáveis maiores | `needs_discovery` |
| [BACKLOG-LOWER-003](backlog/BACKLOG-LOWER-003.md) | Adapter AIR JSON de saída e prova interoperável | `completed` |
| [BACKLOG-LOWER-004](backlog/BACKLOG-LOWER-004.md) | Primeira integração com analysis-cfg | `candidate` |
| [BACKLOG-LOWER-005](backlog/BACKLOG-LOWER-005.md) | DATA nominal e tipos/storage explicitamente desconhecidos | `needs_discovery` |
| [BACKLOG-LOWER-006](backlog/BACKLOG-LOWER-006.md) | MOVE preciso e normalização de valores | `needs_discovery` |
| [BACKLOG-LOWER-007](backlog/BACKLOG-LOWER-007.md) | IF e controle condicional sustentado | `in_progress` |
| [BACKLOG-LOWER-008](backlog/BACKLOG-LOWER-008.md) | CALL e contrato por site de interação | `completed` |
| [BACKLOG-LOWER-009](backlog/BACKLOG-LOWER-009.md) | Fallback opaco com envelopes conservadores | `needs_discovery` |
| [BACKLOG-LOWER-010](backlog/BACKLOG-LOWER-010.md) | PERFORM e controle local | `needs_discovery` |
| [BACKLOG-LOWER-011](backlog/BACKLOG-LOWER-011.md) | EVALUATE/SEARCH e seleção | `needs_discovery` |
| [BACKLOG-LOWER-012](backlog/BACKLOG-LOWER-012.md) | Outros terminais e transferências | `needs_discovery` |
| [BACKLOG-LOWER-013](backlog/BACKLOG-LOWER-013.md) | Storage declarativo preciso e aliases | `needs_discovery` |
| [BACKLOG-LOWER-014](backlog/BACKLOG-LOWER-014.md) | Hardening de escala e limites | `candidate` |
| [BACKLOG-LOWER-015](backlog/BACKLOG-LOWER-015.md) | Adapter de integração em memória e composição Maven | `candidate` |
| [BACKLOG-LOWER-016](backlog/BACKLOG-LOWER-016.md) | Evolução do harness e conformidade declarada | `candidate` |
| [BACKLOG-LOWER-017](backlog/BACKLOG-LOWER-017.md) | Scale AIR JSON transport beyond current 16 MiB operational boundary | `planned` / NOT STARTED |
| [BACKLOG-LOWER-018](backlog/BACKLOG-LOWER-018.md) | Reduce peak-memory amplification across SP and AIR transport | `planned` / NOT STARTED |

A saída AIR em arquivo está mergeada no 2A de BACKLOG-LOWER-003 (PR #4). A integração em memória com CFG (BACKLOG-LOWER-004) não precisa esperar esse codec. A composição Maven futura está em BACKLOG-LOWER-015. Não inventar datas de entrega por item.

## Sincronização pós-CP5 autorizada

[WORK-LOWER-009](history/WORK-LOWER-009.md) COMPLETED local: sincronizou baselines existentes; PR10 aguarda CI final/merge; não inicia capacidades futuras nem CP6.

CP6 W1C: BACKLOG-LOWER-008 completed under [WORK-LOWER-010](history/WORK-LOWER-010.md); first CALL slice only.
