package io.github.gustavo2358.lower.application;

import java.util.*;
import io.github.gustavo2358.lower.domain.SpInput;
import io.github.gustavo2358.lower.domain.NominalValues;
import io.github.gustavo2358.lower.source.NominalValueEvidence;
import io.github.gustavo2358.lower.source.QualifiedSourceDependencies.*;

/** Lossless source/state projection. No AIR assembly or new transfer/value rule. */
public final class QualifiedSourceProjection {
    private QualifiedSourceProjection() { }
    public static UnitEvidence project(SpInput input, Admission admission) {
        if(admission.status()!=Admission.Status.ADMITTED || !admission.input().filter(input::equals).isPresent())
            throw new IllegalArgumentException("qualified source requires admitted identical input");
        var statements=input.statements().stream().sorted(Comparator.comparing(s->s.header().id().handle()))
            .map(s->new Statement(id(s.header().id()),origin(s.header().provenance()))).toList();
        var topology=input.controlTopology();
        // Reuse the R7 engine for ordinary products where Admission did not need
        // to compute handler state. No syntax-based or executable reachability.
        var state=topology.isEmpty()?Optional.<HandlerStateAnalysis>empty():Optional.of(topology.orElseThrow().sourceContinuations().isEmpty()&&topology.orElseThrow().bindings().stream().noneMatch(b->b.reentryPolicy()==io.github.gustavo2358.lower.domain.ControlTopology.ReentryPolicy.SOURCE_UNDEFINED)?admission.handlerState().orElseGet(()->new HandlerStateAnalyzer(input).analyze()):new HandlerStateAnalyzer(input,false,true).analyze());
        var nodes=new ArrayList<Node>();var derivations=new ArrayList<Derivation>();var selections=new ArrayList<Selection>();
        var targets=new ArrayList<Target>();var events=new ArrayList<Event>();var guards=new ArrayList<Guard>();var proofs=new ArrayList<Proof>();var frontiers=new ArrayList<Frontier>();
        var nodeIds=new HashMap<HandlerStateAnalysis.Node,String>();
        var eventGuards=new HashMap<String,List<String>>();
        if(topology.isPresent()) {
            var t=topology.orElseThrow();
            for(var p:t.proofs())proofs.add(new Proof(p.id(),p.kind().name(),p.rule(),origin(p.provenance()),p.dependencies()));
            for(var e:t.exceptionalEvents()) {
                var refs=new ArrayList<String>();
                for(var premise:e.premises()){var ref=e.id()+"/guard/"+premise.name();refs.add(ref);guards.add(new Guard(ref,e.id(),premise.name()));}
                eventGuards.put(e.id(),List.copyOf(refs));
                events.add(new Event(e.id(),new StatementId(unit(input.unit()),e.statement()),e.origin().name(),e.disposition(),e.eligibility().name(),e.scope(),e.runtimeIdentity(),refs,e.proofs()));
            }
        }
        if(state.isPresent()) {
            var a=state.orElseThrow();
            var usedProofs=new HashSet<String>();a.derivations().forEach(d->usedProofs.addAll(d.proofs()));
            for(var b:topology.orElseThrow().bindings())if(usedProofs.contains(HandlerStateAnalyzer.reentryProof(b.id()))) {
                var caller=input.statements().stream().filter(s->s.header().id().handle().equals(b.caller())).findFirst().orElseThrow();
                proofs.add(new Proof(HandlerStateAnalyzer.reentryProof(b.id()),"CONTROL_POSSIBILITY","undefined-active-reentry-may-complete",origin(caller.header().provenance()),b.proofs()));
            }
            for(var n:a.nodes()){var id="node:"+nodes.size();nodeIds.put(n,id);nodes.add(new Node(id,n.context(),n.location(),support(n.support())));}
            for(var t:a.targets())targets.add(new Target(t.id(),t.form().name(),t.entry().stream().map(QualifiedSourceProjection::id).toList(),
                t.registrations().stream().map(r->new Registration(id(r.statement()),origin(r.statementOrigin()),origin(r.operandOrigin()))).toList(),operands(t.program()),values(t.program())));
            var selectedDerivations=new HashMap<HandlerStateAnalysis.Derivation,String>();
            for(var s:a.selections()) {
                var id="selection:"+selections.size();
                selections.add(new Selection(id,s.event(),nodeIds.get(s.source()),s.target().stream().toList(),s.stateOnEntry().stream().map(QualifiedSourceProjection::support).toList(),s.localEntry().stream().map(nodeIds::get).toList(),eventGuards.get(s.event()),s.proofs(),s.unknownLocalRemainder(),s.localInactivePossible(),s.outerLevelRemainder(),s.bypassed()));
                s.localEntry().ifPresent(entry->selectedDerivations.put(new HandlerStateAnalysis.Derivation(Optional.of(s.source()),entry,Optional.empty(),s.event()+"/SELECT/"+s.target().orElseThrow(),s.proofs()),id));
            }
            for(var d:a.derivations())derivations.add(new Derivation("derivation:"+derivations.size(),d.source().stream().map(nodeIds::get).toList(),nodeIds.get(d.destination()),d.callerPremise().stream().map(nodeIds::get).toList(),d.authority(),d.proofs(),Optional.ofNullable(selectedDerivations.get(d)).stream().toList()));
            for(var f:a.frontiers())frontiers.add(new Frontier(nodeIds.get(f.source()),f.authority(),f.reference(),f.proofs()));
        }
        var qualifications=new HashMap<String,List<String>>();for(var n:nodes)qualifications.computeIfAbsent(n.location(),k->new ArrayList<>()).add(n.id());
        var occurrences=new ArrayList<Occurrence>();
        for(var s:input.statements().stream().sorted(Comparator.comparing(x->x.header().id().handle())).toList()) {
            Optional<SpInput.CallTarget> target;String technology,command,namespace,profile;
            if(s instanceof SpInput.CallFact f){target=Optional.of(f.target());technology="COBOL";command="CALL";namespace="PROGRAM";profile="cobol-zos-dynamic-call-minimal@1";}
            else if(s instanceof SpInput.CicsFact f){target=f.target();technology="CICS";command=f.command().name();namespace="PROGRAM";profile=f.nameProfile();}
            else if(s instanceof SpInput.CicsFileFact f){target=f.target();technology="CICS";command=f.command();namespace="FILE";profile=f.nameProfile();}
            else continue;
            var values=values(target);
            occurrences.add(new Occurrence(id(s.header().id()),technology,command,namespace,profile,target.map(t->t instanceof SpInput.LiteralCallTarget?"LITERAL":"COMPUTED").orElse("UNAVAILABLE"),operands(target),values,values.isEmpty(),qualifications.getOrDefault(s.header().id().handle(),List.of())));
        }
        return new UnitEvidence(unit(input.unit()),state.isPresent(),statements,occurrences,targets,nodes,derivations,selections,events,guards,proofs,frontiers,nominal(input,occurrences,derivations),nativeFiles(input,qualifications));
    }
    private static List<NativeFileUse> nativeFiles(SpInput input,Map<String,List<String>> qualifications) {
        var declarations=new HashMap<io.github.gustavo2358.lower.domain.FileFacts.Candidate,io.github.gustavo2358.lower.domain.FileFacts.Declaration>();
        for(var d:input.fileInventory().declarations())declarations.put(new io.github.gustavo2358.lower.domain.FileFacts.Candidate(d.id(),d.owner()),d);
        var result=new ArrayList<NativeFileUse>();
        for(var use:input.fileInventory().operations().uses()) {
            var names=new ArrayList<NativeFileName>();var gaps=new TreeSet<>(use.gapCodes());boolean local=false;
            if(use.bindingStatus()==SpInput.ResolutionStatus.RESOLVED&&use.candidates().size()==1) {
                var d=declarations.get(use.candidates().getFirst());
                if(d!=null) {
                    local=d.kind()==io.github.gustavo2358.lower.domain.FileFacts.Kind.SD&&io.github.gustavo2358.lower.domain.FileFacts.local(use);
                    if(!local&&d.kind()!=io.github.gustavo2358.lower.domain.FileFacts.Kind.SD&&d.assignment().externalFileName().isPresent()&&!d.origins().isEmpty())
                        names.add(new NativeFileName(d.id(),unit(d.owner()),d.logicalFile(),d.assignment().externalFileName().orElseThrow(),d.origins().stream().map(QualifiedSourceProjection::origin).toList()));
                    gaps.addAll(d.gapCodes());gaps.addAll(d.assignment().gapCodes());
                }
            }
            if(!local&&names.isEmpty())gaps.add("SOURCE_FILE_TARGET_UNAVAILABLE");
            var location=input.controlTopology().stream().flatMap(t->t.fileFlows().stream()).filter(f->f.statement().equals(use.statement().handle()))
                .flatMap(f->f.points().stream()).filter(p->p.kind()==io.github.gustavo2358.lower.domain.ControlTopology.FilePointKind.USE&&p.ordinal()==use.ordinal())
                .map(p->"FILE_POINT/"+p.id()).findFirst().orElse(use.statement().handle());
            result.add(new NativeFileUse(id(use.statement()),use.ordinal(),location,use.command().name(),local,names,origin(use.provenance()),qualifications.getOrDefault(location,List.of()),List.copyOf(gaps)));
        }
        return result.stream().sorted(Comparator.comparing((NativeFileUse f)->f.statement().handle()).thenComparingInt(NativeFileUse::ordinal)).toList();
    }
    private static Optional<NominalValueEvidence> nominal(SpInput input,List<Occurrence> occurrences,List<Derivation> derivations) {
        return input.nominalValues().map(raw->{
            var sinks=new HashSet<String>();for(var o:occurrences)if(o.targetKind().equals("COMPUTED"))sinks.add(o.id().handle());
            var facts=new NominalValues(raw.authority(),raw.symbols(),raw.assignments(),raw.conditions(),raw.queries().stream().filter(q->sinks.contains(q.statement())).toList(),raw.tableFields());
            var symbols=new HashSet<String>();facts.symbols().forEach(s->symbols.add(s.node()));
            var storage=input.storage().orElseThrow();
            var declarations=storage.nodes().stream().filter(n->symbols.contains(n.id().handle()))
                .map(n->new NominalValueEvidence.Declaration(n.id().handle(),origin(n.provenance()))).toList();
            var seeds=storage.entryState().conditions().stream().filter(c->symbols.contains(c.node().handle())&&c.logicalText().isPresent())
                .map(c->new NominalValueEvidence.Seed(c.node().handle(),c.logicalText().orElseThrow(),c.proof().name(),origin(c.provenance()))).toList();
            var predicates=new HashSet<String>();facts.conditions().forEach(c->predicates.add(c.statement()));
            var outcomes=new HashMap<String,Boolean>();input.controlTopology().ifPresent(t->t.outcomes().forEach(o->{
                if(predicates.contains(o.statement())&&Set.of("then","else").contains(o.role()))outcomes.put(o.id(),o.role().equals("then"));
            }));
            var branches=derivations.stream().filter(d->outcomes.containsKey(d.authority()))
                .map(d->new NominalValueEvidence.Branch(d.id(),outcomes.get(d.authority()))).toList();
            var uncertainties=input.factDependencies().stream().flatMap(g->g.inputs().stream())
                .filter(i->!i.available()&&!i.kind().name().equals("PHYSICAL_PROFILE"))
                .map(i->new NominalValueEvidence.Uncertainty(i.id(),i.kind().name(),origin(i.provenance()))).toList();
            return new NominalValueEvidence(facts,declarations,seeds,branches,uncertainties);
        });
    }
    /** Multi-unit adapters use the same public input admission before projecting. */
    public static UnitEvidence admitAndProject(SpInput input,LowerInput.Options options) {
        return project(input,new PartialProgramAdmission().plan(input,options.admission(),options.publicationPolicy()).admission());
    }
    private static List<Operand> operands(Optional<SpInput.CallTarget> target){return target.stream().map(t->new Operand(new OperandId(id(t.id().statement()),t.id().handle()),origin(t.provenance()))).toList();}
    private static List<Value> values(Optional<SpInput.CallTarget> target){return target.filter(SpInput.LiteralCallTarget.class::isInstance).map(SpInput.LiteralCallTarget.class::cast).flatMap(SpInput.LiteralCallTarget::logicalValue).filter(v->v.logicalDomain()==SpInput.LogicalDomain.TEXT).stream().map(v->new Value(v.logicalDomain().name(),v.value(),v.logicalExtent())).toList();}
    private static Support support(HandlerStateAnalysis.Support s){return new Support(s.state().kind().name(),s.state().target().isEmpty()?List.of():List.of(s.state().target()),s.activation().stream().map(QualifiedSourceProjection::id).toList(),s.state().cause().name());}
    private static UnitId unit(SpInput.UnitKey u){return new UnitId(u.compilationUnitId(),u.structuralPath(),u.canonicalProgramName());}
    private static StatementId id(SpInput.StatementId s){return new StatementId(unit(s.unit()),s.handle());}
    private static Provenance origin(SpInput.Provenance p){return new Provenance(location(p.expanded()),location(p.original()),p.includeChain().stream().map(i->new Include(i.includingFile(),i.requestedName(),i.includedFile(),i.includeLine())).toList(),p.exact());}
    private static Location location(SpInput.Location l){return new Location(l.file(),l.startLine(),l.startColumn(),l.endLine(),l.endColumn());}
}
