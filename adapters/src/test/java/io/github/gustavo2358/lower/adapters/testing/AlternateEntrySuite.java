package io.github.gustavo2358.lower.adapters.testing;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.node.*;
import io.github.gustavo2358.lower.adapters.sp.SpJsonDecoder;
import io.github.gustavo2358.lower.adapters.cli.CobolLower;
import io.github.gustavo2358.lower.application.*;
import io.github.gustavo2358.lower.domain.SpInput;
import io.github.gustavo2358.air.model.*;
import io.github.gustavo2358.air.model.Ids.*;
import java.util.*;
/** Independent entry-by-entry graph walks. Never use source order, statement IDs or node titles. */
public final class AlternateEntrySuite {
    static final ObjectMapper J=new ObjectMapper();
    static ObjectNode wire(String name)throws Exception {try(var in=AlternateEntrySuite.class.getResourceAsStream("/sp/alternate-entries/"+name+".json")){return (ObjectNode)J.readTree(Objects.requireNonNull(in));}}
    static SpInput input(ObjectNode wire)throws Exception {var d=new SpJsonDecoder(CobolLower.INPUT_LIMITS).decode(J.writeValueAsBytes(wire));if(!(d instanceof SpJsonDecoder.Decoded x))throw new AssertionError(d);return x.input();}
    static Set<String> names(Publication p,LabelId start) {
        var sequences=new HashMap<LabelId,Sequence>();p.units().forEach(u->u.sequences().forEach(s->sequences.put(s.label(),s)));
        var seen=new HashSet<LabelId>();var work=new ArrayDeque<LabelId>();work.add(start);var names=new TreeSet<String>();
        while(!work.isEmpty()) {var id=work.removeFirst();if(!seen.add(id))continue;var t=sequences.get(id).terminator();
            if(t instanceof Operations.Jump x)work.add(x.destination());
            else if(t instanceof Operations.Branch x){work.add(x.trueDestination());work.add(x.falseDestination());}
            else if(t instanceof Operations.Invoke x){if(x.target() instanceof Interactions.LiteralTarget target)names.add(target.name());TerminalSendSuite.knownDestinations(x.outcomes().known(),work);}
            else if(t instanceof Operations.Opaque x)TerminalSendSuite.knownDestinations(x.envelope().control().known(),work);
        }return names;
    }
    public static void main(String[] args)throws Exception {
        var expected=Map.of("alternate-entry",Map.of("PRIMARY",Set.of("PRIMARY"),"ALTPOINT",Set.of("ALT-CALL")),
            "entry-sequential",Map.of("PRIMARY",Set.of("BEFORE","AFTER"),"ALTPOINT",Set.of("AFTER")),
            "entry-consecutive",Map.of("PRIMARY",Set.of("PRIMARY"),"ALTONE",Set.of("ALTBODY"),"ALTTWO",Set.of("ALTBODY")),
            "entry-perform",Map.of("PRIMARY",Set.of("MAINRET","SHARED"),"ALTPOINT",Set.of("ALTRET","SHARED")),
            "entry-using",Map.of("PRIMARY",Set.<String>of(),"ALTPOINT",Set.<String>of()),
            "entry-values",Map.of("PRIMARY",Set.<String>of(),"ALTPOINT",Set.<String>of()),
            "entry-invalid",Map.of("PRIMARY",Set.<String>of()),"entry-trailing",Map.of("PRIMARY",Set.<String>of()));
        for(var test:expected.entrySet()) {
            var in=input(wire(test.getKey()));var result=new CobolLowerer().lower(in,CobolLower.POSITIVE_OPTIONS);
            if(result.publication().isEmpty())throw new AssertionError(test.getKey()+" "+result.status()+" "+result.validation());
            var air=result.publication().orElseThrow();
            if(result.entries().size()!=test.getValue().size())throw new AssertionError("entry count "+test.getKey());
            for(var link:result.entries()) {
                var source=in.entryInventory().entries().stream().filter(e->e.id().equals(link.source())).findFirst().orElseThrow();
                var name=source.externalName().orElse("PRIMARY");var entry=air.units().getFirst().entries().stream().filter(e->e.id().equals(link.target())).findFirst().orElseThrow();
                var actual=names(air,entry.initialLabel().orElseThrow());if(!actual.equals(test.getValue().get(name)))throw new AssertionError(test.getKey()+"/"+name+" "+actual);
                if(test.getKey().equals("entry-using")&&source.role()==SpInput.EntryRole.ALTERNATE) {
                    if(!(entry.signature().parameters().remainder() instanceof Interactions.UnknownRemainder)||!(entry.signature().results().remainder() instanceof Interactions.NoRemainder))throw new AssertionError("USING parameter uncertainty distinct from absent RETURNING");
                }
            }
            var source=QualifiedSourceProjection.project(in,result.admission());
            var evidence=new io.github.gustavo2358.lower.source.QualifiedSourceDependencies("qualified-source-dependencies","1.5.0","test",new io.github.gustavo2358.lower.source.QualifiedSourceDependencies.Document("cobol-semantic-product","2.61.0","0".repeat(64)),List.of(),List.of(source));
            var codec=new io.github.gustavo2358.lower.adapters.source.QualifiedSourceJson();if(!codec.decode(codec.encode(evidence)).equals(evidence))throw new AssertionError("source roundtrip");
        }
        for(var mode:List.of("old","foreign-entry","missing-point","wrong-declaration","wrong-start","proof")) {
            var w=wire("alternate-entry");var points=(ArrayNode)w.path("controlTopology").path("entryPoints");var point=(ObjectNode)points.get(0);
            switch(mode){case "old"->w.put("contractVersion","2.60.0");case "foreign-entry"->point.put("entry","missing");case "missing-point"->points.removeAll();case "wrong-declaration"->point.put("declaration",w.path("entryInventory").path("entries").get(0).path("start").path("statement").asText());case "wrong-start"->((ObjectNode)point.path("target")).put("reference",w.path("entryInventory").path("entries").get(0).path("start").path("statement").asText());case "proof"->point.putArray("proofs");}
            var decoded=new SpJsonDecoder(CobolLower.INPUT_LIMITS).decode(J.writeValueAsBytes(w));
            if(decoded instanceof SpJsonDecoder.Decoded d&&new CobolLowerer().lower(d.input(),CobolLower.POSITIVE_OPTIONS).publication().isPresent())throw new AssertionError("accepted forged "+mode);
        }
        System.out.println("PASS AlternateEntrySuite: 8 entry isolation oracles and 6 contract forgeries");
    }
}
