package io.github.gustavo2358.lower.application;

import io.github.gustavo2358.air.model.*;
import io.github.gustavo2358.air.model.Ids.*;
import io.github.gustavo2358.lower.domain.SpInput;
import java.util.*;

/** One unknown decision and a back edge, independent of runtime iteration count. */
final class PerformLoopAssembler {
    static Operations.Branch decision(SpInput.ProcedurePerformFact p,LabelId body,LabelId resume,
            ScalarDataTranslator.Result data,UnitId unit,LocalIds ids,SourceOrigins origins,
            List<LoweringResult.OperandLink> operands,List<Evidence.CoverageItem> items,List<Evidence.Uncertainty> uncertainties) {
        var loop=p.loop().orElseThrow();
        var operation=new OperationId(unit,ids.id("operation","perform-loop-decision",unit.localId(),p.header().id().handle()));
        var origin=origins.source("perform-loop-condition",p.header().id().handle(),loop.provenance());
        var predicate=IfPredicate.translate(p.header().id(),loop.predicate(),loop.conditionReads(),"perform-loop",
            "perform-loop@1/published-condition-read",operation,data,ids,origins,operands,items,uncertainties);
        var exact=new Evidence.Claim(new Scopes.EntityScope(List.of(operation)),Evidence.PrecisionStatus.EXACT,List.of());
        var values=new Evidence.Claim(new Scopes.EntityScope(List.of(predicate.header().id())),Evidence.PrecisionStatus.OPEN,List.of(predicate.reason()));
        return new Operations.Branch(new Operations.Header(operation,origin,Evidence.CoverageStatus.ABSTRACTED,
            new Evidence.Precision(exact,exact,exact,values,exact),List.of(predicate.reason())),predicate,resume,body);
    }
    static Operations.Branch countDecision(SpInput.ProcedurePerformFact p,boolean initial,LabelId body,LabelId resume,
            ScalarDataTranslator.Result data,UnitId unit,LocalIds ids,SourceOrigins origins,
            List<LoweringResult.OperandLink> operands,List<Evidence.CoverageItem> items,List<Evidence.Uncertainty> uncertainties) {
        var count=p.times().orElseThrow();var role=initial?"perform-count-entry":"perform-count-exhaustion";
        var operation=new OperationId(unit,ids.id("operation",role,unit.localId(),p.header().id().handle()));
        var origin=origins.source(role,p.header().id().handle(),count.provenance());
        var reads=initial?count.reference().stream().toList():List.<SpInput.DataReference>of();
        var predicate=IfPredicate.translateReads(p.header().id(),count.provenance(),reads.stream().map(SpInput.DataReference::id).toList(),reads,role,
            "perform-times@1/count-evaluated-once",operation,data,ids,origins,operands,items,uncertainties);
        var exact=new Evidence.Claim(new Scopes.EntityScope(List.of(operation)),Evidence.PrecisionStatus.EXACT,List.of());
        var values=new Evidence.Claim(new Scopes.EntityScope(List.of(predicate.header().id())),Evidence.PrecisionStatus.OPEN,List.of(predicate.reason()));
        return new Operations.Branch(new Operations.Header(operation,origin,Evidence.CoverageStatus.ABSTRACTED,
            new Evidence.Precision(exact,exact,exact,values,exact),List.of(predicate.reason())),predicate,resume,body);
    }

}
