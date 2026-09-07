package io.github.gustavo2358.lower.application;

import io.github.gustavo2358.air.validation.ValidationOptions;
import io.github.gustavo2358.lower.domain.SpInput;
import java.util.Objects;

/** In-memory use case; all driving adapters must cross this same admission boundary. */
public interface LowerInput {
    LoweringResult lower(SpInput input, Options options);
    record Options(AdmitInput.Limits admission, int maximumIdentityCharacters, ValidationOptions validation) {
        public Options {
            Objects.requireNonNull(admission); Objects.requireNonNull(validation);
            if (maximumIdentityCharacters < 1) throw new IllegalArgumentException("positive identity limit required");
        }
    }
}
