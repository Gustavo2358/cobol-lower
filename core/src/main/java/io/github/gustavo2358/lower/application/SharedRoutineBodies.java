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
    SharedRoutineBodies(ControlTopology product,TopologyBinding topology,java.util.function.Predicate<String> stableState) {
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
        // A leaf-first fixed point admits exactly the acyclic, escape-free call closures.
        // Reverse users avoid rescanning the complete graph on every depth.
        var users=new HashMap<String,List<String>>();var outstanding=new HashMap<String,Integer>();
        dependencies.forEach((id,calls)->{outstanding.put(id,calls.size());calls.forEach(c->users.computeIfAbsent(c,k->new ArrayList<>()).add(id));});
        var ready=new ArrayDeque<String>();outstanding.forEach((id,n)->{if(n==0&&!blocked.contains(id))ready.add(id);});
        var admitted=new HashSet<String>();
        while(!ready.isEmpty()) {
            var id=ready.removeFirst();if(!admitted.add(id))continue;
            for(var user:users.getOrDefault(id,List.of()))if(outstanding.compute(user,(k,n)->n-1)==0&&!blocked.contains(user))ready.addLast(user);
        }
        keys.keySet().retainAll(admitted);
        var grouped=new HashMap<Key,Set<String>>();
        for(var binding:product.bindings())if(keys.containsKey(binding.id()))grouped.computeIfAbsent(keys.get(binding.id()),k->new TreeSet<>()).addAll(binding.proofs());
        grouped.forEach((key,value)->proofs.put(key,List.copyOf(value)));
    }
    Optional<Key> key(Binding binding){return Optional.ofNullable(keys.get(binding.id()));}
    List<String> proofs(Binding binding){var key=keys.get(binding.id());return key==null?binding.proofs():proofs.get(key);}
}
