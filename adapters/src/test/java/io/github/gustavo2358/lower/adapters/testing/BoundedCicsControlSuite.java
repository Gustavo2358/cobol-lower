package io.github.gustavo2358.lower.adapters.testing;

import io.github.gustavo2358.air.model.*;
import io.github.gustavo2358.air.model.Ids.*;
import io.github.gustavo2358.air.json.AirJson;
import io.github.gustavo2358.lower.adapters.sp.SpJsonDecoder;
import io.github.gustavo2358.lower.adapters.cli.CobolLower;
import io.github.gustavo2358.lower.application.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.*;

/** Source topology licenses a local CICS condition route, not a successful XCTL return. */
public final class BoundedCicsControlSuite {
    private BoundedCicsControlSuite() { }
    private static int checks;
    private static void need(boolean value,String message) {checks++;if(!value)throw new AssertionError(message);}
    private static ObjectNode fixture(String name)throws Exception {
        try(var in=BoundedCicsControlSuite.class.getResourceAsStream("/sp/typed-occurrence-r5/"+name+".json")) {
            if(in==null)throw new AssertionError("missing producer fixture "+name);
            return (ObjectNode)new ObjectMapper().readTree(in);
        }
    }
    private static LoweringResult lower(ObjectNode document)throws Exception {
        var decoded=new SpJsonDecoder(CobolLower.INPUT_LIMITS).decode(new ObjectMapper().writeValueAsBytes(document));
        need(decoded instanceof SpJsonDecoder.Decoded,"typed SP accepted: "+decoded);
        var result=new CobolLowerer().lower(((SpJsonDecoder.Decoded)decoded).input(),CobolLower.OPTIONS);
        need(result.publication().isPresent(),"publication: "+result.status()+" "+result.validation());
        var p=result.publication().orElseThrow();var codec=new AirJson();
        need(p.equals(codec.decode(codec.encode(p))),"official AIR strict codec roundtrip");
        return result;
    }
    private static Terminator operation(LoweringResult result,String source) {
        var ids=result.statements().stream().filter(s->s.source().handle().equals(source)).map(LoweringResult.StatementLink::target).toList();
        var operations=result.publication().orElseThrow().units().stream().flatMap(u->u.sequences().stream()).map(Sequence::terminator)
            .filter(o->ids.contains(o.header().id())).toList();
        need(operations.size()==1,"one operation for witness source "+source);return operations.getFirst();
    }
    private static Set<LabelId> local(Scopes.ControlScope scope) {
        if(scope instanceof Scopes.LabelsControl labels)return new HashSet<>(labels.labels());
        if(scope instanceof Scopes.ControlUnion union) {
            var result=new HashSet<LabelId>();for(var member:union.members())result.addAll(local(member));return result;
        }
        need(scope instanceof Scopes.UnitControl u&&!u.labels()&&!u.normalExit()&&!u.exceptionalExit()&&!u.halt()&&!u.diverge()&&u.externalControl(),
            "external uncertainty stays external-only; no handler/global destination");return Set.of();
    }
    private static int external(Scopes.ControlScope scope) {
        if(scope instanceof Scopes.UnitControl u)return u.externalControl()?1:0;
        return scope instanceof Scopes.ControlUnion u?u.members().stream().mapToInt(BoundedCicsControlSuite::external).sum():0;
    }
    private static Scopes.ControlBound bound(Terminator op) {
        return op instanceof Operations.Invoke i?i.outcomes().remainder():((Operations.Opaque)op).envelope().control().remainder();
    }
    private static void bounded(ObjectNode document,boolean typed)throws Exception {
        var result=lower(document);var op=operation(result,"statement:3");
        var expected=result.statements().stream().filter(s->s.source().handle().equals("statement:1")).map(LoweringResult.StatementLink::label).collect(java.util.stream.Collectors.toSet());
        need(expected.size()==1,"independent witness target identity");
        need(bound(op) instanceof Scopes.WithinControl,"bounded open control remains represented");
        var scope=((Scopes.WithinControl)bound(op)).scope();
        need(local(scope).equals(expected),"R6 proved XCTL local condition destination must survive");
        need(external(scope)==1,"existing external-control uncertainty survives");
        if(typed) {
            need(op instanceof Operations.Invoke i&&i.action().equals("execute"),"XCTL occurrence remains execute");
            var invoke=(Operations.Invoke)op;
            need(invoke.outcomes().known().isEmpty(),"no successful normal return or invented handler outcome");
            need(invoke.signature() instanceof Interactions.ExternalSignature s&&s.signature().parameters().remainder() instanceof Interactions.UnknownRemainder,"partial signature survives local proof");
            need(invoke.effectBound().otherwise().reads() instanceof Scopes.NoMemory&&invoke.effectBound().otherwise().writes() instanceof Scopes.NoMemory,"control proof cannot invent global memory");
        } else need(op instanceof Operations.Opaque o&&o.envelope().control().known().isEmpty(),"target omission preserves local bound without fabricating a call or return");
        need(op.header().precision().control().status()==Evidence.PrecisionStatus.OPEN,"condition route remains partial");
        var link=(Operations.Invoke)operation(result,"statement:2");
        need(link.outcomes().known().size()==1&&link.outcomes().known().getFirst() instanceof Control.Normal&&link.outcomes().remainder() instanceof Scopes.NoControl,"LINK semantics unchanged");
        need(result.equals(new CobolLowerer().lower(((SpJsonDecoder.Decoded)new SpJsonDecoder(CobolLower.INPUT_LIMITS).decode(new ObjectMapper().writeValueAsBytes(document))).input(),CobolLower.OPTIONS)),"repeat lowering deterministic");
    }
    private static void contexts(String name,Set<String> sources,Set<String> destinations,int expected)throws Exception {
        ObjectNode document;
        try(var in=BoundedCicsControlSuite.class.getResourceAsStream("/sp/bounded-cics-r6/"+name+".json")) {
            if(in==null)throw new AssertionError("missing R6 fixture "+name);
            document=(ObjectNode)new ObjectMapper().readTree(in);
        }
        var result=lower(document);var p=result.publication().orElseThrow();
        var invokes=p.units().stream().flatMap(u->u.sequences().stream()).map(Sequence::terminator)
            .filter(Operations.Invoke.class::isInstance).map(Operations.Invoke.class::cast).filter(i->i.action().equals("execute")).toList();
        need(invokes.size()==expected,"bounded activation/source count "+name);
        var actualSources=new HashSet<String>();var actualDestinations=new HashSet<LabelId>();
        for(var invoke:invokes) {
            var links=result.statements().stream().filter(l->l.target().equals(invoke.header().id())).toList();
            need(links.size()==1,"one identity link per internal operation");actualSources.add(links.getFirst().source().handle());
            need(invoke.outcomes().known().isEmpty(),"local condition is not normal successful return");
            var allowed=local(((Scopes.WithinControl)invoke.outcomes().remainder()).scope());
            need(allowed.size()==1,"each activation has exactly its own resume");actualDestinations.addAll(allowed);
        }
        var expectedDestinations=result.statements().stream().filter(l->destinations.contains(l.source().handle())).map(LoweringResult.StatementLink::label).collect(java.util.stream.Collectors.toSet());
        need(actualSources.equals(sources),"distinct source identities survive "+name);
        need(actualDestinations.equals(expectedDestinations)&&expectedDestinations.size()==2,"exact activation/source routes; no ordinary tail or cross-context expansion "+name);
    }
    public static void main(String[] args)throws Exception {
        bounded(fixture("I-invokes"),true);
        var lexical=fixture("I-invokes");
        for(var s:lexical.withArray("statements"))if(s.path("command").asText().equals("XCTL"))((ObjectNode)s).put("rawText","X".repeat(s.path("rawText").asText().length()));
        bounded(lexical,true); // semantic facts suffice without usable source text
        var unknown=fixture("I-invokes");
        for(var s:unknown.withArray("statements"))if(s.path("command").asText().equals("XCTL")) {
            var c=(ObjectNode)s;c.put("conditions","UNKNOWN");c.withArray("gapCodes").add("CICS_HANDLER_STATE_UNKNOWN");
        }
        bounded(unknown,true); // independent unknown handler state cannot revoke topology proof
        var legacy=fixture("I-invokes");
        for(var s:legacy.withArray("statements"))if(s.path("command").asText().equals("XCTL"))
            for(var f:List.of("localContinuation","ordinaryContinuation")) {
                var c=(ObjectNode)s.path(f);c.put("availability","UNAVAILABLE");c.putNull("statement");
            }
        bounded(legacy,true); // legacy positional metadata is not an alternate routing authority
        var missing=fixture("I-invokes");
        for(var s:missing.withArray("statements"))if(s.path("command").asText().equals("XCTL"))((ObjectNode)s).putNull("target");
        bounded(missing,false);
        for(var name:List.of("A-known","B-unavailable")) {
            var result=lower(fixture(name));
            for(var u:result.publication().orElseThrow().units())for(var seq:u.sequences())if(seq.terminator() instanceof Operations.Invoke i&&i.action().equals("execute")) {
                need(i.outcomes().known().isEmpty(),"unknown XCTL has no known successor");
                need(i.outcomes().remainder() instanceof Scopes.WithinControl w&&w.scope() instanceof Scopes.LabelsControl l&&l.labels().isEmpty(),"unknown local remains empty/open even with positional KNOWN");
                need(i.header().precision().control().status()==Evidence.PrecisionStatus.UNAVAILABLE,"unavailable control remains unavailable");
            }
        }
        contexts("context-resumes",Set.of("statement:6"),Set.of("statement:0","statement:1"),2);
        contexts("distinct-equal",Set.of("statement:1","statement:2"),Set.of("statement:2","statement:0"),2);
        System.out.println("BOUNDED_CICS_CONTROL_CHECKS="+checks);
    }
}
