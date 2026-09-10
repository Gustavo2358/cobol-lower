package io.github.gustavo2358.lower.adapters.air;

import io.github.gustavo2358.air.json.*;
import io.github.gustavo2358.lower.adapters.cli.CobolLower;
import io.github.gustavo2358.lower.adapters.sp.*;
import io.github.gustavo2358.lower.application.*;
import io.github.gustavo2358.lower.testing.*;
import java.io.*;
import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

/** Shared codec bytes and atomic failure under explicit operational budgets, through real CLI composition. */
final class ScalarOutputSuite {
    private static int checks;
    private static void check(boolean b, String why) { if (!b) throw new AssertionError("SCALAR_OUTPUT " + why); checks++; }
    private static final class CountingFiles extends AirFileOutput.FileOperations {
        int calls;
        @Override Path temporary(Path parent) throws IOException { calls++; return super.temporary(parent); }
    }
    static int run() throws Exception {
        checks = 0; var input = Path.of(ScalarOutputSuite.class.getResource("/sp/scalar-move-1.2.0.json").toURI());
        var driver = new FileLowering(new SpFileInput(CobolLower.INPUT_LIMITS), new CobolLowerer());
        var result = ((FileLowering.Lowered) driver.lower(input, CobolLower.OPTIONS)).result();
        var codec = new AirJson(); var bytes = codec.encode(result.publication().orElseThrow());
        var dir = Files.createTempDirectory("lower-scalar-output-");
        try {
            var output = dir.resolve("air.json"); var diagnostics = new ByteArrayOutputStream();
            try (var err = new PrintStream(diagnostics, true, StandardCharsets.UTF_8)) {
                int code = CobolLower.run(new String[]{input.toString(), output.toString()}, err);
                check(code == 0 && Arrays.equals(bytes, Files.readAllBytes(output)), "CLI bytes exactly shared AirJson");
            }
            // Preserve the historical 16 MiB negative as an explicit budget; upstream defaults no longer impose it.
            var large = ScalarSuite.success(ScalarInputs.create(1, 2500));
            var files = new CountingFiles();
            var outputAdapter = new AirFileOutput(new AirJson(new AirJson.Limits(16 * 1024 * 1024, 128), io.github.gustavo2358.air.validation.ValidationOptions.defaults()), files);
            var fixed = new FileLowering(new SpFileInput(CobolLower.INPUT_LIMITS), (ignored, options) -> large);
            diagnostics.reset();
            try (var err = new PrintStream(diagnostics, true, StandardCharsets.UTF_8)) {
                int code = CobolLower.run(new String[]{input.toString(), output.toString()}, err, fixed, ScalarSuite.OPTIONS, outputAdapter);
                check(code == CobolLower.CODEC && diagnostics.toString(StandardCharsets.UTF_8).contains("AIR codec RESOURCE_LIMIT"), "explicit codec budget exit5 typed failure");
            }
            check(files.calls == 0 && Arrays.equals(bytes, Files.readAllBytes(output)), "encode limit before temp preserves old destination");
            var fresh = dir.resolve("fresh.air.json");
            try { outputAdapter.write(large.publication().orElseThrow(), fresh); throw new AssertionError("must exceed explicit shared codec budget"); }
            catch (AirJsonException ex) { check(ex.code() == AirJsonException.Code.RESOURCE_LIMIT && !Files.exists(fresh) && files.calls == 0, "no fresh partial file on codec limit"); }
            try (var contents = Files.list(dir)) { check(contents.count() == 1, "no temp output residue"); }
            check(AirJson.Limits.defaults().maximumDocumentBytes() == Integer.MAX_VALUE && AirJson.Limits.defaults().maximumDepth() == Integer.MAX_VALUE, "approved upstream defaults have no artificial document/depth cap");
        } finally {
            try (var paths = Files.walk(dir)) { for (var path : paths.sorted(Comparator.reverseOrder()).toList()) Files.delete(path); }
        }
        if (Boolean.getBoolean("lower.performance")) {
            int previous = 0;
            for (int n : List.of(250, 500)) {
                long start = System.nanoTime(); var r = ScalarSuite.success(ScalarInputs.create(n, n));
                byte[] air = codec.encode(r.publication().orElseThrow());
                check(codec.decode(air).equals(r.publication().orElseThrow()), "scale shared codec exact model");
                if (previous > 0) check(air.length > previous && air.length < previous * 2L + 100_000, "scale AIR bytes near linear");
                previous = air.length;
                System.out.println("SCALAR_AIR_SCALE data=" + n + " moves=" + n + " objects=" + n + " cells=" + n + " assigns=" + n
                    + " bytes=" + air.length + " elapsed_nanos=" + (System.nanoTime()-start));
            }
            var shared = ScalarSuite.success(ScalarInputs.create(1, 1000));
            byte[] air = codec.encode(shared.publication().orElseThrow());
            check(codec.decode(air).equals(shared.publication().orElseThrow()), "shared DATA codec round trip");
            System.out.println("SCALAR_AIR_SCALE data=1 moves=1000 objects=1 cells=1 assigns=1000 bytes=" + air.length);
        }
        return checks;
    }
}
