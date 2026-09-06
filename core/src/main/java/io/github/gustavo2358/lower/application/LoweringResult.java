package io.github.gustavo2358.lower.application;

import io.github.gustavo2358.air.model.Publication;
import io.github.gustavo2358.air.model.Ids;
import io.github.gustavo2358.air.validation.ValidationResult;
import io.github.gustavo2358.lower.domain.SpInput;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/** Atomic outcome plus immutable source evidence, typed correlation and complete checker report. */
public record LoweringResult(Status status, Admission admission, Optional<Publication> publication,
        Optional<ValidationResult> validation, List<EntryLink> entries, List<StatementLink> statements,
        List<Limitation> limitations) {
    public static final String PROFILE = "minimal-entry-goback@1";
    public LoweringResult {
        Objects.requireNonNull(status); Objects.requireNonNull(admission);
        Objects.requireNonNull(publication); Objects.requireNonNull(validation);
        entries = List.copyOf(entries); statements = List.copyOf(statements); limitations = List.copyOf(limitations);
        if ((status == Status.SUCCESS) != publication.isPresent()) throw new IllegalArgumentException("atomic publication required");
        if (status == Status.SUCCESS && (admission.status() != Admission.Status.ADMITTED
                || validation.isEmpty() || !validation.orElseThrow().isStructurallyValid()))
            throw new IllegalArgumentException("success requires admission and real validation report");
    }
    public enum Status { SUCCESS, INVALID_INPUT, BLOCKED_LOWERING, UNSUPPORTED_SLICE, IMPLEMENTATION_LIMIT,
        OUTPUT_INVALID, VALIDATION_INCOMPLETE }
    public enum LimitCode { COORDINATES_UNAVAILABLE, INCLUDE_SITE_UNAVAILABLE, IDENTITY_LIMIT }
    public record Limitation(LimitCode code, String subject, String requirement) {
        public Limitation { Objects.requireNonNull(code); Objects.requireNonNull(subject); Objects.requireNonNull(requirement); }
    }
    public record EntryLink(SpInput.EntryId source, Ids.EntryId target, Ids.LabelId start, Ids.OriginId origin) {
        public EntryLink { Objects.requireNonNull(source); Objects.requireNonNull(target); Objects.requireNonNull(start); Objects.requireNonNull(origin); }
    }
    public record StatementLink(SpInput.StatementId source, Ids.OperationId target, Ids.LabelId label, Ids.OriginId origin) {
        public StatementLink { Objects.requireNonNull(source); Objects.requireNonNull(target); Objects.requireNonNull(label); Objects.requireNonNull(origin); }
    }
}
