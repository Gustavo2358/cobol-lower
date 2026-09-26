package io.github.gustavo2358.lower.application;

import java.util.*;
import io.github.gustavo2358.lower.domain.SpInput;
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
        var state=topology.isEmpty()?Optional.<HandlerStateAnalysis>empty():Optional.of(admission.handlerState().orElseGet(()->new HandlerStateAnalyzer(input).analyze()));
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
        return new UnitEvidence(unit(input.unit()),state.isPresent(),statements,occurrences,targets,nodes,derivations,selections,events,guards,proofs,frontiers);
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
