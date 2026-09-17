package io.github.gustavo2358.lower.adapters.sp;
import java.util.List;
import io.github.gustavo2358.lower.domain.FileFacts;
import static io.github.gustavo2358.lower.domain.SpInput.*;
/** SP2.23 native file syntax; declarations remain independently available. */
final class Wire223 {
    record Document(String schema,String contractVersion,Wire.UnitKeyDocument unit,Wire.PolicyDocument policy,
        List<Wire211.DataDocument> dataDeclarations,List<Wire211.StatementDocument> statements,Wire.StructureDocument structure,
        List<Wire.GapDocument> gaps,Wire.CoverageDocument coverage,Wire.EntryInventoryDocument entryInventory,
        Wire211.StorageDocument storageIndependence,Wire215.PhysicalStorageDocument storage,List<Wire217.EffectDocument> statementEffects, Inventory fileInventory) { }
    record Inventory(String version,Availability availability,List<Wire221.Declaration> declarations,List<String> gapCodes,Operations operations) {}
    record Operations(Availability availability,List<Use> uses,List<String> gapCodes) {}
    record Candidate(String id,Wire.UnitKeyDocument owner) {}
    record Use(String statement,int ordinal,FileFacts.Command command,FileFacts.OpenMode mode,FileFacts.SyntaxProfile profile,
        ResolutionStatus bindingStatus,List<Candidate> candidates,Wire.ProvenanceDocument provenance,List<String> gapCodes,
        List<Operand> operands,List<FileFacts.Option> options,FileFacts.KeyRelation keyRelation,boolean explicitTerminator,List<Handler> handlers) {}
    record Operand(FileFacts.OperandRole role,FileFacts.OperandForm form,List<String> references,@Wire.Nullable String writtenValue,Wire.ProvenanceDocument provenance,List<String> gapCodes) {}
    record Handler(FileFacts.HandlerKind kind,List<String> statements,Wire.ProvenanceDocument provenance) {}
    static Wire221.Document common(Document d){return new Wire221.Document(d.schema(),d.contractVersion(),d.unit(),d.policy(),d.dataDeclarations(),d.statements(),d.structure(),d.gaps(),d.coverage(),d.entryInventory(),d.storageIndependence(),d.storage(),d.statementEffects(),new Wire221.Inventory("1.0.0",d.fileInventory().availability(),d.fileInventory().declarations(),d.fileInventory().gapCodes()));}
}
