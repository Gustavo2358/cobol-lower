package io.github.gustavo2358.lower.adapters.testing;

import com.fasterxml.jackson.databind.node.ObjectNode;
import io.github.gustavo2358.lower.adapters.sp.SpJsonDecoder;
import io.github.gustavo2358.lower.adapters.cli.CobolLower;
import io.github.gustavo2358.lower.application.CobolLowerer;
import io.github.gustavo2358.lower.application.QualifiedSourceProjection;
import io.github.gustavo2358.lower.domain.ControlTopology;
import io.github.gustavo2358.air.model.*;
import io.github.gustavo2358.air.model.Ids.*;
import io.github.gustavo2358.air.json.AirJson;
import java.util.*;

/** Active binding identity, not a numeric depth limit or paragraph spelling. */
public final class PerformReentrySuite {
    static void need(boolean b,String m){if(!b)throw new AssertionError(m);}
    public static void main(String[] args)throws Exception {
        int cases=0,mutations=0;
        var names=List.of("reentry-direct","reentry-mutual","reentry-conditional","reentry-callers",
            "reentry-range","reentry-section","reentry-values","reentry-file","sequential-callers",
            "sequential-loop","terminal-before","halt-before","goto-before","reentry-direct-handler",
            "reentry-conditional-handler","reentry-callers-handler","sequential-callers-handler","terminal-before-handler");
        for(var name:names) {
            ObjectNode j;try(var stream=PerformReentrySuite.class.getResourceAsStream("/sp/perform-reentry/"+name+".json")) {
                j=(ObjectNode)CobolControlSuite.J.readTree(Objects.requireNonNull(stream,name));
            }
            var decoded=CobolControlSuite.decode(j);need(decoded instanceof SpJsonDecoder.Decoded,"SP2.57 decoded "+name);
            var input=((SpJsonDecoder.Decoded)decoded).input();var topology=input.controlTopology().orElseThrow();
            need(!topology.bindings().isEmpty()&&topology.bindings().stream().allMatch(b->b.reentryPolicy()==ControlTopology.ReentryPolicy.SOURCE_UNDEFINED),"typed policy transported");
            var binding=topology.bindings().getFirst();
            var legacy=new ControlTopology.Binding(binding.id(),binding.caller(),binding.region(),binding.endpoint(),binding.resume(),binding.entryPhase(),binding.completionPhase(),binding.phases(),binding.proofs());
            need(legacy.reentryPolicy()==ControlTopology.ReentryPolicy.UNSPECIFIED,"legacy typed constructor grants no recursive semantics");
            try {new ControlTopology.Binding(binding.id(),binding.caller(),binding.region(),binding.endpoint(),binding.resume(),binding.entryPhase(),binding.completionPhase(),binding.phases(),binding.proofs(),null);throw new AssertionError("null policy admitted");}catch(NullPointerException expected){}
            var result=new CobolLowerer().lower(input,CobolLower.POSITIVE_OPTIONS);var p=result.publication().orElseThrow();
            need(result.validation().orElseThrow().isStructurallyValid(),"valid AIR");new AirJson().decode(new AirJson().encode(p));
            var qualified=QualifiedSourceProjection.project(input,result.admission());
            if(name.startsWith("reentry-direct")||name.equals("reentry-mutual")) {
                var after=input.statements().stream().filter(f->f instanceof io.github.gustavo2358.lower.domain.SpInput.CallFact).findFirst().orElseThrow().header().id().handle();
                need(qualified.occurrences().stream().anyMatch(o->o.id().handle().equals(after)&&!o.qualifications().isEmpty()),"undefined reentry is no proof of impossible source continuation");
                need(qualified.proofs().stream().anyMatch(proof->proof.kind().equals("CONTROL_POSSIBILITY")&&proof.rule().equals("undefined-active-reentry-may-complete")),"conditional proof identifies source-undefined reentry");
            }
            var reached=TerminalSendSuite.reached(p);var seqs=new HashMap<LabelId,Sequence>();p.units().forEach(u->u.sequences().forEach(s->seqs.put(s.label(),s)));
            var reasons=new HashSet<UncertaintyId>();p.uncertainties().stream().filter(u->u.code().equals("cobol-lower:LOCAL_REENTRY_SOURCE_UNDEFINED")).forEach(u->reasons.add(u.id()));
            int reachedFrontiers=0;var calls=new TreeSet<String>();
            for(var s:seqs.values()) {
                var t=s.terminator();if(!reached.contains(s.label()))continue;
                if(t instanceof Operations.Invoke v&&v.target() instanceof Interactions.LiteralTarget lit)calls.add(lit.name());
                if(t.header().uncertainties().stream().anyMatch(reasons::contains)) {
                    reachedFrontiers++;need(t instanceof Operations.Opaque,"reentry is an open opaque control frontier");
                    var envelope=((Operations.Opaque)t).envelope();
                    need(envelope.memory().mustOverwrite().isEmpty()&&!(envelope.memory().otherReads() instanceof Scopes.NoMemory)&&!(envelope.memory().otherWrites() instanceof Scopes.NoMemory),"undefined source cannot close memory or prove kills");
                    need(envelope.dependencies().remainder() instanceof Scopes.AnyResource,"undefined source cannot close resource effects");
                    var control=envelope.control();need(control.known().isEmpty()&&!(control.remainder() instanceof Scopes.NoControl),"no return, halt or closed empty control invented");
                }
            }
            need(name.startsWith("reentry-")?reachedFrontiers>0:reachedFrontiers==0,"active reentry distinct from sequential/terminated contexts "+name);
            need(calls.stream().noneMatch(c->c.startsWith("DEAD")),"terminal and section boundaries retained "+name);
            if(name.startsWith("reentry-direct")||name.equals("reentry-mutual"))need(!calls.contains("AFTERP"),"unconditional recursion gains no executable return");
            if(name.startsWith("reentry-conditional")||name.startsWith("reentry-range")||name.startsWith("reentry-section"))need(calls.contains("AFTERP"),"non-recursive branch still returns");
            if(name.startsWith("sequential-callers")) {
                need(calls.containsAll(Set.of("FIRST","SECOND","INP")),"sequential invocations both return");
                var resumes=new ArrayList<Set<String>>();
                for(var s:seqs.values())if(reached.contains(s.label())&&s.terminator() instanceof Operations.Invoke v&&v.target() instanceof Interactions.LiteralTarget lit&&lit.name().equals("INP"))
                    for(var outcome:v.outcomes().known())if(outcome instanceof Control.Normal normal)resumes.add(CobolControlSuite.firstCalls(normal.label(),seqs));
                need(resumes.size()==2&&resumes.contains(Set.of("FIRST"))&&resumes.contains(Set.of("SECOND")),"no crossed caller continuation "+name+resumes);
            }
            for(var mode:List.of("missing","null","invalid","downgrade","future")) {
                var changed=j.deepCopy();var b=(ObjectNode)changed.path("controlTopology").path("bindings").get(0);
                switch(mode){case "missing"->b.remove("reentryPolicy");case "null"->b.putNull("reentryPolicy");case "invalid"->b.put("reentryPolicy","RECURSION_IS_FINE");case "downgrade"->changed.put("contractVersion","2.56.0");case "future"->changed.put("contractVersion","2.58.0");}
                need(CobolControlSuite.decode(changed) instanceof SpJsonDecoder.Rejected,"reject invalid contract "+mode);mutations++;
            }
            var old=j.deepCopy();old.put("contractVersion","2.56.0");old.path("controlTopology").path("bindings").forEach(b->((ObjectNode)b).remove("reentryPolicy"));
            var historic=(SpJsonDecoder.Decoded)CobolControlSuite.decode(old);need(historic.input().controlTopology().orElseThrow().bindings().stream().allMatch(b->b.reentryPolicy()==ControlTopology.ReentryPolicy.UNSPECIFIED),"historical missing policy stays unspecified");
            var historical=new CobolLowerer().lower(historic.input(),CobolLower.POSITIVE_OPTIONS).publication().orElseThrow();
            need(historical.uncertainties().stream().noneMatch(u->u.code().equals("cobol-lower:LOCAL_REENTRY_SOURCE_UNDEFINED")),"no source policy guessed for historical input");
            if(name.startsWith("reentry-"))need(historical.uncertainties().stream().anyMatch(u->u.code().equals("cobol-lower:TOPOLOGY_RECURSIVE_ACTIVATION_UNAVAILABLE")),"historical limitation remains explicit");
            cases++;
        }
        System.out.println("PASS PerformReentrySuite cases="+cases+" mutations="+mutations);
    }
}
