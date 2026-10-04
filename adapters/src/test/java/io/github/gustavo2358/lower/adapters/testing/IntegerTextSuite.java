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
public final class IntegerTextSuite {
    private static final ObjectMapper JSON=new ObjectMapper();
    private static final SpJsonDecoder DECODER=new SpJsonDecoder(CobolLower.INPUT_LIMITS);
    public static void main(String[] args) throws Exception {
        var fixture=(ObjectNode)JSON.readTree(IntegerTextSuite.class.getResourceAsStream("/sp/integer-text/checkpoint.json"));
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
        if(!values.equals(List.of("-23","00023   ","000","2300","2300    ","12","0012    ","12","00012   ","-7621","07621   ","00052   ","005     ","0       ","-52")))throw new AssertionError(values);
        reject(fixture,t->((ObjectNode)t.path("statements").get(11).path("source").path("logicalValue")).put("value","00053"));
        reject(fixture,t->t.put("contractVersion","2.65.0"));
        reject(fixture,t->((ObjectNode)t.path("dataDeclarations").get(0).path("scalarNumber")).put("scale",1));
        reject(fixture,t->((ObjectNode)t.path("dataDeclarations").get(0).path("scalarNumber")).put("representation","UNKNOWN"));
        reject(fixture,t->((ObjectNode)t.path("statements").get(1).path("source").path("reference")).putNull("wholeItemAccess"));
        reject(fixture,t->((ObjectNode)t.path("statements").get(1).path("target")).putNull("wholeItemAccess"));
        System.out.println("INTEGER_TEXT=PASS: picture width, sign removal, scaling positions, receiver fitting, hostile proofs");
    }
    private static String eval(Expression expression,Map<ObjectId,String> memory) {
        if(expression instanceof Expressions.Literal l&&l.value() instanceof Values.TextValue text)return text.value();
        if(expression instanceof Expressions.Literal l&&l.value() instanceof Values.IntValue i)return i.value().toString();
        if(expression instanceof Expressions.Literal l&&l.value() instanceof Values.DecimalValue d)return new java.math.BigDecimal(d.coefficient(),d.scale().intValueExact()).toPlainString();
        if(expression instanceof Expressions.Unary u&&u.operator()==Expressions.UnaryOperator.TO_INT)return new java.math.BigDecimal(eval(u.argument(),memory)).toBigInteger().toString();
        if(expression instanceof Expressions.IntegerDigits digits) {
            var number=new java.math.BigInteger(eval(digits.value(),memory)).abs().toString();int n=digits.digits().intValueExact();
            return number.length()>n?number.substring(number.length()-n):"0".repeat(n-number.length())+number;
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
