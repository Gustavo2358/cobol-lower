package io.github.gustavo2358.lower.application;

import io.github.gustavo2358.air.model.*;
import io.github.gustavo2358.air.model.Ids.*;
import io.github.gustavo2358.lower.domain.SpInput;
import java.util.*;

/** A proved conversion can have an unproved result; never select an absent TRUNC option. */
final class NumericTruncation {
    private NumericTruncation() { }
    static boolean open(SpInput.MoveFact move,ScalarDataTranslator.Result data) {
        var targets=new HashMap<SpInput.OperandId,SpInput.DataReference>();targets.put(move.target().id(),move.target());
        move.additionalTransfers().forEach(t->targets.put(t.target().id(),t.target()));
        for(var proof:move.numericTransfers()) {
            var receiver=targets.get(proof.target()).wholeItemAccess().orElseThrow().data();var number=data.numbers().get(receiver);
            if(number==null||!NumericMoveAdmission.requiresFit(number))continue;
            if(move.source() instanceof SpInput.LiteralSource) {if(proof.value().isEmpty())return true;continue;}
            var source=((SpInput.DataReference)move.source()).wholeItemAccess().orElseThrow().data();var descriptor=data.numbers().get(source);
            if(descriptor!=null) {if(!NumericMoveAdmission.fits(NumericMoveAdmission.maximum(descriptor),number))return true;}
            else if((long)data.logicalTextExtents().get(source)+number.scale()>number.digits())return true;
        }
        return false;
    }
    static Expressions.Unknown value(SpInput.ScalarNumber number,List<Expression> dependencies,Operand.Header header,
            OperationId operation,LocalIds ids,List<Evidence.Uncertainty> uncertainties) {
        var reason=new UncertaintyId(operation.publication(),ids.id("uncertainty","numeric-truncation-result",operation.unit().localId(),operation.localId()));
        uncertainties.add(new Evidence.Uncertainty(reason,"BINARY_TRUNCATION_RESULT_UNKNOWN",List.of(Evidence.Dimension.VALUES),
            new Scopes.EntityScope(List.of(operation)),"The receiving BINARY descriptor does not prove a common STD/OPT/BIN result",header.origin()));
        return new Expressions.Unknown(header,Types.known(number.scale()==0?Types.Builtin.INT:Types.Builtin.DECIMAL),dependencies,Scopes.NoMemory.INSTANCE,reason);
    }
}
