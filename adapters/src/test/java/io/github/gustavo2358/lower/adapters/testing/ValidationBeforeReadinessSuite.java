package io.github.gustavo2358.lower.adapters.testing;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.github.gustavo2358.lower.adapters.cli.CobolLower;
import io.github.gustavo2358.lower.adapters.sp.SpJsonDecoder;
import io.github.gustavo2358.lower.application.*;
import io.github.gustavo2358.lower.domain.*;
import java.util.*;
import static io.github.gustavo2358.lower.domain.SpInput.*;

/** Independent historical contradictions take precedence over executable NOT_READY. */
public final class ValidationBeforeReadinessSuite {
    private static final ObjectMapper JSON = new ObjectMapper();
    private static int checks;
    private static final List<String> failures = new ArrayList<>();
    private ValidationBeforeReadinessSuite() { }
    private static void need(boolean condition, String reason) {
        checks++; if (!condition) failures.add(reason);
    }
    private static ObjectNode fixture(String name) throws Exception {
        try (var stream = ValidationBeforeReadinessSuite.class.getResourceAsStream(
                "/sp/validation-before-readiness-r7-r3-r1/" + name + ".json")) {
            if (stream == null) throw new AssertionError(name);
            return (ObjectNode) JSON.readTree(stream);
        }
    }
    private static ObjectNode fact(ObjectNode wire, String variant) {
        for (var s : wire.path("statements"))
            if (s.path("variant").asText().equals(variant)) return (ObjectNode) s;
        throw new AssertionError(variant);
    }
    private static SpInput decode(ObjectNode wire) throws Exception {
        var result = new SpJsonDecoder(CobolLower.INPUT_LIMITS).decode(JSON.writeValueAsBytes(wire));
        if (!(result instanceof SpJsonDecoder.Decoded decoded)) throw new AssertionError("typed decode required: " + result);
        checks++; return decoded.input();
    }
    private static void check(SpInput input, LoweringResult.Status expected, String name) {
        var result = new CobolLowerer().lower(input, CobolLower.OPTIONS);
        System.out.println("R7_R3_R1_CASE=" + name + " STATUS=" + result.status());
        need(result.status() == expected, name + ": expected " + expected + ", got " + result.status());
        need(result.publication().isEmpty(), name + ": no AIR publication");
        need(result.admission().input().filter(input::equals).isPresent(), name + ": typed input preserved");
        if (expected == LoweringResult.Status.IMPLEMENTATION_LIMIT)
            need(result.admission().diagnostics().stream().anyMatch(d -> d.rule() == Admission.Rule.READINESS
                && d.requirement().contains("executable lowering NOT_READY")), name + ": explicit readiness reason");
        else need(result.admission().diagnostics().stream().noneMatch(d -> d.rule() == Admission.Rule.READINESS),
            name + ": factual inconsistency diagnosed before readiness");
    }
    public static void main(String[] args) throws Exception {
        checks = 0; failures.clear();
        for (var family : List.of("handler", "abend", "both")) {
            var input = decode(fixture(family));
            need(input.statements().stream().anyMatch(CicsHandlerFact.class::isInstance) == !family.equals("abend"), "handler presence " + family);
            need(input.statements().stream().anyMatch(CicsAbendFact.class::isInstance) == !family.equals("handler"), "event presence " + family);
            check(input, LoweringResult.Status.IMPLEMENTATION_LIMIT, "valid-" + family);
            var compilation = new SpCompilation(InventoryStatus.COMPLETE, List.of(input.unit()),
                List.of(new SpCompilation.UnitProduct(input, Optional.empty(),
                    input.dataDeclarations().stream().map(DataFact::id).toList(), List.of(), List.of(), List.of())));
            var composed = new CompilationLowerer().lower(compilation, CobolLower.OPTIONS);
            need(composed.status() == LoweringResult.Status.IMPLEMENTATION_LIMIT && composed.publication().isEmpty(),
                "compilation preserves NOT_READY without AIR " + family);
        }
        for (int mutation = 1; mutation <= 3; mutation++) {
            for (var family : List.of("historical", "handler", "abend", "both")) {
                var wire = fixture(family);
                switch (mutation) {
                    case 1 -> fact(wire, "CICS_PROGRAM_CONTROL").put("conditions", "DEFAULT_ENTRY_PREFIX");
                    case 2 -> ((ObjectNode) fact(wire, "CALL").path("normalContinuation")).put("statement", "statement:999999");
                    case 3 -> fact(wire, "CICS_FILE_CONTROL").put("targetMode", "OUTPUT");
                    default -> throw new AssertionError(mutation);
                }
                check(decode(wire), LoweringResult.Status.INVALID_INPUT, "M" + mutation + "-" + family);
            }
        }
        if (!failures.isEmpty()) throw new AssertionError(String.join("\n", failures));
        System.out.println("R7_R3_R1_CHECKS=" + checks + " BEHAVIOR_MUTATIONS=4/4");
    }
}
