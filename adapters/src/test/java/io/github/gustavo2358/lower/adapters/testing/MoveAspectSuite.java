package io.github.gustavo2358.lower.adapters.testing;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import io.github.gustavo2358.air.json.AirJson;
import io.github.gustavo2358.air.model.*;
import io.github.gustavo2358.lower.adapters.cli.CobolLower;
import io.github.gustavo2358.lower.adapters.sp.SpJsonDecoder;
import io.github.gustavo2358.lower.application.CobolLowerer;
import java.util.*;

/** SP2.38 independent logical receiver and strict historical boundary. */
public final class MoveAspectSuite {
    private MoveAspectSuite() { }
    private static void need(boolean yes,String why) { if(!yes)throw new AssertionError(why); }
    private static String memorySignature(Instruction instruction) {
        if(instruction instanceof Operations.Assign a&&a.destination() instanceof Places.ObjectPlace object
                &&a.value() instanceof Expressions.Literal literal&&literal.value() instanceof Values.TextValue text)
            return "TEXT_ASSIGN:"+object.object().localId()+":"+text.value();
        return instruction.getClass().getSimpleName();
    }
    public static void main(String[] args) throws Exception {
        byte[] raw=Objects.requireNonNull(MoveAspectSuite.class.getResourceAsStream("/sp/w8/move-good-gap.json")).readAllBytes();
        var decoder=new SpJsonDecoder(CobolLower.INPUT_LIMITS);
        var decoded=decoder.decode(raw);need(decoded instanceof SpJsonDecoder.Decoded,"SP2.38 decode");
        var publication=new CobolLowerer().lower(((SpJsonDecoder.Decoded)decoded).input(),CobolLower.OPTIONS).publication().orElseThrow();
        var instructions=RegionalTranslationSuite.instructions(publication);
        need(instructions.stream().filter(Operations.Assign.class::isInstance).map(Operations.Assign.class::cast)
            .anyMatch(a->a.value() instanceof Expressions.Literal l&&l.value() instanceof Values.TextValue t
                &&t.value().equals("PROGA001")&&a.destination() instanceof Places.ObjectPlace),
            "supported receiver retains its exact logical assignment");
        need(instructions.stream().anyMatch(Operations.Nop.class::isInstance),"unavailable peer remains a gap");
        need(publication.equals(new AirJson().decode(new AirJson().encode(publication))),"AIR codec roundtrip");
        var mapper=new ObjectMapper();
        for(String mutation:List.of("old-version","bad-value","missing-access","wrong-target","missing-regional","deleted-proof")) {
            var tree=(ObjectNode)mapper.readTree(raw);
            var move=(ObjectNode)tree.path("statements").get(0);
            switch(mutation) {
                case "old-version"->tree.put("contractVersion","2.37.0");
                case "bad-value"->((ObjectNode)move.path("logicalTransfers").get(0).path("value")).put("value","DEAD0001");
                case "missing-access"->((ObjectNode)move.path("target")).putNull("logicalWholeItem");
                case "wrong-target"->((ObjectNode)move.path("logicalTransfers").get(0)).put("target","operand:0:99");
                case "missing-regional"->move.putNull("regionalMove");
                case "deleted-proof"->move.remove("logicalTransfers");
            }
            var result=decoder.decode(mapper.writeValueAsBytes(tree));
            if(mutation.equals("deleted-proof")) {
                need(result instanceof SpJsonDecoder.Decoded,"partial regional sequence remains a valid SP2.38 shape");
                var lowered=new CobolLowerer().lower(((SpJsonDecoder.Decoded)result).input(),CobolLower.OPTIONS);
                need(lowered.publication().isEmpty()||RegionalTranslationSuite.instructions(lowered.publication().orElseThrow())
                    .stream().noneMatch(i->i instanceof Operations.Assign a&&a.value() instanceof Expressions.Literal l
                        &&l.value() instanceof Values.TextValue t&&t.value().equals("PROGA001")),"deleting proof removes positive assignment");
            } else need(result instanceof SpJsonDecoder.Rejected ||new CobolLowerer().lower(((SpJsonDecoder.Decoded)result).input(),CobolLower.OPTIONS).publication().isEmpty(),
                "mutation must fail: "+mutation);
        }
        var exemplar=((ArrayNode)((ObjectNode)mapper.readTree(raw)).path("gaps")).get(0);
        for(int count:List.of(0,1,50)) {
            var tree=(ObjectNode)mapper.readTree(raw);var gaps=(ArrayNode)tree.path("gaps");
            for(int i=0;i<count;i++) {
                var diagnostic=(ObjectNode)exemplar.deepCopy();diagnostic.put("code","W8_INDEPENDENT_DIAGNOSTIC_"+i);
                gaps.add(diagnostic);
            }
            var changed=decoder.decode(mapper.writeValueAsBytes(tree));
            need(changed instanceof SpJsonDecoder.Decoded,"diagnostic count is not an effect: "+count);
            var after=new CobolLowerer().lower(((SpJsonDecoder.Decoded)changed).input(),CobolLower.OPTIONS).publication().orElseThrow();
            need(RegionalTranslationSuite.instructions(after).stream().map(MoveAspectSuite::memorySignature).toList()
                .equals(instructions.stream().map(MoveAspectSuite::memorySignature).toList()),"diagnostics do not change memory operations: "+count);
        }
        System.out.println("MOVE_ASPECT_SP238=PASS logical receiver, gap, codec, 6 mutations, diagnostic counts 0/1/50");
    }
}
