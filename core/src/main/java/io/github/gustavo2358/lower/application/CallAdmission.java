package io.github.gustavo2358.lower.application;

import io.github.gustavo2358.lower.domain.SpInput;
import java.util.*;
import static io.github.gustavo2358.lower.domain.SpInput.*;
import static io.github.gustavo2358.lower.application.Admission.*;

/** CP6 first CALL slice. All joins use the common per-input indices, never source order. */
public final class CallAdmission implements AdmitInput {
    record Plan(Admission admission, List<DataFact> data, List<MoveFact> moves,
                Optional<CallFact> call, Optional<GobackFact> terminal) { }
    @Override public Admission admit(SpInput input, Limits limits) { return plan(input, limits).admission(); }

    Plan plan(SpInput input, Limits limits) {
        var c = new EntryGobackAdmission.Context(input, Objects.requireNonNull(limits), true);
        try {
            if (input == null) {
                c.require(false, Rule.INPUT_REQUIRED, "input", null, "Materialized input required");
                return rejected(c, Status.INVALID_INPUT);
            }
            EntryGobackAdmission.validate(input, c);
            if (!c.diagnostics.isEmpty()) return rejected(c, Status.INVALID_INPUT);
            validateFacts(input, c);
            if (!c.diagnostics.isEmpty()) return rejected(c, Status.INVALID_INPUT);
            c.phase = Phase.ADMISSION;
            c.require(input.coverage().inventoryStatus() == InventoryStatus.COMPLETE && input.coverage().inputMissingStatements() == 0
                    && input.entryInventory().status() == InventoryStatus.PARTIAL && input.entryInventory().entries().size() == 1,
                Rule.PROFILE_FACT, "inventory", null, "Complete observed input and one primary entry required");
            for (var e : input.entryInventory().entries())
                c.require(e.availability() == Availability.KNOWN && e.start().availability() == Availability.KNOWN
                        && e.signature().availability() == Availability.KNOWN && e.coverage() == CoverageStatus.MODELED && e.gaps().isEmpty(),
                    Rule.PROFILE_FACT, e.id().handle(), e.provenance(), "Known primary entry/start/zero-arity signature required");
            if (!c.diagnostics.isEmpty()) return rejected(c, Status.BLOCKED_LOWERING);
            for (var d : input.dataDeclarations()) {
                c.touch();
                c.require(d.coverage() == CoverageStatus.MODELED && scalar(d), Rule.PROFILE_FACT, d.id().handle(),
                    d.provenance(), "Published local WORKING_STORAGE scalar TEXT required");
            }
            input.storageIndependence().filter(p -> p.availability() == Availability.KNOWN)
                .ifPresent(p -> StoragePremise.admit(input, c));
            int calls = 0;
            for (var statement : input.statements()) {
                c.touch(); var h = statement.header();
                boolean continuationGap = statement instanceof CallFact k && k.normalContinuation().availability() != ContinuationAvailability.KNOWN;
                c.require((h.coverage() == CoverageStatus.MODELED || continuationGap) && h.containment().branch() == Branch.ROOT,
                    Rule.PROFILE_SHAPE, h.id().handle(), h.provenance(), "Modeled root statement required");
                if (statement instanceof MoveFact m) admitMove(m, c);
                else if (statement instanceof CallFact call) {
                    calls++;
                    var s = call.surface();
                    c.require(s.using() == ClausePresence.ABSENT && s.returning() == ClausePresence.ABSENT
                            && s.onException() == ClausePresence.ABSENT && s.notOnException() == ClausePresence.ABSENT
                            && s.onOverflow() == ClausePresence.ABSENT,
                        Rule.PROFILE_SHAPE, h.id().handle(), h.provenance(), "CALL USING/RETURNING/handlers not supported");
                    if (call.target() instanceof DataCallTarget d) admitReference(d.reference(), OperandRole.CALL_TARGET, h, c);
                    else c.require(((LiteralCallTarget) call.target()).logicalValue().isPresent(), Rule.PROFILE_FACT,
                        h.id().handle(), call.target().provenance(), "Published logical literal required");
                } else c.require(statement instanceof GobackFact, Rule.PROFILE_SHAPE, h.id().handle(), h.provenance(),
                    "Only supported MOVEs, one CALL and terminal GOBACK; no filtering");
            }
            c.require(calls == 1, Rule.PROFILE_SHAPE, "chain", null, "First CALL slice requires exactly one CALL");
            if (!c.diagnostics.isEmpty()) return rejected(c, Status.UNSUPPORTED_SLICE);
            for (var statement : input.statements()) {
                NormalContinuation next = statement instanceof MoveFact m ? m.normalContinuation()
                    : statement instanceof CallFact call ? call.normalContinuation() : null;
                if (next != null) c.require(next.availability() == ContinuationAvailability.KNOWN, Rule.PROFILE_FACT,
                    statement.header().id().handle(), next.provenance(), "Explicit normal continuation is required");
            }
            if (!c.diagnostics.isEmpty()) return rejected(c, Status.BLOCKED_LOWERING);
            var moves = new ArrayList<MoveFact>(); var visited = new HashSet<StatementId>();
            var current = input.entryInventory().entries().getFirst().start().statement().orElseThrow();
            CallFact call = null; GobackFact terminal = null;
            while (visited.add(current)) {
                c.touch(); var statement = c.lookup(current);
                if (statement instanceof GobackFact g) { terminal = g; break; }
                if (statement instanceof CallFact k) { call = k; current = k.normalContinuation().statement().orElseThrow(); }
                else {
                    if (call != null) break; // Post-CALL work is outside this slice.
                    var move = (MoveFact) statement; moves.add(move); current = move.normalContinuation().statement().orElseThrow();
                }
            }
            c.require(call != null && terminal != null && visited.size() == input.statements().size()
                    && call.normalContinuation().statement().equals(Optional.ofNullable(terminal).map(g -> g.header().id())),
                Rule.PROFILE_SHAPE, "chain", null, "Explicit chain covers every statement once: MOVEs, CALL, its GOBACK continuation");
            if (!c.diagnostics.isEmpty()) return rejected(c, Status.UNSUPPORTED_SLICE);
            return new Plan(c.result(Status.ADMITTED), ScalarDataOrder.canonical(c.data.values()), List.copyOf(moves), Optional.of(call), Optional.of(terminal));
        } catch (EntryGobackAdmission.LimitReached ex) { return rejected(c, Status.IMPLEMENTATION_LIMIT); }
    }
    static void validateFacts(SpInput input, EntryGobackAdmission.Context c) {
            var operands = new HashSet<OperandId>();
            for (var statement : input.statements()) {
                c.touch();
                if (statement instanceof MoveFact m) {
                    if (m.source() instanceof DataReference read) reference(read, m.header(), operands, c);
                    else {
                        operand(m.source().id(), m.header(), operands, c);
                        c.provenance(m.source().provenance());
                    }
                    reference(m.target(), m.header(), operands, c);
                    continuation(m.normalContinuation(), m.header(), c);
                    if (m.source() instanceof LiteralSource literal) literal.logicalValue().ifPresent(v -> logical(v, m.header(), c));
                    c.require((m.copySemantics() == CopySemantics.FITTED_TEXT) == m.textAdjustment().isPresent(),
                        Rule.PROFILE_FACT, m.header().id().handle(), m.header().provenance(), "FITTED_TEXT iff textAdjustment present");
                    m.textAdjustment().ifPresent(a -> {
                        c.provenance(a.provenance()); logical(a.result(), m.header(), c);
                        c.require(a.receiverExtent() > 0 && a.receiverExtent() == a.result().logicalExtent(), Rule.PROFILE_FACT,
                            m.header().id().handle(), a.provenance(), "Adjustment result extent equals positive receiver extent");
                    });
                } else if (statement instanceof CallFact call) {
                    if (call.target() instanceof DataCallTarget d) reference(d.reference(), call.header(), operands, c);
                    else {
                        var l = (LiteralCallTarget) call.target(); operand(l.id(), call.header(), operands, c); c.provenance(l.provenance());
                        l.logicalValue().ifPresent(v -> {
                            logical(v, call.header(), c);
                            c.require(v.value().equals(l.text()), Rule.PROFILE_FACT, l.id().handle(), l.provenance(), "Literal text agrees with logical value");
                        });
                    }
                    c.require((call.syntax() == CallSyntax.LITERAL_PROGRAM_NAME) == (call.target() instanceof LiteralCallTarget),
                        Rule.PROFILE_FACT, call.header().id().handle(), call.target().provenance(), "CALL syntax agrees with target variant");
                    continuation(call.normalContinuation(), call.header(), c);
                    var surface = call.surface();
                    boolean count = switch (surface.using()) {
                        case ABSENT -> surface.argumentCount().equals(Optional.of(0));
                        case PRESENT -> surface.argumentCount().filter(n -> n > 0).isPresent();
                        case UNKNOWN -> surface.argumentCount().isEmpty();
                    };
                    c.require(count && !call.runtimeUncertaintyCode().isBlank(), Rule.PROFILE_FACT, call.header().id().handle(),
                        call.header().provenance(), "USING presence/count and runtime uncertainty must be coherent");
                }
            }
    }
    private static Plan rejected(EntryGobackAdmission.Context c, Status status) {
        return new Plan(c.result(status), List.of(), List.of(), Optional.empty(), Optional.empty());
    }
    static void operand(OperandId id, StatementHeader h, Set<OperandId> seen, EntryGobackAdmission.Context c) {
        String prefix = "operand:" + h.id().handle().substring("statement:".length()) + ":";
        boolean valid = false;
        if (id.handle().startsWith(prefix)) try {
            var suffix = id.handle().substring(prefix.length()); int n = Integer.parseInt(suffix);
            valid = n >= 0 && Integer.toString(n).equals(suffix);
        } catch (NumberFormatException ignored) { /* Identity grammar only. */ }
        c.require(id.statement().equals(h.id()) && valid && seen.add(id), Rule.IDENTITY, id.handle(), h.provenance(),
            "Unique canonical operand identity owned by this statement");
    }
    static void reference(DataReference reference, StatementHeader h, Set<OperandId> seen, EntryGobackAdmission.Context c) {
        operand(reference.id(), h, seen, c); c.provenance(reference.provenance());
        var binding = reference.binding(); var candidates = new HashSet<DataId>();
        for (var d : binding.candidates()) {
            c.touch(); c.require(d.unit().equals(c.input.unit()) && c.data(d) != null && candidates.add(d), Rule.PROFILE_FACT,
                h.id().handle(), reference.provenance(), "Distinct published DATA candidates required");
        }
        boolean selection = binding.status() == ResolutionStatus.RESOLVED
            ? binding.candidates().size() == 1 && binding.selected().filter(candidates::contains).isPresent()
            : binding.selected().isEmpty();
        c.require(selection && (binding.reason().isEmpty() || binding.candidateNames().size() == binding.candidates().size()),
            Rule.PROFILE_FACT, h.id().handle(), reference.provenance(), "Binding selection and candidate evidence coherent");
        reference.wholeItemAccess().ifPresent(w -> c.require(binding.status() == ResolutionStatus.RESOLVED
                && binding.selected().equals(Optional.of(w.data())) && c.data(w.data()) != null,
            Rule.PROFILE_FACT, h.id().handle(), reference.provenance(), "Whole-item proof agrees with uniquely selected DATA"));
    }
    static void continuation(NormalContinuation next, StatementHeader h, EntryGobackAdmission.Context c) {
        c.provenance(next.provenance());
        c.require((next.availability() == ContinuationAvailability.KNOWN) == next.statement().isPresent(), Rule.PROFILE_FACT,
            h.id().handle(), next.provenance(), "Only KNOWN normal continuation carries a reference");
        next.statement().ifPresent(id -> c.require(id.unit().equals(c.input.unit()) && c.lookup(id) != null, Rule.PROFILE_FACT,
            h.id().handle(), next.provenance(), "Continuation references a published statement in this unit"));
    }
    static void logical(LogicalValue value, StatementHeader h, EntryGobackAdmission.Context c) {
        c.require(value.logicalExtent() >= 0 && value.logicalExtent() == value.value().codePointCount(0, value.value().length()),
            Rule.PROFILE_FACT, h.id().handle(), h.provenance(), "Published logical TEXT extent coherent with value");
    }
    static boolean scalar(DataFact d) {
        return d.scalarText().filter(t -> t.logicalDomain() == LogicalDomain.TEXT && t.logicalExtent() > 0
            && t.storageClass() == StorageClass.WORKING_STORAGE && t.declarationScope() == DeclarationScope.LOCAL).isPresent();
    }
    static void admitReference(DataReference reference, OperandRole role, StatementHeader h, EntryGobackAdmission.Context c) {
        var b = reference.binding();
        c.require(reference.role() == role && b.status() == ResolutionStatus.RESOLVED && b.selected().isPresent()
                && b.candidates().size() == 1 && reference.wholeItemAccess().isPresent(), Rule.PROFILE_FACT,
            h.id().handle(), reference.provenance(), "Unique resolved scalar whole-item access required; no candidate selection");
        reference.wholeItemAccess().ifPresent(w -> c.require(scalar(c.data(w.data())), Rule.PROFILE_FACT,
            h.id().handle(), reference.provenance(), "Whole-item target has scalar TEXT proof"));
    }
    static void admitMove(MoveFact m, EntryGobackAdmission.Context c) {
        var h = m.header();
        admitReference(m.target(), OperandRole.WRITE, h, c);
        if (m.source() instanceof DataReference read) {
            admitReference(read, OperandRole.READ, h, c);
            c.require(m.copySemantics() == CopySemantics.FULL_IDENTITY && m.textAdjustment().isEmpty(), Rule.PROFILE_FACT,
                h.id().handle(), h.provenance(), "Data copy requires FULL_IDENTITY; no data fitting");
            if (read.wholeItemAccess().isPresent() && m.target().wholeItemAccess().isPresent()) {
                var source = c.data(read.wholeItemAccess().orElseThrow().data());
                var target = c.data(m.target().wholeItemAccess().orElseThrow().data());
                c.require(source.scalarText().isPresent() && target.scalarText().isPresent()
                        && source.scalarText().orElseThrow().logicalExtent() == target.scalarText().orElseThrow().logicalExtent(),
                    Rule.PROFILE_FACT, h.id().handle(), h.provenance(), "Data copy requires equal scalar extents");
            }
            return;
        }
        var literal = (LiteralSource) m.source();
        var value = literal.logicalValue();
        c.require(literal.kind() == LiteralKind.ALPHANUMERIC && value.isPresent(), Rule.PROFILE_FACT,
            h.id().handle(), m.source().provenance(), "Published logical TEXT source required");
        c.require(m.copySemantics() != CopySemantics.UNAVAILABLE, Rule.PROFILE_FACT, h.id().handle(), h.provenance(),
            "FULL_IDENTITY or FITTED_TEXT required; truncation remains unsupported");
        if (m.target().wholeItemAccess().isPresent() && value.isPresent()) {
            var data = c.data(m.target().wholeItemAccess().orElseThrow().data());
            if (data.scalarText().isPresent()) {
                int extent = data.scalarText().orElseThrow().logicalExtent();
                boolean coherent = m.copySemantics() == CopySemantics.FULL_IDENTITY
                    ? extent == value.orElseThrow().logicalExtent()
                    : m.textAdjustment().filter(a -> a.receiverExtent() == extent && value.orElseThrow().logicalExtent() < extent).isPresent();
                c.require(coherent, Rule.PROFILE_FACT, h.id().handle(), h.provenance(), "Published source/receiver/adjustment extents agree; no fitting performed");
            }
        }
    }
}
