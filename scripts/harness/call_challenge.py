#!/usr/bin/env python3
"""Compilable CP6 mutations, independent oracle, exact restoration and second GREEN."""
import hashlib
import json
import os
from pathlib import Path
import shutil
import subprocess
import tempfile

ROOT = Path(__file__).resolve().parents[2]
APP = 'core/src/main/java/io/github/gustavo2358/lower/application/'
CASES = [
    ('dynamic-to-literal', 'InvokeHandler.java',
     'new Interactions.ComputedTarget("program", "cobol.program", read, namePolicy, targetOrigin)',
     'new Interactions.LiteralTarget("program", "cobol.program", "PROGA", namePolicy, targetOrigin)', 'dynamic target remains computed'),
    ('wrong-object', 'InvokeHandler.java',
     'var object = data.get(reference.wholeItemAccess().orElseThrow().data()).object();',
     'var object = data.values().stream().reduce((a, b) -> b).orElseThrow().object();', 'target ObjectId follows selected DATA'),
    ('erase-x8-padding', 'MoveHandler.java',
     'move.textAdjustment().map(a -> a.result().value()).orElseGet(() -> move.source().logicalValue().orElseThrow().value())',
     'move.source().logicalValue().orElseThrow().value()', 'raw fitted/identity assigned text'),
    ('invent-x5-padding', 'MoveHandler.java',
     'move.textAdjustment().map(a -> a.result().value()).orElseGet(() -> move.source().logicalValue().orElseThrow().value())',
     'move.textAdjustment().map(a -> a.result().value()).orElseGet(() -> move.source().logicalValue().orElseThrow().value() + "   ")', 'raw fitted/identity assigned text'),
    ('wrong-normal-label', 'CallSequenceAssembler.java',
     'data.index(), successor, continuation, unit,', 'data.index(), label, continuation, unit,', 'explicit normal destination'),
    ('pure-effects', 'InvokeHandler.java',
     'new Interactions.ForeignEffects(memory, memory, List.of())',
     'new Interactions.ForeignEffects(Scopes.NoMemory.INSTANCE, Scopes.NoMemory.INSTANCE, List.of())', 'conservative memory effects'),
    ('closed-outcomes', 'InvokeHandler.java',
     'new Scopes.WithinControl(new Scopes.AllControl(unit.publication()))',
     'Scopes.NoControl.INSTANCE', 'open all-control remainder'),
    ('invented-contract', 'InvokeHandler.java', 'new Interactions.UnknownContract(contract)',
     'new Interactions.KnownContract(new Interactions.ContractRef("invented", "1", List.of(origin)))', 'unknown contract'),
    ('name-canonicalization', 'InvokeHandler.java',
     'literal.logicalValue().orElseThrow().value(), namePolicy',
     'literal.logicalValue().orElseThrow().value().strip(), namePolicy', 'literal raw logical text and namespace'),
    ('exact-name-policy', 'InvokeHandler.java', 'new Interactions.UnknownName(name)',
     'Interactions.ExactName.INSTANCE', 'unknown runtime name policy'),
    ('admit-using', 'CallAdmission.java', 's.using() == ClausePresence.ABSENT && s.returning()',
     'true && s.returning()', 'USING: SUCCESS'),
    ('admit-ambiguous', 'CallAdmission.java',
     'if (call.target() instanceof DataCallTarget d) admitReference(d.reference(), OperandRole.CALL_TARGET, h, c);',
     'if (call.target() instanceof DataCallTarget d) { /* missing admission */ }', 'ambiguous target'),
    ('ignore-call-revision', 'CanonicalRevision.java',
     'e.word("LITERAL"); e.word(l.text()); e.word(l.writtenText());',
     'e.word("LITERAL"); e.word("ignored"); e.word("ignored");', 'revision includes literal CALL target'),
]


def digest(raw):
    return hashlib.sha256(raw).hexdigest()


