#!/usr/bin/env python3
"""Execute the frozen W1A producer twice; keep exact bytes and provenance receipts."""
import argparse
import hashlib
import json
from pathlib import Path
import subprocess
import shutil

ROOT = Path(__file__).resolve().parents[2]
W1A = "53d774026a1e4bcd969c7783a1d277aaa87b5f2f"


def sha(data):
    return hashlib.sha256(data).hexdigest()


def main():
    from local_only import require_local
    require_local()
    p = argparse.ArgumentParser(description=__doc__)
    p.add_argument('--producer', type=Path, required=True)
    p.add_argument('--m2', type=Path, required=True)
    p.add_argument('--out', type=Path, required=True)
    p.add_argument('--capture', action='store_true')
    p.add_argument('--lower', action='store_true', help='Run real CLI and independent AIR oracle for dynamic X8 and literal in both producer runs')
    args = p.parse_args()
    producer = args.producer.resolve()
    assert subprocess.check_output(['git', '-C', str(producer), 'rev-parse', 'HEAD'], text=True).strip() == W1A
    assert not subprocess.check_output(['git', '-C', str(producer), 'status', '--porcelain'])
    jars = ['org/antlr/antlr4-runtime/4.13.2/antlr4-runtime-4.13.2.jar',
            'org/slf4j/slf4j-api/2.0.18/slf4j-api-2.0.18.jar',
            'ch/qos/logback/logback-core/1.6.3/logback-core-1.6.3.jar',
            'ch/qos/logback/logback-classic/1.6.3/logback-classic-1.6.3.jar',
            'com/fasterxml/jackson/core/jackson-core/2.22.2/jackson-core-2.22.2.jar',
            'com/fasterxml/jackson/core/jackson-databind/2.22.2/jackson-databind-2.22.2.jar',
            'com/fasterxml/jackson/core/jackson-annotations/2.22/jackson-annotations-2.22.jar']
    cp = ':'.join([str(producer/'target/classes')] + [str(args.m2.resolve()/j) for j in jars])
    lower_jars = jars[-3:] + [
        'io/github/gustavo2358/air-java/0.1.0-SNAPSHOT/air-java-0.1.0-SNAPSHOT.jar',
        'io/github/gustavo2358/air-json/0.1.0-SNAPSHOT/air-json-0.1.0-SNAPSHOT.jar',
        'com/dynatrace/hash4j/hash4j/0.30.0/hash4j-0.30.0.jar']
    lower_cp = ':'.join([str(ROOT/m/'target'/c) for m in ('core', 'adapters') for c in ('classes', 'test-classes')]
                        + [str(args.m2.resolve()/j) for j in lower_jars])
    args.out = args.out.resolve(); args.out.mkdir(parents=True, exist_ok=True)
    fixtures = ROOT/'adapters/src/test/resources/sp/cp6'
    execution = args.out/'execution'
    execution.mkdir(exist_ok=True)
    web = execution/'src/main/resources/web'
    web.parent.mkdir(parents=True, exist_ok=True)
    if not web.exists():
        web.symlink_to(producer/'src/main/resources/web', target_is_directory=True)
    receipt = {'producer_sha': W1A, 'producer_tree': subprocess.check_output(['git', '-C', str(producer), 'rev-parse', 'HEAD^{tree}'], text=True).strip(), 'cases': []}
    for source in sorted(fixtures.glob('*.cbl')):
        shutil.copyfile(source, execution/source.name)
        runs = []
        for run in (1, 2):
            output = args.out/f'{source.stem}-{run}'
            command = ['java', '-cp', cp, 'io.github.gustavo2358.cobolexplorer.ExplorerMain',
                       '--source', source.name, '--copybooks', str(producer/'corpus/cpy'), '--output', str(output)]
            result = subprocess.run(command, cwd=execution, stdout=subprocess.PIPE, stderr=subprocess.STDOUT)
            (args.out/f'{source.stem}-{run}.log').write_bytes(result.stdout)
            wire = output/'cobol-semantic-product.json'
            if not wire.exists():
                raise RuntimeError(f'{source.stem}: no SP output, exit={result.returncode}')
            raw = wire.read_bytes()
            assert json.loads(raw)['contractVersion'] == '1.3.0'
            runs.append({'run': run, 'exit': result.returncode, 'sp_sha256': sha(raw), 'command': command})
            if args.lower and source.stem in ('dynamic-x8', 'literal'):
                if result.returncode != 0:
                    raise RuntimeError('positive producer failed')
                outputs = []
                for label, main_class, suffix in (
                    ('cli', 'io.github.gustavo2358.lower.adapters.cli.CobolLower', []),
                    ('oracle', 'io.github.gustavo2358.lower.adapters.testing.CallIntegrationSuite', [source.stem])):
                    air = output/(label + '.air.json')
                    cmd = ['java', '-ea', '-cp', lower_cp, main_class, str(wire), str(air), *suffix]
                    lowered = subprocess.run(cmd, cwd=ROOT, stdout=subprocess.PIPE, stderr=subprocess.STDOUT)
                    (args.out/f'{source.stem}-{run}-{label}.log').write_bytes(lowered.stdout)
                    if lowered.returncode != 0:
                        raise RuntimeError(f'{source.stem} {label} failed: {lowered.stdout.decode()}')
                    outputs.append(air.read_bytes())
                    runs[-1][label] = {'command': cmd, 'exit': lowered.returncode, 'air_sha256': sha(outputs[-1])}
                assert outputs[0] == outputs[1], 'CLI bytes differ from independent validated/round-trip oracle'
                if run == 2:
                    assert outputs[0] == (args.out/f'{source.stem}-1/cli.air.json').read_bytes(), 'AIR determinism'
        raw1 = (args.out/f'{source.stem}-1/cobol-semantic-product.json').read_bytes()
        assert raw1 == raw, source.stem + ': SP determinism'
        if args.capture:
            destination = source.with_suffix('.json')
            if destination.exists() and destination.read_bytes() != raw1:
                raise RuntimeError('Refusing to replace a different frozen fixture: ' + str(destination))
            destination.write_bytes(raw1)
        else:
            assert source.with_suffix('.json').read_bytes() == raw1, source.stem + ': frozen SP bytes differ'
        receipt['cases'].append({'fixture': source.stem, 'source_sha256': sha(source.read_bytes()), 'runs': runs, 'deterministic': True})
        (args.out/'producer-receipt.json').write_text(json.dumps(receipt, indent=2)+'\n')
        print(source.stem + ': real SP 1.3 twice, identical', flush=True)


if __name__ == '__main__':
    from local_only import require_local
    require_local()
    main()
