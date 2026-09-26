package io.github.gustavo2358.lower.application;
import java.util.*;
import io.github.gustavo2358.lower.domain.*;
import io.github.gustavo2358.air.model.*;
import io.github.gustavo2358.air.model.Ids.*;
/** Per-run composition state. Captures alias source objects; units never execute each other. */
final class CompilationContext {
    final Map<SpInput.UnitKey,SpCompilation.UnitProduct> products=new LinkedHashMap<>();
    final Map<SpInput.UnitKey,UnitId> units=new LinkedHashMap<>();
    final Map<SpInput.UnitKey,PartialProgramLowerer.Fragment> fragments=new LinkedHashMap<>();
    final Map<SpInput.UnitKey,Set<SpInput.DataId>> capturedLogicalText=new HashMap<>();
    Optional<UnitId> parent(SpInput.UnitKey unit){return products.get(unit).parent().map(units::get);}
    Set<SpInput.DataId> requiredData(SpInput input) {
        var required=new LinkedHashSet<SpInput.DataId>(products.get(input.unit()).globalData());
        for(var product:products.values())for(var capture:product.dataCaptures()) {
            if(capture.localData().unit().equals(input.unit()))required.add(capture.localData());
            if(capture.sourceData().unit().equals(input.unit()))required.add(capture.sourceData());
        }
        return Set.copyOf(required);
    }
    Set<SpInput.DataId> logicalText(SpInput input){return capturedLogicalText.getOrDefault(input.unit(),Set.of());}
    Set<SpInput.DataId> captureLocals(SpInput input){return products.get(input.unit()).dataCaptures().stream().map(SpCompilation.DataCapture::localData).collect(java.util.stream.Collectors.toUnmodifiableSet());}
    List<ObjectId> visible(SpInput.UnitKey unit){
        var result=new LinkedHashSet<ObjectId>();
        for(var entry:products.entrySet())if(CompilationAdmission.ancestor(entry.getKey(),unit)){
            var fragment=fragments.get(entry.getKey());for(var id:entry.getValue().globalData()){
                var object=fragment.storage().nominal().get(id);if(object!=null)result.add(object);
            }
        }
        for(var capture:products.get(unit).dataCaptures()){var object=fragments.get(capture.sourceData().unit()).storage().nominal().get(capture.sourceData());if(object!=null)result.add(object);}
        return List.copyOf(result);
    }
    void importFiles(SpInput input,FileResourceLowering files){
        var result=new ArrayList<FileFacts.Declaration>();for(var c:products.get(input.unit()).fileCaptures())
            products.get(c.owner()).product().fileInventory().declarations().stream().filter(f->f.id().equals(c.id())).forEach(result::add);
        files.imports(result);
    }
    ScalarDataTranslator.Result captures(SpInput input,ScalarDataTranslator.Result data,UnitId unit,LocalIds ids,SourceOrigins origins,List<Evidence.CoverageItem> items,List<Evidence.Uncertainty> uncertainties){
        var objects=new ArrayList<>(data.objects());var index=new LinkedHashMap<>(data.index());var nominal=new LinkedHashMap<>(data.nominal());
        for(var capture:products.get(input.unit()).dataCaptures()){
            var source=fragments.get(capture.sourceData().unit()).storage();var target=source.nominal().get(capture.sourceData());
            if(target==null)throw new IllegalStateException("admitted source declaration lacks nominal object");
            var original=source.objects().stream().filter(o->o.id().equals(target)).findFirst().orElseThrow();
            var local=nominal.get(capture.localData());if(local==null)throw new IllegalStateException("capture declaration lacks local object");
            for(int i=0;i<objects.size();i++)if(objects.get(i).id().equals(local)){
                var old=objects.get(i);var origin=origins.derived(ids.id("origin","global-capture",unit.localId(),capture.localData().handle()),List.of(old.origin(),original.origin()),"compilation@1/explicit-global-capture");
                objects.set(i,new Memory.ObjectDeclaration(local,old.displayName(),original.typeRef(),new Memory.AliasBinding(target),Memory.Visibility.PRIVATE,origin,old.coverage(),old.precision()));
                var link=source.index().get(capture.sourceData());if(link!=null)index.put(capture.localData(),new LoweringResult.DataLink(capture.localData(),local,link.storage(),origin));
            }
        }
        return new ScalarDataTranslator.Result(List.copyOf(objects),data.storage(),Collections.unmodifiableMap(index),data.views(),data.physical(),Collections.unmodifiableMap(nominal),data.logicalTextExtents());
    }
}
