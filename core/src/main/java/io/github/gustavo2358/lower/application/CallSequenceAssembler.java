package io.github.gustavo2358.lower.application;

import io.github.gustavo2358.air.model.*;
import io.github.gustavo2358.air.model.Ids.*;
import java.util.*;

/** A CALL terminates the current sequence; its explicit SP continuation owns the Return. */
final class CallSequenceAssembler {
    record Assembly(List<Sequence> sequences, LabelId entryLabel, OriginId entrySequenceOrigin) { }
    static Assembly assemble(CallAdmission.Plan plan, ScalarDataTranslator.Result data, UnitId unit, OriginId entryOrigin,
            LocalIds ids, SourceOrigins origins, List<LoweringResult.StatementLink> statements,
            List<LoweringResult.OperandLink> operands, List<Evidence.CoverageItem> items, List<Evidence.Uncertainty> uncertainties) {
        var call = plan.call().orElseThrow(); var goback = plan.terminal().orElseThrow();
        var start = plan.admission().input().orElseThrow().entryInventory().entries().getFirst().start().statement().orElseThrow();
        var label = new LabelId(unit, ids.id("label", "call-sequence", unit.localId(), start.handle()));
        // The key is the explicit reference, never a program point or list index.
        var successor = new LabelId(unit, ids.id("label", "call-sequence", unit.localId(), call.normalContinuation().statement().orElseThrow().handle()));
        var instructions = new ArrayList<Instruction>(); var inputs = new ArrayList<OriginId>(); inputs.add(entryOrigin);
        for (var move : plan.moves()) {
            var assign = MoveHandler.translate(move, data, unit, ids, origins, operands, items);
            instructions.add(assign); inputs.add(assign.header().origin());
            inputs.add(origins.source("continuation", move.header().id().handle(), move.normalContinuation().provenance()));
            statements.add(new LoweringResult.StatementLink(move.header().id(), assign.header().id(), label, assign.header().origin()));
            items.add(ScalarEvidence.item(unit.publication(), "statement", move.header().id().handle(), assign.header().origin(), List.of(assign.header().id(), label)));
        }
        var continuation = origins.source("continuation", call.header().id().handle(), call.normalContinuation().provenance());
        var invoke = InvokeHandler.translate(call, data.index(), successor, continuation, unit, ids, origins, operands, items, uncertainties);
        inputs.add(invoke.header().origin()); inputs.add(continuation);
        statements.add(new LoweringResult.StatementLink(call.header().id(), invoke.header().id(), label, invoke.header().origin()));
        items.add(new Evidence.CoverageItem("sp-call@1/" + unit.publication().localId() + "/" + call.header().id().handle(),
            invoke.header().origin(), Evidence.CoverageStatus.ABSTRACTED, List.of(invoke.header().id(), label), invoke.header().uncertainties(), Optional.empty()));
        var terminal = GobackHandler.translate(goback, unit, ids, origins, uncertainties);
        statements.add(new LoweringResult.StatementLink(goback.header().id(), terminal.header().id(), successor, terminal.header().origin()));
        items.add(ScalarEvidence.item(unit.publication(), "statement", goback.header().id().handle(), terminal.header().origin(), List.of(terminal.header().id(), successor)));
        var origin = origins.derived(ids.id("origin", "call-sequence", unit.localId(), label.localId()), inputs, "cp6-call@1/explicit-chain-to-invoke");
        var returnOrigin = origins.derived(ids.id("origin", "call-sequence", unit.localId(), successor.localId()),
            List.of(terminal.header().origin(), continuation), "cp6-call@1/explicit-normal-continuation");
        return new Assembly(List.of(new Sequence(label, instructions, invoke, origin), new Sequence(successor, List.of(), terminal, returnOrigin)), label, origin);
    }
}
