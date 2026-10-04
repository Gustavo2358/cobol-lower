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
    private static int numericIdentity(SpInput input,DataFact data,MoveFact move) {
        var number = new ScalarNumber(4, 0, true, "BINARY", "STD");
        var numeric = with(data, "scalarNumber", Optional.of(number));
        var edit = new ScalarEdit(List.of(new EditPart("SIGN", 1, "+", "-"),new EditPart("DIGITS", 4, "", "")),4,0,5);
        var edited = with(data, "scalarEdit", Optional.of(edit));
        int count = 0;
        for (var pair : List.of(List.of(numeric, with(numeric,"scalarNumber",Optional.of(with(number,"trunc","BIN")))),
                List.of(data, edited),
                List.of(edited,with(edited,"scalarEdit",Optional.of(with(edit,"parts",List.of(new EditPart("SIGN",1," ","-"),new EditPart("DIGITS",4,"","")))))))) {
            var a=ScalarInputs.replace(input,List.of(pair.getFirst()),input.statements());
            var b=ScalarInputs.replace(input,List.of(pair.getLast()),input.statements());
            if(CanonicalRevision.partial(a,32).equals(CanonicalRevision.partial(b,32)))throw new AssertionError("PARTIAL_ID numeric metadata absent");count++;
        }
        var slice=new LogicalSlice(data.id(),java.math.BigInteger.ZERO,java.math.BigInteger.ONE);
        var target=with(with(move.target(),"wholeItemAccess",Optional.empty()),"logicalSlice",Optional.of(slice));
        var a=ScalarInputs.replace(input,input.dataDeclarations(),List.of(with(move,"target",target),input.statements().getLast()));
        var b=ScalarInputs.replace(input,input.dataDeclarations(),List.of(with(move,"target",with(target,"logicalSlice",Optional.of(with(slice,"start",java.math.BigInteger.ONE)))),input.statements().getLast()));
        if(CanonicalRevision.partial(a,32).equals(CanonicalRevision.partial(b,32)))throw new AssertionError("PARTIAL_ID slice absent");
        return count+1;
    }
    public static int run() {
        int count = 0; var input = ScalarInputs.create(1, 1); var m = (MoveFact) input.statements().getFirst(); var d = input.dataDeclarations().getFirst();
        var baseline = identity(input, List.of(d), m);
        for (var changed : List.of(with(d, "id", new DataId(input.unit(), "data:9")), with(d, "canonicalName", "renamed"),
                with(d, "scalarText", Optional.of(with(d.scalarText().orElseThrow(), "logicalExtent", 6))),
                with(d, "provenance", ScalarInputs.provenance(50, 0)))) {
            if (baseline.equals(identity(input, List.of(changed), m))) throw new AssertionError("SCALAR_ID DATA fact absent from preimage"); count++;
        }
        var source = (LiteralSource) m.source(); var target = m.target(); var next = m.normalContinuation();
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
        return count + numericIdentity(input, d, m);
    }
}
