package io.github.gustavo2358.lower.application;

import io.github.gustavo2358.air.model.*;
import io.github.gustavo2358.air.model.Ids.*;
import io.github.gustavo2358.lower.domain.SpInput;
import java.util.*;

/** Mechanical BOOLEAN/PURE/TOTAL/COMPLETE projection; runtime truth is never evaluated. */
final class IfPredicate {
    static Expressions.Unknown translate(SpInput.IfFact fact, OperationId operation, ScalarDataTranslator.Result data,
            LocalIds ids, SourceOrigins origins, List<LoweringResult.OperandLink> links,
            List<Evidence.CoverageItem> items, List<Evidence.Uncertainty> uncertainties) {
        var owner = new OperationOwner(operation); var key = fact.header().id().handle();
        var origin = origins.source("if-predicate", key, fact.predicateGuarantee().provenance());
        var operand = new OperandId(owner, ids.id("operand", "if-predicate", operation.localId(), key));
        var reason = new UncertaintyId(operation.unit().publication(), ids.id("uncertainty", "predicate-value-unknown", operation.localId(), key));
        uncertainties.add(new Evidence.Uncertainty(reason, "predicate-value-unknown", List.of(Evidence.Dimension.VALUES),
            new Scopes.EntityScope(List.of(operand)), "Published total pure Boolean predicate; truth value is unknown.", origin));
        var references = new HashMap<SpInput.OperandId, SpInput.DataReference>();
        fact.conditionReads().forEach(r -> references.put(r.id(), r));
        var dependencies = new ArrayList<Expression>();
        for (var known : fact.predicateGuarantee().knownReads()) {
            var reference = references.get(known); var mapping = data.index().get(reference.wholeItemAccess().orElseThrow().data());
            var source = origins.source("if-read", known.handle(), reference.provenance());
            var placeOrigin = origins.derived(ids.id("origin", "if-read-place", operation.localId(), known.handle()),
                List.of(source, mapping.origin()), "simple-if@1/published-whole-item-read");
            var readId = new OperandId(owner, ids.id("operand", "if-read", operation.localId(), known.handle()));
            var placeId = new OperandId(owner, ids.id("operand", "if-read-place", operation.localId(), known.handle()));
            var place = new Places.ObjectPlace(new Operand.Header(placeId, Operand.Role.VALUE_READ, placeOrigin), mapping.object());
            var read = new Expressions.Read(new Operand.Header(readId, Operand.Role.VALUE_READ, source), place);
            dependencies.add(read);
            links.add(new LoweringResult.OperandLink(known, readId, source)); links.add(new LoweringResult.OperandLink(known, placeId, placeOrigin));
            items.add(ScalarEvidence.item(operation.unit().publication(), "operand", known.handle(), source, List.of(readId, placeId)));
        }
        return new Expressions.Unknown(new Operand.Header(operand, Operand.Role.PREDICATE, origin),
            Types.known(Types.Builtin.BOOL), dependencies, Scopes.NoMemory.INSTANCE, reason);
    }
}
