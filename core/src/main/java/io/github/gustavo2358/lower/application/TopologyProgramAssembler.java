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
    private enum FrameKind { ROOT, ACTIVATION, BODY }
    private record BodyKey(String entry,String endpoint) {
        String identity(){return entry.length()+":"+entry+"/"+endpoint.length()+":"+endpoint;}
    }
    private record Context(LocalIds base,Binding binding,String entry,
            HandlerStateAnalysis.Support support,boolean handler,FrameKind frame) {
        LocalIds ids(){return support==null?base:base.activation("handler-state:"+CicsExecutionState.key(support));}
        Context at(String location){return new Context(base,binding,location,support,handler,frame);}
        Context state(HandlerStateAnalysis.Support next){return new Context(base,binding,entry,next,handler,frame);}
    }
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
    private final ConditionNameLowerer conditionNames;
    private final Map<String,SpInput.StatementFact> facts=new HashMap<>();
    private final Map<SpInput.DataId,SpInput.DataFact> declarations=new HashMap<>();
    private final List<Sequence> sequences=new ArrayList<>();
    private final Set<LabelId> emitted=new HashSet<>();
    private final Set<LabelId> synthetic=new HashSet<>();
    private final Map<OperationId,Terminator> explained=new HashMap<>();
    private final Map<String,OriginId> proofOrigins=new HashMap<>();
    private final Deque<Context> work=new ArrayDeque<>();
    private final Set<String> occurrenceFrontiers=new HashSet<>();
    private final Set<String> emittedOccurrenceFrontiers=new HashSet<>();
    private LocalIds occurrenceIds;
    private final CicsExecutionState cicsState;
    private final Map<BodyKey,List<String>> compactProofs=new HashMap<>();
    private record ReturnSignal(HandlerStateAnalysis.Support support,Target escape,String occurrence) { }
    private record Route(OperationId operation,ReturnSignal signal) { }
    private final Map<String,Set<ReturnSignal>> returnStates=new HashMap<>();
    private final Map<String,List<PendingReturn>> returnSubscribers=new HashMap<>();
    private final Deque<Route> returnWork=new ArrayDeque<>();
    private final Set<Route> scheduledReturns=new HashSet<>();
    private record PendingReturn(OperationId operation,String group,java.util.function.Function<ReturnSignal,LabelId> destination,
            Map<String,LabelId> routes) { }
    private final Map<OperationId,PendingReturn> pendingReturns=new LinkedHashMap<>();
    private TopologyProgramAssembler(PartialProgramAdmission.Plan plan,ScalarDataTranslator.Result data,UnitId unit,
            SourceOrigins origins,List<LoweringResult.StatementLink> links,List<LoweringResult.OperandLink> operands,
            List<Evidence.CoverageItem> items,List<Evidence.Uncertainty> uncertainties,FileResourceLowering files) {
        this.plan=plan;this.input=plan.admission().input().orElseThrow();this.topology=new TopologyBinding(input.controlTopology().orElseThrow());
        this.data=data;this.unit=unit;this.origins=origins;this.links=links;this.operands=operands;this.items=items;this.uncertainties=uncertainties;this.files=files;
        this.conditionNames=new ConditionNameLowerer(input,data,operands);
        cicsState=input.controlTopology().orElseThrow().exceptionalEvents().isEmpty()&&input.controlTopology().orElseThrow().conditionEvents().isEmpty()?null:
            new CicsExecutionState(input,plan.admission().handlerState().orElseThrow());
        var grouped=new HashMap<BodyKey,Set<String>>();
        for(var binding:input.controlTopology().orElseThrow().bindings())if(!topology.inline(binding)) {
            var entry=topology.entry(binding);
            if(entry.kind()==TargetKind.OCCURRENCE)grouped.computeIfAbsent(new BodyKey(entry.reference(),binding.endpoint()),k->new TreeSet<>()).addAll(binding.proofs());
        }
        grouped.forEach((key,value)->compactProofs.put(key,List.copyOf(value)));
        input.statements().forEach(s->facts.put(s.header().id().handle(),s));
        input.dataDeclarations().forEach(d->declarations.put(d.id(),d));
        // A frontier with no licensed continuation cannot depend on a PERFORM
        // resume context. Reuse its source occurrence, never its target spelling.
        for(var fact:input.statements())if(cicsState==null&&typedCicsFrontier(fact))
            occurrenceFrontiers.add(fact.header().id().handle());
    }
    static PartialProgramAssembler.Assembly assemble(PartialProgramAdmission.Plan plan,ScalarDataTranslator.Result data,UnitId unit,
            LocalIds ids,SourceOrigins origins,List<LoweringResult.StatementLink> links,List<LoweringResult.OperandLink> operands,
            List<Evidence.CoverageItem> items,List<Evidence.Uncertainty> uncertainties,FileResourceLowering files) {
        return new TopologyProgramAssembler(plan,data,unit,origins,links,operands,items,uncertainties,files).assemble(ids);
    }
    private PartialProgramAssembler.Assembly assemble(LocalIds ids) {
        occurrenceIds=ids;
        var roots=new LinkedHashMap<SpInput.EntryId,Context>();
        for(var source:input.entryInventory().entries()) {
            var target=source.role()==SpInput.EntryRole.PRIMARY?topology.primaryEntry():input.controlTopology().orElseThrow().entryPoints().stream().filter(e->e.entry().equals(source.id().handle())).map(e->topology.resolve(e.target(),null)).findFirst();
            if(target.isEmpty()||target.orElseThrow().kind()!=TargetKind.OCCURRENCE)continue;
            var activation=source.role()==SpInput.EntryRole.PRIMARY?ids:ids.activation("external-entry:"+source.id().handle());
            var root=new Context(activation,null,target.orElseThrow().reference(),
                cicsState==null?null:CicsExecutionState.initial(),false,FrameKind.ROOT);
            roots.put(source.id(),root);work.addLast(root);
        }
        while(!work.isEmpty()||!returnWork.isEmpty()) {
            if(!returnWork.isEmpty()) {
                var route=returnWork.removeFirst();addReturnRoute(pendingReturns.get(route.operation()),route.signal());continue;
            }
            var context=work.removeFirst();
            if(context.entry().startsWith("PHASE/"))statePhase(context);
            else append(facts.get(context.entry()),context);
        }
        for(int i=0;i<sequences.size();i++) {
            var sequence=sequences.get(i);var pending=pendingReturns.get(sequence.terminator().header().id());
            if(pending==null)continue;
            var call=(Operations.LocalInvoke)sequence.terminator();
            var routes=pending.routes().entrySet().stream().sorted(Map.Entry.comparingByKey()).map(e->new Operations.ResumeRoute(e.getKey(),e.getValue())).toList();
            var term=new Operations.LocalInvoke(call.header(),call.entry(),call.completionPorts(),call.resume(),call.fallback(),call.reentryGuard(),routes);
            sequences.set(i,new Sequence(sequence.label(),sequence.instructions(),term,sequence.origin()));
        }
        var entries=new LinkedHashMap<SpInput.EntryId,PartialProgramAssembler.EntryAssembly>();
        for(var root:roots.entrySet()) {
            var context=root.getValue();var activation=context.base();var entry=facts.get(context.entry()).header().id();
            var initial=LogicalTextMove.initial(plan.storage().logical(),data,unit,activation,origins);
            var entryLabel=label(entry.handle(),context.ids());var sourceEntryLabel=entryLabel;
            var origin=sequences.stream().filter(s->s.label().equals(sourceEntryLabel)).findFirst().orElseThrow().origin();
            if(!initial.isEmpty()) {
                var bootstrap=new LabelId(unit,activation.id("label","logical-root-entry",unit.localId(),"entry"));
                origin=initial.getFirst().header().origin();
                sequences.add(new Sequence(bootstrap,initial,PerformSequenceAssembler.jump("logical-root-entry",entry,entryLabel,origin,unit,activation),origin));
                entryLabel=bootstrap;
            }
            entries.put(root.getKey(),new PartialProgramAssembler.EntryAssembly(entryLabel,origin));
        }
        var primary=input.entryInventory().entries().stream().filter(e->e.role()==SpInput.EntryRole.PRIMARY).findFirst().orElseThrow();
        var first=entries.get(primary.id());
        return new PartialProgramAssembler.Assembly(files.complete(sequences),first.label(),first.origin(),entries);
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
        return boundDestination(target,context.support()==null?context:context.state(cicsState.after(fact,context.support())),fact,role);
    }
    private LabelId boundDestination(Target target,Context context,SpInput.StatementFact fact,String role) {
        if(target.kind()==TargetKind.ESCAPE)return compactEscape(target,context,fact,role);
        var r=topology.resolve(target,context.binding(),context.handler());
        if(r.kind()==TargetKind.OCCURRENCE) {
            work.addLast(context.at(r.reference()));
            return label(r.reference(),context.ids());
        }
        if(r.kind()==TargetKind.FILE_POINT)return FileTopologyLowering.label(unit,context.ids(),r.reference());
        if(r.kind()==TargetKind.COMPLETE&&context.frame()==FrameKind.BODY)return compactBodyResume(context,fact);
        if(r.kind()==TargetKind.COMPLETE)return statePhaseLabel(context,context.binding().completionPhase());
        var at=new LabelId(unit,context.ids().id("label","topology-boundary",fact.header().id().handle(),role));
        if(synthetic.add(at)) {
            var origin=evidence(fact.header().id().handle()+"/"+role,r.proofs(),context.ids());
            var op=new OperationId(unit,context.ids().id("operation","topology-boundary",fact.header().id().handle(),role));
            Terminator term;
            if(r.kind()==TargetKind.PROGRAM_HALT)term=programHalt(op,origin);
            else if(r.kind()==TargetKind.PROGRAM_RETURN)term=new Operations.Return(new Operations.Header(op,origin,Evidence.CoverageStatus.MODELED,ScalarEvidence.assign(op),List.of()),List.of());
            else {
                term=frontier(fact,context.ids().activation("frontier:"+role),"TOPOLOGY_CONTROL_UNAVAILABLE",r.reference());
                op=term.header().id();origin=term.header().origin();
            }
            sequences.add(new Sequence(at,List.of(),term,origin));PartialProgramAssembler.link(fact.header().id(),term,at,links,items);
        }
        return at;
    }
    /** STOP RUN closes the run unit. Finalization effects are not a proof of writes or resource absence.
     * The current AIR JSON profile admits halt alternatives, not the standalone Halt operation. */
    private Operations.Opaque programHalt(OperationId op,OriginId origin) {
        var scope=new Scopes.EntityScope(List.of(op));
        var gap=new UncertaintyId(unit.publication(),op.localId()+"/finalization");
        uncertainties.add(new Evidence.Uncertainty(gap,"STOP_RUN_FINALIZATION_EFFECTS_PARTIAL",List.of(Evidence.Dimension.STORAGE,Evidence.Dimension.EFFECTS,Evidence.Dimension.VALUES,Evidence.Dimension.DEPENDENCIES),scope,"Run-unit finalization, status and implicit resource effects remain unknown",origin));
        var exact=new Evidence.Claim(scope,Evidence.PrecisionStatus.EXACT,List.of());var open=new Evidence.Claim(scope,Evidence.PrecisionStatus.OPEN,List.of(gap));
        var header=new Operations.Header(op,origin,Evidence.CoverageStatus.ABSTRACTED,new Evidence.Precision(exact,open,open,open,open),List.of(gap));
        var memory=new Scopes.WithinMemory(new Scopes.AllMemory(unit.publication(),true));
        return new Operations.Opaque(header,"program-halt",List.of(),List.of(),new Envelopes.Envelope(
            new Envelopes.MemoryEnvelope(List.of(),memory,List.of(),memory,List.of()),
            new Control.ControlEnvelope(List.of(Control.HaltAlternative.INSTANCE),Scopes.NoControl.INSTANCE),
            new Envelopes.DependencyEnvelope(List.of(),Scopes.AnyResource.INSTANCE)));
    }
    private LabelId outcome(String role,Context context,SpInput.StatementFact fact) {
        return destination(topology.outcome(fact.header().id().handle(),role).orElseThrow().target(),context,fact,role);
    }
    /** Source inventory scheduling is not an executable continuation through an opaque frontier. */
    private void scheduleSourceOccurrences(SpInput.StatementFact fact,Context context) {
        var targets=new ArrayDeque<Target>();var points=new HashSet<String>();
        for(var outcome:topology.outcomes(fact.header().id().handle()))
            targets.add(outcome.kind()==OutcomeKind.LOCAL_INVOKE?topology.binding(outcome.binding()).resume():outcome.target());
        while(!targets.isEmpty()) {
            var target=topology.resolve(targets.removeFirst(),context.binding());
            if(target.kind()==TargetKind.OCCURRENCE)work.addLast(context.at(target.reference()));
            else if(target.kind()==TargetKind.FILE_POINT&&points.add(target.reference()))targets.addAll(topology.filePoint(target.reference()).targets());
        }
    }
    private void append(SpInput.StatementFact fact,Context suppliedContext) {
        boolean shared=occurrenceFrontiers.contains(fact.header().id().handle());
        if(shared&&!emittedOccurrenceFrontiers.add(fact.header().id().handle()))return;
        var context=shared?new Context(occurrenceIds,null,fact.header().id().handle(),null,false,FrameKind.ROOT):suppliedContext;
        var ids=context.ids();var label=label(fact.header().id().handle(),ids);
        if(!emitted.add(label))return;
        files.sourceEntry(label);
        if(cicsState==null)scheduleSourceOccurrences(fact,context);
        var published=topology.outcomes(fact.header().id().handle());
        var invoke=published.stream().filter(o->o.kind()==OutcomeKind.LOCAL_INVOKE).findFirst();
        var instructions=new ArrayList<Instruction>();Terminator term;
        boolean precise=plan.precise().contains(fact.header().id());
        if(cicsState!=null&&fact instanceof SpInput.CicsAbendFact&&!cicsState.events(fact.header().id().handle()).isEmpty()) {
            term=PartialProgramAssembler.opaque(fact,null,data,unit,ids,origins,uncertainties,operands,true,"CICS_ABEND_RUNTIME_EFFECTS_PARTIAL");
        } else if(NonExecutableCapability.of(fact,plan.storage()).isPresent()
                &&!(fact instanceof SpInput.CicsCommandFact command&&CicsCommandMemory.controlReady(command,input))) {
            term=frontier(fact,ids,"EXECUTABLE_CAPABILITY_NOT_READY",fact.header().id().handle());
        } else if(invoke.isPresent()) {
            var call=topology.binding(invoke.get().binding());
            term=compactInvoke(fact,call,context);
        } else if(context.support()!=null&&fact instanceof SpInput.CicsFact c&&c.command()==SpInput.CicsCommand.XCTL
                &&published.stream().allMatch(o->o.kind()==OutcomeKind.UNKNOWN_LOCAL)&&(!cicsState.events(fact.header().id().handle()).isEmpty()||!cicsState.conditions.events(fact.header().id().handle()).isEmpty())) {
            term=CicsInvokeHandler.translate(c,data,null,unit,ids,origins,operands,items,uncertainties);
        } else if(published.stream().allMatch(o->o.kind()==OutcomeKind.UNKNOWN_LOCAL)) {
            term=frontier(fact,ids,"TOPOLOGY_CONTROL_UNAVAILABLE",published.getFirst().target().reference());
        } else {
            var normal=topology.outcome(fact.header().id().handle(),"normal").map(o->destination(o.target(),context,fact,"normal")).orElse(null);
            if(fact instanceof SpInput.CicsCommandFact command&&CicsCommandMemory.controlReady(command,input)) {
                var targets=published.stream().map(o->destination(o.target(),context,fact,o.role())).distinct().<Control.ControlAlternative>map(Control.JumpAlternative::new).toList();
                term=CicsCommandMemory.translate(command,plan.storage(),data,unit,ids,origins,operands,uncertainties,targets);
            } else if(fact instanceof SpInput.CicsHandlerFact handler&&handler.registrationEffects().isPresent()) {
                var targets=published.stream().map(o->destination(o.target(),context,fact,o.role())).distinct().<Control.ControlAlternative>map(Control.JumpAlternative::new).toList();
                term=CicsCommandMemory.registration(handler,data,unit,ids,origins,operands,uncertainties,targets);
            } else if(files.handles(fact)) {
                var chain=files.sequences(fact,normal,ids,role->outcome(role,context,fact),
                    topology.fileFlow(fact.header().id().handle()).map(flow->FileTopologyLowering.layout(flow,fact,unit,ids,
                        target->destination(target,context,fact,"file-point/"+target.reference()),
                        (key,proofs)->evidence(key,proofs,ids),uncertainties))).stream()
                    .map(s->new Sequence(s.label(),s.instructions(),explain(s.terminator(),fact,context),s.origin())).toList();
                instructions.addAll(chain.getFirst().instructions());term=chain.getFirst().terminator();
                for(int i=1;i<chain.size();i++)sequences.add(chain.get(i));
                for(int i=1;i<chain.size();i++){var s=chain.get(i);for(var op:s.instructions())PartialProgramAssembler.link(fact.header().id(),op,s.label(),links,items);PartialProgramAssembler.link(fact.header().id(),s.terminator(),s.label(),links,items);}
            } else if(fact instanceof SpInput.IfFact f&&conditionNames.has(f.header().id().handle(),"IF")) {
                term=conditionNames.branch(f,"IF",outcome("then",context,fact),outcome("else",context,fact),unit,ids,origins,uncertainties);
            } else if(fact instanceof SpInput.IfFact f&&precise) {
                term=IfSequenceAssembler.branch(f,outcome("then",context,fact),outcome("else",context,fact),data,unit,ids,origins,operands,items,uncertainties);
            } else if(fact instanceof SpInput.EvaluateFact e&&precise) {
                var entries=e.arms().stream().map(a->outcome("when-"+a.ordinal(),context,fact)).toList();
                var chain=EvaluateLowerer.chain(e,entries,outcome("other",context,fact),data,unit,ids,origins,operands,items,uncertainties,conditionNames).stream()
                    .map(s->new Sequence(s.label(),s.instructions(),explain(s.terminator(),fact,context),s.origin())).toList();
                term=chain.getFirst().terminator();for(int i=1;i<chain.size();i++){sequences.add(chain.get(i));PartialProgramAssembler.link(fact.header().id(),chain.get(i).terminator(),chain.get(i).label(),links,items);}
            } else if(published.size()==1&&published.getFirst().kind()==OutcomeKind.PROGRAM_HALT) {
                var origin=evidence(published.getFirst().id(),published.getFirst().proofs(),ids);
                var op=new OperationId(unit,ids.id("operation","topology-halt",unit.localId(),fact.header().id().handle()));
                term=programHalt(op,origin);
            } else if(published.size()==1&&published.getFirst().kind()==OutcomeKind.PROGRAM_RETURN) {
                var origin=evidence(published.getFirst().id(),published.getFirst().proofs(),ids);
                var op=new OperationId(unit,ids.id("operation","topology-return",unit.localId(),fact.header().id().handle()));
                term=new Operations.Return(new Operations.Header(op,origin,Evidence.CoverageStatus.MODELED,ScalarEvidence.assign(op),List.of()),List.of());
            } else if(normal!=null&&conditionNames.hasSet(fact.header().id().handle())) {
                instructions.addAll(conditionNames.set(fact,plan.storage().logical(),unit,ids,origins,uncertainties));
                term=PerformSequenceAssembler.jump("topology-normal",fact.header().id(),normal,evidence(published.getFirst().id(),published.getFirst().proofs(),ids),unit,ids);
            } else if(precise&&fact instanceof SpInput.MoveFact m&&normal!=null&&NumericMoveControl.required(m,data)) {
                var chain=NumericMoveControl.sequences(m,label,normal,plan.fitted().contains(m.header().id()),data,plan.storage(),unit,ids,origins,operands,items,uncertainties)
                    .stream().map(s->new Sequence(s.label(),s.instructions(),explain(s.terminator(),fact,context),s.origin())).toList();
                term=chain.getFirst().terminator();
                for(int i=1;i<chain.size();i++) {
                    var sequence=chain.get(i);sequences.add(sequence);
                    for(var operation:sequence.instructions())PartialProgramAssembler.link(fact.header().id(),operation,sequence.label(),links,items);
                    PartialProgramAssembler.link(fact.header().id(),sequence.terminator(),sequence.label(),links,items);
                }
            } else if(precise&&fact instanceof SpInput.MoveFact m&&normal!=null) {
                instructions.addAll(RegionalMoveHandler.sequence(m,plan.fitted().contains(m.header().id()),data,plan.storage(),unit,ids,origins,operands,items,uncertainties));
                term=PerformSequenceAssembler.jump("topology-normal",fact.header().id(),normal,evidence(published.getFirst().id(),published.getFirst().proofs(),ids),unit,ids);
            } else if(precise&&fact instanceof SpInput.CicsFileFact c&&normal!=null)term=CicsFileInvokeHandler.translate(c,data,normal,unit,ids,origins,operands,items,uncertainties);
            else if(fact instanceof SpInput.CicsFact c&&normal!=null&&(precise||context.support()!=null&&!cicsState.conditions.events(fact.header().id().handle()).isEmpty())) {
                term=CicsInvokeHandler.translate(c,data,normal,unit,ids,origins,operands,items,uncertainties);
                if(c.command()==SpInput.CicsCommand.XCTL)term=boundedCicsContinuation(term,normal);
            }
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
        if(context.support()!=null)term=exceptional(conditioned(term,fact,context),fact,context);
        term=explain(term,fact,context);
        for(var op:instructions)PartialProgramAssembler.link(fact.header().id(),op,label,links,items);
        PartialProgramAssembler.link(fact.header().id(),term,label,links,items);
        sequences.add(new Sequence(label,instructions,term,term.header().origin()));
    }
    /** The topology-bound local condition route is independent of successful
     * XCTL transfer. AIR 05.4/6 carries it in the finite open bound, not as a
     * normal return or a fabricated exception/handler. The caller supplies only
     * an authoritative topology destination in this activation context. */
    private static Terminator boundedCicsContinuation(Terminator payload,LabelId continuation) {
        var previous=payload instanceof Operations.Invoke i?i.outcomes().remainder()
            :((Operations.Opaque)payload).envelope().control().remainder();
        var scopes=new ArrayList<Scopes.ControlScope>();
        scopes.add(new Scopes.LabelsControl(List.of(continuation)));
        if(previous instanceof Scopes.WithinControl within)scopes.add(within.scope());
        var remainder=new Scopes.WithinControl(new Scopes.ControlUnion(scopes));
        if(payload instanceof Operations.Invoke i)
            return new Operations.Invoke(i.header(),i.action(),i.target(),i.arguments(),i.results(),i.signature(),
                i.effectOperands(),i.effectBound(),new Control.InvocationOutcomes(i.outcomes().known(),remainder),i.contract());
        var o=(Operations.Opaque)payload;
        return new Operations.Opaque(o.header(),o.observedKind(),o.knownOperands(),o.valueResults(),
            new Envelopes.Envelope(o.envelope().memory(),new Control.ControlEnvelope(o.envelope().control().known(),remainder),o.envelope().dependencies()));
    }
    /** Unavailable source control is a partial-projection frontier. The empty
     * open set enumerates no licensed target in this model; it is neither a
     * return/diverge claim nor an upper-bound assertion about the entire source.
     * AIR 00.5, 05.6 and 06.1/3.2 require source incompleteness to remain explicit. */
    private boolean typedCicsFrontier(SpInput.StatementFact fact) {
        return fact instanceof SpInput.CicsFact c&&c.command()==SpInput.CicsCommand.XCTL
            &&plan.precise().contains(c.header().id())
            &&topology.outcomes(c.header().id().handle()).stream().allMatch(o->o.kind()==OutcomeKind.UNKNOWN_LOCAL);
    }
    /** Source-undefined activation grants neither a completion nor an effect bound. */
    private Operations.Opaque undefinedReentry(SpInput.StatementFact fact,LocalIds ids,String bound) {
        var opaque=(Operations.Opaque)frontier(fact,ids,"LOCAL_REENTRY_SOURCE_UNDEFINED",bound);
        var h=opaque.header();var scope=new Scopes.EntityScope(List.of(h.id()));
        var open=new Evidence.Claim(scope,Evidence.PrecisionStatus.OPEN,h.uncertainties());
        var header=new Operations.Header(h.id(),h.origin(),h.coverage(),
            new Evidence.Precision(h.precision().control(),open,open,open,open),h.uncertainties());
        var memory=new Scopes.WithinMemory(new Scopes.AllMemory(unit.publication(),true));
        return new Operations.Opaque(header,opaque.observedKind(),opaque.knownOperands(),List.of(),
            new Envelopes.Envelope(new Envelopes.MemoryEnvelope(List.of(),memory,List.of(),memory,List.of()),
                opaque.envelope().control(),new Envelopes.DependencyEnvelope(List.of(),Scopes.AnyResource.INSTANCE)));
    }
    private Terminator frontier(SpInput.StatementFact fact,LocalIds ids,String code,String bound) {
        // Payload admission is independent from outgoing-control completeness.
        // Only an occurrence frontier retains an interaction. A synthetic boundary
        // after a modeled CALL must never invoke it a second time.
        // Existing interaction translators own target/signature/effects;
        // topology still owns the empty open control frontier below.
        boolean call=fact instanceof SpInput.CallFact&&plan.precise().contains(fact.header().id())
            &&topology.outcomes(fact.header().id().handle()).stream().allMatch(o->o.kind()==OutcomeKind.UNKNOWN_LOCAL);
        boolean typed=typedCicsFrontier(fact)||call;
        Terminator payload=call
            ?InvokeHandler.translate((SpInput.CallFact)fact,data.index(),null,
                origins.source("unavailable-call-completion",fact.header().id().handle(),((SpInput.CallFact)fact).normalContinuation().provenance()),
                unit,ids,origins,operands,items,uncertainties,plan.storage())
            :typed?CicsInvokeHandler.translate((SpInput.CicsFact)fact,data,null,unit,ids,origins,operands,items,uncertainties)
            :PartialProgramAssembler.opaque(fact,null,data,unit,ids,origins,uncertainties,operands,true,code);
        var h=payload.header();var precision=h.precision();var scope=new Scopes.EntityScope(List.of(h.id()));
        var reason=new UncertaintyId(unit.publication(),ids.id("uncertainty","topology-region-unavailable",h.id().localId(),bound));
        var evidence=topology.outcomes(fact.header().id().handle()).stream().flatMap(o->o.proofs().stream()).distinct().toList();
        var origin=evidence("unavailable/"+h.id().localId()+"/"+bound,evidence,ids);
        boolean notReady=NonExecutableCapability.of(fact,plan.storage()).isPresent();
        uncertainties.add(new Evidence.Uncertainty(reason,notReady?"EXECUTABLE_CAPABILITY_NOT_READY":"CONTROL_TOPOLOGY_REGION_UNAVAILABLE",List.of(Evidence.Dimension.CONTROL),scope,
            notReady?"Typed "+NonExecutableCapability.of(fact,plan.storage()).orElseThrow().kind()+" retained at "+bound
                +"; executable projection stops here. Source topology is retained in input; no source termination is claimed."
                :"Source control remains unavailable at "+bound+"; no source impossibility or completion is claimed",origin));
        var reasons=new ArrayList<>(h.uncertainties());reasons.add(reason);
        var unavailable=new Evidence.Claim(scope,Evidence.PrecisionStatus.UNAVAILABLE,List.of(reason));
        var header=new Operations.Header(h.id(),typed?h.origin():origin,typed?h.coverage():Evidence.CoverageStatus.UNSUPPORTED,
            new Evidence.Precision(unavailable,precision.storage(),notReady?unavailable:precision.effects(),precision.values(),precision.dependencies()),reasons);
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
        if(context.binding()!=null) {
            proofs.addAll(context.frame()==FrameKind.BODY?compactProofs.get(new BodyKey(topology.entry(context.binding()).reference(),context.binding().endpoint())):context.binding().proofs());
        }
        var topologyOrigin=evidence("operation/"+h.id().localId(),List.copyOf(proofs),context.ids());
        var origin=origins.derived(context.ids().id("origin","topology-operation",unit.localId(),h.id().localId()),List.of(h.origin(),topologyOrigin),"control-topology@2.39/operation-and-bound-outcomes");
        var header=new Operations.Header(h.id(),origin,h.coverage(),h.precision(),h.uncertainties());
        Terminator result=switch(term) {
            case Operations.Jump t -> new Operations.Jump(header,t.destination());
            case Operations.LocalInvoke t -> new Operations.LocalInvoke(header,t.entry(),t.completionPorts(),t.resume(),t.fallback(),t.reentryGuard(),t.resumeRoutes());
            case Operations.LocalResume t -> new Operations.LocalResume(header,t.fallback(),t.resumeKey());
            case Operations.LocalUnwind t -> new Operations.LocalUnwind(header,t.count(),t.destination(),t.fallback(),t.all());
            case Operations.Branch t -> new Operations.Branch(header,t.predicate(),t.trueDestination(),t.falseDestination());
            case Operations.Opaque t -> new Operations.Opaque(header,t.observedKind(),t.knownOperands(),t.valueResults(),t.envelope());
            case Operations.Return t -> new Operations.Return(header,t.values());
            case Operations.Invoke t -> new Operations.Invoke(header,t.action(),t.target(),t.arguments(),t.results(),t.signature(),t.effectOperands(),t.effectBound(),t.outcomes(),t.contract());
            default -> throw new IllegalArgumentException("unsupported topology operation adapter: "+term.kind());
        };
        explained.put(h.id(),result);return result;
    }
    /** Every written binding has one activation representation, including inline calls through GO TO. */
    private Terminator compactInvoke(SpInput.StatementFact fact,Binding call,Context parent) {
        var base=occurrenceIds.activation("compact-perform:"+call.id());
        if(parent.handler())base=base.activation("handler-body");
        var child=new Context(base,call,"",parent.support(),parent.handler(),FrameKind.ACTIVATION);
        var entry=statePhaseLabel(child,call.entryPhase());
        var resume=boundDestination(call.resume(),parent,fact,"resume");
        var origin=evidence(call.id(),call.proofs(),parent.ids());
        var op=new OperationId(unit,parent.ids().id("operation","topology-invoke",fact.header().id().handle(),"jump"));
        var reject=call.reentryPolicy()==ReentryPolicy.SOURCE_UNDEFINED?reentryDestination(fact,call):compactUnknownReentry(fact,call);
        var term=new Operations.LocalInvoke(new Operations.Header(op,origin,Evidence.CoverageStatus.MODELED,ScalarEvidence.assign(op),List.of()),
            entry,List.of(),resume,LocalActivationFrames.fallback(unit),Optional.of(new Operations.ReentryGuard(occurrenceIds.id("activation","local-binding",unit.localId(),call.id()),reject)));
        pendingReturn(term,returnGroup(child,"activation"),signal->{
            var resumed=parent.state(signal.support());
            if(signal.escape()==null)return boundDestination(call.resume(),resumed,fact,"resume");
            var escaping=facts.get(signal.occurrence());var scope=topology.region(signal.escape().reference());
            if(scope.kind()==RegionKind.INLINE_BODY&&call.endpoint().equals(scope.boundary()))
                return boundDestination(call.resume(),resumed,escaping,"inline-escape-return/"+call.id());
            return compactEscape(signal.escape(),resumed,escaping,"propagated-escape/"+scope.id());
        });
        return term;
    }
    private LabelId compactUnknownReentry(SpInput.StatementFact fact,Binding call) {
        var ids=occurrenceIds.activation("reentry-guard:"+call.id());
        var at=new LabelId(unit,ids.id("label","reentry-frontier",call.id(),"unavailable"));
        if(synthetic.add(at)) {
            var term=frontier(fact,ids,"TOPOLOGY_RECURSIVE_ACTIVATION_UNAVAILABLE",call.region());
            sequences.add(new Sequence(at,List.of(),term,term.header().origin()));PartialProgramAssembler.link(fact.header().id(),term,at,links,items);
        }
        return at;
    }
    private LabelId compactBody(SpInput.StatementFact fact,Context activation,String entry) {
        var call=activation.binding();var key=new BodyKey(entry,call.endpoint());
        var base=occurrenceIds.activation("compact-body:"+key.identity());
        if(activation.handler())base=base.activation("handler-body");
        var body=new Context(base,call,entry,activation.support(),activation.handler(),FrameKind.BODY);
        work.addLast(body);
        var at=new LabelId(unit,activation.ids().id("label","compact-body-invoke",call.id(),"BODY"));
        if(synthetic.add(at)) {
            var op=new OperationId(unit,activation.ids().id("operation","compact-body-invoke",call.id(),"BODY"));
            var origin=evidence(call.id()+"/BODY-INVOKE",compactProofs.get(key),activation.ids());
            var resume=statePhaseLabel(activation,call.completionPhase());
            var term=new Operations.LocalInvoke(new Operations.Header(op,origin,Evidence.CoverageStatus.MODELED,ScalarEvidence.assign(op),List.of()),
                label(entry,body.ids()),List.of(),resume,LocalActivationFrames.fallback(unit));
            pendingReturn(term,returnGroup(body,"body"),signal->{
                if(signal.escape()!=null)throw new IllegalStateException("external body cannot abandon an inline activation outside that body");
                return statePhaseLabel(activation.state(signal.support()),call.completionPhase());
            });
            sequences.add(new Sequence(at,List.of(),term,origin));PartialProgramAssembler.link(fact.header().id(),term,at,links,items);
        }
        return at;
    }
    private LabelId compactBodyResume(Context body,SpInput.StatementFact fact) {
        var at=new LabelId(unit,body.ids().id("label","compact-body-resume",unit.localId(),"complete"));
        if(synthetic.add(at)) {
            var origin=evidence("compact-body-resume",compactProofs.get(new BodyKey(topology.entry(body.binding()).reference(),body.binding().endpoint())),body.ids());
            var term=compactResume(body,"body",fact,origin);
            sequences.add(new Sequence(at,List.of(),term,origin));PartialProgramAssembler.link(fact.header().id(),term,at,links,items);
        }
        return at;
    }
    private Terminator compactResume(Context context,String kind,SpInput.StatementFact fact,OriginId origin) {
        var op=new OperationId(unit,context.ids().id("operation","compact-resume",unit.localId(),kind));
        var group=returnGroup(context,kind);
        if(context.support()!=null)publishReturn(group,new ReturnSignal(context.support(),null,""));
        return new Operations.LocalResume(new Operations.Header(op,origin,Evidence.CoverageStatus.MODELED,ScalarEvidence.assign(op),List.of()),LocalActivationFrames.fallback(unit),
            Optional.ofNullable(context.support()).map(this::resumeKey));
    }
    private String returnGroup(Context context,String kind){return context.base().id("continuation","compact-"+kind,unit.localId(),kind);}
    private String resumeKey(HandlerStateAnalysis.Support support){return occurrenceIds.id("continuation","cics-state",unit.localId(),CicsExecutionState.key(support));}
    private String returnKey(ReturnSignal signal) {
        if(signal.escape()==null)return resumeKey(Objects.requireNonNull(signal.support()));
        var state=signal.support()==null?"none":CicsExecutionState.key(signal.support());
        return occurrenceIds.id("continuation","inline-escape",signal.escape().reference(),signal.occurrence()+"/"+state);
    }
    private void scheduleReturn(PendingReturn pending,ReturnSignal signal) {
        var route=new Route(pending.operation(),signal);if(scheduledReturns.add(route))returnWork.addLast(route);
    }
    private void publishReturn(String group,ReturnSignal signal) {
        if(returnStates.computeIfAbsent(group,k->new LinkedHashSet<>()).add(signal))
            for(var pending:returnSubscribers.getOrDefault(group,List.of()))scheduleReturn(pending,signal);
    }
    private void pendingReturn(Operations.LocalInvoke invoke,String group,java.util.function.Function<ReturnSignal,LabelId> destination) {
        var pending=new PendingReturn(invoke.header().id(),group,destination,new TreeMap<>());pendingReturns.put(invoke.header().id(),pending);
        returnSubscribers.computeIfAbsent(group,k->new ArrayList<>()).add(pending);
        for(var signal:returnStates.getOrDefault(group,Set.of()))scheduleReturn(pending,signal);
    }
    private void addReturnRoute(PendingReturn pending,ReturnSignal signal) {
        var key=returnKey(signal);if(!pending.routes().containsKey(key))pending.routes().put(key,pending.destination().apply(signal));
    }
    /** Propagate an outward escape through frames; no caller ancestry is materialized. */
    private LabelId compactEscape(Target target,Context context,SpInput.StatementFact fact,String role) {
        var scope=topology.region(target.reference());
        if(context.frame()==FrameKind.ACTIVATION&&topology.inline(context.binding())
                &&(scope.kind()==RegionKind.INLINE_BODY||!topology.contains(context.binding(),scope.id()))) {
            var at=new LabelId(unit,context.ids().id("label","inline-escape-resume",fact.header().id().handle(),scope.id()));
            if(synthetic.add(at)) {
                var origin=evidence("inline-escape/"+fact.header().id().handle()+"/"+scope.id(),target.proofs(),context.ids());
                var op=new OperationId(unit,context.ids().id("operation","inline-escape-resume",fact.header().id().handle(),scope.id()));
                var signal=new ReturnSignal(context.support(),target,fact.header().id().handle());
                var term=new Operations.LocalResume(new Operations.Header(op,origin,Evidence.CoverageStatus.MODELED,ScalarEvidence.assign(op),List.of()),
                    LocalActivationFrames.fallback(unit),Optional.of(returnKey(signal)));
                sequences.add(new Sequence(at,List.of(),term,origin));PartialProgramAssembler.link(fact.header().id(),term,at,links,items);
                publishReturn(returnGroup(context,"activation"),signal);
            }
            return at;
        }
        if(scope.kind()==RegionKind.INLINE_BODY)throw new IllegalArgumentException("inline escape has no active lexical binding");
        return boundDestination(new Target(TargetKind.COMPLETE,scope.id(),target.proofs()),context,fact,role);
    }
    private LabelId compactUnwind(Context context,int count,boolean all,LabelId destination,SpInput.StatementFact fact,String role) {
        var at=new LabelId(unit,context.ids().id("label","compact-unwind",fact.header().id().handle(),role));
        if(synthetic.add(at)) {
            var op=new OperationId(unit,context.ids().id("operation","compact-unwind",fact.header().id().handle(),role));
            var origin=evidence("compact-unwind/"+fact.header().id().handle()+"/"+role,topology.outcomes(fact.header().id().handle()).stream().flatMap(o->o.proofs().stream()).distinct().toList(),context.ids());
            var term=new Operations.LocalUnwind(new Operations.Header(op,origin,Evidence.CoverageStatus.MODELED,ScalarEvidence.assign(op),List.of()),java.math.BigInteger.valueOf(count),destination,LocalActivationFrames.fallback(unit),all);
            sequences.add(new Sequence(at,List.of(),term,origin));PartialProgramAssembler.link(fact.header().id(),term,at,links,items);
        }
        return at;
    }
    private LabelId reentryDestination(SpInput.StatementFact fact,Binding call) {
        var ids=occurrenceIds.activation("reentry-guard:"+call.id());
        var at=new LabelId(unit,ids.id("label","reentry-frontier",call.id(),"undefined"));
        if(synthetic.add(at)) {
            var term=undefinedReentry(fact,ids,call.region());
            sequences.add(new Sequence(at,List.of(),term,term.header().origin()));PartialProgramAssembler.link(fact.header().id(),term,at,links,items);
        }
        return at;
    }
    private LabelId statePhaseLabel(Context context,String phase) {
        var at=new LabelId(unit,context.ids().id("label","topology-state-phase",context.binding().id(),phase));
        work.addLast(context.at("PHASE/"+phase));return at;
    }
    private void statePhase(Context context) {
        var binding=context.binding();var name=context.entry().substring(6);
        var at=new LabelId(unit,context.ids().id("label","topology-state-phase",binding.id(),name));
        if(!emitted.add(at))return;
        var fact=facts.get(binding.caller());
        if(name.equals("BODY")||name.equals("RESUME")) {
            var origin=evidence(binding.id()+"/"+name,binding.proofs(),context.ids());
            Terminator term;
            if(name.equals("RESUME")) {
                term=compactResume(context,"activation",fact,origin);
            } else {
                var entry=topology.entry(binding);
                var target=!topology.inline(binding)&&entry.kind()==TargetKind.OCCURRENCE?compactBody(fact,context,entry.reference()):
                    boundDestination(topology.region(binding.region()).entry(),context,fact,"body");
                term=PerformSequenceAssembler.jump("topology-state-"+name,fact.header().id(),target,origin,unit,context.ids());
            }
            sequences.add(new Sequence(at,List.of(),term,origin));PartialProgramAssembler.link(fact.header().id(),term,at,links,items);
        } else {
            var phase=binding.phases().stream().filter(p->p.id().equals(name)).findFirst().orElseThrow();
            var labels=new HashMap<String,LabelId>();labels.put(name,at);
            phase.edges().forEach(e->labels.put(e.target(),statePhaseLabel(context,e.target())));
            phase(fact,phase,labels,context);
        }
    }
    private Terminator conditioned(Terminator payload,SpInput.StatementFact fact,Context context) {
        var alternatives=new ArrayList<Control.InvocationAlternative>();var inputs=new LinkedHashSet<OriginId>();inputs.add(payload.header().origin());
        for(var event:cicsState.conditions.events(fact.header().id().handle())) {
            for(var choice:cicsState.conditions.select(event,context.support()).choices()) {
                inputs.add(evidence(event.id()+"/condition-selection/"+choice.registration().map(r->r.registration().handle()+"/"+r.condition()).orElse("bypass"),choice.proofs(),context.ids()));
                choice.registration().ifPresent(r->inputs.add(origins.source("condition-registration",r.registration().handle(),facts.get(r.registration().handle()).header().provenance())));
                // A HANDLE CONDITION branch stays in the current COBOL activation.
                // Its registration stays active and no interrupted return is fabricated.
                var target=boundDestination(choice.target(),context,fact,event.id()+"/condition");
                alternatives.add(new Control.Exceptional("CICS_CONDITION/"+event.condition()+"/"+choice.registration().map(r->r.registration().handle()+"/"+r.condition()).orElse("bypass"),new Control.Handler(target)));
            }
        }
        if(alternatives.isEmpty())return payload;
        var old=payload.header();var origin=origins.derived(context.ids().id("origin","cics-condition-dispatch",unit.localId(),old.id().localId()),List.copyOf(inputs),"CICS condition raised / possible current-activation branch");
        var header=new Operations.Header(old.id(),origin,old.coverage(),old.precision(),old.uncertainties());
        if(payload instanceof Operations.Invoke invoke) {
            var known=new ArrayList<>(invoke.outcomes().known());known.addAll(alternatives);
            return new Operations.Invoke(header,invoke.action(),invoke.target(),invoke.arguments(),invoke.results(),invoke.signature(),invoke.effectOperands(),invoke.effectBound(),new Control.InvocationOutcomes(known.stream().distinct().toList(),invoke.outcomes().remainder()),invoke.contract());
        }
        var opaque=(Operations.Opaque)payload;var known=new ArrayList<>(opaque.envelope().control().known());known.addAll(alternatives);
        return new Operations.Opaque(header,opaque.observedKind(),opaque.knownOperands(),opaque.valueResults(),new Envelopes.Envelope(opaque.envelope().memory(),new Control.ControlEnvelope(known.stream().distinct().toList(),opaque.envelope().control().remainder()),opaque.envelope().dependencies()));
    }
    /** Event guards license possible exceptional alternatives, not proof that an
     * error happened. Runtime premises and activation provenance accompany them. */
    private Terminator exceptional(Terminator payload,SpInput.StatementFact fact,Context context) {
        var events=cicsState.events(fact.header().id().handle()).stream().filter(e->cicsState.conditions.defaultPossible(e.id(),context.support())).toList();if(events.isEmpty())return payload;
        var alternatives=new ArrayList<Control.InvocationAlternative>();
        var inputs=new LinkedHashSet<OriginId>();inputs.add(payload.header().origin());
        var descriptions=new ArrayList<String>();boolean localUnknown=false;
        for(var event:events) {
            inputs.add(evidence(event.id(),event.proofs(),context.ids()));
            var selection=cicsState.selection(event,context.support()).orElse(null);
            descriptions.add(event.origin()+" premises="+event.premises());
            if(selection==null) {localUnknown=true;alternatives.add(new Control.Exceptional("CICS_TASK_ABEND",Control.Propagate.INSTANCE));continue;}
            context.support().activation().ifPresent(id->inputs.add(origins.source("handler-activation",id.handle(),facts.get(id.handle()).header().provenance())));
            if(selection.bypassed())alternatives.add(Control.HaltAlternative.INSTANCE);
            else if(selection.localEntry().isPresent()) {
                var support=selection.stateOnEntry().orElseThrow();
                // Finite exceptional ingress, independent of the interrupted call stack.
                var base=occurrenceIds.activation("handler-ingress:"+event.id()+"/"+CicsExecutionState.key(context.support()));
                var handler=new Context(base,null,selection.localEntry().orElseThrow().location(),support,true,FrameKind.ROOT);
                work.addLast(handler);
                var entry=label(handler.entry(),handler.ids());
                entry=compactUnwind(handler,0,true,entry,fact,"handler-ingress");
                alternatives.add(new Control.Exceptional("CICS_TASK_ABEND/"+event.origin(),new Control.Handler(entry)));
            } else {
                if(selection.outerLevelRemainder())alternatives.add(new Control.Exceptional("CICS_TASK_ABEND",Control.Propagate.INSTANCE));
                localUnknown|=selection.unknownLocalRemainder()||selection.target().isPresent();
            }
        }
        var old=payload.header();var scope=new Scopes.EntityScope(List.of(old.id()));
        var origin=origins.derived(context.ids().id("origin","cics-dispatch",unit.localId(),old.id().localId()),List.copyOf(inputs),
            "qualified-cics-exceptional-selection/"+String.join(";",descriptions));
        var gap=new UncertaintyId(unit.publication(),context.ids().id("uncertainty","cics-dispatch",unit.localId(),old.id().localId()));
        uncertainties.add(new Evidence.Uncertainty(gap,"CICS_EXCEPTIONAL_CONTROL_PARTIAL",List.of(Evidence.Dimension.CONTROL),scope,
            "Possible exceptional selection; "+String.join(";",descriptions)+"; local remainder="+localUnknown+"; interrupted continuation is not restored",origin));
        var reasons=new ArrayList<>(old.uncertainties());reasons.add(gap);var p=old.precision();
        var header=new Operations.Header(old.id(),origin,Evidence.CoverageStatus.ABSTRACTED,
            new Evidence.Precision(new Evidence.Claim(scope,Evidence.PrecisionStatus.OPEN,List.of(gap)),p.storage(),p.effects(),p.values(),p.dependencies()),reasons);
        if(payload instanceof Operations.Invoke invoke) {
            var known=new ArrayList<>(invoke.outcomes().known());known.addAll(alternatives);
            return new Operations.Invoke(header,invoke.action(),invoke.target(),invoke.arguments(),invoke.results(),invoke.signature(),invoke.effectOperands(),invoke.effectBound(),
                new Control.InvocationOutcomes(known.stream().distinct().toList(),invoke.outcomes().remainder()),invoke.contract());
        }
        var o=(Operations.Opaque)payload;var known=new ArrayList<Control.ControlAlternative>();
        if(!(fact instanceof SpInput.CicsAbendFact))known.addAll(o.envelope().control().known());
        known.addAll(alternatives);
        var remainder=fact instanceof SpInput.CicsAbendFact?(localUnknown||known.isEmpty()?new Scopes.WithinControl(new Scopes.LabelsControl(List.of())):Scopes.NoControl.INSTANCE):o.envelope().control().remainder();
        return new Operations.Opaque(header,o.observedKind(),o.knownOperands(),o.valueResults(),new Envelopes.Envelope(o.envelope().memory(),
            new Control.ControlEnvelope(known.stream().distinct().toList(),remainder),o.envelope().dependencies()));
    }
    private boolean varyingEffectAvailable(SpInput.ProcedurePerformFact p) {
        if(p.varying().isEmpty())return false;
        var controls=p.varying().orElseThrow().controls();
        return p.varying().orElseThrow().levels()==1
            &&PerformVaryingAdmission.levelExecutable(controls,declarations::get)
            &&controls.stream().flatMap(o->o.references().stream()).allMatch(r->r.wholeItemAccess()
                .filter(w->data.index().containsKey(w.data())).isPresent());
    }
    private static SpInput.ProcedurePerformFact phasePayload(SpInput.ProcedurePerformFact p,int level) {
        if(level==0)return p;
        var varying=p.varying().orElseThrow();
        var loop=level==1?p.loop():Optional.of(varying.afterLoops().get(level-2));
        var controls=varying.controls().stream().filter(o->o.level()==level).toList();
        return new SpInput.ProcedurePerformFact(p.header(),p.start(),p.end(),p.procedures(),p.normalContinuation(),loop,
            p.times(),Optional.of(new SpInput.PerformVarying(1,controls)),p.gapCodes(),p.publicationKind(),p.targetEntry());
    }
    private void phase(SpInput.StatementFact fact,Phase phase,Map<String,LabelId> labels,Context context) {
        var destinations=new HashMap<String,LabelId>();phase.edges().forEach(e->destinations.put(e.role(),labels.get(e.target())));
        Terminator term;
        if(phase.kind()==PhaseKind.PREDICATE&&conditionNames.has(fact.header().id().handle(),"PERFORM_UNTIL/"+phase.level())) {
            term=conditionNames.branch(fact,"PERFORM_UNTIL/"+phase.level(),destinations.get("true"),destinations.get("false"),unit,context.ids().activation("phase:"+phase.id()),origins,uncertainties);
        } else if(!(fact instanceof SpInput.ProcedurePerformFact)) {
            if(phase.kind()==PhaseKind.PREDICATE) {
                var op=new OperationId(unit,context.ids().id("operation","topology-phase",fact.header().id().handle(),phase.id()));
                var predicate=IfPredicate.translateReads(fact.header().id(),fact.header().provenance(),List.of(),List.of(),"topology-phase-"+phase.id(),
                    "control-topology@2.39/predicate-values-unavailable",op,data,context.ids(),origins,operands,items,uncertainties);
                var origin=evidence("phase/"+fact.header().id().handle()+"/"+phase.id(),phase.proofs(),context.ids());
                var exact=new Evidence.Claim(new Scopes.EntityScope(List.of(op)),Evidence.PrecisionStatus.EXACT,List.of());
                var open=new Evidence.Claim(new Scopes.EntityScope(List.of(op)),Evidence.PrecisionStatus.OPEN,List.of(predicate.reason()));
                term=new Operations.Branch(new Operations.Header(op,origin,Evidence.CoverageStatus.ABSTRACTED,new Evidence.Precision(exact,open,open,open,exact),List.of(predicate.reason())),predicate,destinations.get("true"),destinations.get("false"));
            } else term=PartialProgramAssembler.opaque(fact,destinations.get("next"),data,unit,context.ids().activation("phase:"+phase.id()),origins,uncertainties,operands,true,"TOPOLOGY_PHASE_EFFECTS_PARTIAL");
        } else {var p=phasePayload((SpInput.ProcedurePerformFact)fact,phase.level());
            var phaseIds=phase.level()==0?context.ids():context.ids().activation("phase:"+phase.id());
            term=switch(phase.operation()) {
            case "UNTIL_PREDICATE" -> PerformLoopAssembler.decision(p,Math.max(1,phase.level()),destinations.get("false"),destinations.get("true"),data,unit,phaseIds,origins,operands,items,uncertainties);
            case "COUNT_ENTRY","COUNT_REPEAT" -> PerformLoopAssembler.countDecision(p,phase.operation().equals("COUNT_ENTRY"),destinations.get("false"),destinations.get("true"),data,unit,phaseIds,origins,operands,items,uncertainties);
            case "VARY_INITIAL","VARY_UPDATE" -> !varyingEffectAvailable(p)
                ?PartialProgramAssembler.opaque(fact,destinations.get("next"),data,unit,phaseIds.activation("unavailable-phase:"+phase.id()),origins,uncertainties,operands,true,"PERFORM_VARYING_OPERANDS_UNAVAILABLE")
                :PerformVaryingEffects.effect(p,phase.operation().equals("VARY_INITIAL"),destinations.get("next"),data,unit,phaseIds,origins,operands,uncertainties);
            default -> throw new IllegalArgumentException("unsupported topology phase operation");
        };
        }
        term=explain(term,fact,context);
        var label=labels.get(phase.id());sequences.add(new Sequence(label,List.of(),term,term.header().origin()));PartialProgramAssembler.link(fact.header().id(),term,label,links,items);
    }
}
