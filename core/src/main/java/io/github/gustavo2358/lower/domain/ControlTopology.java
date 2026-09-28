package io.github.gustavo2358.lower.domain;

import java.util.*;
import static io.github.gustavo2358.lower.domain.SpInput.Provenance;

/** Publication-local control facts. Completion is a region outcome, never a
 * pre-bound successor. This model contains no execution contexts or value facts. */
public record ControlTopology(String authority, List<Occurrence> occurrences,
        List<Region> regions, List<Boundary> boundaries, List<Outcome> outcomes,
        List<Binding> bindings, List<Proof> proofs, List<ExceptionalEvent> exceptionalEvents, List<FileFlow> fileFlows, List<SourceContinuation> sourceContinuations,List<EntryPoint> entryPoints,List<ConditionRegistration> conditionRegistrations,List<ConditionEvent> conditionEvents) {
    public ControlTopology(String authority,List<Occurrence> occurrences,List<Region> regions,List<Boundary> boundaries,List<Outcome> outcomes,List<Binding> bindings,List<Proof> proofs,List<ExceptionalEvent> events,List<FileFlow> fileFlows,List<SourceContinuation> sourceContinuations,List<EntryPoint> entryPoints) {
        this(authority,occurrences,regions,boundaries,outcomes,bindings,proofs,events,fileFlows,sourceContinuations,entryPoints,List.of(),List.of());
    }
    public enum ConditionAction { LABEL, DEFAULT, IGNORE }
    public record ConditionRegistration(String statement,String condition,ConditionAction action,List<Target> target,List<String> proofs) {
        public ConditionRegistration {text(statement);text(condition);Objects.requireNonNull(action);target=List.copyOf(target);proofs=sorted(nonempty(proofs),x->x);require(action==ConditionAction.LABEL?target.size()==1:target.isEmpty(),"condition registration target");}
        public String identity(){return statement+"/"+condition;}
    }
    /** Failure is a runtime alternative, not a proved occurrence of the condition. */
    public record ConditionEvent(String id,String statement,String condition,EventEligibility eligibility,Target continuation,String defaultEvent,List<String> proofs) {
        public ConditionEvent {text(id);text(statement);require(condition.equals("PGMIDERR"),"admitted condition event");Objects.requireNonNull(eligibility);Objects.requireNonNull(continuation);Objects.requireNonNull(defaultEvent);proofs=sorted(nonempty(proofs),x->x);require(eligibility==EventEligibility.HANDLERS_BYPASSED?defaultEvent.isEmpty():!defaultEvent.isEmpty(),"condition default event");}
    }
    public ControlTopology(String authority,List<Occurrence> occurrences,List<Region> regions,List<Boundary> boundaries,List<Outcome> outcomes,List<Binding> bindings,List<Proof> proofs,List<ExceptionalEvent> events,List<FileFlow> fileFlows,List<SourceContinuation> sourceContinuations) {
        this(authority,occurrences,regions,boundaries,outcomes,bindings,proofs,events,fileFlows,sourceContinuations,List.of());
    }
    /** An external activation, never a successor of the primary entry. */
    public record EntryPoint(String entry,String declaration,Target target,List<String> proofs) {
        public EntryPoint {text(entry);text(declaration);Objects.requireNonNull(target);proofs=sorted(nonempty(proofs),x->x);}
    }
    public ControlTopology(String authority,List<Occurrence> occurrences,List<Region> regions,
            List<Boundary> boundaries,List<Outcome> outcomes,List<Binding> bindings,List<Proof> proofs,List<ExceptionalEvent> events,List<FileFlow> fileFlows) {
        this(authority,occurrences,regions,boundaries,outcomes,bindings,proofs,events,fileFlows,List.of());
    }
    /** Non-executable source hypothesis; never an outcome of the executable topology. */
    public record SourceContinuation(String statement,Target target,List<String> proofs,List<String> prerequisites) {
        public SourceContinuation(String statement,Target target,List<String> proofs){this(statement,target,proofs,List.of());}
        public SourceContinuation {text(statement);Objects.requireNonNull(target);proofs=sorted(nonempty(proofs),x->x);prerequisites=prerequisites==null?List.of():List.copyOf(prerequisites);require(prerequisites.size()<=1,"source prerequisite arity");}
        public String identity(){return statement+"/"+target.kind()+"/"+target.reference()+"/"+String.join("/",prerequisites);}
    }
    public ControlTopology(String authority,List<Occurrence> occurrences,List<Region> regions,
            List<Boundary> boundaries,List<Outcome> outcomes,List<Binding> bindings,List<Proof> proofs,List<ExceptionalEvent> events) {
        this(authority,occurrences,regions,boundaries,outcomes,bindings,proofs,events,List.of());
    }
    /** Historical contracts have no event ingress authority. */
    public ControlTopology(String authority,List<Occurrence> occurrences,List<Region> regions,
            List<Boundary> boundaries,List<Outcome> outcomes,List<Binding> bindings,List<Proof> proofs) {
        this(authority,occurrences,regions,boundaries,outcomes,bindings,proofs,List.of());
    }
    public enum FilePointKind { USE, CHOICE }
    /** USE's target is its ordinary continuation; event routes remain separate outcomes.
     * CHOICE publishes a finite overapproximation of aggregate participant order/count. */
    public record FilePoint(String id,FilePointKind kind,int ordinal,List<Target> targets,List<String> proofs) {
        public FilePoint {
            text(id);Objects.requireNonNull(kind);targets=List.copyOf(nonempty(targets));proofs=sorted(nonempty(proofs),x->x);
            require(kind==FilePointKind.USE?ordinal>=0&&targets.size()==1:ordinal==-1,"FILE point payload");
            require(targets.stream().map(t->t.kind()+"/"+t.reference()).distinct().count()==targets.size(),"duplicate FILE point target");
        }
    }
    public record FileFlow(String statement,Target entry,List<FilePoint> points,List<String> proofs) {
        public FileFlow {text(statement);Objects.requireNonNull(entry);points=sorted(nonempty(points),FilePoint::id);proofs=sorted(nonempty(proofs),x->x);}
    }
    public enum EventOrigin { EXPLICIT_ABEND, XCTL_PGMIDERR, LINK_PGMIDERR }
    public enum EventEligibility { HANDLER_ELIGIBLE, HANDLERS_BYPASSED }
    public enum EventPremise { CONDITION_RAISED, DEFAULT_DISPOSITION_APPLIES }
    /** A conditional source relation, never a statically selected handler edge. */
    public record ExceptionalEvent(String id,String statement,EventOrigin origin,
            String disposition,EventEligibility eligibility,String scope,String runtimeIdentity,
            List<EventPremise> premises,List<String> proofs) {
        public ExceptionalEvent {
            text(id);text(statement);Objects.requireNonNull(origin);Objects.requireNonNull(eligibility);
            require("TASK_ABEND".equals(disposition),"event disposition");
            require("CURRENT_EXECUTION_LOGICAL_LEVEL".equals(scope)&&"UNAVAILABLE".equals(runtimeIdentity),"relative event scope");
            premises=List.copyOf(premises);proofs=sorted(nonempty(proofs),x->x);
            require(origin==EventOrigin.EXPLICIT_ABEND?premises.isEmpty():
                premises.equals(List.of(EventPremise.CONDITION_RAISED,EventPremise.DEFAULT_DISPOSITION_APPLIES))
                    &&eligibility==EventEligibility.HANDLER_ELIGIBLE,"event guard/origin agreement");
        }
    }
    public enum RegionKind { PROCEDURE, SECTION, PARAGRAPH, RANGE, IF, IF_ARM, EVALUATE, EVALUATE_ARM, FILE, FILE_HANDLER, INLINE_BODY, DECLARATIVE, SENTENCE, SEARCH, SEARCH_ARM }
    public enum TargetKind { FILE_POINT, OCCURRENCE, REGION_ENTRY, COMPLETE, ESCAPE, PROGRAM_RETURN, PROGRAM_HALT, UNKNOWN_LOCAL }
    public enum OutcomeKind { NORMAL, BRANCH, EXPLICIT_TRANSFER, LOCAL_INVOKE, PROGRAM_RETURN, PROGRAM_HALT, UNKNOWN_LOCAL }
    public enum PhaseKind { PREDICATE, EFFECT }
    public record PhaseEdge(String role,String target) { public PhaseEdge {text(role);text(target);} }
    public record Phase(String id,PhaseKind kind,String operation,List<PhaseEdge> edges,List<String> proofs,int level) {
        public Phase(String id,PhaseKind kind,String operation,List<PhaseEdge> edges,List<String> proofs) {this(id,kind,operation,edges,proofs,0);}
        public Phase {require(level>=0,"nonnegative phase level");text(id);Objects.requireNonNull(kind);text(operation);edges=sorted(nonempty(edges),PhaseEdge::role);proofs=sorted(nonempty(proofs),x->x);}
    }
    public enum ProofKind { LOCAL_GRAMMAR, RESOLVED_TARGET, EXPANDED_INCLUDE, INPUT_REGION_ISOLATION, PARTIAL_UNKNOWN, CONTROL_POSSIBILITY }
    public record Target(TargetKind kind, String reference, List<String> proofs) {
        public Target { Objects.requireNonNull(kind); text(reference); proofs=sorted(nonempty(proofs),x->x); }
    }
    public record Occurrence(String statement, String region, List<String> outcomes, List<String> proofs) {
        public Occurrence { text(statement);text(region);outcomes=sorted(nonempty(outcomes),x->x);proofs=sorted(nonempty(proofs),x->x); }
    }
    public record Region(String id, RegionKind kind, String parent, Target entry,
            List<String> members, List<String> regions, String boundary, List<String> proofs) {
        public Region { text(id);Objects.requireNonNull(kind);Objects.requireNonNull(parent);Objects.requireNonNull(entry);
            members=sorted(members,x->x);regions=kind==RegionKind.RANGE?List.copyOf(regions):sorted(regions,x->x);text(boundary);proofs=sorted(nonempty(proofs),x->x); }
    }
    public record Boundary(String id, String region, Target ordinaryDefault, List<String> proofs) {
        public Boundary {text(id);text(region);Objects.requireNonNull(ordinaryDefault);proofs=sorted(nonempty(proofs),x->x);}
    }
    public record Outcome(String id, String statement, OutcomeKind kind, String role, Target target,
            String binding, List<String> proofs) {
        public Outcome {text(id);text(statement);Objects.requireNonNull(kind);text(role);Objects.requireNonNull(target);
            Objects.requireNonNull(binding);proofs=sorted(nonempty(proofs),x->x);}
    }
    /** Policy for repeating this binding while its invocation remains active. */
    public enum ReentryPolicy { UNSPECIFIED, SOURCE_UNDEFINED }
    public record Binding(String id, String caller, String region, String endpoint, Target resume, String entryPhase, String completionPhase, List<Phase> phases, List<String> proofs, ReentryPolicy reentryPolicy) {
        public Binding {Objects.requireNonNull(reentryPolicy);text(id);text(caller);text(region);text(endpoint);Objects.requireNonNull(resume);text(entryPhase);text(completionPhase);phases=sorted(phases,Phase::id);proofs=sorted(nonempty(proofs),x->x);}
        public Binding(String id, String caller, String region, String endpoint, Target resume, String entryPhase, String completionPhase, List<Phase> phases, List<String> proofs) {
            this(id, caller, region, endpoint, resume, entryPhase, completionPhase, phases, proofs, ReentryPolicy.UNSPECIFIED);
        }
    }
    public record Proof(String id, ProofKind kind, String rule, Provenance provenance, List<String> dependencies) {
        public Proof {text(id);Objects.requireNonNull(kind);text(rule);Objects.requireNonNull(provenance);dependencies=sorted(dependencies,x->x);}
    }
    public ControlTopology {
        if(!Set.of("FRONTEND_CONTROL_TOPOLOGY_R1","FRONTEND_CONTROL_TOPOLOGY_R2").contains(authority))throw new IllegalArgumentException("control topology authority");
        occurrences=sorted(occurrences,Occurrence::statement);regions=sorted(regions,Region::id);boundaries=sorted(boundaries,Boundary::id);
        outcomes=sorted(outcomes,Outcome::id);bindings=sorted(bindings,Binding::id);proofs=sorted(proofs,Proof::id);
        exceptionalEvents=sorted(exceptionalEvents==null?List.of():exceptionalEvents,ExceptionalEvent::id);
        index(exceptionalEvents,ExceptionalEvent::id);
        fileFlows=sorted(fileFlows==null?List.of():fileFlows,FileFlow::statement);
        require("FRONTEND_CONTROL_TOPOLOGY_R2".equals(authority)==!fileFlows.isEmpty(),"FILE flow authority/inventory");
        index(fileFlows,FileFlow::statement);
        sourceContinuations=sorted(sourceContinuations==null?List.of():sourceContinuations,SourceContinuation::identity);
        index(sourceContinuations,SourceContinuation::identity);
        entryPoints=sorted(entryPoints==null?List.of():entryPoints,EntryPoint::entry);index(entryPoints,EntryPoint::entry);
        conditionRegistrations=sorted(conditionRegistrations==null?List.of():conditionRegistrations,ConditionRegistration::identity);index(conditionRegistrations,ConditionRegistration::identity);
        conditionEvents=sorted(conditionEvents==null?List.of():conditionEvents,ConditionEvent::id);index(conditionEvents,ConditionEvent::id);
        var fps=index(fileFlows.stream().flatMap(f->f.points().stream()).toList(),FilePoint::id);
        var owners=new HashMap<String,String>();fileFlows.forEach(f->f.points().forEach(p->owners.put(p.id(),f.statement())));
        var os=index(occurrences,Occurrence::statement);var rs=index(regions,Region::id);var bs=index(boundaries,Boundary::id);
        var es=index(outcomes,Outcome::id);var calls=index(bindings,Binding::id);var ps=index(proofs,Proof::id);
        for(var e:exceptionalEvents) {
            require(os.containsKey(e.statement()),"exceptional source occurrence");refs(e.proofs(),ps);
            String rule=e.origin()==EventOrigin.EXPLICIT_ABEND?"cics-explicit-abend-event":e.origin()==EventOrigin.LINK_PGMIDERR?"cics-link-pgmiderr-default-abend-event":"cics-xctl-pgmiderr-default-abend-event";
            require(e.proofs().stream().anyMatch(id->ps.get(id).kind()==ProofKind.LOCAL_GRAMMAR&&ps.get(id).rule().equals(rule)),"exceptional event authority");
        }
        require(exceptionalEvents.stream().map(e->e.statement()+"/"+e.origin()).distinct().count()==exceptionalEvents.size(),"duplicate event source/origin");
        for(var p:proofs) {refs(p.dependencies(),ps);var pending=new ArrayDeque<String>();pending.add(p.id());var visited=new HashSet<String>();
            while(!pending.isEmpty()){var next=pending.removeFirst();if(!visited.add(next))continue;for(var dependency:ps.get(next).dependencies()){
                require(!dependency.equals(p.id()),"proof dependency cycle");pending.addLast(dependency);}}
        }
        java.util.function.Consumer<Target> target=t->{refs(t.proofs(),ps);switch(t.kind()) {
            case FILE_POINT -> require(fps.containsKey(t.reference()),"target FILE point");
            case OCCURRENCE -> require(os.containsKey(t.reference()),"target occurrence");
            case REGION_ENTRY, COMPLETE, ESCAPE, UNKNOWN_LOCAL, PROGRAM_RETURN, PROGRAM_HALT -> require(rs.containsKey(t.reference()),"target region");
        }};
        var exceptions=index(exceptionalEvents,ExceptionalEvent::id);
        for(var registration:conditionRegistrations) {
            require(os.containsKey(registration.statement()),"condition registration occurrence");refs(registration.proofs(),ps);registration.target().forEach(target);
            require(registration.proofs().stream().anyMatch(id->ps.get(id).kind()==ProofKind.LOCAL_GRAMMAR&&ps.get(id).rule().equals("cics-condition-registration")),"condition registration authority");
            require(registration.target().stream().allMatch(t->t.kind()==TargetKind.REGION_ENTRY),"condition label is a resolved procedure target");
        }
        for(var event:conditionEvents) {
            require(os.containsKey(event.statement()),"condition event occurrence");refs(event.proofs(),ps);target.accept(event.continuation());
            require(event.proofs().stream().anyMatch(id->ps.get(id).kind()==ProofKind.LOCAL_GRAMMAR&&ps.get(id).rule().equals("cics-pgmiderr-condition-event")),"condition event authority");
            if(!event.defaultEvent().isEmpty()) {
                var fallback=exceptions.get(event.defaultEvent());require(fallback!=null&&fallback.statement().equals(event.statement())&&fallback.origin()!=EventOrigin.EXPLICIT_ABEND,"condition default event correlation");
            }
        }
        for(var e:entryPoints) {
            require(os.containsKey(e.declaration()),"alternate entry declaration");refs(e.proofs(),ps);target.accept(e.target());
            require(Set.of(TargetKind.OCCURRENCE,TargetKind.UNKNOWN_LOCAL,TargetKind.PROGRAM_RETURN).contains(e.target().kind()),"alternate entry start shape");
            require(e.proofs().stream().anyMatch(id->ps.get(id).kind()==ProofKind.LOCAL_GRAMMAR&&ps.get(id).rule().equals("alternate-entry-start")),"alternate entry authority");
        }
        for(var c:sourceContinuations) {
            require(os.containsKey(c.statement()),"source continuation occurrence");refs(c.proofs(),ps);target.accept(c.target());
            require(c.target().kind()==TargetKind.OCCURRENCE||c.target().kind()==TargetKind.COMPLETE
                ||c.target().kind()==TargetKind.REGION_ENTRY||c.target().kind()==TargetKind.UNKNOWN_LOCAL,"source continuation shape");
            require(c.proofs().stream().anyMatch(id->ps.get(id).kind()==ProofKind.CONTROL_POSSIBILITY),"source continuation hypothesis");
            refs(c.prerequisites(),os);
            require(c.prerequisites().isEmpty()?os.get(c.statement()).outcomes().stream().allMatch(id->es.get(id).kind()==OutcomeKind.UNKNOWN_LOCAL)
                :c.target().kind()==TargetKind.REGION_ENTRY&&os.get(c.statement()).outcomes().stream().anyMatch(id->es.get(id).kind()==OutcomeKind.EXPLICIT_TRANSFER)
                    ||!c.prerequisites().isEmpty()&&conditionEvents.stream().anyMatch(e->e.statement().equals(c.statement())&&e.eligibility()==EventEligibility.HANDLER_ELIGIBLE),"source hypothesis requires unavailable completion or qualified transfer");
        }
        // Hypothesis proofs may support source possibilities only, never executable authority.
        var hypothetical=new HashSet<String>();var dependents=new HashMap<String,List<String>>();var pendingProofs=new ArrayDeque<String>();
        for(var p:proofs) {
            if(p.kind()==ProofKind.CONTROL_POSSIBILITY){hypothetical.add(p.id());pendingProofs.add(p.id());}
            for(var dependency:p.dependencies())dependents.computeIfAbsent(dependency,k->new ArrayList<>()).add(p.id());
        }
        while(!pendingProofs.isEmpty())for(var dependent:dependents.getOrDefault(pendingProofs.removeFirst(),List.of()))if(hypothetical.add(dependent))pendingProofs.add(dependent);
        java.util.function.Consumer<List<String>> executable=ids->require(ids.stream().noneMatch(hypothetical::contains),"hypothesis cannot authorize executable control");
        for(var o:occurrences)executable.accept(o.proofs());
        for(var o:outcomes){executable.accept(o.proofs());executable.accept(o.target().proofs());}
        for(var r:regions){executable.accept(r.proofs());executable.accept(r.entry().proofs());}
        for(var b:boundaries){executable.accept(b.proofs());executable.accept(b.ordinaryDefault().proofs());}
        for(var b:bindings){executable.accept(b.proofs());executable.accept(b.resume().proofs());for(var p:b.phases())executable.accept(p.proofs());}
        for(var e:entryPoints){executable.accept(e.proofs());executable.accept(e.target().proofs());}
        for(var e:conditionRegistrations){executable.accept(e.proofs());e.target().forEach(t->executable.accept(t.proofs()));}
        for(var e:conditionEvents){executable.accept(e.proofs());executable.accept(e.continuation().proofs());}
        for(var e:exceptionalEvents)executable.accept(e.proofs());
        for(var f:fileFlows){executable.accept(f.proofs());executable.accept(f.entry().proofs());for(var p:f.points()){executable.accept(p.proofs());for(var t:p.targets())executable.accept(t.proofs());}}
        for(var flow:fileFlows) {
            require(os.containsKey(flow.statement()),"FILE flow owner");refs(flow.proofs(),ps);target.accept(flow.entry());
            require(flow.entry().kind()==TargetKind.FILE_POINT&&flow.statement().equals(owners.get(flow.entry().reference())),"FILE flow entry ownership");
            var ordinals=new HashSet<Integer>();
            for(var point:flow.points()) {
                refs(point.proofs(),ps);
                if(point.kind()==FilePointKind.USE)require(ordinals.add(point.ordinal()),"duplicate FILE use ordinal");
                for(var edge:point.targets()) {
                    target.accept(edge);require(edge.kind()!=TargetKind.ESCAPE,"FILE point cannot escape a scope");
                    if(edge.kind()==TargetKind.FILE_POINT)require(flow.statement().equals(owners.get(edge.reference())),"FILE point target ownership");
                }
            }
            require(!ordinals.isEmpty(),"FILE flow needs uses");
            var seen=new HashSet<String>();var pending=new ArrayDeque<String>();pending.add(flow.entry().reference());
            while(!pending.isEmpty()) {var id=pending.removeFirst();if(!seen.add(id))continue;
                for(var edge:fps.get(id).targets())if(edge.kind()==TargetKind.FILE_POINT)pending.addLast(edge.reference());}
            require(seen.size()==flow.points().size(),"orphan FILE point");
        }
        for(var o:occurrences){refs(o.proofs(),ps);require(rs.containsKey(o.region()),"occurrence region");refs(o.outcomes(),es);
            require(rs.get(o.region()).members().contains(o.statement()),"inventoried region member");
            // A role selects one published outcome; opaque IDs never break a tie.
            var roles=new HashSet<String>();
            for(var id:o.outcomes()) {
                var e=es.get(id);require(e.statement().equals(o.statement()),"outcome owner");
                require(roles.add(e.role()),"duplicate outcome role: "+o.statement()+"/"+e.role());
            }}
        for(var r:regions){refs(r.proofs(),ps);refs(r.members(),os);refs(r.regions(),rs);target.accept(r.entry());require(r.entry().kind()!=TargetKind.FILE_POINT,"FILE ingress belongs to its occurrence");require(r.entry().kind()!=TargetKind.ESCAPE,"escape only as an occurrence outcome");
            for(var member:r.members())require(os.get(member).region().equals(r.id()),"member owner");
            require(r.parent().isEmpty()||rs.containsKey(r.parent()),"region parent");
            require(bs.containsKey(r.boundary())&&bs.get(r.boundary()).region().equals(r.id()),"region boundary");}
        for(var b:boundaries){refs(b.proofs(),ps);require(rs.containsKey(b.region()),"boundary region");require(rs.get(b.region()).boundary().equals(b.id()),"single region boundary");target.accept(b.ordinaryDefault());require(b.ordinaryDefault().kind()!=TargetKind.FILE_POINT,"FILE ingress belongs to its occurrence");require(b.ordinaryDefault().kind()!=TargetKind.ESCAPE,"escape only as an occurrence outcome");}
        for(var e:outcomes){refs(e.proofs(),ps);require(os.containsKey(e.statement())&&os.get(e.statement()).outcomes().contains(e.id()),"inventoried outcome");target.accept(e.target());
            if(e.target().kind()==TargetKind.FILE_POINT)require(e.statement().equals(owners.get(e.target().reference()))&&e.kind()==OutcomeKind.BRANCH&&e.role().startsWith("file/"),"FILE outcome target ownership");
            require(e.kind()==OutcomeKind.LOCAL_INVOKE?!e.binding().isEmpty()&&calls.containsKey(e.binding()):e.binding().isEmpty(),"outcome binding");
            if(e.kind()==OutcomeKind.LOCAL_INVOKE)require(calls.get(e.binding()).caller().equals(e.statement())
                &&e.target().kind()==TargetKind.REGION_ENTRY&&e.target().reference().equals(calls.get(e.binding()).region()),"invocation target/binding agreement");
            if(e.target().kind()==TargetKind.ESCAPE) {
                var scope=rs.get(e.target().reference());
                require(e.kind()==OutcomeKind.EXPLICIT_TRANSFER
                    &&Set.of(RegionKind.PARAGRAPH,RegionKind.INLINE_BODY,RegionKind.SENTENCE).contains(scope.kind()),"typed scope escape");
                String parent=os.get(e.statement()).region();var seen=new HashSet<String>();
                while(!parent.isEmpty()&&!parent.equals(scope.id())&&seen.add(parent))parent=rs.get(parent).parent();
                require(parent.equals(scope.id()),"escape target is a lexical enclosing scope");
            }
            if(e.kind()==OutcomeKind.PROGRAM_HALT)require(e.target().kind()==TargetKind.PROGRAM_HALT,"halt target");
            if(e.kind()==OutcomeKind.PROGRAM_RETURN)require(e.target().kind()==TargetKind.PROGRAM_RETURN,"return target");
            if(e.kind()==OutcomeKind.UNKNOWN_LOCAL)require(e.target().kind()==TargetKind.UNKNOWN_LOCAL,"unknown target");
        }
        var roleTargets=new HashMap<String,Target>();outcomes.forEach(e->roleTargets.put(e.statement()+"/"+e.role(),e.target()));
        // Ordinary USE continuation and its success outcome are the same authority.
        // A payload without an I/O event model uses the point continuation directly.
        for(var flow:fileFlows)for(var point:flow.points())if(point.kind()==FilePointKind.USE) {
            var success=roleTargets.get(flow.statement()+"/file/"+point.ordinal()+"/SUCCESS/0");
            require(success==null||point.targets().get(0).equals(success),"FILE success/point continuation agreement");
        }
        for(var b:bindings){refs(b.proofs(),ps);require(os.containsKey(b.caller())&&rs.containsKey(b.region())&&bs.containsKey(b.endpoint()),"binding references");target.accept(b.resume());require(b.resume().kind()!=TargetKind.FILE_POINT,"FILE ingress belongs to its occurrence");require(b.resume().kind()!=TargetKind.ESCAPE,"escape only as an occurrence outcome");
            require(rs.get(b.region()).kind()==RegionKind.RANGE,"invoke region is range");
            require(!rs.get(b.region()).regions().isEmpty()&&rs.get(b.region()).regions().get(rs.get(b.region()).regions().size()-1).equals(bs.get(b.endpoint()).region()),"range endpoint");}
        for(var b:bindings) {
            var phaseIds=new HashSet<String>(List.of("BODY","RESUME"));
            for(var phase:b.phases())require(phaseIds.add(phase.id()),"duplicate phase identity");
            require(phaseIds.contains(b.entryPhase())&&phaseIds.contains(b.completionPhase()),"binding phase entry/completion");
            for(var phase:b.phases()) {refs(phase.proofs(),ps);
                require(phase.level()==0||Set.of("UNTIL_PREDICATE","VARY_INITIAL","VARY_UPDATE").contains(phase.operation()),"level selects a VARYING payload");
                require(phase.kind()==PhaseKind.PREDICATE?Set.of("UNTIL_PREDICATE","COUNT_ENTRY","COUNT_REPEAT").contains(phase.operation())
                    :Set.of("VARY_INITIAL","VARY_UPDATE").contains(phase.operation()),"phase operation payload kind");var roles=new HashSet<String>();
                for(var edge:phase.edges())require(phaseIds.contains(edge.target())&&roles.add(edge.role()),"phase target/role");
                require(phase.kind()==PhaseKind.PREDICATE?roles.equals(Set.of("true","false")):roles.equals(Set.of("next")),"phase shape");
            }
        }
        // Symbolic entry/completion aliases terminate before an executable occurrence.
        // Executable loops and invocation recursion are not rejected by this check.
        var aliases=new HashMap<String,String>();
        for(var r:regions)aliases.put("entry:"+r.id(),alias(r.entry()));
        for(var b:boundaries)aliases.put("complete:"+b.region(),alias(b.ordinaryDefault()));
        var finished=new HashSet<String>();
        for(var start:aliases.keySet()) {
            var chain=new HashSet<String>();String current=start;
            while(current!=null&&!finished.contains(current)) {
                require(chain.add(current),"symbolic topology reference cycle");current=aliases.get(current);
            }
            finished.addAll(chain);
        }
        // Parent/completion composition must terminate. Executable control may cycle.
        for(var r:regions){var seen=new HashSet<String>();String parent=r.id();while(!parent.isEmpty()){
            require(seen.add(parent),"region parent cycle");parent=rs.get(parent).parent();}}
    }
    private static String alias(Target target) {
        return switch(target.kind()) {
            case REGION_ENTRY -> "entry:"+target.reference();
            case COMPLETE -> "complete:"+target.reference();
            default -> null;
        };
    }
    private static <T> List<T> sorted(List<T> values,java.util.function.Function<T,String> key) {return values.stream().sorted(Comparator.comparing(key)).toList();}
    private static <T> Map<String,T> index(List<T> values, java.util.function.Function<T,String> key) {
        var m=new HashMap<String,T>();for(var v:values)require(m.put(key.apply(v),v)==null,"duplicate topology identity");return m;
    }
    private static void refs(List<String> ids,Map<String,?> map){var unique=new HashSet<String>();for(var id:ids)require(map.containsKey(id)&&unique.add(id),"unresolved/duplicate topology reference: "+id);}
    private static <T> List<T> nonempty(List<T> values){var copy=List.copyOf(values);require(!copy.isEmpty(),"required topology evidence/inventory");return copy;}
    private static void text(String value){require(value!=null&&!value.isBlank(),"required topology identity");}
    private static void require(boolean condition,String message){if(!condition)throw new IllegalArgumentException(message);}
}
