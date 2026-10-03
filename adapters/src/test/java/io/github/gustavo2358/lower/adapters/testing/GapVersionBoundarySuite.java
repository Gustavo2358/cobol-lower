package io.github.gustavo2358.lower.adapters.testing;

import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.databind.node.*;
import io.github.gustavo2358.lower.adapters.cli.CobolLower;
import io.github.gustavo2358.lower.adapters.sp.SpJsonDecoder;
import io.github.gustavo2358.lower.application.CobolLowerer;
import java.util.*;

/** A positive current proof cannot relax a historical wire contract. */
public final class GapVersionBoundarySuite {
    private static final JsonMapper JSON = new JsonMapper();
    private record Case(String name, String statement, Set<String> codes) { }
    private GapVersionBoundarySuite() { }

    private static void need(boolean ok, String reason) {
        if (!ok) throw new AssertionError(reason);
    }

    private static ObjectNode fixture(String version) throws Exception {
        try (var in = GapVersionBoundarySuite.class.getResourceAsStream(
                "/sp/active-gaps/historical-gaps-" + version + ".json")) {
            return (ObjectNode) JSON.readTree(Objects.requireNonNull(in));
        }
    }

    private static SpJsonDecoder.Result decode(ObjectNode wire) throws Exception {
        return new SpJsonDecoder(CobolLower.INPUT_LIMITS).decode(JSON.writeValueAsBytes(wire));
    }

    private static void admitted(ObjectNode wire, String name) throws Exception {
        var decoded = decode(wire);
        need(decoded instanceof SpJsonDecoder.Decoded, name + " must decode");
        var result = new CobolLowerer().lower(((SpJsonDecoder.Decoded) decoded).input(), CobolLower.POSITIVE_OPTIONS);
        need(result.publication().isPresent(), name + " must publish AIR");
        need(result.validation().orElseThrow().isStructurallyValid(), name + " AIR must validate");
    }

    private static ObjectNode remove(ObjectNode original, Case example) {
        var changed = original.deepCopy();
        var gaps = (ArrayNode) changed.path("gaps");
        int count = 0;
        for (int i = gaps.size() - 1; i >= 0; i--) {
            var gap = gaps.get(i);
            if (gap.path("statement").asText().equals(example.statement())
                    && example.codes().contains(gap.path("code").asText())) {
                gaps.remove(i); count++;
            }
        }
        need(count == example.codes().size(), example.name() + " must remove the intended original gaps");
        for (var statement : changed.path("statements")) {
            if (!statement.path("header").path("id").asText().equals(example.statement())) continue;
            if (statement.path("gapCodes") instanceof ArrayNode codes) {
                for (int i = codes.size() - 1; i >= 0; i--)
                    if (example.codes().contains(codes.get(i).asText())) codes.remove(i);
            }
        }
        return changed;
    }

    private static void historicalRejection(ObjectNode wire, String statement, String name) throws Exception {
        var decoded = decode(wire);
        need(decoded instanceof SpJsonDecoder.Rejected,
            name + ": SP 2.62 must reject the missing obligation before materialized input escapes");
        var diagnostic = ((SpJsonDecoder.Rejected) decoded).diagnostic();
        need(diagnostic.code() == SpJsonDecoder.Code.INPUT_ERROR, name + " is invalid known input, not an unsupported version");
        need(diagnostic.location().startsWith("$/gaps/") && diagnostic.location().contains(statement),
            name + " must identify the violated gap obligation and statement");
    }

    public static void main(String[] args) throws Exception {
        var historical = fixture("2.62");
        var current = fixture("2.63");
        need(historical.path("contractVersion").asText().equals("2.62.0"), "real historical wire");
        need(current.path("contractVersion").asText().equals("2.63.0"), "real current wire");
        admitted(historical, "unaltered SP 2.62 producer");
        admitted(current, "unaltered SP 2.63 producer");
        var cases = List.of(
            new Case("NO_OP", "statement:3", Set.of("OBSERVED_STATEMENT_UNSUPPORTED")),
            new Case("PERFORM", "statement:4", Set.of("PERFORM_ISOLATED_PRIMARY_NOT_PROVEN", "PERFORM_ISOLATED_PRIMARY_FLOW_NOT_PROVEN")),
            new Case("typed MOVE membership", "statement:0", Set.of("CONTAINMENT_NOT_PROJECTED")),
            new Case("STRUCTURE with CAPABILITY retained", "statement:6", Set.of("CONTAINMENT_NOT_PROJECTED")));
        var failures = new ArrayList<String>();
        for (var example : cases) {
            var changed = remove(historical, example);
            var remaining = new ArrayList<String>();
            for (var gap : changed.path("gaps"))
                if (gap.path("statement").asText().equals(example.statement())) remaining.add(gap.path("scope").asText());
            need(remaining.equals(example.statement().equals("statement:6") ? List.of("CAPABILITY") : List.of()),
                example.name() + " must isolate the intended historical obligation");
            admitted(changed.deepCopy().put("contractVersion", "2.63.0"), example.name() + " current proof");
            try { historicalRejection(changed, example.statement(), example.name()); }
            catch (AssertionError failure) { failures.add(failure.getMessage()); }
        }
        // The actual current producer must not become a valid historical payload by relabeling it.
        try { historicalRejection(current.deepCopy().put("contractVersion", "2.62.0"), "statement:3", "version relabel"); }
        catch (AssertionError failure) { failures.add(failure.getMessage()); }
        need(failures.isEmpty(), String.join("\n", failures));
        System.out.println("GAP_VERSION_BOUNDARY: real SP 2.62/2.63 admitted; 4 paired gap mutations and historical relabel rejected PASS");
    }
}
