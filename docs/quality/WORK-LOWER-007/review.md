# Focused self-review

Reviewer: implementing Codex, same context; no independent or human approval claimed.
Candidate identity is resolved only by CP0.json#/candidate_diff_sha256.

Production diff is six files: remove byte/node/visit option components and their
comparisons, complete file reads, preserve long counters, retain depth64 and
remove Jackson's duplicated value-string capacity enforcement. No new traversal,
index, semantic representation, serialization, String conversion, mapper,
translation rule or AIR dependency was introduced. ArrayDeque traversal and
recursive Wire validation are unchanged; width grows counts, not recursion depth.

Existing tests migrated to depth-only and diagnostics-only options. Only obsolete
capacity-rejection assertions changed behavior; malformed JSON, invalid UTF-8,
duplicate keys, trailing tokens, coercion/shape/version failures, missing or
incoherent facts, dangling references, unsupported profiles, diagnostic exhaustion,
identity representability, shared AIR validation and all output/cleanup oracles
remain. ScalarSuite.oracle itself is unchanged from base. The frozen new
CapacitySuite is the exact test that produced the initial RED.

The new corpus is explicitly synthetic, deterministically extended from the
committed SP1.2 fixture, with coherent IDs, continuations, program points, counts
and distinct published locations. No frontend output is fabricated as real.
Publication equality uses the existing in-memory determinism oracle plus the
existing complete relational oracle, not merely counts or IDs. No codec limit
was raised to encode the large result.

Scope guard retains byte-exact enforcement for all unrelated production sources,
including AirFileOutput and every translator/identity implementation. Its new
work-specific snapshot avoids reusing CP4C authorization. Sources.lock, all SP/AIR
fixtures, historical quality files and the previous work package remain unchanged.
The new diagnostic-budget positive runs after existing scalar oracles so it
does not mask historical mutation causes. Their expected assertions remain unchanged.
The source API now accepts only depth / diagnostic arguments; README states that
migration. No silent ignored capacity argument or public configuration was added.

Inherited harness constraints were observed, not labeled PASS: worktree .git file
breaks lifecycle test copies, so full runs in a disposable byte-identical standalone
clone; historical CP4C registry reconciliation is separately constrained by its
frozen remediation manifest. See lifecycle.md and the raw failed-attempt logs.
A new admission mutation also exposed the need to rebuild isolated core dependencies;
its focal assertion now executes against current mutated bytecode before restore.

The 1.5GiB-heap probe is telemetry only. The existing whole-document amplification
is not solved, and no memory/SLA or unlimited-machine-resource claim is made.
Pinned upstream AIR validation and JSON16MiB remain independent constraints.
Final gates, mutation restoration and exact candidate identity are recorded in
CP0.json; human review and merge remain pending and unauthorized respectively.
