# CP6 W2B — merge verified; administrative archival blocked

PR #12: MERGED, method `merge`. Product APPROVED / MERGED and W2B PRODUCT BASELINE
FROZEN. WORK-LOWER-011 lifecycle: `blocked`, CP0 `completed`; CLOSED is not asserted.
The user approved the exact review HEAD and explicitly authorized this merge in the
2026-09-12 task. [Approval provenance](closure/human-approval.json) distinguishes
that authority from the empty GitHub review-event list.

## Three separate authorities

| Authority | HEAD | Tree |
| --- | --- | --- |
| Local qualification | `f588704c5a958af93562fe10e5e6646f5cb6a17e` | `c7acea42fd4ab0fb21cb03c6314016e24ac46544` |
| Final human review | `cc55f45b55c3b9753ff4f1d86f5028a55ad150ab` | `52629a7bb9a97f78a943c924aad28631b9357d72` |
| Integrated W2B product merge | `2b7fa3a5cee865eef5007d6d870618e032047e1e` | `52629a7bb9a97f78a943c924aad28631b9357d72` |

Baseline/main before merge: `9de3825da64898258e647727393f01b9e9198d9e`, tree
`2e3027df2fc4f9e550df8df9518e7e80f89324f1`. The reviewed synthetic merge remained
`cbfa378aedb3f3c35ab76f691d4475810093121d`, with the exact base and head parents and
the final review tree. [Preflight](closure/preflight-identity.json),
[actual merge identity](closure/merge-identity.json), [remote PR](closure/pr-merged.json),
[expected-head request](closure/merge-request.json) and [merge response](closure/merge-response.json).

The direct qualified-product → review child delta was checked by actual Git paths.
FINAL_REVIEW_TREE_PRESERVED and QUALIFIED_PRODUCT_BLOBS_PRESERVED were independently
verified after merge. Protected maps include modes, object types and blob IDs.
Full trees of the qualified product and reviewed documentation successor differ;
their protected product blobs are identical. Relation: DOCUMENTATION_ONLY_SUCCESSOR.
The receipt verifier reports `full_executed_on_current_head=false` for the review
successor ([verification](closure/verify-review.log.gz)).

This administrative record is a documentation-only successor of the product merge.
Its exact commit/tree and DOCS_ONLY remote receipt are recorded in the final task
handoff after publication, avoiding a recursive self-SHA commit. It receives no new
Full Qualification. Qualification source, product merge and current main must remain
three distinct identities; never interpret current main as a newly qualified HEAD.

## Qualification and remote verification

LOCAL_QUALIFICATION: NOT REPEATED. The original 11 PASS stages remain immutable:
exact upstream bootstrap, docs, full semantic regression, performance/capacity,
architecture, complete harness, 76 historical challenges, 26 W2B challenges, real
W2A producers, restoration/second GREEN and Git/certificate. No heavy gate was run
in this closure, locally or remotely. [Original handoff](handoff.md),
[bundle](local-qualification-bundle.json), [original archive](local-qualification.tar.gz).

- Receipt SHA-256: `b962a4377f7eabc2cfa0de8f86884057456012db8bc7cb50f356dbfd8331dadd`.
- Archive SHA-256: `3bc7919aaedd72479fce0d617ffeba53ea7a59facfa9354895ca5d7dffeb9180`.

Product qualification authority is LOCAL_QUALIFICATION at f588704. Merge verification
authority is REMOTE_FAST at the merge SHA plus protected-blob identity.

