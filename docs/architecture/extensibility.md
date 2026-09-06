# Evolução por capacidades, não por exceções de corpus

## Compromisso

O Semantic Product inicial é limitado e vai evoluir. O lowerer deve crescer sem redesenhar portas/envelope a cada construção. Extensão localizada pode exigir novas variantes tipadas, regras, visitors exaustivos, adapters, validators e testes; isso é aceitável. Não prometer compatibilidade binária automática de sealed types.

## Roteiro de uma nova capacidade

Antes do código, verificar o novo contrato upstream, precondições AIR e necessidade de consumidor. Atualizar a [matriz bilateral](../domain/capability-matrix.md), registrar um work item e criar oracle independente com contracaso. Só então acrescentar a família e sua regra. A regra deve preservar todas as ocorrências elegíveis no escopo, sem limite singleton escondido.

O adapter conhece a sintaxe física da nova variante; o domínio conhece seus fatos publicados; o lowerer escolhe a representação AIR. Strings `observedKind` nunca autorizam semântica executável. Handler ausente é diagnóstico explícito, não `nop`.

## Open/Closed aplicado concretamente

O coordenador orquestra; não vira uma classe central que acumula regras COBOL. Cada família semanticamente coesa tem regra testável. Infraestrutura é substituível por interfaces definidas no lado interno. O primeiro slice pode usar um dispatcher exaustivo pequeno. Quando houver registro de handlers, duas regras concorrentes para o mesmo perfil devem ser rejeitadas; ordem de registro não resolve semântica.

Não criar hoje classes vazias de IF, MOVE, CALL, PERFORM ou um motor de plugins universal. A preparação consiste em contratos, seleção explícita de capacidades e testes de arquitetura. Interfaces novas precisam de razão de variação atual ou do próximo slice autorizado.

## Três mudanças diferentes

1. **Troca de transporte:** mesma semântica/porta; altera adapter/wiring e prova equivalência.
2. **Enriquecimento semântico:** novos fatos ou garantias; altera regras/contrato conforme versionamento e exige novas provas.
3. **Mudança AIR/air-java:** revalida o target e consumidores; não é resolvida com DTO paralelo.

Compatibilidade SemVer upstream não é negociada por intuição. Nova versão JSON ou variante só é admitida por uma decisão de contrato, mesmo que tenha sido uma mudança minor no produtor.

## Referência de projeto

REF-MLIR ilustra separação entre alvo de conversão, legalidade e regras de reescrita. Aqui adotamos apenas a disciplina de critérios de admissibilidade e regras delimitadas, não o framework MLIR, sua ordem de traversal ou sua política de partial conversion. A AIR e o contrato de entrada continuam autoridades.
