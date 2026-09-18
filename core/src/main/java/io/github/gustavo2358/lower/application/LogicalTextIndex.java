package io.github.gustavo2358.lower.application;

import io.github.gustavo2358.lower.domain.SpInput;
import io.github.gustavo2358.lower.domain.StorageFacts;
import java.math.BigInteger;
import java.util.*;

/** Validates published character coordinates; never calculates COBOL layout. */
final class LogicalTextIndex {
    final Map<StorageFacts.NodeId,StorageFacts.LogicalTextView> views;
    final Map<SpInput.DataId,StorageFacts.LogicalTextView> byData;
    final Map<StorageFacts.NodeId,StorageFacts.Node> nodes;
    private final Map<StorageFacts.NodeId,List<StorageFacts.LogicalTextView>> leaves;
    LogicalTextIndex(SpInput input,Map<StorageFacts.NodeId,StorageFacts.Node> nodes) {
        this.nodes=nodes;
        input.storage().filter(s->!s.logicalTextViews().isEmpty()).ifPresent(s->require(s.profile()==StorageFacts.Profile.UNSPECIFIED,"logical text W1 cannot select physical profile"));
        var views=new LinkedHashMap<StorageFacts.NodeId,StorageFacts.LogicalTextView>();
        var byData=new HashMap<SpInput.DataId,StorageFacts.LogicalTextView>();
        var leaves=new HashMap<StorageFacts.NodeId,List<StorageFacts.LogicalTextView>>();
        for(var v:input.storage().stream().flatMap(s->s.logicalTextViews().stream()).toList()) {
            require(v.node()!=null&&v.root()!=null&&v.start()!=null&&v.length()!=null,"missing logical coordinate");
            require(v.start().signum()>=0&&v.length().signum()>0&&views.putIfAbsent(v.node(),v)==null,"invalid/duplicate logical coordinate");
            var n=nodes.get(v.node());require(n!=null&&n.kind()!=StorageFacts.Kind.OPAQUE,"logical node must be published and supported");
            n.data().ifPresent(d->byData.put(d,v));
            if(n.kind()==StorageFacts.Kind.ELEMENTARY)leaves.computeIfAbsent(v.root(),k->new ArrayList<>()).add(v);
        }
        for(var v:views.values()) {
            var root=views.get(v.root());var n=nodes.get(v.node());
            require(root!=null&&root.node().equals(root.root())&&root.start().signum()==0&&nodes.get(root.node()).parent().isEmpty(),"logical root must be explicit");
            require(end(v).compareTo(root.length())<=0,"logical view exceeds root");
            if(n.parent().isPresent()) {
                var parent=views.get(n.parent().get());
                require(parent!=null&&parent.root().equals(v.root())&&parent.start().compareTo(v.start())<=0&&end(v).compareTo(end(parent))<=0,"logical parent closure required");
            } else require(v.node().equals(v.root()),"logical root identity mismatch");
        }
        for(var n:nodes.values())if(n.parent().filter(views::containsKey).isPresent())require(views.containsKey(n.id()),"logical inventory cannot omit a child");
        for(var list:leaves.values()) {
            list.sort(Comparator.comparing(StorageFacts.LogicalTextView::start));var cursor=BigInteger.ZERO;
            for(var leaf:list){require(leaf.start().equals(cursor),"logical leaves must partition their root");cursor=end(leaf);}
            require(cursor.equals(views.get(list.getFirst().root()).length()),"logical leaf partition must cover root");
        }
        for(var s:input.storage().stream().toList()) {
            for(var r:s.relations())require(!views.containsKey(r.owner())&&r.target().filter(views::containsKey).isEmpty(),"W1 excludes aliases");
            for(var r:s.renames())require(!views.containsKey(r.owner())&&r.from().filter(views::containsKey).isEmpty()&&r.through().filter(views::containsKey).isEmpty(),"W1 excludes RENAMES");
        }
        this.views=Map.copyOf(views);this.byData=Map.copyOf(byData);
        var frozen=new HashMap<StorageFacts.NodeId,List<StorageFacts.LogicalTextView>>();leaves.forEach((k,v)->frozen.put(k,List.copyOf(v)));this.leaves=Map.copyOf(frozen);
    }
    List<StorageFacts.LogicalTextView> leaves(StorageFacts.LogicalTextView view) {
        var all=leaves.getOrDefault(view.root(),List.of());int lo=0,hi=all.size();
        while(lo<hi){int mid=(lo+hi)>>>1;if(all.get(mid).start().compareTo(view.start())<0)lo=mid+1;else hi=mid;}
        var result=new ArrayList<StorageFacts.LogicalTextView>();
        for(int i=lo;i<all.size()&&all.get(i).start().compareTo(end(view))<0;i++)result.add(all.get(i));
        return result;
    }
    boolean literalMove(SpInput.MoveFact move) {
        boolean admitted=move.source() instanceof SpInput.LiteralSource literal&&literal.logicalValue().isPresent()
            &&move.target().binding().selected().filter(byData::containsKey).isPresent()
            &&move.target().logicalWholeItem().equals(move.target().binding().selected())&&move.additionalTransfers().isEmpty();
        return admitted&&leaves(byData.get(move.target().logicalWholeItem().orElseThrow())).stream().anyMatch(v->nodes.get(v.node()).data().isPresent());
    }
    static BigInteger end(StorageFacts.LogicalTextView v){return v.start().add(v.length());}
    private static void require(boolean condition,String message){if(!condition)throw new RegionalStorageAdmission.Invalid(message);}
}
