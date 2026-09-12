package io.github.gustavo2358.lower.application;

import io.github.gustavo2358.air.model.*;
import io.github.gustavo2358.air.model.Ids.*;
import io.github.gustavo2358.lower.domain.SpInput;
import java.util.*;

/** One explicit source occurrence per sequence; BASIC bodies specialize by activation. */
final class PartialProgramAssembler {
    record Assembly(List<Sequence> sequences, LabelId entryLabel, OriginId entrySequenceOrigin) { }
    static Assembly assemble(PartialProgramAdmission.Plan plan, ScalarDataTranslator.Result data, UnitId unit,
            LocalIds ids, SourceOrigins origins, List<LoweringResult.StatementLink> statements,
            List<LoweringResult.OperandLink> operands, List<Evidence.CoverageItem> items, List<Evidence.Uncertainty> uncertainties) {
        var sequences=new ArrayList<Sequence>();
        for(var fact:plan.statements()) {
            var label=label(fact.header().id(),unit,ids); var next=PartialProgramAdmission.next(fact);
            var destination=next==null?null:next.statement().map(s->label(s,unit,ids)).orElse(null);
            var instructions=new ArrayList<Instruction>(); Terminator term;
            boolean precise=plan.precise().contains(fact.header().id());
            if(precise && fact instanceof SpInput.MoveFact m) {
                var assign=MoveHandler.translate(m,data,unit,ids,origins,operands,items);instructions.add(assign);
                link(m.header().id(),assign,label,statements,items);
                term=destination!=null ? PerformSequenceAssembler.jump("sequential",m.header().id(),destination,assign.header().origin(),unit,ids)
                    : opaque(fact,null,data,unit,ids,origins,uncertainties,operands,false);
            } else if(precise && fact instanceof SpInput.CallFact call) {
                var completion=origins.source("continuation",call.header().id().handle(),call.normalContinuation().provenance());
                term=InvokeHandler.translate(call,data.index(),destination,completion,unit,ids,origins,operands,items,uncertainties);
                link(fact.header().id(),term,label,statements,items);
            } else if(precise && fact instanceof SpInput.IfFact f) {
                term=IfSequenceAssembler.branch(f,label(f.thenArm().entry().statement().orElseThrow(),unit,ids),
                    f.elseArm().entry().statement().map(s->label(s,unit,ids)).orElse(destination),data,unit,ids,origins,operands,items,uncertainties);
                link(fact.header().id(),term,label,statements,items);
            } else if(precise && fact instanceof SpInput.PerformFact p) {
                var activation=ids.activation(p.header().id().handle());
                var target=label(p.targetEntry().orElseThrow(),unit,activation);
                var proof=PerformSequenceAssembler.control(p,target,destination,unit,ids,origins);term=proof.invoke();
                link(fact.header().id(),term,label,statements,items);
                var body=plan.bodies().get(p.header().id());
                for(int i=0;i<body.size();i++) {
                    var move=body.get(i);var here=label(move.header().id(),unit,activation);
                    var resume=i+1<body.size()?label(body.get(i+1).header().id(),unit,activation):destination;
                    var assign=MoveHandler.translate(move,data,unit,activation,origins,operands,items);
                    link(move.header().id(),assign,here,statements,items);
                    var returning=PerformSequenceAssembler.jump("activation-next",move.header().id(),resume,proof.resumeOrigin(),unit,activation);
                    sequences.add(new Sequence(here,List.of(assign),returning,assign.header().origin()));
                }
            } else if(precise && fact instanceof SpInput.GobackFact g) {
                term=GobackHandler.translate(g,unit,ids,origins,uncertainties);link(fact.header().id(),term,label,statements,items);
            } else if(fact instanceof SpInput.MoveFact m && m.target().role()==SpInput.OperandRole.WRITE
                    && m.target().wholeItemAccess().filter(w->data.index().containsKey(w.data())).isPresent()) {
                var havoc=ConservativeMove.translate(m,data,unit,ids,origins,operands,uncertainties);
                instructions.add(havoc);link(m.header().id(),havoc,label,statements,items);
                term=destination!=null ? PerformSequenceAssembler.jump("conservative-move-next",m.header().id(),destination,havoc.header().origin(),unit,ids)
                    : opaque(fact,null,data,unit,ids,origins,uncertainties,operands,false);
            } else {
                term=opaque(fact,fact instanceof SpInput.IfFact || fact instanceof SpInput.PerformFact ? null : destination,data,unit,ids,origins,uncertainties,operands,true);
                link(fact.header().id(),term,label,statements,items);
            }
            sequences.add(new Sequence(label,instructions,term,term.header().origin()));
        }
        Collections.reverse(sequences);
        var input=plan.admission().input().orElseThrow();
        var entryLabel=label(input.entryInventory().entries().getFirst().start().statement().orElseThrow(),unit,ids);
        var entrySequence=sequences.stream().filter(s->s.label().equals(entryLabel)).findFirst().orElseThrow();
        return new Assembly(List.copyOf(sequences),entryLabel,entrySequence.origin());
    }
    private static Operations.Opaque opaque(SpInput.StatementFact fact,LabelId next,ScalarDataTranslator.Result data,UnitId unit,
            LocalIds ids,SourceOrigins origins,List<Evidence.Uncertainty> uncertainties,List<LoweringResult.OperandLink> operands,boolean unknownEffects) {
        var origin=origins.source("statement",fact.header().id().handle(),fact.header().provenance());
        var id=new OperationId(unit,ids.id("operation","opaque",unit.localId(),fact.header().id().handle()));
        var gap=new UncertaintyId(unit.publication(),ids.id("uncertainty","unsupported-region",id.localId(),"semantics"));
        var known=unknownEffects ? OpaqueOperands.translate(fact,id,data,ids,origins,operands,uncertainties)
            : new OpaqueOperands.Known(List.of(),List.of(),List.of(),List.of());
        var scope=new Scopes.EntityScope(List.of(id));
        var code=!unknownEffects?"NORMAL_CONTINUATION_NOT_PROVEN":fact instanceof SpInput.OtherStatement o?o.gapCode():"PRECISE_SEMANTICS_UNAVAILABLE";
        uncertainties.add(new Evidence.Uncertainty(gap,"cobol-lower:"+code,unknownEffects?List.of(Evidence.Dimension.CONTROL,Evidence.Dimension.EFFECTS,Evidence.Dimension.VALUES,Evidence.Dimension.DEPENDENCIES):List.of(Evidence.Dimension.CONTROL),scope,
            "Source region retained with conservative effects and only proved control",origin));
        var open=new Evidence.Claim(scope,Evidence.PrecisionStatus.OPEN,List.of(gap));
        var exact=new Evidence.Claim(scope,Evidence.PrecisionStatus.EXACT,List.of());
        var header=new Operations.Header(id,origin,Evidence.CoverageStatus.ABSTRACTED,new Evidence.Precision(open,unknownEffects?open:exact,unknownEffects?open:exact,unknownEffects?open:exact,unknownEffects?open:exact),List.of(gap));
        Scopes.MemoryBound memory=unknownEffects?new Scopes.WithinMemory(new Scopes.AllMemory(unit.publication(),true)):Scopes.NoMemory.INSTANCE;
        return new Operations.Opaque(header,fact instanceof SpInput.OtherStatement o?o.observedKind():fact.getClass().getSimpleName(),known.operands(),List.of(),
            new Envelopes.Envelope(new Envelopes.MemoryEnvelope(known.reads(),memory,known.writes(),memory,List.of()),
                next==null?new Control.ControlEnvelope(List.of(),new Scopes.WithinControl(new Scopes.UnitControl(unit,true,true,true,true,true,true)))
                    :new Control.ControlEnvelope(List.of(new Control.JumpAlternative(next)),new Scopes.WithinControl(new Scopes.UnitControl(unit,false,true,true,true,true,true))),
                new Envelopes.DependencyEnvelope(List.of(),unknownEffects?Scopes.AnyResource.INSTANCE:Scopes.NoResources.INSTANCE)));
    }
    private static LabelId label(SpInput.StatementId s,UnitId unit,LocalIds ids) {return new LabelId(unit,ids.id("label","partial-sequence",unit.localId(),s.handle()));}
    private static void link(SpInput.StatementId source,Operation op,LabelId label,List<LoweringResult.StatementLink> statements,List<Evidence.CoverageItem> items) {
        var h=op.header();statements.add(new LoweringResult.StatementLink(source,h.id(),label,h.origin()));
        items.add(new Evidence.CoverageItem("sp-partial@1/"+h.id().localId()+"/"+source.handle(),h.origin(),h.coverage(),List.of(h.id(),label),h.uncertainties(),Optional.empty()));
    }
}
