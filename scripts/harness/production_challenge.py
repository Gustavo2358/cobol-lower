"""Production regressions; historical CP4C evidence remains immutable."""
import hashlib
import json
import os
from pathlib import Path
import shutil
import subprocess
import sys
import tempfile

ROOT = Path(__file__).resolve().parents[2]
SP = 'adapters/src/main/java/io/github/gustavo2358/lower/adapters/sp/SpJsonDecoder.java'
FILE = 'adapters/src/main/java/io/github/gustavo2358/lower/adapters/sp/SpFileInput.java'
ADMISSION = 'core/src/main/java/io/github/gustavo2358/lower/application/EntryGobackAdmission.java'
BYTE_GATE = (FILE, 'byte[] bytes = input.readAllBytes();', 'byte[] bytes = input.readAllBytes(); if (bytes.length > 100_000) return new SpJsonDecoder.Rejected(new SpJsonDecoder.Diagnostic(SpJsonDecoder.Code.IMPLEMENTATION_LIMIT, \"physical\", \"$ file byte limit\"));')
AIR = 'adapters/src/main/java/io/github/gustavo2358/lower/adapters/air/AirFileOutput.java'
SUITE = 'adapters/src/test/java/io/github/gustavo2358/lower/adapters/air/ProductionPathSuite.java'
OUTPUT_SUITE = 'adapters/src/test/java/io/github/gustavo2358/lower/adapters/air/AirOutputSuite.java'
NEW_CODEC = 'adapters/src/main/java/io/github/gustavo2358/lower/adapters/sp/StreamingSpReader.java'
CASES = [
 ('literal-value-coherence', [(SP, 'if (!source.value().equals(logical.value()))', 'if (false && !source.value().equals(logical.value()))')], 'coherence', 'literal normalized value mismatch'),
 ('literal-codepoints', [(SP, 'logical.logicalExtent() != logical.value().codePointCount(0, logical.value().length())', 'false')], 'coherence', 'literal code point extent mismatch'),
 ('old-production-bytes', [BYTE_GATE], 'success', 'large SP success exit0'),
 ('old-production-nodes', [(SP, 'meter.nodes++;', 'if (++meter.nodes > 50_000) return reject(Code.IMPLEMENTATION_LIMIT, \"$\");')], 'success', 'large SP success exit0'),
 ('old-production-entities', [(ADMISSION, 'entities++;', 'if (++entities > 100_000) throw new LimitReached();')], 'large', '10k lowering passes production admission'),
 ('remove-production-probe', [(OUTPUT_SUITE, 'int production = ProductionPathSuite.run();', 'int production = 1;')], 'semantic', 'production path probe absent/zero'),
 ('input-failure-as-expected', [BYTE_GATE,
   (SUITE, 'check(code == CobolLower.SUCCESS,', 'check(code == CobolLower.INPUT,')], 'success', 'successful production output exists'),
 ('raise-air-limit', [(AIR, 'this(new AirJson(), new FileOperations());', 'this(new AirJson(new AirJson.Limits(512 * 1024 * 1024, 128), io.github.gustavo2358.air.validation.ValidationOptions.defaults()), new FileOperations());')], 'scope', 'PRODUCTION_SCOPE immutable transport/core'),
 ('new-streaming-codec', [(NEW_CODEC, None, 'package io.github.gustavo2358.lower.adapters.sp; final class StreamingSpReader {}\n')], 'scope', 'PRODUCTION_SCOPE unexpected source'),
]

def sha(raw): return hashlib.sha256(raw).hexdigest()

def scope_errors(root):
    """Frozen production scope of the currently authorized work; historical guards are unchanged."""
    root = Path(root)
    registry = root/'docs/work/registry.json'
    if not registry.exists(): return []
    active = {w['id'] for w in json.loads(registry.read_text())['active']}
    work = next((w for w in ('WORK-LOWER-010', 'WORK-LOWER-007', 'WORK-LOWER-006') if w in active), None)
    if work is None: return []
    guard = json.loads((root/'docs/quality'/work/'production-source-guard.json').read_text())
    expected = guard['source_hashes']; errors = []
    for path, digest in expected.items():
        if not (root/path).is_file() or sha((root/path).read_bytes()) != digest:
            errors.append('PRODUCTION_SCOPE immutable transport/core ' + path)
    allowed = set(expected) | set(guard['allowed_changes'])
    for module in ('core', 'adapters'):
        for path in (root/module/'src/main/java').rglob('*.java'):
            if str(path.relative_to(root)) not in allowed: errors.append('PRODUCTION_SCOPE unexpected source ' + str(path.relative_to(root)))
    return errors

