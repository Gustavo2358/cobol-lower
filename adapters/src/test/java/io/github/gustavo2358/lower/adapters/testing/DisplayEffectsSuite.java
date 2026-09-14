package io.github.gustavo2358.lower.adapters.testing;

import io.github.gustavo2358.air.model.*;
import io.github.gustavo2358.air.json.AirJson;
import io.github.gustavo2358.lower.adapters.cli.CobolLower;
import io.github.gustavo2358.lower.adapters.sp.SpJsonDecoder;
import java.util.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.*;
import static io.github.gustavo2358.lower.testing.CallOracle.check;

/** Source-produced SP; memory knowledge stays separate from output/control effects. */
public final class DisplayEffectsSuite {
    public static void main(String[] args) throws Exception {
        for(var key:List.of("display","display-read")) {
            var bytes=Objects.requireNonNull(DisplayEffectsSuite.class.getResourceAsStream("/sp/recall/"+key+"-effects.json")).readAllBytes();
            var decoded=new SpJsonDecoder(CobolLower.INPUT_LIMITS).decode(bytes);
            check(decoded instanceof SpJsonDecoder.Decoded,"SP2.17 must decode: "+decoded);
            var result=RegionalTranslationSuite.lower(((SpJsonDecoder.Decoded)decoded).input());
            check(result.publication().isPresent(),"effects must lower: "+result);
            var p=result.publication().orElseThrow();
            var opaque=p.units().stream().flatMap(u->u.sequences().stream()).map(s->s.terminator())
                .filter(Operations.Opaque.class::isInstance).map(Operations.Opaque.class::cast).findFirst().orElseThrow();
            var memory=opaque.envelope().memory();
            check(memory.otherWrites() instanceof Scopes.NoMemory,"DISPLAY no memory havoc");
            check(memory.knownWrites().isEmpty()&&memory.mustOverwrite().isEmpty(),"DISPLAY does not write");
            check(memory.knownReads().size()==(key.equals("display-read")?1:0),"canonical DISPLAY reads");
            check(!(opaque.envelope().dependencies().remainder() instanceof Scopes.NoResources),"output/environment effect remains open");
            var codec=new AirJson();check(codec.decode(codec.encode(p)).equals(p),"effect envelope survives JSON");
        }
        var mapper=new ObjectMapper();var base=mapper.readTree(Objects.requireNonNull(DisplayEffectsSuite.class.getResourceAsStream("/sp/recall/display-read-effects.json")).readAllBytes());
        for(var kind:List.of("unknown-owner","duplicate-owner","dangling-read","write-proof","missing-inventory","old-version")) {
            var t=(ObjectNode)base.deepCopy();var effects=(ArrayNode)t.path("statementEffects");var e=(ObjectNode)effects.get(0);
            switch(kind) {
                case "unknown-owner"->e.put("statement","statement:missing");
                case "duplicate-owner"->effects.add(e.deepCopy());
                case "dangling-read"->((ArrayNode)e.path("knownReads")).add("operand:missing");
                case "write-proof"->e.set("mayWrites",e.path("knownReads").deepCopy());
                case "missing-inventory"->t.remove("statementEffects");
                case "old-version"->t.put("contractVersion","2.16.0");
            }
            var d=new SpJsonDecoder(CobolLower.INPUT_LIMITS).decode(mapper.writeValueAsBytes(t));
            if(d instanceof SpJsonDecoder.Decoded v)check(new io.github.gustavo2358.lower.application.CobolLowerer().lower(v.input(),CobolLower.OPTIONS).status()==io.github.gustavo2358.lower.application.LoweringResult.Status.INVALID_INPUT,"invalid effect proof: "+kind);
            else check(d instanceof SpJsonDecoder.Rejected,"wire rejects "+kind);
        }
        System.out.println("DISPLAY_EFFECTS=PASS");
    }
}
