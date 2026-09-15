package io.github.gustavo2358.lower.adapters.sp;

import io.github.gustavo2358.lower.domain.SpInput;
import io.github.gustavo2358.lower.domain.StorageFacts;
import java.util.Optional;
import java.util.List;
import static io.github.gustavo2358.lower.domain.SpInput.*;

/** Mechanical materialization only. Semantic validation belongs to the inner application. */
final class Materialize {
    private Materialize() { }
    static final class EffectShape extends IllegalArgumentException { private static final long serialVersionUID=1L; EffectShape(String message){super(message);} }
    static SpInput input(Wire217.Document wire) {
        var common=input(Wire217.common(wire));var effects=new java.util.HashMap<StatementId,EffectSummary>();
        for(var e:wire.statementEffects()) {
            if(!e.version().equals("1.0.0"))throw new EffectShape("unsupported statement effect version");
            var statement=new StatementId(common.unit(),e.statement());
            java.util.function.Function<List<String>,List<OperandId>> operands=xs->xs.stream().map(id->new OperandId(statement,id)).toList();
            var summary=new EffectSummary(operands.apply(e.knownReads()),operands.apply(e.mayWrites()),operands.apply(e.mustOverwrite()),operands.apply(e.exposedRegions()),
                e.unknownReadBound(),e.unknownWriteBound(),e.unknownExposureBound(),e.environment(),e.values(),e.proof());
            if(effects.putIfAbsent(statement,summary)!=null)throw new EffectShape("duplicate statement effect");
        }
        var statements=new java.util.ArrayList<StatementFact>();
        for(var s:common.statements()) {
            var e=effects.remove(s.header().id());
            if(e==null)statements.add(s);
            else if(s instanceof OtherStatement o)statements.add(new OtherStatement(o.header(),o.variant(),o.observedKind(),o.observedShape(),o.gapCode(),o.normalContinuation(),o.knownReferences(),Optional.of(e)));
            else throw new EffectShape("effect summary requires an observed statement");
        }
        if(!effects.isEmpty())throw new EffectShape("unresolved effect owner");
        return new SpInput(common.unit(),common.policy(),common.dataDeclarations(),statements,common.structure(),common.gaps(),common.coverage(),common.entryInventory(),common.storageIndependence(),common.compositional(),common.storage());
    }
    private static OtherStatement observed(StatementHeader header, String kind, String shape, String gapCode) {
        return observed(header, kind, shape, gapCode,
            new NormalContinuation(ContinuationAvailability.UNAVAILABLE, Optional.empty(), header.provenance()), List.of());
    }
    private static OtherStatement observed(StatementHeader header, String kind, String shape, String gapCode,
            NormalContinuation continuation, List<DataReference> knownReferences) {
        return new OtherStatement(header, Variant.OBSERVED, kind, Optional.of(shape), gapCode, continuation, knownReferences);
    }
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
            case Wire12.CallDocument v -> OtherStatement.unsupported(h, Variant.CALL);
            case Wire12.IfDocument v -> OtherStatement.unsupported(h, Variant.IF);
            case Wire12.ObservedDocument v -> observed(h, v.observedKind(), v.observedShape(), v.gapCode());
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
            case Wire13.IfDocument v -> OtherStatement.unsupported(h, Variant.IF);
            case Wire13.ObservedDocument v -> observed(h, v.observedKind(), v.observedShape(), v.gapCode());
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
            case Wire14.ObservedDocument v -> observed(h, v.observedKind(), v.observedShape(), v.gapCode());
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
            case Wire15.ObservedDocument v -> observed(h, v.observedKind(), v.observedShape(), v.gapCode());
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
    static SpInput input(Wire16.Document d) {
        var unit = unitKey(d.unit(), null);
        return new SpInput(unit, policy(d.policy(), unit), d.dataDeclarations().stream().map(v -> {
            var scalar = Optional.ofNullable(v.scalarText()).map(t -> new ScalarText(t.logicalDomain(), t.logicalExtent(), t.storageClass(), t.declarationScope()));
            return new DataFact(new DataId(unit, v.id()), v.canonicalName(), Optional.ofNullable(v.picture()),
                provenance(v.provenance(), unit), v.coverage(), readiness(v.readiness(), unit), scalar);
        }).toList(), d.statements().stream().map(v -> statement(v, unit)).toList(), structure(d.structure(), unit),
            d.gaps().stream().map(v -> gap(v, unit)).toList(), coverage(d.coverage(), unit), entryInventory(d.entryInventory(), unit), Optional.of(storage(d.storageIndependence(), unit)));
    }
    private static StatementFact statement(Wire16.StatementDocument value, UnitKey unit) {
        var h = statementHeader(value.header(), unit);
        return switch (value) {
            case Wire16.PerformDocument v -> new PerformFact(h, v.profile(), Optional.ofNullable(v.target()).map(t ->
                new PerformTarget(new ProcedureId(unit, t.id()), provenance(t.referenceOrigin(), unit), provenance(t.paragraphOrigin(), unit))),
                Optional.ofNullable(v.targetEntry()).map(id -> new StatementId(unit, id)), v.targetStatements().stream().map(id -> new StatementId(unit,id)).toList(),
                Optional.ofNullable(v.targetExit()).map(id -> new StatementId(unit,id)), continuation(v.normalContinuation(), unit),
                v.primaryStatements().stream().map(id -> new StatementId(unit,id)).toList(), v.gapCodes());
            case Wire16.GobackDocument v -> new GobackFact(h, v.exit(), v.localContinuation());
            case Wire16.MoveDocument v -> {
                MoveSource source = switch (v.source()) {
                    case Wire16.LiteralDocument literal -> new LiteralSource(new OperandId(h.id(), literal.id()), literal.kind(),
                        Optional.ofNullable(literal.logicalValue()).map(Materialize::logical), provenance(literal.provenance(), unit));
                    case Wire16.DataSourceDocument data -> reference(data.reference(), h.id(), unit);
                };
                yield new MoveFact(h, source, reference(v.target(), h.id(), unit), v.copySemantics(), continuation(v.normalContinuation(), unit),
                    Optional.ofNullable(v.textAdjustment()).map(adjustment -> new TextAdjustment(adjustment.rule(), adjustment.receiverExtent(),
                        logical(adjustment.result()), provenance(adjustment.provenance(), unit))));
            }
            case Wire16.CallDocument v -> {
                CallTarget target = switch (v.target()) {
                    case Wire16.DataTargetDocument d -> new DataCallTarget(reference(d.reference(), h.id(), unit));
                    case Wire16.LiteralTargetDocument l -> new LiteralCallTarget(new OperandId(h.id(), l.id()), l.text(), l.writtenText(),
                        Optional.ofNullable(l.logicalValue()).map(Materialize::logical), provenance(l.provenance(), unit));
                };
                var s = v.surface();
                yield new CallFact(h, v.syntax(), target, v.runtimeTarget(), v.runtimeUncertaintyCode(), continuation(v.normalContinuation(), unit),
                    new CallSurface(s.using(), Optional.ofNullable(s.argumentCount()), s.returning(), s.onException(), s.notOnException(), s.onOverflow()), v.effects(), v.outcomes());
            }
            case Wire16.IfDocument v -> {
                var condition=v.condition(); var p=condition.predicate();
                var predicate=new PredicateGuarantee(p.availability(),p.profile(),p.resultDomain(),p.evaluation(),p.normalCompletion(),
                    p.readsCompleteness(),p.truthValue(),p.knownReads().stream().map(id -> new OperandId(h.id(),id)).toList(),provenance(p.provenance(),unit),p.gapCodes());
                yield new IfFact(h,condition.shape(),predicate,condition.references().stream().map(r -> reference(r,h.id(),unit)).toList(),
                    provenance(condition.provenance(),unit),v.explicitlyTerminated(),Optional.ofNullable(v.continuation()).map(id -> new StatementId(unit,id)),
                    continuation(v.normalContinuation(),unit),arm(v.thenArm(),unit),arm(v.elseArm(),unit),v.profile());
            }
            case Wire16.ObservedDocument v -> observed(h, v.observedKind(), v.observedShape(), v.gapCode());
        };
    }
    private static LogicalValue logical(Wire16.LogicalDocument value) {
        return new LogicalValue(value.logicalDomain(), value.value(), value.logicalExtent());
    }
    private static NormalContinuation continuation(Wire16.ContinuationDocument next, UnitKey unit) {
        return new NormalContinuation(next.availability(), Optional.ofNullable(next.statement()).map(id -> new StatementId(unit, id)), provenance(next.provenance(), unit));
    }
    private static DataReference reference(Wire16.ReferenceDocument target, StatementId statement, UnitKey unit) {
        var binding = target.binding();
        return new DataReference(new OperandId(statement, target.id()), target.role(),
            new Binding(ResolutionStatus.valueOf(binding.status().name()), binding.candidates().stream().map(c -> new DataId(unit, c.id())).toList(),
                Optional.ofNullable(binding.selected()).map(id -> new DataId(unit, id)), Optional.of(ResolutionReason.valueOf(binding.reason().name())),
                binding.candidates().stream().map(Wire.CandidateDocument::canonicalName).toList()),
            Optional.ofNullable(target.wholeItemAccess()).map(w -> new WholeItemAccess(new DataId(unit, w.data()))), provenance(target.provenance(), unit));
    }
    private static IfArm arm(Wire16.ArmDocument a, UnitKey unit) {
        return new IfArm(a.presence(),a.contentAvailability(),executableStart(a.entry(),unit),provenance(a.provenance(),unit),a.gapCodes());
    }
    private static IndependentStorageSet storage(Wire16.StorageDocument s, UnitKey unit) {
        return new IndependentStorageSet(s.availability(), s.rule(), s.authority(), s.members().stream().map(id -> new DataId(unit,id)).toList(),
            Optional.ofNullable(s.provenance()).map(p -> provenance(p,unit)), s.gapCodes());
    }
    static SpInput input(Wire18.Document d) {
        var unit = unitKey(d.unit(), null);
        return new SpInput(unit, policy(d.policy(), unit), d.dataDeclarations().stream().map(v -> {
            var scalar = Optional.ofNullable(v.scalarText()).map(t -> new ScalarText(t.logicalDomain(), t.logicalExtent(), t.storageClass(), t.declarationScope()));
            return new DataFact(new DataId(unit, v.id()), v.canonicalName(), Optional.ofNullable(v.picture()),
                provenance(v.provenance(), unit), v.coverage(), readiness(v.readiness(), unit), scalar);
        }).toList(), d.statements().stream().map(v -> statement(v, unit)).toList(), structure(d.structure(), unit),
            d.gaps().stream().map(v -> gap(v, unit)).toList(), coverage(d.coverage(), unit), entryInventory(d.entryInventory(), unit), Optional.of(storage(d.storageIndependence(), unit)), true);
    }
    private static StatementFact statement(Wire18.StatementDocument value, UnitKey unit) {
        var h = statementHeader(value.header(), unit);
        return switch (value) {
            case Wire18.PerformDocument v -> new PerformFact(h, v.profile(), Optional.ofNullable(v.target()).map(t ->
                new PerformTarget(new ProcedureId(unit, t.id()), provenance(t.referenceOrigin(), unit), provenance(t.paragraphOrigin(), unit))),
                Optional.ofNullable(v.targetEntry()).map(id -> new StatementId(unit, id)), v.targetStatements().stream().map(id -> new StatementId(unit,id)).toList(),
                Optional.ofNullable(v.targetExit()).map(id -> new StatementId(unit,id)), continuation(v.normalContinuation(), unit),
                List.of(), v.gapCodes());
            case Wire18.GobackDocument v -> new GobackFact(h, v.exit(), v.localContinuation());
            case Wire18.MoveDocument v -> {
                MoveSource source = switch (v.source()) {
                    case Wire18.LiteralDocument literal -> new LiteralSource(new OperandId(h.id(), literal.id()), literal.kind(),
                        Optional.ofNullable(literal.logicalValue()).map(Materialize::logical), provenance(literal.provenance(), unit));
                    case Wire18.DataSourceDocument data -> reference(data.reference(), h.id(), unit);
                };
                yield new MoveFact(h, source, reference(v.target(), h.id(), unit), v.copySemantics(), continuation(v.normalContinuation(), unit),
                    Optional.ofNullable(v.textAdjustment()).map(adjustment -> new TextAdjustment(adjustment.rule(), adjustment.receiverExtent(),
                        logical(adjustment.result()), provenance(adjustment.provenance(), unit))));
            }
            case Wire18.CallDocument v -> {
                CallTarget target = switch (v.target()) {
                    case Wire18.DataTargetDocument d -> new DataCallTarget(reference(d.reference(), h.id(), unit));
                    case Wire18.LiteralTargetDocument l -> new LiteralCallTarget(new OperandId(h.id(), l.id()), l.text(), l.writtenText(),
                        Optional.ofNullable(l.logicalValue()).map(Materialize::logical), provenance(l.provenance(), unit));
                };
                var s = v.surface();
                yield new CallFact(h, v.syntax(), target, v.runtimeTarget(), v.runtimeUncertaintyCode(), continuation(v.normalContinuation(), unit),
                    new CallSurface(s.using(), Optional.ofNullable(s.argumentCount()), s.returning(), s.onException(), s.notOnException(), s.onOverflow()), v.effects(), v.outcomes());
            }
            case Wire18.IfDocument v -> {
                var condition=v.condition(); var p=condition.predicate();
                var predicate=new PredicateGuarantee(p.availability(),p.profile(),p.resultDomain(),p.evaluation(),p.normalCompletion(),
                    p.readsCompleteness(),p.truthValue(),p.knownReads().stream().map(id -> new OperandId(h.id(),id)).toList(),provenance(p.provenance(),unit),p.gapCodes());
                yield new IfFact(h,condition.shape(),predicate,condition.references().stream().map(r -> reference(r,h.id(),unit)).toList(),
                    provenance(condition.provenance(),unit),v.explicitlyTerminated(),Optional.ofNullable(v.continuation()).map(id -> new StatementId(unit,id)),
                    continuation(v.normalContinuation(),unit),arm(v.thenArm(),unit),arm(v.elseArm(),unit),v.profile());
            }
            case Wire18.ObservedDocument v -> observed(h, v.observedKind(), v.observedShape(), v.gapCode(), continuation(v.normalContinuation(),unit), v.knownReferences().stream().map(r->reference(r,h.id(),unit)).toList());
        };
    }
    private static LogicalValue logical(Wire18.LogicalDocument value) {
        return new LogicalValue(value.logicalDomain(), value.value(), value.logicalExtent());
    }
    private static NormalContinuation continuation(Wire18.ContinuationDocument next, UnitKey unit) {
        return new NormalContinuation(next.availability(), Optional.ofNullable(next.statement()).map(id -> new StatementId(unit, id)), provenance(next.provenance(), unit));
    }
    private static DataReference reference(Wire18.ReferenceDocument target, StatementId statement, UnitKey unit) {
        var binding = target.binding();
        return new DataReference(new OperandId(statement, target.id()), target.role(),
            new Binding(ResolutionStatus.valueOf(binding.status().name()), binding.candidates().stream().map(c -> new DataId(unit, c.id())).toList(),
                Optional.ofNullable(binding.selected()).map(id -> new DataId(unit, id)), Optional.of(ResolutionReason.valueOf(binding.reason().name())),
                binding.candidates().stream().map(Wire.CandidateDocument::canonicalName).toList()),
            Optional.ofNullable(target.wholeItemAccess()).map(w -> new WholeItemAccess(new DataId(unit, w.data()))), provenance(target.provenance(), unit));
    }
    private static IfArm arm(Wire18.ArmDocument a, UnitKey unit) {
        return new IfArm(a.presence(),a.contentAvailability(),executableStart(a.entry(),unit),provenance(a.provenance(),unit),a.gapCodes());
    }
    private static IndependentStorageSet storage(Wire18.StorageDocument s, UnitKey unit) {
        return new IndependentStorageSet(s.availability(), s.rule(), s.authority(), s.members().stream().map(id -> new DataId(unit,id)).toList(),
            Optional.ofNullable(s.provenance()).map(p -> provenance(p,unit)), s.gapCodes());
    }
    static SpInput input(Wire20.Document d) {
        var unit = unitKey(d.unit(), null);
        return new SpInput(unit, policy(d.policy(), unit), d.dataDeclarations().stream().map(v -> {
            var scalar = Optional.ofNullable(v.scalarText()).map(t -> new ScalarText(t.logicalDomain(), t.logicalExtent(), t.storageClass(), t.declarationScope()));
            return new DataFact(new DataId(unit, v.id()), v.canonicalName(), Optional.ofNullable(v.picture()),
                provenance(v.provenance(), unit), v.coverage(), readiness(v.readiness(), unit), scalar);
        }).toList(), d.statements().stream().map(v -> statement(v, unit)).toList(), structure(d.structure(), unit),
            d.gaps().stream().map(v -> gap(v, unit)).toList(), coverage(d.coverage(), unit), entryInventory(d.entryInventory(), unit), Optional.of(storage(d.storageIndependence(), unit)), true);
    }
    private static StatementFact statement(Wire20.StatementDocument value, UnitKey unit) {
        var h = statementHeader(value.header(), unit);
        return switch (value) {
            case Wire20.EvaluateDocument v -> new EvaluateFact(h, Optional.ofNullable(v.subject()).map(s -> reference(s,h.id(),unit)),
                v.arms().stream().map(a -> { var l=(Wire20.LiteralDocument)a.selection();
                    return new EvaluateArm(a.ordinal(), new LiteralSource(new OperandId(h.id(),l.id()),l.kind(),
                        Optional.ofNullable(l.logicalValue()).map(Materialize::logical),provenance(l.provenance(),unit)),
                        a.statements().stream().map(id -> new StatementId(unit,id)).toList(), arm(a.control(),unit)); }).toList(),
                arm(v.otherArm(),unit),v.otherStatements().stream().map(id -> new StatementId(unit,id)).toList(),
                continuation(v.normalContinuation(),unit),v.gapCodes());
            case Wire20.PerformDocument v -> new PerformFact(h, v.profile(), Optional.ofNullable(v.target()).map(t ->
                new PerformTarget(new ProcedureId(unit, t.id()), provenance(t.referenceOrigin(), unit), provenance(t.paragraphOrigin(), unit))),
                Optional.ofNullable(v.targetEntry()).map(id -> new StatementId(unit, id)), v.targetStatements().stream().map(id -> new StatementId(unit,id)).toList(),
                Optional.ofNullable(v.targetExit()).map(id -> new StatementId(unit,id)), continuation(v.normalContinuation(), unit),
                List.of(), v.gapCodes());
            case Wire20.GobackDocument v -> new GobackFact(h, v.exit(), v.localContinuation());
            case Wire20.MoveDocument v -> {
                MoveSource source = switch (v.source()) {
                    case Wire20.LiteralDocument literal -> new LiteralSource(new OperandId(h.id(), literal.id()), literal.kind(),
                        Optional.ofNullable(literal.logicalValue()).map(Materialize::logical), provenance(literal.provenance(), unit));
                    case Wire20.DataSourceDocument data -> reference(data.reference(), h.id(), unit);
                };
                yield new MoveFact(h, source, reference(v.target(), h.id(), unit), v.copySemantics(), continuation(v.normalContinuation(), unit),
                    Optional.ofNullable(v.textAdjustment()).map(adjustment -> new TextAdjustment(adjustment.rule(), adjustment.receiverExtent(),
                        logical(adjustment.result()), provenance(adjustment.provenance(), unit))));
            }
            case Wire20.CallDocument v -> {
                CallTarget target = switch (v.target()) {
                    case Wire20.DataTargetDocument d -> new DataCallTarget(reference(d.reference(), h.id(), unit));
                    case Wire20.LiteralTargetDocument l -> new LiteralCallTarget(new OperandId(h.id(), l.id()), l.text(), l.writtenText(),
                        Optional.ofNullable(l.logicalValue()).map(Materialize::logical), provenance(l.provenance(), unit));
                };
                var s = v.surface();
                yield new CallFact(h, v.syntax(), target, v.runtimeTarget(), v.runtimeUncertaintyCode(), continuation(v.normalContinuation(), unit),
                    new CallSurface(s.using(), Optional.ofNullable(s.argumentCount()), s.returning(), s.onException(), s.notOnException(), s.onOverflow()), v.effects(), v.outcomes());
            }
            case Wire20.IfDocument v -> {
                var condition=v.condition(); var p=condition.predicate();
                var predicate=new PredicateGuarantee(p.availability(),p.profile(),p.resultDomain(),p.evaluation(),p.normalCompletion(),
                    p.readsCompleteness(),p.truthValue(),p.knownReads().stream().map(id -> new OperandId(h.id(),id)).toList(),provenance(p.provenance(),unit),p.gapCodes());
                yield new IfFact(h,condition.shape(),predicate,condition.references().stream().map(r -> reference(r,h.id(),unit)).toList(),
                    provenance(condition.provenance(),unit),v.explicitlyTerminated(),Optional.ofNullable(v.continuation()).map(id -> new StatementId(unit,id)),
                    continuation(v.normalContinuation(),unit),arm(v.thenArm(),unit),arm(v.elseArm(),unit),v.profile());
            }
            case Wire20.ObservedDocument v -> observed(h, v.observedKind(), v.observedShape(), v.gapCode(), continuation(v.normalContinuation(),unit), v.knownReferences().stream().map(r->reference(r,h.id(),unit)).toList());
        };
    }
    private static LogicalValue logical(Wire20.LogicalDocument value) {
        return new LogicalValue(value.logicalDomain(), value.value(), value.logicalExtent());
    }
    private static NormalContinuation continuation(Wire20.ContinuationDocument next, UnitKey unit) {
        return new NormalContinuation(next.availability(), Optional.ofNullable(next.statement()).map(id -> new StatementId(unit, id)), provenance(next.provenance(), unit));
    }
    private static DataReference reference(Wire20.ReferenceDocument target, StatementId statement, UnitKey unit) {
        var binding = target.binding();
        return new DataReference(new OperandId(statement, target.id()), target.role(),
            new Binding(ResolutionStatus.valueOf(binding.status().name()), binding.candidates().stream().map(c -> new DataId(unit, c.id())).toList(),
                Optional.ofNullable(binding.selected()).map(id -> new DataId(unit, id)), Optional.of(ResolutionReason.valueOf(binding.reason().name())),
                binding.candidates().stream().map(Wire.CandidateDocument::canonicalName).toList()),
            Optional.ofNullable(target.wholeItemAccess()).map(w -> new WholeItemAccess(new DataId(unit, w.data()))), provenance(target.provenance(), unit));
    }
    private static IfArm arm(Wire20.ArmDocument a, UnitKey unit) {
        return new IfArm(a.presence(),a.contentAvailability(),executableStart(a.entry(),unit),provenance(a.provenance(),unit),a.gapCodes());
    }
    private static IndependentStorageSet storage(Wire20.StorageDocument s, UnitKey unit) {
        return new IndependentStorageSet(s.availability(), s.rule(), s.authority(), s.members().stream().map(id -> new DataId(unit,id)).toList(),
            Optional.ofNullable(s.provenance()).map(p -> provenance(p,unit)), s.gapCodes());
    }
    static SpInput input(Wire21.Document d) {
        var unit = unitKey(d.unit(), null);
        return new SpInput(unit, policy(d.policy(), unit), d.dataDeclarations().stream().map(v -> {
            var scalar = Optional.ofNullable(v.scalarText()).map(t -> new ScalarText(t.logicalDomain(), t.logicalExtent(), t.storageClass(), t.declarationScope()));
            return new DataFact(new DataId(unit, v.id()), v.canonicalName(), Optional.ofNullable(v.picture()),
                provenance(v.provenance(), unit), v.coverage(), readiness(v.readiness(), unit), scalar);
        }).toList(), d.statements().stream().map(v -> statement(v, unit)).toList(), structure(d.structure(), unit),
            d.gaps().stream().map(v -> gap(v, unit)).toList(), coverage(d.coverage(), unit), entryInventory(d.entryInventory(), unit), Optional.of(storage(d.storageIndependence(), unit)), true);
    }
    private static StatementFact statement(Wire21.StatementDocument value, UnitKey unit) {
        var h = statementHeader(value.header(), unit);
        return switch (value) {
            case Wire21.GoToDocument v -> new GoToFact(h,Optional.ofNullable(v.target()).map(t ->
                new GoToTarget(new ProcedureId(unit,t.id()),provenance(t.paragraphOrigin(),unit))),provenance(v.referenceOrigin(),unit),
                Optional.ofNullable(v.targetEntry()).map(id -> new StatementId(unit,id)),Optional.ofNullable(v.entryOrigin()).map(o -> provenance(o,unit)),v.gapCodes());
            case Wire21.EvaluateDocument v -> new EvaluateFact(h, Optional.ofNullable(v.subject()).map(s -> reference(s,h.id(),unit)),
                v.arms().stream().map(a -> { var l=(Wire21.LiteralDocument)a.selection();
                    return new EvaluateArm(a.ordinal(), new LiteralSource(new OperandId(h.id(),l.id()),l.kind(),
                        Optional.ofNullable(l.logicalValue()).map(Materialize::logical),provenance(l.provenance(),unit)),
                        a.statements().stream().map(id -> new StatementId(unit,id)).toList(), arm(a.control(),unit)); }).toList(),
                arm(v.otherArm(),unit),v.otherStatements().stream().map(id -> new StatementId(unit,id)).toList(),
                continuation(v.normalContinuation(),unit),v.gapCodes());
            case Wire21.PerformDocument v -> new PerformFact(h, v.profile(), Optional.ofNullable(v.target()).map(t ->
                new PerformTarget(new ProcedureId(unit, t.id()), provenance(t.referenceOrigin(), unit), provenance(t.paragraphOrigin(), unit))),
                Optional.ofNullable(v.targetEntry()).map(id -> new StatementId(unit, id)), v.targetStatements().stream().map(id -> new StatementId(unit,id)).toList(),
                Optional.ofNullable(v.targetExit()).map(id -> new StatementId(unit,id)), continuation(v.normalContinuation(), unit),
                List.of(), v.gapCodes());
            case Wire21.GobackDocument v -> new GobackFact(h, v.exit(), v.localContinuation());
            case Wire21.MoveDocument v -> {
                MoveSource source = switch (v.source()) {
                    case Wire21.LiteralDocument literal -> new LiteralSource(new OperandId(h.id(), literal.id()), literal.kind(),
                        Optional.ofNullable(literal.logicalValue()).map(Materialize::logical), provenance(literal.provenance(), unit));
                    case Wire21.DataSourceDocument data -> reference(data.reference(), h.id(), unit);
                };
                yield new MoveFact(h, source, reference(v.target(), h.id(), unit), v.copySemantics(), continuation(v.normalContinuation(), unit),
                    Optional.ofNullable(v.textAdjustment()).map(adjustment -> new TextAdjustment(adjustment.rule(), adjustment.receiverExtent(),
                        logical(adjustment.result()), provenance(adjustment.provenance(), unit))));
            }
            case Wire21.CallDocument v -> {
                CallTarget target = switch (v.target()) {
                    case Wire21.DataTargetDocument d -> new DataCallTarget(reference(d.reference(), h.id(), unit));
                    case Wire21.LiteralTargetDocument l -> new LiteralCallTarget(new OperandId(h.id(), l.id()), l.text(), l.writtenText(),
                        Optional.ofNullable(l.logicalValue()).map(Materialize::logical), provenance(l.provenance(), unit));
                };
                var s = v.surface();
                yield new CallFact(h, v.syntax(), target, v.runtimeTarget(), v.runtimeUncertaintyCode(), continuation(v.normalContinuation(), unit),
                    new CallSurface(s.using(), Optional.ofNullable(s.argumentCount()), s.returning(), s.onException(), s.notOnException(), s.onOverflow()), v.effects(), v.outcomes());
            }
            case Wire21.IfDocument v -> {
                var condition=v.condition(); var p=condition.predicate();
                var predicate=new PredicateGuarantee(p.availability(),p.profile(),p.resultDomain(),p.evaluation(),p.normalCompletion(),
                    p.readsCompleteness(),p.truthValue(),p.knownReads().stream().map(id -> new OperandId(h.id(),id)).toList(),provenance(p.provenance(),unit),p.gapCodes());
                yield new IfFact(h,condition.shape(),predicate,condition.references().stream().map(r -> reference(r,h.id(),unit)).toList(),
                    provenance(condition.provenance(),unit),v.explicitlyTerminated(),Optional.ofNullable(v.continuation()).map(id -> new StatementId(unit,id)),
                    continuation(v.normalContinuation(),unit),arm(v.thenArm(),unit),arm(v.elseArm(),unit),v.profile());
            }
            case Wire21.ObservedDocument v -> observed(h, v.observedKind(), v.observedShape(), v.gapCode(), continuation(v.normalContinuation(),unit), v.knownReferences().stream().map(r->reference(r,h.id(),unit)).toList());
        };
    }
    private static LogicalValue logical(Wire21.LogicalDocument value) {
        return new LogicalValue(value.logicalDomain(), value.value(), value.logicalExtent());
    }
    private static NormalContinuation continuation(Wire21.ContinuationDocument next, UnitKey unit) {
        return new NormalContinuation(next.availability(), Optional.ofNullable(next.statement()).map(id -> new StatementId(unit, id)), provenance(next.provenance(), unit));
    }
    private static DataReference reference(Wire21.ReferenceDocument target, StatementId statement, UnitKey unit) {
        var binding = target.binding();
        return new DataReference(new OperandId(statement, target.id()), target.role(),
            new Binding(ResolutionStatus.valueOf(binding.status().name()), binding.candidates().stream().map(c -> new DataId(unit, c.id())).toList(),
                Optional.ofNullable(binding.selected()).map(id -> new DataId(unit, id)), Optional.of(ResolutionReason.valueOf(binding.reason().name())),
                binding.candidates().stream().map(Wire.CandidateDocument::canonicalName).toList()),
            Optional.ofNullable(target.wholeItemAccess()).map(w -> new WholeItemAccess(new DataId(unit, w.data()))), provenance(target.provenance(), unit));
    }
    private static IfArm arm(Wire21.ArmDocument a, UnitKey unit) {
        return new IfArm(a.presence(),a.contentAvailability(),executableStart(a.entry(),unit),provenance(a.provenance(),unit),a.gapCodes());
    }
    private static IndependentStorageSet storage(Wire21.StorageDocument s, UnitKey unit) {
        return new IndependentStorageSet(s.availability(), s.rule(), s.authority(), s.members().stream().map(id -> new DataId(unit,id)).toList(),
            Optional.ofNullable(s.provenance()).map(p -> provenance(p,unit)), s.gapCodes());
    }
    static SpInput input(Wire22.Document d) {
        var unit = unitKey(d.unit(), null);
        return new SpInput(unit, policy(d.policy(), unit), d.dataDeclarations().stream().map(v -> {
            var scalar = Optional.ofNullable(v.scalarText()).map(t -> new ScalarText(t.logicalDomain(), t.logicalExtent(), t.storageClass(), t.declarationScope()));
            return new DataFact(new DataId(unit, v.id()), v.canonicalName(), Optional.ofNullable(v.picture()),
                provenance(v.provenance(), unit), v.coverage(), readiness(v.readiness(), unit), scalar);
        }).toList(), d.statements().stream().map(v -> statement(v, unit)).toList(), structure(d.structure(), unit),
            d.gaps().stream().map(v -> gap(v, unit)).toList(), coverage(d.coverage(), unit), entryInventory(d.entryInventory(), unit), Optional.of(storage(d.storageIndependence(), unit)), true);
    }
    private static StatementFact statement(Wire22.StatementDocument value, UnitKey unit) {
        var h = statementHeader(value.header(), unit);
        return switch (value) {
            case Wire22.GoToDocument v -> new GoToFact(h,Optional.ofNullable(v.target()).map(t ->
                new GoToTarget(new ProcedureId(unit,t.id()),provenance(t.paragraphOrigin(),unit))),provenance(v.referenceOrigin(),unit),
                Optional.ofNullable(v.targetEntry()).map(id -> new StatementId(unit,id)),Optional.ofNullable(v.entryOrigin()).map(o -> provenance(o,unit)),v.gapCodes());
            case Wire22.EvaluateDocument v -> new EvaluateFact(h, Optional.ofNullable(v.subject()).map(s -> reference(s,h.id(),unit)),
                v.arms().stream().map(a -> { var l=(Wire22.LiteralDocument)a.selection();
                    return new EvaluateArm(a.ordinal(), new LiteralSource(new OperandId(h.id(),l.id()),l.kind(),
                        Optional.ofNullable(l.logicalValue()).map(Materialize::logical),provenance(l.provenance(),unit)),
                        a.statements().stream().map(id -> new StatementId(unit,id)).toList(), arm(a.control(),unit)); }).toList(),
                arm(v.otherArm(),unit),v.otherStatements().stream().map(id -> new StatementId(unit,id)).toList(),
                continuation(v.normalContinuation(),unit),v.gapCodes());
            case Wire22.ProcedurePerformDocument v -> new ProcedurePerformFact(h,
                Optional.ofNullable(v.start()).map(t->new PerformTarget(new ProcedureId(unit,t.id()),provenance(t.referenceOrigin(),unit),provenance(t.paragraphOrigin(),unit))),
                Optional.ofNullable(v.end()).map(t->new PerformTarget(new ProcedureId(unit,t.id()),provenance(t.referenceOrigin(),unit),provenance(t.paragraphOrigin(),unit))),
                v.procedures().stream().map(r->new PerformParagraph(new ProcedureId(unit,r.id()),new StatementId(unit,r.entry()),
                    r.statements().stream().map(id->new StatementId(unit,id)).toList(),r.completions().stream().map(id->new StatementId(unit,id)).toList(),provenance(r.provenance(),unit))).toList(),
                continuation(v.normalContinuation(),unit),v.gapCodes());
            case Wire22.PerformDocument v -> new PerformFact(h, v.profile(), Optional.ofNullable(v.target()).map(t ->
                new PerformTarget(new ProcedureId(unit, t.id()), provenance(t.referenceOrigin(), unit), provenance(t.paragraphOrigin(), unit))),
                Optional.ofNullable(v.targetEntry()).map(id -> new StatementId(unit, id)), v.targetStatements().stream().map(id -> new StatementId(unit,id)).toList(),
                Optional.ofNullable(v.targetExit()).map(id -> new StatementId(unit,id)), continuation(v.normalContinuation(), unit),
                List.of(), v.gapCodes());
            case Wire22.GobackDocument v -> new GobackFact(h, v.exit(), v.localContinuation());
            case Wire22.MoveDocument v -> {
                MoveSource source = switch (v.source()) {
                    case Wire22.LiteralDocument literal -> new LiteralSource(new OperandId(h.id(), literal.id()), literal.kind(),
                        Optional.ofNullable(literal.logicalValue()).map(Materialize::logical), provenance(literal.provenance(), unit));
                    case Wire22.DataSourceDocument data -> reference(data.reference(), h.id(), unit);
                };
                yield new MoveFact(h, source, reference(v.target(), h.id(), unit), v.copySemantics(), continuation(v.normalContinuation(), unit),
                    Optional.ofNullable(v.textAdjustment()).map(adjustment -> new TextAdjustment(adjustment.rule(), adjustment.receiverExtent(),
                        logical(adjustment.result()), provenance(adjustment.provenance(), unit))));
            }
            case Wire22.CallDocument v -> {
                CallTarget target = switch (v.target()) {
                    case Wire22.DataTargetDocument d -> new DataCallTarget(reference(d.reference(), h.id(), unit));
                    case Wire22.LiteralTargetDocument l -> new LiteralCallTarget(new OperandId(h.id(), l.id()), l.text(), l.writtenText(),
                        Optional.ofNullable(l.logicalValue()).map(Materialize::logical), provenance(l.provenance(), unit));
                };
                var s = v.surface();
                yield new CallFact(h, v.syntax(), target, v.runtimeTarget(), v.runtimeUncertaintyCode(), continuation(v.normalContinuation(), unit),
                    new CallSurface(s.using(), Optional.ofNullable(s.argumentCount()), s.returning(), s.onException(), s.notOnException(), s.onOverflow()), v.effects(), v.outcomes());
            }
            case Wire22.IfDocument v -> {
                var condition=v.condition(); var p=condition.predicate();
                var predicate=new PredicateGuarantee(p.availability(),p.profile(),p.resultDomain(),p.evaluation(),p.normalCompletion(),
                    p.readsCompleteness(),p.truthValue(),p.knownReads().stream().map(id -> new OperandId(h.id(),id)).toList(),provenance(p.provenance(),unit),p.gapCodes());
                yield new IfFact(h,condition.shape(),predicate,condition.references().stream().map(r -> reference(r,h.id(),unit)).toList(),
                    provenance(condition.provenance(),unit),v.explicitlyTerminated(),Optional.ofNullable(v.continuation()).map(id -> new StatementId(unit,id)),
                    continuation(v.normalContinuation(),unit),arm(v.thenArm(),unit),arm(v.elseArm(),unit),v.profile());
            }
            case Wire22.ObservedDocument v -> observed(h, v.observedKind(), v.observedShape(), v.gapCode(), continuation(v.normalContinuation(),unit), v.knownReferences().stream().map(r->reference(r,h.id(),unit)).toList());
        };
    }
    private static LogicalValue logical(Wire22.LogicalDocument value) {
        return new LogicalValue(value.logicalDomain(), value.value(), value.logicalExtent());
    }
    private static NormalContinuation continuation(Wire22.ContinuationDocument next, UnitKey unit) {
        return new NormalContinuation(next.availability(), Optional.ofNullable(next.statement()).map(id -> new StatementId(unit, id)), provenance(next.provenance(), unit));
    }
    private static DataReference reference(Wire22.ReferenceDocument target, StatementId statement, UnitKey unit) {
        var binding = target.binding();
        return new DataReference(new OperandId(statement, target.id()), target.role(),
            new Binding(ResolutionStatus.valueOf(binding.status().name()), binding.candidates().stream().map(c -> new DataId(unit, c.id())).toList(),
                Optional.ofNullable(binding.selected()).map(id -> new DataId(unit, id)), Optional.of(ResolutionReason.valueOf(binding.reason().name())),
                binding.candidates().stream().map(Wire.CandidateDocument::canonicalName).toList()),
            Optional.ofNullable(target.wholeItemAccess()).map(w -> new WholeItemAccess(new DataId(unit, w.data()))), provenance(target.provenance(), unit));
    }
    private static IfArm arm(Wire22.ArmDocument a, UnitKey unit) {
        return new IfArm(a.presence(),a.contentAvailability(),executableStart(a.entry(),unit),provenance(a.provenance(),unit),a.gapCodes());
    }
    private static IndependentStorageSet storage(Wire22.StorageDocument s, UnitKey unit) {
        return new IndependentStorageSet(s.availability(), s.rule(), s.authority(), s.members().stream().map(id -> new DataId(unit,id)).toList(),
            Optional.ofNullable(s.provenance()).map(p -> provenance(p,unit)), s.gapCodes());
    }
    static SpInput input(Wire23.Document d) {
        var unit = unitKey(d.unit(), null);
        return new SpInput(unit, policy(d.policy(), unit), d.dataDeclarations().stream().map(v -> {
            var scalar = Optional.ofNullable(v.scalarText()).map(t -> new ScalarText(t.logicalDomain(), t.logicalExtent(), t.storageClass(), t.declarationScope()));
            return new DataFact(new DataId(unit, v.id()), v.canonicalName(), Optional.ofNullable(v.picture()),
                provenance(v.provenance(), unit), v.coverage(), readiness(v.readiness(), unit), scalar);
        }).toList(), d.statements().stream().map(v -> statement(v, unit)).toList(), structure(d.structure(), unit),
            d.gaps().stream().map(v -> gap(v, unit)).toList(), coverage(d.coverage(), unit), entryInventory(d.entryInventory(), unit), Optional.of(storage(d.storageIndependence(), unit)), true);
    }
    private static StatementFact statement(Wire23.StatementDocument value, UnitKey unit) {
        var h = statementHeader(value.header(), unit);
        return switch (value) {
            case Wire23.GoToDocument v -> new GoToFact(h,Optional.ofNullable(v.target()).map(t ->
                new GoToTarget(new ProcedureId(unit,t.id()),provenance(t.paragraphOrigin(),unit))),provenance(v.referenceOrigin(),unit),
                Optional.ofNullable(v.targetEntry()).map(id -> new StatementId(unit,id)),Optional.ofNullable(v.entryOrigin()).map(o -> provenance(o,unit)),v.gapCodes());
            case Wire23.EvaluateDocument v -> new EvaluateFact(h, Optional.ofNullable(v.subject()).map(s -> reference(s,h.id(),unit)),
                v.arms().stream().map(a -> { var l=(Wire23.LiteralDocument)a.selection();
                    return new EvaluateArm(a.ordinal(), new LiteralSource(new OperandId(h.id(),l.id()),l.kind(),
                        Optional.ofNullable(l.logicalValue()).map(Materialize::logical),provenance(l.provenance(),unit)),
                        a.statements().stream().map(id -> new StatementId(unit,id)).toList(), arm(a.control(),unit)); }).toList(),
                arm(v.otherArm(),unit),v.otherStatements().stream().map(id -> new StatementId(unit,id)).toList(),
                continuation(v.normalContinuation(),unit),v.gapCodes());
            case Wire23.ProcedurePerformDocument v -> new ProcedurePerformFact(h,
                Optional.ofNullable(v.start()).map(t->new PerformTarget(new ProcedureId(unit,t.id()),provenance(t.referenceOrigin(),unit),provenance(t.paragraphOrigin(),unit))),
                Optional.ofNullable(v.end()).map(t->new PerformTarget(new ProcedureId(unit,t.id()),provenance(t.referenceOrigin(),unit),provenance(t.paragraphOrigin(),unit))),
                v.procedures().stream().map(r->new PerformParagraph(new ProcedureId(unit,r.id()),new StatementId(unit,r.entry()),
                    r.statements().stream().map(id->new StatementId(unit,id)).toList(),r.completions().stream().map(id->new StatementId(unit,id)).toList(),provenance(r.provenance(),unit))).toList(),
                continuation(v.normalContinuation(),unit),Optional.ofNullable(v.loop()).map(l->loop(l,h.id(),unit)),v.gapCodes());
            case Wire23.PerformDocument v -> new PerformFact(h, v.profile(), Optional.ofNullable(v.target()).map(t ->
                new PerformTarget(new ProcedureId(unit, t.id()), provenance(t.referenceOrigin(), unit), provenance(t.paragraphOrigin(), unit))),
                Optional.ofNullable(v.targetEntry()).map(id -> new StatementId(unit, id)), v.targetStatements().stream().map(id -> new StatementId(unit,id)).toList(),
                Optional.ofNullable(v.targetExit()).map(id -> new StatementId(unit,id)), continuation(v.normalContinuation(), unit),
                List.of(), v.gapCodes());
            case Wire23.GobackDocument v -> new GobackFact(h, v.exit(), v.localContinuation());
            case Wire23.MoveDocument v -> {
                MoveSource source = switch (v.source()) {
                    case Wire23.LiteralDocument literal -> new LiteralSource(new OperandId(h.id(), literal.id()), literal.kind(),
                        Optional.ofNullable(literal.logicalValue()).map(Materialize::logical), provenance(literal.provenance(), unit));
                    case Wire23.DataSourceDocument data -> reference(data.reference(), h.id(), unit);
                };
                yield new MoveFact(h, source, reference(v.target(), h.id(), unit), v.copySemantics(), continuation(v.normalContinuation(), unit),
                    Optional.ofNullable(v.textAdjustment()).map(adjustment -> new TextAdjustment(adjustment.rule(), adjustment.receiverExtent(),
                        logical(adjustment.result()), provenance(adjustment.provenance(), unit))));
            }
            case Wire23.CallDocument v -> {
                CallTarget target = switch (v.target()) {
                    case Wire23.DataTargetDocument d -> new DataCallTarget(reference(d.reference(), h.id(), unit));
                    case Wire23.LiteralTargetDocument l -> new LiteralCallTarget(new OperandId(h.id(), l.id()), l.text(), l.writtenText(),
                        Optional.ofNullable(l.logicalValue()).map(Materialize::logical), provenance(l.provenance(), unit));
                };
                var s = v.surface();
                yield new CallFact(h, v.syntax(), target, v.runtimeTarget(), v.runtimeUncertaintyCode(), continuation(v.normalContinuation(), unit),
                    new CallSurface(s.using(), Optional.ofNullable(s.argumentCount()), s.returning(), s.onException(), s.notOnException(), s.onOverflow()), v.effects(), v.outcomes());
            }
            case Wire23.IfDocument v -> {
                var condition=v.condition(); var p=condition.predicate();
                var predicate=new PredicateGuarantee(p.availability(),p.profile(),p.resultDomain(),p.evaluation(),p.normalCompletion(),
                    p.readsCompleteness(),p.truthValue(),p.knownReads().stream().map(id -> new OperandId(h.id(),id)).toList(),provenance(p.provenance(),unit),p.gapCodes());
                yield new IfFact(h,condition.shape(),predicate,condition.references().stream().map(r -> reference(r,h.id(),unit)).toList(),
                    provenance(condition.provenance(),unit),v.explicitlyTerminated(),Optional.ofNullable(v.continuation()).map(id -> new StatementId(unit,id)),
                    continuation(v.normalContinuation(),unit),arm(v.thenArm(),unit),arm(v.elseArm(),unit),v.profile());
            }
            case Wire23.ObservedDocument v -> observed(h, v.observedKind(), v.observedShape(), v.gapCode(), continuation(v.normalContinuation(),unit), v.knownReferences().stream().map(r->reference(r,h.id(),unit)).toList());
        };
    }
    private static LogicalValue logical(Wire23.LogicalDocument value) {
        return new LogicalValue(value.logicalDomain(), value.value(), value.logicalExtent());
    }
    private static NormalContinuation continuation(Wire23.ContinuationDocument next, UnitKey unit) {
        return new NormalContinuation(next.availability(), Optional.ofNullable(next.statement()).map(id -> new StatementId(unit, id)), provenance(next.provenance(), unit));
    }
    private static DataReference reference(Wire23.ReferenceDocument target, StatementId statement, UnitKey unit) {
        var binding = target.binding();
        return new DataReference(new OperandId(statement, target.id()), target.role(),
            new Binding(ResolutionStatus.valueOf(binding.status().name()), binding.candidates().stream().map(c -> new DataId(unit, c.id())).toList(),
                Optional.ofNullable(binding.selected()).map(id -> new DataId(unit, id)), Optional.of(ResolutionReason.valueOf(binding.reason().name())),
                binding.candidates().stream().map(Wire.CandidateDocument::canonicalName).toList()),
            Optional.ofNullable(target.wholeItemAccess()).map(w -> new WholeItemAccess(new DataId(unit, w.data()))), provenance(target.provenance(), unit));
    }
    private static IfArm arm(Wire23.ArmDocument a, UnitKey unit) {
        return new IfArm(a.presence(),a.contentAvailability(),executableStart(a.entry(),unit),provenance(a.provenance(),unit),a.gapCodes());
    }
    private static IndependentStorageSet storage(Wire23.StorageDocument s, UnitKey unit) {
        return new IndependentStorageSet(s.availability(), s.rule(), s.authority(), s.members().stream().map(id -> new DataId(unit,id)).toList(),
            Optional.ofNullable(s.provenance()).map(p -> provenance(p,unit)), s.gapCodes());
    }
    private static PerformLoop loop(Wire23.PerformLoopDocument l, StatementId owner, UnitKey unit) {
        var condition=l.condition();var p=condition.predicate();
        return new PerformLoop(l.testMode(),condition.shape(),new PredicateGuarantee(p.availability(),p.profile(),p.resultDomain(),p.evaluation(),p.normalCompletion(),
            p.readsCompleteness(),p.truthValue(),p.knownReads().stream().map(id->new OperandId(owner,id)).toList(),provenance(p.provenance(),unit),p.gapCodes()),
            condition.references().stream().map(r->reference(r,owner,unit)).toList(),provenance(condition.provenance(),unit));
    }
    static SpInput input(Wire24.Document d) {
        var unit = unitKey(d.unit(), null);
        return new SpInput(unit, policy(d.policy(), unit), d.dataDeclarations().stream().map(v -> {
            var scalar = Optional.ofNullable(v.scalarText()).map(t -> new ScalarText(t.logicalDomain(), t.logicalExtent(), t.storageClass(), t.declarationScope()));
            return new DataFact(new DataId(unit, v.id()), v.canonicalName(), Optional.ofNullable(v.picture()),
                provenance(v.provenance(), unit), v.coverage(), readiness(v.readiness(), unit), scalar,Optional.ofNullable(v.scalarInteger()).map(n->new ScalarInteger(n.digits())));
        }).toList(), d.statements().stream().map(v -> statement(v, unit)).toList(), structure(d.structure(), unit),
            d.gaps().stream().map(v -> gap(v, unit)).toList(), coverage(d.coverage(), unit), entryInventory(d.entryInventory(), unit), Optional.of(storage(d.storageIndependence(), unit)), true);
    }
    private static StatementFact statement(Wire24.StatementDocument value, UnitKey unit) {
        var h = statementHeader(value.header(), unit);
        return switch (value) {
            case Wire24.GoToDocument v -> new GoToFact(h,Optional.ofNullable(v.target()).map(t ->
                new GoToTarget(new ProcedureId(unit,t.id()),provenance(t.paragraphOrigin(),unit))),provenance(v.referenceOrigin(),unit),
                Optional.ofNullable(v.targetEntry()).map(id -> new StatementId(unit,id)),Optional.ofNullable(v.entryOrigin()).map(o -> provenance(o,unit)),v.gapCodes());
            case Wire24.EvaluateDocument v -> new EvaluateFact(h, Optional.ofNullable(v.subject()).map(s -> reference(s,h.id(),unit)),
                v.arms().stream().map(a -> { var l=(Wire24.LiteralDocument)a.selection();
                    return new EvaluateArm(a.ordinal(), new LiteralSource(new OperandId(h.id(),l.id()),l.kind(),
                        Optional.ofNullable(l.logicalValue()).map(Materialize::logical),provenance(l.provenance(),unit)),
                        a.statements().stream().map(id -> new StatementId(unit,id)).toList(), arm(a.control(),unit)); }).toList(),
                arm(v.otherArm(),unit),v.otherStatements().stream().map(id -> new StatementId(unit,id)).toList(),
                continuation(v.normalContinuation(),unit),v.gapCodes());
            case Wire24.ProcedurePerformDocument v -> new ProcedurePerformFact(h,
                Optional.ofNullable(v.start()).map(t->new PerformTarget(new ProcedureId(unit,t.id()),provenance(t.referenceOrigin(),unit),provenance(t.paragraphOrigin(),unit))),
                Optional.ofNullable(v.end()).map(t->new PerformTarget(new ProcedureId(unit,t.id()),provenance(t.referenceOrigin(),unit),provenance(t.paragraphOrigin(),unit))),
                v.procedures().stream().map(r->new PerformParagraph(new ProcedureId(unit,r.id()),new StatementId(unit,r.entry()),
                    r.statements().stream().map(id->new StatementId(unit,id)).toList(),r.completions().stream().map(id->new StatementId(unit,id)).toList(),provenance(r.provenance(),unit))).toList(),
                continuation(v.normalContinuation(),unit),Optional.ofNullable(v.loop()).map(l->loop(l,h.id(),unit)),Optional.ofNullable(v.times()).map(t->new PerformCount(t.profile(),Optional.ofNullable(t.integer()),Optional.ofNullable(t.reference()).map(ref->reference(ref,h.id(),unit)),provenance(t.provenance(),unit))),v.gapCodes());
            case Wire24.PerformDocument v -> new PerformFact(h, v.profile(), Optional.ofNullable(v.target()).map(t ->
                new PerformTarget(new ProcedureId(unit, t.id()), provenance(t.referenceOrigin(), unit), provenance(t.paragraphOrigin(), unit))),
                Optional.ofNullable(v.targetEntry()).map(id -> new StatementId(unit, id)), v.targetStatements().stream().map(id -> new StatementId(unit,id)).toList(),
                Optional.ofNullable(v.targetExit()).map(id -> new StatementId(unit,id)), continuation(v.normalContinuation(), unit),
                List.of(), v.gapCodes());
            case Wire24.GobackDocument v -> new GobackFact(h, v.exit(), v.localContinuation());
            case Wire24.MoveDocument v -> {
                MoveSource source = switch (v.source()) {
                    case Wire24.LiteralDocument literal -> new LiteralSource(new OperandId(h.id(), literal.id()), literal.kind(),
                        Optional.ofNullable(literal.logicalValue()).map(Materialize::logical), provenance(literal.provenance(), unit));
                    case Wire24.DataSourceDocument data -> reference(data.reference(), h.id(), unit);
                };
                yield new MoveFact(h, source, reference(v.target(), h.id(), unit), v.copySemantics(), continuation(v.normalContinuation(), unit),
                    Optional.ofNullable(v.textAdjustment()).map(adjustment -> new TextAdjustment(adjustment.rule(), adjustment.receiverExtent(),
                        logical(adjustment.result()), provenance(adjustment.provenance(), unit))));
            }
            case Wire24.CallDocument v -> {
                CallTarget target = switch (v.target()) {
                    case Wire24.DataTargetDocument d -> new DataCallTarget(reference(d.reference(), h.id(), unit));
                    case Wire24.LiteralTargetDocument l -> new LiteralCallTarget(new OperandId(h.id(), l.id()), l.text(), l.writtenText(),
                        Optional.ofNullable(l.logicalValue()).map(Materialize::logical), provenance(l.provenance(), unit));
                };
                var s = v.surface();
                yield new CallFact(h, v.syntax(), target, v.runtimeTarget(), v.runtimeUncertaintyCode(), continuation(v.normalContinuation(), unit),
                    new CallSurface(s.using(), Optional.ofNullable(s.argumentCount()), s.returning(), s.onException(), s.notOnException(), s.onOverflow()), v.effects(), v.outcomes());
            }
            case Wire24.IfDocument v -> {
                var condition=v.condition(); var p=condition.predicate();
                var predicate=new PredicateGuarantee(p.availability(),p.profile(),p.resultDomain(),p.evaluation(),p.normalCompletion(),
                    p.readsCompleteness(),p.truthValue(),p.knownReads().stream().map(id -> new OperandId(h.id(),id)).toList(),provenance(p.provenance(),unit),p.gapCodes());
                yield new IfFact(h,condition.shape(),predicate,condition.references().stream().map(r -> reference(r,h.id(),unit)).toList(),
                    provenance(condition.provenance(),unit),v.explicitlyTerminated(),Optional.ofNullable(v.continuation()).map(id -> new StatementId(unit,id)),
                    continuation(v.normalContinuation(),unit),arm(v.thenArm(),unit),arm(v.elseArm(),unit),v.profile());
            }
            case Wire24.ObservedDocument v -> observed(h, v.observedKind(), v.observedShape(), v.gapCode(), continuation(v.normalContinuation(),unit), v.knownReferences().stream().map(r->reference(r,h.id(),unit)).toList());
        };
    }
    private static LogicalValue logical(Wire24.LogicalDocument value) {
        return new LogicalValue(value.logicalDomain(), value.value(), value.logicalExtent());
    }
    private static NormalContinuation continuation(Wire24.ContinuationDocument next, UnitKey unit) {
        return new NormalContinuation(next.availability(), Optional.ofNullable(next.statement()).map(id -> new StatementId(unit, id)), provenance(next.provenance(), unit));
    }
    private static DataReference reference(Wire24.ReferenceDocument target, StatementId statement, UnitKey unit) {
        var binding = target.binding();
        return new DataReference(new OperandId(statement, target.id()), target.role(),
            new Binding(ResolutionStatus.valueOf(binding.status().name()), binding.candidates().stream().map(c -> new DataId(unit, c.id())).toList(),
                Optional.ofNullable(binding.selected()).map(id -> new DataId(unit, id)), Optional.of(ResolutionReason.valueOf(binding.reason().name())),
                binding.candidates().stream().map(Wire.CandidateDocument::canonicalName).toList()),
            Optional.ofNullable(target.wholeItemAccess()).map(w -> new WholeItemAccess(new DataId(unit, w.data()))), provenance(target.provenance(), unit));
    }
    private static IfArm arm(Wire24.ArmDocument a, UnitKey unit) {
        return new IfArm(a.presence(),a.contentAvailability(),executableStart(a.entry(),unit),provenance(a.provenance(),unit),a.gapCodes());
    }
    private static IndependentStorageSet storage(Wire24.StorageDocument s, UnitKey unit) {
        return new IndependentStorageSet(s.availability(), s.rule(), s.authority(), s.members().stream().map(id -> new DataId(unit,id)).toList(),
            Optional.ofNullable(s.provenance()).map(p -> provenance(p,unit)), s.gapCodes());
    }
    private static PerformLoop loop(Wire24.PerformLoopDocument l, StatementId owner, UnitKey unit) {
        var condition=l.condition();var p=condition.predicate();
        return new PerformLoop(l.testMode(),condition.shape(),new PredicateGuarantee(p.availability(),p.profile(),p.resultDomain(),p.evaluation(),p.normalCompletion(),
            p.readsCompleteness(),p.truthValue(),p.knownReads().stream().map(id->new OperandId(owner,id)).toList(),provenance(p.provenance(),unit),p.gapCodes()),
            condition.references().stream().map(r->reference(r,owner,unit)).toList(),provenance(condition.provenance(),unit));
    }
    static SpInput input(Wire25.Document d) {
        var unit = unitKey(d.unit(), null);
        return new SpInput(unit, policy(d.policy(), unit), d.dataDeclarations().stream().map(v -> {
            var scalar = Optional.ofNullable(v.scalarText()).map(t -> new ScalarText(t.logicalDomain(), t.logicalExtent(), t.storageClass(), t.declarationScope()));
            return new DataFact(new DataId(unit, v.id()), v.canonicalName(), Optional.ofNullable(v.picture()),
                provenance(v.provenance(), unit), v.coverage(), readiness(v.readiness(), unit), scalar,Optional.ofNullable(v.scalarInteger()).map(n->new ScalarInteger(n.digits())));
        }).toList(), d.statements().stream().map(v -> statement(v, unit)).toList(), structure(d.structure(), unit),
            d.gaps().stream().map(v -> gap(v, unit)).toList(), coverage(d.coverage(), unit), entryInventory(d.entryInventory(), unit), Optional.of(storage(d.storageIndependence(), unit)), true);
    }
    private static StatementFact statement(Wire25.StatementDocument value, UnitKey unit) {
        var h = statementHeader(value.header(), unit);
        return switch (value) {
            case Wire25.GoToDocument v -> new GoToFact(h,Optional.ofNullable(v.target()).map(t ->
                new GoToTarget(new ProcedureId(unit,t.id()),provenance(t.paragraphOrigin(),unit))),provenance(v.referenceOrigin(),unit),
                Optional.ofNullable(v.targetEntry()).map(id -> new StatementId(unit,id)),Optional.ofNullable(v.entryOrigin()).map(o -> provenance(o,unit)),v.gapCodes());
            case Wire25.EvaluateDocument v -> new EvaluateFact(h, Optional.ofNullable(v.subject()).map(s -> reference(s,h.id(),unit)),
                v.arms().stream().map(a -> { var l=(Wire25.LiteralDocument)a.selection();
                    return new EvaluateArm(a.ordinal(), new LiteralSource(new OperandId(h.id(),l.id()),l.kind(),
                        Optional.ofNullable(l.logicalValue()).map(Materialize::logical),provenance(l.provenance(),unit)),
                        a.statements().stream().map(id -> new StatementId(unit,id)).toList(), arm(a.control(),unit)); }).toList(),
                arm(v.otherArm(),unit),v.otherStatements().stream().map(id -> new StatementId(unit,id)).toList(),
                continuation(v.normalContinuation(),unit),v.gapCodes());
            case Wire25.ProcedurePerformDocument v -> new ProcedurePerformFact(h,
                Optional.ofNullable(v.start()).map(t->new PerformTarget(new ProcedureId(unit,t.id()),provenance(t.referenceOrigin(),unit),provenance(t.paragraphOrigin(),unit))),
                Optional.ofNullable(v.end()).map(t->new PerformTarget(new ProcedureId(unit,t.id()),provenance(t.referenceOrigin(),unit),provenance(t.paragraphOrigin(),unit))),
                v.procedures().stream().map(r->new PerformParagraph(new ProcedureId(unit,r.id()),new StatementId(unit,r.entry()),
                    r.statements().stream().map(id->new StatementId(unit,id)).toList(),r.completions().stream().map(id->new StatementId(unit,id)).toList(),provenance(r.provenance(),unit))).toList(),
                continuation(v.normalContinuation(),unit),Optional.ofNullable(v.loop()).map(l->loop(l,h.id(),unit)),Optional.ofNullable(v.times()).map(t->new PerformCount(t.profile(),Optional.ofNullable(t.integer()),Optional.ofNullable(t.reference()).map(ref->reference(ref,h.id(),unit)),provenance(t.provenance(),unit))),Optional.ofNullable(v.varying()).map(x->new PerformVarying(x.levels(),x.controls().stream().map(o->new VaryingOperand(o.level(),o.role(),Optional.ofNullable(o.integer()),o.references().stream().map(ref->reference(ref,h.id(),unit)).toList(),provenance(o.provenance(),unit))).toList())),v.gapCodes());
            case Wire25.PerformDocument v -> new PerformFact(h, v.profile(), Optional.ofNullable(v.target()).map(t ->
                new PerformTarget(new ProcedureId(unit, t.id()), provenance(t.referenceOrigin(), unit), provenance(t.paragraphOrigin(), unit))),
                Optional.ofNullable(v.targetEntry()).map(id -> new StatementId(unit, id)), v.targetStatements().stream().map(id -> new StatementId(unit,id)).toList(),
                Optional.ofNullable(v.targetExit()).map(id -> new StatementId(unit,id)), continuation(v.normalContinuation(), unit),
                List.of(), v.gapCodes());
            case Wire25.GobackDocument v -> new GobackFact(h, v.exit(), v.localContinuation());
            case Wire25.MoveDocument v -> {
                MoveSource source = switch (v.source()) {
                    case Wire25.LiteralDocument literal -> new LiteralSource(new OperandId(h.id(), literal.id()), literal.kind(),
                        Optional.ofNullable(literal.logicalValue()).map(Materialize::logical), provenance(literal.provenance(), unit));
                    case Wire25.DataSourceDocument data -> reference(data.reference(), h.id(), unit);
                };
                yield new MoveFact(h, source, reference(v.target(), h.id(), unit), v.copySemantics(), continuation(v.normalContinuation(), unit),
                    Optional.ofNullable(v.textAdjustment()).map(adjustment -> new TextAdjustment(adjustment.rule(), adjustment.receiverExtent(),
                        logical(adjustment.result()), provenance(adjustment.provenance(), unit))));
            }
            case Wire25.CallDocument v -> {
                CallTarget target = switch (v.target()) {
                    case Wire25.DataTargetDocument d -> new DataCallTarget(reference(d.reference(), h.id(), unit));
                    case Wire25.LiteralTargetDocument l -> new LiteralCallTarget(new OperandId(h.id(), l.id()), l.text(), l.writtenText(),
                        Optional.ofNullable(l.logicalValue()).map(Materialize::logical), provenance(l.provenance(), unit));
                };
                var s = v.surface();
                yield new CallFact(h, v.syntax(), target, v.runtimeTarget(), v.runtimeUncertaintyCode(), continuation(v.normalContinuation(), unit),
                    new CallSurface(s.using(), Optional.ofNullable(s.argumentCount()), s.returning(), s.onException(), s.notOnException(), s.onOverflow()), v.effects(), v.outcomes());
            }
            case Wire25.IfDocument v -> {
                var condition=v.condition(); var p=condition.predicate();
                var predicate=new PredicateGuarantee(p.availability(),p.profile(),p.resultDomain(),p.evaluation(),p.normalCompletion(),
                    p.readsCompleteness(),p.truthValue(),p.knownReads().stream().map(id -> new OperandId(h.id(),id)).toList(),provenance(p.provenance(),unit),p.gapCodes());
                yield new IfFact(h,condition.shape(),predicate,condition.references().stream().map(r -> reference(r,h.id(),unit)).toList(),
                    provenance(condition.provenance(),unit),v.explicitlyTerminated(),Optional.ofNullable(v.continuation()).map(id -> new StatementId(unit,id)),
                    continuation(v.normalContinuation(),unit),arm(v.thenArm(),unit),arm(v.elseArm(),unit),v.profile());
            }
            case Wire25.ObservedDocument v -> observed(h, v.observedKind(), v.observedShape(), v.gapCode(), continuation(v.normalContinuation(),unit), v.knownReferences().stream().map(r->reference(r,h.id(),unit)).toList());
        };
    }
    private static LogicalValue logical(Wire25.LogicalDocument value) {
        return new LogicalValue(value.logicalDomain(), value.value(), value.logicalExtent());
    }
    private static NormalContinuation continuation(Wire25.ContinuationDocument next, UnitKey unit) {
        return new NormalContinuation(next.availability(), Optional.ofNullable(next.statement()).map(id -> new StatementId(unit, id)), provenance(next.provenance(), unit));
    }
    private static DataReference reference(Wire25.ReferenceDocument target, StatementId statement, UnitKey unit) {
        var binding = target.binding();
        return new DataReference(new OperandId(statement, target.id()), target.role(),
            new Binding(ResolutionStatus.valueOf(binding.status().name()), binding.candidates().stream().map(c -> new DataId(unit, c.id())).toList(),
                Optional.ofNullable(binding.selected()).map(id -> new DataId(unit, id)), Optional.of(ResolutionReason.valueOf(binding.reason().name())),
                binding.candidates().stream().map(Wire.CandidateDocument::canonicalName).toList()),
            Optional.ofNullable(target.wholeItemAccess()).map(w -> new WholeItemAccess(new DataId(unit, w.data()))), provenance(target.provenance(), unit));
    }
    private static IfArm arm(Wire25.ArmDocument a, UnitKey unit) {
        return new IfArm(a.presence(),a.contentAvailability(),executableStart(a.entry(),unit),provenance(a.provenance(),unit),a.gapCodes());
    }
    private static IndependentStorageSet storage(Wire25.StorageDocument s, UnitKey unit) {
        return new IndependentStorageSet(s.availability(), s.rule(), s.authority(), s.members().stream().map(id -> new DataId(unit,id)).toList(),
            Optional.ofNullable(s.provenance()).map(p -> provenance(p,unit)), s.gapCodes());
    }
    private static PerformLoop loop(Wire25.PerformLoopDocument l, StatementId owner, UnitKey unit) {
        var condition=l.condition();var p=condition.predicate();
        return new PerformLoop(l.testMode(),condition.shape(),new PredicateGuarantee(p.availability(),p.profile(),p.resultDomain(),p.evaluation(),p.normalCompletion(),
            p.readsCompleteness(),p.truthValue(),p.knownReads().stream().map(id->new OperandId(owner,id)).toList(),provenance(p.provenance(),unit),p.gapCodes()),
            condition.references().stream().map(r->reference(r,owner,unit)).toList(),provenance(condition.provenance(),unit));
    }
    static SpInput input(Wire26.Document d) {
        var unit = unitKey(d.unit(), null);
        return new SpInput(unit, policy(d.policy(), unit), d.dataDeclarations().stream().map(v -> {
            var scalar = Optional.ofNullable(v.scalarText()).map(t -> new ScalarText(t.logicalDomain(), t.logicalExtent(), t.storageClass(), t.declarationScope()));
            return new DataFact(new DataId(unit, v.id()), v.canonicalName(), Optional.ofNullable(v.picture()),
                provenance(v.provenance(), unit), v.coverage(), readiness(v.readiness(), unit), scalar,Optional.ofNullable(v.scalarInteger()).map(n->new ScalarInteger(n.digits())));
        }).toList(), d.statements().stream().map(v -> statement(v, unit)).toList(), structure(d.structure(), unit),
            d.gaps().stream().map(v -> gap(v, unit)).toList(), coverage(d.coverage(), unit), entryInventory(d.entryInventory(), unit), Optional.of(storage(d.storageIndependence(), unit)), true);
    }
    private static StatementFact statement(Wire26.StatementDocument value, UnitKey unit) {
        var h = statementHeader(value.header(), unit);
        return switch (value) {
            case Wire26.ConditionalGoToDocument v -> new ConditionalGoToFact(h,Optional.ofNullable(v.selector()).map(r->reference(r,h.id(),unit)),
                v.selectorInteger(),provenance(v.selectorOrigin(),unit),v.destinations().stream().map(d->new GoToDestination(d.ordinal(),
                    Optional.ofNullable(d.target()).map(id->new ProcedureId(unit,id)),Optional.ofNullable(d.procedureOrigin()).map(o->provenance(o,unit)),
                    provenance(d.referenceOrigin(),unit),Optional.ofNullable(d.targetEntry()).map(id->new StatementId(unit,id)),
                    Optional.ofNullable(d.entryOrigin()).map(o->provenance(o,unit)),d.gapCodes())).toList(),continuation(v.normalContinuation(),unit),v.gapCodes());
            case Wire26.GoToDocument v -> new GoToFact(h,Optional.ofNullable(v.target()).map(t ->
                new GoToTarget(new ProcedureId(unit,t.id()),provenance(t.paragraphOrigin(),unit))),provenance(v.referenceOrigin(),unit),
                Optional.ofNullable(v.targetEntry()).map(id -> new StatementId(unit,id)),Optional.ofNullable(v.entryOrigin()).map(o -> provenance(o,unit)),v.gapCodes());
            case Wire26.EvaluateDocument v -> new EvaluateFact(h, Optional.ofNullable(v.subject()).map(s -> reference(s,h.id(),unit)),
                v.arms().stream().map(a -> { var l=(Wire26.LiteralDocument)a.selection();
                    return new EvaluateArm(a.ordinal(), new LiteralSource(new OperandId(h.id(),l.id()),l.kind(),
                        Optional.ofNullable(l.logicalValue()).map(Materialize::logical),provenance(l.provenance(),unit)),
                        a.statements().stream().map(id -> new StatementId(unit,id)).toList(), arm(a.control(),unit)); }).toList(),
                arm(v.otherArm(),unit),v.otherStatements().stream().map(id -> new StatementId(unit,id)).toList(),
                continuation(v.normalContinuation(),unit),v.gapCodes());
            case Wire26.ProcedurePerformDocument v -> new ProcedurePerformFact(h,
                Optional.ofNullable(v.start()).map(t->new PerformTarget(new ProcedureId(unit,t.id()),provenance(t.referenceOrigin(),unit),provenance(t.paragraphOrigin(),unit))),
                Optional.ofNullable(v.end()).map(t->new PerformTarget(new ProcedureId(unit,t.id()),provenance(t.referenceOrigin(),unit),provenance(t.paragraphOrigin(),unit))),
                v.procedures().stream().map(r->new PerformParagraph(new ProcedureId(unit,r.id()),new StatementId(unit,r.entry()),
                    r.statements().stream().map(id->new StatementId(unit,id)).toList(),r.completions().stream().map(id->new StatementId(unit,id)).toList(),provenance(r.provenance(),unit))).toList(),
                continuation(v.normalContinuation(),unit),Optional.ofNullable(v.loop()).map(l->loop(l,h.id(),unit)),Optional.ofNullable(v.times()).map(t->new PerformCount(t.profile(),Optional.ofNullable(t.integer()),Optional.ofNullable(t.reference()).map(ref->reference(ref,h.id(),unit)),provenance(t.provenance(),unit))),Optional.ofNullable(v.varying()).map(x->new PerformVarying(x.levels(),x.controls().stream().map(o->new VaryingOperand(o.level(),o.role(),Optional.ofNullable(o.integer()),o.references().stream().map(ref->reference(ref,h.id(),unit)).toList(),provenance(o.provenance(),unit))).toList())),v.gapCodes());
            case Wire26.PerformDocument v -> new PerformFact(h, v.profile(), Optional.ofNullable(v.target()).map(t ->
                new PerformTarget(new ProcedureId(unit, t.id()), provenance(t.referenceOrigin(), unit), provenance(t.paragraphOrigin(), unit))),
                Optional.ofNullable(v.targetEntry()).map(id -> new StatementId(unit, id)), v.targetStatements().stream().map(id -> new StatementId(unit,id)).toList(),
                Optional.ofNullable(v.targetExit()).map(id -> new StatementId(unit,id)), continuation(v.normalContinuation(), unit),
                List.of(), v.gapCodes());
            case Wire26.GobackDocument v -> new GobackFact(h, v.exit(), v.localContinuation());
            case Wire26.MoveDocument v -> {
                MoveSource source = switch (v.source()) {
                    case Wire26.LiteralDocument literal -> new LiteralSource(new OperandId(h.id(), literal.id()), literal.kind(),
                        Optional.ofNullable(literal.logicalValue()).map(Materialize::logical), provenance(literal.provenance(), unit));
                    case Wire26.DataSourceDocument data -> reference(data.reference(), h.id(), unit);
                };
                yield new MoveFact(h, source, reference(v.target(), h.id(), unit), v.copySemantics(), continuation(v.normalContinuation(), unit),
                    Optional.ofNullable(v.textAdjustment()).map(adjustment -> new TextAdjustment(adjustment.rule(), adjustment.receiverExtent(),
                        logical(adjustment.result()), provenance(adjustment.provenance(), unit))));
            }
            case Wire26.CallDocument v -> {
                CallTarget target = switch (v.target()) {
                    case Wire26.DataTargetDocument d -> new DataCallTarget(reference(d.reference(), h.id(), unit));
                    case Wire26.LiteralTargetDocument l -> new LiteralCallTarget(new OperandId(h.id(), l.id()), l.text(), l.writtenText(),
                        Optional.ofNullable(l.logicalValue()).map(Materialize::logical), provenance(l.provenance(), unit));
                };
                var s = v.surface();
                yield new CallFact(h, v.syntax(), target, v.runtimeTarget(), v.runtimeUncertaintyCode(), continuation(v.normalContinuation(), unit),
                    new CallSurface(s.using(), Optional.ofNullable(s.argumentCount()), s.returning(), s.onException(), s.notOnException(), s.onOverflow()), v.effects(), v.outcomes());
            }
            case Wire26.IfDocument v -> {
                var condition=v.condition(); var p=condition.predicate();
                var predicate=new PredicateGuarantee(p.availability(),p.profile(),p.resultDomain(),p.evaluation(),p.normalCompletion(),
                    p.readsCompleteness(),p.truthValue(),p.knownReads().stream().map(id -> new OperandId(h.id(),id)).toList(),provenance(p.provenance(),unit),p.gapCodes());
                yield new IfFact(h,condition.shape(),predicate,condition.references().stream().map(r -> reference(r,h.id(),unit)).toList(),
                    provenance(condition.provenance(),unit),v.explicitlyTerminated(),Optional.ofNullable(v.continuation()).map(id -> new StatementId(unit,id)),
                    continuation(v.normalContinuation(),unit),arm(v.thenArm(),unit),arm(v.elseArm(),unit),v.profile());
            }
            case Wire26.ObservedDocument v -> observed(h, v.observedKind(), v.observedShape(), v.gapCode(), continuation(v.normalContinuation(),unit), v.knownReferences().stream().map(r->reference(r,h.id(),unit)).toList());
        };
    }
    private static LogicalValue logical(Wire26.LogicalDocument value) {
        return new LogicalValue(value.logicalDomain(), value.value(), value.logicalExtent());
    }
    private static NormalContinuation continuation(Wire26.ContinuationDocument next, UnitKey unit) {
        return new NormalContinuation(next.availability(), Optional.ofNullable(next.statement()).map(id -> new StatementId(unit, id)), provenance(next.provenance(), unit));
    }
    private static DataReference reference(Wire26.ReferenceDocument target, StatementId statement, UnitKey unit) {
        var binding = target.binding();
        return new DataReference(new OperandId(statement, target.id()), target.role(),
            new Binding(ResolutionStatus.valueOf(binding.status().name()), binding.candidates().stream().map(c -> new DataId(unit, c.id())).toList(),
                Optional.ofNullable(binding.selected()).map(id -> new DataId(unit, id)), Optional.of(ResolutionReason.valueOf(binding.reason().name())),
                binding.candidates().stream().map(Wire.CandidateDocument::canonicalName).toList()),
            Optional.ofNullable(target.wholeItemAccess()).map(w -> new WholeItemAccess(new DataId(unit, w.data()))), provenance(target.provenance(), unit));
    }
    private static IfArm arm(Wire26.ArmDocument a, UnitKey unit) {
        return new IfArm(a.presence(),a.contentAvailability(),executableStart(a.entry(),unit),provenance(a.provenance(),unit),a.gapCodes());
    }
    private static IndependentStorageSet storage(Wire26.StorageDocument s, UnitKey unit) {
        return new IndependentStorageSet(s.availability(), s.rule(), s.authority(), s.members().stream().map(id -> new DataId(unit,id)).toList(),
            Optional.ofNullable(s.provenance()).map(p -> provenance(p,unit)), s.gapCodes());
    }
    private static PerformLoop loop(Wire26.PerformLoopDocument l, StatementId owner, UnitKey unit) {
        var condition=l.condition();var p=condition.predicate();
        return new PerformLoop(l.testMode(),condition.shape(),new PredicateGuarantee(p.availability(),p.profile(),p.resultDomain(),p.evaluation(),p.normalCompletion(),
            p.readsCompleteness(),p.truthValue(),p.knownReads().stream().map(id->new OperandId(owner,id)).toList(),provenance(p.provenance(),unit),p.gapCodes()),
            condition.references().stream().map(r->reference(r,owner,unit)).toList(),provenance(condition.provenance(),unit));
    }
    static SpInput input(Wire29.Document d) {
        var common=input(Wire29.common(d));var s=common.storage().orElseThrow();var unit=common.unit();
        var renames=d.storage().renames().stream().map(r->new StorageFacts.Renames(new StorageFacts.RelationId(unit,r.id()),
            new StorageFacts.NodeId(unit,r.owner()),Optional.ofNullable(r.from()).map(id->new StorageFacts.NodeId(unit,id)),
            Optional.ofNullable(r.through()).map(id->new StorageFacts.NodeId(unit,id)),r.status(),provenance(r.provenance(),unit),r.gapCodes())).toList();
        return new SpInput(unit,common.policy(),common.dataDeclarations(),common.statements(),common.structure(),common.gaps(),common.coverage(),common.entryInventory(),
            common.storageIndependence(),common.compositional(),Optional.of(new StorageFacts.Inventory(s.profile(),s.profileId(),s.runtimeCodec(),s.nodes(),s.bases(),s.views(),s.gapCodes(),s.relations(),renames)));
    }
    static SpInput input(Wire28.Document d) {
        var common=input(Wire28.common(d));var s=common.storage().orElseThrow();var unit=common.unit();
        var relations=d.storage().relations().stream().map(r->new StorageFacts.Relation(new StorageFacts.RelationId(unit,r.id()),
            new StorageFacts.NodeId(unit,r.owner()),Optional.ofNullable(r.target()).map(id->new StorageFacts.NodeId(unit,id)),r.status(),provenance(r.provenance(),unit),r.gapCodes())).toList();
        return new SpInput(unit,common.policy(),common.dataDeclarations(),common.statements(),common.structure(),common.gaps(),common.coverage(),common.entryInventory(),
            common.storageIndependence(),common.compositional(),Optional.of(new StorageFacts.Inventory(s.profile(),s.profileId(),s.runtimeCodec(),s.nodes(),s.bases(),s.views(),s.gapCodes(),relations)));
    }
    static SpInput input(Wire215.Document d) {
        var common=input(Wire215.common(d));var s=common.storage().orElseThrow();var unit=common.unit();var e=d.storage().entryState();
        var entry=new StorageFacts.EntryState(e.mode(),e.conditions().stream().map(v->new StorageFacts.InitialCondition(new StorageFacts.NodeId(unit,v.node()),
            v.kind(),v.bytes(),v.gapCodes(),provenance(v.provenance(),unit),v.proof())).toList());
        return new SpInput(unit,common.policy(),common.dataDeclarations(),common.statements(),common.structure(),common.gaps(),common.coverage(),common.entryInventory(),
            common.storageIndependence(),common.compositional(),Optional.of(new StorageFacts.Inventory(s.profile(),s.profileId(),s.runtimeCodec(),s.nodes(),s.bases(),s.views(),s.gapCodes(),s.relations(),s.renames(),entry)));
    }
    static SpInput input(Wire212.Document d) {
        var common=input(Wire212.common(d));var s=common.storage().orElseThrow();var unit=common.unit();var e=d.storage().entryState();
        var entry=new StorageFacts.EntryState(e.mode(),e.conditions().stream().map(v->new StorageFacts.InitialCondition(new StorageFacts.NodeId(unit,v.node()),
            v.kind(),v.bytes(),v.gapCodes(),provenance(v.provenance(),unit))).toList());
        return new SpInput(unit,common.policy(),common.dataDeclarations(),common.statements(),common.structure(),common.gaps(),common.coverage(),common.entryInventory(),
            common.storageIndependence(),common.compositional(),Optional.of(new StorageFacts.Inventory(s.profile(),s.profileId(),s.runtimeCodec(),s.nodes(),s.bases(),s.views(),s.gapCodes(),s.relations(),s.renames(),entry)));
    }
    static SpInput input(Wire211.Document d) {
        var unit = unitKey(d.unit(), null);
        return new SpInput(unit, policy(d.policy(), unit), d.dataDeclarations().stream().map(v -> {
            var scalar = Optional.ofNullable(v.scalarText()).map(t -> new ScalarText(t.logicalDomain(), t.logicalExtent(), t.storageClass(), t.declarationScope()));
            return new DataFact(new DataId(unit, v.id()), v.canonicalName(), Optional.ofNullable(v.picture()),
                provenance(v.provenance(), unit), v.coverage(), readiness(v.readiness(), unit), scalar,Optional.ofNullable(v.scalarInteger()).map(n->new ScalarInteger(n.digits())));
        }).toList(), d.statements().stream().map(v -> statement(v, unit)).toList(), structure(d.structure(), unit),
            d.gaps().stream().map(v -> gap(v, unit)).toList(), coverage(d.coverage(), unit), entryInventory(d.entryInventory(), unit), Optional.of(storage(d.storageIndependence(), unit)), true, Optional.of(physicalStorage(d.storage(),unit)));
    }
    private static StatementFact statement(Wire211.StatementDocument value, UnitKey unit) {
        var h = statementHeader(value.header(), unit);
        return switch (value) {
            case Wire211.ConditionalGoToDocument v -> new ConditionalGoToFact(h,Optional.ofNullable(v.selector()).map(r->reference(r,h.id(),unit)),
                v.selectorInteger(),provenance(v.selectorOrigin(),unit),v.destinations().stream().map(d->new GoToDestination(d.ordinal(),
                    Optional.ofNullable(d.target()).map(id->new ProcedureId(unit,id)),Optional.ofNullable(d.procedureOrigin()).map(o->provenance(o,unit)),
                    provenance(d.referenceOrigin(),unit),Optional.ofNullable(d.targetEntry()).map(id->new StatementId(unit,id)),
                    Optional.ofNullable(d.entryOrigin()).map(o->provenance(o,unit)),d.gapCodes())).toList(),continuation(v.normalContinuation(),unit),v.gapCodes());
            case Wire211.GoToDocument v -> new GoToFact(h,Optional.ofNullable(v.target()).map(t ->
                new GoToTarget(new ProcedureId(unit,t.id()),provenance(t.paragraphOrigin(),unit))),provenance(v.referenceOrigin(),unit),
                Optional.ofNullable(v.targetEntry()).map(id -> new StatementId(unit,id)),Optional.ofNullable(v.entryOrigin()).map(o -> provenance(o,unit)),v.gapCodes());
            case Wire211.EvaluateDocument v -> new EvaluateFact(h, Optional.ofNullable(v.subject()).map(s -> reference(s,h.id(),unit)),
                v.arms().stream().map(a -> { var l=(Wire211.LiteralDocument)a.selection();
                    return new EvaluateArm(a.ordinal(), new LiteralSource(new OperandId(h.id(),l.id()),l.kind(),
                        Optional.ofNullable(l.logicalValue()).map(Materialize::logical),provenance(l.provenance(),unit)),
                        a.statements().stream().map(id -> new StatementId(unit,id)).toList(), arm(a.control(),unit)); }).toList(),
                arm(v.otherArm(),unit),v.otherStatements().stream().map(id -> new StatementId(unit,id)).toList(),
                continuation(v.normalContinuation(),unit),v.gapCodes());
            case Wire211.ProcedurePerformDocument v -> new ProcedurePerformFact(h,
                Optional.ofNullable(v.start()).map(t->new PerformTarget(new ProcedureId(unit,t.id()),provenance(t.referenceOrigin(),unit),provenance(t.paragraphOrigin(),unit))),
                Optional.ofNullable(v.end()).map(t->new PerformTarget(new ProcedureId(unit,t.id()),provenance(t.referenceOrigin(),unit),provenance(t.paragraphOrigin(),unit))),
                v.procedures().stream().map(r->new PerformParagraph(new ProcedureId(unit,r.id()),new StatementId(unit,r.entry()),
                    r.statements().stream().map(id->new StatementId(unit,id)).toList(),r.completions().stream().map(id->new StatementId(unit,id)).toList(),provenance(r.provenance(),unit))).toList(),
                continuation(v.normalContinuation(),unit),Optional.ofNullable(v.loop()).map(l->loop(l,h.id(),unit)),Optional.ofNullable(v.times()).map(t->new PerformCount(t.profile(),Optional.ofNullable(t.integer()),Optional.ofNullable(t.reference()).map(ref->reference(ref,h.id(),unit)),provenance(t.provenance(),unit))),Optional.ofNullable(v.varying()).map(x->new PerformVarying(x.levels(),x.controls().stream().map(o->new VaryingOperand(o.level(),o.role(),Optional.ofNullable(o.integer()),o.references().stream().map(ref->reference(ref,h.id(),unit)).toList(),provenance(o.provenance(),unit))).toList())),v.gapCodes());
            case Wire211.PerformDocument v -> new PerformFact(h, v.profile(), Optional.ofNullable(v.target()).map(t ->
                new PerformTarget(new ProcedureId(unit, t.id()), provenance(t.referenceOrigin(), unit), provenance(t.paragraphOrigin(), unit))),
                Optional.ofNullable(v.targetEntry()).map(id -> new StatementId(unit, id)), v.targetStatements().stream().map(id -> new StatementId(unit,id)).toList(),
                Optional.ofNullable(v.targetExit()).map(id -> new StatementId(unit,id)), continuation(v.normalContinuation(), unit),
                List.of(), v.gapCodes());
            case Wire211.GobackDocument v -> new GobackFact(h, v.exit(), v.localContinuation());
            case Wire211.MoveDocument v -> {
                MoveSource source = switch (v.source()) {
                    case Wire211.LiteralDocument literal -> new LiteralSource(new OperandId(h.id(), literal.id()), literal.kind(),
                        Optional.ofNullable(literal.logicalValue()).map(Materialize::logical), provenance(literal.provenance(), unit));
                    case Wire211.DataSourceDocument data -> reference(data.reference(), h.id(), unit);
                };
                yield new MoveFact(h, source, reference(v.target(), h.id(), unit), v.copySemantics(), continuation(v.normalContinuation(), unit),
                    Optional.ofNullable(v.textAdjustment()).map(adjustment -> new TextAdjustment(adjustment.rule(), adjustment.receiverExtent(),
                        logical(adjustment.result()), provenance(adjustment.provenance(), unit))),
                    Optional.ofNullable(v.regionalMove()).map(m->new StorageFacts.Move(m.kind(),m.bytes(),m.gapCodes())),v.additionalTransfers().stream().map(t->new MoveTransfer(source211(t.source(),h.id(),unit),reference(t.target(),h.id(),unit),new StorageFacts.Move(t.effect().kind(),t.effect().bytes(),t.effect().gapCodes()))).toList());
            }
            case Wire211.CicsDocument v -> {
                Optional<CallTarget> target=Optional.ofNullable(v.target()).map(t->switch(t) {
                    case Wire211.DataTargetDocument d -> new DataCallTarget(reference(d.reference(),h.id(),unit));
                    case Wire211.LiteralTargetDocument l -> new LiteralCallTarget(new OperandId(h.id(),l.id()),l.text(),l.writtenText(),
                        Optional.ofNullable(l.logicalValue()).map(x->new LogicalValue(x.logicalDomain(),x.value(),x.logicalExtent())),provenance(l.provenance(),unit));
                });
                yield new CicsFact(h,v.command(),v.rawText(),target,v.options().stream().map(o->new CicsOption(o.name(),Optional.ofNullable(o.operand()),o.start(),o.end(),Optional.ofNullable(o.reference()).map(r->reference(r,h.id(),unit)))).toList(),
                    v.conditions(),continuation(v.localContinuation(),unit),continuation(v.ordinaryContinuation(),unit),v.nameProfile(),v.gapCodes());
            }
            case Wire211.CallDocument v -> {
                CallTarget target = switch (v.target()) {
                    case Wire211.DataTargetDocument d -> new DataCallTarget(reference(d.reference(), h.id(), unit));
                    case Wire211.LiteralTargetDocument l -> new LiteralCallTarget(new OperandId(h.id(), l.id()), l.text(), l.writtenText(),
                        Optional.ofNullable(l.logicalValue()).map(Materialize::logical), provenance(l.provenance(), unit));
                };
                var s = v.surface();
                yield new CallFact(h, v.syntax(), target, v.runtimeTarget(), v.runtimeUncertaintyCode(), continuation(v.normalContinuation(), unit),
                    new CallSurface(s.using(), Optional.ofNullable(s.argumentCount()), s.returning(), s.onException(), s.notOnException(), s.onOverflow()), v.effects(), v.outcomes());
            }
            case Wire211.IfDocument v -> {
                var condition=v.condition(); var p=condition.predicate();
                var predicate=new PredicateGuarantee(p.availability(),p.profile(),p.resultDomain(),p.evaluation(),p.normalCompletion(),
                    p.readsCompleteness(),p.truthValue(),p.knownReads().stream().map(id -> new OperandId(h.id(),id)).toList(),provenance(p.provenance(),unit),p.gapCodes());
                yield new IfFact(h,condition.shape(),predicate,condition.references().stream().map(r -> reference(r,h.id(),unit)).toList(),
                    provenance(condition.provenance(),unit),v.explicitlyTerminated(),Optional.ofNullable(v.continuation()).map(id -> new StatementId(unit,id)),
                    continuation(v.normalContinuation(),unit),arm(v.thenArm(),unit),arm(v.elseArm(),unit),v.profile());
            }
            case Wire211.ObservedDocument v -> observed(h, v.observedKind(), v.observedShape(), v.gapCode(), continuation(v.normalContinuation(),unit), v.knownReferences().stream().map(r->reference(r,h.id(),unit)).toList());
        };
    }
    private static LogicalValue logical(Wire211.LogicalDocument value) {
        return new LogicalValue(value.logicalDomain(), value.value(), value.logicalExtent());
    }
    private static NormalContinuation continuation(Wire211.ContinuationDocument next, UnitKey unit) {
        return new NormalContinuation(next.availability(), Optional.ofNullable(next.statement()).map(id -> new StatementId(unit, id)), provenance(next.provenance(), unit));
    }
    private static DataReference reference(Wire211.ReferenceDocument target, StatementId statement, UnitKey unit) {
        var binding = target.binding();
        return new DataReference(new OperandId(statement, target.id()), target.role(),
            new Binding(ResolutionStatus.valueOf(binding.status().name()), binding.candidates().stream().map(c -> new DataId(unit, c.id())).toList(),
                Optional.ofNullable(binding.selected()).map(id -> new DataId(unit, id)), Optional.of(ResolutionReason.valueOf(binding.reason().name())),
                binding.candidates().stream().map(Wire.CandidateDocument::canonicalName).toList()),
            Optional.ofNullable(target.wholeItemAccess()).map(w -> new WholeItemAccess(new DataId(unit, w.data()))), provenance(target.provenance(), unit),
            Optional.ofNullable(target.regionalAccess()).map(a->new StorageFacts.Access(new StorageFacts.NodeId(unit,a.view()),Optional.ofNullable(a.slice()).map(slice->new StorageFacts.Slice(unsigned(slice.offset()),unsigned(slice.extent()))))),
            target.regionalAlternatives()==null?List.of():target.regionalAlternatives().stream().map(x->new StorageFacts.Access(new StorageFacts.NodeId(unit,x.view()),Optional.ofNullable(x.slice()).map(t->new StorageFacts.Slice(unsigned(t.offset()),unsigned(t.extent()))))).toList());
    }
    private static IfArm arm(Wire211.ArmDocument a, UnitKey unit) {
        return new IfArm(a.presence(),a.contentAvailability(),executableStart(a.entry(),unit),provenance(a.provenance(),unit),a.gapCodes());
    }
    private static IndependentStorageSet storage(Wire211.StorageDocument s, UnitKey unit) {
        return new IndependentStorageSet(s.availability(), s.rule(), s.authority(), s.members().stream().map(id -> new DataId(unit,id)).toList(),
            Optional.ofNullable(s.provenance()).map(p -> provenance(p,unit)), s.gapCodes());
    }
    private static PerformLoop loop(Wire211.PerformLoopDocument l, StatementId owner, UnitKey unit) {
        var condition=l.condition();var p=condition.predicate();
        return new PerformLoop(l.testMode(),condition.shape(),new PredicateGuarantee(p.availability(),p.profile(),p.resultDomain(),p.evaluation(),p.normalCompletion(),
            p.readsCompleteness(),p.truthValue(),p.knownReads().stream().map(id->new OperandId(owner,id)).toList(),provenance(p.provenance(),unit),p.gapCodes()),
            condition.references().stream().map(r->reference(r,owner,unit)).toList(),provenance(condition.provenance(),unit));
    }
    private static StorageFacts.Measure measure(Wire211.MeasureDocument m) {
        return new StorageFacts.Measure(Optional.ofNullable(m.value()).map(java.math.BigInteger::new),m.gapCodes());
    }
    private static StorageFacts.Inventory physicalStorage(Wire211.PhysicalStorageDocument s, UnitKey unit) {
        return new StorageFacts.Inventory(s.profile(),Optional.ofNullable(s.profileId()),Optional.ofNullable(s.runtimeCodec()),
            s.nodes().stream().map(n->new StorageFacts.Node(new StorageFacts.NodeId(unit,n.id()),Optional.ofNullable(n.parent()).map(id->new StorageFacts.NodeId(unit,id)),
                n.order(),n.filler(),n.kind(),Optional.ofNullable(n.data()).map(id->new DataId(unit,id)),measure(n.extent()),provenance(n.provenance(),unit))).toList(),
            s.bases().stream().map(b->new StorageFacts.Base(new StorageFacts.BaseId(unit,b.id()),measure(b.extent()),b.allocation(),provenance(b.provenance(),unit))).toList(),
            s.views().stream().map(v->new StorageFacts.View(new StorageFacts.NodeId(unit,v.node()),new StorageFacts.BaseId(unit,v.base()),measure(v.offset()),measure(v.extent()),
                Optional.ofNullable(v.codec()),provenance(v.provenance(),unit))).toList(),s.gapCodes(),
            s.relations().stream().map(x->new StorageFacts.Relation(new StorageFacts.RelationId(unit,x.id()),new StorageFacts.NodeId(unit,x.owner()),
                Optional.ofNullable(x.target()).map(id->new StorageFacts.NodeId(unit,id)),x.status(),provenance(x.provenance(),unit),x.gapCodes())).toList(),
            s.renames().stream().map(x->new StorageFacts.Renames(new StorageFacts.RelationId(unit,x.id()),new StorageFacts.NodeId(unit,x.owner()),
                Optional.ofNullable(x.from()).map(id->new StorageFacts.NodeId(unit,id)),Optional.ofNullable(x.through()).map(id->new StorageFacts.NodeId(unit,id)),
                x.status(),provenance(x.provenance(),unit),x.gapCodes())).toList());
    }

