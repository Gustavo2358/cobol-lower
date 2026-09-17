package io.github.gustavo2358.lower.adapters.sp;
import java.util.List;
import io.github.gustavo2358.lower.domain.FileFacts;
import static io.github.gustavo2358.lower.domain.SpInput.*;
/** SP2.26/fileInventory1.5: complete participant/phase inventory and local procedures. */
final class Wire226 {
    record Document(String schema,String contractVersion,Wire.UnitKeyDocument unit,Wire.PolicyDocument policy,
        List<Wire211.DataDocument> dataDeclarations,List<Wire211.StatementDocument> statements,Wire.StructureDocument structure,
        List<Wire.GapDocument> gaps,Wire.CoverageDocument coverage,Wire.EntryInventoryDocument entryInventory,
        Wire211.StorageDocument storageIndependence,Wire215.PhysicalStorageDocument storage,List<Wire217.EffectDocument> statementEffects,Inventory fileInventory) {}
    record Inventory(String version,Availability availability,List<Wire221.Declaration> declarations,List<String> gapCodes,Operations operations,List<Wire225.Declarative> declaratives,List<SortPlan> sortPlans,Availability sortAvailability) {}
    record Operations(Availability availability,List<Use> uses,List<String> gapCodes) {}
    record Use(String statement,int ordinal,FileFacts.Command command,FileFacts.OpenMode mode,FileFacts.SyntaxProfile profile,
        ResolutionStatus bindingStatus,List<Wire223.Candidate> candidates,Wire.ProvenanceDocument provenance,List<String> gapCodes,
        List<Wire223.Operand> operands,List<FileFacts.Option> options,FileFacts.KeyRelation keyRelation,boolean explicitTerminator,List<Wire223.Handler> handlers,Wire224.Effects effects,Wire225.Control control,FileFacts.Role role) {}
    record SortPlan(String statement,Availability availability,int work,List<Integer> inputs,List<Integer> outputs,List<ProcedurePlan> procedures,List<String> gapCodes) {}
    record ProcedurePlan(FileFacts.ProcedurePhase phase,@Wire.Nullable Wire211.PerformTargetDocument start,@Wire.Nullable Wire211.PerformTargetDocument end,
        List<String> roots,@Wire.Nullable String entry,List<String> completions,List<ProcedureLink> links,List<String> gapCodes) {}
    record ProcedureLink(String from,String to) {}
    static Wire225.Document common(Document d) {
        var inv=d.fileInventory();return new Wire225.Document(d.schema(),d.contractVersion(),d.unit(),d.policy(),d.dataDeclarations(),d.statements(),d.structure(),d.gaps(),d.coverage(),d.entryInventory(),d.storageIndependence(),d.storage(),d.statementEffects(),
            new Wire225.Inventory("1.4.0",inv.availability(),inv.declarations(),inv.gapCodes(),new Wire225.Operations(inv.operations().availability(),inv.operations().uses().stream().map(u->new Wire225.Use(u.statement(),u.ordinal(),u.command(),u.mode(),u.profile(),u.bindingStatus(),u.candidates(),u.provenance(),u.gapCodes(),u.operands(),u.options(),u.keyRelation(),u.explicitTerminator(),u.handlers(),u.effects(),u.control())).toList(),inv.operations().gapCodes()),inv.declaratives()));
    }
}
