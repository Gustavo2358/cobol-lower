package io.github.gustavo2358.lower.application;

import io.github.gustavo2358.air.model.*;
import io.github.gustavo2358.air.model.Ids.*;
import java.util.*;

/** Local AIR claims only; declaration/return unknown dimensions remain explicit. */
final class ScalarEvidence {
    private ScalarEvidence() { }
    static Evidence.Precision assign(OperationId operation) {
        var exact = new Evidence.Claim(new Scopes.EntityScope(List.of(operation)), Evidence.PrecisionStatus.EXACT, List.of());
        return new Evidence.Precision(exact, exact, exact, exact, exact);
    }
    static Evidence.Precision limited(LocalIds ids, PublicationId publication, Id subject, String key, OriginId origin,
            Evidence.Dimension known, List<Evidence.Uncertainty> uncertainties) {
        var claims = new EnumMap<Evidence.Dimension, Evidence.Claim>(Evidence.Dimension.class);
        for (var dimension : Evidence.Dimension.values()) {
            var scope = new Scopes.EntityScope(List.of(subject));
            if (dimension == known) claims.put(dimension, new Evidence.Claim(scope, Evidence.PrecisionStatus.EXACT, List.of()));
            else {
                var id = new UncertaintyId(publication, ids.id("uncertainty", "scalar-unproved-" + dimension.name(), (subject instanceof ObjectId ? "object" : "operation"), key));
                uncertainties.add(new Evidence.Uncertainty(id, "cobol-lower:UNPROVED_" + dimension.name(), List.of(dimension), scope,
                    "scalar-text-move@1 does not certify " + dimension.name() + " for this entity.", origin));
                claims.put(dimension, new Evidence.Claim(scope, Evidence.PrecisionStatus.UNAVAILABLE, List.of(id)));
            }
        }
        return new Evidence.Precision(claims.get(Evidence.Dimension.CONTROL), claims.get(Evidence.Dimension.STORAGE),
            claims.get(Evidence.Dimension.EFFECTS), claims.get(Evidence.Dimension.VALUES), claims.get(Evidence.Dimension.DEPENDENCIES));
    }
    static List<UncertaintyId> reasons(Evidence.Precision precision) {
        var result = new ArrayList<UncertaintyId>();
        for (var c : List.of(precision.control(), precision.storage(), precision.effects(), precision.values(), precision.dependencies())) result.addAll(c.reasons());
        return result;
    }
    static Evidence.CoverageItem item(PublicationId publication, String kind, String handle, OriginId origin, List<Id> outputs) {
        return new Evidence.CoverageItem("sp-scalar@1/" + publication.localId() + "/" + kind + "/" + handle,
            origin, Evidence.CoverageStatus.MODELED, outputs, List.of(), Optional.empty());
    }
}
