package io.github.gustavo2358.lower.adapters.sp;

import io.github.gustavo2358.lower.application.Admission;
import io.github.gustavo2358.lower.application.AdmitInput;
import java.nio.file.Path;
import java.util.Objects;

/** Driving adapter: one common inner admission port, no duplicate semantic checks. */
public final class FileAdmission {
    public sealed interface Result permits Checked, PhysicalFailure { }
    public record Checked(Admission admission) implements Result { public Checked { Objects.requireNonNull(admission); } }
    public record PhysicalFailure(SpJsonDecoder.Diagnostic diagnostic) implements Result { }
    private final SpFileInput reader;
    private final AdmitInput port;
    public FileAdmission(SpFileInput reader, AdmitInput port) { this.reader = Objects.requireNonNull(reader); this.port = Objects.requireNonNull(port); }
    public Result admit(Path path, AdmitInput.Limits limits) {
        return switch (reader.read(path)) {
            case SpJsonDecoder.Decoded decoded -> new Checked(port.admit(decoded.input(), limits));
            case SpJsonDecoder.Rejected failure -> new PhysicalFailure(failure.diagnostic());
        };
    }
}
