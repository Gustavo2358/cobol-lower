package io.github.gustavo2358.lower.adapters.testing;
import io.github.gustavo2358.lower.adapters.sp.SpJsonDecoder;
import io.github.gustavo2358.lower.adapters.cli.CobolLower;
import io.github.gustavo2358.lower.application.CobolLowerer;
import io.github.gustavo2358.air.model.*;
import io.github.gustavo2358.lower.domain.*;
import java.nio.file.*;
import java.util.*;

/** Source-derived participant counts and local procedure semantics, independent of lower output. */
public final class FileSortSuite {
    public static void main(String[] args)throws Exception{run();}
    public static void run()throws Exception {
        var calls=new HashMap<String,Integer>(Map.of("sort-multiparty",0,"merge-multiparty",0,"release-from",0,"return-into",2,"procedure-ranges",5,"section-procedure",1,"table-key",0,"table-no-key",0,"missing-procedure",0));
        calls.put("same-file",0);calls.put("implicit-use",2);calls.put("perform-procedure",2);
        for(var name:new TreeSet<>(calls.keySet())) {
            var decoded=new SpJsonDecoder(CobolLower.INPUT_LIMITS).decode(bytes(name));check(decoded instanceof SpJsonDecoder.Decoded,"SP2.26 sort contract: "+name+" "+decoded);
            var input=((SpJsonDecoder.Decoded)decoded).input();var result=new CobolLowerer().lower(input,CobolLower.OPTIONS);
            check(result.publication().isPresent(),"sort admitted: "+name+" "+result.status()+" "+result.admission().diagnostics()+" "+result.validation().map(v->v.issues().stream().filter(i->i.kind()==io.github.gustavo2358.air.validation.ValidationIssue.Kind.INVALID_IR).toList()));var p=result.publication().orElseThrow();var u=p.units().getFirst();
            long actual=u.sequences().stream().map(Sequence::terminator).filter(t->t instanceof Operations.Invoke i&&i.target() instanceof Interactions.LiteralTarget target&&target.category().equals("program")).count();
            check(actual==calls.get(name),"CALL body occurrences preserved once: "+name+" actual="+actual);
            check(u.sequences().stream().noneMatch(q->q.terminator() instanceof Operations.Invoke i&&i.target() instanceof Interactions.ComputedTarget t&&t.namespace().equals("cobol.external-file-name")),"known local SD never becomes unknown external filename");
            check(p.uncertainties().stream().noneMatch(g->g.code().equals("SOURCE_NONE_EFFECT")),"NO_OP cannot invent environmental effects");
            var resources=p.resources().stream().filter(r->r.declaration().isPresent()).toList();
            if(name.endsWith("multiparty")) {
                check(resources.size()==4,"all declarations survive");
                var roles=new HashMap<String,List<String>>();for(var r:resources)roles.put(r.declaration().orElseThrow().name(),r.declaration().orElseThrow().uses().stream().map(Interactions.ResourceUse::role).toList());
                check(roles.equals(Map.of("S",List.of("work"),"A",List.of("input"),"B",List.of("input"),"C",List.of("output"))),"multiparty roles, no Cartesian product");
                phaseOrder(u,resources);
            }
            var codec=new io.github.gustavo2358.air.json.AirJson();var wire=codec.encode(p);check(codec.decode(wire).equals(p),"sort AIR transport");
            check(java.util.Arrays.equals(wire,codec.encode(new CobolLowerer().lower(input,CobolLower.OPTIONS).publication().orElseThrow())),"deterministic sort AIR");
            Files.createDirectories(Path.of("adapters/target/fd-w5"));Files.write(Path.of("adapters/target/fd-w5/"+name+".air.json"),wire);
        }
        var mapper=new com.fasterxml.jackson.databind.ObjectMapper();
        for(var mutation:List.of("missing-role","missing-plans","missing-participant","wrong-role","wrong-sd-kind","old-version","foreign-completion","procedure-primary","missing-endpoint")) {
            var root=(com.fasterxml.jackson.databind.node.ObjectNode)mapper.readTree(bytes(mutation.startsWith("foreign")||mutation.startsWith("procedure")||mutation.equals("missing-endpoint")?"procedure-ranges":"sort-multiparty"));
            var inventory=(com.fasterxml.jackson.databind.node.ObjectNode)root.path("fileInventory");var uses=(com.fasterxml.jackson.databind.node.ArrayNode)inventory.path("operations").path("uses");
            switch(mutation) {
                case "missing-role"->((com.fasterxml.jackson.databind.node.ObjectNode)uses.get(0)).remove("role");
                case "missing-plans"->inventory.remove("sortPlans");case "missing-participant"->uses.remove(1);
                case "wrong-role"->((com.fasterxml.jackson.databind.node.ObjectNode)uses.get(1)).put("role","OUTPUT");
                case "wrong-sd-kind"->((com.fasterxml.jackson.databind.node.ObjectNode)inventory.path("declarations").get(0)).put("kind","FD");
                case "old-version"->root.put("contractVersion","2.25.0");
                case "foreign-completion"->((com.fasterxml.jackson.databind.node.ArrayNode)inventory.path("sortPlans").get(0).path("procedures").get(0).path("completions")).add("statement:absent");
                case "procedure-primary"->((com.fasterxml.jackson.databind.node.ObjectNode)inventory.path("sortPlans").get(0).path("procedures").get(0)).set("entry",root.path("entryInventory").path("entries").get(0).path("start").path("statement"));
                case "missing-endpoint"->((com.fasterxml.jackson.databind.node.ObjectNode)inventory.path("sortPlans").get(0).path("procedures").get(0)).putNull("start");
            }
            var decoded=new SpJsonDecoder(CobolLower.INPUT_LIMITS).decode(mapper.writeValueAsBytes(root));
            check(!(decoded instanceof SpJsonDecoder.Decoded d)||new CobolLowerer().lower(d.input(),CobolLower.OPTIONS).publication().isEmpty(),"sort mutation must fail: "+mutation);
        }
        var input=((SpJsonDecoder.Decoded)new SpJsonDecoder(CobolLower.INPUT_LIMITS).decode(bytes("sort-multiparty"))).input();
        var inv=input.fileInventory();
        for(var sorts:List.of(Optional.<FileFacts.SortInventory>empty(),Optional.of(new FileFacts.SortInventory(SpInput.Availability.UNAVAILABLE,List.of())),Optional.of(new FileFacts.SortInventory(SpInput.Availability.KNOWN,List.of())))) {
            var altered=new FileFacts.Inventory(inv.availability(),inv.declarations(),inv.gapCodes(),inv.operations(),inv.declaratives(),sorts);
            var bad=new SpInput(input.unit(),input.policy(),input.dataDeclarations(),input.statements(),input.structure(),input.gaps(),input.coverage(),input.entryInventory(),input.storageIndependence(),input.compositional(),input.storage(),altered);
            check(new CobolLowerer().lower(bad,CobolLower.OPTIONS).publication().isEmpty(),"in-memory aggregate cannot omit phase plan: "+sorts);
        }
        System.out.println("PASS FileSortSuite");
    }
    static void phaseOrder(io.github.gustavo2358.air.model.Unit u,List<Interactions.Resource> resources) {
        var labels=new HashMap<io.github.gustavo2358.air.model.Ids.OperationId,io.github.gustavo2358.air.model.Ids.LabelId>();
        var graph=new HashMap<io.github.gustavo2358.air.model.Ids.LabelId,List<io.github.gustavo2358.air.model.Ids.LabelId>>();
        for(var q:u.sequences()) {
            var t=q.terminator();labels.put(t.header().id(),q.label());var next=new ArrayList<io.github.gustavo2358.air.model.Ids.LabelId>();
            if(t instanceof Operations.Jump j)next.add(j.destination());
            else if(t instanceof Operations.Branch b){next.add(b.trueDestination());next.add(b.falseDestination());}
            else if(t instanceof Operations.Invoke i){for(var outcome:i.outcomes().known())if(outcome instanceof Control.Normal n)next.add(n.label());}
            else if(t instanceof Operations.Opaque o){for(var alternative:o.envelope().control().known())if(alternative instanceof Control.JumpAlternative j)next.add(j.label());}
            graph.put(q.label(),next);
        }
        var byRole=new HashMap<String,List<io.github.gustavo2358.air.model.Ids.LabelId>>();
        for(var r:resources)for(var use:r.declaration().orElseThrow().uses())byRole.computeIfAbsent(use.role(),k->new ArrayList<>()).add(labels.get(use.operation()));
        var work=byRole.get("work").getFirst();var output=byRole.get("output").getFirst();
        var all=reachable(u.entries().getFirst().initialLabel().orElseThrow(),graph,null);
        check(byRole.values().stream().flatMap(List::stream).allMatch(all::contains),"all aggregate participants reachable");
        for(var input:byRole.get("input")) {
            check(reachable(input,graph,null).contains(work),"input returns to work phase");
            check(!reachable(input,graph,work).contains(output),"output cannot precede work phase");
            check(!reachable(work,graph,null).contains(input),"work cannot return to input phase");
        }
        check(reachable(work,graph,null).contains(output),"output follows local sort work");
    }
    static Set<io.github.gustavo2358.air.model.Ids.LabelId> reachable(io.github.gustavo2358.air.model.Ids.LabelId start,
            Map<io.github.gustavo2358.air.model.Ids.LabelId,List<io.github.gustavo2358.air.model.Ids.LabelId>> graph,io.github.gustavo2358.air.model.Ids.LabelId blocked) {
        var seen=new HashSet<io.github.gustavo2358.air.model.Ids.LabelId>();var pending=new ArrayDeque<io.github.gustavo2358.air.model.Ids.LabelId>();pending.add(start);
        while(!pending.isEmpty()){var at=pending.pop();if(!at.equals(blocked)&&seen.add(at))pending.addAll(graph.getOrDefault(at,List.of()));}return seen;
    }
    static byte[] bytes(String name)throws Exception {try(var in=FileSortSuite.class.getResourceAsStream("/sp/file-dependencies/w5/"+name+".json")){return Objects.requireNonNull(in,name).readAllBytes();}}
    static void check(boolean value,String message){if(!value)throw new AssertionError(message);}
}
