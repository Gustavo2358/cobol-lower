package io.github.gustavo2358.lower.application;

import java.util.*;
import io.github.gustavo2358.lower.domain.ControlTopology;
import static io.github.gustavo2358.lower.application.HandlerStateAnalysis.*;

/** Reaching source hypotheses over published identities, never stack execution.
 * A prerequisite must complete on this path. Proven registrations kill the
 * affected possibilities before they reach a later condition event. */
final class ConditionRestorationState {
    private record Possibility(Set<String> killedBy) { }
    private final Map<String,Possibility> possibilities=new HashMap<>();
    private final Map<String,List<String>> introduced=new HashMap<>();
    private final Map<String,Set<String>> updates=new HashMap<>();

    ConditionRestorationState(ControlTopology topology) {
        var events=new HashMap<String,ControlTopology.ConditionEvent>();
        topology.conditionEvents().forEach(e->events.put(e.statement(),e));
        for(var registration:topology.conditionRegistrations())
            updates.computeIfAbsent(registration.statement(),k->new HashSet<>()).add(registration.condition());
        for(var hypothesis:topology.sourceContinuations()) {
            var event=events.get(hypothesis.statement());
            if(event==null||hypothesis.prerequisites().isEmpty())continue;
            var kills=new HashSet<String>();kills.add(event.condition());
            // A specific disposition suppresses every fallback. Replacing ERROR
            // alone cannot eliminate an independently possible specific handler
            // or an unknown restored IGNORE disposition (the continuation).
            boolean specific=topology.conditionRegistrations().stream()
                .filter(r->r.condition().equals(event.condition()))
                .flatMap(r->r.target().stream()).anyMatch(t->sameTarget(t,hypothesis.target()));
            if(!specific&&!sameTarget(event.continuation(),hypothesis.target()))kills.add("ERROR");
            possibilities.put(hypothesis.identity(),new Possibility(Set.copyOf(kills)));
            introduced.computeIfAbsent(hypothesis.prerequisites().getFirst(),k->new ArrayList<>()).add(hypothesis.identity());
        }
    }
    boolean governs(ControlTopology.SourceContinuation hypothesis) { return possibilities.containsKey(hypothesis.identity()); }
    boolean available(ControlTopology.SourceContinuation hypothesis,Support support) {
        return support.restorations().contains(hypothesis.identity());
    }
    Support complete(String statement,Support predecessor) {
        var writes=updates.getOrDefault(statement,Set.of());
        var additions=introduced.getOrDefault(statement,List.of());
        if(writes.isEmpty()&&additions.isEmpty())return predecessor;
        var remaining=new TreeSet<String>();
        for(var id:predecessor.restorations())
            if(Collections.disjoint(possibilities.get(id).killedBy(),writes))remaining.add(id);
        remaining.addAll(additions);
        return predecessor.withRestorations(List.copyOf(remaining));
    }
    private static boolean sameTarget(ControlTopology.Target a,ControlTopology.Target b) {
        return a.kind()==b.kind()&&a.reference().equals(b.reference());
    }
}
