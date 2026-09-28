package io.github.gustavo2358.lower.adapters.testing;

import java.util.*;
import com.fasterxml.jackson.databind.*;
import io.github.gustavo2358.lower.adapters.cli.CobolLower;
import io.github.gustavo2358.lower.adapters.sp.SpJsonDecoder;
import io.github.gustavo2358.lower.application.*;

/** Source hypotheses never create executable flow. Independent per-source oracles. */
public final class SourcePossibilitySuite {
    private static void need(boolean ok,String why){if(!ok)throw new AssertionError(why);}
    public static void main(String[] args)throws Exception {
        var json=new ObjectMapper();
        var expected=new HashMap<String,Set<String>>(Map.of("sql-update",Set.of("AFTERIO"),"unknown-perform",Set.of("AFTERP"),"unknown-before-terminal",Set.of(),"unknown-unused",Set.of(),"unknown-native",Set.of(),"unknown-native-dead",Set.of(),"unknown-copy-kill",Set.of(),"next-sentence",Set.of()));
        expected.put("mutable-transfer",Set.of("POSSIBLE"));expected.put("mutable-perform",Set.of("POSSIBLE"));
        expected.put("mutable-unreachable",Set.of());expected.put("mutable-no-transfer",Set.of());
        expected.put("unknown-branch-query",Set.of());expected.put("unknown-branch-kill",Set.of());
        for(var name:expected.keySet()) {
            byte[] bytes;try(var in=SourcePossibilitySuite.class.getResourceAsStream("/source-possibility/"+name+".sp.json")){bytes=Objects.requireNonNull(in).readAllBytes();}
            var decoded=new SpJsonDecoder(CobolLower.INPUT_LIMITS).decode(bytes);
            need(decoded instanceof SpJsonDecoder.Decoded,"producer input: "+decoded);
            var input=((SpJsonDecoder.Decoded)decoded).input();var lower=new CobolLowerer().lower(input,CobolLower.POSITIVE_OPTIONS);
            need(lower.publication().isPresent(),"AIR available");
            var before=new io.github.gustavo2358.air.json.AirJson().encode(lower.publication().orElseThrow());
            var q=QualifiedSourceProjection.project(input,lower.admission());
            need(Arrays.equals(before,new io.github.gustavo2358.air.json.AirJson().encode(lower.publication().orElseThrow())),"source projection cannot mutate AIR");
            var values=new TreeSet<String>();for(var o:q.occurrences())if(!o.qualifications().isEmpty())for(var v:o.values())values.add(v.value());
            need(values.equals(expected.get(name)),name+" literals: "+values);
            if(name.equals("unknown-native")) {
                need(q.nativeFiles().size()==4,"all native uses");
                need(q.nativeFiles().stream().map(f->f.controlLocation()).distinct().count()==4,"one control point per FILE use");
                for(var file:q.nativeFiles())need(!file.qualifications().isEmpty()&&file.names().size()==1,"native source qualification and declaration");
            }
            if(name.equals("unknown-native-dead"))need(q.nativeFiles().size()==4&&q.nativeFiles().stream().allMatch(f->f.qualifications().isEmpty()),"proven terminal excludes FILE uses");
            var wire=(com.fasterxml.jackson.databind.node.ObjectNode)json.readTree(bytes);
            if(wire.path("contractVersion").asText().equals("2.52.0")) {
                wire.put("contractVersion","2.51.0");need(!(new SpJsonDecoder(CobolLower.INPUT_LIMITS).decode(json.writeValueAsBytes(wire)) instanceof SpJsonDecoder.Decoded),"old version rejects source hypotheses");
            }
        }
        System.out.println("SOURCE_POSSIBILITY: fourteen producer witnesses and FILE point isolation PASS");
    }
}
