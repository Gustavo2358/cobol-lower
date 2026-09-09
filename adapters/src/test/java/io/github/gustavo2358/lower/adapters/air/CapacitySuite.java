package io.github.gustavo2358.lower.adapters.air;

import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.github.gustavo2358.air.model.Publication;
import io.github.gustavo2358.lower.adapters.cli.CobolLower;
import io.github.gustavo2358.lower.adapters.sp.*;
import io.github.gustavo2358.lower.application.*;
import io.github.gustavo2358.lower.testing.ScalarSuite;
import io.github.gustavo2358.lower.adapters.testing.ScalarWireSuite;
import java.nio.file.*;
import java.security.MessageDigest;
import java.util.*;

/** Synthetic extension of the proven 1.2 shared-DATA profile. No huge committed fixture. */
public final class CapacitySuite {
    private static final ObjectMapper JSON = new ObjectMapper();
    private static final int MOVES = 20_000;
    private static int checks;
    private static void check(boolean value, String why) {
        if (!value) throw new AssertionError("CAPACITY " + why);
        checks++;
    }
    private static void lines(JsonNode node, int offset) {
        if (node instanceof ObjectNode object) {
            for (String key : List.of("startLine", "endLine"))
                if (object.has(key)) object.put(key, object.get(key).intValue() + offset);
        }
        node.elements().forEachRemaining(child -> lines(child, offset));
    }
    public static void generate(Path path) throws Exception {
        var base = (ObjectNode) JSON.readTree(ScalarWireSuite.resource("scalar-move-1.2.0.json"));
        try (var out = JSON.getFactory().createGenerator(path.toFile(), com.fasterxml.jackson.core.JsonEncoding.UTF8)) {
            out.writeStartObject();
            var fields = base.properties().iterator();
            while (fields.hasNext()) {
                var field = fields.next(); out.writeFieldName(field.getKey());
                switch (field.getKey()) {
                    case "statements" -> {
                        out.writeStartArray();
                        for (int i = 0; i < MOVES; i++) {
                            var move = (ObjectNode) field.getValue().get(0).deepCopy();
                            ((ObjectNode) move.get("header")).put("id", "statement:" + i).put("programPoint", i);
                            ((ObjectNode) move.get("source")).put("id", "operand:" + i + ":0");
                            ((ObjectNode) move.get("target")).put("id", "operand:" + i + ":1");
                            ((ObjectNode) move.get("normalContinuation")).put("statement", "statement:" + (i + 1));
                            lines(move, i); out.writeTree(move);
                        }
                        var terminal = (ObjectNode) field.getValue().get(1).deepCopy();
                        ((ObjectNode) terminal.get("header")).put("id", "statement:" + MOVES).put("programPoint", MOVES);
                        lines(terminal, MOVES - 1); out.writeTree(terminal); out.writeEndArray();
                    }
                    case "structure" -> {
                        out.writeStartObject(); out.writeArrayFieldStart("branches"); out.writeEndArray();
                        out.writeArrayFieldStart("roots");
                        for (int i = 0; i <= MOVES; i++) out.writeString("statement:" + i);
                        out.writeEndArray(); out.writeEndObject();
                    }
                    case "coverage" -> {
                        var coverage = (ObjectNode) field.getValue().deepCopy();
                        coverage.put("modeledStatements", MOVES + 1).put("observedStatements", MOVES + 1); out.writeTree(coverage);
                    }
                    default -> out.writeTree(field.getValue());
                }
            }
            out.writeEndObject();
        }
    }
    private static Publication verify(SpJsonDecoder.Result decoded) {
        check(decoded instanceof SpJsonDecoder.Decoded, "supported SP decodes: " + decoded);
        var input = ((SpJsonDecoder.Decoded) decoded).input();
        var result = new CobolLowerer().lower(input, CobolLower.OPTIONS);
        check(result.status() == LoweringResult.Status.SUCCESS, "supported SP lowers: " + result.status() + " " + result.admission().diagnostics());
        ScalarSuite.oracle(input, result, Collections.nCopies(MOVES, "PROGA"),
            Collections.nCopies(MOVES, input.dataDeclarations().getFirst().id()));
        check(result.admission().statistics().entitiesVisited() == 19L * MOVES + 21, "exact admission visits exceed old 250000");
        check(result.admission().statistics().referencesChecked() == 7L * MOVES + 3, "linear reference ledger");
        var p = result.publication().orElseThrow();
        System.out.println("CAPACITY_AIR moves=" + MOVES + " data=1 statements=" + result.statements().size()
            + " operands=" + result.operands().size() + " entities=" + result.admission().statistics().entitiesVisited()
            + " references=" + result.admission().statistics().referencesChecked() + " origins=" + p.origins().size()
            + " publication=" + p.id().localId() + " shared_validator=PASS relational_oracle=PASS");
        return p;
    }
    public static int run() throws Exception {
        checks = 0; var path = Files.createTempFile("lower-capacity-", ".json");
        try {
            generate(path);
            byte[] raw = Files.readAllBytes(path);
            long nodes = 0; int depth = 0, maximum = 0;
            try (var parser = JSON.getFactory().createParser(path.toFile())) {
                for (var token = parser.nextToken(); token != null; token = parser.nextToken()) {
                    if (token == JsonToken.START_ARRAY || token == JsonToken.START_OBJECT) { depth++; maximum = Math.max(maximum, depth); }
                    if (token == JsonToken.END_ARRAY || token == JsonToken.END_OBJECT) depth--;
                    else if (token != JsonToken.FIELD_NAME) nodes++;
                }
            }
            String sha = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(raw));
            System.out.println("CAPACITY_SP moves=" + MOVES + " bytes=" + raw.length + " nodes=" + nodes + " container_depth=" + maximum + " sha256=" + sha);
            check(raw.length > 32 * 1024 * 1024 && nodes > 1_500_000, "corpus crosses BOTH old input gates");
            var first = verify(new SpFileInput(CobolLower.INPUT_LIMITS).read(path));
            var measured = new SpJsonDecoder(CobolLower.INPUT_LIMITS).decodeMeasured(raw);
            check(measured.statistics().jsonNodesVisited() == nodes, "independent parser node ledger");
            var second = verify(measured.result());
            check(first.equals(second), "complete Publication equality from independent file/byte executions");
            System.out.println("CAPACITY_DETERMINISM=PASS complete Publication equality; no AIR codec override");
        } finally { Files.deleteIfExists(path); }
        return checks;
    }
    public static void main(String[] args) throws Exception { System.out.println("LOWER_CAPACITY_TESTS=" + run()); }
}
