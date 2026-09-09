"""Reintroduce every old capacity gate; the unchanged 20k oracle must kill it after compilation."""
from production_challenge import main, SP, FILE, ADMISSION

CASES = [
    ('file-32mib', [(FILE, 'byte[] bytes = input.readAllBytes();',
        'byte[] bytes = input.readAllBytes(); if (bytes.length > 32 * 1024 * 1024) return new SpJsonDecoder.Rejected(new SpJsonDecoder.Diagnostic(SpJsonDecoder.Code.IMPLEMENTATION_LIMIT, "physical", "$ file byte limit"));')],
        'capacity', 'CAPACITY supported SP decodes'),
    ('decoder-32mib', [(SP, 'meter.bytes = bytes.length;',
        'if (bytes.length > 32 * 1024 * 1024) return reject(Code.IMPLEMENTATION_LIMIT, "$ byte limit"); meter.bytes = bytes.length;')],
        'capacity', 'CAPACITY supported SP decodes'),
    ('nodes-1500000', [(SP, 'meter.nodes++;',
        'if (++meter.nodes > 1_500_000) return reject(Code.IMPLEMENTATION_LIMIT, "$ node limit");')],
        'capacity', 'CAPACITY supported SP decodes'),
    ('visits-250000', [(ADMISSION, 'entities++;',
        'if (++entities > 250_000) throw new LimitReached();')],
        'capacity', 'CAPACITY supported SP lowers'),
]

if __name__ == '__main__':
    main(CASES, 'capacity')