[Merge FAST run 34707948882](https://github.com/Gustavo2358/cobol-lower/actions/runs/34707948882):
PASS. Workflow ID `351822221`, `.github/workflows/checkpoint.yml`, job `checkpoint`,
event `push`, head and checkout `2b7fa3a5cee865eef5007d6d870618e032047e1e`.
Classification CODE_OR_HARNESS_CHANGE against the former main is expected because
the merge includes all W2B changes. FAST gate 108.879 seconds; job 17:20:46–17:22:43 UTC
on 2026-09-12. `qualification_executed=false`. The only executed profile was FAST.
[Bound receipt](closure/merge-receipt.json), [raw log](closure/merge.log.gz),
[workflow run](closure/merge-run.json), [jobs](closure/merge-jobs.json),
[checks](closure/merge-checks.json).

Earlier successful FAST runs remain historical: qualified product push
[34705094573](https://github.com/Gustavo2358/cobol-lower/actions/runs/34705094573), PR
[34705113765](https://github.com/Gustavo2358/cobol-lower/actions/runs/34705113765);
final review docs push
[34705554076](https://github.com/Gustavo2358/cobol-lower/actions/runs/34705554076), PR
[34705556067](https://github.com/Gustavo2358/cobol-lower/actions/runs/34705556067).
No auto-merge, rebase, squash, force-push or manual workflow was used.

## Administrative blocker

The reviewed `history_errors` implementation in `scripts/harness/checks.py` requires
exact paths `docs/quality/WORK-LOWER-011/CP0.json` and `CP0-manifest.yaml` for historical
registration. W2B's post-commit LOCAL_QUALIFICATION authority contains neither legacy
certificate. The reviewed `ci_policy.documentary` explicitly treats new CP JSON/YAML
files in quality as protected. Creating them would change the protected map and
classify the administrative commit CODE_OR_HARNESS_CHANGE, invalidating the required
documentation-only relation. Reusing the qualification bundle through documentary
history paths fails the existing checker with `final evidence path`.

[Factual blocker](closure/closure-blocker.json) and
[isolated archival probe](closure/lifecycle-probe-result.json) record the contradiction.
The probe used a separate documentary directory and ran only the existing read-only
history checker; it created no legacy certificate and altered no live product bytes.
The original receipt, archive and executable contract remain untouched.
The legacy manifest schema also fixes git.merge_authorized to false; its value is
preserved, while the explicit human merge override is recorded separately. The
initial administrative docs check rejected setting that schema field to true; no
schema change was made. The diagnostic was shown by the tool; raw stderr was not
redirected. Its [explicit observation](closure/admin-docs-initial-failure.json)
and the limitation are retained. The corrected documentary check passed.

Therefore the five active files and registry entry remain explicitly `blocked`;
backlog remains `in_progress` for administrative archival only. This neither reopens
product work nor authorizes a harness repair. Separate human authorization and review
are required to adapt historical LOCAL_QUALIFICATION registration and its documentary
policy before archival can pass truthfully. No source or policy change was made here.
[Current state](../../work/active/WORK-LOWER-011/state.md).

## Frozen delivered semantics

```text
Closed
Branch(Unknown BOOL)
 ├ TRUE  → Assign PROGA → Jump ┐
 └ FALSE → Assign PROGB → Jump ├→ Invoke → Return

Open
Branch(Unknown BOOL)
 ├ TRUE  → Assign PROGA → Jump ┐
 └ FALSE ──────────────────────┤→ Invoke → Return
```

Predicate type BOOL; value UNKNOWN; dependencies are published whole-item reads;
remainingReads none (NoMemory); runtime truth NOT EVALUATED. FLAG is not evaluated,
no path is pruned, absent ELSE creates no unknown write, no solver runs, and no
dependency is resolved by this lowering.

SP IndependentStorageSet → AIR Premise(DisjointStorage). `cobol-lower DOES NOT prove
storage independence`; it translates the upstream proof and its provenance.
Outputs remain STRUCTURALLY_VALID / PARTIAL. Semantic obligations I-59, I-09, I-56
and I-23/I-56 remain open; validator/codec acceptance does not prove all semantics.

W1 regressions remain frozen: SP 1.3, literal CALL, computed CALL, fitted MOVE,
`PROGA   ` including padding, Invoke, Return, UnknownName, UnknownContract, open
effects/outcomes and historical AIR bytes. No new W1 goldens were generated.

| Frozen upstream | Commit | Tree / version |
| --- | --- | --- |
| W2A proleap-poc | `4ffabded1aad39316b8a6f337f732976fdb3ca3e` | `5880e174b33c85ba3f3cdbc70bd2d8dc7b1d567b`; SP 1.4.0 |
| W2C air-java | `1d22068e9d9c1d100ecdef734e5b6995252e7ede` | `ab3a55e13b781ec5424356e83bddbc6fde42dcb1` |
| Normative analysis-ir | `51b4d9a8ae0364232bd97103cd73a77e1a34996c` | AIR 2.0.0; JSON binding 1.0.0 DRAFT |

Pins and sibling repositories were preserved. The pre-existing untracked files in
artefatos-e2e remain unchanged; preservation does not claim that sibling was clean.

## Future handoff — not started

NEXT: CP6 W2D — real consumer vertical. Its future cobol-lower product baseline is
the integrated merge `2b7fa3a5cee865eef5007d6d870618e032047e1e`, not the source branch.
Later documentary main commits preserve productive bytes but acquire no qualification.
No analysis-cfg change or downstream repin occurs in this task.

Future objective only: COBOL → SP 1.4 → AIR diamond → CFG → PossibleValues; closed:
PROGA + PROGB, modelValueRemainder=false; open: PROGA, modelValueRemainder=true;
then dependency edges and candidate supports. These are future consumer expectations,
not results executed by W2B closure.

NO SOLVER/LATTICE CHANGE EXPECTED. If future W2D needs solver/lattice changes merely
to support the simple diamond already proved at model level, human review is required
before changing solver/lattice.

REMOTE CI FAST ONLY. FULL QUALIFICATION LOCAL ONLY.
W2D NOT_STARTED / NOT_AUTHORIZED.
