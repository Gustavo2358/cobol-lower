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

/** Execute assignments independently for two possible collating characters. */
final class FigurativeFillChecks {
    static void run() throws Exception {
        var json=new ObjectMapper();var decoder=new SpJsonDecoder(CobolLower.INPUT_LIMITS);
        var fixture=(ObjectNode)json.readTree(FigurativeFillChecks.class.getResourceAsStream("/sp/logical-slice/figurative.json"));
        var decoded=decoder.decode(json.writeValueAsBytes(fixture));
        if(!(decoded instanceof SpJsonDecoder.Decoded ok))throw new AssertionError(decoded);
        var result=new CobolLowerer().lower(ok.input(),CobolLower.POSITIVE_OPTIONS);
        var publication=result.publication().orElseThrow(()->new AssertionError(result.admission().diagnostics()));
        for(var pair:List.of(List.of(Character.toString(0),"ÿ"),List.of("x","😀"))) {
            var choices=new HashMap<UncertaintyId,String>();
            for(var uncertainty:publication.uncertainties())if(uncertainty.code().equals("COLLATING_CHARACTER_NOT_SELECTED"))
                choices.put(uncertainty.id(),uncertainty.reason().contains("FIGURATIVE_LOW")?pair.get(0):pair.get(1));
            if(choices.size()!=3)throw new AssertionError("one uncertainty for each receiving write");
            var memory=new HashMap<ObjectId,String>();
            for(var statement:ok.input().statements())if(statement instanceof SpInput.MoveFact) {
                var labels=new HashSet<LabelId>();result.statements().stream().filter(l->l.source().equals(statement.header().id())).forEach(l->labels.add(l.label()));
                for(var unit:publication.units())for(var sequence:unit.sequences())if(labels.contains(sequence.label()))for(var instruction:sequence.instructions()) {
                    if(!(instruction instanceof Operations.Assign a)||!(a.destination() instanceof Places.ObjectPlace dest))throw new AssertionError(instruction);
                    if(a.header().uncertainties().stream().anyMatch(choices::containsKey)
                        &&a.header().precision().values().status()!=Evidence.PrecisionStatus.OPEN)throw new AssertionError("unknown character claimed exact");
                    memory.put(dest.object(),eval(a.value(),memory,choices));
                }
            }
            var low=pair.get(0);var high=pair.get(1);
            var expected=Map.of("REC-A","AB"+high+low+low+"FGH","LEFT-A","AB","MID-A",high+low+low+"F","RIGHT-A","GH","OUT-A","ABC"+low+low+"FGH","OUT-B",high+high+high);
            int seen=0;
            for(var unit:publication.units())for(var object:unit.objects())if(object.displayName().filter(expected::containsKey).isPresent()) {
                if(!expected.get(object.displayName().orElseThrow()).equals(memory.get(object.id())))throw new AssertionError(object.displayName()+" "+memory.get(object.id()));seen++;
            }
            if(seen!=expected.size())throw new AssertionError("all aliases checked");
        }
        var forged=fixture.deepCopy();
        for(var s:forged.path("statements"))if(s.path("source").path("kind").asText().equals("FIGURATIVE_LOW"))
            ((ObjectNode)s.path("source")).set("logicalValue",json.readTree("{\"logicalDomain\":\"TEXT\",\"value\":\"x\",\"logicalExtent\":1}"));
        if(decoder.decode(json.writeValueAsBytes(forged)) instanceof SpJsonDecoder.Decoded)throw new AssertionError("forged concrete character admitted");
        System.out.println("FIGURATIVE_FILL=PASS: arbitrary scalar assignments, neighbor preservation, aliases, open precision and hostile certificate");
    }
    static String eval(Expression e,Map<ObjectId,String> memory,Map<UncertaintyId,String> choices) {
        if(e instanceof Expressions.Unknown u)return Objects.requireNonNull(choices.get(u.reason()));
        if(e instanceof Expressions.Literal l&&l.value() instanceof Values.TextValue t)return t.value();
        if(e instanceof Expressions.Read r&&r.place() instanceof Places.ObjectPlace p)return Objects.requireNonNull(memory.get(p.object()));
        if(e instanceof Expressions.FillText f)return eval(f.character(),memory,choices).repeat(f.length().intValueExact());
        if(e instanceof Expressions.FitText f) {
            var value=eval(f.value(),memory,choices);int size=value.codePointCount(0,value.length()),n=f.length().intValueExact();
            return size>n?value.substring(0,value.offsetByCodePoints(0,n)):value+f.pad().repeat(n-size);
        }
        if(e instanceof Expressions.SliceText s) {
            var text=eval(s.value(),memory,choices);int start=((Values.IntValue)((Expressions.Literal)s.start()).value()).value().intValueExact();
            int count=((Values.IntValue)((Expressions.Literal)s.count()).value()).value().intValueExact();
            return text.substring(text.offsetByCodePoints(0,start),text.offsetByCodePoints(0,start+count));
        }
        if(e instanceof Expressions.Binary b&&b.operator()==Expressions.BinaryOperator.CONCAT)return eval(b.left(),memory,choices)+eval(b.right(),memory,choices);
        throw new AssertionError(e);
    }
}
