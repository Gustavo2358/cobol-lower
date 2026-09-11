package io.github.gustavo2358.lower.testing;

import io.github.gustavo2358.lower.domain.SpInput;
import java.util.*;
import static io.github.gustavo2358.lower.domain.SpInput.*;

/** Handwritten current-contract facts, independent of decoder and lowering. */
public final class CallInputs {
    private CallInputs() { }
    public static SpInput create(int dataCount, int moves, int extent, String value, boolean literal) {
        var base = ScalarInputs.create(dataCount, moves); var unit = base.unit();
        var statements = new ArrayList<StatementFact>();
        var data = base.dataDeclarations().stream().map(d -> new DataFact(d.id(), d.canonicalName(), d.picture(), d.provenance(),
            d.coverage(), d.readiness(), Optional.of(new ScalarText(LogicalDomain.TEXT, extent, StorageClass.WORKING_STORAGE, DeclarationScope.LOCAL)))).toList();
        for (var statement : base.statements()) if (statement instanceof MoveFact m) {
            var source = new LiteralSource(m.source().id(), LiteralKind.ALPHANUMERIC, Optional.of(new LogicalValue(LogicalDomain.TEXT, value, 5)), m.source().provenance());
            statements.add(new MoveFact(m.header(), source, m.target(), extent == 8 ? CopySemantics.FITTED_TEXT : CopySemantics.FULL_IDENTITY,
                m.normalContinuation(), extent == 8 ? Optional.of(new TextAdjustment(TextAdjustmentRule.RIGHT_PAD_SPACE, 8,
                    new LogicalValue(LogicalDomain.TEXT, value.equals("PROGA") ? "PROGA   " : "OTHER   ", 8), m.header().provenance())) : Optional.empty()));
        }
        var old = base.statements().getLast().header();
        var targetId = new OperandId(old.id(), "operand:" + moves + ":0");
        CallTarget target = literal
            ? new LiteralCallTarget(targetId, value, "'" + value + "'", Optional.of(new LogicalValue(LogicalDomain.TEXT, value, value.length())), ScalarInputs.provenance(moves + 100, 12))
            : new DataCallTarget(new DataReference(targetId, OperandRole.CALL_TARGET,
                new Binding(ResolutionStatus.RESOLVED, List.of(data.getFirst().id()), Optional.of(data.getFirst().id())),
                Optional.of(new WholeItemAccess(data.getFirst().id())), ScalarInputs.provenance(moves + 100, 12)));
        var terminalId = new StatementId(unit, "statement:" + (moves + 1));
        statements.add(new CallFact(old, literal ? CallSyntax.LITERAL_PROGRAM_NAME : CallSyntax.IDENTIFIER_OR_EXPRESSION, target,
            RuntimeTargetKnowledge.UNKNOWN, "RUNTIME_UNKNOWN", new NormalContinuation(ContinuationAvailability.KNOWN, Optional.of(terminalId), ScalarInputs.provenance(moves + 100, 25)),
            new CallSurface(ClausePresence.ABSENT, Optional.of(0), ClausePresence.ABSENT, ClausePresence.ABSENT, ClausePresence.ABSENT, ClausePresence.ABSENT), CallEffects.UNKNOWN, CallOutcomes.OPEN));
        statements.add(new GobackFact(new StatementHeader(terminalId, moves + 1, old.containment(), ScalarInputs.provenance(moves + 101, 0),
            CoverageStatus.MODELED, old.readiness()), GobackExit.CURRENT_PROGRAM_INVOCATION, LocalContinuation.NONE));
        return ScalarInputs.replace(base, data, statements);
    }
    public static SpInput call(SpInput input, CallFact replacement) {
        return ScalarInputs.replace(input, input.dataDeclarations(), input.statements().stream().map(s -> s instanceof CallFact ? replacement : s).toList());
    }
    public static CallFact target(CallFact call, CallTarget target) {
        return new CallFact(call.header(), target instanceof LiteralCallTarget ? CallSyntax.LITERAL_PROGRAM_NAME : CallSyntax.IDENTIFIER_OR_EXPRESSION,
            target, call.runtimeTarget(), call.runtimeUncertaintyCode(), call.normalContinuation(), call.surface(), call.effects(), call.outcomes());
    }
}
