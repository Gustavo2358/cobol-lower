package io.github.gustavo2358.lower.application;

import io.github.gustavo2358.air.model.*;
import io.github.gustavo2358.air.model.Ids.*;
import io.github.gustavo2358.lower.domain.SpInput;
import java.util.*;

/** Exact direct transfers for an admitted constant local continuation. */
final class PerformSequenceAssembler {
    record Assembly(List<Sequence> sequences, LabelId entryLabel, OriginId entrySequenceOrigin) { }
    static Assembly assemble(PerformAdmission.Plan plan, ScalarDataTranslator.Result data, UnitId unit, OriginId entryOrigin,
            LocalIds ids, SourceOrigins origins, List<LoweringResult.StatementLink> statements,
            List<LoweringResult.OperandLink> operands, List<Evidence.CoverageItem> items, List<Evidence.Uncertainty> uncertainties) {
        var p = plan.perform().orElseThrow(); var call = plan.call().orElseThrow(); var goback = plan.terminal().orElseThrow();
        var start = plan.admission().input().orElseThrow().entryInventory().entries().getFirst().start().statement().orElseThrow();
        var main = label(start, unit, ids); var target = label(p.targetEntry().orElseThrow(), unit, ids);
        var resume = label(p.normalContinuation().statement().orElseThrow(), unit, ids);
        var end = label(call.normalContinuation().statement().orElseThrow(), unit, ids);
        var performOrigin = origins.source("statement", p.header().id().handle(), p.header().provenance());
        var referenceOrigin = origins.source("perform-reference", p.header().id().handle(), p.target().orElseThrow().referenceOrigin());
        var paragraphOrigin = origins.source("paragraph", p.target().orElseThrow().id().handle(), p.target().orElseThrow().paragraphOrigin());
        var resumeOrigin = origins.source("perform-continuation", p.header().id().handle(), p.normalContinuation().provenance());
        var invokeOrigin = origins.derived(ids.id("origin", "perform-jump", unit.localId(), p.header().id().handle()),
            List.of(performOrigin, referenceOrigin, paragraphOrigin), "perform-basic@1/unique-resolved-target");
        var jump = jump("perform", p.header().id(), target, invokeOrigin, unit, ids);
        link(p.header().id(), jump, main, statements, items);
        var mainOrigins = new ArrayList<OriginId>(List.of(entryOrigin, invokeOrigin));
        var prefix = moves(plan.prefix(), main, data, unit, ids, origins, statements, operands, items, mainOrigins);
        var mainOrigin = origins.derived(ids.id("origin", "perform-main", unit.localId(), start.handle()), mainOrigins, "perform-basic@1/explicit-primary-entry");
        var bodyOrigins = new ArrayList<OriginId>(List.of(paragraphOrigin, performOrigin, referenceOrigin, resumeOrigin));
        var body = moves(plan.body(), target, data, unit, ids, origins, statements, operands, items, bodyOrigins);
        var returnOrigin = origins.derived(ids.id("origin", "perform-return", unit.localId(), p.header().id().handle()), bodyOrigins,
            "perform-basic@1/isolated-paragraph-end-to-unique-resume");
        var returning = jump("resume", p.targetExit().orElseThrow(), resume, returnOrigin, unit, ids);
        var continuation = origins.source("continuation", call.header().id().handle(), call.normalContinuation().provenance());
        var invoke = InvokeHandler.translate(call, data.index(), end, continuation, unit, ids, origins, operands, items, uncertainties);
        link(call.header().id(), invoke, resume, statements, items);
        var callOrigin = origins.derived(ids.id("origin", "perform-call", unit.localId(), call.header().id().handle()),
            List.of(invoke.header().origin(), resumeOrigin, continuation), "perform-basic@1/resumed-call");
        var terminal = GobackHandler.translate(goback, unit, ids, origins, uncertainties);
        link(goback.header().id(), terminal, end, statements, items);
        // Deliberately place primary entry last in transport inventory.
        return new Assembly(List.of(new Sequence(end, List.of(), terminal, terminal.header().origin()),
            new Sequence(target, body, returning, returnOrigin), new Sequence(resume, List.of(), invoke, callOrigin),
            new Sequence(main, prefix, jump, mainOrigin)), main, mainOrigin);
    }
    private static List<Instruction> moves(List<SpInput.MoveFact> moves, LabelId label, ScalarDataTranslator.Result data, UnitId unit,
            LocalIds ids, SourceOrigins origins, List<LoweringResult.StatementLink> statements, List<LoweringResult.OperandLink> operands,
            List<Evidence.CoverageItem> items, List<OriginId> inputs) {
        var result = new ArrayList<Instruction>();
        for (var move : moves) {
            var assign = MoveHandler.translate(move, data, unit, ids, origins, operands, items);
            result.add(assign); inputs.add(assign.header().origin());
            inputs.add(origins.source("continuation", move.header().id().handle(), move.normalContinuation().provenance()));
            link(move.header().id(), assign, label, statements, items);
        }
        return List.copyOf(result);
    }
    private static Operations.Jump jump(String role, SpInput.StatementId source, LabelId destination, OriginId origin, UnitId unit, LocalIds ids) {
        var id = new OperationId(unit, ids.id("operation", "perform-" + role, unit.localId(), source.handle()));
        return new Operations.Jump(new Operations.Header(id, origin, Evidence.CoverageStatus.MODELED, ScalarEvidence.assign(id), List.of()), destination);
    }
    private static LabelId label(SpInput.StatementId source, UnitId unit, LocalIds ids) {
        return new LabelId(unit, ids.id("label", "perform-sequence", unit.localId(), source.handle()));
    }
    private static void link(SpInput.StatementId source, Operation operation, LabelId label,
            List<LoweringResult.StatementLink> statements, List<Evidence.CoverageItem> items) {
        var h = operation.header(); statements.add(new LoweringResult.StatementLink(source, h.id(), label, h.origin()));
        items.add(new Evidence.CoverageItem("sp-perform@1/" + h.id().unit().publication().localId() + "/" + source.handle(),
            h.origin(), h.coverage(), List.of(h.id(), label), h.uncertainties(), Optional.empty()));
    }
}
