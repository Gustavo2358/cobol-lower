# Compositional PERFORM — Positive Memory Topology W6

The lower consumes the SP's positive local-invocation facts independently of body
precision. Admitted legacy BASIC/range/repetition normalization remains unchanged.
A separate compositional route consumes STRUCTURAL_FACTS and previously unadmitted
paragraph ranges, including independently supported repetition. It uses AIR core Jump/Branch/Invoke/Return and existing statement handlers;
no new AIR capability, CFG heuristic or dependency-consumer rule is required.

## Authority and premises

SP 2.36 `targetEntry`, ordered `procedures`, membership, conditional `completions`
and callsite `normalContinuation` are the authority. Lower never reads source or
infers edges from names, ProgramPoint, array order or diagnostics. ProgramPoint
only stabilizes serialization. Paragraph order is explicitly part of the SP range.
AIR 2.0 control §05.1–5, producer obligations PROD-01/02/03/04/07/09/10 and local
return oracles O-56–58 require exact pairing and shared memory. The fixed input
contract is pinned in sources.lock.json; W6 does not change its wire version.

Full qualification authorizes whole-profile specialization; it does not authorize
the existence of independently established entry or completion facts. Missing
loop/count/varying semantics cannot become ONCE. The compositional route admits
only known validated loop predicates, POSITIVE_INTEGER/INTEGER_ITEM counts and
executable single-level VARYING. Already admitted legacy repetition stays in its
route; both routes reuse the same repetition wrapper.

## Control model

An activation context is the enclosing context followed by the current callsite's
StatementId. Operation, operand, label, uncertainty and occurrence coverage IDs
are specialized; data objects and storage identities are shared. Source origins
remain correlated to the written occurrence, with derived activation/return rules.
The previous sibling identity scheme is retained at depth one. Extending, rather
than replacing, the parent context prevents conflation of repeated nested calls.

The invocation Jump enters a distinct target occurrence. Each activation owns a
normal-resume block. A published paragraph frontier supplies that paragraph's
next entry or this resume block as the statement's *normal* destination. The
handler preserves its other control: CALL uses Normal, IF/EVALUATE use their
published arms, GOBACK remains Return, GO TO remains an explicit transfer. No
frontier means no new return claim. A neutral effect alone is not a frontier.

A nested PERFORM returns first to its own resume block. That block resumes the
parent's known successor; when the nested occurrence is a parent frontier, it
transfers to the parent's next paragraph or resume block. The intermediate
composition remains explicit even for a terminal inner occurrence with no
concrete source successor. No completion connects to all callers.

The CALL contract can publish an ordinary continuation outside its paragraph as
well as a conditional paragraph frontier. The activated occurrence selects the
frontier; the ordinary occurrence retains the ordinary destination. Validation
allows this specific coexistence but rejects a frontier whose successor remains
inside the same paragraph, GOBACK/GO TO frontiers and inconsistent references.
FILE/CICS retain their separate intrinsic/ordinary contracts and checks.

## Partial graphs and termination

CompositionalPerformAdmission computes a finite closure from targetEntry and
published members using only explicit statement successors and arm/GO TO entries.
A visited set handles source graph cycles. This closure is an execution projection,
not inferred paragraph membership: only published paragraph completion IDs gain
return destinations. Entry without procedures can therefore expose a CALL BEFORE
while leaving return unavailable. Ordinary source occurrences remain separately
published when legacy isolation did not remove them; their presence is not an
entry edge or reachability claim.

Assembly expands nested activations using their complete context and an explicit
task deque, without recursive Java calls. Re-entering a
callsite already in that context produces the existing bounded partial terminator
with `RECURSIVE_PERFORM_NOT_SUPPORTED`; it does not create a resume bypass, grow an
unbounded stack or claim exact recursive execution. The finite prefix preserves
positive entries already executed. Diagnostics describe this refusal, never select
execution edges. No arbitrary PERFORM count or nesting-depth cutoff is introduced.

Planning is O(P*(N+E)) in the worst case for P source activations and explicit
source graph size N+E. Assembly is proportional to emitted statement occurrences
and context descriptors. Flat A activations over B statements emit O(A*B) work;
loops are not unrolled. An acyclic shared call DAG can still have many distinct
contexts, so there is no claim of a polynomial source-size bound for arbitrary
nesting. Evidence measures 1/2/5/40 activations and depths 1/2/3; expansion count,
AIR blocks, CFG nodes/edges, wall time and peak process RSS are reported separately.
A future requirement for compact shared recursive/context graphs would need
summaries or end-to-end control.local, not a silent truncation of this model.

