package io.github.gustavo2358.lower.adapters.sp;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

/** Caller-owned path, complete read, closed resource. Source provenance is never opened. */
public final class SpFileInput {
    private final SpJsonDecoder decoder;
    public SpFileInput(SpJsonDecoder.Limits limits) {
        decoder = new SpJsonDecoder(Objects.requireNonNull(limits));
    }
    public SpJsonDecoder.Result read(Path path) {
        if (path == null) return error();
        try (var input = Files.newInputStream(path)) {
            byte[] bytes = input.readAllBytes();
            return decoder.decode(bytes);
        } catch (IOException ex) { return error(); }
    }
    private static SpJsonDecoder.Rejected error() {
        return new SpJsonDecoder.Rejected(new SpJsonDecoder.Diagnostic(SpJsonDecoder.Code.INPUT_ERROR, "physical", "$ file I/O"));
    }
}
