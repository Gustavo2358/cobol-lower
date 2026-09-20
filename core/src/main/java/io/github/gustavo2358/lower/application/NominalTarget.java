package io.github.gustavo2358.lower.application;
import io.github.gustavo2358.air.model.*;
import io.github.gustavo2358.lower.domain.SpInput;
/** Preserve explicit whole-reference evidence without manufacturing a storage allocation. */
final class NominalTarget {
    private NominalTarget() { }
    static boolean available(SpInput.DataReference ref,ScalarDataTranslator.Result data) {
        return ref.regionalAccess().isPresent()&&ref.binding().selected().filter(data.index()::containsKey).isPresent()
            ||whole(ref).filter(data.nominal()::containsKey).isPresent();
    }
    private static java.util.Optional<SpInput.DataId> whole(SpInput.DataReference ref) {
        return ref.logicalWholeItem().or(()->ref.wholeItemAccess().map(SpInput.WholeItemAccess::data));
    }
    static Place place(SpInput.DataReference ref,ScalarDataTranslator.Result data,Operand.Header header,LocalIds ids) {
        if(ref.regionalAccess().isPresent()&&ref.binding().selected().filter(data.index()::containsKey).isPresent())
            return RegionalPlaces.place(ref,data.index().get(ref.binding().selected().orElseThrow()),header,ids);
        return new Places.ObjectPlace(header,data.nominal().get(whole(ref).orElseThrow()));
    }
}
