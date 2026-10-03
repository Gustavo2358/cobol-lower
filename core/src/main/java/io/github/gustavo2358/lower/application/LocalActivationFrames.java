package io.github.gustavo2358.lower.application;

import io.github.gustavo2358.air.model.*;
import java.util.List;
import io.github.gustavo2358.air.model.Ids.UnitId;

/** The same frame contract for topology and structural source facts. */
final class LocalActivationFrames {
    private LocalActivationFrames() { }
    static Envelopes.Envelope fallback(UnitId unit) {
        var memory=new Scopes.WithinMemory(new Scopes.AllMemory(unit.publication(),true));
        return new Envelopes.Envelope(new Envelopes.MemoryEnvelope(List.of(),memory,List.of(),memory,List.of()),
            new Control.ControlEnvelope(List.of(),new Scopes.WithinControl(new Scopes.UnitControl(unit,true,true,true,true,true,true))),
            new Envelopes.DependencyEnvelope(List.of(),Scopes.AnyResource.INSTANCE));
    }
}
