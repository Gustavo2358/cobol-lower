# CARDDEMO-CONTROL-W5 — SQL e DL/I

Status: IN_PROGRESS. Escopo: CHKP/REPL/ISRT/DLET, SELECT INTO/UPDATE/INSERT/DELETE, DECLARE CURSOR/OPEN/FETCH/CLOSE. Recursão e ALTER executável fora da campanha.

## Semântica e fronteiras

O frontend reconhece sintaxe fechada completa e publica sucesso possível com continuidade normal. Nenhum sucesso externo é inferido. SQL mantém outcomes abertos de erro, warning/fim de dados e possíveis ações de tratamento; SQLCODE/SQLSTATE não têm valores estáticos concedidos. DL/I preserva retorno e falha aberta, sem supor posição de banco/PCB válida. DECLARE CURSOR descreve uma consulta, não aloca área COBOL nem executa a consulta. A ordem DECLARE/OPEN não prova estado runtime.

Operandos host viram referências na AST antes da resolução. SQL INTO/FETCH escreve MAY; predicados/SET/VALUES leem; buffers FROM DL/I são lidos. Efeitos implícitos SQLCA/DIB/PCB e dados desconhecidos conservam limites ALL, ambiente UNKNOWN e nenhum MUST. SP2.55 acrescenta provas explícitas SQL_HOST_OPERANDS e DLI_EXTERNAL_OPERANDS; lower valida a porta tipada e wire e reutiliza tradução genérica de efeitos/topologia. Não interpretar texto SQL/DL/I downstream.

Parser SQL por tokens com offsets, expressões e predicados recursivos; consome payload inteiro, rejeita truncamento, comandos compostos e formas não admitidas. Reutiliza a gramática COBOL para os hosts. Não é parser geral SQL: SQL dinâmico, rowsets e outros dialetos permanecem opacos. Dispatch completo de WHENEVER continua aberto; condições de status testadas explicitamente pelo COBOL conservam os dois braços. DECLARE TABLE já tratado em W3 mantém seu contrato.

Complexidade linear no número de tokens/operandos e profundidade proporcional ao aninhamento da expressão. Nenhuma iteração sobre runtime ou banco. Composição regional/PERFORM permanece autoridade da ControlTopology existente.

## Oráculos antes da implementação

CHKP com área/literal; REPL/ISRT/DLET com PCB, buffers, múltiplos segmentos e LENGTH OF; opções inválidas/truncadas não recebem conclusão. SELECT com WHERE/AND/OR/LIKE/funções e host outputs; UPDATE/INSERT/DELETE com hosts; DECLARE CURSOR antes/depois de áreas independentes, OPEN/FETCH/CLOSE com status e IF; host ausente e SQLCA sintética mantêm MAY e candidatos. CALL pós-comando e retorno PERFORM, terminal em braço, sobrescrita exata posterior, versões e provas malformadas, refs/provenance válidas. Reexecutar as 19 ocorrências SQL e sete DL/I e comparar corpus.

## Fontes primárias verificadas

- [IMS CHKP](https://www.ibm.com/docs/en/ims/15.6.0?topic=commands-chkp-command): área/literal ID, retorno de checkpoint; requer I/O PCB e ambiente batch/BMP, não CICS.
- [IMS REPL](https://www.ibm.com/docs/en/ims/15.6.0?topic=commands-repl-command), [ISRT](https://www.ibm.com/docs/en/ims/15.6.0?topic=commands-isrt-command), [DLET](https://www.ibm.com/docs/en/ims/15.6.0?topic=commands-dlet-command); sintaxe complementar [manual IBM](https://publibz.boulder.ibm.com/epubs/pdf/dfsp50d4.pdf).
- [Db2 SELECT INTO](https://www.ibm.com/docs/en/db2-for-zos/13.0.0?topic=statements-select-into), [SQLCODE +100](https://www.ibm.com/docs/en/db2-for-zos/13.0.0?topic=codes-100).
- [DECLARE CURSOR](https://www.ibm.com/docs/en/db2-for-zos/13.0.0?topic=statements-declare-cursor), [OPEN](https://www.ibm.com/docs/en/db2-for-zos/13.0.0?topic=statements-open), [FETCH](https://www.ibm.com/docs/en/db2-for-zos/13.0.0?topic=statements-fetch).
- [WHENEVER](https://www.ibm.com/docs/en/db2-for-zos/13.0.0?topic=statements-whenever): diretiva de escopo lexical; não assumir que erro implica sucesso/fallthrough quando a disposição não está provada.

## Qualificação W5 — 28/09/2026

Implementação pronta para revisão, sem merge. Runtime congelada `w5-development-01`: 73/73 CardDemo nas quatro etapas; 26/26 testemunhas reais (19 SQL + 7 DL/I) publicam normal e efeitos abertos. 66 SPs byte-idênticos à W4, sete alterados. Zero perda de candidato/support físico; 70 candidatos condicionais preservados, sem migração ou perda de evidência. Um candidato adicional `COTRTLIC` em `statement:295`: VALUE de LIT-THISPGM → MOVE para CDEMO-FROM-PROGRAM → MOVE para CDEMO-TO-PROGRAM; DECLARE CURSOR deixou de contaminar armazenamento independente.

118 sondagens nas quatro etapas; seis fontes geradas ultrapassavam coluna 72 (INSERT/REPL/DLET), corrigidas e reexecutadas em outputs novos; evidência original preservada. 26 sondagens de banco verificam candidatos FIRST/SECOND, dois braços de status, retorno PERFORM e negativos terminais. `DatabaseControlSuite`: 24 fontes/SPs reais, 120 mutações wire/porta tipada; estados de memória ausente/modelada, branches e retorno. O helper de travessia AIR foi completado para Branch; não houve mudança de arestas para satisfazê-lo.

Frontend FAST: 642 testes PASS (`w5-frontend-fast-02.log`). Lower FAST PASS (`w5-lower-fast-01.log`, 344 s); a suíte nova foi executada depois em teste focal (`w5-lower-database-02.log`), e será incluída no próximo FAST. Primeiro FAST frontend encontrou quatro expectativas históricas: unknown-control usa agora SQL dinâmico fora do subset, e REPL exige seus hosts reais; os negativos semânticos foram preservados. Regressão ampla PERFORM/Chaos/aliases e corpus de fixtures será reexecutada ao qualificar W6, que altera topologia compartilhada.

Limites: sintaxe SQL fechada, sem SQL dinâmico/rowsets/dispatch WHENEVER; DL/I sem prova de posicionamento/ambiente correto ou status. SQLCA/DIB/PCB e efeitos implícitos continuam abertos. Gaps e PARTIAL permanecem; concluir execução não é afirmar análise completa. Evidência local em `.carddemo-control/evidence/w5-*`.
