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
            List<LoweringResult.OperandLink> operands, List<Evidence.CoverageItem> items, List<Evidence.Uncertainty> uncertainties,FileResourceLowering files) {
        var sequences=new ArrayList<Sequence>();
        append(plan,plan.statements(),Map.of(),false,data,unit,ids,origins,statements,operands,items,uncertainties,sequences,files);
        Collections.reverse(sequences);
        var input=plan.admission().input().orElseThrow();
        var entryLabel=label(input.entryInventory().entries().getFirst().start().statement().orElseThrow(),unit,ids);
        var entrySequence=sequences.stream().filter(s->s.label().equals(entryLabel)).findFirst().orElseThrow();
        var initial=LogicalTextMove.initial(plan.storage().logical(),data,unit,ids,origins);
        if(!initial.isEmpty()) {
            var bootstrap=new LabelId(unit,ids.id("label","logical-root-entry",unit.localId(),"entry"));
            var origin=initial.getFirst().header().origin();
            sequences.add(new Sequence(bootstrap,initial,PerformSequenceAssembler.jump("logical-root-entry",input.entryInventory().entries().getFirst().start().statement().orElseThrow(),entryLabel,origin,unit,ids),origin));
            return new Assembly(files.complete(sequences),bootstrap,origin);
        }
        return new Assembly(files.complete(sequences),entryLabel,entrySequence.origin());
    }
    private static void append(PartialProgramAdmission.Plan plan,List<SpInput.StatementFact> sourceStatements,
            Map<SpInput.StatementId,LabelId> overrides,boolean intrinsic,ScalarDataTranslator.Result data,UnitId unit,LocalIds ids,SourceOrigins origins,
            List<LoweringResult.StatementLink> statements,List<LoweringResult.OperandLink> operands,List<Evidence.CoverageItem> items,
            List<Evidence.Uncertainty> uncertainties,List<Sequence> sequences,FileResourceLowering files) {
        for(var fact:sourceStatements) {
            var label=label(fact.header().id(),unit,ids);files.sourceEntry(label); var next=intrinsic?PartialProgramAdmission.next(fact):PartialProgramAdmission.ordinaryNext(fact);
            var destination=overrides.containsKey(fact.header().id())?overrides.get(fact.header().id()):next==null?null:next.statement().map(s->label(s,unit,ids)).orElse(null);
            if(!intrinsic)destination=files.completion(fact.header().id(),destination);
            var instructions=new ArrayList<Instruction>(); Terminator term;
            boolean precise=plan.precise().contains(fact.header().id());
            if(files.handles(fact)) {
                var chain=files.sequences(fact,destination,ids);term=chain.getFirst().terminator();instructions.addAll(chain.getFirst().instructions());
                for(int i=1;i<chain.size();i++)sequences.add(chain.get(i));
                for(var sequence:chain){for(var instruction:sequence.instructions())link(fact.header().id(),instruction,sequence.label(),statements,items);link(fact.header().id(),sequence.terminator(),sequence.label(),statements,items);}
            } else if(precise && fact instanceof SpInput.MoveFact m) {
                var transfers=RegionalMoveHandler.sequence(m,plan.fitted().contains(m.header().id()),data,plan.storage(),unit,ids,origins,operands,items,uncertainties);instructions.addAll(transfers);var assign=transfers.getFirst();
                for(var transfer:transfers)link(m.header().id(),transfer,label,statements,items);
                term=destination!=null ? PerformSequenceAssembler.jump("sequential",m.header().id(),destination,assign.header().origin(),unit,ids)
                    : opaque(fact,null,data,unit,ids,origins,uncertainties,operands,false);
            } else if(fact instanceof SpInput.ConditionalGoToFact g) {
                term=ConditionalGoToLowerer.translate(g,destination,data,unit,ids,origins,operands,uncertainties);
                link(g.header().id(),term,label,statements,items);
            } else if(precise && fact instanceof SpInput.GoToFact g) {
                var source=origins.source("statement",g.header().id().handle(),g.header().provenance());
                var reference=origins.source("goto-reference",g.header().id().handle(),g.referenceOrigin());
                var paragraph=origins.source("goto-paragraph",g.target().orElseThrow().id().handle(),g.target().orElseThrow().paragraphOrigin());
                var entry=origins.source("goto-entry",g.targetEntry().orElseThrow().handle(),g.entryOrigin().orElseThrow());
                var origin=origins.derived(ids.id("origin","goto-target",unit.localId(),g.header().id().handle()),
                    List.of(source,reference,paragraph,entry),"goto-paragraph@1/explicit-executable-entry");
                term=PerformSequenceAssembler.jump("goto-target",g.header().id(),label(g.targetEntry().orElseThrow(),unit,ids),origin,unit,ids);
                link(g.header().id(),term,label,statements,items);
            } else if(precise && fact instanceof SpInput.CicsFileFact cics) {
                term=CicsFileInvokeHandler.translate(cics,data,destination,unit,ids,origins,operands,items,uncertainties);
                link(fact.header().id(),term,label,statements,items);
            } else if(precise && fact instanceof SpInput.CicsFact cics) {
                term=CicsInvokeHandler.translate(cics,data,destination,unit,ids,origins,operands,items,uncertainties);
                link(fact.header().id(),term,label,statements,items);
            } else if(precise && fact instanceof SpInput.CallFact call) {
                var completion=origins.source("continuation",call.header().id().handle(),call.normalContinuation().provenance());
                term=InvokeHandler.translate(call,data.index(),destination,completion,unit,ids,origins,operands,items,uncertainties,plan.storage());
                link(fact.header().id(),term,label,statements,items);
            } else if(precise && fact instanceof SpInput.EvaluateFact e) {
                var chain = EvaluateLowerer.chain(e, destination, data, unit, ids, origins, operands, items, uncertainties);
                term=chain.getFirst().terminator();
                for (int i=1;i<chain.size();i++) sequences.add(chain.get(i));
                for (var sequence : chain) link(fact.header().id(),sequence.terminator(),sequence.label(),statements,items);
            } else if(precise && fact instanceof SpInput.IfFact f) {
                term=IfSequenceAssembler.branch(f,label(f.thenArm().entry().statement().orElseThrow(),unit,ids),
                    f.elseArm().entry().statement().map(s->label(s,unit,ids)).orElse(destination),data,unit,ids,origins,operands,items,uncertainties);
                link(fact.header().id(),term,label,statements,items);
            } else if(fact instanceof SpInput.ProcedurePerformFact p && plan.compositions().containsKey(p.header().id())
                    && !ids.containsActivation(p.header().id().handle())) {
                var body=plan.compositions().get(p.header().id());
                var activation=ids.activation(p.header().id().handle());
                var source=origins.source("statement",p.header().id().handle(),p.header().provenance());
                var reference=origins.source("perform-range-reference",p.header().id().handle()+"/structural",p.start().orElseThrow().referenceOrigin());
                var evidence=new LinkedHashSet<OriginId>(List.of(source,reference));
                evidence.add(origins.source("paragraph",p.start().orElseThrow().id().handle(),p.start().orElseThrow().paragraphOrigin()));
                for(var paragraph:p.procedures())evidence.add(origins.source("paragraph",paragraph.id().handle(),paragraph.provenance()));
                var origin=origins.derived(ids.id("origin","compositional-perform-entry",unit.localId(),p.header().id().handle()),
                    List.copyOf(evidence),"perform-structural@1/activation-entry");
                var target=label(p.targetEntry().orElseThrow(),unit,activation);
                term=PerformSequenceAssembler.jump("compositional-entry",p.header().id(),target,origin,unit,ids);
                link(p.header().id(),term,label,statements,items);
                if(!body.isEmpty()) {
                    var resumeLabel=new LabelId(unit,activation.id("label","perform-normal-resume",unit.localId(),p.header().id().handle()));
                    var resumeIds=activation.activation("normal-resume");
                    var resumeOrigin=origins.derived(resumeIds.id("origin","compositional-perform-resume",unit.localId(),p.header().id().handle()),
                        List.of(source,origin,origins.source("perform-continuation",p.header().id().handle(),p.normalContinuation().provenance())),"perform-structural@1/conditional-activation-resume");
                    Terminator resume=destination==null?opaque(p,null,data,unit,resumeIds,origins,uncertainties,operands,false)
                        :PerformSequenceAssembler.jump("compositional-resume",p.header().id(),destination,resumeOrigin,unit,resumeIds);
                    sequences.add(new Sequence(resumeLabel,List.of(),resume,resumeOrigin));
                    link(p.header().id(),resume,resumeLabel,statements,items);
                    var completions=new HashMap<SpInput.StatementId,LabelId>();
                    for(int i=0;i<p.procedures().size();i++) {
                        var after=i+1<p.procedures().size()?label(p.procedures().get(i+1).entry(),unit,activation):resumeLabel;
                        for(var id:p.procedures().get(i).completions())completions.put(id,after);
                    }
                    append(plan,body,completions,true,data,unit,activation,origins,statements,operands,items,uncertainties,sequences,files);
                }
            } else if(precise && fact instanceof SpInput.ProcedurePerformFact p) {
                var activation=ids.activation(p.header().id().handle());
                var target=label(p.procedures().getFirst().entry(),unit,activation);
                var source=origins.source("statement",p.header().id().handle(),p.header().provenance());
                var evidence=new LinkedHashSet<OriginId>(List.of(source));
                int endpointOrdinal=0;
                for(var endpoint:List.of(p.start().orElseThrow(),p.end().orElseThrow())) {
                    evidence.add(origins.source("perform-range-reference",p.header().id().handle()+"/"+(endpointOrdinal++),endpoint.referenceOrigin()));
                    evidence.add(origins.source("paragraph",endpoint.id().handle(),endpoint.paragraphOrigin()));
                }
                for(var paragraph:p.procedures())evidence.add(origins.source("paragraph",paragraph.id().handle(),paragraph.provenance()));
                evidence.add(origins.source("perform-continuation",p.header().id().handle(),p.normalContinuation().provenance()));
                var origin=origins.derived(ids.id("origin","procedure-perform",unit.localId(),p.header().id().handle()),List.copyOf(evidence),"perform-range@1/isolated-activation");
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
                        link(p.header().id(),initial,initialLabel,statements,items);link(p.header().id(),increment,incrementLabel,statements,items);
                        entry=initialLabel;
                        if(before)completion=incrementLabel;else repeat=incrementLabel;
                    }
                    var decision=PerformLoopAssembler.decision(p,repeat,destination,data,unit,ids,origins,operands,items,uncertainties);
                    sequences.add(new Sequence(decisionLabel,List.of(),decision,decision.header().origin()));
                    link(p.header().id(),decision,decisionLabel,statements,items);
                }
                if(p.times().isPresent()) {
                    var repeatLabel=new LabelId(unit,ids.id("label","perform-count-exhaustion",unit.localId(),p.header().id().handle()));
                    var repeat=PerformLoopAssembler.countDecision(p,false,target,destination,data,unit,ids,origins,operands,items,uncertainties);
                    sequences.add(new Sequence(repeatLabel,List.of(),repeat,repeat.header().origin()));
                    link(p.header().id(),repeat,repeatLabel,statements,items);completion=repeatLabel;
                    if(p.times().get().profile()==SpInput.PerformCountProfile.INTEGER_ITEM) {
                        var initialLabel=new LabelId(unit,ids.id("label","perform-count-entry",unit.localId(),p.header().id().handle()));
                        var initial=PerformLoopAssembler.countDecision(p,true,target,destination,data,unit,ids,origins,operands,items,uncertainties);
                        sequences.add(new Sequence(initialLabel,List.of(),initial,initial.header().origin()));
                        link(p.header().id(),initial,initialLabel,statements,items);entry=initialLabel;
                    }
                }
                term=PerformSequenceAssembler.jump("range-entry",p.header().id(),entry,origin,unit,ids);
                link(p.header().id(),term,label,statements,items);
                var completions=new HashMap<SpInput.StatementId,LabelId>();
                for(int i=0;i<p.procedures().size();i++) {
                    var paragraph=p.procedures().get(i);
                    var resume=i+1<p.procedures().size()?label(p.procedures().get(i+1).entry(),unit,activation):completion;
                    for(var id:paragraph.completions())completions.put(id,resume);
                }
                append(plan,plan.ranges().get(p.header().id()),completions,true,data,unit,activation,origins,statements,operands,items,uncertainties,sequences,files);
            } else if(precise && fact instanceof SpInput.PerformFact p) {
                var activation=ids.activation(p.header().id().handle());
                var target=label(p.targetEntry().orElseThrow(),unit,activation);
                var proof=PerformSequenceAssembler.control(p,target,destination,unit,ids,origins);term=proof.invoke();
                link(fact.header().id(),term,label,statements,items);
                var body=plan.bodies().get(p.header().id());
                for(int i=0;i<body.size();i++) {
                    var move=body.get(i);var here=label(move.header().id(),unit,activation);files.sourceEntry(here);
                    var resume=i+1<body.size()?label(body.get(i+1).header().id(),unit,activation):destination;
                    var transfers=RegionalMoveHandler.sequence(move,plan.fitted().contains(move.header().id()),data,plan.storage(),unit,activation,origins,operands,items,uncertainties);var assign=transfers.getFirst();
                    for(var transfer:transfers)link(move.header().id(),transfer,here,statements,items);
                    var continuation=i+1<body.size()
                        ? origins.source("continuation",move.header().id().handle(),move.normalContinuation().provenance())
                        : origins.derived(activation.id("origin","activation-return",unit.localId(),move.header().id().handle()),
                            List.of(proof.performOrigin(),proof.referenceOrigin(),proof.paragraphOrigin(),proof.resumeOrigin(),assign.header().origin()),
                            "perform-basic@2/intrinsic-body-end-to-activation-resume");
                    var returning=PerformSequenceAssembler.jump("activation-next",move.header().id(),resume,continuation,unit,activation);
                    sequences.add(new Sequence(here,transfers,returning,assign.header().origin()));
                }
            } else if(precise && fact instanceof SpInput.GobackFact g) {
                term=GobackHandler.translate(g,unit,ids,origins,uncertainties);link(fact.header().id(),term,label,statements,items);
            } else if(fact instanceof SpInput.MoveFact m && m.target().role()==SpInput.OperandRole.WRITE
                    && m.target().wholeItemAccess().filter(w->data.index().containsKey(w.data())).isPresent()) {
                var omitted=ConservativeMove.translate(m,data,unit,ids,origins,operands,uncertainties);
                instructions.add(omitted);link(m.header().id(),omitted,label,statements,items);
                term=destination!=null ? PerformSequenceAssembler.jump("conservative-move-next",m.header().id(),destination,omitted.header().origin(),unit,ids)
                    : opaque(fact,null,data,unit,ids,origins,uncertainties,operands,false);
            } else if(fact instanceof SpInput.OtherStatement o&&o.effects().filter(e->e.proof()==SpInput.EffectProof.NO_OP).isPresent()) {
                var origin=origins.source("statement",fact.header().id().handle(),fact.header().provenance());
                term=destination!=null?PerformSequenceAssembler.jump("no-op-next",fact.header().id(),destination,origin,unit,ids)
                    :opaque(fact,null,data,unit,ids,origins,uncertainties,operands,false);
                link(fact.header().id(),term,label,statements,items);
            } else {
                term=opaque(fact,fact instanceof SpInput.IfFact || fact instanceof SpInput.PerformFact || fact instanceof SpInput.ProcedurePerformFact || fact instanceof SpInput.EvaluateFact ? null : destination,data,unit,ids,origins,uncertainties,operands,!(fact instanceof SpInput.GoToFact));
                link(fact.header().id(),term,label,statements,items);
            }
            sequences.add(new Sequence(label,instructions,term,term.header().origin()));
        }
    }
    private static Operations.Opaque opaque(SpInput.StatementFact fact,LabelId next,ScalarDataTranslator.Result data,UnitId unit,
            LocalIds ids,SourceOrigins origins,List<Evidence.Uncertainty> uncertainties,List<LoweringResult.OperandLink> operands,boolean unknownEffects) {
        var origin=origins.source("statement",fact.header().id().handle(),fact.header().provenance());
        var id=new OperationId(unit,ids.id("operation","opaque",unit.localId(),fact.header().id().handle()));
        var gap=new UncertaintyId(unit.publication(),ids.id("uncertainty","unsupported-region",id.localId(),"semantics"));
        var known=OpaqueOperands.translate(fact,id,data,ids,origins,operands,uncertainties);
        var scope=new Scopes.EntityScope(List.of(id));
        var code=fact instanceof SpInput.ProcedurePerformFact && ids.containsActivation(fact.header().id().handle()) && unknownEffects?"RECURSIVE_PERFORM_NOT_SUPPORTED":fact instanceof SpInput.GoToFact?"GO_TO_TARGET_NOT_PROVEN":!unknownEffects?"NORMAL_CONTINUATION_NOT_PROVEN":fact instanceof SpInput.OtherStatement o?o.gapCode():"PRECISE_SEMANTICS_UNAVAILABLE";
        uncertainties.add(new Evidence.Uncertainty(gap,"cobol-lower:"+code,unknownEffects?List.of(Evidence.Dimension.CONTROL,Evidence.Dimension.EFFECTS,Evidence.Dimension.VALUES,Evidence.Dimension.DEPENDENCIES):List.of(Evidence.Dimension.CONTROL),scope,
            "Source region retains published operands and only proved control",origin));
        var effectGaps=new ArrayList<UncertaintyId>();effectGaps.add(gap);
        if(fact instanceof SpInput.OtherStatement o&&o.effects().filter(e->e.environment()!=SpInput.EnvironmentEffect.NONE).isPresent()) {
            var environment=new UncertaintyId(unit.publication(),ids.id("uncertainty","statement-environment",id.localId(),"effects"));
            uncertainties.add(new Evidence.Uncertainty(environment,"SOURCE_"+o.effects().orElseThrow().environment().name()+"_EFFECT",
                List.of(Evidence.Dimension.EFFECTS,Evidence.Dimension.DEPENDENCIES),scope,"Memory proof does not close environment effects",origin));
            effectGaps.add(environment);
        }
        var open=new Evidence.Claim(scope,Evidence.PrecisionStatus.OPEN,List.of(gap));
        var exact=new Evidence.Claim(scope,Evidence.PrecisionStatus.EXACT,List.of());
        var memory=OpaqueOperands.memory(fact,known,unit.publication());
        boolean memoryOpen=!(memory.otherReads() instanceof Scopes.NoMemory)||!(memory.otherWrites() instanceof Scopes.NoMemory);
        boolean externalEnvironment=fact instanceof SpInput.OtherStatement o && o.effects().isPresent()
            &&(o.effects().orElseThrow().environment()==SpInput.EnvironmentEffect.OUTPUT
                ||o.effects().orElseThrow().environment()==SpInput.EnvironmentEffect.INPUT);
        var header=new Operations.Header(id,origin,Evidence.CoverageStatus.ABSTRACTED,
            new Evidence.Precision(next==null?open:exact,memoryOpen?open:exact,memoryOpen||externalEnvironment?open:exact,open,externalEnvironment?open:exact),effectGaps);
        // AIR has one descriptive identity for an opaque construction; the SP shape is the most specific published identity.
        return new Operations.Opaque(header,fact instanceof SpInput.OtherStatement o?o.observedShape().orElse(o.observedKind()):fact.getClass().getSimpleName(),known.operands(),List.of(),
            new Envelopes.Envelope(memory,
                next==null?new Control.ControlEnvelope(List.of(),new Scopes.WithinControl(new Scopes.LabelsControl(List.of())))
                    :new Control.ControlEnvelope(List.of(new Control.JumpAlternative(next)),Scopes.NoControl.INSTANCE),
                new Envelopes.DependencyEnvelope(List.of(),externalEnvironment?Scopes.AnyResource.INSTANCE:Scopes.NoResources.INSTANCE)));
    }
    static LabelId label(SpInput.StatementId s,UnitId unit,LocalIds ids) {return new LabelId(unit,ids.id("label","partial-sequence",unit.localId(),s.handle()));}
    private static void link(SpInput.StatementId source,Operation op,LabelId label,List<LoweringResult.StatementLink> statements,List<Evidence.CoverageItem> items) {
        var h=op.header();statements.add(new LoweringResult.StatementLink(source,h.id(),label,h.origin()));
        items.add(new Evidence.CoverageItem("sp-partial@1/"+h.id().localId()+"/"+source.handle(),h.origin(),h.coverage(),List.of(h.id(),label),h.uncertainties(),Optional.empty()));
    }
}
