# CP2 — Self-review focal

Contexto/revisor: codex-root-WORK-LOWER-001-CP2, mesmo implementador; self-review, não independente. Diff integral desde 2e4c55afdaee3e31210f0e295a8e95b2d516ca00: aplicação/porta, testes manuais, driver de arquivo, test-jar exclusivamente test scope, manifesto/state/evidência e roteamento. Identidade do candidato está exclusivamente no digest de CP2.json.

O core recebe SpInput materializado e valida identidades/referências/contagens/estados antes do perfil. ADMITTED não é SUCCESS, não contém Publication e não autoriza bypass de validação na tradução futura. Diagnósticos preservam regra/fase/severidade/namespace/provenance/requisito. Input completo é retido inclusive quando fora do slice/limitado. Source lock, golden, decoder de CP1, oracles AIR/validador real e restrições de arquitetura preservados.

Findings/resoluções:

- RED inicial: stub retornava BLOCKED para input manual admissível; falha semântica esperada, não compilação.
- Preparação do caso IF omitia dois inventários de branches obrigatórios no writer. [Nota de fixture](CP2-fixture-note.md) registra correção da preparação sem alterar expected; o negativo original permanece como INVALID_INPUT/STRUCTURE.
- Gap adicional no inventário era admitido sem prova de compatibilidade; contracaso reproduziu ADMITTED incorreto. Implementação passou a bloquear códigos além do gap alternativo provado, sem apagar payload ou converter unknown em vazio. Mutação da guarda gerou novo RED e restauração/segundo GREEN.
- Um run iniciado durante atualização da composição test-jar falhou ao compilar teste do adapter porque o reactor ainda não conhecia a nova dependência test-only. Classificado como setup, não RED semântico nem falsificação. Reexecução após estabilizar POMs passou. Nenhum dependente runtime novo no core.
- Maven/testes cumulativos verificam igualdade completa entre fixture manual independente e golden; driver chama a mesma porta uma vez por decode válido, jamais repara dangling start. Erro físico impede chamada semântica; limite de bytes e I/O permanecem no adapter.

Falsificações focais restauradas: remover fechamento de start; relaxar cardinalidade; aceitar PRIMARY_ONLY/COMPLETE como contrato íntegro; null count→zero; ignorar gap adicional. Cada caso teve baseline GREEN, RED pelo status/regra esperado, restauração pelo SHA-256 e segundo GREEN. Regressão inclui controles documentais/arquiteturais de CP0, decoder de CP1 e oracles em memória/arquivo de CP2. Full/performance não são gates deste checkpoint.

Limites: validação cobre a superfície consumida, não payloads semânticos de MOVE/CALL/IF/OBSERVED que permanecem fora do slice. A conferência de containment/branch é estrutural e não constrói CFG nem infere execução. Provenance valida dados publicados não negativos, sem converter convenções ou inventar spans AIR (CP3). Metadados estatísticos são operacionais. Garantias de custo N/2N ficam para CP4. Sem runtime frontend/source/regex COBOL; parsing de handle decimal é exclusivamente gramática de identidade publicada.

Registry/active/proposals/history/backlog/index reconciliados com um único work item/branch/PR. Nenhum must_not_change, contrato upstream, oracle/invariant ou gate enfraquecido; nenhuma mutação residual, nova capability, merge/auto-merge ou backlog subsequente iniciado. CP3 continua não iniciado até certificação/push/remoto green.
