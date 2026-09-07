# WORK-LOWER-002 — CP0 review de proveniência

Self-review por Codex, mesma tarefa/contexto do implementador; não é review humano nem independente.
Baseline main limpa/atualizada (`git pull --ff-only`): `e2488a362478057de7d59cdf9ae2b38b1f4040d3`.
Branch própria `chore/pin-air-java-1a`. [Contrato e autorização](../../work/active/WORK-LOWER-002/spec.md).

## Diff e autoridade

O upstream autorizado para os próximos checkpoints é `air-java@b78f4068d8a479f48eb048b8d76fa60a0997dc4a`.
[PR #5 / 1A](https://github.com/Gustavo2358/air-java/pull/5) MERGED em 2026-09-07T21:30:12Z,
head `b8d0d953024dceb37782944886ac02d5ec6d16f6`, merge igual ao pin.
[contracts](https://github.com/Gustavo2358/air-java/actions/runs/34163367232/job/101869534574) e
[harness](https://github.com/Gustavo2358/air-java/actions/runs/34163367187) success no merge.

Revisados source lock, URLs, descrições e todo o pacote de trabalho/evidência. Os 14 paths do lock
existem no objeto Git e constam de [provenance.log](provenance.log) com blob SHA e SHA-256.
O [audit reproduzível](audit-provenance.py) compara blocos normativos, 14 URLs ativas e 177 arquivos históricos;
rejeita pin errado, topologia antiga, path ausente e URL divergente. Segundo GREEN/digest restaurado registrado.
`analysis-ir@122ce54e1b9ef9b00646f93ece409ca8b63bc933` e todos os outros pins permanecem intactos.

```sh
python3 docs/quality/WORK-LOWER-002/audit-provenance.py . /tmp/pin-air-java-1a-upstream e2488a362478057de7d59cdf9ae2b38b1f4040d3
```

`pom.xml` upstream é `air-java-parent`; `air-model/` publica `air-java` (model/validator),
`air-json/` publica o codec compartilhado 1A. POMs e fontes Java do lower não mudaram.
O source lock mantém o contexto histórico do pacote original nos demais campos; a observação nova é somente SRC-AIR-JAVA.

## Finding resolvido no harness

O [full inicial](full-initial-red.log) passou build/testes de produto e falhou em uma fixture de fechamento:
o cenário de histórico sem autoridade herdava a autorização do novo work item ativo.
A correção em `lifecycle_fixture.py`, `test_closure.py` e no challenge equivalente revoga autorizações
somente nas cópias sintéticas desse cenário. A baseline com histórico válido é verificada antes do mutante;
`UNAUTHORIZED_IMPLEMENTATION` continua obrigatório, e o challenge registra restauração/segundo GREEN.
Nenhum checker, oracle esperado ou gate de produção foi enfraquecido. [16 testes de closure](closure-green.log) verdes.
A exceção exata de scope e seu motivo foram registrados no FREEZE antes da correção.

## Validação local

Temurin 21.0.12.1, Maven 3.9.16, Python 3.14. `LOWER_BUILD_ROOT=/tmp/consumer-pinning/lower-build`,
Maven repo isolado, upstream checkout detached no SHA autorizado. Cada comando abaixo exit 0.

| Comando | Prova | Log |
| --- | --- | --- |
| `python3 scripts/harness/run.py bootstrap` | Reactor parent/model/codec; 172 checks de modelo, 57 de transporte; JAR/source-lock provenance | [bootstrap](bootstrap.log) |
| `python3 scripts/harness/run.py full --evidence docs/quality/WORK-LOWER-002/CP0.json` | Docs, semantic 779, performance 95 (+874 assertions), architecture, git/scope, 126 testes do harness, 18 challenges restaurados | [full](full.log) |
| Audit físico acima | Paths/URLs/pins, história, scope e mutantes | [provenance](provenance.log) |

Os logs preservam marcadores do output real e o SHA-256 do output integral local. Full executa `mvn verify`
e todos os componentes atuais; não implementa os gates futuros transport/integration.
A primeira tentativa de bootstrap teve erro de DNS no sandbox; a execução com acesso ao Maven Central passou.

## Manifest histórico

`MANIFEST.sha256` foi criado em `a429abdab3d9f9d57502749e0f34980edc032caf` para o pacote documental original.
Na main usada como baseline, `sha256sum --check --quiet MANIFEST.sha256` já retornava 1:
30 hashes divergentes e 5 paths de propostas removidos; [diagnóstico original](manifest-current.log).
Não é gate da árvore evoluída. O manifest foi preservado byte a byte; a extração por
`git archive a429abdab3d9f9d57502749e0f34980edc032caf` passou `sha256sum --check MANIFEST.sha256`
com os 105 arquivos originais ([prova](manifest-original.log)). Não reescrever evidência histórica para fabricar green atual.

## Fronteira de entrega

Diff integral revisado, inclusive fixture, documentos novos, logs e certificado; nenhum finding bloqueante restante.
Certificação pre_commit e Git/trailer/digest usam o harness existente. PR/CI do head são obrigações pós-push,
com recibo no próprio PR para evitar SHA autorreferente. Parar para review humano, sem merge/auto-merge.
Não iniciado: 2A, 2B, CLI, AIR reader/writer, CFG JSON ou E2E; nenhum upstream alterado.
