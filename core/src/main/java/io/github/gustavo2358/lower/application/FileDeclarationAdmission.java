package io.github.gustavo2358.lower.application;
import java.util.HashSet;
import io.github.gustavo2358.lower.domain.FileFacts;
import io.github.gustavo2358.lower.domain.SpInput;
import static io.github.gustavo2358.lower.domain.SpInput.*;
import static io.github.gustavo2358.lower.application.Admission.*;

/** Bilateral semantic admission shared by all input ports, including in-memory. */
final class FileDeclarationAdmission {
    private FileDeclarationAdmission() { }
    static void validate(SpInput input, EntryGobackAdmission.Context c) {
        var inventory=input.fileInventory(); var files=new HashSet<String>(); var records=new HashSet<DataId>();
        c.require(inventory.availability()==Availability.KNOWN ? inventory.gapCodes().isEmpty() : !inventory.gapCodes().isEmpty(),Rule.PROFILE_FACT,"files",null,"file availability/gaps mismatch");
        c.require(inventory.availability()!=Availability.UNAVAILABLE || inventory.declarations().isEmpty(),Rule.PROFILE_FACT,"files",null,"unavailable file inventory cannot assert empty knowledge");
        for(var f:inventory.declarations()) {
            c.touch(); c.identity(f.owner(),f.id(),"file",null);
            c.require(files.add(f.id()),Rule.DUPLICATE_ID,f.id(),null,"duplicate file identity");
            c.require(!f.logicalFile().isBlank()&&!f.origins().isEmpty(),Rule.PROFILE_FACT,f.id(),null,"file name and origins required");
            for(var origin:f.origins())c.provenance(origin);
            var a=f.assignment();
            c.require(a.profile().equals("ibm-enterprise-cobol-6.4-n-lr@2026-04-28"),Rule.PROFILE_FACT,f.id(),null,"unsupported file declaration profile");
            c.require(a.externalFileName().isPresent()==(a.sourceKind()==FileFacts.NameSource.ASSIGNMENT_NAME),Rule.PROFILE_FACT,f.id(),null,"name/source mismatch");
            c.require(a.availability()==Availability.KNOWN ? a.gapCodes().isEmpty() : !a.gapCodes().isEmpty(),Rule.PROFILE_FACT,f.id(),null,"name availability/gaps mismatch");
            c.require(a.externalFileName().isEmpty()||a.availability()==Availability.KNOWN&&!a.externalFileName().get().isBlank(),Rule.PROFILE_FACT,f.id(),null,"unknown cannot claim exact name");
            c.require(f.kind()!=FileFacts.Kind.SD||a.externalFileName().isEmpty(),Rule.PROFILE_FACT,f.id(),null,"SD cannot assert external file name");
            for(var record:f.records()) {
                c.touch();c.require(record.unit().equals(input.unit())&&c.data.containsKey(record)&&records.add(record),Rule.PROFILE_FACT,f.id(),null,"missing or multiply owned file record");
            }
            for(var ref:f.references()) {
                c.touch();c.provenance(ref.provenance()); var b=ref.binding(); var ids=new HashSet<DataId>();
                for(var id:b.candidates())c.require(c.data.containsKey(id)&&ids.add(id),Rule.PROFILE_FACT,f.id(),null,"missing/duplicate file reference candidate");
                c.require(b.selected().isPresent()==(b.status()==ResolutionStatus.RESOLVED),Rule.PROFILE_FACT,f.id(),null,"file reference resolution/selection mismatch");
                c.require(b.selected().isEmpty()||b.candidates().size()==1&&b.candidates().contains(b.selected().get()),Rule.PROFILE_FACT,f.id(),null,"invalid file reference selection");
            }
        }
        var operations=inventory.operations();
        c.require(operations.availability()==Availability.KNOWN ? operations.gapCodes().isEmpty() : !operations.gapCodes().isEmpty(),Rule.PROFILE_FACT,"file-uses",null,"operation availability/gaps mismatch");
        c.require(operations.availability()!=Availability.UNAVAILABLE||operations.uses().isEmpty(),Rule.PROFILE_FACT,"file-uses",null,"unavailable operations cannot assert uses");
        var statementIds=input.statements().stream().map(s->s.header().id()).collect(java.util.stream.Collectors.toSet());
        var ordinals=new HashSet<java.util.Map.Entry<StatementId,Integer>>();
        for(var use:inventory.operations().uses()) {
            c.touch();c.provenance(use.provenance());
            c.require(statementIds.contains(use.statement())&&use.statement().unit().equals(input.unit()),Rule.PROFILE_FACT,"file-use",null,"file use refers to absent/foreign statement");
            c.require(use.ordinal()>=0&&ordinals.add(java.util.Map.entry(use.statement(),use.ordinal())),Rule.PROFILE_FACT,"file-use",null,"duplicate/negative file use ordinal");
            c.require(use.bindingStatus()!=ResolutionStatus.RESOLVED||use.candidates().size()==1,Rule.PROFILE_FACT,"file-use",null,"resolved file use must have one candidate");
            c.require(new HashSet<>(use.candidates()).size()==use.candidates().size(),Rule.PROFILE_FACT,"file-use",null,"duplicate candidates");
            for(var candidate:use.candidates())c.require(!candidate.id().isBlank()&&(!candidate.owner().equals(input.unit())||files.contains(candidate.id())),Rule.PROFILE_FACT,"file-use",null,"local candidate missing");
            c.require((use.command()==FileFacts.Command.OPEN)==(use.mode()!=FileFacts.OpenMode.UNSPECIFIED),Rule.PROFILE_FACT,"file-use",null,"file command/mode mismatch");
        }
        FileOperationAdmission.validate(input,c);
    }
}
