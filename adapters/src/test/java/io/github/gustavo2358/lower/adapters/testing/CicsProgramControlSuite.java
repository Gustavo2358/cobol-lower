package io.github.gustavo2358.lower.adapters.testing;

import io.github.gustavo2358.air.model.*;
import io.github.gustavo2358.air.json.AirJson;
import io.github.gustavo2358.lower.adapters.sp.SpJsonDecoder;
import io.github.gustavo2358.lower.adapters.cli.CobolLower;
import io.github.gustavo2358.lower.application.*;
import java.util.*;
import java.nio.charset.StandardCharsets;

/** Public SP2.13 producer fixtures; transport/admission/physical target contracts. */
public final class CicsProgramControlSuite {
    private CicsProgramControlSuite(){ }
    static io.github.gustavo2358.lower.domain.SpInput composition(int n) throws Exception {
        var input=PartialIntegrationSuite.fixture("compose-"+n);
        var facts=input.statements().stream().map(f->{
            if(!(f instanceof io.github.gustavo2358.lower.domain.SpInput.CallFact c))return f;
            return new io.github.gustavo2358.lower.domain.SpInput.CicsFact(c.header(),io.github.gustavo2358.lower.domain.SpInput.CicsCommand.LINK,
                "EXEC CICS LINK PROGRAM(WS-A) NOHANDLE END-EXEC",Optional.of(c.target()),List.of(),io.github.gustavo2358.lower.domain.SpInput.CicsConditions.LOCAL_CONDITION,c.normalContinuation(),"cics-ts.program@1",List.of("CICS_EFFECTS_SIGNATURE_PARTIAL"));
        }).toList();
        var result=io.github.gustavo2358.lower.testing.IfInputs.with(input,"statements",facts);
        var lowered=PartialIntegrationSuite.lower(result).publication().orElseThrow();
        long count=lowered.units().getFirst().sequences().stream().map(Sequence::terminator).filter(Operations.Invoke.class::isInstance).map(Operations.Invoke.class::cast)
            .filter(i->i.target() instanceof Interactions.ComputedTarget t&&t.namespace().equals("cics.program")||i.target() instanceof Interactions.LiteralTarget l&&l.namespace().equals("cics.program")).count();
        if(count<n)throw new AssertionError("CICS multiplicity/elision "+n);
        return result;
    }
    public static void main(String[] args) throws Exception {
        int cases=0;var decoder=new SpJsonDecoder(CobolLower.INPUT_LIMITS);
        for(String name:List.of("variable","qualified","short","slice","overlay","group")) {
            byte[] raw;try(var in=CicsProgramControlSuite.class.getResourceAsStream("/sp/cics/"+name+".json")){if(in==null)throw new AssertionError(name);raw=in.readAllBytes();}
            var input=((SpJsonDecoder.Decoded)decoder.decode(raw)).input();
            var result=new CobolLowerer().lower(input,CobolLower.OPTIONS);
            if(result.status()!=LoweringResult.Status.SUCCESS)throw new AssertionError(result.status()+" "+result.validation());
            var publication=result.publication().orElseThrow();var codec=new AirJson();if(!publication.equals(codec.decode(codec.encode(publication))))throw new AssertionError("roundtrip");
            var invoke=publication.units().getFirst().sequences().stream().map(Sequence::terminator).filter(Operations.Invoke.class::isInstance).map(Operations.Invoke.class::cast)
                .filter(i->i.target() instanceof Interactions.ComputedTarget t&&t.namespace().equals("cics.program")).findFirst().orElseThrow();
            var target=(Interactions.ComputedTarget)invoke.target();
            if(!(target.namePolicy() instanceof Interactions.ExtensionName e&&e.name().equals("cics-ts.program")))throw new AssertionError("platform policy");
            if(name.equals("short")!=(target.name() instanceof Expressions.Unknown))throw new AssertionError("eight-byte proof");
            if(!name.equals("short")&&!(target.name() instanceof Expressions.Read))throw new AssertionError("storage read");
            if(name.equals("slice")&&!(((Expressions.Read)target.name()).place() instanceof Places.RegionSlice))throw new AssertionError("slice identity");
            var signature=((Interactions.ExternalSignature)invoke.signature()).signature();
            if(!(signature.parameters().remainder() instanceof Interactions.UnknownRemainder)||!(signature.results().remainder() instanceof Interactions.UnknownRemainder))throw new AssertionError("partial signature");
            if(invoke.outcomes().known().stream().noneMatch(Control.Normal.class::isInstance))throw new AssertionError("LINK return");
            var old=new String(raw,StandardCharsets.UTF_8).replace("2.13.0","2.12.0").getBytes(StandardCharsets.UTF_8);
            if(decoder.decode(old) instanceof SpJsonDecoder.Decoded)throw new AssertionError("old contract accepted new variant");
            cases++;
        }
        System.out.println("CICS_LOWER_CASES="+cases);
    }
}
