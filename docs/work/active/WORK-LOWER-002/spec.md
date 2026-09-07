# WORK-LOWER-002 — Spec

## Problema

O pin ativo anterior e os paths anteriores à modularização não representam o upstream autorizado para os próximos checkpoints.

## Objetivo

Adotar exclusivamente `air-java@b78f4068d8a479f48eb048b8d76fa60a0997dc4a` (merge PR #5 / 1A). Revalidar paths e URLs nesse objeto Git; manter `analysis-ir@122ce54e1b9ef9b00646f93ece409ca8b63bc933` como autoridade normativa separada.

## Autoridade e baseline

Pedido explícito do usuário em 2026-09-07: preparar somente proveniência/pinning para 2A/2B, com commit, push, CI e PR próprios; parar para review humano, sem merge.
Main limpa após `git pull --ff-only`: `e2488a362478057de7d59cdf9ae2b38b1f4040d3`. Branch `chore/pin-air-java-1a`. CP0 deste item é apenas proveniência, não os checkpoints de implementação 2A/2B.

## Contrato de sucesso

O parent `air-java-parent` agrega `air-model` (artefato `air-java`, model/validator) e `air-json` (codec compartilhado, cobertura 1A). Confirmar POMs, `AirJson.java`, todos os paths pinados por `git cat-file -e SHA:path` e URLs `blob/SHA/path`. Comparar o bloco normativo com a baseline e o lock upstream. Referências antigas corretas em evidências históricas permanecem byte a byte.

## Fora de escopo

Nenhum POM, Java, teste de produto, modelo, algoritmo ou capability novo. Não implementar 2A, 2B, CLI, AIR JSON reader/writer, CFG JSON ou E2E; não adicionar dependência Maven de air-json. Não alterar air-java, analysis-ir ou proleap-poc.

## Ajuste mínimo de fixture do harness

O full inicial aceitou build/testes de produto, mas o contracaso de histórico sem autoridade herdou a autorização deste novo item ativo. A correção fica em `scripts/harness/tests/lifecycle_fixture.py`, `scripts/harness/tests/test_closure.py` e no cenário equivalente de `scripts/harness/challenge.py`: revogar autorizações ativas somente na cópia sintética antes de testar histórico arbitrário. Preservar o oracle `UNAUTHORIZED_IMPLEMENTATION` e todos os gates de produção. Esta dependência do protocolo de novo trabalho não implementa 2A/2B.
