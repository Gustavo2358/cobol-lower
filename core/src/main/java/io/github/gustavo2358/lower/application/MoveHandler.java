package io.github.gustavo2358.lower.application;

import io.github.gustavo2358.air.model.*;
import io.github.gustavo2358.air.model.Ids.*;
import io.github.gustavo2358.lower.domain.SpInput;
import java.util.*;

/** Translate literal/fitted values or whole-item reads; never evaluate dataflow or COBOL fitting. */
final class MoveHandler {
    static Operations.Assign translate(SpInput.MoveFact move, ScalarDataTranslator.Result data, UnitId unit,
            LocalIds ids, SourceOrigins origins, List<LoweringResult.OperandLink> links, List<Evidence.CoverageItem> items) {
        var operation = new OperationId(unit, ids.id("operation", "scalar-move-assign", unit.localId(), move.header().id().handle()));
        var owner = new OperationOwner(operation);
        var origin = origins.source("statement", move.header().id().handle(), move.header().provenance());
        var sourceOrigin = origins.source("operand", move.source().id().handle(), move.source().provenance());
        var targetOrigin = origins.source("operand", move.target().id().handle(), move.target().provenance());
        if (move.textAdjustment().isPresent()) {
            var adjustment = move.textAdjustment().orElseThrow();
            var adjustmentOrigin = origins.source("adjustment", move.header().id().handle(), adjustment.provenance());
            sourceOrigin = origins.derived(ids.id("origin", "fitted-literal", operation.localId(), move.source().id().handle()),
                List.of(origin, sourceOrigin, targetOrigin, adjustmentOrigin), "cobol-sp@1.3/FITTED_TEXT/" + adjustment.rule().name());
            origin = origins.derived(ids.id("origin", "fitted-assign", operation.localId(), move.header().id().handle()),
                List.of(origin, sourceOrigin, targetOrigin, adjustmentOrigin), "cp6-call@1/published-fitted-assignment");
        }
        var source = new OperandId(owner, ids.id("operand", move.source() instanceof SpInput.DataReference ? "scalar-data-source" : "scalar-literal-source", operation.localId(), move.source().id().handle()));
        var target = new OperandId(owner, ids.id("operand", "scalar-object-target", operation.localId(), move.target().id().handle()));
        var destination = new Places.ObjectPlace(new Operand.Header(target, Operand.Role.VALUE_WRITE, targetOrigin), data.index().get(move.target().wholeItemAccess().orElseThrow().data()).object());
        Expression value;
        if (move.source() instanceof SpInput.DataReference read) {
            var placeId = new OperandId(owner, ids.id("operand", "scalar-read-place", operation.localId(), read.id().handle()));
            var place = new Places.ObjectPlace(new Operand.Header(placeId, Operand.Role.VALUE_READ, sourceOrigin),
                data.index().get(read.wholeItemAccess().orElseThrow().data()).object());
            value = new Expressions.Read(new Operand.Header(source, Operand.Role.VALUE_READ, sourceOrigin), place);
        } else {
            var literal = (SpInput.LiteralSource) move.source();
            value = new Expressions.Literal(new Operand.Header(source, Operand.Role.VALUE_READ, sourceOrigin),
                new Values.TextValue(move.textAdjustment().map(adjustment -> adjustment.result().value())
                    .orElseGet(() -> literal.logicalValue().orElseThrow().value())));
        }
        links.add(new LoweringResult.OperandLink(move.source().id(), source, sourceOrigin));
        links.add(new LoweringResult.OperandLink(move.target().id(), target, targetOrigin));
        items.add(ScalarEvidence.item(unit.publication(), "operand", move.source().id().handle(), sourceOrigin, List.of(source)));
        items.add(ScalarEvidence.item(unit.publication(), "operand", move.target().id().handle(), targetOrigin, List.of(target)));
        return new Operations.Assign(new Operations.Header(operation, origin, Evidence.CoverageStatus.MODELED,
            ScalarEvidence.assign(operation), List.of()), destination, value);
    }
}
