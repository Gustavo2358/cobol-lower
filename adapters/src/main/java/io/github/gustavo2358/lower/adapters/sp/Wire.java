package io.github.gustavo2358.lower.adapters.sp;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.List;
import static io.github.gustavo2358.lower.domain.SpInput.*;

/** Exact writer 1.1.0 physical DTOs. Never exported by the core. */
final class Wire {
    private Wire() { }
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.RECORD_COMPONENT)
    @interface Nullable { }

    record Document(String schema, String contractVersion, UnitKeyDocument unit, PolicyDocument policy,
        List<DataFactDocument> dataDeclarations, List<StatementDocument> statements, StructureDocument structure,
        List<GapDocument> gaps, CoverageDocument coverage, EntryInventoryDocument entryInventory) { }

    @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "variant")
    @JsonSubTypes({
        @JsonSubTypes.Type(value = GobackFactDocument.class, name = "GOBACK"),
        @JsonSubTypes.Type(value = MoveDocument.class, name = "MOVE"),
        @JsonSubTypes.Type(value = CallDocument.class, name = "CALL"),
        @JsonSubTypes.Type(value = IfDocument.class, name = "IF"),
        @JsonSubTypes.Type(value = ObservedDocument.class, name = "OBSERVED")
    })
    sealed interface StatementDocument permits GobackFactDocument, MoveDocument, CallDocument, IfDocument, ObservedDocument {
        StatementHeaderDocument header();
    }
    record UnitKeyDocument(String compilationUnitId, List<Integer> structuralPath, String canonicalProgramName) { }
    record PolicyDocument(String policyId, String version, QualifyMode qualifyMode, PgmnameMode pgmnameMode, DynamMode dynamMode, DllMode dllMode) { }
    record LocationDocument(String file, int startLine, int startColumn, int endLine, int endColumn) { }
    record IncludeFrameDocument(String includingFile, String requestedName, String includedFile, int includeLine) { }
    record ProvenanceDocument(LocationDocument expanded, LocationDocument original, List<IncludeFrameDocument> includeChain, boolean exact) { }
    record ReadinessClaimDocument(ReadinessStatus status, String scope) { }
    record ReadinessDocument(ReadinessClaimDocument lowering, ReadinessClaimDocument cfg, ReadinessClaimDocument effectsDataflow) { }
    record DataFactDocument(String id, String canonicalName, @Nullable String picture, ProvenanceDocument provenance, CoverageStatus coverage, ReadinessDocument readiness) { }
    record ContainmentDocument(@Nullable String parent, Branch branch) { }
    record StatementHeaderDocument(String id, int programPoint, ContainmentDocument containment, ProvenanceDocument provenance, CoverageStatus coverage, ReadinessDocument readiness) { }
    record GobackFactDocument(StatementHeaderDocument header, GobackExit exit, LocalContinuation localContinuation) implements StatementDocument { }
    record GapDocument(String statement, GapScope scope, String code, String detail, ProvenanceDocument provenance) { }
    record EntryGapDocument(GapScope scope, String code, String detail, ProvenanceDocument provenance) { }
    record ExecutableStartDocument(Availability availability, @Nullable String statement) { }
    record EntrySignatureDocument(Availability availability, @Nullable Integer parameterCount, ReturningClause returningClause) { }
    record EntryFactDocument(String id, EntryRole role, Availability availability, ExecutableStartDocument start, EntrySignatureDocument signature, ProvenanceDocument provenance, CoverageStatus coverage, ReadinessDocument readiness, List<EntryGapDocument> gaps) { }
    record EntryInventoryDocument(InventoryStatus status, EntryInventoryScope scope, List<EntryFactDocument> entries, List<String> gapCodes) { }
    record BranchChildrenDocument(String parent, Branch branch, List<String> children) { }
    record StructureDocument(List<String> roots, List<BranchChildrenDocument> branches) { }
    record CoverageDocument(InventoryStatus inventoryStatus, int observedStatements, int modeledStatements, int partialStatements, int unsupportedStatements, int inputMissingStatements, ReadinessDocument readiness) { }

    enum LiteralKind { ALPHANUMERIC, NUMERIC, UNKNOWN }
    enum OperandRole { READ, WRITE, CALL_TARGET }
    enum ResolutionStatus { RESOLVED, AMBIGUOUS, UNRESOLVED, INPUT_MISSING }
    enum ResolutionReason { UNIQUE_VISIBLE_DECLARATION, QUALIFIED_HIERARCHY_MATCH, MULTIPLE_VALID_CANDIDATES,
        DECLARATION_NOT_FOUND, INPUT_INCOMPLETE, UNSUPPORTED_GRAMMAR_FORM, UNSUPPORTED_DIALECT_OPTION, INVALID_NAMESPACE_FOR_CONTEXT }
    enum CallSyntax { IDENTIFIER_OR_EXPRESSION }
    enum RuntimeTargetKnowledge { UNKNOWN }
    record LiteralDocument(String id, LiteralKind kind, String value, ProvenanceDocument provenance) { }
    record CandidateDocument(String id, String canonicalName) { }
    record BindingDocument(ResolutionStatus status, ResolutionReason reason, List<CandidateDocument> candidates, @Nullable String selected) { }
    record ReferenceDocument(String id, OperandRole role, BindingDocument binding, ProvenanceDocument provenance) { }
    record ConditionDocument(String shape, List<ReferenceDocument> references, ProvenanceDocument provenance) { }
    record MoveDocument(StatementHeaderDocument header, LiteralDocument source, ReferenceDocument target) implements StatementDocument { }
    record CallDocument(StatementHeaderDocument header, CallSyntax syntax, ReferenceDocument operand,
        RuntimeTargetKnowledge runtimeTarget, String runtimeUncertaintyCode) implements StatementDocument { }
    record IfDocument(StatementHeaderDocument header, ConditionDocument condition, boolean explicitlyTerminated,
        @Nullable String continuation) implements StatementDocument { }
    record ObservedDocument(StatementHeaderDocument header, String observedKind, String observedShape, String gapCode) implements StatementDocument { }
}
