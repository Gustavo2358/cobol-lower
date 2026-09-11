"""2A falsifications in isolated copies, byte restoration and a second real GREEN per case."""
import hashlib
import json
import os
from pathlib import Path
import re
import shutil
import subprocess
import sys
import tempfile

ROOT = Path(__file__).resolve().parents[2]
AIR = 'adapters/src/main/java/io/github/gustavo2358/lower/adapters/air/AirFileOutput.java'
CLI = 'adapters/src/main/java/io/github/gustavo2358/lower/adapters/cli/CobolLower.java'
CASES = [
 ('AIR-JACKSON', AIR, 'codec.encode(publication)', 'new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsBytes(publication)', 'boundary', 'ARCH_AIR_OUTPUT JSON library'),
 ('AIR-NEWLINE', AIR, 'files.write(temporary, bytes);', 'files.write(temporary, (new String(bytes, java.nio.charset.StandardCharsets.UTF_8) + "\\n").getBytes(java.nio.charset.StandardCharsets.UTF_8));', 'semantic', 'exact shared codec bytes'),
 ('NON-SUCCESS-WRITES', CLI, 'err.println("Lowering " + result.status());', '''try { output.write(new CobolLowerer().lower(result.admission().input().orElseThrow(), options).publication().orElseThrow(), destination); }
            catch (IOException ex) { throw new IllegalStateException(ex); }
            err.println("Lowering " + result.status());''', 'output', 'non-SUCCESS never invokes physical output'),
 ('LOWERING-EXIT-ZERO', CLI, 'return LOWERING;', 'return SUCCESS;', 'output', 'expected exit 4'),
 ('CODEC-EXIT-ZERO', CLI, 'return CODEC;', 'return SUCCESS;', 'semantic', 'expected exit 5'),
 ('CORE-CODEC', 'core/pom.xml', '</dependencies>', '<dependency><groupId>io.github.gustavo2358</groupId><artifactId>air-json</artifactId></dependency></dependencies>', 'architecture', 'ARCH_DEPENDENCY'),
 ('SUITE-REMOVED', 'adapters/pom.xml', '<execution><id>air-output-suite</id>', '<execution><id>air-output-suite</id>', 'semantic', 'AIR output tests absent/zero/duplicate'),
 ('SUITE-SKIPPED', 'adapters/pom.xml', '<mainClass>io.github.gustavo2358.lower.adapters.air.AirOutputSuite</mainClass>', '<skip>true</skip><mainClass>io.github.gustavo2358.lower.adapters.air.AirOutputSuite</mainClass>', 'semantic', 'AIR output tests absent/zero/duplicate'),
 ('OUTPUT-BEFORE-ENCODE', AIR, 'byte[] bytes = codec.encode(publication);\n        Path target = destination.toAbsolutePath();\n        Path temporary = files.temporary(target.getParent());', 'Path target = destination.toAbsolutePath();\n        Path temporary = files.temporary(target.getParent());\n        byte[] bytes = codec.encode(publication);', 'semantic', 'encode completes before any physical publication'),
]

