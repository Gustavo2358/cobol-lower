# Snapshot de fontes e evidência histórica

Logs, relatórios de execução XML, gravações JFR e pacotes comprimidos históricos
em `docs/quality/` foram retirados da árvore atual. Relatórios Markdown e dados JSON
usados como baselines ou inventários continuam versionados. A limpeza não altera
código, testes, fixtures, dependências, pins ou workflows.

Fixtures SP em adapters/src/test/resources e docs/quality/WORK-LOWER-006/production-fixtures.json permanecem intactos.

Os resultados históricos continuam disponíveis no commit
`6656f2a059dd14243223d1de16080835869b087a`. Links para arquivos removidos apontam para essa revisão imutável.
Metadados históricos que registram caminhos e hashes descrevem a execução original;
esses arquivos brutos podem ser recuperados antes de uma auditoria retrospectiva.

A partir de um checkout Git com o histórico disponível, use uma pasta vazia:

```bash
mkdir /tmp/cobol-lower-historical-evidence
git archive 6656f2a059dd14243223d1de16080835869b087a docs/quality | tar -x -C /tmp/cobol-lower-historical-evidence
```

A remoção reduz o snapshot da main. Um clone com histórico completo ainda contém
os blobs antigos; o histórico Git não foi reescrito.

## Qualificação da limpeza

O gate `python3 -B scripts/harness/lean.py fast` passou nos três repositórios
em 2026-10-02, sem mudanças nos testes ou gates. Código, recursos de teste,
baselines executáveis, dependências e pins foram preservados.

A integração executou novamente o runtime anterior e os checkouts limpos:
CardDemo COACTUPC, `02_if_join` e `cobol/goto/if-jump.cbl`, cada um em JSON puro
e Zstandard, percorrendo frontend → lower → CFG → dependências. As 48 execuções
CLI terminaram com sucesso. Os 144 pares de arquivos de saída foram idênticos
byte a byte, incluindo os produtos JSON e os arquivos de interface regenerados.
Isso qualifica a limpeza nesses casos; não constitui uma nova execução do corpus
completo. Os PRs registram os commits finais e o resultado do FAST remoto.
