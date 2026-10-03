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
    /** Frozen finite-language reference captured from a separately qualified lower commit. */
    static com.fasterxml.jackson.databind.node.ArrayNode snapshot(LoweringResult result) {
        var machine=Machine.of(result);var ids=new LinkedHashMap<Set<LocalControlOracle.Point>,Integer>();
        var pending=new ArrayDeque<Set<LocalControlOracle.Point>>();ids.put(machine.roots(),0);pending.add(machine.roots());
        var rows=CobolControlSuite.J.createArrayNode();
        while(!pending.isEmpty()) {
            var row=rows.addObject();
            for(var edge:machine.transitions(pending.removeFirst()).entrySet()) {
                var to=edge.getValue();if(!ids.containsKey(to)){ids.put(to,ids.size());pending.addLast(to);}
                row.put(edge.getKey(),ids.get(to));
            }
            if(ids.size()>200_000)throw new AssertionError("reference resource bound exceeded; no snapshot verdict");
        }
        return rows;
    }
    static void reference(LoweringResult actual,String resource)throws java.io.IOException {
        com.fasterxml.jackson.databind.JsonNode rows;
        try(var stream=ControlLanguageOracle.class.getResourceAsStream("/control-reference/"+resource+".json")) {
            var reference=CobolControlSuite.J.readTree(Objects.requireNonNull(stream,resource));
            if(!reference.path("referenceCommit").asText().equals("cb1b87250f133535ab26a3056be0d6da144d81bf"))
                throw new AssertionError("unqualified control reference "+resource);
            try(var fixture=ControlLanguageOracle.class.getResourceAsStream("/sp/"+reference.path("fixture").asText())) {
                var digest=java.security.MessageDigest.getInstance("SHA-256").digest(Objects.requireNonNull(fixture,resource).readAllBytes());
                if(!java.util.HexFormat.of().formatHex(digest).equals(reference.path("sourceSha256").asText()))
                    throw new AssertionError("source fixture differs from frozen reference "+resource);
            } catch(java.security.NoSuchAlgorithmException error) { throw new AssertionError(error); }
            rows=reference.path("states");
        }
        if(!rows.isArray()||rows.isEmpty())throw new AssertionError("invalid frozen reference "+resource);
        for(var row:rows) {
            if(!row.isObject())throw new AssertionError("invalid reference state "+resource);
            row.elements().forEachRemaining(to->{if(!to.isIntegralNumber()||to.intValue()<0||to.intValue()>=rows.size())
                throw new AssertionError("invalid reference transition "+resource);});
        }
        var machine=Machine.of(actual);
        record Pair(Set<LocalControlOracle.Point> points,int state) { }
        var pending=new ArrayDeque<Pair>();var seen=new HashSet<Pair>();pending.add(new Pair(machine.roots(),0));
        while(!pending.isEmpty()) {
            var pair=pending.removeFirst();if(!seen.add(pair))continue;
            if(seen.size()>200_000)throw new AssertionError("reference pair bound exceeded; no equivalence verdict");
            var next=machine.transitions(pair.points());var expected=rows.get(pair.state());var keys=new TreeSet<String>();expected.fieldNames().forEachRemaining(keys::add);
            if(!next.keySet().equals(keys))throw new AssertionError(resource+" control language differs: "+next.keySet()+" vs "+keys);
            next.forEach((symbol,points)->pending.addLast(new Pair(points,expected.path(symbol).intValue())));
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
