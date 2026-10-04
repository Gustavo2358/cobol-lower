package io.github.gustavo2358.lower.adapters.testing;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.node.*;
import io.github.gustavo2358.lower.adapters.sp.SpJsonDecoder;
import io.github.gustavo2358.lower.adapters.cli.CobolLower;
import io.github.gustavo2358.lower.application.*;
import io.github.gustavo2358.air.model.*;
import io.github.gustavo2358.air.model.Ids.*;
import java.util.*;

final class SharedNumericChecks {
    static void run() throws Exception {
        var json=new ObjectMapper();var decoder=new SpJsonDecoder(CobolLower.INPUT_LIMITS);
        var fixture=(ObjectNode)json.readTree(SharedNumericChecks.class.getResourceAsStream("/sp/numeric-move/shared-number.json"));
        var decoded=decoder.decode(json.writeValueAsBytes(fixture));
        if(!(decoded instanceof SpJsonDecoder.Decoded ok))throw new AssertionError(decoded);
        var result=new CobolLowerer().lower(ok.input(),CobolLower.POSITIVE_OPTIONS);
        var publication=result.publication().orElseThrow(()->new AssertionError(result.admission().diagnostics()));
        var expected=Set.of("YEAR-A","MONTH-A","DAY-A","DATE-N");var seen=new HashSet<String>();Scopes.MemoryScope bound=null;
        int guards=0,invalid=0;var literals=new HashSet<java.math.BigInteger>();
        for(var u:publication.units()) {
            for(var object:u.objects())if(object.displayName().filter(expected::contains).isPresent()) {
                if(!(object.storage() instanceof Memory.UnknownBinding b)||!(b.scope() instanceof Scopes.StorageMemory))throw new AssertionError("overlap gained independent storage: "+object.displayName());
                if(!object.typeRef().equals(Types.known(Types.Builtin.INT)))throw new AssertionError("numeric type erased");
                if(bound!=null&&!bound.equals(b.scope()))throw new AssertionError("shared source allocation lost");bound=b.scope();seen.add(object.displayName().orElseThrow());
            }
            for(var sequence:u.sequences()) {
                if(sequence.terminator() instanceof Operations.Branch branch&&branch.predicate() instanceof Expressions.Unknown predicate) {
                    if(!predicate.typeRef().equals(Types.known(Types.Builtin.BOOL))||!(predicate.remainingReads() instanceof Scopes.WithinMemory))throw new AssertionError("validity must read shared memory");
                    if(branch.header().precision().control().status()!=Evidence.PrecisionStatus.OPEN)throw new AssertionError("validity claimed exact");guards++;
                }
                if(sequence.terminator() instanceof Operations.Opaque opaque&&opaque.observedKind().equals("invalid-numeric-representation")) {
                    if(!(opaque.envelope().control().remainder() instanceof Scopes.WithinControl)||opaque.envelope().dependencies().remainder()!=Scopes.AnyResource.INSTANCE
                        ||!(opaque.envelope().memory().otherWrites() instanceof Scopes.WithinMemory))throw new AssertionError("invalid representation silently closed");
                    var control=(Scopes.WithinControl)opaque.envelope().control().remainder();
                    if(!(control.scope() instanceof Scopes.UnitControl scope)||scope.labels()||scope.exceptionalExit()||scope.normalExit()||scope.halt()||!scope.externalControl()||!scope.diverge()
                        ||!opaque.envelope().control().known().contains(new Control.AnyException(Control.Propagate.INSTANCE))
                        ||opaque.envelope().control().known().contains(Control.ReturnAlternative.INSTANCE)
                        ||!opaque.envelope().control().known().contains(Control.HaltAlternative.INSTANCE))throw new AssertionError("invalid data invented local control destinations");invalid++;
                }
                for(var instruction:sequence.instructions())if(instruction instanceof Operations.Assign a&&a.value() instanceof Expressions.Literal l&&l.value() instanceof Values.IntValue n)literals.add(n.value());
            }
        }
        if(!seen.equals(expected)||guards!=2||invalid!=2||!literals.containsAll(Set.of(new java.math.BigInteger("20261004"),java.math.BigInteger.valueOf(12))))throw new AssertionError("shared numeric proof incomplete: "+seen+" "+guards+" "+invalid);
        var forged=fixture.deepCopy();var move=(ObjectNode)forged.path("statements").get(1);
        var source=(ObjectNode)move.path("source").path("reference");var target=(ObjectNode)move.path("target");
        target.set("binding",source.path("binding").deepCopy());target.set("wholeItemAccess",source.path("wholeItemAccess").deepCopy());
        var invalidInput=decoder.decode(json.writeValueAsBytes(forged));
        if(invalidInput instanceof SpJsonDecoder.Decoded d&&new CobolLowerer().lower(d.input(),CobolLower.POSITIVE_OPTIONS).publication().isPresent())throw new AssertionError("overlapping DATA transfer admitted");
        System.out.println("SHARED_NUMERIC=PASS: typed numeric views, common storage bound, explicit valid/invalid branches, no independent alias cells");
    }
}
