package io.github.gustavo2358.lower.application;

import io.github.gustavo2358.lower.domain.SpInput;
import java.util.*;
import static io.github.gustavo2358.lower.domain.SpInput.*;
import static io.github.gustavo2358.lower.application.Admission.*;

/** Structural admission is global; semantic precision is selected per occurrence. */
final class PartialProgramAdmission {
    record Plan(Admission admission, List<DataFact> data, List<StatementFact> statements,
                Set<StatementId> precise, Map<StatementId,List<MoveFact>> bodies) { }
    Plan plan(SpInput input, AdmitInput.Limits limits) {
        var c = new EntryGobackAdmission.Context(input, limits, true);
        try {
            if (input == null) { c.require(false,Rule.INPUT_REQUIRED,"input",null,"SP input required"); return rejected(c,Status.INVALID_INPUT); }
            EntryGobackAdmission.validate(input, c);
            if (!c.diagnostics.isEmpty()) return rejected(c, Status.INVALID_INPUT);
            CallAdmission.validateFacts(input,c);
            var evaluateMembers=new HashMap<StatementId,Set<StatementId>>();
            for(var s:input.statements()) if(s.header().containment().branch()==Branch.EVALUATE_ARM)
                s.header().containment().parent().ifPresent(id -> evaluateMembers.computeIfAbsent(id,k->new HashSet<>()).add(s.header().id()));
            for (var s : input.statements()) {
                var n = next(s); if (n != null) CallAdmission.continuation(n,s.header(),c);
                if(s instanceof OtherStatement o) {
                    var operands=new HashSet<OperandId>();
                    for(var ref:o.knownReferences())CallAdmission.reference(ref,o.header(),operands,c);
                }
                if (s instanceof EvaluateFact e) EvaluateAdmission.validate(e,evaluateMembers.getOrDefault(e.header().id(),Set.of()),c);
                if (s instanceof IfFact f) {
                    var operands = new HashSet<OperandId>();
                    for (var ref : f.conditionReads()) CallAdmission.reference(ref,f.header(),operands,c);
                    c.provenance(f.conditionProvenance()); c.provenance(f.predicateGuarantee().provenance());
                    c.provenance(f.thenArm().provenance()); c.provenance(f.elseArm().provenance());
                    validateArm(f,f.thenArm(),Branch.THEN,c); validateArm(f,f.elseArm(),Branch.ELSE,c);
                }
                if (s instanceof PerformFact p) {
                    p.target().ifPresent(t -> { c.identity(t.id().unit(),t.id().handle(),"procedure",t.paragraphOrigin()); c.provenance(t.referenceOrigin()); c.provenance(t.paragraphOrigin()); });
                    for (var list : List.of(p.primaryStatements(),p.targetStatements())) {
                        var unique = new HashSet<StatementId>();
                        for (var id : list) { c.touch(); c.require(id.unit().equals(input.unit()) && c.lookup(id)!=null && unique.add(id),Rule.STRUCTURE,id.handle(),null,"PERFORM members are distinct published statements"); }
                    }
                }
            }
            if (!c.diagnostics.isEmpty()) return rejected(c, Status.INVALID_INPUT);
            input.storageIndependence().filter(p -> p.availability()==Availability.KNOWN).ifPresent(p -> {
                var members=new HashSet<DataId>();
                c.require(p.members().size()>=2 && p.authority().equals("IBM_ENTERPRISE_COBOL_6_4_WORKING_STORAGE")
                    && p.gapCodes().isEmpty() && p.provenance().filter(Provenance::exact).isPresent(),Rule.PROFILE_FACT,"storage-independence",null,"complete source-derived storage proof required");
                for(var id:p.members())c.require(id.unit().equals(input.unit()) && c.data(id)!=null && members.add(id),Rule.STRUCTURE,id.handle(),null,"storage proof has distinct published members");
            });
            if (!c.diagnostics.isEmpty())return rejected(c,Status.INVALID_INPUT);
            c.phase=Phase.ADMISSION;
            c.require(input.entryInventory().entries().size()==1 && input.entryInventory().entries().getFirst().start().statement().isPresent(),
                Rule.ENTRY_START,"entry",null,"usable explicit primary entry required");
            if (!c.diagnostics.isEmpty()) return rejected(c,Status.BLOCKED_LOWERING);
            var data=ScalarDataOrder.canonical(input.dataDeclarations().stream().filter(CallAdmission::scalar).toList());
            var mapped=new HashSet<DataId>(); data.forEach(d->mapped.add(d.id()));
            var precise=new HashSet<StatementId>();
            for(var s:input.statements()) {
                int before=c.diagnostics.size();
                boolean eligible=false;
                if(s instanceof MoveFact m && mapped.contains(m.target().wholeItemAccess().map(WholeItemAccess::data).orElse(null))
                    && (!(m.source() instanceof DataReference r)||mapped.contains(r.wholeItemAccess().map(WholeItemAccess::data).orElse(null)))) {
                    CallAdmission.admitMove(m,c); eligible=true;
                } else if(s instanceof CallFact call) {
                    eligible=true; // The dependency site survives unavailable target values and CALL surface gaps.
                } else if(s instanceof IfFact f && f.predicateGuarantee().availability()==Availability.KNOWN
                        && f.thenArm().entry().statement().isPresent() && f.normalContinuation().statement().isPresent()
                        && (f.elseArm().entry().statement().isPresent() || f.elseArm().presence()==ClausePresence.ABSENT)
                        && f.conditionReads().stream().allMatch(r -> mapped.contains(r.wholeItemAccess().map(WholeItemAccess::data).orElse(null)))) {
                    IfAdmission.admitPredicate(f,c,true); eligible=true;
                } else if(s instanceof EvaluateFact e) eligible=EvaluateAdmission.structured(e);
                else if(s instanceof GobackFact) eligible=true;
                if(eligible&&before==c.diagnostics.size())precise.add(s.header().id());
                c.diagnostics.subList(before,c.diagnostics.size()).clear();
            }
            var bodies=new LinkedHashMap<StatementId,List<MoveFact>>(); var bodyMembers=new HashSet<StatementId>();
            var targets=new HashMap<ProcedureId,List<StatementId>>();
            var owners=new HashMap<StatementId,ProcedureId>();
            var primary=Set.copyOf(primary(input,c,precise));
            for(var s:input.statements()) if(s instanceof PerformFact p && p.profile()==PerformProfile.BASIC_PROCEDURE_PERFORM) {
                boolean valid=p.target().isPresent()&&!p.targetStatements().isEmpty()&&p.normalContinuation().statement().isPresent()&&p.gapCodes().isEmpty();
                valid &= p.primaryStatements().isEmpty() && primary.contains(p.header().id())
                    && p.normalContinuation().statement().filter(primary::contains).isPresent()
                    && p.target().filter(t -> t.referenceOrigin().exact() && t.paragraphOrigin().exact()).isPresent();
                var target=p.target().map(PerformTarget::id).orElse(null);
                if(target!=null) {
                    var previous=targets.putIfAbsent(target,p.targetStatements());
                    valid &= previous==null || previous.equals(p.targetStatements());
                }
                var body=new ArrayList<MoveFact>(); boolean preciseBody=true;
                for(int i=0;i<p.targetStatements().size();i++) {
                    var member=c.lookup(p.targetStatements().get(i));
                    if(!(member instanceof MoveFact m)) {valid=false;continue;}
                    preciseBody &= precise.contains(m.header().id());
                    var expected=i+1<p.targetStatements().size()?Optional.of(p.targetStatements().get(i+1)):Optional.<StatementId>empty();
                    var owner=owners.putIfAbsent(m.header().id(),target);
                    valid &= (owner==null || owner.equals(target)) && !primary.contains(m.header().id())
                        && m.header().containment().equals(new Containment(Optional.empty(),Branch.ROOT))
                        && m.normalContinuation().statement().equals(expected); body.add(m);
                }
                valid &= p.targetEntry().equals(p.targetStatements().stream().findFirst())
                    &&p.targetExit().equals(p.targetStatements().isEmpty()?Optional.empty():Optional.of(p.targetStatements().getLast()));
                if(!valid) {c.require(false,Rule.STRUCTURE,p.header().id().handle(),p.header().provenance(),"BASIC activation contradicts intrinsic body facts");continue;}
                if(!preciseBody)continue; // A semantic body gap is partial, not a contradictory structural proof.
                precise.add(p.header().id());bodies.put(p.header().id(),List.copyOf(body));bodyMembers.addAll(p.targetStatements());
            }
            for (var s : input.statements()) {
                var successor=next(s);
                if (!bodyMembers.contains(s.header().id()) && successor!=null)
                    c.require(successor.statement().filter(bodyMembers::contains).isEmpty(),Rule.STRUCTURE,s.header().id().handle(),s.header().provenance(),"intrinsic BASIC body has no ordinary incoming continuation");
            }
            if(!c.diagnostics.isEmpty())return rejected(c,Status.INVALID_INPUT);
            var statements=input.statements().stream().filter(s->!bodyMembers.contains(s.header().id()))
                .sorted(Comparator.comparingInt(s->s.header().programPoint())).toList();
            return new Plan(c.result(Status.ADMITTED),data,statements,Set.copyOf(precise),Map.copyOf(bodies));
        } catch(EntryGobackAdmission.LimitReached ex) {return rejected(c,Status.IMPLEMENTATION_LIMIT);}
    }
    private static List<StatementId> primary(SpInput input,EntryGobackAdmission.Context c,Set<StatementId> precise) {
        record Visit(StatementId id,boolean complete) { }
        var closed=new LinkedHashSet<StatementId>(); var active=new HashSet<StatementId>(); var pending=new ArrayDeque<Visit>();
        input.entryInventory().entries().getFirst().start().statement().ifPresent(id->pending.push(new Visit(id,false)));
        while(!pending.isEmpty()) {
            var visit=pending.pop(); var id=visit.id();
            if(visit.complete()) { active.remove(id); closed.add(id); continue; }
            if(closed.contains(id))continue; // A shared join was already proved closed.
            if(!active.add(id))return List.of(); // A cycle does not prove a returning primary region.
            c.touch(); var s=c.lookup(id); if(s==null)return List.of();
            if(s instanceof GobackFact) { active.remove(id); closed.add(id); continue; }
            var continuation=next(s);
            if(continuation==null || continuation.availability()!=ContinuationAvailability.KNOWN
                    || continuation.statement().isEmpty() || !continuation.provenance().exact())return List.of();
            pending.push(new Visit(id,true));
            pending.push(new Visit(continuation.statement().orElseThrow(),false));
            if(s instanceof EvaluateFact e) {
                if(!precise.contains(id))return List.of();
                for(var arm:e.arms())pending.push(new Visit(arm.control().entry().statement().orElseThrow(),false));
                e.otherArm().entry().statement().ifPresent(entry->pending.push(new Visit(entry,false)));
            }
            if(s instanceof IfFact f) {
                if(!precise.contains(id) || !primaryArm(f.thenArm(),false) || !primaryArm(f.elseArm(),true))return List.of();
                pending.push(new Visit(f.thenArm().entry().statement().orElseThrow(),false));
                f.elseArm().entry().statement().ifPresent(entry->pending.push(new Visit(entry,false)));
            }
        }
        return List.copyOf(closed);
    }
    private static boolean primaryArm(IfArm arm,boolean mayBeAbsent) {
        return arm.contentAvailability()==Availability.KNOWN && arm.provenance().exact()
            && (arm.presence()==ClausePresence.PRESENT && arm.entry().availability()==Availability.KNOWN && arm.entry().statement().isPresent()
                || mayBeAbsent && arm.presence()==ClausePresence.ABSENT && arm.entry().statement().isEmpty());
    }

    private static void validateArm(IfFact f,IfArm arm,Branch branch,EntryGobackAdmission.Context c) {
        if(arm.entry().statement().isPresent()) {
            var id=arm.entry().statement().orElseThrow();var child=c.lookup(id);
            c.require(child!=null && child.header().containment().equals(new Containment(Optional.of(f.header().id()),branch)),
                Rule.STRUCTURE,id.handle(),arm.provenance(),"IF arm entry belongs to that direct arm");
        }
        c.require(arm.presence()!=ClausePresence.ABSENT || arm.entry().statement().isEmpty(),Rule.STRUCTURE,f.header().id().handle(),arm.provenance(),"absent IF arm has no entry");
    }
    static NormalContinuation next(StatementFact s) {
        if(s instanceof EvaluateFact e)return e.normalContinuation();
        if(s instanceof OtherStatement o)return o.normalContinuation();
        return SupportedProgramAdmission.next(s);
    }
    private static Plan rejected(EntryGobackAdmission.Context c,Status s) {return new Plan(c.result(s),List.of(),List.of(),Set.of(),Map.of());}
}
