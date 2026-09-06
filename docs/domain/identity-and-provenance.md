# Identidade, provenance e determinismo

## Identidade de entrada e saída

Handles locais do Semantic Product são qualificados pela unit/publicação de entrada. Em joins, nunca usar somente `entry:0`, `statement:0`, nome canônico ou linha. A AIR possui namespaces próprios: publicação, unidade, operação, operando etc. Duas ocorrências iguais lexicalmente permanecem distintas quando o contrato assim as identifica.

O lowerer mantém correlação explícita e tipada entre identidades de entrada e saída. Uma origem pode produzir vários IDs AIR; vários conceitos auxiliares podem derivar da mesma ocorrência sem duplicar o fato de origem. O consumidor AIR não precisa interpretar handles COBOL.

## Política de IDs a fechar no primeiro checkpoint

IDs devem ser determinísticos para a mesma publicação semântica, revisão e opções/versionamento do lowering. Não usar relógio, UUID aleatório, object identity, `hashCode()` de objeto ou ordem de HashMap. Determinismo não é estabilidade longitudinal após editar fonte.

**Risco a evitar:** derivar `PublicationId` somente da identidade da unit faz revisões diferentes aparentarem o mesmo namespace. A chave da publicação precisa distinguir o conteúdo/revisão semânticos e a política de tradução. O bootstrap deve especificar a função de identidade, seus componentes e o tratamento de colisões, com testes positivos e negativos; não escolher uma hash apenas para passar a fixture.

Um digest do arquivo JSON bruto não é automaticamente identidade semântica, pois whitespace/ordem de propriedades podem variar sem mudar fatos. Uma estratégia de fingerprint canônica deve funcionar igualmente no caminho em memória. Já hash do arquivo é apropriado para proveniência de uma fixture, fora do domínio do lowering.

## Provenance

Entry deriva da origem publicada para a entry/PROCEDURE DIVISION; Return deriva do GOBACK. Labels e estruturas auxiliares possuem origem derivada com regra identificada. Preserve artefato, cadeia de include, original versus expanded, exatidão e lacunas onde o contrato possibilitar.

Não fabricar linha/coluna a partir de offset nem offset a partir de linha. Não confundir o arquivo JSON de transporte com o artefato COBOL original. Campo `file` é informação de origem; não é caminho a abrir em runtime.

O JSON atual publica números de linha/coluna, mas o uso desses números como um Span AIR exige confirmar base, unidade e convenção de fim no contrato fixado. Se uma convenção não estiver estabelecida, registre a limitação. Preserve a evidência bruta tipada na correlação/relatório e use uma origem AIR permitida sem afirmar precisão de coordenadas inexistente. Não converta silenciosamente “falta de convenção” em coordenadas fabricadas nem afirme suporte total de provenance.

## Ordenação e escala

Ordem de instruções AIR é semântica; ordem entre sequences não cria edges. Coleções de transporte respeitam sua canonicalização, não `sort` por nome como reparo. Construir índices uma vez por execução. Correlacionar inputs/outputs e contar omissões em O(N+R), onde R é o total de referências examinadas, salvo custo adicional explicitamente justificado.

Teste renomeação consistente de handles, colisões entre units, inputs equivalentes construídos independentemente, nova revisão com mesmo nome e metadados de execução fora do payload semântico.
