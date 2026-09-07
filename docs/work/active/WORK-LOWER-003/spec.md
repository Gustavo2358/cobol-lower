# Contrato CP0 — 2A

Autorização nova e explícita do usuário em 2026-09-07, implementation, single-checkpoint CP0.
Baseline limpa/fetch/pull ff-only: 77762ff2705974ecb874bc4824119c031f6d99bc; PR3 merge ancestral confirmado.
Somente 2A: dois paths posicionais, SP reader e FileLowering existentes, SUCCESS com Publication,
AirJson.encode inteiro antes de qualquer publicação física; temp no diretório destino, move atômico
com fallback explícito para replace não atômico. Bytes exatos, sem newline ou alteração AIR.
Core e semântica são imutáveis. AirJson exclusivamente em adapters; Jackson só na entrada SP.

Exit codes: 0 sucesso; 2 uso/path inválido; 3 falha física/JSON SP; 4 lowering não SUCCESS;
5 AirJsonException (code/path preservados); 6 publicação física. Erros esperados em stderr sem stack trace.
Bugs inesperados propagam. Arquivo anterior pode permanecer em falha: caller deve confiar no exit code.
Defaults: SP 100000 bytes, profundidade 64, 50000 nós; admission 100000 entidades/100 diagnostics;
identidade 1000000 caracteres; ValidationOptions.defaults; AirJson defaults. Mesmos valores já usados
nos testes do slice, apenas limites operacionais, sem nova política nem identidade semântica.

O roadmap pai foi lido como sequência; este pedido restringe a CLI a dois argumentos e não autoriza
report JSON, 2B, CFG, orquestrador nem E2E completo. Pins não mudam. Se a Publication legítima da
fixture real não couber no codec 1A, capturar erro tipado, registrar blocker e parar para decisão.

Lifecycle: PR3 merged com reviews=[] não pode virar aprovação fictícia. Ajuste mínimo do harness
para observação not_recorded e links históricos verificáveis pelo Git, sem reescrever certificados.
