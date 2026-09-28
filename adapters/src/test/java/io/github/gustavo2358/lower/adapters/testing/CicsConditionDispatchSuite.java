package io.github.gustavo2358.lower.adapters.testing;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.node.*;
import io.github.gustavo2358.lower.adapters.sp.SpJsonDecoder;
import io.github.gustavo2358.lower.adapters.cli.CobolLower;
import io.github.gustavo2358.lower.application.*;
import io.github.gustavo2358.lower.source.QualifiedSourceDependencies;
import java.util.*;
/** Source-authored exact oracles; traversal follows AIR destinations, never statement order. */
public final class CicsConditionDispatchSuite {
    static final ObjectMapper J=new ObjectMapper();
    static ObjectNode wire(String name)throws Exception {try(var in=CicsConditionDispatchSuite.class.getResourceAsStream("/sp/condition-dispatch/"+name+".json")){return (ObjectNode)J.readTree(Objects.requireNonNull(in));}}
    public static void main(String[] args)throws Exception {
        int count=0;var oracles=wire("oracles");
        for(var names=oracles.fieldNames();names.hasNext();) {
            var name=names.next();var in=AlternateEntrySuite.input(wire(name));var result=new CobolLowerer().lower(in,CobolLower.POSITIVE_OPTIONS);
            if(result.publication().isEmpty())throw new AssertionError(name+" "+result.status()+" "+result.validation());
            var air=result.publication().orElseThrow();var all=new TreeSet<String>();var perEntry=new HashSet<Set<String>>();
            for(var e:air.units().getFirst().entries()){var values=AlternateEntrySuite.names(air,e.initialLabel().orElseThrow());all.addAll(values);perEntry.add(values);}
            var expected=new TreeSet<String>();oracles.path(name).forEach(n->expected.add(n.asText()));
            if(!expected.equals(all))throw new AssertionError(name+" "+all+" expected "+expected);
            if(name.equals("condition-entry-isolation")&&!perEntry.equals(Set.of(Set.of("TARGET","HANDLER"),Set.of("TARGET","AFTER"))))throw new AssertionError("condition registrations cross external activations");
            var source=QualifiedSourceProjection.project(in,result.admission());
            var q=new QualifiedSourceDependencies("qualified-source-dependencies","1.6.0","test",new QualifiedSourceDependencies.Document("cobol-semantic-product","2.62.0","0".repeat(64)),List.of(),List.of(source));
            var codec=new io.github.gustavo2358.lower.adapters.source.QualifiedSourceJson();if(!codec.decode(codec.encode(q)).equals(q))throw new AssertionError("source roundtrip "+name);
            count++;
        }
        for(var name:List.of("condition-stack-open","condition-stack-dead","condition-stack-bypass")) {
            var wire=wire(name);var input=AlternateEntrySuite.input(wire);var result=new CobolLowerer().lower(input,CobolLower.POSITIVE_OPTIONS);
            if(result.publication().isEmpty())throw new AssertionError(name+" "+result.status());
            var air=result.publication().orElseThrow();var reached=AlternateEntrySuite.names(air,air.units().getFirst().entries().getFirst().initialLabel().orElseThrow());
            var expected=name.equals("condition-stack-dead")?Set.of("TARGET","HANDLER"):Set.<String>of();
            if(!reached.equals(expected))throw new AssertionError("source assumptions leaked into executable control: "+name+" "+reached);
        }
        for(var mode:List.of("old","missing-registration-proof","foreign-registration","wrong-disposition","wrong-command","missing-event-proof","foreign-event-proof","wrong-default-origin")) {
            var w=wire("condition-link");var t=(ObjectNode)w.path("controlTopology");var r=(ObjectNode)t.path("conditionRegistrations").get(0);var e=(ObjectNode)t.path("conditionEvents").get(0);
            switch(mode) {
                case "old" -> w.put("contractVersion","2.61.0");
                case "missing-registration-proof" -> r.putArray("proofs");
                case "foreign-registration" -> r.put("statement",e.path("statement").asText());
                case "wrong-disposition" -> {e.put("eligibility","HANDLERS_BYPASSED");e.put("defaultEvent","");}
                case "wrong-command" -> {for(var f:w.path("statements"))if(f.path("variant").asText().equals("CICS_PROGRAM_CONTROL"))((ObjectNode)f).put("command","XCTL");}
                case "missing-event-proof" -> e.putArray("proofs");
                case "foreign-event-proof" -> {for(var p:t.path("proofs"))if(p.path("id").equals(e.path("proofs").get(0)))((ObjectNode)p).set("provenance",w.path("statements").get(0).path("header").path("provenance"));}
                case "wrong-default-origin" -> ((ObjectNode)t.path("exceptionalEvents").get(0)).put("origin","XCTL_PGMIDERR");
            }
            var decoded=new SpJsonDecoder(CobolLower.INPUT_LIMITS).decode(J.writeValueAsBytes(w));
            if(decoded instanceof SpJsonDecoder.Decoded d&&new CobolLowerer().lower(d.input(),CobolLower.POSITIVE_OPTIONS).publication().isPresent())throw new AssertionError("accepted forged "+mode);
        }
        System.out.println("PASS CicsConditionDispatchSuite: "+count+" exact AIR/source oracles and 8 forgeries");
    }
}
