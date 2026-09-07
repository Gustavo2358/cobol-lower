# WORK-LOWER-002 — Avaliação

## O que prova corretude

Oracle físico independente: tree Git do merge `b78f4068d8a479f48eb048b8d76fa60a0997dc4a`. Cada path e URL ativa precisa resolver nesse SHA; nenhum pin ativo pode usar o SHA anterior. A comparação com a baseline preserva o bloco normativo e arquivos históricos.

## Casos adversariais

Em cópia temporária do lock, restaurar `src/main/java/...` sem `air-model/`: a consulta ao tree deve falhar pelo path ausente. Restaurar o lock candidato byte a byte e repetir o GREEN. Pin antigo, URL antiga ou divergente, arquivo inexistente e alteração do pin normativo devem ser rejeitados pelo audit focal.

## Regressões

Executar gates documentais, harness e build/testes atuais contra o artefato do merge em Maven repo isolado. Não inferir integração JSON pela presença do codec upstream. Gates indisponíveis do CFG permanecem indisponíveis, sem claim de PASS. O manifest do pacote documental original do lower é histórico; verificar sua integridade na revisão original, sem reescrever evidência passada.

## Regressão da fixture de autoridade

RED observado: `test_arbitrary_history_does_not_authorize_code` não recebeu `UNAUTHORIZED_IMPLEMENTATION` porque a cópia ainda continha o novo item autorizado. A baseline sem autoridade ativa e com histórico válido deve passar; substituí-lo por histórico arbitrário deve falhar com o mesmo código. O challenge preserva restauração e segundo GREEN. O audit de proveniência registra a exceção exata dos três arquivos de fixture, sem permitir mudanças nos checkers.
