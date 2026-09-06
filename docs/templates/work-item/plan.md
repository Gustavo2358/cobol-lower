# <WORK-ID> — Plano

## Fatiamento

Para cada checkpoint: ID, objetivo, dependências, artefato, evals, gates, condição de parada e fora de escopo. Por padrão executar somente o checkpoint autorizado, na mesma branch/PR.

## Dependências

Snapshots externos, entregas anteriores e decisões não fechadas. Não depender de paths irmãos fixos nem de main flutuante.

## Superfície arquitetural provável

Scope proposto e propriedade de interfaces/adapters. Caminhos futuros usam planned no manifesto e não em must_read.

## Migrações requeridas

APIs/dados/contratos que precisam ser migrados; evitar compatibilidade que preserve semântica incorreta.

## Artefatos esperados

Código/testes/docs somente quando autorizados; evidências e critérios que permitem review independente.
