package io.github.gustavo2358.lower.application;
import io.github.gustavo2358.air.model.*;
import io.github.gustavo2358.air.model.Ids.*;
import io.github.gustavo2358.lower.domain.*;
import java.util.*;

/** Source-published phase order and bounded shared procedure returns. Participant
 * iteration is conservative: no total order or input/output Cartesian product. */
final class FileSortLowering {
    record Step(LabelId entry,LabelId next) { }
    record Layout(List<Sequence> prefix,Map<Integer,Step> uses) { }
    private final UnitId unit;private final LocalIds ids;private final SourceOrigins origins;private final List<Evidence.Uncertainty> gaps;
    private final Map<SpInput.StatementId,FileFacts.SortPlan> plans=new HashMap<>();
    private final Map<SpInput.StatementId,Set<LabelId>> boundaries=new LinkedHashMap<>();
    private final Set<SpInput.StatementId> ordinaryExits=new HashSet<>();
    private final Map<SpInput.StatementId,SpInput.StatementFact> facts=new HashMap<>();
    FileSortLowering(SpInput input,UnitId unit,LocalIds ids,SourceOrigins origins,List<Evidence.Uncertainty> gaps) {
        this.unit=unit;this.ids=ids;this.origins=origins;this.gaps=gaps;input.statements().forEach(s->facts.put(s.header().id(),s));
        input.fileInventory().sorts().ifPresent(inv->inv.plans().forEach(p->{
            plans.put(p.statement(),p);
            for(var r:p.procedures()) {
                for(var s:r.completions())boundaries.computeIfAbsent(s,k->new LinkedHashSet<>());
                for(var link:r.links())boundaries.computeIfAbsent(link.from(),k->new LinkedHashSet<>()).add(PartialProgramAssembler.label(link.to(),unit,ids));
            }
        }));
    }
    LabelId completion(SpInput.StatementId statement,LabelId ordinary) {
        var alternatives=boundaries.get(statement);if(alternatives==null)return ordinary;
        if(ordinary!=null)alternatives.add(ordinary);else ordinaryExits.add(statement);
        return label(ids,"boundary/"+statement.handle());
    }
    Optional<Layout> layout(SpInput.StatementFact fact,List<FileFacts.Use> uses,LabelId destination,LocalIds local) {
        var p=plans.get(fact.header().id());if(p==null||p.work()<0)return Optional.empty();
        var sequences=new ArrayList<Sequence>();var steps=new HashMap<Integer,Step>();
        var origin=origins.source("statement",fact.header().id().handle(),fact.header().provenance());var key=fact.header().id().handle();
        var output=phase(p,FileFacts.ProcedurePhase.OUTPUT,p.outputs(),destination,local,origin,sequences,steps);
        var work=label(local,key+"/use/"+p.work());steps.put(p.work(),new Step(work,output));
        var input=phase(p,FileFacts.ProcedurePhase.INPUT,p.inputs(),work,local,origin,sequences,steps);
        sequences.add(0,new Sequence(PartialProgramAssembler.label(fact.header().id(),unit,local),List.of(),jump(local,key+"/enter",origin,input),origin));
        return Optional.of(new Layout(List.copyOf(sequences),Map.copyOf(steps)));
    }
    private LabelId phase(FileFacts.SortPlan p,FileFacts.ProcedurePhase phase,List<Integer> ordinals,LabelId resume,LocalIds local,
            OriginId origin,List<Sequence> sequences,Map<Integer,Step> steps) {
        var key=p.statement().handle()+"/"+phase;var entry=label(local,key+"/select/0");
        var procedure=p.procedures().stream().filter(r->r.phase()==phase).findFirst().orElse(null);
        if(procedure!=null) {
            for(var s:procedure.completions())if(resume!=null)boundaries.get(s).add(resume);else ordinaryExits.add(s);
            var target=procedure.entry().map(s->PartialProgramAssembler.label(s,unit,ids)).orElse(resume);
            Terminator term=procedure.gapCodes().isEmpty()?continuing(local,key+"/call",origin,target):unavailable(local,key+"/call",origin,target);
            sequences.add(new Sequence(entry,List.of(),term,origin));
        } else if(ordinals.isEmpty())sequences.add(new Sequence(entry,List.of(),continuing(local,key+"/empty",origin,resume),origin));
        else {
            var exit=label(local,key+"/exit");sequences.add(new Sequence(exit,List.of(),continuing(local,key+"/exit",origin,resume),origin));
            for(int i=0;i<ordinals.size();i++) {
                int ordinal=ordinals.get(i);var member=label(local,p.statement().handle()+"/use/"+ordinal);
                steps.put(ordinal,new Step(member,entry));
                var op=operation(local,key+"/select/"+i);var h=unknown(op,origin,"FILE_AGGREGATE_ORDER_COUNT_NOT_PROVEN");
                var predicate=new Expressions.Unknown(new Operand.Header(new OperandId(new OperationOwner(op),"condition"),Operand.Role.PREDICATE,origin),Types.known(Types.Builtin.BOOL),List.of(),Scopes.NoMemory.INSTANCE,h.uncertainties().getFirst());
                sequences.add(new Sequence(label(local,key+"/select/"+i),List.of(),new Operations.Branch(h,predicate,member,i+1==ordinals.size()?exit:label(local,key+"/select/"+(i+1))),origin));
            }
        }
        return entry;
    }
    List<Sequence> complete(List<Sequence> source) {
        var result=new ArrayList<>(source);
        for(var entry:boundaries.entrySet()) {
            var statement=entry.getKey();var fact=facts.get(statement);var key="boundary/"+statement.handle();var origin=origins.source("statement",statement.handle(),fact.header().provenance());
            var alternatives=entry.getValue().stream().sorted(Comparator.comparing(LabelId::localId)).<Control.ControlAlternative>map(Control.JumpAlternative::new).toList();
            Scopes.ControlBound remainder=ordinaryExits.contains(statement)?new Scopes.WithinControl(new Scopes.UnitControl(unit,false,true,true,false,false,false)):Scopes.NoControl.INSTANCE;
            var envelope=new Envelopes.Envelope(new Envelopes.MemoryEnvelope(List.of(),Scopes.NoMemory.INSTANCE,List.of(),Scopes.NoMemory.INSTANCE,List.of()),new Control.ControlEnvelope(alternatives,remainder),new Envelopes.DependencyEnvelope(List.of(),Scopes.NoResources.INSTANCE));
            result.add(new Sequence(label(ids,key),List.of(),new Operations.Opaque(unknown(operation(ids,key),origin,"LOCAL_RETURN_CONTEXT_NOT_PROVEN"),"sort-procedure-boundary",List.of(),List.of(),envelope),origin));
        }
        return List.copyOf(result);
    }
    private Operations.Jump jump(LocalIds local,String key,OriginId origin,LabelId target){var op=operation(local,key);return new Operations.Jump(new Operations.Header(op,origin,Evidence.CoverageStatus.MODELED,ScalarEvidence.assign(op),List.of()),target);}
    private Terminator continuing(LocalIds local,String key,OriginId origin,LabelId target) {
        if(target!=null)return jump(local,key,origin,target);
        return new Operations.Opaque(unknown(operation(local,key),origin,"FILE_NORMAL_CONTINUATION_NOT_PROVEN"),"sort-phase-exit",List.of(),List.of(),new Envelopes.Envelope(new Envelopes.MemoryEnvelope(List.of(),Scopes.NoMemory.INSTANCE,List.of(),Scopes.NoMemory.INSTANCE,List.of()),new Control.ControlEnvelope(List.of(),new Scopes.WithinControl(new Scopes.UnitControl(unit,false,true,true,false,false,false))),new Envelopes.DependencyEnvelope(List.of(),Scopes.NoResources.INSTANCE)));
    }
    private Terminator unavailable(LocalIds local,String key,OriginId origin,LabelId resume) {
        var h=unknown(operation(local,key),origin,"FILE_PROCEDURE_ENDPOINT_NOT_PROVEN");var memory=new Scopes.WithinMemory(new Scopes.VisibleMemory(unit,true));
        var labels=facts.keySet().stream().map(s->PartialProgramAssembler.label(s,unit,ids)).sorted(Comparator.comparing(LabelId::localId)).toList();
        Scopes.ControlBound bound=new Scopes.WithinControl(new Scopes.ControlUnion(List.of(new Scopes.LabelsControl(labels),new Scopes.UnitControl(unit,false,true,true,true,true,true))));
        return new Operations.Opaque(h,"sort-procedure-unavailable",List.of(),List.of(),new Envelopes.Envelope(new Envelopes.MemoryEnvelope(List.of(),memory,List.of(),memory,List.of()),new Control.ControlEnvelope(resume==null?List.of():List.of(new Control.JumpAlternative(resume)),bound),new Envelopes.DependencyEnvelope(List.of(),new Scopes.ResourceCategories(List.of("file","program")))));
    }
    private LabelId label(LocalIds local,String key){return new LabelId(unit,local.id("label","file-sort",unit.localId(),key));}
    private OperationId operation(LocalIds local,String key){return new OperationId(unit,local.id("operation","file-sort",unit.localId(),key));}
    private Operations.Header unknown(OperationId op,OriginId origin,String code) {
        var id=new UncertaintyId(unit.publication(),ids.id("uncertainty","file-sort",op.localId(),code));gaps.add(new Evidence.Uncertainty(id,code,List.of(Evidence.Dimension.CONTROL,Evidence.Dimension.VALUES,Evidence.Dimension.EFFECTS,Evidence.Dimension.DEPENDENCIES),new Scopes.EntityScope(List.of(op)),"Published SORT phases/local return approximation",origin));
        var scope=new Scopes.EntityScope(List.of(op));var known=new Evidence.Claim(scope,Evidence.PrecisionStatus.EXACT,List.of());var open=new Evidence.Claim(scope,Evidence.PrecisionStatus.OPEN,List.of(id));
        return new Operations.Header(op,origin,Evidence.CoverageStatus.ABSTRACTED,new Evidence.Precision(open,known,open,open,open),List.of(id));
    }
}
