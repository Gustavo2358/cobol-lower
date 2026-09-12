package io.github.gustavo2358.lower.adapters.testing;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.node.*;
import io.github.gustavo2358.air.json.*;
import io.github.gustavo2358.air.model.Operations;
import io.github.gustavo2358.lower.testing.IfOracle;
import java.util.*;

/** Local malformed transport challenges for placements excluded by Java's sealed instruction algebra. */
public final class IfPlacementChallenge {
    private IfPlacementChallenge() { }
    public static void main(String[] args) throws Exception {
        var bytes=IfIntegrationSuite.positive(IfIntegrationSuite.fixture("closed"));var codec=new AirJson();var mapper=new ObjectMapper();
        var base=(ObjectNode)mapper.readTree(bytes);var p=codec.decode(bytes);var sequences=p.units().getFirst().sequences();
        int invoke=-1,arm=-1;
        for(int i=0;i<sequences.size();i++) { if(sequences.get(i).terminator() instanceof Operations.Invoke)invoke=i;if(!sequences.get(i).instructions().isEmpty())arm=i; }
        for(int c=0;c<2;c++) {
            var wire=base.deepCopy();var array=(ArrayNode)wire.path("publication").path("units").get(0).path("sequences");
            var invokeNode=(ObjectNode)array.get(invoke);var instructions=(ArrayNode)invokeNode.get("instructions");
            instructions.add(invokeNode.get("terminator").deepCopy());
            if(c==1)instructions.add(array.get(arm).get("instructions").get(0).deepCopy());
            invokeNode.set("terminator",array.get(arm).get("terminator").deepCopy());
            try { codec.decode(mapper.writeValueAsBytes(wire));throw new AssertionError("invalid placement survived"); }
            catch(AirJsonException ex) { IfOracle.check(ex.code()==AirJsonException.Code.INVALID_IR && ex.issues().stream().anyMatch(i -> i.rule().equals("I-04")),"I-04 rejects Invoke in instructions"+(c==1?" followed by Assign":"")); }
            IfOracle.check(Arrays.equals(bytes,codec.encode(codec.decode(bytes))),"original wire byte-exact second GREEN");
            System.out.println("IF_PLACEMENT_MUTANT="+(c==0?"invoke-in-instructions":"assign-after-terminator")+" DETECTED=I-04 RESTORED_BYTE_EXACT SECOND_GREEN");
        }
    }
}
