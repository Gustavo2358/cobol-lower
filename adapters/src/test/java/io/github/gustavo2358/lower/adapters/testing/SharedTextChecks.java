package io.github.gustavo2358.lower.adapters.testing;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.node.*;
import io.github.gustavo2358.lower.adapters.sp.SpJsonDecoder;
import io.github.gustavo2358.lower.adapters.cli.CobolLower;
import io.github.gustavo2358.lower.application.CobolLowerer;
import io.github.gustavo2358.lower.domain.SpInput;
import io.github.gustavo2358.air.model.*;
import io.github.gustavo2358.air.model.Ids.*;
import java.util.*;

final class SharedTextChecks {
    static void run() throws Exception {
        var json=new ObjectMapper();var decoder=new SpJsonDecoder(CobolLower.INPUT_LIMITS);
        var fixture=(ObjectNode)json.readTree(SharedTextChecks.class.getResourceAsStream("/sp/logical-slice/shared-text.json"));
        var decoded=decoder.decode(json.writeValueAsBytes(fixture));
        if(!(decoded instanceof SpJsonDecoder.Decoded ok))throw new AssertionError(decoded);
        var result=new CobolLowerer().lower(ok.input(),CobolLower.POSITIVE_OPTIONS);
        var publication=result.publication().orElseThrow(()->new AssertionError(result.admission().diagnostics()));
        var memory=new HashMap<ObjectId,String>();var choices=new HashMap<UncertaintyId,String>();
        publication.uncertainties().stream().filter(u->u.code().equals("COLLATING_CHARACTER_NOT_SELECTED"))
            .forEach(u->choices.put(u.id(),"ÿ"));
        for(var statement:ok.input().statements())if(statement instanceof SpInput.MoveFact) {
            var labels=new HashSet<LabelId>();result.statements().stream().filter(l->l.source().equals(statement.header().id())).forEach(l->labels.add(l.label()));
            for(var unit:publication.units())for(var sequence:unit.sequences())if(labels.contains(sequence.label()))for(var instruction:sequence.instructions()) {
                if(!(instruction instanceof Operations.Assign a)||!(a.destination() instanceof Places.ObjectPlace dest))throw new AssertionError(instruction);
                memory.put(dest.object(),FigurativeFillChecks.eval(a.value(),memory,choices));
            }
        }
        var expected=Map.of("TEXT-A","PBCGA   ","TEXT-B","ÿÿÿÿÿÿÿÿ","OUT-A","PROGA   ","CALLEE","MENU0001");
        var objects=new HashMap<String,Memory.ObjectDeclaration>();
        for(var unit:publication.units())for(var object:unit.objects())object.displayName().ifPresent(name->objects.put(name,object));
        for(var item:expected.entrySet())if(!item.getValue().equals(memory.get(objects.get(item.getKey()).id())))throw new AssertionError(item+" actual "+memory);
        for(var name:List.of("TEXT-A","TEXT-B")) {
            var object=objects.get(name);var numeric=objects.get(name.equals("TEXT-A")?"NUM-A":"NUM-B");
            if(!(object.storage() instanceof Memory.UnknownBinding t)||!(numeric.storage() instanceof Memory.UnknownBinding n)
                ||!t.scope().equals(n.scope())||!(t.scope() instanceof Scopes.StorageMemory))throw new AssertionError("shared location promoted to independent text cell");
            if(!object.typeRef().equals(Types.known(Types.Builtin.TEXT)))throw new AssertionError("text type lost");
        }
        var forged=fixture.deepCopy();var move=(ObjectNode)forged.path("statements").get(1);
        var source=(ObjectNode)move.path("source").path("reference");var target=(ObjectNode)move.path("target");
        for(var field:List.of("binding","wholeItemAccess","logicalWholeItem"))target.set(field,source.path(field).deepCopy());
        var bad=decoder.decode(json.writeValueAsBytes(forged));
        if(bad instanceof SpJsonDecoder.Decoded d&&new CobolLowerer().lower(d.input(),CobolLower.POSITIVE_OPTIONS).publication().isPresent())throw new AssertionError("overlapping shared textual transfer admitted");
        System.out.println("SHARED_TEXT=PASS: fitting, slices, disjoint copies, unknown collating scalar, original alias bounds and overlap rejection");
    }
}
