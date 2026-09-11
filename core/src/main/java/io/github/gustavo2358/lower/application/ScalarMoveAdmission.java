package io.github.gustavo2358.lower.application;

import io.github.gustavo2358.lower.domain.SpInput;
import java.util.*;
import static io.github.gustavo2358.lower.domain.SpInput.*;
import static io.github.gustavo2358.lower.application.Admission.*;

/** Closed scalar-text-move@1 profile; the validated indices and chain are reused by translation. */
public final class ScalarMoveAdmission implements AdmitInput {
    record Plan(Admission admission, List<DataFact> data, List<MoveFact> moves, Optional<GobackFact> terminal) { }
    @Override public Admission admit(SpInput input, Limits limits) { return plan(input, limits).admission(); }
    Plan plan(SpInput input, Limits limits) {
        var c = new EntryGobackAdmission.Context(input, Objects.requireNonNull(limits), true);
        var moves = new ArrayList<MoveFact>();
        try {
            if (input == null) {
                c.require(false, Rule.INPUT_REQUIRED, "input", null, "Materialized input required");
                return rejected(c, Status.INVALID_INPUT);
            }
            // Common physical-independent input validation builds DATA and statement maps once.
            EntryGobackAdmission.validate(input, c);
            var operands = new HashSet<OperandId>();
            for (var statement : input.statements()) if (statement instanceof MoveFact m) {
                c.touch(); var h = m.header();
                c.require((m.copySemantics() == CopySemantics.FITTED_TEXT) == m.textAdjustment().isPresent(), Rule.PROFILE_FACT,
                    h.id().handle(), h.provenance(), "FITTED_TEXT iff textAdjustment present");
                for (var id : List.of(m.source().id(), m.target().id())) {
                    c.require(id.statement().equals(h.id()) && validOperand(id) && operands.add(id), Rule.IDENTITY,
                            id.handle(), h.provenance(), "Unique operand occurrence owned by its statement");
                }
                c.provenance(m.source().provenance()); c.provenance(m.target().provenance()); c.provenance(m.normalContinuation().provenance());
                var b = m.target().binding(); var candidates = new HashSet<DataId>();
                for (var candidate : b.candidates()) {
                    c.touch();
                    c.require(candidate.unit().equals(input.unit()) && c.data(candidate) != null && candidates.add(candidate),
                        Rule.PROFILE_FACT, h.id().handle(), h.provenance(), "Binding candidates reference distinct published DATA");
                }
                b.selected().ifPresent(d -> c.require(d.unit().equals(input.unit()) && c.data(d) != null && candidates.contains(d),
                    Rule.PROFILE_FACT, h.id().handle(), h.provenance(), "Selected DATA must exist among candidates"));
                m.target().wholeItemAccess().ifPresent(w -> c.require(w.data().unit().equals(input.unit()) && c.data(w.data()) != null,
                    Rule.PROFILE_FACT, h.id().handle(), h.provenance(), "Whole item references published DATA"));
                var next = m.normalContinuation();
                c.require((next.availability() == ContinuationAvailability.KNOWN) == next.statement().isPresent(), Rule.PROFILE_FACT,
                    h.id().handle(), next.provenance(), "Only KNOWN continuation carries a reference");
                next.statement().ifPresent(d -> c.require(d.unit().equals(input.unit()) && c.lookup(d) != null,
                    Rule.PROFILE_FACT, h.id().handle(), next.provenance(), "Continuation references a published statement"));
            }
            if (!c.diagnostics.isEmpty()) return rejected(c, Status.INVALID_INPUT);
            c.phase = Phase.ADMISSION;
            c.require(input.coverage().inventoryStatus() == InventoryStatus.COMPLETE && input.coverage().inputMissingStatements() == 0
                    && input.entryInventory().status() == InventoryStatus.PARTIAL && input.entryInventory().entries().size() == 1,
                Rule.PROFILE_FACT, "inventory", null, "Complete observed inventory and one known primary entry; alternate inventory remains PARTIAL");
            for (var e : input.entryInventory().entries())
                c.require(e.availability() == Availability.KNOWN && e.start().availability() == Availability.KNOWN
                        && e.signature().availability() == Availability.KNOWN && e.coverage() == CoverageStatus.MODELED && e.gaps().isEmpty(),
                    Rule.PROFILE_FACT, e.id().handle(), e.provenance(), "Known primary start and zero/ABSENT signature required");
            for (var d : input.dataDeclarations()) {
                c.touch();
                c.require(d.coverage() == CoverageStatus.MODELED && d.scalarText().filter(t -> t.logicalDomain() == LogicalDomain.TEXT
                    && t.logicalExtent() > 0 && t.storageClass() == StorageClass.WORKING_STORAGE && t.declarationScope() == DeclarationScope.LOCAL).isPresent(),
                    Rule.PROFILE_FACT, d.id().handle(), d.provenance(), "scalarText TEXT positive extent WORKING_STORAGE LOCAL required");
            }
            for (var statement : input.statements()) {
                c.touch(); var h = statement.header();
                c.require(h.coverage() == CoverageStatus.MODELED && h.containment().branch() == Branch.ROOT,
                    Rule.PROFILE_FACT, h.id().handle(), h.provenance(), "Modeled root statement required");
                if (statement instanceof MoveFact m) {
                    var b = m.target().binding();
                    c.require(m.copySemantics() == CopySemantics.FULL_IDENTITY, Rule.PROFILE_FACT, h.id().handle(), h.provenance(), "FULL_IDENTITY proof required");
                    c.require(m.source().kind() == LiteralKind.ALPHANUMERIC && m.source().logicalValue().filter(v -> v.logicalDomain() == LogicalDomain.TEXT && v.logicalExtent() > 0).isPresent(),
                        Rule.PROFILE_FACT, h.id().handle(), m.source().provenance(), "Explicit TEXT logicalValue required");
                    c.require(m.target().role() == OperandRole.WRITE && b.status() == ResolutionStatus.RESOLVED && b.selected().isPresent() && b.candidates().size() == 1,
                        Rule.PROFILE_FACT, h.id().handle(), m.target().provenance(), "WRITE with unique RESOLVED selected required");
                    c.require(m.target().wholeItemAccess().isPresent(), Rule.PROFILE_FACT, h.id().handle(), m.target().provenance(), "wholeItemAccess proof required");
                    if (m.target().wholeItemAccess().isPresent()) {
                        var data = c.data(m.target().wholeItemAccess().orElseThrow().data());
                        c.require(b.selected().equals(Optional.of(data.id())), Rule.PROFILE_FACT, h.id().handle(), m.target().provenance(), "selected equals wholeItemAccess");
                        c.require(data.scalarText().isPresent() && m.source().logicalValue().isPresent()
                                && data.scalarText().orElseThrow().logicalExtent() == m.source().logicalValue().orElseThrow().logicalExtent(),
                            Rule.PROFILE_FACT, h.id().handle(), h.provenance(), "Published logical extents agree; no String/PIC interpretation");
                    }
                    c.require(m.normalContinuation().availability() == ContinuationAvailability.KNOWN, Rule.PROFILE_FACT,
                        h.id().handle(), m.normalContinuation().provenance(), "Known explicit normalContinuation required");
                } else if (!(statement instanceof GobackFact))
                    c.require(false, Rule.PROFILE_SHAPE, h.id().handle(), h.provenance(), "Only typed MOVE and terminal GOBACK supported; no filtering");
            }
            if (!c.diagnostics.isEmpty()) return rejected(c, Status.UNSUPPORTED_SLICE);
            var visited = new HashSet<StatementId>();
            StatementId current = input.entryInventory().entries().getFirst().start().statement().orElseThrow();
            GobackFact terminal = null;
            while (visited.add(current)) {
                c.touch(); var statement = c.lookup(current);
                if (statement instanceof GobackFact g) { terminal = g; break; }
                var move = (MoveFact) statement; moves.add(move);
                current = move.normalContinuation().statement().orElseThrow();
            }
            c.require(terminal != null && visited.size() == input.statements().size() && !moves.isEmpty() && !input.dataDeclarations().isEmpty(),
                Rule.PROFILE_SHAPE, "chain", null, "Explicit chain must cover all statements once and terminate at GOBACK, with DATA and MOVE");
            if (!c.diagnostics.isEmpty()) return rejected(c, Status.UNSUPPORTED_SLICE);
            return new Plan(c.result(Status.ADMITTED), ScalarDataOrder.canonical(c.data.values()), List.copyOf(moves), Optional.of(terminal));
        } catch (EntryGobackAdmission.LimitReached ex) { return rejected(c, Status.IMPLEMENTATION_LIMIT); }
    }
    private static Plan rejected(EntryGobackAdmission.Context c, Status status) { return new Plan(c.result(status), List.of(), List.of(), Optional.empty()); }
    private static boolean validOperand(OperandId id) {
        if (!id.statement().handle().startsWith("statement:")) return false;
        String owner = id.statement().handle().substring("statement:".length());
        String prefix = "operand:" + owner + ":";
        if (!id.handle().startsWith(prefix)) return false;
        try { String suffix = id.handle().substring(prefix.length()); int n = Integer.parseInt(suffix); return n >= 0 && Integer.toString(n).equals(suffix); }
        catch (NumberFormatException ex) { return false; }
    }
}
