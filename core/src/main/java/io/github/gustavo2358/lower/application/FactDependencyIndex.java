package io.github.gustavo2358.lower.application;

import io.github.gustavo2358.lower.domain.*;
import java.util.*;

/** Evaluate the published proof DAG once. No COBOL interpretation or source access. */
final class FactDependencyIndex {
    final FactDependencies graph;
    final Map<String,Boolean> known;
    final Map<String,FactDependencies.Binding> bindings;
    final Map<String,FactDependencies.Fact> cells;
    final Set<String> allocated;
    final Map<String,FactDependencies.Fact> allocations;
    final Map<String,FactDependencies.Proof> proofs;
    FactDependencyIndex(FactDependencies graph) {
        this.graph=graph;known=graph.proofAvailability();
        var bs=new HashMap<String,FactDependencies.Binding>();graph.bindings().forEach(b->bs.put(b.node(),b));bindings=Map.copyOf(bs);
        var cs=new HashMap<String,FactDependencies.Fact>();var as=new HashSet<String>();var af=new HashMap<String,FactDependencies.Fact>();
        for(var f:graph.facts())if(available(f.dependencies())) {
            if(f.kind()==FactDependencies.FactKind.LOCAL_CELL)cs.put(f.subject(),f);
            if(f.kind()==FactDependencies.FactKind.STORAGE_IDENTITY){as.add(f.subject());af.put(f.subject(),f);}
        }
        cells=Map.copyOf(cs);allocated=Set.copyOf(as);allocations=Map.copyOf(af);
        var ps=new HashMap<String,FactDependencies.Proof>();graph.proofs().forEach(p->ps.put(p.id(),p));proofs=Map.copyOf(ps);
    }
    boolean available(List<String> dependencies){return dependencies.stream().allMatch(d->Boolean.TRUE.equals(known.get(d)));}
    boolean materializable(String node){var b=bindings.get(node);return b!=null&&available(b.dependencies());}
}
