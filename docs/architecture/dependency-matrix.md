# Matriz de dependências

Esta matriz é contrato dos futuros gates; não descreve packages já existentes.

| Camada | Pode depender | Não pode depender |
| --- | --- | --- |
| domain | JDK de valores/coleções/matemática e modelo `air-java` | application, adapters, JSON, IO, frontend, CFG |
| application/ports | domain, tipos de aplicação, `air-java`/AirValidator | adapters, Jackson/Gson, filesystem, rede, CLI, frontend, CFG |
| adapter de entrada | portas e tipos internos, JSON/I/O necessários | regras semânticas duplicadas, internals do frontend |
| adapter de saída | resultado/Publicação AIR, binding e tecnologia escolhida | cálculo de controle/valores ou alteração da AIR |
| adapter de integração em memória | porta interna e contrato público upstream | callbacks de enriquecimento, AST/resolver privados |
| composition | application, adapters e configuração | regras de tradução no wiring |
| testkit | API pública sob teste e builders manuais | expected gerado pelo lowerer como única autoridade |

## Enforcement a implementar

G-ARCH verificará imports, assinaturas públicas e bytecode/dependências transitivas. Um grep de strings não prova a regra sozinho. Devem ser detectados `Path`, `File`, `InputStream`, JsonNode/ObjectMapper, annotations de serialização e referência a módulos externos proibidos nos pacotes internos. Tipos semânticos como um identificador textual de artefato são permitidos, sem I/O.

G-ARCH também verifica direção domain ← application ← adapters, ausência de ciclo e de modelo AIR paralelo. O gate não pode concluir que “não existe cópia de AIR” apenas por nomes de arquivos; revisão da API e dos contratos complementa a checagem estrutural.

Os nomes de módulos/pacotes entram em configuração explícita quando o bootstrap existir. Não adivinhar a camada por substring nem exigir layout rígido antes desse momento.

## Negativos obrigatórios

Uma dependência de filesystem no core, uma annotation JSON no input interno e uma cópia de Publication devem produzir RED pelo mecanismo apropriado. Import não utilizado pode não aparecer no bytecode; por isso a evidência deve dizer se foi uma guarda de source, assinatura ou dependência que detectou a mutação.
