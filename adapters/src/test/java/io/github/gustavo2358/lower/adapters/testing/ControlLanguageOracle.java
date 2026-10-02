package io.github.gustavo2358.lower.adapters.testing;

import io.github.gustavo2358.air.model.*;
import io.github.gustavo2358.air.model.Ids.*;
import io.github.gustavo2358.lower.application.LoweringResult;
import java.util.*;

/** Exact finite control-language comparison after hiding administrative jump/frame steps.
 * This oracle checks source occurrence ordering and operation kinds, not value semantics. */
final class ControlLanguageOracle {
    private ControlLanguageOracle() { }
    private record Machine(Map<LabelId,Sequence> sequences,Map<OperationId,String> sources,Set<LocalControlOracle.Point> roots) {
        static Machine of(LoweringResult result) {
            var p=result.publication().orElseThrow();var sources=new HashMap<OperationId,String>();
            result.statements().forEach(l->sources.put(l.target(),l.source().handle()));
            var roots=new HashSet<LocalControlOracle.Point>();
            p.units().forEach(u->u.entries().forEach(e->e.initialLabel().ifPresent(l->roots.add(new LocalControlOracle.Point(l,List.of())))));
            return new Machine(LocalControlOracle.sequences(p),sources,Set.copyOf(roots));
        }
        String symbol(LocalControlOracle.Point point) {
            var s=sequences.get(point.label());var operations=new ArrayList<Operation>(s.instructions());
            var t=s.terminator();
            if(!(t instanceof Operations.Jump||t instanceof Operations.LocalInvoke||t instanceof Operations.LocalResume
                    ||t instanceof Operations.LocalBoundary||t instanceof Operations.LocalUnwind))operations.add(t);
            return operations.stream().map(op->sources.getOrDefault(op.header().id(),"synthetic")+":"+op.kind()
                +(op instanceof Operations.Invoke i&&i.target() instanceof Interactions.LiteralTarget l?":"+l.name():""))
                .collect(java.util.stream.Collectors.joining("/"));
        }
        Map<String,Set<LocalControlOracle.Point>> transitions(Set<LocalControlOracle.Point> from) {
            var result=new TreeMap<String,Set<LocalControlOracle.Point>>();var seen=new HashSet<LocalControlOracle.Point>();
            var todo=new ArrayDeque<>(from);
            while(!todo.isEmpty()) {
                var at=todo.removeFirst();if(!seen.add(at))continue;
                if(seen.size()>200_000)throw new AssertionError("oracle resource bound exceeded; no equivalence verdict");
                var symbol=symbol(at);var next=LocalControlOracle.successors(at,sequences);
                if(symbol.isEmpty())todo.addAll(next);
                else result.computeIfAbsent(symbol,k->new HashSet<>()).addAll(next);
            }
            result.replaceAll((k,v)->Set.copyOf(v));return result;
        }
    }
    static void equivalent(LoweringResult left,LoweringResult right,String name) {
        var a=Machine.of(left);var b=Machine.of(right);
        record Pair(Set<LocalControlOracle.Point> a,Set<LocalControlOracle.Point> b) { }
        var work=new ArrayDeque<Pair>();var seen=new HashSet<Pair>();work.add(new Pair(a.roots(),b.roots()));
        while(!work.isEmpty()) {
            var pair=work.removeFirst();if(!seen.add(pair))continue;
            if(seen.size()>200_000)throw new AssertionError("oracle pair bound exceeded; no equivalence verdict");
            var x=a.transitions(pair.a());var y=b.transitions(pair.b());
            if(!x.keySet().equals(y.keySet()))throw new AssertionError(name+" control language differs: "+x.keySet()+" vs "+y.keySet());
            x.forEach((symbol,next)->work.addLast(new Pair(next,y.get(symbol))));
        }
    }
}
