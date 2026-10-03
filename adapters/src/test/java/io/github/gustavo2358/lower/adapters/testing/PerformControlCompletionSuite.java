package io.github.gustavo2358.lower.adapters.testing;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.node.*;
import io.github.gustavo2358.air.json.AirJson;
import io.github.gustavo2358.air.model.*;
import io.github.gustavo2358.air.model.Ids.*;
import io.github.gustavo2358.lower.adapters.cli.CobolLower;
import io.github.gustavo2358.lower.adapters.sp.SpJsonDecoder;
import io.github.gustavo2358.lower.application.*;
import io.github.gustavo2358.lower.domain.SpInput;
import java.util.*;

/** Source fixtures have independent dependency oracles in analysis-cfg. These checks
 * inspect AIR ordering, local footprints, codec evidence and contract rejection. */
public final class PerformControlCompletionSuite {
    private static final ObjectMapper JSON=new ObjectMapper();
    private static void need(boolean b,String why){if(!b)throw new AssertionError(why);}
    private static byte[] raw(String name)throws Exception {
        try(var s=PerformControlCompletionSuite.class.getResourceAsStream("/sp/perform-completion/"+name+".json")) {
            if(s==null)throw new AssertionError(name);return s.readAllBytes();
        }
    }
    private static SpInput input(byte[] raw) {
        var result=new SpJsonDecoder(CobolLower.INPUT_LIMITS).decode(raw);
        need(result instanceof SpJsonDecoder.Decoded,"valid SP2.48: "+result);
        return ((SpJsonDecoder.Decoded)result).input();
    }
    public static void main(String[] args)throws Exception {
        for(var name:List.of("paragraph_thru_inline_escape","paragraph_goto_outside_active","inline_nearest_exit","inline_exit_outside",
                "section_empty","section_preamble","section_thru","varying_two_before","varying_three_after","varying_inline_exit","varying_nested")) {
            var source=input(raw(name));var result=new CobolLowerer().lower(source,CobolLower.OPTIONS);
            need(result.publication().isPresent(),name+" publishes AIR: "+result.status());
            var codec=new AirJson();var encoded=codec.encode(result.publication().orElseThrow());
            need(Arrays.equals(encoded,codec.encode(codec.decodeForPartialAnalysis(encoded).publication())),name+" codec preserves evidence");
            var tree=(ObjectNode)JSON.readTree(raw(name));reverse(tree.withArray("statements"));
            for(var key:List.of("occurrences","regions","boundaries","outcomes","bindings","proofs"))reverse(((ObjectNode)tree.get("controlTopology")).withArray(key));
            var peer=new CobolLowerer().lower(input(JSON.writeValueAsBytes(tree)),CobolLower.OPTIONS);
            need(result.publication().equals(peer.publication()),name+" inventory permutation is deterministic");
            for(var effect:result.publication().orElseThrow().units().getFirst().sequences().stream().map(Sequence::terminator)
                    .filter(Operations.Opaque.class::isInstance).map(Operations.Opaque.class::cast)
                    .filter(o->o.observedKind().startsWith("perform-varying-")).toList()) {
                var memory=effect.envelope().memory();
                need(memory.knownWrites().size()==1&&memory.knownWrites().equals(memory.mustOverwrite())
                    &&memory.otherWrites()==Scopes.NoMemory.INSTANCE&&memory.otherReads()==Scopes.NoMemory.INSTANCE,"one proved whole-item write");
                need(effect.envelope().control().known().size()==1&&effect.envelope().control().remainder()==Scopes.NoControl.INSTANCE,"one explicit phase successor");
            }
            if(name.equals("paragraph_goto_outside_active")) {
                var reached=LocalControlOracle.reached(result.publication().orElseThrow()).stream().map(LocalControlOracle.Point::label).collect(java.util.stream.Collectors.toSet());
                var call=source.statements().stream().filter(SpInput.CallFact.class::isInstance).findFirst().orElseThrow();
                need(result.statements().stream().anyMatch(l->l.source().equals(call.header().id())&&reached.contains(l.label())),"external GO TO then paragraph exit retains the active endpoint and reaches caller continuation");
            }
            if(name.equals("varying_two_before"))new Graph(source,result).before();
            if(name.equals("varying_three_after"))new Graph(source,result).after();
            tree.put("contractVersion","2.47.0");
            // The ignored EXIT outside an inline body uses the established transfer surface.
            if(!name.equals("inline_exit_outside"))need(new SpJsonDecoder(CobolLower.INPUT_LIMITS).decode(JSON.writeValueAsBytes(tree)) instanceof SpJsonDecoder.Rejected,"old contract cannot consume new semantics "+name);
        }
        for(var mutation:List.of("wrong-scope","outside-scope","wrong-outcome","negative-level","count-level","missing-level","excess-level","missing-predicate")) {
            boolean escape=mutation.contains("scope")||mutation.equals("wrong-outcome");
            var tree=(ObjectNode)JSON.readTree(raw(escape?"paragraph_thru_inline_escape":"varying_three_after"));
            var topology=tree.get("controlTopology");
            var outcome=java.util.stream.StreamSupport.stream(topology.path("outcomes").spliterator(),false)
                .filter(o->o.path("target").path("kind").asText().equals("ESCAPE")).findFirst();
            if(escape) {
                var e=(ObjectNode)outcome.orElseThrow();
                if(mutation.equals("wrong-outcome"))e.put("kind","NORMAL");
                else {var region=java.util.stream.StreamSupport.stream(topology.path("regions").spliterator(),false)
                    .filter(r->r.path("kind").asText().equals(mutation.equals("wrong-scope")?"PROCEDURE":"PARAGRAPH")
                        &&!r.path("id").asText().equals(e.path("target").path("reference").asText())).findFirst().orElseThrow();
                    ((ObjectNode)e.get("target")).put("reference",region.path("id").asText());}
            } else {
                var phase=(ObjectNode)topology.path("bindings").get(0).path("phases").get(0);
                switch(mutation) {
                    case "negative-level" -> phase.put("level",-1);
                    case "count-level" -> {phase.put("kind","PREDICATE");phase.put("operation","COUNT_ENTRY");}
                    case "missing-level" -> phase.remove("level");
                    case "excess-level" -> phase.put("level",99);
                    case "missing-predicate" -> {for(var s:tree.path("statements"))if(s.path("variant").asText().equals("PERFORM_PROCEDURE"))((ArrayNode)s.path("varying").path("afterLoops")).remove(0);}
                    default -> throw new AssertionError(mutation);
                }
            }
            var decoded=new SpJsonDecoder(CobolLower.INPUT_LIMITS).decode(JSON.writeValueAsBytes(tree));
            need(decoded instanceof SpJsonDecoder.Rejected||new CobolLowerer().lower(((SpJsonDecoder.Decoded)decoded).input(),CobolLower.OPTIONS).status()==LoweringResult.Status.INVALID_INPUT,"malformed contract rejected "+mutation);
        }
        var unavailable=(ObjectNode)JSON.readTree(raw("varying_three_after"));
        for(var statement:unavailable.path("statements"))if(statement.path("variant").asText().equals("PERFORM_PROCEDURE"))
            for(var control:statement.path("varying").path("controls"))if(control.path("role").asText().equals("BY")&&control.path("level").asInt()==2)((ObjectNode)control).putNull("integer");
        var partial=new CobolLowerer().lower(input(JSON.writeValueAsBytes(unavailable)),CobolLower.OPTIONS);
        need(partial.publication().isPresent()&&partial.publication().orElseThrow().uncertainties().stream()
            .anyMatch(u->u.code().equals("cobol-lower:PERFORM_VARYING_OPERANDS_UNAVAILABLE")),"unproved operands preserve explicit partial effects and unique operation identities: "+partial.status());
        System.out.println("PERFORM_CONTROL_COMPLETION=PASS fixtures=11 malformed=8");
    }
    private static void reverse(ArrayNode array){var values=new ArrayList<JsonNode>();array.forEach(values::add);Collections.reverse(values);array.removeAll();values.forEach(array::add);}
    private static final class Graph {
        final Map<LabelId,Terminator> terms=new HashMap<>();final Map<LabelId,Sequence> sequences=new HashMap<>();final Map<LabelId,LocalControlOracle.Point> points=new HashMap<>();final Map<String,ObjectId> objects=new HashMap<>();final LabelId start;
        Graph(SpInput input,LoweringResult result) {
            result.publication().orElseThrow().units().getFirst().sequences().forEach(s->{terms.put(s.label(),s.terminator());sequences.put(s.label(),s);});
            LocalControlOracle.reached(result.publication().orElseThrow()).forEach(point->points.putIfAbsent(point.label(),point));
            input.dataDeclarations().forEach(d->result.data().stream().filter(l->l.source().equals(d.id())).findFirst().ifPresent(l->objects.put(d.canonicalName(),l.object())));
            var perform=input.statements().stream().filter(SpInput.ProcedurePerformFact.class::isInstance).findFirst().orElseThrow();
            start=visible(result.statements().stream().filter(l->l.source().equals(perform.header().id())&&terms.get(l.label()) instanceof Operations.LocalInvoke).findFirst().orElseThrow().label());
        }
        LabelId visible(LabelId label) {
            var point=Objects.requireNonNull(points.get(label),"reachable phase "+label);var seen=new HashSet<LocalControlOracle.Point>();
            while(terms.get(point.label()) instanceof Operations.Jump||terms.get(point.label()) instanceof Operations.LocalInvoke
                    ||terms.get(point.label()) instanceof Operations.LocalResume||terms.get(point.label()) instanceof Operations.LocalUnwind) {
                need(seen.add(point),"administrative path terminates");
                var next=LocalControlOracle.successors(point,sequences);need(next.size()==1,"one administrative successor");point=next.getFirst();
            }
            return point.label();
        }
        LabelId effect(LabelId label,String kind,String write,String...reads) {
            need(terms.get(label) instanceof Operations.Opaque,"phase is effect "+kind+" "+write);
            var effect=(Operations.Opaque)terms.get(label);need(effect.observedKind().equals("perform-varying-"+kind),"phase kind "+kind);
            var writes=new HashSet<ObjectId>();var actualReads=new HashSet<ObjectId>();
            for(var operand:effect.knownOperands())if(operand instanceof Places.ObjectPlace place) {
                if(effect.envelope().memory().knownWrites().contains(place.header().id()))writes.add(place.object());
                if(effect.envelope().memory().knownReads().contains(place.header().id()))actualReads.add(place.object());
            }
            need(writes.equals(Set.of(objects.get(write))),"phase writes only "+write);
            need(actualReads.equals(Arrays.stream(reads).map(objects::get).collect(java.util.stream.Collectors.toSet())),"phase reads "+Arrays.toString(reads));
            return ((Control.JumpAlternative)effect.envelope().control().known().getFirst()).label();
        }
        Operations.Branch test(LabelId label){need(terms.get(label) instanceof Operations.Branch,"UNTIL test");return (Operations.Branch)terms.get(label);}
        LabelId bodyCompletion(LabelId body) {
            var call=visible(body);need(terms.get(call) instanceof Operations.Invoke,"body CALL retained");
            var completion=((Operations.Invoke)terms.get(call)).outcomes().known().stream().filter(Control.Normal.class::isInstance)
                .map(Control.Normal.class::cast).findFirst().orElseThrow().label();
            return visible(completion);
        }
        void before() {
            var initI=start;var initJ=effect(initI,"initialization","I");
            var testI=effect(initJ,"initialization","J","I");var outer=test(testI);var testJ=outer.falseDestination();var inner=test(testJ);
            var resetJ=effect(inner.trueDestination(),"increment","I","I");need(effect(resetJ,"initialization","J","I").equals(testI),"carry resets inner from current outer then tests outer");
            var increments=terms.entrySet().stream().filter(e->e.getValue() instanceof Operations.Opaque o&&o.observedKind().equals("perform-varying-increment")).toList();
            need(increments.size()==2,"two updates");var updateJ=increments.stream().filter(e->!e.getKey().equals(inner.trueDestination())).findFirst().orElseThrow().getKey();
            need(bodyCompletion(inner.falseDestination()).equals(updateJ),"BEFORE body completes into innermost update");
            need(effect(updateJ,"increment","J","J").equals(testJ),"inner update tests inner, not outer");
            need(terms.get(visible(outer.trueDestination())) instanceof Operations.Invoke,"only outer exhaustion reaches continuation CALL");
            need(!inner.falseDestination().equals(outer.trueDestination()),"body and continuation remain distinct");
        }
        void after() {
            var initI=start;var initJ=effect(initI,"initialization","I");var initK=effect(initJ,"initialization","J","I");var body=effect(initK,"initialization","K","J");
            need(terms.get(body) instanceof Operations.Jump||terms.get(body) instanceof Operations.LocalInvoke,"AFTER enters body without a predicate");
            var tests=terms.entrySet().stream().filter(e->e.getValue() instanceof Operations.Branch).toList();need(tests.size()==3,"three separate predicates");
            var outer=tests.stream().map(e->(Operations.Branch)e.getValue()).filter(b->terms.get(visible(b.trueDestination())) instanceof Operations.Invoke).findFirst().orElseThrow();
            need(effect(outer.falseDestination(),"increment","I","I").equals(initJ),"outer repeat reinitializes J then K");
            var outerLabel=tests.stream().filter(e->e.getValue()==outer).findFirst().orElseThrow().getKey();
            var middle=tests.stream().map(e->(Operations.Branch)e.getValue()).filter(b->b.trueDestination().equals(outerLabel)).findFirst().orElseThrow();
            need(effect(middle.falseDestination(),"increment","J","J").equals(initK),"middle repeat resets K");
            var middleLabel=tests.stream().filter(e->e.getValue()==middle).findFirst().orElseThrow().getKey();
            var inner=tests.stream().map(e->(Operations.Branch)e.getValue()).filter(b->b.trueDestination().equals(middleLabel)).findFirst().orElseThrow();
            var innerLabel=tests.stream().filter(e->e.getValue()==inner).findFirst().orElseThrow().getKey();
            need(bodyCompletion(body).equals(innerLabel),"AFTER body completes into innermost test before any update");
            need(effect(inner.falseDestination(),"increment","K","K").equals(body),"inner repeat returns to body");
        }
    }
}
