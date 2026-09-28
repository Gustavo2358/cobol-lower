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
