package io.github.gustavo2358.lower.adapters.testing;
import io.github.gustavo2358.air.model.*;
import io.github.gustavo2358.lower.adapters.cli.CobolLower;
import io.github.gustavo2358.lower.adapters.sp.SpJsonDecoder;
import io.github.gustavo2358.lower.application.*;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.node.*;
import java.util.*;
import static io.github.gustavo2358.lower.testing.CallOracle.check;
public final class InitialStorageSuite {
    private InitialStorageSuite() { }
    public static void main(String[] args) throws Exception {
        var bytes=Objects.requireNonNull(InitialStorageSuite.class.getResourceAsStream("/sp/storage-212/value-entry.json")).readAllBytes();
        var mapper=new ObjectMapper();var base=mapper.readTree(bytes);var decoded=new SpJsonDecoder(CobolLower.INPUT_LIMITS).decode(bytes);
        check(decoded instanceof SpJsonDecoder.Decoded,"SP2.12 decode: "+decoded);
        var input=((SpJsonDecoder.Decoded)decoded).input();
        check(input.statements().stream().anyMatch(s->s instanceof io.github.gustavo2358.lower.domain.SpInput.PerformFact f&&f.profile()==io.github.gustavo2358.lower.domain.SpInput.PerformProfile.BASIC_PROCEDURE_PERFORM),"fixture must prove BASIC PERFORM, not an opaque observation");
        var p=RegionalTranslationSuite.lower(input).publication().orElseThrow();
        var state=p.units().getFirst().entries().getFirst().state();check(state.conditions().size()==1,"VALUE needs exactly one initial condition, not a statement");
        var c=state.conditions().getFirst();check(c.value() instanceof Entries.LiteralInitial l&&l.value().value().equals(new Values.BytesValue(List.of(215,199,212,240,240,240,240,241))),"VALUE bytes and explicit initial invocation");
        var place=(Places.RegionSlice)c.place();check(integer(place.offset())==2&&integer(place.length())==8,"initial child range is [2,10)");
        check(RegionalTranslationSuite.instructions(p).stream().filter(Operations.Assign.class::isInstance).count()==1,"only source MOVE is executable");
        for(String mode:List.of("UNKNOWN","PRESERVED")) {
            var t=base.deepCopy();var entry=(ObjectNode)t.path("storage").path("entryState");entry.put("mode",mode);var condition=(ObjectNode)entry.path("conditions").get(0);
            condition.put("kind",mode.equals("UNKNOWN")?"UNKNOWN":"PRESERVE");condition.set("bytes",mapper.createArrayNode());if(mode.equals("UNKNOWN"))condition.withArray("gapCodes").add("ENTRY_STATE_NOT_PROVEN");
            var d=(SpJsonDecoder.Decoded)new SpJsonDecoder(CobolLower.INPUT_LIMITS).decode(mapper.writeValueAsBytes(t));var q=RegionalTranslationSuite.lower(d.input()).publication().orElseThrow();
            check(!q.id().equals(p.id()),"entry facts affect publication identity");var v=q.units().getFirst().entries().getFirst().state().conditions().getFirst().value();
            check(mode.equals("UNKNOWN")?v instanceof Entries.ExternalUnknown:v==Entries.Preserve.INSTANCE,"unknown/preserved cannot become a literal");
        }
        for(String bad:List.of("missing","mode","extent","octet","duplicate","foreign")) {
            var t=base.deepCopy();var entry=(ObjectNode)t.path("storage").path("entryState");var c0=(ObjectNode)entry.path("conditions").get(0);
            switch(bad) {case "missing"->((ObjectNode)t.path("storage")).remove("entryState");case "mode"->entry.put("mode","UNKNOWN");
                case "extent"->((ArrayNode)c0.path("bytes")).remove(0);case "octet"->((ArrayNode)c0.path("bytes")).set(0,mapper.getNodeFactory().numberNode(256));
                case "duplicate"->((ArrayNode)entry.path("conditions")).add(c0.deepCopy());case "foreign"->c0.put("node","storage-node:999999");}
            var result=new SpJsonDecoder(CobolLower.INPUT_LIMITS).decode(mapper.writeValueAsBytes(t));
            if(result instanceof SpJsonDecoder.Decoded d)check(new CobolLowerer().lower(d.input(),CobolLower.OPTIONS).status()==LoweringResult.Status.INVALID_INPUT,"negative entry shape "+bad);else check(bad.equals("missing"),"expected semantic admission for "+bad);
        }
        System.out.println("INITIAL_STORAGE=PASS 3 modes/6 negatives/identity/entry-only/byte roundtrip");
    }
    private static int integer(Expression e) {return ((Values.IntValue)((Expressions.Literal)e).value()).value().intValueExact();}
}
