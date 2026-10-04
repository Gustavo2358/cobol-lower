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
import java.math.BigInteger;
import java.util.*;
import java.util.function.Consumer;
/** Independent assignment oracle plus hostile wire and in-memory certificates. */
public final class NumericMoveSuite {
    private static final ObjectMapper JSON=new ObjectMapper();
    private static final SpJsonDecoder DECODER=new SpJsonDecoder(CobolLower.INPUT_LIMITS);
    public static void main(String[] args) throws Exception {
        var fixture=(ObjectNode)JSON.readTree(NumericMoveSuite.class.getResourceAsStream("/sp/numeric-move/checkpoint.json"));
        var input=decode(fixture);var result=lower(input);
        var publication=result.publication().orElseThrow();
        if(!result.validation().orElseThrow().isStructurallyValid())throw new AssertionError(result.validation());
        var codec=new AirJson();if(!publication.equals(codec.decode(codec.encode(publication))))throw new AssertionError("AIR roundtrip");
        var objects=new HashMap<ObjectId,Memory.ObjectDeclaration>();publication.units().forEach(u->u.objects().forEach(o->objects.put(o.id(),o)));
        var memory=new HashMap<ObjectId,BigInteger>();var values=new ArrayList<BigInteger>();
        for(var statement:input.statements())if(statement instanceof SpInput.MoveFact) {
            var labels=new HashSet<LabelId>();result.statements().stream().filter(l->l.source().equals(statement.header().id())).forEach(l->labels.add(l.label()));
            for(var unit:publication.units())for(var seq:unit.sequences())if(labels.contains(seq.label()))for(var instruction:seq.instructions()) {
                if(!(instruction instanceof Operations.Assign a)||!(a.destination() instanceof Places.ObjectPlace p))throw new AssertionError("exact MOVE assignment missing: "+instruction);
                BigInteger value;
                if(a.value() instanceof Expressions.Literal l&&l.value() instanceof Values.IntValue i)value=i.value();
                else if(a.value() instanceof Expressions.Read r&&r.place() instanceof Places.ObjectPlace source)value=memory.get(source.object());
                else value=evaluate(a.value(),memory);
                if(value==null)throw new AssertionError("read before write");memory.put(p.object(),value);values.add(value);
                if(!objects.get(p.object()).typeRef().equals(Types.known(Types.Builtin.INT)))throw new AssertionError("numeric cell must have INT type");
            }
        }
        if(!values.equals(List.of(BigInteger.valueOf(12),BigInteger.valueOf(12),BigInteger.valueOf(12),BigInteger.ZERO,BigInteger.ZERO)))throw new AssertionError(values);
        reject(fixture,t->proof(t,0).put("value","13"));
        reject(fixture,t->proof(t,0).put("value","123"));
        reject(fixture,t->proof(t,0).put("value","-1"));
        reject(fixture,t->proof(t,0).putNull("value"));
        reject(fixture,t->proof(t,0).put("target","operand:999:1"));
        reject(fixture,t->((ArrayNode)move(t,0).path("numericTransfers")).add(proof(t,0).deepCopy()));
        reject(fixture,t->((ObjectNode)move(t,0).path("source")).put("kind","ALPHANUMERIC"));
        reject(fixture,t->((ObjectNode)move(t,0).path("source")).put("value","garbage"));
        reject(fixture,t->((ArrayNode)move(t,1).path("numericTransfers")).remove(1));
        reject(fixture,t->((ObjectNode)move(t,1).path("additionalTransfers").get(0).path("source").path("reference").path("wholeItemAccess")).put("data","data:99999"));
        reject(fixture,t->t.put("contractVersion","2.64.0"));
        reject(fixture,t->{t.put("contractVersion","2.64.0");for(var m:t.path("statements"))((ObjectNode)m).remove("numericTransfers");});
        reject(fixture,t->((ObjectNode)t.path("dataDeclarations").get(2).path("scalarNumber")).put("digits",0));
        reject(fixture,t->{for(var f:t.path("factDependencies").path("facts"))if(f.path("kind").asText().equals("LOGICAL_NUMBER"))((ObjectNode)f).put("kind","LOGICAL_TEXT");});
        var statements=new ArrayList<>(input.statements());var original=(SpInput.MoveFact)statements.getFirst();
        statements.set(0,new SpInput.MoveFact(original.header(),original.source(),original.target(),original.copySemantics(),original.normalContinuation(),original.textAdjustment(),original.regionalMove(),original.additionalTransfers(),original.logicalTransfers(),List.of(new SpInput.NumericTransfer(original.target().id(),Optional.of(java.math.BigDecimal.valueOf(13))))));
        var forged=new SpInput(input.unit(),input.policy(),input.dataDeclarations(),statements,input.structure(),input.gaps(),input.coverage(),input.entryInventory(),input.storageIndependence(),input.compositional(),input.storage(),input.fileInventory(),input.sourceDependencies(),input.ordinaryContinuations(),input.controlTopology(),input.factDependencies(),input.nominalValues(),input.conditionNames());
        if(new CobolLowerer().lower(forged,CobolLower.POSITIVE_OPTIONS).publication().isPresent())throw new AssertionError("forged in-memory value admitted");
        var mixed=(ObjectNode)JSON.readTree(NumericMoveSuite.class.getResourceAsStream("/sp/numeric-move/mixed.json"));
        var mixedInput=decode(mixed);var mixedResult=lower(mixedInput);
        if(!mixedResult.validation().orElseThrow().isStructurallyValid())throw new AssertionError(mixedResult.validation());
        var mixedMove=(SpInput.MoveFact)mixedInput.statements().get(1);
        if(mixedMove.numericTransfers().size()!=1)throw new AssertionError("only the first DATA receiver is a proved prefix");
        var mixedLabels=new HashSet<LabelId>();mixedResult.statements().stream().filter(l->l.source().equals(mixedMove.header().id())).forEach(l->mixedLabels.add(l.label()));
        long assigns=mixedResult.publication().orElseThrow().units().stream().flatMap(u->u.sequences().stream()).filter(q->mixedLabels.contains(q.label())).flatMap(q->q.instructions().stream()).filter(Operations.Assign.class::isInstance).count();
        if(assigns!=1)throw new AssertionError("later unknown writes must not become precise: "+assigns);
        reject(mixed,t->proof(t,1).put("target","operand:1:5"));
        System.out.println("NUMERIC_MOVE=PASS: INT oracle, DATA widening, ordered multi-target, ZERO, AIR codec, hostile wire and memory");
    }
    private static BigInteger evaluate(Expression expression,Map<ObjectId,BigInteger> memory) {
        return decimal(expression,memory).toBigIntegerExact();
    }
    private static java.math.BigDecimal decimal(Expression e,Map<ObjectId,BigInteger> memory) {
        if(e instanceof Expressions.Read r)return new java.math.BigDecimal(memory.get(((Places.ObjectPlace)r.place()).object()));
        if(e instanceof Expressions.Literal l&&l.value() instanceof Values.IntValue i)return new java.math.BigDecimal(i.value());
        if(e instanceof Expressions.Unary u) {var v=decimal(u.argument(),memory);return switch(u.operator()) {
            case TO_DECIMAL -> v; case TO_INT -> new java.math.BigDecimal(v.toBigInteger());case ABS -> v.abs();default -> throw new AssertionError(u);
        };}
        if(e instanceof Expressions.FitDecimal f) {
            var v=decimal(f.value(),memory);if(f.absolute())v=v.abs();
            return v.setScale(f.scale().intValueExact(),java.math.RoundingMode.DOWN)
                .remainder(java.math.BigDecimal.ONE.scaleByPowerOfTen(f.digits().subtract(f.scale()).intValueExact()));
        }
        throw new AssertionError(e);
    }
    private static ObjectNode move(ObjectNode t,int i){return (ObjectNode)t.path("statements").get(i);}
    private static ObjectNode proof(ObjectNode t,int i){return (ObjectNode)move(t,i).path("numericTransfers").get(0);}
    private static SpInput decode(ObjectNode t) throws Exception {var d=DECODER.decode(JSON.writeValueAsBytes(t));if(!(d instanceof SpJsonDecoder.Decoded ok))throw new AssertionError(d);return ok.input();}
    private static LoweringResult lower(SpInput input){var r=new CobolLowerer().lower(input,CobolLower.POSITIVE_OPTIONS);if(r.publication().isEmpty())throw new AssertionError(r.status()+" "+r.admission().diagnostics());return r;}
    private static void reject(ObjectNode fixture,Consumer<ObjectNode> mutation) throws Exception {var t=fixture.deepCopy();mutation.accept(t);var d=DECODER.decode(JSON.writeValueAsBytes(t));if(d instanceof SpJsonDecoder.Decoded ok&&new CobolLowerer().lower(ok.input(),CobolLower.POSITIVE_OPTIONS).publication().isPresent())throw new AssertionError("forged integer certificate admitted");}
}
