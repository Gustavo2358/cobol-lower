package io.github.gustavo2358.lower.adapters.testing;

import io.github.gustavo2358.air.json.AirJson;
import io.github.gustavo2358.air.model.*;
import io.github.gustavo2358.lower.adapters.cli.CobolLower;
import io.github.gustavo2358.lower.adapters.sp.SpJsonDecoder;
import io.github.gustavo2358.lower.application.*;
import io.github.gustavo2358.lower.domain.SpInput;
import io.github.gustavo2358.lower.testing.IfInputs;
import java.nio.file.*;
import java.util.*;
import static io.github.gustavo2358.lower.testing.CallOracle.check;

/** Real SP1.8 source facts; every statement remains represented and activations remain distinct. */
public final class PartialIntegrationSuite {
    public static SpInput fixture(String name) throws Exception {
        try(var in=PartialIntegrationSuite.class.getResourceAsStream("/sp/partial-program/"+name+".json")) {
            var decoded=new SpJsonDecoder(CobolLower.INPUT_LIMITS).decode(Objects.requireNonNull(in,name).readAllBytes());
            check(decoded instanceof SpJsonDecoder.Decoded,"SP1.8 decoded: "+decoded);
            return ((SpJsonDecoder.Decoded)decoded).input();
        }
    }
    public static LoweringResult lower(SpInput input) {
        var r=new CobolLowerer().lower(input,CobolLower.OPTIONS);
        check(r.status()==LoweringResult.Status.SUCCESS,"partial publication: "+r.status()+" "+r.admission().diagnostics()+" "+r.validation());
        return r;
    }
    public static void run() throws Exception {
        var codec=new AirJson();
        for(var name:List.of("p1","p2","p3","p4","p5","mixed-data","entry-using","body-gap","control-body","display-handler","must-write","call-unknown","call-handlers","read","call-using","call-returning","perform-distinct","perform-repeated","if-arm","if-nested","if-unknown","compose-1","compose-2","compose-5","compose-40")) {
            var input=fixture(name);var r=lower(input);var publication=r.publication().orElseThrow();var unit=publication.units().getFirst();
            var unitOrigin=publication.origins().stream().filter(o->o.id().equals(unit.origin())).map(Origins.Derived.class::cast).findFirst().orElseThrow();
            var entryOrigin=unit.entries().getFirst().origin();
            var entrySequence=unit.sequences().stream().filter(q->q.label().equals(unit.entries().getFirst().initialLabel().orElseThrow())).findFirst().orElseThrow();
            check(unitOrigin.inputs().equals(List.of(entryOrigin,entrySequence.origin())) && !entryOrigin.equals(entrySequence.origin()),"unit lineage contains entry and actual initial sequence "+name);
            var expected=new HashSet<>(input.statements().stream().map(s->s.header().id()).toList());
            check(expected.equals(new HashSet<>(r.statements().stream().map(LoweringResult.StatementLink::source).toList())),"no source statement elision "+name);
            check(r.statements().stream().map(LoweringResult.StatementLink::target).distinct().count()==r.statements().size(),"unique operation per activation "+name);
            check(unit.sequences().stream().filter(s->s.terminator() instanceof Operations.Invoke).count()==input.statements().stream().filter(SpInput.CallFact.class::isInstance).count(),"CALL sites retained "+name);
            if(name.startsWith("compose-")||name.startsWith("perform-")) {
                check(unit.sequences().stream().noneMatch(s->s.terminator() instanceof Operations.Opaque),"precise supported composition "+name);
                for(var p:input.statements())if(p instanceof SpInput.PerformFact f) {
                    var link=r.statements().stream().filter(l->l.source().equals(f.header().id())).findFirst().orElseThrow();
                    var sequence=unit.sequences().stream().filter(s->s.label().equals(link.label())).findFirst().orElseThrow();
                    var first=((Operations.Jump)sequence.terminator()).destination();
                    var target=unit.sequences().stream().filter(s->s.label().equals(first)).findFirst().orElseThrow();
                    check(target.instructions().size()==1 && target.instructions().getFirst() instanceof Operations.Assign,"BASIC body assignment");
                    var resume=r.statements().stream().filter(l->l.source().equals(f.normalContinuation().statement().orElseThrow())).findFirst().orElseThrow().label();
                    check(target.terminator() instanceof Operations.Jump jump && jump.destination().equals(resume),"activation-specific return");
                }
            }
            if(List.of("p1","p2","p3","p5","read","if-unknown").contains(name)) {
                check(unit.sequences().stream().anyMatch(s->s.terminator() instanceof Operations.Opaque),"conservative operation retained "+name);
                check(!publication.uncertainties().isEmpty(),"explicit gap "+name);
            }
            for(var sequence:unit.sequences()) if(sequence.terminator() instanceof Operations.Opaque opaque
                    && publication.uncertainties().stream().anyMatch(u->opaque.header().uncertainties().contains(u.id()) && u.code().equals("cobol-lower:NORMAL_CONTINUATION_NOT_PROVEN"))) {
                var memory=opaque.envelope().memory();
                check(opaque.knownOperands().isEmpty() && memory.knownReads().isEmpty() && memory.knownWrites().isEmpty()
                    && memory.otherReads()==Scopes.NoMemory.INSTANCE && memory.otherWrites()==Scopes.NoMemory.INSTANCE,
                    "control-only frontier does not repeat or reopen a proved write "+name);
            }
            var bytes=codec.encode(publication);check(Arrays.equals(bytes,codec.encode(codec.decode(bytes))),"AIR A/B "+name);
            var facts=new ArrayList<>(input.statements());Collections.reverse(facts);
            var reversed=lower(IfInputs.with(input,"statements",facts));
            check(Arrays.equals(bytes,codec.encode(reversed.publication().orElseThrow())),"physical SP inventory cannot select control or identities "+name);
            var out=Path.of("target/partial-program");Files.createDirectories(out);Files.write(out.resolve(name+".air.json"),bytes);
        }
        var basic=fixture("perform-repeated");
        var facts=new ArrayList<>(basic.statements());
        var first=(SpInput.PerformFact)facts.stream().filter(SpInput.PerformFact.class::isInstance).findFirst().orElseThrow();
        var contradictory=IfInputs.with(first,"targetExit",first.normalContinuation().statement());
        facts.set(facts.indexOf(first),contradictory);
        var rejected=new CobolLowerer().lower(IfInputs.with(basic,"statements",facts),CobolLower.OPTIONS);
        check(rejected.status()==LoweringResult.Status.INVALID_INPUT && rejected.publication().isEmpty(),"contradictory intrinsic body is structural invalidity, not partial success");
        var entries=fixture("entry-using");var entryPublication=lower(entries).publication().orElseThrow();
        check(entryPublication.units().getFirst().entries().getFirst().signature().parameters().remainder() instanceof Interactions.UnknownRemainder,
            "unavailable entry signature remains open");
        for(var name:List.of("must-write","p4")) {
            var publication=lower(fixture(name)).publication().orElseThrow();
            check(publication.units().getFirst().sequences().stream().flatMap(q->q.instructions().stream()).anyMatch(Operations.HavocMust.class::isInstance),
                "proved mandatory write uses generic HavocMust "+name);
        }
        System.out.println("PARTIAL_COMPOSITIONALITY=PASS");
    }
    public static void main(String[] args) throws Exception {run();}
}
