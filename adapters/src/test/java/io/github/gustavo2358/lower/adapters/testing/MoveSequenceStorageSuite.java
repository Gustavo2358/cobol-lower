package io.github.gustavo2358.lower.adapters.testing;
import io.github.gustavo2358.air.model.*;
import io.github.gustavo2358.lower.adapters.cli.CobolLower;
import io.github.gustavo2358.lower.adapters.sp.SpJsonDecoder;
import java.nio.file.*;
import java.util.*;
import static io.github.gustavo2358.lower.testing.CallOracle.check;
public final class MoveSequenceStorageSuite {
    private MoveSequenceStorageSuite() { }
    public static void main(String[] args) throws Exception {
        byte[] bytes=args.length==0?Objects.requireNonNull(MoveSequenceStorageSuite.class.getResourceAsStream("/sp/storage-211/move-sequence.json")).readAllBytes():Files.readAllBytes(Path.of(args[0]));
        var decoded=new SpJsonDecoder(CobolLower.INPUT_LIMITS).decode(bytes);check(decoded instanceof SpJsonDecoder.Decoded,"SP2.11 must decode: "+decoded);
        var result=RegionalTranslationSuite.lower(((SpJsonDecoder.Decoded)decoded).input());var publication=result.publication().orElseThrow();
        var assignments=RegionalTranslationSuite.instructions(publication).stream().filter(Operations.Assign.class::isInstance).map(Operations.Assign.class::cast).toList();
        check(assignments.size()==3,"one initialization and two ordered receivers");
        var lengths=new ArrayList<Integer>();
        for(var assign:assignments)if(assign.value() instanceof Expressions.FitText fit) {
            lengths.add(fit.length().intValueExact());check(fit.pad().equals(" "),"explicit SPACE pad");
            check(fit.value() instanceof Expressions.Read,"DATA source is read as text before fitting");
            check(assign.destination() instanceof Places.RegionSlice,"fit writes a region view");
        }
        check(lengths.equals(List.of(4,1)),"receiver lengths/order preserved: "+lengths);
        var decoder=new SpJsonDecoder(CobolLower.INPUT_LIMITS);var json=new com.fasterxml.jackson.databind.ObjectMapper();
        for(String mutation:List.of("missing","null","unknown","duplicate-id","unavailable","role","no-effect","version","bad-source")) {
            var tree=(com.fasterxml.jackson.databind.node.ObjectNode)json.readTree(bytes);var m=(com.fasterxml.jackson.databind.node.ObjectNode)tree.path("statements").get(1);var t=(com.fasterxml.jackson.databind.node.ObjectNode)m.path("additionalTransfers").get(0);
            switch(mutation) {
                case "missing"->m.remove("additionalTransfers");case "null"->m.putNull("additionalTransfers");case "unknown"->t.put("captureByName",true);
                case "duplicate-id"->((com.fasterxml.jackson.databind.node.ObjectNode)t.path("target")).put("id",m.path("target").path("id").asText());
                case "unavailable"->{var e=(com.fasterxml.jackson.databind.node.ObjectNode)t.path("effect");e.put("kind","UNAVAILABLE");e.putArray("gapCodes").add("UNPROVED");}
                case "role"->((com.fasterxml.jackson.databind.node.ObjectNode)t.path("target")).put("role","READ");
                case "no-effect"->m.putNull("regionalMove");case "version"->tree.put("contractVersion","2.10.0");
                case "bad-source"->((com.fasterxml.jackson.databind.node.ObjectNode)t.path("source").path("reference").path("regionalAccess")).put("view","missing");
            }
            var d=decoder.decode(json.writeValueAsBytes(tree));
            check(d instanceof SpJsonDecoder.Rejected||new io.github.gustavo2358.lower.application.CobolLowerer().lower(((SpJsonDecoder.Decoded)d).input(),CobolLower.OPTIONS).publication().isEmpty(),"malformed transfer rejected: "+mutation);
        }
        for(String name:List.of("move-literal-fit","move-overlap")) {
            var extra=args.length==0?Objects.requireNonNull(MoveSequenceStorageSuite.class.getResourceAsStream("/sp/storage-211/"+name+".json")).readAllBytes():Files.readAllBytes(Path.of(args[0]).resolveSibling(name+".sp.json"));var d=(SpJsonDecoder.Decoded)decoder.decode(extra);
            var p=RegionalTranslationSuite.lower(d.input()).publication().orElseThrow();var instructions=RegionalTranslationSuite.instructions(p);
            if(name.equals("move-overlap"))check(instructions.stream().filter(Operations.HavocMust.class::isInstance).count()==2,"all overlapping source receivers retain unknown values");
            else {
                var values=instructions.stream().filter(Operations.Assign.class::isInstance).map(Operations.Assign.class::cast).map(Operations.Assign::value).map(Expressions.Literal.class::cast).map(Expressions.Literal::value).map(Values.BytesValue.class::cast).map(Values.BytesValue::octets).toList();
                check(values.equals(List.of(List.of(193,194,64,64),List.of(193))),"literal fit padding and truncation byte oracle: "+values);
            }
        }
        System.out.println("MOVE_SEQUENCE=PASS orderedFits=[4,1] AIR roundtrip validated");
    }
}
