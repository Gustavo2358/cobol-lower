package io.github.gustavo2358.lower.application;
import io.github.gustavo2358.lower.domain.*;
import java.util.*;
import static io.github.gustavo2358.lower.application.Admission.*;
import static io.github.gustavo2358.lower.domain.SpInput.*;

/** Common in-memory/file admission. No COBOL parsing or name interpretation. */
final class FileAuxiliaryAdmission {
    static void validate(SpInput input,EntryGobackAdmission.Context c){
        if(input.fileInventory().auxiliary().isEmpty())return;
        var inv=input.fileInventory().auxiliary().orElseThrow();var ids=new HashSet<String>();var files=new HashMap<FileFacts.Candidate,FileFacts.Declaration>();
        input.fileInventory().declarations().forEach(f->files.put(new FileFacts.Candidate(f.id(),f.owner()),f));
        c.require(inv.availability()==Availability.KNOWN?inv.gapCodes().isEmpty():!inv.gapCodes().isEmpty(),Rule.PROFILE_FACT,"auxiliary",null,"availability/gaps mismatch");
        c.require(inv.availability()!=Availability.UNAVAILABLE||inv.clauses().isEmpty(),Rule.PROFILE_FACT,"auxiliary",null,"unavailable inventory asserts clauses");
        for(var a:inv.clauses()){
            c.touch();c.provenance(a.provenance());c.require(!a.id().isBlank()&&ids.add(a.id()),Rule.DUPLICATE_ID,a.id(),null,"duplicate/empty auxiliary identity");
            c.require((a.kind()==FileFacts.AuxKind.RERUN)==(a.trigger()!=FileFacts.Trigger.NONE),Rule.PROFILE_FACT,a.id(),null,"trigger/kind mismatch");
            c.require(a.kind()==FileFacts.AuxKind.RERUN?a.checkpoint().isPresent():a.checkpoint().isEmpty(),Rule.PROFILE_FACT,a.id(),null,"checkpoint/kind mismatch");
            c.require(a.effect()!=FileFacts.AuxEffect.OUTSIDE_N_LR||!a.gapCodes().isEmpty(),Rule.PROFILE_FACT,a.id(),null,"unsupported without gap");
            for(var f:a.fileReferences()){
                c.touch();c.provenance(f.provenance());c.require(f.status()!=ResolutionStatus.RESOLVED||f.candidates().size()==1,Rule.PROFILE_FACT,a.id(),null,"resolved cardinality");
                c.require(new HashSet<>(f.candidates()).size()==f.candidates().size(),Rule.PROFILE_FACT,a.id(),null,"duplicate auxiliary candidate");
                for(var id:f.candidates())c.require(!id.id().isBlank()&&(!id.owner().equals(input.unit())||files.containsKey(id)),Rule.PROFILE_FACT,a.id(),null,"absent auxiliary file");
            }
            for(var f:a.fileReferences())if(f.status()==ResolutionStatus.RESOLVED&&files.containsKey(f.candidates().getFirst())){
                var declaration=files.get(f.candidates().getFirst());var organization=declaration.organization();
                c.require(declaration.kind()!=FileFacts.Kind.SD||f.accessMethod()==FileFacts.SourceAccessMethod.UNKNOWN,Rule.PROFILE_FACT,a.id(),null,"SD has no FD access method");
                if(declaration.kind()==FileFacts.Kind.FD&&declaration.assignment().externalFileName().isPresent()){
                    if(organization==FileFacts.Organization.INDEXED||organization==FileFacts.Organization.RELATIVE)c.require(f.accessMethod()==FileFacts.SourceAccessMethod.VSAM,Rule.PROFILE_FACT,a.id(),null,"indexed/relative method contradicts N-LR");
                    if(organization==FileFacts.Organization.LINE_SEQUENTIAL)c.require(f.accessMethod()==FileFacts.SourceAccessMethod.LINE_SEQUENTIAL,Rule.PROFILE_FACT,a.id(),null,"line-sequential method contradicts organization");
                }
            }
            if(a.kind()==FileFacts.AuxKind.SAME_AREA&&a.effect()==FileFacts.AuxEffect.DOCUMENTARY)c.require(!a.fileReferences().isEmpty()&&a.fileReferences().stream().allMatch(f->f.accessMethod()==FileFacts.SourceAccessMethod.QSAM),Rule.PROFILE_FACT,a.id(),null,"documentary SAME AREA lacks QSAM proof");
            for(var d:a.dataReferences()){
                c.touch();c.provenance(d.provenance());var b=d.binding();c.require(!d.role().isBlank(),Rule.PROFILE_FACT,a.id(),null,"empty data role");
                c.require(b.selected().isPresent()==(b.status()==ResolutionStatus.RESOLVED)&&b.selected().map(s->b.candidates().size()==1&&b.candidates().contains(s)).orElse(true),Rule.PROFILE_FACT,a.id(),null,"auxiliary data selection");
                for(var id:b.candidates())c.require(c.data.containsKey(id),Rule.PROFILE_FACT,a.id(),null,"absent auxiliary data");
            }
            for(var p:a.parameters())c.require(!p.role().isBlank(),Rule.PROFILE_FACT,a.id(),null,"empty parameter role");
            a.checkpoint().ifPresent(n->{c.require(n.profile().equals("ibm-enterprise-cobol-6.4-n-lr@2026-04-28")&&n.externalFileName().isPresent()==(n.sourceKind()==FileFacts.NameSource.ASSIGNMENT_NAME),Rule.PROFILE_FACT,a.id(),null,"checkpoint profile/name source");c.require(n.availability()==Availability.KNOWN?n.gapCodes().isEmpty():!n.gapCodes().isEmpty(),Rule.PROFILE_FACT,a.id(),null,"checkpoint gaps");c.require(n.externalFileName().isEmpty()||n.availability()==Availability.KNOWN&&!n.externalFileName().orElseThrow().isBlank(),Rule.PROFILE_FACT,a.id(),null,"unproved checkpoint name");});
            var permitted=switch(a.kind()){
                case RERUN->a.trigger()==FileFacts.Trigger.UNSUPPORTED?Set.of(FileFacts.AuxEffect.OUTSIDE_N_LR):Set.of(FileFacts.AuxEffect.CHECKPOINT);
                case SAME_AREA->Set.of(FileFacts.AuxEffect.RECORD_ALIAS,FileFacts.AuxEffect.DOCUMENTARY,FileFacts.AuxEffect.CONDITIONAL_RECORD_ALIAS);
                case SAME_RECORD_AREA->Set.of(FileFacts.AuxEffect.RECORD_ALIAS);
                case RESERVE,APPLY_WRITE_ONLY,BLOCK->Set.of(FileFacts.AuxEffect.BUFFER_ALLOCATION);
                case RECORD->Set.of(FileFacts.AuxEffect.RECORD_LAYOUT);
                case CODE_SET,RECORDING_MODE->Set.of(FileFacts.AuxEffect.RECORD_LAYOUT,FileFacts.AuxEffect.DOCUMENTARY);
                case LINAGE->Set.of(FileFacts.AuxEffect.PAGE_CONTROL,FileFacts.AuxEffect.DOCUMENTARY);
                case PASSWORD->Set.of(FileFacts.AuxEffect.ACCESS_CHECK);
                case COMMITMENT_CONTROL,REPORT->Set.of(FileFacts.AuxEffect.OUTSIDE_N_LR);
                default->Set.of(FileFacts.AuxEffect.DOCUMENTARY);};
            c.require(permitted.contains(a.effect()),Rule.PROFILE_FACT,a.id(),null,"auxiliary effect contradicts kind");
            if(a.kind()==FileFacts.AuxKind.SAME_AREA&&a.effect()==FileFacts.AuxEffect.RECORD_ALIAS)c.require(!a.fileReferences().isEmpty()&&a.fileReferences().stream().allMatch(f->f.status()==ResolutionStatus.RESOLVED&&files.containsKey(f.candidates().getFirst())&&f.accessMethod()==FileFacts.SourceAccessMethod.VSAM),Rule.PROFILE_FACT,a.id(),null,"SAME AREA alias lacks VSAM proof");
        }
    }
}
