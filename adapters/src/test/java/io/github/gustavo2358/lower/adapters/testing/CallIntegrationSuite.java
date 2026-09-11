package io.github.gustavo2358.lower.adapters.testing;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.github.gustavo2358.air.json.AirJson;
import io.github.gustavo2358.air.json.AirJsonException;
import io.github.gustavo2358.air.validation.AirValidator;
import io.github.gustavo2358.lower.adapters.cli.CobolLower;
import io.github.gustavo2358.lower.adapters.sp.SpJsonDecoder;
import io.github.gustavo2358.lower.application.*;
import io.github.gustavo2358.lower.domain.SpInput;
import io.github.gustavo2358.lower.testing.CallOracle;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.function.Consumer;
import static io.github.gustavo2358.lower.domain.SpInput.*;

/** Real producer fixtures plus independently specified mutations; no CALL injection in E2E. */
public final class CallIntegrationSuite {
    private static final ObjectMapper JSON = new ObjectMapper();
    private static final SpJsonDecoder DECODER = new SpJsonDecoder(CobolLower.INPUT_LIMITS);
    private static int assertions;
    private static void check(boolean b, String why) { CallOracle.check(b, why); assertions++; }
    public static SpInput decoded(byte[] bytes) {
        var result = DECODER.decode(bytes); check(result instanceof SpJsonDecoder.Decoded, "strict SP1.3 decode: " + result);
        return ((SpJsonDecoder.Decoded) result).input();
    }
    private static byte[] fixture(String name) throws IOException {
        try (var stream = CallIntegrationSuite.class.getResourceAsStream("/sp/cp6/" + name + ".json")) {
            if (stream == null) throw new AssertionError(name); return stream.readAllBytes();
        }
    }
    private static LoweringResult lower(SpInput input) { return new CobolLowerer().lower(input, CobolLower.OPTIONS); }
    public static byte[] positive(byte[] raw, List<String> values, boolean computed, String name) {
        var input = decoded(raw); var result = lower(input); CallOracle.inspect(input, result, values, computed, name);
        var publication = result.publication().orElseThrow(); var codec = new AirJson();
        var bytes = codec.encode(publication); var restored = codec.decode(bytes);
        check(restored.equals(publication), "AIR semantic round-trip");
        check(Arrays.equals(bytes, codec.encode(restored)), "canonical AIR byte round-trip");
        check(AirValidator.validate(restored).equals(AirValidator.validate(publication)), "validator I-56 unchanged after round-trip");
        check(lower(decoded(raw)).publication().equals(result.publication()), "AIR deterministic repeated pipeline");
        return bytes;
    }
    private static void placementChallenge() throws IOException {
        var codec = new AirJson();
        var publication = lower(decoded(fixture("literal"))).publication().orElseThrow();
        var bytes = codec.encode(publication);
        var wire = (ObjectNode) JSON.readTree(bytes);
        // AIR wire remains physically well-formed; I-04 must reject a terminator in instructions.
        var sequences = wire.path("publication").path("units").get(0).path("sequences");
        var sequence = (ObjectNode) sequences.get(0);
        var invoke = sequence.get("terminator");
        ((com.fasterxml.jackson.databind.node.ArrayNode) sequence.get("instructions")).add(invoke);
        sequence.set("terminator", sequences.get(1).get("terminator"));
        try { codec.decode(JSON.writeValueAsBytes(wire)); throw new AssertionError("CP6 Invoke instruction admitted"); }
        catch (AirJsonException rejected) {
            check(rejected.code() == AirJsonException.Code.INVALID_IR && rejected.issues().stream().anyMatch(i -> i.rule().equals("I-04")), "Invoke-as-instruction semantic I-04 rejection");
        }
        check(Arrays.equals(bytes, codec.encode(codec.decode(bytes))), "placement mutation restored byte-exact and second GREEN");
        System.out.println("CALL_PLACEMENT_CHALLENGE=I-04; byte-exact restoration; second GREEN");
    }
    private static void reject(ObjectNode base, Consumer<ObjectNode> mutation, String label) throws IOException {
        var changed = base.deepCopy(); mutation.accept(changed);
        var result = DECODER.decode(JSON.writeValueAsBytes(changed));
        check(result instanceof SpJsonDecoder.Rejected r && r.diagnostic().code() == SpJsonDecoder.Code.INPUT_ERROR, "physical reject " + label + ": " + result);
    }
    private static ObjectNode call(ObjectNode d) { return (ObjectNode) d.path("statements").get(1); }
    private static ObjectNode move(ObjectNode d) { return (ObjectNode) d.path("statements").get(0); }
    private static void physical() throws IOException {
        var base = (ObjectNode) JSON.readTree(fixture("dynamic-x8"));
        for (String field : List.of("target", "syntax", "runtimeTarget", "runtimeUncertaintyCode", "normalContinuation", "surface", "effects", "outcomes")) {
            reject(base, d -> call(d).remove(field), "missing CALL " + field);
            reject(base, d -> call(d).putNull(field), "null CALL " + field);
        }
        reject(base, d -> call(d).put("unexpected", true), "unknown CALL field");
        reject(base, d -> call(d).put("runtimeTarget", "RESOLVED"), "wrong runtime enum");
        reject(base, d -> call(d).put("effects", "NONE"), "wrong effects enum");
        reject(base, d -> call(d).put("outcomes", "CLOSED"), "wrong outcome enum");
        reject(base, d -> call(d).put("syntax", 0), "numeric enum");
        reject(base, d -> ((ObjectNode)call(d).path("target")).put("kind", "UNKNOWN"), "unknown target variant");
        reject(base, d -> ((ObjectNode)call(d).path("target")).put("extra", "x"), "unknown target field");
        for (String field : List.of("using", "argumentCount", "returning", "onException", "notOnException", "onOverflow"))
            reject(base, d -> ((ObjectNode)call(d).path("surface")).remove(field), "surface missing " + field);
        reject(base, d -> ((ObjectNode)call(d).path("surface")).put("argumentCount", "0"), "count string coercion");
        reject(base, d -> ((ObjectNode)call(d).path("surface")).put("using", "NEVER"), "wrong surface enum");
        reject(base, d -> move(d).remove("textAdjustment"), "required nullable adjustment missing");
        for (String field : List.of("rule", "receiverExtent", "result", "provenance"))
            reject(base, d -> ((ObjectNode)move(d).path("textAdjustment")).remove(field), "adjustment missing " + field);
        reject(base, d -> ((ObjectNode)move(d).path("textAdjustment")).put("rule", "TRUNCATE"), "wrong adjustment rule");
        reject(base, d -> ((ObjectNode)move(d).path("textAdjustment")).put("receiverExtent", 8.5), "fractional extent");
        reject(base, d -> ((ObjectNode)move(d).path("textAdjustment").path("result")).put("logicalExtent", 5), "logical text extent coherence");
        // Use a compact representation so the duplicate challenge is independent of pretty-print spacing.
        var compact = JSON.writeValueAsString(base).replace("\"effects\":\"UNKNOWN\"", "\"effects\":\"UNKNOWN\",\"effects\":\"UNKNOWN\"");
        var duplicate = DECODER.decode(compact.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        check(duplicate instanceof SpJsonDecoder.Rejected r && r.diagnostic().code() == SpJsonDecoder.Code.INPUT_ERROR, "duplicate CALL field");
        var literal = (ObjectNode)JSON.readTree(fixture("literal"));
        for (String field : List.of("id", "text", "writtenText", "logicalValue", "provenance"))
            reject(literal, d -> ((ObjectNode)d.path("statements").get(0).path("target")).remove(field), "literal missing " + field);
        // Absence is preserved, not repaired from spelling.
        ((ObjectNode)literal.path("statements").get(0).path("target")).putNull("logicalValue");
        var missing = decoded(JSON.writeValueAsBytes(literal));
        check(((LiteralCallTarget)((CallFact)missing.statements().getFirst()).target()).logicalValue().isEmpty(), "literal knowledge absence retained");
        check(lower(missing).status() == LoweringResult.Status.UNSUPPORTED_SLICE, "unknown literal not lowered from spelling");
    }
    private static void negatives() throws IOException {
        for (String name : List.of("using", "returning", "on-exception", "not-on-exception", "on-overflow", "ambiguous", "unresolved", "refmod", "subscript", "truncation", "if", "evaluate", "goto", "perform")) {
            var input = decoded(fixture(name)); var result = lower(input);
            check(result.status() == LoweringResult.Status.UNSUPPORTED_SLICE && result.publication().isEmpty(), name + " rejected explicitly: " + result.status());
            var call = input.statements().stream().filter(CallFact.class::isInstance).map(CallFact.class::cast).findFirst().orElseThrow();
            check(call.effects() == CallEffects.UNKNOWN && call.outcomes() == CallOutcomes.OPEN, "knowledge preserved even on unsupported " + name);
            if (name.equals("using")) check(call.surface().using() == ClausePresence.PRESENT && call.surface().argumentCount().orElseThrow() > 0, "USING decoded");
            if (name.equals("returning")) check(call.surface().returning() == ClausePresence.PRESENT, "RETURNING decoded");
            if (name.equals("on-exception")) check(call.surface().onException() == ClausePresence.PRESENT, "ON EXCEPTION decoded");
            if (name.equals("not-on-exception")) check(call.surface().notOnException() == ClausePresence.PRESENT, "NOT ON EXCEPTION decoded");
            if (name.equals("on-overflow")) check(call.surface().onOverflow() == ClausePresence.PRESENT, "ON OVERFLOW decoded");
            if (name.equals("ambiguous")) {
                var reference = ((DataCallTarget)call.target()).reference();
                check(reference.binding().status() == ResolutionStatus.AMBIGUOUS && reference.binding().selected().isEmpty()
                    && reference.binding().candidates().size() == 2 && reference.binding().candidateNames().size() == 2
                    && reference.binding().reason().isPresent() && reference.wholeItemAccess().isEmpty(), "ambiguous binding preserved");
            }
        }
        check(lower(decoded(fixture("incomplete"))).status() == LoweringResult.Status.BLOCKED_LOWERING, "incomplete input blocks");
        var noNext = lower(decoded(fixture("no-continuation")));
        check(noNext.status() == LoweringResult.Status.BLOCKED_LOWERING && noNext.publication().isEmpty(), "missing continuation blocks");
    }
    private static void cli() throws IOException {
        var dir = Files.createTempDirectory("cp6-cli-");
        try {
            var input = dir.resolve("input.json"); var output = dir.resolve("output.air.json");
            var err = new PrintStream(new ByteArrayOutputStream());
            for (String name : List.of("dynamic-x8", "literal")) {
                Files.write(input, fixture(name)); check(CobolLower.run(new String[]{input.toString(), output.toString()}, err) == 0, "real CLI success");
                check(new AirJson().decode(Files.readAllBytes(output)).equals(lower(decoded(fixture(name))).publication().orElseThrow()), "CLI whole AIR committed");
                Files.delete(output);
            }
            Files.write(input, fixture("using"));
            check(CobolLower.run(new String[]{input.toString(), output.toString()}, err) == 4 && !Files.exists(output), "unsupported CALL no destination");
            Files.writeString(input, "{invalid");
            check(CobolLower.run(new String[]{input.toString(), output.toString()}, err) == 3 && !Files.exists(output), "invalid SP no destination");
        } finally { try (var files = Files.list(dir)) { for (var file : files.toList()) Files.delete(file); } Files.delete(dir); }
    }
    public static int run() throws IOException {
        for (String name : List.of("dynamic-x8", "two-data")) positive(fixture(name), List.of("PROGA   "), true, "unused");
        positive(fixture("dynamic-x5"), List.of("PROGA"), true, "unused");
        positive(fixture("dynamic-other"), List.of("OTHER   "), true, "unused");
        positive(fixture("dynamic-no-move"), List.of(), true, "unused");
        positive(fixture("literal"), List.of(), false, "PROGA");
        positive(fixture("literal-raw"), List.of(), false, " ProGa ");
        physical(); negatives(); cli(); placementChallenge(); System.out.println("LOWER_CALL_INTEGRATION_TESTS=" + assertions); return assertions;
    }
    public static void main(String[] args) throws IOException {
        if (args.length == 0) { run(); return; }
        var raw = Files.readAllBytes(Path.of(args[0]));
        var bytes = positive(raw, args[2].equals("literal") ? List.of() : List.of("PROGA   "), !args[2].equals("literal"), "PROGA");
        Files.write(Path.of(args[1]), bytes);
        System.out.println("REAL_E2E STRUCTURALLY_VALID I-56 PRESENT; AIR round-trip and canonical bytes PASS");
    }
}
