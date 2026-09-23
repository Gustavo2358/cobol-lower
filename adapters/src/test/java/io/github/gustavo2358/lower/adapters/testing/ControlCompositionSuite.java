package io.github.gustavo2358.lower.adapters.testing;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.github.gustavo2358.air.model.*;
import io.github.gustavo2358.air.model.Ids.*;
import io.github.gustavo2358.air.json.AirJson;
import io.github.gustavo2358.lower.adapters.cli.CobolLower;
import io.github.gustavo2358.lower.adapters.sp.SpJsonDecoder;
import io.github.gustavo2358.lower.application.*;
import io.github.gustavo2358.lower.domain.SpInput;
import io.github.gustavo2358.lower.testing.IfInputs;
import java.util.*;
import static io.github.gustavo2358.lower.testing.CallOracle.check;

/** Source-produced control graphs: exact edges, missing outcomes, contexts and wire boundaries. */
public final class ControlCompositionSuite {
    private static final ObjectMapper JSON=new ObjectMapper();
    private static byte[] bytes(String name) throws Exception {
        try(var stream=ControlCompositionSuite.class.getResourceAsStream("/sp/control-composition/"+name+".json")) {
            return Objects.requireNonNull(stream,name).readAllBytes();
        }
    }
    private static SpInput decode(byte[] bytes) {
        var result=new SpJsonDecoder(CobolLower.INPUT_LIMITS).decode(bytes);
        check(result instanceof SpJsonDecoder.Decoded,"SP 2.37/historical decoding "+result);
        return ((SpJsonDecoder.Decoded)result).input();
    }
    private static LoweringResult lower(SpInput input) {
        var result=new CobolLowerer().lower(input,CobolLower.OPTIONS);
        check(result.publication().isPresent(),"valid AIR "+result.status()+" "+result.admission().diagnostics()+" "+result.validation());return result;
    }
    private static Map<String,List<String>> edges(LoweringResult result) {
        var map=new HashMap<String,List<String>>();result.publication().orElseThrow().units().getFirst().sequences()
            .forEach(s->map.put(s.label().localId(),CompositionalPerformSuite.successors(s.terminator()).stream().map(LabelId::localId).toList()));return map;
    }
    public static void main(String[] args) throws Exception {
        OrdinaryContinuationBoundarySuite.main(new String[0]);
        var names=List.of("terminal-if-no-else","terminal-evaluate-no-other","terminal-if-else-continue","terminal-evaluate-other-continue",
            "if-all-goto","if-all-transfer-no-join","evaluate-mixed-exits","evaluate-all-transfer-no-join","nested-if-evaluate",
            "goto-adjacency","goto-escape-performed","ordinary-contextual","goback-no-join","special-exit-paragraph",
            "ordinary-base","ordinary-dead-depending","perf-if-1","perf-if-5","perf-if-20","perf-evaluate-2","perf-evaluate-10","perf-evaluate-40","ordinary-numeric-dead-depending","perf-mixed-1","perf-mixed-5",
            "perf-goto-fanout-2","perf-goto-fanout-10","perf-goto-fanout-40","legacy-ordinary-incoming-dead","legacy-ordinary-incoming-live");
        for(var name:names) {
            var input=decode(bytes(name));check(input.equals(decode(JSON.writeValueAsBytes(JSON.readTree(bytes(name))))),"SP typed transport round-trip "+name);var result=lower(input);var pub=result.publication().orElseThrow();var unit=pub.units().getFirst();
            var codec=new AirJson();var wire=codec.encode(pub);check(pub.equals(codec.decode(wire)),"AIR round-trip "+name);
            var reversed=new ArrayList<>(input.statements());Collections.reverse(reversed);
            check(Arrays.equals(wire,codec.encode(lower(IfInputs.with(input,"statements",reversed)).publication().orElseThrow())),"M5 inventory permutation "+name);
            check(Arrays.equals(wire,codec.encode(lower(input).publication().orElseThrow())),"deterministic lowering "+name);
            var byLabel=new HashMap<LabelId,Sequence>();unit.sequences().forEach(s->byLabel.put(s.label(),s));
            var reach=CompositionalPerformSuite.reachable(unit);
            for(var fact:input.statements()) {
                var labels=result.statements().stream().filter(l->l.source().equals(fact.header().id())).map(LoweringResult.StatementLink::label).distinct().toList();
                if(fact instanceof SpInput.GobackFact)for(var label:labels) {
                    check(byLabel.get(label).terminator() instanceof Operations.Return,"GOBACK remains Return");
                    check(CompositionalPerformSuite.successors(byLabel.get(label).terminator()).isEmpty(),"GOBACK has no join");
                }
                if(fact instanceof SpInput.GoToFact g && g.targetEntry().isPresent())for(var label:labels) {
                    var t=byLabel.get(label).terminator();check(t instanceof Operations.Jump,"explicit GO TO has exactly one transfer");
                    var destination=((Operations.Jump)t).destination();
                    check(result.statements().stream().anyMatch(l->l.source().equals(g.targetEntry().orElseThrow())&&l.label().equals(destination)),"GO TO target correlation, no physical fallthrough");
                }
            }
            long reachableCalls=unit.sequences().stream().filter(s->s.terminator() instanceof Operations.Invoke && reach.contains(s.label())).count();
            check(reachableCalls==(name.equals("special-exit-paragraph")?0:name.equals("ordinary-contextual")||name.equals("legacy-ordinary-incoming-live")||name.equals("goto-escape-performed")||name.equals("evaluate-mixed-exits")?2:1),"expected CALL reachability "+name);
            if(name.equals("terminal-if-no-else")||name.equals("terminal-evaluate-no-other")) {
                var branch=unit.sequences().stream().filter(s->s.terminator() instanceof Operations.Branch).toList();
                check(branch.size()==2,"ordinary and activated branch occurrences");
                var destinations=branch.stream().map(s->((Operations.Branch)s.terminator()).falseDestination()).toList();
                check(!destinations.getFirst().equals(destinations.getLast()),"M9 ordinary outcome cannot receive activation resume");
                check(destinations.stream().map(byLabel::get).filter(s->s.terminator() instanceof Operations.Opaque).count()==1,"ordinary missing outcome has a local boundary");
                check(destinations.stream().map(byLabel::get).filter(s->s.terminator() instanceof Operations.Jump).count()==1,"activation false/no-match returns contextually");
            }
            // M6 independent predicate coverage cannot erase branches or alter targets.
            var changed=new ArrayList<SpInput.StatementFact>();
            for(var f:input.statements()) {
                if(f instanceof SpInput.IfFact i)f=IfInputs.with(i,"predicateGuarantee",IfInputs.with(i.predicateGuarantee(),"gapCodes",List.of("W7_DIAGNOSTIC_ONLY")));
                if(f instanceof SpInput.EvaluateFact e)f=IfInputs.with(e,"gapCodes",List.of("W7_DIAGNOSTIC_ONLY"));
                changed.add(f);
            }
            check(edges(result).equals(edges(lower(IfInputs.with(input,"statements",changed)))),"M6 gaps do not execute "+name);
        }
        var baseInput=decode(bytes("ordinary-base"));var peerInput=decode(bytes("ordinary-dead-depending"));
        var baseEdges=edges(lower(baseInput));var peerEdges=edges(lower(peerInput));
        for(var edge:baseEdges.entrySet())check(edge.getValue().equals(peerEdges.get(edge.getKey())),"M4 irrelevant DEPENDING preserves original normalized edges");
        for(var source:baseInput.statements())check(baseInput.ordinaryContinuations().get(source.header().id())==null
            || baseInput.ordinaryContinuations().get(source.header().id()).statement().map(SpInput.StatementId::handle)
                .equals(peerInput.ordinaryContinuations().entrySet().stream().filter(r->r.getKey().handle().equals(source.header().id().handle())).findFirst().orElseThrow().getValue().statement().map(SpInput.StatementId::handle)),
            "M4 producer ordinary facts equivalent");
        var tree=(ObjectNode)JSON.readTree(bytes("ordinary-base"));
        check(tree.path("contractVersion").asText().equals("2.37.0"),"new relation is versioned");
        var downgraded=tree.deepCopy().put("contractVersion","2.36.0");
        check(new SpJsonDecoder(CobolLower.INPUT_LIMITS).decode(JSON.writeValueAsBytes(downgraded)) instanceof SpJsonDecoder.Rejected,"historical wire rejects new relation");
        var duplicate=tree.deepCopy();var array=(com.fasterxml.jackson.databind.node.ArrayNode)duplicate.path("ordinaryContinuations");array.add(array.get(0).deepCopy());
        check(new SpJsonDecoder(CobolLower.INPUT_LIMITS).decode(JSON.writeValueAsBytes(duplicate)) instanceof SpJsonDecoder.Rejected,"duplicate relation rejected");
        var bad=decode(bytes("ordinary-base"));var terminal=bad.statements().stream().filter(SpInput.GobackFact.class::isInstance).findFirst().orElseThrow();
        var relations=new HashMap<>(bad.ordinaryContinuations());relations.put(terminal.header().id(),relations.values().iterator().next());
        check(new CobolLowerer().lower(IfInputs.with(bad,"ordinaryContinuations",relations),CobolLower.OPTIONS).publication().isEmpty(),"in-memory GOBACK continuation rejected");
        var input=decode(bytes("terminal-if-no-else"));var fact=(SpInput.IfFact)input.statements().stream().filter(SpInput.IfFact.class::isInstance).findFirst().orElseThrow();
        var partial=IfInputs.with(fact,"thenArm",IfInputs.with(fact.thenArm(),"contentAvailability",SpInput.Availability.PARTIAL));
        var facts=new ArrayList<>(input.statements());facts.replaceAll(f->f.header().id().equals(fact.header().id())?partial:f);
        check(edges(lower(input)).equals(edges(lower(IfInputs.with(input,"statements",facts)))),"known entry independent of body coverage");
        var withoutOrdinary=lower(IfInputs.with(bad,"ordinaryContinuations",Map.of()));
        var withoutUnit=withoutOrdinary.publication().orElseThrow().units().getFirst();
        var withoutReach=CompositionalPerformSuite.reachable(withoutUnit);
        check(withoutUnit.sequences().stream().noneMatch(q->q.terminator() instanceof Operations.Invoke && withoutReach.contains(q.label())),
            "missing ordinary relation cannot become next ProgramPoint");
        check(!withoutOrdinary.publication().orElseThrow().id().equals(lower(bad).publication().orElseThrow().id()),"ordinary facts participate in publication identity");
        var absentEntry=IfInputs.with(partial.thenArm(),"entry",new SpInput.ExecutableStart(SpInput.Availability.UNAVAILABLE,Optional.empty()));
        var missingArm=IfInputs.with(partial,"thenArm",absentEntry);facts.replaceAll(f->f.header().id().equals(fact.header().id())?missingArm:f);
        var missingResult=lower(IfInputs.with(input,"statements",facts));var missingUnit=missingResult.publication().orElseThrow().units().getFirst();
        check(missingUnit.sequences().stream().filter(q->q.terminator() instanceof Operations.Branch).count()==2,"missing THEN entry retains ordinary/activated false alternatives");
        var missingIndex=new HashMap<LabelId,Sequence>();missingUnit.sequences().forEach(q->missingIndex.put(q.label(),q));
        for(var q:missingUnit.sequences())if(q.terminator() instanceof Operations.Branch b)
            check(missingIndex.get(b.trueDestination()).terminator() instanceof Operations.Opaque,"missing present arm entry cannot bypass to completion");
        System.out.println("CONTROL_COMPOSITION_W7=PASS");
    }
}
