# R7-R4 — local state and nonexecutable dispatch assessment

Admission.handlerState exposes immutable lower-owned evidence after all known-fact validation and before the executable NOT_READY barrier. Invalid inputs have no analysis. Executable handler/event lowering and AIR publication remain unavailable.

# R7-R4 IBM language oracle — frozen before transfer implementation

Retrieved 2026-09-25 from IBM CICS TS 6.x/5.5 primary documentation through web search. Direct opens of three IBM TS pages returned retrieval errors; substantive text was available through the primary IBM search results. No TXSeries/CICS TX rule is imported. Raw retrieved state and program-flow search output is preserved.

## Language/runtime semantics

[HANDLE ABEND](https://www.ibm.com/docs/en/cics-ts/6.x?topic=summary-handle-abend) establishes a LABEL/PROGRAM exit, disables it with CANCEL (the default option), or reactivates a canceled exit with RESET. CANCEL is level-local. A syntactic PROGRAM target does not prove successful installation/security checks. Operand binding and activation remain distinct from dispatch.

[Abnormal termination recovery](https://www.ibm.com/docs/en/cics-ts/5.5.0?topic=applications-abnormal-termination-recovery) states that a new HANDLE overrides its predecessor at the same logical level. Only one exit is active there. [How it works](https://www.ibm.com/docs/en/cics-ts/6.x?topic=processing-how-it-works-abend-exit-code) says dispatch selects the current active exit, searching higher levels only when no local exit is active. CICS disables an exit on entry. This wave does not execute that entry/dispatch transition.

[ABEND](https://www.ibm.com/docs/en/cics-ts/6.x?topic=summary-abend) is an explicit abnormal-termination request. Its CANCEL qualifier bypasses HANDLE exits at all levels; it is different from HANDLE ABEND CANCEL. No ordinary successor is justified here. [DFHEPC](https://www.ibm.com/docs/en/cics-ts/6.x?topic=control-dfhepc) separately describes registration and its ordinary return. Exceptional global retry mechanisms are outside this abstraction.

[Program linking](https://www.ibm.com/docs/en/cics-ts/6.x?topic=control-program-linking) assigns LINK a new logical level and restores caller HANDLE settings on return. XCTL transfers within a level without returning. [COBOL subprogram flow](https://www.ibm.com/docs/en/cics-ts/6.x?topic=programs-flow-control-between-subprograms) places native CALL in the same CICS level. [Subprogram rules](https://www.ibm.com/docs/en/cics-ts/6.x?topic=programs-rules-calling-subprograms) distinguishes static/dynamic CALL and CBLPSHPOP settings. No source UnitKey proves a runtime level, and a generic CALL without a published handler-effect summary cannot guarantee preservation of local state.

RESET is documented for a canceled exit. The consulted documentation does not establish the needed behavior for every uncanceled predecessor. It does not justify restoring an older A after a new B has replaced it. [Exit routines](https://www.ibm.com/docs/en/cics-ts/6.x?topic=recovery-creating-program-level-abend-program-routine) supports RESET after automatic cancellation but gives no permission to choose a nearest textual registration.

## Chosen analyzer abstraction and frozen laws

Start ENTRY_UNKNOWN, never NONE. Successful ACTIVATE replaces all previous local alternatives. Successful CANCEL maps a known active/saved target A to CANCELED(A); unknown history to CANCELED_UNKNOWN (local inactive, saved history unknown). Qualified RESET of CANCELED(A) restores A. RESET of CANCELED_UNKNOWN retains unknown history: no proof that any prior exit existed, so neither a definite target nor definite active status is invented. RESET of an ACTIVE/unknown uncanceled predecessor produces localized unknown with a reason; no textual lookup. This intentionally incomplete RESET rule is not asserted to be IBM's full runtime behavior.

Frozen second-activation oracle: ACTIVATE(A), CANCEL, ACTIVATE(B) kills A's saved registration; before RESET = ACTIVE(B). RESET has no established canceled predecessor; afterward = localized UNKNOWN, candidates not invented, A forbidden. A further ACTIVATE(C) kills that uncertainty.

Transfers occur only on published positive ordinary outcomes; UNKNOWN_LOCAL stops propagation. ABEND reads the incoming state and stops, even if a malformed future topology suggests fallthrough. BYPASSED suppresses candidates, preserving the incoming state evidence. Known resolved local LABEL intercepts the eligible event with no unjustified outer remainder. Canceled/possibly inactive alternatives retain an outer-level remainder; unknown current targets remain unknown. PROGRAM is preserved as typed target information, never a local label or dependency; actual producer PROGRAM registrations currently have no ordinary proof, so their outgoing state is not propagated.

This is analysis of explicit published paths/events, not an exception-CFG or full runtime CICS model. Unknown CALL effects are localized on their proved return route, never resolved by program name. No implicit event, enclosing-level search, value analysis or runtime probability is introduced.


## State domain (frozen design, qualified below)
Finite correlated atoms: ENTRY_UNKNOWN; ACTIVE(target); CANCELED(target); CANCELED_UNKNOWN; UNKNOWN with a localized cause (RESET without proved canceled history, unmodeled native CALL effects, unavailable operation). Bottom is empty/unreached, not an empty runtime handler. ENTRY_UNKNOWN includes possible local absence.

A target is lower-owned, derived only from the typed handler. LABEL canonical identity is ProcedureId; targetEntry and operand, declaration, entry and activation provenance remain evidence. Duplicate registrations for the same ProcedureId do not create distinct semantic targets. PROGRAM literal/DATA stay typed, and unresolved targets never enumerate labels. Their absent ordinary proof in today's producer prevents propagation.

The result retains operation observations, BEFORE/AFTER atoms, event assessments, matched context and finite causal relations. Atom sets are joined by union; a transfer maps each incoming atom independently. ACTIVATE is a constant replacement function on each nonempty input. This is monotone over sets despite killing each predecessor's registration. CANCEL preserves correlation with saved target. No independent active/saved sets, global current handler, or ProgramUnit-to-runtime-level mapping.


# Transfers — frozen before production
Only along a positive NORMAL outcome of a reached CICS_HANDLER:

| Operation | Incoming atom | Outgoing atom |
|---|---|---|
| ACTIVATE target T | any | ACTIVE(T), replacing prior active/saved history |
| CANCEL | ACTIVE(T) / CANCELED(T) | CANCELED(T) |
| CANCEL | unknown history | CANCELED_UNKNOWN |
| RESET | CANCELED(T) | ACTIVE(T) |
| RESET | CANCELED_UNKNOWN | UNKNOWN(RESET_HISTORY_UNAVAILABLE) |
| RESET | ACTIVE(T) / other unknown | UNKNOWN(RESET_WITHOUT_CANCELED_EVIDENCE) |
| UNAVAILABLE | any, if an independent ordinary path is published | UNKNOWN(OPERATION_UNAVAILABLE) |

If no positive ordinary successor/completion exists: no outgoing transfer, no guessed next occurrence. ABEND is a BEFORE query with no successor. Ordinary local source effects preserve the handler dimension; native external CALL cannot guarantee preservation without its absent context/effect summary, and creates a localized state remainder on its already-proved continuation. CICS LINK return preserves the caller's level by IBM rule; no callee analysis or caller level is constructed. Unknown control is a stopped frontier, never a traversal route.

Frozen oracles: replacement B only; A-CANCEL no active A plus outer remainder; A-CANCEL-RESET active A; A-CANCEL-B-RESET unknown (A forbidden); branch A/B both; known A + entry unknown keeps A/remainder; dead B absent; duplicate A same target; ABEND CANCEL bypasses regardless of prestate; loops converge; shared/nested PERFORM returns preserve caller correspondence. PROGRAM/unresolved normal frontier must not be crossed.


## Qualified execution topology
Use the existing TopologyBinding algebra for primary entry, region entry and completion resolution. The same published binding determines range, endpoint, phase entry/completion and caller resume. Ordinary region completion without a binding follows the published boundary ordinaryDefault. Never consume programPoint, source file order, target spelling or targetEntry as an execution edge.

Finite tabulation contexts: ROOT or (LOCAL_INVOKE binding identity, incoming handler atom). Each call subscribes its actual caller context to exactly that callee input summary. A reached callee RESUME publishes a summary output atom only to subscribed callers; the resumed target is resolved in that caller's binding context. Nested/recursive invocations are memoized, never cloned by call history. Phase BODY enters the published bound region; a matching endpoint enters completionPhase; predicate phases use both published roles (no new value solver), effect phases preserve handler state, RESUME returns to its subscribed caller.

This is a consumption of ControlTopology, not a second source-CFG builder. A call resume is not a bypass: it receives no state until positive completion is proved. ABEND and UNKNOWN_LOCAL never complete the binding. Recursion has a finite summary domain and cannot fabricate return by assuming it.

The cross-return gate passed: two differently registered callers of the same P, including nested calls, do not receive the other's registration. Existing topology supplies the required authority; no expansion was needed.


# Fixed point — pre-implementation argument
Reference: [Reps, Horwitz, Sagiv, Precise Interprocedural Dataflow Analysis Via Graph Reachability](https://research.cs.wisc.edu/wpis/abstracts/popl95.abs.html). The read primary abstract supports finite distributive facts and matched interprocedural paths; it rules out flattening returns or enumerating histories. This implementation must prove its concrete simpler tabulation, not claim certification by that paper.

Index immutable statements/topology once. Work item = (memoized context, occurrence/phase, atom). Process each new tuple once; union at joins. Calls subscribe to a context keyed by binding and input atom; summary output insertions wake exactly those callers. Finite targets/causes imply finite atoms A; bindings B imply at most 1+B*A contexts. State storage is bounded by context locations × A; propagation cost by reached context edges × A plus finite summary/return joins. Worst-case context-sensitive cost can include additional A factors; no claim of a strictly context-free E*A bound. With fixed finite atom/context domain, work is proportional to the graph processed, never execution-path count.

Monotone insertion reaches a least fixed point; constant replacement transfers do not union the old state into their output. Evidence relations have finite tuple endpoints and identities. Different queue insertion/removal orders must produce equal normalized semantic results; telemetry order-sensitive counters/timing are compared separately.


# Nonexecutable dispatch assessment — frozen design
For each typed ABEND: owned event identity, eligibility, reached BEFORE atoms, known typed candidates, activation/provenance evidence, local target remainder, local inactive possibility, outer-level remainder, bypass status and analysis status. UNREACHED in the published graph is distinct from a runtime impossibility or an empty handler. Frontiers are listed explicitly.

BYPASSED: candidates empty regardless of incoming active state; no handler search assessment. HANDLER_ELIGIBLE: ACTIVE resolved LABEL yields that positive candidate; known CANCELED yields no active candidate, local inactive true, outer remainder true. Unknown history retains local target/inactive/outer possibilities. Multiple active targets survive union; known + unknown preserves both positive candidates and honest remainder. Unavailable eligibility does not assert eligible dispatch.

No assessment creates an executable destination, AIR instruction, CFG edge or PROGRAM/FILE dependency. All handler/event documents remain NOT_READY and publication empty. The result is available on the lower's admission evidence after consistency validation and before readiness. Contradictory input does not run the engine.

Causal relations cite published outcomes/proofs, matched calls/returns, operation transfers and joins. An activation is an evidence producer, never a direct edge to the label. Candidate support must terminate at a reaching ACTIVATE through the recorded causal relations; an overwritten registration cannot be used as current support merely because it appears earlier in the input.


## Implemented finite supports and limits

The implementation separates semantic State from Support(state, activation StatementId). A reaching definition is not a trace. Repeated LABEL activation has one semantic ProcedureId target; replacement kills the earlier activation support. This extra finite provenance dimension also belongs to the invocation summary input, preventing causal definitions from leaking across callers.

Let H be activation occurrences, N topology locations, E outcomes and phase edges, B bindings. There are O(H) correlated provenance supports and at most 1+B*O(H) contexts. Cost is proportional to reached context-edge/support pairs plus matching summary/subscriber combinations (polynomial worst case). It is not an unconditional O(E*T) bound independent of contexts. Each distinct (context,location,support) is popped once. No recursion depth limit, execution history, path enumeration or CFG cloning is used. Indexes are built once; handler transfer never scans all statements. Canonically sorted results, causal derivations and deterministic counters agree under FIFO/LIFO worklists.

State is consumed only on existing topology relations. Source unit ownership is retained without runtime logical-level identity. PROGRAM and unresolved LABEL remain frontiers with the current producer: typed targets are retained, but no post-state or target edge is invented. A returned native CALL has unknown handler effect; a returned CICS LINK retains caller-level state per the IBM logical-level rule. ABEND queries pre-state and produces no successor. Unreached event means NOT_REACHED_IN_PUBLISHED_TOPOLOGY, never proof of runtime impossibility.

Each derivation stores the reached source/destination nodes, authority/proof IDs and caller premise for matched return. Candidate activations are actual reached successful registrations, and target records retain operand/statement/include provenance. The evidence graph has finite size and lets review reconstruct replacement, cancel/reset and joins without storing paths.

## Qualification

HandlerStateSuite consumes authentic frontend fixtures from 1db76b3525588aa41438acd893bf51273e650909. Sources and hashes are checked in. The suite covers replacement, cancel/reset, partial RESET history, joins, loops, dead code, copy, independent units, nested/recursive PERFORM, bypass and validation-before-analysis. Ten metamorphic relations and ten isolated wrong-code mutations are recorded in campaign evidence. All historical suites remain unchanged. No AIR/CFG change, dependency producer, enclosing-level model, ALTER, R8 or R9.
