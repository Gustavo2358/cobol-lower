package io.github.gustavo2358.lower.adapters.testing;

import java.util.*;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.node.*;
import io.github.gustavo2358.lower.application.*;
import io.github.gustavo2358.lower.adapters.sp.*;
import io.github.gustavo2358.lower.adapters.cli.CobolLower;
import io.github.gustavo2358.lower.domain.SpInput;
import io.github.gustavo2358.air.model.*;
import io.github.gustavo2358.air.json.AirJson;
import static io.github.gustavo2358.lower.application.HandlerStateAnalysis.*;

/** SP2.45 real frontend fixtures; source exceptional entry is not executable AIR. */
public final class ExceptionalHandlerSuite {
    static final ObjectMapper J=new ObjectMapper(); static int checks,mutations,metamorphics;
    static void need(boolean b,String m){checks++;if(!b)throw new AssertionError(m);}
    static ObjectNode wire(String name)throws Exception {try(var in=ExceptionalHandlerSuite.class.getResourceAsStream("/cics-exceptional-r7/"+name+".json")){return (ObjectNode)J.readTree(Objects.requireNonNull(in,name));}}
    record Run(SpInput input,LoweringResult result,HandlerStateAnalysis a){}
    static Run run(String name)throws Exception{return run(wire(name));}
    static Run run(ObjectNode wire)throws Exception{
        var d=new SpJsonDecoder(CobolLower.INPUT_LIMITS).decode(J.writeValueAsBytes(wire));need(d instanceof SpJsonDecoder.Decoded,"decode "+d);
        var in=((SpJsonDecoder.Decoded)d).input();var r=new CobolLowerer().lower(in,CobolLower.POSITIVE_OPTIONS);
        need(r.status()==LoweringResult.Status.BOUNDED_PUBLICATION,"bounded publication "+r.status()+" "+r.admission().diagnostics());
        need(r.admission().input().orElseThrow().equals(in),"no fact deletion");
        var a=r.admission().handlerState().orElseThrow();need(a.equals(HandlerStateScheduleProbe.reverse(in)),"worklist schedule independence");
        need(a.metrics().worklistPops()==a.nodes().size(),"finite facts, no histories");
        var nodes=Set.copyOf(a.nodes());
        for(var x:a.derivations()){need(nodes.contains(x.destination()),"destination reached");need(x.source().map(nodes::contains).orElse(true),"positive predecessor");}
        for(var selection:a.selections())if(selection.localEntry().isPresent()){
            need(selection.source().support().state().kind()==Kind.ACTIVE,"only active positive handler selected");
            need(selection.stateOnEntry().orElseThrow().state().kind()==Kind.DEACTIVATED,"deactivated before executing exit");
            need(selection.localEntry().orElseThrow().support().equals(selection.stateOnEntry().orElseThrow()),"entry state correlated");
            need(!selection.proofs().isEmpty(),"selection event proof");
            need(selection.source().support().activation().isPresent(),"selection activation support");
        }
        var p=r.publication().orElseThrow();need(r.validation().orElseThrow().isStructurallyValid(),"strict AIR");new AirJson().decode(new AirJson().encode(p));
        for(var cap:r.admission().nonExecutableCapabilities())for(var link:r.statements().stream().filter(x->x.source().equals(cap.statement())).toList()){
            var t=p.units().stream().flatMap(u->u.sequences().stream()).map(Sequence::terminator).filter(x->x.header().id().equals(link.target())).findFirst().orElseThrow();
            need(t instanceof Operations.Opaque,"NOT_READY remains frontier");var o=(Operations.Opaque)t;
            need(o.envelope().control().known().isEmpty(),"source ingress cannot become executable successor");
            need(o.envelope().memory().knownWrites().isEmpty(),"no fabricated effects");
        }
        var old=new CobolLowerer().lower(in,CobolLower.OPTIONS);need(old.status()==LoweringResult.Status.IMPLEMENTATION_LIMIT&&old.publication().isEmpty(),"historical opt-in boundary");
        return new Run(in,r,a);
    }
    static List<Selection> entries(Run r){return r.a().selections().stream().filter(s->s.localEntry().isPresent()).toList();}
    static Event last(Run r){return r.a().events().getLast();}
    static void reject(ObjectNode wire,String why)throws Exception{
        var d=new SpJsonDecoder(CobolLower.INPUT_LIMITS).decode(J.writeValueAsBytes(wire));
        need(d instanceof SpJsonDecoder.Rejected||new CobolLowerer().lower(((SpJsonDecoder.Decoded)d).input(),CobolLower.POSITIVE_OPTIONS).status()==LoweringResult.Status.INVALID_INPUT,why);mutations++;
    }
    public static void main(String[] args)throws Exception{
        checks=0;mutations=0;metamorphics=0;
        var explicit=run("explicit");need(entries(explicit).size()==1,"one explicit ingress");need(last(explicit).status()==EventStatus.ASSESSED,"handler ABEND reached");
        need(last(explicit).candidates().isEmpty()&&last(explicit).localInactivePossible()&&last(explicit).outerLevelRemainder(),"CANCEL local, outer unknown");
        var def=run("default");need(entries(def).size()==1,"qualified XCTL condition ingress");need(entries(def).getFirst().origin().equals("XCTL_PGMIDERR"),"not synthetic explicit ABEND");
        need(entries(def).getFirst().premises().equals(List.of("CONDITION_RAISED","DEFAULT_DISPOSITION_APPLIES")),"open runtime premises retained");
        need(last(def).status()==EventStatus.ASSESSED_WITH_CONDITIONAL_INGRESS,"honest bounded reachability");
        need(!last(def).unknownLocalRemainder()&&last(def).localInactivePossible()&&last(def).outerLevelRemainder(),"inactive is not absent globally");
        for(var name:List.of("resp","nohandle","bypass","cancel","entry-unknown","registration","dead","opaque","return","link","normal-perform","unsupported-xctl")){
            var r=run(name);need(entries(r).isEmpty(),"no fabricated ingress "+name);
            if(!name.equals("entry-unknown"))need(last(r).status()==EventStatus.NOT_REACHED_IN_PUBLISHED_TOPOLOGY,"unreached is not proven absence "+name);
        }
        var inactive=run("deactivated");need(entries(inactive).size()==1,"no recursive self entry");need(last(inactive).before().stream().allMatch(s->s.state().kind()==Kind.DEACTIVATED),"CICS deactivation distinct from CANCEL");
        need(last(inactive).candidates().isEmpty()&&last(inactive).outerLevelRemainder(),"deactivated exit cannot select itself");
        var reset=run("reset");need(entries(reset).stream().map(Selection::event).distinct().count()==2,"RESET restores deactivated registration, finite repeated entry");need(reset.a().metrics().worklistPops()<100,"finite RESET cycle");
        var replacement=run("replacement");need(entries(replacement).size()==1,"replacement does not union old target");need(entries(replacement).getFirst().source().support().activation().orElseThrow().equals(replacement.a().operations().get(1).statement()),"latest activation selected");
        var nested=run("nested-perform");need(entries(nested).size()==1,"fault inside PERFORM enters once");need(last(nested).status()==EventStatus.ASSESSED,"handler nested PERFORM resumes handler");
        need(nested.a().derivations().stream().anyMatch(d->d.callerPremise().isPresent()&&d.destination().context().startsWith("HANDLER/")),"matched handler caller resume");
        var completion=run("completion-bound");need(last(completion).status()==EventStatus.NOT_REACHED_IN_PUBLISHED_TOPOLOGY,"unproved restored completion cannot fall into adjacent code");
        need(completion.a().frontiers().stream().anyMatch(x->x.reference().startsWith("HANDLER_COMPLETION_CONTEXT_UNAVAILABLE")),"completion uncertainty localized");
        need(entries(run("resp2")).size()==1,"RESP2 alone not suppression");metamorphics++;
        need(entries(run("renamed")).size()==1,"irrelevant target rename preserves conditional entry");metamorphics++;
        need(run("storage").a().metrics().worklistPops()==def.a().metrics().worklistPops(),"independent storage cannot remove CONTROL");metamorphics++;
        need(entries(run("bounded-command")).size()==1,"additional NOT_READY does not erase source facts");metamorphics++;
        need(entries(run("handler-branch")).stream().map(Selection::event).distinct().count()==2,"RESET branch preserves correlated active and deactivated alternatives");metamorphics++;
        var perm=wire("default");for(var field:List.of("statements")){var arr=(ArrayNode)perm.path(field);var xs=new ArrayList<JsonNode>();arr.forEach(xs::add);Collections.reverse(xs);arr.removeAll();xs.forEach(arr::add);}
        for(var st:perm.path("statements"))((ObjectNode)st.path("header")).put("programPoint",1000-st.path("header").path("programPoint").asInt());
        need(run(perm).a().equals(def.a()),"inventory/programPoint not control authority");metamorphics++;
        var noAuthority=wire("default");noAuthority.put("contractVersion","2.44.0");noAuthority.withObject("controlTopology").remove("exceptionalEvents");
        need(entries(run(noAuthority)).isEmpty(),"no event authority means no ingress even with known registration");metamorphics++;
        need(entries(run("default-dead")).stream().allMatch(x->x.origin().equals("XCTL_PGMIDERR")),"dead ABEND never fires");metamorphics++;
        need(entries(run("copy")).size()==1,"COPY source event preserves selection");metamorphics++;
        var reordered=wire("default");for(var field:List.of("occurrences","regions","boundaries","outcomes","bindings","proofs","exceptionalEvents")) {
            var arr=(ArrayNode)reordered.path("controlTopology").path(field);var xs=new ArrayList<JsonNode>();arr.forEach(xs::add);Collections.reverse(xs);arr.removeAll();xs.forEach(arr::add);
        }need(run(reordered).a().equals(def.a()),"topology inventory permutation");metamorphics++;
        var ev=(ObjectNode)wire("default").path("controlTopology").path("exceptionalEvents").get(0); // wire negatives separate from real fixtures
        for(var version:List.of("2.40.0","2.41.0","2.42.0","2.43.0","2.44.0")){var w=wire("default");w.put("contractVersion",version);reject(w,"old profile rejects exceptional authority");}
        for(var version:List.of("2.46.0","9.99","unknown")){var w=wire("default");w.put("contractVersion",version);w.withObject("controlTopology").put("exceptionalEvents",false);var r=new SpJsonDecoder(CobolLower.INPUT_LIMITS).decode(J.writeValueAsBytes(w));need(r instanceof SpJsonDecoder.Rejected x&&x.diagnostic().code()==SpJsonDecoder.Code.UNSUPPORTED_CONTRACT,"version before malformed semantic shape");mutations++;}
        for(var field:List.of("premises","scope","eligibility","statement","proofs")){
            var w=wire("default");var x=(ObjectNode)w.path("controlTopology").path("exceptionalEvents").get(0);
            if(field.equals("premises")||field.equals("proofs"))x.putArray(field);else x.put(field,"INVENTED");reject(w,"invalid event "+field);
        }
        var bad=wire("default");((ObjectNode)bad.path("controlTopology").path("exceptionalEvents").get(0)).put("handlerTarget","statement:0");reject(bad,"closed event DTO no target");
        var wrong=wire("default");var x=(ObjectNode)wrong.path("controlTopology").path("exceptionalEvents").get(0);x.put("statement",wrong.path("statements").get(0).path("header").path("id").asText());reject(wrong,"source fact must justify event");
        System.out.println("R7_EXCEPTIONAL_CHECKS="+checks+" METAMORPHICS="+metamorphics+" WIRE_MUTATIONS="+mutations);
    }
}
