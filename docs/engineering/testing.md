# Testes e oracles

## Ordem

Regra/contrato → classes de equivalência → contracasos → expected independente → RED → implementação → GREEN → challenge → segundo GREEN. Expected não nasce do output atual do lowerer. Snapshot só pode ser atualizado com justificativa de mudança de contrato ou oracle corrigido revisado.

## Pirâmide de evidência do primeiro slice

1. AIR manual com Return valida o target, sem decoder/lowerer.
2. Fixture real SP 1.1.0, produzida pelo frontend fixado, valida o input físico.
3. Input em memória valida a porta e as regras sem JSON.
4. JSON adapter + core produz resultado equivalente ao caminho em memória.
5. AIR checker valida estrutura da saída; oracle independente verifica Entry/start/Return/coverage/provenance.
6. E2E com CFG ocorre em checkpoint separado e verifica comportamento composto, não substitui os testes locais.

O build deve falhar se a suíte obrigatória não for descoberta/executada. “Zero failures, zero tests” não é evidência. Tests skipped ou gates não disponíveis são reportados, não somados aos verdes.

## Classes mínimas

Versão/schema; referência inexistente; duplicata/namespace; availability KNOWN/PARTIAL/UNAVAILABLE/INPUT_MISSING; assinatura zero versus null versus count positivo; GOBACK versus OBSERVED/STOP; statement extra não omitido; gap preservado; input/output imutáveis; determinismo entre construções independentes; arquivo versus memória; ownership da origem.

Contagem exata de 1 Unit/Entry/Sequence/Return vale no fixture mínimo por construção. Totais de corpus grande são telemetria, não prova semântica isolada. Reconciliar contagem com inventário é invariante válida em qualquer tamanho.

## Metamorfismo

Definir pré-condição, transformação e relação esperada. Exemplos: permutar propriedades JSON não muda fato; renomear consistentemente handles não muda controle modulo correlação; mudar nome/filename não transforma Return em Halt; perder start conhecido não aumenta readiness; adicionar statement fora do perfil muda o resultado para bloqueado, não mantém sucesso amputado.

Não permutar arrays com ordem significativa, não supor que inserir statement é neutro e não comparar bytes AIR antes de existir binding determinístico. Testes do core operam sobre contratos tipados, não código-fonte COBOL.

## Independência

Oracle não pode chamar o mesmo mapper de ID/coverage/controle que tenta verificar. É permitido reutilizar builders mecânicos de fixtures, desde que expected relacional não seja calculado pela implementação. AirValidator não prova o mapping GOBACK→Return: uma AIR com Halt pode ser estruturalmente válida e semanticamente errada para esse input.

Fixtures de produção são versionadas com provenance/digest e procedimento de regeneração isolado. Um arquivo escrito manualmente deve ser rotulado synthetic, nunca “output real do proleap”. [Protocolo de fixtures](../evals/fixtures.md).
