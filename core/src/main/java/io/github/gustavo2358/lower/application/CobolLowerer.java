package io.github.gustavo2358.lower.application;

import io.github.gustavo2358.lower.domain.SpInput;

/** Focused profile dispatch at the in-memory use case. */
public final class CobolLowerer implements LowerInput {
    @Override public LoweringResult lower(SpInput input, Options options) {
        if (input != null && input.statements().stream().anyMatch(s -> s instanceof SpInput.PerformFact || s instanceof SpInput.IfFact || s instanceof SpInput.CallFact))
            return new SupportedProgramLowerer().lower(input, options);
        if (input != null && (!input.dataDeclarations().isEmpty() || input.statements().stream().anyMatch(SpInput.MoveFact.class::isInstance)))
            return new ScalarMoveLowerer().lower(input, options);
        return new EntryGobackLowerer().lower(input, options);
    }
}
