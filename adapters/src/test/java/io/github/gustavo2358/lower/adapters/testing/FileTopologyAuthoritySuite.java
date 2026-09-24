package io.github.gustavo2358.lower.adapters.testing;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.node.*;
import io.github.gustavo2358.lower.adapters.sp.SpJsonDecoder;
import io.github.gustavo2358.lower.adapters.cli.CobolLower;
import io.github.gustavo2358.lower.application.*;
import io.github.gustavo2358.lower.domain.*;
import java.util.*;

/** SP2.39 FILE metadata cannot replace the executable topology target. */
public final class FileTopologyAuthoritySuite {
    private FileTopologyAuthoritySuite() { }
    private static final ObjectMapper JSON=new ObjectMapper();
    private static int checks;
    private static void need(boolean ok,String reason){checks++;if(!ok)throw new AssertionError(reason);}
    private static SpInput decode(JsonNode tree)throws Exception {
        var result=new SpJsonDecoder(CobolLower.INPUT_LIMITS).decode(JSON.writeValueAsBytes(tree));
        need(result instanceof SpJsonDecoder.Decoded,"well-shaped topology must decode: "+result);
        return ((SpJsonDecoder.Decoded)result).input();
    }
    private static LoweringResult lower(SpInput input){return new CobolLowerer().lower(input,CobolLower.OPTIONS);}
    private static void rejected(SpInput input,String why) {
        var result=lower(input);
        need(result.status()==LoweringResult.Status.INVALID_INPUT&&result.publication().isEmpty(),why+": "+result.status());
        need(result.toString().contains("FILE destination belongs to topology inventory"),"specific missing FILE role diagnosis");
    }
    private static JsonNode normalized(LoweringResult result)throws Exception {
        var publication=result.publication().orElseThrow();
        var tree=JSON.readTree(new io.github.gustavo2358.air.json.AirJson().encode(publication));
        // fileInventory contributes to publication identity. Erase that namespace
        // only; retain every local ID, target, operation, proof and source correlation.
        return normalizeNamespace(tree,publication.id().localId());
    }
    private static JsonNode normalizeNamespace(JsonNode node,String namespace) {
        if(node.isTextual())return TextNode.valueOf(node.asText().replace(namespace,"PUBLICATION_NAMESPACE"));
        if(node.isObject()) {
            var copy=JSON.createObjectNode();node.properties().forEach(e->copy.set(e.getKey(),normalizeNamespace(e.getValue(),namespace)));return copy;
        }
        if(node.isArray()) {var copy=JSON.createArrayNode();node.forEach(n->copy.add(normalizeNamespace(n,namespace)));return copy;}
        return node;
    }
    public static void main(String[] args)throws Exception {
        checks=0;
        JsonNode source;
        try(var in=FileTopologyAuthoritySuite.class.getResourceAsStream("/sp/control-topology-r1/file-authority.json")) {
            if(in==null)throw new AssertionError("missing real producer FILE fixture");source=JSON.readTree(in);
        }
        var input=decode(source);var original=lower(input);
        need(original.publication().isPresent(),"producer FILE publication lowers: "+original);
        // Same topology, deliberately reversed legacy handler member order. All
        // members still belong to the same FILE operation; metadata is no entry authority.
        ObjectNode metadata=source.deepCopy();int reversed=0;
        for(var use:metadata.path("fileInventory").path("operations").path("uses"))for(var handler:use.path("handlers")) {
            var members=(ArrayNode)handler.path("statements");var values=new ArrayList<JsonNode>();members.forEach(values::add);
            need(values.size()==2,"two distinguishable handler members");Collections.reverse(values);members.removeAll();values.forEach(members::add);reversed++;
        }
        need(reversed==2,"both event handlers challenged");
        var reordered=lower(decode(metadata));
        need(reordered.publication().isPresent(),"metadata peer still lowers: "+reordered.status());
        need(normalized(original).equals(normalized(reordered)),"FILE metadata cannot redirect executable targets");
        var topology=input.controlTopology().orElseThrow();
        var fileOutcomes=topology.outcomes().stream().filter(o->o.role().startsWith("file/")).toList();
        need(fileOutcomes.size()>2,"handler and ordinary event roles covered");
        for(var removed:fileOutcomes) {
            var outcomes=topology.outcomes().stream().filter(o->!o.id().equals(removed.id())).toList();
            var occurrences=topology.occurrences().stream().map(o->new ControlTopology.Occurrence(o.statement(),o.region(),
                o.outcomes().stream().filter(id->!id.equals(removed.id())).toList(),o.proofs())).toList();
            var mutated=new ControlTopology(topology.authority(),occurrences,topology.regions(),topology.boundaries(),outcomes,topology.bindings(),topology.proofs());
            var memory=new SpInput(input.unit(),input.policy(),input.dataDeclarations(),input.statements(),input.structure(),input.gaps(),input.coverage(),input.entryInventory(),
                input.storageIndependence(),input.compositional(),input.storage(),input.fileInventory(),input.sourceDependencies(),input.ordinaryContinuations(),Optional.of(mutated));
            rejected(memory,"missing FILE outcome rejected in memory before AIR: "+removed.role());
            ObjectNode wire=source.deepCopy();wire.set("controlTopology",JSON.valueToTree(mutated));
            rejected(decode(wire),"missing FILE outcome rejected from wire before AIR: "+removed.role());
        }
        System.out.println("FILE_TOPOLOGY_AUTHORITY_CHECKS="+checks);
    }
}
