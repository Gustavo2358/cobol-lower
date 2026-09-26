package io.github.gustavo2358.lower.adapters.testing;
import io.github.gustavo2358.lower.adapters.sp.SpJsonDecoder;
import io.github.gustavo2358.lower.adapters.cli.CobolLower;
import io.github.gustavo2358.lower.application.*;
import io.github.gustavo2358.air.model.*;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.node.*;
public final class LogicalInitialInvariantSuite {
    public static void main(String[] args)throws Exception {
        var json=new ObjectMapper();
        var tree=(ObjectNode)json.readTree(LogicalInitialInvariantSuite.class.getResourceAsStream("/sp/recall-locality/logical-invariant.json"));
        var decoder=new SpJsonDecoder(CobolLower.INPUT_LIMITS);var decoded=decoder.decode(json.writeValueAsBytes(tree));
        if(!(decoded instanceof SpJsonDecoder.Decoded d))throw new AssertionError(decoded);
        var result=new CobolLowerer().lower(d.input(),CobolLower.OPTIONS);
        if(result.publication().isEmpty()||!result.validation().orElseThrow().isStructurallyValid())throw new AssertionError(result.status()+" "+result.admission().diagnostics());
        var conditions=result.publication().orElseThrow().units().stream().flatMap(u->u.entries().stream()).flatMap(e->e.state().conditions().stream()).toList();
        if(conditions.stream().noneMatch(c->c.place() instanceof Places.ObjectPlace && c.value() instanceof Entries.LiteralInitial l
            &&l.value().value().equals(new Values.TextValue("PROGA001"))))throw new AssertionError("logical invariant is an exact entry literal");
        var bad=tree.deepCopy();bad.put("contractVersion","2.45.0");
        if(!(decoder.decode(json.writeValueAsBytes(bad)) instanceof SpJsonDecoder.Rejected))throw new AssertionError("logical invariant requires SP2.46");
        bad=tree.deepCopy();bad.remove("factDependencies");
        var absent=decoder.decode(json.writeValueAsBytes(bad));
        if(absent instanceof SpJsonDecoder.Decoded a&&new CobolLowerer().lower(a.input(),CobolLower.OPTIONS).publication().isPresent())throw new AssertionError("entry invariant cannot bypass cell proof");
        System.out.println("LOGICAL_INITIAL_INVARIANT=PASS");
    }
}
