package io.github.gustavo2358.lower.application;

import io.github.gustavo2358.air.model.*;
import io.github.gustavo2358.air.model.Ids.*;
import io.github.gustavo2358.lower.domain.SpInput;
import java.util.*;

/** A typed single-receiver MOVE proves a write even when its value conversion is unavailable. */
final class ConservativeMove {
    private ConservativeMove() { }
    static Operations.HavocMust translate(SpInput.MoveFact move,ScalarDataTranslator.Result data,UnitId unit,
            LocalIds ids,SourceOrigins origins,List<LoweringResult.OperandLink> links,List<Evidence.Uncertainty> uncertainties) {
        var id=new OperationId(unit,ids.id("operation","move-unknown-value",unit.localId(),move.header().id().handle()));
        var origin=origins.source("statement",move.header().id().handle(),move.header().provenance());
        var source=origins.source("operand",move.source().id().handle(),move.source().provenance());
        var target=origins.source("operand",move.target().id().handle(),move.target().provenance());
        var reason=new UncertaintyId(unit.publication(),ids.id("uncertainty","move-value",id.localId(),"unknown"));
        var scope=new Scopes.EntityScope(List.of(id));
        uncertainties.add(new Evidence.Uncertainty(reason,"MOVE_VALUE_NOT_MODELED",List.of(Evidence.Dimension.VALUES),scope,
            "Typed MOVE writes the proved whole receiver; source conversion is unknown",source));
        var exact=new Evidence.Claim(scope,Evidence.PrecisionStatus.EXACT,List.of());
        var open=new Evidence.Claim(scope,Evidence.PrecisionStatus.OPEN,List.of(reason));
        var operand=new OperandId(new OperationOwner(id),ids.id("operand","move-must-write",id.localId(),move.target().id().handle()));
        var place=new Places.ObjectPlace(new Operand.Header(operand,Operand.Role.VALUE_WRITE,target),data.index().get(move.target().wholeItemAccess().orElseThrow().data()).object());
        links.add(new LoweringResult.OperandLink(move.target().id(),operand,target));
        return new Operations.HavocMust(new Operations.Header(id,origin,Evidence.CoverageStatus.ABSTRACTED,
            new Evidence.Precision(exact,exact,exact,open,exact),List.of(reason)),place,reason);
    }
}
