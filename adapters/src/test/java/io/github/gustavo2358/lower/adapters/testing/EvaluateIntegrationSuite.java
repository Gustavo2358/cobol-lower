package io.github.gustavo2358.lower.adapters.testing;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.node.*;
import io.github.gustavo2358.air.json.AirJson;
import io.github.gustavo2358.air.model.*;
import io.github.gustavo2358.lower.adapters.cli.CobolLower;
import io.github.gustavo2358.lower.adapters.sp.SpJsonDecoder;
import io.github.gustavo2358.lower.application.*;
import io.github.gustavo2358.lower.domain.SpInput;
import java.nio.file.*;
import java.util.*;
import static io.github.gustavo2358.lower.testing.CallOracle.check;

public final class EvaluateIntegrationSuite {
    private static final ObjectMapper JSON=new ObjectMapper();
    public static final List<String> NAMES=List.of("e1","e2","e3","e4","e5","strong","closed","unknown",
        "partial-body","three-arms","also","empty","perform","goback","compose-1","compose-2","compose-5","compose-40");
    private static byte[] fixture(String name) throws Exception {
        try(var in=EvaluateIntegrationSuite.class.getResourceAsStream("/sp/evaluate/"+name+".json")) {
            return Objects.requireNonNull(in,name).readAllBytes();
        }
    }
    private static SpInput decode(byte[] bytes) {
        var decoded=new SpJsonDecoder(CobolLower.INPUT_LIMITS).decode(bytes);
        check(decoded instanceof SpJsonDecoder.Decoded,"SP2 decoded: "+decoded);
        return ((SpJsonDecoder.Decoded)decoded).input();
    }
    private static LoweringResult lower(SpInput input) { return PartialIntegrationSuite.lower(input); }
    public static void main(String[] args) throws Exception { run(); }
    public static void run() throws Exception {
        var codec=new AirJson(); var out=Path.of("target/evaluate"); Files.createDirectories(out);
        for(var name:NAMES) {
            var raw=fixture(name);var input=decode(raw);var r=lower(input);var p=r.publication().orElseThrow();var unit=p.units().getFirst();
            var sequences=new HashMap<io.github.gustavo2358.air.model.Ids.LabelId,Sequence>(); unit.sequences().forEach(s->sequences.put(s.label(),s));
            check(new HashSet<>(input.statements().stream().map(s->s.header().id()).toList()).equals(
                new HashSet<>(r.statements().stream().map(LoweringResult.StatementLink::source).toList())),"no silent elision "+name);
            check(unit.sequences().stream().filter(s->s.terminator() instanceof Operations.Invoke).count()
                == input.statements().stream().filter(SpInput.CallFact.class::isInstance).count(),"one dependency site per source CALL "+name);
            for(var fact:input.statements()) if(fact instanceof SpInput.EvaluateFact e) {
                var links=r.statements().stream().filter(l->l.source().equals(e.header().id())).toList();
                if(name.equals("empty")) { check(links.size()==1 && sequences.get(links.getFirst().label()).terminator() instanceof Operations.Opaque,"empty arm retains open control"); continue; }
                check(links.size()==e.arms().size(),"one branch per ordered WHEN "+name);
                for(int i=0;i<links.size();i++) {
                    var op=(Operations.Branch)sequences.get(links.get(i).label()).terminator();
                    var member=e.arms().get(i).control().entry().statement().orElseThrow();
                    var target=r.statements().stream().filter(l->l.source().equals(member)).findFirst().orElseThrow().label();
                    check(op.trueDestination().equals(target),"WHEN edge names its own entry "+name);
                    var falseTarget=i+1<links.size()?links.get(i+1).label():r.statements().stream().filter(l->l.source().equals(
                        e.otherArm().entry().statement().orElseGet(()->e.normalContinuation().statement().orElseThrow()))).findFirst().orElseThrow().label();
                    check(op.falseDestination().equals(falseTarget),"false chain ends at OTHER or explicit no-match continuation "+name);
                    check(p.origins().stream().anyMatch(o->o.id().equals(op.header().origin()) && o instanceof Origins.Derived d && d.inputs().size()>=4),"choice provenance includes control evidence");
                }
            }
            if(name.startsWith("compose-"))check(input.statements().stream().filter(SpInput.EvaluateFact.class::isInstance).count()==Integer.parseInt(name.substring(8)),"multiplicity "+name);
            if(name.equals("perform"))check(unit.sequences().stream().flatMap(s->s.instructions().stream()).filter(Operations.Assign.class::isInstance).count()==2,"BASIC body retains precise MOVE under EVALUATE");
            var bytes=codec.encode(p); check(Arrays.equals(bytes,codec.encode(codec.decode(bytes))),"AIR canonical round-trip "+name);
            var reordered=(ObjectNode)JSON.readTree(raw); reverseArray((ArrayNode)reordered.get("statements"));
            check(Arrays.equals(bytes,codec.encode(lower(decode(JSON.writeValueAsBytes(reverseFields(reordered)))).publication().orElseThrow())),"physical object/statement order cannot determine control "+name);
            Files.write(out.resolve(name+".air.json"),bytes);
        }
        var raw=(ObjectNode)JSON.readTree(fixture("e1"));
        var eval=(ObjectNode)java.util.stream.StreamSupport.stream(raw.get("statements").spliterator(),false).filter(s->s.path("variant").asText().equals("EVALUATE")).findFirst().orElseThrow();
        var bad=raw.deepCopy(); var id=eval.path("header").path("id").asText();
        var badEval=(ObjectNode)java.util.stream.StreamSupport.stream(bad.get("statements").spliterator(),false).filter(s->s.path("header").path("id").asText().equals(id)).findFirst().orElseThrow();
        ((ObjectNode)badEval.withArray("arms").get(0)).put("ordinal",1);
        var rejected=new CobolLowerer().lower(decode(JSON.writeValueAsBytes(bad)),CobolLower.OPTIONS);
        check(rejected.status()==LoweringResult.Status.INVALID_INPUT,"contradictory WHEN order rejected");
        eval.put("futureField",true);
        check(new SpJsonDecoder(CobolLower.INPUT_LIMITS).decode(JSON.writeValueAsBytes(raw)) instanceof SpJsonDecoder.Rejected,"SP2 closed wire rejects unknown field");
        System.out.println("LOWER_EVALUATE_FIXTURES="+NAMES.size());
    }
    private static void reverseArray(ArrayNode array) { var nodes=new ArrayList<JsonNode>(); array.forEach(nodes::add); Collections.reverse(nodes);array.removeAll();nodes.forEach(array::add); }
    private static JsonNode reverseFields(JsonNode node) {
        if(node.isObject()) { var result=JSON.createObjectNode();var fields=new ArrayList<String>();node.fieldNames().forEachRemaining(fields::add);Collections.reverse(fields);for(var f:fields)result.set(f,reverseFields(node.get(f)));return result; }
        if(node.isArray()) { var result=JSON.createArrayNode();node.forEach(n->result.add(reverseFields(n)));return result; } return node;
    }
}
