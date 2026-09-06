# Política semântica: regras gerais, não heurísticas

## Fonte de corretude

A entrada é o contrato público SP fixado; o target é a AIR normativa. Corpus, testes, Java existente e respostas de LLM são evidência/contexto, não autoridade semântica. Uma rotina simples correta é preferível a uma heurística ampla apresentada como exata.

Para cada regra não trivial, registrar: domínio de entrada; premissas; saída/observação; algoritmo; invariante; argumento de preservação ou sobreaproximação; limite de completude; terminação; complexidade; contraposição ao atalho óbvio. Usar o [template de pesquisa](../templates/research-note.md).

## Ordem de preferência

1. Regra exata derivada dos contratos e algoritmo canônico aplicável.
2. Aproximação conservadora explicitamente sustentada, com escopo e incerteza.
3. Sem fundamento suficiente: bloquear/rejeitar o slice ou executar discovery delimitado.

**Heurística semântica de corpus/texto não entra em produção neste projeto**, mesmo documentada. Isso é mais restritivo que a terceira opção admitida genericamente no ProLeap e reflete a direção solicitada para o lowerer. Experimentos comparativos podem existir fora de produção, autorizados e marcados, sem contaminar contratos.

## Pesquisa obrigatória e proporcional

Antes de algoritmo novo ou mudança semântica não trivial, consultar fontes primárias confiáveis: especificações oficiais, papers de autores/instituições e implementações canônicas oficiais. Registrar o que foi efetivamente lido, sua versão e sua aplicabilidade. Pesquisa não é uma lista de links decorativa; precisa eliminar ao menos uma alternativa inadequada ou sustentar a escolha.

Correção editorial ou wiring trivial não exige discovery acadêmico artificial. Uma regra normativa já fixada pode ser reutilizada com referência precisa; mudança de baseline ou incerteza factual pede nova verificação. Fonte indisponível é limitação explícita, não licença para inventar atribuição.

## Classes de premissas

`SPECIFICATION_GUARANTEED`, `LANGUAGE_GUARANTEED`, `ARCHITECTURE_GUARANTEED`, `OBSERVED_IN_CURRENT_CORPUS_ONLY`, `UNCERTAIN`. As duas últimas não sustentam produção silenciosamente. Garantia da linguagem só pode entrar no lowerer quando seu contrato de entrada publicou fatos suficientes e a responsabilidade de tradução estiver aprovada; não autoriza abrir source ou inferir dialect oculto.

## Anti-atalhos concretos

Proibidos: escolher primeiro root como entry; usar menor ProgramPoint como execução; GOBACK por string em OBSERVED; escolher Halt por arquivo top-level; tratar unknown count como 0; inferir tipo por PIC/rawLexeme; binding nominal como programa chamado; DataItemId distinto como storage independente; filtrar statements que o handler não entende.

Regex para validar uma forma lexical definida de ID/versão no adapter não é o mesmo que inferir semântica COBOL por regex. A distinção deve ser demonstrada pelo contrato, não pelo nome do método.

## Critério de tradução

AIR bem formada é necessária, não suficiente. A tradução deve preservar fatos/observações do slice e não afirmar precisão ausente. Uma saída conservadora deve incluir os comportamentos relevantes que a abstração promete cobrir; não importar mecanicamente o sentido de refinamento de um compilador otimizador. REF-TV/REF-COMPCERT inspiram a disciplina de provas, não certificam este projeto.
