package io.github.gustavo2358.lower.adapters.testing;
import com.fasterxml.jackson.databind.*;
import io.github.gustavo2358.air.json.AirJson;
import io.github.gustavo2358.air.model.*;
import io.github.gustavo2358.lower.adapters.sp.SpJsonDecoder;
import io.github.gustavo2358.lower.adapters.cli.CobolLower;
import io.github.gustavo2358.lower.application.CobolLowerer;
import java.util.*;
/** Independent IBM alias oracle; real producer inputs preserve source spelling and typed authority. */
public final class CicsFileAliasSuite {
    static final ObjectMapper JSON=new ObjectMapper();
    static void need(boolean b,String m){if(!b)throw new AssertionError(m);}
    static Publication run(String name)throws Exception {
        try(var in=CicsFileAliasSuite.class.getResourceAsStream("/sp/cics-aliases/"+name+".json")) {
            var decoded=new SpJsonDecoder(CobolLower.INPUT_LIMITS).decode(in.readAllBytes());
            need(decoded instanceof SpJsonDecoder.Decoded,"decode "+name);
            var result=new CobolLowerer().lower(((SpJsonDecoder.Decoded)decoded).input(),CobolLower.POSITIVE_OPTIONS);
            need(result.publication().isPresent(),"admit "+name+" "+result.admission().diagnostics());
            var p=result.publication().orElseThrow();
            var tree=JSON.readTree(new AirJson().encode(p)).path("publication").path("units").get(0);
            var terms=new HashMap<JsonNode,JsonNode>();for(var s:tree.path("sequences"))terms.put(s.path("label"),s.path("terminator"));
            var todo=new ArrayDeque<JsonNode>();todo.add(tree.path("entries").get(0).path("initialLabel"));var seen=new HashSet<JsonNode>();var calls=new HashSet<String>();
            while(!todo.isEmpty()) {var at=todo.removeFirst();if(!seen.add(at))continue;var t=terms.get(at);if(t.path("kind").asText().equals("invoke"))calls.add(t.path("target").path("name").asText());todo.addAll(FileCompositeFlowSuite.labels(t));}
            if(name.startsWith("handler")) {
                need(calls.isEmpty(),"existing unqualified FILE condition remains an executable frontier "+name);
                var source=((SpJsonDecoder.Decoded)decoded).input();
                need(source.statements().stream().anyMatch(f->f instanceof io.github.gustavo2358.lower.domain.SpInput.CicsFileFact file&&file.target().orElse(null) instanceof io.github.gustavo2358.lower.domain.SpInput.LiteralCallTarget target&&target.text().equals("DDNAME")),"bounded FILE retains typed target "+name);
                need(terms.values().stream().anyMatch(t->t.path("kind").asText().equals("opaque")&&t.path("observedKind").asText().equals("CicsFileFact")&&t.path("envelope").path("control").path("known").isEmpty()),"FILE condition gap is not fabricated normal/handler control "+name);
            } else need(calls.containsAll(Set.of("DDNAME","AFTERIO")),"FILE and normal PERFORM return reachable "+name+" "+calls);
            return p;
        }
    }
    public static void main(String[] args)throws Exception {
        for(var verb:List.of("read","write","rewrite","delete","startbr","readnext","readprev","resetbr","endbr","unlock")) {
            var a=CicsFileControlSuite.files(run("alias-"+verb+"-file")).getFirst();
            var b=CicsFileControlSuite.files(run("alias-"+verb+"-dataset")).getFirst();
            need(a.action().equals(verb)&&b.action().equals(verb),"same command "+verb);
            need(a.target() instanceof Interactions.LiteralTarget x&&b.target() instanceof Interactions.LiteralTarget y&&x.name().equals(y.name())&&x.namespace().equals(y.namespace()),"same literal resource "+verb);
            need(a.outcomes().known().size()==b.outcomes().known().size()&&a.outcomes().remainder().getClass()==b.outcomes().remainder().getClass(),"same control alternatives "+verb);
            need(a.effectBound().otherwise().mustOverwrite().isEmpty()&&b.effectBound().otherwise().mustOverwrite().isEmpty(),"no unconditional overwrite "+verb);
        }
        run("handler-file");run("handler-dataset");
        System.out.println("PASS CicsFileAliasSuite 22 producer fixtures");
    }
}
