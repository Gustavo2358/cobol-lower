package io.github.gustavo2358.lower.application;

import io.github.gustavo2358.lower.domain.SpInput;
import io.github.gustavo2358.lower.domain.ControlTopology;
import java.util.*;
import static io.github.gustavo2358.lower.application.HandlerStateAnalysis.*;

/** Executable specialization reuses the validated source state algebra. It never
 * unions selections from different reaching definitions or interprets COBOL text. */
final class CicsExecutionState {
    private record Key(String event,Support support) { }
    private final HandlerStateAnalyzer transfer;
    final ConditionExecutionState conditions;
    private final Map<String,List<ControlTopology.ExceptionalEvent>> events=new HashMap<>();
    private final Map<Key,Selection> selections=new HashMap<>();
    CicsExecutionState(SpInput input,HandlerStateAnalysis assessment) {
        transfer=new HandlerStateAnalyzer(input);conditions=new ConditionExecutionState(input);
        input.controlTopology().orElseThrow().exceptionalEvents().forEach(e->events.computeIfAbsent(e.statement(),k->new ArrayList<>()).add(e));
        // Source contexts with the same support have the same state selection.
        // Their callerPremise/return relations are deliberately not projected.
        assessment.selections().forEach(s->selections.putIfAbsent(new Key(s.event(),s.source().support()),s));
    }
    static Support initial(){return new Support(new State(Kind.ENTRY_UNKNOWN,"",Cause.NONE),Optional.empty());}
    static String key(Support s){return HandlerStateAnalyzer.supportKey(s);}
    Support after(SpInput.StatementFact fact,Support support) {
        if(fact instanceof SpInput.CicsHandlerFact h)return transfer.transfer(h,support);
        if(fact instanceof SpInput.CallFact)return ConditionExecutionState.open(new Support(new State(Kind.UNKNOWN,"",Cause.CALL_EFFECT_UNAVAILABLE),Optional.empty(),support.conditions()));
        return conditions.update(fact.header().id().handle(),support);
    }
    List<ControlTopology.ExceptionalEvent> events(String statement){return events.getOrDefault(statement,List.of());}
    Optional<Selection> selection(ControlTopology.ExceptionalEvent event,Support support){return Optional.ofNullable(selections.get(new Key(event.id(),support)));}
}
