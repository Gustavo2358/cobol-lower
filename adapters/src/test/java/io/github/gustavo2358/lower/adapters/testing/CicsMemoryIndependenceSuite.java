package io.github.gustavo2358.lower.adapters.testing;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.node.*;
import static io.github.gustavo2358.lower.domain.SpInput.*;
import io.github.gustavo2358.lower.adapters.sp.SpJsonDecoder;
import io.github.gustavo2358.lower.adapters.cli.CobolLower;
import io.github.gustavo2358.lower.application.CobolLowerer;
import io.github.gustavo2358.air.model.*;
import java.util.*;
public final class CicsMemoryIndependenceSuite {
    static void need(boolean b,String m){if(!b)throw new AssertionError(m);}
    static final ObjectMapper J=new ObjectMapper();
    static ObjectNode wire(String name)throws Exception {try(var in=CicsMemoryIndependenceSuite.class.getResourceAsStream("/sp/cics-memory/"+name+".json")){return (ObjectNode)J.readTree(in);}}
    static ObjectNode command(ObjectNode j){for(var s:j.path("statements"))if(s.path("variant").asText().equals("CICS_COMMAND"))return (ObjectNode)s;throw new AssertionError();}
    static SpJsonDecoder.Result decode(ObjectNode j)throws Exception{return new SpJsonDecoder(CobolLower.INPUT_LIMITS).decode(J.writeValueAsBytes(j));}
    static void rejects(ObjectNode j)throws Exception {need(decode(j) instanceof SpJsonDecoder.Rejected,"malformed implicit binding rejected");}
    static void implicitContract()throws Exception {
        for(var n:List.of("bms-receive-implicit","bms-send-implicit")) {
            var j=wire(n);need(j.path("contractVersion").asText().equals("2.53.0"),"implicit capability version");
            var in=((SpJsonDecoder.Decoded)decode(j)).input();var c=in.statements().stream().filter(CicsCommandFact.class::isInstance).map(CicsCommandFact.class::cast).findFirst().orElseThrow();
            var area=c.implicitArea().orElseThrow();need(area.role()==(n.contains("receive")?OperandRole.WRITE:OperandRole.READ),"direction");
            need(!area.provenance().exact()&&area.binding().selected().equals(area.logicalWholeItem()),"derived canonical binding");
            var result=new CobolLowerer().lower(in,CobolLower.POSITIVE_OPTIONS);var command=result.publication().orElseThrow().units().stream().flatMap(u->u.sequences().stream()).map(Sequence::terminator).filter(t->t instanceof Operations.Opaque o&&o.observedKind().startsWith("cics-host-command/")).map(Operations.Opaque.class::cast).findFirst().orElseThrow();
            need(command.envelope().memory().otherWrites()==Scopes.NoMemory.INSTANCE,"proved implicit area is bounded");
            var copy=j.deepCopy();((ObjectNode)command(copy).path("implicitArea")).put("role",n.contains("receive")?"READ":"WRITE");rejects(copy);
            copy=j.deepCopy();((ObjectNode)command(copy).path("implicitArea")).put("id","renamed-opaque-handle");need(decode(copy) instanceof SpJsonDecoder.Decoded,"wire handles are opaque; enclosing statement owns operand");
            copy=j.deepCopy();((ObjectNode)command(copy).path("implicitArea").path("provenance")).put("exact",true);rejects(copy);
            copy=j.deepCopy();((ObjectNode)command(copy).path("implicitArea")).put("logicalWholeItem","data:9999");rejects(copy);
            copy=j.deepCopy();((ObjectNode)command(copy).path("options").get(0)).set("reference",command(copy).path("implicitArea").deepCopy());rejects(copy);
            copy=j.deepCopy();copy.put("contractVersion","2.51.0");rejects(copy);
            copy=j.deepCopy();command(copy).put("commandKind","RETRIEVE");rejects(copy);
            var foreign=new DataReference(new OperandId(new StatementId(c.header().id().unit(),"foreign-statement"),area.id().handle()),area.role(),area.binding(),area.wholeItemAccess(),area.provenance(),area.regionalAccess(),area.regionalAlternatives(),area.logicalWholeItem());
            try {new CicsCommandFact(c.header(),c.commandKind(),c.syntaxStatus(),c.rawText(),c.options(),c.gapCodes(),c.length(),c.hostEffects(),Optional.of(foreign));throw new AssertionError("typed foreign owner");}catch(IllegalArgumentException expected){}
            // Constructor validation also protects the in-memory port, independent of JSON.
            try {new CicsCommandFact(c.header(),CicsCommandKind.RETRIEVE,c.syntaxStatus(),c.rawText(),c.options(),c.gapCodes(),c.length(),c.hostEffects(),c.implicitArea());throw new AssertionError("typed contradictory family");}catch(IllegalArgumentException expected){}
            copy=j.deepCopy();command(copy).put("rawText"," ".repeat(c.rawText().length()));need(decode(copy) instanceof SpJsonDecoder.Decoded,"no raw-text reinterpretation");
        }
        for(var n:List.of("bms-receive-missing","bms-receive-ambiguous","bms-receive-dynamic","memory-copy-kill")) {
            var j=wire(n);need(command(j).path("implicitArea").isMissingNode()||command(j).path("implicitArea").isNull(),"no invented binding "+n);
            var decoded=decode(j);need(decoded instanceof SpJsonDecoder.Decoded,"unresolved accepted "+n);
            var r=new CobolLowerer().lower(((SpJsonDecoder.Decoded)decoded).input(),CobolLower.POSITIVE_OPTIONS);need(r.publication().isPresent()&&r.validation().orElseThrow().isStructurallyValid(),"unresolved operand stays bounded "+n);
        }
    }
    public static void main(String[] args)throws Exception {
        for(var name:List.of("retrieve-model","retrieve-real","receive-implicit"))try(var stream=CicsMemoryIndependenceSuite.class.getResourceAsStream("/sp/cics-memory/"+name+".json")) {
            var decoded=new SpJsonDecoder(CobolLower.INPUT_LIMITS).decode(stream.readAllBytes());need(decoded instanceof SpJsonDecoder.Decoded,"decode "+name);
            var result=new CobolLowerer().lower(((SpJsonDecoder.Decoded)decoded).input(),CobolLower.POSITIVE_OPTIONS);need(result.publication().isPresent(),"publish "+name);
            var operations=result.publication().orElseThrow().units().stream().flatMap(u->u.sequences().stream()).map(Sequence::terminator).toList();
            var command=operations.stream().filter(t->t instanceof Operations.Opaque o&&o.observedKind().startsWith("cics-host-command/")).map(Operations.Opaque.class::cast).findFirst().orElseThrow(()->new AssertionError("known control requires no physical host admission "+name));
            need(!command.envelope().control().known().isEmpty(),"proved destination retained");need(command.envelope().memory().mustOverwrite().isEmpty(),"no kill from unproved host memory");
            if(!name.equals("retrieve-real"))need(command.envelope().memory().otherWrites() instanceof Scopes.WithinMemory,"unavailable host footprint remains conservative "+name);
            else need(command.envelope().memory().otherWrites()==Scopes.NoMemory.INSTANCE&&!command.envelope().memory().knownWrites().isEmpty(),"real bounded receiver retained");
            need(operations.stream().anyMatch(t->t instanceof Operations.Invoke i&&i.target() instanceof Interactions.LiteralTarget x&&x.name().equals("AFTERIO")),"continuation materialized "+name);
        }
        implicitContract();
        System.out.println("PASS CicsMemoryIndependenceSuite model, missing implicit binding, real receiver");
    }
}
