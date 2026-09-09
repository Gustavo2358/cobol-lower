# WORK-LOWER-007: capacity-independent lower input

The production input/admission path no longer rejects supported valid SP because
of total bytes, JSON nodes or admission work. Removed SP_BYTES=32MiB,
SP_NODES=1,500,000 and ADMISSION_ENTITIES=250,000, including the duplicate file
and decoder checks and Jackson string ceiling tied to byte capacity. No larger
magic constant, configuration, truncation, filtering or semantic fallback replaced
them. [Inventory](inventory.md) records enforcement and classification.

CP4C deliberately calibrated those thresholds using its measured corpus; this
new work item changes that capacity policy. Its history, fixtures and source pins
are unchanged. The existing semantic profiles, identities, provenance, readiness,
PARTIAL coverage and lowering rules are unchanged.

## Falsifiable proof

[CapacitySuite](../../../adapters/src/test/java/io/github/gustavo2358/lower/adapters/air/CapacitySuite.java)
generates a synthetic shared-DATA / 20,000-MOVE SP from the already-proven 1.2
fixture, without a large committed JSON. It is byte-identical to the test run
before implementation. Initial compiled RED: `CAPACITY supported SP decodes`
with `IMPLEMENTATION_LIMIT / physical / $ file byte limit`, exit1. It is not an
OOM, codec, malformed fixture or compiler failure.

The identical corpus passes complete file intake, byte decode, shape/coherence,
admission, lowering, shared AirValidator and the unchanged ScalarSuite relational
oracle twice. Full Publication equality is the existing in-memory determinism
oracle; both independent runs produce publication `66cf27c9e8562a9a8d46624b2412d512`.

| Measurement | Actual |
| --- | ---: |
| SP bytes | 38,179,536 |
| JSON nodes | 2,100,154 |
| Admission visits | 380,021 |
| Indexed references | 140,003 |
| Container depth / including leaves | 7 / 8 |
| DATA Object / Cell / Entry / Sequence | 1 each |
| Assign / Return | 20,000 / 1 |
| Statement / operand correlations | 20,001 / 40,000 |
| AIR origins | 240,013 |

SP SHA-256: `202ce49d1a632ab2997bd8d9beb0300c4c81bfd6935e0cdfa64a564a844f3a3e`.
[Measurements](measurements.json) include the single-string case (33,554,433
characters preserved, 33,559,606 SP bytes) and heap telemetry. The exact raw RED
and first GREEN are in [RED](logs/initial-red.log.gz) and
[GREEN](logs/first-green.log.gz).

[capacity_challenge.py](../../../scripts/harness/capacity_challenge.py) reintroduces
file32MiB, decoder32MiB, nodes1.5M and visits250k separately. Each experiment must
compile, reach the frozen capacity assertion, restore exact source digests and
pass the same suite again. The full harness includes these experiments, CP4C
coherence/probes and all historical challenges. CP0.json records actual final
execution results; early failed setups are preserved, never counted as PASS.

## Safety, baseline and complexity

Depth64 remains justified by the parser and recursive wire-shape traversal;
supported repeated facts grow flat arrays rather than nesting. Strict UTF-8,
duplicate keys, JSON/shape, contract versions, coherent values, reference/profile
validation, numeric/identity overflow, diagnostic exhaustion on invalid input,
AIR validation, failure taxonomy and atomic output/cleanup remain covered.
Jackson numeric token/property-name safety guards remain; its pinned default
whole-document/token limits are disabled. No source or contract in another
repository changed.

[Canonical comparisons](canonical-bytes.json) independently ran the clean base
50cc57d and this candidate through the real CLI. CP3: 13,827 bytes, SHA-256
`46919c1429db4aa310e66fc9df9374eeba53fd98e50a287fdd005c17622f33ad`.
Scalar MOVE: 32,138 bytes, SHA-256
`dd3bb4819282e609a97937ea01b7e202786e2c2d9ba9c8a1821a8b982eeb8788`.
Both are byte-identical. Existing fixtures/goldens remain untouched. Ordinary
semantic execution has 203,099 assertions, plus the output suite; the performance
profile adds 93 existing ledger assertions and the new capacity proofs. Removal
of obsolete capacity-rejection tests accounts for the changed count.

The input node walk, one-time indexing, reference resolution and assembly remain
approximately O(D+S+R+P), plus input bytes/hashing. Exact N/2N ledgers and the
20k measurements are preserved; no repeated global index/scan, serialization or
whole-document representation was introduced. A standalone 1,536MiB-heap run
passed (17.22s, max RSS 1,627,832KiB), including generation and two retained
Publications. This is telemetry, not an amplification comparison or SLA.
**BACKLOG-LOWER-018 is not claimed solved.**

## Independent boundaries and reproduction

Source lock still selects air-java `ce530a7e17ab12b23c48f29425f503ff920b09fb`.
The shared codec's 16MiB boundary is unchanged: the real 10k production probe
still reaches it at exit5 while preserving its destination and leaving no temp
residue. The 20k proof deliberately ends at valid in-memory Publication; no local
codec override or unmerged AIR artifact was used. Upstream AirValidator options
remain unchanged as well. BACKLOG-LOWER-017/018 are not repurposed. Requalifying
large file-to-file E2E after independently merged AIR work is a future task.

Use Java21, Maven, the unchanged Python requirements and an isolated
`LOWER_BUILD_ROOT`; run the standard bootstrap from the lock, then
`python3 scripts/harness/run.py full --evidence docs/quality/WORK-LOWER-007/CP0.json`.
The harness currently assumes `.git` is a directory in its fixture-copy helpers.
For local validation we copied every candidate file byte-exactly into a disposable
standalone clone; no check was skipped or marked PASS in the failing worktree run.
CI already uses a standalone clone. [Lifecycle note](lifecycle.md) records the
separate inherited CP4C registration constraint. Neither issue required changing
production or weakening the harness.

[Self-review](review.md), [certificate](CP0.json) and the final PR provide the
reviewed candidate digest, executed gates, raw logs and exact published SHA/CI
receipt. Human review is pending; no merge or auto-merge is authorized.
