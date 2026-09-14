package io.github.gustavo2358.lower.adapters.testing;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.node.*;
import io.github.gustavo2358.air.model.*;
import io.github.gustavo2358.lower.adapters.cli.CobolLower;
import io.github.gustavo2358.lower.adapters.sp.SpJsonDecoder;
import io.github.gustavo2358.lower.application.*;
import java.util.*;
import static io.github.gustavo2358.lower.testing.CallOracle.check;

/** Production frontend fixture, independent entry-value/shape/provenance assertions. */
public final class DeclarativeValueSuite {
    private DeclarativeValueSuite() { }
    public static void main(String[] args) throws Exception {
        var bytes=Objects.requireNonNull(DeclarativeValueSuite.class.getResourceAsStream("/sp/dvi/invariant.json")).readAllBytes();
        var mapper=new ObjectMapper();var base=mapper.readTree(bytes);
        var result=new SpJsonDecoder(CobolLower.INPUT_LIMITS).decode(bytes);
        check(result instanceof SpJsonDecoder.Decoded,"SP2.15 source fixture must decode: "+result);
        var input=((SpJsonDecoder.Decoded)result).input();var p=RegionalTranslationSuite.lower(input).publication().orElseThrow();
        var state=p.units().getFirst().entries().getFirst().state();check(state.conditions().size()==1,"one entry VALUE");
        var condition=state.conditions().getFirst();
        check(condition.place() instanceof Places.RegionSlice&&condition.value() instanceof Entries.LiteralInitial l
            &&l.value().value().equals(new Values.BytesValue(List.of(215,217,214,199,193,64,64,64))),"existing regional entry representation retains exact IBM1047 bytes");
        check(RegionalTranslationSuite.instructions(p).stream().noneMatch(o->o instanceof Operations.Assign||o instanceof Operations.CopyBytes),"VALUE is not executable");
        var origin=p.origins().stream().filter(o->o.id().equals(condition.origin())).findFirst().orElseThrow();
        check(origin instanceof Origins.Derived d&&d.rule().equals("storage@1.4/entry-mode=UNKNOWN; proof=DECLARATIVE_INVARIANT; source-proved invocation condition"),"proof kind/version reaches AIR origin");
        var changedProof=base.deepCopy();((ObjectNode)changedProof.path("storage").path("entryState").path("conditions").get(0)).put("proof","PROGRAM_INITIAL");
        var other=(SpJsonDecoder.Decoded)new SpJsonDecoder(CobolLower.INPUT_LIMITS).decode(mapper.writeValueAsBytes(changedProof));
        var q=RegionalTranslationSuite.lower(other.input()).publication().orElseThrow();
        check(!p.id().equals(q.id()),"source proof is material publication identity");
        for(var bad:List.of("proof-missing","proof-unknown","proof-kind","proof-mode","old-version","storage-version","extent","locality")) {
            var t=base.deepCopy();var entry=(ObjectNode)t.path("storage").path("entryState");var c=(ObjectNode)entry.path("conditions").get(0);
            switch(bad) {
                case "proof-missing"->c.remove("proof");case "proof-unknown"->c.put("proof","LOOKS_CONSTANT");
                case "proof-kind"->c.put("proof","NONE");case "proof-mode"->entry.put("mode","INITIAL");
                case "old-version"->((ObjectNode)t).put("contractVersion","2.14.0");
                case "storage-version"->((ObjectNode)t.path("storage")).put("version","1.3.0");
                case "extent"->((ArrayNode)c.path("bytes")).remove(0);
                case "locality"->((ObjectNode)t.path("storage").path("bases").get(0)).put("allocation","UNPROVEN");
            }
            var decoded=new SpJsonDecoder(CobolLower.INPUT_LIMITS).decode(mapper.writeValueAsBytes(t));
            if(decoded instanceof SpJsonDecoder.Decoded d)check(new CobolLowerer().lower(d.input(),CobolLower.OPTIONS).status()==LoweringResult.Status.INVALID_INPUT,"typed admission rejects "+bad);
            else check(decoded instanceof SpJsonDecoder.Rejected,"wire rejection for "+bad);
        }
        System.out.println("DVI=PASS source fixture/entry literal/proof origin/no synthetic MOVE/8 negatives");
    }
}
