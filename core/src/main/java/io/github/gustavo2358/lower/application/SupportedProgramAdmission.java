package io.github.gustavo2358.lower.application;

import io.github.gustavo2358.lower.domain.SpInput;
import java.util.*;
import static io.github.gustavo2358.lower.domain.SpInput.*;
import static io.github.gustavo2358.lower.application.Admission.*;

/** SUPPORTED_CP6_PROGRAM: closed typed composition, no physical-order control. */
public final class SupportedProgramAdmission implements AdmitInput {
    record Diamond(IfFact fact, List<MoveFact> thenMoves, List<MoveFact> elseMoves) { }
    record Plan(Admission admission, List<DataFact> data, List<StatementFact> primary,
                Map<StatementId, Diamond> diamonds, Optional<PerformFact> perform, List<MoveFact> body) {
        List<MoveFact> moves() {
            var moves = new ArrayList<MoveFact>();
            for (var s : primary) {
                if (s instanceof MoveFact m) moves.add(m);
                if (s instanceof IfFact f) { var d = diamonds.get(f.header().id()); moves.addAll(d.thenMoves()); moves.addAll(d.elseMoves()); }
            }
            moves.addAll(body); return List.copyOf(moves);
        }
        GobackFact terminal() { return (GobackFact) primary.getLast(); }
    }
    @Override public Admission admit(SpInput input, Limits limits) { return plan(input, limits).admission(); }
    Plan plan(SpInput input, Limits limits) {
        var c = new EntryGobackAdmission.Context(input, Objects.requireNonNull(limits), true);
        try {
            if (input == null) { need(c, false, "materialized input required"); return rejected(c, Status.INVALID_INPUT); }
            EntryGobackAdmission.validate(input, c);
            if (!c.diagnostics.isEmpty()) return rejected(c, Status.INVALID_INPUT);
            CallAdmission.validateFacts(input, c);
            for (var s : input.statements()) {
                if (s instanceof IfFact f) {
                    var operands = new HashSet<OperandId>();
                    for (var ref : f.conditionReads()) CallAdmission.reference(ref, f.header(), operands, c);
                    c.provenance(f.conditionProvenance()); c.provenance(f.predicateGuarantee().provenance());
                    c.provenance(f.thenArm().provenance()); c.provenance(f.elseArm().provenance());
                }
                if (s instanceof PerformFact p) {
                    p.target().ifPresent(t -> { c.identity(t.id().unit(), t.id().handle(), "procedure", t.paragraphOrigin()); c.provenance(t.referenceOrigin()); c.provenance(t.paragraphOrigin()); });
                    for (var ids : List.of(p.primaryStatements(), p.targetStatements())) for (var id : ids) {
                        c.touch(); need(c, id.unit().equals(input.unit()) && c.lookup(id) != null, "PERFORM member published in same unit");
                    }
                }
                var next = next(s); if (next != null) CallAdmission.continuation(next, s.header(), c);
            }
            if (!c.diagnostics.isEmpty()) return rejected(c, Status.INVALID_INPUT);
            c.phase = Phase.ADMISSION;
            need(c, input.coverage().inventoryStatus() == InventoryStatus.COMPLETE && input.coverage().inputMissingStatements() == 0, "complete observed inventory");
            need(c, input.entryInventory().status() == InventoryStatus.PARTIAL && input.entryInventory().entries().size() == 1
                && input.entryInventory().gapCodes().equals(List.of("ALTERNATE_ENTRIES_NOT_PROJECTED")), "one primary entry and original scoped inventory remainder");
            for (var e : input.entryInventory().entries()) need(c, e.availability() == Availability.KNOWN && e.coverage() == CoverageStatus.MODELED
                && e.start().availability() == Availability.KNOWN && e.signature().availability() == Availability.KNOWN
                && e.signature().parameterCount().equals(Optional.of(0)) && e.signature().returningClause() == ReturningClause.ABSENT
                && e.provenance().exact() && e.gaps().isEmpty(), "known explicit entry/start/zero signature");
            if (!c.diagnostics.isEmpty()) return rejected(c, Status.BLOCKED_LOWERING);
            var performs = new ArrayList<PerformFact>();
            for (var s : input.statements()) {
                c.touch(); var h = s.header();
                boolean missingCallContinuation = s instanceof CallFact call && call.normalContinuation().availability() != ContinuationAvailability.KNOWN;
                need(c, (h.coverage() == CoverageStatus.MODELED || missingCallContinuation) && h.provenance().exact(), "every observed statement modeled with exact provenance");
                if (s instanceof MoveFact m) CallAdmission.admitMove(m, c);
                else if (s instanceof CallFact call) admitCall(call, c);
                else if (s instanceof IfFact f) {
                    IfAdmission.admitPredicate(f, c);
                    need(c, f.continuation().equals(f.normalContinuation().statement()), "IF completion and structural continuation agree");
                } else if (s instanceof PerformFact p) performs.add(p);
                else need(c, s instanceof GobackFact, "only MOVE/IF/PERFORM/CALL/GOBACK; no filtering");
                if (!(s instanceof MoveFact)) need(c, root(s), "control statements are direct primary roots");
            }
            need(c, performs.size() <= 1, "at most one PERFORM callsite");
            for (var d : input.dataDeclarations()) { c.touch(); need(c, d.coverage() == CoverageStatus.MODELED && d.provenance().exact() && CallAdmission.scalar(d), "all DATA have admitted scalar storage"); }
            if (!c.diagnostics.isEmpty()) return rejected(c, Status.UNSUPPORTED_SLICE);
            // An unavailable continuation is a missing lowering prerequisite, never a fallthrough.
            for (var s : input.statements()) if (next(s) != null)
                need(c, next(s).availability() == ContinuationAvailability.KNOWN && next(s).provenance().exact(), "explicit exact normal continuation required");
            if (!c.diagnostics.isEmpty()) return rejected(c, Status.BLOCKED_LOWERING);
            for (var gap : input.gaps()) { c.touch(); need(c, c.lookup(gap.statement()) instanceof CallFact && gap.scope() == GapScope.RUNTIME_CALL_TARGET, "only site-specific runtime CALL source gaps"); }
            if (!c.diagnostics.isEmpty()) return rejected(c, Status.UNSUPPORTED_SLICE);
            var relations = new HashMap<StatementId, EnumMap<Branch,List<StatementId>>>();
            for (var branch : input.structure().branches()) {
                c.touch(); relations.computeIfAbsent(branch.parent(), id -> new EnumMap<>(Branch.class)).put(branch.branch(), branch.children());
            }
            var diamonds = new LinkedHashMap<StatementId, Diamond>(); var visited = new HashSet<StatementId>();
            var primary = new ArrayList<StatementFact>();
            var current = input.entryInventory().entries().getFirst().start().statement().orElseThrow();
            while (true) {
                c.touch(); var s = c.lookup(current);
                if (s == null || !root(s) || !visited.add(current)) { need(c, false, "closed acyclic primary flow with unique membership"); break; }
                primary.add(s);
                if (s instanceof GobackFact) break;
                if (s instanceof IfFact f) {
                    var arms = relations.get(f.header().id()); var merge = f.normalContinuation().statement().orElseThrow();
                    var thenMoves = IfAdmission.arm(f, f.thenArm(), Branch.THEN, arms.getOrDefault(Branch.THEN, List.of()), merge, c);
                    var elseMoves = IfAdmission.arm(f, f.elseArm(), Branch.ELSE, arms.getOrDefault(Branch.ELSE, List.of()), merge, c);
                    for (var moves : List.of(thenMoves, elseMoves)) for (var m : moves) need(c, visited.add(m.header().id()), "unique disjoint IF arm membership");
                    diamonds.put(current, new Diamond(f, thenMoves, elseMoves));
                }
                if (next(s) == null) { need(c, false, "unsupported primary member"); break; }
                current = next(s).statement().orElseThrow();
            }
            var body = new ArrayList<MoveFact>();
            if (!performs.isEmpty()) {
                var p = performs.getFirst();
                need(c, p.profile() == PerformProfile.SIMPLE_SINGLE_CALLSITE_PROCEDURE_PERFORM && p.gapCodes().isEmpty() && p.target().isPresent()
                    && !p.targetStatements().isEmpty(), "isolated BASIC paragraph activation proof");
                need(c, p.primaryStatements().equals(primary.stream().map(s -> s.header().id()).toList()), "published complete primary membership agrees with explicit flow");
                if (!c.diagnostics.isEmpty()) return rejected(c, Status.UNSUPPORTED_SLICE);
                var members = p.targetStatements();
                need(c, p.targetEntry().equals(Optional.of(members.getFirst())) && p.targetExit().equals(Optional.of(members.getLast())), "target entry/exit agree with ordered body");
                need(c, p.target().orElseThrow().referenceOrigin().exact() && p.target().orElseThrow().paragraphOrigin().exact(), "exact canonical target proof");
                for (int i = 0; i < members.size(); i++) {
                    c.touch(); var s = c.lookup(members.get(i));
                    need(c, visited.add(members.get(i)) && s instanceof MoveFact && root(s), "disjoint nonempty linear MOVE target with no ordinary activation");
                    if (!(s instanceof MoveFact m)) continue;
                    need(c, m.normalContinuation().statement().equals(i + 1 < members.size() ? Optional.of(members.get(i + 1)) : p.normalContinuation().statement()), "target MOVE completion reaches next member or unique resume");
                    body.add(m);
                }
            }
            need(c, !primary.isEmpty() && primary.getLast() instanceof GobackFact && visited.equals(c.statements.keySet()), "primary GOBACK and full statement closure; no dropped or orphan source statements");
            if (!diamonds.isEmpty() || !performs.isEmpty() && input.dataDeclarations().size() > 1
                    || input.storageIndependence().filter(p -> p.availability() == Availability.KNOWN).isPresent()) StoragePremise.admit(input, c);
            if (!c.diagnostics.isEmpty()) return rejected(c, Status.UNSUPPORTED_SLICE);
            return new Plan(c.result(Status.ADMITTED), ScalarDataOrder.canonical(c.data.values()), List.copyOf(primary), Map.copyOf(diamonds), performs.stream().findFirst(), List.copyOf(body));
        } catch (EntryGobackAdmission.LimitReached ex) { return rejected(c, Status.IMPLEMENTATION_LIMIT); }
    }
    static NormalContinuation next(StatementFact s) {
        return switch (s) { case MoveFact m -> m.normalContinuation(); case CallFact k -> k.normalContinuation();
            case IfFact f -> f.normalContinuation(); case PerformFact p -> p.normalContinuation(); default -> null; };
    }
    private static boolean root(StatementFact s) { return s.header().containment().equals(new Containment(Optional.empty(), Branch.ROOT)); }
    private static void admitCall(CallFact call, EntryGobackAdmission.Context c) {
        var s = call.surface();
        need(c, s.using() == ClausePresence.ABSENT && s.argumentCount().equals(Optional.of(0)) && s.returning() == ClausePresence.ABSENT
            && s.onException() == ClausePresence.ABSENT && s.notOnException() == ClausePresence.ABSENT && s.onOverflow() == ClausePresence.ABSENT, "CALL stays in qualified surface");
        if (call.target() instanceof DataCallTarget d) CallAdmission.admitReference(d.reference(), OperandRole.CALL_TARGET, call.header(), c);
        else need(c, ((LiteralCallTarget) call.target()).logicalValue().isPresent(), "CALL published logical literal");
    }
    private static void need(EntryGobackAdmission.Context c, boolean value, String rule) { c.require(value, Rule.PROFILE_FACT, "supported-program", null, rule); }
    private static Plan rejected(EntryGobackAdmission.Context c, Status status) { return new Plan(c.result(status), List.of(), List.of(), Map.of(), Optional.empty(), List.of()); }
}
