package io.github.gustavo2358.lower.application;

import io.github.gustavo2358.air.model.*;
import io.github.gustavo2358.air.model.Ids.*;
import io.github.gustavo2358.lower.domain.SpInput;
import java.util.*;

/** Composes existing handlers at explicit control boundaries. MOVE stays an instruction. */
final class SupportedProgramAssembler {
    record Assembly(List<Sequence> sequences, LabelId entryLabel, OriginId entrySequenceOrigin) { }
    static Assembly assemble(SupportedProgramAdmission.Plan plan, ScalarDataTranslator.Result data, UnitId unit, OriginId entryOrigin,
            LocalIds ids, SourceOrigins origins, List<LoweringResult.StatementLink> statements,
            List<LoweringResult.OperandLink> operands, List<Evidence.CoverageItem> items, List<Evidence.Uncertainty> uncertainties) {
        var start = plan.primary().getFirst().header().id(); var entry = label(start, unit, ids);
        var sequences = new ArrayList<Sequence>(); var instructions = new ArrayList<Instruction>();
        var inputs = new ArrayList<OriginId>(List.of(entryOrigin)); var current = entry;
        OriginId entrySequenceOrigin = null;
        for (var fact : plan.primary()) {
            if (fact instanceof SpInput.MoveFact move) {
                var assign = MoveHandler.translate(move, data, unit, ids, origins, operands, items);
                instructions.add(assign); inputs.add(assign.header().origin());
                inputs.add(origins.source("continuation", move.header().id().handle(), move.normalContinuation().provenance()));
                link(move.header().id(), assign, current, statements, items); continue;
            }
            var next = SupportedProgramAdmission.next(fact);
            var continuation = next == null ? null : label(next.statement().orElseThrow(), unit, ids);
            Terminator terminator;
            if (fact instanceof SpInput.CallFact call) {
                var completion = origins.source("continuation", call.header().id().handle(), call.normalContinuation().provenance());
                terminator = InvokeHandler.translate(call, data.index(), continuation, completion, unit, ids, origins, operands, items, uncertainties);
                inputs.add(completion);
            } else if (fact instanceof SpInput.IfFact f) {
                var diamond = plan.diamonds().get(f.header().id());
                var thenLabel = label(f.thenArm().entry().statement().orElseThrow(), unit, ids);
                var elseLabel = f.elseArm().entry().statement().map(id -> label(id, unit, ids)).orElse(continuation);
                terminator = IfSequenceAssembler.branch(f, thenLabel, elseLabel, data, unit, ids, origins, operands, items, uncertainties);
                var completion = origins.source("if-completion", f.header().id().handle(), f.normalContinuation().provenance());
                inputs.add(completion);
                sequences.add(IfSequenceAssembler.arm(f, f.thenArm(), diamond.thenMoves(), thenLabel, continuation, completion, data, unit, ids, origins, statements, operands, items));
                if (f.elseArm().presence() == SpInput.ClausePresence.PRESENT)
                    sequences.add(IfSequenceAssembler.arm(f, f.elseArm(), diamond.elseMoves(), elseLabel, continuation, completion, data, unit, ids, origins, statements, operands, items));
            } else if (fact instanceof SpInput.PerformFact p) {
                var target = label(p.targetEntry().orElseThrow(), unit, ids);
                var proof = PerformSequenceAssembler.control(p, target, continuation, unit, ids, origins);
                terminator = proof.invoke();
                var bodyOrigins = new ArrayList<>(List.of(proof.paragraphOrigin(), proof.performOrigin(), proof.referenceOrigin(), proof.resumeOrigin()));
                var body = PerformSequenceAssembler.moves(plan.body(), target, data, unit, ids, origins, statements, operands, items, bodyOrigins);
                var returnOrigin = origins.derived(ids.id("origin", "perform-return", unit.localId(), p.header().id().handle()), bodyOrigins,
                    "perform-basic@1/isolated-paragraph-end-to-unique-resume");
                var returning = PerformSequenceAssembler.jump("resume", p.targetExit().orElseThrow(), continuation, returnOrigin, unit, ids);
                sequences.add(new Sequence(target, body, returning, returnOrigin));
            } else terminator = GobackHandler.translate((SpInput.GobackFact) fact, unit, ids, origins, uncertainties);
            link(fact.header().id(), terminator, current, statements, items); inputs.add(terminator.header().origin());
            var sequenceOrigin = origins.derived(ids.id("origin", "program-sequence", unit.localId(), current.localId()), inputs,
                "supported-cp6-program@1/explicit-control");
            if (current.equals(entry)) entrySequenceOrigin = sequenceOrigin;
            sequences.add(new Sequence(current, List.copyOf(instructions), terminator, sequenceOrigin));
            instructions.clear(); inputs.clear();
            if (next != null) inputs.add(origins.source("flow-continuation", fact.header().id().handle(), next.provenance()));
            current = continuation;
        }
        // Transport inventory deliberately differs from executable order.
        Collections.reverse(sequences);
        return new Assembly(List.copyOf(sequences), entry, Objects.requireNonNull(entrySequenceOrigin));
    }
    private static LabelId label(SpInput.StatementId source, UnitId unit, LocalIds ids) {
        return new LabelId(unit, ids.id("label", "program-sequence", unit.localId(), source.handle()));
    }
    private static void link(SpInput.StatementId source, Operation operation, LabelId label,
            List<LoweringResult.StatementLink> statements, List<Evidence.CoverageItem> items) {
        var h = operation.header(); statements.add(new LoweringResult.StatementLink(source, h.id(), label, h.origin()));
        items.add(new Evidence.CoverageItem("sp-program@1/" + h.id().unit().publication().localId() + "/" + source.handle(),
            h.origin(), h.coverage(), List.of(h.id(), label), h.uncertainties(), Optional.empty()));
    }
}
