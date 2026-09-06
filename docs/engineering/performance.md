# Performance e escala

## Contexto de produto

O ecossistema pretende analisar muitos arquivos e programas grandes. Não foi medido throughput deste lowerer e não há SLA de tempo/memória comprovado. O primeiro fixture é mínimo; arquitetura não deve introduzir custos que se tornem quadráticos quando a cobertura crescer.

Definir N=statements, D=declarações, E=entries, R=referências, G=gaps e P=componentes de provenance efetivamente processados. Meta inicial: leitura/indexação/validação do inventário e tradução do perfil sem scans globais por item; aproximadamente O(N+D+E+R+G+P) além de serialização/hashing explicitamente medidos. Não declarar complexidade só porque foi usado HashMap.

## Regras

Construir índices uma vez por execução; resolver IDs pela identidade completa; evitar `for statement → scan statements`, reconstrução repetida de árvore e cópia profunda de Publication por etapa. Cada visita deve ter propósito; caches são locais ao snapshot e não misturam revisões.

Recursão em estrutura aninhada exige limite explícito/estratégia de traversal; limite não pode esconder parte do input. Fechar recursos no adapter. Paralelismo não é requisito do primeiro slice; não introduzir estado global mutável nem ordem de thread como origem de IDs.

## Gates

G-PERFORMANCE começa por contadores determinísticos: número de visitas/indexações/lookups sobre input sintético controlado. Dobrar tamanho deve respeitar limite calculado, não threshold arbitrário de hardware. Tempo real e heap são telemetria complementar com máquina/JDK/versão registrados.

Mesmo fora do primeiro shape, o decoder/validator pode ser testado com muitos statements GOBACK para assegurar diagnóstico sem truncamento e custo proporcional. Isso não transforma a tradução plural em suporte declarado antes de seu checkpoint.

## Decisão de otimização

Primeiro demonstrar bottleneck ou custo assintótico, escolher algoritmo com oracle independente e preservar semântica. Não trocar exatidão por heurística de corpus para atingir prazo. Um algoritmo exato mais lento em entradas pequenas pode ser oracle do otimizado, nunca regressão de performance escondida.
