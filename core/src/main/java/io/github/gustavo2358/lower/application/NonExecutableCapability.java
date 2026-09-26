package io.github.gustavo2358.lower.application;

import io.github.gustavo2358.lower.domain.SpInput;
import java.util.Optional;
import java.util.Objects;

/** A validated source fact whose execution is deliberately absent from this projection. */
public record NonExecutableCapability(SpInput.StatementId statement, Kind kind,
        SpInput.Provenance provenance) {
    public enum Kind { CICS_HANDLER, CICS_ABEND, CICS_COMMAND }
    public NonExecutableCapability { Objects.requireNonNull(statement); Objects.requireNonNull(kind); Objects.requireNonNull(provenance); }
    public SpInput.ExecutableLowering executableLowering() { return SpInput.ExecutableLowering.NOT_READY; }
    static Optional<NonExecutableCapability> of(SpInput.StatementFact fact,RegionalStorageAdmission.Index storage) {
        Kind kind = switch (fact) {
            case SpInput.CicsHandlerFact handler -> handler.registrationEffects().isPresent()&&storage!=null&&storage.owner().controlTopology().isPresent()?null:Kind.CICS_HANDLER;
            case SpInput.CicsAbendFact ignored -> Kind.CICS_ABEND;
            case SpInput.CicsCommandFact command -> CicsCommandMemory.ready(command,storage)?null:Kind.CICS_COMMAND;
            default -> null;
        };
        return kind == null ? Optional.empty() : Optional.of(new NonExecutableCapability(fact.header().id(),kind,fact.header().provenance()));
    }
}
