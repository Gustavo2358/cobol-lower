package io.github.gustavo2358.lower.adapters.testing;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.*;
import io.github.gustavo2358.lower.adapters.sp.SpJsonDecoder;
import io.github.gustavo2358.lower.adapters.cli.CobolLower;
import io.github.gustavo2358.lower.application.*;
import io.github.gustavo2358.lower.domain.ControlTopology;
import io.github.gustavo2358.air.model.*;
import java.util.*;

/** Frozen old wire retains old interpretation; new wire has exactly one authority. */
public final class ControlTopologyWireSuite {
    private ControlTopologyWireSuite() { }
    private static final ObjectMapper JSON=new ObjectMapper();
    private static byte[] fixture(String name)throws Exception {
        try(var stream=ControlTopologyWireSuite.class.getResourceAsStream("/sp/control-topology-r1/"+name+".json")){
            if(stream==null)throw new AssertionError("missing migration fixture");return stream.readAllBytes();}
    }
    private static void need(boolean value,String message){if(!value)throw new AssertionError(message);}
    public static void main(String[] args)throws Exception {
        var decoder=new SpJsonDecoder(CobolLower.INPUT_LIMITS);var raw=fixture("current");
        var current=decoder.decode(raw);need(current instanceof SpJsonDecoder.Decoded,"new wire decodes");
        var input=((SpJsonDecoder.Decoded)current).input();need(input.controlTopology().isPresent(),"new authority present");
        var old=decoder.decode(fixture("legacy"));need(old instanceof SpJsonDecoder.Decoded,"historical wire still decodes");
        var legacy=((SpJsonDecoder.Decoded)old).input();need(legacy.controlTopology().isEmpty(),"old wire has no new authority");
        var oldResult=new CobolLowerer().lower(legacy,CobolLower.OPTIONS);
        need(oldResult.status()==LoweringResult.Status.INVALID_INPUT,"legacy F2 remains legacy F2: "+oldResult.status());
        var result=new CobolLowerer().lower(input,CobolLower.OPTIONS);
        need(result.publication().isPresent(),"new topology removes representation contradiction: "+result.status());
        var topology=input.controlTopology().orElseThrow();
        need(topology.equals(JSON.readValue(JSON.writeValueAsBytes(topology),ControlTopology.class)),"typed topology roundtrip");
        for(var mutation:List.of("missing","null","old-version","missing-regions","target-removed","bad-kind","unknown-field","bad-endpoint","missing-phases")) {
            ObjectNode tree=(ObjectNode)JSON.readTree(raw);var t=(ObjectNode)tree.get("controlTopology");
            switch(mutation) {
                case "missing" -> tree.remove("controlTopology");
                case "null" -> tree.putNull("controlTopology");
                case "old-version" -> tree.put("contractVersion","2.37.0");
                case "missing-regions" -> t.remove("regions");
                case "target-removed" -> ((ArrayNode)t.get("occurrences")).remove(0);
                case "bad-kind" -> ((ObjectNode)t.withArray("outcomes").get(0)).put("kind","MAYBE_VERB");
                case "unknown-field" -> t.put("fallbackToLegacy",true);
                case "bad-endpoint" -> ((ObjectNode)t.withArray("bindings").get(0)).put("endpoint","missing-boundary");
                case "missing-phases" -> ((ObjectNode)t.withArray("bindings").get(0)).remove("phases");
                default -> throw new AssertionError(mutation);
            }
            need(!(decoder.decode(JSON.writeValueAsBytes(tree)) instanceof SpJsonDecoder.Decoded),"malformed shape rejected: "+mutation);
        }
        ObjectNode permuted=(ObjectNode)JSON.readTree(raw);
        reverse(permuted.withArray("statements"));var t=(ObjectNode)permuted.get("controlTopology");
        for(var name:List.of("occurrences","regions","boundaries","outcomes","bindings","proofs"))reverse(t.withArray(name));
        var peer=((SpJsonDecoder.Decoded)decoder.decode(JSON.writeValueAsBytes(permuted))).input();
        need(topology.equals(peer.controlTopology().orElseThrow()),"topology physical permutation");
        var peerResult=new CobolLowerer().lower(peer,CobolLower.OPTIONS);
        need(result.publication().equals(peerResult.publication()),"statement/topology reorder preserves complete AIR publication");
        var missing=new ArrayList<>(topology.occurrences());missing.remove(0);boolean rejected=false;
        try{new ControlTopology(topology.authority(),missing,topology.regions(),topology.boundaries(),topology.outcomes(),topology.bindings(),topology.proofs());}
        catch(IllegalArgumentException ex){rejected=true;}
        need(rejected,"in-memory rejection parity for removed target");
        ObjectNode unknown=(ObjectNode)JSON.readTree(raw);var unknownTopology=(ObjectNode)unknown.get("controlTopology");
        var completion=java.util.stream.StreamSupport.stream(unknownTopology.withArray("outcomes").spliterator(),false)
            .filter(o->o.path("target").path("kind").asText().equals("COMPLETE")).findFirst().orElseThrow();
        ((ObjectNode)completion).put("kind","UNKNOWN_LOCAL");((ObjectNode)completion.path("target")).put("kind","UNKNOWN_LOCAL");
        var unknownInput=((SpJsonDecoder.Decoded)decoder.decode(JSON.writeValueAsBytes(unknown))).input();
        var partial=new CobolLowerer().lower(unknownInput,CobolLower.OPTIONS);
        need(partial.publication().isPresent(),"an unavailable frontier preserves the independent known prefix");
        var frontier=partial.publication().orElseThrow().units().getFirst().sequences().stream().map(Sequence::terminator)
            .filter(o->o.header().precision().control().status()==Evidence.PrecisionStatus.UNAVAILABLE).findFirst().orElseThrow();
        need(frontier instanceof Operations.Opaque,"unknown is not a return/halt or invented jump");
        var envelope=((Operations.Opaque)frontier).envelope().control();
        need(envelope.known().isEmpty()&&envelope.remainder() instanceof Scopes.WithinControl w
            &&w.scope() instanceof Scopes.LabelsControl labels&&labels.labels().isEmpty(),"unknown has an open frontier, no fabricated/all labels");
        need(frontier.header().coverage()==Evidence.CoverageStatus.UNSUPPORTED,"frontier never certifies modeled source control");
        need(partial.publication().orElseThrow().coverage().inventory()==Evidence.InventoryStatus.PARTIAL,"partial source coverage remains explicit");
        need(partial.publication().orElseThrow().uncertainties().stream().anyMatch(u->u.code().equals("CONTROL_TOPOLOGY_REGION_UNAVAILABLE")),"source knowledge bound remains explained");
        System.out.println("CONTROL_TOPOLOGY_WIRE_TESTS=26");
    }
    private static void reverse(ArrayNode array){var values=new ArrayList<JsonNode>();array.forEach(values::add);Collections.reverse(values);array.removeAll();values.forEach(array::add);}
}
