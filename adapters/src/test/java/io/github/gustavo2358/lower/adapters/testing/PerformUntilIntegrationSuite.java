package io.github.gustavo2358.lower.adapters.testing;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.github.gustavo2358.air.json.AirJson;
import io.github.gustavo2358.lower.adapters.cli.CobolLower;
import io.github.gustavo2358.lower.adapters.sp.SpJsonDecoder;
import io.github.gustavo2358.lower.application.*;
import io.github.gustavo2358.lower.domain.SpInput;
import io.github.gustavo2358.lower.testing.IfInputs;
import java.util.*;
import static io.github.gustavo2358.lower.testing.CallOracle.check;

public final class PerformUntilIntegrationSuite {
    public static void main(String[] args) throws Exception {run();}
    public static void run() throws Exception {
        var codec=new AirJson();
        for(var name:List.of("before","default","after","constant","mixed","branches","thru","1","2","5","40","unresolved","unsupported",
                "incoming","escape","cycle","recursive","partial-end","unknown-body")) {
            var input=PerformFamilyIntegrationSuite.inputFixture("until-"+name);
            var result=PartialIntegrationSuite.lower(input);var reverse=new ArrayList<>(input.statements());Collections.reverse(reverse);
            check(Arrays.equals(codec.encode(result.publication().orElseThrow()),codec.encode(PartialIntegrationSuite.lower(IfInputs.with(input,"statements",reverse)).publication().orElseThrow())),"UNTIL statement permutation "+name);
        }
        var input=PerformFamilyIntegrationSuite.inputFixture("until-after");
        var p=(SpInput.ProcedurePerformFact)input.statements().stream().filter(SpInput.ProcedurePerformFact.class::isInstance).findFirst().orElseThrow();
        var loop=p.loop().orElseThrow();
        var altered=IfInputs.with(loop,"predicate",IfInputs.with(loop.predicate(),"knownReads",List.of()));
        var bad=IfInputs.with(p,"loop",Optional.of(altered));
        check(new CobolLowerer().lower(IfInputs.with(input,"statements",input.statements().stream().map(s->s==p?bad:s).toList()),CobolLower.OPTIONS).status()==LoweringResult.Status.INVALID_INPUT,"missing predicate reads contradict proof");
        for(var name:List.of("incoming","escape","cycle","recursive","partial-end","unsupported","unresolved")) {
            var adversarial=PerformFamilyIntegrationSuite.inputFixture("until-"+name);
            var facts=adversarial.statements().stream().map(s->s instanceof SpInput.ProcedurePerformFact f?IfInputs.with(f,"gapCodes",List.of()):s).toList();
            var original=new CobolLowerer().lower(adversarial,CobolLower.OPTIONS);
            var diagnosticOnly=new CobolLowerer().lower(IfInputs.with(adversarial,"statements",facts),CobolLower.OPTIONS);
            check(original.publication().isPresent()&&diagnosticOnly.status()==original.status(),
                "UNTIL gap metadata cannot change structural admission "+name+" original="+original.status()+" changed="+diagnosticOnly.status());
        }
        try(var stream=PerformUntilIntegrationSuite.class.getResourceAsStream("/sp/perform-family/until-after.json")) {
            var json=new ObjectMapper();var raw=(ObjectNode)json.readTree(stream);raw.put("contractVersion","2.2.0");
            check(new SpJsonDecoder(CobolLower.INPUT_LIMITS).decode(json.writeValueAsBytes(raw)) instanceof SpJsonDecoder.Rejected,"SP2.2 cannot reinterpret UNTIL fields");
        }
        System.out.println("LOWER_PERFORM_UNTIL=PASS");
    }
}
