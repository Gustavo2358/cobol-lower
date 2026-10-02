package io.github.gustavo2358.lower.adapters.testing;

import io.github.gustavo2358.air.model.*;
import io.github.gustavo2358.air.model.Ids.LabelId;
import java.util.*;

/** Small independent AIR interpreter for test reachability; never consumes lowering internals or CFG. */
final class LocalControlOracle {
    record Point(LabelId label,List<Operations.LocalInvoke> stack) {
        Point{stack=List.copyOf(stack);}
    }
    private LocalControlOracle() { }
    static List<Point> successors(Point at,Map<LabelId,Sequence> seqs) {
        return successors(at,seqs,false);
    }
    static List<Point> successors(Point at,Map<LabelId,Sequence> seqs,boolean open) {
        var t=Objects.requireNonNull(seqs.get(at.label())).terminator();var stack=at.stack();
        if(t instanceof Operations.LocalInvoke i) {
            if(i.reentryGuard().isPresent()) {
                var guard=i.reentryGuard().orElseThrow();
                for(var frame:stack)if(frame.header().id().unit().equals(i.header().id().unit())
                        &&frame.reentryGuard().map(g->g.activationKey().equals(guard.activationKey())).orElse(false))
                    return List.of(new Point(guard.destination(),stack));
            }
            if(stack.stream().anyMatch(frame->frame.header().id().equals(i.header().id())))throw new AssertionError("producer emitted recursive shared control");
            var pushed=new ArrayList<>(stack);pushed.add(i);return List.of(new Point(i.entry(),pushed));
        }
        if(t instanceof Operations.LocalResume)return stack.isEmpty()?List.of():List.of(new Point(stack.getLast().resume(),stack.subList(0,stack.size()-1)));
        if(t instanceof Operations.LocalBoundary b) {
            if(!stack.isEmpty()&&stack.getLast().completionPorts().contains(b.port()))return List.of(new Point(stack.getLast().resume(),stack.subList(0,stack.size()-1)));
            return List.of(new Point(b.defaultDestination(),stack));
        }
        if(t instanceof Operations.LocalUnwind u)return u.count().compareTo(java.math.BigInteger.valueOf(stack.size()))>0?List.of():List.of(new Point(u.destination(),stack.subList(0,stack.size()-u.count().intValueExact())));
        var targets=new ArrayDeque<LabelId>();
        if(t instanceof Operations.Jump j)targets.add(j.destination());
        else if(t instanceof Operations.Branch b){targets.add(b.trueDestination());targets.add(b.falseDestination());}
        else if(t instanceof Operations.Invoke i)TerminalSendSuite.knownDestinations(i.outcomes().known(),targets);
        else if(t instanceof Operations.Opaque o)TerminalSendSuite.knownDestinations(o.envelope().control().known(),targets);
        if(open) {
            var bound=t instanceof Operations.Invoke i?i.outcomes().remainder():t instanceof Operations.Opaque o?o.envelope().control().remainder():Scopes.NoControl.INSTANCE;
            if(bound instanceof Scopes.WithinControl within)finiteLocal(within.scope(),targets);
        }
        return targets.stream().distinct().map(l->new Point(l,stack)).toList();
    }
    private static void finiteLocal(Scopes.ControlScope scope,Deque<LabelId> targets) {
        if(scope instanceof Scopes.LabelsControl labels)targets.addAll(labels.labels());
        else if(scope instanceof Scopes.ControlUnion union)union.members().forEach(s->finiteLocal(s,targets));
        else if(!(scope instanceof Scopes.UnitControl unit&&!unit.labels()))throw new AssertionError("test requires finite local control");
    }
    static Map<LabelId,Sequence> sequences(Publication p){var seqs=new HashMap<LabelId,Sequence>();p.units().forEach(u->u.sequences().forEach(s->seqs.put(s.label(),s)));return seqs;}
    static Set<Point> reached(Publication p) {return reached(p,false);}
    static Set<Point> reached(Publication p,boolean open) {
        var seqs=sequences(p);var seen=new HashSet<Point>();var work=new ArrayDeque<Point>();
        p.units().forEach(u->u.entries().forEach(e->e.initialLabel().ifPresent(l->work.add(new Point(l,List.of())))));
        while(!work.isEmpty()){var point=work.removeFirst();if(seen.add(point))work.addAll(successors(point,seqs,open));}
        return Set.copyOf(seen);
    }
    static Set<String> firstCalls(Point start,Map<LabelId,Sequence> seqs) {
        var calls=new TreeSet<String>();var seen=new HashSet<Point>();var work=new ArrayDeque<Point>();work.add(start);
        while(!work.isEmpty()) {
            var point=work.removeFirst();if(!seen.add(point))continue;
            if(seqs.get(point.label()).terminator() instanceof Operations.Invoke i) {
                if(i.target() instanceof Interactions.LiteralTarget target)calls.add(target.name());
            } else work.addAll(successors(point,seqs));
        }
        return Set.copyOf(calls);
    }
}
