package io.github.gustavo2358.lower.adapters.testing;
import java.util.List;
import io.github.gustavo2358.lower.adapters.sp.SpJsonDecoder;
import io.github.gustavo2358.lower.adapters.cli.CobolLower;
import io.github.gustavo2358.lower.application.*;
import io.github.gustavo2358.air.model.*;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.node.*;
public final class DliEffectsSuite {
    public static void main(String[] args)throws Exception {
        var json=new ObjectMapper();var tree=(ObjectNode)json.readTree(DliEffectsSuite.class.getResourceAsStream("/sp/recall-locality/dli-effects.json"));
        var decoder=new SpJsonDecoder(CobolLower.INPUT_LIMITS);var decoded=decoder.decode(json.writeValueAsBytes(tree));
        if(!(decoded instanceof SpJsonDecoder.Decoded d))throw new AssertionError(decoded);
        var result=new CobolLowerer().lower(d.input(),CobolLower.OPTIONS);
        if(result.publication().isEmpty()||!result.validation().orElseThrow().isStructurallyValid())throw new AssertionError(result.status()+" "+result.admission().diagnostics());
        var op=result.publication().orElseThrow().units().stream().flatMap(u->u.sequences().stream()).map(Sequence::terminator)
            .filter(t->t instanceof Operations.Opaque o&&o.observedKind().equals("OPAQUE_DLI")&&!o.envelope().control().known().isEmpty()).map(Operations.Opaque.class::cast).findFirst().orElseThrow();
        if(op.envelope().memory().knownWrites().size()!=1||!(op.envelope().memory().otherWrites() instanceof Scopes.NoMemory)
            ||!op.envelope().memory().mustOverwrite().isEmpty()||op.envelope().control().known().size()!=2
            ||!(op.envelope().dependencies().remainder() instanceof Scopes.AnyResource))throw new AssertionError(op);
        var bad=tree.deepCopy();bad.put("contractVersion","2.45.0");
        if(!(decoder.decode(json.writeValueAsBytes(bad)) instanceof SpJsonDecoder.Rejected))throw new AssertionError("DLI proof requires SP2.46");
        // A published write without an admitted address must remain an open write.
        bad=tree.deepCopy();for(var s:bad.path("statements"))if(s.path("observedShape").asText().equals("OPAQUE_DLI"))
            for(var ref:s.path("knownReferences"))if(ref.path("role").asText().equals("WRITE"))((ObjectNode)ref).putNull("wholeItemAccess");
        var unknown=decoder.decode(json.writeValueAsBytes(bad));if(!(unknown instanceof SpJsonDecoder.Decoded u))throw new AssertionError(unknown);
        var changed=new CobolLowerer().lower(u.input(),CobolLower.OPTIONS);
        var writes=changed.publication().orElseThrow().units().stream().flatMap(x->x.sequences().stream()).map(Sequence::terminator)
            .filter(t->t instanceof Operations.Opaque o&&o.observedKind().equals("OPAQUE_DLI")&&!o.envelope().control().known().isEmpty()).map(Operations.Opaque.class::cast).findFirst().orElseThrow().envelope().memory();
        if(writes.otherWrites() instanceof Scopes.NoMemory)throw new AssertionError("unavailable output address cannot erase mutation");
        for(var field:List.of("unknownReadBound","unknownWriteBound","unknownExposureBound","environment")) {
            var forgedTree=tree.deepCopy();
            for(var e:forgedTree.path("statementEffects"))if(e.path("proof").asText().equals("DLI_HOST_OPERANDS"))((ObjectNode)e).put(field,field.equals("environment")?"NONE":"ALL");
            var forgedInput=decoder.decode(json.writeValueAsBytes(forgedTree));
            if(forgedInput instanceof SpJsonDecoder.Decoded f&&new CobolLowerer().lower(f.input(),io.github.gustavo2358.lower.adapters.cli.CobolLower.POSITIVE_OPTIONS).publication().isPresent())throw new AssertionError("forged effect proof admitted: "+field);
        }
        System.out.println("DLI_EFFECTS=PASS");
    }
}
