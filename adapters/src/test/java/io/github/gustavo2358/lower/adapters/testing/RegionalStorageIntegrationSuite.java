package io.github.gustavo2358.lower.adapters.testing;

import io.github.gustavo2358.lower.adapters.cli.CobolLower;
import io.github.gustavo2358.lower.adapters.sp.SpJsonDecoder;
import java.util.*;
import java.math.BigInteger;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.github.gustavo2358.lower.domain.SpInput;
import io.github.gustavo2358.lower.application.*;
import io.github.gustavo2358.lower.testing.IfInputs;
import static io.github.gustavo2358.lower.testing.CallOracle.check;

/** Producer fixture is evidence; expected physical offsets and bytes are independent constants. */
public final class RegionalStorageIntegrationSuite {
    private RegionalStorageIntegrationSuite() { }
    static byte[] fixture(String name) throws Exception {
        try(var in=RegionalStorageIntegrationSuite.class.getResourceAsStream("/sp/storage-27/"+name+".json")) {
            return Objects.requireNonNull(in,"missing SP2.7 fixture").readAllBytes();
        }
    }
    public static void main(String[] args) throws Exception { run(); }
    public static void run() throws Exception {
        for(var name:List.of("group-child","nested-filler","copy-capture","unknown-prefix")) {
            var decoded=new SpJsonDecoder(CobolLower.INPUT_LIMITS).decode(fixture(name));
            check(decoded instanceof SpJsonDecoder.Decoded,"SP2.7 physical contract must decode: "+name+" "+decoded);
        }
        var decoder=new SpJsonDecoder(CobolLower.INPUT_LIMITS);var json=new ObjectMapper();
        var base=((SpJsonDecoder.Decoded)decoder.decode(fixture("group-child"))).input();
        var storage=base.storage().orElseThrow();
        check(storage.bases().size()==1 && storage.nodes().size()==2,"physical group inventory");
        check(storage.bases().getFirst().extent().value().orElseThrow().equals(BigInteger.valueOf(8)),"explicit 8-byte base");
        var move=(SpInput.MoveFact)base.statements().getFirst();
        check(move.regionalMove().orElseThrow().bytes().equals(List.of(215,199,212,240,240,240,240,241)),"independent PGM00001 IBM1047 octets");
        for(var mutation:List.of("missing-storage","unknown-field","number-measure","noncanonical-measure","missing-version")) {
            var tree=(ObjectNode)json.readTree(fixture("group-child"));var physical=(ObjectNode)tree.path("storage");
            if(mutation.equals("missing-storage"))tree.remove("storage");
            if(mutation.equals("unknown-field"))physical.put("invented",true);
            if(mutation.equals("number-measure"))((ObjectNode)physical.path("bases").get(0).path("extent")).put("value",8);
            if(mutation.equals("noncanonical-measure"))((ObjectNode)physical.path("bases").get(0).path("extent")).put("value","08");
            if(mutation.equals("missing-version"))physical.remove("version");
            check(decoder.decode(json.writeValueAsBytes(tree)) instanceof SpJsonDecoder.Rejected,"physical error: "+mutation);
        }
        var bases=new ArrayList<>(storage.bases());var b=bases.getFirst();
        bases.set(0,IfInputs.with(b,"extent",IfInputs.with(b.extent(),"value",Optional.of(BigInteger.valueOf(7)))));
        invalid(IfInputs.with(base,"storage",Optional.of(IfInputs.with(storage,"bases",bases))),"out-of-bounds view");
        invalid(IfInputs.with(base,"storage",Optional.of(IfInputs.with(storage,"bases",List.of()))),"dangling base");
        invalid(IfInputs.with(base,"storage",Optional.empty()),"regional access without environment");
        var wrongBytes=IfInputs.with(move,"regionalMove",Optional.of(IfInputs.with(move.regionalMove().orElseThrow(),"bytes",List.of(193,193,193,193,193,193,193,193))));
        invalid(IfInputs.with(base,"statements",List.of(wrongBytes,base.statements().get(1),base.statements().get(2))),"same-length bytes disagree with logical source");
        var target=move.target();var call=((SpInput.DataCallTarget)((SpInput.CallFact)base.statements().get(1)).target()).reference();
        var wrongView=IfInputs.with(move,"target",IfInputs.with(target,"regionalAccess",call.regionalAccess()));
        invalid(IfInputs.with(base,"statements",List.of(wrongView,base.statements().get(1),base.statements().get(2))),"access disagrees with nominal selection");
        invalid(IfInputs.with(base,"storage",Optional.of(IfInputs.with(storage,"runtimeCodec",Optional.of("text.ascii@1")))),"conflicting runtime codec");
        invalid(IfInputs.with(base,"storage",Optional.of(IfInputs.with(storage,"profile",io.github.gustavo2358.lower.domain.StorageFacts.Profile.UNSPECIFIED))),"known physical proof without selected profile");
        var nodes=new ArrayList<>(storage.nodes());var root=nodes.getFirst();var child=nodes.get(1);
        nodes.set(0,IfInputs.with(root,"parent",Optional.of(child.id())));
        invalid(IfInputs.with(base,"storage",Optional.of(IfInputs.with(storage,"nodes",nodes))),"cyclic parent");
        nodes=new ArrayList<>(storage.nodes());nodes.add(root);
        invalid(IfInputs.with(base,"storage",Optional.of(IfInputs.with(storage,"nodes",nodes))),"duplicate node");
        var unknown=((SpJsonDecoder.Decoded)decoder.decode(fixture("unknown-prefix"))).input();var us=unknown.storage().orElseThrow();
        var ub=us.bases().getFirst();var ubases=List.of(IfInputs.with(ub,"extent",IfInputs.with(ub.extent(),"gapCodes",List.of())));
        invalid(IfInputs.with(unknown,"storage",Optional.of(IfInputs.with(us,"bases",ubases))),"unknown extent without reason");
        var copy=((SpJsonDecoder.Decoded)decoder.decode(fixture("copy-capture"))).input();var cs=copy.storage().orElseThrow();
        var cbases=cs.bases().stream().map(x->IfInputs.with(x,"allocation",io.github.gustavo2358.lower.domain.StorageFacts.Allocation.UNPROVEN)).toList();
        invalid(IfInputs.with(copy,"storage",Optional.of(IfInputs.with(cs,"bases",cbases))),"distinct identifiers do not prove disjoint byte copy");
        var before=new CobolLowerer().lower(base,CobolLower.OPTIONS);
        check(before.status()==LoweringResult.Status.SUCCESS,"conservative SP2.7 publication remains valid before precise lowering");
        var larger=IfInputs.with(b,"extent",IfInputs.with(b.extent(),"value",Optional.of(BigInteger.valueOf(9))));
        var changed=IfInputs.with(base,"storage",Optional.of(IfInputs.with(storage,"bases",List.of(larger))));
        var after=new CobolLowerer().lower(changed,CobolLower.OPTIONS);
        check(after.status()==LoweringResult.Status.SUCCESS,"larger enclosing region is valid explicit input");
        check(!before.publication().orElseThrow().id().equals(after.publication().orElseThrow().id()),"physical facts must participate in publication identity");
        var reordered=new ArrayList<>(storage.nodes());Collections.reverse(reordered);var reorderedViews=new ArrayList<>(storage.views());Collections.reverse(reorderedViews);
        var permuted=IfInputs.with(base,"storage",Optional.of(IfInputs.with(IfInputs.with(storage,"nodes",reordered),"views",reorderedViews)));
        var permutation=new CobolLowerer().lower(permuted,CobolLower.OPTIONS);
        check(permutation.status()==LoweringResult.Status.SUCCESS&&permutation.publication().orElseThrow().id().equals(before.publication().orElseThrow().id()),"unordered physical inventories have canonical identity");
        System.out.println("LOWER_REGIONAL_STORAGE_WIRE_CASES=22");
    }
    private static void invalid(SpInput input,String reason) {
        var result=new EntryGobackAdmission().admit(input,CobolLower.OPTIONS.admission());
        check(result.status()==Admission.Status.INVALID_INPUT,"core must reject "+reason+": "+result.status());
    }
}
