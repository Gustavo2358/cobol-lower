package io.github.gustavo2358.lower.adapters.sp;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import java.util.List;
import static io.github.gustavo2358.lower.domain.SpInput.*;
import static io.github.gustavo2358.lower.adapters.sp.Wire.Nullable;

/** Exact SP 1.6.0 closed contract, including IF and storage evidence. No semantic parsing in core. */
final class Wire16 {
    private Wire16() { }
    record Document(String schema, String contractVersion, Wire.UnitKeyDocument unit, Wire.PolicyDocument policy,
        List<DataDocument> dataDeclarations, List<StatementDocument> statements, Wire.StructureDocument structure,
        List<Wire.GapDocument> gaps, Wire.CoverageDocument coverage, Wire.EntryInventoryDocument entryInventory, StorageDocument storageIndependence) { }
    @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "variant")
    @JsonSubTypes({
        @JsonSubTypes.Type(value = GobackDocument.class, name = "GOBACK"),
        @JsonSubTypes.Type(value = MoveDocument.class, name = "MOVE"),
        @JsonSubTypes.Type(value = CallDocument.class, name = "CALL"),
        @JsonSubTypes.Type(value = IfDocument.class, name = "IF"),
        @JsonSubTypes.Type(value = PerformDocument.class, name = "PERFORM"),
        @JsonSubTypes.Type(value = ObservedDocument.class, name = "OBSERVED")
    })
    sealed interface StatementDocument permits GobackDocument, MoveDocument, CallDocument, IfDocument, ObservedDocument, PerformDocument {
        Wire.StatementHeaderDocument header();
    }
    record PerformTargetDocument(String id, Wire.ProvenanceDocument referenceOrigin, Wire.ProvenanceDocument paragraphOrigin) { }
    record PerformDocument(Wire.StatementHeaderDocument header, PerformProfile profile, @Nullable PerformTargetDocument target,
        @Nullable String targetEntry, List<String> targetStatements, @Nullable String targetExit,
        ContinuationDocument normalContinuation, List<String> primaryStatements, List<String> gapCodes) implements StatementDocument { }
    record ScalarDocument(LogicalDomain logicalDomain, int logicalExtent, StorageClass storageClass, DeclarationScope declarationScope) { }
    record LogicalDocument(LogicalDomain logicalDomain, String value, int logicalExtent) { }
    record WholeDocument(String data) { }
    record DataDocument(String id, String canonicalName, @Nullable String picture, Wire.ProvenanceDocument provenance,
        CoverageStatus coverage, Wire.ReadinessDocument readiness, @Nullable ScalarDocument scalarText) { }
    @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "variant")
    @JsonSubTypes({@JsonSubTypes.Type(value = LiteralDocument.class, name = "LITERAL"),
        @JsonSubTypes.Type(value = DataSourceDocument.class, name = "DATA")})
    sealed interface MoveSourceDocument permits LiteralDocument, DataSourceDocument { }
    record DataSourceDocument(ReferenceDocument reference) implements MoveSourceDocument { }
    record LiteralDocument(String id, LiteralKind kind, String value, Wire.ProvenanceDocument provenance, @Nullable LogicalDocument logicalValue) implements MoveSourceDocument { }
    record ReferenceDocument(String id, OperandRole role, Wire.BindingDocument binding, Wire.ProvenanceDocument provenance, @Nullable WholeDocument wholeItemAccess) { }
    record ContinuationDocument(ContinuationAvailability availability, @Nullable String statement, Wire.ProvenanceDocument provenance) { }
    record MoveDocument(Wire.StatementHeaderDocument header, MoveSourceDocument source, ReferenceDocument target,
        CopySemantics copySemantics, ContinuationDocument normalContinuation, @Nullable AdjustmentDocument textAdjustment) implements StatementDocument { }
    record GobackDocument(Wire.StatementHeaderDocument header, GobackExit exit, LocalContinuation localContinuation) implements StatementDocument { }
    record AdjustmentDocument(TextAdjustmentRule rule, int receiverExtent, LogicalDocument result, Wire.ProvenanceDocument provenance) { }
    @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "kind")
    @JsonSubTypes({@JsonSubTypes.Type(value = DataTargetDocument.class, name = "DATA"),
        @JsonSubTypes.Type(value = LiteralTargetDocument.class, name = "LITERAL")})
    sealed interface TargetDocument permits DataTargetDocument, LiteralTargetDocument { }
    record DataTargetDocument(ReferenceDocument reference) implements TargetDocument { }
    record LiteralTargetDocument(String id, String text, String writtenText, @Nullable LogicalDocument logicalValue,
        Wire.ProvenanceDocument provenance) implements TargetDocument { }
    record SurfaceDocument(ClausePresence using, @Nullable Integer argumentCount, ClausePresence returning,
        ClausePresence onException, ClausePresence notOnException, ClausePresence onOverflow) { }
    record CallDocument(Wire.StatementHeaderDocument header, CallSyntax syntax, TargetDocument target,
        RuntimeTargetKnowledge runtimeTarget, String runtimeUncertaintyCode, ContinuationDocument normalContinuation,
        SurfaceDocument surface, CallEffects effects, CallOutcomes outcomes) implements StatementDocument { }
    record PredicateDocument(Availability availability, PredicateProfile profile, PredicateDomain resultDomain,
        PredicateEvaluation evaluation, PredicateCompletion normalCompletion, ReadsCompleteness readsCompleteness,
        PredicateTruth truthValue, List<String> knownReads, Wire.ProvenanceDocument provenance, List<String> gapCodes) { }
    record ConditionDocument(String shape, List<ReferenceDocument> references, Wire.ProvenanceDocument provenance, PredicateDocument predicate) { }
    record ArmDocument(ClausePresence presence, Availability contentAvailability, Wire.ExecutableStartDocument entry,
        Wire.ProvenanceDocument provenance, List<String> gapCodes) { }
    record StorageDocument(Availability availability, StorageIndependenceRule rule, String authority, List<String> members,
        @Nullable Wire.ProvenanceDocument provenance, List<String> gapCodes) { }
    record IfDocument(Wire.StatementHeaderDocument header, ConditionDocument condition, boolean explicitlyTerminated,
        @Nullable String continuation, ContinuationDocument normalContinuation, ArmDocument thenArm, ArmDocument elseArm, IfProfile profile) implements StatementDocument { }
    record ObservedDocument(Wire.StatementHeaderDocument header, String observedKind, String observedShape, String gapCode) implements StatementDocument { }
}
