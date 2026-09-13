package io.github.gustavo2358.lower.adapters.testing;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.node.*;
import io.github.gustavo2358.air.json.AirJson;
import io.github.gustavo2358.lower.adapters.cli.CobolLower;
import io.github.gustavo2358.lower.adapters.sp.SpJsonDecoder;
import io.github.gustavo2358.lower.application.*;
import io.github.gustavo2358.lower.domain.SpInput;
import io.github.gustavo2358.lower.testing.IfInputs;
import java.util.*;
import static io.github.gustavo2358.lower.testing.CallOracle.check;

public final class PerformFamilyIntegrationSuite {
    private static final ObjectMapper JSON=new ObjectMapper();
    private static byte[] fixture(String name) throws Exception {
        try(var in=PerformFamilyIntegrationSuite.class.getResourceAsStream("/sp/perform-family/"+name+".json")){return Objects.requireNonNull(in,name).readAllBytes();}
    }
    private static SpInput decode(byte[] bytes) {
        var result=new SpJsonDecoder(CobolLower.INPUT_LIMITS).decode(bytes);
        check(result instanceof SpJsonDecoder.Decoded,"SP2.2 decodes: "+result);return ((SpJsonDecoder.Decoded)result).input();
    }
    static SpInput inputFixture(String name) throws Exception { return decode(fixture(name)); }
    public static void main(String[] args) throws Exception {run();}
    public static void run() throws Exception {
        var codec=new AirJson();
        for(var name:List.of("t1","t2","t3","through","if-body","evaluate-body","local-jump","terminal","call-body",
                "thru-1","thru-2","thru-5","thru-40","unknown-body","incoming","escape","overlap","recursive","cycle","partial-end","partial-start","reverse","empty")) {
            var input=decode(fixture(name));var result=PartialIntegrationSuite.lower(input);
            var reverse=new ArrayList<>(input.statements());Collections.reverse(reverse);
            check(Arrays.equals(codec.encode(result.publication().orElseThrow()),codec.encode(PartialIntegrationSuite.lower(IfInputs.with(input,"statements",reverse)).publication().orElseThrow())),"range physical order independence "+name);
        }
        var input=decode(fixture("t2"));var p=(SpInput.ProcedurePerformFact)input.statements().stream().filter(SpInput.ProcedurePerformFact.class::isInstance).findFirst().orElseThrow();
        var first=p.procedures().getFirst();var last=p.procedures().getLast();
        var wrongEntry=IfInputs.with(first,"entry",last.entry());
        var wrongCompletions=IfInputs.with(first,"completions",List.of(last.entry()));
        var dangling=IfInputs.with(first,"entry",new SpInput.StatementId(input.unit(),"statement:999999"));
        var badParagraphs=new ArrayList<SpInput.ProcedurePerformFact>();
        for(var changed:List.of(wrongEntry,wrongCompletions,dangling)) {
            var paragraphs=new ArrayList<>(p.procedures());paragraphs.set(0,changed);badParagraphs.add(IfInputs.with(p,"procedures",paragraphs));
        }
        badParagraphs.add(IfInputs.with(p,"start",p.end()));
        badParagraphs.add(IfInputs.with(p,"normalContinuation",IfInputs.with(p.normalContinuation(),"statement",Optional.of(first.entry()))));
        for(var bad:badParagraphs) {
            var facts=input.statements().stream().map(s->s==p?bad:s).toList();
            check(new CobolLowerer().lower(IfInputs.with(input,"statements",facts),CobolLower.OPTIONS).status()==LoweringResult.Status.INVALID_INPUT,"contradictory range facts rejected in memory");
        }
        for(var name:List.of("incoming","escape","overlap","recursive","cycle")) {
            var adversarial=decode(fixture(name));
            var facts=adversarial.statements().stream().map(s->s instanceof SpInput.ProcedurePerformFact p2?IfInputs.with(p2,"gapCodes",List.of()):s).toList();
            check(new CobolLowerer().lower(IfInputs.with(adversarial,"statements",facts),CobolLower.OPTIONS).status()==LoweringResult.Status.INVALID_INPUT,"false closed activation rejected "+name);
        }
        var raw=(ObjectNode)JSON.readTree(fixture("t2"));raw.put("contractVersion","2.1.0");
        check(new SpJsonDecoder(CobolLower.INPUT_LIMITS).decode(JSON.writeValueAsBytes(raw)) instanceof SpJsonDecoder.Rejected,"SP2.1 cannot reinterpret new range facts");
        System.out.println("LOWER_PERFORM_FAMILY_PHASE1=PASS");
    }
}
