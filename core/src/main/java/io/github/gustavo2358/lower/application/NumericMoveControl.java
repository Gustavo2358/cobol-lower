package io.github.gustavo2358.lower.application;

import io.github.gustavo2358.air.model.*;
import io.github.gustavo2358.air.model.Ids.*;
import io.github.gustavo2358.lower.domain.SpInput;
import java.util.*;

/** Numeric input validity is explicit; shared representation never becomes a proved integer by default. */
final class NumericMoveControl {
    private NumericMoveControl() { }
    static boolean required(SpInput.MoveFact move,ScalarDataTranslator.Result data) {
        if(move.numericTransfers().isEmpty()&&move.copySemantics()!=SpInput.CopySemantics.FORMATTED_NUMBER)return false;
        if(!(move.source() instanceof SpInput.DataReference read)||read.wholeItemAccess().isEmpty())return false;
        var id=read.wholeItemAccess().orElseThrow().data();var link=data.index().get(id);
        return data.logicalTextExtents().containsKey(id)||data.numbers().containsKey(id)&&link!=null&&link.storage().isEmpty();
    }
    private static UncertaintyId reason(SpInput.MoveFact move,UnitId unit,LocalIds ids) {
        return new UncertaintyId(unit.publication(),ids.id("uncertainty","numeric-text-invalid",unit.localId(),move.header().id().handle()));
    }
    static Expression parse(Expression read,SpInput.MoveFact move,OperationId op,OriginId origin,UnitId unit,LocalIds ids) {
        var owner=new OperationOwner(op);
        var fallback=new Expressions.Unknown(new Operand.Header(new OperandId(owner,"invalid-digit-value"),Operand.Role.VALUE_READ,origin),
            Types.known(Types.Builtin.INT),List.of(),Scopes.NoMemory.INSTANCE,reason(move,unit,ids));
        return new Expressions.ParseInteger(new Operand.Header(new OperandId(owner,"parse-digit-value"),Operand.Role.VALUE_READ,origin),read,fallback);
    }
    static List<Sequence> sequences(SpInput.MoveFact move,LabelId entry,LabelId normal,boolean fitted,
            ScalarDataTranslator.Result data,RegionalStorageAdmission.Index storage,UnitId unit,LocalIds ids,SourceOrigins origins,
            List<LoweringResult.OperandLink> links,List<Evidence.CoverageItem> items,List<Evidence.Uncertainty> uncertainties) {
        var key=move.header().id().handle();
        var origin=origins.source("statement",key,move.header().provenance());
        var source=move.source();var sourceOrigin=origins.source("operand",source.id().handle(),source.provenance());
        var guard=new OperationId(unit,ids.id("operation","numeric-text-guard",unit.localId(),key));var owner=new OperationOwner(guard);
        var valid=new LabelId(unit,ids.id("label","numeric-text-valid",unit.localId(),key));
        var invalid=new LabelId(unit,ids.id("label","numeric-text-invalid",unit.localId(),key));
        var sourceId=source instanceof SpInput.DataReference ref?ref.wholeItemAccess().orElseThrow().data():null;
        var object=sourceId==null?null:data.index().get(sourceId).object();
        boolean text=sourceId!=null&&data.logicalTextExtents().containsKey(sourceId);
        var read=object==null?null:new Expressions.Read(new Operand.Header(new OperandId(owner,"read"),Operand.Role.VALUE_READ,sourceOrigin),
            new Places.ObjectPlace(new Operand.Header(new OperandId(owner,"source"),Operand.Role.VALUE_READ,sourceOrigin),object));
        Expression predicate;UncertaintyId validityReason=null;
        if(text)predicate=new Expressions.Unary(new Operand.Header(new OperandId(owner,"digits"),Operand.Role.PREDICATE,sourceOrigin),Expressions.UnaryOperator.IS_DIGITS,read);
        else {
            var reason=new UncertaintyId(unit.publication(),ids.id("uncertainty","numeric-representation-validity",unit.localId(),key));validityReason=reason;
            uncertainties.add(new Evidence.Uncertainty(reason,"NUMERIC_REPRESENTATION_VALIDITY_UNKNOWN",List.of(Evidence.Dimension.CONTROL,Evidence.Dimension.VALUES),
                new Scopes.EntityScope(List.of(guard)),"A shared numeric view can contain a representation written through another view",sourceOrigin));
            predicate=new Expressions.Unknown(new Operand.Header(new OperandId(owner,"valid-numeric-representation"),Operand.Role.PREDICATE,sourceOrigin),
                Types.known(Types.Builtin.BOOL),List.of(),object==null?Scopes.NoMemory.INSTANCE:new Scopes.WithinMemory(new Scopes.ObjectsMemory(List.of(object))),reason);
        }
        if(text)links.add(new LoweringResult.OperandLink(source.id(),read.header().id(),sourceOrigin));
        else links.add(new LoweringResult.OperandLink(source.id(),predicate.header().id(),sourceOrigin));
        if(text)uncertainties.add(new Evidence.Uncertainty(reason(move,unit,ids),"NUMERIC_TEXT_INVALID_VALUE",List.of(Evidence.Dimension.VALUES),
            new Scopes.UnitScope(unit),"Invalid digit text has no invented numeric value",origin));
        var guardPrecision=ScalarEvidence.assign(guard);
        if(validityReason!=null) {
            var claim=new Evidence.Claim(new Scopes.EntityScope(List.of(guard)),Evidence.PrecisionStatus.OPEN,List.of(validityReason));
            guardPrecision=new Evidence.Precision(claim,guardPrecision.storage(),guardPrecision.effects(),claim,guardPrecision.dependencies());
        }
        var branch=new Operations.Branch(new Operations.Header(guard,origin,Evidence.CoverageStatus.MODELED,guardPrecision,validityReason==null?List.of():List.of(validityReason)),predicate,valid,invalid);
        var instructions=RegionalMoveHandler.sequence(move,fitted,data,storage,unit,ids,origins,links,items,uncertainties);
        var jump=PerformSequenceAssembler.jump("numeric-text-normal",move.header().id(),normal,origin,unit,ids);
        var opaque=PartialProgramAssembler.opaque(move,normal,data,unit,ids,origins,uncertainties,links,true,text?"NUMERIC_TEXT_INVALID_SOURCE_BEHAVIOR":"NUMERIC_INVALID_REPRESENTATION_BEHAVIOR");
        var h=opaque.header();var scope=new Scopes.EntityScope(List.of(h.id()));
        var open=new Evidence.Claim(scope,Evidence.PrecisionStatus.OPEN,h.uncertainties());
        var header=new Operations.Header(h.id(),h.origin(),h.coverage(),new Evidence.Precision(open,h.precision().storage(),open,open,open),h.uncertainties());
        var all=new Scopes.WithinMemory(new Scopes.AllMemory(unit.publication(),true));
        // MOVE has no source label destination. Invalid numeric data can continue,
        // escape or fail; it does not synthesize a GO TO into arbitrary local PERFORMs.
        var failure=new Operations.Opaque(header,text?"invalid-numeric-text":"invalid-numeric-representation",opaque.knownOperands(),List.of(),new Envelopes.Envelope(
            new Envelopes.MemoryEnvelope(List.of(),all,List.of(),all,List.of()),
            new Control.ControlEnvelope(List.of(new Control.JumpAlternative(normal),
                new Control.AnyException(Control.Propagate.INSTANCE),Control.HaltAlternative.INSTANCE),
                new Scopes.WithinControl(new Scopes.UnitControl(unit,false,false,false,false,true,true))),
            new Envelopes.DependencyEnvelope(List.of(),Scopes.AnyResource.INSTANCE)));
        return List.of(new Sequence(entry,List.of(),branch,origin),new Sequence(valid,instructions,jump,origin),new Sequence(invalid,List.of(),failure,origin));
    }
}
