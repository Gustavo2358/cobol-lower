package io.github.gustavo2358.lower.application;

import java.util.*;
import io.github.gustavo2358.lower.domain.SpInput;
import io.github.gustavo2358.lower.domain.ControlTopology;
import static io.github.gustavo2358.lower.domain.SpInput.*;
import static io.github.gustavo2358.lower.domain.ControlTopology.*;
import static io.github.gustavo2358.lower.application.HandlerStateAnalysis.*;

/** Finite distributive tabulation over validated ControlTopology. No source order,
 * path enumeration, runtime level identity, AIR or exceptional edges. */
final class HandlerStateAnalyzer {
    private record Context(String id, ControlTopology.Binding binding) { }
    private record Subscriber(Node caller, String outcome, List<String> proofs) { }
    private final SpInput input;
    private final ControlTopology topology;
    private final TopologyBinding binder;
    private final boolean reverse;
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
    private long pops, joins, maxAtPoint;

    HandlerStateAnalyzer(SpInput input) { this(input,false); }
    // Package access solely to exercise worklist scheduling independence.
    HandlerStateAnalyzer(SpInput input,boolean reverse) {
        this.input=input;this.reverse=reverse;topology=input.controlTopology().orElseThrow();binder=new TopologyBinding(topology);
        input.statements().forEach(s->statements.put(s.header().id().handle(),s));
        for(var s:statements.values())if(s instanceof CicsHandlerFact h&&h.action()==CicsHandlerAction.ACTIVATE)catalog(h);
        for(var b:topology.bindings()) {var map=new HashMap<String,ControlTopology.Phase>();b.phases().forEach(p->map.put(p.id(),p));phases.put(b.id(),map);}
    }
    HandlerStateAnalysis analyze() {
        var root=new Context("ROOT",null);contexts.put(root.id(),root);
        var initial=new Support(new State(Kind.ENTRY_UNKNOWN,"",Cause.NONE),Optional.empty());
        binder.primaryEntry().ifPresent(e->route(root,e,initial,Optional.empty(),Optional.empty(),"PRIMARY_ENTRY",e.proofs()));
        while(!work.isEmpty()) {
            var node=reverse?work.removeLast():work.removeFirst();pops++;
            if(node.location().startsWith("PHASE/"))phase(node);else occurrence(node);
        }
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
                operations.size(),targets.size(),reached.stream().map(n->n.support().state()).distinct().count(),contexts.size(),pops,reached.size(),joins,maxAtPoint));
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
        // ABEND is a query, never a completion/return, including unavailable eligibility.
        if(statement instanceof CicsAbendFact)return;
        var context=contexts.get(node.context());
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
            } else if(statement instanceof CallFact)next=unknown(Cause.CALL_EFFECT_UNAVAILABLE);
            var resolved=binder.resolve(outcome.target(),context.binding());
            route(context,resolved,next,Optional.of(node),Optional.empty(),outcome.id(),merge(outcome.proofs(),resolved.proofs()));
        }
    }
    private Support transfer(CicsHandlerFact h,Support predecessor) {
        return switch(h.action()) {
            case ACTIVATE -> new Support(new State(Kind.ACTIVE,registrationTargets.get(h.header().id().handle()),Cause.NONE),Optional.of(h.header().id()));
            case CANCEL -> predecessor.state().target().isEmpty()
                ?new Support(new State(Kind.CANCELED_UNKNOWN,"",Cause.NONE),Optional.empty())
                :new Support(new State(Kind.CANCELED,predecessor.state().target(),Cause.NONE),predecessor.activation());
            case RESET -> switch(predecessor.state().kind()) {
                case CANCELED -> new Support(new State(Kind.ACTIVE,predecessor.state().target(),Cause.NONE),predecessor.activation());
                case CANCELED_UNKNOWN -> unknown(Cause.RESET_HISTORY_UNAVAILABLE);
                default -> unknown(Cause.RESET_WITHOUT_CANCELED_EVIDENCE);
            };
            case UNAVAILABLE -> unknown(Cause.HANDLER_OPERATION_UNAVAILABLE);
        };
    }
    private static Support unknown(Cause cause) {return new Support(new State(Kind.UNKNOWN,"",cause),Optional.empty());}
    private void invoke(Node caller,Outcome outcome) {
        var binding=binder.binding(outcome.binding());
        String key=binding.id()+"|"+supportKey(caller.support());
        var callee=contexts.computeIfAbsent(key,k->new Context(k,binding));
        var subscriber=new Subscriber(caller,outcome.id(),outcome.proofs());
        if(subscribers.computeIfAbsent(key,k->new HashSet<>()).add(subscriber))
            for(var exit:summaries.getOrDefault(key,Set.of()))resume(callee,exit,subscriber);
        insert(new Node(key,"PHASE/"+binding.entryPhase(),caller.support()),Optional.of(caller),Optional.empty(),outcome.id(),merge(outcome.proofs(),binding.proofs()));
    }
    private void phase(Node node) {
        var context=contexts.get(node.context());var binding=context.binding();var name=node.location().substring(6);
        if(name.equals("BODY")) {
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
        var resolved=binder.resolve(callee.binding().resume(),callerContext.binding());
        route(callerContext,resolved,exit.support(),Optional.of(exit),Optional.of(subscriber.caller()),
            callee.binding().id()+"/RESUME/"+subscriber.outcome(),merge(callee.binding().proofs(),resolved.proofs()));
    }
    private void route(Context context,TopologyBinding.Resolved target,Support support,Optional<Node> source,
            Optional<Node> caller,String authority,List<String> proofs) {
        switch(target.kind()) {
            case OCCURRENCE -> insert(new Node(context.id(),target.reference(),support),source,caller,authority,proofs);
            case COMPLETE -> insert(new Node(context.id(),"PHASE/"+Objects.requireNonNull(context.binding()).completionPhase(),support),source,caller,authority,proofs);
            case UNKNOWN_LOCAL -> source.ifPresent(n->frontiers.add(new Frontier(n,authority,target.reference(),proofs)));
            case PROGRAM_RETURN -> { /* program return is not a PERFORM completion */ }
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
            event.dispatchEligibility()==CicsAbendEligibility.UNAVAILABLE?EventStatus.ELIGIBILITY_UNAVAILABLE:EventStatus.ASSESSED;
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
                case CANCELED,CANCELED_UNKNOWN -> {inactive=true;outer=true;}
                case ENTRY_UNKNOWN,UNKNOWN -> {unknown=true;inactive=true;outer=true;}
            }
        }
        if(status==EventStatus.ELIGIBILITY_UNAVAILABLE) {candidates.clear();unknown=true;outer=true;}
        var list=new ArrayList<Candidate>();candidates.forEach((key,value)->list.add(new Candidate(key,ordered(value,StatementId::handle))));
        var definite=!unknown&&!inactive&&!bypass&&list.size()==1?Optional.of(list.getFirst().target()):Optional.<String>empty();
        return new Event(event.header().id(),event.dispatchEligibility(),status,states,list,definite,unknown,inactive,outer,bypass);
    }
    private static String supportKey(Support s) {return s.state().kind()+"/"+s.state().target()+"/"+s.state().cause()+"/"+s.activation().map(StatementId::handle).orElse("");}
    private static String nodeKey(Node n) {return n.context()+"/"+n.location()+"/"+supportKey(n.support());}
    private static String derivationKey(Derivation d) {return nodeKey(d.destination())+"/"+d.source().map(HandlerStateAnalyzer::nodeKey).orElse("")+"/"+d.callerPremise().map(HandlerStateAnalyzer::nodeKey).orElse("")+"/"+d.authority();}
    private static <T> List<T> ordered(Collection<T> values,java.util.function.Function<T,String> key) {return values.stream().sorted(Comparator.comparing(key)).toList();}
    private static List<String> merge(List<String> a,List<String> b) {var all=new TreeSet<String>();all.addAll(a);all.addAll(b);return List.copyOf(all);}
}
