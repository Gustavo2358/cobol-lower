package io.github.gustavo2358.lower.application;
import java.util.*;
import io.github.gustavo2358.lower.domain.SpInput;
import io.github.gustavo2358.lower.domain.ControlTopology;
import static io.github.gustavo2358.lower.application.HandlerStateAnalysis.*;
/** Finite reaching registrations over published event-relevant keys. No COBOL parsing. */
final class ConditionExecutionState {
    record Choice(ControlTopology.Target target,Optional<ConditionState> registration,List<String> proofs) { }
    record Selection(List<Choice> choices,boolean defaultPossible) {Selection{choices=List.copyOf(choices);}}
    private final SpInput input;
    private final Map<String,List<ControlTopology.ConditionRegistration>> updates=new HashMap<>();
    private final Map<String,ControlTopology.ConditionRegistration> definitions=new HashMap<>();
    private final Map<String,List<ControlTopology.ConditionEvent>> events=new HashMap<>();
    private final Map<String,ControlTopology.ConditionEvent> defaults=new HashMap<>();
    ConditionExecutionState(SpInput input) {
        this.input=input;var topology=input.controlTopology().orElseThrow();var relevant=new HashSet<String>();
        for(var event:topology.conditionEvents()){relevant.add(event.condition());relevant.add("ERROR");events.computeIfAbsent(event.statement(),k->new ArrayList<>()).add(event);if(!event.defaultEvent().isEmpty())defaults.put(event.defaultEvent(),event);}
        for(var registration:topology.conditionRegistrations())if(relevant.contains(registration.condition())) {
            updates.computeIfAbsent(registration.statement(),k->new ArrayList<>()).add(registration);definitions.put(registration.identity(),registration);
        }
    }
    Support update(String statement,Support predecessor) {
        if(!updates.containsKey(statement))return predecessor;
        var state=new TreeMap<String,ConditionState>();predecessor.conditions().forEach(c->state.put(c.condition(),c));
        for(var update:updates.get(statement))state.put(update.condition(),new ConditionState(update.condition(),new SpInput.StatementId(input.unit(),statement),false,update.proofs()));
        return predecessor.withConditions(List.copyOf(state.values()));
    }
    static Support open(Support predecessor){return predecessor.withConditions(predecessor.conditions().stream().map(c->new ConditionState(c.condition(),c.registration(),true,c.proofs())).toList());}
    List<ControlTopology.ConditionEvent> events(String statement){return events.getOrDefault(statement,List.of());}
    boolean defaultPossible(String event,Support support){var condition=defaults.get(event);return condition==null||select(condition,support).defaultPossible();}
    Selection select(ControlTopology.ConditionEvent event,Support support) {
        var choices=new ArrayList<Choice>();
        if(event.eligibility()==ControlTopology.EventEligibility.HANDLERS_BYPASSED)return new Selection(List.of(new Choice(event.continuation(),Optional.empty(),event.proofs())),false);
        var specific=support.conditions().stream().filter(c->c.condition().equals(event.condition())).findFirst();
        if(specific.isPresent()) {
            var state=specific.orElseThrow();var definition=definition(state);add(choices,event,state,definition);
            if(!state.uncertain())return new Selection(choices,definition.action()==ControlTopology.ConditionAction.DEFAULT);
        }
        // No proof of a specific disposition. ERROR is an admitted possible fallback;
        // the initial/foreign state keeps the default-disposition remainder open.
        support.conditions().stream().filter(c->c.condition().equals("ERROR")).findFirst().ifPresent(state->add(choices,event,state,definition(state)));
        return new Selection(choices,true);
    }
    private ControlTopology.ConditionRegistration definition(ConditionState state) {
        return Objects.requireNonNull(definitions.get(state.registration().handle()+"/"+state.condition()),"condition support is a published definition");
    }
    private static void add(List<Choice> choices,ControlTopology.ConditionEvent event,ConditionState state,ControlTopology.ConditionRegistration definition) {
        var proofs=new TreeSet<String>(event.proofs());proofs.addAll(definition.proofs());
        switch(definition.action()) {
            case LABEL -> {var target=definition.target().getFirst();proofs.addAll(target.proofs());choices.add(new Choice(target,Optional.of(state),List.copyOf(proofs)));}
            case IGNORE -> choices.add(new Choice(event.continuation(),Optional.of(state),List.copyOf(proofs)));
            case DEFAULT -> { }
        }
    }
}
