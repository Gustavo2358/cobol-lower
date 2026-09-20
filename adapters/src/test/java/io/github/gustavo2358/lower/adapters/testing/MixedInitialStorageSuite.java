package io.github.gustavo2358.lower.adapters.testing;
import io.github.gustavo2358.air.model.*;
import io.github.gustavo2358.lower.adapters.cli.CobolLower;
import io.github.gustavo2358.lower.adapters.sp.SpJsonDecoder;
import java.util.*;
import static io.github.gustavo2358.lower.testing.CallOracle.check;
public final class MixedInitialStorageSuite {
    private MixedInitialStorageSuite() { }
    public static void main(String[] args) throws Exception {
        var bytes=Objects.requireNonNull(MixedInitialStorageSuite.class.getResourceAsStream("/sp/storage-212/mixed-entry.json")).readAllBytes();
        var decoded=new SpJsonDecoder(CobolLower.INPUT_LIMITS).decode(bytes);check(decoded instanceof SpJsonDecoder.Decoded,"typed mixed SP2.12");
        var input=((SpJsonDecoder.Decoded)decoded).input();var result=RegionalTranslationSuite.lower(input);var p=result.publication().orElseThrow();
        check(p.storage().size()==4,"one allocation per component, aliases never allocate twice");
        check(p.storage().stream().filter(Memory.Cell.class::isInstance).count()==1,"one legacy integer Cell");
        check(p.storage().stream().filter(s->s instanceof Memory.Region r&&r.extent().isPresent()).count()==1,"known group Region");
        check(p.storage().stream().filter(s->s instanceof Memory.Region r&&r.extent().isEmpty()&&r.extentUnknown().isPresent()).count()==2,"OCCURS/NATIONAL unknown Regions retained");
        check(p.units().getFirst().objects().size()==input.dataDeclarations().size(),"unsupported declaration objects cannot disappear");
        check(p.units().getFirst().entries().getFirst().state().conditions().size()==1,"known child VALUE retained");
        check(p.premises().isEmpty(),"positive source bases require no generated disjoint premise");
        check(p.units().getFirst().objects().stream().filter(o->o.storage() instanceof Memory.UnknownBinding&&o.typeRef() instanceof Types.UnknownType).count()==3,"unsupported objects retain unknown association/type without precise handler admission");
        check(!p.uncertainties().isEmpty(),"unsupported storage uncertainty survives");
        check(p.units().getFirst().sequences().stream().map(Sequence::terminator).filter(Operations.Invoke.class::isInstance).count()==3,"both computed and literal calls survive mixed storage");
        System.out.println("MIXED_STORAGE=PASS 1 Cell/1 known Region/2 unknown Regions/alias identity/entry/literal call/explicit separation");
    }
}
