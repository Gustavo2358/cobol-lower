# CP0 único

RECOVER e reconciliar merges PR4/PR2 sem editar certificados históricos; criar WORK-LOWER-004.
FREEZE v2 renovado por autorização explícita XXH3-128, de contrato, oracles, fixture, pins e gates antes da implementação.
RED de ID compacto e vetor independente; implementação mínima na identidade; primeiro GREEN.
Falsificar omissão de campo, enquadramento/versão, seed e buffer, conferir restauração e segundo GREEN.
Rodar full (docs, architecture, semantic, performance, git, harness-tests, challenges existentes),
diff --check, self-review integral e certificação pre_commit. Sem obrigação de review independente
no harness atual; self-review será identificado como tal. Publicar uma branch/PR, confirmar HEAD
remoto e check checkpoint no SHA exato (limite 1800 s desde primeiro push), pedir revisão humana.
Manter HEAD commitado e árvore limpa nesta branch, sem merge/auto-merge ou E2E cross-repo.
