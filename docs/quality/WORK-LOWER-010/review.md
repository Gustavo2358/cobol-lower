# W1C self-review

Review do implementador no mesmo contexto; não é aprovação humana nem revisão
independente. A identidade do candidato é `CP0.json#/candidate_diff_sha256`.

Foi revisado o diff produtivo inteiro: Wire13/Materialize/decoder, SpInput,
admissão, revisão canônica, MoveHandler, InvokeHandler e montagem de Publication.
A admissão comum verifica fechamento de identidades, estrutura, inventário e
entry antes dos casts/joins CALL. CallAdmission constrói um plano de cadeia
explícita sem escolher sucessores por posição. Nenhuma ocorrência é filtrada.
O read do alvo seleciona o DataId da prova whole-item, e o oracle compara com
DataLink do mesmo binding. O ramo literal conserva texto lógico bruto.

Verificados effects all-memory incluindo ambiente, remainder all-control,
UnknownName/UnknownContract, assinatura vazia, coverage parcial e cinco
incertezas. Não há tracing de MOVE nem interpretação de PIC/nomes. O valor
fitted vem diretamente do ajuste SP com origem derivada. A identidade inclui
alvo, conhecimento, continuação e ajuste. Não foram introduzidos tipos AIR ou
codec/validator paralelos; a dependência core → codec continua proibida.

Testes: regra/oracle em prosa e fixtures reais foram congelados antes do código;
os três REDs são preservados. CallOracle é relacional e não chama helpers de
tradução/identidade. Testes físicos usam contracasos de shape; testes de domínio
também chamam a porta em memória. CallIntegrationSuite verifica validator,
round-trip, bytes canônicos, negativas e CLI. CallAtomicitySuite exercita falhas
reais do codec e I/O. Treze mutações compiláveis foram detectadas e restauradas;
o desafio adicional de placement testa I-04 em JSON físico válido.

Correções durante validação (logs de falha preservados): probe de versão futura
passou de 1.3 para 1.4 porque 1.3 agora é suportado; a árvore de teste AirJson
passou a acessar o envelope publication; a mutação dynamic-to-literal remove
correlações dos operandos que ela própria elimina, permitindo que o oracle
de alvo observe uma AIR estruturalmente válida. Isso não altera o esperado.

O guard de escopo anterior estava congelado para WORK-LOWER-007. W1C tem um novo
guard vinculado à baseline exata e allowlist de 12 arquivos; os demais fontes
produtivos permanecem imutáveis. A seleção dá prioridade explícita ao item W1C
ativo. O guard histórico não foi reescrito, e as mutações antigas que alteram
AirFileOutput ou introduzem codec alternativo continuam obrigatórias.
Os gates semânticos passaram a exigir marcadores CALL não zero e únicos; testes
do harness rejeitam ausência, zero, duplicação e receipt de tree AIR divergente.

Não há finding produtivo aberto identificado nesta revisão. A certificação e
os comandos efetivamente concluídos estão em CP0.json; este texto não substitui
o estado dos gates nem o recibo remoto do HEAD.

Limites: primeira slice com um CALL, sem argumentos/results/handlers, truncation,
refmod/subscript ou controle não linear. I-56 é obrigação, não finding. Há
limites operacionais no codec; os probes não qualificam programas grandes de
forma geral. Gates dedicados transport/integration continuam
SPECIFIED_NOT_IMPLEMENTED; a evidência executada é semantic/full e o script
real E2E, sem promover executores ausentes a PASS. W1D/W2 não iniciados.

A inclusão de CLI CALL no decoder suite antecipou a execução de duas mutações
legadas de saída. NON-SUCCESS-WRITES/LOWERING-EXIT-ZERO agora compilam o reactor
mutado e executam AirOutputSuite focal para preservar o oracle original. GREEN
e segundo GREEN continuam executando todas as suítes sem skip; erro de
compilação nunca conta como detecção. A falha anterior é preservada em
`full-output-oracle-order-failure.log`.

O diff staged revelou espaços finais emitidos pelo logger Maven e no patch RED
preservado. `.gitattributes` nesta pasta desativa normalização/whitespace somente
para logs/patches brutos. Os bytes e hashes são preservados; fontes e documentos
continuam sujeitos ao `diff --check` normal. A falha inicial do check foi mantida.
