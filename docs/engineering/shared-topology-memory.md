# Shared topology bodies and dynamic completion

Domain: admitted canonical ControlTopology, explicit region entries, boundaries,
ordinary defaults and bindings. No COBOL text, spelling, source ordering or range
size participates. AIR 05.7.2/7.9 defines matched top-frame completion; SP topology
owns the exact endpoint and default. These contracts are the primary authority.

For non-inline bindings, one source graph is materialized independently of entry
and endpoint. Each body invoke chooses its published entry and declares only its
endpoint port. Region-entry targets resolve through published entries. COMPLETE
stops at the published boundary instead of resolving it using a static binding.
A LocalBoundary signals that boundary's port and otherwise follows its published
ordinary default. Nested completions remain separate boundaries. Endpoint ports
are declared in the AIR Unit. Explicit inline bodies retain their lexical escape
propagation, which removes frames according to their published containment.

Finite CICS support remains part of a source operation's state identity. Its
boundary resumeKey selects the completion phase for that support in the body
frame; it cannot resume a different caller. Return routes subscribe by endpoint
and handler mode. Each call keeps its binding premises on its own invoke/phase.
Shared body operations retain source/outcome/boundary premises, not an expanded
union of unrelated caller proofs. Proof nodes remain shared by their source ID.

Invariant: every executable source transition either resolves a published entry,
executes a published completion boundary, invokes a matched frame or retains its
explicit unknown frontier. The same source/support graph supports every entry
and endpoint. Removing static binding duplication changes identity representation,
not source effects or allowed continuations. The dynamic frame selects exactly
what static resolution selected when its endpoint is reached; intermediate
boundaries follow ordinary defaults. Concrete trace oracles and dependency/value
oracles compare source-correlated operations instead of representation IDs.

Termination follows finite source occurrences, boundaries, bindings, phases and
admitted support states; scheduling deduplicates their labels. For S source nodes,
B bindings, C completion boundaries and H distinct finite support states, graph
materialization is O(H*(S+C+B+declared routes/proofs)), excluding operation payload
size and the upstream topology inventory. There is no entry-by-endpoint body
product. Intrinsically large H, arbitrary declared routes and upstream expanded
membership inventories remain distinct problems; no polynomial claim for them.

Oracles: entry permutation, distinct endpoints sharing one body, nested helper,
empty/structured regions, explicit inline escape, loop phase payloads, matching
top frame only, CICS state selection, missing route and partial frontiers. Scale
families record sequence/ID growth and compare dependencies, supports, proofs and
remainders. Existing IDs may change because graph occurrences are shared; source
correlation, namespaces, uniqueness and determinism remain required.

The caller-proof oracle observes the concrete body point together with every
active invocation frame's origin closure. A sequential two-caller fixture has
exactly its own distinct source binding marker at each invocation of the same
body, and the union across executions retains both markers. A static union of
all callers on the body fails the correlation oracle: it attributes an unrelated
caller's proof to this path. Proof inventory, invocation origin closure and source
state derivation remain published; sharing never removes a causal marker. This
changes representation ownership, and reviewers must assess it alongside full
consumer provenance/support comparisons before closure.
