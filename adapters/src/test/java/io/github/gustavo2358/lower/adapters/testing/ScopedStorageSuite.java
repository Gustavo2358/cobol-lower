package io.github.gustavo2358.lower.adapters.testing;

import io.github.gustavo2358.lower.adapters.cli.CobolLower;
import io.github.gustavo2358.lower.adapters.sp.SpJsonDecoder;
import io.github.gustavo2358.lower.application.*;
import io.github.gustavo2358.lower.domain.*;
import io.github.gustavo2358.lower.testing.IfInputs;
import java.util.*;
import java.math.BigInteger;
import static io.github.gustavo2358.lower.testing.CallOracle.check;

/** Actual scoped SP fixtures, plus hostile facts that must not certify layout. */
public final class ScopedStorageSuite {
    public static void main(String[] args) throws Exception { run(); }
    public static void run() throws Exception {
        var decoder=new SpJsonDecoder(CobolLower.INPUT_LIMITS);
        for(var name:List.of("nested-overlay","unknown-renames","independent-unit")) {
            byte[] bytes;
            try(var in=ScopedStorageSuite.class.getResourceAsStream("/sp/recall-first/"+name+".json")){bytes=Objects.requireNonNull(in).readAllBytes();}
            var decoded=decoder.decode(bytes);check(decoded instanceof SpJsonDecoder.Decoded,"scoped producer wire: "+decoded);
            var input=((SpJsonDecoder.Decoded)decoded).input();
            var p=RegionalTranslationSuite.lower(input).publication().orElseThrow();
            check(p.units().getFirst().entries().getFirst().state().conditions().size()==1,"independent entry literal retained");
            if(!name.equals("nested-overlay"))continue;
            var st=input.storage().orElseThrow();var relation=st.relations().getFirst();
            var view=st.views().stream().filter(v->v.node().equals(relation.owner())).findFirst().orElseThrow();
            var bases=st.bases().stream().map(b->b.id().equals(view.base())?IfInputs.with(b,"extent",new StorageFacts.Measure(Optional.of(BigInteger.valueOf(8)),List.of())):b).toList();
            invalid(IfInputs.with(input,"storage",Optional.of(IfInputs.with(st,"bases",bases))),"invented component extent");
            var root=st.nodes().stream().filter(n->n.parent().isEmpty()&&st.views().stream().anyMatch(v->v.node().equals(n.id())&&v.base().equals(view.base()))).findFirst().orElseThrow();
            var unbounded=IfInputs.with(relation,"owner",root.id());
            invalid(IfInputs.with(input,"storage",Optional.of(IfInputs.with(st,"relations",List.of(unbounded)))),"unbounded relation cannot retain independent bases");
        }
        System.out.println("SCOPED_STORAGE_FIXTURES=3; HOSTILE_FACTS=2");
    }
    private static void invalid(SpInput input,String label) {
        check(new EntryGobackAdmission().admit(input,CobolLower.OPTIONS.admission()).status()==Admission.Status.INVALID_INPUT,label);
    }
}
