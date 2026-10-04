package io.github.gustavo2358.lower.adapters.testing;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.node.*;
import io.github.gustavo2358.lower.adapters.sp.SpJsonDecoder;
import io.github.gustavo2358.lower.adapters.cli.CobolLower;
import io.github.gustavo2358.lower.application.CobolLowerer;
import io.github.gustavo2358.air.model.*;
import java.util.*;

final class UncertainTruncationChecks {
    static void run() throws Exception {
        var json=new ObjectMapper();var decoder=new SpJsonDecoder(CobolLower.INPUT_LIMITS);
        var fixture=(ObjectNode)json.readTree(UncertainTruncationChecks.class.getResourceAsStream("/sp/numeric-move/uncertain-trunc.json"));
        var decoded=decoder.decode(json.writeValueAsBytes(fixture));
        if(!(decoded instanceof SpJsonDecoder.Decoded ok))throw new AssertionError(decoded);
        var result=new CobolLowerer().lower(ok.input(),CobolLower.POSITIVE_OPTIONS);
        var publication=result.publication().orElseThrow(()->new AssertionError(result.admission().diagnostics()));
        int unknown=0,reads=0,common=0;
        for(var unit:publication.units())for(var sequence:unit.sequences()) {
            if(sequence.terminator() instanceof Operations.Branch||sequence.terminator() instanceof Operations.Opaque)
                throw new AssertionError("Unknown receiving truncation must not invent runtime behavior for valid numeric inputs");
            for(var instruction:sequence.instructions())if(instruction instanceof Operations.Assign a) {
                if(a.value() instanceof Expressions.Unknown u) {
                    if(!u.typeRef().equals(Types.known(Types.Builtin.INT))||a.header().precision().values().status()!=Evidence.PrecisionStatus.OPEN)throw new AssertionError("unknown binary result gained exact value");
                    unknown++;if(!u.dependencies().isEmpty())reads++;
                } else if(a.value() instanceof Expressions.Literal l&&l.value() instanceof Values.IntValue n&&n.value().intValueExact()==12)common++;
                else throw new AssertionError("invented STD/BIN result "+a.value());
            }
        }
        if(unknown!=3||reads!=1||common!=1)throw new AssertionError("nonlinear or missing conversion: "+List.of(unknown,reads,common));
        for(boolean invented:List.of(true,false)) {
            var forged=fixture.deepCopy();var transfer=(ObjectNode)forged.path("statements").get(invented?0:2).path("numericTransfers").get(0);
            if(invented)transfer.put("value","51");else transfer.putNull("value");
            var bad=decoder.decode(json.writeValueAsBytes(forged));
            if(bad instanceof SpJsonDecoder.Decoded d&&new CobolLowerer().lower(d.input(),CobolLower.POSITIVE_OPTIONS).publication().isPresent())throw new AssertionError("forged binary result certificate admitted");
        }
        System.out.println("UNCERTAIN_TRUNCATION=PASS: common result, open literal/DATA results, source cause, no invented control, linear receivers and hostile certificates");
    }
}
