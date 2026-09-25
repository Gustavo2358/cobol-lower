package io.github.gustavo2358.lower.application;

import java.util.*;
import static io.github.gustavo2358.lower.domain.SpInput.*;

/** Local, nonexecutable assessment over published topology. Source unit is an ownership
 * boundary, never a runtime logical-level identity. Empty states mean not reached. */
public record HandlerStateAnalysis(UnitKey unit, List<Target> targets, List<Operation> operations,
        List<Event> events, List<Node> nodes, List<Derivation> derivations, List<Frontier> frontiers,
        Metrics metrics, List<Selection> selections) {
    public HandlerStateAnalysis(UnitKey unit,List<Target> targets,List<Operation> operations,List<Event> events,
            List<Node> nodes,List<Derivation> derivations,List<Frontier> frontiers,Metrics metrics) {
        this(unit,targets,operations,events,nodes,derivations,frontiers,metrics,List.of());
    }
    /** Source-only selection; premises remain open runtime conditions, not facts of failure. */
    public record Selection(String event,Node source,String origin,List<String> premises,
            Optional<String> target,Optional<Support> stateOnEntry,Optional<Node> localEntry,
            boolean unknownLocalRemainder,boolean localInactivePossible,boolean outerLevelRemainder,
            boolean bypassed,List<String> proofs) {
        public Selection { premises=List.copyOf(premises);proofs=List.copyOf(proofs); }
    }
    public HandlerStateAnalysis {
        selections=List.copyOf(selections);
        targets=List.copyOf(targets); operations=List.copyOf(operations); events=List.copyOf(events);
        nodes=List.copyOf(nodes); derivations=List.copyOf(derivations); frontiers=List.copyOf(frontiers);
    }
    public enum Kind { ENTRY_UNKNOWN, ACTIVE, CANCELED, DEACTIVATED, CANCELED_UNKNOWN, UNKNOWN }
    public enum Cause { NONE, RESET_HISTORY_UNAVAILABLE, RESET_WITHOUT_CANCELED_EVIDENCE,
        CALL_EFFECT_UNAVAILABLE, HANDLER_OPERATION_UNAVAILABLE }
    public enum TargetForm { LABEL_LOCAL, LABEL_UNRESOLVED, PROGRAM_LITERAL, PROGRAM_DATA, PROGRAM_UNRESOLVED }
    public record State(Kind kind, String target, Cause cause) {
        public State {
            Objects.requireNonNull(kind); Objects.requireNonNull(target); Objects.requireNonNull(cause);
            if((kind==Kind.ACTIVE||kind==Kind.CANCELED||kind==Kind.DEACTIVATED)!=!target.isEmpty())throw new IllegalArgumentException("correlated target state");
            if((kind==Kind.UNKNOWN)!=(cause!=Cause.NONE))throw new IllegalArgumentException("localized unknown cause");
        }
    }
    /** Single reaching definition, not an execution history. Joining two definitions
     * of the same target keeps one semantic target and distinct causal supports. */
    public record Support(State state, Optional<StatementId> activation) {
        public Support { Objects.requireNonNull(state);Objects.requireNonNull(activation);
            if(!state.target().isEmpty()!=activation.isPresent())throw new IllegalArgumentException("activation support"); }
    }
    public record Target(String id, TargetForm form, Optional<CicsHandlerLabelTarget> label,
            Optional<StatementId> entry, Optional<Provenance> entryOrigin, Optional<CallTarget> program,
            List<Registration> registrations) {
        public Target { registrations=List.copyOf(registrations); }
    }
    public record Registration(StatementId statement, Provenance statementOrigin, Provenance operandOrigin) { }
    public record Operation(StatementId statement, CicsHandlerAction action, List<Support> before,
            List<Support> afterSuccessfulCompletion) {
        public Operation { before=List.copyOf(before);afterSuccessfulCompletion=List.copyOf(afterSuccessfulCompletion); }
    }
    public enum EventStatus { NOT_REACHED_IN_PUBLISHED_TOPOLOGY, ASSESSED, ASSESSED_WITH_CONDITIONAL_INGRESS, ELIGIBILITY_UNAVAILABLE }
    public record Candidate(String target, List<StatementId> activations) {
        public Candidate { activations=List.copyOf(activations); }
    }
    public record Event(StatementId statement, CicsAbendEligibility eligibility, EventStatus status,
            List<Support> before, List<Candidate> candidates, Optional<String> definiteTarget,
            boolean unknownLocalRemainder, boolean localInactivePossible, boolean outerLevelRemainder,
            boolean bypassed) {
        public Event { before=List.copyOf(before);candidates=List.copyOf(candidates); }
    }
    /** Context is ROOT or a published binding plus one finite input Support. */
    public record Node(String context, String location, Support support) { }
    public record Derivation(Optional<Node> source, Node destination, Optional<Node> callerPremise,
            String authority, List<String> proofs) {
        public Derivation { proofs=List.copyOf(proofs); }
    }
    public record Frontier(Node source, String authority, String reference, List<String> proofs) {
        public Frontier { proofs=List.copyOf(proofs); }
    }
    public record Metrics(long topologyOccurrences, long topologyTransitions, long handlerOperations,
            long semanticTargets, long abstractStates, long contexts, long worklistPops,
            long stateInsertions, long joinAttempts, long maxSupportsPerPoint) { }
}
