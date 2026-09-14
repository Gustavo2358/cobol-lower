package io.github.gustavo2358.lower.adapters.testing;

import io.github.gustavo2358.air.model.*;
import io.github.gustavo2358.lower.adapters.cli.CobolLower;
import io.github.gustavo2358.lower.adapters.sp.SpJsonDecoder;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.math.BigInteger;
import java.nio.file.*;
import java.util.*;
import static io.github.gustavo2358.lower.testing.CallOracle.check;

public final class RenamesStorageSuite {
    private RenamesStorageSuite() { }
    public static void main(String[] args) throws Exception {
        byte[] bytes=args.length==0?Objects.requireNonNull(RenamesStorageSuite.class.getResourceAsStream("/sp/storage-29/renames.json")).readAllBytes():Files.readAllBytes(Path.of(args[0]));var json=new ObjectMapper();
        var decoder=new SpJsonDecoder(CobolLower.INPUT_LIMITS);var decoded=decoder.decode(bytes);
        check(decoded instanceof SpJsonDecoder.Decoded,"SP2.9 fixed RENAMES must decode: "+decoded);
        var input=((SpJsonDecoder.Decoded)decoded).input();var result=RegionalTranslationSuite.lower(input);var p=result.publication().orElseThrow();
        check(p.storage().size()==1&&p.storage().getFirst() instanceof Memory.Region,"RENAMES must never allocate another Region or Cell");
        check(((Memory.Region)p.storage().getFirst()).extent().equals(Optional.of(BigInteger.valueOf(6))),"record extent is 6");
        var objects=p.units().getFirst().objects();check(objects.size()==5,"unused and used aliases survive");
        check(objects.stream().anyMatch(o->o.displayName().equals(Optional.of("TAIL-ALIAS"))&&o.storage() instanceof Memory.ViewBinding v&&v.offset().equals(BigInteger.valueOf(3))&&v.extent().equals(BigInteger.valueOf(3))),"tail alias is [3,6)");
        check(p.coverage().items().stream().filter(c->c.sourceKey().contains("/storage-renames/")).count()==2,"each RENAMES has origin and coverage");
        for(String mutation:List.of("missing","null","unknown-field","dangling","wrong-start","wrong-end","new-base","no-gap")) {
            var tree=(ObjectNode)json.readTree(bytes);var st=(ObjectNode)tree.path("storage");var r=(ObjectNode)st.path("renames").get(0);
            switch(mutation) {
                case "missing"->st.remove("renames");case "null"->st.putNull("renames");case "unknown-field"->r.put("matchByName",true);
                case "dangling"->r.put("from","storage-node:99999");case "wrong-start"->r.set("from",r.path("through"));case "wrong-end"->r.set("through",r.path("from"));
                case "new-base"->{var b=((ObjectNode)st.path("bases").get(0)).deepCopy();b.put("id","storage-base:99999");((com.fasterxml.jackson.databind.node.ArrayNode)st.path("bases")).add(b);for(var v:st.path("views"))if(v.path("node").equals(r.path("owner")))((ObjectNode)v).put("base","storage-base:99999");}
                case "no-gap"->r.put("status","UNPROVEN");
            }
            var d=decoder.decode(json.writeValueAsBytes(tree));
            if(d instanceof SpJsonDecoder.Decoded ok) {
                var invalid=new io.github.gustavo2358.lower.application.CobolLowerer().lower(ok.input(),CobolLower.OPTIONS);
                check(invalid.publication().isEmpty(),"invalid RENAMES rejected at admission: "+mutation);
            } else check(d instanceof SpJsonDecoder.Rejected,"invalid wire rejected: "+mutation);
        }
        System.out.println("RENAMES_CONTRACT=PASS negatives=8 physical=[0,6),[3,6)");
    }
}
