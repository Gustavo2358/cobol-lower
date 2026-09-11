package io.github.gustavo2358.lower.adapters.sp;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import java.util.List;
import static io.github.gustavo2358.lower.domain.SpInput.*;
import static io.github.gustavo2358.lower.adapters.sp.Wire.Nullable;

/** Exact SP 1.3.0 target sum and fitted text facts. No semantic parsing in core. */
final class Wire13 {
    private Wire13() { }
    record Document(String schema, String contractVersion, Wire.UnitKeyDocument unit, Wire.PolicyDocument policy,
        List<DataDocument> dataDeclarations, List<StatementDocument> statements, Wire.StructureDocument structure,
        List<Wire.GapDocument> gaps, Wire.CoverageDocument coverage, Wire.EntryInventoryDocument entryInventory) { }
    @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "variant")
    @JsonSubTypes({
        @JsonSubTypes.Type(value = GobackDocument.class, name = "GOBACK"),
        @JsonSubTypes.Type(value = MoveDocument.class, name = "MOVE"),
        @JsonSubTypes.Type(value = CallDocument.class, name = "CALL"),
        @JsonSubTypes.Type(value = IfDocument.class, name = "IF"),
        @JsonSubTypes.Type(value = ObservedDocument.class, name = "OBSERVED")
    })
    sealed interface StatementDocument permits GobackDocument, MoveDocument, CallDocument, IfDocument, ObservedDocument {
        Wire.StatementHeaderDocument header();
    }
    record ScalarDocument(LogicalDomain logicalDomain, int logicalExtent, StorageClass storageClass, DeclarationScope declarationScope) { }
    record LogicalDocument(LogicalDomain logicalDomain, String value, int logicalExtent) { }
    record WholeDocument(String data) { }
    record DataDocument(String id, String canonicalName, @Nullable String picture, Wire.ProvenanceDocument provenance,
        CoverageStatus coverage, Wire.ReadinessDocument readiness, @Nullable ScalarDocument scalarText) { }
    record LiteralDocument(String id, LiteralKind kind, String value, Wire.ProvenanceDocument provenance, @Nullable LogicalDocument logicalValue) { }
    record ReferenceDocument(String id, OperandRole role, Wire.BindingDocument binding, Wire.ProvenanceDocument provenance, @Nullable WholeDocument wholeItemAccess) { }
    record ContinuationDocument(ContinuationAvailability availability, @Nullable String statement, Wire.ProvenanceDocument provenance) { }
    record MoveDocument(Wire.StatementHeaderDocument header, LiteralDocument source, ReferenceDocument target,
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
    record ConditionDocument(String shape, List<ReferenceDocument> references, Wire.ProvenanceDocument provenance) { }
    record IfDocument(Wire.StatementHeaderDocument header, ConditionDocument condition, boolean explicitlyTerminated,
        @Nullable String continuation) implements StatementDocument { }
    record ObservedDocument(Wire.StatementHeaderDocument header, String observedKind, String observedShape, String gapCode) implements StatementDocument { }
}
