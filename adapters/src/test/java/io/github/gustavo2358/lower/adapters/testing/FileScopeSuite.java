package io.github.gustavo2358.lower.adapters.testing;
import io.github.gustavo2358.lower.adapters.sp.*;
import io.github.gustavo2358.lower.adapters.cli.CobolLower;
import io.github.gustavo2358.lower.application.*;
import io.github.gustavo2358.lower.domain.*;
import io.github.gustavo2358.air.model.*;
import java.util.*;
import static io.github.gustavo2358.lower.testing.CallOracle.check;
/** Hand-written oracle: declarations stay with their owner; executable uses stay with theirs. */
public final class FileScopeSuite {
    public static void main(String[] args)throws Exception {
        var decoder=new CompilationJsonDecoder(CobolLower.INPUT_LIMITS);var mapper=new com.fasterxml.jackson.databind.ObjectMapper();
        for(var name:List.of("nested","external","copy","qualified","missing-copy","global-read")){
            var bytes=Objects.requireNonNull(FileScopeSuite.class.getResourceAsStream("/sp/file-scope/"+name+".json")).readAllBytes();
            var decoded=decoder.decode(bytes);check(decoded instanceof CompilationJsonDecoder.Compilation,"compilation decode "+decoded);
            var input=((CompilationJsonDecoder.Compilation)decoded).input();var result=new CompilationLowerer().lower(input,CobolLower.OPTIONS);
            check(result.publication().isPresent(),"composition admitted and AIR validated: "+result.status()+" "+result.admission().diagnostics()+" "+result.validation());
            var p=result.publication().orElseThrow();check(p.units().size()==(name.equals("nested")?3:name.equals("qualified")?1:2),"all units covered");
            if(name.equals("nested")){
                var parent=p.units().get(0);var child=p.units().get(1);var shadow=p.units().get(2);
                check(child.containingUnit().equals(Optional.of(parent.id()))&&shadow.containingUnit().equals(Optional.of(parent.id())),"canonical parentage");
                var shared=p.resources().stream().filter(r->r.declaration().orElseThrow().owner().equals(parent.id())&&r.declaration().orElseThrow().name().equals("SHARED-F")).findFirst().orElseThrow();
                check(shared.declaration().orElseThrow().uses().size()==2&&shared.declaration().orElseThrow().uses().stream().allMatch(u->u.operation().unit().equals(child.id())),"GLOBAL uses belong only to child");
                check(child.objects().stream().anyMatch(o->o.storage() instanceof Memory.AliasBinding a&&a.object().unit().equals(parent.id())),"captured DATA aliases original object");
                check(p.resources().stream().filter(r->r.declaration().orElseThrow().owner().equals(shadow.id())).allMatch(r->r.declaration().orElseThrow().uses().stream().allMatch(u->u.operation().unit().equals(shadow.id()))),"shadow stays local");
                for(var mutation:List.of("missing-unit","missing-parent","self-parent","missing-capture","private-capture","missing-record","foreign-file","unknown-field","version")){
                    var doc=mapper.readTree(bytes);var units=(com.fasterxml.jackson.databind.node.ArrayNode)doc.path("units");var c=(com.fasterxml.jackson.databind.node.ObjectNode)units.get(1);
                    switch(mutation){
                        case "missing-unit"->units.remove(0);
                        case "missing-parent"->c.putNull("parent");
                        case "self-parent"->c.set("parent",c.path("product").path("unit"));
                        case "missing-capture"->c.putArray("dataCaptures");
                        case "private-capture"->((com.fasterxml.jackson.databind.node.ObjectNode)c.path("dataCaptures").get(0)).put("sourceDataId","data:1");
                        case "missing-record"->((com.fasterxml.jackson.databind.node.ObjectNode)c.path("dataCaptures").get(0)).put("sourceDataId","data:99999");
                        case "foreign-file"->((com.fasterxml.jackson.databind.node.ObjectNode)c.path("fileCaptures").get(0)).put("id","file:99999");
                        case "unknown-field"->c.put("bindingMechanism","UNKNOWN");
                        case "version"->((com.fasterxml.jackson.databind.node.ObjectNode)doc).put("contractVersion","9.0.0");
                    }
                    var d=decoder.decode(mapper.writeValueAsBytes(doc));
                    if(d instanceof CompilationJsonDecoder.Compilation bad)check(new CompilationLowerer().lower(bad.input(),CobolLower.OPTIONS).status()==LoweringResult.Status.INVALID_INPUT,"memory admission rejects "+mutation);
                    else check(d instanceof CompilationJsonDecoder.Rejected,"wire rejects "+mutation);
                }
            }else if(name.equals("external")){
                check(p.units().stream().allMatch(u->u.containingUnit().isEmpty()&&u.visibleObjects().isEmpty()),"EXTERNAL homonyms not captured by spelling");
                check(p.resources().size()==2&&p.resources().get(0).declaration().orElseThrow().owner()!=p.resources().get(1).declaration().orElseThrow().owner(),"distinct EXTERNAL declarations");
            }
            if(name.equals("global-read")){
                var child=p.units().get(1);var read=child.sequences().stream().map(Sequence::terminator).filter(Operations.Invoke.class::isInstance).map(Operations.Invoke.class::cast).filter(i->i.action().equals("read")).findFirst().orElseThrow();
                check(read.effectBound().otherwise().writes() instanceof Scopes.NoMemory,"known I/O bound cannot be replaced by all visible writes");
                check(child.objects().stream().anyMatch(o->o.storage() instanceof Memory.AliasBinding),"implicit record capture materialized");
            }
            if(name.equals("missing-copy"))check(p.uncertainties().stream().anyMatch(u->u.code().equals("COMPILATION_UNIT_INVENTORY_INPUT_MISSING")),"missing input remains explicit at compilation boundary");
            var reversed=new ArrayList<>(input.units());Collections.reverse(reversed);var inventory=new ArrayList<>(input.unitInventory());Collections.reverse(inventory);
            check(new CompilationLowerer().lower(new SpCompilation(input.inventoryStatus(),inventory,reversed),CobolLower.OPTIONS).publication().equals(result.publication()),"unit enumeration order cannot change composition");
            check(new SpJsonDecoder(CobolLower.INPUT_LIMITS).decode(bytes) instanceof SpJsonDecoder.Rejected,"unit-only reader rejects new envelope explicitly");
            var dir=java.nio.file.Path.of(".harness-results/fd-w9");java.nio.file.Files.createDirectories(dir);
            new io.github.gustavo2358.lower.adapters.air.AirFileOutput().write(p,dir.resolve(name+".air.json"));
        }
        System.out.println("FILE_SCOPE=PASS 6 sources, composition, owner, captures, 9 negative contracts, unit permutations");
    }
}
