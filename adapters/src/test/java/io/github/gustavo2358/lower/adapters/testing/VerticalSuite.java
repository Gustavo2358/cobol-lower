package io.github.gustavo2358.lower.adapters.testing;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.github.gustavo2358.air.model.*;
import io.github.gustavo2358.air.validation.AirValidator;
import io.github.gustavo2358.lower.adapters.sp.*;
import io.github.gustavo2358.lower.application.*;
import io.github.gustavo2358.lower.testing.SpFixtures;
import io.github.gustavo2358.lower.testing.LoweringSuite;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/** FIRST-LOWER observations independently applied to both driving paths. */
public final class VerticalSuite {
    private static int count;
    private VerticalSuite() { }
    private static void check(boolean condition, String message) { if (!condition) throw new AssertionError("CP4 " + message); count++; }
    private static void observe(LoweringResult result) {
        check(result.status() == LoweringResult.Status.SUCCESS, "vertical FIRST-LOWER success");
        var p = result.publication().orElseThrow(); var u = p.units().getFirst();
        check(p.airVersion().equals(SemanticVersion.AIR_2_0_0) && p.units().size() == 1 && u.body() == Unit.BodyAvailability.AVAILABLE, "vertical AIR2 available unit");
        check(u.entries().size() == 1 && u.sequences().size() == 1, "vertical one entry and sequence");
        var e = u.entries().getFirst(); var s = u.sequences().getFirst();
        check(s.instructions().isEmpty() && s.terminator() instanceof Operations.Return && ((Operations.Return)s.terminator()).values().isEmpty(), "vertical Return never Halt");
        check(e.initialLabel().equals(Optional.of(s.label())) && e.signature().parameters().known().isEmpty()
                && e.signature().parameters().remainder() == Interactions.NoRemainder.INSTANCE && e.signature().results().known().isEmpty()
                && e.signature().results().remainder() == Interactions.NoRemainder.INSTANCE, "vertical known closed zero signature and start");
        check(p.coverage().inventory() == Evidence.InventoryStatus.PARTIAL && u.coverage().inventory() == Evidence.InventoryStatus.PARTIAL
                && p.uncertainties().stream().anyMatch(x -> x.code().equals("cobol-lower:ALTERNATE_ENTRIES_NOT_PROJECTED") && p.coverage().uncertainties().contains(x.id())), "vertical alternate gap retained in AIR");
        check(result.entries().getFirst().source().equals(SpFixtures.minimal().entryInventory().entries().getFirst().id())
                && result.statements().getFirst().source().equals(SpFixtures.minimal().statements().getFirst().header().id())
                && result.entries().getFirst().start().equals(result.statements().getFirst().label()), "vertical independent identity correlation");
        check(result.validation().orElseThrow().equals(AirValidator.validate(p)) && result.validation().orElseThrow().isStructurallyValid(), "vertical complete validator result");
    }
    public static int run(byte[] golden) throws Exception {
        var mapper = new ObjectMapper(); var port = new EntryGobackLowerer(); int[] calls = {0};
        var reader = new SpFileInput(new SpJsonDecoder.Limits(64));
        var driver = new FileLowering(reader, (input, options) -> { calls[0]++; return port.lower(input, options); });
        var file = Files.createTempFile("lower-vertical-", ".json");
        try {
            Files.write(file, golden);
            var physical = driver.lower(file, LoweringSuite.OPTIONS);
            check(physical instanceof FileLowering.Lowered, "file driver must invoke lowering port");
            var actual = ((FileLowering.Lowered)physical).result(); var independent = port.lower(SpFixtures.minimal(), LoweringSuite.OPTIONS);
            observe(actual); observe(independent);
            check(actual.equals(independent), "all file and memory evidence/correlation/publication equal");
            check(calls[0] == 1, "exactly one shared port call per valid physical input");
            var reordered = reverse(mapper.readTree(golden), mapper);
            Files.writeString(file, mapper.writerWithDefaultPrettyPrinter().writeValueAsString(reordered));
            check(((FileLowering.Lowered)driver.lower(file, LoweringSuite.OPTIONS)).result().equals(actual), "property order and whitespace never alter semantic identity");
            var bad = (ObjectNode)mapper.readTree(golden);
            ((ObjectNode)bad.path("entryInventory").path("entries").get(0).path("start")).put("statement", "statement:999");
            Files.write(file, mapper.writeValueAsBytes(bad));
            var rejected = ((FileLowering.Lowered)driver.lower(file, LoweringSuite.OPTIONS)).result();
            check(rejected.status() == LoweringResult.Status.INVALID_INPUT && rejected.publication().isEmpty()
                    && rejected.admission().diagnostics().stream().anyMatch(x -> x.rule() == Admission.Rule.ENTRY_START), "file cannot repair semantic start");
            var decoded = ((SpJsonDecoder.Decoded)new SpJsonDecoder(new SpJsonDecoder.Limits(64)).decode(mapper.writeValueAsBytes(bad))).input();
            check(rejected.equals(port.lower(decoded, LoweringSuite.OPTIONS)), "contradictory input gets identical port result");
            int beforeFailure = calls[0];
            for (String payload : List.of("{", new String(golden, java.nio.charset.StandardCharsets.UTF_8).replace("1.1.0", "9.0.0"))) {
                Files.writeString(file, payload);
                check(driver.lower(file, LoweringSuite.OPTIONS) instanceof FileLowering.PhysicalFailure, "physical failure distinct from lowering failure");
                check(calls[0] == beforeFailure, "physical rejection cannot invoke semantic port");
            }
            Files.write(file, golden);
            Files.writeString(file, new String(golden, java.nio.charset.StandardCharsets.UTF_8) + " {\"unexpected\":true}");
            check(new FileLowering(new SpFileInput(new SpJsonDecoder.Limits(64)), port).lower(file, LoweringSuite.OPTIONS)
                    instanceof FileLowering.PhysicalFailure failure && failure.diagnostic().code() == SpJsonDecoder.Code.INPUT_ERROR,
                    "valid prefix plus trailing input must not become a successful Publication");
            Files.delete(file);
            observe(actual);
            check(actual.equals(independent) && actual.admission().input().orElseThrow().equals(SpFixtures.minimal()), "closed/deleted transport cannot change retained AIR or source evidence");
            check(driver.lower(file, LoweringSuite.OPTIONS) instanceof FileLowering.PhysicalFailure, "deleted transport reports I/O failure");
        } finally { Files.deleteIfExists(file); }
        return count;
    }
    private static JsonNode reverse(JsonNode node, ObjectMapper mapper) {
        if (node.isObject()) {
            ObjectNode result = mapper.createObjectNode(); var names = new ArrayList<String>(); node.properties().forEach(entry -> names.add(entry.getKey())); Collections.reverse(names);
            for (String name : names) result.set(name, reverse(node.get(name), mapper)); return result;
        }
        if (node.isArray()) { var result = mapper.createArrayNode(); for (var child : node) result.add(reverse(child, mapper)); return result; }
        return node.deepCopy();
    }
}
