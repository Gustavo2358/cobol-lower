package io.github.gustavo2358.lower.application;

import java.util.*;
import io.github.gustavo2358.lower.domain.SpInput;
import io.github.gustavo2358.lower.domain.ControlTopology;
import static io.github.gustavo2358.lower.domain.ControlTopology.*;

/** Binds the published algebra only. No COBOL fact variants, source order,
 * paragraph discovery or language-specific completion rules. */
final class TopologyBinding {
    record Resolved(TargetKind kind,String reference,List<String> proofs) { }
    private final Map<String,FileFlow> fileFlows=new HashMap<>();
    private final Map<String,String> filePointOwners=new HashMap<>();
    private final Map<String,FilePoint> filePoints=new HashMap<>();
    private final Map<String,Occurrence> occurrences=new HashMap<>();
    private final Map<String,Region> regions=new HashMap<>();
    private final Map<String,Boundary> boundaries=new HashMap<>();
    private final Map<String,Outcome> outcomes=new HashMap<>();
    private final Map<String,Binding> bindings=new HashMap<>();
    private final Map<String,Proof> proofs=new HashMap<>();
    TopologyBinding(ControlTopology topology) {
        topology.fileFlows().forEach(f->{fileFlows.put(f.statement(),f);f.points().forEach(p->{filePoints.put(p.id(),p);filePointOwners.put(p.id(),f.statement());});});
        topology.occurrences().forEach(x->occurrences.put(x.statement(),x));topology.regions().forEach(x->regions.put(x.id(),x));
        topology.boundaries().forEach(x->boundaries.put(x.id(),x));topology.outcomes().forEach(x->outcomes.put(x.id(),x));
        topology.bindings().forEach(x->bindings.put(x.id(),x));topology.proofs().forEach(x->proofs.put(x.id(),x));
    }
    String filePointOwner(String id){return Objects.requireNonNull(filePointOwners.get(id));}
    FilePoint filePoint(String id){return Objects.requireNonNull(filePoints.get(id));}
    Optional<FileFlow> fileFlow(String statement){return Optional.ofNullable(fileFlows.get(statement));}
    Region region(String id) { return Objects.requireNonNull(regions.get(id)); }
    boolean inline(Binding binding) { return region(boundaries.get(binding.endpoint()).region()).kind()==RegionKind.INLINE_BODY; }
    boolean contains(Binding binding,String scope) {
        String current=scope;
        while(!current.isEmpty()) {
            if(regions.get(binding.region()).regions().contains(current))return true;
            current=region(current).parent();
        }
        return false;
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
    Resolved resolve(Target target,Binding active) { return resolve(target,active,false); }
    /** A handler ingress does not prove restoration of an interrupted PERFORM continuation. */
    Resolved resolve(Target target,Binding active,boolean boundedHandlerCompletion) {
        var premises=new LinkedHashSet<String>();var seen=new HashSet<String>();
        while(true) {
            premises.addAll(target.proofs());
            switch(target.kind()) {
                case REGION_ENTRY -> {if(!seen.add("entry:"+target.reference()))throw new IllegalArgumentException("cyclic topology entry");
                    var r=regions.get(target.reference());premises.addAll(r.proofs());target=r.entry();}
                case COMPLETE -> {if(!seen.add("completion:"+target.reference()))throw new IllegalArgumentException("cyclic topology completion");
                    var r=regions.get(target.reference());var b=boundaries.get(r.boundary());premises.addAll(b.proofs());
                    if(active!=null&&active.endpoint().equals(b.id())){premises.addAll(active.proofs());return new Resolved(TargetKind.COMPLETE,b.id(),List.copyOf(premises));}
                    if(boundedHandlerCompletion&&active==null&&r.kind()==RegionKind.PARAGRAPH)
                        return new Resolved(TargetKind.UNKNOWN_LOCAL,"HANDLER_COMPLETION_CONTEXT_UNAVAILABLE/"+b.id(),List.copyOf(premises));
                    target=b.ordinaryDefault();}
                default -> {return new Resolved(target.kind(),target.reference(),List.copyOf(premises));}
            }
        }
    }
    /** Reference closure of exactly the outcomes consumed by the materializer.
     * Invocation completion is a scheduling bound, not a bypass edge. */
    Set<String> closure(String entry,Binding active) {
        var seen=new LinkedHashSet<String>();var points=new HashSet<String>();
        var occurrences=new ArrayDeque<String>();occurrences.add(entry);var targets=new ArrayDeque<Target>();
        while(!occurrences.isEmpty()||!targets.isEmpty()) {
            if(!occurrences.isEmpty()) {
                String id=occurrences.removeFirst();if(!seen.add(id))continue;
                for(var e:outcomes(id))targets.add(e.kind()==OutcomeKind.LOCAL_INVOKE?binding(e.binding()).resume():e.target());
            } else {
                var resolved=resolve(targets.removeFirst(),active);
                if(resolved.kind()==TargetKind.OCCURRENCE)occurrences.addLast(resolved.reference());
                else if(resolved.kind()==TargetKind.FILE_POINT&&points.add(resolved.reference()))targets.addAll(filePoints.get(resolved.reference()).targets());
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
            for(var occurrence:closure(w.entry(),w.active())) {
                fileFlow(occurrence).ifPresent(flow->{for(var point:flow.points())for(var edge:point.targets()) {
                    var target=resolve(edge,w.active());if(target.kind()==TargetKind.UNKNOWN_LOCAL)gaps.add(occurrence+"/"+target.reference());
                }});
                for(var outcome:outcomes(occurrence)) {
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
            for(var entry:input.entryInventory().entries())if(entry.role()==SpInput.EntryRole.PRIMARY)entry.start().statement().ifPresent(start->
                c.require(start.handle().equals(primary.get().reference()),Admission.Rule.STRUCTURE,"entry",null,
                    "entry metadata agrees with authoritative topology root"));
        var entries=new HashMap<String,SpInput.EntryFact>();input.entryInventory().entries().forEach(e->entries.put(e.id().handle(),e));
        var suppliedEntries=new HashSet<String>();
        for(var point:topology.entryPoints()) {
            var e=entries.get(point.entry());suppliedEntries.add(point.entry());
            c.require(e!=null&&e.role()==SpInput.EntryRole.ALTERNATE&&e.availability()==SpInput.Availability.KNOWN,Admission.Rule.ENTRY_START,"entryPoints",null,"alternate root belongs to a known entry");
            if(e==null)continue;
            c.require(e.declaration().map(SpInput.StatementId::handle).orElse("").equals(point.declaration()),Admission.Rule.ENTRY_START,point.entry(),e.provenance(),"alternate declaration agrees");
            c.require(point.target().kind()==TargetKind.OCCURRENCE?e.start().statement().map(SpInput.StatementId::handle).orElse("").equals(point.target().reference()):e.start().statement().isEmpty(),Admission.Rule.ENTRY_START,point.entry(),e.provenance(),"alternate start agrees with topology");
        }
        for(var e:entries.values())if(e.role()==SpInput.EntryRole.ALTERNATE&&e.availability()==SpInput.Availability.KNOWN)
            c.require(suppliedEntries.contains(e.id().handle()),Admission.Rule.ENTRY_START,e.id().handle(),e.provenance(),"known alternate requires topology authority");
        for(var p:topology.proofs()){c.touch();c.provenance(p.provenance());}
        for(var e:topology.outcomes()){c.touch();binder.resolve(e.target(),null);}
        for(var b:topology.bindings()){c.touch();binder.resolve(b.resume(),null);binder.entry(b);}
        var facts=new HashMap<String,SpInput.StatementFact>();input.statements().forEach(s->facts.put(s.header().id().handle(),s));
        for(var binding:topology.bindings()) {
            var fact=facts.get(binding.caller());
            if(fact instanceof SpInput.ProcedurePerformFact p&&p.varying().filter(v->v.levels()>1&&!v.afterLoops().isEmpty()).isPresent())
                c.require(binding.phases().stream().allMatch(phase->phase.level()>0),Admission.Rule.STRUCTURE,binding.caller(),null,"multi-level phases explicitly select their payload");
        }
        for(var binding:topology.bindings())for(var phase:binding.phases())if(phase.level()>0) {
            var fact=facts.get(binding.caller());
            c.require(fact instanceof SpInput.ProcedurePerformFact p&&p.varying().isPresent()
                &&phase.level()<=p.varying().get().levels()&&p.varying().get().afterLoops().size()==p.varying().get().levels()-1,
                Admission.Rule.STRUCTURE,binding.caller(),null,"phase level has a typed control/predicate payload");
        }
        for(var event:topology.exceptionalEvents()) {
            var fact=facts.get(event.statement());
            boolean valid;
            if(event.origin()==EventOrigin.EXPLICIT_ABEND)valid=fact instanceof SpInput.CicsAbendFact abend
                &&abend.dispatchEligibility().name().equals(event.eligibility().name());
            else valid=fact instanceof SpInput.CicsFact command&&command.command()==(event.origin()==EventOrigin.LINK_PGMIDERR?SpInput.CicsCommand.LINK:SpInput.CicsCommand.XCTL)
                &&command.options().stream().allMatch(o->Set.of("PROGRAM","COMMAREA","LENGTH","RESP2").contains(o.name()))
                &&command.options().stream().filter(o->o.name().equals("PROGRAM")).count()==1
                &&command.options().stream().map(SpInput.CicsOption::name).distinct().count()==command.options().size()
                &&command.gapCodes().stream().allMatch(g->Set.of("CICS_EFFECTS_SIGNATURE_PARTIAL","CICS_HANDLER_STATE_UNKNOWN","CICS_HOST_BINDING_UNAVAILABLE").contains(g));
            c.require(valid,Admission.Rule.STRUCTURE,event.statement(),null,"exceptional event agrees with typed source fact");
            if(fact!=null)for(var proof:event.proofs())c.require(binder.proof(proof).provenance().equals(fact.header().provenance()),
                Admission.Rule.STRUCTURE,event.statement(),null,"exceptional event canonical source provenance");
        }
        for(var registration:topology.conditionRegistrations()) {
            var fact=facts.get(registration.statement());
            c.require(fact instanceof SpInput.OtherStatement observed&&observed.effects().map(e->e.proof()==SpInput.EffectProof.CICS_CONDITION_REGISTRATION).orElse(false),
                Admission.Rule.STRUCTURE,registration.statement(),null,"condition registration has typed bounded effects");
            if(fact!=null)for(var proof:registration.proofs())c.require(binder.proof(proof).provenance().equals(fact.header().provenance()),
                Admission.Rule.STRUCTURE,registration.statement(),null,"condition registration canonical source provenance");
        }
        for(var event:topology.conditionEvents()) {
            var fact=facts.get(event.statement());
            boolean valid=fact instanceof SpInput.CicsFact command
                &&command.options().stream().allMatch(o->Set.of("PROGRAM","COMMAREA","LENGTH","RESP","RESP2","NOHANDLE").contains(o.name()))
                &&command.options().stream().filter(o->o.name().equals("PROGRAM")).count()==1
                &&command.options().stream().map(SpInput.CicsOption::name).distinct().count()==command.options().size()
                &&command.gapCodes().stream().allMatch(g->Set.of("CICS_EFFECTS_SIGNATURE_PARTIAL","CICS_HANDLER_STATE_UNKNOWN","CICS_HOST_BINDING_UNAVAILABLE","CICS_CONDITION_VALUES_UNKNOWN").contains(g))
                &&(event.eligibility()==EventEligibility.HANDLERS_BYPASSED)==command.options().stream().anyMatch(o->Set.of("RESP","NOHANDLE").contains(o.name()));
            c.require(valid,Admission.Rule.STRUCTURE,event.statement(),null,"condition event agrees with typed command and bypass options");
            if(fact!=null)for(var proof:event.proofs())c.require(binder.proof(proof).provenance().equals(fact.header().provenance()),
                Admission.Rule.STRUCTURE,event.statement(),null,"condition event canonical source provenance");
        }
        var uses=new HashMap<String,List<io.github.gustavo2358.lower.domain.FileFacts.Use>>();
        input.fileInventory().operations().uses().forEach(u->uses.computeIfAbsent(u.statement().handle(),k->new ArrayList<>()).add(u));
        for(var flow:topology.fileFlows()) {
            var inventory=uses.getOrDefault(flow.statement(),List.of());
            var ordinals=new HashSet<Integer>();inventory.forEach(u->ordinals.add(u.ordinal()));
            var suppliedOrdinals=new HashSet<Integer>();flow.points().stream().filter(p->p.kind()==FilePointKind.USE).forEach(p->suppliedOrdinals.add(p.ordinal()));
            c.require(!inventory.isEmpty()&&ordinals.equals(suppliedOrdinals),Admission.Rule.STRUCTURE,flow.statement(),null,"FILE flow equals use inventory");
            var fact=facts.get(flow.statement());
            for(var point:flow.points())for(var proof:point.proofs())c.require(fact!=null&&binder.proof(proof).provenance().equals(fact.header().provenance()),
                Admission.Rule.STRUCTURE,flow.statement(),null,"FILE point canonical source provenance");
        }
        if(topology.authority().equals("FRONTEND_CONTROL_TOPOLOGY_R2"))for(var group:uses.entrySet())
            if(group.getValue().size()>1&&binder.outcomes(group.getKey()).stream().anyMatch(o->o.kind()!=OutcomeKind.UNKNOWN_LOCAL))
                c.require(binder.fileFlow(group.getKey()).isPresent(),Admission.Rule.STRUCTURE,group.getKey(),null,"composite FILE flow required");
        for(var use:input.fileInventory().operations().uses())use.control().ifPresent(control->{
            for(var route:control.routes())for(int i=0;i<route.destinations().size();i++)
                c.require(binder.outcome(use.statement().handle(),"file/"+use.ordinal()+"/"+route.event()+"/"+i).isPresent(),
                    Admission.Rule.STRUCTURE,use.statement().handle(),use.provenance(),"FILE destination belongs to topology inventory");
        });
    }
}
