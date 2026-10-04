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
public final class ZeroFillSuite {
    private static final ObjectMapper JSON=new ObjectMapper();
    private static final SpJsonDecoder DECODER=new SpJsonDecoder(CobolLower.INPUT_LIMITS);
    public static void main(String[] args) throws Exception {
        var fixture=(ObjectNode)JSON.readTree(ZeroFillSuite.class.getResourceAsStream("/sp/zero-fill/checkpoint.json"));
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
        if(!values.subList(0,3).equals(List.of("0000","0","0   "))
            ||values.size()!=6||!new HashSet<>(values.subList(3,6)).equals(Set.of("00000","00","000")))throw new AssertionError(values);
        reject(fixture,t->t.put("contractVersion","2.65.0"));
        reject(fixture,t->((ObjectNode)t.path("statements").get(0).path("source")).put("kind","ALPHANUMERIC"));
        reject(fixture,t->((ObjectNode)t.path("statements").get(0).path("source")).put("value","1"));
        reject(fixture,t->((ObjectNode)t.path("statements").get(0).path("textAdjustment")).put("rule","RIGHT_FIT_SPACE"));
        reject(fixture,t->((ObjectNode)t.path("statements").get(0).path("textAdjustment")).putObject("result").put("value","0   "));
        reject(fixture,t->((ObjectNode)t.path("statements").get(1).path("numericTransfers").get(0)).put("value","1"));
        var first=(SpInput.MoveFact)input.statements().getFirst();
        var literal=(SpInput.LiteralSource)first.source();
        var badLiteral=io.github.gustavo2358.lower.testing.IfInputs.with(literal,"numericValue",Optional.of(java.math.BigDecimal.ONE));
        var bad=io.github.gustavo2358.lower.testing.IfInputs.with(first,"source",badLiteral);
        var badInput=io.github.gustavo2358.lower.testing.IfInputs.with(input,"statements",input.statements().stream().map(s->s==first?bad:s).toList());
        if(new CobolLowerer().lower(badInput,CobolLower.POSITIVE_OPTIONS).publication().isPresent())throw new AssertionError("forged in-memory figurative admitted");
        System.out.println("ZERO_FILL=PASS: receiver categories, shared group projection, independent AIR values, hostile wire and memory");
    }
    private static String eval(Expression expression,Map<ObjectId,String> memory) {
        if(expression instanceof Expressions.Literal l&&l.value() instanceof Values.TextValue text)return text.value();
        if(expression instanceof Expressions.Literal l&&l.value() instanceof Values.IntValue i)return i.value().toString();
        if(expression instanceof Expressions.Binary b&&b.operator()==Expressions.BinaryOperator.CONCAT)return eval(b.left(),memory)+eval(b.right(),memory);
        if(expression instanceof Expressions.SliceText slice) {
            var value=eval(slice.value(),memory);int start=Integer.parseInt(eval(slice.start(),memory)),length=Integer.parseInt(eval(slice.count(),memory));
            return value.substring(start,start+length);
        }
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
