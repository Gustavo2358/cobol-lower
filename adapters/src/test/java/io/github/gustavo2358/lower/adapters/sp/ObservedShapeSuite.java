package io.github.gustavo2358.lower.adapters.sp;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.github.gustavo2358.lower.domain.SpInput;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/** Synthetic SP2.15 witnesses for the descriptive observed-statement boundary. */
public final class ObservedShapeSuite {
    private static final ObjectMapper JSON = new ObjectMapper();
    private static int checks;

    private ObservedShapeSuite() { }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
        checks++;
    }

    private static ObjectNode witness(String shape) throws Exception {
        var resource = ObservedShapeSuite.class.getResourceAsStream("/sp/dvi/invariant.json");
        var root = (ObjectNode) JSON.readTree(Objects.requireNonNull(resource, "SP2.15 base fixture"));
        var previous = (ObjectNode) root.path("statements").get(0);
        var header = (ObjectNode) previous.path("header").deepCopy();
        var readiness = (ObjectNode) header.path("readiness");
        for (String dimension : List.of("lowering", "cfg", "effectsDataflow")) {
            var claim = (ObjectNode) readiness.path(dimension);
            claim.put("status", "BLOCKED").put("scope", "opaque statement semantics are unavailable");
        }

        var observed = JSON.createObjectNode();
        observed.put("variant", "OBSERVED");
        observed.set("header", header);
        observed.put("observedKind", "EMBEDDED_LANGUAGE");
        observed.put("observedShape", shape);
        observed.put("gapCode", "OBSERVED_STATEMENT_PARTIAL");
        var continuation = observed.putObject("normalContinuation");
        continuation.put("availability", "UNAVAILABLE").putNull("statement");
        continuation.set("provenance", header.path("provenance"));
        observed.putArray("knownReferences");
        ((ArrayNode) root.path("statements")).set(0, observed);

        var gaps = (ArrayNode) root.path("gaps");
        gaps.removeAll();
        var gap = gaps.addObject();
        gap.put("statement", "statement:0").put("scope", "CAPABILITY")
            .put("code", "OBSERVED_STATEMENT_PARTIAL").put("detail", "synthetic opaque witness");
        gap.set("provenance", header.path("provenance"));
        return root;
    }

    private static SpInput.OtherStatement decode(ObjectNode document) throws Exception {
        var bytes = JSON.writeValueAsBytes(document);
        var wire = JSON.readValue(bytes, Wire215.Document.class);
        check(wire.statements().getFirst() instanceof Wire211.ObservedDocument,
            "SP JSON must decode to the typed observed wire variant");
        var observed = (Wire211.ObservedDocument) wire.statements().getFirst();
        check(observed.observedShape().equals(document.path("statements").get(0).path("observedShape").textValue()),
            "wire must retain observedShape exactly");

        var decoded = new SpJsonDecoder(new SpJsonDecoder.Limits(64)).decode(bytes);
        check(decoded instanceof SpJsonDecoder.Decoded, "synthetic SP2.15 witness must decode: " + decoded);
        var statement = ((SpJsonDecoder.Decoded) decoded).input().statements().getFirst();
        check(statement instanceof SpInput.OtherStatement, "observed wire fact must materialize as OtherStatement");
        return (SpInput.OtherStatement) statement;
    }

    private static String observedShape(SpInput.OtherStatement statement) throws Exception {
        var component = Arrays.stream(SpInput.OtherStatement.class.getRecordComponents())
            .filter(value -> value.getName().equals("observedShape")).findFirst();
        check(component.isPresent(), "materialized OtherStatement must expose observedShape");
        return (String) component.orElseThrow().getAccessor().invoke(statement);
    }

    public static void main(String[] ignored) throws Exception {
        for (String shape : List.of("OPAQUE_DLI", "OPAQUE_CICS", "OPAQUE_FUTURE_LANGUAGE")) {
            check(observedShape(decode(witness(shape))).equals(shape),
                "materialization must retain open observed shape " + shape);
        }
        for (String malformed : List.of("missing", "null")) {
            var document = witness("OPAQUE_DLI");
            var observed = (ObjectNode) document.path("statements").get(0);
            if (malformed.equals("missing")) observed.remove("observedShape");
            else observed.putNull("observedShape");
            check(new SpJsonDecoder(new SpJsonDecoder.Limits(64)).decode(JSON.writeValueAsBytes(document))
                    instanceof SpJsonDecoder.Rejected,
                malformed + " observedShape must fail closed");
        }
        System.out.println("OBSERVED_SHAPE_TESTS=" + checks);
    }
}
