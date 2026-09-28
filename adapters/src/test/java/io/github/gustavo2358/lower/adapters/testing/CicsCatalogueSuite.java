package io.github.gustavo2358.lower.adapters.testing;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.node.*;
import io.github.gustavo2358.lower.adapters.sp.SpJsonDecoder;
import io.github.gustavo2358.lower.adapters.cli.CobolLower;
import io.github.gustavo2358.lower.application.CobolLowerer;
import io.github.gustavo2358.lower.domain.SpInput.*;
import io.github.gustavo2358.air.model.*;
import io.github.gustavo2358.air.json.AirJson;
import java.util.*;
/** Real SP2.54 fixtures. No source re-parsing; asserts control, open effects and malformed boundaries. */
public final class CicsCatalogueSuite {
    static final ObjectMapper J=new ObjectMapper();static int cases,mutations;
    static void need(boolean b,String m){if(!b)throw new AssertionError(m);}
    static SpJsonDecoder.Result decode(ObjectNode j)throws Exception{return new SpJsonDecoder(CobolLower.INPUT_LIMITS).decode(J.writeValueAsBytes(j));}
    static ObjectNode command(ObjectNode j){for(var s:j.path("statements"))if(s.path("variant").asText().equals("CICS_COMMAND"))return (ObjectNode)s;throw new AssertionError();}
    static void reject(ObjectNode j)throws Exception{need(decode(j) instanceof SpJsonDecoder.Rejected,"malformed SP rejected");mutations++;}
    public static void main(String[] args)throws Exception {
        for(var family:List.of("asktime","formattime","assign","inquire","send-text","writeq"))for(var mode:List.of("default","local","response","duplicate")) {
            String name="catalogue-"+family+"-"+mode;ObjectNode j;
            try(var in=CicsCatalogueSuite.class.getResourceAsStream("/sp/cics-catalogue/"+name+".json")){j=(ObjectNode)J.readTree(Objects.requireNonNull(in,name));}
            var decoded=decode(j);need(decoded instanceof SpJsonDecoder.Decoded,"SP2.54 admitted "+name);
            var input=((SpJsonDecoder.Decoded)decoded).input();var result=new CobolLowerer().lower(input,CobolLower.POSITIVE_OPTIONS);var pub=result.publication().orElseThrow();
            need(result.validation().orElseThrow().isStructurallyValid(),"strict AIR");new AirJson().decode(new AirJson().encode(pub));
            var reached=TerminalSendSuite.reached(pub);var seqs=pub.units().stream().flatMap(u->u.sequences().stream()).toList();
            var c=seqs.stream().map(Sequence::terminator).filter(t->t instanceof Operations.Opaque o&&o.observedKind().startsWith("cics-host-command/")).map(Operations.Opaque.class::cast).findFirst().orElseThrow();
            need(!c.envelope().control().known().isEmpty(),"known completion "+name);
            need(c.envelope().memory().mustOverwrite().isEmpty()&&c.envelope().memory().otherWrites() instanceof Scopes.WithinMemory,"implicit effects MAY only "+name);
            need(seqs.stream().anyMatch(s->reached.contains(s.label())&&s.terminator() instanceof Operations.Invoke v&&v.target() instanceof Interactions.LiteralTarget t&&t.name().equals("AFTERP")),"PERFORM returns to its caller "+name);
            need(seqs.stream().noneMatch(s->reached.contains(s.label())&&s.terminator() instanceof Operations.Invoke v&&v.target() instanceof Interactions.LiteralTarget t&&t.name().equals("ERRORPGM")),"do not invent handler dispatch "+name);
            need(input.statements().stream().noneMatch(s->s instanceof CicsFact||s instanceof CicsFileFact),"queue and INQUIRE are not CALL/FILE");
            var copy=j.deepCopy();copy.put("contractVersion","2.53.0");reject(copy);
            copy=j.deepCopy();command(copy).put("commandKind","INVENTED");reject(copy);
            copy=j.deepCopy();command(copy).set("hostEffects",J.createObjectNode().set("literalOptions",J.createArrayNode()));reject(copy);
            copy=j.deepCopy();command(copy).put("syntaxStatus","UNAVAILABLE");reject(copy);
            for(var option:command(j).path("options"))if(option.hasNonNull("reference")) {
                copy=j.deepCopy();for(var o:command(copy).path("options"))if(o.path("start").equals(option.path("start")))((ObjectNode)o.path("reference")).put("role",option.path("reference").path("role").asText().equals("WRITE")?"READ":"WRITE");reject(copy);break;
            }
            copy=j.deepCopy();command(copy).put("rawText"," ".repeat(command(copy).path("rawText").asText().length()));need(decode(copy) instanceof SpJsonDecoder.Decoded,"rawText is not authority");cases++;
        }
        System.out.println("PASS CicsCatalogueSuite cases="+cases+" mutations="+mutations);
    }
}
