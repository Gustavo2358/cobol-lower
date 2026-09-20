package io.github.gustavo2358.lower.application;

import io.github.gustavo2358.air.model.*;
import io.github.gustavo2358.air.model.Ids.*;
import io.github.gustavo2358.lower.domain.SpInput;
import java.util.*;
import static io.github.gustavo2358.lower.domain.SpInput.*;
import static io.github.gustavo2358.lower.application.Admission.*;

/** Validate optional legacy assertions; the executable projection uses positive storage identity. */
final class StoragePremise {
    static void admit(SpInput input, EntryGobackAdmission.Context c) {
            // Optional legacy proof is still validated when supplied; identity itself separates bases.
            input.storageIndependence().ifPresent(proof -> {
                need(c,proof.availability()==Availability.KNOWN && proof.rule()==StorageIndependenceRule.INDEPENDENT_WORKING_STORAGE_ROOTS
                    && proof.authority().equals("IBM_ENTERPRISE_COBOL_6_4_WORKING_STORAGE") && proof.gapCodes().isEmpty()
                    && proof.provenance().filter(Provenance::exact).isPresent() && proof.members().size()>=2,"known source-derived storage authority, complete provenance, no gaps");
                var members=new HashSet<DataId>();
                for(var id:proof.members()) { c.touch();need(c,id.unit().equals(input.unit()) && c.data(id)!=null && members.add(id),"each published proof member maps exactly once"); }
                proof.provenance().ifPresent(c::provenance);
            });
    }
    private static void need(EntryGobackAdmission.Context c, boolean value, String detail) {
        c.require(value, Rule.PROFILE_FACT, "storage-independence", null, detail);
    }
    static List<Proofs.Premise> available(SpInput input, ScalarDataTranslator.Result data,
            UnitId unit, LocalIds ids, SourceOrigins origins) {
        return List.of(); // Positive StorageId semantics does not require negative source premises.
    }
}
