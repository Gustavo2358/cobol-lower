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
    private final Map<SpInput.DataId,StorageFacts.LogicalTextView> local;
    private final Map<StorageFacts.NodeId,StorageFacts.LogicalTextView> localRoots;
    private final Set<SpInput.DataId> edited;
    private final Map<StorageFacts.NodeId,Set<String>> bounded;
    private final Map<StorageFacts.NodeId,Set<String>> locationsByNode=new HashMap<>();
    private final Map<StorageFacts.NodeId,List<StorageFacts.LogicalTextView>> leaves;
    private final Map<StorageFacts.NodeId,List<StorageFacts.LogicalTextView>> families;
    LogicalTextIndex(SpInput input,Map<StorageFacts.NodeId,StorageFacts.Node> nodes,FactDependencyIndex facts) {
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
        var declarations=new HashMap<SpInput.DataId,SpInput.DataFact>();input.dataDeclarations().forEach(d->declarations.put(d.id(),d));
        var bounded=new HashMap<StorageFacts.NodeId,Set<String>>();
        var local=new HashMap<SpInput.DataId,StorageFacts.LogicalTextView>();var localRoots=new HashMap<StorageFacts.NodeId,StorageFacts.LogicalTextView>();
        this.edited=input.dataDeclarations().stream().filter(d->d.scalarEdit().isPresent()).map(SpInput.DataFact::id).collect(java.util.stream.Collectors.toUnmodifiableSet());
        if(facts!=null)for(var node:nodes.values())if(node.data().isPresent()&&!byData.containsKey(node.data().orElseThrow())) {
            var declaration=declarations.get(node.data().orElseThrow());var binding=facts.bindings.get(node.id().handle());
            if(declaration==null||declaration.scalarText().isEmpty()||binding==null||!facts.texts.contains(node.id().handle())||!facts.available(binding.dependencies()))continue;
            var root=binding.exactCell().isEmpty()?node.id():new StorageFacts.NodeId(input.unit(),binding.exactCell());
            if(binding.exactCell().isEmpty()) {
                var locations=new HashSet<>(binding.cells());locations.addAll(binding.regions());
                if(locations.isEmpty())continue;
                bounded.put(node.id(),Set.copyOf(locations));
            }
            var length=BigInteger.valueOf(declaration.scalarText().orElseThrow().logicalExtent());
            local.put(declaration.id(),new StorageFacts.LogicalTextView(node.id(),root,BigInteger.ZERO,length));
            localRoots.put(root,new StorageFacts.LogicalTextView(root,root,BigInteger.ZERO,length));
        }
        // Every family root has a canonical bound; bounded local views do not
        // assert a disjoint family merely because their nominal roots differ.
        if(facts!=null)for(var node:nodes.values()) {
            var binding=facts.bindings.get(node.id().handle());
            if(binding!=null&&facts.available(binding.dependencies())) {
                var locations=new HashSet<>(binding.cells());locations.addAll(binding.regions());
                if(!locations.isEmpty()&&!bounded.containsKey(node.id()))locationsByNode.put(node.id(),Set.copyOf(locations));
            }
        }
        locationsByNode.putAll(bounded);this.bounded=Map.copyOf(bounded);
        this.local=Map.copyOf(local);this.localRoots=Map.copyOf(localRoots);
        var grouped = new HashMap<StorageFacts.NodeId,List<StorageFacts.LogicalTextView>>();
        views.values().forEach(v -> grouped.computeIfAbsent(v.root(), k -> new ArrayList<>()).add(v));
        var families = new HashMap<StorageFacts.NodeId,List<StorageFacts.LogicalTextView>>();
        grouped.forEach((root, family) -> families.put(root, family.stream()
                .sorted(Comparator.comparing(v -> v.node().handle())).toList()));
        this.families = Map.copyOf(families);
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
        return families.getOrDefault(view.root(), List.of());
    }
    private StorageFacts.LogicalTextView whole(SpInput.DataId id){return byData.containsKey(id)?byData.get(id):local.get(id);}
    StorageFacts.LogicalTextView root(StorageFacts.LogicalTextView view){return views.containsKey(view.root())?views.get(view.root()):localRoots.get(view.root());}
    void validate(SpInput.DataReference reference) {
        reference.logicalSlice().ifPresent(slice->{
            var whole=whole(slice.data());
            require(whole!=null&&reference.binding().status()==SpInput.ResolutionStatus.RESOLVED
                &&reference.binding().selected().equals(Optional.of(slice.data()))
                &&reference.wholeItemAccess().isEmpty()&&reference.logicalWholeItem().isEmpty()
                &&slice.start().signum()>=0&&slice.length().signum()>0
                &&slice.start().add(slice.length()).compareTo(whole.length())<=0,
                "logical slice requires a uniquely selected complete character view and in-bounds interval");
        });
    }
    StorageFacts.LogicalTextView access(SpInput.DataReference reference) {
        if(reference.logicalSlice().isEmpty())return reference.logicalWholeItem().map(this::whole).orElse(null);
        var slice=reference.logicalSlice().orElseThrow();var whole=whole(slice.data());
        return whole==null?null:new StorageFacts.LogicalTextView(whole.node(),whole.root(),whole.start().add(slice.start()),slice.length());
    }
    boolean literalMove(SpInput.MoveFact move) {
        var target=access(move.target());
        if(target==null||!move.additionalTransfers().isEmpty())return false;
        if(move.target().logicalSlice().isEmpty()&&move.target().binding().selected().filter(edited::contains).isPresent()
                &&move.copySemantics()!=SpInput.CopySemantics.FORMATTED_NUMBER)return false;
        if(move.source() instanceof SpInput.LiteralSource literal)return literal.logicalValue().isPresent()||literal.kind().collatingFill();
        if(move.source() instanceof SpInput.DataReference source) {
            var from=access(source);
            if(from!=null&&(bounded.containsKey(from.node())||bounded.containsKey(target.node()))) {
                var a=locationsByNode.get(from.node());var b=locationsByNode.get(target.node());
                return a!=null&&b!=null&&Collections.disjoint(a,b);
            }
            return from!=null&&(!from.root().equals(target.root())||end(from).compareTo(target.start())<=0||end(target).compareTo(from.start())<=0);
        }
        return false;
    }
    boolean sequence(SpInput.MoveFact move) {
        if(move.additionalTransfers().isEmpty())return literalMove(move);
        // A partially admitted sequence must not reread a source after a receiver
        // with unknown aliases. A literal has no such capture obligation.
        boolean literal=move.source() instanceof SpInput.LiteralSource l&&(l.logicalValue().isPresent()||l.kind().collatingFill());
        boolean any=false;
        for(var transfer:move.transfers()) {
            var single=new SpInput.MoveFact(move.header(),transfer.source(),transfer.target(),
                SpInput.CopySemantics.UNAVAILABLE,move.normalContinuation(),Optional.empty(),Optional.of(transfer.effect()));
            boolean supported=literalMove(single);any|=supported;
            if(!literal&&!supported)return false;
        }
        return any;
    }
    static BigInteger end(StorageFacts.LogicalTextView v){return v.start().add(v.length());}
    private static void require(boolean condition,String message){if(!condition)throw new RegionalStorageAdmission.Invalid(message);}
}
