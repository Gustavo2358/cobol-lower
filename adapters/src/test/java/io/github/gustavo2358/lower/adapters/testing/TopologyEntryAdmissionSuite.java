package io.github.gustavo2358.lower.adapters.testing;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.*;
import io.github.gustavo2358.air.model.*;
import io.github.gustavo2358.lower.adapters.cli.CobolLower;
import io.github.gustavo2358.lower.adapters.sp.SpJsonDecoder;
import io.github.gustavo2358.lower.application.*;
import io.github.gustavo2358.lower.domain.SpInput;

/** A real recovered-source SP: old entry precision is absent, topology authority is present. */
public final class TopologyEntryAdmissionSuite {
    private static final ObjectMapper JSON=new ObjectMapper();
    private static int checks;
    private static void need(boolean value,String reason){checks++;if(!value)throw new AssertionError(reason);}
    private static byte[] fixture(String path)throws Exception {
        try(var in=TopologyEntryAdmissionSuite.class.getResourceAsStream(path)){return Objects.requireNonNull(in).readAllBytes();}
    }
    private static SpInput decode(byte[] bytes){
        var d=new SpJsonDecoder(CobolLower.INPUT_LIMITS).decode(bytes);
        need(d instanceof SpJsonDecoder.Decoded,"fixture remains a valid SP");return ((SpJsonDecoder.Decoded)d).input();
    }
    private static LoweringResult lower(JsonNode tree)throws Exception{return new CobolLowerer().lower(decode(JSON.writeValueAsBytes(tree)),CobolLower.POSITIVE_OPTIONS);}
    public static void main(String[] args)throws Exception {
        var raw=fixture("/sp/topology-entry/parser-recovery.json");var input=decode(raw);
        need(input.entryInventory().entries().getFirst().start().statement().isEmpty(),"legacy start unavailable");
        var root=input.controlTopology().orElseThrow().regions().stream().filter(r->r.parent().isEmpty()).findFirst().orElseThrow();
        need(root.entry().reference().equals("statement:0"),"producer explicitly selects the first CALL");
        var result=new CobolLowerer().lower(input,CobolLower.POSITIVE_OPTIONS);
        need(result.publication().isPresent(),"authoritative start survives missing legacy precision: "+result.admission().diagnostics());
        need(result.validation().orElseThrow().isStructurallyValid(),"real AIR validator accepts publication");
        var pub=result.publication().orElseThrow();
        need(!pub.uncertainties().isEmpty()&&pub.coverage().inventory()==Evidence.InventoryStatus.PARTIAL,"input and control uncertainty survive");
        need(pub.uncertainties().stream().anyMatch(u->u.code().equals("ENTRY_SIGNATURE_UNKNOWN")),"signature not fabricated");
        need(result.statements().stream().noneMatch(s->s.source().handle().equals("statement:1")),"later CALL gains no executable path");
        var source=QualifiedSourceProjection.project(input,result.admission());
        var known=source.occurrences().stream().filter(o->!o.qualifications().isEmpty()).flatMap(o->o.values().stream()).map(v->v.value()).toList();
        need(known.equals(List.of("KEEPPGM")),"known root dependency survives; later dependency not invented");
        need(!source.frontiers().isEmpty(),"unknown continuation remains bounded in source certificate");
        var unknown=(ObjectNode)JSON.readTree(raw);
        var rootWire=java.util.stream.StreamSupport.stream(unknown.path("controlTopology").path("regions").spliterator(),false).filter(r->r.path("parent").asText().isEmpty()).findFirst().orElseThrow();
        ((ObjectNode)rootWire.path("entry")).put("kind","UNKNOWN_LOCAL").put("reference",rootWire.path("id").asText());
        var blocked=lower(unknown);need(blocked.status()==LoweringResult.Status.BLOCKED_LOWERING&&blocked.publication().isEmpty(),"unknown root cannot fabricate entry");
        var mismatch=(ObjectNode)JSON.readTree(raw);var entry=(ObjectNode)mismatch.path("entryInventory").path("entries").get(0);
        entry.put("availability","KNOWN");((ObjectNode)entry.path("start")).put("availability","KNOWN").put("statement","statement:1");
        var invalid=lower(mismatch);need(invalid.status()==LoweringResult.Status.INVALID_INPUT&&invalid.publication().isEmpty(),"contradictory known entry still rejected");
        var absent=(ObjectNode)JSON.readTree(raw);((ObjectNode)absent.path("entryInventory")).withArray("entries").removeAll();
        var noIdentity=lower(absent);need(noIdentity.publication().isEmpty(),"primary identity cannot be fabricated");
        var legacy=decode(fixture("/sp/entry-localization/procedure-unsafe.json"));
        need(legacy.controlTopology().isEmpty(),"legacy authority check is independent");
        need(new CobolLowerer().lower(legacy,CobolLower.OPTIONS).status()==LoweringResult.Status.BLOCKED_LOWERING,"legacy missing start remains blocked");
        callFrontier();
        System.out.println("TOPOLOGY_ENTRY_ADMISSION_CHECKS="+checks);
    }
    private static void callFrontier()throws Exception {
        var raw=fixture("/sp/topology-entry/call-frontier.json");
        var tree=(ObjectNode)JSON.readTree(raw);
        for(var target:List.of("computed","literal")) {
            var sample=tree.deepCopy();
            var call=(ObjectNode)sample.path("statements").get(1);
            if(target.equals("literal")) {
                call.put("syntax","LITERAL_PROGRAM_NAME");
                var reference=call.path("target").path("reference");
                var literal=JSON.createObjectNode().put("kind","LITERAL").put("id",reference.path("id").asText())
                    .put("text","KEEPPGM").put("writtenText","'KEEPPGM'");
                literal.set("logicalValue",JSON.createObjectNode().put("logicalDomain","TEXT").put("logicalExtent",7).put("value","KEEPPGM"));
                literal.set("provenance",reference.path("provenance").deepCopy());call.set("target",literal);
            }
            var result=lower(sample);need(result.publication().isPresent(),"bounded CALL publication survives");
            need(result.validation().orElseThrow().isStructurallyValid(),"CALL frontier passes AIR validator");
            var unit=result.publication().orElseThrow().units().getFirst();
            var invokes=unit.sequences().stream().map(Sequence::terminator).filter(Operations.Invoke.class::isInstance)
                .map(Operations.Invoke.class::cast).toList();
            need(invokes.size()==1,"unavailable completion must retain the CALL target");
            var invoke=invokes.getFirst();
            if(target.equals("computed"))need(invoke.target() instanceof Interactions.ComputedTarget c&&c.name() instanceof Expressions.Read,"known target operand retained");
            else need(invoke.target() instanceof Interactions.LiteralTarget,"literal target survives unavailable completion");
            need(invoke.outcomes().known().isEmpty()&&invoke.outcomes().remainder() instanceof Scopes.WithinControl w
                &&w.scope() instanceof Scopes.LabelsControl labels&&labels.labels().isEmpty(),"no handler, return or successor fabricated");
            need(result.statements().stream().noneMatch(link->Set.of("statement:2","statement:3").contains(link.source().handle())),
                "unlicensed handler and following GOBACK remain inventory only");
            var reversed=sample.deepCopy();var statements=(com.fasterxml.jackson.databind.node.ArrayNode)reversed.path("statements");
            var facts=new ArrayList<JsonNode>();statements.forEach(facts::add);Collections.reverse(facts);statements.removeAll();facts.forEach(statements::add);
            var codec=new io.github.gustavo2358.air.json.AirJson();
            need(Arrays.equals(codec.encode(result.publication().orElseThrow()),codec.encode(lower(reversed).publication().orElseThrow())),
                "source array order cannot select frontier or continuation");
        }
    }

}
