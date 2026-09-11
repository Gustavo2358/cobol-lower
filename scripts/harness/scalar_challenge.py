"""Compiled 4C semantic falsifications in an isolated copy, exact restore and second GREEN."""
import hashlib
import json
import os
from pathlib import Path
import shutil
import subprocess
import tempfile

ROOT = Path(__file__).resolve().parents[2]
APP = 'core/src/main/java/io/github/gustavo2358/lower/application/'
DOMAIN = 'core/src/main/java/io/github/gustavo2358/lower/domain/SpInput.java'
# Each case lists exact, reviewable replacements. Compiler/setup failures are never accepted.
CASES = [
 ('program-point-successor', [(APP+'ScalarMoveAdmission.java',
  'current = move.normalContinuation().statement().orElseThrow();',
  'current = input.statements().stream().filter(s -> s.header().programPoint() > move.header().programPoint()).min(java.util.Comparator.comparingInt(s -> s.header().programPoint())).map(s -> s.header().id()).orElse(move.header().id());')], 'SCALAR reject unavailable dangling cyclic continuation'),
 ('ignore-full-identity', [(APP+'ScalarMoveAdmission.java','m.copySemantics() == CopySemantics.FULL_IDENTITY','true')], 'SCALAR reject FULL_IDENTITY absent'),
 ('invent-whole-access', [(DOMAIN, 'Objects.requireNonNull(wholeItemAccess); Objects.requireNonNull(provenance);',
  'Objects.requireNonNull(wholeItemAccess); wholeItemAccess = wholeItemAccess.or(() -> binding.selected().map(WholeItemAccess::new)); Objects.requireNonNull(provenance);')], 'SCALAR reject wholeItemAccess absent'),
 ('ignore-selected-mismatch', [(APP+'ScalarMoveAdmission.java','b.selected().equals(Optional.of(data.id()))','true')], 'SCALAR reject selected wholeItem mismatch'),
 ('object-per-reference', [(APP+'ScalarMoveLowerer.java','ScalarDataTranslator.translate(plan.data(),',
  'ScalarDataTranslator.translate(plan.moves().stream().map(m -> plan.data().stream().filter(d -> d.id().equals(m.target().binding().selected().orElseThrow())).findFirst().orElseThrow()).toList(),')], 'SCALAR one Object per declaration'),
 ('cell-per-move', [(APP+'ScalarMoveLowerer.java','List.of(body), data.storage(),',
  'List.of(body), java.util.Collections.nCopies(plan.moves().size(), data.storage().getFirst()),')], 'SCALAR precise input must lower'),
 ('name-join', [(APP+'ScalarSequenceAssembler.java', 'var target = data.index().get(move.target().wholeItemAccess().orElseThrow().data());',
  'var target = data.index().values().stream().filter(link -> data.objects().stream().anyMatch(o -> o.id().equals(link.object()) && o.displayName().equals(data.objects().getFirst().displayName()))).findFirst().orElseThrow();')], 'SCALAR destination by DataId, never name'),
 ('write-as-read', [(APP+'MoveHandler.java','Operand.Role.VALUE_WRITE','Operand.Role.VALUE_READ')], 'SCALAR precise input must lower: OUTPUT_INVALID'),
 ('literal-as-unknown', [(APP+'MoveHandler.java',
  'new Expressions.Literal(new Operand.Header(source, Operand.Role.VALUE_READ, sourceOrigin),\n            new Values.TextValue(move.textAdjustment().map(a -> a.result().value()).orElseGet(() -> move.source().logicalValue().orElseThrow().value())))',
  'new Expressions.Unknown(new Operand.Header(source, Operand.Role.VALUE_READ, sourceOrigin), Types.known(Types.Builtin.TEXT), List.of(), Scopes.NoMemory.INSTANCE, new UncertaintyId(unit.publication(), ids.id("uncertainty", "scalar-entry-inventory", unit.localId(), "0")))')], 'SCALAR literal exact logical text, never UNKNOWN'),
 ('drop-data-link', [(APP+'ScalarMoveLowerer.java','List.copyOf(data.index().values()), operands','List.of(), operands')], 'SCALAR DATA correlation complete'),
 ('lose-literal-provenance', [(APP+'MoveHandler.java','move.source().id().handle(), move.source().provenance()','move.source().id().handle(), move.header().provenance()')], 'SCALAR literal provenance preserved'),
 ('lose-target-provenance', [(APP+'MoveHandler.java','move.target().id().handle(), move.target().provenance()','move.target().id().handle(), move.header().provenance()')], 'SCALAR target provenance preserved'),
 ('goback-halt', [(APP+'GobackHandler.java','static Operations.Return translate','static Terminator translate'),
  (APP+'GobackHandler.java','new Operations.Return(header, List.of())','new Operations.Halt(header, Operations.HaltKind.NORMAL)')], 'SCALAR GOBACK Return never Halt'),
 ('two-sequences', [(APP+'ScalarMoveLowerer.java','List.of(entry), List.of(sequence),',
  'List.of(entry), List.of(sequence, new Sequence(new LabelId(unit, "extra-label"), List.of(), new Operations.Return(new Operations.Header(new OperationId(unit, "extra-return"), sequence.terminator().header().origin(), sequence.terminator().header().coverage(), sequence.terminator().header().precision(), sequence.terminator().header().uncertainties()), List.of()), sequence.origin())),')], 'SCALAR one Sequence for linear chain'),
 ('revision-missing-value', [(APP+'CanonicalRevision.java','e.word(value.value());','e.word("ignored");')], 'SCALAR new logical value in PublicationId'),
 ('expanded-local-id', [(APP+'LocalIds.java','return digest;', 'return CanonicalRevision.token(namespace + "/" + role + "/" + owner + "/" + key);')], 'SCALAR compact ObjectId'),
 ('scalar-index-scan', [(APP+'EntryGobackAdmission.java', 'StatementFact lookup(StatementId id) { references++; return statements.get(id); }',
  'StatementFact lookup(StatementId id) { for (var item : statements.entrySet()) { references++; if (item.getKey().equals(id)) return item.getValue(); } return null; }')], 'SCALAR scalar reference ledger 7N+3 no scans'),
]

