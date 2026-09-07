package io.github.gustavo2358.lower.application;

import io.github.gustavo2358.lower.domain.SpInput;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/** Admission is not lowering success and never contains an AIR Publication. */
public record Admission(Status status, Optional<SpInput> input, List<Diagnostic> diagnostics,
                        Statistics statistics, boolean diagnosticsTruncated) {
    public Admission {
        Objects.requireNonNull(status); Objects.requireNonNull(input); Objects.requireNonNull(statistics);
        diagnostics = List.copyOf(diagnostics);
    }
    public enum Status { ADMITTED, INVALID_INPUT, BLOCKED_LOWERING, UNSUPPORTED_SLICE, IMPLEMENTATION_LIMIT }
    public enum Phase { INPUT_VALIDATION, ADMISSION }
    public enum Severity { ERROR }
    public enum Rule { INPUT_REQUIRED, IDENTITY, DUPLICATE_ID, PROGRAM_POINT, ENTRY_START, SIGNATURE,
        ENTRY_STATE, ENTRY_INVENTORY, CONTAINMENT, STRUCTURE, GAP, COVERAGE, READINESS, PROFILE_FACT,
        PROFILE_SHAPE, LIMIT }
    public record Diagnostic(Rule rule, Phase phase, Severity severity, Optional<SpInput.UnitKey> unit,
                             String subject, Optional<SpInput.Provenance> provenance, String requirement) {
        public Diagnostic {
            Objects.requireNonNull(rule); Objects.requireNonNull(phase); Objects.requireNonNull(severity);
            Objects.requireNonNull(unit); Objects.requireNonNull(subject); Objects.requireNonNull(provenance);
            Objects.requireNonNull(requirement);
        }
    }
    public record Statistics(long entitiesVisited, long referencesChecked, long provenanceComponents) { }
}
