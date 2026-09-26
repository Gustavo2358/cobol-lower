package io.github.gustavo2358.lower.adapters.testing;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.node.*;
import io.github.gustavo2358.lower.adapters.sp.SpJsonDecoder;
import io.github.gustavo2358.lower.adapters.cli.CobolLower;
import io.github.gustavo2358.lower.application.*;
import io.github.gustavo2358.lower.domain.*;
import java.util.*;
import static io.github.gustavo2358.lower.domain.SpInput.*;

/** Producer compatibility bytes and explicitly separate adversarial wire mutations. */
public final class CicsConsumerContractSuite {
    private static final ObjectMapper JSON=new ObjectMapper();
    private static int checks,mutations;
    private CicsConsumerContractSuite() { }
    private static void need(boolean condition,String reason) {checks++;if(!condition)throw new AssertionError(reason);}
    private static ObjectNode fixture(String name)throws Exception {
        try(var stream=CicsConsumerContractSuite.class.getResourceAsStream("/sp/cics-r7-r3/"+name+".json")) {
            if(stream==null)throw new AssertionError(name);return (ObjectNode)JSON.readTree(stream);
        }
    }
    private static SpJsonDecoder.Result result(JsonNode tree)throws Exception {
        return new SpJsonDecoder(CobolLower.INPUT_LIMITS).decode(JSON.writeValueAsBytes(tree));
    }
    private static SpInput accept(JsonNode tree)throws Exception {
        var r=result(tree);need(r instanceof SpJsonDecoder.Decoded,"valid typed decode: "+r);
        return ((SpJsonDecoder.Decoded)r).input();
    }
    private static void reject(JsonNode tree,SpJsonDecoder.Code code,String reason)throws Exception {
        var r=result(tree);need(r instanceof SpJsonDecoder.Rejected x&&x.diagnostic().code()==code,reason+": "+r);
    }
    private static ObjectNode fact(ObjectNode tree,String variant) {
        for(var s:tree.path("statements"))if(s.path("variant").asText().equals(variant))return (ObjectNode)s;
        throw new AssertionError(variant);
    }
    private static CicsHandlerFact handler(SpInput input) {return input.statements().stream().filter(CicsHandlerFact.class::isInstance).map(CicsHandlerFact.class::cast).findFirst().orElseThrow();}
    private static CicsAbendFact event(SpInput input) {return input.statements().stream().filter(CicsAbendFact.class::isInstance).map(CicsAbendFact.class::cast).findFirst().orElseThrow();}
    public static void main(String[] args)throws Exception {
        checks=0;mutations=0;compatibility();versionMatrix();wireMutations();contractNegatives();
        for(int n:List.of(1,2,5,40))composition(n);
        System.out.println("R7_R3_CONTRACT_CHECKS="+checks+" WIRE_MUTATIONS="+mutations+"/11");
    }
    public static SpInput composition(int n)throws Exception {
        var input=accept(fixture("many-"+n));
        need(input.statements().stream().filter(CicsHandlerFact.class::isInstance).count()==n,"handler multiplicity by identity");
        need(input.statements().stream().filter(CicsAbendFact.class::isInstance).count()==n,"event multiplicity by identity");
        var result=new CobolLowerer().lower(input,CobolLower.OPTIONS);
        need(result.status()==LoweringResult.Status.IMPLEMENTATION_LIMIT&&result.publication().isEmpty(),"multiplicity preserves NOT_READY without AIR");
        return input;
    }
    private static void compatibility()throws Exception {
        for(var name:List.of("label","unresolved","literal","data","cancel","reset","abend","abend-cancel","abcode","unsupported","unavailable-handler","combined","copy")) {
            var tree=fixture(name);var input=accept(tree);
            need(input.equals(accept(tree)),"deterministic typed snapshot "+name);
            need(input.factDependencies().orElseThrow().equals(JSON.treeToValue(tree.path("factDependencies"),FactDependencies.class)),"FactDependencies unchanged");
            need(input.controlTopology().orElseThrow().equals(JSON.treeToValue(tree.path("controlTopology"),ControlTopology.class)),"ControlTopology unchanged");
            need(input.sourceDependencies().equals(JSON.treeToValue(tree.path("sourceDependencies"),SourceFacts.Inventory.class)),"source dependencies unchanged");
            var out=new CobolLowerer().lower(input,CobolLower.OPTIONS);
            need(out.status()==LoweringResult.Status.IMPLEMENTATION_LIMIT,"explicit executable NOT_READY: "+name+" "+out);
            need(out.publication().isEmpty(),"no AIR, opaque op or handler edge");
            need(out.admission().input().filter(input::equals).isPresent(),"typed facts survive executable boundary");
            var compilation=new SpCompilation(InventoryStatus.COMPLETE,List.of(input.unit()),List.of(new SpCompilation.UnitProduct(input,Optional.empty(),
                input.dataDeclarations().stream().map(DataFact::id).toList(),List.of(),List.of(),List.of())));
            var composed=new CompilationLowerer().lower(compilation,CobolLower.OPTIONS);
            need(composed.status()==LoweringResult.Status.IMPLEMENTATION_LIMIT&&composed.publication().isEmpty(),"compilation boundary also NOT_READY without identity/AIR synthesis");

            for(var s:input.statements()) {
                if(s instanceof CicsHandlerFact h) {
                    need(h.executableLowering()==ExecutableLowering.NOT_READY,"handler disposition");
                    var wire=fact(tree,"CICS_HANDLER");
                    need(h.rawText().equals(wire.path("rawText").asText())&&h.gapCodes().size()==wire.path("gapCodes").size(),"raw/gaps retained");
                    need(h.scope().provenance().equals(JSON.treeToValue(wire.path("scope").path("provenance"),Provenance.class)),"scope provenance parity");
                    if(h.targetOrigin().isPresent())need(h.targetOrigin().orElseThrow().equals(JSON.treeToValue(wire.path("targetOrigin"),Provenance.class)),"wire target provenance parity");
                    if(h.entryOrigin().isPresent())need(h.entryOrigin().orElseThrow().equals(JSON.treeToValue(wire.path("entryOrigin"),Provenance.class)),"wire entry provenance parity");
                    if(h.labelTarget().isPresent())need(h.labelTarget().orElseThrow().declarationOrigin().equals(JSON.treeToValue(wire.path("labelTarget").path("declarationOrigin"),Provenance.class)),"wire declaration provenance parity");
                    for(int i=0;i<h.options().size();i++) {
                        var option=h.options().get(i);var w=wire.path("options").get(i);
                        need(option.name().equals(w.path("name").asText())&&option.start()==w.path("start").asInt()&&option.end()==w.path("end").asInt()
                            &&option.operand().equals(w.path("operand").isNull()?Optional.empty():Optional.of(w.path("operand").asText())),"option order/offset/operand parity");
                    }
                    need(h.scope().runtimeIdentity()==Availability.UNAVAILABLE,"logical level not source unit identity");
                    if(h.programTarget().isPresent())need(h.targetOrigin().orElseThrow().equals(h.programTarget().orElseThrow().provenance()),"operand provenance parity");
                    if(h.action()==CicsHandlerAction.CANCEL||h.action()==CicsHandlerAction.RESET||h.action()==CicsHandlerAction.UNAVAILABLE)
                        need(h.targetOrigin().isEmpty()&&h.targetSyntax().isEmpty()&&h.programTarget().isEmpty()&&h.labelTarget().isEmpty(),"no invented target");
                }
                if(s instanceof CicsAbendFact e) {
                    need(e.executableLowering()==ExecutableLowering.NOT_READY,"event disposition");
                    need(e.options().size()==fact(tree,"CICS_ABEND").path("options").size(),"ordered options retained");
                    need(e.rawText().equals(fact(tree,"CICS_ABEND").path("rawText").asText()),"raw event retained");
                }
            }
        }
        var label=handler(accept(fixture("label")));need(label.labelBindingStatus().orElseThrow()==ResolutionStatus.RESOLVED&&label.targetEntry().isPresent(),"resolved identity");
        var missing=handler(accept(fixture("unresolved")));need(missing.labelTarget().isEmpty()&&missing.targetOrigin().isPresent(),"unresolved syntax retains origin without destination");
        need(handler(accept(fixture("literal"))).programTarget().orElseThrow() instanceof LiteralCallTarget,"typed literal");
        need(handler(accept(fixture("data"))).programTarget().orElseThrow() instanceof DataCallTarget,"typed data");
        need(event(accept(fixture("abend-cancel"))).dispatchEligibility()==CicsAbendEligibility.HANDLERS_BYPASSED,"CANCEL eligibility");
        need(event(accept(fixture("unsupported"))).dispatchEligibility()==CicsAbendEligibility.UNAVAILABLE,"unsupported cause survives");
        need(!handler(accept(fixture("copy"))).targetOrigin().orElseThrow().includeChain().isEmpty(),"COPY provenance survives");
        need(!handler(accept(fixture("literal"))).header().id().equals(handler(accept(fixture("data"))).header().id()),"unit identity distinct");
        // Foreign-unit identity is representable through the memory port, never repaired by names.
        try {new CicsHandlerFact(label.header(),label.handlerKind(),label.action(),label.targetKind(),label.targetSyntax(),label.labelBindingStatus(),label.labelTarget(),
            Optional.of(new StatementId(new UnitKey("foreign",List.of(0),"foreign"),label.targetEntry().orElseThrow().handle())),label.entryOrigin(),label.targetOrigin(),label.programTarget(),label.scope(),label.rawText(),label.options(),label.gapCodes());throw new AssertionError("foreign memory entry accepted");}
        catch(IllegalArgumentException expected){need(true,"foreign entry rejected in memory");}
    }
    private static void versionMatrix()throws Exception {
        ObjectNode old;try(var stream=CicsConsumerContractSuite.class.getResourceAsStream("/sp/fact-dependency-r2/mixed-profile-absent.json")){old=(ObjectNode)JSON.readTree(stream);}
        need(old.path("contractVersion").asText().equals("2.40.0"),"historical 2.40 fixture unchanged");accept(old);
        var t=fixture("literal");need(t.path("contractVersion").asText().equals("2.41.0"),"real 2.41 handler");accept(t);
        t=fixture("literal");t.put("contractVersion","2.40.0");reject(t,SpJsonDecoder.Code.INPUT_ERROR,"2.40 handler prohibited");
        t=fixture("abend");t.put("contractVersion","2.40.0");reject(t,SpJsonDecoder.Code.INPUT_ERROR,"2.40 event prohibited");
        t=fixture("abend");t.put("contractVersion","2.41.0");reject(t,SpJsonDecoder.Code.INPUT_ERROR,"2.41 event prohibited");
        accept(fixture("combined"));accept(fixture("abend"));accept(fixture("label"));
        t=fixture("label");t.put("contractVersion","2.41.0");reject(t,SpJsonDecoder.Code.INPUT_ERROR,"2.41 ordinary proof prohibited");
        for(var version:List.of("2.48.0","2.43","9.99","unknown-text"))for(var source:List.of("combined","literal","unsupported")) {
            t=fixture(source);t.put("contractVersion",version);reject(t,SpJsonDecoder.Code.UNSUPPORTED_CONTRACT,"unknown version wins before feature shapes");
            t.set("factDependencies",JSON.createArrayNode());t.set("controlTopology",JSON.createArrayNode());t.set("statements",JSON.createArrayNode().add(JSON.createObjectNode().put("variant","CICS_ABEND").put("target",true)));
            reject(t,SpJsonDecoder.Code.UNSUPPORTED_CONTRACT,"unknown version wins over malformed facts");
        }
        for(String version:List.of("1.1.0","2.28.0","2.39.0")) {
            t=fixture("literal");t.put("contractVersion",version);reject(t,SpJsonDecoder.Code.INPUT_ERROR,"no old handler acceptance");
        }
    }
    private static void wireMutations()throws Exception {
        for(int mutation=1;mutation<=11;mutation++) {
            var t=fixture(mutation==2?"literal":mutation==7?"cancel":mutation==8||mutation==9?"label":"combined");
            switch(mutation) {
                case 1 -> t.put("contractVersion","2.41.0");
                case 2 -> t.put("contractVersion","2.40.0");
                case 3 -> t.remove("factDependencies");
                case 4 -> t.remove("controlTopology");
                case 5 -> fact(t,"CICS_ABEND").put("dispatchEligibility","HANDLERS_BYPASSED");
                case 6 -> fact(t,"CICS_ABEND").put("target","statement:2");
                case 7 -> fact(t,"CICS_HANDLER").set("targetOrigin",fact(t,"CICS_HANDLER").path("header").path("provenance"));
                case 8 -> fact(t,"CICS_HANDLER").put("targetEntry","foreign-unit/statement:2");
                case 9 -> {
                    var h=fact(t,"CICS_HANDLER");
                    for(var outcome:t.path("controlTopology").path("outcomes"))if(outcome.path("statement").asText().equals(h.path("header").path("id").asText())&&outcome.path("kind").asText().equals("NORMAL"))
                        ((ObjectNode)outcome.path("target")).put("reference",h.path("targetEntry").asText());
                    // Consumer never reconstructs the canonical next statement from handler semantics.
                    // The producer fixture's independent source oracle requires statement:1, not handler statement:2.
                    var r=result(t);if(r instanceof SpJsonDecoder.Decoded d)need(!ordinaryIsNext(d.input()),"M9 canonical-successor oracle detects forged target");
                    mutations++;continue;
                }
                case 10 -> t.put("contractVersion","2.48.0");
                case 11 -> {t.put("contractVersion","9.99");fact(t,"CICS_ABEND").put("dispatchEligibility","BROKEN").put("target",true);}
                default -> throw new AssertionError();
            }
            reject(t,mutation>=10?SpJsonDecoder.Code.UNSUPPORTED_CONTRACT:SpJsonDecoder.Code.INPUT_ERROR,"M"+mutation);mutations++;
        }
    }
    private static boolean ordinaryIsNext(SpInput input) {
        return input.controlTopology().orElseThrow().outcomes().stream().anyMatch(o->o.statement().equals("statement:0")&&o.kind()==ControlTopology.OutcomeKind.NORMAL&&o.target().reference().equals("statement:1"));
    }
    private static void contractNegatives()throws Exception {
        for(var change:List.of("missing-origin","literal-origin","data-origin","label-binding","program-label","reset-origin","event-gap","unavailable-cause","duplicate","offset","binding","cancel-eligible")) {
            var t=fixture(change.equals("data-origin")?"data":change.equals("reset-origin")?"reset":change.equals("label-binding")?"label":change.startsWith("event")||Set.of("unavailable-cause","duplicate","offset","binding","cancel-eligible").contains(change)?"abend-cancel":"literal");
            switch(change) {
                case "missing-origin" -> fact(t,"CICS_HANDLER").putNull("targetOrigin");
                case "literal-origin","data-origin","reset-origin" -> fact(t,"CICS_HANDLER").set("targetOrigin",fact(t,"CICS_HANDLER").path("header").path("provenance"));
                case "label-binding" -> fact(t,"CICS_HANDLER").put("labelBindingStatus","UNRESOLVED");
                case "program-label" -> fact(t,"CICS_HANDLER").put("labelBindingStatus","RESOLVED");
                case "event-gap" -> ((ArrayNode)fact(t,"CICS_ABEND").path("gapCodes")).add("SYNTAX_GAP");
                case "unavailable-cause" -> fact(t,"CICS_ABEND").put("dispatchEligibility","UNAVAILABLE");
                case "duplicate" -> {var e=fact(t,"CICS_ABEND");var a=(ArrayNode)e.path("options");var o=a.get(0).deepCopy();((ObjectNode)o).put("start",a.get(0).path("end").asInt()).put("end",e.path("rawText").asText().length());a.add(o);}
                case "offset" -> ((ObjectNode)fact(t,"CICS_ABEND").path("options").get(0)).put("start",-1);
                case "binding" -> ((ObjectNode)fact(t,"CICS_ABEND").path("options").get(0)).set("reference",fact(fixture("data"),"CICS_HANDLER").path("programTarget").path("reference"));
                case "cancel-eligible" -> fact(t,"CICS_ABEND").put("dispatchEligibility","HANDLER_ELIGIBLE");
                default -> throw new AssertionError();
            }
            reject(t,SpJsonDecoder.Code.INPUT_ERROR,change);
        }
    }
}
