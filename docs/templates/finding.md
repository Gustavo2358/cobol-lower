# Finding <ID> — <título verificável>

**Status:** em avaliação. **Severity/prioridade:** a determinar separadamente.
**Baseline:** <repo e SHA>. **Arquivo/linhas:** <local real>.

## Reprodução

Input mínimo, comando/opções, resultado observado, resultado esperado e fonte da expectativa. Referenciar evidência reproduzível; não incluir fonte/confidencialidade indevida.

## Regra e causa

Qual contrato falhou? Que fatos permanecem corretos? Distinguir erro de implementação, teste, contrato, limite operacional e suporte ainda não entregue. Não transformar comportamento conservador em defeito sem argumento.

## Impacto downstream

```yaml
downstream_impact:
  class: UNASSESSED
  rationale: "A primeira fronteira incorreta ainda não foi demonstrada."
  evidence:
    - "Substituir pela observação real; não considerar este placeholder evidência."
  reassess_when:
    - "Substituir pelo fato/oracle necessário."
```

Usar somente a taxonomia de oito classes da política. Se faltar evidência, manter UNASSESSED. Impacto não define severity nem autoriza remediação.

## Correção proposta e teste de regressão

Menor alteração que corrige a classe geral, oracle independente e limites. Não reescrever o contrato upstream nem expected para acomodar o erro.
