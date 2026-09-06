# Falsificação e revisão adversarial

## Objetivo

Demonstrar que o gate detecta uma alternativa errada plausível. RED é sucesso do experimento de falsificação, não defeito a esconder. O mutante precisa atingir a propriedade, não apenas quebrar o compilador aleatoriamente.

## Loop obrigatório

Fixar baseline limpa do escopo; guardar hashes/diff; introduzir uma mudança controlada; executar teste/gate focal e confirmar a falha esperada; restaurar somente a mutação; conferir hash/diff; rodar focal e gates novamente. Nunca reverter arquivo inteiro com mudanças alheias. Preferir fake/input controlado quando mutar produção for arriscado. Mutações não entram no commit final.

## Mutantes de alto valor

- Return trocado por Halt: checker AIR pode passar; oracle de tradução deve falhar.
- Start ignorado em favor do primeiro statement: dangling/rename/start conhecido detectam o atalho.
- Count null convertido para zero: regra de assinatura deve rejeitar.
- Gap alternativo removido ou coverage global elevada: oracle dimensional deve falhar.
- Statement extra filtrado: teste de admissão deve deixar de aceitar o sucesso amputado.
- Tipo Jackson/Path entrando na porta: gate arquitetural de source/assinatura/bytecode apropriado deve falhar.
- Índice recomposto dentro de cada consulta: contador estrutural/performance deve detectar repetição, sem depender só de milissegundos.

## Evidência

Registrar propriedade atacada, diff ou descrição reproduzível do mutante, teste, saída/exit code, causa esperada, causa observada, restauração e segundo verde. Um log red por dependência indisponível não prova que a semântica foi falsificada. Gate fake que sempre falha também não vale: precisa aceitar baseline e rejeitar contracaso.

Matriz permanente de propriedades fica em evals. Logs/mutações temporários podem ser removidos após extrair evidência curta e tests duráveis. Não manter scaffolding sem valor só para aumentar tamanho do PR.

## Independência do review

Segunda passagem do mesmo agente é self-review, não revisão independente certificada. Um oracle escrito separadamente é evidência adicional, não humano. Reviews de outro agente/humano citam SHA e verificam contratos/testes, não só o resumo do autor.
