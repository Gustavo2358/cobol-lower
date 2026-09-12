package io.github.gustavo2358.lower.adapters.testing;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.node.*;
import io.github.gustavo2358.air.json.AirJson;
import io.github.gustavo2358.air.validation.AirValidator;
import io.github.gustavo2358.lower.adapters.cli.CobolLower;
import io.github.gustavo2358.lower.adapters.sp.SpJsonDecoder;
import io.github.gustavo2358.lower.application.*;
import io.github.gustavo2358.lower.domain.SpInput;
import io.github.gustavo2358.lower.testing.IfOracle;
import java.nio.file.*;
import java.util.*;

/** Public producer JSON to AIR model/codec boundary, with strict closed-contract countercases. */
public final class IfIntegrationSuite {
    private IfIntegrationSuite() { }
    private static final ObjectMapper JSON=new ObjectMapper();
    public static byte[] fixture(String name) throws Exception {
        try(var in=IfIntegrationSuite.class.getResourceAsStream("/sp/w2b/"+name+".json")) { if(in==null)throw new AssertionError(name);return in.readAllBytes(); }
    }
    public static byte[] positive(byte[] raw) {
        var decoded=new SpJsonDecoder(CobolLower.INPUT_LIMITS).decode(raw);IfOracle.check(decoded instanceof SpJsonDecoder.Decoded,"strict SP1.4 decode: "+decoded);
        var input=((SpJsonDecoder.Decoded)decoded).input();var result=new CobolLowerer().lower(input,CobolLower.OPTIONS);IfOracle.inspect(input,result);
        var codec=new AirJson();var p=result.publication().orElseThrow();var bytes=codec.encode(p);var restored=codec.decode(bytes);
        IfOracle.check(p.equals(restored),"whole AIR model round-trip");IfOracle.check(Arrays.equals(bytes,codec.encode(restored)),"canonical AIR byte-identical re-encode");
        IfOracle.check(AirValidator.validate(p).equals(AirValidator.validate(restored)),"all validator issues/obligations preserved by codec");
        return bytes;
    }
    public static int focal() throws Exception {
        int before=IfOracle.assertions();for(var name:List.of("closed","open"))positive(fixture(name));
        int count=IfOracle.assertions()-before;System.out.println("LOWER_IF_INTEGRATION_FAST_TESTS="+count);return count;
    }
    public static int run() throws Exception {
        int before=IfOracle.assertions();focal();MoveDataIntegrationSuite.run(); PerformIntegrationSuite.run();
        var base=(ObjectNode)JSON.readTree(fixture("closed"));
        for(String pointer:List.of("/storageIndependence","/statements/0/condition/predicate","/statements/0/thenArm","/statements/0/elseArm")) {
            var changed=base.deepCopy();((ObjectNode)changed.at(pointer)).put("futureField",true);reject(changed,"future property "+pointer);
        }
        for(var pair:List.of(new String[]{"/statements/0/condition/predicate","truthValue","FALSE"},new String[]{"/statements/0/condition/predicate","resultDomain","NUMBER"},new String[]{"/storageIndependence","rule","DISTINCT_IDS"})) {
            var changed=base.deepCopy();((ObjectNode)changed.at(pair[0])).put(pair[1],pair[2]);reject(changed,"closed enum "+pair[1]);
        }
        for(String name:List.of("storageIndependence")) { var changed=base.deepCopy();changed.remove(name);reject(changed,"missing "+name); }
        var changed=base.deepCopy();((ObjectNode)changed.at("/statements/0/condition/predicate")).putNull("knownReads");reject(changed,"null knownReads");
        int count=IfOracle.assertions()-before;System.out.println("LOWER_IF_INTEGRATION_TESTS="+count);return count;
    }
    private static void reject(ObjectNode json,String why) throws Exception {
        var decoded=new SpJsonDecoder(CobolLower.INPUT_LIMITS).decode(JSON.writeValueAsBytes(json));
        IfOracle.check(decoded instanceof SpJsonDecoder.Rejected r && r.diagnostic().code()==SpJsonDecoder.Code.INPUT_ERROR,"physical reject "+why+": "+decoded);
    }
    public static void main(String[] args) throws Exception {
        if(args.length==0)run();else { var raw=Files.readAllBytes(Path.of(args[0]));Files.write(Path.of(args[1]),positive(raw));System.out.println("REAL_W2B_E2E STRUCTURALLY_VALID I-09 I-59 I-56 PRESENT; codec canonical bytes PASS"); }
    }
}
