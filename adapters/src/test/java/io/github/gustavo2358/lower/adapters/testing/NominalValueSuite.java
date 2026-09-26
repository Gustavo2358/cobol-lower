package io.github.gustavo2358.lower.adapters.testing;
import java.util.*;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.node.*;
import io.github.gustavo2358.lower.adapters.cli.CobolLower;
import io.github.gustavo2358.lower.adapters.sp.SpJsonDecoder;
import io.github.gustavo2358.lower.adapters.source.QualifiedSourceJson;
import io.github.gustavo2358.lower.application.*;
import io.github.gustavo2358.lower.source.QualifiedSourceDependencies;

public final class NominalValueSuite {
    public static void main(String[] args)throws Exception {
        var json=new ObjectMapper();var tree=(ObjectNode)json.readTree(NominalValueSuite.class.getResourceAsStream("/sp/conditional-source/nominal.json"));
        var decoder=new SpJsonDecoder(CobolLower.INPUT_LIMITS);var decoded=decoder.decode(json.writeValueAsBytes(tree));
        if(!(decoded instanceof SpJsonDecoder.Decoded d))throw new AssertionError(decoded);
        var input=d.input();var facts=input.nominalValues().orElseThrow();
        if(facts.queries().size()!=1||facts.assignments().size()!=1||facts.conditions().size()!=1)throw new AssertionError("nominal facts lost at admission");
        var result=new CobolLowerer().lower(input,CobolLower.OPTIONS);
        if(result.publication().isEmpty()||!result.validation().orElseThrow().isStructurallyValid())throw new AssertionError(result.status());
        var unit=QualifiedSourceProjection.admitAndProject(input,CobolLower.OPTIONS);
        var value=unit.nominalValues().orElseThrow();
        if(!value.facts().equals(facts)||value.seeds().isEmpty()||value.uncertainties().isEmpty())throw new AssertionError("source provenance/uncertainty lost");
        var source=new QualifiedSourceDependencies("qualified-source-dependencies","1.0.0","test",new QualifiedSourceDependencies.Document("cobol-semantic-product","2.47.0","0".repeat(64)),List.of(),List.of(unit));
        var codec=new QualifiedSourceJson();if(!source.equals(codec.decode(codec.encode(source))))throw new AssertionError("source round trip");
        var old=tree.deepCopy();old.put("contractVersion","2.46.0");reject(decoder,json,old);
        var foreign=tree.deepCopy();((ObjectNode)foreign.path("nominalValues").path("queries").get(0)).put("node","missing");reject(decoder,json,foreign);
        var foreignStatement=tree.deepCopy();((ObjectNode)foreignStatement.path("nominalValues").path("assignments").get(0)).put("statement","absent");reject(decoder,json,foreignStatement);
        var legacy=tree.deepCopy();legacy.remove("nominalValues");legacy.put("contractVersion","2.46.0");
        var historical=decoder.decode(json.writeValueAsBytes(legacy));
        if(!(historical instanceof SpJsonDecoder.Decoded h))throw new AssertionError(historical);
        var prior=new CobolLowerer().lower(h.input(),CobolLower.OPTIONS);
        if(!prior.publication().equals(result.publication()))throw new AssertionError("nominal source facts must not synthesize or change AIR");
        System.out.println("NOMINAL_VALUES_CONTRACT=PASS");
    }
    private static void reject(SpJsonDecoder d,ObjectMapper j,ObjectNode n)throws Exception {if(!(d.decode(j.writeValueAsBytes(n)) instanceof SpJsonDecoder.Rejected))throw new AssertionError("invalid nominal evidence admitted");}
}
