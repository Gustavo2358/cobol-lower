package io.github.gustavo2358.lower.application;

import io.github.gustavo2358.air.model.*;
import io.github.gustavo2358.air.model.Ids.*;
import io.github.gustavo2358.lower.domain.SpInput;
import io.github.gustavo2358.lower.domain.ControlTopology;
import static io.github.gustavo2358.lower.domain.ControlTopology.*;
import java.util.*;

/** SP2.39-only materializer. All source control references and completion
 * bindings come from the same topology used by closure. Fact dispatch below
 * selects value/effect operations, never a source successor. */
final class TopologyProgramAssembler {
    private record Context(LocalIds ids,Binding binding,LabelId completion,String entry) { }
    private final PartialProgramAdmission.Plan plan;
    private final SpInput input;
    private final TopologyBinding topology;
    private final ScalarDataTranslator.Result data;
    private final UnitId unit;
    private final SourceOrigins origins;
    private final List<LoweringResult.StatementLink> links;
    private final List<LoweringResult.OperandLink> operands;
    private final List<Evidence.CoverageItem> items;
    private final List<Evidence.Uncertainty> uncertainties;
    private final FileResourceLowering files;
    private final Map<String,SpInput.StatementFact> facts=new HashMap<>();
    private final List<Sequence> sequences=new ArrayList<>();
    private final Set<LabelId> synthetic=new HashSet<>();
    private final Map<OperationId,Terminator> explained=new HashMap<>();
    private final Map<String,OriginId> proofOrigins=new HashMap<>();
    private final Deque<Context> work=new ArrayDeque<>();
    private final Set<String> occurrenceFrontiers=new HashSet<>();
    private final Set<String> emittedOccurrenceFrontiers=new HashSet<>();
    private LocalIds occurrenceIds;
    private TopologyProgramAssembler(PartialProgramAdmission.Plan plan,ScalarDataTranslator.Result data,UnitId unit,
            SourceOrigins origins,List<LoweringResult.StatementLink> links,List<LoweringResult.OperandLink> operands,
            List<Evidence.CoverageItem> items,List<Evidence.Uncertainty> uncertainties,FileResourceLowering files) {
        this.plan=plan;this.input=plan.admission().input().orElseThrow();this.topology=new TopologyBinding(input.controlTopology().orElseThrow());
        this.data=data;this.unit=unit;this.origins=origins;this.links=links;this.operands=operands;this.items=items;this.uncertainties=uncertainties;this.files=files;
        input.statements().forEach(s->facts.put(s.header().id().handle(),s));
        // A frontier with no licensed continuation cannot depend on a PERFORM
        // resume context. Reuse its source occurrence, never its target spelling.
        for(var fact:input.statements())if(fact instanceof SpInput.CicsFact c
                &&c.command()==SpInput.CicsCommand.XCTL&&plan.precise().contains(c.header().id())
                &&topology.outcomes(c.header().id().handle()).stream().allMatch(o->o.kind()==OutcomeKind.UNKNOWN_LOCAL))
            occurrenceFrontiers.add(c.header().id().handle());
    }
    static PartialProgramAssembler.Assembly assemble(PartialProgramAdmission.Plan plan,ScalarDataTranslator.Result data,UnitId unit,
            LocalIds ids,SourceOrigins origins,List<LoweringResult.StatementLink> links,List<LoweringResult.OperandLink> operands,
            List<Evidence.CoverageItem> items,List<Evidence.Uncertainty> uncertainties,FileResourceLowering files) {
        return new TopologyProgramAssembler(plan,data,unit,origins,links,operands,items,uncertainties,files).assemble(ids);
    }
    private PartialProgramAssembler.Assembly assemble(LocalIds ids) {
        occurrenceIds=ids;
        var entry=facts.get(topology.primaryEntry().orElseThrow().reference()).header().id();
        var entryLabel=label(entry.handle(),ids);work.add(new Context(ids,null,null,entry.handle()));
        while(!work.isEmpty()) {
            var context=work.removeFirst();
            for(var statement:topology.closure(context.entry(),context.binding()).stream().sorted().toList())append(facts.get(statement),context);
        }
        var initial=LogicalTextMove.initial(plan.storage().logical(),data,unit,ids,origins);
        var sourceEntryLabel=entryLabel;
        var origin=sequences.stream().filter(s->s.label().equals(sourceEntryLabel)).findFirst().orElseThrow().origin();
        if(!initial.isEmpty()) {
            var bootstrap=new LabelId(unit,ids.id("label","logical-root-entry",unit.localId(),"entry"));
            origin=initial.getFirst().header().origin();
            sequences.add(new Sequence(bootstrap,initial,PerformSequenceAssembler.jump("logical-root-entry",entry,entryLabel,origin,unit,ids),origin));
            entryLabel=bootstrap;
        }
        return new PartialProgramAssembler.Assembly(files.complete(sequences),entryLabel,origin);
    }
    private LabelId label(String id,LocalIds ids){
        return PartialProgramAssembler.label(facts.get(id).header().id(),unit,occurrenceFrontiers.contains(id)?occurrenceIds:ids);
    }
    private OriginId evidence(String key,List<String> proofs,LocalIds ids) {
        var sources=new LinkedHashSet<OriginId>();
        var todo=new ArrayDeque<String>(proofs);var seen=new HashSet<String>();
        while(!todo.isEmpty()) {var id=todo.removeFirst();if(!seen.add(id))continue;var p=topology.proof(id);
            sources.add(proofOrigins.computeIfAbsent(id,proofKey->origins.derived(ids.id("origin","topology-premise",unit.localId(),proofKey),
                List.of(origins.source("topology-proof",proofKey,p.provenance())),"SP2.39/"+p.kind()+"/"+p.rule())));todo.addAll(p.dependencies());}
        return origins.derived(ids.id("origin","topology-binding",unit.localId(),key),List.copyOf(sources),"control-topology@2.39/context-binding");
    }
    private LabelId destination(Target target,Context context,SpInput.StatementFact fact,String role) {
        var r=topology.resolve(target,context.binding());
        if(r.kind()==TargetKind.OCCURRENCE)return label(r.reference(),context.ids());
        if(r.kind()==TargetKind.COMPLETE)return Objects.requireNonNull(context.completion());
        var at=new LabelId(unit,context.ids().id("label","topology-boundary",fact.header().id().handle(),role));
        if(synthetic.add(at)) {
            var origin=evidence(fact.header().id().handle()+"/"+role,r.proofs(),context.ids());
            var op=new OperationId(unit,context.ids().id("operation","topology-boundary",fact.header().id().handle(),role));
            Terminator term;
            if(r.kind()==TargetKind.PROGRAM_RETURN)term=new Operations.Return(new Operations.Header(op,origin,Evidence.CoverageStatus.MODELED,ScalarEvidence.assign(op),List.of()),List.of());
            else {
                term=frontier(fact,context.ids().activation("frontier:"+role),"TOPOLOGY_CONTROL_UNAVAILABLE",r.reference());
                op=term.header().id();origin=term.header().origin();
            }
            sequences.add(new Sequence(at,List.of(),term,origin));PartialProgramAssembler.link(fact.header().id(),term,at,links,items);
        }
        return at;
    }
    private LabelId outcome(String role,Context context,SpInput.StatementFact fact) {
        return destination(topology.outcome(fact.header().id().handle(),role).orElseThrow().target(),context,fact,role);
    }
    private void append(SpInput.StatementFact fact,Context suppliedContext) {
        boolean shared=occurrenceFrontiers.contains(fact.header().id().handle());
        if(shared&&!emittedOccurrenceFrontiers.add(fact.header().id().handle()))return;
        var context=shared?new Context(occurrenceIds,null,null,fact.header().id().handle()):suppliedContext;
        var ids=context.ids();var label=label(fact.header().id().handle(),ids);files.sourceEntry(label);
        var published=topology.outcomes(fact.header().id().handle());
        var invoke=published.stream().filter(o->o.kind()==OutcomeKind.LOCAL_INVOKE).findFirst();
        var instructions=new ArrayList<Instruction>();Terminator term;
        boolean precise=plan.precise().contains(fact.header().id());
        if(invoke.isPresent()) {
            var call=topology.binding(invoke.get().binding());
            if(ids.containsActivation(call.id())) {
                term=frontier(fact,ids,"TOPOLOGY_RECURSIVE_ACTIVATION_UNAVAILABLE",call.region());
            } else {
                var childIds=ids.activation(call.id());
                var entry=topology.entry(call);
                var resume=destination(call.resume(),context,fact,"resume");
                var phases=new HashMap<String,LabelId>();phases.put("RESUME",resume);
                for(var phase:call.phases())phases.put(phase.id(),new LabelId(unit,ids.id("label","topology-phase",call.id(),phase.id())));
                if(entry.kind()==TargetKind.OCCURRENCE) {
                    phases.put("BODY",label(entry.reference(),childIds));
                    work.addLast(new Context(childIds,call,phases.get(call.completionPhase()),entry.reference()));
                } else if(entry.kind()==TargetKind.COMPLETE)phases.put("BODY",Objects.requireNonNull(phases.get(call.completionPhase())));
                else throw new IllegalArgumentException("unadmitted invocation entry bound");
                for(var phase:call.phases())phase(fact,phase,phases,context);
                var origin=evidence(call.id(),call.proofs(),ids);
                term=PerformSequenceAssembler.jump("topology-invoke",fact.header().id(),phases.get(call.entryPhase()),origin,unit,ids);
            }
        } else if(published.stream().allMatch(o->o.kind()==OutcomeKind.UNKNOWN_LOCAL)) {
            term=frontier(fact,ids,"TOPOLOGY_CONTROL_UNAVAILABLE",published.getFirst().target().reference());
        } else {
            var normal=topology.outcome(fact.header().id().handle(),"normal").map(o->destination(o.target(),context,fact,"normal")).orElse(null);
            if(files.handles(fact)) {
                var chain=files.sequences(fact,normal,ids,role->outcome(role,context,fact)).stream()
                    .map(s->new Sequence(s.label(),s.instructions(),explain(s.terminator(),fact,context),s.origin())).toList();
                instructions.addAll(chain.getFirst().instructions());term=chain.getFirst().terminator();
                for(int i=1;i<chain.size();i++)sequences.add(chain.get(i));
                for(int i=1;i<chain.size();i++){var s=chain.get(i);for(var op:s.instructions())PartialProgramAssembler.link(fact.header().id(),op,s.label(),links,items);PartialProgramAssembler.link(fact.header().id(),s.terminator(),s.label(),links,items);}
            } else if(fact instanceof SpInput.IfFact f&&precise) {
                term=IfSequenceAssembler.branch(f,outcome("then",context,fact),outcome("else",context,fact),data,unit,ids,origins,operands,items,uncertainties);
            } else if(fact instanceof SpInput.EvaluateFact e&&precise) {
                var entries=e.arms().stream().map(a->outcome("when-"+a.ordinal(),context,fact)).toList();
                var chain=EvaluateLowerer.chain(e,entries,outcome("other",context,fact),data,unit,ids,origins,operands,items,uncertainties).stream()
                    .map(s->new Sequence(s.label(),s.instructions(),explain(s.terminator(),fact,context),s.origin())).toList();
                term=chain.getFirst().terminator();for(int i=1;i<chain.size();i++){sequences.add(chain.get(i));PartialProgramAssembler.link(fact.header().id(),chain.get(i).terminator(),chain.get(i).label(),links,items);}
            } else if(published.size()==1&&published.getFirst().kind()==OutcomeKind.PROGRAM_RETURN) {
                var origin=evidence(published.getFirst().id(),published.getFirst().proofs(),ids);
                var op=new OperationId(unit,ids.id("operation","topology-return",unit.localId(),fact.header().id().handle()));
                term=new Operations.Return(new Operations.Header(op,origin,Evidence.CoverageStatus.MODELED,ScalarEvidence.assign(op),List.of()),List.of());
            } else if(precise&&fact instanceof SpInput.MoveFact m&&normal!=null) {
                instructions.addAll(RegionalMoveHandler.sequence(m,plan.fitted().contains(m.header().id()),data,plan.storage(),unit,ids,origins,operands,items,uncertainties));
                term=PerformSequenceAssembler.jump("topology-normal",fact.header().id(),normal,evidence(published.getFirst().id(),published.getFirst().proofs(),ids),unit,ids);
            } else if(precise&&fact instanceof SpInput.CicsFileFact c&&normal!=null)term=CicsFileInvokeHandler.translate(c,data,normal,unit,ids,origins,operands,items,uncertainties);
            else if(precise&&fact instanceof SpInput.CicsFact c&&normal!=null)term=CicsInvokeHandler.translate(c,data,normal,unit,ids,origins,operands,items,uncertainties);
            else if(precise&&fact instanceof SpInput.CallFact c&&normal!=null)term=InvokeHandler.translate(c,data.index(),normal,evidence(published.getFirst().id(),published.getFirst().proofs(),ids),unit,ids,origins,operands,items,uncertainties,plan.storage());
            else if(fact instanceof SpInput.OtherStatement o&&o.effects().filter(e->e.proof()==SpInput.EffectProof.NO_OP).isPresent()&&published.size()==1) {
                term=PerformSequenceAssembler.jump("topology-no-op",fact.header().id(),destination(published.getFirst().target(),context,fact,"only"),evidence(published.getFirst().id(),published.getFirst().proofs(),ids),unit,ids);
            } else if(published.size()==1&&published.getFirst().kind()==OutcomeKind.EXPLICIT_TRANSFER) {
                term=PerformSequenceAssembler.jump("topology-transfer",fact.header().id(),destination(published.getFirst().target(),context,fact,"transfer"),evidence(published.getFirst().id(),published.getFirst().proofs(),ids),unit,ids);
            } else {
                var opaque=PartialProgramAssembler.opaque(fact,normal,data,unit,ids,origins,uncertainties,operands,true,"TOPOLOGY_OPERATION_VALUES_EFFECTS_PARTIAL");
                var targets=published.stream().map(o->destination(o.target(),context,fact,o.role())).distinct().<Control.ControlAlternative>map(Control.JumpAlternative::new).toList();
                term=new Operations.Opaque(opaque.header(),opaque.observedKind(),opaque.knownOperands(),opaque.valueResults(),new Envelopes.Envelope(opaque.envelope().memory(),new Control.ControlEnvelope(targets,Scopes.NoControl.INSTANCE),opaque.envelope().dependencies()));
            }
        }
        term=explain(term,fact,context);
        for(var op:instructions)PartialProgramAssembler.link(fact.header().id(),op,label,links,items);
        PartialProgramAssembler.link(fact.header().id(),term,label,links,items);
        sequences.add(new Sequence(label,instructions,term,term.header().origin()));
    }
    /** Unavailable source control is a partial-projection frontier. The empty
     * open set enumerates no licensed target in this model; it is neither a
     * return/diverge claim nor an upper-bound assertion about the entire source.
     * AIR 00.5, 05.6 and 06.1/3.2 require source incompleteness to remain explicit. */
    private Terminator frontier(SpInput.StatementFact fact,LocalIds ids,String code,String bound) {
        // Payload admission is independent from outgoing-control completeness.
        // The existing CICS translator owns target/signature/effects; topology
        // still owns the control below, including the empty open frontier.
        boolean typed=occurrenceFrontiers.contains(fact.header().id().handle());
        Terminator payload=typed
            ?CicsInvokeHandler.translate((SpInput.CicsFact)fact,data,null,unit,ids,origins,operands,items,uncertainties)
            :PartialProgramAssembler.opaque(fact,null,data,unit,ids,origins,uncertainties,operands,true,code);
        var h=payload.header();var precision=h.precision();var scope=new Scopes.EntityScope(List.of(h.id()));
        var reason=new UncertaintyId(unit.publication(),ids.id("uncertainty","topology-region-unavailable",h.id().localId(),bound));
        var evidence=topology.outcomes(fact.header().id().handle()).stream().flatMap(o->o.proofs().stream()).distinct().toList();
        var origin=evidence("unavailable/"+bound,evidence,ids);
        uncertainties.add(new Evidence.Uncertainty(reason,"CONTROL_TOPOLOGY_REGION_UNAVAILABLE",List.of(Evidence.Dimension.CONTROL),scope,
            "Source control remains unavailable at "+bound+"; no source impossibility or completion is claimed",origin));
        var reasons=new ArrayList<>(h.uncertainties());reasons.add(reason);
        var unavailable=new Evidence.Claim(scope,Evidence.PrecisionStatus.UNAVAILABLE,List.of(reason));
        var header=new Operations.Header(h.id(),typed?h.origin():origin,typed?h.coverage():Evidence.CoverageStatus.UNSUPPORTED,
            new Evidence.Precision(unavailable,precision.storage(),precision.effects(),precision.values(),precision.dependencies()),reasons);
        var remainder=new Scopes.WithinControl(new Scopes.LabelsControl(List.of()));
        if(payload instanceof Operations.Invoke invoke)
            return new Operations.Invoke(header,invoke.action(),invoke.target(),invoke.arguments(),invoke.results(),invoke.signature(),
                invoke.effectOperands(),invoke.effectBound(),new Control.InvocationOutcomes(List.of(),remainder),invoke.contract());
        var opaque=(Operations.Opaque)payload;
        return new Operations.Opaque(header,opaque.observedKind(),opaque.knownOperands(),opaque.valueResults(),
            new Envelopes.Envelope(opaque.envelope().memory(),new Control.ControlEnvelope(List.of(),remainder),opaque.envelope().dependencies()));
    }
    private Terminator explain(Terminator term,SpInput.StatementFact fact,Context context) {
        var h=term.header();if(explained.containsKey(h.id()))return explained.get(h.id());
        var proofs=new LinkedHashSet<String>();
        for(var o:topology.outcomes(fact.header().id().handle())) {
            proofs.addAll(o.proofs());proofs.addAll(topology.resolve(o.target(),context.binding()).proofs());
            if(o.kind()==OutcomeKind.LOCAL_INVOKE)proofs.addAll(topology.binding(o.binding()).proofs());
        }
        if(context.binding()!=null)proofs.addAll(context.binding().proofs());
        var topologyOrigin=evidence("operation/"+h.id().localId(),List.copyOf(proofs),context.ids());
        var origin=origins.derived(context.ids().id("origin","topology-operation",unit.localId(),h.id().localId()),List.of(h.origin(),topologyOrigin),"control-topology@2.39/operation-and-bound-outcomes");
        var header=new Operations.Header(h.id(),origin,h.coverage(),h.precision(),h.uncertainties());
        Terminator result=switch(term) {
            case Operations.Jump t -> new Operations.Jump(header,t.destination());
            case Operations.Branch t -> new Operations.Branch(header,t.predicate(),t.trueDestination(),t.falseDestination());
            case Operations.Opaque t -> new Operations.Opaque(header,t.observedKind(),t.knownOperands(),t.valueResults(),t.envelope());
            case Operations.Return t -> new Operations.Return(header,t.values());
            case Operations.Invoke t -> new Operations.Invoke(header,t.action(),t.target(),t.arguments(),t.results(),t.signature(),t.effectOperands(),t.effectBound(),t.outcomes(),t.contract());
            default -> throw new IllegalArgumentException("unsupported topology operation adapter: "+term.kind());
        };
        explained.put(h.id(),result);return result;
    }
    private void phase(SpInput.StatementFact fact,Phase phase,Map<String,LabelId> labels,Context context) {
        var destinations=new HashMap<String,LabelId>();phase.edges().forEach(e->destinations.put(e.role(),labels.get(e.target())));
        Terminator term;
        if(!(fact instanceof SpInput.ProcedurePerformFact)) {
            if(phase.kind()==PhaseKind.PREDICATE) {
                var op=new OperationId(unit,context.ids().id("operation","topology-phase",fact.header().id().handle(),phase.id()));
                var predicate=IfPredicate.translateReads(fact.header().id(),fact.header().provenance(),List.of(),List.of(),"topology-phase-"+phase.id(),
                    "control-topology@2.39/predicate-values-unavailable",op,data,context.ids(),origins,operands,items,uncertainties);
                var origin=evidence("phase/"+fact.header().id().handle()+"/"+phase.id(),phase.proofs(),context.ids());
                var exact=new Evidence.Claim(new Scopes.EntityScope(List.of(op)),Evidence.PrecisionStatus.EXACT,List.of());
                var open=new Evidence.Claim(new Scopes.EntityScope(List.of(op)),Evidence.PrecisionStatus.OPEN,List.of(predicate.reason()));
                term=new Operations.Branch(new Operations.Header(op,origin,Evidence.CoverageStatus.ABSTRACTED,new Evidence.Precision(exact,open,open,open,exact),List.of(predicate.reason())),predicate,destinations.get("true"),destinations.get("false"));
            } else term=PartialProgramAssembler.opaque(fact,destinations.get("next"),data,unit,context.ids().activation("phase:"+phase.id()),origins,uncertainties,operands,true,"TOPOLOGY_PHASE_EFFECTS_PARTIAL");
        } else {var p=(SpInput.ProcedurePerformFact)fact;term=switch(phase.operation()) {
            case "UNTIL_PREDICATE" -> PerformLoopAssembler.decision(p,destinations.get("false"),destinations.get("true"),data,unit,context.ids(),origins,operands,items,uncertainties);
            case "COUNT_ENTRY","COUNT_REPEAT" -> PerformLoopAssembler.countDecision(p,phase.operation().equals("COUNT_ENTRY"),destinations.get("false"),destinations.get("true"),data,unit,context.ids(),origins,operands,items,uncertainties);
            case "VARY_INITIAL","VARY_UPDATE" -> PerformVaryingEffects.effect(p,phase.operation().equals("VARY_INITIAL"),destinations.get("next"),data,unit,context.ids(),origins,operands,uncertainties);
            default -> throw new IllegalArgumentException("unsupported topology phase operation");
        };
        }
        term=explain(term,fact,context);
        var label=labels.get(phase.id());sequences.add(new Sequence(label,List.of(),term,term.header().origin()));PartialProgramAssembler.link(fact.header().id(),term,label,links,items);
    }
}
