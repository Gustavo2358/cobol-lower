# Fronteiras e responsabilidades

## Domínio

Regras de tradução, critérios semânticos de admissibilidade, mapeamento de cobertura/lacunas e construção da AIR com identidades próprias. Não lê arquivos; não parseia JSON; não busca símbolos; não consulta o CFG. Uma regra pode conhecer COBOL **conforme o significado publicado pelo Semantic Product**, não inferir esse significado novamente pelo source.

## Aplicação e portas

Orquestra validação da entrada normalizada, seleção explícita de perfil, aplicação de regras, validação AIR e entrega do resultado. Define interfaces e tipos que expressam suas necessidades. Não escolhe biblioteca JSON nem caminho de saída. Não captura exceções indiscriminadamente para transformá-las em sucesso parcial.

O caso de uso é acessível por uma porta como `LowerCobol.lower(SemanticProductInput, LoweringOptions)`. Nomes concretos são fechados no bootstrap. O resultado de sucesso contém a `Publication` de `air-java`, relatório tipado e correlações; bloqueios não contêm publicação apresentada como válida.

## Adapters

Entrada JSON: valida encoding, sintaxe, schema/version e forma física; materializa DTOs locais ao adapter; converte para o contrato interno. Validação semântica de referências e elegibilidade continua necessária no core para chamadas em memória.

Saída JSON futura: codifica uma AIR já publicada segundo o binding. Não normaliza semântica, não elimina gaps, não infere operações e não usa serialização reflexiva como autoridade normativa.

Adapter em memória futuro: traduz apenas o contrato público materializado do frontend para a porta interna. Pode conhecer a API pública upstream em um módulo isolado; o core não ganha dependência de ProLeap. Não recebe callbacks para enriquecer fatos.

## Composition root

Escolhe adapters, parâmetros operacionais e políticas explícitas; injeta dependências por construtor. Não contém switches de semântica, tabelas de conversão COBOL, regras de readiness ou joins do Semantic Product. Sem service locator global, singletons mutáveis ou descoberta refletiva que torne a seleção de regras incidental.

## air-java

É o target semântico compartilhado. Nenhum `LocalPublication`, cópia de `Entry`, `Sequence` ou `Return` substitui seu modelo. Wrappers de resultado e correlação são legítimos quando não criam uma segunda IR. `AirValidator` é reutilizado; um oracle independente de tradução não é um segundo validador completo da AIR.

## O que diferimos

Jackson versus outra biblioteca, formato de CLI, deploy, armazenamento e integração de processo são decisões de borda. Java 21 e o contrato AIR compartilhado já restringem o runtime; Clean Architecture não significa adiar decisões semânticas necessárias à corretude. Base conceitual: REF-CLEAN e REF-HEX em [fontes externas](../sources/research.md).
