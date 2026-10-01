package io.github.gustavo2358.lower.adapters.testing;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.node.*;
import io.github.gustavo2358.air.json.AirJson;
import io.github.gustavo2358.lower.adapters.sp.SpJsonDecoder;
import io.github.gustavo2358.lower.adapters.cli.CobolLower;
import io.github.gustavo2358.lower.application.*;
import io.github.gustavo2358.lower.domain.*;
import java.util.*;
import java.util.function.Consumer;

/** Independent success-path oracle. IDs are joined by their complete published identity. */
public final class FileCompositeFlowSuite {
    private static final ObjectMapper JSON=new ObjectMapper();
    private static int checks;
    private static void need(boolean ok,String reason){checks++;if(!ok)throw new AssertionError(reason);}
    private static ObjectNode raw(String name)throws Exception {
        try(var in=FileCompositeFlowSuite.class.getResourceAsStream("/sp/file-composite/"+name+".json")) {
            if(in==null)throw new AssertionError(name);return (ObjectNode)JSON.readTree(in);
        }
    }
    private static SpInput decode(JsonNode tree)throws Exception {
        var result=new SpJsonDecoder(CobolLower.INPUT_LIMITS).decode(JSON.writeValueAsBytes(tree));
        need(result instanceof SpJsonDecoder.Decoded,"valid composite SP: "+result);return ((SpJsonDecoder.Decoded)result).input();
    }
    private static LoweringResult lower(SpInput source) {
        var result=new CobolLowerer().lower(source,CobolLower.POSITIVE_OPTIONS);
        need(result.publication().isPresent(),"composite publishes AIR: "+result);return result;
    }
    static Set<JsonNode> labels(JsonNode node) {
        var result=new HashSet<JsonNode>();
        if(node.isObject()&&node.path("domain").asText().equals("label"))result.add(node);
        else if(node.isContainerNode())node.forEach(n->result.addAll(labels(n)));
        return result;
    }
    private static class Graph {
        final Map<JsonNode,JsonNode> terms=new HashMap<>();final Set<JsonNode> critical=new HashSet<>();final JsonNode start;
        Graph(LoweringResult result)throws Exception {
            var tree=JSON.readTree(new AirJson().encode(result.publication().orElseThrow())).path("publication");
            var gaps=new HashSet<JsonNode>();for(var gap:tree.path("uncertainties"))if(gap.path("code").asText().equals("FILE_CRITICAL_ERROR_EXIT_NOT_PROVEN"))gaps.add(gap.path("id"));
            var unit=tree.path("units").get(0);start=unit.path("entries").get(0).path("initialLabel");
            for(var sequence:unit.path("sequences")) {
                var label=sequence.path("label");var term=sequence.path("terminator");terms.put(label,term);
                for(var gap:term.path("header").path("uncertainties"))if(gaps.contains(gap)) {
                    critical.add(label);need(!term.path("envelope").path("control").path("remainder").path("kind").asText().equals("none"),"critical exit remains open");
                }
            }
        }
        record Flow(JsonNode label,List<JsonNode> returns) {Flow{returns=List.copyOf(returns);}}
        List<Flow> successors(Flow point) {
            var term=terms.get(point.label());var stack=point.returns();
            if(term.path("kind").asText().equals("local.invoke")) {
                var pushed=new ArrayList<>(stack);pushed.add(term.path("resume"));return List.of(new Flow(term.path("entry"),pushed));
            }
            if(term.path("kind").asText().equals("local.resume")) {
                need(!stack.isEmpty(),"no unmatched body return on the ordinary fixture path");
                return List.of(new Flow(stack.getLast(),stack.subList(0,stack.size()-1)));
            }
            need(!term.path("kind").asText().startsWith("local."),"oracle explicitly covers producer local operations");
            return labels(term).stream().map(to->new Flow(to,stack)).toList();
        }
        Set<JsonNode> reached(JsonNode entry,Set<JsonNode> forbidden) {
            var seen=new HashSet<Flow>();var todo=new ArrayDeque<Flow>();todo.add(new Flow(entry,List.of()));
            while(!todo.isEmpty()) {var at=todo.removeFirst();if(forbidden.contains(at.label())||critical.contains(at.label())||!seen.add(at))continue;todo.addAll(successors(at));}
            return seen.stream().map(Flow::label).collect(java.util.stream.Collectors.toSet());
        }
        String call(JsonNode at) {
            var term=terms.get(at);if(!term.path("kind").asText().equals("invoke"))return "";
            return term.path("action").asText()+" "+term.path("target").path("name").asText();
        }
        void sequence(List<String> expected) {
            record State(Flow point,int index) {JsonNode label(){return point.label();}}
            var todo=new ArrayDeque<State>();todo.add(new State(new Flow(start,List.of()),0));var seen=new HashSet<State>();int exits=0;
            while(!todo.isEmpty()) {
                var s=todo.removeFirst();if(critical.contains(s.label())||!seen.add(s))continue;var term=terms.get(s.label());int index=s.index();
                var action=call(s.label());if(!action.isEmpty()) {need(index<expected.size()&&expected.get(index).equals(action),"success trace: expected "+expected+" index="+index+" got="+action);index++;}
                if(term.path("kind").asText().equals("return")){need(index==expected.size(),"no early return/bypass");exits++;}
                for(var to:successors(s.point()))todo.addLast(new State(to,index));
            }
            need(exits>0,"ordinary completion exists");
        }
    }
    private static JsonNode normalized(LoweringResult result)throws Exception {
        var publication=result.publication().orElseThrow();
        return JSON.readTree(new String(new AirJson().encode(publication),java.nio.charset.StandardCharsets.UTF_8)
            .replace(publication.id().localId(),"PUBLICATION_NAMESPACE"));
    }
    private static void permutation(ObjectNode tree) {
        reverse((ArrayNode)tree.path("statements"));var t=tree.path("controlTopology");
        for(var key:List.of("occurrences","regions","boundaries","outcomes","bindings","proofs","fileFlows"))reverse((ArrayNode)t.path(key));
        for(var flow:t.path("fileFlows"))reverse((ArrayNode)flow.path("points"));
        reverse((ArrayNode)tree.path("fileInventory").path("operations").path("uses"));
    }
    private static void reverse(ArrayNode a){var list=new ArrayList<JsonNode>();a.forEach(list::add);Collections.reverse(list);a.removeAll();list.forEach(a::add);}
    private static SpInput withTopology(SpInput s,ControlTopology t) {
        return new SpInput(s.unit(),s.policy(),s.dataDeclarations(),s.statements(),s.structure(),s.gaps(),s.coverage(),s.entryInventory(),s.storageIndependence(),s.compositional(),s.storage(),s.fileInventory(),s.sourceDependencies(),s.ordinaryContinuations(),Optional.of(t),s.factDependencies(),s.nominalValues());
    }
    private static void reject(ObjectNode source,Consumer<ObjectNode> mutate,String why)throws Exception {
        var tree=source.deepCopy();mutate.accept(tree);
        var decoded=new SpJsonDecoder(CobolLower.INPUT_LIMITS).decode(JSON.writeValueAsBytes(tree));
        boolean rejected=decoded instanceof SpJsonDecoder.Rejected;
        if(decoded instanceof SpJsonDecoder.Decoded d)rejected=new CobolLowerer().lower(d.input(),CobolLower.POSITIVE_OPTIONS).status()==LoweringResult.Status.INVALID_INPUT;
        need(rejected,"wire rejects "+why);
        if(why.equals("old version")||why.equals("missing ordinal"))return;
        try {
            var topology=JSON.treeToValue(tree.path("controlTopology"),ControlTopology.class);
            var result=new CobolLowerer().lower(withTopology(decode(source),topology),CobolLower.POSITIVE_OPTIONS);
            need(result.status()==LoweringResult.Status.INVALID_INPUT,"typed rejects "+why);
        } catch(com.fasterxml.jackson.core.JsonProcessingException|IllegalArgumentException expected) {checks++;}
    }
    private static void rejectionMatrix(ObjectNode source)throws Exception {
        reject(source,t->{var f=t.path("controlTopology").path("fileFlows").get(0);((ObjectNode)f.path("points").get(0)).set("targets",f.path("points").get(1).path("targets").deepCopy());},"success agreement");
        reject(source,t->t.put("contractVersion","2.50.0"),"old version");
        reject(source,t->((ObjectNode)t.path("controlTopology")).remove("fileFlows"),"missing flows");
        reject(source,t->((ArrayNode)t.path("controlTopology").path("fileFlows")).remove(1),"missing one flow");
        reject(source,t->{var f=t.path("controlTopology").path("fileFlows");((ObjectNode)f.get(0).path("entry")).put("reference",f.get(1).path("entry").path("reference").asText());},"foreign entry");
        reject(source,t->{var p=(ArrayNode)t.path("controlTopology").path("fileFlows").get(0).path("points");p.add(p.get(0).deepCopy());},"duplicate point");
        reject(source,t->((ArrayNode)t.path("controlTopology").path("fileFlows").get(0).path("points")).remove(1),"missing point");
        reject(source,t->((ObjectNode)t.path("controlTopology").path("fileFlows").get(0).path("points").get(0)).put("ordinal",999),"unowned use");
        reject(source,t->((ObjectNode)t.path("controlTopology").path("fileFlows").get(0).path("points").get(0)).remove("ordinal"),"missing ordinal");
        reject(source,t->{var f=t.path("controlTopology").path("fileFlows");((ObjectNode)f.get(0).path("points").get(0).path("targets").get(0)).put("reference",f.get(1).path("entry").path("reference").asText());},"foreign successor");
    }
    public static void main(String[] args)throws Exception {
        checks=0;
        var pair=List.of("open CLIENTDD","open OTHERDD","close CLIENTDD","close OTHERDD");
        for(var name:List.of("original","three","perform-twice","status","sort-multi","sort-output","merge-multi","use-error","no-event-model")) {
            var tree=raw(name);var source=decode(tree);var result=lower(source);var g=new Graph(result);
            if(!name.equals("no-event-model"))need(!g.critical.isEmpty(),"error remainder retained");
            if(name.equals("no-event-model")) {
                g.sequence(List.of("open INA","open OUTB","close INA","close OUTB"));
                var wrong=tree.deepCopy();((ObjectNode)wrong.path("fileInventory").path("operations").path("uses").get(0).path("control")).put("continuation","statement:2");
                var invalid=decode(wrong);need(new CobolLowerer().lower(invalid,CobolLower.POSITIVE_OPTIONS).status()==LoweringResult.Status.INVALID_INPUT,"unavailable dispatch cannot assert a contradictory continuation");
            }
            else if(name.equals("three"))g.sequence(List.of("open CLIENTDD","open OTHERDD","open THIRDDD","close CLIENTDD","close OTHERDD","close THIRDDD"));
            else if(name.equals("perform-twice")){var expected=new ArrayList<>(pair);expected.add("call BETWEEN");expected.addAll(pair);expected.add("call AFTER");g.sequence(expected);}
            else if(!name.contains("sort")&&!name.contains("merge"))g.sequence(pair);
            else {
                var reached=g.reached(g.start,Set.of());
                long participants=g.terms.keySet().stream().filter(at->g.call(at).startsWith(name.startsWith("merge")?"merge ":"sort ")).peek(at->need(reached.contains(at),"all aggregate participants reachable")).count();
                need(participants==3,"three external participants; SD is local");
                need(result.publication().orElseThrow().uncertainties().stream().anyMatch(u->u.code().equals("FILE_AGGREGATE_ORDER_COUNT_NOT_PROVEN")),"aggregate order/count stays open");
            }
            if(name.startsWith("sort")||name.startsWith("merge")) {
                var metadata=tree.deepCopy();for(var plan:metadata.path("fileInventory").path("sortPlans")) {
                    reverse((ArrayNode)plan.path("inputs"));reverse((ArrayNode)plan.path("outputs"));
                }
                need(normalized(result).equals(normalized(lower(decode(metadata)))),"legacy phase ordering is not routing authority");
            }
            var bytes=new AirJson().encode(result.publication().orElseThrow());
            need(Arrays.equals(bytes,new AirJson().encode(new AirJson().decodeForPartialAnalysis(bytes).publication())),"AIR codec roundtrip");
            var peer=tree.deepCopy();permutation(peer);need(normalized(result).equals(normalized(lower(decode(peer)))),"inventory permutation "+name);
            // Qualified projection also consumes topology; this catches a second consumer silently missing FILE_POINT.
            need(!QualifiedSourceProjection.project(source,result.admission()).nodes().isEmpty(),"handler-state traversal admits FILE points");
        }
        rejectionMatrix(raw("original"));
        System.out.println("FILE_COMPOSITE_FLOW_CHECKS="+checks);
    }
}
