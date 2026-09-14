package io.github.gustavo2358.lower.adapters.testing;
import io.github.gustavo2358.air.model.*;
import io.github.gustavo2358.lower.adapters.cli.CobolLower;
import io.github.gustavo2358.lower.adapters.sp.SpJsonDecoder;
import java.util.*;
import static io.github.gustavo2358.lower.testing.CallOracle.check;
public final class CorrespondingStorageSuite {
    private CorrespondingStorageSuite() { }
    public static void main(String[] args) throws Exception {
        var bytes=Objects.requireNonNull(CorrespondingStorageSuite.class.getResourceAsStream("/sp/storage-211/corresponding.json")).readAllBytes();
        var decoded=new SpJsonDecoder(CobolLower.INPUT_LIMITS).decode(bytes);check(decoded instanceof SpJsonDecoder.Decoded,"corresponding SP decodes: "+decoded);
        var input=((SpJsonDecoder.Decoded)decoded).input();var p=RegionalTranslationSuite.lower(input).publication().orElseThrow();
        check(p.storage().size()==2,"two allocations, no pair or alias allocation");
        var ops=RegionalTranslationSuite.instructions(p);check(ops.size()==4,"two initializers and exactly two pair writes");
        var fits=ops.stream().filter(Operations.Assign.class::isInstance).map(Operations.Assign.class::cast).filter(a->a.value() instanceof Expressions.FitText).toList();
        check(fits.size()==2,"two textual pair transformations, never whole-group copy");
        var from=new ArrayList<Integer>();var to=new ArrayList<Integer>();var lengths=new ArrayList<Integer>();
        for(var assign:fits) {
            var fit=(Expressions.FitText)assign.value();var read=(Expressions.Read)fit.value();var source=(Places.RegionSlice)read.place();var dest=(Places.RegionSlice)assign.destination();
            from.add(integer(source.offset()));to.add(integer(dest.offset()));lengths.add(fit.length().intValueExact());
            check(!source.region().equals(dest.region()),"source and destination retain distinct allocation identities");
        }
        check(from.equals(List.of(0,3))&&to.equals(List.of(4,0))&&lengths.equals(List.of(4,1)),"constant pair ranges and source order: "+from+" "+to+" "+lengths);
        // Nominal spelling is presentation: changing it cannot alter the physical AIR.
        var json=new com.fasterxml.jackson.databind.ObjectMapper();var tree=json.readTree(bytes);
        rename(tree);var renamed=(SpJsonDecoder.Decoded)new SpJsonDecoder(CobolLower.INPUT_LIMITS).decode(json.writeValueAsBytes(tree));
        var q=RegionalTranslationSuite.lower(renamed.input()).publication().orElseThrow();
        check(shape(RegionalTranslationSuite.instructions(q)).equals(shape(ops)),"lower never matches names; publication/operation identities may change");
        System.out.println("CORRESPONDING=PASS pairs=2 srcOffsets=[0,3] dstOffsets=[4,0] lengths=[4,1]; generic AIR roundtrip");
    }
    private static List<List<Integer>> shape(List<Instruction> ops) {
        return ops.stream().filter(Operations.Assign.class::isInstance).map(Operations.Assign.class::cast)
            .filter(a->a.value() instanceof Expressions.FitText).map(a->{var fit=(Expressions.FitText)a.value();
                return List.of(integer(((Places.RegionSlice)((Expressions.Read)fit.value()).place()).offset()),integer(((Places.RegionSlice)a.destination()).offset()),fit.length().intValueExact());}).toList();
    }
    private static int integer(Expression e) {return ((Values.IntValue)((Expressions.Literal)e).value()).value().intValueExact();}
    private static void rename(com.fasterxml.jackson.databind.JsonNode node) {
        if(node.isObject()&&node.has("canonicalName"))((com.fasterxml.jackson.databind.node.ObjectNode)node).put("canonicalName","DISPLAY-NAME");
        node.elements().forEachRemaining(CorrespondingStorageSuite::rename);
    }
}
