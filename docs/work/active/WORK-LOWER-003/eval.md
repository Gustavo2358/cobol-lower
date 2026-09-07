# Oracles congelados CP0

A fixture principal é adapters/src/test/resources/sp/cobol-semantic-product.json, original imutável.
A: CLI exit 0 e arquivo bytes == AirJson.encode(Publication do mesmo lowering em memória).
B: AirJson.decode(file).equals(Publication), sem projetar ou retirar fatos.
C: suites independentes anteriores mantêm Entry/Sequence/Return/PARTIAL/claims/origins/uncertainties/correlation.
D: execuções repetidas produzem bytes idênticos.
E: erro de encode ocorre antes de qualquer I/O; falha após escrita parcial de temp limpa temp e preserva destino.
F: SP ausente/malformado/versão errada: exit 3, stderr tipado, sem novo output.
G: todos os status não SUCCESS: exit 4, status em stderr, writer não invocado.
H: destino impossível: exit 6, stderr, temp limpo.
I: argumentos incorretos: exit 2/usage, zero chamadas semânticas.
Codec failure real via limite de AirJson: exit 5/code/path, sem escrita; bugs inesperados propagam.
Fallback AtomicMoveNotSupportedException é testado; não se promete atomicidade nem durabilidade no fallback.

Challenges executáveis (cada baseline GREEN → RED pela causa certa → restauração byte a byte → GREEN):
Jackson AIR, newline, escrever não SUCCESS, exit 0 lowering, exit 0 codec, core→air-json,
suite removida/skipped, I/O antes do encode. Fakes de I/O pertencem só à borda de adapters.
Gates: bootstrap; docs/architecture/semantic/performance/full/git; scope via preflight/candidate;
certify/verify-commit e remote. transport/integration especificados, indisponíveis, não PASS.

Correção de oracle novo antes de GREEN: assinatura KNOWN com parameterCount=1 é
INVALID_INPUT/SIGNATURE pela regra existente EntryGobackAdmission (validação antecede admissão).
A primeira expectativa UNSUPPORTED_SLICE deste teste era incorreta. O contrato solicitado aceita
qualquer status não SUCCESS; a expectativa foi corrigida para o status tipado exato, sem mudar o lower
nem os oracles anteriores. FREEZE renovado; teste ainda exige exit 4 e ausência de output.
