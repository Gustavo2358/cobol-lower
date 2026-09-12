package io.github.gustavo2358.lower.application;

import io.github.gustavo2358.air.model.*;
import io.github.gustavo2358.air.model.Ids.*;
import io.github.gustavo2358.lower.domain.SpInput;
import java.util.*;

/** Transport upstream evidence verbatim; distinct identities are never a disjointness proof. */
final class StoragePremise {
    static Proofs.Premise translate(SpInput.IndependentStorageSet proof, ScalarDataTranslator.Result data,
            UnitId unit, LocalIds ids, SourceOrigins origins) {
        var origin = origins.source("storage-independence", unit.localId(), proof.provenance().orElseThrow());
        var members = new ArrayList<StorageId>();
        for (var member : proof.members()) members.add(data.index().get(member).storage());
        return new Proofs.Premise(new PremiseId(unit.publication(), ids.id("premise", "sp-storage-independence", unit.localId(), proof.rule().name())),
            proof.authority(), "SP rule: " + proof.rule().name(), origin, new Proofs.DisjointStorage(members));
    }
}