def sha(data): return hashlib.sha256(data).hexdigest()
def execute(root, gate):
    command = [sys.executable,'scripts/harness/run.py',gate]
    if gate == 'output':
        # W1C adds CLI cases to the decoder suite. Compile the real mutant, then
        # let the existing output oracle observe its deliberately injected seam.
        # Baseline/restored semantic() still executes every suite.
        m2 = Path(os.environ['LOWER_BUILD_ROOT'])/'m2'
        build = subprocess.run(['mvn', '-B', '-ntp', '-Dmaven.repo.local='+str(m2),
                                'test-compile', '-Dexec.skip=true'], cwd=root, text=True,
                               stdout=subprocess.PIPE, stderr=subprocess.STDOUT)
        if build.returncode: raise RuntimeError('output mutation must compile\n'+build.stdout)
        jars = ['io/github/gustavo2358/air-java/0.1.0-SNAPSHOT/air-java-0.1.0-SNAPSHOT.jar',
                'io/github/gustavo2358/air-json/0.1.0-SNAPSHOT/air-json-0.1.0-SNAPSHOT.jar',
                'com/dynatrace/hash4j/hash4j/0.30.0/hash4j-0.30.0.jar',
                'com/fasterxml/jackson/core/jackson-core/2.22.2/jackson-core-2.22.2.jar',
                'com/fasterxml/jackson/core/jackson-databind/2.22.2/jackson-databind-2.22.2.jar',
                'com/fasterxml/jackson/core/jackson-annotations/2.22/jackson-annotations-2.22.jar']
        cp = ':'.join([str(root/m/'target'/c) for m in ('core','adapters') for c in ('classes','test-classes')]
                      + [str(m2/j) for j in jars])
        command = ['java', '-ea', '-cp', cp, 'io.github.gustavo2358.lower.adapters.air.AirOutputSuite']
    if gate == 'boundary':
        command = [sys.executable,'-c','from pathlib import Path; import sys; sys.path.insert(0,"scripts/harness"); from architecture import output_boundary_errors; e=output_boundary_errors(Path.cwd()); print("\\n".join(e)); sys.exit(bool(e))']
    p=subprocess.run(command,cwd=root,text=True,stdout=subprocess.PIPE,stderr=subprocess.STDOUT)
    return p.returncode,p.stdout

def green(root):
    code,out=execute(root,'semantic')
    if code or 'PASS semantic' not in out: raise RuntimeError('output challenge GREEN failed\n'+out)
    return out

def main():
    with tempfile.TemporaryDirectory(prefix='lower-output-challenge-') as name:
        root=Path(name)
        for folder in ('docs','scripts','core','adapters','.github'):
            shutil.copytree(ROOT/folder,root/folder,ignore=shutil.ignore_patterns('target','__pycache__'))
        for file in ('AGENTS.md','README.md','ARCHITECTURE.md','pom.xml','.gitignore'):
            shutil.copy2(ROOT/file,root/file)
        for ident,relative,before,after,gate,oracle in CASES:
            baseline=green(root)
            if gate != 'semantic':
                code,out=execute(root,gate)
                if code: raise RuntimeError('focal baseline failed\n'+out)
            path=root/relative; original=path.read_bytes()
            if ident == 'SUITE-REMOVED':
                replacement=re.sub(rb'<execution><id>air-output-suite</id>.*?</execution>', b'', original, flags=re.S)
            else:
                if original.count(before.encode()) != 1: raise RuntimeError('ambiguous/missing mutation '+ident)
                replacement=original.replace(before.encode(),after.encode())
            try:
                path.write_bytes(replacement)
                code,out=execute(root,gate)
            finally:
                path.write_bytes(original)
            restored=green(root)
            if gate != 'semantic':
                green_code,green_out=execute(root,gate)
                if green_code: raise RuntimeError('focal second GREEN failed\n'+green_out)
            causes=[line.strip() for line in out.splitlines() if oracle in line]
            if not code or not causes or path.read_bytes()!=original or (ROOT/relative).read_bytes()!=original:
                raise RuntimeError('invalid challenge '+ident+'\n'+out)
            print(json.dumps(dict(id=ident,mutation=before+' -> '+('remove execution' if ident=='SUITE-REMOVED' else after),expected_oracle=oracle,
                observed_exit_code=code,failure_reason=causes[0],baseline_exit_code=0,baseline_digest=sha(original),restored_digest=sha(path.read_bytes()),
                restoration_status='RESTORED',green_command='python3 scripts/harness/run.py semantic; focal '+gate+' (isolated copy)',green_exit_code=0,
                baseline_output_sha256=sha(baseline.encode()),red_output_sha256=sha(out.encode()),restored_output_sha256=sha(restored.encode()))),flush=True)
    print('PASS AIR OUTPUT CHALLENGE: 9 restored falsifications and second GREEN')

if __name__=='__main__': main()
