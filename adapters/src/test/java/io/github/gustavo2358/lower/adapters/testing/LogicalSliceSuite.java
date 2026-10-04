package io.github.gustavo2358.lower.adapters.testing;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.node.*;
import io.github.gustavo2358.lower.adapters.sp.SpJsonDecoder;
import io.github.gustavo2358.lower.adapters.cli.CobolLower;
import io.github.gustavo2358.lower.application.CobolLowerer;
import io.github.gustavo2358.lower.domain.SpInput;
import io.github.gustavo2358.air.model.*;
import io.github.gustavo2358.air.model.Ids.*;
import io.github.gustavo2358.air.json.AirJson;
import java.util.*;
import java.util.function.Consumer;
/** Handwritten character-store oracle, including a disjoint copy within the same family. */
public final class LogicalSliceSuite {
    private static final ObjectMapper JSON=new ObjectMapper();
    private static final SpJsonDecoder DECODER=new SpJsonDecoder(CobolLower.INPUT_LIMITS);
    public static void main(String[] args) throws Exception {
        FigurativeFillChecks.run();
        SharedTextChecks.run();
        var fixture=(ObjectNode)JSON.readTree(LogicalSliceSuite.class.getResourceAsStream("/sp/logical-slice/checkpoint.json"));
        var decoded=DECODER.decode(JSON.writeValueAsBytes(fixture));
        if(!(decoded instanceof SpJsonDecoder.Decoded ok))throw new AssertionError(decoded);
        var result=new CobolLowerer().lower(ok.input(),CobolLower.POSITIVE_OPTIONS);
        var publication=result.publication().orElseThrow(()->new AssertionError(result.admission().diagnostics()));
        var codec=new AirJson();if(!publication.equals(codec.decode(codec.encode(publication))))throw new AssertionError("slice transport");
        var memory=new HashMap<ObjectId,Object>();
        for(var statement:ok.input().statements())if(statement instanceof SpInput.MoveFact) {
            var labels=new HashSet<LabelId>();result.statements().stream().filter(l->l.source().equals(statement.header().id())).forEach(l->labels.add(l.label()));
            for(var u:publication.units())for(var sequence:u.sequences())if(labels.contains(sequence.label()))for(var operation:sequence.instructions()) {
                if(!(operation instanceof Operations.Assign assign)||!(assign.destination() instanceof Places.ObjectPlace target))throw new AssertionError(operation);
                memory.put(target.object(),NumericEditSuite.eval(assign.value(),memory));
            }
        }
        var expected=Map.of("REC-A","ABxyABGzRS","A","ABxyABGz","B","RS","OUTPUT-A","ABxyABGzRS","SOURCE-A","qRSTuv");
        int checked=0;
        for(var u:publication.units())for(var object:u.objects())if(object.displayName().filter(expected::containsKey).isPresent()) {
            var value=expected.get(object.displayName().orElseThrow());
            if(!value.equals(memory.get(object.id())))throw new AssertionError(object.displayName()+" "+memory.get(object.id()));checked++;
        }
        if(checked!=expected.size())throw new AssertionError("missing objects "+checked);
        var shifted=fixture.deepCopy();slice(shifted).put("start","1");
        var shiftedInput=(SpJsonDecoder.Decoded)DECODER.decode(JSON.writeValueAsBytes(shifted));
        var shiftedResult=new CobolLowerer().lower(shiftedInput.input(),CobolLower.POSITIVE_OPTIONS);
        var shiftedPublication=shiftedResult.publication().orElseThrow(()->new AssertionError(shiftedResult.admission().diagnostics()));
        if(shiftedPublication.id().equals(publication.id()))throw new AssertionError("different writes must have different publication identities");
        reject(fixture,t->slice(t).put("length","0"));
        reject(fixture,t->slice(t).put("start","8"));
        reject(fixture,t->slice(t).put("data","data:99999"));
        reject(fixture,t->t.put("contractVersion","2.65.0"));
        var localDecoded=DECODER.decode(LogicalSliceSuite.class.getResourceAsStream("/sp/logical-slice/local.json").readAllBytes());
        if(!(localDecoded instanceof SpJsonDecoder.Decoded local))throw new AssertionError(localDecoded);
        var localResult=new CobolLowerer().lower(local.input(),CobolLower.POSITIVE_OPTIONS);
        var localPublication=localResult.publication().orElseThrow(()->new AssertionError(localResult.admission().diagnostics()));
        var aliasGroups=new HashMap<StorageId,List<ObjectId>>();var cells=new HashMap<ObjectId,StorageId>();
        for(var unit:localPublication.units())for(var object:unit.objects())if(object.storage() instanceof Memory.CellBinding cell) {
            cells.put(object.id(),cell.storage());aliasGroups.computeIfAbsent(cell.storage(),k->new ArrayList<>()).add(object.id());
        }
        var localMemory=new HashMap<ObjectId,Object>();
        for(var statement:local.input().statements())if(statement instanceof SpInput.MoveFact) {
            var labels=new HashSet<LabelId>();localResult.statements().stream().filter(l->l.source().equals(statement.header().id())).forEach(l->labels.add(l.label()));
            for(var unit:localPublication.units())for(var sequence:unit.sequences())if(labels.contains(sequence.label()))for(var operation:sequence.instructions()) {
                if(!(operation instanceof Operations.Assign assign)||!(assign.destination() instanceof Places.ObjectPlace target))throw new AssertionError(operation);
                var value=NumericEditSuite.eval(assign.value(),localMemory);
                for(var alias:aliasGroups.get(cells.get(target.object())))localMemory.put(alias,value);
            }
        }
        for(var unit:localPublication.units())for(var object:unit.objects())if(object.displayName().filter(Set.of("A","A-ALIAS","OUTPUT-A")::contains).isPresent())
            if(!"ABxyEFGH".equals(localMemory.get(object.id())))throw new AssertionError(object.displayName()+" "+localMemory.get(object.id()));
        System.out.println("LOGICAL_SLICE=PASS: AIR execution, adjacent characters, same-family disjoint copy, projections, hostile bounds");
    }
    private static ObjectNode slice(ObjectNode root) {
        for(var s:root.path("statements"))if(s.path("target").path("logicalSlice").isObject())return (ObjectNode)s.path("target").path("logicalSlice");
        throw new AssertionError("missing slice");
    }
    private static void reject(ObjectNode fixture,Consumer<ObjectNode> mutate) throws Exception {
        var forged=fixture.deepCopy();mutate.accept(forged);var d=DECODER.decode(JSON.writeValueAsBytes(forged));
        if(d instanceof SpJsonDecoder.Decoded ok&&new CobolLowerer().lower(ok.input(),CobolLower.POSITIVE_OPTIONS).publication().isPresent())throw new AssertionError("forged slice admitted");
    }
}
