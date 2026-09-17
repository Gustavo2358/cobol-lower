package io.github.gustavo2358.lower.adapters.sp;
import java.util.List;
import io.github.gustavo2358.lower.domain.FileFacts;
import static io.github.gustavo2358.lower.domain.SpInput.*;
import static io.github.gustavo2358.lower.adapters.sp.Wire.Nullable;
/** SP2.21 mandatory declaration inventory; no FILE operation yet. */
final class Wire221 {
    record Document(String schema,String contractVersion,Wire.UnitKeyDocument unit,Wire.PolicyDocument policy,
        List<Wire211.DataDocument> dataDeclarations,List<Wire211.StatementDocument> statements,Wire.StructureDocument structure,
        List<Wire.GapDocument> gaps,Wire.CoverageDocument coverage,Wire.EntryInventoryDocument entryInventory,
        Wire211.StorageDocument storageIndependence,Wire215.PhysicalStorageDocument storage,List<Wire217.EffectDocument> statementEffects, Inventory fileInventory) { }
    record Inventory(String version, Availability availability, List<Declaration> declarations, List<String> gapCodes) { }
    record Assignment(Availability availability, String profile, String original, FileFacts.NameSource sourceKind,
            @Nullable String externalFileName, List<String> gapCodes) { }
    record Reference(FileFacts.ReferenceRole role, Wire.BindingDocument binding, boolean duplicates, Wire.ProvenanceDocument provenance) { }
    record Declaration(String id, Wire.UnitKeyDocument owner, String logicalFile, FileFacts.Kind kind, @Nullable Boolean optional,
            Assignment assignment, FileFacts.Organization organization, FileFacts.AccessMode accessMode, FileFacts.Visibility visibility,
            List<String> records, List<Reference> references, List<Wire.ProvenanceDocument> origins, List<String> gapCodes) { }
    static Wire217.Document common(Document d) { return new Wire217.Document(d.schema(),d.contractVersion(),d.unit(),d.policy(),
            d.dataDeclarations(),d.statements(),d.structure(),d.gaps(),d.coverage(),d.entryInventory(),d.storageIndependence(),d.storage(),d.statementEffects()); }
}
