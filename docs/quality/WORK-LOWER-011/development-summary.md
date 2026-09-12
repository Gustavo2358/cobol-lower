# W2B development evidence

This is Development RED/GREEN, not LOCAL_QUALIFICATION or REMOTE FAST CI.
All logs are raw gzip streams with SHA-256 in [development-logs.json](development-logs.json).
The original files remain under `/tmp/cp6-w2b-session`; gzip preserves every byte,
including Maven whitespace that cannot be stored as new plain text under diff-check.
No historical evidence or goldens were rewritten.

Five separate product stages were observed before completing each capability:

| Stage | Observation | Exit |
| --- | --- | --- |
| RED1 version | Real closed/open SP1.4 rejected, UNSUPPORTED_CONTRACT, no AIR | 3 each |
| RED2 materialization | Wire14 decodes, IF remains OtherStatement | 1 |
| RED3 admission | IfFact materialized; W1C linear admission UNSUPPORTED_SLICE | 1 |
| RED4 assembly | New IF admission ADMITTED; public lower still lacks diamond | 1 |
| RED5 premise | Successful five-sequence diamond; premises=0 | 1 |

The initial independent oracle hash precedes product work. Its final challenge
freeze records the public API correction from initializations to conditions and
stronger artifact-name/exact-origin checking; structural expectations were retained.
Compilation mistakes and one probe launcher syntax error remain in logs, separately
from semantic REDs.

Focused GREEN: 961 IF core assertions; fixed FAST core totals 2325 assertions;
real frozen SP closed/open adapter focal contributes 144 assertions. Additional
closed-wire counterexamples total 153 integration assertions. These include full
AIR model equality, canonical byte re-encode and unchanged Validator issues. W1
literal/computed CALL, fitting, effects/outcomes and unknown name/contract pass.
No producer process or N/2N/performance qualification was run during development.

Local challenge: 24 compiled product mutants plus 2 malformed AIR transport
placements, each restored byte-exact with second GREEN. The sealed AIR Java types
exclude Invoke as Instruction and Assign after a terminator; malformed wire tests
verify real W2C decoder I-04 rejection for those two cases. The first attempt to
drop a proof member reached I-58 because a two-member set became one. The refined
mutant preserves at least two bases and is killed by the unchanged ordered-member
oracle on the larger proof. Both campaign logs/receipts are retained under development.
No product correction was needed after that campaign.

CI guards: 15 orchestration/context tests, including all ten requested countercases;
8 local-receipt binding tests; missing/zero/duplicate W1/W2 suite markers; exact
push/main/PR context. A real clean documentation-only commit e6874cb ran docs-only
CI locally in 1.116 seconds without bootstrap or Maven. This is local execution of
the CI entrypoint, not a remote success claim.

Self-review covered closed Wire14 without reinterpretation of Wire13, in-memory
proof admission, canonical child/explicit continuation joins, entry independent
of first sequence, mechanical ordered proof mapping, Unknown BOOL purity/read/value
claims, shared W1 handlers and origin/identity preservation, indexed complexity,
fixed FAST closure and local-only heavyweight entrypoints. Fixture copy helpers now
include MANIFEST because the W2B documentary scope references that real file; the
scope validators and historical review expectations were retained. No blocker found.

Final local qualification, producer E2E, performance and exact-head remote FAST are
pending until the clean candidate is committed and the canonical local command runs.
W2D NOT_STARTED / NOT_AUTHORIZED. No sibling write, push or merge.
