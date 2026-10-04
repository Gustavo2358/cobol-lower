package io.github.gustavo2358.lower.application;

import io.github.gustavo2358.air.model.*;
import io.github.gustavo2358.air.model.Ids.*;
import io.github.gustavo2358.lower.domain.SpInput;
import java.util.*;

/** Translate literal/fitted values or whole-item reads; never evaluate dataflow or COBOL fitting. */
final class MoveHandler {
    private static Values.LiteralValue numericLiteral(java.math.BigDecimal value,SpInput.ScalarNumber number) {
        if(number.scale()==0)return new Values.IntValue(value.toBigIntegerExact());
        var normalized=value.setScale(Math.max(0,value.scale()));
        return new Values.DecimalValue(normalized.unscaledValue(),java.math.BigInteger.valueOf(normalized.scale()));
    }
    static Operations.Assign translate(SpInput.MoveFact move, ScalarDataTranslator.Result data, UnitId unit,
            LocalIds ids, SourceOrigins origins, List<LoweringResult.OperandLink> links, List<Evidence.CoverageItem> items,List<Evidence.Uncertainty> uncertainties) {
        var operation = new OperationId(unit, ids.id("operation", "scalar-move-assign", unit.localId(), move.header().id().handle()+(move.numericTransfers().isEmpty()?"":"/"+move.target().id().handle())));
        var owner = new OperationOwner(operation);
        var origin = origins.source("statement", move.header().id().handle(), move.header().provenance());
        var sourceOrigin = origins.source("operand", move.source().id().handle(), move.source().provenance());
        var targetOrigin = origins.source("operand", move.target().id().handle(), move.target().provenance());
        if (move.textAdjustment().isPresent()) {
            var adjustment = move.textAdjustment().orElseThrow();
            var adjustmentOrigin = origins.source("adjustment", move.header().id().handle(), adjustment.provenance());
            sourceOrigin = origins.derived(ids.id("origin", "fitted-literal", operation.localId(), move.source().id().handle()),
                List.of(origin, sourceOrigin, targetOrigin, adjustmentOrigin), (move.copySemantics()==SpInput.CopySemantics.POSSIBLE_TEXT?"cobol-sp@2.32/POSSIBLE_TEXT/":"cobol-sp@1.3/FITTED_TEXT/") + adjustment.rule().name());
            origin = origins.derived(ids.id("origin", "fitted-assign", operation.localId(), move.header().id().handle()),
                List.of(origin, sourceOrigin, targetOrigin, adjustmentOrigin), move.copySemantics()==SpInput.CopySemantics.POSSIBLE_TEXT?"cobol-sp@2.32/possible-nominal-assignment":"cp6-call@1/published-fitted-assignment");
        }
        var source = new OperandId(owner, ids.id("operand", move.source() instanceof SpInput.DataReference ? "scalar-data-source" : "scalar-literal-source", operation.localId(), move.source().id().handle()));
        var target = new OperandId(owner, ids.id("operand", "scalar-object-target", operation.localId(), move.target().id().handle()));
        var destination = new Places.ObjectPlace(new Operand.Header(target, Operand.Role.VALUE_WRITE, targetOrigin), data.index().get(move.copySemantics()==SpInput.CopySemantics.POSSIBLE_TEXT?move.target().logicalWholeItem().orElseThrow():move.target().wholeItemAccess().orElseThrow().data()).object());
        Expression value;
        if (move.source() instanceof SpInput.DataReference read) {
            var placeId = new OperandId(owner, ids.id("operand", "scalar-read-place", operation.localId(), read.id().handle()));
            var place = new Places.ObjectPlace(new Operand.Header(placeId, Operand.Role.VALUE_READ, sourceOrigin),
                data.index().get(read.wholeItemAccess().map(SpInput.WholeItemAccess::data).or(() -> read.logicalWholeItem()).orElseThrow()).object());
            var readHeader=new Operand.Header((move.copySemantics()==SpInput.CopySemantics.FITTED_TEXT||move.copySemantics()==SpInput.CopySemantics.FORMATTED_NUMBER||!move.numericTransfers().isEmpty())
                ?new OperandId(owner,ids.id("operand","fit-text-input",operation.localId(),read.id().handle())):source,Operand.Role.VALUE_READ,sourceOrigin);
            value = new Expressions.Read(readHeader, place);
            if(move.copySemantics()==SpInput.CopySemantics.FITTED_TEXT)
                value=new Expressions.FitText(new Operand.Header(source,Operand.Role.VALUE_READ,sourceOrigin),value,
                    java.math.BigInteger.valueOf(data.logicalTextExtents().get(move.target().wholeItemAccess().orElseThrow().data()))," ");
            if(move.copySemantics()==SpInput.CopySemantics.FORMATTED_NUMBER) {
                var number=data.numbers().get(read.wholeItemAccess().orElseThrow().data());
                var receiver=move.target().wholeItemAccess().orElseThrow().data();
                var formattingOrigin=sourceOrigin;
                value=NumericFormatting.format(value,number,data.formats().get(receiver),data.logicalTextExtents().get(receiver),
                    name->new Operand.Header(name.equals("result")?source:new OperandId(owner,ids.id("operand","numeric-format-"+name,operation.localId(),read.id().handle())),Operand.Role.VALUE_READ,formattingOrigin));
            }
            if(!move.numericTransfers().isEmpty()&&move.copySemantics()!=SpInput.CopySemantics.FORMATTED_NUMBER) {
                var sourceNumber=data.numbers().get(read.wholeItemAccess().orElseThrow().data());
                var number=data.numbers().get(move.target().wholeItemAccess().orElseThrow().data());
                if(sourceNumber==null) {
                    value=NumericMoveControl.parse(value,move,operation,sourceOrigin,unit,ids);
                    sourceNumber=new SpInput.ScalarNumber(1,0,false,"DISPLAY","UNSPECIFIED");
                }
                value=NumericTruncation.open(move,data)
                    ?NumericTruncation.value(number,List.of(value),new Operand.Header(source,Operand.Role.VALUE_READ,sourceOrigin),operation,ids,uncertainties)
                    :NumericExpressions.adjust(value,sourceNumber,number,source,operation,sourceOrigin,ids);
            }
        } else {
            var literal = (SpInput.LiteralSource) move.source();
            if(move.copySemantics()==SpInput.CopySemantics.FORMATTED_NUMBER) {
                var edit=data.formats().get(move.target().wholeItemAccess().orElseThrow().data());
                var literalHeader=new Operand.Header(new OperandId(owner,ids.id("operand","edited-literal-input",operation.localId(),literal.id().handle())),Operand.Role.VALUE_READ,sourceOrigin);
                value=new Expressions.FormatDecimal(new Operand.Header(source,Operand.Role.VALUE_READ,sourceOrigin),
                    NumericFormatting.literal(literal.numericValue().orElseThrow(),edit,literalHeader),NumericFormatting.parts(edit));
            } else if(!move.numericTransfers().isEmpty()&&move.numericTransfers().getFirst().value().isEmpty()) {
                value=NumericTruncation.value(data.numbers().get(move.target().wholeItemAccess().orElseThrow().data()),List.of(),
                    new Operand.Header(source,Operand.Role.VALUE_READ,sourceOrigin),operation,ids,uncertainties);
            } else value = new Expressions.Literal(new Operand.Header(source, Operand.Role.VALUE_READ, sourceOrigin),
                move.numericTransfers().isEmpty()?new Values.TextValue(literal.logicalValue().orElseThrow().value()):numericLiteral(move.numericTransfers().getFirst().value().orElseThrow(),data.numbers().get(move.target().wholeItemAccess().orElseThrow().data())));
        }
        if(move.textAdjustment().isPresent()) {
            var adjustment=move.textAdjustment().orElseThrow();
            // Keep the public source correlation on the outer expression; every nested operand has its own identity.
            var literal=(Expressions.Literal)value;
            var inner=new Expressions.Literal(new Operand.Header(new OperandId(owner,ids.id("operand","fit-input",operation.localId(),move.source().id().handle())),Operand.Role.VALUE_READ,sourceOrigin),literal.value());
            value=new Expressions.FitText(literal.header(),inner,java.math.BigInteger.valueOf(adjustment.receiverExtent()),adjustment.rule()==SpInput.TextAdjustmentRule.ZERO_FILL?"0":" ");
        }
        links.add(new LoweringResult.OperandLink(move.source().id(), source, sourceOrigin));
        links.add(new LoweringResult.OperandLink(move.target().id(), target, targetOrigin));
        items.add(ScalarEvidence.item(unit.publication(), "operand", ids.sourceKey(move.source().id().handle()), sourceOrigin, List.of(source)));
        items.add(ScalarEvidence.item(unit.publication(), "operand", ids.sourceKey(move.target().id().handle()), targetOrigin, List.of(target)));
        var precision=ScalarEvidence.assign(operation);var reasons=List.<UncertaintyId>of();
        if(value instanceof Expressions.Unknown unknown) {
            reasons=List.of(unknown.reason());
            var values=new Evidence.Claim(new Scopes.EntityScope(List.of(operation)),Evidence.PrecisionStatus.OPEN,reasons);
            precision=new Evidence.Precision(precision.control(),precision.storage(),precision.effects(),values,precision.dependencies());
        }
        return new Operations.Assign(new Operations.Header(operation, origin, Evidence.CoverageStatus.MODELED,precision,reasons), destination, value);
    }
}
