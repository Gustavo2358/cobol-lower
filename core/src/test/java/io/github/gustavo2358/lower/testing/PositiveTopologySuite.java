package io.github.gustavo2358.lower.testing;

import io.github.gustavo2358.air.model.*;
import io.github.gustavo2358.lower.application.*;
import io.github.gustavo2358.lower.domain.SpInput;
import io.github.gustavo2358.lower.domain.StorageFacts;
import java.util.*;

/** W1 causal oracles: missing source implementation is coverage, never a foreign effect. */
public final class PositiveTopologySuite {
    private PositiveTopologySuite() { }
    public static int run() {
        var result = new CobolLowerer().lower(CallInputs.create(2, 1, 8, "PROGA", false), ScalarSuite.OPTIONS);
        require(result.status() == LoweringResult.Status.SUCCESS, "supported CALL admitted");
        var publication = result.publication().orElseThrow();
        require(publication.premises().stream().noneMatch(p -> p.assertion() instanceof Proofs.DisjointStorage), "positive bases need no manufactured negative premise");
        var invoke = (Operations.Invoke) publication.units().getFirst().sequences().stream().map(Sequence::terminator).filter(Operations.Invoke.class::isInstance).findFirst().orElseThrow();
        require(invoke.effectBound().otherwise().reads() == Scopes.NoMemory.INSTANCE, "unknown body does not manufacture reads");
        require(invoke.effectBound().otherwise().writes() == Scopes.NoMemory.INSTANCE, "unknown body does not manufacture writes");
        require(invoke.outcomes().known().size() == 1 && invoke.outcomes().remainder() == Scopes.NoControl.INSTANCE, "supported normal continuation has no compensation outcomes");
        require(invoke.target() instanceof Interactions.ComputedTarget t && t.name() instanceof Expressions.Read, "name read remains executable");
        require(publication.uncertainties().stream().anyMatch(u -> u.code().equals("EXTERNAL_EFFECTS_NOT_MODELED")), "omitted body remains coverage");
        var regional = RegionalInputs.group(1);
        var move = (SpInput.MoveFact) regional.statements().getFirst();
        var omitted = IfInputs.with(move, "regionalMove", Optional.of(new StorageFacts.Move(
            StorageFacts.MoveKind.MUST_UNKNOWN, List.of(), List.of("VALUE_TRANSFORM_NOT_IMPLEMENTED"))));
        var statements = new ArrayList<>(regional.statements()); statements.set(0, omitted);
        var partial = new CobolLowerer().lower(IfInputs.with(regional,"statements",statements), ScalarSuite.OPTIONS);
        require(partial.status() == LoweringResult.Status.SUCCESS, "omitted transform keeps regional program admitted");
        var projected = partial.publication().orElseThrow();
        require(projected.storage().size() == 1 && projected.units().getFirst().objects().size() == 2,
            "group/member/base remain represented");
        require(projected.units().getFirst().sequences().stream().flatMap(q -> q.instructions().stream())
            .anyMatch(Operations.Nop.class::isInstance), "omitted transform records an executable identity step");
        require(projected.units().getFirst().sequences().stream().flatMap(q -> q.instructions().stream())
            .noneMatch(Operations.HavocMust.class::isInstance), "omission cannot kill the prior value");
        require(projected.uncertainties().stream().anyMatch(u -> u.code().equals("MOVE_VALUE_NOT_MODELED")),
            "omitted MOVE coverage remains visible");
        System.out.println("LOWER_POSITIVE_TOPOLOGY_TESTS=12");
        return 12;
    }
    private static void require(boolean value, String message) { if (!value) throw new AssertionError("W1 " + message); }
    public static void main(String[] args) { run(); }
}
