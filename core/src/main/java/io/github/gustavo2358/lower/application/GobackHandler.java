package io.github.gustavo2358.lower.application;

import io.github.gustavo2358.air.model.*;
import io.github.gustavo2358.air.model.Ids.*;
import io.github.gustavo2358.lower.domain.SpInput;
import java.util.*;

/** Same local current-invocation return rule as CP3; other dimensions remain unproved. */
final class GobackHandler {
    static Operations.Return translate(SpInput.GobackFact goback, UnitId unit, LocalIds ids, SourceOrigins origins,
            List<Evidence.Uncertainty> uncertainties) {
        var operation = new OperationId(unit, ids.id("operation", "goback-return", unit.localId(), goback.header().id().handle()));
        var origin = origins.source("statement", goback.header().id().handle(), goback.header().provenance());
        var precision = ScalarEvidence.limited(ids, unit.publication(), operation, goback.header().id().handle(), origin, Evidence.Dimension.CONTROL, uncertainties);
        var header = new Operations.Header(operation, origin, Evidence.CoverageStatus.MODELED, precision, ScalarEvidence.reasons(precision));
        return new Operations.Return(header, List.of());
    }
}
