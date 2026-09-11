# CP0 — correção do timeout remoto

Autorização: CP6 W1C, implementação/testes/harness e CI no mesmo CP0/branch/PR.
O protocolo de sessão exige corrigir CI no checkpoint corrente, com novo commit,
sem amend/rewrite e sem iniciar o checkpoint seguinte.

Commit inicial: `989637a7fd840f43aaf04abce299c839b82fcb55`.
[Certificado original](initial-certificate.json) preservado byte a byte e no commit.
[Run 34633251413](https://github.com/Gustavo2358/cobol-lower/actions/runs/34633251413)
atingiu o limite operacional de 30 minutos. [Log bruto](ci-timeout.log) e
[recibo terminal](ci-timeout-run.json) são preservados sem editar a saída.
Os grupos anteriores passaram, com restauração e segundo GREEN. O cancelamento
ocorreu após CI BOOTSTRAP CHALLENGE e antes do resultado final de CALL CHALLENGE.
Não é evidência de PASS do full remoto; tampouco foi observado erro de oracle
fora das mutações deliberadas antes do cancelamento.

Delta autorizado e congelado antes da alteração: aumentar timeout-minutes do job
checkpoint de 30 para 45, corrigir o comentário de duração e registrar PR11 nos
metadados atuais. Nenhum comando, gate, teste, oracle, source lock, implementação
Java ou expectativa muda. O limite de espera remoto congelado permanece 3600s.

Validação: executar full canônico cumulativo novamente, incluindo todos os
challenges; docs/git/diff/scope e certificação do novo candidato. Conferir por
comparação estrutural YAML que só timeout muda no workflow. Self-review do delta,
commit filho sem rewrite, push no mesmo Draft PR e CI no novo HEAD exato.
O CP0.json corrente receberá nova certificação; a versão inicial permanece
recuperável pelo commit e pela cópia byte-exact acima. Novo FREEZE só altera os
hashes dos metadados operacionais autorizados; oracles semânticos ficam idênticos.

Critério de sucesso: full local PASS e CI obrigatório terminal PASS no novo SHA,
checkout certificado e synthetic merge tree registrados. W1C permanece aguardando
review humano; W1D/W2 não autorizados. Nenhum merge/auto-merge.
