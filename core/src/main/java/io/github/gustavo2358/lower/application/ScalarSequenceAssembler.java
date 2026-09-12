package io.github.gustavo2358.lower.application;

import io.github.gustavo2358.air.model.*;
import io.github.gustavo2358.air.model.Ids.*;
import java.util.*;

/** The plan is ordered exclusively by Entry.start and explicit continuation references. */
final class ScalarSequenceAssembler {
    static Sequence assemble(ScalarMoveAdmission.Plan plan, ScalarDataTranslator.Result data, UnitId unit, OriginId entryOrigin,
            LocalIds ids, SourceOrigins origins, List<LoweringResult.StatementLink> statements,
            List<LoweringResult.OperandLink> operands, List<Evidence.CoverageItem> items, List<Evidence.Uncertainty> uncertainties) {
        var label = new LabelId(unit, ids.id("label", "scalar-linear-sequence", unit.localId(), plan.moves().getFirst().header().id().handle()));
        var instructions = new ArrayList<Instruction>(); var inputs = new ArrayList<OriginId>(); inputs.add(entryOrigin);
        for (var move : plan.moves()) {
            var assign = MoveHandler.translate(move, data, unit, ids, origins, operands, items);
            instructions.add(assign); inputs.add(assign.header().origin());
            inputs.add(origins.source("continuation", move.header().id().handle(), move.normalContinuation().provenance()));
            statements.add(new LoweringResult.StatementLink(move.header().id(), assign.header().id(), label, assign.header().origin()));
            items.add(ScalarEvidence.item(unit.publication(), "statement", move.header().id().handle(), assign.header().origin(), List.of(assign.header().id(), label)));
        }
        var goback = plan.terminal().orElseThrow(); var terminal = GobackHandler.translate(goback, unit, ids, origins, uncertainties);
        inputs.add(terminal.header().origin());
        statements.add(new LoweringResult.StatementLink(goback.header().id(), terminal.header().id(), label, terminal.header().origin()));
        items.add(ScalarEvidence.item(unit.publication(), "statement", goback.header().id().handle(), terminal.header().origin(), List.of(terminal.header().id(), label)));
        var origin = origins.derived(ids.id("origin", "scalar-sequence", unit.localId(), label.localId()), inputs, "scalar-text-move@1/explicit-linear-chain");
        return new Sequence(label, instructions, terminal, origin);
    }
}
