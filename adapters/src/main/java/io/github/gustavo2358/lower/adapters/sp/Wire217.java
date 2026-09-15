package io.github.gustavo2358.lower.adapters.sp;

import java.util.List;
import static io.github.gustavo2358.lower.domain.SpInput.*;

/** Additive statement-effect inventory; existing statement and storage shapes unchanged. */
final class Wire217 {
    record Document(String schema,String contractVersion,Wire.UnitKeyDocument unit,Wire.PolicyDocument policy,
        List<Wire211.DataDocument> dataDeclarations,List<Wire211.StatementDocument> statements,Wire.StructureDocument structure,
        List<Wire.GapDocument> gaps,Wire.CoverageDocument coverage,Wire.EntryInventoryDocument entryInventory,
        Wire211.StorageDocument storageIndependence,Wire215.PhysicalStorageDocument storage,List<EffectDocument> statementEffects) { }
    record EffectDocument(String version,String statement,List<String> knownReads,List<String> mayWrites,List<String> mustOverwrite,List<String> exposedRegions,
        EffectBound unknownReadBound,EffectBound unknownWriteBound,EffectBound unknownExposureBound,
        EnvironmentEffect environment,EffectValueTransform values,EffectProof proof) { }
    static Wire215.Document common(Document d) {
        return new Wire215.Document(d.schema(),d.contractVersion(),d.unit(),d.policy(),d.dataDeclarations(),d.statements(),d.structure(),d.gaps(),d.coverage(),d.entryInventory(),d.storageIndependence(),d.storage());
    }
}
