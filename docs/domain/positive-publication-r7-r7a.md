# Bounded positive publication — R7-R7A

A typed capability that is not executable must not erase independent validated facts. Semantic inventory availability and executable completeness are separate contracts.

## Request and result

`LowerInput.Options.publicationPolicy` is explicit. `EXECUTABLE_ONLY` retains the existing request contract and NOT_READY rejection. The three-argument Options constructor and `CobolLower.OPTIONS` retain that policy for existing callers. The configured CLI uses `CobolLower.POSITIVE_OPTIONS` / `BOUNDED_POSITIVE`. This is a requested projection policy, with no physical profile, source parsing or automatic fallback.

The positive policy runs the same wire/domain/factual validation, then the unchanged handler-state analysis. It records each validated `NonExecutableCapability` by StatementId, typed family, provenance and NOT_READY disposition. It continues structural projection admission; unrelated contradictions and missing entry facts retain their own rejection status. In particular, missing ENTRY_START remains BLOCKED_LOWERING.

A real validated Publication with non-executable capabilities has result `BOUNDED_PUBLICATION`. It cannot be SUCCESS or the existing PARTIAL result. PARTIAL still means the existing opt-in, scoped incomplete AIR validation. Bounded output retains the real validator result and uses the partial codec only for that existing condition. No validator checks are bypassed.

CLI exit 0 means a real AIR file was serialized and published. It does not certify full source executability. Bounded output prints `BOUNDED_PUBLICATION` and `NOT_READY`; the AIR carries occurrence-owned coverage and uncertainty metadata even for capabilities outside the entry projection. Other errors retain their exit codes and atomic file behavior.

## Publication boundaries

The AIR2.0 partial-projection contract already represents an open control frontier with no enumerated destinations and an empty finite projection bound. New CICS capability occurrences use that existing representation, with explicit unavailable control/effect precision. They publish no executable continuation, effects or handler dispatch. It is neither a source termination assertion nor a no-op. No global memory/control remainder is introduced.

Source ControlTopology remains intact in typed input. The pre-AIR HandlerStateAnalyzer sees precisely the same topology as before. The executable AIR projection stops earlier where lowering is NOT_READY, including where source topology already knows ordinary completion. Source and executable projection reachability must not be confused.

CALL/CICS invocation payloads, FILE resources, declarations and source dependencies use their existing lower authorities. Inventory after a frontier is not a proof of execution or reaching values. Consumers may enumerate such occurrences but must not derive runtime candidates through the frontier. Source resource facts (COPYBOOK/DCLGEN/static SQL) do not depend on that execution. FactDependencies remains the same storage/value authority; no new dependency producer or policy is added.

New CICS facts are not deleted, downgraded or reinterpreted. They remain typed in the input (Admission.input for a single unit), with deterministic canonical identity and observable capability coverage in AIR. Coverage codes are diagnostics, never execution rules.

## Qualification

Frontend-generated fixtures retain source, COPY and SP bytes under `adapters/src/test/resources/sp/positive-publication-r7a`. `PositivePublicationSuite` exercises add/remove, dead code, operand rename, family change, a second capability, independent source declaration movement, wire inventory permutation, strict AIR roundtrip, the actual configured CLI, and factual-invalidity precedence. Historical executable-only suites keep their original semantics.

The rejection-only enum sweep in AirOutputSuite excludes the new publication-bearing status; every previous case and oracle is unchanged. The new result's publication/validation invariants have separate tests.

CardDemo73 is audited as 31 R6 regressions, 9 previously limited handler products, 1 independent ENTRY_START blocker and 32 unchanged successes. Evidence and executed wrong-code mutations live in the R7 campaign `positive-publication-r7a` evidence directory. AIR, SP, CFG, dependency contracts and the handler-state engine remain unchanged. SEND1313, executable dispatch, R8 and R9 are outside scope.
