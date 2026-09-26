# SP 2.41/2.42 consumer boundary — R7-R3

The consumer admits exact versions before interpreting version-specific fields. An unknown version is UNSUPPORTED_CONTRACT even when known features are malformed. Basic JSON, schema and textual version checks precede that gate. Existing historical versions remain closed profiles. SP 2.40 requires FactDependencies and ControlTopology; 2.41 inherits them and adds CICS_HANDLER; 2.42 adds CICS_ABEND and the cics-handle-abend-ordinary-return proof. Older profiles reject these additions. The shared wire DTO projection retains every new fact; legacy adapter normalization is not an SP republishing/downgrade mechanism.

Authority: [frontend contract at 1db76b3](https://github.com/Gustavo2358/proleap-poc/blob/1db76b3525588aa41438acd893bf51273e650909/src/main/java/io/github/gustavo2358/cobolexplorer/semanticproduct/CobolSemanticProduct.java) and its pinned JSON writer. No new COBOL semantics are inferred by this consumer.

`SpInput.CicsHandlerFact` independently retains action, target kind/syntax, binding identity, operand/entry provenance, PROGRAM literal/DATA target, relative scope, ordered options, offsets and gaps. Constructor guards reject contradictory shapes. The input snapshot indexes published statements once per validation pass and validates same-unit root entries and canonical entry origins by identity. Source ProgramUnit is never a runtime logical-level identity.

`SpInput.CicsAbendFact` retains event kind, dispatch eligibility, raw text, options and gaps. It has no state/target/dispatch fields. Known eligibility requires coherent typed CANCEL evidence and supported nonduplicate options. An UNAVAILABLE event can retain raw unsupported/duplicate options with its syntax-gap cause, as required by the producer contract; that is not qualified eligibility. No rawText parsing, source reopening or spelling-based binding occurs.

Both facts expose `executableLowering() == NOT_READY`. An attempt to execute lowering returns IMPLEMENTATION_LIMIT with the immutable input retained and no AIR publication. Registration is not dispatch; CANCEL/RESET have no computed state transition. The ordinary-return proof is transported within the generic topology algebra; targetEntry never becomes a successor. No handler/event memory dependency is created. AIR, analysis-ir and analysis-cfg remain unchanged.

The architecture experiment succeeds at the typed input boundary. A future consumer can receive these immutable facts without an AIR handler operation; R7-R3 stops here.

## Qualification

CicsConsumerContractSuite is part of FAST. Its 17 frontend-generated fixtures include resolved/unresolved labels, PROGRAM literal/DATA, CANCEL/RESET, plain/bypassed/ABCODE/unsupported ABEND, unavailable handler, combined handler/event COPY literal provenance and multiplicities 1/2/5/40. The historical multiplicity assertion is unchanged; its fixture dispatcher now supplies these two additional families. The resource manifest records producer SHA and source/SP digests. Adversarial JSON mutations are generated only inside tests and never mislabeled as producer output. M9 is an independent canonical-successor oracle: a structurally valid forged destination must differ from statement:1; the consumer does not rederive language semantics to detect that forgery.

Historical SP 2.40 byte fixtures and semantic oracles are unchanged. Validation includes FAST, local full, historical wire/FD/topology/FILE gates, 73 hash-certified CardDemo SP products, deterministic typed replay and decode telemetry. No frontend tests are claimed as rerun by R7-R3.

To regenerate each compatibility fixture, use ExplorerMain from frontend 1db76b3, Java 21 and -Xmx2g with `--source <name.cbl> --copybooks <cpy> --output <output>`; retain the emitted cobol-semantic-product.json unchanged. The consumer uses the existing 1 GiB test heap and INPUT_LIMITS.

## R7-R3-R1 — consistency before executable readiness

`PartialProgramAdmission.validateKnownFacts` reuses the existing structural, topology, storage, CALL, CICS/FILE, PERFORM, GO TO, EVALUATE, IF and other-effect validators before checking NOT_READY. A detectable independent contradiction returns INVALID_INPUT, including when valid handler/event facts coexist. Only a consistent input reaches the READINESS diagnostic in Phase.ADMISSION. NOT_READY still blocks all executable profile selection and publication; it does not reclassify capability gaps as invalid facts. Legacy executable profile qualification remains after this barrier.

`ValidationBeforeReadinessSuite` adds paired real-producer fixtures with historical statements and handler/event/both. Three separate wire contradictions (CICS conditions, CALL continuation, FILE target mode) must each remain INVALID_INPUT in all four contexts. Valid mixed inputs retain IMPLEMENTATION_LIMIT and empty publication at both single-unit and compilation boundaries. The R7-R3 contract suite and its eleven mutations retain their original expectations.
