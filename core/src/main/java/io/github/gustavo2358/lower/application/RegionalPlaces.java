package io.github.gustavo2358.lower.application;

import io.github.gustavo2358.air.model.*;
import io.github.gustavo2358.air.model.Ids.OperandId;
import io.github.gustavo2358.lower.domain.SpInput;
import java.math.BigInteger;

/** Translate a validated occurrence range without changing the declared object view. */
final class RegionalPlaces {
    private RegionalPlaces() { }
    static Memory.ViewBinding view(Memory.ViewBinding base,SpInput.DataReference reference) {
        return reference.regionalAccess().flatMap(a->a.slice()).map(s->new Memory.ViewBinding(base.region(),s.offset(),s.extent(),base.codec())).orElse(base);
    }
    static Place place(SpInput.DataReference reference,LoweringResult.DataLink link,Operand.Header header,LocalIds ids) {
        var slice=reference.regionalAccess().flatMap(a->a.slice());
        if(slice.isEmpty())return new Places.ObjectPlace(header,link.object());
        var s=slice.get();
        return new Places.RegionSlice(header,link.storage(),integer(s.offset(),"offset",header,ids),integer(s.extent(),"extent",header,ids),
            RegionalStorageAdmission.IBM1047,Types.known(Types.Builtin.TEXT));
    }
    private static Expression integer(BigInteger value,String kind,Operand.Header parent,LocalIds ids) {
        var id=new OperandId(parent.id().owner(),ids.id("operand","regional-slice-"+kind,parent.id().localId(),kind));
        return new Expressions.Literal(new Operand.Header(id,Operand.Role.VALUE_READ,parent.origin()),new Values.IntValue(value));
    }
}
