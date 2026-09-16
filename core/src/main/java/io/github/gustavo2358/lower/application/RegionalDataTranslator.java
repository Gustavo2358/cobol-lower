package io.github.gustavo2358.lower.application;

import io.github.gustavo2358.air.model.*;
import io.github.gustavo2358.air.model.Ids.*;
import io.github.gustavo2358.lower.domain.SpInput;
import io.github.gustavo2358.lower.domain.StorageFacts;
import java.util.*;

/** Project explicit source bases once; never derive offsets, extents or independence from names. */
final class RegionalDataTranslator {
    private RegionalDataTranslator() { }
    static boolean sourceText(RegionalStorageAdmission.Index source,SpInput.DataId data) {
        return source.owner().storage().filter(s->s.entryState().possibilityDomain()==StorageFacts.PossibilityDomain.LOGICAL_SOURCE)
            .stream().flatMap(s->s.entryState().conditions().stream()).anyMatch(c->c.kind()==StorageFacts.InitialKind.POSSIBLE_LITERAL_BYTES
                &&source.nodes().get(c.node()).data().filter(data::equals).isPresent());
    }
    static boolean textual(RegionalStorageAdmission.Index source,SpInput.DataId data) {
        var view=source.byData().get(data);
        return view!=null&&view.codec().isPresent()&&view.offset().value().isPresent()&&view.extent().value().isPresent()
            &&source.bases().get(view.base()).extent().value().isPresent();
    }
    static boolean encodableLiteral(RegionalStorageAdmission.Index source,SpInput.MoveFact move) {
        if(move.source() instanceof SpInput.DataReference)return true;
        var selected=move.target().wholeItemAccess().orElseThrow().data();
        if(!textual(source,selected))return true;
        var view=source.byData().get(selected);
        return MemoryCodecs.encodeText(RegionalStorageAdmission.IBM1047,new Values.TextValue(move.textAdjustment().map(a->a.result().value()).orElseGet(()->((SpInput.LiteralSource)move.source()).logicalValue().orElseThrow().value())),
            view.extent().value().orElseThrow()).status()==MemoryCodecs.Status.EXACT;
    }
    static ScalarDataTranslator.Result translate(List<SpInput.DataFact> declarations,RegionalStorageAdmission.Index source,
            UnitId unit,LocalIds ids,SourceOrigins origins,List<Evidence.CoverageItem> items,List<Evidence.Uncertainty> uncertainties) {
        var aliasData=new HashSet<SpInput.DataId>();
        source.owner().storage().ifPresent(st->st.renames().forEach(r->source.nodes().get(r.owner()).data().ifPresent(aliasData::add)));
        var legacy=ScalarDataTranslator.translate(declarations.stream().filter(d->!textual(source,d.id())&&!aliasData.contains(d.id())).toList(),unit,ids,origins,items,uncertainties);
        if(source.owner().storage().isEmpty())return legacy;
        var relationOrigins=new LinkedHashMap<StorageFacts.RelationId,OriginId>();
        var allocationEvidence=new HashMap<StorageFacts.BaseId,List<OriginId>>();
        for(var relation:source.owner().storage().get().relations().stream().sorted(Comparator.comparing(r->r.id().handle())).toList()) {
            var origin=origins.source("storage-relation",relation.id().handle(),relation.provenance());relationOrigins.put(relation.id(),origin);
            if(relation.status()==StorageFacts.RelationStatus.PROVEN)
                allocationEvidence.computeIfAbsent(source.views().get(relation.owner()).base(),ignored->new ArrayList<>()).add(origin);
        }
        var renamesOrigins=new LinkedHashMap<StorageFacts.RelationId,OriginId>();
        for(var r:source.owner().storage().get().renames().stream().sorted(Comparator.comparing(x->x.id().handle())).toList()) {
            var origin=origins.source("storage-renames",r.id().handle(),r.provenance());renamesOrigins.put(r.id(),origin);
            if(r.status()==StorageFacts.RelationStatus.PROVEN)
                allocationEvidence.computeIfAbsent(source.views().get(r.owner()).base(),ignored->new ArrayList<>()).add(origin);
        }
        if(source.owner().storage().get().profile()==StorageFacts.Profile.UNSPECIFIED) {
            renamesCoverage(source,Map.of(),renamesOrigins,unit,ids,items,uncertainties);
            relationCoverage(source,Map.of(),relationOrigins,unit,ids,items,uncertainties);return legacy;
        }
        var objects=new ArrayList<>(legacy.objects());var storage=new ArrayList<>(legacy.storage());
        var index=new LinkedHashMap<>(legacy.index());var bindings=new LinkedHashMap<SpInput.DataId,Memory.ViewBinding>();
        var physical=new LinkedHashMap<StorageFacts.BaseId,StorageId>();var baseOrigins=new HashMap<StorageFacts.BaseId,OriginId>();
        // A proved standalone legacy scalar may have an unknown byte representation. Keep its
        // single abstract Cell as this component's representative, without a second allocation.
        for(var link:legacy.index().values()) {
            var view=source.byData().get(link.source());
            if(view!=null)physical.put(view.base(),link.storage().orElseThrow());
        }
        for(var base:source.bases().values().stream().sorted(Comparator.comparing(b->b.id().handle())).toList()) {
            var origin=origins.source("storage-base",base.id().handle(),base.provenance());
            var relations=allocationEvidence.getOrDefault(base.id(),List.of());
            if(!relations.isEmpty()) {
                var inputs=new ArrayList<OriginId>();inputs.add(origin);inputs.addAll(relations);
                origin=origins.derived(ids.id("origin","shared-source-base",unit.localId(),base.id().handle()),inputs,"storage@1/explicit-shared-location");
            }
            baseOrigins.put(base.id(),origin);
            if(physical.containsKey(base.id()))continue;
            boolean privateAllocation=base.allocation()==StorageFacts.Allocation.INDEPENDENT_LOCAL_WORKING_STORAGE;
            // An unproved, unknown base does not establish ordinary persistent storage duration.
            // Retain a source gap rather than invent an AIR lifetime that the wire cannot express.
            if(!privateAllocation&&base.extent().value().isEmpty()) {
                gap(base.id().handle(),origin,new Scopes.UnitScope(unit),List.of(),"STORAGE_ALLOCATION_UNAVAILABLE",base.extent().gapCodes(),unit,ids,items,uncertainties);
                continue;
            }
            var id=new StorageId(unit.publication(),ids.id("storage","source-region",unit.localId(),base.id().handle()));physical.put(base.id(),id);
            Optional<UncertaintyId> unknown=Optional.empty();
            if(base.extent().value().isEmpty())unknown=Optional.of(gap(base.id().handle(),origin,new Scopes.EntityScope(List.of(id)),List.of(id),
                "STORAGE_EXTENT_UNKNOWN",base.extent().gapCodes(),unit,ids,items,uncertainties));
            storage.add(new Memory.Region(new Memory.StorageHeader(id,Optional.of(unit),Memory.Lifetime.PERSISTENT,
                privateAllocation?Memory.Visibility.PRIVATE:Memory.Visibility.UNKNOWN,origin),base.extent().value(),unknown));
            if(unknown.isEmpty())items.add(ScalarEvidence.item(unit.publication(),"storage-base",base.id().handle(),origin,List.of(id)));
        }
        var declared=new HashMap<SpInput.DataId,SpInput.DataFact>();declarations.forEach(d->declared.put(d.id(),d));
        for(var node:source.nodes().values().stream().sorted(Comparator.comparing(n->n.id().handle())).toList()) {
            var view=source.views().get(node.id());var region=physical.get(view.base());
            var origin=origins.source("storage-node",node.id().handle(),node.provenance());
            if(region==null)continue; // Base gap above covers its unavailable physical representation.
            var viewOrigin=origins.source("storage-view",node.id().handle(),view.provenance());
            if(node.data().isPresent()&&declared.containsKey(node.data().get())&&textual(source,node.data().get())) {
                var data=declared.get(node.data().get());
                var dataOrigin=origins.source("data",data.id().handle(),data.provenance());
                var object=new ObjectId(unit,ids.id("object","regional-view",unit.localId(),data.id().handle()));
                var objectOrigin=origins.derived(ids.id("origin","regional-object",unit.localId(),node.id().handle()),
                    List.of(dataOrigin,origin,viewOrigin,baseOrigins.get(view.base())),"storage@1/explicit-source-view");
                var binding=new Memory.ViewBinding(region,view.offset().value().orElseThrow(),view.extent().value().orElseThrow(),RegionalStorageAdmission.IBM1047);
                var visibility=source.bases().get(view.base()).allocation()==StorageFacts.Allocation.INDEPENDENT_LOCAL_WORKING_STORAGE?Memory.Visibility.PRIVATE:Memory.Visibility.UNKNOWN;
                objects.add(new Memory.ObjectDeclaration(object,Optional.of(data.canonicalName()),Types.known(Types.Builtin.TEXT),binding,visibility,objectOrigin,
                    Evidence.CoverageStatus.MODELED,ScalarEvidence.limited(ids,unit.publication(),object,data.id().handle(),dataOrigin,Evidence.Dimension.STORAGE,uncertainties)));
                index.put(data.id(),new LoweringResult.DataLink(data.id(),object,region,dataOrigin));bindings.put(data.id(),binding);
                items.add(ScalarEvidence.item(unit.publication(),"data",data.id().handle(),dataOrigin,List.of(object,region)));
            }
            if(view.offset().value().isEmpty()||view.extent().value().isEmpty()) {
                var reasons=new LinkedHashSet<>(view.offset().gapCodes());reasons.addAll(view.extent().gapCodes());
                gap(node.id().handle(),origin,new Scopes.EntityScope(List.of(region)),List.of(region),"STORAGE_VIEW_UNKNOWN",List.copyOf(reasons),unit,ids,items,uncertainties);
            } else items.add(ScalarEvidence.item(unit.publication(),"storage-node",node.id().handle(),viewOrigin,List.of(region)));
        }
        // Inventory presence is distinct from admission for precise expression translation.
        // Unsupported declarations keep an object identity without inventing a typed read.
        for(var declaration:ScalarDataOrder.canonical(source.owner().dataDeclarations())) {
            if(index.containsKey(declaration.id()))continue;
            var view=source.byData().get(declaration.id());var base=view==null?null:physical.get(view.base());
            var dataOrigin=origins.source("data",declaration.id().handle(),declaration.provenance());
            var object=new ObjectId(unit,ids.id("object","unknown-regional-view",unit.localId(),declaration.id().handle()));
            var inputs=new ArrayList<OriginId>();inputs.add(dataOrigin);
            if(view!=null) {inputs.add(origins.source("storage-view",view.node().handle(),view.provenance()));inputs.add(baseOrigins.get(view.base()));}
            var objectOrigin=origins.derived(ids.id("origin","unknown-regional-object",unit.localId(),declaration.id().handle()),inputs,"storage@1/unproved-source-view");
            var reason=gap(declaration.id().handle(),objectOrigin,new Scopes.EntityScope(List.of(object)),List.of(object),
                "STORAGE_DECLARATION_UNKNOWN",List.of("LOGICAL_TYPE_OR_PHYSICAL_VIEW_UNPROVEN"),unit,ids,items,uncertainties);
            Types.TypeRef type;
            if(sourceText(source,declaration.id())) {
                type=Types.known(Types.Builtin.TEXT);
                index.put(declaration.id(),new LoweringResult.DataLink(declaration.id(),object,Optional.empty(),dataOrigin));
            } else {
                var typeReason=new UncertaintyId(unit.publication(),ids.id("uncertainty","unknown-declaration-type",unit.localId(),declaration.id().handle()));
                uncertainties.add(new Evidence.Uncertainty(typeReason,"TYPE_UNKNOWN",List.of(Evidence.Dimension.VALUES),new Scopes.EntityScope(List.of(object)),"No source proof of AIR logical type",objectOrigin));
                type=new Types.UnknownType(typeReason);
            }
            var binding=new Memory.UnknownBinding(base==null?new Scopes.AllMemory(unit.publication(),true):new Scopes.StorageMemory(List.of(base)),reason);
            objects.add(new Memory.ObjectDeclaration(object,Optional.of(declaration.canonicalName()),type,binding,Memory.Visibility.UNKNOWN,objectOrigin,
                Evidence.CoverageStatus.ABSTRACTED,ScalarEvidence.limited(ids,unit.publication(),object,declaration.id().handle(),dataOrigin,Evidence.Dimension.STORAGE,uncertainties)));
        }
        relationCoverage(source,physical,relationOrigins,unit,ids,items,uncertainties);
        renamesCoverage(source,physical,renamesOrigins,unit,ids,items,uncertainties);
        return new ScalarDataTranslator.Result(List.copyOf(objects),List.copyOf(storage),Collections.unmodifiableMap(index),Map.copyOf(bindings),Map.copyOf(physical));
    }
    private static void renamesCoverage(RegionalStorageAdmission.Index source,Map<StorageFacts.BaseId,StorageId> physical,
            Map<StorageFacts.RelationId,OriginId> renamesOrigins,UnitId unit,LocalIds ids,List<Evidence.CoverageItem> items,List<Evidence.Uncertainty> uncertainties) {
        for(var r:source.owner().storage().orElseThrow().renames()) {
            var base=physical.get(source.views().get(r.owner()).base());var origin=renamesOrigins.get(r.id());
            if(r.status()==StorageFacts.RelationStatus.PROVEN&&base!=null)
                items.add(ScalarEvidence.item(unit.publication(),"storage-renames",r.id().handle(),origin,List.of(base)));
            else gap(r.id().handle(),origin,new Scopes.UnitScope(unit),List.of(),"STORAGE_RENAMES_UNPROVEN",
                r.gapCodes().isEmpty()?List.of("PHYSICAL_BASE_UNAVAILABLE"):r.gapCodes(),unit,ids,items,uncertainties);
        }
    }
    private static void relationCoverage(RegionalStorageAdmission.Index source,Map<StorageFacts.BaseId,StorageId> physical,
            Map<StorageFacts.RelationId,OriginId> relationOrigins,UnitId unit,LocalIds ids,List<Evidence.CoverageItem> items,List<Evidence.Uncertainty> uncertainties) {
        var relations=new HashMap<StorageFacts.RelationId,StorageFacts.Relation>();
        source.owner().storage().orElseThrow().relations().forEach(r->relations.put(r.id(),r));
        for(var entry:relationOrigins.entrySet()) {
            var r=relations.get(entry.getKey());var base=physical.get(source.views().get(r.owner()).base());
            if(r.status()==StorageFacts.RelationStatus.PROVEN&&base!=null)
                items.add(ScalarEvidence.item(unit.publication(),"storage-relation",r.id().handle(),entry.getValue(),List.of(base)));
            else {
                var code=r.status()==StorageFacts.RelationStatus.UNPROVEN?"STORAGE_RELATION_UNPROVEN":"STORAGE_RELATION_UNREPRESENTED";
                var reasons=r.gapCodes().isEmpty()?List.of("PHYSICAL_BASE_UNAVAILABLE"):r.gapCodes();
                gap(r.id().handle(),entry.getValue(),new Scopes.UnitScope(unit),List.of(),code,reasons,unit,ids,items,uncertainties);
            }
        }
    }
    private static UncertaintyId gap(String key,OriginId origin,Scopes.FactScope scope,List<Id> outputs,String code,List<String> reasons,
            UnitId unit,LocalIds ids,List<Evidence.CoverageItem> items,List<Evidence.Uncertainty> uncertainties) {
        var id=new UncertaintyId(unit.publication(),ids.id("uncertainty",code,unit.localId(),key));
        uncertainties.add(new Evidence.Uncertainty(id,"cobol-lower:"+code,List.of(Evidence.Dimension.STORAGE),scope,String.join(",",reasons),origin));
        items.add(new Evidence.CoverageItem("storage@1/"+code+"/"+key,origin,Evidence.CoverageStatus.ABSTRACTED,outputs,List.of(id),Optional.empty()));return id;
    }
    static Capabilities.Manifest capabilities(ScalarDataTranslator.Result data) {
        var required=new ArrayList<Capabilities.Capability>();
        if(data.storage().stream().anyMatch(Memory.Region.class::isInstance))required.add(Capabilities.MEMORY_REGIONS);
        if(!data.views().isEmpty())required.add(Capabilities.IBM1047);
        return new Capabilities.Manifest(required,List.of());
    }
    static List<Proofs.Premise> premises(RegionalStorageAdmission.Index source,ScalarDataTranslator.Result data,
            UnitId unit,LocalIds ids,SourceOrigins origins) {
        var members=new ArrayList<StorageId>();var evidence=new ArrayList<OriginId>();
        for(var base:source.bases().values().stream().sorted(Comparator.comparing(b->b.id().handle())).toList())
            if(base.allocation()==StorageFacts.Allocation.INDEPENDENT_LOCAL_WORKING_STORAGE&&data.physical().containsKey(base.id())) {
                members.add(data.physical().get(base.id()));evidence.add(origins.source("storage-base",base.id().handle(),base.provenance()));
            }
        if(members.size()<2)return List.of();
        var origin=origins.derived(ids.id("origin","regional-independence",unit.localId(),"allocations"),evidence,"storage@1/independent-local-working-storage");
        return List.of(new Proofs.Premise(new PremiseId(unit.publication(),ids.id("premise","regional-independence",unit.localId(),"allocations")),
            StorageFacts.PROFILE_ID,"Explicit source allocation proof; one representative per physical base",origin,new Proofs.DisjointStorage(members)));
    }
}
