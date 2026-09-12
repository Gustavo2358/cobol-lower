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
            CallAdmission.validateFacts(input, c);
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
            input.storageIndependence().filter(p -> p.availability() == Availability.KNOWN)
                .ifPresent(p -> StoragePremise.admit(input, c));
            for (var statement : input.statements()) {
                c.touch(); var h = statement.header();
                c.require(h.coverage() == CoverageStatus.MODELED && h.containment().branch() == Branch.ROOT,
                    Rule.PROFILE_FACT, h.id().handle(), h.provenance(), "Modeled root statement required");
                if (statement instanceof MoveFact m) {
                    CallAdmission.admitMove(m, c);
                    c.require(m.copySemantics() == CopySemantics.FULL_IDENTITY, Rule.PROFILE_FACT,
                        h.id().handle(), h.provenance(), "Scalar-only profile requires FULL_IDENTITY");
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
}
