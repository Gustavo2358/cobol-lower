# Identidade, provenance e determinismo

## Identidade de entrada e saída

Handles locais do Semantic Product são qualificados pela unit/publicação de entrada. Em joins, nunca usar somente `entry:0`, `statement:0`, nome canônico ou linha. A AIR possui namespaces próprios: publicação, unidade, operação, operando etc. Duas ocorrências iguais lexicalmente permanecem distintas quando o contrato assim as identifica.

O lowerer mantém correlação explícita e tipada entre identidades de entrada e saída. Uma origem pode produzir vários IDs AIR; vários conceitos auxiliares podem derivar da mesma ocorrência sem duplicar o fato de origem. O consumidor AIR não precisa interpretar handles COBOL.

## Política de IDs implementada — xxh3-128-v1

IDs devem ser determinísticos para a mesma publicação semântica, revisão e opções/versionamento do lowering. Não usar relógio, UUID aleatório, object identity, `hashCode()` de objeto ou ordem de HashMap. Determinismo não é estabilidade longitudinal após editar fonte.

**Risco a evitar:** derivar `PublicationId` somente da identidade da unit faz revisões diferentes aparentarem o mesmo namespace. A chave da publicação precisa distinguir o conteúdo/revisão semânticos e a política de tradução. O bootstrap deve especificar a função de identidade, seus componentes e o tratamento de colisões, com testes positivos e negativos; não escolher uma hash apenas para passar a fixture.

Um hash do arquivo JSON bruto não é automaticamente identidade semântica, pois whitespace/ordem de propriedades podem variar sem mudar fatos. Uma estratégia de fingerprint canônica deve funcionar igualmente no caminho em memória. Já hash do arquivo é apropriado para proveniência de uma fixture, fora do domínio do lowering.

## Provenance

WORK-LOWER-004 substitui explicitamente a identidade integral canonical-v1 do CP3 por
XXH3-128 completo, 32 caracteres hexadecimais minúsculos. A preimagem tem domínio
`minimal-entry-goback@1/AIR2/SP1.1/xxh3-128-v1/` e os mesmos fatos admitidos e enquadramento:
presença opcional, contagem de listas, strings com comprimento UTF-16 e quatro hex por
code unit. hash4j 0.30.0, seed zero, formato high64/low64 com zeros iniciais preservados. Bytes ASCII são enviados incrementalmente ao hash, sem montar revisão integral.
A hipótese é boa dispersão de hash não criptográfico e baixa probabilidade de colisões acidentais; não se alega resistência criptográfica ou injetividade matemática
do hash ou impossibilidade de colisão. A política é aplicável igualmente à porta em memória.

Tempo O(B) para processar fatos, memória auxiliar O(1) no cálculo do hash. O ID tem tamanho
fixo; maximumIdentityCharacters limita somente esses 32 caracteres finais. Valores menores
produzem IMPLEMENTATION_LIMIT/IDENTITY_LIMIT sem truncamento; não há limite oculto sobre
o volume canônico. Os limites de admissão/validação continuam independentes.
`CanonicalRevision.token` conserva sua codificação integral anterior para IDs locais/SourceKeys.
Opções operacionais/telemetria não integram fatos semânticos. Unit/Entry/Sequence/Return
possuem namespaces AIR próprios; joins usam IDs tipados completos. Contrato/fontes e oracles:
[CP0](../work/active/WORK-LOWER-004/spec.md).

Entry deriva da origem publicada para a entry/PROCEDURE DIVISION; Return deriva do GOBACK. Labels e estruturas auxiliares possuem origem derivada com regra identificada. Preserve artefato, cadeia de include, original versus expanded, exatidão e lacunas onde o contrato possibilitar.

Não fabricar linha/coluna a partir de offset nem offset a partir de linha. Não confundir o arquivo JSON de transporte com o artefato COBOL original. Campo `file` é informação de origem; não é caminho a abrir em runtime.

O JSON atual publica números de linha/coluna, mas o uso desses números como um Span AIR exige confirmar base, unidade e convenção de fim no contrato fixado. Se uma convenção não estiver estabelecida, registre a limitação. Preserve a evidência bruta tipada na correlação/relatório e use uma origem AIR permitida sem afirmar precisão de coordenadas inexistente. Não converta silenciosamente “falta de convenção” em coordenadas fabricadas nem afirme suporte total de provenance.

## Ordenação e escala

Convenções verificadas no produtor fixado em CP3: linhas base1, colunas base0, UNICODE_SCALAR, fim inclusivo. Original e expanded geram artefatos/origens separados combinados por Derived; auxiliares usam regra identificada. Zero/ordem inválida de coordenadas remove apenas a afirmação de localização exata e produz limitação tipada; evidência bruta permanece no input correlacionado. Include frame preserva including/included/requestedName, mas includeLine sozinho não vira span completo: site ausente e limitação explícita, com linha bruta preservada. Nunca abrir esses filenames.

Ledger CP4 para o corpus sintético N GOBACKs: admissão visits=7N+9, references=N+1, provenance=3N+3; JSON nodes=82+37N, physical values=82+35N. Esses contadores verificam o algoritmo/shape de teste, não todos os custos internos de biblioteca nem SLA. N>1 permanece fora do perfil. Oracle de contagem independente e mutação índice→scan detectam custo quadrático; JSON é materializado sob limite de bytes, não streaming.

Ordem de instruções AIR é semântica; ordem entre sequences não cria edges. Coleções de transporte respeitam sua canonicalização, não `sort` por nome como reparo. Construir índices uma vez por execução. Correlacionar inputs/outputs e contar omissões em O(N+R), onde R é o total de referências examinadas, salvo custo adicional explicitamente justificado.

Teste renomeação consistente de handles, colisões entre units, inputs equivalentes construídos independentemente, nova revisão com mesmo nome e metadados de execução fora do payload semântico.
