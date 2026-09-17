package io.github.gustavo2358.lower.application;
import io.github.gustavo2358.lower.domain.SpInput;
import io.github.gustavo2358.lower.domain.StorageFacts;
import java.math.BigInteger;
import java.util.*;

/** Linearithmic proof that a published precise source cannot be changed by any receiver. */
final class RegionalTransferAdmission {
    private RegionalTransferAdmission() { }
    private record Intervals(List<BigInteger> starts,List<BigInteger> prefixEnds) {
        boolean intersects(StorageFacts.View read) {
            var end=end(read);int lo=0,hi=starts.size();
            while(lo<hi){int mid=(lo+hi)>>>1;if(starts.get(mid).compareTo(end)<0)lo=mid+1;else hi=mid;}
            return lo>0&&prefixEnds.get(lo-1).compareTo(read.offset().value().orElseThrow())>0;
        }
    }
    static void validate(SpInput.MoveFact move,RegionalStorageAdmission.Index storage) {
        if(move.additionalTransfers().isEmpty())return;
        require(move.transfers().stream().noneMatch(t->t.effect().kind()==StorageFacts.MoveKind.LOGICAL_FIT_TEXT),"logical source capture only admits one receiver");
        var writes=new HashMap<StorageFacts.BaseId,List<StorageFacts.View>>();
        for(var transfer:move.transfers()) {
            require(transfer.effect().kind()!=StorageFacts.MoveKind.UNAVAILABLE,"sequence cannot omit an unproved receiver");
            var view=storage.access(transfer.target()).orElseThrow();writes.computeIfAbsent(view.base(),unused->new ArrayList<>()).add(view);
        }
        var indexes=new HashMap<StorageFacts.BaseId,Intervals>();var unproved=new HashSet<StorageFacts.BaseId>();
        for(var entry:writes.entrySet()) {
            entry.getValue().sort(Comparator.comparing(v->v.offset().value().orElseThrow()));
            var starts=new ArrayList<BigInteger>();var ends=new ArrayList<BigInteger>();var max=BigInteger.ZERO;
            for(var view:entry.getValue()){starts.add(view.offset().value().orElseThrow());max=max.max(end(view));ends.add(max);}
            indexes.put(entry.getKey(),new Intervals(starts,ends));
            if(!storage.bases().get(entry.getKey()).allocation().proved())unproved.add(entry.getKey());
        }
        for(var transfer:move.transfers())if(transfer.effect().kind()==StorageFacts.MoveKind.COPY_BYTES||transfer.effect().kind()==StorageFacts.MoveKind.FIT_TEXT) {
            var read=storage.access((SpInput.DataReference)transfer.source()).orElseThrow();var same=indexes.get(read.base());
            require(same==null||!same.intersects(read),"a receiver can change a precise sequence source");
            boolean other=writes.size()>(writes.containsKey(read.base())?1:0);
            require(!other||storage.bases().get(read.base()).allocation().proved()
                &&unproved.size()==(unproved.contains(read.base())?1:0),"source separation from other bases is unproved");
        }
    }
    private static BigInteger end(StorageFacts.View view){return view.offset().value().orElseThrow().add(view.extent().value().orElseThrow());}
    private static void require(boolean condition,String message){if(!condition)throw new RegionalStorageAdmission.Invalid(message);}
}
