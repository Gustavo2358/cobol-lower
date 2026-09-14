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
    static Instruction translate(SpInput.MoveFact move,boolean admittedFitting,ScalarDataTranslator.Result data,UnitId unit,
            LocalIds ids,SourceOrigins origins,List<LoweringResult.OperandLink> links,List<Evidence.CoverageItem> items,
            List<Evidence.Uncertainty> uncertainties) {
        if(move.regionalMove().isEmpty()||move.regionalMove().get().kind()==StorageFacts.MoveKind.UNAVAILABLE||admittedFitting)
            return MoveHandler.translate(move,data,unit,ids,origins,links,items);
        var effect=move.regionalMove().get();var key=move.header().id().handle();
        var operation=new OperationId(unit,ids.id("operation","regional-move",unit.localId(),key));
        var statement=origins.source("statement",key,move.header().provenance());
        var sourceOrigin=origins.source("operand",move.source().id().handle(),move.source().provenance());
        var targetOrigin=origins.source("operand",move.target().id().handle(),move.target().provenance());
        var origin=origins.derived(ids.id("origin","regional-move",unit.localId(),key),List.of(statement,sourceOrigin,targetOrigin),"storage@1/published-byte-effect/"+effect.kind());
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
        var range=range(dest,targetId,targetOrigin,ids);
        var place=new Places.RegionSlice(new Operand.Header(targetId,Operand.Role.VALUE_WRITE,targetOrigin),dest.region(),range.offset(),range.extent(),
            Memory.IdentityBytes.INSTANCE,Types.known(Types.Builtin.BYTES));
        correlate(move.target().id(),targetId,targetOrigin,links,items,unit,ids);
        if(effect.kind()==StorageFacts.MoveKind.LITERAL_BYTES) {
            correlate(move.source().id(),sourceId,sourceOrigin,links,items,unit,ids);
            return new Operations.Assign(header,place,new Expressions.Literal(new Operand.Header(sourceId,Operand.Role.VALUE_READ,sourceOrigin),new Values.BytesValue(effect.bytes())));
        }
        var reason=new UncertaintyId(unit.publication(),ids.id("uncertainty","regional-move-value",operation.localId(),key));
        var scope=new Scopes.EntityScope(List.of(operation));
        uncertainties.add(new Evidence.Uncertainty(reason,"cobol-lower:REGIONAL_MOVE_VALUE_UNKNOWN",List.of(Evidence.Dimension.VALUES),scope,String.join(",",effect.gapCodes()),sourceOrigin));
        var exact=new Evidence.Claim(scope,Evidence.PrecisionStatus.EXACT,List.of());var open=new Evidence.Claim(scope,Evidence.PrecisionStatus.OPEN,List.of(reason));
        return new Operations.HavocMust(new Operations.Header(operation,origin,Evidence.CoverageStatus.ABSTRACTED,
            new Evidence.Precision(exact,exact,exact,open,exact),List.of(reason)),place,reason);
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
