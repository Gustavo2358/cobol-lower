# SP 1.4 simple IF diamond

CP6 W2B consumes the [locked productive W2A/W2C authorities](../sources/sources.lock.json).
Wire14 is closed and separate from Wire13. The core transports only public SP facts.
No AST, source parsing, name-based join, runtime truth evaluation or CFG projection occurs.

Admission requires one explicit root SIMPLE_TEXT_EQUALITY IF, one primary entry at
that IF, complete BOOLEAN/PURE/TOTAL/COMPLETE predicate facts with UNKNOWN truth,
whole-item uniquely resolved known read, THEN PRESENT/KNOWN and ELSE PRESENT/KNOWN
or ABSENT/KNOWN. Every arm child is an admitted scalar MOVE. Canonical branch child
order and each MOVE continuation must agree. The IF completion names the root CALL,
whose W1C normal continuation names GOBACK. All observed statements and declarations
must be accounted for. Partial, nested, unresolved and unsupported facts reject the
whole slice. Availability cannot become empty code or implicit fallthrough.

The algorithm indexes statements, declarations, references and branch relations once.
It validates membership and joins successors by published identity. Each arm is visited
once in canonical child order. ScalarDataTranslator supplies the object/cell map. The
upstream IndependentStorageSet must cover every relevant mapped declaration; each
published member maps through that existing map in its original order. Distinct IDs
never prove separation. Authority is copied; justification is `SP rule: ` plus the
published rule. Premise provenance is the upstream proof provenance.

Assembly allocates deterministic role/source labels before operations and returns an
explicit entry label. The serialization inventory starts with Return, demonstrating
that physical first sequence is not control authority. Branch holds an Unknown of
known BOOL type, ordered Read dependencies and NoMemory remaining reads. Its one
predicate uncertainty is VALUES only, scoped to that predicate occurrence. THEN and
present ELSE contain the real Assign operations and end in Jump to the shared CALL.
Absent ELSE uses FALSE directly to that CALL, with no extra sequence, write or value.
Invoke remains the W1C terminator with UnknownName, UnknownContract, open outcomes
and conservative effects. GOBACK remains Return. No fallthrough edge is inferred.

Source/derived origins retain IF, predicate/read occurrences, actual MOVEs and fitting
inputs, arm completion, CALL and GOBACK, and storage evidence. The lower copies the
published fitted text; it does not pad it or pretend derived spaces were written.
Publication identity includes canonical admitted facts, ordered arms/read/proof facts,
and their provenance. Input inventory permutations do not change output identity.

Termination follows finite indexed inventories; time and space are linear in statements,
branch relations, declarations, proof members and encoded fact text. No pairwise storage
proof, repeated sequence scan or repeated statement scan per arm is used. N/2N checks
run in local qualification only.

The independent consumer oracle observes public SP, public lower correlations and AIR.
It checks explicit entry/destination relationships, exact MOVE order, origin artifacts and
coordinates, all proof members (including unused extras), predicate dependencies and
uncertainty, and absence of invented state. It accepts arbitrary sequence inventory order.
The real AIR Validator traverses the publication and retains I-09, I-59, I-56 and related
semantic obligations. STRUCTURALLY_VALID does not discharge them; global coverage
remains PARTIAL. W2C codec preserves whole model equality and canonical byte re-encode.

Limits: one root simple IF, scalar text equality profile, admitted MOVE arms, one W1C
CALL/GOBACK merge chain. No generalized COBOL control, truth evaluation, path pruning,
solver/lattice or W2D. See [CI and qualification](../engineering/ci-qualification.md).