## Tests and scope

CompositionalPerformSuite uses source-produced SP snapshots and checks exact
per-callsite resumes, nesting through all intermediate frames, source permutation,
AIR codec round-trip, gap independence, missing membership, recursion refusal,
neutral/special endpoints, dead code and multiplicity. It runs in FAST. Integrated
source tests assert exact dependency candidates, 34 context separation, 32 peer
isolation, 39 DEAD exclusion, and all frozen w4-b.2 expectations without edits.

CALL operand coverage now distinguishes activation occurrences. This also fixes
09's preexisting duplicate-coverage OUTPUT_INVALID as a byproduct of supporting
08; no separate 09 semantics were added. FILE/SORT inventory tests distinguish
ordinary and activated occurrences and assert contextual reachability.

No inline/SECTION/EXIT PERFORM/EXIT PARAGRAPH execution, multilevel VARYING,
recursive-return support, broad control/memory fallback or source rediscovery is
introduced. General ordinary/IF/EVALUATE topology reconciliation remains W7.

## W6-R1 review corrections

Occurrence operand coverage includes activation identity in CICS PROGRAM target
and options, CICS FILE, and predicate reads as well as CALL/MOVE. Written origins
and COBOL storage remain shared; no coverage item is discarded or validator rule
relaxed. Source-produced nominal and regional CICS fixtures exercise actual
operand materialization, ordinary copies, and one/two activations.

PerformRepetitionAssembler is the existing SP2.3/2.4/2.5 topology extracted from
the legacy assembler. Positive TIMES enters the body at least once; INTEGER_ITEM
has an entry zero/body branch and reads its count only there. Exhaustion is an
unknown decision, independent of the exact positive iteration count. UNTIL BEFORE
enters the decision, AFTER enters the body. Single-level VARYING retains localized
initialization/increment and AFTER's exit bypasses the increment. Body completion
feeds this wrapper; its exit feeds the activation's explicit resume, including a
parent-supplied completion frontier. No numeric evaluation, unrolling, multilevel
support or new INITIALIZE effects are implied by body composition.

PerformActivationDemand is a scheduling upper bound, not an executable control
analysis or source membership inference. For each context it follows explicit SP
successors, arms, GO TO entries and that context's published completion routing
from the declared entry. A call's normal successor is a may-return scheduling
bound; it does not become an AIR bypass. Every ordinary statement remains in the
source inventory. An undemanded PERFORM is inventoried with the localized
ACTIVATION_NOT_MATERIALIZED_IN_ENTRY_PROJECTION gap and no invented control.
This describes the declared known-entry projection, not universal source deadness
or absence of unmodeled alternate entries.

When `fileInventory.operations().uses()` is nonempty, scheduling remains eager.
This includes **any published FILE operation**, not only proved declarative/SORT callbacks. This is an explicit optimization limit,
not an expansion of control or a gap-code semantic branch. Existing FILE semantics
and negative controls remain unchanged. Demand conservatively retains some
activations after calls which ultimately never return; precision of scheduling is
not needed for execution correctness.

The task deque removes JVM activation-stack depth from assembly. Source closure
and demand use visited sets; recursive PERFORM still ends at the explicit local
gap. Cold shared DAGs no longer expand dead invocation trees in the supported
scheduling graph. Live shared DAGs retain output-sensitive static context expansion
and can reach operational resource limits. No universal polynomial bound, hidden
cutoff or exact-result claim after truncation is made. R1 evidence measures staged
cold/live DAGs and deeper chains under external time/heap budgets.

CompositionalPerformRevisionSuite permanently checks CICS identity/correlation,
repetition zero/body guarantees in both nesting directions, AIR validation and
round-trip, inventory permutation, cold depth8 DAG inventory and live depth64
chain. Existing VARYING tests now distinguish unavailable repetition operands from
independent body/isolation gaps, while retaining localized must-write and recursion
refusal assertions. All frozen W4 sources/oracles remain unchanged.

W7 reconciles occurrence continuations and branch composition in [control-composition.md](control-composition.md), preserving these activation and repetition rules.
