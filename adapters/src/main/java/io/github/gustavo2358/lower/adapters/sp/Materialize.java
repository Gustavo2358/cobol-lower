package io.github.gustavo2358.lower.adapters.sp;

import io.github.gustavo2358.lower.domain.SpInput;
import java.util.Optional;
import static io.github.gustavo2358.lower.domain.SpInput.*;

/** Mechanical materialization only. Semantic validation belongs to the inner application. */
final class Materialize {
    private Materialize() { }
    static SpInput input(Wire.Document d) {
        var unit = unitKey(d.unit(), null);
        return new SpInput(unit, policy(d.policy(), unit), d.dataDeclarations().stream().map(v -> dataFact(v, unit)).toList(),
            d.statements().stream().map(v -> statement(v, unit)).toList(), structure(d.structure(), unit),
            d.gaps().stream().map(v -> gap(v, unit)).toList(), coverage(d.coverage(), unit), entryInventory(d.entryInventory(), unit));
    }
    static SpInput input(Wire12.Document d) {
        var unit = unitKey(d.unit(), null);
        return new SpInput(unit, policy(d.policy(), unit), d.dataDeclarations().stream().map(v -> {
            var scalar = Optional.ofNullable(v.scalarText()).map(t -> new ScalarText(t.logicalDomain(), t.logicalExtent(), t.storageClass(), t.declarationScope()));
            return new DataFact(new DataId(unit, v.id()), v.canonicalName(), Optional.ofNullable(v.picture()),
                provenance(v.provenance(), unit), v.coverage(), readiness(v.readiness(), unit), scalar);
        }).toList(), d.statements().stream().map(v -> statement(v, unit)).toList(), structure(d.structure(), unit),
            d.gaps().stream().map(v -> gap(v, unit)).toList(), coverage(d.coverage(), unit), entryInventory(d.entryInventory(), unit));
    }
    private static StatementFact statement(Wire12.StatementDocument value, UnitKey unit) {
        var h = statementHeader(value.header(), unit);
        return switch (value) {
            case Wire12.GobackDocument v -> new GobackFact(h, v.exit(), v.localContinuation());
            case Wire12.MoveDocument v -> {
                var source = v.source(); var target = v.target(); var binding = target.binding(); var next = v.normalContinuation();
                yield new MoveFact(h, new LiteralSource(new OperandId(h.id(), source.id()), source.kind(),
                    Optional.ofNullable(source.logicalValue()).map(t -> new LogicalValue(t.logicalDomain(), t.value(), t.logicalExtent())), provenance(source.provenance(), unit)),
                    new DataReference(new OperandId(h.id(), target.id()), target.role(),
                        new Binding(switch (binding.status()) {
                            case RESOLVED -> ResolutionStatus.RESOLVED; case AMBIGUOUS -> ResolutionStatus.AMBIGUOUS;
                            case UNRESOLVED -> ResolutionStatus.UNRESOLVED; case INPUT_MISSING -> ResolutionStatus.INPUT_MISSING;
                        }, binding.candidates().stream().map(c -> new DataId(unit, c.id())).toList(),
                            Optional.ofNullable(binding.selected()).map(id -> new DataId(unit, id))),
                        Optional.ofNullable(target.wholeItemAccess()).map(w -> new WholeItemAccess(new DataId(unit, w.data()))), provenance(target.provenance(), unit)),
                    v.copySemantics(), new NormalContinuation(next.availability(), Optional.ofNullable(next.statement()).map(id -> new StatementId(unit, id)), provenance(next.provenance(), unit)));
            }
            case Wire12.CallDocument v -> new OtherStatement(h, Variant.CALL);
            case Wire12.IfDocument v -> new OtherStatement(h, Variant.IF);
            case Wire12.ObservedDocument v -> new OtherStatement(h, Variant.OBSERVED);
        };
    }
    private static StatementFact statement(Wire.StatementDocument value, UnitKey unit) {
        return switch (value) {
            case Wire.GobackFactDocument v -> gobackFact(v, unit);
            case Wire.MoveDocument v -> new OtherStatement(statementHeader(v.header(), unit), Variant.MOVE);
            case Wire.CallDocument v -> new OtherStatement(statementHeader(v.header(), unit), Variant.CALL);
            case Wire.IfDocument v -> new OtherStatement(statementHeader(v.header(), unit), Variant.IF);
            case Wire.ObservedDocument v -> new OtherStatement(statementHeader(v.header(), unit), Variant.OBSERVED);
        };
    }
    private static UnitKey unitKey(Wire.UnitKeyDocument d, UnitKey unit) {
        return new UnitKey(d.compilationUnitId(), d.structuralPath(), d.canonicalProgramName());
    }
    private static Policy policy(Wire.PolicyDocument d, UnitKey unit) {
        return new Policy(d.policyId(), d.version(), d.qualifyMode(), d.pgmnameMode(), d.dynamMode(), d.dllMode());
    }
    private static Location location(Wire.LocationDocument d, UnitKey unit) {
        return new Location(d.file(), d.startLine(), d.startColumn(), d.endLine(), d.endColumn());
    }
    private static IncludeFrame includeFrame(Wire.IncludeFrameDocument d, UnitKey unit) {
        return new IncludeFrame(d.includingFile(), d.requestedName(), d.includedFile(), d.includeLine());
    }
    private static Provenance provenance(Wire.ProvenanceDocument d, UnitKey unit) {
        return new Provenance(location(d.expanded(), unit), location(d.original(), unit), d.includeChain().stream().map(v -> includeFrame(v, unit)).toList(), d.exact());
    }
    private static ReadinessClaim readinessClaim(Wire.ReadinessClaimDocument d, UnitKey unit) {
        return new ReadinessClaim(d.status(), d.scope());
    }
    private static Readiness readiness(Wire.ReadinessDocument d, UnitKey unit) {
        return new Readiness(readinessClaim(d.lowering(), unit), readinessClaim(d.cfg(), unit), readinessClaim(d.effectsDataflow(), unit));
    }
    private static DataFact dataFact(Wire.DataFactDocument d, UnitKey unit) {
        return new DataFact(new DataId(unit, d.id()), d.canonicalName(), Optional.ofNullable(d.picture()), provenance(d.provenance(), unit), d.coverage(), readiness(d.readiness(), unit));
    }
    private static Containment containment(Wire.ContainmentDocument d, UnitKey unit) {
        return new Containment(Optional.ofNullable(d.parent()).map(v -> new StatementId(unit, v)), d.branch());
    }
    private static StatementHeader statementHeader(Wire.StatementHeaderDocument d, UnitKey unit) {
        return new StatementHeader(new StatementId(unit, d.id()), d.programPoint(), containment(d.containment(), unit), provenance(d.provenance(), unit), d.coverage(), readiness(d.readiness(), unit));
    }
    private static GobackFact gobackFact(Wire.GobackFactDocument d, UnitKey unit) {
        return new GobackFact(statementHeader(d.header(), unit), d.exit(), d.localContinuation());
    }
    private static Gap gap(Wire.GapDocument d, UnitKey unit) {
        return new Gap(new StatementId(unit, d.statement()), d.scope(), d.code(), d.detail(), provenance(d.provenance(), unit));
    }
    private static EntryGap entryGap(Wire.EntryGapDocument d, UnitKey unit) {
        return new EntryGap(d.scope(), d.code(), d.detail(), provenance(d.provenance(), unit));
    }
    private static ExecutableStart executableStart(Wire.ExecutableStartDocument d, UnitKey unit) {
        return new ExecutableStart(d.availability(), Optional.ofNullable(d.statement()).map(v -> new StatementId(unit, v)));
    }
    private static EntrySignature entrySignature(Wire.EntrySignatureDocument d, UnitKey unit) {
        return new EntrySignature(d.availability(), Optional.ofNullable(d.parameterCount()), d.returningClause());
    }
    private static EntryFact entryFact(Wire.EntryFactDocument d, UnitKey unit) {
        return new EntryFact(new EntryId(unit, d.id()), d.role(), d.availability(), executableStart(d.start(), unit), entrySignature(d.signature(), unit), provenance(d.provenance(), unit), d.coverage(), readiness(d.readiness(), unit), d.gaps().stream().map(v -> entryGap(v, unit)).toList());
    }
    private static EntryInventory entryInventory(Wire.EntryInventoryDocument d, UnitKey unit) {
        return new EntryInventory(d.status(), d.scope(), d.entries().stream().map(v -> entryFact(v, unit)).toList(), d.gapCodes());
    }
    private static BranchChildren branchChildren(Wire.BranchChildrenDocument d, UnitKey unit) {
        return new BranchChildren(new StatementId(unit, d.parent()), d.branch(), d.children().stream().map(v -> new StatementId(unit, v)).toList());
    }
    private static Structure structure(Wire.StructureDocument d, UnitKey unit) {
        return new Structure(d.roots().stream().map(v -> new StatementId(unit, v)).toList(), d.branches().stream().map(v -> branchChildren(v, unit)).toList());
    }
    private static Coverage coverage(Wire.CoverageDocument d, UnitKey unit) {
        return new Coverage(d.inventoryStatus(), d.observedStatements(), d.modeledStatements(), d.partialStatements(), d.unsupportedStatements(), d.inputMissingStatements(), readiness(d.readiness(), unit));
    }
}
