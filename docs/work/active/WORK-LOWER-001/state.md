# Estado — WORK-LOWER-001

## Onde estamos

Autorização concedida pelo pedido explícito do usuário nesta sessão em 2026-09-06: WORK-LOWER-001, execution mode `multi-checkpoint`, lista finita CP0, CP1, CP2, CP3, CP4, CP5, sem reviews humanos intermediários após certificação e checks remotos; review humano final obrigatório. Sem merge/auto-merge, alteração upstream ou ampliação de capability.

`current_checkpoint: CP0`; certificação local `pre_commit` PASS por `python3 scripts/harness/run.py certify`, exit 0. Fase COMMIT/PUSH e confirmação remota pendentes. `UNTRUSTED BOOTSTRAP` só termina após o SHA publicado e seu check obrigatório green. CP1..CP5 não iniciados. Branch `feat/first-entry-goback-slice`, criada da base limpa `14aaafc5051eb287af6a8f126e8eaff78e30e365`; PR ainda não criado. Abertura Draft após o primeiro commit certificado, quando existir diff remoto.

Certificação atual: [CP0.json](../../../quality/WORK-LOWER-001/CP0.json), commit a resolver pelo trailer `Checkpoint-Evidence: docs/quality/WORK-LOWER-001/CP0.json`. `certified_commit` permanece null na própria evidência conforme protocolo. Último recovery plenamente certificado: nenhum até confirmação remota; base anterior confirmada permanece a main acima. O SHA literal e recibo pós-push pertencem ao PR/handoff e state do próximo checkpoint.

Em 2026-09-06, fetch e pull --ff-only confirmaram main igual a origin/main. API GitHub confirmou PR de preparação #1 MERGED em 2026-09-06T20:59:54Z, mergeCommit igual à base. Working tree inicial limpa; nenhuma alteração alheia encontrada. Nenhum checkpoint/recovery point certificado existe; a base é o único ponto Git anterior confirmado.

## Verde conhecido

Air-java no SHA fixado: 172 checks com Java 21, build/install em clone/cache isolados. Core: 20 assertions AIR manual/boundary em memória. Regressão final: docs/architecture/semantic/git/fast exit 0 e 58 testes do harness com exit 0, incluindo os quatro contracasos acrescentados no review. Certificação mecânica pre_commit passou; confirmação remota não executada ainda.

Falsificações: link, invariant inexistente, active completed, must_read ausente, Path em source/API/bytecode e uso interno; 7 mutações isoladas com hashes restaurados e segundo GREEN. Return→Halt por propriedade de processo: AirValidator continua aceitando a forma, oracle próprio falha em `expected Return, never Halt`, exit 1; segunda execução sem propriedade retorna 20 assertions/exit 0. Não há mutação no source de produção. Evidência e logs adjacentes preservados.

## Restante

Commit focal com trailer, push, abertura do único PR Draft e check `checkpoint` do SHA publicado (limite congelado 1200 segundos desde primeiro push). CP1 não começa antes de todos esses requisitos. Performance/full, decoder, porta de lowering e fixture SP ainda não executados/implementados; pertencem aos próximos checkpoints.

## Descobertas que afetam o plano

Refs remotas SP, AIR e air-java coincidem com o lock. SRC-CFG main avançou para b468fb48e6f3e9346776c7b8bd65616838902069 (bootstrap da fronteira Java, PR #3); não é dependência de runtime deste trabalho. Lock preservado; integração CFG permanece fora do escopo. Snapshots AIR e air-java obtidos em clones temporários isolados; checkout upstream vizinho preservado. Java 21.0.12.1+1 provisionado em diretório temporário, checksum conferido; Maven 3.9.16. Actions habilitado; endpoint de proteção de main respondeu HTTP 404 “Branch not protected”, não evidência de gate/CI.

Falhas de setup resolvidas: Maven -f fora do cwd upstream quebrava teste por caminho relativo; repetição no diretório do snapshot passou. Download do plugin de dependências bloqueado por DNS do sandbox; execução autorizada com rede resolveu. G-ARCH inicialmente confundia cabeçalhos de archives/modules do jdeps com classes; separação corrigida mantendo verificação das dependências Maven e das referências de classe.

Arranjo do teste de obrigações corrigido com [nota explícita](../../../quality/WORK-LOWER-001/CP0-obligation-oracle.md): o runtime não emite obrigação automaticamente para Return mínimo; fixture controlada de declaração de perfil torna a prova de preservação não vacuosa, sem claim do produto. Review do certificador detectou falta de checagem das referências normativas; dois contracasos adicionados. O novo teste acusou ausência do ponteiro FREEZE de semantic-input-contract; referência à mesma base adicionada, sem alterar os bytes do contrato.

O verificador Git também passou a exigir a evidência efetivamente incluída no commit, após REDs de evidência ausente/divergente e GREEN da correção. [Self-review](../../../quality/WORK-LOWER-001/CP0-review.md) concluído sem findings bloqueantes; nenhum revisor independente foi usado. Evidência/state passaram por revisão documental final; não carregam novas regras de produção.

Binding AIR JSON é draft e fica fora do primeiro slice. Identity por revisão, convenções de spans, shape real e coverage dimensional precisam de evidência no checkpoint correspondente. Nenhuma fixture SP real foi criada e nenhuma lacuna foi preenchida por heurística. Não houve merge/auto-merge nem início de outro work item.
