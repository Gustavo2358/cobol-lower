package io.github.gustavo2358.lower.domain;
import java.util.*;
/** Closed references only; no language/storage inference. */
public final class FactDependencyContract {
    private FactDependencyContract() { }
    public static void validate(FactDependencies graph,StorageFacts.Inventory storage) {
        var nodes=new HashSet<String>();for(var n:storage.nodes())nodes.add(n.id().handle());
        var bases=new HashSet<String>();for(var b:storage.bases())bases.add(b.id().handle());
        var gs=new HashSet<String>();var ns=new HashSet<String>();for(var r:graph.regions()){gs.add(r.id());ns.addAll(r.members());}
        if(!gs.equals(bases)||!ns.equals(nodes))throw new IllegalArgumentException("fact graph inventory equals storage inventory");
        var owner=new HashMap<String,String>();for(var r:graph.regions())for(var n:r.members())owner.put(n,r.id());
        for(var v:storage.views())if(!Objects.equals(owner.get(v.node().handle()),v.base().handle()))throw new IllegalArgumentException("fact graph region agrees with nominal storage component");
    }
}
