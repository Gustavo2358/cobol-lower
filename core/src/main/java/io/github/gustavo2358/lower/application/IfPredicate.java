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
        return translate(fact.header().id(),fact.predicateGuarantee(),fact.conditionReads(),"if","simple-if@1/published-whole-item-read",
            operation,data,ids,origins,links,items,uncertainties);
    }
    static Expressions.Unknown translate(SpInput.StatementId statement, SpInput.PredicateGuarantee guarantee,
            List<SpInput.DataReference> conditionReads,String role,String rule,OperationId operation,ScalarDataTranslator.Result data,
            LocalIds ids,SourceOrigins origins,List<LoweringResult.OperandLink> links,List<Evidence.CoverageItem> items,List<Evidence.Uncertainty> uncertainties) {
        return translateReads(statement,guarantee.provenance(),guarantee.knownReads(),conditionReads,role,rule,operation,data,ids,origins,links,items,uncertainties);
    }
    static Expressions.Unknown translateReads(SpInput.StatementId statement,SpInput.Provenance provenance,List<SpInput.OperandId> knownReads,
            List<SpInput.DataReference> conditionReads,String role,String rule,OperationId operation,ScalarDataTranslator.Result data,
            LocalIds ids,SourceOrigins origins,List<LoweringResult.OperandLink> links,List<Evidence.CoverageItem> items,List<Evidence.Uncertainty> uncertainties) {
        var owner = new OperationOwner(operation); var key = statement.handle();
        var origin = origins.source(role+"-predicate", key, provenance);
        var operand = new OperandId(owner, ids.id("operand", role+"-predicate", operation.localId(), key));
        var reason = new UncertaintyId(operation.unit().publication(), ids.id("uncertainty", "predicate-value-unknown", operation.localId(), key));
        uncertainties.add(new Evidence.Uncertainty(reason, "predicate-value-unknown", List.of(Evidence.Dimension.VALUES),
            new Scopes.EntityScope(List.of(operand)), "Boolean abstraction of the published branch decision; runtime truth remains unknown.", origin));
        var references = new HashMap<SpInput.OperandId, SpInput.DataReference>();
        conditionReads.forEach(r -> references.put(r.id(), r));
        var dependencies = new ArrayList<Expression>();
        for (var known : knownReads) {
            var reference = references.get(known);
            if (reference == null || reference.wholeItemAccess().isEmpty()) continue;
            var mapping = data.index().get(reference.wholeItemAccess().orElseThrow().data());
            if (mapping == null) continue;
            var source = origins.source(role+"-read", known.handle(), reference.provenance());
            var placeOrigin = origins.derived(ids.id("origin", role+"-read-place", operation.localId(), known.handle()),
                List.of(source, mapping.origin()), rule);
            var readId = new OperandId(owner, ids.id("operand", role+"-read", operation.localId(), known.handle()));
            var placeId = new OperandId(owner, ids.id("operand", role+"-read-place", operation.localId(), known.handle()));
            var place = new Places.ObjectPlace(new Operand.Header(placeId, Operand.Role.VALUE_READ, placeOrigin), mapping.object());
            var read = new Expressions.Read(new Operand.Header(readId, Operand.Role.VALUE_READ, source), place);
            dependencies.add(read);
            links.add(new LoweringResult.OperandLink(known, readId, source)); links.add(new LoweringResult.OperandLink(known, placeId, placeOrigin));
            items.add(ScalarEvidence.item(operation.unit().publication(), "operand", ids.sourceKey(known.handle()), source, List.of(readId, placeId)));
        }
        return new Expressions.Unknown(new Operand.Header(operand, Operand.Role.PREDICATE, origin),
            Types.known(Types.Builtin.BOOL), dependencies, Scopes.NoMemory.INSTANCE, reason);
    }
}
