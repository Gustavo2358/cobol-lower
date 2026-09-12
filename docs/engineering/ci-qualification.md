# Remote FAST and local qualification

CP6 W2B supersedes the former remote cumulative/full orchestration. This policy
applies to push, pull_request and merge/main. REMOTE FULL QUALIFICATION IS PROHIBITED,
including workflow_dispatch. One workflow `.github/workflows/checkpoint.yml`, one
job/check `checkpoint`, 15-minute timeout. No redundant heavy/manual workflow.

`python3 scripts/harness/run.py ci-fast --commit <SHA>` checks actual checkout,
event/head identity, docs/lifecycle/links, source lock and MANIFEST integrity,
Git diff/scope and the orchestration guard. A conservative classifier has only two
results: DOCS_ONLY or CODE_OR_HARNESS_CHANGE. It never selects semantic tests by diff.
Protected Git blobs (including file modes) must match for DOCS_ONLY; source locks,
POMs, src/test/resources, scripts and workflows are protected. MANIFEST alone
cannot turn a protected change into documentation.

DOCS_ONLY runs docs/integrity/Git and focal orchestration tests only. Code/harness
changes add exact AIR dependency compilation, lower reactor compilation, fixed
FastSuite/FastAdapterSuite contracts (W1 and W2B, codec round-trip), and architecture.
The dependency FAST build uses javac/jar from the exact pin plus original POMs:
upstream Maven explicitly prohibits skipped tests, so no skip flag or POM rewrite
is used there. This compilation is not upstream qualification. Lower Maven uses
exec.skip for compile/install, then invokes only the reviewed focal mains.

The executable guard rejects unreviewed workflows/jobs/actions/commands, direct
heavy invocations, hidden aliases behind the content-locked entrypoint closure,
and manual full workflows. `remote-fast.lock.json` fixes reviewed entrypoint/test
blobs; update deliberately with source review when that closure changes. Runtime
local-only checks also reject heavy gate/script entrypoints under GITHUB_ACTIONS.
The guard is protection against accidental orchestration drift; editing both the
guard and its authority still requires human review, as does any executable policy.

Development uses focused RED/GREEN and local mutations, with exact restoration.
After self-review and second GREEN, create the final candidate commit with a clean
tree. Then run once:

```
python3 scripts/harness/run.py qualification-local --commit <SHA>
```

This executes the original exact upstream Maven build/tests, docs, full semantic
regression, N/2N/performance/capacity, architecture, complete harness tests, every
historical challenge, W2B challenges, repeated real producer W2A closed/open E2E,
restoration/second GREEN and Git/certificate checks. Existing heavy scripts and
full gate remain intact and mandatory locally. No analysis-cfg execution.
The explicit stage contract is the complete full battery, ordered without a
recursive full call or dropping components after failure.

The committed LOCAL_QUALIFICATION_CONTRACT freezes authority, oracle/fixtures,
stages, scopes and remote check. Receipts/logs are written outside the checkout,
with exact qualified HEAD/tree, commands, exits, durations and SHA-256. This
post-commit qualification protocol supersedes pre-commit full certification for
W2B; historical v1/v2 certificates keep their original meaning and bytes. A
candidate commit before qualification is not yet a certified recovery point.

Any subsequent protected code/test/harness/workflow/executable-contract blob
change invalidates qualification. A documentation-only successor is checked by
`verify-qualification --receipt <path> --commit <current SHA>`, docs-fast, integrity
and Git diff checks; the original HEAD/tree retains its qualification. An identical
merge tree reports QUALIFIED_TREE_PRESERVED. Neither case asserts full execution
on a later commit or schedules another giant run. Historical certificate tooling
remains a read-only audit facility, never a remote full entrypoint.

Push/Draft PR follows successful local qualification. Remote FAST must succeed at
the exact source checkout; receipt binds event, head SHA, checkout SHA, workflow,
job and success. Wait limit is 900 seconds from first push, with observed duration
reported; prolonged FAST is a finding, not a reason to increase timeout. Product
qualification belongs to LOCAL_QUALIFICATION, not FAST_CI. No merge/auto-merge.
