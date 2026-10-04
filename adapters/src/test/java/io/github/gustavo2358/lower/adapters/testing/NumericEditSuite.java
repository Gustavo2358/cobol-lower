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
/** Execute the produced AIR against handwritten IBM numeric-editing results. */
public final class NumericEditSuite {
    private static final ObjectMapper JSON=new ObjectMapper();
    private static final SpJsonDecoder DECODER=new SpJsonDecoder(CobolLower.INPUT_LIMITS);
    public static void main(String[] args) throws Exception {
        var fixture=(ObjectNode)JSON.readTree(NumericEditSuite.class.getResourceAsStream("/sp/numeric-edit/checkpoint.json"));
        var decoded=DECODER.decode(JSON.writeValueAsBytes(fixture));
        if(!(decoded instanceof SpJsonDecoder.Decoded ok))throw new AssertionError(decoded);
        var input=ok.input();var result=new CobolLowerer().lower(input,CobolLower.POSITIVE_OPTIONS);
        var publication=result.publication().orElseThrow(()->new AssertionError(result.status()+" "+result.admission().diagnostics()));
        var codec=new AirJson();if(!publication.equals(codec.decode(codec.encode(publication))))throw new AssertionError("editing roundtrip");
        var memory=new HashMap<ObjectId,Object>();var values=new ArrayList<String>();
        for(var statement:input.statements())if(statement instanceof SpInput.MoveFact) {
            var labels=new HashSet<LabelId>();result.statements().stream().filter(l->l.source().equals(statement.header().id())).forEach(l->labels.add(l.label()));
            for(var u:publication.units())for(var sequence:u.sequences())if(labels.contains(sequence.label()))for(var operation:sequence.instructions()) {
                if(!(operation instanceof Operations.Assign assign)||!(assign.destination() instanceof Places.ObjectPlace target))throw new AssertionError(operation);
                var value=eval(assign.value(),memory);memory.put(target.object(),value);values.add(value.toString());
            }
        }
        var expected=List.of("-1234.56","-  1,234.56","-     12.34","   23","  -23","       ","-     12.34 ");
        if(!values.equals(expected))throw new AssertionError(values);
        reject(fixture,t->editing(t).put("scale",3));
        reject(fixture,t->editing(t).put("digits",1));
        reject(fixture,t->((ObjectNode)editing(t).path("parts").get(0)).put("count",2));
        reject(fixture,t->((ObjectNode)editing(t).path("parts").get(0)).put("kind","PICTURE"));
        reject(fixture,t->t.put("contractVersion","2.65.0"));
        var familyBytes=NumericEditSuite.class.getResourceAsStream("/sp/numeric-edit/family.json").readAllBytes();
        var family=(SpJsonDecoder.Decoded)DECODER.decode(familyBytes);var lowered=new CobolLowerer().lower(family.input(),CobolLower.POSITIVE_OPTIONS);
        var familyPublication=lowered.publication().orElseThrow(()->new AssertionError(lowered.admission().diagnostics()));
        var familyMemory=new HashMap<ObjectId,Object>();
        for(var statement:family.input().statements())if(statement instanceof SpInput.MoveFact) {
            var labels=new HashSet<LabelId>();lowered.statements().stream().filter(l->l.source().equals(statement.header().id())).forEach(l->labels.add(l.label()));
            for(var u:familyPublication.units())for(var sequence:u.sequences())if(labels.contains(sequence.label()))for(var operation:sequence.instructions()) {
                if(!(operation instanceof Operations.Assign assign)||!(assign.destination() instanceof Places.ObjectPlace target))throw new AssertionError(operation);
                familyMemory.put(target.object(),eval(assign.value(),familyMemory));
            }
        }
        var expectedFamily=Map.of("RECORD-A","L- 12.34R","BEFORE-A","L","EDIT-A","- 12.34","AFTER-A","R","OUTPUT-A","L- 12.34R");
        for(var u:familyPublication.units())for(var object:u.objects())if(object.displayName().filter(expectedFamily::containsKey).isPresent()) {
            var expectedValue=expectedFamily.get(object.displayName().orElseThrow());
            if(!expectedValue.equals(familyMemory.get(object.id())))throw new AssertionError(object.displayName()+" "+familyMemory.get(object.id()));
        }
        compound();
        System.out.println("NUMERIC_EDIT=PASS: independent AIR execution, fixed/floating sign, zero suppression, decimal scale, text copy, hostile formats");
    }
    private static void compound() throws Exception {
        var fixture=(ObjectNode)JSON.readTree(NumericEditSuite.class.getResourceAsStream("/sp/numeric-edit/compound.json"));
        var decoded=(SpJsonDecoder.Decoded)DECODER.decode(JSON.writeValueAsBytes(fixture));
        var result=new CobolLowerer().lower(decoded.input(),CobolLower.POSITIVE_OPTIONS);
        var publication=result.publication().orElseThrow(()->new AssertionError(result.admission().diagnostics()));
        var memory=new HashMap<ObjectId,Object>();
        for(var statement:decoded.input().statements())if(statement instanceof SpInput.MoveFact) {
            var labels=new HashSet<LabelId>();result.statements().stream().filter(l->l.source().equals(statement.header().id())).forEach(l->labels.add(l.label()));
            for(var unit:publication.units())for(var seq:unit.sequences())if(labels.contains(seq.label()))for(var op:seq.instructions()) {
                if(!(op instanceof Operations.Assign a)||!(a.destination() instanceof Places.ObjectPlace p))throw new AssertionError(op);
                memory.put(p.object(),eval(a.value(),memory));
            }
        }
        var expected=Map.of("SOURCE-N","-23","TARGET-N","23","TARGET-X","0023  ","RECORD-A","L-0023R","PREFIX-A","L","EDIT-A","-0023","SUFFIX-A","R","OUTPUT-A","L-0023R");
        int checked=0;
        for(var unit:publication.units())for(var object:unit.objects())if(object.displayName().filter(expected::containsKey).isPresent()) {
            var name=object.displayName().orElseThrow();
            if(!expected.get(name).equals(Objects.toString(memory.get(object.id()))))throw new AssertionError(name+" "+memory.get(object.id()));checked++;
        }
        if(checked!=expected.size())throw new AssertionError("compound receiver omitted");
        reject(fixture,t->((ObjectNode)t.path("statements").get(4).path("numericTransfers").get(0)).put("value","23"));
        reject(fixture,t->((ObjectNode)t.path("dataDeclarations").get(0).path("scalarNumber")).put("scale",1));
    }
    private static ObjectNode editing(ObjectNode root) {
        for(var d:root.path("dataDeclarations"))if(d.path("canonicalName").asText().equals("EDIT-A"))return (ObjectNode)d.path("scalarEdit");
        throw new AssertionError("missing edit declaration");
    }
    private static void reject(ObjectNode fixture,Consumer<ObjectNode> mutate) throws Exception {
        var forged=fixture.deepCopy();mutate.accept(forged);var d=DECODER.decode(JSON.writeValueAsBytes(forged));
        if(d instanceof SpJsonDecoder.Decoded ok&&new CobolLowerer().lower(ok.input(),CobolLower.POSITIVE_OPTIONS).publication().isPresent())throw new AssertionError("forged editing descriptor admitted");
    }
    static Object eval(Expression e,Map<ObjectId,Object> memory) {
        if(e instanceof Expressions.Literal l)return switch(l.value()) {
            case Values.TextValue v -> v.value();case Values.IntValue v -> new BigDecimal(v.value());
            case Values.DecimalValue v -> new BigDecimal(v.coefficient(),v.scale().intValueExact());
            default -> throw new AssertionError(l.value());
        };
        if(e instanceof Expressions.Read r&&r.place() instanceof Places.ObjectPlace p)return Objects.requireNonNull(memory.get(p.object()));
        if(e instanceof Expressions.Unary u) {
            var value=(BigDecimal)eval(u.argument(),memory);
            return switch(u.operator()) {case TO_DECIMAL -> value;case TO_INT -> new BigDecimal(value.toBigInteger());case ABS -> value.abs();default -> throw new AssertionError(u);};
        }
        if(e instanceof Expressions.IntegerDigits d) {
            int width=d.digits().intValueExact();var digits=((BigDecimal)eval(d.value(),memory)).toBigInteger().abs().remainder(BigInteger.TEN.pow(width)).toString();
            return "0".repeat(width-digits.length())+digits;
        }
        if(e instanceof Expressions.FitDecimal f) {
            var value=(BigDecimal)eval(f.value(),memory);if(f.absolute())value=value.abs();
            return value.setScale(f.scale().intValueExact(),RoundingMode.DOWN).remainder(BigDecimal.ONE.scaleByPowerOfTen(f.digits().subtract(f.scale()).intValueExact()));
        }
        if(e instanceof Expressions.FitText f) {
            String value=(String)eval(f.value(),memory);int n=f.length().intValueExact();return value.length()>n?value.substring(0,n):value+f.pad().repeat(n-value.length());
        }
        if(e instanceof Expressions.Binary b&&b.operator()==Expressions.BinaryOperator.CONCAT)return (String)eval(b.left(),memory)+(String)eval(b.right(),memory);
        if(e instanceof Expressions.SliceText slice) {
            String value=(String)eval(slice.value(),memory);int start=((BigDecimal)eval(slice.start(),memory)).intValueExact(),count=((BigDecimal)eval(slice.count(),memory)).intValueExact();
            return value.substring(start,start+count);
        }
        if(e instanceof Expressions.FormatDecimal f)return format((BigDecimal)eval(f.value(),memory),f.parts());
        throw new AssertionError(e);
    }
    private static String format(BigDecimal number,List<DecimalText.Part> parts) {
        // Small test interpreter. No implementation or published constant is used as the expected value.
        int precision=0,scale=0;boolean point=false,mandatory=false;var kinds=new ArrayList<DecimalText.Kind>();var positive=new ArrayList<String>();var negative=new ArrayList<String>();
        for(var p:parts)for(int i=0;i<p.count().intValueExact();i++) {
            kinds.add(p.kind());positive.add(p.text());negative.add(p.negative());
            if(p.kind()==DecimalText.Kind.RADIX)point=true;
            if(Set.of(DecimalText.Kind.DIGITS,DecimalText.Kind.SUPPRESS_SPACE,DecimalText.Kind.SUPPRESS_STAR).contains(p.kind())){precision++;if(point)scale++;}
            mandatory|=p.kind()==DecimalText.Kind.DIGITS;
        }
        var coefficient=number.movePointRight(scale).toBigInteger().abs().remainder(BigInteger.TEN.pow(precision));
        String digits=coefficient.toString();digits="0".repeat(precision-digits.length())+digits;
        if(!mandatory&&coefficient.signum()==0)return " ".repeat(kinds.size());
        StringBuilder output=new StringBuilder();boolean significant=false,started=false;int at=0,floatSlot=-1;String floatSign=null;
        for(int i=0;i<kinds.size();i++) {
            var kind=kinds.get(i);
            if(kind==DecimalText.Kind.SIGN){output.append(number.signum()<0?negative.get(i):positive.get(i));continue;}
            if(kind==DecimalText.Kind.FLOAT_SIGN){floatSlot=output.length();floatSign=number.signum()<0?negative.get(i):positive.get(i);output.append(' ');started=true;continue;}
            if(kind==DecimalText.Kind.INSERT){output.append(started&&!significant?" ":positive.get(i));continue;}
            char character;
            if(kind==DecimalText.Kind.RADIX){character=positive.get(i).charAt(0);significant=true;}
            else {character=digits.charAt(at++);started=true;significant|=kind==DecimalText.Kind.DIGITS||character!='0';if(!significant)character=' ';}
            if(significant&&floatSlot>=0){output.setCharAt(output.length()-1,floatSign.charAt(0));floatSlot=-1;}
            output.append(character);
        }
        return output.toString();
    }
}
