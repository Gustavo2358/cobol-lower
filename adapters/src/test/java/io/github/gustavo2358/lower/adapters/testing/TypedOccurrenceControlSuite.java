package io.github.gustavo2358.lower.adapters.testing;

import io.github.gustavo2358.air.model.*;
import io.github.gustavo2358.air.json.AirJson;
import io.github.gustavo2358.lower.adapters.sp.SpJsonDecoder;
import io.github.gustavo2358.lower.adapters.cli.CobolLower;
import io.github.gustavo2358.lower.application.*;
import io.github.gustavo2358.lower.domain.SpInput;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.*;

/** An occurrence reached by the model survives an unavailable outgoing frontier. */
public final class TypedOccurrenceControlSuite {
    private TypedOccurrenceControlSuite() { }
    private static int checks;
    private static void need(boolean value,String message) {checks++;if(!value)throw new AssertionError(message);}
    private static byte[] fixture(String name)throws Exception {
        try(var in=TypedOccurrenceControlSuite.class.getResourceAsStream("/sp/typed-occurrence-r5/"+name+".json")) {
            if(in==null)throw new AssertionError("missing R5 fixture "+name);return in.readAllBytes();
        }
    }
    private static SpInput decode(byte[] bytes) {
        var decoded=new SpJsonDecoder(CobolLower.INPUT_LIMITS).decode(bytes);
        if(!(decoded instanceof SpJsonDecoder.Decoded d))throw new AssertionError("decode "+decoded);
        return d.input();
    }
    private static LoweringResult check(SpInput input,int expected) {
        var result=new CobolLowerer().lower(input,CobolLower.OPTIONS);
        need(result.publication().isPresent(),"R5 publication: "+result.status()+" "+result.validation());
        var p=result.publication().orElseThrow();
        var codec=new AirJson();need(p.equals(codec.decode(codec.encode(p))),"official strict AIR codec roundtrip");
        var invokes=p.units().stream().flatMap(u->u.sequences().stream()).map(Sequence::terminator)
            .filter(Operations.Invoke.class::isInstance).map(Operations.Invoke.class::cast)
            .filter(i->i.action().equals("execute")).toList();
        need(invokes.size()==expected,"one typed execute per source occurrence: expected "+expected+", actual "+invokes.size());
        var seen=new HashSet<SpInput.StatementId>();
        for(var invoke:invokes) {
            var links=result.statements().stream().filter(l->l.target().equals(invoke.header().id())).toList();
            need(links.size()==1&&seen.add(links.getFirst().source()),"source identity, not internal operation multiplicity");
            var fact=(SpInput.CicsFact)input.statements().stream().filter(s->s.header().id().equals(links.getFirst().source())).findFirst().orElseThrow();
            need(fact.command()==SpInput.CicsCommand.XCTL,"XCTL remains execute");
            need(invoke.outcomes().known().isEmpty(),"unknown outgoing control has no invented successor");
            need(invoke.outcomes().remainder() instanceof Scopes.WithinControl w&&w.scope() instanceof Scopes.LabelsControl l&&l.labels().isEmpty(),"same empty open topology frontier");
            need(invoke.header().precision().control().status()==Evidence.PrecisionStatus.UNAVAILABLE,"control remains unavailable");
            need(p.uncertainties().stream().anyMatch(u->u.code().equals("CONTROL_TOPOLOGY_REGION_UNAVAILABLE")&&invoke.header().uncertainties().contains(u.id())),"topology gap retained on typed operation");
            need(invoke.signature() instanceof Interactions.ExternalSignature sig&&sig.signature().parameters().remainder() instanceof Interactions.UnknownRemainder,"signature stays partial");
            need(invoke.effectBound().otherwise().reads() instanceof Scopes.NoMemory&&invoke.effectBound().otherwise().writes() instanceof Scopes.NoMemory,"PROGRAM does not introduce global memory effects");
            if(fact.target().orElse(null) instanceof SpInput.DataCallTarget data) {
                need(invoke.target() instanceof Interactions.ComputedTarget t&&t.name() instanceof Expressions.Read,"variable is a computed read, never a written-name candidate");
                var read=(Expressions.Read)((Interactions.ComputedTarget)invoke.target()).name();
                need(result.operands().stream().anyMatch(l->l.source().equals(data.reference().id())&&l.target().equals(read.header().id())),"target operand identity preserved");
                need(p.origins().stream().anyMatch(o->o.id().equals(read.header().origin())),"target provenance preserved");
            }
            for(var code:fact.gapCodes())if(input.gaps().stream().anyMatch(g->g.code().equals(code)))
                need(p.uncertainties().stream().anyMatch(u->u.code().equals("cobol-sp:"+code)),"SP gap retained: "+code);
        }
        return result;
    }
    public static void main(String[] args)throws Exception {
        var names=args.length==0?List.of("A-known","B-unavailable","D-multiplicity-1","D-multiplicity-2","D-multiplicity-3","D-distinct","E-dead","F-no-value","G-overwrite","J-unit-a","J-unit-b"):List.of(args);
        for(var name:names)check(decode(fixture(name)),name.equals("D-distinct")?2:1);
        if(args.length==0) {
            var raw=fixture("A-known");var json=new ObjectMapper();var tree=(ObjectNode)json.readTree(raw);
            for(var s:tree.withArray("statements"))if(s.path("variant").asText().equals("CICS_PROGRAM_CONTROL"))((ObjectNode)s).putArray("gapCodes");
            check(decode(json.writeValueAsBytes(tree)),1); // diagnostics do not select execution
            var a=check(decode(fixture("J-unit-a")),1).publication().orElseThrow();
            var b=check(decode(fixture("J-unit-b")),1).publication().orElseThrow();
            need(!a.id().equals(b.id()),"same spelling in separate units does not share publication identity");
        }
        System.out.println("TYPED_OCCURRENCE_CONTROL_CHECKS="+checks);
    }
}
