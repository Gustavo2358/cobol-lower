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

public final class PerformTimesIntegrationSuite {
    public static void main(String[] args) throws Exception {run();}
    public static void run() throws Exception {
        var codec=new AirJson();
        for(var name:List.of("identifier","one","positive","large","thru","1","2","5","40","unresolved","noninteger","zero",
                "incoming","escape","cycle","recursive","partial-end","unknown-body")) {
            var input=PerformFamilyIntegrationSuite.inputFixture("times-"+name);
            var result=PartialIntegrationSuite.lower(input);var reverse=new ArrayList<>(input.statements());Collections.reverse(reverse);
            check(Arrays.equals(codec.encode(result.publication().orElseThrow()),codec.encode(PartialIntegrationSuite.lower(IfInputs.with(input,"statements",reverse)).publication().orElseThrow())),"TIMES statement permutation "+name);
        }
        var input=PerformFamilyIntegrationSuite.inputFixture("times-identifier");
        var p=(SpInput.ProcedurePerformFact)input.statements().stream().filter(SpInput.ProcedurePerformFact.class::isInstance).findFirst().orElseThrow();
        var count=p.times().orElseThrow();
        for(var bad:List.of(IfInputs.with(count,"reference",Optional.empty()),IfInputs.with(count,"integer",Optional.of("5")),
                IfInputs.with(count,"reference",Optional.of(IfInputs.with(count.reference().orElseThrow(),"wholeItemAccess",Optional.empty()))))) {
            var changed=IfInputs.with(p,"times",Optional.of(bad));
            check(new CobolLowerer().lower(IfInputs.with(input,"statements",input.statements().stream().map(s->s==p?changed:s).toList()),CobolLower.OPTIONS).status()==LoweringResult.Status.INVALID_INPUT,"contradictory count rejected");
        }
        for(var name:List.of("incoming","escape","cycle","recursive","partial-end","noninteger","unresolved","zero")) {
            var adversarial=PerformFamilyIntegrationSuite.inputFixture("times-"+name);
            var facts=adversarial.statements().stream().map(s->s instanceof SpInput.ProcedurePerformFact f?IfInputs.with(f,"gapCodes",List.of()):s).toList();
            check(new CobolLowerer().lower(IfInputs.with(adversarial,"statements",facts),CobolLower.OPTIONS).status()==LoweringResult.Status.INVALID_INPUT,"false closed TIMES rejected "+name);
        }
        try(var stream=PerformTimesIntegrationSuite.class.getResourceAsStream("/sp/perform-family/times-identifier.json")) {
            var json=new ObjectMapper();var raw=(ObjectNode)json.readTree(stream);raw.put("contractVersion","2.3.0");
            check(new SpJsonDecoder(CobolLower.INPUT_LIMITS).decode(json.writeValueAsBytes(raw)) instanceof SpJsonDecoder.Rejected,"SP2.3 cannot reinterpret TIMES fields");
        }
        System.out.println("LOWER_PERFORM_TIMES=PASS");
    }
}
