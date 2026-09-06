# Oracle independente FIRST-LOWER

## Input testemunha

Programa de origem apenas para geração offline da fixture SP: AIR-FIRST, sem DATA/parâmetros/RETURNING, GOBACK único. O lowerer nunca recebe esse COBOL em runtime. O input real é SP 1.1.0 cujo start referencia o GOBACK.

## Observação esperada

```text
Publication AIR 2.0.0
  Unit selecionada, corpo AVAILABLE
    Entry PRIMARY correlacionada
      signature: parameters=[]/remainder=none; results=[]/remainder=none
      initialLabel: label criado para o start publicado
    Sequence inicial
      instructions=[]
      terminator=Return(values=[])
```

Inventory de entries alternativas permanece parcial com lacuna correlacionada. O controle local é conhecido; não existe Halt nem successor local por ordem física. Entry e Return mantêm proveniências distintas conforme input; estrutura auxiliar possui origem derivada.

## Não basta

Uma Publication só com Halt pode passar no validador e continuar errada para esse source. Um Return sem origem/gap pode reproduzir o desenho e perder o contrato. `1 sequence` sozinho não prova vínculo start. Comparar todos esses aspectos por expected independente.

## Entrada maior

Adicionar CONTINUE/OBSERVED muda a admissão do perfil: não publicar apenas o primeiro Return. Retornar unsupported/blocked com todas as ocorrências relevantes apontadas. Não inventar CFG para demonstrar isso.

## Generalização mínima

Variar nome do programa, filename e handle do GOBACK com referências coerentes não muda a espécie Return. IDs AIR podem mudar segundo revisão/correlação; equivalência de controle não exige igualdade longitudinal de IDs.
