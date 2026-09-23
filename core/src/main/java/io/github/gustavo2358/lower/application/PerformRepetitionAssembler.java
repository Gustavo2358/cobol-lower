package io.github.gustavo2358.lower.application;

import io.github.gustavo2358.air.model.*;
import io.github.gustavo2358.air.model.Ids.*;
import io.github.gustavo2358.lower.domain.SpInput;
import java.util.*;

/** Existing SP2.3/2.4/2.5 topology, independent of body precision and call depth. */
final class PerformRepetitionAssembler {
    record Routing(LabelId entry, LabelId completion) { }
    static Routing wrap(SpInput.ProcedurePerformFact p, LabelId target, LabelId destination,
            ScalarDataTranslator.Result data, UnitId unit, LocalIds ids, SourceOrigins origins,
            List<LoweringResult.StatementLink> statements, List<LoweringResult.OperandLink> operands,
            List<Evidence.CoverageItem> items, List<Evidence.Uncertainty> uncertainties, List<Sequence> sequences) {
        var completion=destination;var entry=target;
        if(p.loop().isPresent()) {
            var decisionLabel=new LabelId(unit,ids.id("label","perform-loop-decision",unit.localId(),p.header().id().handle()));
            boolean before=p.loop().get().testMode()==SpInput.PerformTestMode.BEFORE;
            var repeat=target;
            completion=decisionLabel;
            if(before)entry=decisionLabel;
            if(p.varying().isPresent()) {
                var initialLabel=new LabelId(unit,ids.id("label","perform-varying-initialization",unit.localId(),p.header().id().handle()));
                var incrementLabel=new LabelId(unit,ids.id("label","perform-varying-increment",unit.localId(),p.header().id().handle()));
                var initial=PerformVaryingEffects.effect(p,true,before?decisionLabel:target,data,unit,ids,origins,operands,uncertainties);
                var increment=PerformVaryingEffects.effect(p,false,before?decisionLabel:target,data,unit,ids,origins,operands,uncertainties);
                sequences.add(new Sequence(initialLabel,List.of(),initial,initial.header().origin()));
                sequences.add(new Sequence(incrementLabel,List.of(),increment,increment.header().origin()));
                PartialProgramAssembler.link(p.header().id(),initial,initialLabel,statements,items);PartialProgramAssembler.link(p.header().id(),increment,incrementLabel,statements,items);
                entry=initialLabel;
                if(before)completion=incrementLabel;else repeat=incrementLabel;
            }
            var decision=PerformLoopAssembler.decision(p,repeat,destination,data,unit,ids,origins,operands,items,uncertainties);
            sequences.add(new Sequence(decisionLabel,List.of(),decision,decision.header().origin()));
            PartialProgramAssembler.link(p.header().id(),decision,decisionLabel,statements,items);
        }
        if(p.times().isPresent()) {
            var repeatLabel=new LabelId(unit,ids.id("label","perform-count-exhaustion",unit.localId(),p.header().id().handle()));
            var repeat=PerformLoopAssembler.countDecision(p,false,target,destination,data,unit,ids,origins,operands,items,uncertainties);
            sequences.add(new Sequence(repeatLabel,List.of(),repeat,repeat.header().origin()));
            PartialProgramAssembler.link(p.header().id(),repeat,repeatLabel,statements,items);completion=repeatLabel;
            if(p.times().get().profile()==SpInput.PerformCountProfile.INTEGER_ITEM) {
                var initialLabel=new LabelId(unit,ids.id("label","perform-count-entry",unit.localId(),p.header().id().handle()));
                var initial=PerformLoopAssembler.countDecision(p,true,target,destination,data,unit,ids,origins,operands,items,uncertainties);
                sequences.add(new Sequence(initialLabel,List.of(),initial,initial.header().origin()));
                PartialProgramAssembler.link(p.header().id(),initial,initialLabel,statements,items);entry=initialLabel;
            }
        }
        return new Routing(entry,completion);
    }
}
