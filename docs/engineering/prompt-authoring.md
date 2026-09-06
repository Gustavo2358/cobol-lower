# Prompts operacionais pequenos

**Base:** SRC-PROMPTS e direção do usuário. O harness contém contexto durável; o prompt contém a autorização e o delta humano da tarefa.

## Estrutura

Um prompt útil informa: work item/checkpoint, resultado observável, restrições não dedutíveis do harness, evidência de conclusão e parada. Roteia AGENTS/manifesto; não cola toda a arquitetura. Prefira objetivo a receita algorítmica antes do discovery. Não solicitar raciocínio interno passo a passo.

A higiene Git deve aparecer por referência explícita ao [protocolo Git](git-and-review.md), com branch/PR do trabalho quando conhecidos. O prompt inicial autoriza criar branch/PR; os seguintes continuam os mesmos. Uma remediação não autoriza o próximo checkpoint.

## Modelo de autorização de checkpoint

```text
Leia AGENTS.md e o manifesto/estado de WORK-LOWER-001.
Autorize/execute somente CP<n>, buscando o objetivo e os critérios definidos no plano.
Preserve contratos, escopo e a higiene Git do harness; continue na branch/PR do trabalho.
O único requisito novo é: <delta humano, ou nenhum>.
Faça os gates e a revisão/falsificação previstas, registre evidência, commit/push e pare para review.
```

O template é ilustrativo: substituir identificadores antes de executar. Se a promoção de proposta para active for necessária, explicitá-la. Não usar “faça tudo” sem lista finita e critérios.

## Prompts multi-checkpoint

O prompt seleciona trabalho, autorização e modo; a [transação](agent-session-protocol.md) fornece o algoritmo. Sem modo e lista explícitos, manter o padrão; uma lista no plano ou “faça tudo” não autoriza avanço automático.

Modelo ilustrativo para uma sessão futura, após review/merge da preparação do harness (não é autorização atual):

```text
Leia AGENTS.md e WORK-LOWER-001. Autorizo promover a proposta e executar
CP0..CP5 em modo multi-checkpoint, conforme o protocolo de sessão.
Siga a higiene Git; use uma única branch e o mesmo PR para todo o work item.
Certifique, faça commit/push de cada checkpoint e avance sem esperar review
humano intermediário. Pare em blocker material e ao final para review humano.
Sem merge/auto-merge. Nenhuma capability fora do plano está autorizada.
```

Persistir modo, lista expandida e origem da autorização no manifesto/state ao recebê-la. Não copiar o exemplo como autoridade. Alterações de contrato e demais stop conditions suspendem avanço mesmo sob pedido de execução contínua.

## Anti-padrões

Não repetir AGENTS, prescrever interfaces futuras vazias, confundir discovery com autorização de refactor, anexar toda a história ou exigir paper para uma correção ortográfica. Não existe meta mágica de tokens; use o menor texto que preserva objetivo, limites e prova. A documentação pode ser rica porque seu carregamento é seletivo.
