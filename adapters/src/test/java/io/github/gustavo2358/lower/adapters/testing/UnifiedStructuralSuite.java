package io.github.gustavo2358.lower.adapters.testing;

import java.util.*;
import io.github.gustavo2358.air.model.*;
import io.github.gustavo2358.lower.adapters.cli.CobolLower;
import io.github.gustavo2358.lower.application.CobolLowerer;
import io.github.gustavo2358.lower.domain.SpInput;

/** Older structural facts must not enumerate acyclic call histories either. */
public final class UnifiedStructuralSuite {
    private UnifiedStructuralSuite() { }
    public static void main(String[] args)throws Exception {
        var input=CompositionalPerformRevisionSuite.input("F03-dag-live-004");
        var result=new CobolLowerer().lower(input,CobolLower.OPTIONS);var p=result.publication().orElseThrow();
        var written=input.statements().stream().filter(SpInput.ProcedurePerformFact.class::isInstance).count();
        var activations=p.origins().stream().filter(o->o instanceof Origins.Derived d&&d.rule().equals("perform-structural@1/conditional-activation-resume")).count();
        if(activations>written)throw new AssertionError("acyclic source graph expanded caller histories: "+activations+" activations for "+written+" written PERFORMs");
        ControlLanguageOracle.reference(result,"compositional-perform-r1--F03-dag-live-004--published");
        for(var name:List.of("F03-dag-cold-008","F03-chain-live-064")) {
            var other=CompositionalPerformRevisionSuite.input(name);
            ControlLanguageOracle.reference(new CobolLowerer().lower(other,CobolLower.OPTIONS),"compositional-perform-r1--"+name+"--published");
        }
        System.out.println("UNIFIED_STRUCTURAL=PASS written="+written+" activations="+activations);
    }
}
