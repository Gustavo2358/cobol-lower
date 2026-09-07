package io.github.gustavo2358.lower.adapters.testing;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.github.gustavo2358.lower.adapters.sp.SpJsonDecoder;
import io.github.gustavo2358.lower.testing.ScaleInputs;

/** Independent schema ledger and recursive test walker challenge iterative production metrics. */
public final class DecoderPerformanceSuite {
    private static int count;
    private DecoderPerformanceSuite() { }
    private static void check(boolean condition, String message) { if (!condition) throw new AssertionError("CP4 performance " + message); count++; }
    private static long nodes(JsonNode value) { long result = 1; for (var child : value) result += nodes(child); return result; }
    private static boolean limit(SpJsonDecoder.Result result) {
        return result instanceof SpJsonDecoder.Rejected rejected && rejected.diagnostic().code() == SpJsonDecoder.Code.IMPLEMENTATION_LIMIT;
    }
    public static int run(byte[] golden) throws Exception {
        var mapper = new ObjectMapper();
        for (int n : new int[] {1, 64, 128, 1024, 2048}) {
            var tree = (ObjectNode)mapper.readTree(golden); var first = tree.path("statements").get(0).deepCopy();
            var statements = tree.putArray("statements"); var roots = ((ObjectNode)tree.path("structure")).putArray("roots");
            for (int i = 0; i < n; i++) {
                var statement = (ObjectNode)first.deepCopy();
                ((ObjectNode)statement.path("header")).put("id", "statement:" + i).put("programPoint", i);
                statements.add(statement); roots.add("statement:" + i);
            }
            ((ObjectNode)tree.path("coverage")).put("observedStatements", n).put("modeledStatements", n);
            ((ObjectNode)tree.path("entryInventory").path("entries").get(0).path("start")).put("statement", "statement:" + (n - 1));
            byte[] bytes = mapper.writeValueAsBytes(tree); int expectedNodes = 82 + 37 * n;
            check(nodes(tree) == expectedNodes, "independent JSON schema ledger 82+37N");
            var decoder = new SpJsonDecoder(new SpJsonDecoder.Limits(bytes.length, 64, expectedNodes)); long started = System.nanoTime();
            var measured = decoder.decodeMeasured(bytes); long elapsed = System.nanoTime() - started;
            check(measured.result() instanceof SpJsonDecoder.Decoded, "exact decoder limits materialize full inventory");
            check(((SpJsonDecoder.Decoded)measured.result()).input().equals(ScaleInputs.create(n)), "decoded scale facts equal independently handwritten memory input");
            check(measured.statistics().jsonNodesVisited() == expectedNodes, "measured JSON ledger 82+37N");
            check(measured.statistics().physicalValuesVisited() == 82L + 35L * n, "measured non-null DTO ledger 82+35N");
            check(measured.statistics().bytesProcessed() == bytes.length, "bytes measured not characters");
            check(decoder.decode(bytes).equals(measured.result()), "measured and ordinary decoder are one implementation");
            check(limit(new SpJsonDecoder(new SpJsonDecoder.Limits(bytes.length,64,expectedNodes - 1)).decode(bytes)), "node limit cannot truncate to success");
            check(limit(new SpJsonDecoder(new SpJsonDecoder.Limits(bytes.length - 1,64,expectedNodes)).decode(bytes)), "byte limit cannot truncate to success");
            check(limit(new SpJsonDecoder(new SpJsonDecoder.Limits(bytes.length,2,expectedNodes)).decode(bytes)), "depth limit explicit");
            System.out.println("PERFORMANCE_DECODER n=" + n + " bytes=" + measured.statistics().bytesProcessed() + " nodes=" + measured.statistics().jsonNodesVisited()
                    + " physical=" + measured.statistics().physicalValuesVisited() + " elapsed_ns=" + elapsed + " java=" + System.getProperty("java.version")
                    + " os=" + System.getProperty("os.name") + " heap_used=" + (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()));
        }
        return count;
    }
}
