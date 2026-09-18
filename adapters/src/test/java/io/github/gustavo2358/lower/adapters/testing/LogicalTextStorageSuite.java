package io.github.gustavo2358.lower.adapters.testing;

import io.github.gustavo2358.air.model.*;
import io.github.gustavo2358.lower.adapters.cli.CobolLower;
import io.github.gustavo2358.lower.adapters.sp.SpJsonDecoder;
import io.github.gustavo2358.lower.application.CobolLowerer;
import java.util.*;
import static io.github.gustavo2358.lower.testing.CallOracle.check;

/** Real frontend SP; literals and malformed coordinate oracles are independent. */
public final class LogicalTextStorageSuite {
    private LogicalTextStorageSuite() { }
    public static void main(String[] args) throws Exception {
        var bytes=Objects.requireNonNull(LogicalTextStorageSuite.class.getResourceAsStream("/sp/logical-text-w1/group-literal.json")).readAllBytes();
        var decoder=new SpJsonDecoder(CobolLower.INPUT_LIMITS);var decoded=decoder.decode(bytes);
        check(decoded instanceof SpJsonDecoder.Decoded,"SP2.29 logical contract admission");
        var input=((SpJsonDecoder.Decoded)decoded).input();
        check(input.storage().orElseThrow().runtimeCodec().isEmpty(),"no encoding selected");
        var p=RegionalTranslationSuite.lower(input).publication().orElseThrow();
        check(p.storage().stream().allMatch(Memory.Cell.class::isInstance),"logical AIR has no physical Region");
        var values=new HashSet<String>();
        for(var sequence:p.units().getFirst().sequences()) {
            var state=new HashMap<io.github.gustavo2358.air.model.Ids.ObjectId,String>();
            for(var instruction:sequence.instructions())if(instruction instanceof Operations.Assign assign) {
                var value=interpret(assign.value(),state);
                state.put(((Places.ObjectPlace)assign.destination()).object(),value);
                if(value!=null)values.add(value);
            }
        }
        check(values.equals(Set.of("PROGA   1234","1234","PROGA   ")),"root snapshot and child character projections");
        var json=new com.fasterxml.jackson.databind.ObjectMapper();
        for(var mutation:List.of("missing","negative","escape","root","omit-child","unknown-field","old-version")) {
            var tree=(com.fasterxml.jackson.databind.node.ObjectNode)json.readTree(bytes);var storage=(com.fasterxml.jackson.databind.node.ObjectNode)tree.path("storage");
            var views=(com.fasterxml.jackson.databind.node.ArrayNode)storage.path("logicalTextViews");var leaf=(com.fasterxml.jackson.databind.node.ObjectNode)views.get(1);
            switch(mutation) {
                case "missing"->storage.remove("logicalTextViews");case "negative"->leaf.put("start","-1");
                case "escape"->leaf.put("length","100");case "root"->leaf.put("root","absent");
                case "omit-child"->views.remove(1);case "unknown-field"->leaf.put("codec","IBM1047");
                case "old-version"->tree.put("contractVersion","2.28.0");
                default->throw new AssertionError(mutation);
            }
            var result=decoder.decode(json.writeValueAsBytes(tree));
            check(result instanceof SpJsonDecoder.Rejected||new CobolLowerer().lower(((SpJsonDecoder.Decoded)result).input(),CobolLower.OPTIONS).publication().isEmpty(),"reject malformed logical inventory: "+mutation);
        }
        System.out.println("LOGICAL_TEXT_STORAGE=PASS noPhysicalProfile roundtrip=true negativeCases=7");
    }
    private static String interpret(Expression expression,Map<io.github.gustavo2358.air.model.Ids.ObjectId,String> state) {
        if(expression instanceof Expressions.Literal l)return ((Values.TextValue)l.value()).value();
        if(expression instanceof Expressions.Read r)return state.get(((Places.ObjectPlace)r.place()).object());
        if(expression instanceof Expressions.FitText f) {
            var text=interpret(f.value(),state);if(text==null)return null;
            return (text+f.pad().repeat(f.length().intValueExact())).substring(0,f.length().intValueExact());
        }
        if(expression instanceof Expressions.SliceText s) {
            var text=interpret(s.value(),state);if(text==null)return null;
            int start=((Values.IntValue)((Expressions.Literal)s.start()).value()).value().intValueExact();
            int count=((Values.IntValue)((Expressions.Literal)s.count()).value()).value().intValueExact();return text.substring(start,start+count);
        }
        var b=(Expressions.Binary)expression;var left=interpret(b.left(),state);var right=interpret(b.right(),state);
        return left==null||right==null?null:left+right;
    }

}
