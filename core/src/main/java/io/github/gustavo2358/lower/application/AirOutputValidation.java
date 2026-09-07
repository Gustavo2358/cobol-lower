package io.github.gustavo2358.lower.application;

import io.github.gustavo2358.air.model.Publication;
import io.github.gustavo2358.air.validation.AirValidator;
import io.github.gustavo2358.air.validation.ValidationResult;

/** Output validation boundary; does not certify producer semantics. */
public final class AirOutputValidation {
    private AirOutputValidation() {}

    public static ValidationResult validate(Publication publication) {
        return AirValidator.validate(publication);
    }
}
