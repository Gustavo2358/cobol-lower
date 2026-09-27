package io.github.gustavo2358.lower.adapters.testing;

import io.github.gustavo2358.lower.adapters.sp.*;
import io.github.gustavo2358.lower.adapters.cli.CobolLower;
import io.github.gustavo2358.lower.application.*;
import io.github.gustavo2358.lower.domain.*;
import io.github.gustavo2358.air.model.*;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.node.*;
import java.util.*;
import java.util.zip.GZIPInputStream;
import static io.github.gustavo2358.lower.testing.CallOracle.check;

/** Incomplete source keeps known effects; forged closed omissions remain invalid. */
public final class IncompleteFileEffectsSuite {
    public static void main(String[] args)throws Exception {
        var decoder=new CompilationJsonDecoder(CobolLower.INPUT_LIMITS);var mapper=new ObjectMapper();
        for(var name:List.of("file-namespace-shadowing","nested-global-through-local-file","statements")) {
            byte[] bytes;
            try(var in=new GZIPInputStream(Objects.requireNonNull(IncompleteFileEffectsSuite.class.getResourceAsStream("/sp/incomplete-file-effects/"+name+".json.gz")))){bytes=in.readAllBytes();}
            var decoded=decoder.decode(bytes);check(decoded instanceof CompilationJsonDecoder.Compilation,"partial producer contract decodes");
            var input=((CompilationJsonDecoder.Compilation)decoded).input();
            var result=new CompilationLowerer().lower(input,CobolLower.OPTIONS);
            check(result.publication().isPresent(),"partial I/O is admitted: "+result.admission().diagnostics());
            var p=result.publication().orElseThrow();var codec=new io.github.gustavo2358.air.json.AirJson();
            check(p.equals(codec.decode(codec.encode(p))),"partial I/O AIR round trip");
            var reversed=new ArrayList<>(input.units());Collections.reverse(reversed);
            check(new CompilationLowerer().lower(new SpCompilation(input.inventoryStatus(),input.unitInventory(),reversed),CobolLower.OPTIONS).publication().equals(result.publication()),"partial file owners do not depend on unit order");
            var plans=input.units().stream().flatMap(u->u.product().fileInventory().operations().uses().stream()).map(u->u.effects().orElseThrow()).toList();
            check(plans.stream().anyMatch(e->e.availability()==SpInput.Availability.PARTIAL),"incomplete input remains partial");
            if(name.equals("statements")) {
                check(plans.stream().flatMap(e->e.before().stream()).count()==3,"WRITE/REWRITE/RELEASE keep their FROM effect");
                check(plans.stream().flatMap(e->e.before().stream()).allMatch(s->s.kind()==FileFacts.MemoryKind.MAY_UNKNOWN&&s.source().isPresent()),"unproved owner retains source and cannot authorize copy kill");
                check(p.units().stream().flatMap(u->u.sequences().stream()).map(Sequence::terminator).anyMatch(t->t instanceof Operations.Opaque o
                    &&o.envelope().memory().otherWrites() instanceof Scopes.WithinMemory b&&b.scope() instanceof Scopes.VisibleMemory),"unknown target remains a visible MAY write");
            } else check(plans.stream().flatMap(e->e.outcomes().stream()).flatMap(o->o.steps().stream()).anyMatch(s->s.role()==FileFacts.MemoryRole.RECORD),"FD without SELECT keeps known record effects");
            var broken=mapper.readTree(bytes);int mutations=0;
            for(var u:broken.path("units"))for(var use:u.path("product").path("fileInventory").path("operations").path("uses")) {
                var effects=(ObjectNode)use.path("effects");
                if(name.equals("statements")&&!effects.path("before").isEmpty()) {effects.putArray("before");effects.put("unknownWriteBound",false);mutations++;}
                if(!name.equals("statements"))for(var outcome:effects.path("outcomes")) {
                    var steps=(ArrayNode)outcome.path("steps");if(!steps.isEmpty()){steps.removeAll();effects.put("unknownWriteBound",false);mutations++;}
                }
            }
            check(mutations>0,"negative actually removes a required effect");
            var invalid=decoder.decode(mapper.writeValueAsBytes(broken));
            check(invalid instanceof CompilationJsonDecoder.Rejected||invalid instanceof CompilationJsonDecoder.Compilation c
                &&new CompilationLowerer().lower(c.input(),CobolLower.OPTIONS).status()==LoweringResult.Status.INVALID_INPUT,"closed omitted buffer/FROM must still reject");
        }
        System.out.println("INCOMPLETE_FILE_EFFECTS=PASS: 3 real products, MAY bounds, ordered FROM, owner permutation, roundtrip, closed-omission negatives");
    }
}
