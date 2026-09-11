package io.github.gustavo2358.lower.testing;

import io.github.gustavo2358.air.model.*;
import io.github.gustavo2358.air.validation.*;
import io.github.gustavo2358.lower.application.AirOutputValidation;

/** Independent observations from the CP0 FREEZE. No decoder or lowerer generates expected values. */
public final class BoundarySuite {
    private static int count;
    private BoundarySuite() {}

    private static void require(boolean condition, String property) {
        if (!condition) throw new AssertionError(property);
        count++;
        System.out.println("ok " + count + " - " + property);
    }

    private static void shape(Publication publication) {
        require(publication.airVersion().equals(SemanticVersion.AIR_2_0_0), "AIR 2.0.0");
        require(publication.units().size() == 1, "one unit");
        Unit unit = publication.units().getFirst();
        require(unit.body() == Unit.BodyAvailability.AVAILABLE, "available body");
        require(unit.entries().size() == 1 && unit.sequences().size() == 1, "one entry and sequence");
        Entries.Entry entry = unit.entries().getFirst();
        Sequence sequence = unit.sequences().getFirst();
        require(entry.initialLabel().orElseThrow().equals(sequence.label()), "entry label closes");
        require(entry.signature().parameters().known().isEmpty()
                && entry.signature().parameters().remainder() == Interactions.NoRemainder.INSTANCE,
                "closed zero parameters");
        require(entry.signature().results().known().isEmpty()
                && entry.signature().results().remainder() == Interactions.NoRemainder.INSTANCE,
                "closed zero results");
        require(sequence.instructions().isEmpty(), "no common instructions");
        require(sequence.terminator() instanceof Operations.Return, "expected Return, never Halt");
        require(((Operations.Return) sequence.terminator()).values().isEmpty(), "empty Return");
    }

    public static void main(String[] args) {
        Publication baseline = ManualAir.create(false, Boolean.getBoolean("challenge.halt"));
        require(AirValidator.validate(baseline).status() == ValidationResult.Status.STRUCTURALLY_VALID,
                "manual AIR accepted by real validator");
        shape(baseline);
        Publication invalid = ManualAir.create(true, false);
        require(AirValidator.validate(invalid).status() == ValidationResult.Status.INVALID_IR,
                "real validator rejects dangling label");
        require(AirOutputValidation.validate(invalid).status() == ValidationResult.Status.INVALID_IR,
                "boundary must preserve INVALID_IR for dangling label");
        ValidationResult result = AirOutputValidation.validate(baseline);
        require(result.equals(AirValidator.validate(baseline)), "boundary preserves complete result and issues");
        Publication profileClaim = ManualAir.withProfileObligation();
        ValidationResult obligation = AirValidator.validate(profileClaim);
        require(obligation.issues().stream().anyMatch(i -> i.kind() == ValidationIssue.Kind.SEMANTIC_OBLIGATION),
                "profile fixture contains a real semantic obligation");
        require(AirOutputValidation.validate(profileClaim).equals(obligation),
                "semantic obligations remain visible");
        require(AirValidator.validate(ManualAir.create(false, true)).isStructurallyValid(),
                "Halt can be valid AIR but is not the frozen Return shape");
        try {
            baseline.units().clear();
            throw new AssertionError("publication must be immutable");
        } catch (UnsupportedOperationException expected) {
            require(true, "publication immutable");
        }
        require(ManualAir.create(false, false).equals(baseline), "independent construction deterministic");
        require(count > 0, "nonzero executed assertions");
        int semantic = count + CallSuite.run() + ScalarSuite.run() + io.github.gustavo2358.lower.application.ScalarIdentitySuite.run() + io.github.gustavo2358.lower.application.LocalIdentitySuite.run() + InputSuite.run() + io.github.gustavo2358.lower.application.CompactIdentitySuite.run() + LoweringSuite.run();
        int performance = Boolean.getBoolean("lower.performance") ? PerformanceSuite.run() + ScalarSuite.scale() + CallSuite.scale() : 0;
        System.out.println("LOWER_TESTS=" + (semantic + performance));
        if (Boolean.getBoolean("lower.performance")) System.out.println("LOWER_PERFORMANCE_TESTS=" + performance);
    }
}
