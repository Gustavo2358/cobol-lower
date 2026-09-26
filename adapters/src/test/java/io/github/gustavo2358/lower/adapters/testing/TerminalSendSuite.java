package io.github.gustavo2358.lower.adapters.testing;

import java.util.*;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.node.*;
import io.github.gustavo2358.air.model.*;
import io.github.gustavo2358.air.json.AirJson;
import io.github.gustavo2358.lower.adapters.sp.SpJsonDecoder;
import io.github.gustavo2358.lower.adapters.cli.CobolLower;
import io.github.gustavo2358.lower.application.*;
import io.github.gustavo2358.lower.domain.*;
import static io.github.gustavo2358.lower.domain.SpInput.*;

/** Real frontend SP2.44, separate wire mutations, no source parsing in the consumer. */
public final class TerminalSendSuite {
    static final ObjectMapper J=new ObjectMapper();static int checks,mutations,metamorphics;
    static void need(boolean x,String why){checks++;if(!x)throw new AssertionError(why);}
    static ObjectNode wire(String name)throws Exception {try(var in=TerminalSendSuite.class.getResourceAsStream("/sp/terminal-send-r7-r7b/"+name+".json")){return (ObjectNode)J.readTree(Objects.requireNonNull(in,name));}}
    static SpJsonDecoder.Result decode(ObjectNode j)throws Exception{return new SpJsonDecoder(CobolLower.INPUT_LIMITS).decode(J.writeValueAsBytes(j));}
    static SpInput input(ObjectNode j)throws Exception{var x=decode(j);need(x instanceof SpJsonDecoder.Decoded,"SP decode "+x);return ((SpJsonDecoder.Decoded)x).input();}
    static CicsCommandFact command(SpInput input){return input.statements().stream().filter(CicsCommandFact.class::isInstance).map(CicsCommandFact.class::cast).findFirst().orElseThrow();}
    static ObjectNode command(ObjectNode j){for(var s:j.path("statements"))if(s.path("variant").asText().equals("CICS_COMMAND"))return (ObjectNode)s;throw new AssertionError("command absent");}
    static LoweringResult lower(ObjectNode j)throws Exception{return new CobolLowerer().lower(input(j),CobolLower.POSITIVE_OPTIONS);}
    static List<Operations.Opaque> sends(LoweringResult r){return r.publication().orElseThrow().units().stream().flatMap(u->u.sequences().stream()).map(Sequence::terminator).filter(Operations.Opaque.class::isInstance).map(Operations.Opaque.class::cast).filter(o->o.observedKind().equals("cics-terminal-send@1")).toList();}
    static Set<io.github.gustavo2358.air.model.Ids.LabelId> reached(Publication p) {
        var seqs=new HashMap<io.github.gustavo2358.air.model.Ids.LabelId,Sequence>();p.units().forEach(u->u.sequences().forEach(s->seqs.put(s.label(),s)));
        var seen=new HashSet<io.github.gustavo2358.air.model.Ids.LabelId>();var work=new ArrayDeque<io.github.gustavo2358.air.model.Ids.LabelId>();p.units().forEach(u->u.entries().forEach(e->e.initialLabel().ifPresent(work::add)));
        while(!work.isEmpty()){var at=work.removeFirst();if(!seen.add(at))continue;var t=seqs.get(at).terminator();
            if(t instanceof Operations.Jump x)work.add(x.destination());
            else if(t instanceof Operations.Invoke x)x.outcomes().known().forEach(o->{if(o instanceof Control.Normal n)work.add(n.label());});
            else if(t instanceof Operations.Opaque x)x.envelope().control().known().forEach(o->{if(o instanceof Control.JumpAlternative n)work.add(n.label());});
        }return seen;
    }
    static HandlerStateAnalysis state(ObjectNode j)throws Exception{return lower(j).admission().handlerState().orElseThrow();}
    static void reject(ObjectNode j,SpJsonDecoder.Code code)throws Exception{var x=decode(j);need(x instanceof SpJsonDecoder.Rejected r&&r.diagnostic().code()==code,"wire mutation expected "+code+" got "+x);mutations++;}
    public static void main(String[] args)throws Exception {
        checks=0;mutations=0;metamorphics=0;
        for(var name:List.of("from","literal","length-of","data-length","nohandle","erase","real-derived","standalone","renamed","copy","unresolved","bad-length","bad-option","duplicate","missing-from","truncated")) {
            var j=wire(name);var in=input(j);var c=command(in);need(c.commandKind()==CicsCommandKind.SEND_TERMINAL,"typed terminal family "+name);
            need(c.executableLowering()==ExecutableLowering.NOT_READY,"fact is not executable support");
            need(in.controlTopology().orElseThrow().equals(J.treeToValue(j.path("controlTopology"),ControlTopology.class)),"source topology transported exactly");
            need(in.factDependencies().orElseThrow().equals(J.treeToValue(j.path("factDependencies"),FactDependencies.class)),"FactDependencies inherited");
            var r=lower(j);need(r.status()==LoweringResult.Status.BOUNDED_PUBLICATION,"bounded publication "+name);need(r.validation().orElseThrow().isStructurallyValid(),"strict AIR "+name);
            need(!r.admission().nonExecutableCapabilities().isEmpty(),"explicit NOT_READY");need(r.admission().input().orElseThrow().equals(in),"typed fact retained");
            var again=lower(j);need(Arrays.equals(new AirJson().encode(r.publication().orElseThrow()),new AirJson().encode(again.publication().orElseThrow())),"AIR bytes deterministic "+name);
            need(sends(r).isEmpty(),"no experimental executable SEND retained");
            need(new CobolLowerer().lower(in,CobolLower.OPTIONS).status()==LoweringResult.Status.IMPLEMENTATION_LIMIT,"EXECUTABLE_ONLY preserved");
        }
        var standalone=wire("standalone");var in=input(standalone);var fact=command(in);var r=lower(standalone);
        var pub=r.publication().orElseThrow();need(!pub.units().getFirst().sequences().isEmpty(),"real bounded AIR");
        var from=fact.options().stream().filter(o->o.name().equals("FROM")).findFirst().orElseThrow().reference().orElseThrow();
        var length=fact.length().orElseThrow();need(length.kind()==OperandExpressionKind.LENGTH_OF,"structural LENGTH_OF");
        need(length.reference().orElseThrow().binding().selected().equals(from.binding().selected()),"same canonical data identity");
        need(!length.provenance().equals(fact.header().provenance()),"operand provenance not statement");
        need(pub.units().stream().flatMap(u->u.sequences().stream()).noneMatch(s->s.terminator() instanceof Operations.Invoke&&reached(pub).contains(s.label())),"no post-frontier CALL execution");
        need(pub.uncertainties().stream().anyMatch(u->u.code().equals("EXECUTABLE_CAPABILITY_NOT_READY")),"explicit capability metadata");
        new AirJson().decode(new AirJson().encode(pub));need(true,"strict roundtrip");
        for(var name:List.of("real-derived","cancel-entry","replace-cancel","reset-cancel","cancel-noop","noop-cancel","changed-handler","abend-cancel")) {
            var j=wire(name);var a=state(j);var e=a.events().getFirst();need(e.status()==HandlerStateAnalysis.EventStatus.NOT_REACHED_IN_PUBLISHED_TOPOLOGY,"unqualified SEND stops before ABEND");
            need(e.before().isEmpty()&&e.candidates().isEmpty(),"unreached is NOT_ASSESSED, not absent");need(a.equals(HandlerStateScheduleProbe.reverse(input(j))),"state schedule invariant");
        }
        need(command(input(wire("no-nohandle"))).commandKind()==fact.commandKind(),"NOHANDLE affects option fact, not family");metamorphics++;
        need(command(input(wire("erase"))).commandKind()==command(input(wire("from"))).commandKind(),"ERASE does not select family");metamorphics++;
        need(sends(lower(wire("dead"))).isEmpty(),"dead SEND remains unreachable");metamorphics++;
        need(command(input(wire("map"))).commandKind()==CicsCommandKind.SEND_MAP,"MAP distinct");metamorphics++;
        need(command(input(wire("renamed"))).commandKind()==fact.commandKind(),"rename preserves family");metamorphics++;
        need(command(input(wire("literal"))).commandKind()==fact.commandKind(),"literal length preserves family");metamorphics++;
        need(command(input(wire("changed-handler"))).commandKind()==fact.commandKind(),"handler target cannot classify SEND");metamorphics++;
        var changed=standalone.deepCopy();command(changed).put("rawText"," ".repeat(fact.rawText().length()));need(command(input(changed)).length().equals(fact.length()),"no rawText reinterpretation");metamorphics++;
        changed=wire("real-derived");var values=new ArrayList<JsonNode>();changed.path("statements").forEach(values::add);Collections.reverse(values);((ArrayNode)changed.path("statements")).removeAll();values.forEach(((ArrayNode)changed.path("statements"))::add);need(state(changed).equals(state(wire("real-derived"))),"input order is not execution order");metamorphics++;
        for(var version:List.of("2.40.0","2.41.0","2.42.0","2.43.0")){var j=standalone.deepCopy();j.put("contractVersion",version);reject(j,SpJsonDecoder.Code.INPUT_ERROR);}
        for(var version:List.of("2.46.0","future","9.99")){var j=standalone.deepCopy();j.put("contractVersion",version);command(j).put("commandKind","BROKEN");reject(j,SpJsonDecoder.Code.UNSUPPORTED_CONTRACT);}
        var j=standalone.deepCopy();command(j).remove("length");reject(j,SpJsonDecoder.Code.INPUT_ERROR);
        j=standalone.deepCopy();((ObjectNode)command(j).path("length")).put("kind","INTEGER");reject(j,SpJsonDecoder.Code.INPUT_ERROR);
        j=standalone.deepCopy();command(j).put("commandKind","SEND_MAP");reject(j,SpJsonDecoder.Code.INPUT_ERROR);
        j=standalone.deepCopy();command(j).put("handlerTarget","statement:0");reject(j,SpJsonDecoder.Code.INPUT_ERROR);
        j=standalone.deepCopy();((ArrayNode)command(j).path("options")).add(command(j).path("options").get(0).deepCopy());reject(j,SpJsonDecoder.Code.INPUT_ERROR);
        for(var field:List.of("factDependencies","controlTopology")){j=standalone.deepCopy();j.remove(field);reject(j,SpJsonDecoder.Code.INPUT_ERROR);}
        System.out.println("R7_R7B_TERMINAL_CHECKS="+checks+" METAMORPHICS="+metamorphics+" WIRE_MUTATIONS="+mutations);
    }
}
