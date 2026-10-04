package io.github.gustavo2358.lower.adapters.testing;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.node.*;
import io.github.gustavo2358.lower.adapters.sp.SpJsonDecoder;
import io.github.gustavo2358.lower.adapters.cli.CobolLower;
import io.github.gustavo2358.lower.application.*;
import io.github.gustavo2358.lower.domain.SpInput;
import io.github.gustavo2358.air.model.*;
import io.github.gustavo2358.air.model.Ids.*;
import io.github.gustavo2358.air.json.AirJson;
import java.util.*;
import java.util.function.Consumer;
public final class MoveTextFitSuite {
    private static final ObjectMapper JSON=new ObjectMapper();
    private static final SpJsonDecoder DECODER=new SpJsonDecoder(CobolLower.INPUT_LIMITS);
    public static void main(String[] args) throws Exception {
        var fixture=(ObjectNode)JSON.readTree(MoveTextFitSuite.class.getResourceAsStream("/sp/text-fit/checkpoint.json"));
        var decoded=DECODER.decode(JSON.writeValueAsBytes(fixture));
        if(!(decoded instanceof SpJsonDecoder.Decoded ok))throw new AssertionError(decoded);
        var input=ok.input();var result=new CobolLowerer().lower(input,CobolLower.POSITIVE_OPTIONS);
        var p=result.publication().orElseThrow(()->new AssertionError(result.status()+" "+result.admission().diagnostics()));
        if(!result.validation().orElseThrow().isStructurallyValid())throw new AssertionError(result.validation());
        var codec=new AirJson();if(!p.equals(codec.decode(codec.encode(p))))throw new AssertionError("codec");
        var memory=new HashMap<ObjectId,String>();var values=new ArrayList<String>();
        for(var statement:input.statements())if(statement instanceof SpInput.MoveFact) {
            var labels=new HashSet<LabelId>();result.statements().stream().filter(l->l.source().equals(statement.header().id())).forEach(l->labels.add(l.label()));
            for(var u:p.units())for(var q:u.sequences())if(labels.contains(q.label()))for(var instruction:q.instructions()) {
                if(!(instruction instanceof Operations.Assign a)||!(a.destination() instanceof Places.ObjectPlace target))throw new AssertionError("MOVE was not lowered precisely: "+instruction);
                var value=eval(a.value(),memory);memory.put(target.object(),value);values.add(value);
            }
        }
        if(!values.equals(List.of("ABCDEFGH","ABC","ABC     ","TOO")))throw new AssertionError(values);
        var mapped=fixture.deepCopy();var fit=mapped.path("statements").get(3);
        ((ObjectNode)fit.path("header").path("provenance")).put("exact",false);
        ((ObjectNode)fit.path("target").path("provenance")).put("exact",false);
        ((ObjectNode)fit.path("textAdjustment").path("provenance")).put("exact",false);
        var mappedInput=DECODER.decode(JSON.writeValueAsBytes(mapped));
        if(!(mappedInput instanceof SpJsonDecoder.Decoded mappedOk)
                ||new CobolLowerer().lower(mappedOk.input(),CobolLower.POSITIVE_OPTIONS).publication().isEmpty())
            throw new AssertionError("source-map precision erased a typed whole-item fitting proof");
        reject(fixture,t->t.put("contractVersion","2.65.0"));
        reject(fixture,t->((ObjectNode)t.path("statements").get(3).path("textAdjustment")).putObject("result").put("value","BAD"));
        reject(fixture,t->((ObjectNode)t.path("statements").get(3).path("textAdjustment")).put("receiverExtent",4));
        reject(fixture,t->((ObjectNode)t.path("statements").get(3).path("textAdjustment")).put("rule","RIGHT_PAD_SPACE"));
        reject(fixture,t->((ObjectNode)t.path("statements").get(1).path("source").path("reference")).putNull("wholeItemAccess"));
        reject(fixture,t->((ObjectNode)t.path("statements").get(1).path("target")).putNull("wholeItemAccess"));
        System.out.println("MOVE_TEXT_FIT=PASS: independent AIR execution, qualified names, padding, truncation, hostile proofs");
    }
    private static String eval(Expression expression,Map<ObjectId,String> memory) {
        if(expression instanceof Expressions.Literal l&&l.value() instanceof Values.TextValue text)return text.value();
        if(expression instanceof Expressions.Read r&&r.place() instanceof Places.ObjectPlace place)return Objects.requireNonNull(memory.get(place.object()),"read before write");
        if(expression instanceof Expressions.FitText fit) {
            var value=eval(fit.value(),memory);int n=fit.length().intValueExact();
            return value.length()>n?value.substring(0,n):value+fit.pad().repeat(n-value.length());
        }
        throw new AssertionError(expression);
    }
    private static void reject(ObjectNode fixture,Consumer<ObjectNode> mutate) throws Exception {
        var value=fixture.deepCopy();mutate.accept(value);var d=DECODER.decode(JSON.writeValueAsBytes(value));
        if(d instanceof SpJsonDecoder.Decoded ok&&new CobolLowerer().lower(ok.input(),CobolLower.POSITIVE_OPTIONS).publication().isPresent())throw new AssertionError("forged fitting proof admitted");
    }
}
