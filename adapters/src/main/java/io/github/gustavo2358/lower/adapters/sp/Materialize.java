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
    static SpInput input(Wire13.Document d) {
        var unit = unitKey(d.unit(), null);
        return new SpInput(unit, policy(d.policy(), unit), d.dataDeclarations().stream().map(v -> {
            var scalar = Optional.ofNullable(v.scalarText()).map(t -> new ScalarText(t.logicalDomain(), t.logicalExtent(), t.storageClass(), t.declarationScope()));
            return new DataFact(new DataId(unit, v.id()), v.canonicalName(), Optional.ofNullable(v.picture()),
                provenance(v.provenance(), unit), v.coverage(), readiness(v.readiness(), unit), scalar);
        }).toList(), d.statements().stream().map(v -> statement(v, unit)).toList(), structure(d.structure(), unit),
            d.gaps().stream().map(v -> gap(v, unit)).toList(), coverage(d.coverage(), unit), entryInventory(d.entryInventory(), unit));
    }
    private static StatementFact statement(Wire13.StatementDocument value, UnitKey unit) {
        var h = statementHeader(value.header(), unit);
        return switch (value) {
            case Wire13.GobackDocument v -> new GobackFact(h, v.exit(), v.localContinuation());
            case Wire13.MoveDocument v -> {
                var source = v.source(); var target = v.target(); var binding = target.binding(); var next = v.normalContinuation();
                yield new MoveFact(h, new LiteralSource(new OperandId(h.id(), source.id()), source.kind(),
                    Optional.ofNullable(source.logicalValue()).map(t -> new LogicalValue(t.logicalDomain(), t.value(), t.logicalExtent())), provenance(source.provenance(), unit)),
                    new DataReference(new OperandId(h.id(), target.id()), target.role(),
                        new Binding(switch (binding.status()) {
                            case RESOLVED -> ResolutionStatus.RESOLVED; case AMBIGUOUS -> ResolutionStatus.AMBIGUOUS;
                            case UNRESOLVED -> ResolutionStatus.UNRESOLVED; case INPUT_MISSING -> ResolutionStatus.INPUT_MISSING;
                        }, binding.candidates().stream().map(c -> new DataId(unit, c.id())).toList(),
                            Optional.ofNullable(binding.selected()).map(id -> new DataId(unit, id)), Optional.of(ResolutionReason.valueOf(binding.reason().name())), binding.candidates().stream().map(Wire.CandidateDocument::canonicalName).toList()),
                        Optional.ofNullable(target.wholeItemAccess()).map(w -> new WholeItemAccess(new DataId(unit, w.data()))), provenance(target.provenance(), unit)),
                    v.copySemantics(), continuation(next, unit), Optional.ofNullable(v.textAdjustment()).map(a -> new TextAdjustment(a.rule(), a.receiverExtent(), logical(a.result()), provenance(a.provenance(), unit))));
            }
            case Wire13.CallDocument v -> {
                CallTarget target = switch (v.target()) {
                    case Wire13.DataTargetDocument d -> new DataCallTarget(reference(d.reference(), h.id(), unit));
                    case Wire13.LiteralTargetDocument l -> new LiteralCallTarget(new OperandId(h.id(), l.id()), l.text(), l.writtenText(),
                        Optional.ofNullable(l.logicalValue()).map(Materialize::logical), provenance(l.provenance(), unit));
                };
                var s = v.surface();
                yield new CallFact(h, v.syntax(), target, v.runtimeTarget(), v.runtimeUncertaintyCode(), continuation(v.normalContinuation(), unit),
                    new CallSurface(s.using(), Optional.ofNullable(s.argumentCount()), s.returning(), s.onException(), s.notOnException(), s.onOverflow()), v.effects(), v.outcomes());
            }
            case Wire13.IfDocument v -> new OtherStatement(h, Variant.IF);
            case Wire13.ObservedDocument v -> new OtherStatement(h, Variant.OBSERVED);
        };
    }
    private static LogicalValue logical(Wire13.LogicalDocument value) {
        return new LogicalValue(value.logicalDomain(), value.value(), value.logicalExtent());
    }
    private static NormalContinuation continuation(Wire13.ContinuationDocument next, UnitKey unit) {
        return new NormalContinuation(next.availability(), Optional.ofNullable(next.statement()).map(id -> new StatementId(unit, id)), provenance(next.provenance(), unit));
    }
    private static DataReference reference(Wire13.ReferenceDocument target, StatementId statement, UnitKey unit) {
        var binding = target.binding();
        return new DataReference(new OperandId(statement, target.id()), target.role(),
            new Binding(ResolutionStatus.valueOf(binding.status().name()), binding.candidates().stream().map(c -> new DataId(unit, c.id())).toList(),
                Optional.ofNullable(binding.selected()).map(id -> new DataId(unit, id)), Optional.of(ResolutionReason.valueOf(binding.reason().name())),
                binding.candidates().stream().map(Wire.CandidateDocument::canonicalName).toList()),
            Optional.ofNullable(target.wholeItemAccess()).map(w -> new WholeItemAccess(new DataId(unit, w.data()))), provenance(target.provenance(), unit));
    }
    static SpInput input(Wire14.Document d) {
        var unit = unitKey(d.unit(), null);
        return new SpInput(unit, policy(d.policy(), unit), d.dataDeclarations().stream().map(v -> {
            var scalar = Optional.ofNullable(v.scalarText()).map(t -> new ScalarText(t.logicalDomain(), t.logicalExtent(), t.storageClass(), t.declarationScope()));
            return new DataFact(new DataId(unit, v.id()), v.canonicalName(), Optional.ofNullable(v.picture()),
                provenance(v.provenance(), unit), v.coverage(), readiness(v.readiness(), unit), scalar);
        }).toList(), d.statements().stream().map(v -> statement(v, unit)).toList(), structure(d.structure(), unit),
            d.gaps().stream().map(v -> gap(v, unit)).toList(), coverage(d.coverage(), unit), entryInventory(d.entryInventory(), unit), Optional.of(storage(d.storageIndependence(), unit)));
    }
    private static StatementFact statement(Wire14.StatementDocument value, UnitKey unit) {
        var h = statementHeader(value.header(), unit);
        return switch (value) {
            case Wire14.GobackDocument v -> new GobackFact(h, v.exit(), v.localContinuation());
            case Wire14.MoveDocument v -> {
                var source = v.source(); var target = v.target(); var binding = target.binding(); var next = v.normalContinuation();
                yield new MoveFact(h, new LiteralSource(new OperandId(h.id(), source.id()), source.kind(),
                    Optional.ofNullable(source.logicalValue()).map(t -> new LogicalValue(t.logicalDomain(), t.value(), t.logicalExtent())), provenance(source.provenance(), unit)),
                    new DataReference(new OperandId(h.id(), target.id()), target.role(),
                        new Binding(switch (binding.status()) {
                            case RESOLVED -> ResolutionStatus.RESOLVED; case AMBIGUOUS -> ResolutionStatus.AMBIGUOUS;
                            case UNRESOLVED -> ResolutionStatus.UNRESOLVED; case INPUT_MISSING -> ResolutionStatus.INPUT_MISSING;
                        }, binding.candidates().stream().map(c -> new DataId(unit, c.id())).toList(),
                            Optional.ofNullable(binding.selected()).map(id -> new DataId(unit, id)), Optional.of(ResolutionReason.valueOf(binding.reason().name())), binding.candidates().stream().map(Wire.CandidateDocument::canonicalName).toList()),
                        Optional.ofNullable(target.wholeItemAccess()).map(w -> new WholeItemAccess(new DataId(unit, w.data()))), provenance(target.provenance(), unit)),
                    v.copySemantics(), continuation(next, unit), Optional.ofNullable(v.textAdjustment()).map(a -> new TextAdjustment(a.rule(), a.receiverExtent(), logical(a.result()), provenance(a.provenance(), unit))));
            }
            case Wire14.CallDocument v -> {
                CallTarget target = switch (v.target()) {
                    case Wire14.DataTargetDocument d -> new DataCallTarget(reference(d.reference(), h.id(), unit));
                    case Wire14.LiteralTargetDocument l -> new LiteralCallTarget(new OperandId(h.id(), l.id()), l.text(), l.writtenText(),
                        Optional.ofNullable(l.logicalValue()).map(Materialize::logical), provenance(l.provenance(), unit));
                };
                var s = v.surface();
                yield new CallFact(h, v.syntax(), target, v.runtimeTarget(), v.runtimeUncertaintyCode(), continuation(v.normalContinuation(), unit),
                    new CallSurface(s.using(), Optional.ofNullable(s.argumentCount()), s.returning(), s.onException(), s.notOnException(), s.onOverflow()), v.effects(), v.outcomes());
            }
            case Wire14.IfDocument v -> {
                var condition=v.condition(); var p=condition.predicate();
                var predicate=new PredicateGuarantee(p.availability(),p.profile(),p.resultDomain(),p.evaluation(),p.normalCompletion(),
                    p.readsCompleteness(),p.truthValue(),p.knownReads().stream().map(id -> new OperandId(h.id(),id)).toList(),provenance(p.provenance(),unit),p.gapCodes());
                yield new IfFact(h,condition.shape(),predicate,condition.references().stream().map(r -> reference(r,h.id(),unit)).toList(),
                    provenance(condition.provenance(),unit),v.explicitlyTerminated(),Optional.ofNullable(v.continuation()).map(id -> new StatementId(unit,id)),
                    continuation(v.normalContinuation(),unit),arm(v.thenArm(),unit),arm(v.elseArm(),unit),v.profile());
            }
            case Wire14.ObservedDocument v -> new OtherStatement(h, Variant.OBSERVED);
        };
    }
    private static LogicalValue logical(Wire14.LogicalDocument value) {
        return new LogicalValue(value.logicalDomain(), value.value(), value.logicalExtent());
    }
    private static NormalContinuation continuation(Wire14.ContinuationDocument next, UnitKey unit) {
        return new NormalContinuation(next.availability(), Optional.ofNullable(next.statement()).map(id -> new StatementId(unit, id)), provenance(next.provenance(), unit));
    }
    private static DataReference reference(Wire14.ReferenceDocument target, StatementId statement, UnitKey unit) {
        var binding = target.binding();
        return new DataReference(new OperandId(statement, target.id()), target.role(),
            new Binding(ResolutionStatus.valueOf(binding.status().name()), binding.candidates().stream().map(c -> new DataId(unit, c.id())).toList(),
                Optional.ofNullable(binding.selected()).map(id -> new DataId(unit, id)), Optional.of(ResolutionReason.valueOf(binding.reason().name())),
                binding.candidates().stream().map(Wire.CandidateDocument::canonicalName).toList()),
            Optional.ofNullable(target.wholeItemAccess()).map(w -> new WholeItemAccess(new DataId(unit, w.data()))), provenance(target.provenance(), unit));
    }
    private static IfArm arm(Wire14.ArmDocument a, UnitKey unit) {
        return new IfArm(a.presence(),a.contentAvailability(),executableStart(a.entry(),unit),provenance(a.provenance(),unit),a.gapCodes());
    }
    private static IndependentStorageSet storage(Wire14.StorageDocument s, UnitKey unit) {
        return new IndependentStorageSet(s.availability(), s.rule(), s.authority(), s.members().stream().map(id -> new DataId(unit,id)).toList(),
            Optional.ofNullable(s.provenance()).map(p -> provenance(p,unit)), s.gapCodes());
    }
    static SpInput input(Wire15.Document d) {
        var unit = unitKey(d.unit(), null);
        return new SpInput(unit, policy(d.policy(), unit), d.dataDeclarations().stream().map(v -> {
            var scalar = Optional.ofNullable(v.scalarText()).map(t -> new ScalarText(t.logicalDomain(), t.logicalExtent(), t.storageClass(), t.declarationScope()));
            return new DataFact(new DataId(unit, v.id()), v.canonicalName(), Optional.ofNullable(v.picture()),
                provenance(v.provenance(), unit), v.coverage(), readiness(v.readiness(), unit), scalar);
        }).toList(), d.statements().stream().map(v -> statement(v, unit)).toList(), structure(d.structure(), unit),
            d.gaps().stream().map(v -> gap(v, unit)).toList(), coverage(d.coverage(), unit), entryInventory(d.entryInventory(), unit), Optional.of(storage(d.storageIndependence(), unit)));
    }
    private static StatementFact statement(Wire15.StatementDocument value, UnitKey unit) {
        var h = statementHeader(value.header(), unit);
        return switch (value) {
            case Wire15.GobackDocument v -> new GobackFact(h, v.exit(), v.localContinuation());
            case Wire15.MoveDocument v -> {
                MoveSource source = switch (v.source()) {
                    case Wire15.LiteralDocument literal -> new LiteralSource(new OperandId(h.id(), literal.id()), literal.kind(),
                        Optional.ofNullable(literal.logicalValue()).map(Materialize::logical), provenance(literal.provenance(), unit));
                    case Wire15.DataSourceDocument data -> reference(data.reference(), h.id(), unit);
                };
                yield new MoveFact(h, source, reference(v.target(), h.id(), unit), v.copySemantics(), continuation(v.normalContinuation(), unit),
                    Optional.ofNullable(v.textAdjustment()).map(adjustment -> new TextAdjustment(adjustment.rule(), adjustment.receiverExtent(),
                        logical(adjustment.result()), provenance(adjustment.provenance(), unit))));
            }
            case Wire15.CallDocument v -> {
                CallTarget target = switch (v.target()) {
                    case Wire15.DataTargetDocument d -> new DataCallTarget(reference(d.reference(), h.id(), unit));
                    case Wire15.LiteralTargetDocument l -> new LiteralCallTarget(new OperandId(h.id(), l.id()), l.text(), l.writtenText(),
                        Optional.ofNullable(l.logicalValue()).map(Materialize::logical), provenance(l.provenance(), unit));
                };
                var s = v.surface();
                yield new CallFact(h, v.syntax(), target, v.runtimeTarget(), v.runtimeUncertaintyCode(), continuation(v.normalContinuation(), unit),
                    new CallSurface(s.using(), Optional.ofNullable(s.argumentCount()), s.returning(), s.onException(), s.notOnException(), s.onOverflow()), v.effects(), v.outcomes());
            }
            case Wire15.IfDocument v -> {
                var condition=v.condition(); var p=condition.predicate();
                var predicate=new PredicateGuarantee(p.availability(),p.profile(),p.resultDomain(),p.evaluation(),p.normalCompletion(),
                    p.readsCompleteness(),p.truthValue(),p.knownReads().stream().map(id -> new OperandId(h.id(),id)).toList(),provenance(p.provenance(),unit),p.gapCodes());
                yield new IfFact(h,condition.shape(),predicate,condition.references().stream().map(r -> reference(r,h.id(),unit)).toList(),
                    provenance(condition.provenance(),unit),v.explicitlyTerminated(),Optional.ofNullable(v.continuation()).map(id -> new StatementId(unit,id)),
                    continuation(v.normalContinuation(),unit),arm(v.thenArm(),unit),arm(v.elseArm(),unit),v.profile());
            }
            case Wire15.ObservedDocument v -> new OtherStatement(h, Variant.OBSERVED);
        };
    }
    private static LogicalValue logical(Wire15.LogicalDocument value) {
        return new LogicalValue(value.logicalDomain(), value.value(), value.logicalExtent());
    }
    private static NormalContinuation continuation(Wire15.ContinuationDocument next, UnitKey unit) {
        return new NormalContinuation(next.availability(), Optional.ofNullable(next.statement()).map(id -> new StatementId(unit, id)), provenance(next.provenance(), unit));
    }
    private static DataReference reference(Wire15.ReferenceDocument target, StatementId statement, UnitKey unit) {
        var binding = target.binding();
        return new DataReference(new OperandId(statement, target.id()), target.role(),
            new Binding(ResolutionStatus.valueOf(binding.status().name()), binding.candidates().stream().map(c -> new DataId(unit, c.id())).toList(),
                Optional.ofNullable(binding.selected()).map(id -> new DataId(unit, id)), Optional.of(ResolutionReason.valueOf(binding.reason().name())),
                binding.candidates().stream().map(Wire.CandidateDocument::canonicalName).toList()),
            Optional.ofNullable(target.wholeItemAccess()).map(w -> new WholeItemAccess(new DataId(unit, w.data()))), provenance(target.provenance(), unit));
    }
    private static IfArm arm(Wire15.ArmDocument a, UnitKey unit) {
        return new IfArm(a.presence(),a.contentAvailability(),executableStart(a.entry(),unit),provenance(a.provenance(),unit),a.gapCodes());
    }
    private static IndependentStorageSet storage(Wire15.StorageDocument s, UnitKey unit) {
        return new IndependentStorageSet(s.availability(), s.rule(), s.authority(), s.members().stream().map(id -> new DataId(unit,id)).toList(),
            Optional.ofNullable(s.provenance()).map(p -> provenance(p,unit)), s.gapCodes());
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
