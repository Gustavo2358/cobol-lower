package io.github.gustavo2358.lower.adapters.testing;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.node.*;
import io.github.gustavo2358.air.json.AirJson;
import io.github.gustavo2358.lower.adapters.cli.CobolLower;
import io.github.gustavo2358.lower.adapters.sp.SpJsonDecoder;
import io.github.gustavo2358.lower.application.*;
import io.github.gustavo2358.lower.domain.SpInput;
import io.github.gustavo2358.lower.testing.PerformOracle;
import java.util.*;
import static io.github.gustavo2358.lower.testing.CallOracle.check;

public final class PerformIntegrationSuite {
    private static final ObjectMapper JSON = new ObjectMapper();
    public static void run() throws Exception {
        var decoder = new SpJsonDecoder(CobolLower.INPUT_LIMITS); var codec = new AirJson();
        for (String name : List.of("literal", "copy", "overwrite")) {
            ObjectNode wire;
            try (var in = PerformIntegrationSuite.class.getResourceAsStream("/sp/perform-basic/" + name + ".json")) {
                if (in == null) throw new AssertionError("real SP1.6 fixture absent"); wire = (ObjectNode) JSON.readTree(in);
            }
            var decoded = decoder.decode(JSON.writeValueAsBytes(wire));
            check(decoded instanceof SpJsonDecoder.Decoded, "strict SP1.6 decoder: " + decoded);
            var input = ((SpJsonDecoder.Decoded) decoded).input();
            var result = new CobolLowerer().lower(input, CobolLower.OPTIONS); PerformOracle.inspect(input, result);
            var bytes = codec.encode(result.publication().orElseThrow());
            check(Arrays.equals(bytes, codec.encode(codec.decode(bytes))), "unchanged AIR codec roundtrip");
            var statements = new ArrayList<>(input.statements()); Collections.reverse(statements);
            var permuted = new SpInput(input.unit(), input.policy(), input.dataDeclarations(), statements, input.structure(), input.gaps(), input.coverage(), input.entryInventory(), input.storageIndependence());
            var reordered = new CobolLowerer().lower(permuted, CobolLower.OPTIONS); PerformOracle.inspect(permuted, reordered);
            check(Arrays.equals(bytes, codec.encode(reordered.publication().orElseThrow())), "SP physical statement order is not control or identity");
            for (String mutation : List.of("profile", "target", "exit", "resume", "body", "primary", "entry", "second-callsite", "unknown-field")) {
                var bad = wire.deepCopy(); var facts = (ArrayNode) bad.path("statements"); ObjectNode perform = null;
                for (var s : facts) if (s.path("variant").asText().equals("PERFORM")) perform = (ObjectNode) s;
                var original = Objects.requireNonNull(perform);
                String call = original.path("normalContinuation").path("statement").asText();
                switch (mutation) {
                    case "profile" -> original.put("profile", "OUTSIDE_SLICE");
                    case "target" -> original.put("targetEntry", call);
                    case "exit" -> original.put("targetExit", call);
                    case "resume" -> ((ObjectNode) original.path("normalContinuation")).put("statement", original.path("targetEntry").asText());
                    case "body" -> ((ArrayNode) original.path("targetStatements")).add(call);
                    case "primary" -> ((ArrayNode) original.path("primaryStatements")).remove(0);
                    case "entry" -> ((ObjectNode) bad.at("/entryInventory/entries/0/start")).put("statement", original.path("targetEntry").asText());
                    case "second-callsite" -> facts.add(original.deepCopy());
                    case "unknown-field" -> original.put("throughReference", "procedure:1");
                }
                var changed = decoder.decode(JSON.writeValueAsBytes(bad));
                check(changed instanceof SpJsonDecoder.Rejected || new CobolLowerer().lower(((SpJsonDecoder.Decoded) changed).input(), CobolLower.OPTIONS).publication().isEmpty(), "refused " + mutation);
            }
        }
        System.out.println("LOWER_PERFORM_INTEGRATION=PASS");
    }
    public static void main(String[] args) throws Exception { run(); }
}
