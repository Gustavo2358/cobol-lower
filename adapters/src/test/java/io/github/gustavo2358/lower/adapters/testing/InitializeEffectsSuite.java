package io.github.gustavo2358.lower.adapters.testing;

import io.github.gustavo2358.air.model.*;
import io.github.gustavo2358.air.json.AirJson;
import io.github.gustavo2358.lower.adapters.cli.CobolLower;
import io.github.gustavo2358.lower.adapters.sp.SpJsonDecoder;
import java.util.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.*;
import static io.github.gustavo2358.lower.testing.CallOracle.check;

public final class InitializeEffectsSuite {
    public static void main(String[] args) throws Exception {
        for(var key:List.of("initialize","initialize-group")) {
            var bytes=Objects.requireNonNull(InitializeEffectsSuite.class.getResourceAsStream("/sp/recall/"+key+"-effects.json")).readAllBytes();
            var d=new SpJsonDecoder(CobolLower.INPUT_LIMITS).decode(bytes);
            check(d instanceof SpJsonDecoder.Decoded,"typed INITIALIZE effect reader");
            var result=RegionalTranslationSuite.lower(((SpJsonDecoder.Decoded)d).input());
            check(result.publication().isPresent(),"INITIALIZE effect lower: "+result.status());var p=result.publication().orElseThrow();
            var op=p.units().getFirst().sequences().stream().map(s->s.terminator()).filter(Operations.Opaque.class::isInstance).map(Operations.Opaque.class::cast).findFirst().orElseThrow();
            var m=op.envelope().memory();
            check(m.knownWrites().size()==1&&m.otherWrites() instanceof Scopes.NoMemory,"known destination bounds writes");
            check(m.mustOverwrite().size()==(key.equals("initialize")?1:0),"elementary MUST/group MAY");
            var codec=new AirJson();check(codec.decode(codec.encode(p)).equals(p),"MUST wire roundtrip");
            if(key.equals("initialize-group")) {
                var mapper=new ObjectMapper();var wire=(ObjectNode)mapper.readTree(bytes);var e=(ObjectNode)wire.path("statementEffects").get(0);
                e.set("mustOverwrite",e.path("mayWrites").deepCopy());
                var changed=new SpJsonDecoder(CobolLower.INPUT_LIMITS).decode(mapper.writeValueAsBytes(wire));
                check(changed instanceof SpJsonDecoder.Decoded,"forged shape reaches semantic admission");
                var rejected=new io.github.gustavo2358.lower.application.CobolLowerer().lower(((SpJsonDecoder.Decoded)changed).input(),CobolLower.OPTIONS);
                check(rejected.status()==io.github.gustavo2358.lower.application.LoweringResult.Status.INVALID_INPUT,"group cannot forge whole MUST");
            }
        }
        System.out.println("INITIALIZE_EFFECTS=PASS");
    }
}
