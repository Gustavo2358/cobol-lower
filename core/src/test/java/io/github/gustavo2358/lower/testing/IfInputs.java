package io.github.gustavo2358.lower.testing;

import io.github.gustavo2358.lower.domain.SpInput;
import java.util.*;
import static io.github.gustavo2358.lower.domain.SpInput.*;

/** Handwritten public SP proofs. Handles deliberately disagree with execution order. */
public final class IfInputs {
    private IfInputs() { }
    public static SpInput create(int dataCount, int thenCount, int elseCount) {
        var base=CallInputs.create(dataCount,thenCount+elseCount,8,"PROGA",false);var unit=base.unit();
        var sourceMoves=base.statements().stream().filter(MoveFact.class::isInstance).map(MoveFact.class::cast).toList();
        var call=(CallFact)base.statements().get(base.statements().size()-2);var terminal=(GobackFact)base.statements().getLast();
        var ifId=new StatementId(unit,"statement:"+(thenCount+elseCount+2));var p=ScalarInputs.provenance(10,4);
        var thenChildren=new ArrayList<StatementId>();var elseChildren=new ArrayList<StatementId>();
        for(int i=thenCount-1;i>=0;i--)thenChildren.add(sourceMoves.get(i).header().id());
        for(int i=thenCount+elseCount-1;i>=thenCount;i--)elseChildren.add(sourceMoves.get(i).header().id());
        var moves=new HashMap<StatementId,MoveFact>();sourceMoves.forEach(m -> moves.put(m.header().id(),m));
        var statements=new ArrayList<StatementFact>();
        for(var children:List.of(thenChildren,elseChildren))for(int i=0;i<children.size();i++) {
            var m=moves.get(children.get(i));boolean thenArm=children==thenChildren;
            var header=new StatementHeader(m.header().id(),statements.size()+1,new Containment(Optional.of(ifId),thenArm?Branch.THEN:Branch.ELSE),m.header().provenance(),m.header().coverage(),m.header().readiness());
            var value=thenArm?"PROGA":"PROGB";
            var source=new LiteralSource(m.source().id(),((io.github.gustavo2358.lower.domain.SpInput.LiteralSource) m.source()).kind(),Optional.of(new LogicalValue(LogicalDomain.TEXT,value,5)),m.source().provenance());
            var next=new NormalContinuation(ContinuationAvailability.KNOWN,Optional.of(i+1<children.size()?children.get(i+1):call.header().id()),m.normalContinuation().provenance());
            statements.add(new MoveFact(header,source,m.target(),CopySemantics.FITTED_TEXT,next,Optional.of(new TextAdjustment(TextAdjustmentRule.RIGHT_PAD_SPACE,8,new LogicalValue(LogicalDomain.TEXT,value+"   ",8),m.header().provenance()))));
        }
        var ifHeader=new StatementHeader(ifId,0,new Containment(Optional.empty(),Branch.ROOT),p,CoverageStatus.MODELED,call.header().readiness());
        var id=new OperandId(ifId,"operand:"+(thenCount+elseCount+2)+":0");var selected=base.dataDeclarations().get(1).id();
        var ref=new DataReference(id,OperandRole.READ,new Binding(ResolutionStatus.RESOLVED,List.of(selected),Optional.of(selected)),Optional.of(new WholeItemAccess(selected)),ScalarInputs.provenance(10,7));
        var predicate=new PredicateGuarantee(Availability.KNOWN,PredicateProfile.SCALAR_TEXT_EQUALITY,PredicateDomain.BOOLEAN,PredicateEvaluation.PURE,PredicateCompletion.TOTAL,ReadsCompleteness.COMPLETE,PredicateTruth.UNKNOWN,List.of(id),ref.provenance(),List.of());
        var thenArm=new IfArm(ClausePresence.PRESENT,Availability.KNOWN,new ExecutableStart(Availability.KNOWN,Optional.of(thenChildren.getFirst())),ScalarInputs.provenance(11,4),List.of());
        var elseArm=new IfArm(elseCount==0?ClausePresence.ABSENT:ClausePresence.PRESENT,Availability.KNOWN,new ExecutableStart(elseCount==0?Availability.UNAVAILABLE:Availability.KNOWN,elseChildren.stream().findFirst()),ScalarInputs.provenance(12,4),List.of());
        var next=new NormalContinuation(ContinuationAvailability.KNOWN,Optional.of(call.header().id()),ScalarInputs.provenance(13,4));
        statements.add(new IfFact(ifHeader,"RELATION",predicate,List.of(ref),ref.provenance(),true,Optional.of(call.header().id()),next,thenArm,elseArm,IfProfile.SIMPLE_TEXT_EQUALITY));
        var h=call.header();var callHeader=new StatementHeader(h.id(),thenCount+elseCount+1,h.containment(),h.provenance(),h.coverage(),h.readiness());
        statements.add(new CallFact(callHeader,call.syntax(),call.target(),call.runtimeTarget(),call.runtimeUncertaintyCode(),call.normalContinuation(),call.surface(),call.effects(),call.outcomes()));
        h=terminal.header();statements.add(new GobackFact(new StatementHeader(h.id(),thenCount+elseCount+2,h.containment(),h.provenance(),h.coverage(),h.readiness()),terminal.exit(),terminal.localContinuation()));
        var e=base.entryInventory().entries().getFirst();var entry=new EntryFact(e.id(),e.role(),e.availability(),new ExecutableStart(Availability.KNOWN,Optional.of(ifId)),e.signature(),e.provenance(),e.coverage(),e.readiness(),e.gaps());
        var coverage=new Coverage(base.coverage().inventoryStatus(),statements.size(),statements.size(),0,0,0,base.coverage().readiness());
        var proofMembers=new ArrayList<>(base.dataDeclarations().stream().map(DataFact::id).toList());Collections.reverse(proofMembers);
        var proof=new IndependentStorageSet(Availability.KNOWN,StorageIndependenceRule.INDEPENDENT_WORKING_STORAGE_ROOTS,"IBM_ENTERPRISE_COBOL_6_4_WORKING_STORAGE",proofMembers,Optional.of(ScalarInputs.provenance(5,0)),List.of());
        return new SpInput(unit,base.policy(),base.dataDeclarations(),statements,new Structure(List.of(ifId,call.header().id(),terminal.header().id()),List.of(new BranchChildren(ifId,Branch.THEN,thenChildren),new BranchChildren(ifId,Branch.ELSE,elseChildren))),base.gaps(),coverage,new EntryInventory(base.entryInventory().status(),base.entryInventory().scope(),List.of(entry),base.entryInventory().gapCodes()),Optional.of(proof));
    }
    public static IfFact branch(SpInput input) { return input.statements().stream().filter(IfFact.class::isInstance).map(IfFact.class::cast).findFirst().orElseThrow(); }
    public static SpInput branch(SpInput input,IfFact replacement) {
        return new SpInput(input.unit(),input.policy(),input.dataDeclarations(),input.statements().stream().map(s -> s instanceof IfFact?replacement:s).toList(),input.structure(),input.gaps(),input.coverage(),input.entryInventory(),input.storageIndependence());
    }
    public static IfFact predicate(IfFact f,PredicateGuarantee p) { return new IfFact(f.header(),f.conditionShape(),p,f.conditionReads(),f.conditionProvenance(),f.explicitlyTerminated(),f.continuation(),f.normalContinuation(),f.thenArm(),f.elseArm(),f.profile()); }
    public static SpInput proof(SpInput input,Optional<IndependentStorageSet> proof) { return new SpInput(input.unit(),input.policy(),input.dataDeclarations(),input.statements(),input.structure(),input.gaps(),input.coverage(),input.entryInventory(),proof); }
    /** Change one named public record component for independent contract counterexamples. */
    public static <T extends Record> T with(T value,String field,Object replacement) {
        try {
            var components=value.getClass().getRecordComponents();var types=new Class<?>[components.length];var values=new Object[components.length];boolean found=false;
            for(int i=0;i<components.length;i++) { types[i]=components[i].getType();values[i]=components[i].getAccessor().invoke(value);if(components[i].getName().equals(field)){values[i]=replacement;found=true;} }
            if(!found)throw new IllegalArgumentException(field);
            @SuppressWarnings("unchecked") T changed=(T)value.getClass().getDeclaredConstructor(types).newInstance(values);return changed;
        } catch(ReflectiveOperationException ex) { throw new AssertionError("public record fixture mutation failed",ex); }
    }

}
