package io.github.gustavo2358.lower.adapters.testing;

import io.github.gustavo2358.lower.adapters.cli.CobolLower;
import io.github.gustavo2358.lower.adapters.sp.SpJsonDecoder;
import java.util.Objects;
import static io.github.gustavo2358.lower.testing.CallOracle.check;

/** B-SP: independent assertions over real producer bytes, plus wire/admission mutants. */
public final class FileDeclarationSuite {
    public static void main(String[] args) throws Exception {
        var bytes = Objects.requireNonNull(FileDeclarationSuite.class.getResourceAsStream("/sp/file-dependencies/declaration.json")).readAllBytes();
        var decoded = new SpJsonDecoder(CobolLower.INPUT_LIMITS).decode(bytes);
        check(decoded instanceof SpJsonDecoder.Decoded, "SP2.21 declarations must decode: " + decoded);
        var input=((SpJsonDecoder.Decoded)decoded).input();
        var inputPath=java.nio.file.Files.createTempFile("fd-w0-input-",".json");
        java.nio.file.Files.write(inputPath,bytes);
        var fromFile=new io.github.gustavo2358.lower.adapters.sp.SpFileInput(CobolLower.INPUT_LIMITS).read(inputPath);
        check(fromFile instanceof SpJsonDecoder.Decoded&&((SpJsonDecoder.Decoded)fromFile).input().equals(input),"file and memory input preserve identical declaration facts");
        java.nio.file.Files.delete(inputPath);
        var inventory=input.fileInventory();
        check(inventory.availability()==io.github.gustavo2358.lower.domain.SpInput.Availability.KNOWN,"known declaration inventory");
        check(inventory.declarations().size()==1,"one SELECT/FD connector");
        var f=inventory.declarations().get(0);
        check(f.owner().equals(input.unit())&&f.logicalFile().equals("CLIENTES"),"canonical owner and logical name");
        check(f.assignment().externalFileName().orElseThrow().equals("CLIENTDD"),"exact source external name");
        check(f.assignment().sourceKind()==io.github.gustavo2358.lower.domain.FileFacts.NameSource.ASSIGNMENT_NAME,"source kind survives");
        check(f.origins().size()==2&&f.records().size()==1&&input.dataDeclarations().stream().anyMatch(d->d.id().equals(f.records().get(0))),"two origins and resolved record owner");
        var result=new io.github.gustavo2358.lower.application.CobolLowerer().lower(input,CobolLower.OPTIONS);
        check(result.status()!=io.github.gustavo2358.lower.application.LoweringResult.Status.INVALID_INPUT,"declaration admitted in memory");
        result.publication().ifPresent(p -> check(p.resources().isEmpty(),"W0 emits no AIR FILE resources"));
        var mapper=new com.fasterxml.jackson.databind.ObjectMapper();var base=(com.fasterxml.jackson.databind.node.ObjectNode)mapper.readTree(bytes);
        for(var mutation:java.util.List.of("missing","version","superseded","variant","owner","record","duplicate","sd","mechanism","unknown-exact")) {
            var document=base.deepCopy();var file=(com.fasterxml.jackson.databind.node.ObjectNode)document.path("fileInventory").path("declarations").get(0);
            switch(mutation) {
                case "missing" -> document.remove("fileInventory");
                case "version" -> ((com.fasterxml.jackson.databind.node.ObjectNode)document.path("fileInventory")).put("version","9.0.0");
                case "superseded" -> document.put("contractVersion","2.20.0");
                case "variant" -> file.put("kind","NOT_A_FILE");
                case "owner" -> ((com.fasterxml.jackson.databind.node.ObjectNode)file.path("owner")).put("canonicalProgramName","OTHER");
                case "record" -> ((com.fasterxml.jackson.databind.node.ArrayNode)file.path("records")).add("data:999");
                case "duplicate" -> ((com.fasterxml.jackson.databind.node.ArrayNode)document.path("fileInventory").path("declarations")).add(file.deepCopy());
                case "sd" -> file.put("kind","SD");
                case "mechanism" -> ((com.fasterxml.jackson.databind.node.ObjectNode)file.path("assignment")).put("bindingMechanism","UNKNOWN");
                case "unknown-exact" -> ((com.fasterxml.jackson.databind.node.ObjectNode)file.path("assignment")).put("availability","UNAVAILABLE");
            }
            var candidate=new SpJsonDecoder(CobolLower.INPUT_LIMITS).decode(mapper.writeValueAsBytes(document));
            if(candidate instanceof SpJsonDecoder.Decoded d)
                check(new io.github.gustavo2358.lower.application.CobolLowerer().lower(d.input(),CobolLower.OPTIONS).status()==io.github.gustavo2358.lower.application.LoweringResult.Status.INVALID_INPUT,"admission rejects "+mutation);
            else check(candidate instanceof SpJsonDecoder.Rejected,"wire rejects "+mutation);
        }
        for(var name:java.util.List.of("sort","keys-status","copy")) {
            var data=Objects.requireNonNull(FileDeclarationSuite.class.getResourceAsStream("/sp/file-dependencies/"+name+".json")).readAllBytes();
            var d=new SpJsonDecoder(CobolLower.INPUT_LIMITS).decode(data);
            check(d instanceof SpJsonDecoder.Decoded,"bilateral "+name+": "+d);
            var facts=((SpJsonDecoder.Decoded)d).input();var declarations=facts.fileInventory().declarations();
            check(new io.github.gustavo2358.lower.application.CobolLowerer().lower(facts,CobolLower.OPTIONS).status()!=io.github.gustavo2358.lower.application.LoweringResult.Status.INVALID_INPUT,"admit "+name);
            if(name.equals("sort"))check(declarations.size()==2&&declarations.get(1).kind()==io.github.gustavo2358.lower.domain.FileFacts.Kind.SD&&declarations.get(1).assignment().externalFileName().isEmpty(),"SD has no target");
            if(name.equals("keys-status"))check(declarations.get(0).references().size()==3&&declarations.get(0).records().size()==2&&declarations.get(0).references().stream().allMatch(r->r.binding().status()==io.github.gustavo2358.lower.domain.SpInput.ResolutionStatus.RESOLVED),"keys/status and records survive");
            if(name.equals("copy"))check(declarations.get(0).origins().get(1).includeChain().size()==1,"COPY chain survives");
        }
        for(var availability:java.util.List.of("KNOWN","UNAVAILABLE")) {
            var document=base.deepCopy();var inv=(com.fasterxml.jackson.databind.node.ObjectNode)document.path("fileInventory");
            inv.putArray("declarations");inv.put("availability",availability);var gaps=inv.putArray("gapCodes");
            if(availability.equals("UNAVAILABLE"))gaps.add("FILE_INVENTORY_UNAVAILABLE");
            var d=new SpJsonDecoder(CobolLower.INPUT_LIMITS).decode(mapper.writeValueAsBytes(document));
            check(d instanceof SpJsonDecoder.Decoded&&((SpJsonDecoder.Decoded)d).input().fileInventory().availability().name().equals(availability),"known empty and unavailable remain distinct");
        }
        // The memory port cannot bypass owner checks performed on file input.
        var foreign=new io.github.gustavo2358.lower.domain.FileFacts.Declaration(f.id(),new io.github.gustavo2358.lower.domain.SpInput.UnitKey("OTHER",java.util.List.of(0),"OTHER"),f.logicalFile(),f.kind(),f.optional(),f.assignment(),f.organization(),f.accessMode(),f.visibility(),f.records(),f.references(),f.origins(),f.gapCodes());
        var bad=new io.github.gustavo2358.lower.domain.SpInput(input.unit(),input.policy(),input.dataDeclarations(),input.statements(),input.structure(),input.gaps(),input.coverage(),input.entryInventory(),input.storageIndependence(),input.compositional(),input.storage(),new io.github.gustavo2358.lower.domain.FileFacts.Inventory(inventory.availability(),java.util.List.of(foreign),inventory.gapCodes()));
        check(new io.github.gustavo2358.lower.application.CobolLowerer().lower(bad,CobolLower.OPTIONS).status()==io.github.gustavo2358.lower.application.LoweringResult.Status.INVALID_INPUT,"in-memory admission rejects foreign owner");
        System.out.println("FILE_DECLARATIONS=PASS 4 source fixtures, known-empty/unavailable, 10 wire/admission mutants, memory owner");
    }
}
