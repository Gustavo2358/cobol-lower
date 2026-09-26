package io.github.gustavo2358.lower.adapters.testing;

import java.util.*;
import java.nio.file.*;
import java.io.*;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.node.*;
import io.github.gustavo2358.air.model.*;
import io.github.gustavo2358.air.json.AirJson;
import io.github.gustavo2358.lower.adapters.cli.CobolLower;
import io.github.gustavo2358.lower.adapters.sp.*;
import io.github.gustavo2358.lower.application.*;
import io.github.gustavo2358.lower.domain.SpInput;

/** Independent inventories survive bounded non-executable capabilities; no path crosses a projection frontier. */
public final class PositivePublicationSuite {
    private static final ObjectMapper J=new ObjectMapper(); private static int checks,metamorphics;
    private static void need(boolean v,String message){checks++;if(!v)throw new AssertionError(message);}
    private static byte[] bytes(String n)throws Exception {try(var in=PositivePublicationSuite.class.getResourceAsStream("/sp/positive-publication-r7a/"+n+".json")){return in.readAllBytes();}}
    private static SpInput input(byte[] b){var r=new SpJsonDecoder(CobolLower.INPUT_LIMITS).decode(b);need(r instanceof SpJsonDecoder.Decoded,"valid SP "+r);return ((SpJsonDecoder.Decoded)r).input();}
    private static LoweringResult lower(byte[] b){return new CobolLowerer().lower(input(b),CobolLower.POSITIVE_OPTIONS);}
    private static List<Operations.Invoke> calls(Publication p){return p.units().stream().flatMap(u->u.sequences().stream()).map(Sequence::terminator).filter(Operations.Invoke.class::isInstance).map(Operations.Invoke.class::cast).toList();}
    private static List<String> callInventory(Publication p){return calls(p).stream().map(i->i.target() instanceof Interactions.LiteralTarget l?"literal:"+l.name():"computed").sorted().toList();}
    private static List<String> sourceInventory(Publication p){return p.resources().stream().flatMap(r->r.declaration().stream()).map(d->d.classification()+"/"+d.name()+"/"+d.nameSource()).sorted().toList();}
    private static Set<io.github.gustavo2358.air.model.Ids.LabelId> reached(Publication p){
        var seqs=new HashMap<io.github.gustavo2358.air.model.Ids.LabelId,Sequence>();p.units().forEach(u->u.sequences().forEach(s->seqs.put(s.label(),s)));
        var seen=new HashSet<io.github.gustavo2358.air.model.Ids.LabelId>();var work=new ArrayDeque<io.github.gustavo2358.air.model.Ids.LabelId>();p.units().forEach(u->u.entries().forEach(e->e.initialLabel().ifPresent(work::add)));
        while(!work.isEmpty()){var at=work.removeFirst();if(!seen.add(at))continue;var t=seqs.get(at).terminator();
            if(t instanceof Operations.Jump x)work.add(x.destination());
            else if(t instanceof Operations.Invoke x)x.outcomes().known().forEach(o->{if(o instanceof Control.Normal n)work.add(n.label());});
            else if(t instanceof Operations.Opaque x)x.envelope().control().known().forEach(o->{if(o instanceof Control.JumpAlternative j)work.add(j.label());});
            else if(!(t instanceof Operations.Return))throw new AssertionError("fixture has unexpected control "+t);
        }return seen;
    }
    private static void bounded(LoweringResult r,int n){
        need(r.status()==LoweringResult.Status.BOUNDED_PUBLICATION,"explicit bounded status "+r.status()+" "+r.admission().diagnostics());
        need(r.admission().status()==Admission.Status.ADMITTED,"projection admitted");need(r.admission().nonExecutableCapabilities().size()==n,"all capabilities inventoried");
        need(r.validation().orElseThrow().isStructurallyValid(),"strict AIR validity");var p=r.publication().orElseThrow();
        need(!p.units().getFirst().sequences().isEmpty(),"real AIR, not empty success");
        need(p.coverage().items().stream().filter(i->i.sourceKey().startsWith("sp-non-executable@1/")).count()==n,"capability coverage includes dead regions");
        var facts=r.admission().input().orElseThrow().statements();
        for(var cap:r.admission().nonExecutableCapabilities()){
            need(facts.stream().anyMatch(f->f.header().id().equals(cap.statement())),"typed source fact retained");
            need(cap.executableLowering()==SpInput.ExecutableLowering.NOT_READY,"not made executable");
            for(var link:r.statements().stream().filter(l->l.source().equals(cap.statement())).toList()){
                var term=p.units().stream().flatMap(u->u.sequences().stream()).map(Sequence::terminator).filter(t->t.header().id().equals(link.target())).findFirst().orElseThrow();
                need(term instanceof Operations.Opaque,"frontier cannot dispatch/invoke");var o=(Operations.Opaque)term;
                need(o.envelope().control().known().isEmpty(),"no invented successor, handler edge or fallthrough");
                need(o.envelope().control().remainder() instanceof Scopes.WithinControl w&&w.scope() instanceof Scopes.LabelsControl ls&&ls.labels().isEmpty(),"finite empty projection frontier, no AllControl");
                need(o.envelope().memory().knownWrites().isEmpty()&&o.envelope().memory().mustOverwrite().isEmpty(),"no invented effects");
                need(o.header().precision().control().status()==Evidence.PrecisionStatus.UNAVAILABLE,"frontier explicitly unavailable");
            }
        }
        need(p.uncertainties().stream().anyMatch(u->u.code().equals("EXECUTABLE_CAPABILITY_NOT_READY")),"explicit capability metadata in AIR");
        new AirJson().decode(new AirJson().encode(p));need(true,"strict codec roundtrip");
    }
    public static void main(String[] args)throws Exception{
        checks=0;metamorphics=0;var base=lower(bytes("base"));need(base.status()==LoweringResult.Status.SUCCESS,"baseline publishable");var bp=base.publication().orElseThrow();
        need(callInventory(bp).equals(List.of("computed","literal:BEFORE")),"known CALL oracle");need(sourceInventory(bp).stream().anyMatch(s->s.contains("COPY_SYNTAX")),"known COPYBOOK oracle");
        for(var name:List.of("one","dead","resp","renamed","family","two","copyshift")){
            var raw=bytes(name);var in=input(raw);var r=lower(raw);bounded(r,name.equals("two")?2:1);var p=r.publication().orElseThrow();
            need(callInventory(p).equals(callInventory(bp)),"independent CALL inventory monotonic "+name);
            need(sourceInventory(p).equals(sourceInventory(bp)),"independent source inventory monotonic "+name);
            need(r.admission().input().orElseThrow().equals(in),"no CICS deletion, no input rewriting");
            need(r.admission().input().orElseThrow().controlTopology().equals(in.controlTopology()),"source topology unchanged");
            need(Arrays.equals(new AirJson().encode(p),new AirJson().encode(lower(raw).publication().orElseThrow())),"deterministic publication "+name);
            var historical=new CobolLowerer().lower(in,CobolLower.OPTIONS);need(historical.status()==LoweringResult.Status.IMPLEMENTATION_LIMIT&&historical.publication().isEmpty(),"explicit old request retained");
            if(!name.equals("dead")){
                var reach=reached(p);var after=p.units().getFirst().sequences().stream().filter(s->s.terminator() instanceof Operations.Invoke i&&i.target() instanceof Interactions.ComputedTarget).findFirst().orElseThrow();
                need(!reach.contains(after.label()),"post-frontier runtime fact cannot execute "+name);
            }
            metamorphics++;
        }
        var wire=(ObjectNode)J.readTree(bytes("one"));var inv=(ArrayNode)wire.path("statements");var reversed=new ArrayList<JsonNode>();inv.forEach(reversed::add);Collections.reverse(reversed);inv.removeAll();reversed.forEach(inv::add);
        var a=lower(bytes("one"));var b=lower(J.writeValueAsBytes(wire));need(Arrays.equals(new AirJson().encode(a.publication().orElseThrow()),new AirJson().encode(b.publication().orElseThrow())),"SP inventory order deterministic");metamorphics++;
        // A real historical fact contradiction outranks new capability availability.
        var bad=(ObjectNode)J.readTree(bytes("one"));for(var s:bad.path("statements"))if(s.path("variant").asText().equals("CALL")){((ObjectNode)s).put("targetSyntax","INCONSISTENT");break;}
        var decoded=new SpJsonDecoder(CobolLower.INPUT_LIMITS).decode(J.writeValueAsBytes(bad));
        need(decoded instanceof SpJsonDecoder.Rejected||new CobolLowerer().lower(((SpJsonDecoder.Decoded)decoded).input(),CobolLower.POSITIVE_OPTIONS).status()==LoweringResult.Status.INVALID_INPUT,"invalid input outranks readiness");
        try {new LoweringResult(LoweringResult.Status.SUCCESS,a.admission(),a.publication(),a.validation(),a.entries(),a.statements(),a.limitations());throw new AssertionError("silent success");}catch(IllegalArgumentException expected){checks++;}
        // Compilation uses the same positive projection policy; independent units do not share capability identity.
        var units=new ArrayList<io.github.gustavo2358.lower.domain.SpCompilation.UnitProduct>();
        for(var n:List.of("one","family")){var sp=input(bytes(n));units.add(new io.github.gustavo2358.lower.domain.SpCompilation.UnitProduct(sp,Optional.empty(),sp.dataDeclarations().stream().map(SpInput.DataFact::id).toList(),List.of(),List.of(),List.of()));}
        var compilation=new io.github.gustavo2358.lower.domain.SpCompilation(SpInput.InventoryStatus.COMPLETE,units.stream().map(u->u.product().unit()).toList(),units);
        var composed=new CompilationLowerer().lower(compilation,CobolLower.POSITIVE_OPTIONS);
        need(composed.status()==LoweringResult.Status.BOUNDED_PUBLICATION,"compilation preserves positive availability");
        need(composed.admission().nonExecutableCapabilities().size()==2,"compilation capabilities retained separately");
        need(composed.admission().nonExecutableCapabilities().stream().map(c->c.statement().unit()).distinct().count()==2,"capability ownership never merges units");
        need(composed.publication().orElseThrow().units().size()==2&&calls(composed.publication().orElseThrow()).size()==4,"both independent inventories retained");
        new AirJson().decode(new AirJson().encode(composed.publication().orElseThrow()));need(true,"composed strict AIR");
        need(new CompilationLowerer().lower(compilation,CobolLower.OPTIONS).status()==LoweringResult.Status.IMPLEMENTATION_LIMIT,"historical compilation request unchanged");
        var dir=Files.createTempDirectory("positive-publication-cli");try{
            var sp=dir.resolve("sp.json");var air=dir.resolve("air.json");Files.write(sp,bytes("one"));var err=new ByteArrayOutputStream();
            int exit=CobolLower.run(new String[]{sp.toString(),air.toString()},new PrintStream(err));need(exit==0&&Files.size(air)>0,"configured CLI really publishes AIR");
            need(err.toString().contains("BOUNDED_PUBLICATION")&&err.toString().contains("NOT_READY"),"CLI honest capability status");new AirJson().decode(Files.readAllBytes(air));
        }finally{try(var stream=Files.walk(dir)){for(var p:stream.sorted(Comparator.reverseOrder()).toList())Files.delete(p);}}
        System.out.println("R7_R7A_POSITIVE_PUBLICATION_CHECKS="+checks+" METAMORPHICS="+metamorphics);
    }
}
