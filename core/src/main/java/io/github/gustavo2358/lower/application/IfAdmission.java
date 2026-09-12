package io.github.gustavo2358.lower.application;

import io.github.gustavo2358.lower.domain.SpInput;
import java.util.*;
import static io.github.gustavo2358.lower.domain.SpInput.*;
import static io.github.gustavo2358.lower.application.Admission.*;

/** SP1.4 simple IF proof admission. Linear indexed passes; no control inferred from IDs. */
public final class IfAdmission implements AdmitInput {
    record Plan(Admission admission, List<DataFact> data, Optional<IfFact> branch, List<MoveFact> thenMoves,
                List<MoveFact> elseMoves, Optional<CallFact> call, Optional<GobackFact> terminal) {
        List<MoveFact> moves() { var r=new ArrayList<>(thenMoves);r.addAll(elseMoves);return List.copyOf(r); }
    }
    @Override public Admission admit(SpInput input, Limits limits) { return plan(input,limits).admission(); }
    Plan plan(SpInput input, Limits limits) {
        var c=new EntryGobackAdmission.Context(input,Objects.requireNonNull(limits),true);
        try {
            if(input==null) { c.require(false,Rule.INPUT_REQUIRED,"input",null,"SP input required");return rejected(c,Status.INVALID_INPUT); }
            EntryGobackAdmission.validate(input,c);
            if(!c.diagnostics.isEmpty())return rejected(c,Status.INVALID_INPUT);
            CallAdmission.validateFacts(input,c);
            var ifs=new ArrayList<IfFact>();var calls=new ArrayList<CallFact>();var terminals=new ArrayList<GobackFact>();
            for(var s:input.statements()) {
                c.touch();
                if(s instanceof IfFact f) {
                    ifs.add(f);var seen=new HashSet<OperandId>();
                    for(var r:f.conditionReads())CallAdmission.reference(r,f.header(),seen,c);
                    c.provenance(f.conditionProvenance());c.provenance(f.predicateGuarantee().provenance());
                    c.provenance(f.thenArm().provenance());c.provenance(f.elseArm().provenance());
                    CallAdmission.continuation(f.normalContinuation(),f.header(),c);
                } else if(s instanceof CallFact k)calls.add(k);
                else if(s instanceof GobackFact g)terminals.add(g);
            }
            if(!c.diagnostics.isEmpty())return rejected(c,Status.INVALID_INPUT);
            c.phase=Phase.ADMISSION;
            need(c,ifs.size()==1 && calls.size()==1 && terminals.size()==1,"one root IF, one CALL, one GOBACK");
            if(!c.diagnostics.isEmpty())return rejected(c,Status.UNSUPPORTED_SLICE);
            var f=ifs.getFirst();var call=calls.getFirst();var terminal=terminals.getFirst();var predicate=f.predicateGuarantee();
            need(c,input.coverage().inventoryStatus()==InventoryStatus.COMPLETE && input.coverage().inputMissingStatements()==0,"complete observed inventory");
            need(c,input.entryInventory().status()==InventoryStatus.PARTIAL && input.entryInventory().entries().size()==1
                && input.entryInventory().gapCodes().equals(List.of("ALTERNATE_ENTRIES_NOT_PROJECTED")),"one primary entry with only alternate-entry inventory gap");
            for(var e:input.entryInventory().entries()) {
                c.touch();need(c,e.availability()==Availability.KNOWN && e.coverage()==CoverageStatus.MODELED && e.start().availability()==Availability.KNOWN
                    && e.start().statement().equals(Optional.of(f.header().id())) && e.signature().availability()==Availability.KNOWN
                    && e.signature().parameterCount().equals(Optional.of(0)) && e.signature().returningClause()==ReturningClause.ABSENT
                    && e.provenance().exact() && e.gaps().isEmpty(),"entry explicitly starts at IF with closed zero signature");
            }
            need(c,f.header().containment().equals(new Containment(Optional.empty(),Branch.ROOT)) && f.explicitlyTerminated()
                && f.profile()==IfProfile.SIMPLE_TEXT_EQUALITY,"root explicitly terminated SIMPLE_TEXT_EQUALITY only; nested outside slice");
            need(c,predicate.availability()==Availability.KNOWN && predicate.profile()==PredicateProfile.SCALAR_TEXT_EQUALITY
                && predicate.resultDomain()==PredicateDomain.BOOLEAN && predicate.evaluation()==PredicateEvaluation.PURE
                && predicate.normalCompletion()==PredicateCompletion.TOTAL && predicate.readsCompleteness()==ReadsCompleteness.COMPLETE
                && predicate.truthValue()==PredicateTruth.UNKNOWN && predicate.gapCodes().isEmpty() && predicate.provenance().exact()
                && f.conditionProvenance().exact() && f.conditionShape().equals("RELATION"),"published BOOLEAN/PURE/TOTAL/COMPLETE, truth UNKNOWN");
            need(c,predicate.knownReads().size()==1 && predicate.knownReads().equals(f.conditionReads().stream().map(DataReference::id).toList()),"all known read occurrences in published order");
            for(var read:f.conditionReads()) {
                c.touch();CallAdmission.admitReference(read,OperandRole.READ,f.header(),c);
                need(c,read.provenance().exact(),"exact predicate read provenance");
            }
            need(c,f.normalContinuation().availability()==ContinuationAvailability.KNOWN
                && f.normalContinuation().statement().equals(Optional.of(call.header().id())) && f.continuation().equals(f.normalContinuation().statement())
                && f.normalContinuation().provenance().exact(),"IF completion explicitly identifies post-merge CALL");
            var surface=call.surface();
            need(c,call.header().containment().equals(new Containment(Optional.empty(),Branch.ROOT))
                && surface.using()==ClausePresence.ABSENT && surface.argumentCount().equals(Optional.of(0)) && surface.returning()==ClausePresence.ABSENT
                && surface.onException()==ClausePresence.ABSENT && surface.notOnException()==ClausePresence.ABSENT && surface.onOverflow()==ClausePresence.ABSENT,
                "CALL remains within W1C surface");
            if(call.target() instanceof DataCallTarget d)CallAdmission.admitReference(d.reference(),OperandRole.CALL_TARGET,call.header(),c);
            else need(c,((LiteralCallTarget)call.target()).logicalValue().isPresent(),"CALL published logical literal");
            need(c,call.normalContinuation().availability()==ContinuationAvailability.KNOWN && call.normalContinuation().statement().equals(Optional.of(terminal.header().id()))
                && terminal.header().containment().equals(new Containment(Optional.empty(),Branch.ROOT)),"CALL explicit normal GOBACK continuation");
            for(var s:input.statements()) {
                c.touch();need(c,s.header().coverage()==CoverageStatus.MODELED && s.header().provenance().exact(),"all modeled source statements with exact provenance");
                if(s instanceof MoveFact m)CallAdmission.admitMove(m,c);
                else need(c,s==f || s==call || s==terminal,"only admitted arm MOVEs, root IF/CALL/GOBACK; no filtering");
            }
            for(var d:input.dataDeclarations()) { c.touch();need(c,d.coverage()==CoverageStatus.MODELED && d.provenance().exact() && CallAdmission.scalar(d),"all DATA have scalar storage mapping"); }
            for(var gap:input.gaps()) { c.touch();need(c,gap.statement().equals(call.header().id()) && gap.scope()==GapScope.RUNTIME_CALL_TARGET,"no unaccounted semantic gaps"); }
            var armRelations=new EnumMap<Branch,List<StatementId>>(Branch.class);
            for(var b:input.structure().branches()) { c.touch();need(c,b.parent().equals(f.header().id()),"branch owner is the admitted IF");armRelations.put(b.branch(),b.children()); }
            var thenMoves=arm(f,f.thenArm(),Branch.THEN,armRelations.getOrDefault(Branch.THEN,List.of()),call.header().id(),c);
            var elseMoves=arm(f,f.elseArm(),Branch.ELSE,armRelations.getOrDefault(Branch.ELSE,List.of()),call.header().id(),c);
            need(c,thenMoves.size()+elseMoves.size()+3==input.statements().size(),"arms and three root statements cover complete input exactly");
            StoragePremise.admit(input, c);
            if(!c.diagnostics.isEmpty())return rejected(c,Status.UNSUPPORTED_SLICE);
            return new Plan(c.result(Status.ADMITTED),ScalarDataOrder.canonical(c.data.values()),Optional.of(f),thenMoves,elseMoves,Optional.of(call),Optional.of(terminal));
        } catch(EntryGobackAdmission.LimitReached ex) { return rejected(c,Status.IMPLEMENTATION_LIMIT); }
    }
    private static List<MoveFact> arm(IfFact owner,IfArm arm,Branch branch,List<StatementId> children,StatementId merge,EntryGobackAdmission.Context c) {
        need(c,arm.contentAvailability()==Availability.KNOWN && arm.gapCodes().isEmpty() && arm.provenance().exact(),"complete arm content proof");
        if(arm.presence()==ClausePresence.ABSENT) {
            need(c,branch==Branch.ELSE && children.isEmpty() && arm.entry().statement().isEmpty() && arm.entry().availability()==Availability.UNAVAILABLE,"proven absent ELSE without executable entry");
            return List.of();
        }
        need(c,arm.presence()==ClausePresence.PRESENT && !children.isEmpty() && arm.entry().availability()==Availability.KNOWN
            && !children.isEmpty() && arm.entry().statement().equals(children.stream().findFirst()),"present arm explicit first canonical child");
        var moves=new ArrayList<MoveFact>();
        for(int i=0;i<children.size();i++) {
            c.touch();var s=c.lookup(children.get(i));
            if(!(s instanceof MoveFact m)) { need(c,false,"arm child must be MOVE; nested not flattened");continue; }
            need(c,m.header().containment().equals(new Containment(Optional.of(owner.header().id()),branch)),"arm membership agrees with direct child containment");
            var expected=i+1<children.size()?children.get(i+1):merge;
            need(c,m.normalContinuation().availability()==ContinuationAvailability.KNOWN && m.normalContinuation().statement().equals(Optional.of(expected))
                && m.normalContinuation().provenance().exact(),"MOVE completion follows canonical next child or IF completion");
            moves.add(m);
        }
        return List.copyOf(moves);
    }
    private static void need(EntryGobackAdmission.Context c,boolean value,String rule) { c.require(value,Rule.PROFILE_FACT,"simple-if",null,rule); }
    private static Plan rejected(EntryGobackAdmission.Context c,Status status) { return new Plan(c.result(status),List.of(),Optional.empty(),List.of(),List.of(),Optional.empty(),Optional.empty()); }
}
