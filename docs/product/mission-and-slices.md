# Missão e vertical slices

## Objetivo

Gerar uma publicação AIR utilizável por consumidores independentes a partir do conhecimento que o frontend efetivamente publicou. A utilidade final é sustentar CFG, effects/storage, reaching definitions, possible values e fatos de dependência, mas esses produtos não são responsabilidade deste repositório.

## Estratégia

Construir uma cadeia pequena fim a fim antes de enriquecer horizontalmente toda a linguagem. Prazo de produto discutido: **15/09/2026**. Essa é uma meta de priorização, não promessa de dataflow completo nem permissão para enfraquecer corretude. Ao cortar escopo, reduza capacidades declaradas e registre o que ficou de fora.

| Marco | Evidência observável | O que não promete |
| --- | --- | --- |
| Harness | Regras, fontes, lifecycle, backlog, evals e gates especificados | Software ou CI já implementados |
| FIRST-LOWER | JSON SP real 1.1.0 → AIR válida com Return | Outras famílias, AIR JSON ou CFG pronto |
| FIRST-PIPELINE | Mesma Publication aceita pelo CFG e saída da invocação representada | Perfil integral da linguagem |
| FILE-PIPELINE | Writer/reader externos interoperáveis no binding fixado | JSON como domínio ou substituição da AIR normativa |
| Enriquecimentos | Uma capability por slice com provas positivas/negativas | Promoção automática pelo nome MOVE/IF/CALL |

O primeiro perfil está em [primeiro slice](../domain/first-slice-entry-goback.md). A expansão está em [matriz bilateral](../domain/capability-matrix.md) e [backlog](../work/backlog.md).

## Escopo versus cardinalidade

Uma fixture com um statement não autoriza `findFirst()` como implementação geral. `minimal-entry-goback@1` inicialmente admite uma publicação selecionada explicitamente estreita. A rejeição de outro shape deve informar todas as ocorrências relevantes que impedem o perfil, sem publicar uma AIR amputada.

As coleções e identidades já devem permitir crescer. Suporte futuro a uma família deve abranger suas ocorrências no escopo declarado. Se houver limite operacional, ele produz diagnóstico de limite; não truncamento silencioso.

## Critério de progresso

Contar testes/linhas não substitui comportamento. Cada checkpoint deve responder: qual input antes não era aceito corretamente, qual observação agora é provada, qual atalho plausível o oracle elimina e qual fronteira permaneceu estável?
