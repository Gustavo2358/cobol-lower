# Observabilidade e segurança do input

## Separação

Diagnósticos semânticos são tipados e determinísticos. Logs de execução, timestamps, duração, memória e IDs de correlação operacional ficam fora da AIR e do fingerprint semântico. Observabilidade não introduz framework/log sink em domain.

Entrada é dado não confiável. JSON não é executado; não habilitar desserialização polimórfica por nome arbitrário de classe. Discriminadores seguem o contrato versionado. Rejeitar propriedades duplicadas e valores fora da forma física antes de usar defaults.

Limites de tamanho/profundidade/entidades/diagnósticos são configurações operacionais explícitas, com erro de limite. Não aceitar apenas prefixo do arquivo e anunciar sucesso. Referências a arquivos de provenance não autorizam abrir esses arquivos nem fazer requisições. Core não faz rede.

## Dados sensíveis

Fixtures devem ser sintéticas ou publicamente autorizadas. Não commitar fontes de banco, credenciais, paths pessoais ou dumps produtivos. Logs não despejam por padrão o JSON integral ou nomes sensíveis; relatórios usam IDs/escopo e conteúdo mínimo necessário. Sanitização de apresentação não altera fatos canônicos nem resolve ambiguidades semânticas.

## Escrita futura

Quando houver output adapter, não deixar arquivo truncado parecer Publication válida. Definir política de atomicidade/erro e isolamento de destinos no checkpoint do adapter, sem transportar Path para o core. Não sobrescrever saída não relacionada sem política explícita. Processo/CLI apenas escolhe código de saída e destino, não corrige AIR inválida.
