# Frontend control topology — SP 2.39.0

The frontend owns COBOL control structure. The closed Semantic Product transports
that structure; lower binds and materializes it without discovering paragraph,
arm or range completion from legacy statement facts.

## Authority and migration

`controlTopology` is required in 2.39.0. Its authority is
`FRONTEND_CONTROL_TOPOLOGY_R1`. Versions through 2.38 retain their historical
interpretation and reject the new field. New wire without topology is rejected.
The producer's branch already contained 2.38 before this wave. Historical State
constructors and typed ports remain available and do not acquire topology.
New and legacy assemblers are selected once. Legacy control fields remain useful
for compatibility and operation payloads, never as an alternative authority for
completion in the new assembler. The initial executable statement is resolved from
the root PROCEDURE region; entryInventory retains signature/identity metadata and
a published known start must agree with that root. A mismatch is rejected before AIR.
An unavailable legacy start does not veto a known topology root. Exactly one
primary entry identity is still required, and an unknown topology root remains
blocked. Input/signature gaps and UNKNOWN_LOCAL outcomes remain explicit; no
continuation or dependency value is inferred from the missing legacy metadata.

## Closed algebra

Occurrence identity reuses `statement:N`. Region identities use frontend AST
identity plus role; they do not depend on transport ordering or program names.
Every region owns an explicit boundary. Root procedure, paragraph, IF/EVALUATE
parent and arms, FILE parent and handlers, inline body and invocation range are
first-class records. Declarative sections are isolated from ordinary flow.
A RANGE references ordered existing regions and its binding names the last
boundary as endpoint. The range does not duplicate source occurrences.

Targets are OCCURRENCE, REGION_ENTRY, COMPLETE(region), PROGRAM_RETURN or
UNKNOWN_LOCAL(region). Outcomes distinguish NORMAL, BRANCH, EXPLICIT_TRANSFER,
LOCAL_INVOKE, PROGRAM_RETURN and UNKNOWN_LOCAL. COMPLETE is symbolic: the same
paragraph has an ordinary default and can complete the matching active binding.
A THRU intermediate boundary follows its ordinary default; only the final active
endpoint resumes the caller. Arm completion composes through the parent boundary.
An explicit transfer/return does not acquire a completion edge.

Bindings carry their own resume and finite phase graph. BODY and RESUME are
endpoints; predicate/effect phases publish routing explicitly. ONCE, BEFORE/AFTER,
TIMES and single-level VARYING use this graph. Nonpositive literal counts retain
the historical unavailable-count capability frontier; their resolved range stays
inventoried without an executable binding or resume bypass. Value/effect precision is separate
from routing. Inline repetition may retain unknown predicate/effect values.

## Proof and integrity

Every region, boundary, outcome, target and binding cites proof identities.
Proofs distinguish local grammar, resolved targets, expanded includes, input
isolation and partial unknown knowledge; provenance retains expanded/original
locations and include chains. Missing DATA input does not erase independently
closed procedure syntax. Missing procedure structure stays unavailable.

The same occurrence/outcome inventory drives reference closure and emission,
including event-specific FILE handlers. Targets are bound in the same activation
as the emitting operation. Invalid identities, missing targets, inconsistent
ownership/endpoints, malformed phases and symbolic alias/proof cycles reject
before AIR. Executable control loops remain representable.

## Partiality and scope

UNKNOWN_LOCAL identifies the region whose source control knowledge is unavailable.
It is not a guessed set of destinations and does not claim source impossibility.
The current AIR backend preserves an explicit unavailable projection frontier:
UNSUPPORTED coverage, control UNAVAILABLE, proof/context origins, and an open
control envelope with no licensed labels. No return, divergence, fallthrough,
all-label scope or synthetic caller bypass is inferred. CFG exposes the open
frontier; dependencies distinguish completed computation in the known model from
incomplete source coverage. See AIR 00 §5, 05 §6 and 06 §§1,3.2,8.

Source occurrences outside the entry projection remain explicit coverage items;
unreachable activation trees are not expanded. Recursion retains its existing
unsupported local frontier, without a return bypass. Section-target PERFORM,
special EXIT, multilevel VARYING and unresolved callbacks remain explicit limits.
USE/SORT callback bindings are not implemented in this slice: source occurrences
remain inventoried, unavailable routes are explicit, and SORT cannot bypass a
required callback. The pinned CardDemo 73 has no USE/SORT local callback plans.

## Validation and review

