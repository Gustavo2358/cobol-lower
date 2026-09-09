package io.github.gustavo2358.lower.adapters.air;

import io.github.gustavo2358.air.json.AirJson;
import io.github.gustavo2358.air.model.Operations;
import io.github.gustavo2358.air.validation.AirValidator;
import io.github.gustavo2358.lower.adapters.cli.CobolLower;
import io.github.gustavo2358.lower.adapters.sp.*;
import io.github.gustavo2358.lower.application.*;
import io.github.gustavo2358.lower.testing.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.MessageDigest;
import java.util.*;
import java.util.zip.GZIPInputStream;

/** Real merged upstream snapshots through the production file composition, with semantic and structural safety oracles. */
public final class ProductionPathSuite {
    private static int checks;
    private static void check(boolean value, String message) { if (!value) throw new AssertionError("PRODUCTION " + message); checks++; }
    private static byte[] fixture(boolean large) throws Exception {
        var name = large ? "production-shared-10000" : "production-400";
        try (var stream = new GZIPInputStream(ProductionPathSuite.class.getResourceAsStream("/sp/" + name + ".json.gz"))) {
            byte[] raw = stream.readAllBytes();
            check(HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(raw)).equals(large
                ? "fe6178078abe4269676e08602df3a50773f53971a4b35234dc29606ef656ab72"
                : "a59262f8ae9e7c2c9b0aa78b602020511b731146bb91b9f7fcfd16fa4317b64e"), "real SP snapshot hash");
            return raw;
        }
    }
    private static int cli(Path source, Path destination, ByteArrayOutputStream diagnostics) {
        diagnostics.reset();
        try (var err = new PrintStream(diagnostics, true, StandardCharsets.UTF_8)) {
            return CobolLower.run(new String[]{source.toString(), destination.toString()}, err);
        }
    }
    private static void success(Path directory) throws Exception {
        byte[] raw = fixture(false); check(raw.length > 100_000, "probe exceeds old byte limit");
        var source = directory.resolve("success.sp.json"); var destination = directory.resolve("success.air.json"); Files.write(source, raw);
        var diagnostics = new ByteArrayOutputStream();
        int code = cli(source, destination, diagnostics);
        check(code == CobolLower.SUCCESS, "large SP success exit0: " + diagnostics.toString(StandardCharsets.UTF_8));
        check(Files.isRegularFile(destination), "successful production output exists");
        byte[] bytes = Files.readAllBytes(destination); var codec = new AirJson(); var publication = codec.decode(bytes);
        check(bytes.length < 16 * 1024 * 1024, "success fits unchanged AirJson limit");
        check(AirValidator.validate(publication).issues().isEmpty(), "large output shared validator");
        var unit = publication.units().getFirst(); var sequence = unit.sequences().getFirst();
        check(unit.objects().size() == 400 && publication.storage().size() == 400, "400 declaration Objects Cells");
        check(sequence.instructions().size() == 400 && sequence.instructions().stream().allMatch(Operations.Assign.class::isInstance)
            && sequence.terminator() instanceof Operations.Return, "400 Assigns Return");
        check(Arrays.equals(bytes, codec.encode(publication)), "large production canonical round trip");
        System.out.println("PRODUCTION_SUCCESS_PROBE data=400 moves=400 sp_bytes=" + raw.length + " air_bytes=" + bytes.length + " exit=" + code);
    }
    private static void measurements(boolean production) throws Exception {
        byte[] raw = fixture(true);
        var limits = CobolLower.INPUT_LIMITS;
        var measurement = new SpJsonDecoder(limits).decodeMeasured(raw);
        check(measurement.result() instanceof SpJsonDecoder.Decoded, "10k decoder passes production bound: " + measurement.result());
        var result = new CobolLowerer().lower(((SpJsonDecoder.Decoded)measurement.result()).input(), production ? CobolLower.OPTIONS : ScalarSuite.OPTIONS);
        check(result.status() == LoweringResult.Status.SUCCESS, "10k lowering passes production admission: " + result.status());
        var publication = result.publication().orElseThrow();
        check(publication.units().getFirst().objects().size() == 1 && publication.storage().size() == 1
            && publication.units().getFirst().sequences().getFirst().instructions().size() == 10_000, "10k shared DATA counts");
        check(result.admission().statistics().referencesChecked() == 70_003, "10k indexed reference ledger");
        System.out.println("PRODUCTION_MEASUREMENT sp_bytes=" + raw.length + " nodes=" + measurement.statistics().jsonNodesVisited()
            + " physical_values=" + measurement.statistics().physicalValuesVisited() + " entities=" + result.admission().statistics().entitiesVisited());
    }
    private static void large(Path directory) throws Exception {
        measurements(true); // No retained Publication/DTO graph across the second real CLI invocation.
        var source = directory.resolve("large.sp.json"); Files.write(source, fixture(true));
        var destination = directory.resolve("large.air.json"); byte[] sentinel = {42, 17}; Files.write(destination, sentinel);
        var diagnostics = new ByteArrayOutputStream(); int code = cli(source, destination, diagnostics);
        check(code == CobolLower.CODEC && diagnostics.toString(StandardCharsets.UTF_8).contains("AIR codec IMPLEMENTATION_LIMIT"),
            "10k first limit is shared AirJson: " + diagnostics.toString(StandardCharsets.UTF_8));
        check(Arrays.equals(Files.readAllBytes(destination), sentinel), "10k codec limit preserves destination");
        try (var files = Files.list(directory)) { check(files.noneMatch(p -> p.getFileName().toString().endsWith(".tmp")), "10k no temporary residue"); }
        System.out.println("PRODUCTION_LIMIT_ORDER data=1 moves=10000 exit=" + code + " first_limit=AIR_JSON");
    }
    private static void guards(Path directory) throws Exception {
        var path = directory.resolve("deep.sp.json");
        Files.writeString(path, "{\"nested\":" + "[".repeat(65) + "0" + "]".repeat(65) + "}");
        var destination = directory.resolve("rejected.air.json"); Files.write(destination, new byte[]{37});
        var diagnostics = new ByteArrayOutputStream();
        check(cli(path, destination, diagnostics) == CobolLower.INPUT && diagnostics.toString(StandardCharsets.UTF_8).contains("IMPLEMENTATION_LIMIT")
            && Arrays.equals(Files.readAllBytes(destination), new byte[]{37}), "depth rejection remains atomic");
        check(CobolLower.INPUT_LIMITS.maxDepth() == 64 && AirJson.Limits.defaults().maximumDocumentBytes() == 16 * 1024 * 1024
            && AirJson.Limits.defaults().maximumDepth() == 128, "depth and shared codec defaults unchanged");
    }
    public static int run() throws Exception { return execute(Boolean.getBoolean("lower.performance") ? "all" : "ordinary"); }
    private static int execute(String mode) throws Exception {
        checks = 0; var directory = Files.createTempDirectory("lower-production-path-");
        try {
            if (mode.equals("measure")) measurements(false);
            else if (mode.equals("large")) large(directory);
            else { success(directory); if (!mode.equals("success")) guards(directory); if (mode.equals("all")) large(directory); }
        } finally {
            try (var files = Files.walk(directory)) { for (var path : files.sorted(Comparator.reverseOrder()).toList()) Files.delete(path); }
        }
        if (mode.equals("all")) {
            System.out.println("LOWER_CAPACITY_TESTS=" + CapacitySuite.run());
            CapacitySafetySuite.run();
        }
        return checks;
    }
    public static void main(String[] args) throws Exception { System.out.println("LOWER_PRODUCTION_TESTS=" + execute(args.length == 0 ? "all" : args[0])); }
}
