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

Final FAST/full/corpus qualification is in progress; no merge or complete-qualification claim at this checkpoint.
