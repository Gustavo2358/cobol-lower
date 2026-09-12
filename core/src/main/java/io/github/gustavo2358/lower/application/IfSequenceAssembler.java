package io.github.gustavo2358.lower.application;

import io.github.gustavo2358.air.model.*;
import io.github.gustavo2358.air.model.Ids.*;
import io.github.gustavo2358.lower.domain.SpInput;
import java.util.*;

/** Explicit diamond joins from admitted SP identities and canonical arm children. */
final class IfSequenceAssembler {
    record Assembly(List<Sequence> sequences, LabelId entryLabel, OriginId entrySequenceOrigin) { }
    static Assembly assemble(IfAdmission.Plan plan, ScalarDataTranslator.Result data, UnitId unit, OriginId entryOrigin,
            LocalIds ids, SourceOrigins origins, List<LoweringResult.StatementLink> statements,
            List<LoweringResult.OperandLink> operands, List<Evidence.CoverageItem> items, List<Evidence.Uncertainty> uncertainties) {
        var f = plan.branch().orElseThrow(); var call = plan.call().orElseThrow(); var goback = plan.terminal().orElseThrow();
        var entry = label("root", f.header().id(), unit, ids);
        var merge = label("call", f.normalContinuation().statement().orElseThrow(), unit, ids);
        var returning = label("return", call.normalContinuation().statement().orElseThrow(), unit, ids);
        var thenLabel = label("then", f.thenArm().entry().statement().orElseThrow(), unit, ids);
        var elseLabel = f.elseArm().entry().statement().map(id -> label("else", id, unit, ids)).orElse(merge);
        var completion = origins.source("if-completion", f.header().id().handle(), f.normalContinuation().provenance());
        var origin = origins.source("statement", f.header().id().handle(), f.header().provenance());
        var operation = new OperationId(unit, ids.id("operation", "if-branch", unit.localId(), f.header().id().handle()));
        var predicate = IfPredicate.translate(f, operation, data, ids, origins, operands, items, uncertainties);
        var exact = new Evidence.Claim(new Scopes.EntityScope(List.of(operation)), Evidence.PrecisionStatus.EXACT, List.of());
        var values = new Evidence.Claim(new Scopes.EntityScope(List.of(predicate.header().id())), Evidence.PrecisionStatus.OPEN, List.of(predicate.reason()));
        var branch = new Operations.Branch(new Operations.Header(operation, origin, Evidence.CoverageStatus.ABSTRACTED,
            new Evidence.Precision(exact, exact, exact, values, exact), List.of(predicate.reason())), predicate, thenLabel, elseLabel);
        link(f.header().id(), branch, entry, statements, items);
        var entrySequenceOrigin = origins.derived(ids.id("origin", "if-sequence", unit.localId(), entry.localId()),
            List.of(entryOrigin, origin, completion), "simple-if@1/explicit-entry");
        var sequences = new ArrayList<Sequence>();
        var continuation = origins.source("continuation", call.header().id().handle(), call.normalContinuation().provenance());
        var terminal = GobackHandler.translate(goback, unit, ids, origins, uncertainties);
        link(goback.header().id(), terminal, returning, statements, items);
        var returnOrigin = origins.derived(ids.id("origin", "if-sequence", unit.localId(), returning.localId()),
            List.of(terminal.header().origin(), continuation), "simple-if@1/explicit-call-normal-continuation");
        // Serialization inventory order has no control meaning. Entry is returned explicitly.
        sequences.add(new Sequence(returning, List.of(), terminal, returnOrigin));
        sequences.add(arm(f, f.thenArm(), plan.thenMoves(), thenLabel, merge, completion, data, unit, ids, origins, statements, operands, items));
        if (f.elseArm().presence() == SpInput.ClausePresence.PRESENT)
            sequences.add(arm(f, f.elseArm(), plan.elseMoves(), elseLabel, merge, completion, data, unit, ids, origins, statements, operands, items));
        var invoke = InvokeHandler.translate(call, data.index(), returning, continuation, unit, ids, origins, operands, items, uncertainties);
        link(call.header().id(), invoke, merge, statements, items);
        var mergeOrigin = origins.derived(ids.id("origin", "if-sequence", unit.localId(), merge.localId()),
            List.of(invoke.header().origin(), completion, continuation), "simple-if@1/explicit-merge-call");
        sequences.add(new Sequence(merge, List.of(), invoke, mergeOrigin));
        sequences.add(new Sequence(entry, List.of(), branch, entrySequenceOrigin));
        return new Assembly(List.copyOf(sequences), entry, entrySequenceOrigin);
    }
    private static LabelId label(String role, SpInput.StatementId source, UnitId unit, LocalIds ids) {
        return new LabelId(unit, ids.id("label", "if-" + role, unit.localId(), source.handle()));
    }
    private static Sequence arm(SpInput.IfFact owner, SpInput.IfArm arm, List<SpInput.MoveFact> moves, LabelId label, LabelId merge,
            OriginId completion, ScalarDataTranslator.Result data, UnitId unit, LocalIds ids, SourceOrigins origins,
            List<LoweringResult.StatementLink> statements, List<LoweringResult.OperandLink> operands, List<Evidence.CoverageItem> items) {
        var key = arm.entry().statement().orElseThrow().handle();
        var armOrigin = origins.source("if-arm", key, arm.provenance());
        var inputs = new ArrayList<OriginId>(); inputs.add(armOrigin); inputs.add(completion);
        var instructions = new ArrayList<Instruction>();
        for (var move : moves) {
            var assign = MoveHandler.translate(move, data.index().get(move.target().wholeItemAccess().orElseThrow().data()), unit, ids, origins, operands, items);
            instructions.add(assign); inputs.add(assign.header().origin());
            inputs.add(origins.source("continuation", move.header().id().handle(), move.normalContinuation().provenance()));
            link(move.header().id(), assign, label, statements, items);
        }
        var origin = origins.derived(ids.id("origin", "if-arm-completion", unit.localId(), key), inputs, "simple-if@1/arm-normal-completion");
        var id = new OperationId(unit, ids.id("operation", "if-arm-jump", owner.header().id().handle(), key));
        var jump = new Operations.Jump(new Operations.Header(id, origin, Evidence.CoverageStatus.MODELED, ScalarEvidence.assign(id), List.of()), merge);
        return new Sequence(label, instructions, jump, origin);
    }
    private static void link(SpInput.StatementId source, Operation operation, LabelId label,
            List<LoweringResult.StatementLink> statements, List<Evidence.CoverageItem> items) {
        var h = operation.header(); statements.add(new LoweringResult.StatementLink(source, h.id(), label, h.origin()));
        items.add(new Evidence.CoverageItem("sp-if@1/" + h.id().unit().publication().localId() + "/" + source.handle(),
            h.origin(), h.coverage(), List.of(h.id(), label), h.uncertainties(), Optional.empty()));
    }
}
