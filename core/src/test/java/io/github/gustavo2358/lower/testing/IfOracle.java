package io.github.gustavo2358.lower.testing;

import io.github.gustavo2358.air.model.*;
import io.github.gustavo2358.air.model.Ids.*;
import io.github.gustavo2358.air.validation.*;
import io.github.gustavo2358.lower.application.*;
import io.github.gustavo2358.lower.domain.SpInput;
import java.util.*;

/** Consumer boundary oracle: only published SP facts, public correlations and AIR model.
 * No assembler, LocalIds, source text/AST, display-name join or physical sequence order. */
public final class IfOracle {
    private IfOracle() { }
    private static int assertions;
    public static int assertions() { return assertions; }
    public static void check(boolean ok, String rule) { if (!ok) throw new AssertionError("W2B " + rule); assertions++; }
    public static void inspect(SpInput input, LoweringResult result) {
        check(result.status() == LoweringResult.Status.SUCCESS, "successful diamond: " + result.status() + " " + result.admission().diagnostics() + " " + result.validation());
        var p = result.publication().orElseThrow();
        check(p.units().size() == 1, "one unit"); var u = p.units().getFirst();
        var sequences = new HashMap<LabelId,Sequence>(); u.sequences().forEach(s -> check(sequences.put(s.label(),s)==null,"unique sequence labels"));
        var links = new HashMap<SpInput.StatementId,LoweringResult.StatementLink>(); result.statements().forEach(s -> check(links.put(s.source(),s)==null,"unique statement links"));
        var data = new HashMap<SpInput.DataId,LoweringResult.DataLink>(); result.data().forEach(d -> check(data.put(d.source(),d)==null,"unique data links"));
        var facts = new HashMap<SpInput.StatementId,SpInput.StatementFact>(); input.statements().forEach(s -> facts.put(s.header().id(),s));
        var artifacts = new HashMap<ArtifactId,Origins.Artifact>(); p.artifacts().forEach(a -> artifacts.put(a.id(),a));
        var origins = new HashMap<OriginId,Origins.Origin>(); p.origins().forEach(o -> origins.put(o.id(),o));
        var f = input.statements().stream().filter(SpInput.IfFact.class::isInstance).map(SpInput.IfFact.class::cast).findFirst().orElseThrow();
        var entryFact = input.entryInventory().entries().getFirst();
        check(entryFact.start().statement().equals(Optional.of(f.header().id())),"source IF is explicit entry");
        var entryLink = result.entries().getFirst();
        check(entryLink.source().equals(entryFact.id()) && entryLink.start().equals(links.get(f.header().id()).label()),"public entry correlation follows IF identity");
        check(u.entries().getFirst().initialLabel().equals(Optional.of(entryLink.start())),"entry independent of physical order");
        var entry = sequences.get(entryLink.start());
        check(entry.instructions().isEmpty() && entry.terminator() instanceof Operations.Branch,"Branch entry terminator");
        var b = (Operations.Branch)entry.terminator();
        check(links.get(f.header().id()).target().equals(b.header().id()),"IF Branch operation correlation");
        check(hasLocation(origins,artifacts,b.header().origin(),f.header().provenance().original()),"Branch IF origin");
        check(b.predicate() instanceof Expressions.Unknown,"predicate stays Unknown");
        var predicate = (Expressions.Unknown)b.predicate();
        check(predicate.header().role()==Operand.Role.PREDICATE && predicate.typeRef().equals(Types.known(Types.Builtin.BOOL)),"Unknown known BOOL predicate");
        check(predicate.remainingReads()==Scopes.NoMemory.INSTANCE,"COMPLETE reads -> NoMemory remainder");
        check(hasLocation(origins,artifacts,predicate.header().origin(),f.predicateGuarantee().provenance().original()),"predicate provenance");
        var unknown = p.uncertainties().stream().filter(x -> x.id().equals(predicate.reason())).findFirst().orElseThrow();
        check(unknown.dimensions().equals(List.of(Evidence.Dimension.VALUES)) && unknown.code().equals("predicate-value-unknown"),"only predicate VALUES unknown");
        check(unknown.scope().equals(new Scopes.EntityScope(List.of(predicate.header().id()))) && unknown.origin().equals(predicate.header().origin()),"predicate uncertainty scope and origin");
        var reads = new HashMap<SpInput.OperandId,SpInput.DataReference>(); f.conditionReads().forEach(r -> reads.put(r.id(),r));
        check(predicate.dependencies().size()==f.predicateGuarantee().knownReads().size(),"all and only knownReads");
        for(int i=0;i<predicate.dependencies().size();i++) {
            var ref = reads.get(f.predicateGuarantee().knownReads().get(i));
            check(predicate.dependencies().get(i) instanceof Expressions.Read,"published dependency is Read");
            var read = (Expressions.Read)predicate.dependencies().get(i);
            check(read.header().role()==Operand.Role.VALUE_READ && read.place() instanceof Places.ObjectPlace,"whole-item VALUE_READ");
            var place = (Places.ObjectPlace)read.place();
            check(place.object().equals(data.get(ref.wholeItemAccess().orElseThrow().data()).object()),"dependency maps published FLAG identity");
            check(hasLocation(origins,artifacts,read.header().origin(),ref.provenance().original()),"read occurrence provenance");
            check(result.operands().stream().anyMatch(l -> l.source().equals(ref.id()) && l.target().equals(read.header().id())),"predicate read public correlation");
        }
        var callId = f.normalContinuation().statement().orElseThrow();
        var call = (SpInput.CallFact)facts.get(callId); var callLabel = links.get(callId).label();
        var merge = sequences.get(callLabel);
        check(merge.instructions().isEmpty() && merge.terminator() instanceof Operations.Invoke,"merge contains Invoke terminator only");
        var invoke = (Operations.Invoke)merge.terminator();
        check(links.get(callId).target().equals(invoke.header().id()) && hasLocation(origins,artifacts,invoke.header().origin(),call.header().provenance().original()),"real CALL Invoke origin/link");
        var retLabel = links.get(call.normalContinuation().statement().orElseThrow()).label();
        check(invoke.outcomes().known().equals(List.of(new Control.Normal(retLabel))),"Invoke normal -> real GOBACK");
        check(invoke.outcomes().remainder().equals(new Scopes.WithinControl(new Scopes.AllControl(p.id()))) && invoke.contract() instanceof Interactions.UnknownContract,"W1 open outcomes/unknown contract");
        check(invoke.target() instanceof Interactions.ComputedTarget,"W1 computed CALL preserved");
        var target = (Interactions.ComputedTarget)invoke.target();
        check(target.name() instanceof Expressions.Read && target.namePolicy() instanceof Interactions.UnknownName,"computed CALL Read/UnknownName");
        var ref = ((SpInput.DataCallTarget)call.target()).reference();
        check(((Places.ObjectPlace)((Expressions.Read)target.name()).place()).object().equals(data.get(ref.wholeItemAccess().orElseThrow().data()).object()),"computed CALL selected object");
        var memory = new Scopes.WithinMemory(new Scopes.AllMemory(p.id(),true));
        check(invoke.effectBound().otherwise().reads().equals(memory) && invoke.effectBound().otherwise().writes().equals(memory),"W1 effects preserved");
        var ret = sequences.get(retLabel);
        check(ret.instructions().isEmpty() && ret.terminator() instanceof Operations.Return,"Return sequence");
        check(hasLocation(origins,artifacts,ret.terminator().header().origin(),facts.get(call.normalContinuation().statement().orElseThrow()).header().provenance().original()),"real GOBACK origin");
        int arms=0;
        for(var relation:input.structure().branches()) {
            check(relation.parent().equals(f.header().id()),"only root IF arm relations");
            var arm = relation.branch()==SpInput.Branch.THEN ? f.thenArm() : f.elseArm();
            var destination = relation.branch()==SpInput.Branch.THEN ? b.trueDestination() : b.falseDestination();
            if(arm.presence()==SpInput.ClausePresence.ABSENT) {
                check(relation.branch()==SpInput.Branch.ELSE && relation.children().isEmpty() && destination.equals(callLabel),"open FALSE directly to CALL; no fake ELSE");
                continue;
            }
            arms++; check(destination.equals(links.get(arm.entry().statement().orElseThrow()).label()),"TRUE/FALSE follows explicit arm entry");
            var seq=sequences.get(destination);
            check(seq.instructions().size()==relation.children().size(),"exact arm Assign count");
            check(seq.terminator() instanceof Operations.Jump j && j.destination().equals(callLabel),"arm Jump to shared CALL merge");
            check(hasLocation(origins,artifacts,seq.terminator().header().origin(),arm.provenance().original()) && hasLocation(origins,artifacts,seq.terminator().header().origin(),f.normalContinuation().provenance().original()),"Jump arm/completion origin");
            for(int i=0;i<relation.children().size();i++) {
                var m=(SpInput.MoveFact)facts.get(relation.children().get(i));var a=seq.instructions().get(i);
                check(a instanceof Operations.Assign && a.header().id().equals(links.get(m.header().id()).target()),"MOVE order from canonical children, not StatementId");
                var assign=(Operations.Assign)a;
                String value=m.textAdjustment().map(x -> x.result().value()).orElseGet(() -> m.source().logicalValue().orElseThrow().value());
                check(assign.value() instanceof Expressions.Literal l && l.value().equals(new Values.TextValue(value)),"fitted text from published MOVE");
                check(((Places.ObjectPlace)assign.destination()).object().equals(data.get(m.target().wholeItemAccess().orElseThrow().data()).object()),"MOVE selected object");
                check(hasLocation(origins,artifacts,assign.header().origin(),m.header().provenance().original()),"Assign real MOVE provenance");
                if(m.textAdjustment().isPresent()) check(origins.get(assign.value().header().origin()) instanceof Origins.Derived,"fitted spaces have derived origin");
            }
        }
        check(u.sequences().size()==3+arms && links.size()==input.statements().size(),"no fake sequences or lost statements");
        check(u.objects().size()==input.dataDeclarations().size() && p.storage().size()==data.size() && u.entries().getFirst().state().conditions().isEmpty(),"no fake data or initial value");
        var proof=input.storageIndependence().orElseThrow();
        check(p.premises().size()==1 && p.premises().getFirst().assertion() instanceof Proofs.DisjointStorage,"published DisjointStorage premise");
        var premise=p.premises().getFirst();
        check(((Proofs.DisjointStorage)premise.assertion()).storage().equals(proof.members().stream().map(x -> data.get(x).storage()).toList()),"exact ordered proof members, including extra unused members");
        check(premise.authority().equals(proof.authority()) && premise.justification().equals("SP rule: " + proof.rule().name()),"published authority and descriptive rule");
        check(hasLocation(origins,artifacts,premise.origin(),proof.provenance().orElseThrow().original()),"premise source origin");
        var validation=AirValidator.validate(p);
        check(validation.isStructurallyValid(),"Validator traversal complete");
        for(String rule:List.of("I-09","I-59","I-56")) check(validation.issues().stream().anyMatch(i -> i.rule().equals(rule) && i.kind()==ValidationIssue.Kind.SEMANTIC_OBLIGATION),"obligation retained " + rule);
        check(p.coverage().inventory()==Evidence.InventoryStatus.PARTIAL,"global PARTIAL preserved");
    }
    private static boolean hasLocation(Map<OriginId,Origins.Origin> origins,Map<ArtifactId,Origins.Artifact> artifacts,OriginId id,SpInput.Location location) {
        var todo=new ArrayDeque<OriginId>();todo.add(id);var seen=new HashSet<OriginId>();
        while(!todo.isEmpty()) {
            var current=todo.remove();if(!seen.add(current))continue;var o=origins.get(current);
            if(o instanceof Origins.Derived d)todo.addAll(d.inputs());
            if(o instanceof Origins.Written w && w.exact() && artifacts.get(w.artifact()).logicalName().equals(location.file()) && w.location().orElse(null) instanceof Origins.LineColumns lc) {
                var s=lc.span();if(s.start().line().intValueExact()==location.startLine() && s.start().column().intValueExact()==location.startColumn() && s.end().line().intValueExact()==location.endLine() && s.end().column().intValueExact()==location.endColumn())return true;
            }
        }return false;
    }
}