    private static MoveSource source211(Wire211.MoveSourceDocument source,StatementId statement,UnitKey unit) {
        return switch(source) {
            case Wire211.LiteralDocument l->new LiteralSource(new OperandId(statement,l.id()),l.kind(),Optional.ofNullable(l.logicalValue()).map(Materialize::logical),provenance(l.provenance(),unit));
            case Wire211.DataSourceDocument d->reference(d.reference(),statement,unit);
        };
    }
    static SpInput input(Wire210.Document d) {
        var unit = unitKey(d.unit(), null);
        return new SpInput(unit, policy(d.policy(), unit), d.dataDeclarations().stream().map(v -> {
            var scalar = Optional.ofNullable(v.scalarText()).map(t -> new ScalarText(t.logicalDomain(), t.logicalExtent(), t.storageClass(), t.declarationScope()));
            return new DataFact(new DataId(unit, v.id()), v.canonicalName(), Optional.ofNullable(v.picture()),
                provenance(v.provenance(), unit), v.coverage(), readiness(v.readiness(), unit), scalar,Optional.ofNullable(v.scalarInteger()).map(n->new ScalarInteger(n.digits())));
        }).toList(), d.statements().stream().map(v -> statement(v, unit)).toList(), structure(d.structure(), unit),
            d.gaps().stream().map(v -> gap(v, unit)).toList(), coverage(d.coverage(), unit), entryInventory(d.entryInventory(), unit), Optional.of(storage(d.storageIndependence(), unit)), true, Optional.of(physicalStorage(d.storage(),unit)));
    }
    private static StatementFact statement(Wire210.StatementDocument value, UnitKey unit) {
        var h = statementHeader(value.header(), unit);
        return switch (value) {
            case Wire210.ConditionalGoToDocument v -> new ConditionalGoToFact(h,Optional.ofNullable(v.selector()).map(r->reference(r,h.id(),unit)),
                v.selectorInteger(),provenance(v.selectorOrigin(),unit),v.destinations().stream().map(d->new GoToDestination(d.ordinal(),
                    Optional.ofNullable(d.target()).map(id->new ProcedureId(unit,id)),Optional.ofNullable(d.procedureOrigin()).map(o->provenance(o,unit)),
                    provenance(d.referenceOrigin(),unit),Optional.ofNullable(d.targetEntry()).map(id->new StatementId(unit,id)),
                    Optional.ofNullable(d.entryOrigin()).map(o->provenance(o,unit)),d.gapCodes())).toList(),continuation(v.normalContinuation(),unit),v.gapCodes());
            case Wire210.GoToDocument v -> new GoToFact(h,Optional.ofNullable(v.target()).map(t ->
                new GoToTarget(new ProcedureId(unit,t.id()),provenance(t.paragraphOrigin(),unit))),provenance(v.referenceOrigin(),unit),
                Optional.ofNullable(v.targetEntry()).map(id -> new StatementId(unit,id)),Optional.ofNullable(v.entryOrigin()).map(o -> provenance(o,unit)),v.gapCodes());
            case Wire210.EvaluateDocument v -> new EvaluateFact(h, Optional.ofNullable(v.subject()).map(s -> reference(s,h.id(),unit)),
                v.arms().stream().map(a -> { var l=(Wire210.LiteralDocument)a.selection();
                    return new EvaluateArm(a.ordinal(), new LiteralSource(new OperandId(h.id(),l.id()),l.kind(),
                        Optional.ofNullable(l.logicalValue()).map(Materialize::logical),provenance(l.provenance(),unit)),
                        a.statements().stream().map(id -> new StatementId(unit,id)).toList(), arm(a.control(),unit)); }).toList(),
                arm(v.otherArm(),unit),v.otherStatements().stream().map(id -> new StatementId(unit,id)).toList(),
                continuation(v.normalContinuation(),unit),v.gapCodes());
            case Wire210.ProcedurePerformDocument v -> new ProcedurePerformFact(h,
                Optional.ofNullable(v.start()).map(t->new PerformTarget(new ProcedureId(unit,t.id()),provenance(t.referenceOrigin(),unit),provenance(t.paragraphOrigin(),unit))),
                Optional.ofNullable(v.end()).map(t->new PerformTarget(new ProcedureId(unit,t.id()),provenance(t.referenceOrigin(),unit),provenance(t.paragraphOrigin(),unit))),
                v.procedures().stream().map(r->new PerformParagraph(new ProcedureId(unit,r.id()),new StatementId(unit,r.entry()),
                    r.statements().stream().map(id->new StatementId(unit,id)).toList(),r.completions().stream().map(id->new StatementId(unit,id)).toList(),provenance(r.provenance(),unit))).toList(),
                continuation(v.normalContinuation(),unit),Optional.ofNullable(v.loop()).map(l->loop(l,h.id(),unit)),Optional.ofNullable(v.times()).map(t->new PerformCount(t.profile(),Optional.ofNullable(t.integer()),Optional.ofNullable(t.reference()).map(ref->reference(ref,h.id(),unit)),provenance(t.provenance(),unit))),Optional.ofNullable(v.varying()).map(x->new PerformVarying(x.levels(),x.controls().stream().map(o->new VaryingOperand(o.level(),o.role(),Optional.ofNullable(o.integer()),o.references().stream().map(ref->reference(ref,h.id(),unit)).toList(),provenance(o.provenance(),unit))).toList())),v.gapCodes());
            case Wire210.PerformDocument v -> new PerformFact(h, v.profile(), Optional.ofNullable(v.target()).map(t ->
                new PerformTarget(new ProcedureId(unit, t.id()), provenance(t.referenceOrigin(), unit), provenance(t.paragraphOrigin(), unit))),
                Optional.ofNullable(v.targetEntry()).map(id -> new StatementId(unit, id)), v.targetStatements().stream().map(id -> new StatementId(unit,id)).toList(),
                Optional.ofNullable(v.targetExit()).map(id -> new StatementId(unit,id)), continuation(v.normalContinuation(), unit),
                List.of(), v.gapCodes());
            case Wire210.GobackDocument v -> new GobackFact(h, v.exit(), v.localContinuation());
            case Wire210.MoveDocument v -> {
                MoveSource source = switch (v.source()) {
                    case Wire210.LiteralDocument literal -> new LiteralSource(new OperandId(h.id(), literal.id()), literal.kind(),
                        Optional.ofNullable(literal.logicalValue()).map(Materialize::logical), provenance(literal.provenance(), unit));
                    case Wire210.DataSourceDocument data -> reference(data.reference(), h.id(), unit);
                };
                yield new MoveFact(h, source, reference(v.target(), h.id(), unit), v.copySemantics(), continuation(v.normalContinuation(), unit),
                    Optional.ofNullable(v.textAdjustment()).map(adjustment -> new TextAdjustment(adjustment.rule(), adjustment.receiverExtent(),
                        logical(adjustment.result()), provenance(adjustment.provenance(), unit))),
                    Optional.ofNullable(v.regionalMove()).map(m->new StorageFacts.Move(m.kind(),m.bytes(),m.gapCodes())));
            }
            case Wire210.CallDocument v -> {
                CallTarget target = switch (v.target()) {
                    case Wire210.DataTargetDocument d -> new DataCallTarget(reference(d.reference(), h.id(), unit));
                    case Wire210.LiteralTargetDocument l -> new LiteralCallTarget(new OperandId(h.id(), l.id()), l.text(), l.writtenText(),
                        Optional.ofNullable(l.logicalValue()).map(Materialize::logical), provenance(l.provenance(), unit));
                };
                var s = v.surface();
                yield new CallFact(h, v.syntax(), target, v.runtimeTarget(), v.runtimeUncertaintyCode(), continuation(v.normalContinuation(), unit),
                    new CallSurface(s.using(), Optional.ofNullable(s.argumentCount()), s.returning(), s.onException(), s.notOnException(), s.onOverflow()), v.effects(), v.outcomes());
            }
            case Wire210.IfDocument v -> {
                var condition=v.condition(); var p=condition.predicate();
                var predicate=new PredicateGuarantee(p.availability(),p.profile(),p.resultDomain(),p.evaluation(),p.normalCompletion(),
                    p.readsCompleteness(),p.truthValue(),p.knownReads().stream().map(id -> new OperandId(h.id(),id)).toList(),provenance(p.provenance(),unit),p.gapCodes());
                yield new IfFact(h,condition.shape(),predicate,condition.references().stream().map(r -> reference(r,h.id(),unit)).toList(),
                    provenance(condition.provenance(),unit),v.explicitlyTerminated(),Optional.ofNullable(v.continuation()).map(id -> new StatementId(unit,id)),
                    continuation(v.normalContinuation(),unit),arm(v.thenArm(),unit),arm(v.elseArm(),unit),v.profile());
            }
            case Wire210.ObservedDocument v -> observed(h, v.observedKind(), v.observedShape(), v.gapCode(), continuation(v.normalContinuation(),unit), v.knownReferences().stream().map(r->reference(r,h.id(),unit)).toList());
        };
    }
    private static LogicalValue logical(Wire210.LogicalDocument value) {
        return new LogicalValue(value.logicalDomain(), value.value(), value.logicalExtent());
    }
    private static NormalContinuation continuation(Wire210.ContinuationDocument next, UnitKey unit) {
        return new NormalContinuation(next.availability(), Optional.ofNullable(next.statement()).map(id -> new StatementId(unit, id)), provenance(next.provenance(), unit));
    }
    private static DataReference reference(Wire210.ReferenceDocument target, StatementId statement, UnitKey unit) {
        var binding = target.binding();
        return new DataReference(new OperandId(statement, target.id()), target.role(),
            new Binding(ResolutionStatus.valueOf(binding.status().name()), binding.candidates().stream().map(c -> new DataId(unit, c.id())).toList(),
                Optional.ofNullable(binding.selected()).map(id -> new DataId(unit, id)), Optional.of(ResolutionReason.valueOf(binding.reason().name())),
                binding.candidates().stream().map(Wire.CandidateDocument::canonicalName).toList()),
            Optional.ofNullable(target.wholeItemAccess()).map(w -> new WholeItemAccess(new DataId(unit, w.data()))), provenance(target.provenance(), unit),
            Optional.ofNullable(target.regionalAccess()).map(a->new StorageFacts.Access(new StorageFacts.NodeId(unit,a.view()),Optional.ofNullable(a.slice()).map(slice->new StorageFacts.Slice(unsigned(slice.offset()),unsigned(slice.extent()))))));
    }
    private static IfArm arm(Wire210.ArmDocument a, UnitKey unit) {
        return new IfArm(a.presence(),a.contentAvailability(),executableStart(a.entry(),unit),provenance(a.provenance(),unit),a.gapCodes());
    }
    private static IndependentStorageSet storage(Wire210.StorageDocument s, UnitKey unit) {
        return new IndependentStorageSet(s.availability(), s.rule(), s.authority(), s.members().stream().map(id -> new DataId(unit,id)).toList(),
            Optional.ofNullable(s.provenance()).map(p -> provenance(p,unit)), s.gapCodes());
    }
    private static PerformLoop loop(Wire210.PerformLoopDocument l, StatementId owner, UnitKey unit) {
        var condition=l.condition();var p=condition.predicate();
        return new PerformLoop(l.testMode(),condition.shape(),new PredicateGuarantee(p.availability(),p.profile(),p.resultDomain(),p.evaluation(),p.normalCompletion(),
            p.readsCompleteness(),p.truthValue(),p.knownReads().stream().map(id->new OperandId(owner,id)).toList(),provenance(p.provenance(),unit),p.gapCodes()),
            condition.references().stream().map(r->reference(r,owner,unit)).toList(),provenance(condition.provenance(),unit));
    }
    private static StorageFacts.Measure measure(Wire210.MeasureDocument m) {
        return new StorageFacts.Measure(Optional.ofNullable(m.value()).map(java.math.BigInteger::new),m.gapCodes());
    }
    private static StorageFacts.Inventory physicalStorage(Wire210.PhysicalStorageDocument s, UnitKey unit) {
        return new StorageFacts.Inventory(s.profile(),Optional.ofNullable(s.profileId()),Optional.ofNullable(s.runtimeCodec()),
            s.nodes().stream().map(n->new StorageFacts.Node(new StorageFacts.NodeId(unit,n.id()),Optional.ofNullable(n.parent()).map(id->new StorageFacts.NodeId(unit,id)),
                n.order(),n.filler(),n.kind(),Optional.ofNullable(n.data()).map(id->new DataId(unit,id)),measure(n.extent()),provenance(n.provenance(),unit))).toList(),
            s.bases().stream().map(b->new StorageFacts.Base(new StorageFacts.BaseId(unit,b.id()),measure(b.extent()),b.allocation(),provenance(b.provenance(),unit))).toList(),
            s.views().stream().map(v->new StorageFacts.View(new StorageFacts.NodeId(unit,v.node()),new StorageFacts.BaseId(unit,v.base()),measure(v.offset()),measure(v.extent()),
                Optional.ofNullable(v.codec()),provenance(v.provenance(),unit))).toList(),s.gapCodes(),
            s.relations().stream().map(x->new StorageFacts.Relation(new StorageFacts.RelationId(unit,x.id()),new StorageFacts.NodeId(unit,x.owner()),
                Optional.ofNullable(x.target()).map(id->new StorageFacts.NodeId(unit,id)),x.status(),provenance(x.provenance(),unit),x.gapCodes())).toList(),
            s.renames().stream().map(x->new StorageFacts.Renames(new StorageFacts.RelationId(unit,x.id()),new StorageFacts.NodeId(unit,x.owner()),
                Optional.ofNullable(x.from()).map(id->new StorageFacts.NodeId(unit,id)),Optional.ofNullable(x.through()).map(id->new StorageFacts.NodeId(unit,id)),
                x.status(),provenance(x.provenance(),unit),x.gapCodes())).toList());
    }

