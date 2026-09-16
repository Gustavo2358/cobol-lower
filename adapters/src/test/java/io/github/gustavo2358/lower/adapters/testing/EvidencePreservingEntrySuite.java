package io.github.gustavo2358.lower.adapters.testing;

import io.github.gustavo2358.air.model.*;
import io.github.gustavo2358.air.json.AirJson;
import io.github.gustavo2358.lower.adapters.cli.CobolLower;
import io.github.gustavo2358.lower.adapters.sp.SpJsonDecoder;
import io.github.gustavo2358.lower.application.*;
import java.nio.file.*;
import java.util.*;
import static io.github.gustavo2358.lower.testing.CallOracle.check;

/** Actual producer fixtures: logical evidence must survive missing physical location. */
public final class EvidencePreservingEntrySuite {
    private EvidencePreservingEntrySuite() { }
    public static void main(String[] args) throws Exception {
        for(var name:List.of("unknown-offset","unknown-base","unknown-allocation","unknown-profile")) {
            var bytes=Objects.requireNonNull(EvidencePreservingEntrySuite.class.getResourceAsStream("/sp/evidence/"+name+".json")).readAllBytes();
            var decoded=new SpJsonDecoder(CobolLower.INPUT_LIMITS).decode(bytes);
            check(decoded instanceof SpJsonDecoder.Decoded,"source evidence contract: "+name+" "+decoded);
            var result=RegionalTranslationSuite.lower(((SpJsonDecoder.Decoded)decoded).input());
            check(result.publication().isPresent(),"source evidence must lower: "+name+" "+result.status()+" "+result.admission());
            var p=result.publication().orElseThrow();var unit=p.units().getFirst();
            check(!unit.entries().getFirst().state().conditions().isEmpty(),"supported entry fact was lost: "+name);
            var condition=unit.entries().getFirst().state().conditions().getFirst();
            check(condition.value() instanceof Entries.PossibleLiterals,"possible must remain possible");
            var literals=((Entries.PossibleLiterals)condition.value()).candidates().stream().map(Expressions.Literal::value).toList();
            check(literals.equals(List.of(new Values.TextValue("PROGA   ")))||literals.equals(List.of(new Values.BytesValue(List.of(215,217,214,199,193,64,64,64)))),"source candidate conserved: "+literals);
            check(p.capabilities().required().contains(new Capabilities.Capability("entry.possibilities","2")),"negotiated v2");
            var invoke=(Operations.Invoke)unit.sequences().stream().map(Sequence::terminator).filter(Operations.Invoke.class::isInstance).findFirst().orElseThrow();
            check(invoke.target() instanceof Interactions.ComputedTarget t&&t.name() instanceof Expressions.Read,"logical CALL read remains representable");
            if(!name.equals("unknown-allocation")) {
                check(condition.place() instanceof Places.ObjectPlace,"unknown physical view uses logical object");
                var object=((Places.ObjectPlace)condition.place()).object();
                check(unit.objects().stream().filter(o->o.id().equals(object)).allMatch(o->o.storage() instanceof Memory.UnknownBinding),"no invented physical binding");
                check(result.data().stream().filter(d->d.object().equals(object)).allMatch(d->d.storage().isEmpty()),"no invented storage identity");
            }
            var codec=new AirJson();check(p.equals(codec.decode(codec.encode(p))),"AIR codec preserves entry and target");
        }
        System.out.println("EP_ENTRY_LOWER=PASS source/entry/target/codec/no invented allocation");
    }
}
