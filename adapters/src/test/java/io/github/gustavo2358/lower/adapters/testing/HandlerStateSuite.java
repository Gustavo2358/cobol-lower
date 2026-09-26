package io.github.gustavo2358.lower.adapters.testing;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.node.*;
import io.github.gustavo2358.lower.application.*;
import io.github.gustavo2358.lower.adapters.sp.SpJsonDecoder;
import io.github.gustavo2358.lower.adapters.cli.CobolLower;
import io.github.gustavo2358.lower.domain.SpInput;
import java.util.*;
import static io.github.gustavo2358.lower.domain.SpInput.*;
import static io.github.gustavo2358.lower.application.HandlerStateAnalysis.*;

/** IBM rule -> frontend bytes -> typed input -> causal state quality (no AIR). */
public final class HandlerStateSuite {
    private static final ObjectMapper JSON=new ObjectMapper();
    private static int checks;
    private static final List<String> failures=new ArrayList<>();
    private record Run(SpInput input,HandlerStateAnalysis analysis) { }
    private HandlerStateSuite() { }
    private static void need(boolean value,String message) {checks++;if(!value)failures.add(message);}
    private static ObjectNode wire(String name) throws Exception {
        try(var in=HandlerStateSuite.class.getResourceAsStream("/cics-handler-state-r4/"+name+".json")) {
            if(in==null)throw new AssertionError("fixture "+name);return (ObjectNode)JSON.readTree(in);
        }
    }
    private static Run run(String name) throws Exception {return run(wire(name));}
    private static Run run(ObjectNode wire) throws Exception {
        var decoded=new SpJsonDecoder(CobolLower.INPUT_LIMITS).decode(JSON.writeValueAsBytes(wire));
        if(!(decoded instanceof SpJsonDecoder.Decoded d))throw new AssertionError("fixture decode "+decoded);
        var result=new CobolLowerer().lower(d.input(),CobolLower.OPTIONS);
        need(result.status()==LoweringResult.Status.IMPLEMENTATION_LIMIT,"valid input stays NOT_READY "+result.status());
        need(result.publication().isEmpty(),"no AIR publication");
        need(result.admission().input().orElseThrow().equals(d.input()),"typed input preserved");
        var analysis=result.admission().handlerState().orElseThrow();
        var reverse=HandlerStateScheduleProbe.reverse(d.input());
        need(analysis.equals(reverse),"FIFO/LIFO identical including deterministic counters");
        need(analysis.metrics().worklistPops()==analysis.nodes().size(),"one pop per inserted finite fact");
        var nodes=Set.copyOf(analysis.nodes());
        for(var edge:analysis.derivations())need(nodes.contains(edge.destination())&&edge.source().map(nodes::contains).orElse(true)
            &&edge.callerPremise().map(nodes::contains).orElse(true),"causal references belong to reached facts");
        for(var event:analysis.events()) {
            need(analysis.derivations().stream().noneMatch(e->e.source().filter(n->n.location().equals(event.statement().handle())).isPresent()),"ABEND emits no continuation");
            for(var c:event.candidates()) {
                need(!c.activations().isEmpty(),"candidate has positive activation provenance");
                for(var id:c.activations())need(analysis.operations().stream().anyMatch(o->o.statement().equals(id)&&!o.afterSuccessfulCompletion().isEmpty()),"candidate activation actually reached and completed");
            }
        }
        return new Run(d.input(),analysis);
    }
    private static Event event(Run run,int index) {return run.analysis().events().get(index);}
    private static Set<String> candidates(Event event) {return new TreeSet<>(event.candidates().stream().map(Candidate::target).toList());}
    private static String target(Run run,String statement) {
        var h=(CicsHandlerFact)run.input().statements().stream().filter(s->s.header().id().handle().equals(statement)).findFirst().orElseThrow();
        return "LABEL/"+h.labelTarget().orElseThrow().id().handle();
    }
    private static void expected(Run run,int eventIndex,Set<String> required,boolean unknown,boolean inactive,boolean outer,boolean bypass) {
        var e=event(run,eventIndex);
        need(e.status()==EventStatus.ASSESSED,"reachable event");need(candidates(e).equals(required),"required/forbidden candidates: "+required+" got "+candidates(e));
        need(e.unknownLocalRemainder()==unknown,"honest unknown remainder");need(e.localInactivePossible()==inactive,"local inactivity");
        need(e.outerLevelRemainder()==outer,"outer-level remainder");need(e.bypassed()==bypass,"event bypass");
    }
    private static void sameSemanticEvent(Event a,Event b,String why) {
        need(candidates(a).equals(candidates(b))&&a.unknownLocalRemainder()==b.unknownLocalRemainder()
            &&a.localInactivePossible()==b.localInactivePossible()&&a.outerLevelRemainder()==b.outerLevelRemainder()&&a.bypassed()==b.bypassed(),why);
    }
    private static ObjectNode fact(ObjectNode wire,String id) {
        for(var s:wire.path("statements"))if(s.path("header").path("id").asText().equals(id))return (ObjectNode)s;
        throw new AssertionError(id);
    }
    public static void main(String[] args) throws Exception {
        checks=0;failures.clear();
        var replacement=run("replacement");expected(replacement,0,Set.of(target(replacement,"statement:1")),false,false,false,false);
        need(event(replacement,0).definiteTarget().equals(Optional.of(target(replacement,"statement:1"))),"replacement definite B");
        var cancel=run("cancel");expected(cancel,0,Set.of(),false,true,true,false);
        need(event(cancel,0).before().stream().allMatch(s->s.state().kind()==Kind.CANCELED),"canceled correlation retained");
        var reset=run("reset");expected(reset,0,Set.of(target(reset,"statement:0")),false,false,false,false);
        need(event(reset,0).candidates().getFirst().activations().getFirst().handle().equals("statement:0"),"RESET restores causal activation");
        var newReset=run("cancel-new-reset");expected(newReset,0,Set.of(),true,true,true,false);
        need(event(newReset,0).before().getFirst().state().cause()==Cause.RESET_WITHOUT_CANCELED_EVIDENCE,"undocumented active RESET is localized uncertainty");
        var branch=run("branch");expected(branch,0,Set.of(target(branch,"statement:1"),target(branch,"statement:2")),false,false,false,false);
        need(event(branch,0).definiteTarget().isEmpty(),"join never selects arbitrary winner");
        var knownUnknown=run("known-unknown");expected(knownUnknown,0,Set.of(target(knownUnknown,"statement:1")),true,true,true,false);
        var loop=run("loop");expected(loop,0,Set.of(target(loop,"statement:0"),target(loop,"statement:2")),false,false,false,false);
        need(loop.analysis().metrics().worklistPops()<100,"loop finite independent of iterations");
        var duplicate=run("duplicate");expected(duplicate,0,Set.of(target(duplicate,"statement:1")),false,false,false,false);
        need(duplicate.analysis().targets().size()==1,"duplicate operations do not duplicate semantic LABEL identity");
        need(event(duplicate,0).candidates().getFirst().activations().stream().map(StatementId::handle).toList().equals(List.of("statement:1")),"duplicate replacement kills previous reaching definition");
        var bypass=run("bypass");expected(bypass,0,Set.of(),false,false,false,true);
        need(event(bypass,0).before().getFirst().state().kind()==Kind.ACTIVE,"bypass preserves pre-event state");
        var initial=run("entry-unknown");expected(initial,0,Set.of(),true,true,true,false);
        need(event(initial,0).before().getFirst().state().kind()==Kind.ENTRY_UNKNOWN,"initial state is never NONE");
        expected(run("unknown-cancel"),0,Set.of(),false,true,true,false);
        expected(run("unknown-cancel-reset"),0,Set.of(),true,true,true,false);
        expected(run("unknown-reset"),0,Set.of(),true,true,true,false);
        var unreachable=run("unreachable");expected(unreachable,0,Set.of(),true,true,true,false);
        need(unreachable.analysis().operations().stream().allMatch(o->o.before().isEmpty()),"dead activation never analyzed as executed");
        for(var fixture:List.of("program-literal","program-data","unresolved","unavailable-handler")) {
            var r=run(fixture);need(event(r,0).status()==EventStatus.NOT_REACHED_IN_PUBLISHED_TOPOLOGY,"no successor inferred for "+fixture);
            need(r.analysis().operations().getFirst().afterSuccessfulCompletion().isEmpty(),"operation has no successful proof "+fixture);
            need(event(r,0).candidates().isEmpty(),"no target fanout");need(!r.analysis().frontiers().isEmpty(),"frontier retained");
        }
        need(run("program-literal").analysis().targets().getFirst().form()==TargetForm.PROGRAM_LITERAL,"typed PROGRAM literal retained");
        need(run("program-data").analysis().targets().getFirst().program().orElseThrow() instanceof DataCallTarget,"typed PROGRAM DATA retained without runtime name");
        need(event(run("unsupported-event"),0).status()==EventStatus.ELIGIBILITY_UNAVAILABLE,"unsupported event eligibility not invented");
        expected(run("call-unknown"),0,Set.of(),true,true,true,false);
        var link=run("link-preserved");expected(link,0,Set.of(target(link,"statement:0")),false,false,false,false);
        for(var fixture:List.of("cross-return","nested-return")) {
            var r=run(fixture);expected(r,0,Set.of(target(r,"statement:1")),false,false,false,false);
            expected(r,1,Set.of(target(r,"statement:4")),false,false,false,false);
            need(r.analysis().derivations().stream().anyMatch(d->d.callerPremise().isPresent()),"matched return carries caller premise");
        }
        var recursive=run("recursive");expected(recursive,0,Set.of(target(recursive,"statement:3")),false,false,false,false);
        need(recursive.analysis().metrics().contexts()<10,"recursive contexts finite");
        var copy=run("copy");expected(copy,0,Set.of(target(copy,"statement:0")),false,false,false,false);
        need(!copy.analysis().targets().getFirst().registrations().getFirst().operandOrigin().includeChain().isEmpty(),"COPY provenance retained");
        // Metamorphics M1/M8: source inventory and ordinal are not execution authority.
        var permuted=wire("unreachable");var inventory=(ArrayNode)permuted.path("statements");var reversed=new ArrayList<JsonNode>();inventory.forEach(reversed::add);Collections.reverse(reversed);inventory.removeAll();reversed.forEach(inventory::add);
        for(int i=0;i<inventory.size();i++)((ObjectNode)inventory.get(i).path("header")).put("programPoint",i);
        need(run(permuted).analysis().equals(unreachable.analysis()),"M1/M8 inventory/programPoint permutation");
        sameSemanticEvent(event(unreachable,0),event(run("dead-extra"),0),"M2 dead activation addition");
        var changed=run("replacement-c");expected(changed,0,Set.of(target(changed,"statement:1")),false,false,false,false);
        need(changed.analysis().operations().getFirst().afterSuccessfulCompletion().getFirst().state().equals(replacement.analysis().operations().getFirst().afterSuccessfulCompletion().getFirst().state()),"M3 replacement only alters causal suffix");
        var branchCancel=run("branch-cancel");expected(branchCancel,0,Set.of(target(branchCancel,"statement:3")),false,true,true,false);
        sameSemanticEvent(event(branch,0),event(run("branch-swapped"),0),"M6 physical arm swap preserves candidate set");
        need(candidates(event(knownUnknown,0)).contains(target(knownUnknown,"statement:1"))&&event(knownUnknown,0).unknownLocalRemainder(),"M9 unknown predecessor retains known candidate");
        // M5: predecessor relation, not nearest textual HANDLE, is the only RESET authority.
        var resetPerm=wire("reset");fact(resetPerm,"statement:0").withObject("header").put("programPoint",2);fact(resetPerm,"statement:2").withObject("header").put("programPoint",0);
        need(run(resetPerm).analysis().equals(reset.analysis()),"M5 RESET independent of textual ordinal");
        // M10: mutate only the typed event (wire surface remains consistent), preserve topology.
        var eligible=wire("bypass");var ev=fact(eligible,"statement:1");ev.put("dispatchEligibility","HANDLER_ELIGIBLE");ev.put("rawText","EXEC CICS ABEND END-EXEC");ev.set("options",JSON.createArrayNode());
        var eligibleResult=run(eligible);expected(eligibleResult,0,Set.of(target(eligibleResult,"statement:0")),false,false,false,false);
        need(event(eligibleResult,0).before().equals(event(bypass,0).before()),"M10 eligibility changes assessment only");
        need(event(run("no-return"),0).status()==EventStatus.NOT_REACHED_IN_PUBLISHED_TOPOLOGY,"recursive call cannot fabricate a return");
        run("perform-times");
        // M7: independent frontend unit products, same spelling and ordinal identities.
        var first=run("two-active-units");var second=run("two-active-units-second");
        need(!first.input().unit().equals(second.input().unit()),"units distinct");
        need(!first.analysis().targets().getFirst().label().equals(second.analysis().targets().getFirst().label()),"M7 target identity includes owning unit");
        var noRegistration=run("two-units-second");expected(noRegistration,0,Set.of(),true,true,true,false);
        // R3-R1: invalid input is rejected without running state analysis.
        try(var in=HandlerStateSuite.class.getResourceAsStream("/sp/validation-before-readiness-r7-r3-r1/both.json")) {
            var invalid=(ObjectNode)JSON.readTree(in);
            for(var s:invalid.path("statements"))if(s.path("variant").asText().equals("CALL"))((ObjectNode)s.path("normalContinuation")).put("statement","statement:999999");
            var sp=((SpJsonDecoder.Decoded)new SpJsonDecoder(CobolLower.INPUT_LIMITS).decode(JSON.writeValueAsBytes(invalid))).input();
            var out=new CobolLowerer().lower(sp,CobolLower.OPTIONS);
            need(out.status()==LoweringResult.Status.INVALID_INPUT&&out.admission().handlerState().isEmpty(),"invalid facts dominate state analysis");
        }
        if(!failures.isEmpty())throw new AssertionError(String.join("\n",failures));
        System.out.println("R7_R4_HANDLER_STATE_CHECKS="+checks+" METAMORPHICS=10/10");
    }
}
