package io.github.gustavo2358.lower.adapters.testing;

import io.github.gustavo2358.lower.adapters.sp.SpJsonDecoder;
import io.github.gustavo2358.lower.adapters.cli.CobolLower;
import io.github.gustavo2358.lower.application.*;
import io.github.gustavo2358.air.model.*;

/** Emitted frontend facts must preserve the logical transfer across the AIR boundary. */
public final class RecallLocalitySuite {
    public static void main(String[] args)throws Exception {
        try(var stream=RecallLocalitySuite.class.getResourceAsStream("/sp/recall-locality/local-copy.json")) {
            var decoded=new SpJsonDecoder(CobolLower.INPUT_LIMITS).decode(stream.readAllBytes());
            if(!(decoded instanceof SpJsonDecoder.Decoded d))throw new AssertionError(decoded);
            var result=new CobolLowerer().lower(d.input(),CobolLower.OPTIONS);
            if(result.publication().isEmpty()||!result.validation().orElseThrow().isStructurallyValid())throw new AssertionError(result.status()+" "+result.admission().diagnostics());
            var publication=result.publication().orElseThrow();
            var copies=publication.units().stream().flatMap(u->u.sequences().stream()).flatMap(s->s.instructions().stream())
                .filter(i->i instanceof Operations.Assign a&&a.value() instanceof Expressions.Read).count();
            if(copies!=1)throw new AssertionError("proved logical data copy must survive missing physical effect: "+copies);
            System.out.println("RECALL_LOCALITY_COPY=PASS");
        }
    }
}