def digest(data): return hashlib.sha256(data).hexdigest()

def main():
    logs = Path(os.environ['LOWER_BUILD_ROOT']) / 'scalar-challenge-logs'
    logs.mkdir(parents=True, exist_ok=True)
    with tempfile.TemporaryDirectory(prefix='lower-scalar-challenge-') as folder:
        root = Path(folder)
        for name in ('core', 'adapters'):
            shutil.copytree(ROOT/name, root/name, ignore=shutil.ignore_patterns('target', '__pycache__'))
        shutil.copy2(ROOT/'pom.xml', root/'pom.xml')
        command = ['mvn', '-B', '-ntp', '-Dmaven.repo.local='+str(Path(os.environ['LOWER_BUILD_ROOT'])/'m2'), '-pl', 'core', 'test-compile', 'exec:java', '-Dexec.mainClass=io.github.gustavo2358.lower.testing.ScalarSuite', '-Dexec.classpathScope=test']
        def execute(name):
            result = subprocess.run(command, cwd=root, stdout=subprocess.PIPE, stderr=subprocess.STDOUT)
            (logs/(name+'.log')).write_bytes(result.stdout)
            return result.returncode, result.stdout.decode()
        code, green = execute('baseline')
        if code or 'LOWER_SCALAR_TESTS=' not in green: raise RuntimeError('baseline not GREEN\n'+green)
        records = []
        for ident, edits, oracle in CASES:
            paths = {path: (root/path).read_bytes() for path, _, _ in edits}
            before = digest(b''.join(paths[path] for path in sorted(paths)))
            try:
                for path, token, replacement in edits:
                    file = root/path; text = file.read_text()
                    if text.count(token) != 1: raise RuntimeError('mutation target missing/ambiguous '+ident)
                    file.write_text(text.replace(token, replacement))
                red_code, red = execute(ident+'-red')
            finally:
                for path, raw in paths.items(): (root/path).write_bytes(raw)
            restored = digest(b''.join((root/path).read_bytes() for path in sorted(paths)))
            second_code, second = execute(ident+'-restored')
            causes = [line.strip() for line in red.splitlines() if 'AssertionError' in line and oracle in line]
            if not red_code or not causes or 'COMPILATION ERROR' in red or second_code or before != restored:
                raise RuntimeError('invalid semantic challenge '+ident+'\n'+red+'\nSECOND\n'+second[-2000:])
            if any((ROOT/path).read_bytes() != raw for path, raw in paths.items()): raise RuntimeError('source checkout changed during challenge')
            record = dict(id=ident, mutation=edits, expected_oracle=oracle, baseline_exit_code=0, observed_exit_code=red_code,
                failure_reason=causes[0], baseline_digest=before, restored_digest=restored, restoration_status='RESTORED',
                green_command='mvn -pl core test-compile exec:java -Dexec.mainClass=io.github.gustavo2358.lower.testing.ScalarSuite -Dexec.classpathScope=test (isolated; pinned m2)',green_exit_code=second_code,
                red_log_sha256=digest(red.encode()), second_green_log_sha256=digest(second.encode()))
            records.append(record); print(json.dumps(record), flush=True)
        (logs/'receipt.json').write_text(json.dumps(records,indent=2)+'\n')
        print('PASS SCALAR CHALLENGE: '+str(len(records))+' compiled semantic RED, exact restore and second GREEN',flush=True)
if __name__ == '__main__': main()
