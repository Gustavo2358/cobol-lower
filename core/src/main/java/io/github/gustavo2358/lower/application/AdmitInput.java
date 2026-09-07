package io.github.gustavo2358.lower.application;

import io.github.gustavo2358.lower.domain.SpInput;

/** Inner application port; callers bring a closed typed snapshot, never a transport. */
public interface AdmitInput {
    record Limits(long maxEntities, int maxDiagnostics) {
        public Limits {
            if (maxEntities < 1 || maxDiagnostics < 1) throw new IllegalArgumentException("positive limits required");
        }
    }
    Admission admit(SpInput input, Limits limits);
}
