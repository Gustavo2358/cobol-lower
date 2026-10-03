package io.github.gustavo2358.lower.adapters.testing;

import com.fasterxml.jackson.databind.node.ObjectNode;
import io.github.gustavo2358.lower.adapters.cli.CobolLower;
import io.github.gustavo2358.lower.adapters.sp.SpJsonDecoder;
import io.github.gustavo2358.lower.application.CobolLowerer;
import java.util.*;

/** An input policy changes its reentry frontier, never its materialization complexity. */
public final class UnifiedActivationSuite {
    private UnifiedActivationSuite() { }
    public static void main(String[] args)throws Exception {
        int cases=0;
        for(var name:List.of("ctxboom-04","ctxboom-08","ctxboom-inline-04","ctxboom-inline-08","ctxboom-cics-04","ctxboom-cics-08","ctxboom-escape-04","ctxboom-escape-08",
                "cics-changing","cics-condition","cics-handler-reentry","escape-nested","escape-inline","cics-escape-changing",
                "ctxboom-times","ctxboom-until","ctxboom-varying","cics-escape-times","cics-escape-until","cics-escape-varying")) {
            for(var mode:List.of("unspecified","mixed","published")) {
                ObjectNode tree;try(var stream=UnifiedActivationSuite.class.getResourceAsStream("/sp/compact-perform/"+name+".json")) {
                    tree=(ObjectNode)CobolControlSuite.J.readTree(Objects.requireNonNull(stream,name));
                }
                int n=0;for(var b:tree.path("controlTopology").path("bindings")) {
                    if(mode.equals("unspecified")||mode.equals("mixed")&&n++%2==0)((ObjectNode)b).put("reentryPolicy","UNSPECIFIED");
                }
                var input=((SpJsonDecoder.Decoded)CobolControlSuite.decode(tree)).input();
                var result=new CobolLowerer().lower(input,CobolLower.POSITIVE_OPTIONS);
                var p=result.publication().orElseThrow();
                if(!result.validation().orElseThrow().isStructurallyValid())throw new AssertionError(name+" invalid publication");
                // Cross-paragraph inline bodies have distinct endpoints and may each contain all source points.
                // Bound source-point/binding pairs, never the number of caller histories (N=4 used 975).
                var topology=input.controlTopology().orElseThrow();
                long limit=name.startsWith("ctxboom-inline-")
                    ?(long)(topology.bindings().size()+1)*input.statements().size()+topology.bindings().stream().mapToLong(b->b.phases().size()+2).sum():200;
                if(p.units().getFirst().sequences().size()>limit)throw new AssertionError(name+"/"+mode+": expanded call chains: "+p.units().getFirst().sequences().size()+" > "+limit);
                if(mode.equals("unspecified")&&p.uncertainties().stream().anyMatch(u->u.code().equals("cobol-lower:LOCAL_REENTRY_SOURCE_UNDEFINED")))
                    throw new AssertionError("source policy was invented for "+name);
                if(!name.endsWith("08"))ControlLanguageOracle.reference(result,"compact-perform--"+name+"--"+(mode.equals("unspecified")?mode:"published"));
                cases++;System.out.println("UNIFIED "+name+"/"+mode+" sequences="+p.units().getFirst().sequences().size());
            }
        }
        System.out.println("UNIFIED_ACTIVATIONS=PASS cases="+cases);
    }
}
