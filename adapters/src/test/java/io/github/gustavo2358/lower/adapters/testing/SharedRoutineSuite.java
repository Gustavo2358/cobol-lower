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
            var decoded=CobolControlSuite.decode(tree);
                if(!(decoded instanceof SpJsonDecoder.Decoded accepted))throw new AssertionError("fixture decode: "+decoded);
                var input=accepted.input();
            var result=new CobolLowerer().lower(input,CobolLower.POSITIVE_OPTIONS);
            var p=result.publication().orElseThrow();
            if(!result.validation().orElseThrow().isStructurallyValid())throw new AssertionError(result.validation());
            var seqs=p.units().getFirst().sequences();
            var calls=seqs.stream().map(Sequence::terminator).filter(Operations.LocalInvoke.class::isInstance).map(Operations.LocalInvoke.class::cast).toList();
            if(calls.size()!=4)throw new AssertionError("two activations and two body invokes required, got "+calls.size());
            var byEntry=new HashMap<io.github.gustavo2358.air.model.Ids.LabelId,List<Operations.LocalInvoke>>();
            calls.forEach(call->byEntry.computeIfAbsent(call.entry(),k->new ArrayList<>()).add(call));
            var shared=byEntry.values().stream().filter(group->group.size()==2).toList();
            if(shared.size()!=1)throw new AssertionError("both activations must invoke one shared body");
            if(shared.getFirst().get(0).resume().equals(shared.getFirst().get(1).resume()))throw new AssertionError("caller continuations must differ");
            ControlLanguageOracle.reference(result,"perform-reentry--sequential-callers--published");
            long bodies=seqs.stream().filter(s->s.terminator() instanceof Operations.Invoke i&&i.target() instanceof Interactions.LiteralTarget l&&l.name().equals("INP")).count();
            if(bodies!=1)throw new AssertionError("exactly one native body call, got "+bodies);
            if(!p.capabilities().required().contains(Capabilities.LOCAL_CONTROL))throw new AssertionError("local-control capability required");
            if(!new io.github.gustavo2358.air.json.AirJson().decode(new io.github.gustavo2358.air.json.AirJson().encode(p)).equals(p))throw new AssertionError("transport");
        }
        checkSharedProofs();
        checkContextModes();
        checkCyclic();
        checkContextExplosion();
        checkCics("shared-cics-stable", true);
        checkCics("shared-cics-distinct-state", false);
        System.out.println("SHARED_ROUTINES=PASS; compact fixtures=19; exhaustive control comparisons=16; adversarial mutations=4");
    }
    private static void checkSharedProofs()throws Exception {
        com.fasterxml.jackson.databind.node.ObjectNode tree;
        try(var stream=SharedRoutineSuite.class.getResourceAsStream("/sp/perform-reentry/sequential-callers.json")) {
            tree=(com.fasterxml.jackson.databind.node.ObjectNode)CobolControlSuite.J.readTree(Objects.requireNonNull(stream));
        }
        var proofs=(com.fasterxml.jackson.databind.node.ArrayNode)tree.path("controlTopology").path("proofs");
        var required=new HashSet<String>();int caller=0;
        for(var binding:tree.path("controlTopology").path("bindings")) {
            var proof=(com.fasterxml.jackson.databind.node.ObjectNode)proofs.get(0).deepCopy();
            var marker="shared-caller-proof-"+caller++;proof.put("id",marker);proof.put("rule",marker);proofs.add(proof);
            ((com.fasterxml.jackson.databind.node.ArrayNode)binding.path("proofs")).add(marker);
            required.add("SP2.39/"+proof.path("kind").asText()+"/"+marker);
        }
        var decoded=CobolControlSuite.decode(tree);
                if(!(decoded instanceof SpJsonDecoder.Decoded accepted))throw new AssertionError("fixture decode: "+decoded);
                var input=accepted.input();
        var p=new CobolLowerer().lower(input,CobolLower.POSITIVE_OPTIONS).publication().orElseThrow();
        var byId=new HashMap<io.github.gustavo2358.air.model.Ids.OriginId,Origins.Origin>();p.origins().forEach(o->byId.put(o.id(),o));
        var body=p.units().getFirst().sequences().stream().map(Sequence::terminator)
            .filter(t->t instanceof Operations.Invoke i&&i.target() instanceof Interactions.LiteralTarget l&&l.name().equals("INP")).findFirst().orElseThrow();
        var todo=new ArrayDeque<io.github.gustavo2358.air.model.Ids.OriginId>();todo.add(body.header().origin());var seen=new HashSet<io.github.gustavo2358.air.model.Ids.OriginId>();
        while(!todo.isEmpty()) {var id=todo.removeFirst();if(!seen.add(id))continue;
            if(byId.get(id) instanceof Origins.Derived d){required.remove(d.rule());todo.addAll(d.inputs());}}
        if(!required.isEmpty())throw new AssertionError("shared body lost caller proofs: "+required);
    }
    private static void checkContextExplosion()throws Exception {
        for(var name:List.of("ctxboom-04","ctxboom-08","ctxboom-times","ctxboom-until","ctxboom-varying")) {
            try(var stream=SharedRoutineSuite.class.getResourceAsStream("/sp/compact-perform/"+name+".json")) {
                var tree=(com.fasterxml.jackson.databind.node.ObjectNode)CobolControlSuite.J.readTree(Objects.requireNonNull(stream));
                var decoded=CobolControlSuite.decode(tree);
                if(!(decoded instanceof SpJsonDecoder.Decoded accepted))throw new AssertionError("fixture decode: "+decoded);
                var input=accepted.input();
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
                // Compare both policies with frozen machines from the previous implementation.
                // The machines retain instruction steps in TIMES/UNTIL/VARYING phases.
                // N=8 is a size regression only: expanding it is the original OOM fixture.
                if(!name.equals("ctxboom-08")) {
                    var old=tree.deepCopy();
                    old.path("controlTopology").path("bindings").forEach(b->((com.fasterxml.jackson.databind.node.ObjectNode)b).put("reentryPolicy","UNSPECIFIED"));
                    var historical=((SpJsonDecoder.Decoded)CobolControlSuite.decode(old)).input();
                    var historicalResult=new CobolLowerer().lower(historical,CobolLower.POSITIVE_OPTIONS);
                    ControlLanguageOracle.reference(result,"compact-perform--"+name+"--published");
                    ControlLanguageOracle.reference(historicalResult,"compact-perform--"+name+"--unspecified");
                }
            }
        }
    }
    private static void checkContextModes()throws Exception {
        for(var name:List.of("ctxboom-cics-04","ctxboom-escape-04","ctxboom-cics-08","ctxboom-escape-08",
                "cics-return-state","cics-changing","cics-condition","cics-handler-reentry","escape-nested","escape-inline",
                "cics-escape-changing","cics-escape-times","cics-escape-until","cics-escape-varying")) {
            try(var stream=SharedRoutineSuite.class.getResourceAsStream("/sp/compact-perform/"+name+".json")) {
                var tree=(com.fasterxml.jackson.databind.node.ObjectNode)CobolControlSuite.J.readTree(Objects.requireNonNull(stream));
                var decoded=CobolControlSuite.decode(tree);
                if(!(decoded instanceof SpJsonDecoder.Decoded accepted))throw new AssertionError("fixture decode: "+decoded);
                var input=accepted.input();
                var result=new CobolLowerer().lower(input,CobolLower.POSITIVE_OPTIONS);var p=result.publication().orElseThrow();
                if(!result.validation().orElseThrow().isStructurallyValid())throw new AssertionError(result.validation());
                if(name.startsWith("ctxboom-")&&p.units().getFirst().sequences().size()>200)throw new AssertionError(name+": context expansion remains: "+p.units().getFirst().sequences().size());
                var codec=new io.github.gustavo2358.air.json.AirJson();
                if(!codec.decode(codec.encode(p)).equals(p))throw new AssertionError(name+": roundtrip");
                if(name.equals("cics-return-state")) {
                    var seqs=LocalControlOracle.sequences(p);var handlers=new TreeSet<String>();
                    for(var point:LocalControlOracle.reached(p))if(seqs.get(point.label()).terminator() instanceof Operations.Invoke i
                        &&i.target() instanceof Interactions.LiteralTarget target&&target.name().equals("AFTER"))
                        for(var next:LocalControlOracle.successors(point,seqs))handlers.addAll(LocalControlOracle.firstCalls(next,seqs));
                    if(!handlers.equals(Set.of("NEWHDLR")))throw new AssertionError("returned state selected wrong handler: "+handlers);
                    rejectMutation(result,"entry-state-return",t->t instanceof Operations.LocalInvoke i&&!i.resumeRoutes().isEmpty()
                        ?new Operations.LocalInvoke(i.header(),i.entry(),i.completionPorts(),i.resume(),i.fallback(),i.reentryGuard(),i.resumeRoutes().stream().map(route->new Operations.ResumeRoute(route.key(),i.resume())).toList()):t);
                }
                if(name.equals("cics-handler-reentry"))rejectMutation(result,"handler-keeps-frames",t->t instanceof Operations.LocalUnwind u&&u.all()?new Operations.LocalUnwind(u.header(),u.count(),u.destination(),u.fallback()):t);
                if(name.equals("escape-nested"))rejectMutation(result,"escape-loses-selected-return",t->t instanceof Operations.LocalResume r&&r.resumeKey().isPresent()?new Operations.LocalResume(r.header(),r.fallback()):t);
                if(name.equals("ctxboom-cics-04"))rejectMutation(result,"unguarded-cycle",t->t instanceof Operations.LocalInvoke i?new Operations.LocalInvoke(i.header(),i.entry(),i.completionPorts(),i.resume(),i.fallback(),Optional.empty(),i.resumeRoutes()):t);
                var permutation=tree.deepCopy();
                for(var field:List.of("occurrences","regions","boundaries","outcomes","bindings","proofs")) {
                    var values=(com.fasterxml.jackson.databind.node.ArrayNode)permutation.path("controlTopology").path(field);
                    var reversed=new ArrayList<com.fasterxml.jackson.databind.JsonNode>();values.forEach(reversed::add);Collections.reverse(reversed);values.removeAll();reversed.forEach(values::add);
                }
                var permuted=((SpJsonDecoder.Decoded)CobolControlSuite.decode(permutation)).input();
                if(!p.equals(new CobolLowerer().lower(permuted,CobolLower.POSITIVE_OPTIONS).publication().orElseThrow()))throw new AssertionError(name+": scheduling changes publication");
                if(!name.endsWith("08")) {
                    // Select the retained historical expansion without discarding newer CICS facts.
                    var old=tree.deepCopy();
                    old.path("controlTopology").path("bindings").forEach(b->((com.fasterxml.jackson.databind.node.ObjectNode)b).put("reentryPolicy","UNSPECIFIED"));
                    var historical=((SpJsonDecoder.Decoded)CobolControlSuite.decode(old)).input();
                    ControlLanguageOracle.reference(result,"compact-perform--"+name+"--published");
                    ControlLanguageOracle.reference(new CobolLowerer().lower(historical,CobolLower.POSITIVE_OPTIONS),"compact-perform--"+name+"--unspecified");
                    System.out.println("CONTEXT_EQUIVALENT "+name+" sequences="+p.units().getFirst().sequences().size());
                }
            }
        }
    }
    private static void rejectMutation(io.github.gustavo2358.lower.application.LoweringResult original,String name,java.util.function.UnaryOperator<Terminator> mutate) {
        var p=original.publication().orElseThrow();var units=p.units().stream().map(u->new Unit(u.id(),u.containingUnit(),u.objects(),u.visibleObjects(),u.entries(),
            u.sequences().stream().map(s->new Sequence(s.label(),s.instructions(),mutate.apply(s.terminator()),s.origin())).toList(),u.completionPorts(),u.body(),u.bodyUnavailable(),u.coverage(),u.origin())).toList();
        var changed=new Publication(p.id(),p.airVersion(),p.capabilities(),p.artifacts(),units,p.storage(),p.resources(),p.artifactRelations(),p.origins(),p.coverage(),p.uncertainties(),p.premises());
        var validation=io.github.gustavo2358.air.validation.AirValidator.validate(changed);
        if(!validation.isStructurallyValid())throw new AssertionError("mutation must remain structurally valid: "+name);
        var result=new io.github.gustavo2358.lower.application.LoweringResult(original.status(),original.admission(),Optional.of(changed),Optional.of(validation),original.entries(),original.statements(),original.limitations(),original.data(),original.operands());
        try {ControlLanguageOracle.equivalent(original,result,name);}
        catch(AssertionError failure) {
            if(!failure.getMessage().contains("control language differs")&&!failure.getMessage().contains("recursive shared control"))throw failure;
            return;
        }
        throw new AssertionError("oracle accepted incorrect control: "+name);
    }
    private static void checkCyclic()throws Exception {
        try(var stream=SharedRoutineSuite.class.getResourceAsStream("/sp/perform-reentry/reentry-mutual.json")) {
            var tree=(com.fasterxml.jackson.databind.node.ObjectNode)CobolControlSuite.J.readTree(Objects.requireNonNull(stream));
            var decoded=CobolControlSuite.decode(tree);
                if(!(decoded instanceof SpJsonDecoder.Decoded accepted))throw new AssertionError("fixture decode: "+decoded);
                var input=accepted.input();
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
            var decoded=CobolControlSuite.decode(tree);
                if(!(decoded instanceof SpJsonDecoder.Decoded accepted))throw new AssertionError("fixture decode: "+decoded);
                var input=accepted.input();
            var result=new CobolLowerer().lower(input,CobolLower.POSITIVE_OPTIONS);
            var p=result.publication().orElseThrow();
            if(!result.validation().orElseThrow().isStructurallyValid())throw new AssertionError(result.validation());
            var calls=p.units().getFirst().sequences().stream().map(Sequence::terminator)
                .filter(Operations.LocalInvoke.class::isInstance).map(Operations.LocalInvoke.class::cast).toList();
            if(calls.size()!=4)throw new AssertionError(fixture+": expected activation and body frames for both calls, got "+calls.size());
            calls=calls.stream().filter(call->call.reentryGuard().isEmpty()).toList();
            if(calls.size()!=2)throw new AssertionError(fixture+": two body invocations required");
            ControlLanguageOracle.reference(result,"shared-routines--"+fixture+"--published");
            if(calls.get(0).entry().equals(calls.get(1).entry())!=sameState)
                throw new AssertionError(fixture+": body identity must distinguish entry handler states");
            var reached=LocalControlOracle.reached(p);var seqs=LocalControlOracle.sequences(p);
            long resumes=reached.stream().filter(point->seqs.get(point.label()).terminator() instanceof Operations.LocalResume).count();
            if(resumes!=4)throw new AssertionError(fixture+": matched returns "+resumes);
        }
    }
}
