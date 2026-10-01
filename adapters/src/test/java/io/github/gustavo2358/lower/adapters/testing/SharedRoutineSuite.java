package io.github.gustavo2358.lower.adapters.testing;

import io.github.gustavo2358.air.model.*;
import io.github.gustavo2358.lower.adapters.cli.CobolLower;
import io.github.gustavo2358.lower.adapters.sp.SpJsonDecoder;
import io.github.gustavo2358.lower.application.CobolLowerer;
import java.util.*;

/** Two source callers, one body, distinct returns. Existing producer fixture is the authority. */
public final class SharedRoutineSuite {
    private SharedRoutineSuite() { }
    public static void main(String[] args)throws Exception {
        var resource="/sp/perform-reentry/sequential-callers.json";
        try(var stream=SharedRoutineSuite.class.getResourceAsStream(resource)) {
            var tree=(com.fasterxml.jackson.databind.node.ObjectNode)CobolControlSuite.J.readTree(Objects.requireNonNull(stream));
            var input=((SpJsonDecoder.Decoded)CobolControlSuite.decode(tree)).input();
            var result=new CobolLowerer().lower(input,CobolLower.POSITIVE_OPTIONS);
            var p=result.publication().orElseThrow();
            if(!result.validation().orElseThrow().isStructurallyValid())throw new AssertionError(result.validation());
            var seqs=p.units().getFirst().sequences();
            var calls=seqs.stream().map(Sequence::terminator).filter(Operations.LocalInvoke.class::isInstance).map(Operations.LocalInvoke.class::cast).toList();
            if(calls.size()!=2)throw new AssertionError("two local calls required, got "+calls.size());
            if(!calls.get(0).entry().equals(calls.get(1).entry()))throw new AssertionError("body must be shared");
            if(calls.get(0).resume().equals(calls.get(1).resume()))throw new AssertionError("caller continuations must differ");
            long bodies=seqs.stream().filter(s->s.terminator() instanceof Operations.Invoke i&&i.target() instanceof Interactions.LiteralTarget l&&l.name().equals("INP")).count();
            if(bodies!=1)throw new AssertionError("exactly one native body call, got "+bodies);
            if(!p.capabilities().required().contains(Capabilities.LOCAL_CONTROL))throw new AssertionError("local-control capability required");
            if(!new io.github.gustavo2358.air.json.AirJson().decode(new io.github.gustavo2358.air.json.AirJson().encode(p)).equals(p))throw new AssertionError("transport");
        }
        checkCics("shared-cics-stable", true);
        checkCics("shared-cics-distinct-state", false);
        System.out.println("SHARED_ROUTINE_CHECKS=13");
    }
    private static void checkCics(String fixture, boolean sameState)throws Exception {
        try(var stream=SharedRoutineSuite.class.getResourceAsStream("/sp/shared-routines/"+fixture+".json")) {
            var tree=(com.fasterxml.jackson.databind.node.ObjectNode)CobolControlSuite.J.readTree(Objects.requireNonNull(stream));
            var input=((SpJsonDecoder.Decoded)CobolControlSuite.decode(tree)).input();
            var result=new CobolLowerer().lower(input,CobolLower.POSITIVE_OPTIONS);
            var p=result.publication().orElseThrow();
            if(!result.validation().orElseThrow().isStructurallyValid())throw new AssertionError(result.validation());
            var calls=p.units().getFirst().sequences().stream().map(Sequence::terminator)
                .filter(Operations.LocalInvoke.class::isInstance).map(Operations.LocalInvoke.class::cast).toList();
            if(calls.size()!=2)throw new AssertionError(fixture+": expected two local invocations, got "+calls.size());
            if(calls.get(0).entry().equals(calls.get(1).entry())!=sameState)
                throw new AssertionError(fixture+": body identity must distinguish entry handler states");
            var reached=LocalControlOracle.reached(p);var seqs=LocalControlOracle.sequences(p);
            long resumes=reached.stream().filter(point->seqs.get(point.label()).terminator() instanceof Operations.LocalResume).count();
            if(resumes!=2)throw new AssertionError(fixture+": matched returns "+resumes);
        }
    }
}
