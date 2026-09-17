package io.github.gustavo2358.lower.adapters.testing;
import io.github.gustavo2358.lower.adapters.sp.SpJsonDecoder;
import io.github.gustavo2358.lower.adapters.cli.CobolLower;
import io.github.gustavo2358.lower.application.CobolLowerer;
import java.nio.file.*;
import java.util.*;

/** Oracle: handler/USE are conditional, distinct; USE never becomes primary prefix. */
public final class FileControlSuite {
    public static void main(String[] args)throws Exception {run();}
    public static void run()throws Exception {
        paragraphCompletion();
        for(var name:List.of("use-and-handler","mode-and-nested-handler","nested-use","implicit-handler","key-status","escape-use")) {
            var decoded=new SpJsonDecoder(CobolLower.INPUT_LIMITS).decode(bytes(name));
            check(decoded instanceof SpJsonDecoder.Decoded,"SP2.25 control facts decode: "+decoded);
            var result=new CobolLowerer().lower(((SpJsonDecoder.Decoded)decoded).input(),CobolLower.OPTIONS);
            check(result.publication().isPresent(),"USE and handler publication admitted: "+result.status()+" "+result.admission().diagnostics());
        }
        for(var name:List.of("use-and-handler","mode-and-nested-handler","nested-use","implicit-handler","key-status","escape-use")) {
            var input=((SpJsonDecoder.Decoded)new SpJsonDecoder(CobolLower.INPUT_LIMITS).decode(bytes(name))).input();
            var result=new CobolLowerer().lower(input,CobolLower.OPTIONS);var publication=result.publication().orElseThrow();var unit=publication.units().getFirst();
            var returns=unit.sequences().stream().map(q->q.terminator()).filter(t->t instanceof io.github.gustavo2358.air.model.Operations.Opaque o&&o.observedKind().equals("use-body-return")).map(io.github.gustavo2358.air.model.Operations.Opaque.class::cast).toList();
            check(returns.size()==(name.equals("implicit-handler")?0:1),"one shared USE completion, no cloned source body");
            if(!returns.isEmpty())check(returns.getFirst().envelope().memory().otherWrites() instanceof io.github.gustavo2358.air.model.Scopes.NoMemory,"return adds no body memory effect a second time");
            if(!returns.isEmpty())check(!returns.getFirst().envelope().control().known().isEmpty(),"USE completion retains its actual call-site resumes");
            check(returns.isEmpty()||publication.uncertainties().stream().anyMatch(u->u.code().equals("LOCAL_RETURN_CONTEXT_NOT_PROVEN")),"return pairing approximation is explicit");
            var calls=unit.sequences().stream().filter(q->q.terminator() instanceof io.github.gustavo2358.air.model.Operations.Invoke i&&i.target() instanceof io.github.gustavo2358.air.model.Interactions.LiteralTarget t&&t.category().equals("program")).toList();
            check(calls.size()==Map.of("use-and-handler",3,"mode-and-nested-handler",5,"nested-use",4,"implicit-handler",2,"key-status",6,"escape-use",3).get(name),"handler/USE CALL occurrences are never cloned or dropped: "+name+" actual="+calls.size());
            check(calls.stream().noneMatch(q->unit.entries().getFirst().initialLabel().filter(q.label()::equals).isPresent()),"USE cannot be primary entry");
            check(unit.sequences().stream().noneMatch(q->q.terminator() instanceof io.github.gustavo2358.air.model.Operations.Opaque o&&o.observedKind().equals("file-outcome-continuation")),"qualified control does not use W3 global continuation bound");
            var codec=new io.github.gustavo2358.air.json.AirJson();var wire=codec.encode(publication);
            check(publication.equals(codec.decode(wire)),"local handler AIR codec roundtrip");
            Files.createDirectories(Path.of("adapters/target/fd-w4"));Files.write(Path.of("adapters/target/fd-w4/"+name+".air.json"),wire);
        }
        var mapper=new com.fasterxml.jackson.databind.ObjectMapper();
        for(var mutation:List.of("missing-control","missing-declaratives","foreign-use","wrong-effect","missing-event","handler-cross-owner","use-primary","old-version","remove-use","remove-critical","unbound-use-file","wrong-open-mode","wrong-continuation")) {
            var root=(com.fasterxml.jackson.databind.node.ObjectNode)mapper.readTree(bytes("use-and-handler"));
            var inv=(com.fasterxml.jackson.databind.node.ObjectNode)root.path("fileInventory");var use=(com.fasterxml.jackson.databind.node.ObjectNode)inv.path("operations").path("uses").get(0);var control=(com.fasterxml.jackson.databind.node.ObjectNode)use.path("control");
            var routes=(com.fasterxml.jackson.databind.node.ArrayNode)control.path("routes");
            switch(mutation) {
                case "missing-control"->use.remove("control");case "missing-declaratives"->inv.remove("declaratives");
                case "foreign-use"->((com.fasterxml.jackson.databind.node.ObjectNode)routes.get(2).path("destinations").get(0)).put("declarative","use:absent");
                case "wrong-effect"->((com.fasterxml.jackson.databind.node.ObjectNode)routes.get(1)).put("effects","SUCCESS");
                case "missing-event"->routes.remove(1);
                case "handler-cross-owner"->((com.fasterxml.jackson.databind.node.ObjectNode)routes.get(1).path("destinations").get(0)).put("handler","INVALID_KEY");
                case "use-primary"->((com.fasterxml.jackson.databind.node.ObjectNode)root.path("entryInventory").path("entries").get(0).path("start")).set("statement",inv.path("declaratives").get(0).path("entry"));
                case "old-version"->root.put("contractVersion","2.24.0");
                case "wrong-continuation"->control.set("continuation",inv.path("declaratives").get(0).path("entry"));
                case "remove-use"->{var d=(com.fasterxml.jackson.databind.node.ObjectNode)routes.get(2).path("destinations").get(0);d.put("kind","CONTINUE");d.putNull("declarative");}
                case "remove-critical"->((com.fasterxml.jackson.databind.node.ObjectNode)routes.get(2)).put("criticalExit",false);
                case "unbound-use-file"->((com.fasterxml.jackson.databind.node.ObjectNode)inv.path("declaratives").get(0).path("files").get(0)).put("id","absent");
                case "wrong-open-mode"->{root=(com.fasterxml.jackson.databind.node.ObjectNode)mapper.readTree(bytes("mode-and-nested-handler"));((com.fasterxml.jackson.databind.node.ObjectNode)root.path("fileInventory").path("declaratives").get(0)).put("mode","OUTPUT");}

            }
            var decoded=new SpJsonDecoder(CobolLower.INPUT_LIMITS).decode(mapper.writeValueAsBytes(root));
            check(!(decoded instanceof SpJsonDecoder.Decoded d)||new CobolLowerer().lower(d.input(),CobolLower.OPTIONS).publication().isEmpty(),"control mutation must fail: "+mutation);
        }
        var input=((SpJsonDecoder.Decoded)new SpJsonDecoder(CobolLower.INPUT_LIMITS).decode(bytes("use-and-handler"))).input();
        var inv=input.fileInventory();var operations=inv.operations();var uses=new ArrayList<>(operations.uses());var use=uses.getFirst();var plan=use.control().orElseThrow();var routes=new ArrayList<>(plan.routes());
        var error=routes.getLast();routes.set(routes.size()-1,new io.github.gustavo2358.lower.domain.FileFacts.ControlRoute(error.event(),error.effects(),error.destinations(),false));
        var badPlan=new io.github.gustavo2358.lower.domain.FileFacts.ControlPlan(plan.availability(),plan.continuation(),routes,plan.gapCodes());
        uses.set(0,new io.github.gustavo2358.lower.domain.FileFacts.Use(use.statement(),use.ordinal(),use.command(),use.mode(),use.profile(),use.bindingStatus(),use.candidates(),use.provenance(),use.gapCodes(),use.surface(),use.effects(),Optional.of(badPlan)));
        var bad=new io.github.gustavo2358.lower.domain.SpInput(input.unit(),input.policy(),input.dataDeclarations(),input.statements(),input.structure(),input.gaps(),input.coverage(),input.entryInventory(),input.storageIndependence(),input.compositional(),input.storage(),
            new io.github.gustavo2358.lower.domain.FileFacts.Inventory(inv.availability(),inv.declarations(),inv.gapCodes(),new io.github.gustavo2358.lower.domain.FileFacts.Operations(operations.availability(),uses,operations.gapCodes()),inv.declaratives()));
        check(new CobolLowerer().lower(bad,CobolLower.OPTIONS).publication().isEmpty(),"in-memory callers cannot omit the critical error alternative");
        System.out.println("PASS FileControlSuite");
    }
    static byte[] bytes(String name)throws Exception {try(var in=FileControlSuite.class.getResourceAsStream("/sp/file-dependencies/w4/"+name+".json")){return Objects.requireNonNull(in).readAllBytes();}}
    static void paragraphCompletion()throws Exception {
        var mapper=new com.fasterxml.jackson.databind.ObjectMapper();
        com.fasterxml.jackson.databind.node.ObjectNode root;
        try(var in=FileControlSuite.class.getResourceAsStream("/sp/file-dependencies/w11/perform-file-paragraph.json")) {
            root=(com.fasterxml.jackson.databind.node.ObjectNode)mapper.readTree(Objects.requireNonNull(in));
        }
        var use=root.path("fileInventory").path("operations").path("uses").get(0);
        var statement=java.util.stream.StreamSupport.stream(root.path("statements").spliterator(),false)
            .filter(s->s.path("header").path("id").equals(use.path("statement"))).findFirst().orElseThrow();
        check(statement.path("normalContinuation").path("statement").isNull(),"manual paragraph-end intrinsic completion");
        check(!use.path("control").path("continuation").isNull(),"ordinary outcome reaches WRITE-END");
        var input=((SpJsonDecoder.Decoded)new SpJsonDecoder(CobolLower.INPUT_LIMITS).decode(mapper.writeValueAsBytes(root))).input();
        var result=new CobolLowerer().lower(input,CobolLower.OPTIONS);
        check(result.publication().isPresent(),"separate intrinsic/ordinary completion: "+result.admission().diagnostics());
        var operations=result.publication().orElseThrow().units().getFirst().sequences().stream()
            .map(q->q.terminator()).filter(io.github.gustavo2358.air.model.Operations.Invoke.class::isInstance)
            .map(io.github.gustavo2358.air.model.Operations.Invoke.class::cast).toList();
        check(operations.stream().filter(i->i.action().equals("write")).count()==1,"WRITE occurrence kept in performed paragraph");
        check(operations.stream().filter(i->i.action().equals("call")).count()==1,"AFTER CALL occurrence kept");
        ((com.fasterxml.jackson.databind.node.ObjectNode)statement.path("normalContinuation"))
            .set("statement",use.path("control").path("continuation"));
        ((com.fasterxml.jackson.databind.node.ObjectNode)statement.path("normalContinuation"))
            .put("availability","KNOWN");
        var malformed=((SpJsonDecoder.Decoded)new SpJsonDecoder(CobolLower.INPUT_LIMITS).decode(mapper.writeValueAsBytes(root))).input();
        var rejected=new CobolLowerer().lower(malformed,CobolLower.OPTIONS);
        check(rejected.publication().isEmpty(),
            "ordinary edge cannot be relabeled intrinsic to cross the paragraph");
        check(rejected.admission().diagnostics().toString().contains("intrinsic normal edge stays in paragraph"),
            "negative reaches the paragraph structure invariant: "+rejected.admission().diagnostics());
    }
    static void check(boolean value,String message){if(!value)throw new AssertionError(message);}
}
