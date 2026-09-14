package io.github.gustavo2358.lower.application;
import io.github.gustavo2358.air.model.*;
import io.github.gustavo2358.air.model.Ids.*;
import io.github.gustavo2358.lower.domain.StorageFacts;
import java.util.*;
/** Source-proved invocation conditions projected once, never executable assignments. */
final class RegionalEntryTranslator {
    private RegionalEntryTranslator() { }
    static Entries.EntryState translate(RegionalStorageAdmission.Index source,ScalarDataTranslator.Result data,EntryId entry,
            LocalIds ids,SourceOrigins origins,List<Evidence.CoverageItem> items,List<Evidence.Uncertainty> uncertainties) {
        if(source.owner().storage().isEmpty())return new Entries.EntryState(List.of(),List.of());
        var state=source.owner().storage().get().entryState();var conditions=new ArrayList<Entries.InitialCondition>();var gaps=new ArrayList<UncertaintyId>();
        var represented=new HashMap<StorageId,Memory.Storage>();data.storage().forEach(s->represented.put(s.header().id(),s));
        for(var fact:state.conditions().stream().sorted(Comparator.comparing(c->c.node().handle())).toList()) {
            var key=fact.node().handle();var origin=origins.source("storage-initial",key,fact.provenance());
            origin=origins.derived(ids.id("origin","storage-entry-profile",entry.localId(),key),List.of(origin),
                "storage@1/entry-mode="+state.mode().name()+"; source-proved invocation condition");
            var view=source.views().get(fact.node());var storage=data.physical().get(view.base());
            Place place=null;
            if(storage!=null&&represented.get(storage) instanceof Memory.Region&&view.offset().value().isPresent()&&view.extent().value().isPresent()) {
                place=new Places.RegionSlice(header(entry,key,"place",Operand.Role.VALUE_WRITE,origin,ids),storage,
                    integer(entry,key,"offset",view.offset().value().get(),origin,ids),integer(entry,key,"extent",view.extent().value().get(),origin,ids),
                    Memory.IdentityBytes.INSTANCE,Types.known(Types.Builtin.BYTES));
            }
            // A source physical component already represented by a legacy Cell is never allocated twice.
            if(storage!=null&&represented.get(storage) instanceof Memory.Cell) {
                var link=source.nodes().get(fact.node()).data().map(data.index()::get).orElse(null);
                if(link!=null)place=new Places.ObjectPlace(header(entry,key,"place",Operand.Role.VALUE_WRITE,origin,ids),link.object());
            }
            Entries.InitialValue value=null;
            if(fact.kind()==StorageFacts.InitialKind.LITERAL_BYTES&&place!=null) {
                Values.LiteralValue literal=new Values.BytesValue(fact.bytes());
                if(place instanceof Places.ObjectPlace)literal=MemoryCodecs.decodeText(RegionalStorageAdmission.IBM1047,(Values.BytesValue)literal,view.extent().value().orElseThrow()).value().orElseThrow();
                value=new Entries.LiteralInitial(new Expressions.Literal(header(entry,key,"value",Operand.Role.VALUE_READ,origin,ids),literal));
            } else if(fact.kind()==StorageFacts.InitialKind.PRESERVE&&place!=null)value=Entries.Preserve.INSTANCE;
            else {
                var reason=new UncertaintyId(entry.unit().publication(),ids.id("uncertainty","initial-storage",entry.localId(),key));gaps.add(reason);
                uncertainties.add(new Evidence.Uncertainty(reason,"cobol-lower:INITIAL_STORAGE_UNKNOWN",List.of(Evidence.Dimension.STORAGE,Evidence.Dimension.VALUES),
                    place==null?new Scopes.UnitScope(entry.unit()):new Scopes.EntityScope(List.of(storage)),
                    fact.gapCodes().isEmpty()?"Initial physical representation unavailable":String.join(",",fact.gapCodes()),origin));
                value=new Entries.ExternalUnknown(reason);
            }
            if(place!=null)conditions.add(new Entries.InitialCondition(place,value,origin,List.of()));
            items.add(new Evidence.CoverageItem("storage@1/initial/"+key,origin,place!=null&&fact.kind()!=StorageFacts.InitialKind.UNKNOWN?Evidence.CoverageStatus.MODELED:Evidence.CoverageStatus.ABSTRACTED,
                storage==null?List.of():List.of(storage),value instanceof Entries.ExternalUnknown v?List.of(v.reason()):List.of(),Optional.empty()));
        }
        return new Entries.EntryState(conditions,gaps);
    }
    private static Operand.Header header(EntryId entry,String key,String role,Operand.Role kind,OriginId origin,LocalIds ids) {
        return new Operand.Header(new OperandId(new EntryOwner(entry),ids.id("operand","initial-"+role,entry.localId(),key)),kind,origin);
    }
    private static Expression integer(EntryId entry,String key,String role,java.math.BigInteger value,OriginId origin,LocalIds ids) {
        return new Expressions.Literal(header(entry,key,role,Operand.Role.VALUE_READ,origin,ids),new Values.IntValue(value));
    }
}
