package io.github.gustavo2358.lower.application;

import io.github.gustavo2358.air.model.*;
import io.github.gustavo2358.air.model.Ids.*;
import io.github.gustavo2358.lower.domain.SpInput;
import java.util.*;

/** Ordered first-match choices, with the existing Unknown BOOL predicate abstraction. */
final class EvaluateLowerer {
    static List<Sequence> chain(SpInput.EvaluateFact e,LabelId continuation,ScalarDataTranslator.Result data,
            UnitId unit,LocalIds ids,SourceOrigins origins,List<LoweringResult.OperandLink> links,
            List<Evidence.CoverageItem> items,List<Evidence.Uncertainty> uncertainties) {
        var result=new ArrayList<Sequence>();
        var source=origins.source("statement",e.header().id().handle(),e.header().provenance());
        var completion=origins.source("evaluate-completion",e.header().id().handle(),e.normalContinuation().provenance());
        for(int i=0;i<e.arms().size();i++) {
            var arm=e.arms().get(i);var key=e.header().id().handle()+"/when/"+arm.ordinal();
            var operation=new OperationId(unit,ids.id("operation","evaluate-branch",unit.localId(),key));
            var selection=origins.source("evaluate-selection",key,arm.selection().provenance());
            var entry=origins.source("evaluate-arm",key,arm.control().provenance());
            var inputs=new ArrayList<OriginId>(List.of(source,selection,entry,completion));
            e.subject().ifPresent(s -> inputs.add(origins.source("evaluate-subject",e.header().id().handle(),s.provenance())));
            if(i+1==e.arms().size()) inputs.add(origins.source("evaluate-other",e.header().id().handle(),e.otherArm().provenance()));
            var origin=origins.derived(ids.id("origin","evaluate-choice",unit.localId(),key),inputs,"evaluate-literal@1/ordered-first-match-or-normal-completion");
            var owner=new OperationOwner(operation);
            var operand=new OperandId(owner,ids.id("operand","evaluate-predicate",operation.localId(),key));
            var reason=new UncertaintyId(unit.publication(),ids.id("uncertainty","evaluate-predicate-value",operation.localId(),key));
            uncertainties.add(new Evidence.Uncertainty(reason,"predicate-value-unknown",List.of(Evidence.Dimension.VALUES),
                new Scopes.EntityScope(List.of(operand)),"Subject equals this WHEN literal; runtime truth remains unknown.",origin));
            var dependencies=new ArrayList<Expression>();
            var literalId=new OperandId(owner,ids.id("operand","evaluate-literal",operation.localId(),key));
            dependencies.add(new Expressions.Literal(new Operand.Header(literalId,Operand.Role.VALUE_READ,selection),new Values.TextValue(arm.selection().logicalValue().orElseThrow().value())));
            links.add(new LoweringResult.OperandLink(arm.selection().id(),literalId,selection));
            Scopes.MemoryBound unknownReads=new Scopes.WithinMemory(new Scopes.AllMemory(unit.publication(),true));
            var ref=e.subject().orElse(null);
            var mapping=ref==null?null:ref.wholeItemAccess().map(w -> data.index().get(w.data())).orElse(null);
            if(mapping!=null) {
                var readOrigin=origins.source("evaluate-subject",e.header().id().handle(),ref.provenance());
                var readId=new OperandId(owner,ids.id("operand","evaluate-read",operation.localId(),key));
                var placeId=new OperandId(owner,ids.id("operand","evaluate-place",operation.localId(),key));
                var place=new Places.ObjectPlace(new Operand.Header(placeId,Operand.Role.VALUE_READ,readOrigin),mapping.object());
                dependencies.add(new Expressions.Read(new Operand.Header(readId,Operand.Role.VALUE_READ,readOrigin),place));
                links.add(new LoweringResult.OperandLink(ref.id(),readId,readOrigin));
                links.add(new LoweringResult.OperandLink(ref.id(),placeId,readOrigin));
                unknownReads=Scopes.NoMemory.INSTANCE;
            }
            var predicate=new Expressions.Unknown(new Operand.Header(operand,Operand.Role.PREDICATE,origin),Types.known(Types.Builtin.BOOL),dependencies,unknownReads,reason);
            var exact=new Evidence.Claim(new Scopes.EntityScope(List.of(operation)),Evidence.PrecisionStatus.EXACT,List.of());
            var values=new Evidence.Claim(new Scopes.EntityScope(List.of(operand)),Evidence.PrecisionStatus.OPEN,List.of(reason));
            var thenLabel=PartialProgramAssembler.label(arm.control().entry().statement().orElseThrow(),unit,ids);
            var elseLabel=i+1<e.arms().size()?label(e,i+1,unit,ids):e.otherArm().entry().statement()
                .map(s -> PartialProgramAssembler.label(s,unit,ids)).orElse(continuation);
            var branch=new Operations.Branch(new Operations.Header(operation,origin,Evidence.CoverageStatus.ABSTRACTED,
                new Evidence.Precision(exact,exact,exact,values,exact),List.of(reason)),predicate,thenLabel,elseLabel);
            result.add(new Sequence(label(e,i,unit,ids),List.of(),branch,origin));
        }
        return List.copyOf(result);
    }
    private static LabelId label(SpInput.EvaluateFact e,int ordinal,UnitId unit,LocalIds ids) {
        return ordinal==0?PartialProgramAssembler.label(e.header().id(),unit,ids)
            :new LabelId(unit,ids.id("label","evaluate-choice",e.header().id().handle(),Integer.toString(ordinal)));
    }
}
