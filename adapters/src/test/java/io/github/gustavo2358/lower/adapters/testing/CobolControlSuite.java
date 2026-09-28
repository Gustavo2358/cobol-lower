package io.github.gustavo2358.lower.adapters.testing;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.node.*;
import io.github.gustavo2358.lower.adapters.sp.SpJsonDecoder;
import io.github.gustavo2358.lower.adapters.cli.CobolLower;
import io.github.gustavo2358.lower.application.CobolLowerer;
import io.github.gustavo2358.air.model.*;
import io.github.gustavo2358.air.model.Ids.*;
import io.github.gustavo2358.air.json.AirJson;
import java.util.*;
public final class CobolControlSuite {
    static final ObjectMapper J=new ObjectMapper();static int cases,mutations;
    static void need(boolean b,String m){if(!b)throw new AssertionError(m);}
    static SpJsonDecoder.Result decode(ObjectNode j)throws Exception{return new SpJsonDecoder(CobolLower.INPUT_LIMITS).decode(J.writeValueAsBytes(j));}
    static void reject(ObjectNode j)throws Exception {
        var d=decode(j);need(d instanceof SpJsonDecoder.Rejected||new CobolLowerer().lower(((SpJsonDecoder.Decoded)d).input(),CobolLower.POSITIVE_OPTIONS).publication().isEmpty(),"invalid control/effect rejected");mutations++;
    }
    static Set<String> firstCalls(LabelId start,Map<LabelId,Sequence> seqs) {
        var names=new TreeSet<String>();var pending=new ArrayDeque<LabelId>();pending.add(start);var seen=new HashSet<LabelId>();
        while(!pending.isEmpty()) {var at=pending.removeFirst();if(!seen.add(at))continue;var t=seqs.get(at).terminator();
            if(t instanceof Operations.Invoke call){if(call.target() instanceof Interactions.LiteralTarget lit)names.add(lit.name());continue;}
            if(t instanceof Operations.Jump jump)pending.add(jump.destination());
            if(t instanceof Operations.Branch branch){pending.add(branch.trueDestination());pending.add(branch.falseDestination());}
            if(t instanceof Operations.Opaque opaque)for(var e:opaque.envelope().control().known())if(e instanceof Control.JumpAlternative jump)pending.add(jump.label());
        }return names;
    }
    public static void main(String[] args)throws Exception {
        var names=new ArrayList<>(List.of("next-direct","next-inline","next-paragraph","next-callers","next-if","next-evaluate","search-arms","search-no-atend","search-terminal","search-next","stop-perform","exit-runtime","entry-dead","entry-sequential"));
        for(var n:List.of("next-inline","next-paragraph","next-callers","stop-perform"))names.add(n+"-handler");
        for(var name:names) {
            ObjectNode j;try(var stream=CobolControlSuite.class.getResourceAsStream("/sp/cobol-control/control-"+name+".json")){j=(ObjectNode)J.readTree(Objects.requireNonNull(stream,name));}
            var d=decode(j);need(d instanceof SpJsonDecoder.Decoded,"SP2.56 "+name);var input=((SpJsonDecoder.Decoded)d).input();
            var result=new CobolLowerer().lower(input,CobolLower.POSITIVE_OPTIONS);var p=result.publication().orElseThrow();need(result.validation().orElseThrow().isStructurallyValid(),"AIR valid");new AirJson().decode(new AirJson().encode(p));
            var reached=TerminalSendSuite.reached(p);var seqs=new HashMap<LabelId,Sequence>();p.units().forEach(u->u.sequences().forEach(s->seqs.put(s.label(),s)));
            var calls=new TreeSet<String>();for(var s:seqs.values())if(reached.contains(s.label())&&s.terminator() instanceof Operations.Invoke v&&v.target() instanceof Interactions.LiteralTarget t)calls.add(t.name());
            need(calls.stream().noneMatch(n->n.startsWith("DEAD")),"dead paths absent "+name+calls);
            if(name.startsWith("next-callers")) {
                need(calls.equals(Set.of("FIRST","SECOND")),"both invocations return");
                var source=input.controlTopology().orElseThrow().outcomes().stream().filter(o->o.role().equals("next-sentence")).findFirst().orElseThrow().statement();
                var firsts=new ArrayList<Set<String>>();
                for(var link:result.statements())if(link.source().handle().equals(source)&&seqs.get(link.label()).terminator().header().id().equals(link.target())&&reached.contains(link.label()))firsts.add(firstCalls(link.label(),seqs));
                need(firsts.size()==2&&firsts.contains(Set.of("FIRST"))&&firsts.contains(Set.of("SECOND")),"no cross return between PERFORM activations "+name+firsts);
            }
            if(name.startsWith("stop-perform"))need(seqs.values().stream().anyMatch(s->reached.contains(s.label())&&s.terminator() instanceof Operations.Opaque o&&o.envelope().control().known().equals(List.of(Control.HaltAlternative.INSTANCE))&&o.envelope().control().remainder() instanceof Scopes.NoControl),"closed terminal halt alternative");
            if(name.equals("exit-runtime"))need(calls.contains("MAINONLY"),"unknown main/called role preserves continuation");
            var copy=j.deepCopy();copy.put("contractVersion","2.55.0");
            // Contextual return/ENTRY use the historical shape; the new region/halt/effect capabilities are version gated.
            if(name.startsWith("next-")||name.startsWith("search-")||name.startsWith("stop-"))reject(copy);
            for(var outcome:j.path("controlTopology").path("outcomes"))if(outcome.path("target").path("kind").asText().equals("ESCAPE")) {
                copy=j.deepCopy();for(var o:copy.path("controlTopology").path("outcomes"))if(o.path("id").equals(outcome.path("id")))((ObjectNode)o.path("target")).put("reference",input.controlTopology().orElseThrow().regions().stream().filter(r->r.kind().name().equals("PROCEDURE")).findFirst().orElseThrow().id());reject(copy);break;
            }
            cases++;
        }
        System.out.println("PASS CobolControlSuite cases="+cases+" mutations="+mutations);
    }
}
