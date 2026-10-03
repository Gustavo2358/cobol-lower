package io.github.gustavo2358.lower.adapters.testing;

import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.databind.node.*;
import io.github.gustavo2358.lower.adapters.cli.CobolLower;
import io.github.gustavo2358.lower.adapters.sp.SpJsonDecoder;
import io.github.gustavo2358.lower.application.*;
import java.util.*;

/** SP2.63 from the real producer: current proof, never the gap count, authorizes admission. */
public final class ActiveGapsSuite {
    private static final JsonMapper JSON = new JsonMapper();
    private ActiveGapsSuite() { }
    private static void need(boolean ok, String reason) { if (!ok) throw new AssertionError(reason); }
    private static LoweringResult lower(ObjectNode wire) throws Exception {
        var decoded = new SpJsonDecoder(CobolLower.INPUT_LIMITS).decode(JSON.writeValueAsBytes(wire));
        need(decoded instanceof SpJsonDecoder.Decoded, "valid wire: " + decoded);
        return new CobolLowerer().lower(((SpJsonDecoder.Decoded) decoded).input(), CobolLower.POSITIVE_OPTIONS);
    }
    private static void rejected(ObjectNode wire) throws Exception {
        var decoded = new SpJsonDecoder(CobolLower.INPUT_LIMITS).decode(JSON.writeValueAsBytes(wire));
        if (decoded instanceof SpJsonDecoder.Decoded d)
            need(new CobolLowerer().lower(d.input(), CobolLower.POSITIVE_OPTIONS).status() == LoweringResult.Status.INVALID_INPUT,
                "proof loss must not authorize a gap-free statement");
    }
    public static void main(String[] args) throws Exception {
        ObjectNode source;
        try (var in = ActiveGapsSuite.class.getResourceAsStream("/sp/active-gaps/active-gaps.json")) {
            source = (ObjectNode) JSON.readTree(Objects.requireNonNull(in));
        }
        need(source.path("contractVersion").asText().equals("2.63.0"), "current producer contract");
        var result = lower(source);
        need(result.publication().isPresent(), "real publication admits: " + result.admission().diagnostics());
        need(result.validation().orElseThrow().isStructurallyValid(), "AIR remains valid");
        need(!result.publication().orElseThrow().uncertainties().isEmpty(), "real unknown effects remain explicit");
        var noOps = new HashSet<String>();
        for (var e : source.path("statementEffects")) if (e.path("proof").asText().equals("NO_OP")) noOps.add(e.path("statement").asText());
        need(noOps.size() == 2, "two independent real no-ops");
        for (var g : source.path("gaps")) need(!noOps.contains(g.path("statement").asText()), "no-op has no obsolete capability gap");
        var absent = source.deepCopy(); absent.remove("controlTopology"); absent.remove("factDependencies"); rejected(absent);
        for (String kind : List.of("PARTIAL_UNKNOWN", "CONTROL_POSSIBILITY")) {
            var weak = source.deepCopy(); int changed = 0;
            for (var p : weak.path("controlTopology").path("proofs")) if (p.path("rule").asText().equals("statement-scope")) {
                ((ObjectNode)p).put("kind",kind); changed++;
            }
            need(changed > 0, "transitive proof mutation executed"); rejected(weak);
        }
        var missing = source.deepCopy(); var gaps = (ArrayNode) missing.path("gaps"); int removed = 0;
        for (int i = gaps.size()-1; i >= 0; i--) if (gaps.get(i).path("code").asText().equals("OBSERVED_STATEMENT_UNSUPPORTED")) { gaps.remove(i); removed++; }
        need(removed > 0, "arithmetic capability gap mutation executed"); rejected(missing);
        var inputMissing = source.deepCopy();
        for (var s : inputMissing.path("statements")) if (noOps.contains(s.path("header").path("id").asText())) ((ObjectNode)s.path("header")).put("coverage","INPUT_MISSING");
        rejected(inputMissing);
        var future = source.deepCopy().put("contractVersion","2.64.0");
        need(new SpJsonDecoder(CobolLower.INPUT_LIMITS).decode(JSON.writeValueAsBytes(future)) instanceof SpJsonDecoder.Rejected, "future remains closed");
        System.out.println("ACTIVE_GAPS: real SP2.63, valid AIR, retained uncertainty and 6 proof-loss/version mutations PASS");
    }
}
