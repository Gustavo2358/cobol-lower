package io.github.gustavo2358.lower.adapters.testing;

import io.github.gustavo2358.air.json.AirJson;
import io.github.gustavo2358.air.model.*;
import io.github.gustavo2358.air.model.Ids.*;
import io.github.gustavo2358.lower.adapters.cli.CobolLower;
import io.github.gustavo2358.lower.adapters.sp.SpJsonDecoder;
import io.github.gustavo2358.lower.application.*;
import io.github.gustavo2358.lower.domain.SpInput;
import io.github.gustavo2358.lower.testing.IfInputs;
import java.util.*;
import static io.github.gustavo2358.lower.testing.CallOracle.check;

/** Cross-handler identity, repetition/body composition, cold DAG inventory and deep contexts. */
public final class CompositionalPerformRevisionSuite {
    static SpInput input(String name) throws Exception {
        try(var stream=CompositionalPerformRevisionSuite.class.getResourceAsStream("/sp/compositional-perform-r1/"+name+".json")) {
            var decoded=new SpJsonDecoder(CobolLower.INPUT_LIMITS).decode(Objects.requireNonNull(stream,name).readAllBytes());
            check(decoded instanceof SpJsonDecoder.Decoded,"source-produced R1 SP decodes "+name);
            return ((SpJsonDecoder.Decoded)decoded).input();
        }
    }
    static Set<LabelId> reachable(Unit unit,Set<LabelId> stop) {
        var index=new HashMap<LabelId,Sequence>();unit.sequences().forEach(s->index.put(s.label(),s));
        var seen=new HashSet<LabelId>();var todo=new ArrayDeque<LabelId>();todo.add(unit.entries().getFirst().initialLabel().orElseThrow());
        while(!todo.isEmpty()) {var id=todo.removeFirst();if(seen.add(id)&&!stop.contains(id))todo.addAll(CompositionalPerformSuite.successors(index.get(id).terminator()));}
        return seen;
    }
    public static void main(String[] args) throws Exception {
        var names=new ArrayList<String>();
        for(int n:List.of(1,2))for(var shape:List.of("computed","literal","options","options-regional","file"))names.add("F01-"+shape+"-"+n);
        names.add("F01-if-read");
        for(var role:List.of("inner","outer"))for(var rep:List.of("times","times-item","until-before","until-after","varying-before","varying-after"))names.add("F02-"+role+"-"+rep);
        names.addAll(List.of("F02-times-partial","F02-unavailable-count","F03-dag-cold-008","F03-dag-live-004","F03-chain-live-064"));
        for(var name:names) {
            var input=input(name);var result=new CobolLowerer().lower(input,CobolLower.OPTIONS);
            check(result.publication().isPresent(),"R1 AIR valid "+name+" "+result.status()+" "+result.validation());
            var pub=result.publication().orElseThrow();var unit=pub.units().getFirst();var reach=reachable(unit,Set.of());
            var codec=new AirJson();var bytes=codec.encode(pub);check(pub.equals(codec.decode(bytes)),"R1 AIR round-trip "+name);
            var reversed=new ArrayList<>(input.statements());Collections.reverse(reversed);
            check(Arrays.equals(bytes,codec.encode(new CobolLowerer().lower(IfInputs.with(input,"statements",reversed),CobolLower.OPTIONS).publication().orElseThrow())),"R1 physical inventory permutation "+name);
            if(name.startsWith("F01-")&&!name.equals("F01-if-read")) {
                int n=Integer.parseInt(name.substring(name.length()-1));
                var calls=unit.sequences().stream().filter(s->s.terminator() instanceof Operations.Invoke).toList();
                check(calls.size()==n+1,"ordinary + each activated CICS inventory "+name);
                check(calls.stream().filter(s->reach.contains(s.label())).count()==n,"only activated CICS reachable "+name);
                var cicsOperands=new HashSet<SpInput.OperandId>();
                for(var fact:input.statements()) {
                    if(fact instanceof SpInput.CicsFact c) {
                        c.target().ifPresent(t->cicsOperands.add(t.id()));
                        c.options().forEach(o->o.reference().ifPresent(r->cicsOperands.add(r.id())));
                    } else if(fact instanceof SpInput.CicsFileFact c) {
                        c.target().ifPresent(t->cicsOperands.add(t.id()));
                        c.options().forEach(o->o.reference().ifPresent(r->cicsOperands.add(r.id())));
                    }
                }
                var grouped=new HashMap<SpInput.OperandId,List<LoweringResult.OperandLink>>();
                result.operands().stream().filter(l->cicsOperands.contains(l.source()))
                    .forEach(l->grouped.computeIfAbsent(l.source(),x->new ArrayList<>()).add(l));
                if(name.contains("computed")||name.contains("file")||name.contains("options-regional")) {
                    check(!grouped.isEmpty(),"referenced operands actually materialized "+name);
                    for(var links:grouped.values()) {
                        check(links.size()>=n+1 && links.stream().map(LoweringResult.OperandLink::target).distinct().count()==links.size(),"distinct occurrence outputs, no deduplication "+name);
                        check(links.stream().map(LoweringResult.OperandLink::origin).distinct().count()==1,"same written source origin "+name);
                    }
                }
            }
            if(name.startsWith("F02-")) {
                var calls=unit.sequences().stream().filter(s->s.terminator() instanceof Operations.Invoke).toList();
                check(calls.size()==1,"one source CALL "+name);var call=calls.getFirst();
                check(reach.contains(call.label())!=name.equals("F02-unavailable-count"),"supported repetition returns; unavailable repetition has no bypass "+name);
                if(!name.equals("F02-unavailable-count")) {
                    var members=new HashSet<SpInput.StatementId>();
                    input.statements().stream().filter(SpInput.ProcedurePerformFact.class::isInstance).map(SpInput.ProcedurePerformFact.class::cast)
                        .forEach(p->p.procedures().forEach(r->members.addAll(r.statements())));
                    var moves=new HashSet<SpInput.StatementId>();input.statements().stream().filter(SpInput.MoveFact.class::isInstance)
                        .map(s->s.header().id()).filter(members::contains).forEach(moves::add);
                    var blocked=new HashSet<LabelId>();result.statements().stream().filter(l->moves.contains(l.source())).forEach(l->blocked.add(l.label()));
                    boolean zeroPath=name.endsWith("before")||name.endsWith("times-item");
                    check(reachable(unit,blocked).contains(call.label())==zeroPath,"zero/body guarantee independent of body precision "+name);
                }
            }
            if(name.startsWith("F03-")) {
                var resumes=pub.origins().stream().filter(o->o instanceof Origins.Derived d && d.rule().equals("perform-structural@1/conditional-activation-resume")).count();
                if(name.contains("cold")) {
                    check(resumes==0 && unit.sequences().size()==input.statements().size(),"cold DAG keeps linear ordinary inventory without activation tree");
                    var sources=new HashSet<SpInput.StatementId>();result.statements().forEach(l->sources.add(l.source()));
                    check(input.statements().stream().allMatch(s->sources.contains(s.header().id())),"cold source inventory never discarded");
                } else check(resumes==(name.contains("chain")?65:31),"only demanded contexts emitted "+name);
            }
        }
        System.out.println("COMPOSITIONAL_PERFORM_R1=PASS");
    }
}
