package io.github.gustavo2358.lower.adapters.sp;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

/** Caller-owned path, bounded read, closed resource. Source provenance is never opened. */
public final class SpFileInput {
    private final SpJsonDecoder.Limits limits;
    private final SpJsonDecoder decoder;
    public SpFileInput(SpJsonDecoder.Limits limits) {
        this.limits = Objects.requireNonNull(limits); decoder = new SpJsonDecoder(limits);
    }
    public SpJsonDecoder.Result read(Path path) {
        if (path == null) return error();
        try (var input = Files.newInputStream(path)) {
            byte[] bytes = input.readNBytes(limits.maxBytes());
            if (input.read() != -1) return new SpJsonDecoder.Rejected(new SpJsonDecoder.Diagnostic(SpJsonDecoder.Code.IMPLEMENTATION_LIMIT, "physical", "$ file byte limit"));
            return decoder.decode(bytes);
        } catch (IOException ex) { return error(); }
    }
    private static SpJsonDecoder.Rejected error() {
        return new SpJsonDecoder.Rejected(new SpJsonDecoder.Diagnostic(SpJsonDecoder.Code.INPUT_ERROR, "physical", "$ file I/O"));
    }
}
