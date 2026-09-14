package io.github.gustavo2358.lower.adapters.testing;

import io.github.gustavo2358.air.model.*;
import io.github.gustavo2358.lower.adapters.cli.CobolLower;
import io.github.gustavo2358.lower.adapters.sp.SpJsonDecoder;
import java.util.*;
import static io.github.gustavo2358.lower.testing.CallOracle.check;

/** A known prefix does not establish the total extent of its physical Region. */
public final class UnknownEntryBoundsSuite {
    private UnknownEntryBoundsSuite() { }
    public static void main(String[] args) throws Exception {
        var bytes=Objects.requireNonNull(UnknownEntryBoundsSuite.class.getResourceAsStream("/sp/storage-212/unknown-entry-bounds.json")).readAllBytes();
        var decoded=new SpJsonDecoder(CobolLower.INPUT_LIMITS).decode(bytes);
        check(decoded instanceof SpJsonDecoder.Decoded,"typed source-produced unknown-entry fixture");
        var input=((SpJsonDecoder.Decoded)decoded).input();
        var storage=input.storage().orElseThrow();
        check(storage.bases().size()==1&&storage.bases().getFirst().extent().value().isEmpty(),"unknown total physical extent");
        check(storage.views().stream().anyMatch(v->v.offset().value().isPresent()&&v.extent().value().isPresent()),"known prefix view is not a bounds proof");
        check(!storage.entryState().conditions().isEmpty(),"entry facts must exercise the translator");
        var result=RegionalTranslationSuite.lower(input); // SUCCESS, AirValidator and semantic/byte JSON roundtrip.
        var publication=result.publication().orElseThrow();
        check(publication.storage().size()==1&&publication.storage().getFirst() instanceof Memory.Region r&&r.extent().isEmpty()&&r.extentUnknown().isPresent(),"one unknown Region retained, no invented extent");
        var unit=publication.units().getFirst();
        check(unit.objects().size()==input.dataDeclarations().size(),"all source objects retained");
        var state=unit.entries().getFirst().state();
        check(state.conditions().isEmpty(),"no total RegionSlice when base bounds are unproved");
        check(publication.uncertainties().stream().anyMatch(u->u.code().equals("cobol-lower:INITIAL_STORAGE_UNKNOWN")),"initial knowledge remains explicitly unknown");
        check(unit.sequences().stream().map(Sequence::terminator).filter(Operations.Invoke.class::isInstance).map(Operations.Invoke.class::cast).anyMatch(i->i.target() instanceof Interactions.LiteralTarget),"literal CALL survives the storage gap");
        System.out.println("UNKNOWN_ENTRY_BOUNDS=PASS unknown base/known prefix/retained objects/explicit gap/literal CALL/AIR wire");
    }
}