Contract tests cover old/new separation, malformed wire and in-memory parity,
roundtrip, physical permutation and reference mutation. Region tests cover terminal
verb substitution, ordinary versus performed entry, THRU endpoints, two callers,
nesting, predicate/input gaps and FILE handler inventory/event separation.
Real source witnesses, external full73 redistribution and backend scale remain
mandatory campaign gates; unit tests alone do not qualify the wave.

## FILE executable target authority (R1-R1)

For SP2.39, ControlTopology is the sole executable target authority. FILE
`fileInventory` retains domain facts: operation identity, event/effect association,
handler metadata and source evidence used by the frontend when building topology.
Each destination slot has exactly one published outcome role
`file/<use ordinal>/<event>/<destination ordinal>` on its source occurrence.
The producer verifies that a handler outcome names the corresponding FILE_HANDLER
region and that the region entry is the source handler entry.

The consumer validates FILE domain shape and role coverage, but it never obtains
an executable target from the legacy handler member order or continuation. The
source reference closure and the emitter both resolve the published outcome.
There is no consumer fallback or target equality reconstruction against FILE
metadata; an absent role is invalid input before AIR. Unsupported callback
knowledge remains UNKNOWN_LOCAL. Historical SP contracts retain legacy routing.

Permanent tests: producer FileTopologyAuthorityTest checks every destination and
its handler entry; lower FileTopologyAuthoritySuite reverses legacy handler member
metadata while retaining topology and requires identical AIR modulo publication
namespace. It removes each FILE outcome and its occurrence reference, leaving an
internally well-shaped graph, and requires rejection before AIR through both wire
and typed ports. The unchanged ControlTopologyAuthorityTest is in fixed FAST.

### Outcome role cardinality (R1-R1-F03)

Each occurrence has at most one outcome for a given role. IDs distinguish records,
not competing interpretations of a control slot. This invariant applies to all
outcomes, including FILE destination slots; alternatives have distinct roles.
The same role on different source occurrences is valid. Duplicate roles reject
before AIR even when their targets agree, regardless of outcome identity or
physical inventory order. Producer and consumer typed constructors validate this
invariant; the new wire decoder uses the same consumer validation. No target is
recovered from fileInventory, and no verb or program exception is introduced.

SP2.39's shape and authority are unchanged. This corrects acceptance of malformed
input under the existing single-outcome role contract; it does not reinterpret
historical SP versions or introduce R2 fact locality. Permanent FILE authority
tests cover FILE and non-FILE duplicate roles, equal/conflicting targets,
first/last identities and reversed inventory, with typed/wire rejection parity.

### Reached interaction versus outgoing frontier (R5)

