# STAGE5-PARTIAL-FILE-ROUTES

Status: IN_PROGRESS

Frontend PR #77 now preserves FILE destination roles as UNKNOWN_LOCAL when
parser recovery did not prove normal control. This includes handlers whose
regions were not admitted. Lower production and contracts are unchanged;
this addition to Draft #52 updates the producer authority pin.

The actual dependency-input CLI is exercised with partial SPs, preserving
open frontiers and source-native FILE evidence. No guessed normal successor,
handler entry, complete coverage or runtime target proof is authorized.
The frontend work item and PartialFileTopologyTest define the adversarial cases.
Integrated qualification is recorded in analysis-cfg's companion work item.

## Qualification

- FAST PASS, 407.577 s: production, adapters, architecture and harness tests.
- 24 actual four-stage witnesses pass; 18 old frontend aborts removed. Six
  previously successful controls retain all five products byte for byte.
- Existing FILE composition oracle: 22/22 PASS, including matched PERFORM
  returns, I/O outcomes, aliases and callback limitations.
- Fresh frontend replay: all 560 SPs byte-identical, including CardDemo 73,
  PERFORM 39 and Chaos 48. All consumer bytecode is unchanged; the 2,240
  corresponding downstream products are reused explicitly, not rerun.
- Six oracle corruptions rejected. Source-native FILE qualifiers/provenance
  survive; partial input grants no new executable control or target proof.

The final repin follows a frontend documentation-only commit after the tested
production commit. Pin/document checks cover that final delta; semantic inputs
are identical. Full lower qualification was not rerun for this leaf producer fix.
Draft #52 remains open, with no merge. Integrated evidence and limits:
analysis-cfg `docs/work/stage5-partial-file-routes.md` and local E2E report
`artefatos-e2e/shared-routine-bodies-20260930/partial-file-routes/REPORT.md`.
