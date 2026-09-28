package io.github.gustavo2358.lower.adapters.testing;
import com.fasterxml.jackson.databind.*;
import io.github.gustavo2358.lower.adapters.sp.SpJsonDecoder;
import io.github.gustavo2358.lower.adapters.cli.CobolLower;
import io.github.gustavo2358.lower.application.CobolLowerer;
import io.github.gustavo2358.air.model.*;
import java.util.*;
/** Executable successors come exclusively from the producer's lexical dispatch. */
public final class SqlDispatchSuite {
    public static void main(String[] args)throws Exception {
        var expected=Map.of("sql-whenever",Set.of("NORMAL","ERROR"),"sql-dynamic",Set.of("AFTERSQL"),
            "sql-lexical",Set.of("NORMAL","ERRORPGM"),"sql-continue",Set.of("NORMAL"),
            "sql-perform",Set.of("RESUMED","ERRORPGM"),"sql-missing",Set.of("NORMAL"),"sql-dead-operation",Set.<String>of());
        for(var test:expected.entrySet()) {
            byte[] wire;try(var in=SqlDispatchSuite.class.getResourceAsStream("/sp/sql-dispatch/"+test.getKey()+".json")){wire=in.readAllBytes();}
            var decoded=new SpJsonDecoder(CobolLower.INPUT_LIMITS).decode(wire);
            if(!(decoded instanceof SpJsonDecoder.Decoded d))throw new AssertionError(decoded);
            var result=new CobolLowerer().lower(d.input(),CobolLower.POSITIVE_OPTIONS);var air=result.publication().orElseThrow();
            if(!result.validation().orElseThrow().isStructurallyValid())throw new AssertionError("invalid AIR");
            var reached=TerminalSendSuite.reached(air);var names=new TreeSet<String>();
            for(var unit:air.units())for(var sequence:unit.sequences())if(reached.contains(sequence.label())
                    &&sequence.terminator() instanceof Operations.Invoke call&&call.target() instanceof Interactions.LiteralTarget target)names.add(target.name());
            if(!names.equals(test.getValue()))throw new AssertionError(test.getKey()+" expected "+test.getValue()+" got "+names);
        }
        System.out.println("PASS SqlDispatchSuite 7 independent reachability oracles");
    }
}
