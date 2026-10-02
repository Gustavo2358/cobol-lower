# Compactação de contextos de PERFORM

id: LOWER-COMPACT-PERFORM
status: IN_PROGRESS
scope: lower; extensão explícita AIR; interpretação e transporte CFG

## Problema e regra

O materializador atual copia o corpo para cada cadeia de bindings ativos. A guarda
`LocalIds.containsActivation` impede apenas repetir um binding na mesma cadeia.
Na fixture sintética CTXBOOM, N retornos permitem todas as permutações parciais:
`C(N) = sum(N!/(N-k)!, k=0..N)`. O baseline mediu 260.302 sequences com N=7;
N=8 encerrou por OOM com heap de 4 GiB. Evidência bruta está em
`artefatos-e2e/lower-context-explosion-20261002`, fora deste repositório.

Regra: compartilhar corpos independentes do chamador e representar a cadeia de
ativações pela pilha de controle local. Para bindings recursivos cuja política
publicada é SOURCE_UNDEFINED, transportar uma guarda explícita com identidade
do binding e destino para a fronteira conservadora já existente. Verificar a
guarda ANTES das fases do PERFORM, não somente antes de entrar no corpo.

## Autoridades consultadas e premissas

- SP fixado em e6d1fa7f54bee07469bdb7ebd4a98701ca68419b: contrato tipado
  `ControlTopology.Binding`, `ReentryPolicy`, outcomes e endpoints. A política
  vem do produto; o lower não deduz regras a partir do texto COBOL.
- AIR 2.0, analysis-ir 2c7f31f19efbe3211a2aea5bbda90173a9666fe2,
  especificação 05 §7, lida integralmente: frames, retorno, unwind e fallback.
  A invocação comum continua permitindo recursão. A nova guarda exige uma
  capacidade separada; não altera silenciosamente control.local@1.
- Implementação anterior fixada em 01096a2a4b7e103bb679f308c9df7bce34a45dd5:
  `TopologyProgramAssembler`, `SharedRoutineBodies`, `LocalIds`, lidos. Serve
  como oracle diferencial de comportamento, não como autoridade da linguagem.
