package io.github.gustavo2358.lower.adapters.testing;
import io.github.gustavo2358.lower.adapters.sp.SpJsonDecoder;
import io.github.gustavo2358.lower.adapters.cli.CobolLower;
import io.github.gustavo2358.lower.application.CobolLowerer;
import io.github.gustavo2358.air.model.*;
import io.github.gustavo2358.lower.domain.SpInput;
import java.nio.file.*;
import java.util.*;
/** IBM CICS TS5.6 oracle: twelve FILE commands, distinct contexts and output-name direction. */
public final class CicsFileControlSuite {
    static final List<String> COMMANDS=List.of("read","write","rewrite","delete","startbr","readnext","readprev","resetbr","endbr","unlock","inquire","set");
    static SpInput composition(int n)throws Exception {
        var input=PartialIntegrationSuite.fixture("compose-"+n);
        var facts=input.statements().stream().map(f->{
            if(!(f instanceof SpInput.CallFact call))return f;
            SpInput.CallTarget target=call.target() instanceof SpInput.DataCallTarget d?new SpInput.DataCallTarget(io.github.gustavo2358.lower.testing.IfInputs.with(d.reference(),"role",SpInput.OperandRole.READ)):call.target();
            return new SpInput.CicsFileFact(call.header(),"ENDBR","EXEC CICS ENDBR FILE(WS-A) NOHANDLE END-EXEC",SpInput.CicsFileTargetMode.INPUT,Optional.of(target),
                List.of(new SpInput.CicsFileOption("FILE","FILE",Optional.of("WS-A"),16,26,SpInput.CicsFileRole.READ,Optional.empty(),Optional.empty(),Optional.empty()),new SpInput.CicsFileOption("NOHANDLE","NOHANDLE",Optional.empty(),27,35,SpInput.CicsFileRole.NONE,Optional.empty(),Optional.empty(),Optional.empty())),SpInput.CicsConditions.LOCAL_CONDITION,call.normalContinuation(),call.normalContinuation(),"cics-ts.file@1",List.of("CICS_FILE_OUTCOME_VALUES_UNKNOWN"));
        }).toList();
        var result=io.github.gustavo2358.lower.testing.IfInputs.with(input,"statements",facts);var lowered=PartialIntegrationSuite.lower(result).publication().orElseThrow();
        check(files(lowered).size()>=n,"FILE composition multiplicity "+n);var codec=new io.github.gustavo2358.air.json.AirJson();check(codec.decode(codec.encode(lowered)).equals(lowered),"composition AIR roundtrip "+n);return result;
    }
    public static void main(String[] args)throws Exception {
        for(var name:COMMANDS){var p=lower(name);var sites=files(p);check(sites.size()==1,"one FILE command "+name);var i=sites.getFirst();
            check(i.action().equals(name),"exact action "+name);check(i.target() instanceof Interactions.LiteralTarget t&&t.namespace().equals("cics.file")&&t.name().equals("ACCOUNTS"),"source FILE name "+name);
            check(i.contract() instanceof Interactions.KnownContract c&&c.reference().authority().equals("cics-ts.file-control")&&c.reference().version().equals("1"),"versioned FILE context "+name);
            check(i.arguments().size()>=2,"context parameters "+name);
            check(i.outcomes().known().stream().anyMatch(Control.Normal.class::isInstance),"local normal return "+name);
            if(name.equals("read"))check(!commandMayWriteVisible(i)&&i.effectBound().otherwise().writes() instanceof Scopes.WithinMemory,
                "external READ retains its known receiver without a visible-memory fallback");
            if(name.equals("write")||name.equals("rewrite"))check(i.effectBound().otherwise().reads() instanceof Scopes.WithinMemory m
                &&!(m.scope() instanceof Scopes.VisibleMemory),"WRITE reads its known buffer without global memory");
            if(name.equals("write")||name.equals("rewrite")||name.equals("endbr")||name.equals("unlock"))check(i.effectBound().otherwise().writes()==Scopes.NoMemory.INSTANCE,"input-only command has no invented host writes "+name);

        }
        var p=lower("host-and-output");var sites=files(p);check(sites.size()==2,"computed input and INQUIRE output survive alongside LINK");
        var read=sites.stream().filter(i->i.action().equals("read")).findFirst().orElseThrow();check(read.target() instanceof Interactions.ComputedTarget t&&t.name() instanceof Expressions.Read,"computed FILE reads exact area");
        check(((Interactions.ValueArgument)read.arguments().get(1)).value() instanceof Expressions.Read,"computed SYSID reads separate area");
        var next=sites.stream().filter(i->i.action().equals("inquire")).findFirst().orElseThrow();check(next.target() instanceof Interactions.ComputedTarget t&&t.name() instanceof Expressions.Unknown,"NEXT output never reads old filename");
        check(p.units().stream().flatMap(u->u.sequences().stream()).map(Sequence::terminator).anyMatch(i->i instanceof Operations.Invoke v&&v.target() instanceof Interactions.LiteralTarget t&&t.namespace().equals("cics.program")),"LINK remains Program Control");
        for(var name:List.of("read-call","write-call","length-open","response","set-pointer","browse-id","system-two","inquire-next","inquire-start","inquire-end","set-alias","handler-open","sysid-short","file-slice","computed-exact","computed-closed","computed-partial","computed-unknown","computed-systems","computed-timing")) {
            var value=lower(name);var commands=files(value);check(commands.size()==(name.equals("computed-timing")?2:1),"preserve source sites "+name);var command=commands.getFirst();
            if(name.equals("length-open"))check(!commandMayWriteVisible(command)&&command.effectBound().otherwise().writes() instanceof Scopes.WithinMemory,
                "unproved LENGTH keeps the known INTO receiver without global memory");
            if(name.equals("response")){check(command.effectBound().otherwise().mustOverwrite().isEmpty(),"no unconditional RESP MUST before return");check(command.effectBound().perOutcome().size()==1&&command.effectBound().perOutcome().getFirst().effects().mustOverwrite().size()==2,"RESP/RESP2 four-byte writes on return");}
            if(name.equals("browse-id")){check(command.arguments().size()==3,"REQID protocol slot");check(((Interactions.ValueArgument)command.arguments().get(2)).value() instanceof Expressions.Literal l&&l.value() instanceof Values.IntValue i&&i.value().intValueExact()==5,"REQID input value");}
            if(name.startsWith("inquire-"))check(command.target() instanceof Interactions.ComputedTarget t&&t.name() instanceof Expressions.Unknown,"browse has no previous FILE input "+name);
            if(name.equals("set-alias"))check(command.target() instanceof Interactions.LiteralTarget t&&t.name().equals("ACCOUNTS"),"admin DSNAME cannot replace FILE target");
            if(name.equals("sysid-short"))check(((Interactions.ValueArgument)command.arguments().get(1)).value() instanceof Expressions.Unknown,"four-byte SYSID proof required");
            check(command.outcomes().remainder()==Scopes.NoControl.INSTANCE,
                "option diagnostics do not add arbitrary FILE control "+name);
        }
        var responseInput=((SpJsonDecoder.Decoded)new SpJsonDecoder(CobolLower.INPUT_LIMITS).decode(bytes("response"))).input();
        var responseFact=responseInput.statements().stream().filter(SpInput.CicsFileFact.class::isInstance).map(SpInput.CicsFileFact.class::cast).findFirst().orElseThrow();
        var responseBase=files(PartialIntegrationSuite.lower(responseInput).publication().orElseThrow()).getFirst();
        var missingLiteral=io.github.gustavo2358.lower.testing.IfInputs.with(responseFact,"target",Optional.empty());
        var invalidLiteral=io.github.gustavo2358.lower.testing.IfInputs.with(responseInput,"statements",responseInput.statements().stream().map(f->f==responseFact?missingLiteral:f).toList());
        check(new CobolLowerer().lower(invalidLiteral,CobolLower.OPTIONS).publication().isEmpty(),"missing typed target for literal FILE is invalid input");
        var hostInput=((SpJsonDecoder.Decoded)new SpJsonDecoder(CobolLower.INPUT_LIMITS).decode(bytes("computed-unknown"))).input();
        var hostFact=hostInput.statements().stream().filter(SpInput.CicsFileFact.class::isInstance).map(SpInput.CicsFileFact.class::cast).findFirst().orElseThrow();
        var missingFile=io.github.gustavo2358.lower.testing.IfInputs.with(hostFact,"target",Optional.empty());
        var missingInput=io.github.gustavo2358.lower.testing.IfInputs.with(hostInput,"statements",hostInput.statements().stream().map(f->f==hostFact?missingFile:f).toList());
        var missingResult=PartialIntegrationSuite.lower(missingInput).publication().orElseThrow();
        var missingTerm=missingResult.units().stream().flatMap(u->u.sequences().stream()).map(Sequence::terminator)
            .filter(t->t instanceof Operations.Opaque o&&o.observedKind().equals("cics-file-target-unavailable/endbr")).findFirst().orElseThrow();
        var missingEnvelope=((Operations.Opaque)missingTerm).envelope();
        check(missingEnvelope.memory().mustOverwrite().isEmpty()&&missingEnvelope.control().known().size()==1
            &&missingEnvelope.memory().otherReads() instanceof Scopes.WithinMemory,
            "unmaterialized host FILE name is coverage while SYSID read and normal return remain executable");
        for(int count:List.of(1,50)) {
            var diagnostics=new ArrayList<>(responseFact.gapCodes());
            for(int n=0;n<count;n++)diagnostics.add("W3_OPTION_DIAGNOSTIC_"+n);
            var changed=io.github.gustavo2358.lower.testing.IfInputs.with(responseFact,"gapCodes",diagnostics);
            var candidate=io.github.gustavo2358.lower.testing.IfInputs.with(responseInput,"statements",responseInput.statements().stream().map(f->f==responseFact?changed:f).toList());
            var lowered=PartialIntegrationSuite.lower(candidate);
            check(lowered.publication().isPresent(),"CICS FILE diagnostic-only mutation publishes "+count);
            var command=files(lowered.publication().orElseThrow()).getFirst();
            check(command.target() instanceof Interactions.LiteralTarget target&&responseBase.target() instanceof Interactions.LiteralTarget baseTarget
                &&target.name().equals(baseTarget.name())&&target.namespace().equals(baseTarget.namespace())
                &&command.effectBound().otherwise().reads().getClass()==responseBase.effectBound().otherwise().reads().getClass()
                &&command.effectBound().otherwise().writes().getClass()==responseBase.effectBound().otherwise().writes().getClass()
                &&command.effectBound().perOutcome().size()==responseBase.effectBound().perOutcome().size()
                &&command.effectBound().perOutcome().getFirst().effects().mustOverwrite().size()==2
                &&command.outcomes().known().size()==responseBase.outcomes().known().size()
                &&command.outcomes().remainder().getClass()==responseBase.outcomes().remainder().getClass(),
                "CICS FILE option diagnostics cannot change target, effects or control "+count);
        }
        // C06-HUMAN-20260917: real SP keeps spelling; lowering consumes canonical facts only.
        for(var name:List.of("read-dataset-literal","read-dataset-computed")) {
            var decoded=(SpJsonDecoder.Decoded)new SpJsonDecoder(CobolLower.INPUT_LIMITS).decode(bytes(name));
            var fact=decoded.input().statements().stream().filter(SpInput.CicsFileFact.class::isInstance).map(SpInput.CicsFileFact.class::cast).findFirst().orElseThrow();
            var option=fact.options().getFirst();
            check(option.name().equals("DATASET")&&option.canonicalName().equals("FILE")&&option.role()==SpInput.CicsFileRole.READ,"source spelling and canonical role preserved");
            check(fact.rawText().substring(option.start(),option.end()).startsWith("DATASET("),"source offsets preserved");
            var publication=lower(name);var commands=files(publication);check(commands.size()==1&&commands.getFirst().action().equals("read"),"exactly one legacy READ FILE");
            var target=commands.getFirst().target();
            if(name.endsWith("literal"))check(target instanceof Interactions.LiteralTarget t&&t.namespace().equals("cics.file")&&t.name().equals("ACCOUNTS"),"literal is CICS FILE, never DSNAME");
            else check(target instanceof Interactions.ComputedTarget t&&t.namespace().equals("cics.file")&&t.name() instanceof Expressions.Read,"computed CICS FILE uses general storage read");
            check(publication.units().stream().flatMap(u->u.sequences().stream()).map(Sequence::terminator).anyMatch(t->t instanceof Operations.Invoke i&&i.target() instanceof Interactions.LiteralTarget n&&n.namespace().equals("cics.program")&&n.name().equals("PGM")),"Program Control remains independent");
            var noFile=io.github.gustavo2358.lower.testing.IfInputs.with(option,"canonicalName","DSNAME");
            var badFact=io.github.gustavo2358.lower.testing.IfInputs.with(fact,"options",fact.options().stream().map(o->o==option?noFile:o).toList());
            var badInput=io.github.gustavo2358.lower.testing.IfInputs.with(decoded.input(),"statements",decoded.input().statements().stream().map(f->f==fact?badFact:f).toList());
            check(new CobolLowerer().lower(badInput,CobolLower.OPTIONS).publication().isEmpty(),"DSNAME cannot stand in for canonical FILE identity");
            var broadened=io.github.gustavo2358.lower.testing.IfInputs.with(fact,"command","READNEXT");
            broadened=io.github.gustavo2358.lower.testing.IfInputs.with(broadened,"options",fact.options().stream().map(o->o.canonicalName().equals("RIDFLD")?io.github.gustavo2358.lower.testing.IfInputs.with(o,"role",SpInput.CicsFileRole.READ_WRITE):o).toList());
            var finalBroadened=broadened;
            var broadenedInput=io.github.gustavo2358.lower.testing.IfInputs.with(decoded.input(),"statements",decoded.input().statements().stream().map(f->f==fact?finalBroadened:f).toList());
            var rejected=new CobolLowerer().lower(broadenedInput,CobolLower.OPTIONS);
            check(rejected.publication().isEmpty()&&rejected.admission().diagnostics().stream().anyMatch(d->d.requirement().equals("aliases are command scoped")),"READNEXT DATASET remains outside the authorized alias contract");
        }
        var mapper=new com.fasterxml.jackson.databind.ObjectMapper();
        for(var mutation:List.of("old-version","wrong-role","wrong-mode","missing-context","unknown-field","false-local")) {
            var root=(com.fasterxml.jackson.databind.node.ObjectNode)mapper.readTree(bytes("read"));var f=(com.fasterxml.jackson.databind.node.ObjectNode)root.path("statements").findValues("variant").stream().findFirst().map(x->root.path("statements").get(0)).orElseThrow();
            for(var s:root.path("statements"))if(s.path("variant").asText().equals("CICS_FILE_CONTROL"))f=(com.fasterxml.jackson.databind.node.ObjectNode)s;
            switch(mutation){case "old-version"->root.put("contractVersion","2.27.0");case "wrong-role"->((com.fasterxml.jackson.databind.node.ObjectNode)f.path("options").get(0)).put("role","WRITE");case "wrong-mode"->f.put("targetMode","OUTPUT");case "missing-context"->f.remove("targetMode");case "unknown-field"->f.put("bindingMechanism","UNKNOWN");case "false-local"->f.put("conditions","DEFAULT_ENTRY_PREFIX");}
            var d=new SpJsonDecoder(CobolLower.INPUT_LIMITS).decode(mapper.writeValueAsBytes(root));check(!(d instanceof SpJsonDecoder.Decoded v)||new CobolLowerer().lower(v.input(),CobolLower.OPTIONS).publication().isEmpty(),"reject "+mutation);
        }
        for(var name:COMMANDS){
            var decoded=(SpJsonDecoder.Decoded)new SpJsonDecoder(CobolLower.INPUT_LIMITS).decode(bytes(name));var input=decoded.input();
            var original=input.statements().stream().filter(io.github.gustavo2358.lower.domain.SpInput.CicsFileFact.class::isInstance).map(io.github.gustavo2358.lower.domain.SpInput.CicsFileFact.class::cast).findFirst().orElseThrow();
            var absent=io.github.gustavo2358.lower.testing.IfInputs.with(original,"options",original.options().stream().filter(o->!o.canonicalName().equals("FILE")).toList());
            var contradiction=io.github.gustavo2358.lower.testing.IfInputs.with(original,"targetMode",io.github.gustavo2358.lower.domain.SpInput.CicsFileTargetMode.OUTPUT);
            for(var wrong:List.of(absent,contradiction)){
                var bad=io.github.gustavo2358.lower.testing.IfInputs.with(input,"statements",input.statements().stream().map(f->f==original?wrong:f).toList());
                check(new CobolLowerer().lower(bad,CobolLower.OPTIONS).publication().isEmpty(),"in-memory command contradiction "+name);
            }
        }
        var input=((SpJsonDecoder.Decoded)new SpJsonDecoder(CobolLower.INPUT_LIMITS).decode(bytes("host-and-output"))).input();
        var source=input.statements().stream().filter(SpInput.CicsFileFact.class::isInstance).map(SpInput.CicsFileFact.class::cast).filter(f->f.target().orElse(null) instanceof SpInput.DataCallTarget).findFirst().orElseThrow();
        var reference=((SpInput.DataCallTarget)source.target().orElseThrow()).reference();
        var badReference=io.github.gustavo2358.lower.testing.IfInputs.with(reference,"role",SpInput.OperandRole.WRITE);
        var badFact=io.github.gustavo2358.lower.testing.IfInputs.with(source,"target",Optional.of(new SpInput.DataCallTarget(badReference)));
        var badInput=io.github.gustavo2358.lower.testing.IfInputs.with(input,"statements",input.statements().stream().map(f->f==source?badFact:f).toList());
        check(new CobolLowerer().lower(badInput,CobolLower.OPTIONS).publication().isEmpty(),"input FILE target must be a read");
        System.out.println("PASS CicsFileControlSuite 35 fixtures / 12 commands + READ DATASET + computed/output + wire and memory negatives");
    }
    static boolean commandMayWriteVisible(Operations.Invoke i){return i.effectBound().otherwise().writes() instanceof Scopes.WithinMemory m&&(m.scope() instanceof Scopes.VisibleMemory||m.scope() instanceof Scopes.MemoryUnion u&&u.members().stream().anyMatch(Scopes.VisibleMemory.class::isInstance));}
    static List<Operations.Invoke> files(Publication p){return p.units().stream().flatMap(u->u.sequences().stream()).map(Sequence::terminator).filter(Operations.Invoke.class::isInstance).map(Operations.Invoke.class::cast).filter(i->i.target() instanceof Interactions.LiteralTarget t&&t.namespace().equals("cics.file")||i.target() instanceof Interactions.ComputedTarget c&&c.namespace().equals("cics.file")).toList();}
    static Publication lower(String name)throws Exception{var d=new SpJsonDecoder(CobolLower.INPUT_LIMITS).decode(bytes(name));check(d instanceof SpJsonDecoder.Decoded,"SP2.28 FILE decoded "+name+" "+d);var r=new CobolLowerer().lower(((SpJsonDecoder.Decoded)d).input(),CobolLower.OPTIONS);check(r.publication().isPresent(),"FILE admitted "+name+" "+r.admission().diagnostics()+" "+r.validation());var p=r.publication().orElseThrow();var codec=new io.github.gustavo2358.air.json.AirJson();var wire=codec.encode(p);check(p.equals(codec.decode(wire)),"AIR roundtrip "+name);Files.createDirectories(Path.of("adapters/target/fd-w8"));Files.write(Path.of("adapters/target/fd-w8/"+name+".air.json"),wire);return p;}
    static byte[] bytes(String name)throws Exception{return Files.readAllBytes(Path.of("adapters/src/test/resources/sp/file-dependencies/w8/"+name+".sp.json"));}
    static void check(boolean b,String message){if(!b)throw new AssertionError(message);}
}
