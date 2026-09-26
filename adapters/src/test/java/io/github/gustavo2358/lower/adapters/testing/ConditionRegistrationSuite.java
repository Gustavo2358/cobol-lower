package io.github.gustavo2358.lower.adapters.testing;
import java.util.*;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.github.gustavo2358.lower.adapters.sp.*;
import io.github.gustavo2358.lower.application.*;
import io.github.gustavo2358.air.model.*;
public final class ConditionRegistrationSuite {
    public static void main(String[] args)throws Exception {
        var json=new ObjectMapper();var tree=(ObjectNode)json.readTree(ConditionRegistrationSuite.class.getResourceAsStream("/sp/recall-locality/condition-registration.json"));
        var decoder=new SpJsonDecoder(io.github.gustavo2358.lower.adapters.cli.CobolLower.INPUT_LIMITS);
        var decoded=decoder.decode(json.writeValueAsBytes(tree));
        if(!(decoded instanceof SpJsonDecoder.Decoded d))throw new AssertionError(decoded);
        var r=new CobolLowerer().lower(d.input(),io.github.gustavo2358.lower.adapters.cli.CobolLower.POSITIVE_OPTIONS);
        if(r.publication().isEmpty())throw new AssertionError(r.status());
        var operation=r.publication().get().units().getFirst().sequences().stream().map(Sequence::terminator).filter(Operations.Opaque.class::isInstance).map(Operations.Opaque.class::cast)
            .filter(o->o.envelope().control().known().size()==1).findFirst().orElseThrow();
        var memory=operation.envelope().memory();
        if(!memory.knownWrites().isEmpty()||!(memory.otherWrites() instanceof Scopes.NoMemory))throw new AssertionError("registration invents a write");
        if(!(operation.envelope().dependencies().remainder() instanceof Scopes.AnyResource))throw new AssertionError("external state must remain open");
        var old=tree.deepCopy();old.put("contractVersion","2.45.0");
        if(!(decoder.decode(json.writeValueAsBytes(old)) instanceof SpJsonDecoder.Rejected))throw new AssertionError("new proof accepted in old contract");
        for(var field:List.of("unknownReadBound","unknownWriteBound","unknownExposureBound","environment")) {
            var forgedTree=tree.deepCopy();
            for(var e:forgedTree.path("statementEffects"))if(e.path("proof").asText().equals("CICS_CONDITION_REGISTRATION"))((ObjectNode)e).put(field,field.equals("environment")?"NONE":"ALL");
            var forgedInput=decoder.decode(json.writeValueAsBytes(forgedTree));
            if(forgedInput instanceof SpJsonDecoder.Decoded f&&new CobolLowerer().lower(f.input(),io.github.gustavo2358.lower.adapters.cli.CobolLower.POSITIVE_OPTIONS).publication().isPresent())throw new AssertionError("forged effect proof admitted: "+field);
        }
        System.out.println("CONDITION_REGISTRATION=PASS");
    }
}
