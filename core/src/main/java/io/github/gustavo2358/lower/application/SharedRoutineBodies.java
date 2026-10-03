package io.github.gustavo2358.lower.application;

import io.github.gustavo2358.lower.domain.ControlTopology;
import static io.github.gustavo2358.lower.domain.ControlTopology.*;
import java.util.*;

/** Proof of caller-independent body control. Unknown eligibility retains existing specialization. */
final class SharedRoutineBodies {
    record Key(String entry,String endpoint) {
        String identity(){return entry.length()+":"+entry+"/"+endpoint.length()+":"+endpoint;}
    }
    private final Map<String,Key> keys=new HashMap<>();
    private final Map<Key,List<String>> proofs=new HashMap<>();
    private final Set<String> guarded=new HashSet<>();
    private boolean recursiveUndefined;
    SharedRoutineBodies(ControlTopology product,TopologyBinding topology,java.util.function.Predicate<String> stableState) {
        this(product,topology,stableState,false);
    }
    SharedRoutineBodies(ControlTopology product,TopologyBinding topology,java.util.function.Predicate<String> stableState,boolean admitGuardedCycles) {
        var dependencies=new LinkedHashMap<String,Set<String>>();var blocked=new HashSet<String>();
        for(var binding:product.bindings()) {
            var entry=topology.entry(binding);var calls=new HashSet<String>();dependencies.put(binding.id(),calls);
            if(entry.kind()!=TargetKind.OCCURRENCE){blocked.add(binding.id());continue;}
            keys.put(binding.id(),new Key(entry.reference(),binding.endpoint()));
            for(var occurrence:topology.closure(entry.reference(),binding)) {
                if(!stableState.test(occurrence))blocked.add(binding.id());
                for(var outcome:topology.outcomes(occurrence)) {
                    if(outcome.kind()==OutcomeKind.LOCAL_INVOKE)calls.add(outcome.binding());
                    if(outcome.target().kind()==TargetKind.ESCAPE)blocked.add(binding.id());
                }
                topology.fileFlow(occurrence).ifPresent(flow->{
                    if(flow.points().stream().flatMap(p->p.targets().stream()).anyMatch(t->t.kind()==TargetKind.ESCAPE))blocked.add(binding.id());
                });
            }
        }
        var users=new HashMap<String,List<String>>();
        dependencies.forEach((id,calls)->calls.forEach(c->users.computeIfAbsent(c,k->new ArrayList<>()).add(id)));
        var recursive=recursive(dependencies,users);
        recursiveUndefined=product.bindings().stream().anyMatch(b->recursive.contains(b.id())&&b.reentryPolicy()==ReentryPolicy.SOURCE_UNDEFINED);
        for(var binding:product.bindings())if(recursive.contains(binding.id())) {
            if(admitGuardedCycles&&binding.reentryPolicy()==ReentryPolicy.SOURCE_UNDEFINED)guarded.add(binding.id());
            else blocked.add(binding.id());
        }
        // A caller can share only when every transitive body can share. Cycles are
        // admitted together; one unsupported member blocks all callers, including its SCC.
        dependencies.forEach((id,calls)->{if(calls.stream().anyMatch(c->!dependencies.containsKey(c)))blocked.add(id);});
        var queue=new ArrayDeque<>(blocked);
        while(!queue.isEmpty())for(var user:users.getOrDefault(queue.removeFirst(),List.of()))if(blocked.add(user))queue.addLast(user);
        keys.keySet().removeAll(blocked);guarded.retainAll(keys.keySet());
        var grouped=new HashMap<Key,Set<String>>();
        for(var binding:product.bindings())if(keys.containsKey(binding.id()))grouped.computeIfAbsent(keys.get(binding.id()),k->new TreeSet<>()).addAll(binding.proofs());
        grouped.forEach((key,value)->proofs.put(key,List.copyOf(value)));
    }
    /** Kosaraju with explicit DFS stacks: no recursion bound on the Java stack. */
    private static Set<String> recursive(Map<String,Set<String>> graph,Map<String,List<String>> reverse) {
        record Visit(String id,Iterator<String> next) { }
        var seen=new HashSet<String>();var order=new ArrayList<String>();var stack=new ArrayDeque<Visit>();
        for(var start:graph.keySet())if(seen.add(start)) {
            stack.push(new Visit(start,graph.get(start).iterator()));
            while(!stack.isEmpty()) {
                var at=stack.peek();
                if(at.next().hasNext()) {
                    var next=at.next().next();
                    if(graph.containsKey(next)&&seen.add(next))stack.push(new Visit(next,graph.get(next).iterator()));
                } else {order.add(at.id());stack.pop();}
            }
        }
        seen.clear();var recursive=new HashSet<String>();var work=new ArrayDeque<String>();
        for(int i=order.size()-1;i>=0;i--)if(seen.add(order.get(i))) {
            var component=new ArrayList<String>();work.push(order.get(i));
            while(!work.isEmpty()) {
                var id=work.pop();component.add(id);
                for(var caller:reverse.getOrDefault(id,List.of()))if(seen.add(caller))work.push(caller);
            }
            if(component.size()>1||graph.get(component.getFirst()).contains(component.getFirst()))recursive.addAll(component);
        }
        return recursive;
    }
    boolean recursiveUndefined(){return recursiveUndefined;}
    boolean guarded(Binding binding){return guarded.contains(binding.id());}
    Optional<Key> key(Binding binding){return Optional.ofNullable(keys.get(binding.id()));}
    List<String> proofs(Binding binding){var key=keys.get(binding.id());return key==null?binding.proofs():proofs.get(key);}
}
