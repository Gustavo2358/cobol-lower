package io.github.gustavo2358.lower.adapters.testing;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.github.gustavo2358.air.json.AirJson;
import io.github.gustavo2358.air.model.*;
import io.github.gustavo2358.lower.adapters.cli.CobolLower;
import io.github.gustavo2358.lower.adapters.sp.SpJsonDecoder;
import io.github.gustavo2358.lower.application.*;
import io.github.gustavo2358.lower.domain.SpInput;
import io.github.gustavo2358.lower.testing.IfInputs;
import java.util.*;
import static io.github.gustavo2358.lower.testing.CallOracle.check;

public final class PerformVaryingIntegrationSuite {
    public static void main(String[] args) throws Exception {run();}
    public static void run() throws Exception {
        var codec=new AirJson();
        var partial=List.of("unresolved-control","noninteger","nonscalar","subscript-control","unknown-from","unknown-by","variable-by","zero-by",
            "unsupported-condition","unresolved-condition","after-level","incoming","escape","cycle","recursive","partial-end","unknown-body");
        var names=new ArrayList<>(List.of("before","default","after","thru","from-read","decrement","branches","1","2","5","40"));names.addAll(partial);
        for(var name:names) {
            var input=PerformFamilyIntegrationSuite.inputFixture("varying-"+name);
            var result=PartialIntegrationSuite.lower(input);var reverse=new ArrayList<>(input.statements());Collections.reverse(reverse);
            check(Arrays.equals(codec.encode(result.publication().orElseThrow()),codec.encode(PartialIntegrationSuite.lower(IfInputs.with(input,"statements",reverse)).publication().orElseThrow())),"VARYING statement permutation "+name);
            var effects=result.publication().orElseThrow().units().getFirst().sequences().stream().map(Sequence::terminator)
                .filter(Operations.Opaque.class::isInstance).map(Operations.Opaque.class::cast)
                .filter(o->o.observedKind().startsWith("perform-varying-")).toList();
            if(!partial.contains(name)||name.equals("unsupported-condition")||name.equals("unresolved-condition")) {
                check(effects.size()==2*input.statements().stream().filter(SpInput.ProcedurePerformFact.class::isInstance).count(),
                    "initialization and update per callsite "+name+" actual="+effects.size());
                for(var e:effects)check(e.envelope().memory().knownWrites().size()==1 && e.envelope().memory().knownWrites().equals(e.envelope().memory().mustOverwrite())
                    && e.envelope().memory().otherWrites()==Scopes.NoMemory.INSTANCE && e.envelope().control().remainder()==Scopes.NoControl.INSTANCE,"localized must-write with closed continuation "+name);
            } else if(Set.of("incoming","escape","cycle","recursive","unknown-body").contains(name)) {
                check(!effects.isEmpty(),"known repetition composes independently of body/isolation qualification "+name);
                for(var e:effects)check(e.envelope().memory().knownWrites().size()==1
                    && e.envelope().memory().knownWrites().equals(e.envelope().memory().mustOverwrite())
                    && e.envelope().memory().otherWrites()==Scopes.NoMemory.INSTANCE,
                    "partial body does not broaden implicit VARYING writes "+name);
                if(name.equals("recursive"))check(result.publication().orElseThrow().uncertainties().stream()
                    .anyMatch(u->u.code().contains("RECURSIVE_PERFORM_NOT_SUPPORTED")),"recursive return remains unsupported");
            } else check(effects.isEmpty(),"unavailable repetition/entry not specialized "+name);
        }
        var input=PerformFamilyIntegrationSuite.inputFixture("varying-after");
        var p=(SpInput.ProcedurePerformFact)input.statements().stream().filter(SpInput.ProcedurePerformFact.class::isInstance).findFirst().orElseThrow();
        var varying=p.varying().orElseThrow();
        for(var bad:List.of(IfInputs.with(varying,"levels",2),IfInputs.with(varying,"controls",List.of()),
                IfInputs.with(varying,"controls",varying.controls().stream().map(o->o.role()==SpInput.VaryingOperandRole.BY?IfInputs.with(o,"integer",Optional.of("0")):o).toList()),
                IfInputs.with(varying,"controls",varying.controls().stream().map(o->o.role()==SpInput.VaryingOperandRole.CONTROL_VARIABLE?IfInputs.with(o,"references",List.of(IfInputs.with(o.references().getFirst(),"wholeItemAccess",Optional.empty()))):o).toList()))) {
            var changed=IfInputs.with(p,"varying",Optional.of(bad));
            var result=new CobolLowerer().lower(IfInputs.with(input,"statements",input.statements().stream().map(s->s==p?changed:s).toList()),CobolLower.OPTIONS);
            check(result.publication().isPresent(),"unsupported varying detail preserves the published range");
            check(result.publication().orElseThrow().units().getFirst().sequences().stream().map(Sequence::terminator)
                .filter(Operations.Opaque.class::isInstance).map(Operations.Opaque.class::cast)
                .noneMatch(o->o.observedKind().startsWith("perform-varying-")),"unproved varying update is not executable");
        }
        for(var name:partial) {
            var adversarial=PerformFamilyIntegrationSuite.inputFixture("varying-"+name);
            var facts=adversarial.statements().stream().map(s->s instanceof SpInput.ProcedurePerformFact f?IfInputs.with(f,"gapCodes",List.of()):s).toList();
            var original=new CobolLowerer().lower(adversarial,CobolLower.OPTIONS);
            var diagnosticOnly=new CobolLowerer().lower(IfInputs.with(adversarial,"statements",facts),CobolLower.OPTIONS);
            check(original.publication().isPresent()&&diagnosticOnly.status()==original.status(),
                "VARYING gap metadata cannot change structural admission "+name);
        }
        try(var stream=PerformVaryingIntegrationSuite.class.getResourceAsStream("/sp/perform-family/varying-after.json")) {
            var json=new ObjectMapper();var raw=(ObjectNode)json.readTree(stream);raw.put("contractVersion","2.4.0");
            check(new SpJsonDecoder(CobolLower.INPUT_LIMITS).decode(json.writeValueAsBytes(raw)) instanceof SpJsonDecoder.Rejected,"SP2.4 cannot reinterpret VARYING fields");
            for(var s:raw.path("statements"))if(s.path("variant").asText().equals("PERFORM_PROCEDURE"))((ObjectNode)s).remove("varying");
            check(new SpJsonDecoder(CobolLower.INPUT_LIMITS).decode(json.writeValueAsBytes(raw)) instanceof SpJsonDecoder.Rejected,"SP2.4 cannot reinterpret numeric predicate profile");
        }
        System.out.println("LOWER_PERFORM_VARYING=PASS");
    }
}
