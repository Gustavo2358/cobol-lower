package io.github.gustavo2358.lower.adapters.testing;
import java.util.*;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.node.*;
import io.github.gustavo2358.lower.adapters.cli.CobolLower;
import io.github.gustavo2358.lower.adapters.sp.SpJsonDecoder;
import io.github.gustavo2358.lower.adapters.source.QualifiedSourceJson;
import io.github.gustavo2358.lower.application.*;
import io.github.gustavo2358.lower.source.QualifiedSourceDependencies;
/** Source tables survive typed/JSON admission and never authorize executable memory. */
public final class SourceTableSuite {
    public static void main(String[] args)throws Exception {
        var json=new ObjectMapper();var decoder=new SpJsonDecoder(CobolLower.INPUT_LIMITS);var codec=new QualifiedSourceJson();
        for(var name:List.of("table-columns","table-write","table-group-write","table-alias-write","table-nested","review-table-base","review-table-88","review-table-sibling-base","review-table-sibling-88","review-table-write-88")) {
            var tree=(ObjectNode)json.readTree(SourceTableSuite.class.getResourceAsStream("/sp/source-tables/"+name+".json"));
            var decoded=decoder.decode(json.writeValueAsBytes(tree));if(!(decoded instanceof SpJsonDecoder.Decoded d))throw new AssertionError(decoded);
            var facts=d.input().nominalValues().orElseThrow();if(!facts.authority().equals("NOMINAL_TEXT_SOURCE_V4")||facts.tableFields().isEmpty())throw new AssertionError("table facts lost");
            var unit=QualifiedSourceProjection.admitAndProject(d.input(),CobolLower.POSITIVE_OPTIONS);
            if(!unit.nominalValues().orElseThrow().facts().equals(facts))throw new AssertionError("source table facts changed");
            var source=new QualifiedSourceDependencies("qualified-source-dependencies","1.4.0","test",new QualifiedSourceDependencies.Document("cobol-semantic-product","2.60.0","0".repeat(64)),List.of(),List.of(unit));
            if(!source.equals(codec.decode(codec.encode(source))))throw new AssertionError("table round trip");
            var result=new CobolLowerer().lower(d.input(),CobolLower.POSITIVE_OPTIONS);
            var noSource=tree.deepCopy();noSource.remove("nominalValues");var prior=decoder.decode(json.writeValueAsBytes(noSource));
            if(!(prior instanceof SpJsonDecoder.Decoded p)||!new CobolLowerer().lower(p.input(),CobolLower.POSITIVE_OPTIONS).publication().equals(result.publication()))throw new AssertionError("table source facts changed AIR");
            var old=tree.deepCopy();old.put("contractVersion","2.59.0");reject(decoder,json,old);
            var absent=tree.deepCopy();((ObjectNode)absent.path("nominalValues")).remove("tableFields");reject(decoder,json,absent);
            var downgraded=tree.deepCopy();((ObjectNode)downgraded.path("nominalValues")).put("authority","NOMINAL_TEXT_SOURCE_V3");reject(decoder,json,downgraded);
            var origin=tree.deepCopy();((ObjectNode)origin.path("nominalValues").path("tableFields").get(0).path("initial").get(0)).put("origin","foreign");reject(decoder,json,origin);
            try{new QualifiedSourceDependencies("qualified-source-dependencies","1.3.0","test",source.source(),List.of(),List.of(unit));throw new AssertionError("old envelope admitted table facts");}catch(IllegalArgumentException expected){}
        }
        System.out.println("SOURCE_TABLES=PASS: 10 real products, round trips, AIR independence, version/identity forgeries");
    }
    private static void reject(SpJsonDecoder decoder,ObjectMapper json,ObjectNode node)throws Exception{if(!(decoder.decode(json.writeValueAsBytes(node)) instanceof SpJsonDecoder.Rejected))throw new AssertionError("invalid table facts admitted");}
}
