# Self-review — WORK-LOWER-003 CP0 / 2A

Revisor: Codex, mesmo implementador, segunda passagem pelo diff integral staged, arquivos novos,
contratos e classes negativas. Não é review independente nem aprovação humana.
Base: `77762ff2705974ecb874bc4824119c031f6d99bc`; identidade do candidato no digest único do [certificado](CP0.json).

## Resultado

Writer/CLI compõem os adapters e o lower existentes. `AirFileOutput` codifica a Publication inteira
antes do primeiro efeito físico, escreve bytes exatos no temp e publica por move. Fallback explícito
não promete atomicidade inexistente. Não há serializer/DTO AIR local, nova porta no core nem política
semântica. Main/run e códigos 0/2/3/4/5/6 são exercitados; erros esperados preservam diagnóstico tipado.
A fixture real SP continua com SHA256 `7ebce874bb98262598b908b176290368f738a21561c35a6ac342fc72e66d04ed`.
CLI real gera 398532 bytes; SHA256 AIR `79270eabe0d2d836594c6d58fc3b2abb462c6d736d698477b680a656153bab44`,
igual ao encode direto e decode igual à Publication do lower.

Core, decoder/materialization, testes semânticos anteriores, fixture original e evidência histórica
não foram alterados. Pin air-java `b78f4068d8a479f48eb048b8d76fa60a0997dc4a`; norma
`122ce54e1b9ef9b00646f93ece409ca8b63bc933`. A única mudança no source lock é o papel runtime do codec
em adapters e suas coordenadas Maven. Os dois jars têm proveniência/digest verificados pelo bootstrap.
G-ARCH cobre source/API/bytecode/jdeps/Maven do core, JSON restrito ao SP e ownership/shading do produto.
A suite de saída é obrigatória em Maven/semantic/full/CI; removê-la ou skippá-la falha mesmo com Maven 0.

## Findings resolvidos

- PR3 foi mergeada sem reviews formais na API. O harness antigo não representava esse fato sem
  fabricar review. `not_recorded` exige observação explícita, null nos campos de review e reviews=[];
  reconcile reconsulta head/merge/reviews. Isso nunca concede autorização. O manifesto histórico
  congelado antes de PR3 preserva PR null; certificado e registry concordam no PR3 real.
- O review imutável de WORK-LOWER-002 aponta para seu pacote ativo agora arquivado. O checker resolve
  somente links de evidence byte-idêntica no head histórico e checa o target/anchor nessa revisão.
  Links correntes continuam estritos; contracasos protegem mudança de evidence, anchor e README.
- Novo teste esperava UNSUPPORTED_SLICE ao mudar signature KNOWN para count=1. A regra existente
  rejeita esse contrato contraditório antes da admissão com INVALID_INPUT/SIGNATURE. Corrigido o
  oracle novo e renovado FREEZE; nenhum expected anterior nem código do lower foi alterado.
- Execução full inicial em sandbox sem rede parou no gate Git; registrada como falha de ambiente.
  A execução completa usa a mesma suite com acesso remoto e não reutiliza esse FAIL como PASS.

## Limites e handoff

Cobertura 1A/DRAFT, shape mínimo, limites operacionais documentados. Sem reader analysis-cfg,
CFG JSON, report JSON, 2B, E2E completo, novo serializer ou mudança upstream. Fallback de move sem
atomicidade garantida; cleanup também pode falhar no filesystem e preserva exception suprimida.
Nenhum finding bloqueante conhecido; review humano permanece obrigatório após PR e CI no head exato.
Somente cobol-lower foi editado por esta tarefa; havia alterações alheias no checkout analysis-cfg
na inspeção final, preservadas sem edição. Roadmap pai não foi editado.
