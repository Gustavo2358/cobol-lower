# Fontes externas verificadas

Fontes consultadas em 2026-09-06. As sínteses abaixo são curtas; decisões específicas do projeto estão nos documentos canônicos. Papers acessados somente por resumo não são apresentados como leitura integral. Nenhum PDF/paper ou código externo é redistribuído.

## REF-CLEAN

**Autores/organização:** Robert C. Martin. **Título:** The Clean Architecture. **Data/edição:** 2012.

**Fonte:** [The Clean Architecture](https://blog.cleancoder.com/uncle-bob/2012/08/13/the-clean-architecture.html).

**Acesso efetivo:** Artigo primário: seção Dependency Rule e passagem de dados entre fronteiras.

**Sustenta:** Dependências e formatos externos não devem governar círculos internos.

**Aplicação e limite local:** Portas/tipos internos e adapters externos. Não é citação literal de passagem não consultada do livro.

## REF-HEX

**Autores/organização:** Alistair Cockburn. **Título:** Hexagonal architecture — original 2005 article. **Data/edição:** 2005.

**Fonte:** [Hexagonal architecture — original 2005 article](https://alistair.cockburn.us/hexagonal-architecture).

**Acesso efetivo:** Artigo primário: intent, ports/adapters e isolamento de testes.

**Sustenta:** Aplicação pode ser dirigida por testes/programas/adapters tecnológicos sem conhecer a tecnologia.

**Aplicação e limite local:** Teste arquivo/memória e driving/driven ports. Não prescreve quantidade fixa de interfaces.

## REF-MLIR

**Autores/organização:** LLVM/MLIR project. **Título:** Dialect Conversion. **Data/edição:** documentação consultada em 2026-09-06.

**Fonte:** [Dialect Conversion](https://mlir.llvm.org/docs/DialectConversion/).

**Acesso efetivo:** Documentação oficial: conversion target, legalidade, patterns e modos.

**Sustenta:** O framework separa alvo de conversão, padrões e conversão opcional de tipos.

**Aplicação e limite local:** Inspirar admissibilidade/rules locais, sem importar algoritmo, framework, ordem de traversal ou semântica de partial conversion.

## REF-TV

**Autores/organização:** Amir Pnueli; M. Siegel; E. Singerman. **Título:** Translation validation — TACAS 1998, LNCS 1384, 151–166. **Data/edição:** 1998.

**Fonte:** [Translation validation — TACAS 1998, LNCS 1384, 151–166](https://weizmann.esploro.exlibrisgroup.com/esploro/outputs/conferenceProceeding/Translation-validation/993262143603596).

**Acesso efetivo:** Resumo e metadata em repositório institucional; não leitura integral do paper. DOI 10.1007/BFb0054170.

**Sustenta:** Validação de traduções individuais é distinta de verificar previamente todo compilador.

**Aplicação e limite local:** Motiva oracle de tradução source/target além da forma AIR. Não afirmar implementação de translation validation formal completa.

## REF-COMPCERT

**Autores/organização:** CompCert project / Xavier Leroy. **Título:** CompCert C: a trustworthy compiler. **Data/edição:** manual consultado em 2026-09-06.

**Fonte:** [CompCert C: a trustworthy compiler](https://compcert.org/man/manual001.html).

**Acesso efetivo:** Manual oficial: semantic preservation e limites de escopo.

**Sustenta:** Corretude do compilador é enunciada como propriedade de preservação entre origem e destino, com fronteiras explícitas.

**Aplicação e limite local:** Separar validade estrutural de fidelidade do lowering. Não herdar prova do CompCert nem transplantar refinamento para análise conservadora sem definir a relação.

## REF-MT

**Autores/organização:** T. Y. Chen; F.-C. Kuo; H. Liu; P.-L. Poon; D. Towey; T. H. Tse; Z. Q. Zhou. **Título:** Metamorphic Testing: A Review of Challenges and Opportunities, ACM Computing Surveys 51(1), art. 4. **Data/edição:** 2018.

**Fonte:** [Metamorphic Testing: A Review of Challenges and Opportunities, ACM Computing Surveys 51(1), art. 4](https://nottingham-repository.worktribe.com/output/925152/metamorphic-testing-a-review-of-challenges-and-opportunities).

**Acesso efetivo:** Resumo autoral e metadata institucional; não leitura integral. DOI 10.1145/3143561.

**Sustenta:** Relações metamórficas expressam propriedades necessárias entre múltiplos inputs e outputs esperados.

**Aplicação e limite local:** Declarar pré-condições e relações antes de mutar fixtures; não afirmar que mutações arbitrárias preservam semântica.

## REF-MAVEN

**Autores/organização:** Apache Maven project. **Título:** Guide to Working with Multiple Modules. **Data/edição:** guia Maven 3 consultado em 2026-09-06.

**Fonte:** [Guide to Working with Multiple Modules](https://maven.apache.org/guides/mini/guide-multiple-modules.html).

**Acesso efetivo:** Documentação oficial: reactor e ordenação de módulos.

**Sustenta:** O reactor agrega módulos e ordena builds conforme dependências.

**Aplicação e limite local:** Integração futura pode usar reactor; isso não garante compatibilidade de API, release ou ausência de trabalho de wiring.
