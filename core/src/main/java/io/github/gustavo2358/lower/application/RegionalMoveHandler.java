package io.github.gustavo2358.lower.application;

import io.github.gustavo2358.air.model.*;
import io.github.gustavo2358.air.model.Ids.*;
import io.github.gustavo2358.lower.domain.SpInput;
import io.github.gustavo2358.lower.domain.StorageFacts;
import java.math.BigInteger;
import java.util.*;

/** Byte effects consume admitted SP facts. CopyBytes captures its source before destination writes. */
final class RegionalMoveHandler {
    private RegionalMoveHandler() { }
    static List<Instruction> sequence(SpInput.MoveFact move,boolean fitted,ScalarDataTranslator.Result data,RegionalStorageAdmission.Index storage,UnitId unit,
            LocalIds ids,SourceOrigins origins,List<LoweringResult.OperandLink> links,List<Evidence.CoverageItem> items,List<Evidence.Uncertainty> uncertainties) {
        if(move.copySemantics()==SpInput.CopySemantics.POSSIBLE_TEXT) {
            boolean available=move.target().logicalWholeItem().filter(data.index()::containsKey).isPresent()
                &&(!(move.source() instanceof SpInput.DataReference r)||r.wholeItemAccess().map(SpInput.WholeItemAccess::data).or(r::logicalWholeItem).filter(data.index()::containsKey).isPresent());
            return List.of(available?MoveHandler.translate(move,data,unit,ids,origins,links,items):ConservativeMove.translate(move,data,unit,ids,origins,links,uncertainties));
        }
        if(move.logicalTransfers().isEmpty()&&storage.logical().literalMove(move))return LogicalTextMove.translate(move,storage.logical(),data,unit,ids,origins,links,items);
        if(move.regionalMove().isEmpty())return List.of(translate(move,fitted,data,unit,ids,origins,links,items,uncertainties));
        var result=new ArrayList<Instruction>();
        var transfers=move.transfers();
        for(int i=0;i<transfers.size();i++) {
            var transfer=transfers.get(i);
            var logical=move.logicalTransfers().stream().filter(t->t.target().equals(transfer.target().id())).findFirst();
            if(logical.isPresent()) {
                result.add(logical(move,transfer,logical.orElseThrow(),data,unit,ids,origins,links,items));
            } else {
                var single=i==0?move:new SpInput.MoveFact(move.header(),transfer.source(),transfer.target(),
                    SpInput.CopySemantics.UNAVAILABLE,move.normalContinuation(),Optional.empty(),Optional.of(transfer.effect()));
                result.add(translate(single,fitted&&transfers.size()==1,data,unit,ids,origins,links,items,uncertainties));
            }
        }
        return List.copyOf(result);
    }
    private static Instruction logical(SpInput.MoveFact move,SpInput.MoveTransfer transfer,SpInput.LogicalTransfer proof,
            ScalarDataTranslator.Result data,UnitId unit,LocalIds ids,SourceOrigins origins,
            List<LoweringResult.OperandLink> links,List<Evidence.CoverageItem> items) {
        var key=move.header().id().handle();
        var operation=new OperationId(unit,ids.id("operation","logical-receiver-move",unit.localId(),key+"/"+proof.target().handle()));
        var statement=origins.source("statement",key,move.header().provenance());
        var source=origins.source("operand",transfer.source().id().handle(),transfer.source().provenance());
        var target=origins.source("operand",transfer.target().id().handle(),transfer.target().provenance());
        var origin=origins.derived(ids.id("origin","logical-receiver-move",unit.localId(),operation.localId()),
            List.of(statement,source,target),"sp2.38/independent-logical-receiver");
        var targetId=new OperandId(new OperationOwner(operation),ids.id("operand","logical-receiver-target",operation.localId(),proof.target().handle()));
        var sourceId=new OperandId(new OperationOwner(operation),ids.id("operand","logical-receiver-literal",operation.localId(),transfer.source().id().handle()));
        var object=data.index().get(transfer.target().logicalWholeItem().orElseThrow()).object();
        var assign=new Operations.Assign(new Operations.Header(operation,origin,Evidence.CoverageStatus.MODELED,
            ScalarEvidence.assign(operation),List.of()),
            new Places.ObjectPlace(new Operand.Header(targetId,Operand.Role.VALUE_WRITE,target),object),
            new Expressions.Literal(new Operand.Header(sourceId,Operand.Role.VALUE_READ,source),new Values.TextValue(proof.value().value())));
        correlate(transfer.source().id(),sourceId,source,links,items,unit,ids);
        correlate(transfer.target().id(),targetId,target,links,items,unit,ids);
        return assign;
    }
    static Instruction translate(SpInput.MoveFact move,boolean admittedFitting,ScalarDataTranslator.Result data,UnitId unit,
            LocalIds ids,SourceOrigins origins,List<LoweringResult.OperandLink> links,List<Evidence.CoverageItem> items,
            List<Evidence.Uncertainty> uncertainties) {
        if(move.regionalMove().filter(e->e.kind()==StorageFacts.MoveKind.UNAVAILABLE).isPresent())
            return ConservativeMove.translate(move,data,unit,ids,origins,links,uncertainties);
        if(move.regionalMove().isEmpty()||admittedFitting)
            return MoveHandler.translate(move,data,unit,ids,origins,links,items);
        var effect=move.regionalMove().get();
        // In the SP MOVE contract MUST_UNKNOWN denotes an unimplemented transform,
        // not an external input. Retain the source occurrence without a substitute write.
        if(effect.kind()==StorageFacts.MoveKind.MUST_UNKNOWN)
            return ConservativeMove.translate(move,data,unit,ids,origins,links,uncertainties);
        var key=move.header().id().handle();
        var operation=new OperationId(unit,ids.id("operation","regional-move",unit.localId(),key+"/"+move.target().id().handle()));
        var statement=origins.source("statement",key,move.header().provenance());
        var sourceOrigin=origins.source("operand",move.source().id().handle(),move.source().provenance());
        var targetOrigin=origins.source("operand",move.target().id().handle(),move.target().provenance());
        var origin=origins.derived(ids.id("origin","regional-move",unit.localId(),operation.localId()),List.of(statement,sourceOrigin,targetOrigin),"storage@1/published-byte-effect/"+effect.kind());
        var dest=RegionalPlaces.view(data.views().get(move.target().binding().selected().orElseThrow()),move.target());
        var targetId=new OperandId(new OperationOwner(operation),ids.id("operand","regional-target",operation.localId(),move.target().id().handle()));
        var sourceId=new OperandId(new OperationOwner(operation),ids.id("operand","regional-source",operation.localId(),move.source().id().handle()));
        var header=new Operations.Header(operation,origin,Evidence.CoverageStatus.MODELED,ScalarEvidence.assign(operation),List.of());
        if(effect.kind()==StorageFacts.MoveKind.COPY_BYTES) {
            var source=RegionalPlaces.view(data.views().get(((SpInput.DataReference)move.source()).binding().selected().orElseThrow()),(SpInput.DataReference)move.source());
            var destinationRange=range(dest,targetId,targetOrigin,ids);var sourceRange=range(source,sourceId,sourceOrigin,ids);
            // The fallback bounds remain conservative even for consumers unable to prove the copy.
            var fallback=new Envelopes.Envelope(new Envelopes.MemoryEnvelope(List.of(),new Scopes.WithinMemory(new Scopes.StorageMemory(List.of(source.region()))),
                List.of(),new Scopes.WithinMemory(new Scopes.StorageMemory(List.of(dest.region()))),List.of()),
                new Control.ControlEnvelope(List.of(Control.ContinueAlternative.INSTANCE),Scopes.NoControl.INSTANCE),new Envelopes.DependencyEnvelope(List.of(),Scopes.NoResources.INSTANCE));
            correlate(move.target().id(),destinationRange.offset().header().id(),targetOrigin,links,items,unit,ids);
            correlate(move.source().id(),sourceRange.offset().header().id(),sourceOrigin,links,items,unit,ids);
            return new Operations.CopyBytes(header,destinationRange,sourceRange,dest.extent(),fallback);
        }
        if(effect.kind()==StorageFacts.MoveKind.LOGICAL_FIT_TEXT) {
            var reference=(SpInput.DataReference)move.source();var source=data.index().get(reference.logicalWholeItem().orElseThrow()).object();
            var sourcePlaceId=new OperandId(sourceId.owner(),ids.id("operand","logical-source",operation.localId(),reference.id().handle()));
            var readId=new OperandId(sourceId.owner(),ids.id("operand","logical-read",operation.localId(),reference.id().handle()));
            var read=new Expressions.Read(new Operand.Header(readId,Operand.Role.VALUE_READ,sourceOrigin),new Places.ObjectPlace(new Operand.Header(sourcePlaceId,Operand.Role.VALUE_READ,sourceOrigin),source));
            var dstRange=range(dest,targetId,targetOrigin,ids);
            var destination=new Places.RegionSlice(new Operand.Header(targetId,Operand.Role.VALUE_WRITE,targetOrigin),dest.region(),dstRange.offset(),dstRange.extent(),dest.codec(),Types.known(Types.Builtin.TEXT));
            var fit=new Expressions.FitText(new Operand.Header(sourceId,Operand.Role.VALUE_READ,sourceOrigin),read,dest.extent()," ");
            correlate(move.source().id(),sourceId,sourceOrigin,links,items,unit,ids);correlate(move.target().id(),targetId,targetOrigin,links,items,unit,ids);
            return new Operations.Assign(header,destination,fit);
        }
        if(effect.kind()==StorageFacts.MoveKind.FIT_TEXT) {
            var reference=(SpInput.DataReference)move.source();var source=RegionalPlaces.view(data.views().get(reference.binding().selected().orElseThrow()),reference);
            var srcRange=range(source,sourceId,sourceOrigin,ids);var dstRange=range(dest,targetId,targetOrigin,ids);
            var placeId=new OperandId(sourceId.owner(),ids.id("operand","fit-read-place",operation.localId(),reference.id().handle()));
            var readId=new OperandId(sourceId.owner(),ids.id("operand","fit-read",operation.localId(),reference.id().handle()));
            var srcPlace=new Places.RegionSlice(new Operand.Header(placeId,Operand.Role.VALUE_READ,sourceOrigin),source.region(),srcRange.offset(),srcRange.extent(),source.codec(),Types.known(Types.Builtin.TEXT));
            var destination=new Places.RegionSlice(new Operand.Header(targetId,Operand.Role.VALUE_WRITE,targetOrigin),dest.region(),dstRange.offset(),dstRange.extent(),dest.codec(),Types.known(Types.Builtin.TEXT));
            var read=new Expressions.Read(new Operand.Header(readId,Operand.Role.VALUE_READ,sourceOrigin),srcPlace);
            var fit=new Expressions.FitText(new Operand.Header(sourceId,Operand.Role.VALUE_READ,sourceOrigin),read,dest.extent()," ");
            correlate(move.source().id(),sourceId,sourceOrigin,links,items,unit,ids);correlate(move.target().id(),targetId,targetOrigin,links,items,unit,ids);
            return new Operations.Assign(header,destination,fit);
        }
        var range=range(dest,targetId,targetOrigin,ids);
        var place=new Places.RegionSlice(new Operand.Header(targetId,Operand.Role.VALUE_WRITE,targetOrigin),dest.region(),range.offset(),range.extent(),
            Memory.IdentityBytes.INSTANCE,Types.known(Types.Builtin.BYTES));
        correlate(move.target().id(),targetId,targetOrigin,links,items,unit,ids);
        if(effect.kind()==StorageFacts.MoveKind.LITERAL_BYTES||effect.kind()==StorageFacts.MoveKind.FITTED_LITERAL_BYTES) {
            correlate(move.source().id(),sourceId,sourceOrigin,links,items,unit,ids);
            return new Operations.Assign(header,place,new Expressions.Literal(new Operand.Header(sourceId,Operand.Role.VALUE_READ,sourceOrigin),new Values.BytesValue(effect.bytes())));
        }
        throw new IllegalArgumentException("Unsupported promised regional MOVE: "+effect.kind());
    }
    private static Memory.ByteRange range(Memory.ViewBinding view,OperandId parent,OriginId origin,LocalIds ids) {
        return new Memory.ByteRange(view.region(),integer(view.offset(),parent,"offset",origin,ids),integer(view.extent(),parent,"extent",origin,ids));
    }
    private static Expression integer(BigInteger value,OperandId parent,String kind,OriginId origin,LocalIds ids) {
        var id=new OperandId(parent.owner(),ids.id("operand","regional-"+kind,parent.localId(),kind));
        return new Expressions.Literal(new Operand.Header(id,Operand.Role.VALUE_READ,origin),new Values.IntValue(value));
    }
    private static void correlate(SpInput.OperandId source,OperandId target,OriginId origin,List<LoweringResult.OperandLink> links,
            List<Evidence.CoverageItem> items,UnitId unit,LocalIds ids) {
        links.add(new LoweringResult.OperandLink(source,target,origin));
        items.add(ScalarEvidence.item(unit.publication(),"operand",ids.sourceKey(source.handle()),origin,List.of(target)));
    }
}
