package io.github.gustavo2358.lower.adapters.sp;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import java.util.List;
import static io.github.gustavo2358.lower.domain.SpInput.*;
import static io.github.gustavo2358.lower.adapters.sp.Wire.Nullable;

/** Exact 1.2.0 DTOs. Common unchanged records are shared with 1.1.0; new fields are mandatory, nullable only by contract. */
final class Wire12 {
    private Wire12() { }
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
        CopySemantics copySemantics, ContinuationDocument normalContinuation) implements StatementDocument { }
    record GobackDocument(Wire.StatementHeaderDocument header, GobackExit exit, LocalContinuation localContinuation) implements StatementDocument { }
    record CallDocument(Wire.StatementHeaderDocument header, Wire.CallSyntax syntax, ReferenceDocument operand,
        Wire.RuntimeTargetKnowledge runtimeTarget, String runtimeUncertaintyCode) implements StatementDocument { }
    record ConditionDocument(String shape, List<ReferenceDocument> references, Wire.ProvenanceDocument provenance) { }
    record IfDocument(Wire.StatementHeaderDocument header, ConditionDocument condition, boolean explicitlyTerminated,
        @Nullable String continuation) implements StatementDocument { }
    record ObservedDocument(Wire.StatementHeaderDocument header, String observedKind, String observedShape, String gapCode) implements StatementDocument { }
}
