package io.github.gustavo2358.lower.application;

import io.github.gustavo2358.lower.domain.SpInput;
import java.util.*;
import static io.github.gustavo2358.lower.domain.SpInput.*;
import static io.github.gustavo2358.lower.application.Admission.*;

/** Validates the closed SP1.6 isolated activation proof, without reconstructing source. */
public final class PerformAdmission implements AdmitInput {
    record Plan(Admission admission, List<DataFact> data, Optional<PerformFact> perform,
                List<MoveFact> prefix, List<MoveFact> body, Optional<CallFact> call, Optional<GobackFact> terminal) {
        List<MoveFact> moves() { var result = new ArrayList<>(prefix); result.addAll(body); return List.copyOf(result); }
    }
    @Override public Admission admit(SpInput input, Limits limits) { return plan(input, limits).admission(); }
    Plan plan(SpInput input, Limits limits) {
        var c = new EntryGobackAdmission.Context(input, Objects.requireNonNull(limits), true);
        try {
            if (input == null) { need(c, false, "materialized input required"); return rejected(c, Status.INVALID_INPUT); }
            EntryGobackAdmission.validate(input, c);
            if (!c.diagnostics.isEmpty()) return rejected(c, Status.INVALID_INPUT);
            CallAdmission.validateFacts(input, c);
            var performs = new ArrayList<PerformFact>(); var calls = new ArrayList<CallFact>(); var terminals = new ArrayList<GobackFact>();
            for (var statement : input.statements()) {
                c.touch();
                if (statement instanceof PerformFact p) {
                    performs.add(p); CallAdmission.continuation(p.normalContinuation(), p.header(), c);
                    p.target().ifPresent(t -> { c.identity(t.id().unit(), t.id().handle(), "procedure", t.paragraphOrigin()); c.provenance(t.referenceOrigin()); c.provenance(t.paragraphOrigin()); });
                    for (var id : p.targetStatements()) { c.touch(); need(c, id.unit().equals(input.unit()) && c.lookup(id) != null, "target member published in same unit"); }
                    for (var id : p.primaryStatements()) { c.touch(); need(c, id.unit().equals(input.unit()) && c.lookup(id) != null, "primary member published in same unit"); }
                } else if (statement instanceof CallFact k) calls.add(k);
                else if (statement instanceof GobackFact g) terminals.add(g);
            }
            if (!c.diagnostics.isEmpty()) return rejected(c, Status.INVALID_INPUT);
            c.phase = Phase.ADMISSION;
            need(c, performs.size() == 1 && calls.size() == 1 && terminals.size() == 1, "one PERFORM/callsite, one CALL, one GOBACK");
            if (!c.diagnostics.isEmpty()) return rejected(c, Status.UNSUPPORTED_SLICE);
            var p = performs.getFirst(); var call = calls.getFirst(); var terminal = terminals.getFirst();
            need(c, p.profile() == PerformProfile.SIMPLE_SINGLE_CALLSITE_PROCEDURE_PERFORM && p.gapCodes().isEmpty()
                && p.target().isPresent() && !p.targetStatements().isEmpty() && !p.primaryStatements().isEmpty(), "closed isolated single-callsite paragraph proof required");
            need(c, input.coverage().inventoryStatus() == InventoryStatus.COMPLETE && input.coverage().inputMissingStatements() == 0, "complete observed inventory");
            need(c, input.entryInventory().status() == InventoryStatus.PARTIAL && input.entryInventory().entries().size() == 1
                && input.entryInventory().gapCodes().equals(List.of("ALTERNATE_ENTRIES_NOT_PROJECTED")), "one primary entry with original scoped inventory remainder");
            if (!c.diagnostics.isEmpty()) return rejected(c, Status.UNSUPPORTED_SLICE);
            var main = p.primaryStatements(); var body = p.targetStatements(); int callsite = main.indexOf(p.header().id());
            need(c, callsite >= 0 && main.size() == callsite + 3, "optional MOVE prefix then PERFORM/CALL/GOBACK");
            need(c, p.targetEntry().equals(Optional.of(body.getFirst())) && p.targetExit().equals(Optional.of(body.getLast())), "body entry and exit must match ordered membership");
            need(c, p.normalContinuation().availability() == ContinuationAvailability.KNOWN
                && p.normalContinuation().statement().equals(Optional.of(call.header().id())) && p.normalContinuation().provenance().exact(), "unique static resume is the CALL");
            need(c, p.target().orElseThrow().referenceOrigin().exact() && p.target().orElseThrow().paragraphOrigin().exact(), "canonical target origins required");
            for (var e : input.entryInventory().entries()) need(c,
                e.availability() == Availability.KNOWN && e.coverage() == CoverageStatus.MODELED && e.start().availability() == Availability.KNOWN
                && e.start().statement().equals(Optional.of(main.getFirst())) && e.signature().availability() == Availability.KNOWN
                && e.signature().parameterCount().equals(Optional.of(0)) && e.signature().returningClause() == ReturningClause.ABSENT
                && e.provenance().exact() && e.gaps().isEmpty(), "explicit primary entry and zero signature; target is not entry");
            if (!c.diagnostics.isEmpty()) return rejected(c, Status.UNSUPPORTED_SLICE);
            need(c, main.get(callsite + 1).equals(call.header().id()) && main.get(callsite + 2).equals(terminal.header().id()), "unique resume followed by primary GOBACK");
            var members = new HashSet<StatementId>();
            for (var id : main) { c.touch(); need(c, members.add(id), "unique primary membership"); }
            for (var id : body) { c.touch(); need(c, members.add(id), "target disjoint from primary and each member unique"); }
            need(c, members.equals(c.statements.keySet()), "closed proof covers every statement; no other modeled entry or statement is dropped");
            var prefix = chain(main.subList(0, callsite), p.header().id(), c);
            var bodyMoves = chain(body, call.header().id(), c);
            need(c, call.normalContinuation().availability() == ContinuationAvailability.KNOWN
                && call.normalContinuation().statement().equals(Optional.of(terminal.header().id())), "CALL explicitly resumes at GOBACK");
            var s = call.surface();
            need(c, s.using() == ClausePresence.ABSENT && s.argumentCount().equals(Optional.of(0)) && s.returning() == ClausePresence.ABSENT
                && s.onException() == ClausePresence.ABSENT && s.notOnException() == ClausePresence.ABSENT && s.onOverflow() == ClausePresence.ABSENT, "CALL remains in supported surface");
            if (call.target() instanceof DataCallTarget d) CallAdmission.admitReference(d.reference(), OperandRole.CALL_TARGET, call.header(), c);
            else need(c, ((LiteralCallTarget) call.target()).logicalValue().isPresent(), "CALL requires logical literal");
            for (var statement : input.statements()) {
                c.touch(); var h = statement.header();
                need(c, h.coverage() == CoverageStatus.MODELED && h.provenance().exact() && h.containment().equals(new Containment(Optional.empty(), Branch.ROOT)), "modeled direct statements with exact origins");
                if (statement instanceof MoveFact m) CallAdmission.admitMove(m, c);
                else need(c, statement == p || statement == call || statement == terminal, "only linear MOVEs and admitted PERFORM/CALL/GOBACK");
            }
            for (var data : input.dataDeclarations()) { c.touch(); need(c, data.coverage() == CoverageStatus.MODELED && data.provenance().exact() && CallAdmission.scalar(data), "all DATA have admitted scalar storage"); }
            for (var gap : input.gaps()) { c.touch(); need(c, gap.statement().equals(call.header().id()) && gap.scope() == GapScope.RUNTIME_CALL_TARGET, "no unaccounted source gaps"); }
            if (input.dataDeclarations().size() > 1) StoragePremise.admit(input, c);
            if (!c.diagnostics.isEmpty()) return rejected(c, Status.UNSUPPORTED_SLICE);
            return new Plan(c.result(Status.ADMITTED), ScalarDataOrder.canonical(c.data.values()), Optional.of(p), prefix, bodyMoves, Optional.of(call), Optional.of(terminal));
        } catch (EntryGobackAdmission.LimitReached ex) { return rejected(c, Status.IMPLEMENTATION_LIMIT); }
    }
    private static List<MoveFact> chain(List<StatementId> members, StatementId end, EntryGobackAdmission.Context c) {
        var result = new ArrayList<MoveFact>();
        for (int i = 0; i < members.size(); i++) {
            c.touch(); var statement = c.lookup(members.get(i));
            if (!(statement instanceof MoveFact move)) { need(c, false, "body/prefix member must be MOVE"); continue; }
            var next = i + 1 < members.size() ? members.get(i + 1) : end;
            need(c, move.normalContinuation().availability() == ContinuationAvailability.KNOWN
                && move.normalContinuation().statement().equals(Optional.of(next)) && move.normalContinuation().provenance().exact(), "published MOVE relation agrees with body/prefix proof");
            result.add(move);
        }
        return List.copyOf(result);
    }
    private static void need(EntryGobackAdmission.Context c, boolean value, String rule) { c.require(value, Rule.PROFILE_FACT, "perform-basic", null, rule); }
    private static Plan rejected(EntryGobackAdmission.Context c, Status status) { return new Plan(c.result(status), List.of(), Optional.empty(), List.of(), List.of(), Optional.empty(), Optional.empty()); }
}
