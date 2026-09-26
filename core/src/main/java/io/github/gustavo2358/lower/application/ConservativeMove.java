package io.github.gustavo2358.lower.application;

import io.github.gustavo2358.air.model.*;
import io.github.gustavo2358.air.model.Ids.*;
import io.github.gustavo2358.lower.domain.SpInput;
import java.util.*;

/** Preserve the statement and provenance while omitting an unimplemented value transformation. */
final class ConservativeMove {
    private ConservativeMove() { }
    static Operations.Nop translate(SpInput.MoveFact move,ScalarDataTranslator.Result data,UnitId unit,
            LocalIds ids,SourceOrigins origins,List<LoweringResult.OperandLink> links,List<Evidence.Uncertainty> uncertainties) {
        var id=new OperationId(unit,ids.id("operation","move-omitted-value",unit.localId(),move.header().id().handle()+"/"+move.target().id().handle()));
        var statement=origins.source("statement",move.header().id().handle(),move.header().provenance());
        var source=origins.source("operand",move.source().id().handle(),move.source().provenance());
        var target=origins.source("operand",move.target().id().handle(),move.target().provenance());
        var origin=origins.derived(ids.id("origin","move-omitted-value",unit.localId(),id.localId()),
            List.of(statement,source,target),"positive-memory@1/omitted-value-transform");
        var reason=new UncertaintyId(unit.publication(),ids.id("uncertainty","move-value",id.localId(),"not-modeled"));
        uncertainties.add(new Evidence.Uncertainty(reason,"MOVE_VALUE_NOT_MODELED",List.of(Evidence.Dimension.VALUES),
            new Scopes.EntityScope(List.of(id)),"The source transformation is outside this projection; no substitute write is modeled.",source));
        return new Operations.Nop(new Operations.Header(id,origin,Evidence.CoverageStatus.ABSTRACTED,
            ScalarEvidence.assign(id),List.of(reason)));
    }
}
