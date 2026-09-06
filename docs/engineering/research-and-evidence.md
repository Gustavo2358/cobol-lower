# Pesquisa e evidência verificável

## Quando abrir uma nota

Algoritmo novo, mudança de controle, regra de conversão/efeitos, hipótese de performance, semântica ambígua ou novo contrato cross-repo. A nota não precisa ser grande; precisa apoiar uma decisão.

## Procedimento

Delimitar pergunta e observação downstream. Ler primeiro as autoridades locais/upstream fixadas. Consultar uma fonte primária de semântica e, quando houver escolha algorítmica, literatura/implementação oficial relevante. Comparar alternativas por precondições, preservação, complexidade e custo de manutenção. Registrar resultado adotado, rejeitado ou pendente e vincular invariant/eval/work item.

Não pesquisar algoritmos sofisticados para justificar um mapeamento trivial já normativo. Não importar um framework de compiler pass inteiro apenas porque usa a palavra lowering.

## Registro mínimo

Identificador da fonte; autores/organização; título; URL verificável; data/versão/commit; seção lida; nível de acesso (texto integral, seção, resumo, apenas metadata); afirmação apoiada; o que não apoia; premissas de aplicação; decisão local e teste relacionado. Citação precisa permite outro agente reconstruir a decisão sem ler toda a literatura.

Hierarquia de autoridade por domínio está em [autoridade](../sources/authority.md). Documentos externos não alteram AIR/SP. Divergência pede finding e proposta upstream, nunca edição unilateral.

## Evidência de teste

Separar observação executada, resultado do autor/CI, leitura estática e expectativa futura. Registrar commit, comando, exit code, contagem de testes, seed/limite quando houver, arquivos/hash dos logs e ambiente. Console humano não é interface de oracle; assert sobre objeto/estrutura.

Uma pesquisa por resumo sustenta apenas o que o resumo diz. Os registros REF-TV e REF-MT fornecidos aqui não equivalem à leitura integral dos papers. URLs e datas de consulta não são certificados de validade de implementação.

## Reuso e limites

Uma mesma fonte pode fundamentar vários slices, mas a aplicação deve ser reavaliada quando muda a premissa. Não copiar papers, licenças ou código sem verificar direitos; este harness fornece referências e sínteses próprias. Não carregar a biblioteca inteira em cada prompt.
