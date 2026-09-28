package io.github.gustavo2358.lower.application;

import java.util.*;
import io.github.gustavo2358.lower.domain.SpInput;
import io.github.gustavo2358.lower.domain.ControlTopology;
import static io.github.gustavo2358.lower.domain.SpInput.*;
import static io.github.gustavo2358.lower.domain.ControlTopology.*;
import static io.github.gustavo2358.lower.application.HandlerStateAnalysis.*;

/** Finite distributive tabulation over validated ControlTopology. No source order,
 * path enumeration, runtime level identity, AIR executable edges. Source exceptional ingress requires a published descriptor. */
final class HandlerStateAnalyzer {
    private record Context(String id, ControlTopology.Binding binding, String ingress, boolean conditional) { }
    private record Subscriber(Node caller, String outcome, List<String> proofs) { }
    private final SpInput input;
    private final ControlTopology topology;
    private final TopologyBinding binder;
    private final boolean reverse;
    private final boolean sourceOnly;
    private final ConditionExecutionState conditions;
    private final Map<String,Set<String>> localCalls=new HashMap<>();
    private final Map<String,List<ControlTopology.SourceContinuation>> sourceContinuations=new HashMap<>();
    private final Map<String,StatementFact> statements=new TreeMap<>();
    private final Map<String,HandlerStateAnalysis.Target> targets=new TreeMap<>();
    private final Map<String,String> registrationTargets=new HashMap<>();
    private final Map<String,Context> contexts=new HashMap<>();
    private final Map<String,Map<String,ControlTopology.Phase>> phases=new HashMap<>();
    private final Set<Node> reached=new HashSet<>();
    private final ArrayDeque<Node> work=new ArrayDeque<>();
    private final Set<Derivation> derivations=new HashSet<>();
    private final Set<Frontier> frontiers=new HashSet<>();
    private final Map<String,Set<Subscriber>> subscribers=new HashMap<>();
    private final Map<String,Set<Node>> summaries=new HashMap<>();
    private final Map<String,Set<Support>> before=new HashMap<>(), after=new HashMap<>();
    private final Map<String,Integer> pointCounts=new HashMap<>();
    private final Map<String,List<ExceptionalEvent>> exceptionalEvents=new HashMap<>();
    private final Set<Selection> selections=new HashSet<>();
    private final Set<String> conditionallyReached=new HashSet<>();
    private final Map<String,List<ControlTopology.SourceContinuation>> sourcePrerequisites=new HashMap<>();
    private final Map<String,Set<Node>> sourceLocations=new HashMap<>();
    private long pops, joins, maxAtPoint;

