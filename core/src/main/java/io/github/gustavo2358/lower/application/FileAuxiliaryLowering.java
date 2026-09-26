package io.github.gustavo2358.lower.application;
import io.github.gustavo2358.air.model.*;
import io.github.gustavo2358.air.model.Ids.*;
import io.github.gustavo2358.lower.domain.*;
import java.util.*;

/** Auxiliary declarations in the existing compositional lowering. Optional repeated checkpoints
 * preserve a source-proven name/trigger without asserting a count or external allocation. */
final class FileAuxiliaryLowering {
    private final UnitId unit;private final LocalIds ids;private final SourceOrigins origins;private final List<Evidence.Uncertainty> gaps;
    private final List<FileFacts.AuxClause> checkpoints=new ArrayList<>();private final Map<String,List<Interactions.ResourceUse>> uses=new HashMap<>();
    FileAuxiliaryLowering(SpInput input,UnitId unit,LocalIds ids,SourceOrigins origins,List<Evidence.Uncertainty> gaps){
        this.unit=unit;this.ids=ids;this.origins=origins;this.gaps=gaps;
        input.fileInventory().auxiliary().ifPresent(inv->{for(var c:inv.clauses()){
            var origin=origins.source("file-auxiliary",c.id(),c.provenance());
            for(var code:c.gapCodes())uncertainty("declaration/"+c.id(),unit,origin,code,List.of(Evidence.Dimension.DEPENDENCIES,Evidence.Dimension.STORAGE,Evidence.Dimension.EFFECTS));
            if(c.effect()==FileFacts.AuxEffect.CHECKPOINT)checkpoints.add(c);
        }});
    }
    LabelId prefix(FileFacts.Use use,LabelId entry,OriginId origin,LocalIds local,List<Sequence> out){
        var selected=checkpoints.stream().filter(c->c.gapCodes().isEmpty()&&c.checkpoint().flatMap(FileFacts.Assignment::externalFileName).isPresent()&&matches(c,use)).toList();
        var current=entry;for(var c:selected){var key=use.statement().handle()+"/"+use.ordinal()+"/"+c.id();
            var next=new LabelId(unit,local.id("label","checkpoint-after",unit.localId(),key));var invokeLabel=new LabelId(unit,local.id("label","checkpoint-invoke",unit.localId(),key));
            var choice=new OperationId(unit,local.id("operation","checkpoint-choice",unit.localId(),key));
            var gap=uncertainty(key,choice,origin,"CHECKPOINT_OCCURRENCE_COUNT_NOT_PROVEN",List.of(Evidence.Dimension.CONTROL,Evidence.Dimension.VALUES));
            var predicate=new Expressions.Unknown(new Operand.Header(new OperandId(new OperationOwner(choice),"condition"),Operand.Role.PREDICATE,origin),Types.known(Types.Builtin.BOOL),List.of(),Scopes.NoMemory.INSTANCE,gap);
            out.add(new Sequence(current,List.of(),new Operations.Branch(header(choice,origin,gap),predicate,invokeLabel,next),origin));
            var op=new OperationId(unit,local.id("operation","checkpoint-invoke",unit.localId(),key));var clauseOrigin=origins.source("file-auxiliary",c.id(),c.provenance());
            var derived=origins.derived(local.id("origin","checkpoint-use",unit.localId(),key),List.of(origin,clauseOrigin),"file-auxiliary@1/typed-trigger");
            var contract=uncertainty(key+"/contract",op,derived,"CONTRACT_UNKNOWN",List.of(Evidence.Dimension.CONTROL,Evidence.Dimension.EFFECTS));
            var target=new Interactions.LiteralTarget("file","cobol.external-file-name",c.checkpoint().orElseThrow().externalFileName().orElseThrow(),Interactions.ExactName.INSTANCE,clauseOrigin);
            var signature=new Interactions.Signature(new Interactions.ParameterInventory(List.of(),Interactions.NoRemainder.INSTANCE),new Interactions.ResultInventory(List.of(),Interactions.NoRemainder.INSTANCE),derived);
            var invoke=new Operations.Invoke(header(op,derived,contract),"checkpoint",target,List.of(),List.of(),new Interactions.ExternalSignature(signature),List.of(),new Interactions.EffectBound(new Interactions.ForeignEffects(Scopes.NoMemory.INSTANCE,Scopes.NoMemory.INSTANCE,List.of()),List.of()),new Control.InvocationOutcomes(List.of(new Control.Normal(current)),Scopes.NoControl.INSTANCE),new Interactions.UnknownContract(contract));
            out.add(new Sequence(invokeLabel,List.of(),invoke,derived));uses.computeIfAbsent(c.id(),k->new ArrayList<>()).add(new Interactions.ResourceUse(op,"checkpoint",derived));current=next;
        }
        return current;
    }
    private static boolean matches(FileFacts.AuxClause c,FileFacts.Use u){
        if(c.trigger()==FileFacts.Trigger.SORT_MERGE)return FileFacts.aggregate(u)&&u.role()==FileFacts.Role.WORK;
        if(c.trigger()!=FileFacts.Trigger.RECORD_COUNT&&c.trigger()!=FileFacts.Trigger.END_VOLUME)return false;
        if(u.command()!=FileFacts.Command.READ&&u.command()!=FileFacts.Command.WRITE&&u.command()!=FileFacts.Command.REWRITE&&!(FileFacts.aggregate(u)&&(u.role()==FileFacts.Role.INPUT||u.role()==FileFacts.Role.OUTPUT)))return false;
        return u.bindingStatus()==SpInput.ResolutionStatus.RESOLVED&&c.fileReferences().stream().anyMatch(f->f.status()==SpInput.ResolutionStatus.RESOLVED&&f.candidates().contains(u.candidates().getFirst()));
    }
    List<Interactions.Resource> resources(){var result=new ArrayList<Interactions.Resource>();for(var c:checkpoints){
        var origin=origins.source("file-auxiliary",c.id(),c.provenance());var id=new ResourceId(unit.publication(),ids.id("resource","checkpoint",unit.localId(),c.id()));
        var name=c.checkpoint().flatMap(FileFacts.Assignment::externalFileName);Interactions.ResourceDescription target;
        if(name.isPresent())target=new Interactions.LiteralTarget("file","cobol.external-file-name",name.orElseThrow(),Interactions.ExactName.INSTANCE,origin);
        else target=new Interactions.UnknownResource("file","cobol.external-file-name",uncertainty(c.id(),id,origin,"CHECKPOINT_NAME_NOT_PROVEN",List.of(Evidence.Dimension.DEPENDENCIES)));
        result.add(new Interactions.Resource(id,target,origin,Optional.of(new Interactions.ResourceDeclaration(unit,c.checkpoint().map(FileFacts.Assignment::original).filter(n->!n.isBlank()).orElse(c.id()),"cobol.checkpoint",name.isPresent()?"cobol.assignment-name":"cobol.unsupported-name",List.of(),uses.getOrDefault(c.id(),List.of())))));
    }return List.copyOf(result);}
    private UncertaintyId uncertainty(String key,Id subject,OriginId origin,String code,List<Evidence.Dimension> dimensions){var id=new UncertaintyId(unit.publication(),ids.id("uncertainty","file-auxiliary",subject.localId(),key+"/"+code));gaps.add(new Evidence.Uncertainty(id,code,dimensions,new Scopes.EntityScope(List.of(subject)),"Source-only auxiliary clause; "+code,origin));return id;}
    private Operations.Header header(OperationId op,OriginId origin,UncertaintyId gap){var scope=new Scopes.EntityScope(List.of(op));var known=new Evidence.Claim(scope,Evidence.PrecisionStatus.EXACT,List.of());var open=new Evidence.Claim(scope,Evidence.PrecisionStatus.OPEN,List.of(gap));return new Operations.Header(op,origin,Evidence.CoverageStatus.ABSTRACTED,new Evidence.Precision(open,known,open,open,known),List.of(gap));}
}
