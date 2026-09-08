package io.github.gustavo2358.lower.application;

import io.github.gustavo2358.lower.domain.SpInput;
import io.github.gustavo2358.lower.testing.ScalarInputs;
import io.github.gustavo2358.lower.testing.ScalarSuite;
import java.lang.reflect.*;
import java.util.*;
import static io.github.gustavo2358.lower.domain.SpInput.*;

/** Direct preimage sensitivity, including incompatible facts that admission separately rejects. */
public final class ScalarIdentitySuite {
    private ScalarIdentitySuite() { }
    private static <T extends Record> T with(T record, String field, Object value) {
        try {
            var parts = record.getClass().getRecordComponents(); var types = new Class<?>[parts.length]; var values = new Object[parts.length];
            for (int i = 0; i < parts.length; i++) { types[i] = parts[i].getType(); values[i] = parts[i].getName().equals(field) ? value : parts[i].getAccessor().invoke(record); }
            @SuppressWarnings("unchecked") T result = (T) record.getClass().getDeclaredConstructor(types).newInstance(values); return result;
        } catch (ReflectiveOperationException ex) { throw new AssertionError(ex); }
    }
    private static String identity(SpInput input, List<DataFact> data, MoveFact move) {
        return CanonicalRevision.scalar(input, data, List.of(move), (GobackFact) input.statements().getLast(), 32).orElseThrow();
    }
    public static int run() {
        int count = 0; var input = ScalarInputs.create(1, 1); var m = (MoveFact) input.statements().getFirst(); var d = input.dataDeclarations().getFirst();
        var baseline = identity(input, List.of(d), m);
        for (var changed : List.of(with(d, "id", new DataId(input.unit(), "data:9")), with(d, "canonicalName", "renamed"),
                with(d, "scalarText", Optional.of(with(d.scalarText().orElseThrow(), "logicalExtent", 6))),
                with(d, "provenance", ScalarInputs.provenance(50, 0)))) {
            if (baseline.equals(identity(input, List.of(changed), m))) throw new AssertionError("SCALAR_ID DATA fact absent from preimage"); count++;
        }
        var source = m.source(); var target = m.target(); var next = m.normalContinuation();
        for (var changed : List.of(
                with(m, "source", with(source, "id", new OperandId(m.header().id(), "operand:0:9"))),
                with(m, "source", with(source, "kind", LiteralKind.NUMERIC)),
                with(m, "source", with(source, "logicalValue", Optional.of(with(source.logicalValue().orElseThrow(), "value", "CCCCC")))),
                with(m, "source", with(source, "logicalValue", Optional.of(with(source.logicalValue().orElseThrow(), "logicalExtent", 6)))),
                with(m, "source", with(source, "provenance", ScalarInputs.provenance(60, 0))),
                with(m, "target", with(target, "id", new OperandId(m.header().id(), "operand:0:8"))),
                with(m, "target", with(target, "role", OperandRole.READ)),
                with(m, "target", with(target, "binding", with(target.binding(), "status", ResolutionStatus.AMBIGUOUS))),
                with(m, "target", with(target, "binding", with(target.binding(), "selected", Optional.of(new DataId(input.unit(), "data:2"))))),
                with(m, "target", with(target, "wholeItemAccess", Optional.of(new WholeItemAccess(new DataId(input.unit(), "data:2"))))),
                with(m, "target", with(target, "provenance", ScalarInputs.provenance(70, 0))),
                with(m, "copySemantics", CopySemantics.UNAVAILABLE),
                with(m, "normalContinuation", with(next, "availability", ContinuationAvailability.UNAVAILABLE)),
                with(m, "normalContinuation", with(next, "statement", Optional.of(m.header().id()))),
                with(m, "normalContinuation", with(next, "provenance", ScalarInputs.provenance(80, 0))))) {
            if (baseline.equals(identity(input, List.of(d), changed))) throw new AssertionError("SCALAR_ID MOVE fact absent from preimage: " + changed); count++;
        }
        var longData = with(d, "canonicalName", "Z".repeat(100_000));
        var large = ScalarSuite.success(ScalarInputs.replace(input, List.of(longData), input.statements()));
        var pub = large.publication().orElseThrow();
        if (!pub.units().getFirst().objects().getFirst().id().localId().matches("[0-9a-f]{32}")
                || pub.coverage().items().stream().anyMatch(item -> item.sourceKey().length() > 120))
            throw new AssertionError("SCALAR_ID no expanded identities or source keys"); count++;
        return count;
    }
}