    private static java.math.BigInteger unsigned(String value) {
        return new java.math.BigInteger(value);
    }
    static SpInput input(Wire27.Document d) {
        var unit = unitKey(d.unit(), null);
        return new SpInput(unit, policy(d.policy(), unit), d.dataDeclarations().stream().map(v -> {
            var scalar = Optional.ofNullable(v.scalarText()).map(t -> new ScalarText(t.logicalDomain(), t.logicalExtent(), t.storageClass(), t.declarationScope()));
            return new DataFact(new DataId(unit, v.id()), v.canonicalName(), Optional.ofNullable(v.picture()),
                provenance(v.provenance(), unit), v.coverage(), readiness(v.readiness(), unit), scalar,Optional.ofNullable(v.scalarInteger()).map(n->new ScalarInteger(n.digits())));
        }).toList(), d.statements().stream().map(v -> statement(v, unit)).toList(), structure(d.structure(), unit),
            d.gaps().stream().map(v -> gap(v, unit)).toList(), coverage(d.coverage(), unit), entryInventory(d.entryInventory(), unit), Optional.of(storage(d.storageIndependence(), unit)), true, Optional.of(physicalStorage(d.storage(),unit)));
    }
    private static StatementFact statement(Wire27.StatementDocument value, UnitKey unit) {
        var h = statementHeader(value.header(), unit);
        return switch (value) {
            case Wire27.ConditionalGoToDocument v -> new ConditionalGoToFact(h,Optional.ofNullable(v.selector()).map(r->reference(r,h.id(),unit)),
                v.selectorInteger(),provenance(v.selectorOrigin(),unit),v.destinations().stream().map(d->new GoToDestination(d.ordinal(),
                    Optional.ofNullable(d.target()).map(id->new ProcedureId(unit,id)),Optional.ofNullable(d.procedureOrigin()).map(o->provenance(o,unit)),
                    provenance(d.referenceOrigin(),unit),Optional.ofNullable(d.targetEntry()).map(id->new StatementId(unit,id)),
                    Optional.ofNullable(d.entryOrigin()).map(o->provenance(o,unit)),d.gapCodes())).toList(),continuation(v.normalContinuation(),unit),v.gapCodes());
            case Wire27.GoToDocument v -> new GoToFact(h,Optional.ofNullable(v.target()).map(t ->
                new GoToTarget(new ProcedureId(unit,t.id()),provenance(t.paragraphOrigin(),unit))),provenance(v.referenceOrigin(),unit),
                Optional.ofNullable(v.targetEntry()).map(id -> new StatementId(unit,id)),Optional.ofNullable(v.entryOrigin()).map(o -> provenance(o,unit)),v.gapCodes());
            case Wire27.EvaluateDocument v -> new EvaluateFact(h, Optional.ofNullable(v.subject()).map(s -> reference(s,h.id(),unit)),
                v.arms().stream().map(a -> { var l=(Wire27.LiteralDocument)a.selection();
                    return new EvaluateArm(a.ordinal(), new LiteralSource(new OperandId(h.id(),l.id()),l.kind(),
                        Optional.ofNullable(l.logicalValue()).map(Materialize::logical),provenance(l.provenance(),unit)),
                        a.statements().stream().map(id -> new StatementId(unit,id)).toList(), arm(a.control(),unit)); }).toList(),
                arm(v.otherArm(),unit),v.otherStatements().stream().map(id -> new StatementId(unit,id)).toList(),
                continuation(v.normalContinuation(),unit),v.gapCodes());
            case Wire27.ProcedurePerformDocument v -> new ProcedurePerformFact(h,
                Optional.ofNullable(v.start()).map(t->new PerformTarget(new ProcedureId(unit,t.id()),provenance(t.referenceOrigin(),unit),provenance(t.paragraphOrigin(),unit))),
                Optional.ofNullable(v.end()).map(t->new PerformTarget(new ProcedureId(unit,t.id()),provenance(t.referenceOrigin(),unit),provenance(t.paragraphOrigin(),unit))),
                v.procedures().stream().map(r->new PerformParagraph(new ProcedureId(unit,r.id()),new StatementId(unit,r.entry()),
                    r.statements().stream().map(id->new StatementId(unit,id)).toList(),r.completions().stream().map(id->new StatementId(unit,id)).toList(),provenance(r.provenance(),unit))).toList(),
                continuation(v.normalContinuation(),unit),Optional.ofNullable(v.loop()).map(l->loop(l,h.id(),unit)),Optional.ofNullable(v.times()).map(t->new PerformCount(t.profile(),Optional.ofNullable(t.integer()),Optional.ofNullable(t.reference()).map(ref->reference(ref,h.id(),unit)),provenance(t.provenance(),unit))),Optional.ofNullable(v.varying()).map(x->new PerformVarying(x.levels(),x.controls().stream().map(o->new VaryingOperand(o.level(),o.role(),Optional.ofNullable(o.integer()),o.references().stream().map(ref->reference(ref,h.id(),unit)).toList(),provenance(o.provenance(),unit))).toList())),v.gapCodes());
            case Wire27.PerformDocument v -> new PerformFact(h, v.profile(), Optional.ofNullable(v.target()).map(t ->
                new PerformTarget(new ProcedureId(unit, t.id()), provenance(t.referenceOrigin(), unit), provenance(t.paragraphOrigin(), unit))),
                Optional.ofNullable(v.targetEntry()).map(id -> new StatementId(unit, id)), v.targetStatements().stream().map(id -> new StatementId(unit,id)).toList(),
                Optional.ofNullable(v.targetExit()).map(id -> new StatementId(unit,id)), continuation(v.normalContinuation(), unit),
                List.of(), v.gapCodes());
            case Wire27.GobackDocument v -> new GobackFact(h, v.exit(), v.localContinuation());
            case Wire27.MoveDocument v -> {
                MoveSource source = switch (v.source()) {
                    case Wire27.LiteralDocument literal -> new LiteralSource(new OperandId(h.id(), literal.id()), literal.kind(),
                        Optional.ofNullable(literal.logicalValue()).map(Materialize::logical), provenance(literal.provenance(), unit));
                    case Wire27.DataSourceDocument data -> reference(data.reference(), h.id(), unit);
                };
                yield new MoveFact(h, source, reference(v.target(), h.id(), unit), v.copySemantics(), continuation(v.normalContinuation(), unit),
                    Optional.ofNullable(v.textAdjustment()).map(adjustment -> new TextAdjustment(adjustment.rule(), adjustment.receiverExtent(),
                        logical(adjustment.result()), provenance(adjustment.provenance(), unit))),
                    Optional.ofNullable(v.regionalMove()).map(m->new StorageFacts.Move(m.kind(),m.bytes(),m.gapCodes())));
            }
            case Wire27.CallDocument v -> {
                CallTarget target = switch (v.target()) {
                    case Wire27.DataTargetDocument d -> new DataCallTarget(reference(d.reference(), h.id(), unit));
                    case Wire27.LiteralTargetDocument l -> new LiteralCallTarget(new OperandId(h.id(), l.id()), l.text(), l.writtenText(),
                        Optional.ofNullable(l.logicalValue()).map(Materialize::logical), provenance(l.provenance(), unit));
                };
                var s = v.surface();
                yield new CallFact(h, v.syntax(), target, v.runtimeTarget(), v.runtimeUncertaintyCode(), continuation(v.normalContinuation(), unit),
                    new CallSurface(s.using(), Optional.ofNullable(s.argumentCount()), s.returning(), s.onException(), s.notOnException(), s.onOverflow()), v.effects(), v.outcomes());
            }
            case Wire27.IfDocument v -> {
                var condition=v.condition(); var p=condition.predicate();
                var predicate=new PredicateGuarantee(p.availability(),p.profile(),p.resultDomain(),p.evaluation(),p.normalCompletion(),
                    p.readsCompleteness(),p.truthValue(),p.knownReads().stream().map(id -> new OperandId(h.id(),id)).toList(),provenance(p.provenance(),unit),p.gapCodes());
                yield new IfFact(h,condition.shape(),predicate,condition.references().stream().map(r -> reference(r,h.id(),unit)).toList(),
                    provenance(condition.provenance(),unit),v.explicitlyTerminated(),Optional.ofNullable(v.continuation()).map(id -> new StatementId(unit,id)),
                    continuation(v.normalContinuation(),unit),arm(v.thenArm(),unit),arm(v.elseArm(),unit),v.profile());
            }
            case Wire27.ObservedDocument v -> observed(h, v.observedKind(), v.observedShape(), v.gapCode(), continuation(v.normalContinuation(),unit), v.knownReferences().stream().map(r->reference(r,h.id(),unit)).toList());
        };
    }
    private static LogicalValue logical(Wire27.LogicalDocument value) {
        return new LogicalValue(value.logicalDomain(), value.value(), value.logicalExtent());
    }
    private static NormalContinuation continuation(Wire27.ContinuationDocument next, UnitKey unit) {
        return new NormalContinuation(next.availability(), Optional.ofNullable(next.statement()).map(id -> new StatementId(unit, id)), provenance(next.provenance(), unit));
    }
    private static DataReference reference(Wire27.ReferenceDocument target, StatementId statement, UnitKey unit) {
        var binding = target.binding();
        return new DataReference(new OperandId(statement, target.id()), target.role(),
            new Binding(ResolutionStatus.valueOf(binding.status().name()), binding.candidates().stream().map(c -> new DataId(unit, c.id())).toList(),
                Optional.ofNullable(binding.selected()).map(id -> new DataId(unit, id)), Optional.of(ResolutionReason.valueOf(binding.reason().name())),
                binding.candidates().stream().map(Wire.CandidateDocument::canonicalName).toList()),
            Optional.ofNullable(target.wholeItemAccess()).map(w -> new WholeItemAccess(new DataId(unit, w.data()))), provenance(target.provenance(), unit),
            Optional.ofNullable(target.regionalAccess()).map(a->new StorageFacts.Access(new StorageFacts.NodeId(unit,a.view()))));
    }
    private static IfArm arm(Wire27.ArmDocument a, UnitKey unit) {
        return new IfArm(a.presence(),a.contentAvailability(),executableStart(a.entry(),unit),provenance(a.provenance(),unit),a.gapCodes());
    }
    private static IndependentStorageSet storage(Wire27.StorageDocument s, UnitKey unit) {
        return new IndependentStorageSet(s.availability(), s.rule(), s.authority(), s.members().stream().map(id -> new DataId(unit,id)).toList(),
            Optional.ofNullable(s.provenance()).map(p -> provenance(p,unit)), s.gapCodes());
    }
    private static PerformLoop loop(Wire27.PerformLoopDocument l, StatementId owner, UnitKey unit) {
        var condition=l.condition();var p=condition.predicate();
        return new PerformLoop(l.testMode(),condition.shape(),new PredicateGuarantee(p.availability(),p.profile(),p.resultDomain(),p.evaluation(),p.normalCompletion(),
            p.readsCompleteness(),p.truthValue(),p.knownReads().stream().map(id->new OperandId(owner,id)).toList(),provenance(p.provenance(),unit),p.gapCodes()),
            condition.references().stream().map(r->reference(r,owner,unit)).toList(),provenance(condition.provenance(),unit));
    }
    private static StorageFacts.Measure measure(Wire27.MeasureDocument m) {
        return new StorageFacts.Measure(Optional.ofNullable(m.value()).map(java.math.BigInteger::new),m.gapCodes());
    }
    private static StorageFacts.Inventory physicalStorage(Wire27.PhysicalStorageDocument s, UnitKey unit) {
        return new StorageFacts.Inventory(s.profile(),Optional.ofNullable(s.profileId()),Optional.ofNullable(s.runtimeCodec()),
            s.nodes().stream().map(n->new StorageFacts.Node(new StorageFacts.NodeId(unit,n.id()),Optional.ofNullable(n.parent()).map(id->new StorageFacts.NodeId(unit,id)),
                n.order(),n.filler(),n.kind(),Optional.ofNullable(n.data()).map(id->new DataId(unit,id)),measure(n.extent()),provenance(n.provenance(),unit))).toList(),
            s.bases().stream().map(b->new StorageFacts.Base(new StorageFacts.BaseId(unit,b.id()),measure(b.extent()),b.allocation(),provenance(b.provenance(),unit))).toList(),
            s.views().stream().map(v->new StorageFacts.View(new StorageFacts.NodeId(unit,v.node()),new StorageFacts.BaseId(unit,v.base()),measure(v.offset()),measure(v.extent()),
                Optional.ofNullable(v.codec()),provenance(v.provenance(),unit))).toList(),s.gapCodes());
    }

    private static StatementFact statement(Wire.StatementDocument value, UnitKey unit) {
        return switch (value) {
            case Wire.GobackFactDocument v -> gobackFact(v, unit);
            case Wire.MoveDocument v -> OtherStatement.unsupported(statementHeader(v.header(), unit), Variant.MOVE);
            case Wire.CallDocument v -> OtherStatement.unsupported(statementHeader(v.header(), unit), Variant.CALL);
            case Wire.IfDocument v -> OtherStatement.unsupported(statementHeader(v.header(), unit), Variant.IF);
            case Wire.ObservedDocument v -> observed(statementHeader(v.header(), unit), v.observedKind(), v.observedShape(), v.gapCode());
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
