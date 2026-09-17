package io.github.gustavo2358.lower.application;

import io.github.gustavo2358.air.model.*;
import io.github.gustavo2358.air.model.Ids.*;
import io.github.gustavo2358.lower.domain.FileFacts;
import io.github.gustavo2358.lower.domain.SpInput;
import io.github.gustavo2358.lower.domain.StorageFacts;
import java.math.BigInteger;
import java.util.*;

/** Admitted source memory plans become ordinary AIR byte transfers and effects.
 * Cases describe conditional memory semantics, not COBOL outcome feasibility. */
final class FileMemoryLowering {
    private final ScalarDataTranslator.Result data;
    private final Map<StorageFacts.NodeId,StorageFacts.View> views;
    private final Map<StorageFacts.BaseId,StorageFacts.Base> bases;
    private final UnitId unit;
    private final LocalIds ids;
    private final SourceOrigins origins;
    private final List<Evidence.Uncertainty> uncertainties;
    private final Set<OperationId> continuations;
    FileMemoryLowering(SpInput input,ScalarDataTranslator.Result data,UnitId unit,LocalIds ids,SourceOrigins origins,List<Evidence.Uncertainty> uncertainties) {
        this.data=data;this.unit=unit;this.ids=ids;this.origins=origins;this.uncertainties=uncertainties;
        this.views=new HashMap<>();this.bases=new HashMap<>();
        this.continuations=new HashSet<>();
        input.storage().ifPresent(s->{s.views().forEach(v->views.put(v.node(),v));s.bases().forEach(b->bases.put(b.id(),b));});
    }
    private FileMemoryLowering(FileMemoryLowering source,LocalIds ids) {
        data=source.data;unit=source.unit;this.ids=ids;origins=source.origins;uncertainties=source.uncertainties;views=source.views;bases=source.bases;continuations=source.continuations;
    }
    FileMemoryLowering withIds(LocalIds local){return ids==local?this:new FileMemoryLowering(this,local);}
    List<Sequence> restrictContinuations(List<Sequence> sequences,List<LabelId> sourceEntries) {
        var bound=new Scopes.WithinControl(new Scopes.ControlUnion(List.of(new Scopes.LabelsControl(sourceEntries),new Scopes.UnitControl(unit,false,true,true,true,true,true))));
        return sequences.stream().map(s->{
            if(!continuations.contains(s.terminator().header().id()))return s;
            var op=(Operations.Opaque)s.terminator();var e=op.envelope();
            return new Sequence(s.label(),s.instructions(),new Operations.Opaque(op.header(),op.observedKind(),op.knownOperands(),op.valueResults(),
                new Envelopes.Envelope(e.memory(),new Control.ControlEnvelope(e.control().known(),bound),e.dependencies())),s.origin());
        }).toList();
    }
    LabelId label(String key){return new LabelId(unit,ids.id("label","file-memory",unit.localId(),key));}
    OperationId operation(String key){return new OperationId(unit,ids.id("operation","file-memory",unit.localId(),key));}
    Scopes.MemoryBound bound(List<FileFacts.MemoryTarget> targets,boolean unknown) {
        if(unknown)return visible();
        var scopes=new LinkedHashSet<Scopes.MemoryScope>();
        for(var target:targets) {
            var scope=scope(target);if(scope instanceof Scopes.VisibleMemory)return new Scopes.WithinMemory(scope);
            scopes.add(scope);
        }
        return scopes.isEmpty()?Scopes.NoMemory.INSTANCE:new Scopes.WithinMemory(scopes.size()==1?scopes.iterator().next():new Scopes.MemoryUnion(List.copyOf(scopes)));
    }
    private Scopes.MemoryBound visible(){return new Scopes.WithinMemory(new Scopes.VisibleMemory(unit,true));}
    private Scopes.MemoryScope scope(FileFacts.MemoryTarget target) {
        if(target.wholeBase()){var link=target.data().map(data.index()::get).orElse(null);if(link!=null&&link.storage().isPresent())return new Scopes.StorageMemory(List.of(link.storage().get()));}
        var view=target.regional().map(a->views.get(a.view())).orElse(null);
        if(view!=null&&data.physical().containsKey(view.base()))return new Scopes.StorageMemory(List.of(data.physical().get(view.base())));
        var object=target.data().map(data.nominal()::get).orElse(null);
        return object!=null?new Scopes.ObjectsMemory(List.of(object)):new Scopes.VisibleMemory(unit,true);
    }
    private Memory.ViewBinding exact(FileFacts.MemoryTarget target,boolean text) {
        if(target.wholeBase()||target.regional().isEmpty())return null;
        var access=target.regional().orElseThrow();var view=views.get(access.view());
        if(view==null||!data.physical().containsKey(view.base())||bases.get(view.base()).extent().value().isEmpty()
                ||view.offset().value().isEmpty()||view.extent().value().isEmpty()||view.extent().value().orElseThrow().signum()<=0)return null;
        var offset=access.slice().map(StorageFacts.Slice::offset).orElseGet(()->view.offset().value().orElseThrow());
        var extent=access.slice().map(StorageFacts.Slice::extent).orElseGet(()->view.extent().value().orElseThrow());
        return new Memory.ViewBinding(data.physical().get(view.base()),offset,extent,text?RegionalStorageAdmission.IBM1047:Memory.IdentityBytes.INSTANCE);
    }
    private Expression integer(BigInteger value,OperationId op,String key,OriginId origin) {
        return new Expressions.Literal(new Operand.Header(new OperandId(new OperationOwner(op),key),Operand.Role.ADDRESS_READ,origin),new Values.IntValue(value));
    }
    private Memory.ByteRange range(Memory.ViewBinding view,OperationId op,String key,OriginId origin) {
        return new Memory.ByteRange(view.region(),integer(view.offset(),op,key+"/offset",origin),integer(view.extent(),op,key+"/extent",origin));
    }
    private Place place(Memory.ViewBinding view,OperationId op,String key,OriginId origin,boolean text,Operand.Role role) {
        var range=range(view,op,key,origin);
        return new Places.RegionSlice(new Operand.Header(new OperandId(new OperationOwner(op),key),role,origin),view.region(),range.offset(),range.extent(),view.codec(),Types.known(text?Types.Builtin.TEXT:Types.Builtin.BYTES));
    }
    private UncertaintyId reason(OperationId op,OriginId origin,String code,List<Evidence.Dimension> dimensions) {
        var id=new UncertaintyId(unit.publication(),ids.id("uncertainty","file-memory",op.localId(),code));
        uncertainties.add(new Evidence.Uncertainty(id,code,dimensions,new Scopes.EntityScope(List.of(op)),"Published conditional file memory effect",origin));return id;
    }
    private Operations.Header abstractHeader(OperationId op,OriginId origin,UncertaintyId reason) {
        var scope=new Scopes.EntityScope(List.of(op));var exact=new Evidence.Claim(scope,Evidence.PrecisionStatus.EXACT,List.of());
        var open=new Evidence.Claim(scope,Evidence.PrecisionStatus.OPEN,List.of(reason));
        return new Operations.Header(op,origin,Evidence.CoverageStatus.ABSTRACTED,new Evidence.Precision(exact,open,open,open,exact),List.of(reason));
    }
    private Operations.Jump jump(String key,OriginId origin,LabelId next) {
        var op=operation(key+"/continue");return new Operations.Jump(new Operations.Header(op,origin,Evidence.CoverageStatus.MODELED,ScalarEvidence.assign(op),List.of()),next);
    }
    List<Sequence> steps(String key,List<FileFacts.MemoryStep> steps,LabelId start,LabelId next,OriginId parent) {
        var sequences=new ArrayList<Sequence>();
        for(int n=0;n<steps.size();n++) {
            var step=steps.get(n);var stepKey=key+"/"+n;var op=operation(stepKey);
            var source=origins.source("file-memory-step",op.localId(),step.provenance());
            var origin=origins.derived(ids.id("origin","file-memory",unit.localId(),op.localId()),List.of(parent,source),"file-memory@1/"+key+"/"+step.role());
            var label=n==0?start:label(stepKey);var dest=n+1<steps.size()?label(key+"/"+(n+1)):next;
            var instructions=new ArrayList<Instruction>();Terminator term=jump(stepKey,origin,dest);
            if(step.kind()==FileFacts.MemoryKind.MAY_UNKNOWN) {
                var reason=reason(op,origin,"FILE_MEMORY_MAY_UNKNOWN",List.of(Evidence.Dimension.EFFECTS,Evidence.Dimension.STORAGE,Evidence.Dimension.VALUES));
                var view=exact(step.destination(),false);
                // An explicit regional operand retains the precise MAY interval. A whole-base
                // bound is used only when the producer cannot prove the receiver address.
                var operands=new ArrayList<Operand>();var reads=new ArrayList<OperandId>();var writes=new ArrayList<OperandId>();
                Scopes.MemoryBound otherReads=Scopes.NoMemory.INSTANCE;
                if(step.role()==FileFacts.MemoryRole.FROM_RECORD) {
                    var from=step.source().map(t->exact(t,false)).orElse(null);
                    if(from!=null) {var sourcePlace=place(from,op,"source",origin,false,Operand.Role.VALUE_READ);operands.add(sourcePlace);reads.add(sourcePlace.header().id());}
                    else otherReads=bound(step.source().stream().toList(),step.source().isEmpty()||step.source().orElseThrow().wholeBase());
                }
                if(view!=null){var receiver=place(view,op,"receiver",origin,false,Operand.Role.VALUE_WRITE);operands.add(receiver);writes.add(receiver.header().id());}
                var envelope=new Envelopes.Envelope(new Envelopes.MemoryEnvelope(reads,otherReads,writes,view==null?new Scopes.WithinMemory(scope(step.destination())):Scopes.NoMemory.INSTANCE,List.of()),
                    new Control.ControlEnvelope(List.of(new Control.JumpAlternative(dest)),Scopes.NoControl.INSTANCE),new Envelopes.DependencyEnvelope(List.of(),Scopes.NoResources.INSTANCE));
                term=new Operations.Opaque(abstractHeader(op,origin,reason),"published-memory-may-write",operands,List.of(),envelope);
            } else if(step.kind()==FileFacts.MemoryKind.MUST_UNKNOWN) {
                var reason=reason(op,origin,"FILE_RECEIVER_VALUE_UNKNOWN",List.of(Evidence.Dimension.VALUES));
                instructions.add(new Operations.HavocMust(abstractHeader(op,origin,reason),place(Objects.requireNonNull(exact(step.destination(),true)),op,"receiver",origin,true,Operand.Role.VALUE_WRITE),reason));
            } else {
                var destination=Objects.requireNonNull(exact(step.destination(),true));var from=Objects.requireNonNull(exact(step.source().orElseThrow(),true));
                var header=new Operations.Header(op,origin,Evidence.CoverageStatus.MODELED,ScalarEvidence.assign(op),List.of());
                if(step.kind()==FileFacts.MemoryKind.COPY_BYTES) {
                    var fallback=new Envelopes.Envelope(new Envelopes.MemoryEnvelope(List.of(),new Scopes.WithinMemory(new Scopes.StorageMemory(List.of(from.region()))),List.of(),
                        new Scopes.WithinMemory(new Scopes.StorageMemory(List.of(destination.region()))),List.of()),new Control.ControlEnvelope(List.of(Control.ContinueAlternative.INSTANCE),Scopes.NoControl.INSTANCE),new Envelopes.DependencyEnvelope(List.of(),Scopes.NoResources.INSTANCE));
                    instructions.add(new Operations.CopyBytes(header,range(destination,op,"destination",origin),range(from,op,"source",origin),destination.extent(),fallback));
                } else {
                    var read=new Expressions.Read(new Operand.Header(new OperandId(new OperationOwner(op),"read"),Operand.Role.VALUE_READ,origin),place(from,op,"source",origin,true,Operand.Role.VALUE_READ));
                    var fit=new Expressions.FitText(new Operand.Header(new OperandId(new OperationOwner(op),"fit"),Operand.Role.VALUE_READ,origin),read,destination.extent()," ");
                    instructions.add(new Operations.Assign(header,place(destination,op,"destination",origin,true,Operand.Role.VALUE_WRITE),fit));
                }
            }
            sequences.add(new Sequence(label,instructions,term,origin));
        }
        return List.copyOf(sequences);
    }
    List<Sequence> after(String key,FileFacts.EffectPlan plan,LabelId next,OriginId origin) {
        var result=new ArrayList<Sequence>();var cases=plan.outcomes();
        for(int n=0;n<cases.size();n++) {
            var outcome=cases.get(n);var caseKey=key+"/"+outcome.outcome();var entry=label(caseKey);var end=label(caseKey+"/end");
            if(n<cases.size()-1) {
                var op=operation(key+"/select/"+n);var reason=reason(op,origin,"FILE_OUTCOME_UNKNOWN",List.of(Evidence.Dimension.CONTROL,Evidence.Dimension.VALUES));
                var select=new Operations.Branch(abstractHeader(op,origin,reason),new Expressions.Unknown(new Operand.Header(new OperandId(new OperationOwner(op),"outcome"),Operand.Role.PREDICATE,origin),
                    Types.known(Types.Builtin.BOOL),List.of(),Scopes.NoMemory.INSTANCE,reason),entry,n+2==cases.size()?label(key+"/"+cases.get(n+1).outcome()):label(key+"/select/"+(n+1)));
                result.add(new Sequence(label(key+"/select/"+n),List.of(),select,origin));
            }
            result.addAll(steps(caseKey,outcome.steps(),entry,end,origin));
            // W3 does not certify handlers/USE or feasible outcomes. Keep the residual
            // control explicit AFTER the conditional effects, never before status/INTO.
            var op=operation(caseKey+"/continuation");var reason=reason(op,origin,"FILE_CONTROL_PARTIAL",List.of(Evidence.Dimension.CONTROL));
            continuations.add(op);
            var envelope=new Envelopes.Envelope(new Envelopes.MemoryEnvelope(List.of(),Scopes.NoMemory.INSTANCE,List.of(),Scopes.NoMemory.INSTANCE,List.of()),
                new Control.ControlEnvelope(next==null?List.of():List.of(new Control.JumpAlternative(next)),new Scopes.WithinControl(new Scopes.UnitControl(unit,true,true,true,true,true,true))),new Envelopes.DependencyEnvelope(List.of(),Scopes.NoResources.INSTANCE));
            result.add(new Sequence(outcome.steps().isEmpty()?entry:end,List.of(),new Operations.Opaque(abstractHeader(op,origin,reason),"file-outcome-continuation",List.of(),List.of(),envelope),origin));
        }
        return List.copyOf(result);
    }
}
