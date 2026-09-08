# Oracles CP0

EVAL-LWR-007/015/018, INV-LWR-007/017/021. Todos os oracles anteriores permanecem,
exceto expectativas explícitas de IDs/versionamento/tamanho agora supersedidas.

- Porta pública: todas as identidades derivadas têm exatamente 32 hex; SP→AIR fecha.
  Renomear handles preservando conteúdo muda os localIds de operation/label/origin/entry.
- Vetores libxxhash independentes; framing a/bc versus ab/c, delimitadores/vazio,
  Unicode/surrogates, namespaces, proprietários e original/expanded separados.
- PublicationId muda com a política local e mantém limites31/32 e todos os fatos.
- Golden AIR anterior: bijeção global por IDs tipados, todas as referências/owners
  consistentes e igualdade exata de todo outro campo; AirValidator + AirJson roundtrip.
  Não impor estimativa de tamanho como golden. Registrar tamanho realmente observado.
- 100.000 identidades de operação com handles distintos, mesmo papel/proprietário,
  reuso determinístico e nenhuma perda; segundo GOBACK continua fora do slice e
  inventário original preservado pelo oracle cumulativo. Não são 100.000 instruções baixadas.
- Texto isolado de 1.000.000 code units UTF-16 com Unicode e lone surrogate, incremental,
  exatamente 4.000.000 bytes de conteúdo + framing/domínio; spy verifica buffers≤256,
  contagem/finalização e ausência de cópia expandida no registro. Porta pública separada
  com compilation-unit key e filename de 100.000 caracteres; preservar SourceKeys/nomes e AIR válida.
  Usar maior handle canônico statement:2147483647 e rejeitar handle textual longo inválido.
  Medir tempos observados, sem threshold de hardware nem alegar ganho heap/CPU.
- Colisão artificial injeta tupla distinta sob digest conhecido no registro privado;
  tradução falha LOCAL_ID_COLLISION sem resultado. Mesmo artifact reusado é positivo.
- Falsificar política de publicação, papel/original-expanded, framing, ocorrência e
  guard de colisão; RED esperado, restaurar bytes/digest, segundo GREEN. Gates full
  preservam contracasos anteriores e nunca enfraquecem expected para acomodar código.

Correção antes do GREEN: [premissa inválida e FREEZE anterior](../../../quality/WORK-LOWER-005/oracle-correction.json).
A porta limita kind:n a inteiro canônico; a prova focal do hash independe dessa admissão.
