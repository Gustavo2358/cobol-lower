package io.github.gustavo2358.lower.adapters.testing;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.node.*;
import io.github.gustavo2358.air.json.AirJson;
import io.github.gustavo2358.air.model.*;
import io.github.gustavo2358.lower.adapters.cli.CobolLower;
import io.github.gustavo2358.lower.adapters.sp.SpJsonDecoder;
import io.github.gustavo2358.lower.application.*;
import io.github.gustavo2358.lower.domain.SpInput;
import io.github.gustavo2358.lower.testing.IfInputs;
import java.util.*;
import static io.github.gustavo2358.lower.testing.CallOracle.check;

public final class GoToIntegrationSuite {
    private static final ObjectMapper JSON=new ObjectMapper();
    static final List<String> NAMES=List.of("g1","g2","g3","g4","g5","backward","cycle","if-jump","evaluate-jump",
        "empty","unknown","depending","alter","section","ambiguous","qualified","perform-adjacent","perform-disjoint","perform-overlap",
        "compose-1","compose-2","compose-5","compose-40");
    private static byte[] fixture(String name) throws Exception {
        try(var in=GoToIntegrationSuite.class.getResourceAsStream("/sp/goto/"+name+".json")) { return Objects.requireNonNull(in,name).readAllBytes(); }
    }
    private static SpInput decode(byte[] bytes) {
        var r=new SpJsonDecoder(CobolLower.INPUT_LIMITS).decode(bytes);
        check(r instanceof SpJsonDecoder.Decoded,"SP2.1 decoded: "+r);return ((SpJsonDecoder.Decoded)r).input();
    }
    private static void primaryClosure() throws Exception {
        var input=decode(fixture("perform-adjacent"));
        var perform=(SpInput.PerformFact)input.statements().stream().filter(SpInput.PerformFact.class::isInstance).findFirst().orElseThrow();
        var g=(SpInput.GoToFact)input.statements().stream().filter(SpInput.GoToFact.class::isInstance).findFirst().orElseThrow();
        var body=input.statements().stream().filter(s->s.header().id().equals(perform.targetEntry().orElseThrow())).findFirst().orElseThrow();
        var overlap=new SpInput.GoToFact(g.header(),Optional.of(new SpInput.GoToTarget(perform.target().orElseThrow().id(),perform.target().orElseThrow().paragraphOrigin())),
            g.referenceOrigin(),Optional.of(body.header().id()),Optional.of(body.header().provenance()),List.of());
        var cycle=new SpInput.GoToFact(g.header(),g.target(),g.referenceOrigin(),Optional.of(g.header().id()),Optional.of(g.header().provenance()),List.of());
        var partial=new SpInput.GoToFact(g.header(),g.target(),g.referenceOrigin(),Optional.empty(),Optional.empty(),List.of("GO_TO_TARGET_ENTRY_UNAVAILABLE"));
        PartialIntegrationSuite.lower(input);
        for(var changed:List.of(overlap,cycle,partial)) {
            var facts=input.statements().stream().map(s->s==g?changed:s).toList();
            var result=new CobolLowerer().lower(IfInputs.with(input,"statements",facts),CobolLower.OPTIONS);
            check(result.status()==LoweringResult.Status.INVALID_INPUT && result.admission().diagnostics().stream()
                .anyMatch(d->d.requirement().equals("BASIC activation contradicts intrinsic body facts")),"GO TO overlap/cycle/open frontier cannot prove closed disjoint primary");
        }
    }
    public static void main(String[] args) throws Exception { run(); }
    public static void run() throws Exception {
        var codec=new AirJson();
        for(var name:NAMES) {
            var raw=fixture(name);var input=decode(raw);var result=PartialIntegrationSuite.lower(input);var p=result.publication().orElseThrow();
            var sequences=new HashMap<io.github.gustavo2358.air.model.Ids.LabelId,Sequence>();p.units().getFirst().sequences().forEach(s->sequences.put(s.label(),s));
            for(var fact:input.statements())if(fact instanceof SpInput.GoToFact g) {
                var links=result.statements().stream().filter(l->l.source().equals(g.header().id())).toList();check(links.size()==1,"one GO TO occurrence retained");
                var sequence=sequences.get(links.getFirst().label());check(sequence.instructions().isEmpty(),"GO TO is control only");
                if(g.gapCodes().isEmpty()) {
                    check(sequence.terminator() instanceof Operations.Jump,"precise GO TO is Jump "+name);
                    var target=result.statements().stream().filter(l->l.source().equals(g.targetEntry().orElseThrow())).findFirst().orElseThrow().label();
                    check(((Operations.Jump)sequence.terminator()).destination().equals(target),"sole successor is published target entry "+name);
                    var origin=p.origins().stream().filter(o->o.id().equals(sequence.origin())).findFirst().orElseThrow();
                    check(origin instanceof Origins.Derived d && d.inputs().size()==4,"occurrence/reference/paragraph/entry provenance");
                } else {
                    check(sequence.terminator() instanceof Operations.Opaque,"partial remains Opaque");
                    var opaque=(Operations.Opaque)sequence.terminator();
                    check(opaque.envelope().control().known().isEmpty(),"no invented fallthrough");
                    check(opaque.envelope().control().remainder() instanceof Scopes.WithinControl,"unit control frontier stays open");
                }
            }
            if(name.startsWith("compose-"))check(input.statements().stream().filter(SpInput.GoToFact.class::isInstance).count()==Integer.parseInt(name.substring(8)),"no multiplicity cap");
            var statements=new ArrayList<>(input.statements());Collections.reverse(statements);
            check(Arrays.equals(codec.encode(p),codec.encode(PartialIntegrationSuite.lower(IfInputs.with(input,"statements",statements)).publication().orElseThrow())),"SP physical inventory independence "+name);
        }
        primaryClosure();
        var input=decode(fixture("g1"));var g=(SpInput.GoToFact)input.statements().stream().filter(SpInput.GoToFact.class::isInstance).findFirst().orElseThrow();
        for(var bad:List.of(IfInputs.with(g,"targetEntry",Optional.of(new SpInput.StatementId(input.unit(),"statement:999999"))),
                IfInputs.with(g,"entryOrigin",Optional.of(g.header().provenance())))) {
            var statements=input.statements().stream().map(s->s==g?bad:s).toList();
            check(new CobolLowerer().lower(IfInputs.with(input,"statements",statements),CobolLower.OPTIONS).status()==LoweringResult.Status.INVALID_INPUT,"contradictory GO TO rejected in memory");
        }
        var raw=(ObjectNode)JSON.readTree(fixture("g1")); raw.put("contractVersion","2.0.0");
        check(new SpJsonDecoder(CobolLower.INPUT_LIMITS).decode(JSON.writeValueAsBytes(raw)) instanceof SpJsonDecoder.Rejected,"SP2.0 meaning unchanged");
        raw=(ObjectNode)JSON.readTree(fixture("g1"));
        for(var node:raw.withArray("statements"))if(node.path("variant").asText().equals("GO_TO"))((ObjectNode)node).put("normalContinuation","forbidden");
        check(new SpJsonDecoder(CobolLower.INPUT_LIMITS).decode(JSON.writeValueAsBytes(raw)) instanceof SpJsonDecoder.Rejected,"GO TO wire has no fallthrough field");
        System.out.println("LOWER_GOTO_FIXTURES="+NAMES.size());
    }
}
