# CP0 — Review técnico

Tipo: self-review pelo mesmo agente Codex desta sessão, contexto `codex-root-WORK-LOWER-001-CP0`. Não houve review independente. Identidade do candidato é exclusivamente `candidate_diff_sha256` em CP0.json, com `review.candidate_reference` apontando para esse campo; a revisão deve ser repetida se esse conteúdo mudar.

Escopo examinado: diff completo desde 14aaafc5051eb287af6a8f126e8eaff78e30e365, incluindo arquivos novos, staged e unstaged. Bootstrap Java/Maven, um único ponto de delegação AirOutputValidation, fixtures/testes, executores/certificador, workflow, autorização/lifecycle e evidência. Modelo/validator AIR são os tipos reais de air-java; nenhum decoder, source COBOL, AST, resolver, CFG ou regra GOBACK de produção foi introduzido. Não há dependência de transporte no core. Adapters está deliberadamente vazio neste checkpoint.

Findings corrigidos antes de certificação:

- A implementação inicial que fabricava ValidationResult vazio foi rejeitada por `boundary must preserve INVALID_IR for dangling label` (exit 1, depois de baseline manual e checker real passarem). Delegação ao AirValidator substituiu o stub, preservando issues e status.
- Obrigação não vazia na fixture mínima era premissa errada do arranjo de teste; correção e fonte primária estão em CP0-obligation-oracle.md. O contracaso separado exercita uma obrigação real sem promover o produto a AIR-STRUCTURE.
- Leitura de jdeps confundia cabeçalhos de arquivos/módulos com classes. Maven verifica a árvore de artefatos; API/javap/jdeps verificam classes. Source/API/bytecode rejeitam contracasos Path e annotation de transporte.
- Certificador não exigia inicialmente todos os ponteiros normativos nem presença de campos cujo valor permitido é null. Dois contracasos foram adicionados; referência ausente de semantic-input-contract foi recomposta da base original, sem alterar contrato/lock.
- Recovery inicialmente conferia trailer/digest, mas podia aceitar um registro diferente da evidência incluída no commit. Dois testes produziram RED pela causa esperada (evidência ausente no commit ou alterada fora dele). Verificação agora lê o blob do SHA solicitado e confere a identidade do registro; suíte focal de 11 testes passou.
- Transcrição dos logs Maven continha espaços finais e `git diff --cached --check` retornou 2. Espaços finais foram removidos, com nota explícita nos logs; resultados/diagnósticos não foram alterados.

Falhas de setup não contadas como falsificação: primeiro build upstream com cwd incorreto e resolução Maven impedida pelo DNS do sandbox. Nova hipótese baseada no diagnóstico: executar no cwd do snapshot e resolver plugin com rede autorizada, respectivamente. Ambos resolvidos sem alterar arquivos/testes upstream.

Contraprovas preservadas: documentos inválidos não passam; autorização inexistente/fora da lista não avança; gate obrigatório ausente/não executado, regressão omitida, FREEZE incompleto, self-review rotulado independente e mutação não restaurada impedem certificado. Suíte Git verifica digest, index/worktree, trailer e evidência do commit. Casos remotos rejeitam SHA errado, push ausente, pending/missing/failure/cancelled/skipped/neutral/unknown; só success de todos os obrigatórios no SHA correto passa a fase remota.

Limites: mecanismos documentais verificam registros, não a verdade dos logs. Checks estáticos não identificam qualquer modelo semanticamente duplicado sob nomes arbitrários; revisão do diff complementa a automação. AIR estruturalmente válida não certifica lowering. EVAL-LWR-004 aqui é smoke em memória da boundary AIR. Falsificações de gap/start e demais oracles SP ficam nos checkpoints que introduzem essas regras. Nenhum estado remoto futuro é considerado PASS antecipadamente. Não há branch protection configurada por este trabalho.

Resultado da revisão: nenhum finding bloqueante remanescente no candidato revisto; certificação ainda requer a regressão final registrada e o verificador mecânico. Somente a evidência/state são excluídos do digest; ambos recebem revisão documental final, sem novos contratos/código. Sem merge/auto-merge ou trabalho fora de WORK-LOWER-001.
