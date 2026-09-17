package io.github.gustavo2358.lower.application;

import io.github.gustavo2358.air.model.*;
import io.github.gustavo2358.air.model.Ids.*;
import io.github.gustavo2358.lower.domain.FileFacts;
import io.github.gustavo2358.lower.domain.SpInput;
import java.util.*;

/** Typed SP file facts in the compositional path; no source lookup, parsing or filename solver. */
final class FileResourceLowering {
    private final SpInput input;
    private final ScalarDataTranslator.Result data;
    private final UnitId unit;
    private final LocalIds ids;
    private final SourceOrigins origins;
    private final List<Evidence.Uncertainty> gaps;
    private final FileMemoryLowering memory;
    private final FileControlLowering control;
    private final FileSortLowering sort;
    private final Set<LabelId> sourceEntries=new LinkedHashSet<>();
    private final Map<FileFacts.Candidate,FileFacts.Declaration> declarations=new LinkedHashMap<>();
    private final Map<SpInput.StatementId,List<FileFacts.Use>> byStatement=new HashMap<>();
    private final Map<FileFacts.Candidate,List<Interactions.ResourceUse>> associations=new HashMap<>();
    FileResourceLowering(SpInput input,ScalarDataTranslator.Result data,UnitId unit,LocalIds ids,SourceOrigins origins,List<Evidence.Uncertainty> gaps){
        this.input=input;this.data=data;this.unit=unit;this.ids=ids;this.origins=origins;this.gaps=gaps;
        this.memory=new FileMemoryLowering(input,data,unit,ids,origins,gaps);
        this.control=new FileControlLowering(input,unit,ids,origins,gaps);
        this.sort=new FileSortLowering(input,unit,ids,origins,gaps);
        for(var f:input.fileInventory().declarations())declarations.put(new FileFacts.Candidate(f.id(),f.owner()),f);
        for(var use:input.fileInventory().operations().uses())byStatement.computeIfAbsent(use.statement(),k->new ArrayList<>()).add(use);
        byStatement.values().forEach(uses->uses.sort(Comparator.comparingInt(FileFacts.Use::ordinal)));
    }
    boolean handles(SpInput.StatementFact fact){var uses=byStatement.get(fact.header().id());return uses!=null&&!uses.isEmpty()&&uses.stream().allMatch(u->u.profile()==FileFacts.SyntaxProfile.N_LR);}
    void sourceEntry(LabelId label){sourceEntries.add(label);}
    List<Sequence> complete(List<Sequence> sequences){return memory.restrictContinuations(sort.complete(control.complete(sequences)),List.copyOf(sourceEntries));}
    LabelId completion(SpInput.StatementId statement,LabelId ordinary){return sort.completion(statement,control.completion(statement,ordinary));}
    List<Sequence> sequences(SpInput.StatementFact fact,LabelId destination,LocalIds local){
        var result=new ArrayList<Sequence>();var uses=byStatement.get(fact.header().id());
        var layout=sort.layout(fact,uses,destination,local);layout.ifPresent(l->result.addAll(l.prefix()));
        for(int n=0;n<uses.size();n++){
            var use=uses.get(n);var key=fact.header().id().handle()+"/"+use.ordinal();
            var op=new OperationId(unit,local.id("operation","file-use",unit.localId(),key));
            var label=n==0?PartialProgramAssembler.label(fact.header().id(),unit,local):new LabelId(unit,local.id("label","file-use",unit.localId(),key));
            var next=n+1<uses.size()?new LabelId(unit,local.id("label","file-use",unit.localId(),fact.header().id().handle()+"/"+uses.get(n+1).ordinal())):destination;
            if(layout.isPresent()){label=layout.orElseThrow().uses().get(use.ordinal()).entry();next=layout.orElseThrow().uses().get(use.ordinal()).next();}
            var source=origins.source("statement",fact.header().id().handle(),fact.header().provenance());
            var ref=origins.source("file-reference",key,use.provenance());
            var origin=origins.derived(local.id("origin","file-use",unit.localId(),key),List.of(source,ref),"native-file-sites@1/typed-sp-use");
            var gap=gap("file-effects-control",op,origin,List.of(Evidence.Dimension.EFFECTS,Evidence.Dimension.CONTROL,Evidence.Dimension.VALUES,Evidence.Dimension.STORAGE),"FILE_EFFECTS_CONTROL_PARTIAL");
            var scope=new Scopes.EntityScope(List.of(op));var open=new Evidence.Claim(scope,Evidence.PrecisionStatus.OPEN,List.of(gap));
            FileFacts.Candidate candidate=use.bindingStatus()==SpInput.ResolutionStatus.RESOLVED?use.candidates().getFirst():null;
            var declaration=declarations.get(candidate);Interactions.Target target=null;
            boolean localResource=declaration!=null&&declaration.kind()==FileFacts.Kind.SD&&FileFacts.local(use);
            var opGaps=new ArrayList<UncertaintyId>(List.of(gap));
            use.control().ifPresent(c->{for(var code:c.gapCodes())opGaps.add(gap("file-control-input",op,origin,List.of(Evidence.Dimension.CONTROL),code));});
            Evidence.Claim dependency;
            if(declaration!=null&&declaration.kind()!=FileFacts.Kind.SD&&declaration.assignment().externalFileName().isPresent()){
                target=new Interactions.LiteralTarget("file","cobol.external-file-name",declaration.assignment().externalFileName().orElseThrow(),Interactions.ExactName.INSTANCE,ref);
                dependency=new Evidence.Claim(scope,Evidence.PrecisionStatus.EXACT,List.of());
            }else if(localResource){
                dependency=new Evidence.Claim(scope,Evidence.PrecisionStatus.EXACT,List.of());
            }else{
                var reason=gap("file-target",op,ref,List.of(Evidence.Dimension.DEPENDENCIES,Evidence.Dimension.VALUES),"FILE_TARGET_NOT_PROVEN");opGaps.add(reason);
                var name=new Expressions.Unknown(new Operand.Header(new OperandId(new OperationOwner(op),"file-name"),Operand.Role.CALL_TARGET,ref),Types.known(Types.Builtin.TEXT),List.of(),Scopes.NoMemory.INSTANCE,reason);
                target=new Interactions.ComputedTarget("file","cobol.external-file-name",name,Interactions.ExactName.INSTANCE,ref);
                dependency=new Evidence.Claim(scope,Evidence.PrecisionStatus.OPEN,List.of(reason));
            }
            var header=new Operations.Header(op,origin,Evidence.CoverageStatus.ABSTRACTED,new Evidence.Precision(open,open,open,open,dependency),opGaps);
            var contract=gap("file-contract",op,origin,List.of(Evidence.Dimension.EFFECTS),"CONTRACT_UNKNOWN");
            var signature=new Interactions.Signature(new Interactions.ParameterInventory(List.of(),Interactions.NoRemainder.INSTANCE),new Interactions.ResultInventory(List.of(),Interactions.NoRemainder.INSTANCE),origin);
            var plan=use.effects().filter(e->e.availability()==SpInput.Availability.KNOWN||e.availability()==SpInput.Availability.PARTIAL).orElse(null);
            var memory=this.memory.withIds(local);
            var after=plan==null?next:memory.label(key+"/select/0");
            Terminator invoke;
            if(localResource)invoke=new Operations.Opaque(header,"source-local-file-use",List.of(),List.of(),new Envelopes.Envelope(
                new Envelopes.MemoryEnvelope(List.of(),plan==null?new Scopes.WithinMemory(new Scopes.VisibleMemory(unit,true)):memory.bound(plan.ioReads(),plan.unknownReadBound()),List.of(),plan==null?new Scopes.WithinMemory(new Scopes.VisibleMemory(unit,true)):memory.bound(List.of(),plan.unknownWriteBound()),List.of()),
                new Control.ControlEnvelope(after==null?List.of():List.of(new Control.JumpAlternative(after)),new Scopes.WithinControl(new Scopes.UnitControl(unit,false,false,true,true,true,false))),new Envelopes.DependencyEnvelope(List.of(),Scopes.NoResources.INSTANCE)));
            else invoke=new Operations.Invoke(header,action(use.command()),target,List.of(),List.of(),new Interactions.ExternalSignature(signature),List.of(),
                new Interactions.EffectBound(new Interactions.ForeignEffects(plan==null?new Scopes.WithinMemory(new Scopes.VisibleMemory(unit,true)):memory.bound(plan.ioReads(),plan.unknownReadBound()),
                    plan==null?new Scopes.WithinMemory(new Scopes.VisibleMemory(unit,true)):memory.bound(List.of(),plan.unknownWriteBound()),List.of()),List.of()),
                new Control.InvocationOutcomes(after==null?List.of():List.of(new Control.Normal(after)),new Scopes.WithinControl(new Scopes.UnitControl(unit,plan==null,true,true,true,true,true))),new Interactions.UnknownContract(contract));
            var invokeLabel=plan!=null&&!plan.before().isEmpty()?memory.label(key+"/invoke"):label;
            if(plan!=null)result.addAll(memory.steps(key+"/before",plan.before(),label,invokeLabel,origin));
            result.add(new Sequence(invokeLabel,List.of(),invoke,origin));
            if(plan!=null)result.addAll(use.control().filter(c->!c.routes().isEmpty()).isPresent()?control.after(key,use,plan,next,origin,local,memory):memory.after(key,plan,next,origin));
            if(declaration!=null)associations.computeIfAbsent(candidate,k->new ArrayList<>()).add(new Interactions.ResourceUse(op,role(use),origin));
        }
        return List.copyOf(result);
    }
    private static String role(FileFacts.Use use){return switch(use.command()){
        case RELEASE->"release";case RETURN->"return";case SORT,MERGE->switch(use.role()){case INPUT->"input";case OUTPUT->"output";case WORK->"work";case DIRECT->throw new IllegalArgumentException("SORT role absent");};case READ->"input";case WRITE->"output";case REWRITE->"update";case DELETE_RECORD->"delete-record";case START->"position";case CLOSE->"lifecycle";case OPEN->switch(use.mode()){
            case INPUT->"open-input";case OUTPUT->"open-output";case IO->"open-io";case EXTEND->"open-extend";case UNSPECIFIED->throw new IllegalArgumentException("OPEN mode absent");};};}
    private static String action(FileFacts.Command c){return switch(c){case OPEN->"open";case READ->"read";case WRITE->"write";case REWRITE->"rewrite";case DELETE_RECORD->"delete-record";case START->"start";case CLOSE->"close";case RELEASE->"release";case RETURN->"return";case SORT->"sort";case MERGE->"merge";};}
    private UncertaintyId gap(String role,Id subject,OriginId origin,List<Evidence.Dimension> dimensions,String code){
        var id=new UncertaintyId(unit.publication(),ids.id("uncertainty",role,subject.localId(),code));
        gaps.add(new Evidence.Uncertainty(id,code,dimensions,new Scopes.EntityScope(List.of(subject)),"Source-only file fact; "+code,origin));return id;
    }
    List<Interactions.Resource> resources(){
        var result=new ArrayList<Interactions.Resource>();
        for(var entry:declarations.entrySet()){
            var f=entry.getValue();var id=new ResourceId(unit.publication(),ids.id("resource","file-declaration",unit.localId(),f.id()));
            var evidence=new ArrayList<OriginId>();int ordinal=0;for(var p:f.origins())evidence.add(origins.source("file-declaration",f.id()+"/"+ordinal++,p));
            var origin=origins.derived(ids.id("origin","file-declaration",unit.localId(),f.id()),evidence,"file-declaration@1/typed-owner");
            Interactions.ResourceDescription description;
            if(f.kind()==FileFacts.Kind.SD)description=new Interactions.LocalResource("file");
            else if(f.assignment().externalFileName().isPresent())description=new Interactions.LiteralTarget("file","cobol.external-file-name",f.assignment().externalFileName().orElseThrow(),Interactions.ExactName.INSTANCE,origin);
            else description=new Interactions.UnknownResource("file","cobol.external-file-name",gap("file-declaration-target",id,origin,List.of(Evidence.Dimension.DEPENDENCIES),"FILE_NAME_NOT_PROVEN"));
            var objects=new ArrayList<Interactions.ResourceObject>();
            for(var record:f.records()){
                var object=data.nominal().get(record);
                if(object!=null)objects.add(new Interactions.ResourceObject(object,"record"));
                else gap("file-record",id,origin,List.of(Evidence.Dimension.DEPENDENCIES,Evidence.Dimension.STORAGE),"FILE_RECORD_NOT_MATERIALIZED_"+record.handle());
            }
            var kind=switch(f.kind()){case FD->"cobol.fd";case SD->"cobol.sd";case UNKNOWN->"cobol.unknown";};
            var source=switch(f.assignment().sourceKind()){case ASSIGNMENT_NAME->"cobol.assignment-name";case SORT_COMMENT->"cobol.sort-comment";case UNSUPPORTED->"cobol.unsupported-name";case ABSENT->"cobol.absent-name";};
            result.add(new Interactions.Resource(id,description,origin,Optional.of(new Interactions.ResourceDeclaration(unit,f.logicalFile(),kind,source,objects,associations.getOrDefault(entry.getKey(),List.of())))));
        }
        return List.copyOf(result);
    }
    boolean available(){return input.fileInventory().availability()!=SpInput.Availability.UNAVAILABLE;}
}
