package io.github.gustavo2358.lower.adapters.testing;
import io.github.gustavo2358.lower.adapters.sp.SpJsonDecoder;
import io.github.gustavo2358.lower.adapters.cli.CobolLower;
import io.github.gustavo2358.lower.application.CobolLowerer;
import io.github.gustavo2358.air.model.*;
import java.nio.file.*;
import java.util.*;

/** Independent LR oracles: documentary clauses add no execution; checkpoint has source-only identity. */
public final class FileAuxiliarySuite {
    public static void main(String[] args)throws Exception{run();}
    public static void run()throws Exception {
        for(var name:List.of("rerun-sort-declaration","rerun-sort-use","rerun-record_count","rerun-end_volume","documentary","effective","sd-documentary","same-area","same-record-area","same-sort-area","same-sort-merge-area","outside-profile","password","linage-counter","copy","rerun-ambiguous","line-sequential","same-vsam-sequential","same-qsam")) {
            var decoded=new SpJsonDecoder(CobolLower.INPUT_LIMITS).decode(bytes(name));check(decoded instanceof SpJsonDecoder.Decoded,"SP2.27 auxiliary: "+name+" "+decoded);
            var input=((SpJsonDecoder.Decoded)decoded).input();var r=new CobolLowerer().lower(input,CobolLower.OPTIONS);
            check(r.publication().isPresent(),"aux admitted "+name+" "+r.admission().diagnostics()+" "+r.validation().map(v->v.issues().stream().filter(i->i.kind()==io.github.gustavo2358.air.validation.ValidationIssue.Kind.INVALID_IR).toList()));var p=r.publication().orElseThrow();
            var checkpoint=p.resources().stream().filter(f->f.declaration().map(d->d.classification().equals("cobol.checkpoint")).orElse(false)).toList();
            check(checkpoint.size()==(name.startsWith("rerun-")?1:0),"only selected RERUN declares checkpoint "+name);
            if(!checkpoint.isEmpty()&&!name.equals("rerun-ambiguous"))check(checkpoint.getFirst().description() instanceof Interactions.LiteralTarget t&&t.name().equals("CHKPT")&&t.namespace().equals("cobol.external-file-name"),"checkpoint exact source-level name");
            if(name.equals("rerun-sort-use"))check(checkpoint.getFirst().declaration().orElseThrow().uses().size()==1,"one checkpoint site per source SORT, optional repeated execution");
            if(name.equals("rerun-sort-declaration"))check(checkpoint.getFirst().declaration().orElseThrow().uses().isEmpty(),"declaration cannot invent checkpoint execution");
            var codec=new io.github.gustavo2358.air.json.AirJson();var wire=codec.encode(p);check(codec.decode(wire).equals(p),"aux AIR roundtrip");
            Files.createDirectories(Path.of("adapters/target/fd-w6"));Files.write(Path.of("adapters/target/fd-w6/"+name+".air.json"),wire);
        }
        var mapper=new com.fasterxml.jackson.databind.ObjectMapper();
        for(var mutation:List.of("missing-inventory","missing-kind","missing-trigger","missing-checkpoint","wrong-effect","foreign-file","old-version")) {
            var root=(com.fasterxml.jackson.databind.node.ObjectNode)mapper.readTree(bytes("rerun-record_count"));var inv=(com.fasterxml.jackson.databind.node.ObjectNode)root.path("fileInventory");var c=(com.fasterxml.jackson.databind.node.ObjectNode)inv.path("auxiliary").path("clauses").get(0);
            switch(mutation){case "missing-inventory"->inv.remove("auxiliary");case "missing-kind"->c.remove("kind");case "missing-trigger"->c.remove("trigger");case "missing-checkpoint"->c.putNull("checkpoint");case "wrong-effect"->c.put("effect","DOCUMENTARY");case "foreign-file"->((com.fasterxml.jackson.databind.node.ObjectNode)c.path("fileReferences").get(0).path("candidates").get(0)).put("id","file:absent");case "old-version"->root.put("contractVersion","2.26.0");}
            var d=new SpJsonDecoder(CobolLower.INPUT_LIMITS).decode(mapper.writeValueAsBytes(root));check(!(d instanceof SpJsonDecoder.Decoded value)||new CobolLowerer().lower(value.input(),CobolLower.OPTIONS).publication().isEmpty(),"mutation "+mutation);
        }
        var base=((SpJsonDecoder.Decoded)new SpJsonDecoder(CobolLower.INPUT_LIMITS).decode(bytes("rerun-sort-declaration"))).input();var inv=base.fileInventory();var aux=inv.auxiliary().orElseThrow();var original=aux.clauses().getFirst();
        for(var effect:List.of(io.github.gustavo2358.lower.domain.FileFacts.AuxEffect.DOCUMENTARY,io.github.gustavo2358.lower.domain.FileFacts.AuxEffect.RECORD_ALIAS)){
            var wrong=new io.github.gustavo2358.lower.domain.FileFacts.AuxClause(original.id(),original.kind(),effect,original.fileReferences(),original.dataReferences(),original.parameters(),original.checkpoint(),original.trigger(),original.gapCodes(),original.provenance());
            var changed=new io.github.gustavo2358.lower.domain.FileFacts.Inventory(inv.availability(),inv.declarations(),inv.gapCodes(),inv.operations(),inv.declaratives(),inv.sorts(),Optional.of(new io.github.gustavo2358.lower.domain.FileFacts.Auxiliary(aux.availability(),List.of(wrong),aux.gapCodes())));
            var bad=new io.github.gustavo2358.lower.domain.SpInput(base.unit(),base.policy(),base.dataDeclarations(),base.statements(),base.structure(),base.gaps(),base.coverage(),base.entryInventory(),base.storageIndependence(),base.compositional(),base.storage(),changed);
            check(new CobolLowerer().lower(bad,CobolLower.OPTIONS).publication().isEmpty(),"in-memory auxiliary contradiction rejected");
        }
        System.out.println("PASS FileAuxiliarySuite");
    }
    static byte[] bytes(String name)throws Exception{return Files.readAllBytes(Path.of("adapters/src/test/resources/sp/file-dependencies/w6/"+name+".json"));}
    static void check(boolean b,String text){if(!b)throw new AssertionError(text);}
}
