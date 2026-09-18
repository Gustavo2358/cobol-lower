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
            var n=nodes.get(v.node());require(n!=null,"logical node must be published and supported");
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
            } else require(v.start().signum()==0,"logical root must begin at zero");
        }
        for(var n:nodes.values())if(n.parent().filter(views::containsKey).isPresent()&&n.kind()!=StorageFacts.Kind.OPAQUE)require(views.containsKey(n.id()),"logical inventory cannot omit a child");
        for(var list:leaves.values()) {
            list.sort(Comparator.comparing(StorageFacts.LogicalTextView::start));var cursor=BigInteger.ZERO;
            for(var leaf:list){require(leaf.start().compareTo(cursor)<=0,"logical leaves must cover their root");cursor=cursor.max(end(leaf));}
            require(cursor.equals(views.get(list.getFirst().root()).length()),"logical leaf partition must cover root");
        }
        for(var s:input.storage().stream().toList()) {
            var physicalViews=new HashMap<StorageFacts.NodeId,StorageFacts.View>();s.views().forEach(v->physicalViews.put(v.node(),v));
            for(var v:views.values())require(physicalViews.get(v.node()).base().equals(physicalViews.get(v.root()).base()),"logical family requires canonical shared source identity");
            for(var r:s.relations())if(views.containsKey(r.owner())) {
                var owner=views.get(r.owner());var target=r.target().map(views::get).orElse(null);
                require(r.status()==StorageFacts.RelationStatus.PROVEN&&target!=null&&owner.root().equals(target.root())&&owner.start().equals(target.start()),"logical overlay must have proved shared start");
            }
            for(var r:s.renames())if(views.containsKey(r.owner())) {
                var owner=views.get(r.owner());var from=r.from().map(views::get).orElse(null);var through=r.through().map(views::get).orElse(from);
                require(from!=null&&through!=null&&from.root().equals(owner.root())&&through.root().equals(owner.root())&&owner.start().equals(from.start())&&end(owner).equals(end(through)),"logical RENAMES endpoints must match range");
            }
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
    List<StorageFacts.LogicalTextView> family(StorageFacts.LogicalTextView view) {
        return views.values().stream().filter(v->v.root().equals(view.root())).sorted(Comparator.comparing(v->v.node().handle())).toList();
    }
    boolean literalMove(SpInput.MoveFact move) {
        var target=move.target().logicalWholeItem().map(byData::get).orElse(null);
        if(target==null||!move.target().logicalWholeItem().equals(move.target().binding().selected())||!move.additionalTransfers().isEmpty())return false;
        if(move.source() instanceof SpInput.LiteralSource literal)return literal.logicalValue().isPresent();
        if(move.source() instanceof SpInput.DataReference source) {
            var from=source.logicalWholeItem().map(byData::get).orElse(null);
            return from!=null&&source.logicalWholeItem().equals(source.binding().selected())&&!from.root().equals(target.root());
        }
        return false;
    }
    static BigInteger end(StorageFacts.LogicalTextView v){return v.start().add(v.length());}
    private static void require(boolean condition,String message){if(!condition)throw new RegionalStorageAdmission.Invalid(message);}
}
