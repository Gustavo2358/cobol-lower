package io.github.gustavo2358.lower.adapters.testing;

import java.util.*;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.node.*;
import io.github.gustavo2358.lower.adapters.sp.SpJsonDecoder;
import io.github.gustavo2358.lower.adapters.cli.CobolLower;
import io.github.gustavo2358.lower.application.*;
import io.github.gustavo2358.lower.domain.*;
import static io.github.gustavo2358.lower.domain.SpInput.*;

/** Frontend-generated SP2.43 compatibility and separate wire adversaries. */
public final class CicsCommandContractSuite {
    private static final ObjectMapper J=new ObjectMapper();private static int checks,mutations;
    private CicsCommandContractSuite(){}
    private static void need(boolean b,String message){checks++;if(!b)throw new AssertionError(message);}
    private static ObjectNode wire(String name)throws Exception {try(var in=CicsCommandContractSuite.class.getResourceAsStream("/sp/cics-command-r7-r6/"+name+".json")){return (ObjectNode)J.readTree(in);}}
    private static SpJsonDecoder.Result decode(ObjectNode j)throws Exception{return new SpJsonDecoder(CobolLower.INPUT_LIMITS).decode(J.writeValueAsBytes(j));}
    private static SpInput accept(ObjectNode j)throws Exception {var r=decode(j);need(r instanceof SpJsonDecoder.Decoded,"typed input decode "+r);return ((SpJsonDecoder.Decoded)r).input();}
    private static ObjectNode command(ObjectNode j){for(var s:j.path("statements"))if(s.path("variant").asText().equals("CICS_COMMAND"))return (ObjectNode)s;throw new AssertionError("fixture command");}
    private static HandlerStateAnalysis analyze(SpInput input){var r=new CobolLowerer().lower(input,CobolLower.OPTIONS);need(r.status()==LoweringResult.Status.IMPLEMENTATION_LIMIT,"NOT_READY "+r.admission().diagnostics());need(r.publication().isEmpty(),"no AIR publication");need(r.admission().input().orElseThrow().equals(input),"typed input preserved");return r.admission().handlerState().orElseThrow();}
    private static void reject(ObjectNode j,SpJsonDecoder.Code code)throws Exception{var r=decode(j);need(r instanceof SpJsonDecoder.Rejected x&&x.diagnostic().code()==code,"wire mutation "+code+": "+r);mutations++;}
    public static SpInput composition(int n)throws Exception {
        var input=accept(wire("many-"+n));need(input.statements().stream().filter(CicsCommandFact.class::isInstance).count()==n,"command multiplicity");
        need(analyze(input).events().getFirst().status()==HandlerStateAnalysis.EventStatus.ASSESSED,"composed command control");return input;
    }
    public static void main(String[] args)throws Exception {
        checks=0;mutations=0;
        for(var name:List.of("syncpoint","receive","receive-resp","receive-resp2","send","send-resp","send-nohandle","unsupported")) {
            var j=wire(name);var input=accept(j);var fact=input.statements().stream().filter(CicsCommandFact.class::isInstance).map(CicsCommandFact.class::cast).findFirst().orElseThrow();
            need(fact.executableLowering()==ExecutableLowering.NOT_READY,"typed disposition");need(fact.rawText().equals(command(j).path("rawText").asText()),"raw preserved, never interpreted");
            need(input.controlTopology().orElseThrow().equals(J.treeToValue(j.path("controlTopology"),ControlTopology.class)),"topology parity");
            need(input.factDependencies().orElseThrow().equals(J.treeToValue(j.path("factDependencies"),FactDependencies.class)),"causal storage parity");
            var analysis=analyze(input);need(analysis.equals(analyze(input)),"deterministic analysis");need(analysis.equals(HandlerStateScheduleProbe.reverse(input)),"worklist-order invariant");
            var event=analysis.events().getFirst();need(event.status()==(name.equals("unsupported")?HandlerStateAnalysis.EventStatus.NOT_REACHED_IN_PUBLISHED_TOPOLOGY:HandlerStateAnalysis.EventStatus.ASSESSED),"only qualified topology reaches ABEND "+name);
            if(!name.equals("unsupported")){need(event.candidates().size()==1,"positive registered label preserved");need(!event.unknownLocalRemainder(),"unresolved condition outcome does not manufacture a path to event");}
            var altered=j.deepCopy();var inv=(ArrayNode)altered.path("statements");var reversed=new ArrayList<JsonNode>();inv.forEach(reversed::add);Collections.reverse(reversed);inv.removeAll();reversed.forEach(inv::add);
            int ordinal=900;for(var s:inv)((ObjectNode)s.path("header")).put("programPoint",ordinal--);
            need(analysis.equals(analyze(accept(altered))),"inventory/programPoint are not control authority");
            altered=j.deepCopy();command(altered).put("rawText"," ".repeat(fact.rawText().length()));need(analysis.equals(analyze(accept(altered))),"lower never reparses source spelling");
        }
        for(var name:List.of("terminal","unknown")){var a=analyze(accept(wire(name)));need(a.events().getFirst().status()==HandlerStateAnalysis.EventStatus.NOT_REACHED_IN_PUBLISHED_TOPOLOGY,"opaque/RETURN cannot continue");}
        for(int n:List.of(1,2,5,40))composition(n);
        for(var name:List.of("dead-before","dead-reordered")) {
            var input=accept(wire(name));var a=analyze(input);need(a.events().getFirst().status()==HandlerStateAnalysis.EventStatus.ASSESSED,"dead source cannot stop live command");
            need(a.events().getFirst().candidates().size()==1,"dead source cannot introduce target");
            var dead=input.statements().stream().filter(OtherStatement.class::isInstance).map(OtherStatement.class::cast).filter(o->o.header().id().handle().equals("statement:2")).findFirst().orElseThrow();
            need(a.nodes().stream().noneMatch(node->node.location().equals(dead.header().id().handle())),"unreachable command remains unreachable");
        }
        var base=wire("receive-resp");
        for(var version:List.of("2.40.0","2.41.0","2.42.0")){var j=base.deepCopy();j.put("contractVersion",version);reject(j,SpJsonDecoder.Code.INPUT_ERROR);}
        for(var version:List.of("2.48.0","9.99","future")){var j=base.deepCopy();j.put("contractVersion",version);command(j).put("commandKind","INVENTED");reject(j,SpJsonDecoder.Code.UNSUPPORTED_CONTRACT);}
        for(var field:List.of("controlTopology","factDependencies")){var j=base.deepCopy();j.remove(field);reject(j,SpJsonDecoder.Code.INPUT_ERROR);}
        var j=base.deepCopy();command(j).put("commandKind","SYNCPOINT");reject(j,SpJsonDecoder.Code.INPUT_ERROR);
        j=base.deepCopy();command(j).put("handlerTarget","statement:3");reject(j,SpJsonDecoder.Code.INPUT_ERROR);
        j=base.deepCopy();((ArrayNode)command(j).path("gapCodes")).add("CICS_COMMAND_UNMODELED_OPTION");reject(j,SpJsonDecoder.Code.INPUT_ERROR);
        j=base.deepCopy();command(j).put("syntaxStatus","UNAVAILABLE");reject(j,SpJsonDecoder.Code.INPUT_ERROR);
        j=base.deepCopy();((ArrayNode)command(j).path("options")).add(command(j).path("options").get(0).deepCopy());reject(j,SpJsonDecoder.Code.INPUT_ERROR);
        j=base.deepCopy();((ObjectNode)command(j).path("options").get(0)).put("end",999999);reject(j,SpJsonDecoder.Code.INPUT_ERROR);
        j=wire("receive-resp2");((ObjectNode)command(j).path("options").get(2).path("reference")).put("role","READ");reject(j,SpJsonDecoder.Code.INPUT_ERROR);
        System.out.println("R7_R6_COMMAND_CHECKS="+checks+" WIRE_MUTATIONS="+mutations);
    }
}