def main():
    build = Path(os.environ['LOWER_BUILD_ROOT'])
    logs = build/'call-challenge-logs'; logs.mkdir(parents=True, exist_ok=True)
    jars = [build/'m2/io/github/gustavo2358/air-java/0.1.0-SNAPSHOT/air-java-0.1.0-SNAPSHOT.jar',
            build/'m2/com/dynatrace/hash4j/hash4j/0.30.0/hash4j-0.30.0.jar']
    dependency = ':'.join(str(p) for p in jars)
    report = []
    with tempfile.TemporaryDirectory(prefix='lower-call-challenge-') as temp:
        root = Path(temp)
        shutil.copytree(ROOT/'core/src', root/'core/src')
        classes = root/'classes'; classes.mkdir()
        source_paths = sorted(root.glob('core/src/**/*.java'))
        before = {str(p.relative_to(root)): digest(p.read_bytes()) for p in source_paths}
        def execute(name, expected=None):
            compile_result = subprocess.run(['javac', '--release', '21', '-cp', dependency, '-d', str(classes), *map(str, source_paths)], stdout=subprocess.PIPE, stderr=subprocess.STDOUT)
            (logs/(name+'-compile.log')).write_bytes(compile_result.stdout)
            if compile_result.returncode: raise RuntimeError(name + ': compilation is not semantic detection')
            result = subprocess.run(['java', '-ea', '-cp', str(classes)+':'+dependency, 'io.github.gustavo2358.lower.testing.CallSuite'], stdout=subprocess.PIPE, stderr=subprocess.STDOUT)
            (logs/(name+'.log')).write_bytes(result.stdout)
            if expected is None:
                if result.returncode: raise RuntimeError(name + ': GREEN failed: '+result.stdout.decode()[:2000])
            elif result.returncode == 0 or 'java.lang.AssertionError: CP6 '+expected not in result.stdout.decode():
                raise RuntimeError(name + ': wrong semantic detection: '+result.stdout.decode()[:2000])
            return result
        execute('first-green')
        for ident, filename, old, new, oracle in CASES:
            path = root/APP/filename; raw = path.read_bytes(); original = raw.decode();
            if original.count(old) != 1: raise RuntimeError(ident+': mutation site is not unique')
            # To falsify literal target revision, remove all three independently hashed literal facts.
            changed = original.replace(old, new)
            if ident == 'dynamic-to-literal':
                # Remove the now-absent operands' correlations as part of this mutation,
                # so a structurally valid literal reaches the independent target oracle.
                changed = '\n'.join(line for line in changed.split('\n') if not (
                    'links.add(new LoweringResult.OperandLink(reference.id(),' in line
                    or 'items.add(ScalarEvidence.item(unit.publication(), "operand", reference.id()' in line))
            if ident == 'ignore-call-revision': changed = changed.replace('e.word(v.value());', 'e.word("ignored");')
            try:
                path.write_text(changed)
                red = execute(ident, oracle)
            finally:
                path.write_bytes(raw)
            if path.read_bytes() != raw: raise RuntimeError('restoration mismatch')
            green = execute(ident+'-restored')
            report.append(dict(id=ident, mutation=[filename,old,new], expected_oracle=oracle,
                baseline_exit_code=0, observed_exit_code=red.returncode, failure_reason='java.lang.AssertionError: CP6 '+oracle,
                baseline_digest=digest(raw), restored_digest=digest(path.read_bytes()), restoration_status='RESTORED',
                green_command='javac --release 21 then CallSuite in isolated copy with exact pinned m2', green_exit_code=green.returncode,
                red_log_sha256=digest(red.stdout), second_green_log_sha256=digest(green.stdout)))
            (logs/'receipt.json').write_text(json.dumps(report,indent=2)+'\n')
            print(ident+': semantic RED, byte-exact restoration, second GREEN', flush=True)
        if before != {str(p.relative_to(root)): digest(p.read_bytes()) for p in source_paths}: raise RuntimeError('whole-source restore mismatch')
        execute('second-green')
    print('PASS CALL CHALLENGE: '+str(len(report))+' compiled mutations', flush=True)


if __name__ == '__main__':
    main()
