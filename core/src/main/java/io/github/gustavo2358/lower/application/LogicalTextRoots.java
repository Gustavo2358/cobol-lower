package io.github.gustavo2358.lower.application;

import io.github.gustavo2358.air.model.*;
import io.github.gustavo2358.air.model.Ids.*;
import io.github.gustavo2358.lower.domain.StorageFacts;
import java.util.*;

/** Internal character-family cells follow structural identity, not COBOL names. */
final class LogicalTextRoots {
    private LogicalTextRoots() { }

    static Map<StorageFacts.NodeId,ObjectId> allocate(LogicalTextIndex logical,
            List<Memory.ObjectDeclaration> objects,List<Memory.Storage> storage,
            UnitId unit,LocalIds ids,SourceOrigins origins,List<Evidence.CoverageItem> items,
            List<Evidence.Uncertainty> uncertainties) {
        var result=new LinkedHashMap<StorageFacts.NodeId,ObjectId>();
        for(var view:logical.views.values().stream().filter(v->v.node().equals(v.root()))
                .sorted(Comparator.comparing(v->v.node().handle())).toList()) {
            var node=logical.nodes.get(view.node());
            if(node.kind()!=StorageFacts.Kind.GROUP||node.data().isPresent())continue;
            var object=new ObjectId(unit,ids.id("object","anonymous-logical-root",unit.localId(),node.id().handle()));
            var cell=new StorageId(unit.publication(),ids.id("storage","anonymous-logical-root",unit.localId(),node.id().handle()));
            var origin=origins.source("logical-root",node.id().handle(),node.provenance());
            var type=Types.known(Types.Builtin.TEXT);
            objects.add(new Memory.ObjectDeclaration(object,Optional.empty(),type,new Memory.CellBinding(cell),
                Memory.Visibility.PRIVATE,origin,Evidence.CoverageStatus.MODELED,
                ScalarEvidence.limited(ids,unit.publication(),object,"anonymous-logical-root/"+unit.localId()+"/"+node.id().handle(),
                    origin,Evidence.Dimension.STORAGE,uncertainties)));
            storage.add(new Memory.Cell(new Memory.StorageHeader(cell,Optional.of(unit),Memory.Lifetime.PERSISTENT,
                Memory.Visibility.PRIVATE,origin),type));
            items.add(ScalarEvidence.item(unit.publication(),"logical-root",node.id().handle(),origin,List.of(object,cell)));
            result.put(node.id(),object);
        }
        return Map.copyOf(result);
    }

    static ObjectId object(StorageFacts.Node root,ScalarDataTranslator.Result data) {
        // Named roots retain their established object (including capture aliases).
        // The anonymous cell is not a DataLink and grants no physical byte proof.
        return root.data().map(d->data.index().get(d).object())
            .orElseGet(()->Objects.requireNonNull(data.anonymousLogicalRoots().get(root.id()),"logical root cell"));
    }
}
