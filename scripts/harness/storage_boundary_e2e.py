"""Run storage/capture laws through the reviewed four-stage production CLI oracle.

The runtime manifest supplies exact checkouts, binaries and hashes; no toolchain
or product is rebuilt here. The CFG repository owns the shared dependency oracle.
"""
import argparse
import concurrent.futures
import importlib.util
import json
from pathlib import Path
import subprocess

ROOT = Path(__file__).resolve().parents[2]


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--runtime', required=True, type=Path)
    parser.add_argument('--work', required=True, type=Path)
    parser.add_argument('--java', default='java')
    args = parser.parse_args()
    config = json.loads(args.runtime.read_text())
    cfg = Path(config['checkouts']['analysis-cfg'])
    if subprocess.check_output(['git', '-C', str(cfg), 'rev-parse', 'HEAD'], text=True).strip() != config['sources']['analysis-cfg']:
        raise ValueError('dependency oracle checkout differs from runtime source pin')
    spec = importlib.util.spec_from_file_location('storage_boundary_oracle', cfg / 'scripts/project/e2e_perform_completion.py')
    oracle = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(oracle)
    base_evaluate = oracle.evaluate

    def evaluate(case, doc):
        result = base_evaluate(case, doc)
        for tag, expected in case['calls'].items():
            observed = result['calls'][tag]
            if expected.get('requiresUnknownRemainder') and not any(
                    a['effectiveUnknownRemainder'] for a in observed['activations']):
                observed['status'] = result['status'] = 'FAIL'
                observed['failures'].append('UNKNOWN_REMAINDER_MISSING')
        return result

    oracle.evaluate = evaluate
    oracle.ROOT = ROOT / 'adapters/src/test/resources/e2e/storage-boundary'
    hashes = {p: oracle.sha(Path(p)) for stage in oracle.STAGES for p in config[stage]['classpath']}
    if hashes != config['artifactHashes']:
        raise ValueError('runtime artifact hashes differ')
    config['semanticProductFile'] = 'cobol-semantic-compilation.json'
    out = args.work.resolve()
    out.mkdir(parents=True, exist_ok=False)
    oracle.dump(out / 'runtime.json', config)
    cases = json.loads((oracle.ROOT / 'expected.json').read_text())['cases']
    rows = []
    with concurrent.futures.ThreadPoolExecutor(max_workers=2) as pool:
        for row in pool.map(lambda case: oracle.run_case(case, config, out, 120, args.java, '1g'), cases):
            rows.append(row)
            oracle.dump(out / 'results.json', {'runs': rows})
            print(row['id'], row['status'], flush=True)
    if hashes != {p: oracle.sha(Path(p)) for p in hashes}:
        raise ValueError('runtime changed during execution')
    return 0 if all(row['status'] == 'PASS' for row in rows) else 1


if __name__ == '__main__':
    raise SystemExit(main())
