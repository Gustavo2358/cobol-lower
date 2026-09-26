package io.github.gustavo2358.lower.adapters.testing;

import io.github.gustavo2358.lower.adapters.sp.SpJsonDecoder;
import io.github.gustavo2358.lower.adapters.cli.CobolLower;
import io.github.gustavo2358.lower.application.*;
import io.github.gustavo2358.air.model.*;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.node.*;

public final class CicsHostEffectsSuite {
    public static void main(String[] args)throws Exception {
        var json=new ObjectMapper();
        try(var stream=CicsHostEffectsSuite.class.getResourceAsStream("/sp/recall-locality/registration.json")) {
            var tree=(ObjectNode)json.readTree(stream);var decoder=new SpJsonDecoder(CobolLower.INPUT_LIMITS);
            var decoded=decoder.decode(json.writeValueAsBytes(tree));
            if(!(decoded instanceof SpJsonDecoder.Decoded d))throw new AssertionError(decoded);
            var result=new CobolLowerer().lower(d.input(),CobolLower.OPTIONS);
            if(result.publication().isEmpty()||!result.validation().orElseThrow().isStructurallyValid())throw new AssertionError(result.status()+" "+result.admission().diagnostics());
            var operations=result.publication().orElseThrow().units().stream().flatMap(u->u.sequences().stream()).map(Sequence::terminator).toList();
            if(operations.stream().noneMatch(t->t instanceof Operations.Opaque o&&o.observedKind().equals("cics-handler-registration/ACTIVATE")
                &&o.envelope().memory().knownWrites().isEmpty()&&o.envelope().memory().knownReads().isEmpty()
                &&o.envelope().control().known().size()==1))throw new AssertionError("registration preserves host memory and only its published completion");
            var bad=tree.deepCopy();bad.put("contractVersion","2.45.0");
            if(!(decoder.decode(json.writeValueAsBytes(bad)) instanceof SpJsonDecoder.Rejected))throw new AssertionError("registration proof requires new version");
            var h=(ObjectNode)tree.path("statements").get(0);h.remove("registrationEffects");
            var absent=decoder.decode(json.writeValueAsBytes(tree));
            if(!(absent instanceof SpJsonDecoder.Decoded a))throw new AssertionError(absent);
            var conservative=new CobolLowerer().lower(a.input(),CobolLower.OPTIONS);
            if(conservative.admission().status()!=Admission.Status.IMPLEMENTATION_LIMIT)throw new AssertionError("missing proof preserves executable admission barrier");
            if(conservative.publication().stream().flatMap(p->p.units().stream()).flatMap(u->u.sequences().stream()).map(Sequence::terminator)
                .anyMatch(t->t instanceof Operations.Opaque o&&o.observedKind().startsWith("cics-handler-registration")))throw new AssertionError("absence is not empty memory proof");
        }
        try(var stream=CicsHostEffectsSuite.class.getResourceAsStream("/sp/recall-locality/receive.json")) {
            var tree=(ObjectNode)json.readTree(stream);var decoder=new SpJsonDecoder(CobolLower.INPUT_LIMITS);
            var decoded=decoder.decode(json.writeValueAsBytes(tree));
            if(!(decoded instanceof SpJsonDecoder.Decoded d))throw new AssertionError(decoded);
            var result=new CobolLowerer().lower(d.input(),CobolLower.OPTIONS);
            if(result.publication().isEmpty()||!result.validation().orElseThrow().isStructurallyValid())throw new AssertionError(result.status()+" "+result.admission().diagnostics());
            boolean command=result.publication().orElseThrow().units().stream().flatMap(u->u.sequences().stream()).map(Sequence::terminator)
                .anyMatch(t->t instanceof Operations.Opaque o&&o.observedKind().equals("cics-host-command/RECEIVE_MAP")
                    &&o.envelope().memory().knownWrites().size()==2&&o.envelope().memory().mustOverwrite().isEmpty()
                    &&!o.envelope().control().known().isEmpty()&&o.envelope().dependencies().remainder() instanceof Scopes.AnyResource);
            if(!command)throw new AssertionError("qualified RECEIVE must retain bounded MAY writes and published completion");
            var bad=tree.deepCopy();bad.put("contractVersion","2.45.0");
            if(!(decoder.decode(json.writeValueAsBytes(bad)) instanceof SpJsonDecoder.Rejected))throw new AssertionError("new effect proof rejected under old version");
            for(var statement:tree.path("statements"))if(statement.path("variant").asText().equals("CICS_COMMAND"))
                ((ArrayNode)statement.path("hostEffects").path("literalOptions")).removeAll();
            if(!(decoder.decode(json.writeValueAsBytes(tree)) instanceof SpJsonDecoder.Rejected))throw new AssertionError("missing literal classification cannot close host effects");
            System.out.println("CICS_HOST_EFFECTS=PASS");
        }
    }
}
