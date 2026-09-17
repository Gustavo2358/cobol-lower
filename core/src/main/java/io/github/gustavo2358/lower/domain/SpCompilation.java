package io.github.gustavo2358.lower.domain;
import java.util.*;
import static io.github.gustavo2358.lower.domain.SpInput.*;
/** Published unit inventory, canonical parentage and explicit imports. No source lookup. */
public record SpCompilation(InventoryStatus inventoryStatus,List<UnitKey> unitInventory,List<UnitProduct> units){
    public SpCompilation{Objects.requireNonNull(inventoryStatus);unitInventory=List.copyOf(unitInventory);units=List.copyOf(units);}
    public record DataCapture(DataId localData,DataId sourceData){public DataCapture{Objects.requireNonNull(localData);Objects.requireNonNull(sourceData);}}
    public record UnitProduct(SpInput product,Optional<UnitKey> parent,List<DataId> ownedData,List<DataId> globalData,List<DataCapture> dataCaptures,List<FileFacts.Candidate> fileCaptures){
        public UnitProduct{Objects.requireNonNull(product);Objects.requireNonNull(parent);ownedData=List.copyOf(ownedData);globalData=List.copyOf(globalData);dataCaptures=List.copyOf(dataCaptures);fileCaptures=List.copyOf(fileCaptures);}
    }
}
