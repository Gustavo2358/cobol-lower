package io.github.gustavo2358.lower.application;

import io.github.gustavo2358.air.model.*;
import io.github.gustavo2358.air.model.Ids.*;
import io.github.gustavo2358.lower.domain.SpInput;
import java.util.*;
import static io.github.gustavo2358.lower.domain.SpInput.*;
import static io.github.gustavo2358.lower.application.Admission.*;

/** Transport upstream evidence verbatim; distinct identities are never a disjointness proof. */
final class StoragePremise {
    static void admit(SpInput input, EntryGobackAdmission.Context c) {
            need(c,input.storageIndependence().isPresent(),"upstream IndependentStorageSet required");
            input.storageIndependence().ifPresent(proof -> {
                need(c,proof.availability()==Availability.KNOWN && proof.rule()==StorageIndependenceRule.INDEPENDENT_WORKING_STORAGE_ROOTS
                    && proof.authority().equals("IBM_ENTERPRISE_COBOL_6_4_WORKING_STORAGE") && proof.gapCodes().isEmpty()
                    && proof.provenance().filter(Provenance::exact).isPresent() && proof.members().size()>=2,"known source-derived storage authority, complete provenance, no gaps");
                var members=new HashSet<DataId>();
                for(var id:proof.members()) { c.touch();need(c,id.unit().equals(input.unit()) && c.data(id)!=null && members.add(id),"each published proof member maps exactly once"); }
                // Every relevant storage is covered. This validates the published set, never creates one.
                for(var d:input.dataDeclarations()) { c.touch();need(c,members.contains(d.id()),"storage evidence covers all declared/relevant scalar storages"); }
                proof.provenance().ifPresent(c::provenance);
            });
    }
    private static void need(EntryGobackAdmission.Context c, boolean value, String detail) {
        c.require(value, Rule.PROFILE_FACT, "storage-independence", null, detail);
    }
    static List<Proofs.Premise> available(SpInput input, ScalarDataTranslator.Result data,
            UnitId unit, LocalIds ids, SourceOrigins origins) {
        return input.storageIndependence().filter(p -> p.availability() == Availability.KNOWN)
            .map(p -> List.of(translate(p, data, unit, ids, origins))).orElseGet(List::of);
    }
    static Proofs.Premise translate(SpInput.IndependentStorageSet proof, ScalarDataTranslator.Result data,
            UnitId unit, LocalIds ids, SourceOrigins origins) {
        var origin = origins.source("storage-independence", unit.localId(), proof.provenance().orElseThrow());
        var members = new ArrayList<StorageId>();
        for (var member : proof.members()) members.add(data.index().get(member).storage());
        return new Proofs.Premise(new PremiseId(unit.publication(), ids.id("premise", "sp-storage-independence", unit.localId(), proof.rule().name())),
            proof.authority(), "SP rule: " + proof.rule().name(), origin, new Proofs.DisjointStorage(members));
    }
}
