package io.github.gustavo2358.lower.adapters.testing;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.node.*;
import io.github.gustavo2358.lower.adapters.sp.SpJsonDecoder;
import io.github.gustavo2358.lower.adapters.cli.CobolLower;
import io.github.gustavo2358.lower.application.*;
import io.github.gustavo2358.air.model.*;
import io.github.gustavo2358.air.model.Ids.*;
import java.util.*;
import java.math.*;
/** Execute guard/assign/control with an independent small interpreter. */
final class TextNumberChecks {
    static void run() throws Exception {
        var json=new ObjectMapper();var decoder=new SpJsonDecoder(CobolLower.INPUT_LIMITS);
        var fixture=(ObjectNode)json.readTree(TextNumberChecks.class.getResourceAsStream("/sp/numeric-move/text-number.json"));
        for(var input:List.of("00052"," 0052","A0052","-0052","5.200","     ")) {
            var tree=fixture.deepCopy();var literal=(ObjectNode)tree.path("statements").get(0).path("source");literal.put("value",input);
            ((ObjectNode)literal.path("logicalValue")).put("value",input);
            var decoded=decoder.decode(json.writeValueAsBytes(tree));if(!(decoded instanceof SpJsonDecoder.Decoded ok))throw new AssertionError(decoded);
            var result=new CobolLowerer().lower(ok.input(),CobolLower.POSITIVE_OPTIONS);
            var publication=result.publication().orElseThrow(()->new AssertionError(result.admission().diagnostics()));
            if(!result.validation().orElseThrow().isStructurallyValid())throw new AssertionError(result.validation());
            var unit=publication.units().getFirst();var sequences=new HashMap<LabelId,Sequence>();unit.sequences().forEach(s->sequences.put(s.label(),s));
            var memory=new HashMap<ObjectId,Object>();var at=unit.entries().getFirst().initialLabel().orElseThrow();boolean failed=false,finished=false;int steps=0;
            while(!finished&&steps++<50) {
                var sequence=Objects.requireNonNull(sequences.get(at));
                for(var instruction:sequence.instructions()) {
                    if(!(instruction instanceof Operations.Assign a)||!(a.destination() instanceof Places.ObjectPlace p))throw new AssertionError(instruction);
                    memory.put(p.object(),eval(a.value(),memory));
                }
                var term=sequence.terminator();
                if(term instanceof Operations.Jump j)at=j.destination();
                else if(term instanceof Operations.Branch b)at=(Boolean)eval(b.predicate(),memory)?b.trueDestination():b.falseDestination();
                else if(term instanceof Operations.Return)finished=true;
                else if(term instanceof Operations.Opaque o) {
                    if(!o.observedKind().equals("invalid-numeric-text")||!(o.envelope().memory().otherWrites() instanceof Scopes.WithinMemory)
                        ||o.envelope().dependencies().remainder()!=Scopes.AnyResource.INSTANCE||o.envelope().control().known().isEmpty()
                        ||!(o.envelope().control().remainder() instanceof Scopes.WithinControl))throw new AssertionError("invalid input silently closed");
                    failed=true;finished=true;
                } else throw new AssertionError(term);
            }
            if(!finished||failed==input.equals("00052"))throw new AssertionError("wrong conversion branch: "+input);
            for(var object:unit.objects())if(object.displayName().filter(n->n.equals("TARGET-N")||n.equals("TARGET-P")).isPresent()) {
                if(failed&&memory.containsKey(object.id()))throw new AssertionError("invalid text received an invented number");
                if(!failed&&((BigDecimal)memory.get(object.id())).compareTo(new BigDecimal("52"))!=0)throw new AssertionError(memory);
            }
            long parsers=publication.units().stream().flatMap(u->u.sequences().stream()).flatMap(s->s.instructions().stream()).filter(Operations.Assign.class::isInstance).map(Operations.Assign.class::cast).filter(a->containsParse(a.value())).count();
            if(parsers!=2)throw new AssertionError("numeric receiver omitted");
        }
        var forged=fixture.deepCopy();((ObjectNode)forged.path("statements").get(1).path("numericTransfers").get(0)).put("value","0");
        var decoded=decoder.decode(json.writeValueAsBytes(forged));
        if(decoded instanceof SpJsonDecoder.Decoded ok&&new CobolLowerer().lower(ok.input(),CobolLower.POSITIVE_OPTIONS).publication().isPresent())throw new AssertionError("DATA cannot claim constant zero");
        System.out.println("TEXT_NUMBER=PASS: valid digits, invalid data branch, explicit unknown fallback, all receivers, hostile constant");
    }
    private static boolean containsParse(Expression expression) {
        var pending=new ArrayDeque<Operand>();pending.push(expression);boolean found=false;
        while(!pending.isEmpty()) {var operand=pending.pop();if(operand instanceof Expressions.ParseInteger p){found=true;if(!(p.onInvalid() instanceof Expressions.Unknown))throw new AssertionError("implicit numeric default");}pending.addAll(Operands.children(operand));}
        return found;
    }
    private static Object eval(Expression expression,Map<ObjectId,Object> memory) {
        if(expression instanceof Expressions.Unary u&&u.operator()==Expressions.UnaryOperator.IS_DIGITS) {
            String value=(String)eval(u.argument(),memory);return !value.isEmpty()&&value.chars().allMatch(c->c>='0'&&c<='9');
        }
        if(expression instanceof Expressions.ParseInteger p) {
            String value=(String)eval(p.value(),memory);
            if(value.isEmpty()||value.chars().anyMatch(c->c<'0'||c>'9'))throw new AssertionError("invalid input reached numeric path");
            return new BigDecimal(value);
        }
        if(expression instanceof Expressions.Unary u) {
            var value=(BigDecimal)eval(u.argument(),memory);return switch(u.operator()) {
                case TO_DECIMAL -> value;case TO_INT -> new BigDecimal(value.toBigInteger());case ABS -> value.abs();default -> throw new AssertionError(u);
            };
        }
        if(expression instanceof Expressions.FitDecimal f) {
            var value=(BigDecimal)eval(f.value(),memory);if(f.absolute())value=value.abs();
            return value.setScale(f.scale().intValueExact(),RoundingMode.DOWN).remainder(BigDecimal.ONE.scaleByPowerOfTen(f.digits().subtract(f.scale()).intValueExact()));
        }
        return NumericEditSuite.eval(expression,memory);
    }
}
