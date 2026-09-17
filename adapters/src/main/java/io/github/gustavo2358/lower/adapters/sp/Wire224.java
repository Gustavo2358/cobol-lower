package io.github.gustavo2358.lower.adapters.sp;
import java.util.List;
import io.github.gustavo2358.lower.domain.FileFacts;
import static io.github.gustavo2358.lower.domain.SpInput.*;
/** SP2.24/fileInventory1.3/storage1.8: conditional memory effects, not handler selection. */
final class Wire224 {
    record Document(String schema,String contractVersion,Wire.UnitKeyDocument unit,Wire.PolicyDocument policy,
        List<Wire211.DataDocument> dataDeclarations,List<Wire211.StatementDocument> statements,Wire.StructureDocument structure,
        List<Wire.GapDocument> gaps,Wire.CoverageDocument coverage,Wire.EntryInventoryDocument entryInventory,
        Wire211.StorageDocument storageIndependence,Wire215.PhysicalStorageDocument storage,List<Wire217.EffectDocument> statementEffects,Inventory fileInventory) {}
    record Inventory(String version,Availability availability,List<Wire221.Declaration> declarations,List<String> gapCodes,Operations operations) {}
    record Operations(Availability availability,List<Use> uses,List<String> gapCodes) {}
    record Use(String statement,int ordinal,FileFacts.Command command,FileFacts.OpenMode mode,FileFacts.SyntaxProfile profile,
        ResolutionStatus bindingStatus,List<Wire223.Candidate> candidates,Wire.ProvenanceDocument provenance,List<String> gapCodes,
        List<Wire223.Operand> operands,List<FileFacts.Option> options,FileFacts.KeyRelation keyRelation,boolean explicitTerminator,List<Wire223.Handler> handlers,Effects effects) {}
    record Target(@Wire.Nullable String data,@Wire.Nullable Wire211.RegionalAccessDocument regional,boolean wholeBase,@Wire.Nullable String reference,Wire.ProvenanceDocument provenance) {}
    record Step(FileFacts.MemoryRole role,FileFacts.MemoryKind kind,Target destination,@Wire.Nullable Target source,List<String> gapCodes,Wire.ProvenanceDocument provenance) {}
    record Outcome(FileFacts.EffectOutcome outcome,List<Step> steps) {}
    record Effects(Availability availability,List<Target> ioReads,List<Step> before,List<Outcome> outcomes,boolean unknownReadBound,boolean unknownWriteBound,List<String> gapCodes) {}
    static Wire223.Document common(Document d) {
        var inv=d.fileInventory();return new Wire223.Document(d.schema(),d.contractVersion(),d.unit(),d.policy(),d.dataDeclarations(),d.statements(),d.structure(),d.gaps(),d.coverage(),d.entryInventory(),d.storageIndependence(),d.storage(),d.statementEffects(),
            new Wire223.Inventory("1.2.0",inv.availability(),inv.declarations(),inv.gapCodes(),new Wire223.Operations(inv.operations().availability(),inv.operations().uses().stream().map(u->new Wire223.Use(u.statement(),u.ordinal(),u.command(),u.mode(),u.profile(),u.bindingStatus(),u.candidates(),u.provenance(),u.gapCodes(),u.operands(),u.options(),u.keyRelation(),u.explicitTerminator(),u.handlers())).toList(),inv.operations().gapCodes())));
    }
}
