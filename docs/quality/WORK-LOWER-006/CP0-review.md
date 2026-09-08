# CP0 — review do candidato 4C

Self-review pelo mesmo Codex que implementou; não é review independente nem aprovação humana.
A identidade do candidato é exclusivamente `CP0.json#/candidate_diff_sha256`.
Revisão do diff integral: produção, testes/fixtures, scripts e documentação/lifecycle.

- SP 1.2.0 tem DTO próprio estrito; campos novos não escapam pela versão 1.1.0.
  Adapter transporta fatos tipados próprios. PIC/legacy literal/readiness.scope não
  completam provas; joins usam DataId/StatementId/OperandId, nomes só display.
- Admission comum valida integridade e constrói índices. Profile novo exige provas
  positivas e percorre Entry.start/normalContinuation, rejeitando ciclo, dangling,
  ocorrência extra e terminal incompatível. Enums fechados mantêm GOBACK current
  invocation/NONE; profile escalar não altera o caminho CP3.
- DATA canônica por radix e tradução uma vez; Map DATA→Object/Cell é reaproveitado
  pelos handlers. Uma Sequence recebe Assigns e Return terminator. Sem scan global
  no hot path, sem reconstrução por referência; custo incremental documentado.
- Object/Cell usam known(text), lifetime persistente e visibilidade privada pela
  regra LOCAL/WORKING_STORAGE. Origins derivadas referenciam a DATA real; source e
  target preservam ocorrências distintas. Links tipados permitem mesmo Label.
- PublicationId incorpora novos fatos consumidos em domínio versionado; IDs locais
  mantêm 32 lowerhex, papéis separados e collision registry. Não há string canônica
  gigante. SourceKeys do profile escalar evitam expansão da identidade de unit.
- Validator compartilhado precede SUCCESS; codec compartilhado precede criação do
  temporário. Não há outro codec/validator, cópia String ou aumento dos defaults.
  Fixture positiva continua PARTIAL; GOBACK conserva dimensões UNAVAILABLE.
- Oracles relacionais e snapshot SP real não derivam expected do lower. CP3 recebe
  assertion do hash aprovado; contracasos, permutação, dados sem referências, nomes
  iguais, 60k linhas físicas, N/2N e compartilhamento falsificam os riscos centrais.
- 17 desafios escalares exigem compilação e AssertionError, restore exato e segundo
  GREEN; desafios históricos permanecem no gate full. O ajuste em output_challenge
  usa o novo dispatch para manter a mutação compilável. A suíte nova roda depois
  dos oracles históricos, preservando o diagnóstico anterior sem remover assertions.

Tentativas de setup (accessor Java incorreto, hook inicial ausente, rede Maven) não
contam como RED semântico. Desafios que encontraram primeiro outro oracle tiveram
somente seleção/ordem de diagnóstico ajustada; todos continuam exigindo erro real.
Logs das tentativas e dos passes são preservados. Sem findings de implementação
pendentes nesta revisão; limites de transporte, profile e claims ficam explícitos
no handoff. Review humano e CI no commit publicado são etapas distintas.

Logs brutos são armazenados em gzip lossless (mtime=0), com hash do conteúdo
original e prova de descompressão exata; whitespace Maven não é normalizado.
