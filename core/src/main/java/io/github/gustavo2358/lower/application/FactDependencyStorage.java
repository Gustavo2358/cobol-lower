package io.github.gustavo2358.lower.application;

import io.github.gustavo2358.air.model.*;
import io.github.gustavo2358.air.model.Ids.*;
import io.github.gustavo2358.lower.domain.*;
import java.util.*;

/** AIR translation of published cells/bounds; does not inspect declaration hierarchy. */
final class FactDependencyStorage {
    private FactDependencyStorage() { }
    static Map<String,StorageId> cells(FactDependencyIndex facts,RegionalStorageAdmission.Index source,
            ScalarDataTranslator.Result legacy,List<Memory.Storage> storage,UnitId unit,LocalIds ids,SourceOrigins origins) {
        if(facts==null)return Map.of();
        var result=new TreeMap<String,StorageId>();var nodes=new HashMap<String,StorageFacts.Node>();source.nodes().values().forEach(n->nodes.put(n.id().handle(),n));
        var required=new TreeSet<String>();facts.bindings.values().forEach(b->required.addAll(b.cells()));
        for(var node:required) {
            var n=nodes.get(node);var data=n.data().map(legacy.index()::get).orElse(null);
            if(data!=null&&data.storage().isPresent()){result.put(node,data.storage().orElseThrow());continue;}
            var id=new StorageId(unit.publication(),ids.id("storage","r2-logical-cell",unit.localId(),node));
            var origin=proofOrigin(facts,facts.cells.get(node).dependencies(),"cell/"+node,unit,ids,origins);
            storage.add(new Memory.Cell(new Memory.StorageHeader(id,Optional.of(unit),Memory.Lifetime.PERSISTENT,Memory.Visibility.PRIVATE,origin),Types.known(Types.Builtin.TEXT)));
            result.put(node,id);
        }
        return Map.copyOf(result);
    }
    static OriginId proofOrigin(FactDependencyIndex facts,List<String> dependencies,String key,UnitId unit,LocalIds ids,SourceOrigins origins) {
        var pending=new ArrayDeque<>(dependencies);var seen=new TreeSet<String>();
        while(!pending.isEmpty()){var id=pending.remove();if(seen.add(id))pending.addAll(facts.proofs.get(id).dependencies());}
        var inputs=new ArrayList<OriginId>();for(var id:seen)inputs.add(origins.source("r2-proof",id,facts.proofs.get(id).provenance()));
        return origins.derived(ids.id("origin","r2-fact-dependencies",unit.localId(),key),inputs,"sp2.40/frontend-fact-dependency-authority");
    }
    static Memory.Binding binding(FactDependencies.Binding proof,Map<String,StorageId> cells,
            Map<StorageFacts.BaseId,StorageId> regions,SpInput.UnitKey source,UncertaintyId reason) {
        if(!proof.exactCell().isEmpty())return new Memory.CellBinding(Objects.requireNonNull(cells.get(proof.exactCell())));
        var ids=new LinkedHashSet<StorageId>();for(var cell:proof.cells())ids.add(Objects.requireNonNull(cells.get(cell)));
        for(var region:proof.regions())ids.add(Objects.requireNonNull(regions.get(new StorageFacts.BaseId(source,region))));
        if(ids.isEmpty())throw new IllegalArgumentException("available R2 binding must have a grounded published bound");
        return new Memory.UnknownBinding(new Scopes.StorageMemory(List.copyOf(ids)),reason);
    }
}
