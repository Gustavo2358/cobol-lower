# CP6 W2B handoff

W2B IMPLEMENTED / QUALIFIED / AWAITING_HUMAN_REVIEW.
Work item WORK-LOWER-011, CP0, single-checkpoint. Branch `feat/cp6-w2b-if-lowering`.
[Draft PR #12](https://github.com/Gustavo2358/cobol-lower/pull/12). No merge/auto-merge.
W2D NOT_STARTED / NOT_AUTHORIZED. REMOTE CI = FAST ONLY; FULL QUALIFICATION = LOCAL ONLY.

| Identity | Exact value |
| --- | --- |
| Baseline | `9de3825da64898258e647727393f01b9e9198d9e` |
| Baseline tree | `2e3027df2fc4f9e550df8df9518e7e80f89324f1` |
| W1C administrative commit | `a427f04b779ddf3e7682e05a5917935c581095e0` |
| CI split commit | `e1ae37379f1277ab7019a57b74ea40518266728c` |
| Qualified product HEAD | `f588704c5a958af93562fe10e5e6646f5cb6a17e` |
| Qualified product tree | `c7acea42fd4ab0fb21cb03c6314016e24ac46544` |
| SP W2A product pin | `4ffabded1aad39316b8a6f337f732976fdb3ca3e` |
| SP W2A tree | `5880e174b33c85ba3f3cdbc70bd2d8dc7b1d567b` |
| AIR Java W2C product pin | `1d22068e9d9c1d100ecdef734e5b6995252e7ede` |
| AIR Java W2C tree | `ab3a55e13b781ec5424356e83bddbc6fde42dcb1` |
| AIR normative pin | `51b4d9a8ae0364232bd97103cd73a77e1a34996c` — AIR 2.0.0 / JSON 1.0.0 DRAFT |

The final published PR head may be the documentation-only successor containing this
handoff. Its identity and exact FAST checkout are recorded in the final delivery and
GitHub run. Product qualification belongs to the exact HEAD/tree above. Packaging
receipts does not claim FULL execution on a later HEAD/tree. Protected-blob comparison
and docs/integrity/Git checks govern that successor; no second giant run is required.

## Product and boundaries

Closed produces five sequences: explicit IF Branch, real THEN/ELSE Assign arms with
Jump to one CALL merge, Invoke, and real GOBACK Return. Open produces four sequences:
FALSE targets CALL directly; there is no synthetic ELSE, havoc, assignment or initial
WS-PGM. Entry is explicit and remains independent of physical sequence order.

The predicate is Unknown with known BOOL type, exact ordered whole-item published Read
dependencies, NoMemory remainder and deterministic VALUES-only uncertainty. Runtime
FLAG truth is not evaluated. The one DisjointStorage premise maps every published
member in order through the existing scalar object/cell mapping, including unused extra
members in contract tests. Authority/rule/provenance derive from SP; lower proves no
separation by comparing IDs. W1 Invoke, UnknownName, UnknownContract, open effects/
outcomes, fitting and real statement origins remain intact.

Both real W2A publications are STRUCTURALLY_VALID with no INVALID_IR findings and four
semantic obligations each: I-59, I-09, I-56, I-23/I-56. These remain open; global coverage
is PARTIAL. The shared W2C codec preserves whole Publication equality and byte-identical
re-encode. [Read-only publication audit](final-publication-audit.log.gz).

Product files: SpInput; EntryGobackAdmission; CallAdmission; CallLowerer;
CallSequenceAssembler; CanonicalRevision; CobolLowerer; new IfAdmission, IfLowerer,
IfPredicate, IfSequenceAssembler, StoragePremise; adapters Materialize, SpJsonDecoder
and new Wire14. Wire13, MoveHandler, InvokeHandler, AIR model/validator/codec and
historical SP/AIR goldens are unchanged. [Algorithm and limits](../../domain/simple-if-diamond.md).

## Three distinct evidence classes

[Development RED/GREEN](development-summary.md) preserves all five independent REDs:
version, materialization, linear admission, valid plan without assembler, diamond
without premise. Focused final GREEN: 2325 core assertions including 961 IF assertions,
144 closed/open adapter focal assertions, and strict-wire negatives. The development
campaign's initial I-58 masking failure and refined ordered-member detection remain
explicit; no oracle expectation was weakened.

LOCAL_QUALIFICATION command, exit 0:

```
python3 scripts/harness/run.py qualification-local --commit f588704c5a958af93562fe10e5e6646f5cb6a17e
```

| Local stage | Exit | Seconds |
| --- | --- | --- |
| Exact upstream bootstrap/build/tests | 0 | 19.945 |
| Docs | 0 | 0.865 |
| Full semantic regression | 0 | 6.957 |
| Performance/capacity | 0 | 42.165 |
| Architecture | 0 | 63.389 |
| Complete harness tests (198) | 0 | 39.449 |
| Historical challenges (76) | 0 | 1197.576 |
| W2B challenges (26) | 0 | 103.288 |
| Real producers | 0 | 115.049 |
| Restoration/second GREEN | 0 | 1.360 |
| Git/certificate | 0 | 0.116 |

Semantic count 204992; performance count 39215. IF N/2N: 53070/106070 visits and
0.254/0.534 seconds for N=1000/2000. No repeated linear lookup or pairwise storage
proof. W2B challenges comprise 24 compilable product mutations and two malformed
AIR transport placements rejected by I-04, since Java's sealed types exclude them.
All restore byte-exact with second GREEN.

Real producer integration uses isolated clones at W1A and W2A product SHAs. All 23 W1A
fixtures run twice and match frozen SP bytes. Literal/computed W1 AIR is also byte-equal
to historical W1C outputs ([hash comparison](w1-historical-air-comparison.json)). Real
W2A closed/open run twice through SP1.4, lower, CLI, independent AIR oracle and W2C codec;
SP/AIR are deterministic. No analysis-cfg execution.

[Local evidence archive](local-qualification.tar.gz) contains the original receipt and
761 raw evidence files; [bundle integrity](local-qualification-bundle.json) binds:

- Receipt SHA-256: `b962a4377f7eabc2cfa0de8f86884057456012db8bc7cb50f356dbfd8331dadd`.
- Archive SHA-256: `3bc7919aaedd72479fce0d617ffeba53ea7a59facfa9354895ca5d7dffeb9180`.

Extract outside the checkout, then verify the extracted `receipt.json` with
`run.py verify-qualification --receipt <path> --commit <current HEAD>`. Every original
stage log and evidence digest is checked; a documentary successor reports
DOCUMENTATION_ONLY_SUCCESSOR and `full_executed_on_current_head=false`.

REMOTE FAST CI: [push run 34705094573](https://github.com/Gustavo2358/cobol-lower/actions/runs/34705094573),
[exact checkout receipt](remote-code-push-receipt.json). Event push; head and checkout
`f588704c5a958af93562fe10e5e6646f5cb6a17e`; workflow `.github/workflows/checkpoint.yml`,
job/check `checkpoint`, app `github-actions`; success; FAST gate 120.098 seconds.
The [PR run](https://github.com/Gustavo2358/cobol-lower/actions/runs/34705113765) also succeeds
on the same SHA. Remote CI certifies the fast gate only. The W2B product qualification
is the local qualification receipt for the exact source HEAD/tree.

## CI policy and retained limits

One remote workflow, 15-minute timeout, no manual full or workflow_dispatch. Fixed
FastSuite/FastAdapterSuite cover W1/W2, strict wire and codec. Docs-only avoids bootstrap
and Maven; the clean e6874cb documentary commit ran the CI entrypoint locally in 1.116 s.
The final documentary push supplies the remote counterpart. Fifteen CI guard tests
cover required countercases, direct/hidden aliases, workflow events and protected blob
classification; eight receipt tests reject stale/forged execution ownership. Source
locks, executable freezes/guards, POMs, tests/resources, scripts and workflows are protected.

Changed CI/harness files include checkpoint.yml; run.py; ci_fast.py; ci_policy.py;
local_only.py; focal.py; upstream_fast.py; remote-fast.lock.json; qualification_local.py;
if_challenge.py; w2b_e2e.py; local guards in existing challenge/E2E entrypoints; and their
focal tests/fixture copy helpers. No heavy suite was deleted. The content-locked closed
entrypoint guard plus runtime checks reject remote heavy execution. As with any code
review guard, deliberately changing both the guard and its authority is subject to
human review; this is not a security sandbox against a hostile repository maintainer.

[Siblings receipt](siblings-read-only.json): all five sibling HEAD/status/diff observations
match intake; untracked byte maps match qualification start/end. Artefatos-e2e's pre-existing
untracked state was preserved. No sibling branch/push/write and no root Git changes.

Limitations remain explicit: one root simple IF, scalar text equality proof, admitted
MOVE arms, one W1C CALL/GOBACK merge chain; no general/nested control, runtime target
resolution, FLAG evaluation, path pruning, solver/lattice, CFG or W2D. W1C is merged/closed
with official review_status `not_recorded`, because no review event was recorded by GitHub.
Human review of W2B is pending; no merge or next wave has started.
