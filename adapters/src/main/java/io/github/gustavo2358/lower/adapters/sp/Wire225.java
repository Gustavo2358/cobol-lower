package io.github.gustavo2358.lower.adapters.sp;
import java.util.List;
import io.github.gustavo2358.lower.domain.FileFacts;
import static io.github.gustavo2358.lower.domain.SpInput.*;
/** SP2.25/fileInventory1.4: typed conditional routes and declarative bodies. */
final class Wire225 {
    record Document(String schema,String contractVersion,Wire.UnitKeyDocument unit,Wire.PolicyDocument policy,
        List<Wire211.DataDocument> dataDeclarations,List<Wire211.StatementDocument> statements,Wire.StructureDocument structure,
        List<Wire.GapDocument> gaps,Wire.CoverageDocument coverage,Wire.EntryInventoryDocument entryInventory,
        Wire211.StorageDocument storageIndependence,Wire215.PhysicalStorageDocument storage,List<Wire217.EffectDocument> statementEffects,Inventory fileInventory) {}
    record Inventory(String version,Availability availability,List<Wire221.Declaration> declarations,List<String> gapCodes,Operations operations,List<Declarative> declaratives) {}
    record Operations(Availability availability,List<Use> uses,List<String> gapCodes) {}
    record Use(String statement,int ordinal,FileFacts.Command command,FileFacts.OpenMode mode,FileFacts.SyntaxProfile profile,
        ResolutionStatus bindingStatus,List<Wire223.Candidate> candidates,Wire.ProvenanceDocument provenance,List<String> gapCodes,
        List<Wire223.Operand> operands,List<FileFacts.Option> options,FileFacts.KeyRelation keyRelation,boolean explicitTerminator,List<Wire223.Handler> handlers,Wire224.Effects effects,Control control) {}
    record Declarative(String id,Wire.UnitKeyDocument owner,FileFacts.UseKind kind,boolean global,FileFacts.OpenMode mode,List<Wire223.Candidate> files,List<String> roots,@Wire.Nullable String entry,List<String> completions,List<String> gapCodes,Wire.ProvenanceDocument provenance) {}
    record Destination(FileFacts.DestinationKind kind,@Wire.Nullable FileFacts.HandlerKind handler,@Wire.Nullable String declarative) {}
    record Route(FileFacts.ControlEvent event,FileFacts.EffectOutcome effects,List<Destination> destinations,boolean criticalExit) {}
    record Control(Availability availability,@Wire.Nullable String continuation,List<Route> routes,List<String> gapCodes) {}
    static Wire224.Document common(Document d) {
        var inv=d.fileInventory();return new Wire224.Document(d.schema(),d.contractVersion(),d.unit(),d.policy(),d.dataDeclarations(),d.statements(),d.structure(),d.gaps(),d.coverage(),d.entryInventory(),d.storageIndependence(),d.storage(),d.statementEffects(),
            new Wire224.Inventory("1.3.0",inv.availability(),inv.declarations(),inv.gapCodes(),new Wire224.Operations(inv.operations().availability(),inv.operations().uses().stream().map(u->new Wire224.Use(u.statement(),u.ordinal(),u.command(),u.mode(),u.profile(),u.bindingStatus(),u.candidates(),u.provenance(),u.gapCodes(),u.operands(),u.options(),u.keyRelation(),u.explicitTerminator(),u.handlers(),u.effects())).toList(),inv.operations().gapCodes())));
    }
}
