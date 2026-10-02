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
        checkCyclic();
        checkContextExplosion();
        checkCics("shared-cics-stable", true);
        checkCics("shared-cics-distinct-state", false);
        System.out.println("SHARED_ROUTINE_CHECKS=13; compact fixtures=5; exhaustive control comparisons=4");
    }
    private static void checkContextExplosion()throws Exception {
        for(var name:List.of("ctxboom-04","ctxboom-08","ctxboom-times","ctxboom-until","ctxboom-varying")) {
            try(var stream=SharedRoutineSuite.class.getResourceAsStream("/sp/compact-perform/"+name+".json")) {
                var tree=(com.fasterxml.jackson.databind.node.ObjectNode)CobolControlSuite.J.readTree(Objects.requireNonNull(stream));
                var input=((SpJsonDecoder.Decoded)CobolControlSuite.decode(tree)).input();
                var result=new CobolLowerer().lower(input,CobolLower.POSITIVE_OPTIONS);var p=result.publication().orElseThrow();
                if(!result.validation().orElseThrow().isStructurallyValid())throw new AssertionError(result.validation());
                if(p.units().getFirst().sequences().size()>100)throw new AssertionError(name+": copied call chains instead of compact bodies");
                if(!p.capabilities().required().contains(Capabilities.LOCAL_REENTRY_GUARD))throw new AssertionError(name+": missing guard");
                var wire=new io.github.gustavo2358.air.json.AirJson();if(!wire.decode(wire.encode(p)).equals(p))throw new AssertionError(name+": transport");
                var permuted=tree.deepCopy();
                for(var field:List.of("occurrences","regions","boundaries","outcomes","bindings","proofs")) {
                    var values=(com.fasterxml.jackson.databind.node.ArrayNode)permuted.path("controlTopology").path(field);
                    var reversed=new ArrayList<com.fasterxml.jackson.databind.JsonNode>();values.forEach(reversed::add);Collections.reverse(reversed);values.removeAll();reversed.forEach(values::add);
                }
                var reordered=((SpJsonDecoder.Decoded)CobolControlSuite.decode(permuted)).input();
                if(!p.equals(new CobolLowerer().lower(reordered,CobolLower.POSITIVE_OPTIONS).publication().orElseThrow()))throw new AssertionError(name+": SCC scheduling changed the publication");
                // Historical policy selects the previous explicit expansion. Compare all finite
                // control traces, including instruction steps in TIMES/UNTIL/VARYING phases.
                // N=8 is a size regression only: expanding it is the original OOM fixture.
                if(!name.equals("ctxboom-08")) {
                    var old=tree.deepCopy();old.put("contractVersion","2.56.0");
                    old.path("controlTopology").path("bindings").forEach(b->((com.fasterxml.jackson.databind.node.ObjectNode)b).remove("reentryPolicy"));
                    var historical=((SpJsonDecoder.Decoded)CobolControlSuite.decode(old)).input();
                    var expanded=new CobolLowerer().lower(historical,CobolLower.POSITIVE_OPTIONS);
                    ControlLanguageOracle.equivalent(result,expanded,name);
                }
            }
        }
    }
    private static void checkCyclic()throws Exception {
        try(var stream=SharedRoutineSuite.class.getResourceAsStream("/sp/perform-reentry/reentry-mutual.json")) {
            var tree=(com.fasterxml.jackson.databind.node.ObjectNode)CobolControlSuite.J.readTree(Objects.requireNonNull(stream));
            var input=((SpJsonDecoder.Decoded)CobolControlSuite.decode(tree)).input();
            var result=new CobolLowerer().lower(input,CobolLower.POSITIVE_OPTIONS);
            var p=result.publication().orElseThrow();
            if(!result.validation().orElseThrow().isStructurallyValid())throw new AssertionError(result.validation());
            var guards=p.units().getFirst().sequences().stream().map(Sequence::terminator)
                .filter(Operations.LocalInvoke.class::isInstance).map(Operations.LocalInvoke.class::cast)
                .flatMap(i->i.reentryGuard().stream()).toList();
            if(guards.isEmpty())throw new AssertionError("recursive body must share with explicit activation guards");
            if(!p.capabilities().required().contains(Capabilities.LOCAL_REENTRY_GUARD))throw new AssertionError("guard capability required");
            if(!new io.github.gustavo2358.air.json.AirJson().decode(new io.github.gustavo2358.air.json.AirJson().encode(p)).equals(p))throw new AssertionError("guard transport");
        }
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
