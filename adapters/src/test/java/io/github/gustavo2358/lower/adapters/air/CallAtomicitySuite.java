package io.github.gustavo2358.lower.adapters.air;

import io.github.gustavo2358.air.json.*;
import io.github.gustavo2358.air.validation.ValidationOptions;
import io.github.gustavo2358.lower.application.CobolLowerer;
import io.github.gustavo2358.lower.testing.*;
import java.io.IOException;
import java.nio.file.*;

/** Real Invoke output through physical failure seams; no partial destination. */
final class CallAtomicitySuite {
    static int run() throws IOException {
        var publication = new CobolLowerer().lower(CallInputs.create(1, 1, 8, "PROGA", false), ScalarSuite.OPTIONS).publication().orElseThrow();
        var dir = Files.createTempDirectory("call-atomicity-"); var destination = dir.resolve("result.air.json");
        try {
            Files.writeString(destination, "previous-complete-destination");
            var limited = new AirJson(new AirJson.Limits(1, 128), ValidationOptions.defaults());
            try { new AirFileOutput(limited, new AirFileOutput.FileOperations()).write(publication, destination); throw new AssertionError("codec should fail"); }
            catch (AirJsonException expected) { CallOracle.check(expected.code() == AirJsonException.Code.RESOURCE_LIMIT, "real codec resource failure"); }
            CallOracle.check(Files.readString(destination).equals("previous-complete-destination"), "codec failure preserves destination");
            for (boolean failWrite : new boolean[]{true, false}) {
                var operations = new AirFileOutput.FileOperations() {
                    @Override void write(Path path, byte[] bytes) throws IOException {
                        if (failWrite) { Files.writeString(path, "partial"); throw new IOException("injected write failure"); }
                        super.write(path, bytes);
                    }
                    @Override void move(Path source, Path target, CopyOption... options) throws IOException { throw new IOException("injected move failure"); }
                };
                try { new AirFileOutput(new AirJson(), operations).write(publication, destination); throw new AssertionError("I/O should fail"); }
                catch (IOException expected) { CallOracle.check(expected.getMessage().startsWith("injected"), "physical failure reached"); }
                CallOracle.check(Files.readString(destination).equals("previous-complete-destination"), "failed write preserves old destination");
                try (var files = Files.list(dir)) { CallOracle.check(files.count() == 1, "no temporary residue"); }
            }
        } finally { Files.deleteIfExists(destination); Files.delete(dir); }
        return 8;
    }
}
