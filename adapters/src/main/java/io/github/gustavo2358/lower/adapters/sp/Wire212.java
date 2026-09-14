package io.github.gustavo2358.lower.adapters.sp;
import java.util.List;
import io.github.gustavo2358.lower.domain.StorageFacts;
import static io.github.gustavo2358.lower.adapters.sp.Wire.Nullable;
/** Exact SP2.12 envelope; unchanged statements use the closed SP2.11 family. */
final class Wire212 {
    private Wire212() { }
    record Document(String schema,String contractVersion,Wire.UnitKeyDocument unit,Wire.PolicyDocument policy,
        List<Wire211.DataDocument> dataDeclarations,List<Wire211.StatementDocument> statements,Wire.StructureDocument structure,
        List<Wire.GapDocument> gaps,Wire.CoverageDocument coverage,Wire.EntryInventoryDocument entryInventory,
        Wire211.StorageDocument storageIndependence,PhysicalStorageDocument storage) { }
    record InitialDocument(String node,StorageFacts.InitialKind kind,List<Integer> bytes,List<String> gapCodes,Wire.ProvenanceDocument provenance) { }
    record EntryDocument(StorageFacts.EntryMode mode,List<InitialDocument> conditions) { }
    record PhysicalStorageDocument(String version,StorageFacts.Profile profile,@Nullable String profileId,@Nullable String runtimeCodec,
        List<Wire211.PhysicalNodeDocument> nodes,List<Wire211.BaseDocument> bases,List<Wire211.ViewDocument> views,List<String> gapCodes,
        List<Wire28.RelationDocument> relations,List<Wire29.RenamesDocument> renames,EntryDocument entryState) { }
    static Wire211.Document common(Document d) {
        var s=d.storage();return new Wire211.Document(d.schema(),d.contractVersion(),d.unit(),d.policy(),d.dataDeclarations(),d.statements(),d.structure(),
            d.gaps(),d.coverage(),d.entryInventory(),d.storageIndependence(),new Wire211.PhysicalStorageDocument(s.version(),s.profile(),s.profileId(),s.runtimeCodec(),s.nodes(),s.bases(),s.views(),s.gapCodes(),s.relations(),s.renames()));
    }
}
