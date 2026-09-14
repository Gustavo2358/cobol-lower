package io.github.gustavo2358.lower.adapters.testing;

import io.github.gustavo2358.air.model.*;
import io.github.gustavo2358.lower.adapters.cli.CobolLower;
import io.github.gustavo2358.lower.adapters.sp.SpJsonDecoder;
import io.github.gustavo2358.lower.application.CobolLowerer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.math.BigInteger;
import java.nio.file.*;
import java.util.*;
import static io.github.gustavo2358.lower.testing.CallOracle.check;

public final class SliceStorageSuite {
    private SliceStorageSuite() { }
    public static void main(String[] args) throws Exception {
        byte[] bytes=args.length==0?Objects.requireNonNull(SliceStorageSuite.class.getResourceAsStream("/sp/storage-210/slice.json")).readAllBytes():Files.readAllBytes(Path.of(args[0]));
        var decoder=new SpJsonDecoder(CobolLower.INPUT_LIMITS);var decoded=decoder.decode(bytes);
        check(decoded instanceof SpJsonDecoder.Decoded,"SP2.10 must decode: "+decoded);
        var result=RegionalTranslationSuite.lower(((SpJsonDecoder.Decoded)decoded).input());var p=result.publication().orElseThrow();
        var assign=(Operations.Assign)RegionalTranslationSuite.instructions(p).getFirst();
        check(assign.destination() instanceof Places.RegionSlice,"partial write must use AIR RegionSlice");
        var write=(Places.RegionSlice)assign.destination();range(write);
        var invoke=p.units().getFirst().sequences().stream().map(Sequence::terminator).filter(Operations.Invoke.class::isInstance).map(Operations.Invoke.class::cast).findFirst().orElseThrow();
        check(invoke.target() instanceof Interactions.ComputedTarget,"slice CALL remains computed");
        var read=(Expressions.Read)((Interactions.ComputedTarget)invoke.target()).name();
        check(read.place() instanceof Places.RegionSlice,"CALL reads slice, not whole item");range((Places.RegionSlice)read.place());
        check(p.storage().size()==1,"one source base with aliases and slices");
        var json=new ObjectMapper();
        for(var values:List.of(List.of("1","3"),List.of("7","3"),List.of("3","0"),List.of("-1","3"),List.of("03","3"))) {
            var tree=(ObjectNode)json.readTree(bytes);var slice=(ObjectNode)tree.path("statements").get(0).path("target").path("regionalAccess").path("slice");
            slice.put("offset",values.get(0));slice.put("extent",values.get(1));var d=decoder.decode(json.writeValueAsBytes(tree));
            check(d instanceof SpJsonDecoder.Rejected||new CobolLowerer().lower(((SpJsonDecoder.Decoded)d).input(),CobolLower.OPTIONS).publication().isEmpty(),"out-of-item/noncanonical slice rejected: "+values);
        }
        var changed=(ObjectNode)json.readTree(bytes);
        ((ObjectNode)changed.path("statements").get(1).path("target").path("reference").path("regionalAccess").path("slice")).put("offset","4");
        var changedInput=((SpJsonDecoder.Decoded)decoder.decode(json.writeValueAsBytes(changed))).input();
        var changedPublication=RegionalTranslationSuite.lower(changedInput).publication().orElseThrow();
        check(!p.id().equals(changedPublication.id()),"changing a slice must change publication identity");
        System.out.println("SLICE_CONTRACT=PASS write/call=[3,6) negatives=5");
    }
    private static void range(Places.RegionSlice slice) {
        check(((Values.IntValue)((Expressions.Literal)slice.offset()).value()).value().equals(BigInteger.valueOf(3)),"offset=3");
        check(((Values.IntValue)((Expressions.Literal)slice.length()).value()).value().equals(BigInteger.valueOf(3)),"extent=3");
    }
}
