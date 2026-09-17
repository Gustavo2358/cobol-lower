package io.github.gustavo2358.lower.adapters.sp;
import java.util.List;
import io.github.gustavo2358.lower.domain.FileFacts;
import static io.github.gustavo2358.lower.domain.SpInput.*;
/** SP2.27/fileInventory1.6: closed auxiliary declarations, source-level checkpoint and trigger. */
final class Wire227 {
    record Document(String schema,String contractVersion,Wire.UnitKeyDocument unit,Wire.PolicyDocument policy,
        List<Wire211.DataDocument> dataDeclarations,List<Wire211.StatementDocument> statements,Wire.StructureDocument structure,
        List<Wire.GapDocument> gaps,Wire.CoverageDocument coverage,Wire.EntryInventoryDocument entryInventory,
        Wire211.StorageDocument storageIndependence,Wire215.PhysicalStorageDocument storage,List<Wire217.EffectDocument> statementEffects,Inventory fileInventory) {}
    record Inventory(String version,Availability availability,List<Wire221.Declaration> declarations,List<String> gapCodes,Wire226.Operations operations,List<Wire225.Declarative> declaratives,List<Wire226.SortPlan> sortPlans,Availability sortAvailability,Auxiliary auxiliary) {}
    record Auxiliary(Availability availability,List<Clause> clauses,List<String> gapCodes){}
    record Clause(String id,FileFacts.AuxKind kind,FileFacts.AuxEffect effect,List<Reference> fileReferences,List<Data> dataReferences,
        List<FileFacts.AuxParameter> parameters,@Wire.Nullable Wire221.Assignment checkpoint,FileFacts.Trigger trigger,List<String> gapCodes,Wire.ProvenanceDocument provenance){}
    record Reference(ResolutionStatus status,List<Wire223.Candidate> candidates,FileFacts.SourceAccessMethod accessMethod,Wire.ProvenanceDocument provenance){}
    record Data(String role,Wire.BindingDocument binding,Wire.ProvenanceDocument provenance){}
    static Wire226.Document common(Document d){var inv=d.fileInventory();return new Wire226.Document(d.schema(),d.contractVersion(),d.unit(),d.policy(),d.dataDeclarations(),d.statements(),d.structure(),d.gaps(),d.coverage(),d.entryInventory(),d.storageIndependence(),d.storage(),d.statementEffects(),new Wire226.Inventory("1.5.0",inv.availability(),inv.declarations(),inv.gapCodes(),inv.operations(),inv.declaratives(),inv.sortPlans(),inv.sortAvailability()));}
}
