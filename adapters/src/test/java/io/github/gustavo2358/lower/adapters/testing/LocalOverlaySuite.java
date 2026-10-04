package io.github.gustavo2358.lower.adapters.testing;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.node.*;
import io.github.gustavo2358.lower.adapters.sp.SpJsonDecoder;
import io.github.gustavo2358.lower.adapters.cli.CobolLower;
import io.github.gustavo2358.lower.application.*;
import io.github.gustavo2358.air.model.*;
import io.github.gustavo2358.air.model.Ids.*;
import io.github.gustavo2358.air.json.AirJson;
import java.util.*;
/** A BMS alias has one value cell; group writes include bytes named only in the other view. */
public final class LocalOverlaySuite {
    public static void main(String[] args) throws Exception {
        var json=new ObjectMapper();var decoder=new SpJsonDecoder(CobolLower.INPUT_LIMITS);
        var fixture=(ObjectNode)json.readTree(LocalOverlaySuite.class.getResourceAsStream("/sp/local-overlay/checkpoint.json"));
        var decoded=decoder.decode(json.writeValueAsBytes(fixture));
        if(!(decoded instanceof SpJsonDecoder.Decoded ok))throw new AssertionError(decoded);
        var result=new CobolLowerer().lower(ok.input(),CobolLower.POSITIVE_OPTIONS);
        var p=result.publication().orElseThrow(()->new AssertionError(result.admission().diagnostics()));
        if(!result.validation().orElseThrow().isStructurallyValid())throw new AssertionError(result.validation());
        var codec=new AirJson();if(!p.equals(codec.decode(codec.encode(p))))throw new AssertionError("alias codec");
        var objects=new HashMap<String,Memory.ObjectDeclaration>();p.units().forEach(u->u.objects().forEach(o->o.displayName().ifPresent(n->objects.put(n,o))));
        var input=(Memory.CellBinding)objects.get("NAME-I").storage();var output=(Memory.CellBinding)objects.get("NAME-O").storage();
        if(!input.equals(output))throw new AssertionError("exact aliases have independent cells");
        var length=(Memory.CellBinding)objects.get("LENGTH-A").storage();var flag=(Memory.CellBinding)objects.get("FLAG-A").storage();
        var group=(Memory.UnknownBinding)objects.get("OUTPUT-AREA").storage();
        var scope=(Scopes.StorageMemory)group.scope();
        if(!scope.storage().containsAll(List.of(input.storage(),length.storage(),flag.storage())))throw new AssertionError("group omits fields covered by FILLER: "+scope);
        var memory=new HashMap<StorageId,String>();boolean write=false;
        for(var unit:p.units())for(var q:unit.sequences())for(var instruction:q.instructions())if(instruction instanceof Operations.Assign a
                &&a.destination() instanceof Places.ObjectPlace place&&place.object().equals(objects.get("NAME-O").id())) {
            if(!(a.value() instanceof Expressions.Literal l)||!(l.value() instanceof Values.TextValue text))throw new AssertionError(a);
            memory.put(output.storage(),text.value());write=true;
        }
        if(!write||!"PGM00001".equals(memory.get(input.storage())))throw new AssertionError("alias does not read the written value");
        scope.storage().forEach(memory::remove);
        if(memory.containsKey(input.storage()))throw new AssertionError("group write did not invalidate alias");
        for(var declaration:fixture.path("dataDeclarations"))if(declaration.path("canonicalName").asText().equals("NAME-O"))
            ((ObjectNode)declaration.path("scalarText")).put("logicalExtent",7);
        var hostile=decoder.decode(json.writeValueAsBytes(fixture));
        if(hostile instanceof SpJsonDecoder.Decoded bad&&new CobolLowerer().lower(bad.input(),CobolLower.POSITIVE_OPTIONS).publication().isPresent())throw new AssertionError("incompatible alias extent accepted");
        System.out.println("LOCAL_OVERLAY=PASS: one alias cell, binary value, group/FILLER invalidation, hostile extent");
    }
}
