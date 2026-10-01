# Analyzer integration — DONE / MERGED

[Lower #52](https://github.com/Gustavo2358/cobol-lower/pull/52) merged on
2026-10-01 as `842597a711bf1a98dad79cf9daedf3ee7870e223`, preserving qualified
implementation HEAD `fcadfc91fdeb393eca3d2ed3734e701f17edf37d`.

The lowerer shares only bodies whose typed entry/completion, transitive closure,
CICS support and handler mode prove equivalent behavior. Each invocation keeps
its own resume. Recursive/reentry frontiers, nonlocal escapes and state-changing
handlers retain specialization. Anonymous logical FILLER roots keep structural
identity without invented names, data links or physical proof.

## Producer revisions

- Frontend main: `cc1fb20a0e19284b8b97b44628403a74630bd466` (#77/#78).
- AIR main: `6c4a6eb225fb4bb4fdc5871bc5232f03387ef099` (#23/#24/#25).
- AIR specification remains `2c7f31f19efbe3211a2aea5bbda90173a9666fe2`.

Workflow and source lock use the same immutable AIR SHA. Frontend product,
grammar, test and build sources equal its qualified HEAD. AIR product sources
equal qualified codec HEAD `13518a709908eb3824ccd83d5c52d26cf83c1aac`.
The lowerer itself is unchanged; its Fast is rerun against the new AIR dependency.
Earlier harness/consumer reference snapshots remain separately pinned history.

## Evidence and bounds

[Implementation Fast CI](https://github.com/Gustavo2358/cobol-lower/actions/runs/36794719702)
passed. Shared-body qualification covered 560 pipelines, CardDemo 73, PERFORM 39,
Chaos 48, aliases 14 and 25 PERFORM adversaries, with zero candidate/support/
provenance loss. CardDemo nodes decreased 110,570 → 79,176 (28.4%); caller states
remain separate. Publication fixes and JSON GENERATE received their own focal,
source-replay and selected downstream checks, documented in the linked records.

[Sharing](shared-routine-bodies.md), [anonymous roots](stage5-anonymous-logical-roots.md),
[publication fixes](stage5-photo-publication-fixes.md), [partial FILE](stage5-partial-file-routes.md)
and [JSON GENERATE](stage5-json-generate.md) retain their original measurements.
These corpus/full results are reused evidence, not a new full execution.
The optimized AIR codec separately preserved 2,240 products across 560 cases.

PARTIAL, model assumptions, recursion limits and source qualifications remain.
CFG v5 requires a compatible viewer; that UI migration is outside these merges.
Final pins, new consumer Fast results and main CIs are recorded in workspace
`artefatos-e2e/analyzer-integration-20261001/REPORT.md`.
