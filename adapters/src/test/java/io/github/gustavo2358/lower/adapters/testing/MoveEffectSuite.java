package io.github.gustavo2358.lower.adapters.testing;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.node.*;
import io.github.gustavo2358.lower.adapters.sp.SpJsonDecoder;
import io.github.gustavo2358.lower.adapters.cli.CobolLower;
import io.github.gustavo2358.lower.domain.SpInput.*;
import java.util.*;
/** A bounded receiving footprint never becomes a full-overwrite certificate. */
public final class MoveEffectSuite {
    public static void main(String[] args)throws Exception {
        var j=new ObjectMapper();ObjectNode source;
        try(var in=MoveEffectSuite.class.getResourceAsStream("/sp/move-effects/current-date.json")){source=(ObjectNode)j.readTree(in);}
        var decoder=new SpJsonDecoder(CobolLower.INPUT_LIMITS);var d=decoder.decode(j.writeValueAsBytes(source));
        if(!(d instanceof SpJsonDecoder.Decoded good))throw new AssertionError(d);
        var e=good.input().statements().stream().filter(OtherStatement.class::isInstance).map(OtherStatement.class::cast)
            .flatMap(s->s.effects().stream()).filter(s->s.proof()==EffectProof.MOVE_TARGETS).findFirst().orElseThrow();
        if(e.mayWrites().size()!=1||!e.mustOverwrite().isEmpty())throw new AssertionError("receiving MAY bound");
        for(var mode:List.of("downgrade","must","exposure","value")) {
            var copy=source.deepCopy();
            if(mode.equals("downgrade"))copy.put("contractVersion","2.57.0");
            else for(var raw:copy.path("statementEffects"))if(raw.path("proof").asText().equals("MOVE_TARGETS")) {
                var effect=(ObjectNode)raw;
                switch(mode) {case "must"->effect.set("mustOverwrite",effect.path("mayWrites").deepCopy());
                    case "exposure"->effect.put("unknownExposureBound","ALL");case "value"->effect.put("values","NONE");}
            }
            if(!(decoder.decode(j.writeValueAsBytes(copy)) instanceof SpJsonDecoder.Rejected))throw new AssertionError("forged "+mode);
        }
        try {new EffectSummary(e.knownReads(),e.mayWrites(),e.mayWrites(),e.exposedRegions(),e.unknownReadBound(),e.unknownWriteBound(),e.unknownExposureBound(),e.environment(),e.values(),e.proof());throw new AssertionError("typed MUST forged");}
        catch(IllegalArgumentException expected) { }
        System.out.println("PASS MoveEffectSuite");
    }
}