- A pesquisa em resumos dos autores sobre weighted pushdown systems
  (https://pages.cs.wisc.edu/~reps/research-summary.html) não estabelece
  equivalência de um solver de summaries para os domínios não distributivos do
  consumidor. Não substituir esse solver faz parte do limite desta mudança.

Premissas SPECIFICATION_GUARANTEED: identidades, destinos e políticas publicados.
Premissas ARCHITECTURE_GUARANTEED verificadas: ausência de ESCAPE tipado no corpo,
ausência de mudança de estado CICS na nova admissão de ciclos. A ocorrência de
GO TO fora do intervalo textual não basta para bloquear compartilhamento:
os destinos tipados e o endpoint governam a tradução.

## Algoritmo e preservação

1. Construir o grafo de chamadas de bindings usando a closure existente.
2. Encontrar SCCs com DFS iterativa e grafo reverso. Uma SCC não trivial ou uma
   autoaresta identifica bindings recursivos. Bloquear ciclos sem SOURCE_UNDEFINED,
   corpos com ESCAPE/estado dependente e todos os seus chamadores transitivos.
3. Admitir o restante, incluindo ciclos guardados. Manter a chave de corpo
   `(entry, endpoint, support, handler)` existente. No modo com CICS, manter
   a admissão acíclica anterior nesta mudança.
4. Em chamada recursiva admitida, emitir local.invoke guardado antes das fases.
   A chave é a identidade global do binding, não o ID físico da operação.
   O frame guarda o resume do chamador. O término das fases executa local.resume.
   O corpo compartilhado continua usando seu próprio frame de retorno.
5. Se a chave já estiver ativa, saltar à fronteira SOURCE_UNDEFINED sem modificar
   a pilha; caso contrário, empilhar e seguir para a primeira fase. A fronteira
   preserva os efeitos abertos e não inventa continuação normal.

Invariante: as chaves dos frames guardados correspondem aos bindings recursivos
ativos da expansão antiga; cada frame conserva seu próprio retorno. Passos
administrativos de invoke/resume são silenciosos. Por indução sobre chamadas,
fases e retornos, as operações observáveis e as fronteiras coincidem. Não há
limite arbitrário de profundidade nem descarte de ocorrências.

Terminação: o grafo de bindings e o conjunto de chaves de corpo são finitos;
cada corpo é enfileirado uma vez. Construção do grafo custa a soma das closures;
SCC e propagação custam O(B+E). Materialização custa a soma dos corpos distintos
e fases, sem fator de número de cadeias. Closure por binding ainda pode custar
O(B*(V+E_source)); este trabalho não afirma linearidade no tamanho do SP inteiro.

## Oracles e gates

Antes da implementação: testes novos devem falhar (RED). AIR: campo da guarda,
capability obrigatória, identidade textual não vazia e destino na mesma Unit;
codec roundtrip e rejeição de formas inválidas. Bytes antigos permanecem iguais.
CFG: igualdade de chave em operações físicas distintas, chave abaixo do topo,
pop/unwind liberando chave, escopo por Unit e retorno específico do chamador.
Lower: traces dos casos pequenos comparados à expansão baseline, inclusive
reentrada antes das fases; ciclos não admitidos e CICS preservados. Escala:
CTXBOOM 8 e maiores sob heap limitado, AIR validada e CLI serializando resultado.
Executar FAST e qualificação local dos repositórios de produto afetados, com
pins exatos coordenados; registrar falhas e gates não executados.

## Limites

Não há claim de completude COBOL: coverage PARTIAL continua PARTIAL. Corpos com
ESCAPE tipado ou estado CICS não recebem a nova compactação cíclica. A construção
CFG permanece compacta; análises que enumeram todas as pilhas ainda podem ter
muitos contextos. Esta mudança não prova escala do dataflow para SCCs grandes.
Revisão humana é necessária antes de merge; nenhum merge está autorizado.

## Evidência executada do candidato

- Oito retornos: a MESMA entrada SP do OOM de 4 GiB foi validada e serializada
  pela CLI com `-Xmx512m`: 49 sequences, 0,92 s, pico RSS 158.616 KiB.
- Vinte retornos: 109 sequences, 1,02 s, pico RSS 183.908 KiB.
- Cem retornos: 509 sequences, 1,72 s, pico RSS 249.712 KiB.
- A CLI CFG construiu os casos 8 e 100 com 60/612 nós em 0,36/0,67 s; ambos
  passaram no oracle independente do wire 6.0.0. Isso não mede o solver completo.
- PerformReentrySuite: 22 casos, 110 mutações; a comparação antiga de IDs
  físicos foi substituída por equivalência de linguagem de controle após ocultar
  jumps e passos administrativos de frame. O oracle conserva as ocorrências-fonte
  e tipos de operação; não afirma equivalência de valores por si só.
- SharedRoutineSuite: fixtures reais CTXBOOM 4/8, TIMES, UNTIL e VARYING; limite
  estrutural abaixo de 100 sequences, roundtrip e comparação com a expansão
  histórica nos casos pequenos. Não se executa a expansão histórica de N=8 em CI.
- AIR: FAST e qualification-local passaram; 189 verificações de modelo e 136
  de transporte. Lower: qualification-local passou, inclusive suites semânticas
  e de desempenho. CFG: FAST passou; qualificação integrada em andamento.

Resultados são locais, anteriores à revisão humana. A cobertura permanece PARTIAL.
O programa corporativo original não está disponível; os resultados acima pertencem
às fixtures sintéticas. Logs, comandos, hashes e medições ficam no workspace em
`.lower-context-fix/evidence` e no baseline preservado em `artefatos-e2e`.
