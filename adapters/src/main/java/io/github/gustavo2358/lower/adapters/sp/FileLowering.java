package io.github.gustavo2358.lower.adapters.sp;

import io.github.gustavo2358.lower.application.LowerInput;
import io.github.gustavo2358.lower.application.LoweringResult;
import java.nio.file.Path;
import java.util.Objects;

/** Driving adapter; physical input then the same transport-independent lowering port. */
public final class FileLowering {
    public sealed interface Result permits Lowered, PhysicalFailure { }
    public record Lowered(LoweringResult result) implements Result { public Lowered { Objects.requireNonNull(result); } }
    public record PhysicalFailure(SpJsonDecoder.Diagnostic diagnostic) implements Result { }
    private final SpFileInput reader;
    private final LowerInput port;
    public FileLowering(SpFileInput reader, LowerInput port) { this.reader = Objects.requireNonNull(reader); this.port = Objects.requireNonNull(port); }
    public Result lower(Path path, LowerInput.Options options) {
        return switch (reader.read(path)) {
            case SpJsonDecoder.Decoded decoded -> new Lowered(port.lower(decoded.input(), options));
            case SpJsonDecoder.Rejected failure -> new PhysicalFailure(failure.diagnostic());
        };
    }
}