    HandlerStateAnalyzer(SpInput input) { this(input,false); }
    // Package access solely to exercise worklist scheduling independence.
    HandlerStateAnalyzer(SpInput input,boolean reverse) { this(input,reverse,false); }
    HandlerStateAnalyzer(SpInput input,boolean reverse,boolean sourceOnly) {
        this.input=input;this.reverse=reverse;this.sourceOnly=sourceOnly;conditions=new ConditionExecutionState(input);topology=input.controlTopology().orElseThrow();binder=new TopologyBinding(topology);
        topology.exceptionalEvents().forEach(e->exceptionalEvents.computeIfAbsent(e.statement(),k->new ArrayList<>()).add(e));
        input.statements().forEach(s->statements.put(s.header().id().handle(),s));
        if(sourceOnly)for(var c:topology.sourceContinuations()) {
            sourceContinuations.computeIfAbsent(c.statement(),k->new ArrayList<>()).add(c);
            for(var prerequisite:c.prerequisites())sourcePrerequisites.computeIfAbsent(prerequisite,k->new ArrayList<>()).add(c);
        }
        for(var s:statements.values())if(s instanceof CicsHandlerFact h&&h.action()==CicsHandlerAction.ACTIVATE)catalog(h);
        for(var b:topology.bindings()) {var map=new HashMap<String,ControlTopology.Phase>();b.phases().forEach(p->map.put(p.id(),p));phases.put(b.id(),map);}
    }
    HandlerStateAnalysis analyze() {
        var root=new Context("ROOT",null,"",false);contexts.put(root.id(),root);
        var initial=new Support(new State(Kind.ENTRY_UNKNOWN,"",Cause.NONE),Optional.empty());
        binder.primaryEntry().ifPresent(e->route(root,e,initial,Optional.empty(),Optional.empty(),"PRIMARY_ENTRY",e.proofs()));
        for(var e:topology.entryPoints())if(e.target().kind()==TargetKind.OCCURRENCE) {
            var alternate=new Context("ENTRY/"+e.entry(),null,"",false);contexts.put(alternate.id(),alternate);
            route(alternate,binder.resolve(e.target(),null),initial,Optional.empty(),Optional.empty(),"ALTERNATE_ENTRY",merge(e.proofs(),e.target().proofs()));
        }
        do {
        while(!work.isEmpty()) {
            var node=reverse?work.removeLast():work.removeFirst();pops++;
            if(node.location().startsWith("FILE_POINT/"))filePoint(node);
            else if(node.location().startsWith("PHASE/")||node.location().startsWith("ESCAPE/"))phase(node);else occurrence(node);
        }
        } while(sourceOnly&&undefinedReentrySummaries());
        var operations=new ArrayList<Operation>();var events=new ArrayList<Event>();
        for(var s:statements.values()) {
            var id=s.header().id();var states=ordered(before.getOrDefault(id.handle(),Set.of()),HandlerStateAnalyzer::supportKey);
            if(s instanceof CicsHandlerFact h)operations.add(new Operation(id,h.action(),states,ordered(after.getOrDefault(id.handle(),Set.of()),HandlerStateAnalyzer::supportKey)));
            if(s instanceof CicsAbendFact a)events.add(assess(a,states));
        }
        return new HandlerStateAnalysis(input.unit(),List.copyOf(targets.values()),operations,events,
            ordered(reached,HandlerStateAnalyzer::nodeKey),ordered(derivations,HandlerStateAnalyzer::derivationKey),
            ordered(frontiers,f->nodeKey(f.source())+"/"+f.authority()+"/"+f.reference()),
            new Metrics(topology.occurrences().size(),topology.outcomes().size()+topology.bindings().stream().flatMap(b->b.phases().stream()).mapToLong(p->p.edges().size()).sum(),
                operations.size(),targets.size(),reached.stream().map(n->n.support().state()).distinct().count(),contexts.size(),pops,reached.size(),joins,maxAtPoint),
            ordered(selections,x->x.event()+"/"+nodeKey(x.source())));
    }
    private void catalog(CicsHandlerFact h) {
        String id;TargetForm form;
        if(h.targetKind()==CicsHandlerTargetKind.LABEL&&h.labelTarget().isPresent()) {
            id="LABEL/"+h.labelTarget().orElseThrow().id().handle();form=TargetForm.LABEL_LOCAL;
        } else if(h.targetKind()==CicsHandlerTargetKind.LABEL) {id="UNRESOLVED/"+h.header().id().handle();form=TargetForm.LABEL_UNRESOLVED;}
        else {id="PROGRAM/"+h.header().id().handle();form=h.programTarget().map(t->t instanceof LiteralCallTarget?TargetForm.PROGRAM_LITERAL:TargetForm.PROGRAM_DATA).orElse(TargetForm.PROGRAM_UNRESOLVED);}
        registrationTargets.put(h.header().id().handle(),id);
        var registrations=new ArrayList<Registration>();var old=targets.get(id);if(old!=null)registrations.addAll(old.registrations());
        registrations.add(new Registration(h.header().id(),h.header().provenance(),h.targetOrigin().orElseThrow()));
        targets.put(id,new HandlerStateAnalysis.Target(id,form,h.labelTarget(),h.targetEntry(),h.entryOrigin(),h.programTarget(),registrations));
    }
    private void occurrence(Node node) {
        var statement=statements.get(node.location());
        before.computeIfAbsent(node.location(),k->new HashSet<>()).add(node.support());
        var context=contexts.get(node.context());
        if(context.conditional())conditionallyReached.add(node.location());
        for(var event:conditions.events(node.location()))for(var choice:conditions.select(event,node.support()).choices()) {
            var target=binder.resolve(choice.target(),context.binding(),!context.ingress().isEmpty());
            route(context,target,node.support(),Optional.of(node),Optional.empty(),event.id()+"/CONDITION",merge(choice.proofs(),target.proofs()));
        }
        for(var event:exceptionalEvents.getOrDefault(node.location(),List.of()))if(conditions.defaultPossible(event.id(),node.support()))select(node,event,context);
        // ABEND is a query, never a completion/return, including unavailable eligibility.
        if(statement instanceof CicsAbendFact)return;
        var flow=binder.fileFlow(node.location());
        if(flow.isPresent()) {
            var entry=flow.get().entry();route(context,binder.resolve(entry,context.binding(),!context.ingress().isEmpty()),
                node.support(),Optional.of(node),Optional.empty(),node.location()+"/FILE_ENTRY",flow.get().proofs());return;
        }
        if(!sourceContinuations.isEmpty()) {
            sourceLocations.computeIfAbsent(node.location(),k->new HashSet<>()).add(node);
            for(var possible:sourcePrerequisites.getOrDefault(node.location(),List.of()))
                for(var transfer:List.copyOf(sourceLocations.getOrDefault(possible.statement(),Set.of())))sourcePossibility(transfer,possible,Optional.of(node));
            for(var possible:sourceContinuations.getOrDefault(node.location(),List.of())) {
                if(possible.prerequisites().isEmpty())sourcePossibility(node,possible,Optional.empty());
                else for(var premise:List.copyOf(sourceLocations.getOrDefault(possible.prerequisites().getFirst(),Set.of())))sourcePossibility(node,possible,Optional.of(premise));
            }
        }
        for(var outcome:binder.outcomes(node.location())) {
            if(outcome.kind()==OutcomeKind.LOCAL_INVOKE) {invoke(node,outcome);continue;}
            var next=node.support();
            if(statement instanceof CicsHandlerFact h) {
                // Existence/targetEntry is not successful execution proof.
                if(outcome.kind()!=OutcomeKind.NORMAL) {
                    frontiers.add(new Frontier(node,outcome.id(),outcome.target().reference(),outcome.proofs()));continue;
                }
                next=transfer(h,next);
                after.computeIfAbsent(node.location(),k->new HashSet<>()).add(next);
            } else if(statement instanceof CallFact)next=ConditionExecutionState.open(unknown(Cause.CALL_EFFECT_UNAVAILABLE).withConditions(node.support().conditions()));
            if(outcome.kind()==OutcomeKind.NORMAL)next=conditions.update(node.location(),next);
            var resolved=binder.resolve(outcome.target(),context.binding(),!context.ingress().isEmpty());
            route(context,resolved,next,Optional.of(node),Optional.empty(),outcome.id(),merge(outcome.proofs(),resolved.proofs()));
        }
    }
    private void sourcePossibility(Node source,ControlTopology.SourceContinuation possible,Optional<Node> prerequisite) {
        var context=contexts.get(source.context());var target=binder.resolve(possible.target(),context.binding(),!context.ingress().isEmpty());
        route(context,target,possible.prerequisites().isEmpty()?ConditionExecutionState.open(source.support()):source.support(),Optional.of(source),prerequisite,(possible.prerequisites().isEmpty()?possible.statement():possible.identity())+"/SOURCE_POSSIBILITY",merge(possible.proofs(),target.proofs()));
    }
    private void filePoint(Node node) {
        var point=binder.filePoint(node.location().substring("FILE_POINT/".length()));
        var context=contexts.get(node.context());
        if(point.kind()==FilePointKind.USE) {
            var owner=binder.filePointOwner(point.id());
            var routes=binder.outcomes(owner).stream().filter(o->o.role().startsWith("file/"+point.ordinal()+"/")).toList();
            if(!routes.isEmpty()) {
                for(var outcome:routes) {
                    var resolved=binder.resolve(outcome.target(),context.binding(),!context.ingress().isEmpty());
                    route(context,resolved,node.support(),Optional.of(node),Optional.empty(),outcome.id(),merge(outcome.proofs(),resolved.proofs()));
                }
                return;
            }
        }
        for(var target:point.targets()) {
            var resolved=binder.resolve(target,context.binding(),!context.ingress().isEmpty());
            route(context,resolved,node.support(),Optional.of(node),Optional.empty(),point.id(),merge(point.proofs(),resolved.proofs()));
        }
    }
    private void select(Node source,ExceptionalEvent event,Context sourceContext) {
        boolean bypass=event.eligibility()==EventEligibility.HANDLERS_BYPASSED;
        var state=source.support().state();boolean unknown=false,inactive=false,outer=false;
        Optional<String> selected=Optional.empty();Optional<Support> entryState=Optional.empty();Optional<Node> localEntry=Optional.empty();
        if(!bypass) {
            if(state.kind()==Kind.ACTIVE) {
                var target=targets.get(state.target());
                selected=Optional.of(target.id());
                var deactivated=new Support(new State(Kind.DEACTIVATED,target.id(),Cause.NONE),source.support().activation(),source.support().conditions());
                entryState=Optional.of(deactivated);
                unknown=target.form()==TargetForm.LABEL_UNRESOLVED||target.form()==TargetForm.PROGRAM_UNRESOLVED;
                if(target.form()==TargetForm.LABEL_LOCAL&&target.entry().isPresent()) {
                    // No return to the faulting callsite. Unknown restored completion stays bounded.
                    boolean conditional=sourceContext.conditional()||!event.premises().isEmpty();
                    String ingress=event.id()+"/"+supportKey(source.support());
                    String key="HANDLER/"+ingress+"/"+conditional;
                    contexts.computeIfAbsent(key,k->new Context(k,null,ingress,conditional));
                    var destination=new Node(key,target.entry().orElseThrow().handle(),deactivated);
                    localEntry=Optional.of(destination);
                    insert(destination,Optional.of(source),Optional.empty(),event.id()+"/SELECT/"+target.id(),event.proofs());
                }
            } else if(state.kind()==Kind.CANCELED||state.kind()==Kind.DEACTIVATED||state.kind()==Kind.CANCELED_UNKNOWN) {
                inactive=true;outer=true;
            } else {unknown=true;inactive=true;outer=true;}
        }
        selections.add(new Selection(event.id(),source,event.origin().name(),event.premises().stream().map(Enum::name).toList(),
            selected,entryState,localEntry,unknown,inactive,outer,bypass,event.proofs()));
    }
    Support transfer(CicsHandlerFact h,Support predecessor) {
        var result=switch(h.action()) {
            case ACTIVATE -> new Support(new State(Kind.ACTIVE,registrationTargets.get(h.header().id().handle()),Cause.NONE),Optional.of(h.header().id()));
            case CANCEL -> predecessor.state().target().isEmpty()
                ?new Support(new State(Kind.CANCELED_UNKNOWN,"",Cause.NONE),Optional.empty())
                :new Support(new State(Kind.CANCELED,predecessor.state().target(),Cause.NONE),predecessor.activation());
            case RESET -> switch(predecessor.state().kind()) {
                case CANCELED,DEACTIVATED -> new Support(new State(Kind.ACTIVE,predecessor.state().target(),Cause.NONE),predecessor.activation());
                case CANCELED_UNKNOWN -> unknown(Cause.RESET_HISTORY_UNAVAILABLE);
                default -> unknown(Cause.RESET_WITHOUT_CANCELED_EVIDENCE);
            };
            case UNAVAILABLE -> unknown(Cause.HANDLER_OPERATION_UNAVAILABLE);
        };
        return result.withConditions(predecessor.conditions());
    }
    private static Support unknown(Cause cause) {return new Support(new State(Kind.UNKNOWN,"",cause),Optional.empty());}
    private void invoke(Node caller,Outcome outcome) {
        var binding=binder.binding(outcome.binding());
        var parent=contexts.get(caller.context());
        String key=binding.id()+"|"+supportKey(caller.support())+(parent.ingress().isEmpty()?"":"|INGRESS/"+parent.ingress()+"/"+parent.conditional());
        var callee=contexts.computeIfAbsent(key,k->new Context(k,binding,parent.ingress(),parent.conditional()));
        localCalls.computeIfAbsent(parent.id(),k->new HashSet<>()).add(key);
        var subscriber=new Subscriber(caller,outcome.id(),outcome.proofs());
        if(subscribers.computeIfAbsent(key,k->new HashSet<>()).add(subscriber))
            for(var exit:summaries.getOrDefault(key,Set.of()))resume(callee,exit,subscriber);
        insert(new Node(key,"PHASE/"+binding.entryPhase(),caller.support()),Optional.of(caller),Optional.empty(),outcome.id(),merge(outcome.proofs(),binding.proofs()));
    }
    static String reentryProof(String binding) {return "source-reentry/"+binding;}
    /** A cycle in the finite source context dependency graph permits an unknown
     * completion. This is source evidence only; executable state never calls it. */
    private boolean undefinedReentrySummaries() {
        for(var callee:List.copyOf(contexts.values())) {
            if(callee.binding()==null||callee.binding().reentryPolicy()!=ReentryPolicy.SOURCE_UNDEFINED)continue;
            var descendants=new HashSet<String>();var pending=new ArrayDeque<String>();pending.add(callee.id());
            while(!pending.isEmpty()) {var at=pending.removeFirst();if(descendants.add(at))pending.addAll(localCalls.getOrDefault(at,Set.of()));}
            for(var subscriber:List.copyOf(subscribers.getOrDefault(callee.id(),Set.of()))) {
                // The subscriber itself provides the final edge of the cycle.
                if(!descendants.contains(subscriber.caller().context()))continue;
                var entry=new Node(callee.id(),"PHASE/"+callee.binding().entryPhase(),subscriber.caller().support());
                if(!reached.contains(entry))continue;
                insert(new Node(callee.id(),"PHASE/RESUME",ConditionExecutionState.open(unknown(Cause.SOURCE_REENTRY_UNDEFINED).withConditions(subscriber.caller().support().conditions()))),
                    Optional.of(subscriber.caller()),Optional.of(entry),callee.binding().id()+"/SOURCE_REENTRY_POSSIBILITY",
                    List.of(reentryProof(callee.binding().id())));
            }
        }
        return !work.isEmpty();
    }
    private void phase(Node node) {
        var context=contexts.get(node.context());var binding=context.binding();var name=node.location().substring(6);
        if(node.location().startsWith("ESCAPE/")) {
            if(summaries.computeIfAbsent(context.id(),k->new HashSet<>()).add(node))
                for(var subscriber:subscribers.getOrDefault(context.id(),Set.of()))resume(context,node,subscriber);
        } else if(name.equals("BODY")) {
            var entry=binder.entry(binding);route(context,entry,node.support(),Optional.of(node),Optional.empty(),binding.id()+"/BODY",entry.proofs());
        } else if(name.equals("RESUME")) {
            if(summaries.computeIfAbsent(context.id(),k->new HashSet<>()).add(node))
                for(var subscriber:subscribers.getOrDefault(context.id(),Set.of()))resume(context,node,subscriber);
        } else {
            var phase=Objects.requireNonNull(phases.get(binding.id()).get(name));
            for(var edge:phase.edges())insert(new Node(context.id(),"PHASE/"+edge.target(),node.support()),Optional.of(node),Optional.empty(),phase.id()+"/"+edge.role(),phase.proofs());
        }
    }
    private void resume(Context callee,Node exit,Subscriber subscriber) {
        var callerContext=contexts.get(subscriber.caller().context());
        var resolved=exit.location().startsWith("ESCAPE/")
            ?new TopologyBinding.Resolved(TargetKind.ESCAPE,exit.location().substring(7),callee.binding().proofs())
            :binder.resolve(callee.binding().resume(),callerContext.binding(),!callerContext.ingress().isEmpty());
        route(callerContext,resolved,exit.support(),Optional.of(exit),Optional.of(subscriber.caller()),
            callee.binding().id()+"/RESUME/"+subscriber.outcome(),merge(callee.binding().proofs(),resolved.proofs()));
    }
    private void route(Context context,TopologyBinding.Resolved target,Support support,Optional<Node> source,
            Optional<Node> caller,String authority,List<String> proofs) {
        switch(target.kind()) {
            case ESCAPE -> {
                var region=binder.region(target.reference());
                if(region.kind()==RegionKind.INLINE_BODY&&context.binding()!=null&&context.binding().endpoint().equals(region.boundary()))
                    insert(new Node(context.id(),"PHASE/RESUME",support),source,caller,authority,proofs);
                else if(region.kind()!=RegionKind.INLINE_BODY&&(context.binding()==null||!binder.inline(context.binding())||binder.contains(context.binding(),region.id()))) {
                    var completion=new ControlTopology.Target(TargetKind.COMPLETE,region.id(),target.proofs());
                    route(context,binder.resolve(completion,context.binding(),!context.ingress().isEmpty()),support,source,caller,authority,proofs);
                } else if(context.binding()!=null)insert(new Node(context.id(),"ESCAPE/"+region.id(),support),source,caller,authority,proofs);
                else source.ifPresent(n->frontiers.add(new Frontier(n,authority,target.reference(),proofs)));
            }
            case FILE_POINT -> insert(new Node(context.id(),"FILE_POINT/"+target.reference(),support),source,caller,authority,proofs);
            case OCCURRENCE -> insert(new Node(context.id(),target.reference(),support),source,caller,authority,proofs);
            case COMPLETE -> insert(new Node(context.id(),"PHASE/"+Objects.requireNonNull(context.binding()).completionPhase(),support),source,caller,authority,proofs);
            case UNKNOWN_LOCAL -> source.ifPresent(n->frontiers.add(new Frontier(n,authority,target.reference(),proofs)));
            case PROGRAM_RETURN, PROGRAM_HALT -> { /* program return is not a PERFORM completion */ }
            default -> throw new IllegalStateException("unresolved topology alias");
        }
    }
    private void insert(Node node,Optional<Node> source,Optional<Node> caller,String authority,List<String> proofs) {
        joins++;derivations.add(new Derivation(source,node,caller,authority,merge(proofs,List.of())));
        if(reached.add(node)) {work.addLast(node);maxAtPoint=Math.max(maxAtPoint,pointCounts.merge(node.context()+"/"+node.location(),1,Integer::sum));}
    }
    private Event assess(CicsAbendFact event,List<Support> states) {
        boolean bypass=event.dispatchEligibility()==CicsAbendEligibility.HANDLERS_BYPASSED;
        var status=states.isEmpty()?EventStatus.NOT_REACHED_IN_PUBLISHED_TOPOLOGY:
            event.dispatchEligibility()==CicsAbendEligibility.UNAVAILABLE?EventStatus.ELIGIBILITY_UNAVAILABLE:
            conditionallyReached.contains(event.header().id().handle())?EventStatus.ASSESSED_WITH_CONDITIONAL_INGRESS:EventStatus.ASSESSED;
        var candidates=new TreeMap<String,Set<StatementId>>();boolean unknown=false,inactive=false,outer=false;
        if(!bypass)for(var support:states) {
            var state=support.state();
            switch(state.kind()) {
                case ACTIVE -> {
                    var target=targets.get(state.target());
                    if(target.form()==TargetForm.LABEL_LOCAL||target.form()==TargetForm.PROGRAM_LITERAL||target.form()==TargetForm.PROGRAM_DATA)
                        candidates.computeIfAbsent(target.id(),k->new HashSet<>()).add(support.activation().orElseThrow());
                    else unknown=true;
                    // Successfully established local exit intercepts before outer levels,
                    // even when the static identity/name remains unresolved.
                }
                case CANCELED,DEACTIVATED,CANCELED_UNKNOWN -> {inactive=true;outer=true;}
                case ENTRY_UNKNOWN,UNKNOWN -> {unknown=true;inactive=true;outer=true;}
            }
        }
        if(status==EventStatus.ELIGIBILITY_UNAVAILABLE) {candidates.clear();unknown=true;outer=true;}
        var list=new ArrayList<Candidate>();candidates.forEach((key,value)->list.add(new Candidate(key,ordered(value,StatementId::handle))));
        var definite=!unknown&&!inactive&&!bypass&&list.size()==1?Optional.of(list.getFirst().target()):Optional.<String>empty();
        return new Event(event.header().id(),event.dispatchEligibility(),status,states,list,definite,unknown,inactive,outer,bypass);
    }
    static String supportKey(Support s) {return s.state().kind()+"/"+s.state().target()+"/"+s.state().cause()+"/"+s.activation().map(StatementId::handle).orElse("")+(s.conditions().isEmpty()?"":"/CONDITIONS/"+s.conditions().stream().map(c->c.condition()+"/"+c.registration().handle()+"/"+c.uncertain()).toList());}
    private static String nodeKey(Node n) {return n.context()+"/"+n.location()+"/"+supportKey(n.support());}
    private static String derivationKey(Derivation d) {return nodeKey(d.destination())+"/"+d.source().map(HandlerStateAnalyzer::nodeKey).orElse("")+"/"+d.callerPremise().map(HandlerStateAnalyzer::nodeKey).orElse("")+"/"+d.authority();}
    private static <T> List<T> ordered(Collection<T> values,java.util.function.Function<T,String> key) {return values.stream().sorted(Comparator.comparing(key)).toList();}
    private static List<String> merge(List<String> a,List<String> b) {var all=new TreeSet<String>();all.addAll(a);all.addAll(b);return List.copyOf(all);}
}