def main(cases=CASES, log_name="production"):
    logs = Path(os.environ['LOWER_BUILD_ROOT'])/(log_name + '-challenge-logs'); logs.mkdir(exist_ok=True)
    with tempfile.TemporaryDirectory(prefix='lower-production-challenge-') as directory:
        root = Path(directory)
        for folder in ('core','adapters','docs','scripts','.github'):
            shutil.copytree(ROOT/folder,root/folder,ignore=shutil.ignore_patterns('target','__pycache__'))
        for name in ('pom.xml','AGENTS.md','ARCHITECTURE.md','README.md','.gitignore'): shutil.copy2(ROOT/name,root/name)
        maven = ['mvn','-B','-ntp','-Dmaven.repo.local='+str(Path(os.environ['LOWER_BUILD_ROOT'])/'m2')]
        # Focal adapter invocations do not have a reactor. Produce their current
        # core and test-jar dependencies explicitly, even with a clean CI cache.
        prepared = subprocess.run(maven+['-pl','core','-am','install'],cwd=root,
            stdout=subprocess.PIPE,stderr=subprocess.STDOUT)
        (logs/'prepare-core.log').write_bytes(prepared.stdout)
        if prepared.returncode: raise RuntimeError('production challenge core preparation failed\n'+prepared.stdout.decode())
        print('PRODUCTION_CHALLENGE_PREPARED_CORE=current isolated source and tests',flush=True)
        core_command = maven + ['-pl','core','-am','install','-Dexec.skip=true']
        maven += ['-pl','adapters','test-compile']
        def execute(label, mode):
            if mode == 'semantic': command = [sys.executable,'scripts/harness/run.py','semantic']
            elif mode == 'scope': command = maven
            else:
                clazz = ('io.github.gustavo2358.lower.adapters.testing.ScalarWireSuite' if mode == 'coherence' else
                    'io.github.gustavo2358.lower.adapters.air.CapacitySuite' if mode == 'capacity' else
                    'io.github.gustavo2358.lower.adapters.air.ProductionPathSuite')
                command = maven+['exec:java','-Dexec.mainClass='+clazz,'-Dexec.classpathScope=test','-Dexec.args='+mode]
            # Capacity challenges can mutate inner admission. Rebuild the current
            # isolated core before adapter execution; a stale test-jar is not an oracle.
            # Compile/package only here so the declared focal adapter assertion,
            # not another core suite, observes the mutation. Full runs core tests.
            prefix = b''
            if mode in ('capacity', 'large'):
                core = subprocess.run(core_command,cwd=root,stdout=subprocess.PIPE,stderr=subprocess.STDOUT)
                prefix = core.stdout
                if core.returncode:
                    (logs/(label+'.log')).write_bytes(prefix)
                    return core.returncode, prefix.decode()
            p = subprocess.run(command,cwd=root,stdout=subprocess.PIPE,stderr=subprocess.STDOUT)
            out = prefix + p.stdout; code = p.returncode
            if mode == 'scope' and code == 0:
                errors = scope_errors(root); out += ('\n'.join(errors)+'\n').encode(); code = int(bool(errors))
            (logs/(label+'.log')).write_bytes(out)
            return code,out.decode()
        records=[]
        for ident, edits, mode, oracle in cases:
            baseline_code,baseline = execute(ident+'-baseline',mode)
            if baseline_code: raise RuntimeError('production challenge baseline failed '+ident+'\n'+baseline)
            paths={p:(root/p).read_bytes() if (root/p).exists() else None for p,_,_ in edits}
            def digest():
                return sha(json.dumps({p:sha((root/p).read_bytes()) if (root/p).exists() else None for p in sorted(paths)},sort_keys=True).encode())
            before=digest()
            try:
                for path, token, replacement in edits:
                    p=root/path
                    if token is None: p.write_text(replacement)
                    else:
                        source=p.read_text()
                        if source.count(token)!=1: raise RuntimeError('ambiguous/missing mutation '+ident)
                        p.write_text(source.replace(token,replacement))
                red_code,red=execute(ident+'-red',mode)
            finally:
                for path,raw in paths.items():
                    if raw is None: (root/path).unlink(missing_ok=True)
                    else: (root/path).write_bytes(raw)
            restored=digest(); green_code,green=execute(ident+'-restored',mode)
            causes=[s.strip() for s in red.splitlines() if oracle in s and (mode in ('scope','semantic') or 'AssertionError' in s)]
            if not red_code or 'COMPILATION ERROR' in red or not causes or green_code or before!=restored:
                raise RuntimeError('invalid production challenge '+ident+'\n'+red+'\nSECOND\n'+green[-2000:])
            for path,raw in paths.items():
                if ((ROOT/path).read_bytes() if (ROOT/path).exists() else None)!=raw: raise RuntimeError('source checkout altered')
            record=dict(id=ident,mutation=edits,expected_oracle=oracle,baseline_exit_code=0,observed_exit_code=red_code,
                failure_reason=causes[0],baseline_digest=before,restored_digest=restored,restoration_status='RESTORED',green_exit_code=green_code,
                green_command='compiled isolated '+mode+' production challenge; pinned m2',baseline_log_sha256=sha(baseline.encode()),
                red_log_sha256=sha(red.encode()),second_green_log_sha256=sha(green.encode()),kind='scope/diff guard after compilation' if mode=='scope' else 'compiled semantic/gate')
            records.append(record);print(json.dumps(record),flush=True)
        (logs/'receipt.json').write_text(json.dumps(records,indent=2)+'\n')
        print('PASS PRODUCTION CHALLENGE: '+str(len(records))+' compiled RED, exact restore and second GREEN',flush=True)
if __name__ == '__main__': main()
