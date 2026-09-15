package io.github.gustavo2358.lower.adapters.testing;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.node.*;
import io.github.gustavo2358.air.model.*;
import io.github.gustavo2358.air.json.AirJson;
import io.github.gustavo2358.lower.adapters.cli.CobolLower;
import io.github.gustavo2358.lower.adapters.sp.SpJsonDecoder;
import io.github.gustavo2358.lower.application.*;
import java.util.*;
import static io.github.gustavo2358.lower.testing.CallOracle.check;

/** Real RF-W1 frontend product; expected meaning is independent of the lower. */
public final class PossibleEntrySuite {
    private PossibleEntrySuite() { }
    public static void main(String[] args) throws Exception {
        var bytes=Objects.requireNonNull(PossibleEntrySuite.class.getResourceAsStream("/sp/recall/possible-entry.json")).readAllBytes();
        var mapper=new ObjectMapper();var base=mapper.readTree(bytes);
        var decoded=new SpJsonDecoder(CobolLower.INPUT_LIMITS).decode(bytes);
        check(decoded instanceof SpJsonDecoder.Decoded,"SP2.16 possible entry must decode: "+decoded);
        var input=((SpJsonDecoder.Decoded)decoded).input();var result=RegionalTranslationSuite.lower(input);
        check(result.publication().isPresent(),"possible entry must lower: "+result.status());
        var p=result.publication().orElseThrow();var state=p.units().getFirst().entries().getFirst().state();
        var condition=state.conditions().getFirst();
        check(condition.value() instanceof Entries.PossibleLiterals,"possible entry must never strengthen to literal");
        var value=(Entries.PossibleLiterals)condition.value();
        check(value.candidates().stream().map(Expressions.Literal::value).toList().equals(List.of(new Values.BytesValue(List.of(215,217,214,199,193,64,64,64)))),"source bytes conserved");
        check(p.uncertainties().stream().anyMatch(u->u.id().equals(value.remainder())&&u.dimensions().contains(Evidence.Dimension.VALUES)),"entry remainder materialized");
        check(p.capabilities().required().contains(Capabilities.ENTRY_POSSIBILITIES),"required entry capability");
        check(RegionalTranslationSuite.instructions(p).stream().noneMatch(o->o instanceof Operations.Assign||o instanceof Operations.CopyBytes),"no synthetic VALUE write");
        var codec=new AirJson();check(p.equals(codec.decode(codec.encode(p))),"entry possibility survives production codec");
        for(var bad:List.of("proof","closed","extent","mode","allocation","old-version")) {
            var t=base.deepCopy();var entry=(ObjectNode)t.path("storage").path("entryState");var c=(ObjectNode)entry.path("conditions").get(0);
            switch(bad) {
                case "proof"->c.put("proof","DECLARATIVE_INVARIANT");
                case "closed"->c.set("gapCodes",mapper.createArrayNode());
                case "extent"->((ArrayNode)c.path("bytes")).remove(0);
                case "mode"->entry.put("mode","INITIAL");
                case "allocation"->((ObjectNode)t.path("storage").path("bases").get(0)).put("allocation","UNPROVEN");
                case "old-version"->{((ObjectNode)t).put("contractVersion","2.15.0");((ObjectNode)t.path("storage")).put("version","1.4.0");}
            }
            var d=new SpJsonDecoder(CobolLower.INPUT_LIMITS).decode(mapper.writeValueAsBytes(t));
            if(d instanceof SpJsonDecoder.Decoded v)check(new CobolLowerer().lower(v.input(),CobolLower.OPTIONS).status()==LoweringResult.Status.INVALID_INPUT,"typed rejection "+bad);
            else check(d instanceof SpJsonDecoder.Rejected,"explicit wire rejection "+bad);
        }
        System.out.println("POSSIBLE_ENTRY=PASS real SP fixture/typed possibility/remainder/no synthetic MOVE/codec/6 negatives");
    }
}
