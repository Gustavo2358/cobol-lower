package io.github.gustavo2358.lower.adapters.sp;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import java.util.List;
import io.github.gustavo2358.lower.domain.StorageFacts;
import static io.github.gustavo2358.lower.domain.SpInput.*;
import static io.github.gustavo2358.lower.adapters.sp.Wire.Nullable;

/** Exact SP 2.7.0 closed contract, including IF and storage evidence. No semantic parsing in core. */
final class Wire211 {
    private Wire211() { }
    record Document(String schema, String contractVersion, Wire.UnitKeyDocument unit, Wire.PolicyDocument policy,
        List<DataDocument> dataDeclarations, List<StatementDocument> statements, Wire.StructureDocument structure,
        List<Wire.GapDocument> gaps, Wire.CoverageDocument coverage, Wire.EntryInventoryDocument entryInventory, StorageDocument storageIndependence, PhysicalStorageDocument storage) { }
    @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "variant")
    @JsonSubTypes({
        @JsonSubTypes.Type(value = GobackDocument.class, name = "GOBACK"),
        @JsonSubTypes.Type(value = MoveDocument.class, name = "MOVE"),
        @JsonSubTypes.Type(value = CallDocument.class, name = "CALL"),
        @JsonSubTypes.Type(value = CicsDocument.class, name = "CICS_PROGRAM_CONTROL"),
        @JsonSubTypes.Type(value = IfDocument.class, name = "IF"),
        @JsonSubTypes.Type(value = PerformDocument.class, name = "PERFORM"),
        @JsonSubTypes.Type(value = ProcedurePerformDocument.class, name = "PERFORM_PROCEDURE"),
        @JsonSubTypes.Type(value = EvaluateDocument.class, name = "EVALUATE"),
        @JsonSubTypes.Type(value = GoToDocument.class, name = "GO_TO"),
        @JsonSubTypes.Type(value = ConditionalGoToDocument.class, name = "GO_TO_DEPENDING_ON"),
        @JsonSubTypes.Type(value = ObservedDocument.class, name = "OBSERVED")
    })
    sealed interface StatementDocument permits GobackDocument, MoveDocument, CallDocument, CicsDocument, IfDocument, ObservedDocument, PerformDocument, EvaluateDocument, GoToDocument, ConditionalGoToDocument, ProcedurePerformDocument {
        Wire.StatementHeaderDocument header();
    }
    record GoToDestinationDocument(int ordinal,@Nullable String target,@Nullable Wire.ProvenanceDocument procedureOrigin,
        Wire.ProvenanceDocument referenceOrigin,@Nullable String targetEntry,@Nullable Wire.ProvenanceDocument entryOrigin,List<String> gapCodes) { }
    record ConditionalGoToDocument(Wire.StatementHeaderDocument header,@Nullable ReferenceDocument selector,boolean selectorInteger,
        Wire.ProvenanceDocument selectorOrigin,List<GoToDestinationDocument> destinations,ContinuationDocument normalContinuation,List<String> gapCodes) implements StatementDocument { }
    record GoToTargetDocument(String id, Wire.ProvenanceDocument paragraphOrigin) { }
    record GoToDocument(Wire.StatementHeaderDocument header, @Nullable GoToTargetDocument target, Wire.ProvenanceDocument referenceOrigin,
        @Nullable String targetEntry, @Nullable Wire.ProvenanceDocument entryOrigin, List<String> gapCodes) implements StatementDocument { }
    record EvaluateArmDocument(int ordinal, MoveSourceDocument selection, List<String> statements, ArmDocument control) { }
    record EvaluateDocument(Wire.StatementHeaderDocument header, @Nullable ReferenceDocument subject, List<EvaluateArmDocument> arms,
        ArmDocument otherArm, List<String> otherStatements, ContinuationDocument normalContinuation, List<String> gapCodes) implements StatementDocument { }
    record PerformParagraphDocument(String id, String entry, List<String> statements, List<String> completions, Wire.ProvenanceDocument provenance) { }
    record PerformCountDocument(PerformCountProfile profile,@Nullable String integer,@Nullable ReferenceDocument reference,Wire.ProvenanceDocument provenance) { }
    record VaryingOperandDocument(int level,VaryingOperandRole role,@Nullable String integer,List<ReferenceDocument> references,Wire.ProvenanceDocument provenance) { }
    record PerformVaryingDocument(int levels,List<VaryingOperandDocument> controls) { }
    record PerformLoopDocument(PerformTestMode testMode, ConditionDocument condition) { }
    record ProcedurePerformDocument(Wire.StatementHeaderDocument header, @Nullable PerformTargetDocument start, @Nullable PerformTargetDocument end,
        List<PerformParagraphDocument> procedures, ContinuationDocument normalContinuation, @Nullable PerformLoopDocument loop,@Nullable PerformCountDocument times,@Nullable PerformVaryingDocument varying,List<String> gapCodes) implements StatementDocument { }
    record PerformTargetDocument(String id, Wire.ProvenanceDocument referenceOrigin, Wire.ProvenanceDocument paragraphOrigin) { }
    record PerformDocument(Wire.StatementHeaderDocument header, PerformProfile profile, @Nullable PerformTargetDocument target,
        @Nullable String targetEntry, List<String> targetStatements, @Nullable String targetExit,
        ContinuationDocument normalContinuation, List<String> gapCodes) implements StatementDocument { }
    record ScalarDocument(LogicalDomain logicalDomain, int logicalExtent, StorageClass storageClass, DeclarationScope declarationScope) { }
    record LogicalDocument(LogicalDomain logicalDomain, String value, int logicalExtent) { }
    record WholeDocument(String data) { }
    record IntegerDocument(int digits) { }
    record DataDocument(String id, String canonicalName, @Nullable String picture, Wire.ProvenanceDocument provenance,
        CoverageStatus coverage, Wire.ReadinessDocument readiness, @Nullable ScalarDocument scalarText,@Nullable IntegerDocument scalarInteger) { }
    @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "variant")
    @JsonSubTypes({@JsonSubTypes.Type(value = LiteralDocument.class, name = "LITERAL"),
        @JsonSubTypes.Type(value = DataSourceDocument.class, name = "DATA")})
    sealed interface MoveSourceDocument permits LiteralDocument, DataSourceDocument { }
    record DataSourceDocument(ReferenceDocument reference) implements MoveSourceDocument { }
    record LiteralDocument(String id, LiteralKind kind, String value, Wire.ProvenanceDocument provenance, @Nullable LogicalDocument logicalValue) implements MoveSourceDocument { }
    record ReferenceDocument(String id, OperandRole role, Wire.BindingDocument binding, Wire.ProvenanceDocument provenance, @Nullable WholeDocument wholeItemAccess, @Nullable RegionalAccessDocument regionalAccess) { }
    record ContinuationDocument(ContinuationAvailability availability, @Nullable String statement, Wire.ProvenanceDocument provenance) { }
    record MoveDocument(Wire.StatementHeaderDocument header, MoveSourceDocument source, ReferenceDocument target,
        CopySemantics copySemantics, ContinuationDocument normalContinuation, @Nullable AdjustmentDocument textAdjustment, @Nullable RegionalMoveDocument regionalMove,List<MoveTransferDocument> additionalTransfers) implements StatementDocument { }
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
    record CicsOptionDocument(String name,@Nullable String operand,int start,int end,@Nullable ReferenceDocument reference) { }
    record CicsDocument(Wire.StatementHeaderDocument header,CicsCommand command,String rawText,@Nullable TargetDocument target,
        List<CicsOptionDocument> options,CicsConditions conditions,ContinuationDocument localContinuation,ContinuationDocument ordinaryContinuation,String nameProfile,List<String> gapCodes) implements StatementDocument { }
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
    record ObservedDocument(Wire.StatementHeaderDocument header, String observedKind, String observedShape, String gapCode, ContinuationDocument normalContinuation, List<ReferenceDocument> knownReferences) implements StatementDocument { }
    record MeasureDocument(@Nullable String value,List<String> gapCodes) { }
    record PhysicalNodeDocument(String id,@Nullable String parent,int order,boolean filler,StorageFacts.Kind kind,
        @Nullable String data,MeasureDocument extent,Wire.ProvenanceDocument provenance) { }
    record BaseDocument(String id,MeasureDocument extent,StorageFacts.Allocation allocation,Wire.ProvenanceDocument provenance) { }
    record ViewDocument(String node,String base,MeasureDocument offset,MeasureDocument extent,@Nullable String codec,Wire.ProvenanceDocument provenance) { }
    record PhysicalStorageDocument(String version,StorageFacts.Profile profile,@Nullable String profileId,@Nullable String runtimeCodec,
        List<PhysicalNodeDocument> nodes,List<BaseDocument> bases,List<ViewDocument> views,List<String> gapCodes,List<Wire28.RelationDocument> relations,List<Wire29.RenamesDocument> renames) { }
    record SliceDocument(String offset,String extent) { }
    record RegionalAccessDocument(String view,@Nullable SliceDocument slice) { }
    record MoveTransferDocument(MoveSourceDocument source,ReferenceDocument target,RegionalMoveDocument effect) { }
    record RegionalMoveDocument(StorageFacts.MoveKind kind,List<Integer> bytes,List<String> gapCodes) { }
}
