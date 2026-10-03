# Materialização de PERFORM com frames compartilhados

Status: implementação em qualificação local; revisão humana pendente.

O lower usa frames explícitos para todas as ativações de PERFORM. A política
SOURCE_UNDEFINED, sua ausência em entradas históricas, CICS e ESCAPE não escolhem
algoritmos de materialização diferentes. Não existe mais o fallback que copiava
corpos para cada cadeia de chamadores.

## Regra e autoridades

O lower interpreta exclusivamente os fatos do Semantic Product fixado em
`e6d1fa7f54bee07469bdb7ebd4a98701ca68419b`: bindings, fases, endpoints, destinos
ESCAPE e políticas de reentrada. SOURCE_UNDEFINED publica a fronteira aberta
prevista pela fonte; UNSPECIFIED conserva o diagnóstico de recursão não suportada.
Uma política ausente não passa a afirmar comportamento definido pela linguagem.

A AIR 2.0, especificação 05 §§7.1–7.8, fixada em
`f8c723e5f023cde92a6f4650e3be9334ebbaba1a`, governa frames, guardas, retorno,
resume_routes e unwind_all. Esta unificação não acrescenta campos ao contrato.
O algoritmo anterior, `cb1b872`, é referência diferencial de comportamento;
não substitui a autoridade do SP ou da AIR.

## Algoritmo

1. Identificar cada ativação por binding, estado CICS e modo handler. O chamador
   e seus antecessores não entram na identidade, inclusive para PERFORM inline.
2. Compartilhar corpos externos por entrada e endpoint. Fases de repetição
   pertencem à ativação. A guarda do binding é verificada antes dessas fases.
3. Emitir local.invoke com a continuação do chamador no frame. A conclusão usa
   local.resume. Estados CICS observados na saída selecionam resume_routes;
   o estado de entrada não substitui o estado retornado.
4. Para ESCAPE que abandona um inline, retornar com uma chave formada pelo alvo
   tipado, ocorrência-fonte e estado de saída. O chamador segue sua continuação
   se o alvo é o inline chamado; caso contrário, propaga o sinal no próprio
   contexto. Em um corpo externo ou na raiz, COMPLETE resolve o destino já
   publicado. Não há enumeração de ancestrais nem busca de ordem textual.
5. Descobrir retornos e inscrever chamadores por worklists iterativas. Cada par
   operação/sinal é processado uma vez; a propagação de ESCAPE não usa recursão
   Java. HANDLE ABEND abandona a pilha via unwind_all; HANDLE CONDITION preserva
   a ativação conforme o contrato existente.
6. Preservar o inventário de ocorrências-fonte após fronteiras opacas, sem criar
   arestas executáveis através dessas fronteiras. O compartilhamento conserva
   a união das provas publicadas dos bindings que usam o corpo.

As entradas estruturais anteriores a controlTopology também usam uma ativação
compartilhada por PERFORM composicional escrito, com seu retorno no frame.
As traduções de intervalos isolados e corpos básicos de MOVE permanecem adapters
dos fatos antigos: sua admissão exclui PERFORM aninhado; não enumeram cadeias.
Não se infere topologia a partir do texto COBOL para migrar essas entradas.

## Preservação e terminação

Cada configuração antiga corresponde a um ponto compartilhado e uma pilha de
frames. Cada frame conserva seu retorno e sua guarda. Invocar e retornar são
passos administrativos; instruções, decisões, efeitos e fronteiras permanecem
observáveis. ESCAPE percorre retornos selecionados que abandonam exatamente os
frames anteriormente removidos por unwind. As provas dos chamadores continuam
alcançáveis na proveniência do corpo.

Bindings, pontos-fonte, estados CICS e sinais de ESCAPE são finitos. As identidades
não contêm históricos de chamadas. O custo depende dos pares ponto/binding/estado
e dos pares assinante/sinal, e não das permutações das cadeias. Corpos inline com
endpoints diferentes e saltos cruzados podem ter custo quadrático. Não há promessa
de memória constante ou complexidade linear universal, nem truncamento de
profundidade, candidatos, suportes ou ocorrências.

## Oracles e falsificação

- 171 máquinas de controle congeladas foram obtidas com os 156 arquivos de
  produção de cb1b872; os hashes foram conferidos contra Git. Cada referência
  identifica o fixture e seu SHA-256. O teste recusa fixture alterado ou outro
  commit de referência. A comparação oculta somente passos administrativos e
  mantém ocorrência-fonte, tipo de operação e target literal.
- Os testes manuais de estados, efeitos, chamadas, completions e retornos
  continuam independentes. Mutações removem guardas, alteram estado retornado,
  retêm frames de handler e retiram a rota selecionada de ESCAPE.
- 60 combinações cobrem políticas publicadas, históricas e mistas, CICS, ESCAPE,
  saltos entre inline e fases TIMES/UNTIL/VARYING. O limite estrutural usa pontos
  e bindings; não permite crescimento pelo número de históricos.
- Um teste de proveniência introduz provas distintas nos chamadores e exige
  que todas sejam alcançáveis a partir da operação compartilhada.
- O DAG estrutural antigo materializava 31 ativações para 10 PERFORMs escritos.
  Um DAG de 24 níveis esgotou 512 MiB no baseline; o candidato produziu 247
  sequences para 53 ocorrências e preservou o target computado PROGLAST.
- INLINEBOOM expôs outro caminho de cópia: quatro retornos produziam 975 sequences
  e oito esgotavam 512 MiB. O materializador final produz 75 e 211 sequences.

A análise de valores/dependências é verificada separadamente pelo consumidor e
pelo corpus E2E. Equivalência de linguagem de controle, sozinha, não prova valores.

## Qualificação e limites

A mudança invalida a qualificação da representação anterior: requer FAST e
qualification-local do lower, fronteira integrada com CFG e as 560 fontes do
corpus. O comparador deve relacionar identidades à fonte e contabilizar candidatos,
suportes, proveniência, incertezas e remainders; quantidades ou nomes isolados não
bastam. Toda divergência precisa ser explicada, sem modificar evidência para PASS.

Os resultados brutos, referências, REDs, comandos, hashes e execuções preservadas
estão no workspace em `artefatos-e2e/lower-unification-20261003`. A qualificação
final e a comparação completa ainda estão em andamento. PARTIAL permanece PARTIAL.
O programa corporativo original não está disponível; a reprodução é sintética.
Não há autorização de merge.
