# SHARED-ROUTINE-BODIES — stage 5 producer

- status: IN_PROGRESS
- scope: share proven equivalent PERFORM bodies using AIR local control; preserve specialized representations where context affects semantics. User authorized all checkpoints through completion without pauses, with separate PRs and no merge.

## Design fixed before implementation

Authority: existing Semantic Product control topology, AIR 2.0.0 §05.7 and O-56–O-60 (`analysis-ir@2c7f31f19efbe3211a2aea5bbda90173a9666fe2`). Body equivalence uses typed entry and completion endpoint; no names, source order, or textual matching. Each caller retains its own entry/count/varying/predicate phases. Its BODY phase invokes the shared entry with an exact resume to the caller's completion phase. Body completion uses local.resume; shared storage is unchanged.

Eligibility is a proof, not a corpus heuristic: entry CICS support is part of the body key, every transitive body occurrence provably preserves that support, and no additional exceptional/condition ingress is present; no ESCAPE outcome in the body or transitive invoked bodies; no cycle in the reachable binding-call relation. The remaining contexts retain the established materialization, including source-undefined reentry, nonlocal escapes and CICS registrations/dispatch. This preserves all prior behavior while avoiding an unproved abstraction of handler state or frame abandonment. General calls, IF/EVALUATE/GO TO, FILE and loop payloads inside admitted bodies keep their published destinations/effects. An acyclic body key is (entry occurrence, completion endpoint, entry CICS support, handler mode); binding proof sets are retained across equivalent calls. Canonical source correlations remain on original body statements and on each caller.

The eligibility fixed point walks each binding closure and its invocation references; no depth threshold, node quota, or fixture identity. Materialization emits one body per eligible key and one invoke per caller context; the consumer keeps pending returns and abstract states separate. Fallback envelopes stay conservative; the admitted local-control consumer executes the body effects and does not apply fallback havoc.

Independent acceptance: two sequential callers emit one body and two invocations with distinct resumes; nested caller values and completion endpoints do not mix; ranges with distinct endpoints remain distinct; EXIT PERFORM/PARAGRAPH, recursive and CICS cases preserve baseline semantics through specialization. Equivalent binding/occurrence permutations do not change semantics. Qualified source facts and dependency supports must survive the representation change.

Validation: focused RED/GREEN, existing PERFORM/control gates, full 560-case four-stage replay (all73 CardDemo,39 PERFORM,48 Chaos,14 aliases,25 PERFORM adversarial plus fixtures/focals), audited dependency deltas, measured AIR/CFG size; mandatory FAST and relevant local qualification. Work remains IN_PROGRESS until merge under repository policy.

## S4 producer checkpoint

The real-SP adversaries pass 13 assertions: two invocations share one body and have distinct resumes, CICS state-preserving bodies can share, different entry handler states stay separate. Existing PERFORM, terminal, alternate-entry and FILE test interpreters now carry local frames; their expected calls and continuations are unchanged. The qualified four-stage caller-values probe yields A only at the first continuation, B only at the second, and A/B at the shared call. The CICS replacement probe retains NEWPROG and does not resurrect OLDPROG.

Local full exposed a historical performance-oracle mismatch: SP2.61 normalizes two nullable Entry fields before the metered JSON shape walk. The independent original wire ledger remains 82+37N; the normalized walk is 84+37N. The decoder is byte-identical to baseline. Only that test ledger is corrected; DTO counts and exact decoded scale-input equality remain required.

That checkpoint preceded the qualification below; no merge was performed.

## S5 qualification — ready for review, no merge

- Lower FAST: PASS (`lower-fast-06.log`, 527.423 s). Full local wrapper: PASS (`lower-qualification-03.log`, exit 0), including semantics, 20,000-MOVE capacity, deterministic products, performance ledgers and architecture. The two historical input-node ledgers now include exactly two normalized nullable Entry fields; wire inputs, physical DTO ledger, source equality and production decoder are unchanged.
- All **560** real four-stage pipelines pass: CardDemo 73, PERFORM 39, Chaos 48, aliases 14, PERFORM adversaries 25, general fixtures 331, final focal contracts 29, frontier payload 1. All 560 SP products are byte-identical to the baseline. Candidate losses/additions and lost supports/provenance: **0/0/0**.
- PERFORM **39/39**, Chaos **48/48**, aliases **14/14**, PERFORM adversaries **25/25**. Chaos retains every manifest expectation, including caller-specific values. Its context oracle now executes frames independently because a shared body has one operation, rather than requiring one physical copy per caller. Context removal, value swapping and invented values are rejected.
- New real-SP probes: same/different CICS handler states; caller A and B retain separate values at their continuations and union only at the shared site. The value probe passes with qualified source and through the executable AIR consumer alone.
- CFG rule correlation: all 3,600 local rules across the 560 outputs agree with full AIR identities/destinations. Three compiled consumer mutations (skip body, use outer frame, join before replay) are rejected, with clean runs before/after. AIR transport has three additional compiled mutations from S1.

### Measured representation

All 73 CardDemo programs: CFG nodes **110,570 → 79,176** (−28.4%); ordinary
transitions 124,356 → 84,504 plus 3,063 local rules. AIR JSON bytes
1,338,048,740 → 1,122,289,870; CFG bytes 80,302,820 → 57,186,388.
COACTUPC nodes 5,037 → 3,132; COTRTLIC 5,656 → 2,943; COTRTUPC 1,600 → 598.
COACCT01 and CODATE01 retain their former sizes because their contexts do not meet
the equivalence proof. There is no claim that every orphan disappears.

Two alternating baseline/candidate runs on each of those five programs, identical
SP input and 2 GiB Java heap, measured 242.57 → 221.28 summed process seconds and
2,562,472 → 2,556,092 KiB maximum process RSS. These are observations under shared
machine load, not a universal latency/memory guarantee. Solver context count can
remain large even when serialized bodies shrink.

### Integration and remaining limits

Order: AIR #23 → lower #52 → CFG #57; repin to actual merge commits only after
review authorization. Existing PARTIAL statuses, source hypotheses, modelAssumed,
unknown control and proof-of-kill policy remain. Recursion, nonlocal escapes and
state-changing/exceptional CICS closures keep existing specialization; the generic
consumer refuses repeated simultaneously active local invocation instead of
silently cutting the stack. Sharing is not an implementation of ALTER.

CFG JSON v5 requires consumers to interpret local rules. The separate graph
visualizer currently accepts only v1–4; its importer/traversal upgrade is an
integration dependency outside these analyzer PRs. No UI compatibility claim or
unconditional return edges were added to conceal that boundary.

Raw commands, hashes, immutable runtime, 560 products, comparison and metrics:
workspace `.shared-routine-bodies/evidence/` (`runtime-02`, `final-replay`,
`final-qualification-02`, `focal-final-03`, `context-mutations`, `measurements`).
Production source hashes are checked against the frozen runtime; later commits
contain tests/docs/pins only. The work item remains IN_PROGRESS per repository
policy until review and merge; implementation/qualification scope is complete.


## Pre-existing failures found during review

The same review campaign now includes the structural anonymous-root correction
and frontend continuation PR #77. [Causes, tests and limits](stage5-anonymous-logical-roots.md)
describe the additional scope; the original S5 measurements above remain the
historical sharing comparison. The consumer PR #57 records the new comparison
against that qualified stage-5 baseline.
