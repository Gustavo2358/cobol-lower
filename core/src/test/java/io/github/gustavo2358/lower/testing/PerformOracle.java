package io.github.gustavo2358.lower.testing;

import io.github.gustavo2358.air.model.*;
import io.github.gustavo2358.air.model.Ids.*;
import io.github.gustavo2358.lower.application.*;
import io.github.gustavo2358.lower.domain.SpInput;
import java.util.*;
import static io.github.gustavo2358.lower.testing.CallOracle.check;

/** Control oracle follows correlations and explicit targets, never physical sequence order. */
public final class PerformOracle {
    public static void inspect(SpInput input, LoweringResult result) {
        check(result.status() == LoweringResult.Status.SUCCESS, "PERFORM lowers: " + result.admission());
        var p = input.statements().stream().filter(SpInput.PerformFact.class::isInstance).map(SpInput.PerformFact.class::cast).findFirst().orElseThrow();
        var output = result.publication().orElseThrow(); var unit = output.units().getFirst();
        var links = new HashMap<SpInput.StatementId,LoweringResult.StatementLink>(); result.statements().forEach(l -> links.put(l.source(),l));
        var sequences = new HashMap<LabelId,Sequence>(); unit.sequences().forEach(s -> sequences.put(s.label(),s));
        var main = sequences.get(links.get(p.header().id()).label());
        var target = sequences.get(links.get(p.targetEntry().orElseThrow()).label());
        var resume = sequences.get(links.get(p.normalContinuation().statement().orElseThrow()).label());
        check(unit.sequences().size() == 4, "four explicit sequences");
        check(unit.entries().getFirst().initialLabel().orElseThrow().equals(main.label()), "primary entry preserved");
        check(!main.label().equals(target.label()) && !main.label().equals(resume.label()), "distinct target and resume");
        check(main.terminator() instanceof Operations.Jump jump && jump.destination().equals(target.label()), "PERFORM jumps to target before CALL");
        check(target.instructions().size() == p.targetStatements().size(), "complete ordered body");
        for (int i = 0; i < target.instructions().size(); i++)
            check(target.instructions().get(i).header().id().equals(links.get(p.targetStatements().get(i)).target()), "body membership correlation");
        check(target.terminator() instanceof Operations.Jump jump && jump.destination().equals(resume.label()), "body returns to unique resume");
        check(resume.instructions().isEmpty() && resume.terminator() instanceof Operations.Invoke, "CALL belongs only to resume");
        check(unit.sequences().stream().filter(s -> s.terminator() instanceof Operations.Invoke).count() == 1, "PERFORM is not an external invoke");
        check(unit.sequences().stream().filter(s -> s.terminator() instanceof Operations.Return).count() == 1, "primary program return exists");
        check(unit.sequences().stream().allMatch(s -> s.terminator() instanceof Operations.Jump || s.terminator() instanceof Operations.Invoke || s.terminator() instanceof Operations.Return), "no control.local operations");
        check(links.size() == input.statements().size(), "all source statement origins correlated");
    }
}
