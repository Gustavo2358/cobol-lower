package io.github.gustavo2358.lower.testing;

import io.github.gustavo2358.lower.application.*;
import io.github.gustavo2358.lower.domain.SpInput;
import java.util.*;
import static io.github.gustavo2358.lower.domain.SpInput.*;
import static io.github.gustavo2358.lower.testing.CallOracle.check;

public final class CallSuite {
    private CallSuite() { }
    private static LoweringResult lower(SpInput input) { return new CobolLowerer().lower(input, ScalarSuite.OPTIONS); }
    public static int run() {
        int before = CallOracle.assertions();
        for (int extent : List.of(5, 8)) for (String value : List.of("PROGA", "OTHER")) for (int count : List.of(0, 1, 3)) {
            var input = CallInputs.create(2, count, extent, value, false);
            var expected = Collections.nCopies(count, extent == 5 ? value : value.equals("PROGA") ? "PROGA   " : "OTHER   ");
            CallOracle.inspect(input, lower(input), expected, true, "unused");
        }
        var literal = CallInputs.create(0, 0, 5, "PROGA", true);
        CallOracle.inspect(literal, lower(literal), List.of(), false, "PROGA");
        var raw = CallInputs.create(0, 0, 5, " ProGa ", true);
        CallOracle.inspect(raw, lower(raw), List.of(), false, " ProGa ");
        var a = CallInputs.create(2, 1, 8, "PROGA", false); var result = lower(a);
        var call = a.statements().stream().filter(CallFact.class::isInstance).map(CallFact.class::cast).findFirst().orElseThrow();
        var reversed = new ArrayList<>(a.statements()); Collections.reverse(reversed);
        var reverseData = new ArrayList<>(a.dataDeclarations()); Collections.reverse(reverseData);
        check(lower(ScalarInputs.replace(a, reverseData, reversed)).publication().equals(result.publication()), "physical order does not choose continuation");
        check(lower(a).publication().equals(result.publication()), "deterministic independent lowering");
        var reference = ((DataCallTarget) call.target()).reference(); var other = a.dataDeclarations().getLast().id();
        var changedTarget = new DataCallTarget(new DataReference(reference.id(), reference.role(),
            new Binding(ResolutionStatus.RESOLVED, List.of(other), Optional.of(other)), Optional.of(new WholeItemAccess(other)), reference.provenance()));
        check(!lower(CallInputs.call(a, CallInputs.target(call, changedTarget))).publication().orElseThrow().id().equals(result.publication().orElseThrow().id()), "revision includes DATA CALL target");
        check(!lower(CallInputs.create(2, 1, 8, "OTHER", false)).publication().orElseThrow().id().equals(result.publication().orElseThrow().id()), "revision includes fitted result");
        check(!lower(CallInputs.create(0, 0, 5, "PROGB", true)).publication().orElseThrow().id().equals(lower(literal).publication().orElseThrow().id()), "revision includes literal CALL target");
        check(!lower(ScalarInputs.create(2, 1)).publication().orElseThrow().id().equals(lower(CallInputs.create(2, 1, 5, "PROGA", false)).publication().orElseThrow().id()), "revision includes CALL occurrence");
        var source = (MoveFact) a.statements().getFirst();
        for (var invalid : List.of(
                new MoveFact(source.header(), source.source(), source.target(), CopySemantics.FITTED_TEXT, source.normalContinuation()),
                new MoveFact(source.header(), source.source(), source.target(), CopySemantics.FULL_IDENTITY, source.normalContinuation(), source.textAdjustment())))
            rejected(ScalarInputs.move(a, invalid), LoweringResult.Status.INVALID_INPUT, "copy/adjustment coherence");
        var using = new CallSurface(ClausePresence.PRESENT, Optional.of(1), ClausePresence.ABSENT, ClausePresence.ABSENT, ClausePresence.ABSENT, ClausePresence.ABSENT);
        rejected(CallInputs.call(a, new CallFact(call.header(), call.syntax(), call.target(), call.runtimeTarget(), call.runtimeUncertaintyCode(), call.normalContinuation(), using, call.effects(), call.outcomes())), LoweringResult.Status.UNSUPPORTED_SLICE, "USING");
        var ambiguous = new DataCallTarget(new DataReference(reference.id(), reference.role(), new Binding(ResolutionStatus.AMBIGUOUS,
            a.dataDeclarations().stream().map(DataFact::id).toList(), Optional.empty()), Optional.empty(), reference.provenance()));
        check(new CallAdmission().admit(CallInputs.call(a, CallInputs.target(call, ambiguous)), ScalarSuite.OPTIONS.admission()).status()
            == Admission.Status.UNSUPPORTED_SLICE, "ambiguous target admitted");
        rejected(CallInputs.call(a, CallInputs.target(call, ambiguous)), LoweringResult.Status.UNSUPPORTED_SLICE, "ambiguous target");
        var unavailable = new NormalContinuation(ContinuationAvailability.UNAVAILABLE, Optional.empty(), call.normalContinuation().provenance());
        rejected(CallInputs.call(a, new CallFact(call.header(), call.syntax(), call.target(), call.runtimeTarget(), call.runtimeUncertaintyCode(), unavailable, call.surface(), call.effects(), call.outcomes())), LoweringResult.Status.BLOCKED_LOWERING, "unknown continuation");
        var dangling = new NormalContinuation(ContinuationAvailability.KNOWN, Optional.of(new StatementId(a.unit(), "statement:987")), call.normalContinuation().provenance());
        rejected(CallInputs.call(a, new CallFact(call.header(), call.syntax(), call.target(), call.runtimeTarget(), call.runtimeUncertaintyCode(), dangling, call.surface(), call.effects(), call.outcomes())), LoweringResult.Status.INVALID_INPUT, "dangling continuation");
        int count = CallOracle.assertions() - before;
        System.out.println("LOWER_CALL_TESTS=" + count); return count;
    }
    private static void rejected(SpInput input, LoweringResult.Status status, String why) {
        var result = lower(input); check(result.status() == status && result.publication().isEmpty(), why + ": " + result.status());
    }
    public static int scale() {
        long[] visits = new long[2], refs = new long[2];
        for (int i = 0; i < 2; i++) {
            int n = 500 * (i + 1); var input = CallInputs.create(n, n, 8, "PROGA", false); var result = lower(input);
            check(result.status() == LoweringResult.Status.SUCCESS, "CALL capacity success");
            visits[i] = result.admission().statistics().entitiesVisited(); refs[i] = result.admission().statistics().referencesChecked();
            System.out.println("CALL_SCALE n=" + n + " visits=" + visits[i] + " references=" + refs[i]);
        }
        check(visits[1] <= 2 * visits[0] && refs[1] <= 2 * refs[0], "linear CALL admission/index joins");
        return 3;
    }
    public static void main(String[] args) { run(); if (args.length > 0) scale(); }
}
