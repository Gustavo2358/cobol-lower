"""Fixed technical profiles for cobol-lower. Product dependency pins stay strict."""
from pathlib import Path
import shutil
import subprocess
import sys
from lean import validate_pins, require_local

LOCK = 'docs/sources/sources.lock.json'
PINS = ('Gustavo2358/air-java', 'Gustavo2358/analysis-ir', 'Gustavo2358/proleap-poc', 'dynatrace-oss/hash4j')


def pin_errors(root):
    return validate_pins(root, LOCK, PINS)


def copy_pin_fixture(source, destination):
    target = destination / LOCK
    target.parent.mkdir(parents=True, exist_ok=True)
    shutil.copyfile(source / LOCK, target)


def break_pin_fixture(root):
    (root / LOCK).write_text('{}')


def technical_fast(root):
    import run as harness
    harness.bootstrap(fast=True)
    harness.run(harness.maven('clean', '-Dexec.skip=true', '-DskipTests=true', 'install'))
    from focal import execute
    execute(root, harness.build_root(), 'fast')
    harness.architecture()
    harness.run([sys.executable, '-B', '-m', 'unittest', 'discover', '-s', 'scripts/harness/tests', '-v'])


def challenges(root):
    require_local()
    # Algorithmic challenges remain useful on demand; administrative challenges retired.
    for name in ('semantic_challenge', 'output_challenge', 'scalar_challenge', 'production_challenge',
                 'capacity_challenge', 'call_challenge', 'if_challenge'):
        subprocess.run([sys.executable, '-B', 'scripts/harness/' + name + '.py'], cwd=root, check=True)


def full_local(root):
    require_local()
    import run as harness
    harness.bootstrap(fast=True)
    harness.semantic()
    harness.performance()
    harness.architecture()
    # Mutation and real producer E2E remain separate, on-demand technical tools.
