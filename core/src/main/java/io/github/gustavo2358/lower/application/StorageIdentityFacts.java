package io.github.gustavo2358.lower.application;

import io.github.gustavo2358.lower.domain.StorageFacts;
import java.util.Comparator;
import java.util.function.Consumer;

/** Canonical encoding of explicit physical facts; never derives a layout or an alias from handles. */
final class StorageIdentityFacts {
    private StorageIdentityFacts() { }
    static StorageFacts.Inventory canonical(StorageFacts.Inventory s) {
        return new StorageFacts.Inventory(s.profile(),s.profileId(),s.runtimeCodec(),
            s.nodes().stream().sorted(Comparator.comparing(n->n.id().handle())).toList(),
            s.bases().stream().sorted(Comparator.comparing(b->b.id().handle())).toList(),
            s.views().stream().sorted(Comparator.comparing(v->v.node().handle())).toList(),s.gapCodes(),
            s.relations().stream().sorted(Comparator.comparing(r->r.id().handle())).toList(),
            s.renames().stream().sorted(Comparator.comparing(r->r.id().handle())).toList(),new StorageFacts.EntryState(s.entryState().mode(),
                s.entryState().conditions().stream().sorted(Comparator.comparing(c->c.node().handle())).toList(),s.entryState().possibilityDomain()),s.logicalTextViews().stream().sorted(Comparator.comparing(v->v.node().handle())).toList());
    }
    static void write(Object fact,Consumer<String> field,Consumer<Object> value) {
        switch(fact) {
            case StorageFacts.NodeId r -> { field.accept("StorageNodeId");field.accept("unit");value.accept(r.unit());field.accept("handle");value.accept(r.handle()); }
            case StorageFacts.BaseId r -> { field.accept("StorageBaseId");field.accept("unit");value.accept(r.unit());field.accept("handle");value.accept(r.handle()); }
            case StorageFacts.RelationId r -> { field.accept("StorageRelationId");field.accept("unit");value.accept(r.unit());field.accept("handle");value.accept(r.handle()); }
            case StorageFacts.Relation r -> {
                field.accept("StorageRelation");field.accept("id");value.accept(r.id());field.accept("owner");value.accept(r.owner());
                field.accept("target");value.accept(r.target());field.accept("status");value.accept(r.status());
                field.accept("provenance");value.accept(r.provenance());field.accept("gapCodes");value.accept(r.gapCodes());
            }
            case StorageFacts.Measure r -> { field.accept("StorageMeasure");field.accept("value");value.accept(r.value());field.accept("gapCodes");value.accept(r.gapCodes()); }
            case StorageFacts.Node r -> {
                field.accept("PhysicalNode");field.accept("id");value.accept(r.id());field.accept("parent");value.accept(r.parent());
                field.accept("order");value.accept(r.order());field.accept("filler");value.accept(r.filler());field.accept("kind");value.accept(r.kind());
                field.accept("data");value.accept(r.data());field.accept("extent");value.accept(r.extent());field.accept("provenance");value.accept(r.provenance());
            }
            case StorageFacts.Base r -> {
                field.accept("StorageBase");field.accept("id");value.accept(r.id());field.accept("extent");value.accept(r.extent());
                field.accept("allocation");value.accept(r.allocation());field.accept("provenance");value.accept(r.provenance());
            }
            case StorageFacts.View r -> {
                field.accept("StorageView");field.accept("node");value.accept(r.node());field.accept("base");value.accept(r.base());
                field.accept("offset");value.accept(r.offset());field.accept("extent");value.accept(r.extent());
                field.accept("codec");value.accept(r.codec());field.accept("provenance");value.accept(r.provenance());
            }
            case StorageFacts.Slice r -> { field.accept("RegionalSlice");field.accept("offset");value.accept(r.offset());field.accept("extent");value.accept(r.extent()); }
            case StorageFacts.Renames r -> { field.accept("StorageRenames");field.accept("id");value.accept(r.id());field.accept("owner");value.accept(r.owner());field.accept("from");value.accept(r.from());field.accept("through");value.accept(r.through());field.accept("status");value.accept(r.status());field.accept("provenance");value.accept(r.provenance());field.accept("gapCodes");value.accept(r.gapCodes()); }
            case StorageFacts.Access r -> { field.accept("RegionalAccess");field.accept("view");value.accept(r.view());if(r.slice().isPresent()){field.accept("slice@1");value.accept(r.slice().get());} }
            case StorageFacts.Move r -> {
                field.accept("RegionalMove");field.accept("kind");value.accept(r.kind());field.accept("bytes");value.accept(r.bytes());field.accept("gapCodes");value.accept(r.gapCodes());
            }
            case StorageFacts.InitialCondition r -> {
                field.accept("StorageInitialCondition");value.accept(r.node());value.accept(r.kind());value.accept(r.bytes());value.accept(r.gapCodes());value.accept(r.provenance());
                field.accept("entryProof@1.4");value.accept(r.proof());if(r.logicalText().isPresent()){field.accept("logicalText@1.6");value.accept(r.logicalText().get());}
            }
            case StorageFacts.EntryState r -> {field.accept("StorageEntryState");value.accept(r.mode());value.accept(r.conditions());if(r.possibilityDomain()==StorageFacts.PossibilityDomain.LOGICAL_SOURCE){field.accept("sourceEvidence@1.6");value.accept(r.possibilityDomain());}}
            case StorageFacts.LogicalTextView r -> {
                field.accept("LogicalTextView");field.accept("node");value.accept(r.node());field.accept("root");value.accept(r.root());
                field.accept("start");value.accept(r.start());field.accept("length");value.accept(r.length());
            }
            case StorageFacts.Inventory r -> {
                field.accept("StorageInventory");field.accept("profile");value.accept(r.profile());field.accept("profileId");value.accept(r.profileId());
                field.accept("runtimeCodec");value.accept(r.runtimeCodec());field.accept("nodes");value.accept(r.nodes());field.accept("bases");value.accept(r.bases());
                field.accept("views");value.accept(r.views());field.accept("gapCodes");value.accept(r.gapCodes());
                if(!r.relations().isEmpty()){field.accept("relations");value.accept(r.relations());}
                if(!r.logicalTextViews().isEmpty()){field.accept("logicalTextViews@1");value.accept(r.logicalTextViews());}
                if(!r.renames().isEmpty()){field.accept("renames@1");value.accept(r.renames());}
                if(!r.entryState().equals(StorageFacts.EntryState.unknown())){field.accept("entryState@1");value.accept(r.entryState());}
            }
            default -> throw new IllegalArgumentException("unsupported SP identity fact");
        }
    }
}
