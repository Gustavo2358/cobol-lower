package io.github.gustavo2358.lower.application;
import java.util.*;
import io.github.gustavo2358.lower.domain.*;
import static io.github.gustavo2358.lower.domain.SpInput.*;
/** The same closed cross-unit checks govern JSON and direct memory callers. */
final class CompilationAdmission {
    static Optional<String> validate(SpCompilation input){
        if(input==null||input.units().isEmpty())return Optional.of("nonempty compilation required");
        var units=new LinkedHashMap<UnitKey,SpCompilation.UnitProduct>();
        for(var u:input.units())if(units.put(u.product().unit(),u)!=null)return Optional.of("duplicate unit");
        if(new HashSet<>(input.unitInventory()).size()!=input.unitInventory().size()||!new HashSet<>(input.unitInventory()).equals(units.keySet()))return Optional.of("inventory must cover every unit exactly once");
        var paths=new HashSet<java.util.Map.Entry<String,List<Integer>>>();
        for(var u:input.units())if(!paths.add(Map.entry(u.product().unit().compilationUnitId(),u.product().unit().structuralPath())))return Optional.of("canonical structural path cannot have two identities");
        for(var u:input.units()){
            var id=u.product().unit();var path=id.structuralPath();
            if(path.isEmpty()||path.stream().anyMatch(i->i<0))return Optional.of("canonical structural path required");
            if(path.size()==1&&u.parent().isPresent()||path.size()>1&&u.parent().isEmpty())return Optional.of("canonical parent required");
            if(u.parent().isPresent()){
                var parent=u.parent().orElseThrow();
                if(!units.containsKey(parent)||!parent.compilationUnitId().equals(id.compilationUnitId())||!parent.structuralPath().equals(path.subList(0,path.size()-1)))return Optional.of("parent identity/path must close");
            }
            var data=new HashMap<DataId,DataFact>();u.product().dataDeclarations().forEach(d->data.put(d.id(),d));
            var partition=new HashSet<DataId>();
            for(var d:u.ownedData())if(!d.unit().equals(id)||!data.containsKey(d)||!partition.add(d))return Optional.of("owned DATA inventory must close");
            if(new HashSet<>(u.globalData()).size()!=u.globalData().size()||!partition.containsAll(u.globalData()))return Optional.of("GLOBAL DATA must be owned");
            for(var c:u.dataCaptures()){
                if(!c.localData().unit().equals(id)||!data.containsKey(c.localData())||!partition.add(c.localData()))return Optional.of("capture local identity must close once");
                var source=units.get(c.sourceData().unit());
                if(source==null||!ancestor(c.sourceData().unit(),id)||!source.globalData().contains(c.sourceData())&&!implicitFileData(u,source,c.sourceData()))return Optional.of("capture requires canonical ancestor GLOBAL DATA");
                var original=source.product().dataDeclarations().stream().filter(d->d.id().equals(c.sourceData())).findFirst();
                if(original.isEmpty()||!same(data.get(c.localData()),original.orElseThrow()))return Optional.of("capture declaration metadata differs");
                // Captures cannot assert an independent local storage allocation or entry value.
                if(u.product().storage().stream().flatMap(s->s.nodes().stream()).anyMatch(n->n.data().filter(c.localData()::equals).isPresent()))return Optional.of("captured DATA must not acquire a local storage base");
                if(u.product().storageIndependence().stream().anyMatch(s->s.members().contains(c.localData())))return Optional.of("capture cannot be asserted independent");
            }
            if(!partition.equals(data.keySet()))return Optional.of("owned/captured DATA must partition declarations");
            var required=new HashSet<FileFacts.Candidate>();
            for(var use:u.product().fileInventory().operations().uses())for(var c:use.candidates())if(!c.owner().equals(id))required.add(c);
            if(u.fileCaptures().size()!=new HashSet<>(u.fileCaptures()).size()||!required.equals(new HashSet<>(u.fileCaptures())))return Optional.of("FILE captures must close foreign uses");
            var captureMap=new HashMap<DataId,DataId>();u.dataCaptures().forEach(c->captureMap.put(c.localData(),c.sourceData()));
            for(var use:u.product().fileInventory().operations().uses())if(use.bindingStatus()==ResolutionStatus.RESOLVED&&!use.candidates().isEmpty()&&!use.candidates().getFirst().owner().equals(id)){
                var file=use.candidates().getFirst();var source=units.get(file.owner());
                if(source==null)return Optional.of("foreign FILE source absent");
                var declaration=source.product().fileInventory().declarations().stream().filter(f->f.id().equals(file.id())).findFirst();
                if(declaration.isEmpty())return Optional.of("foreign FILE declaration absent");
                if(declaration.get().kind()!=FileFacts.expectedKind(use))return Optional.of("foreign FILE kind mismatch");
                var statement=u.product().statements().stream().filter(t->t.header().id().equals(use.statement())).findFirst();
                for(var operand:use.surface().stream().flatMap(s->s.operands().stream()).toList())if(operand.role()==FileFacts.OperandRole.RECORD){
                    if(statement.isEmpty()||!(statement.get() instanceof OtherStatement observed))return Optional.of("foreign record operand unavailable");
                    for(var refId:operand.references()){
                        var ref=observed.knownReferences().stream().filter(r->r.id().equals(refId)).findFirst();
                        if(ref.isEmpty()||ref.get().binding().selected().isEmpty()||!declaration.get().records().contains(captureMap.get(ref.get().binding().selected().get())))return Optional.of("foreign record capture/FILE owner mismatch");
                    }
                }
            }
            for(var c:u.fileCaptures()){
                var source=units.get(c.owner());
                if(source==null||!ancestor(c.owner(),id)||source.product().fileInventory().declarations().stream().noneMatch(f->f.id().equals(c.id())&&f.visibility()==FileFacts.Visibility.GLOBAL))return Optional.of("FILE capture requires ancestor GLOBAL declaration");
            }
        }
        return Optional.empty();
    }
    private static boolean implicitFileData(SpCompilation.UnitProduct u,SpCompilation.UnitProduct source,DataId data){
        return source.product().fileInventory().declarations().stream().filter(f->u.fileCaptures().contains(new FileFacts.Candidate(f.id(),f.owner()))&&f.visibility()==FileFacts.Visibility.GLOBAL).anyMatch(f->f.records().contains(data)||f.references().stream().anyMatch(r->r.binding().selected().filter(data::equals).isPresent()));
    }
    private static boolean same(DataFact a,DataFact b){return a.canonicalName().equals(b.canonicalName())&&a.picture().equals(b.picture())&&a.provenance().equals(b.provenance())&&a.scalarText().equals(b.scalarText())&&a.scalarInteger().equals(b.scalarInteger());}
    static boolean ancestor(UnitKey a,UnitKey b){return a.compilationUnitId().equals(b.compilationUnitId())&&a.structuralPath().size()<b.structuralPath().size()&&b.structuralPath().subList(0,a.structuralPath().size()).equals(a.structuralPath());}
    static final Comparator<UnitKey> ORDER=(a,b)->{int c=a.compilationUnitId().compareTo(b.compilationUnitId());if(c!=0)return c;int n=Math.min(a.structuralPath().size(),b.structuralPath().size());for(int i=0;i<n;i++){c=Integer.compare(a.structuralPath().get(i),b.structuralPath().get(i));if(c!=0)return c;}c=Integer.compare(a.structuralPath().size(),b.structuralPath().size());return c!=0?c:a.canonicalProgramName().compareTo(b.canonicalProgramName());};
}
