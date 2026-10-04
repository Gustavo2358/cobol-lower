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
import java.math.*;
import java.util.*;
import java.util.function.Consumer;
/** Independent AIR execution: expected values are handwritten, never read from SP certificates. */
public final class NumericConversionSuite {
    private static final ObjectMapper JSON=new ObjectMapper();
    private static final SpJsonDecoder DECODER=new SpJsonDecoder(CobolLower.INPUT_LIMITS);
    public static void main(String[] args) throws Exception {
        TextNumberChecks.run();
        SharedNumericChecks.run();
        UncertainTruncationChecks.run();
        var fixture=(ObjectNode)JSON.readTree(NumericConversionSuite.class.getResourceAsStream("/sp/numeric-conversion/checkpoint.json"));
        var decoded=DECODER.decode(JSON.writeValueAsBytes(fixture));
        if(!(decoded instanceof SpJsonDecoder.Decoded ok))throw new AssertionError(decoded);
        var input=ok.input();var result=new CobolLowerer().lower(input,CobolLower.POSITIVE_OPTIONS);
        var p=result.publication().orElseThrow(()->new AssertionError(result.status()+" "+result.admission().diagnostics()));
        if(!result.validation().orElseThrow().isStructurallyValid())throw new AssertionError(result.validation());
        var codec=new AirJson();if(!p.equals(codec.decode(codec.encode(p))))throw new AssertionError("numeric AIR codec");
        var memory=new HashMap<ObjectId,BigDecimal>();var values=new ArrayList<BigDecimal>();
        for(var statement:input.statements())if(statement instanceof SpInput.MoveFact) {
            var labels=new HashSet<LabelId>();result.statements().stream().filter(l->l.source().equals(statement.header().id())).forEach(l->labels.add(l.label()));
            for(var u:p.units())for(var q:u.sequences())if(labels.contains(q.label()))for(var instruction:q.instructions()) {
                if(!(instruction instanceof Operations.Assign a)||!(a.destination() instanceof Places.ObjectPlace target))throw new AssertionError("numeric MOVE lacks a precise assignment: "+instruction);
                var value=eval(a.value(),memory);memory.put(target.object(),value);values.add(value);
            }
        }
        var expected=List.of("-123.45","23","-123.4","-123","-7621","-621.0","1","1","2300","300.0","52.00","52");
        if(values.size()!=expected.size())throw new AssertionError(values);
        for(int n=0;n<expected.size();n++)if(values.get(n).compareTo(new BigDecimal(expected.get(n)))!=0)throw new AssertionError(n+": "+values);
        var coarse=fixture.deepCopy();
        for(var declaration:coarse.path("dataDeclarations"))((ObjectNode)declaration.path("provenance")).put("exact",false);
        var coarseDecoded=DECODER.decode(JSON.writeValueAsBytes(coarse));
        if(!(coarseDecoded instanceof SpJsonDecoder.Decoded coarseOk)
            ||new CobolLowerer().lower(coarseOk.input(),CobolLower.POSITIVE_OPTIONS).publication().isEmpty())
            throw new AssertionError("coarse declaration mapping invalidated a numeric value proof");
        reject(fixture,t->proof(t,10).put("value","52.01"));
        reject(fixture,t->{var source=(ObjectNode)t.path("statements").get(10).path("source");source.put("value","00A52");((ObjectNode)source.path("logicalValue")).put("value","00A52");});
        reject(fixture,t->proof(t,0).put("value","-123.459"));
        reject(fixture,t->proof(t,0).put("value","123.45"));
        reject(fixture,t->proof(t,4).put("value","3451"));
        reject(fixture,t->proof(t,4).put("value","123451"));
        reject(fixture,t->((ObjectNode)t.path("dataDeclarations").get(0).path("scalarNumber")).put("scale",32));
        reject(fixture,t->((ObjectNode)t.path("dataDeclarations").get(0).path("scalarNumber")).put("representation","UNKNOWN"));
        reject(fixture,t->((ObjectNode)t.path("dataDeclarations").get(0).path("scalarNumber")).putNull("representation"));
        reject(fixture,t->((ObjectNode)t.path("statements").get(1).path("source").path("reference")).putNull("wholeItemAccess"));
        for (var field : List.of("digits", "scale", "signed", "representation", "trunc"))
            reject(fixture, t -> ((ObjectNode)t.path("dataDeclarations").get(0).path("scalarNumber")).remove(field));
        reject(fixture,t->((ObjectNode)t.path("dataDeclarations").get(0).path("scalarNumber")).put("signed","false"));
        reject(fixture,t->((ObjectNode)t.path("dataDeclarations").get(0).path("scalarNumber")).put("digits",1.5));
        reject(fixture,t->((ObjectNode)t.path("dataDeclarations").get(0).path("scalarNumber")).put("trunc","BIN"));
        reject(fixture,t->t.put("contractVersion","2.65.0"));
        System.out.println("NUMERIC_CONVERSION=PASS: decimal sign/scale/truncation, binary capacity, independent AIR values, hostile certificates");
    }
    private static ObjectNode proof(ObjectNode root,int statement) {return (ObjectNode)root.path("statements").get(statement).path("numericTransfers").get(0);}
    private static void reject(ObjectNode fixture,Consumer<ObjectNode> edit) throws Exception {
        var copy=fixture.deepCopy();edit.accept(copy);var decoded=DECODER.decode(JSON.writeValueAsBytes(copy));
        if(decoded instanceof SpJsonDecoder.Decoded ok&&new CobolLowerer().lower(ok.input(),CobolLower.POSITIVE_OPTIONS).publication().isPresent())throw new AssertionError("forged numeric proof admitted");
    }
    static BigDecimal eval(Expression expression,Map<ObjectId,BigDecimal> memory) {
        if(expression instanceof Expressions.Literal l) {
            if(l.value() instanceof Values.IntValue i)return new BigDecimal(i.value());
            if(l.value() instanceof Values.DecimalValue d)return new BigDecimal(d.coefficient(),d.scale().intValueExact());
        }
        if(expression instanceof Expressions.Read r)return Objects.requireNonNull(memory.get(((Places.ObjectPlace)r.place()).object()),"read before write");
        if(expression instanceof Expressions.Unary u) {
            var v=eval(u.argument(),memory);
            return switch(u.operator()){case TO_DECIMAL -> v;case TO_INT -> new BigDecimal(v.toBigInteger());case ABS -> v.abs();default -> throw new AssertionError(u);};
        }
        if(expression instanceof Expressions.Binary b&&b.operator()==Expressions.BinaryOperator.MUL)return eval(b.left(),memory).multiply(eval(b.right(),memory));
        if(expression instanceof Expressions.FitDecimal f) {
            var v=eval(f.value(),memory);if(f.absolute())v=v.abs();
            return v.setScale(f.scale().intValueExact(),RoundingMode.DOWN)
                .remainder(BigDecimal.ONE.scaleByPowerOfTen(f.digits().subtract(f.scale()).intValueExact()));
        }
        if(expression instanceof Expressions.WrapInteger w) {
            var modulus=BigInteger.ONE.shiftLeft(w.width().intValueExact());var value=eval(w.value(),memory).toBigIntegerExact().mod(modulus);
            if(w.signed()&&value.compareTo(modulus.shiftRight(1))>=0)value=value.subtract(modulus);
            return new BigDecimal(value);
        }
        throw new AssertionError(expression);
    }
}
