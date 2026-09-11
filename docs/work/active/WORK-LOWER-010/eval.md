# Independent oracle

Dynamic X8: one WS-PGM Object, Assign TextValue("PROGA   "), Invoke ComputedTarget(Read(ObjectPlace(the same object))), Normal to the unique GOBACK Return sequence. X5 assigns "PROGA". Literal CALL: LiteralTarget("PROGA"), no fake DATA. Source PROGA/OTHER/no MOVE always retains the computed shape.

No arguments/results; closed empty signature; all-memory conservative reads/writes, empty mustOverwrite, all-control open remainder, UnknownContract/UnknownName. Validator STRUCTURALLY_VALID with I-56 before/after AirJson; semantic and canonical-byte round-trips. Real COBOL -> W1A SP bytes -> lower -> AIR JSON twice, with SP and AIR determinism.

Strict wire unknown/missing/null/type/enum/duplicate/variant tests; typed surface/binding/provenance preservation; unsupported USING/RETURNING/handlers/ambiguous/unresolved/refmod/subscript/truncation/nonlinear; incomplete input blocks; invalid coherence invalidates. Wrong dynamic literal, wrong ObjectId, X8/X5 padding, terminator placement, continuation, pure effects, closed outcomes, invented contract, trim, weakened admission and revision must be detected semantically. Compilation failure does not count.

Existing entry/GOBACK, scalar MOVE, output/CLI atomicity, capacity and architecture remain green. Canonical first-slice full gate includes docs, semantic, performance, architecture, git, harness-tests and challenge. Dedicated transport/integration executors remain unavailable; W1C real E2E checks run as semantic tests and an exact-producer script. Remote required checkpoint push workflow at exact HEAD; record any PR workflows actually configured, not imagined ones.
