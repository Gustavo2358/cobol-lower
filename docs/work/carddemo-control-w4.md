# CARDDEMO-CONTROL-W4 — famílias CICS

Status: IN_PROGRESS. Escopo: ASKTIME, FORMATTIME, ASSIGN APPLID/SYSID, INQUIRE PROGRAM, SEND TEXT e WRITEQ TD; sem recursão, filas TS ou inferência de dependências pelo nome do comando.

## Regra, contrato e limites

Cada forma é reconhecida pelo parser canônico CICS no frontend. A tabela de opções é fechada por comando. SP2.54 estende CicsCommandKind; opções, referências host e LENGTH estrutural preservam identidade/provenance. Lower somente valida e traduz fatos tipados. Sucesso possível autoriza conclusão normal; não prova sucesso externo nem valor retornado. RESP/NOHANDLE tratam condições localmente; sem eles permanece alternativa aberta de handler/default, sem fabricar destinos de handler. A forma SEND MAP conserva OVERFLOW próprio.

Direção de operandos: ASKTIME ABSTIME escreve; FORMATTIME lê ABSTIME/separadores e escreve resultados; ASSIGN escreve APPLID/SYSID; INQUIRE PROGRAM lê PROGRAM, sem invocar programa; SEND TEXT e WRITEQ TD leem FROM/LENGTH e escrevem RESP/RESP2. Acesso não materializável mantém MAY visível. EIB/efeitos externos não fechados impedem hostEffects completo nessas novas famílias. Nunca MUST por valor retornado. QUEUE não vira FILE.

Repetição de NOHANDLE sem operandos é idempotente segundo DFH7057I W. Preservar ambas as opções escritas e diagnóstico de warning; duplicatas com binding/operando continuam fora do subconjunto. Separadores DATESEP/TIMESEP permitem argumento omitido. Flags estranhas/combinações inválidas permanecem indisponíveis.

Algoritmo linear nas opções mais parsing canônico dos operandos; sem interpretação downstream do rawText. Tabela explícita e enums limitam superfície, sem fallback genérico.

## Oráculos antes da implementação

Cada família com CALL posterior e dentro de PERFORM, sem/RESP/NOHANDLE, estado HANDLE existente, memória disponível/ausente; outputs WRITE, inputs READ e LENGTH OF não escreve objeto; unknown effects não mata valores; literal de fila/programa consultado não cria relação CALL/FILE. Mutação de opção, duplicata de binding, versão anterior, enum/role contraditório e ausência de campo obrigatório são rejeitados ou gaps. Todos os 36 witnesses reais serão reexecutados e comparados por fatos, candidatos e supports.

## Fontes primárias verificadas

- [ASKTIME](https://www.ibm.com/docs/en/cics-ts/6.x?topic=summary-asktime): ABSTIME opcional e atualização EIBDATE/EIBTIME.
- [FORMATTIME](https://www.ibm.com/docs/en/cics-ts/6.x?topic=summary-formattime): entradas, outputs, separadores e INVREQ.
- [ASSIGN](https://www.ibm.com/docs/en/cics-ts/6.x?topic=summary-assign): data-areas APPLID e SYSID; INVREQ.
- [INQUIRE PROGRAM](https://www.ibm.com/docs/en/cics-ts/6.x?topic=commands-inquire-program): consulta, sem load/invoke; condições APPNOTFOUND/END/ILLOGIC/NOTAUTH/PGMIDERR.
- [SEND TEXT](https://www.ibm.com/docs/en/cics-ts/6.x?topic=summary-send-text): FROM/LENGTH/ERASE/FREEKB; erros separados da conclusão.
- [WRITEQ TD](https://www.ibm.com/docs/en/cics-ts/6.x?topic=summary-writeq-td): QUEUE/FROM/LENGTH/SYSID e condições da fila.
- [DFH7057I W](https://www.ibm.com/docs/en/cics-ts/5.5.0?topic=messages-dfh7057i-w): especificação duplicada de opção ignorada pelo translator.

## Qualificação final da onda

- Runtime `w4-development-02`; `w4-carddemo-02`: 73/73 em quatro etapas, 73/73 wire. 53 SPs byte-idênticos à W3 e 20 alterados; zero perdas e zero novos candidatos. Supports físicos preservados; os 70 supports condicionais anteriores conservam sua evidência fonte (nenhuma migração física ou evidência sem correspondência).
- Todos os 36 witnesses D5 publicam família tipada suportada e NORMAL com prova; condições abertas permanecem separadas (`w4-witnesses.json`).
- `w4-probes-02`: 92/92 nas quatro etapas. CALL posterior/PERFORM, memória ausente e kill posterior comprovado, aliases, terminais e negativos de W1–W3 incluídos. Consulta de programa/fila não cria dependência CALL/FILE.
- Frontend FAST: 639 testes, zero falhas (86 s). Lower FAST: passou (412 s). CicsCatalogueSuite adicional: 25 casos e 120 mutações inválidas, incluindo wire antigo, enum, role, hostEffects indevidamente fechado e indexed query. Novo teste registrado no FAST para execuções seguintes; passou focalmente após a execução principal.
- Primeira runtime e seus 73 casos preservados. O único gap da primeira qualificação era INQUIRE com subscrito; o novo replay cobre a correção. Uma tentativa de gate lower foi invalidada pela sobreposição de builds no mesmo diretório, que removeu classes durante javap. O FAST foi repetido sem build concorrente e passou. Não foi falha semântica.
- Qualificação histórica ampla e combinação W5/W6 serão reexecutadas ao fim do escopo autorizado. W7 não iniciada; sem merge. Modelos IBM e efeitos externos não concedem valor runtime ou MUST.
