package io.github.gustavo2358.lower.adapters.testing;

import io.github.gustavo2358.lower.adapters.cli.CobolLower;
import io.github.gustavo2358.lower.adapters.sp.SpJsonDecoder;
import io.github.gustavo2358.air.model.*;
import io.github.gustavo2358.air.json.AirJson;
import java.util.*;
import static io.github.gustavo2358.lower.testing.CallOracle.check;

/** Real frontend SP fixtures; no COBOL or artifact IO by the lower. */
public final class SourceDependencySuite {
    private SourceDependencySuite() {}
    public static void main(String[] args)throws Exception {
        var decoder=new SpJsonDecoder(CobolLower.INPUT_LIMITS);
        for(var name:List.of("copy-nested","copy-repeated","copy-missing","dclgen-inside-copybook","unresolved-include")) {
            var bytes=Objects.requireNonNull(SourceDependencySuite.class.getResourceAsStream("/sp/source-dependencies-w3/"+name+".json")).readAllBytes();
            var decoded=decoder.decode(bytes);check(decoded instanceof SpJsonDecoder.Decoded,"source SP admitted: "+name);
            var input=((SpJsonDecoder.Decoded)decoded).input();
            var p=RegionalTranslationSuite.lower(input).publication().orElseThrow();
            var resources=p.resources().stream().filter(r->r.description() instanceof Interactions.LiteralTarget t&&Set.of("source-copybook","source-dclgen","source-sql_include").contains(t.category())).toList();
            check(resources.size()==input.sourceDependencies().occurrences().size(),"all source occurrences transported");
            check(resources.stream().allMatch(r->r.declaration().orElseThrow().uses().isEmpty()&&r.declaration().orElseThrow().objects().isEmpty()),"not runtime operations");
            var codec=new AirJson();check(codec.decode(codec.encode(p)).equals(p),"existing AIR roundtrip");
            check(p.storage().stream().noneMatch(Memory.Region.class::isInstance),"no physical profile");
            for(var mutation:List.of("omit-inventory","omit-field","unknown-field","unknown-kind","false-dclgen","resolved-without-artifact","duplicate","old-version","no-provenance")) {
                var mapper=new com.fasterxml.jackson.databind.ObjectMapper();var tree=(com.fasterxml.jackson.databind.node.ObjectNode)mapper.readTree(bytes);
                var inventory=(com.fasterxml.jackson.databind.node.ObjectNode)tree.path("sourceDependencies");
                var occurrences=(com.fasterxml.jackson.databind.node.ArrayNode)inventory.path("occurrences");
                var first=(com.fasterxml.jackson.databind.node.ObjectNode)occurrences.get(0);
                switch(mutation) {
                    case "omit-inventory"->tree.remove("sourceDependencies");case "omit-field"->first.remove("resolution");
                    case "unknown-field"->first.put("runtime",true);case "unknown-kind"->first.put("kind","TABLE");
                    case "false-dclgen"->{first.put("kind","DCLGEN");first.put("authority","UNKNOWN");}
                    case "resolved-without-artifact"->{first.put("resolution","RESOLVED");first.put("artifact","");}
                    case "duplicate"->occurrences.add(first.deepCopy());case "old-version"->tree.put("contractVersion","2.28.0");
                    case "no-provenance"->first.remove("provenance");default->throw new AssertionError(mutation);
                }
                check(decoder.decode(mapper.writeValueAsBytes(tree)) instanceof SpJsonDecoder.Rejected,"reject malformed source wire "+mutation);
            }
        }
        System.out.println("SOURCE_DEPENDENCIES: 5 real SP roundtrips, 45 negative mutations PASS; AIR model unchanged");
    }
}