An all-UNKNOWN_LOCAL XCTL retains its independently admitted typed payload and the
same empty open control frontier. Copies of that context-independent sink share
one unit/source occurrence label; source transitions are unchanged modulo this
identity quotient. No continuation or handler is inferred. See
[cics-program-control](cics-program-control.md#typed-occurrence-at-an-unavailable-control-frontier-r5).


## Bounded local CICS routes (R6)

A proved XCTL local condition destination is transported as a finite open AIR
control bound alongside external uncertainty. It is not a successful normal
return. Context resolution remains owned by this topology; source text and
legacy continuation metadata are not routing authorities. See
[CICS control](cics-program-control.md#r6--bounded-local-xctl-condition-control)
for the rule, limits, complexity and tests.

## SP 2.48: scoped exits, SECTION and VARYING levels

See [PERFORM control completion](perform-control-completion.md). `ESCAPE` is admitted
only as an explicit occurrence outcome to a lexical enclosing paragraph or inline
body. Lowering unwinds intervening inline contexts to that scope and uses its own
endpoint/resume binding. Handler summaries propagate the same escape until the
owning context, without treating a called paragraph's EXIT PERFORM as a dynamic exit.
`SECTION` entry and completion are ordinary published regions; no CFG changes are
required. Positive phase levels select independent typed predicates and controls,
with distinct operation identities and source origins for each level/reset.

AIR retains existing jumps, branches, invocations and open numeric effects. The
existing integer operand admission applies separately at each level; a missing or
unsupported operand produces explicit partial effects with a proved continuation.
The decoder rejects 2.48 fields under older contracts. Legacy phase level 0 keeps
its existing interpretation. Historical paragraph-profile diagnostics do not
replace the topology's control authority.

## FILE composite control — SP 2.51

`FRONTEND_CONTROL_TOPOLOGY_R2` adds `fileFlows` and the `FILE_POINT` target.
Other topology rules are inherited from R1. The writer selects 2.51 only when
a nonempty composite flow is published; empty inventories are omitted for R1.
Each flow owns an existing source occurrence and points keyed by opaque IDs.
USE points bind fileInventory ordinals; CHOICE points publish finite aggregate
alternatives. USE has one ordinary target. Event outcomes remain the authority
for each result (including handler/UNKNOWN_LOCAL routes and critical exits).
Only same-owner FILE outcomes/points can target a FILE_POINT. Regions, boundaries
and invocation resumes cannot enter halfway through another statement.

The consumer checks use inventory equality, ownership, proof provenance, entry,
references and reachability of every point in the ordinary internal graph. These
points never enlarge the COBOL occurrence inventory. It binds them with the
current activation, including PERFORM endpoint and CICS state context. The
handler-state analysis traverses the same published points, retaining derivations.

OPEN/CLOSE chains visit successive operands before completing their statement.
SORT/MERGE without procedure callbacks publish INPUT → WORK → OUTPUT; selection
loops retain unknown participant order/count. Critical-error remainders and
unknown USE/SORT callbacks remain explicit. No physical FILE metadata becomes
an alternative source of executable destinations. Earlier contracts retain
their historical routing. See [work item and oracles](../work/file-composite-control.md).

The structural-only port may omit I/O event routes. Its USE point then supplies
ordinary continuation directly. This does not admit memory effects or close error
coverage. SP2.51 still requires a locality inventory: an unavailable physical-profile
input and empty facts/bindings express absence of storage analysis explicitly.
An unavailable FILE event plan with no continuation is not a contradictory claim.
An explicit event continuation is still validated against structural completion.

## SP 2.53 — área BMS implícita e controle independente de memória

CICS_COMMAND admite `implicitArea` como DataReference opcional resolvida no produtor: RECEIVE_MAP WRITE, SEND_MAP READ, área inteira, owner do statement e provenance derivada (`exact=false`) do MAP literal. Exclui INTO/FROM/SET explícitos. Binding ausente/ambíguo não é publicado como selecionado. As opções escritas não são modificadas. O wire exige 2.53 para esse campo e conserva os perfis anteriores. SourceContinuations pode estar vazio quando a nova versão decorre apenas dessa capacidade.

Destinos da ControlTopology independem de footprint físico. Lower conserva o efeito MAY aberto para referências não admitidas, nunca MUST; comandos sem destino provado continuam sem sucessor inventado. INITIAL concede existência de alocação, não seed forte. DECLARE TABLE reconhecido não aloca storage; INCLUDE desconhecido permanece input indisponível. [Regra e qualificação W3](../work/carddemo-control-w3.md).

## SP 2.54 — catálogo CICS fechado

CICS_COMMAND acrescenta ASKTIME, FORMATTIME, ASSIGN, INQUIRE_PROGRAM, SEND_TEXT e WRITEQ_TD. Cada família conserva opções, direção host e LENGTH estrutural; nenhum efeito implícito dessas famílias autoriza hostEffects fechado. Conhecimento de controle permanece na topologia. NOHANDLE duplicado sem operando conserva opções e warning CICS_COMMAND_DUPLICATE_FLAG_IGNORED; a paridade warning/duplicação é validada e a capacidade exige SP2.54. [Regra, fontes e testes W4](../work/carddemo-control-w4.md).

## SP2.56 — sentenças, busca e término

SENTENCE compõe uma fronteira do período e aceita ESCAPE lexical, usando a mesma pilha de contextos e caminho de handlers já usados por EXIT PARAGRAPH. SEARCH/SEARCH_ARM representam a decisão abstrata e seus corpos; SEARCH_INDEX_MAY conserva memória desconhecida sem MUST. PROGRAM_HALT materializa Opaque com HaltAlternative e NoControl, com memória/recursos de finalização abertos; não é Return. Versões anteriores não admitem as novas capabilities. [W6](../work/carddemo-control-w6.md).

## SP 2.57: source reentry policy

Binding.reentryPolicy is producer authority. When the same binding is already
active, SOURCE_UNDEFINED yields `cobol-lower:LOCAL_REENTRY_SOURCE_UNDEFINED`.
The finite active-binding guard preserves an open frontier and existing effects;
it does not emit a return, halt, divergence or kill. UNSPECIFIED retains the
historical unsupported-recursion diagnostic. The decoder requires the field in
2.57, rejects invalid/null values and rejects specified policy on older contracts.
Sequential reuse after a completed activation is valid and retains its own resume.
No paragraph-name or source-text inference is used. The AIR local-control extension
cannot grant source semantics absent from the producer.

Expansion still materializes contexts and may be exponential in distinct active
bindings. This change does not implement recursion summaries or claim that all
CFG nodes become reachable. [Decision and limits](../work/carddemo-control-w7.md).
