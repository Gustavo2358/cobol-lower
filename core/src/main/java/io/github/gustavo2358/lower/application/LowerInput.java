package io.github.gustavo2358.lower.application;

import io.github.gustavo2358.air.validation.ValidationOptions;
import io.github.gustavo2358.lower.domain.SpInput;
import java.util.Objects;

/** In-memory use case; all driving adapters must cross this same admission boundary. */
public interface LowerInput {
    LoweringResult lower(SpInput input, Options options);
    /** maximumIdentityCharacters bounds the final 32-character PublicationId, not canonical input volume. */
    enum PublicationPolicy { EXECUTABLE_ONLY, BOUNDED_POSITIVE }
    record Options(AdmitInput.Limits admission, int maximumIdentityCharacters, ValidationOptions validation,
                   PublicationPolicy publicationPolicy) {
        public Options(AdmitInput.Limits admission,int maximumIdentityCharacters,ValidationOptions validation) {
            this(admission,maximumIdentityCharacters,validation,PublicationPolicy.EXECUTABLE_ONLY);
        }
        public Options {
            Objects.requireNonNull(admission); Objects.requireNonNull(validation); Objects.requireNonNull(publicationPolicy);
            if (maximumIdentityCharacters < 1) throw new IllegalArgumentException("positive identity limit required");
        }
    }
}
