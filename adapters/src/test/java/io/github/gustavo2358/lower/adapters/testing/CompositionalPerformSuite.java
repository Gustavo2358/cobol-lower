package io.github.gustavo2358.lower.adapters.testing;

import io.github.gustavo2358.air.model.*;
import io.github.gustavo2358.air.model.Ids.*;
import io.github.gustavo2358.lower.adapters.cli.CobolLower;
import io.github.gustavo2358.lower.adapters.sp.SpJsonDecoder;
import io.github.gustavo2358.lower.application.*;
import io.github.gustavo2358.lower.domain.SpInput;
import java.util.*;
import io.github.gustavo2358.air.json.AirJson;
import io.github.gustavo2358.lower.testing.IfInputs;
import static io.github.gustavo2358.lower.testing.CallOracle.check;

/** Source-produced W5 snapshots; assertions concern paths, not physical sequence order. */
public final class CompositionalPerformSuite {
    static SpInput input(String name) throws Exception {
        try(var stream=CompositionalPerformSuite.class.getResourceAsStream("/sp/compositional-perform/"+name+".json")) {
            var decoded=new SpJsonDecoder(CobolLower.INPUT_LIMITS).decode(Objects.requireNonNull(stream).readAllBytes());
            check(decoded instanceof SpJsonDecoder.Decoded,"source SP decodes "+name);
            return ((SpJsonDecoder.Decoded)decoded).input();
        }
    }
    static List<LabelId> successors(Terminator t) {
        if(t instanceof Operations.Jump j)return List.of(j.destination());
        if(t instanceof Operations.Branch b)return List.of(b.trueDestination(),b.falseDestination());
        if(t instanceof Operations.Invoke i)return i.outcomes().known().stream().filter(Control.Normal.class::isInstance).map(Control.Normal.class::cast).map(Control.Normal::label).toList();
        if(t instanceof Operations.Opaque o)return o.envelope().control().known().stream().filter(Control.JumpAlternative.class::isInstance).map(Control.JumpAlternative.class::cast).map(Control.JumpAlternative::label).toList();
        return List.of();
    }
    static Set<LabelId> reachable(Unit unit) {
        return LocalControlOracle.reached(unit,Set.of()).stream().map(LocalControlOracle.Point::label).collect(java.util.stream.Collectors.toSet());
    }
    public static void main(String[] args) throws Exception {
        for(var name:List.of("06","08","09","11","24","25","26","33","34","36","39")) {
            var input=input(name);var result=new CobolLowerer().lower(input,CobolLower.OPTIONS);
            check(result.publication().isPresent(),"valid AIR "+name+" "+result.status()+" "+result.validation());
            ControlLanguageOracle.reference(result,"compositional-perform--"+name+"--published");
            var unit=result.publication().orElseThrow().units().getFirst();var reached=reachable(unit);
            var calls=unit.sequences().stream().filter(s->reached.contains(s.label())&&s.terminator() instanceof Operations.Invoke).toList();
            check(calls.size()==(name.equals("08")||name.equals("09")||name.equals("33")||name.equals("34")?2:1),"reachable CALL occurrences "+name+": "+calls.size());
            if(name.equals("06")) {
                var call=(Operations.Invoke)calls.getFirst().terminator();check(successors(call).size()==1,"conditional normal CALL completion");
                var resume=unit.sequences().stream().filter(s->s.label().equals(successors(call).getFirst())).findFirst().orElseThrow();
                check(resume.terminator() instanceof Operations.LocalResume,"explicit activation resume frame");
            }
        }
        for(var name:List.of("multiplicity-1","multiplicity-2","multiplicity-5","multiplicity-40",
                "nested-depth-1","nested-depth-2","nested-depth-3","nested-contexts",
                "completion-move","completion-exit","completion-continue","completion-exit-paragraph",
                "completion-exit-perform","completion-goback","peer-false","peer-true","dead-body","recursion")) {
            var input=input(name);var result=new CobolLowerer().lower(input,CobolLower.OPTIONS);
            check(result.publication().isPresent(),"metamorphic valid AIR "+name+" "+result.admission().diagnostics());
            ControlLanguageOracle.reference(result,"compositional-perform--"+name+"--published");
            var pub=result.publication().orElseThrow();var unit=pub.units().getFirst();var reach=reachable(unit);
            var seqs=new HashMap<LabelId,Sequence>();unit.sequences().forEach(q->seqs.put(q.label(),q));
            long calls=LocalControlOracle.reached(unit,Set.of()).stream().filter(point->seqs.get(point.label()).terminator() instanceof Operations.Invoke).count();
            int expected=name.startsWith("multiplicity-")?Integer.parseInt(name.substring("multiplicity-".length()))
                :name.equals("nested-contexts")?2:name.equals("completion-exit-paragraph")||name.equals("completion-exit-perform")||name.equals("completion-goback")?0:name.equals("recursion")?2:1;
            check(calls==expected,"M1-M7 CALL reachability "+name+": "+calls);
            var codec=new AirJson();var wire=codec.encode(pub);
            check(pub.equals(codec.decode(wire)),"AIR round-trip "+name);
            var reverse=new ArrayList<>(input.statements());Collections.reverse(reverse);
            check(Arrays.equals(wire,codec.encode(new CobolLowerer().lower(IfInputs.with(input,"statements",reverse),CobolLower.OPTIONS).publication().orElseThrow())),"physical permutation invariance "+name);
            if(name.startsWith("multiplicity-")||name.equals("peer-true"))assertReturns(input,result);
            if(name.equals("nested-depth-3")) {
                var points=LocalControlOracle.reached(unit,Set.of());
                long frames=points.stream().filter(point->seqs.get(point.label()).terminator() instanceof Operations.LocalResume).count();
                check(frames==3,"M2 inner completion pops all three activation frames");
            }
            if(name.equals("recursion")) {
                check(pub.uncertainties().stream().anyMatch(x->x.code().contains("RECURSIVE_PERFORM_NOT_SUPPORTED")),"recursion is an explicit gap");
                check(unit.sequences().size()<30,"recursive graph has a finite explicit partial boundary");
            }
        }
        var input=input("06");
        var facts=input.statements().stream().map(x->x instanceof SpInput.ProcedurePerformFact p?IfInputs.with(p,"procedures",List.of()):x).toList();
        var entryOnly=new CobolLowerer().lower(IfInputs.with(input,"statements",facts),CobolLower.OPTIONS);
        check(entryOnly.publication().isPresent(),"entry without membership validates");
        var unit=entryOnly.publication().orElseThrow().units().getFirst();var reached=reachable(unit);
        check(unit.sequences().stream().anyMatch(x->reached.contains(x.label())&&x.terminator() instanceof Operations.Invoke),"entry-only CALL remains observable BEFORE");
        check(unit.sequences().stream().noneMatch(x->reached.contains(x.label())&&x.terminator() instanceof Operations.Return),"entry alone fabricates no return");
        System.out.println("COMPOSITIONAL_PERFORM=PASS");
    }
    private static void assertReturns(SpInput input,LoweringResult result) {
        var pub=result.publication().orElseThrow();var seqs=LocalControlOracle.sequences(pub);
        var source=new HashMap<OperationId,SpInput.StatementId>();result.statements().forEach(l->source.put(l.target(),l.source()));
        var labels=new HashMap<LabelId,SpInput.StatementId>();result.statements().forEach(l->labels.put(l.label(),l.source()));
        for(var fact:input.statements())if(fact instanceof SpInput.ProcedurePerformFact perform&&perform.loop().isEmpty()&&perform.times().isEmpty()) {
            var calls=LocalControlOracle.reached(pub).stream().filter(point->seqs.get(point.label()).terminator() instanceof Operations.LocalInvoke
                &&perform.header().id().equals(source.get(seqs.get(point.label()).terminator().header().id()))).toList();
            check(calls.size()==1,"one written activation entry "+perform.header().id());
            var invoke=(Operations.LocalInvoke)seqs.get(calls.getFirst().label()).terminator();
            check(perform.normalContinuation().statement().orElseThrow().equals(labels.get(invoke.resume())),"M1 exact callsite-specific resume");
            var returns=LocalControlOracle.reached(pub).stream().filter(point->seqs.get(point.label()).terminator() instanceof Operations.LocalResume
                &&!point.stack().isEmpty()&&point.stack().getLast().header().id().equals(invoke.header().id())).toList();
            check(!returns.isEmpty(),"normal completion actually returns to this caller");
            for(var point:returns)check(LocalControlOracle.successors(point,seqs).stream().allMatch(next->next.label().equals(invoke.resume())),"no crossed caller return");
        }
    }

}
