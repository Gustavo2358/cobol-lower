package io.github.gustavo2358.lower.application;

import java.util.*;
import io.github.gustavo2358.lower.domain.SpInput;
import io.github.gustavo2358.lower.domain.ControlTopology;
import static io.github.gustavo2358.lower.domain.ControlTopology.*;

/** Binds the published algebra only. No COBOL fact variants, source order,
 * paragraph discovery or language-specific completion rules. */
final class TopologyBinding {
    record Resolved(TargetKind kind,String reference,List<String> proofs) { }
    private final Map<String,Occurrence> occurrences=new HashMap<>();
    private final Map<String,Region> regions=new HashMap<>();
    private final Map<String,Boundary> boundaries=new HashMap<>();
    private final Map<String,Outcome> outcomes=new HashMap<>();
    private final Map<String,Binding> bindings=new HashMap<>();
    private final Map<String,Proof> proofs=new HashMap<>();
    TopologyBinding(ControlTopology topology) {
        topology.occurrences().forEach(x->occurrences.put(x.statement(),x));topology.regions().forEach(x->regions.put(x.id(),x));
        topology.boundaries().forEach(x->boundaries.put(x.id(),x));topology.outcomes().forEach(x->outcomes.put(x.id(),x));
        topology.bindings().forEach(x->bindings.put(x.id(),x));topology.proofs().forEach(x->proofs.put(x.id(),x));
    }
    List<Outcome> outcomes(String statement){return occurrences.get(statement).outcomes().stream().map(outcomes::get).toList();}
    Optional<Outcome> outcome(String statement,String role){return outcomes(statement).stream().filter(e->e.role().equals(role)).findFirst();}
    Binding binding(String id){return Objects.requireNonNull(bindings.get(id));}
    Proof proof(String id){return Objects.requireNonNull(proofs.get(id));}
    Optional<Resolved> primaryEntry() {
        var roots=regions.values().stream().filter(r->r.kind()==RegionKind.PROCEDURE&&r.parent().isEmpty()).toList();
        return roots.size()==1?Optional.of(resolve(roots.getFirst().entry(),null)):Optional.empty();
    }
    Resolved entry(Binding active){return resolve(regions.get(active.region()).entry(),active);}
    Resolved resolve(Target target,Binding active) {
        var premises=new LinkedHashSet<String>();var seen=new HashSet<String>();
        while(true) {
            premises.addAll(target.proofs());
            switch(target.kind()) {
                case REGION_ENTRY -> {if(!seen.add("entry:"+target.reference()))throw new IllegalArgumentException("cyclic topology entry");
                    var r=regions.get(target.reference());premises.addAll(r.proofs());target=r.entry();}
                case COMPLETE -> {if(!seen.add("completion:"+target.reference()))throw new IllegalArgumentException("cyclic topology completion");
                    var r=regions.get(target.reference());var b=boundaries.get(r.boundary());premises.addAll(b.proofs());
                    if(active!=null&&active.endpoint().equals(b.id())){premises.addAll(active.proofs());return new Resolved(TargetKind.COMPLETE,b.id(),List.copyOf(premises));}
                    target=b.ordinaryDefault();}
                default -> {return new Resolved(target.kind(),target.reference(),List.copyOf(premises));}
            }
        }
    }
    /** Reference closure of exactly the outcomes consumed by the materializer.
     * Invocation completion is a scheduling bound, not a bypass edge. */
    Set<String> closure(String entry,Binding active) {
        var seen=new LinkedHashSet<String>();var todo=new ArrayDeque<String>();todo.add(entry);
        while(!todo.isEmpty()) {
            String id=todo.removeFirst();if(!seen.add(id))continue;
            for(var e:outcomes(id)) {
                var target=e.kind()==OutcomeKind.LOCAL_INVOKE?binding(e.binding()).resume():e.target();
                var resolved=resolve(target,active);
                if(resolved.kind()==TargetKind.OCCURRENCE)todo.addLast(resolved.reference());
            }
        }
        return Set.copyOf(seen);
    }
    /** Capability check over the published graph. An unknown region localizes
     * missing control proof; it does not authorize a guessed set of AIR labels. */
    List<String> unavailableBounds(String entry) {
        record Work(String entry,Binding active) { }
        var pending=new ArrayDeque<Work>();pending.add(new Work(entry,null));
        var visited=new HashSet<String>();var gaps=new TreeSet<String>();
        while(!pending.isEmpty()) {
            var w=pending.removeFirst();String key=(w.active()==null?"ordinary":w.active().id())+"/"+w.entry();if(!visited.add(key))continue;
            for(var occurrence:closure(w.entry(),w.active()))for(var outcome:outcomes(occurrence)) {
                var resolved=resolve(outcome.target(),w.active());
                if(resolved.kind()==TargetKind.UNKNOWN_LOCAL)gaps.add(occurrence+"/"+resolved.reference());
                if(outcome.kind()==OutcomeKind.LOCAL_INVOKE) {
                    var b=binding(outcome.binding());var first=entry(b);
                    if(first.kind()==TargetKind.UNKNOWN_LOCAL)gaps.add(occurrence+"/"+first.reference());
                    else if(first.kind()==TargetKind.OCCURRENCE)pending.addLast(new Work(first.reference(),b));
                    var resume=resolve(b.resume(),w.active());if(resume.kind()==TargetKind.UNKNOWN_LOCAL)gaps.add(occurrence+"/"+resume.reference());
                }
            }
        }
        return List.copyOf(gaps);
    }
    static void validate(SpInput input,EntryGobackAdmission.Context c) {
        var topology=input.controlTopology().orElseThrow();var published=new HashSet<String>();
        for(var s:input.statements())published.add(s.header().id().handle());
        var supplied=new HashSet<String>();topology.occurrences().forEach(x->supplied.add(x.statement()));
        c.require(published.equals(supplied),Admission.Rule.STRUCTURE,"controlTopology",null,"topology inventory equals source occurrence inventory");
        var binder=new TopologyBinding(topology);
        var primary=binder.primaryEntry();
        c.require(input.statements().isEmpty()||primary.isPresent(),Admission.Rule.STRUCTURE,"controlTopology",null,"one topology procedure root required");
        if(primary.isPresent()&&primary.get().kind()==TargetKind.OCCURRENCE)
            for(var entry:input.entryInventory().entries())entry.start().statement().ifPresent(start->
                c.require(start.handle().equals(primary.get().reference()),Admission.Rule.STRUCTURE,"entry",null,
                    "entry metadata agrees with authoritative topology root"));
        for(var p:topology.proofs()){c.touch();c.provenance(p.provenance());}
        for(var e:topology.outcomes()){c.touch();binder.resolve(e.target(),null);}
        for(var b:topology.bindings()){c.touch();binder.resolve(b.resume(),null);binder.entry(b);}
        for(var use:input.fileInventory().operations().uses())use.control().ifPresent(control->{
            for(var route:control.routes())for(int i=0;i<route.destinations().size();i++)
                c.require(binder.outcome(use.statement().handle(),"file/"+use.ordinal()+"/"+route.event()+"/"+i).isPresent(),
                    Admission.Rule.STRUCTURE,use.statement().handle(),use.provenance(),"FILE destination belongs to topology inventory");
        });
    }
}
