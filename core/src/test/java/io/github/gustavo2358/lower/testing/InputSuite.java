package io.github.gustavo2358.lower.testing;

import io.github.gustavo2358.lower.application.Admission;
import io.github.gustavo2358.lower.application.AdmitInput;
import io.github.gustavo2358.lower.application.EntryGobackAdmission;
import io.github.gustavo2358.lower.domain.SpInput;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import static io.github.gustavo2358.lower.domain.SpInput.*;
import static io.github.gustavo2358.lower.application.Admission.*;
import static io.github.gustavo2358.lower.testing.SpFixtures.*;

public final class InputSuite {
    private static final AdmitInput PORT = new EntryGobackAdmission();
    private static final AdmitInput.Limits LIMITS = new AdmitInput.Limits(100);
    private static int count;
    private InputSuite() { }
    private static void check(boolean value, String message) { if (!value) throw new AssertionError(message); count++; }
    private static Admission expect(SpInput input, Status status, Rule rule) {
        var result = PORT.admit(input, LIMITS);
        check(result.status() == status, "expected " + status + " for " + rule + ", got " + result);
        check(result.input().equals(Optional.ofNullable(input)), "snapshot/inventory never amputated");
        if (rule != null) check(result.diagnostics().stream().anyMatch(d -> d.rule() == rule), "expected specific rule " + rule);
        check(result.diagnostics().stream().allMatch(d -> !d.requirement().isBlank() && !d.subject().isBlank()), "typed diagnostic must explain requirement/scope");
        return result;
    }
    public static int run() {
        var input = minimal(); var e = input.entryInventory().entries().get(0); var h = input.statements().get(0).header();
        expect(input, Status.ADMITTED, null);
        expect(null, Status.INVALID_INPUT, Rule.INPUT_REQUIRED);
        var alternate = minimal(new UnitKey("other.cbl", List.of(3, 7), "OTHER"), "entry:29", "statement:73");
        expect(alternate, Status.ADMITTED, null);
        check(!input.statements().get(0).header().id().equals(alternate.statements().get(0).header().id()), "independent namespaces");

        for (StatementId target : List.of(new StatementId(input.unit(), "statement:999"), new StatementId(alternate.unit(), "statement:0"))) {
            expect(entry(input, entryFacts(e, new ExecutableStart(Availability.KNOWN, Optional.of(target)), e.signature(), e.coverage(), e.readiness(), e.gaps())), Status.INVALID_INPUT, Rule.ENTRY_START);
        }
        expect(entry(input, entryFacts(e, new ExecutableStart(Availability.KNOWN, Optional.empty()), e.signature(), e.coverage(), e.readiness(), e.gaps())), Status.INVALID_INPUT, Rule.ENTRY_START);
        expect(entry(input, entryFacts(e, new ExecutableStart(Availability.UNAVAILABLE, e.start().statement()), e.signature(), e.coverage(), e.readiness(), e.gaps())), Status.INVALID_INPUT, Rule.ENTRY_START);
        for (EntrySignature bad : List.of(new EntrySignature(Availability.KNOWN, Optional.empty(), ReturningClause.ABSENT), new EntrySignature(Availability.KNOWN, Optional.of(1), ReturningClause.ABSENT), new EntrySignature(Availability.KNOWN, Optional.of(0), ReturningClause.PRESENT), new EntrySignature(Availability.UNAVAILABLE, Optional.of(0), ReturningClause.UNKNOWN), new EntrySignature(Availability.PARTIAL, Optional.of(-1), ReturningClause.ABSENT))) {
            expect(entry(input, entryFacts(e, e.start(), bad, e.coverage(), e.readiness(), e.gaps())), Status.INVALID_INPUT, Rule.SIGNATURE);
        }
        for (Availability unavailable : List.of(Availability.UNAVAILABLE, Availability.INPUT_MISSING, Availability.PARTIAL)) {
            var signature = new EntrySignature(unavailable, unavailable == Availability.PARTIAL ? Optional.of(1) : Optional.empty(), unavailable == Availability.PARTIAL ? ReturningClause.PRESENT : ReturningClause.UNKNOWN);
            expect(entry(input, entryFacts(e, e.start(), signature, CoverageStatus.PARTIAL, blocked(), List.of(new EntryGap(GapScope.ENTRY_SIGNATURE, "ENTRY_SIGNATURE_NOT_PROJECTED", "signature unavailable", e.provenance())))), Status.BLOCKED_LOWERING, Rule.PROFILE_FACT);
        }
        var startGap = new EntryGap(GapScope.ENTRY_START, "PRIMARY_ENTRY_START_NOT_AVAILABLE", "start unavailable", e.provenance());
        expect(entry(input, entryFacts(e, new ExecutableStart(Availability.UNAVAILABLE, Optional.empty()), e.signature(), CoverageStatus.PARTIAL, blocked(), List.of(startGap))), Status.BLOCKED_LOWERING, Rule.PROFILE_FACT);
        expect(entry(input, entryFacts(e, new ExecutableStart(Availability.UNAVAILABLE, Optional.empty()), e.signature(), CoverageStatus.PARTIAL, blocked(), List.of())), Status.INVALID_INPUT, Rule.ENTRY_STATE);

        var inv = input.entryInventory();
        expect(inventory(input, new EntryInventory(InventoryStatus.COMPLETE, inv.scope(), inv.entries(), inv.gapCodes())), Status.INVALID_INPUT, Rule.ENTRY_INVENTORY);
        expect(inventory(input, new EntryInventory(inv.status(), inv.scope(), inv.entries(), List.of())), Status.INVALID_INPUT, Rule.ENTRY_INVENTORY);
        expect(inventory(input, new EntryInventory(inv.status(), inv.scope(), inv.entries(), List.of("UNRELATED_GAP"))), Status.INVALID_INPUT, Rule.ENTRY_INVENTORY);
        expect(inventory(input, new EntryInventory(inv.status(), inv.scope(), inv.entries(), List.of("ALTERNATE_ENTRIES_NOT_PROJECTED", "UNPROVEN_INVENTORY_GAP"))), Status.BLOCKED_LOWERING, Rule.PROFILE_FACT);
        expect(inventory(input, new EntryInventory(inv.status(), inv.scope(), List.of(e, e), inv.gapCodes())), Status.INVALID_INPUT, Rule.DUPLICATE_ID);
        expect(inventory(input, new EntryInventory(inv.status(), inv.scope(), List.of(), inv.gapCodes())), Status.BLOCKED_LOWERING, Rule.PROFILE_FACT);

        var c = input.coverage();
        for (Coverage bad : List.of(new Coverage(c.inventoryStatus(), 0, 0, 0, 0, 0, c.readiness()), new Coverage(c.inventoryStatus(), 1, 0, 1, 0, 0, c.readiness()), new Coverage(c.inventoryStatus(), Integer.MAX_VALUE, Integer.MAX_VALUE, 1, 0, 0, c.readiness()))) {
            expect(body(input, input.statements(), input.structure(), input.gaps(), bad), Status.INVALID_INPUT, Rule.COVERAGE);
        }
        expect(body(input, input.statements(), new Structure(List.of(), List.of()), input.gaps(), c), Status.INVALID_INPUT, Rule.STRUCTURE);
        expect(body(input, input.statements(), new Structure(List.of(new StatementId(input.unit(), "statement:999")), List.of()), input.gaps(), c), Status.INVALID_INPUT, Rule.STRUCTURE);
        expect(body(input, input.statements(), input.structure(), List.of(new Gap(new StatementId(alternate.unit(), "statement:0"), GapScope.STRUCTURE, "G", "cross unit", h.provenance())), c), Status.INVALID_INPUT, Rule.GAP);
        expect(body(input, input.statements(), input.structure(), List.of(new Gap(h.id(), GapScope.STRUCTURE, "UNKNOWN_SEQUENCING", "unproved extra gap", h.provenance())), c), Status.BLOCKED_LOWERING, Rule.PROFILE_FACT);
        for (Containment bad : List.of(new Containment(Optional.of(h.id()), Branch.ROOT), new Containment(Optional.empty(), Branch.THEN), new Containment(Optional.of(h.id()), Branch.THEN))) {
            var fact = new GobackFact(header(h, h.id(), 0, bad, h.coverage(), h.readiness()), GobackExit.CURRENT_PROGRAM_INVOCATION, LocalContinuation.NONE);
            expect(body(input, List.of(fact), input.structure(), List.of(), c), Status.INVALID_INPUT, Rule.CONTAINMENT);
        }
        for (String malformed : List.of("statement:-1", "statement:01", "statement:+1", "statement:2147483648", "data:0", "")) {
            var fact = new GobackFact(header(h, new StatementId(input.unit(), malformed), 0, h.containment(), h.coverage(), h.readiness()), GobackExit.CURRENT_PROGRAM_INVOCATION, LocalContinuation.NONE);
            expect(body(input, List.of(fact), input.structure(), List.of(), c), Status.INVALID_INPUT, Rule.IDENTITY);
        }

        var secondId = new StatementId(input.unit(), "statement:5");
        var secondHeader = header(h, secondId, 1, h.containment(), h.coverage(), h.readiness());
        var two = List.<StatementFact>of(input.statements().get(0), new GobackFact(secondHeader, GobackExit.CURRENT_PROGRAM_INVOCATION, LocalContinuation.NONE));
        var twice = body(input, two, new Structure(List.of(h.id(), secondId), List.of()), List.of(), new Coverage(InventoryStatus.COMPLETE, 2, 2, 0, 0, 0, c.readiness()));
        expect(twice, Status.UNSUPPORTED_SLICE, Rule.PROFILE_SHAPE);
        expect(entry(twice, entryFacts(e, new ExecutableStart(Availability.KNOWN, Optional.of(secondId)), e.signature(), e.coverage(), e.readiness(), e.gaps())), Status.UNSUPPORTED_SLICE, Rule.PROFILE_SHAPE);
        var duplicate = new GobackFact(header(h, h.id(), 1, h.containment(), h.coverage(), h.readiness()), GobackExit.CURRENT_PROGRAM_INVOCATION, LocalContinuation.NONE);
        expect(body(twice, List.of(input.statements().get(0), duplicate), new Structure(List.of(h.id(), h.id()), List.of()), List.of(), twice.coverage()), Status.INVALID_INPUT, Rule.DUPLICATE_ID);
        var samePoint = new GobackFact(header(h, secondId, 0, h.containment(), h.coverage(), h.readiness()), GobackExit.CURRENT_PROGRAM_INVOCATION, LocalContinuation.NONE);
        expect(body(twice, List.of(input.statements().get(0), samePoint), twice.structure(), List.of(), twice.coverage()), Status.INVALID_INPUT, Rule.PROGRAM_POINT);

        for (Variant variant : Variant.values()) {
            var otherHeader = header(h, secondId, 1, h.containment(), CoverageStatus.UNSUPPORTED, blocked());
            var larger = body(input, List.of(input.statements().get(0), new OtherStatement(otherHeader, variant)), new Structure(List.of(h.id(), secondId), List.of()), List.of(new Gap(secondId, GapScope.CAPABILITY, "UNSUPPORTED", "known family", h.provenance())), new Coverage(InventoryStatus.COMPLETE, 2, 1, 0, 1, 0, blocked()));
            if (variant == Variant.IF) {
                expect(larger, Status.INVALID_INPUT, Rule.STRUCTURE);
                // The public writer emits both branch inventories even when empty.
                larger = body(larger, larger.statements(), new Structure(larger.structure().roots(), List.of(new BranchChildren(secondId, Branch.THEN, List.of()), new BranchChildren(secondId, Branch.ELSE, List.of()))), larger.gaps(), larger.coverage());
            }
            expect(larger, Status.UNSUPPORTED_SLICE, Rule.PROFILE_SHAPE);
        }
        var data = new DataFact(new DataId(input.unit(), "data:2"), "ITEM", Optional.empty(), h.provenance(), CoverageStatus.PARTIAL, blocked());
        expect(new SpInput(input.unit(), input.policy(), List.of(data), input.statements(), input.structure(), input.gaps(), c, inv), Status.UNSUPPORTED_SLICE, Rule.PROFILE_SHAPE);

        var weakHeader = header(h, h.id(), 0, h.containment(), h.coverage(), blocked());
        var weakBody = List.<StatementFact>of(new GobackFact(weakHeader, GobackExit.CURRENT_PROGRAM_INVOCATION, LocalContinuation.NONE));
        expect(body(input, weakBody, input.structure(), List.of(), c), Status.INVALID_INPUT, Rule.READINESS);
        expect(body(input, weakBody, input.structure(), List.of(), new Coverage(c.inventoryStatus(), 1, 1, 0, 0, 0, blocked())), Status.BLOCKED_LOWERING, Rule.PROFILE_FACT);
        for (InventoryStatus status : List.of(InventoryStatus.PARTIAL, InventoryStatus.INPUT_MISSING)) {
            expect(body(input, input.statements(), input.structure(), List.of(), new Coverage(status, 1, 1, 0, 0, 0, blocked())), Status.BLOCKED_LOWERING, Rule.PROFILE_FACT);
        }
        var sufficientEffects = new Readiness(h.readiness().lowering(), h.readiness().cfg(), claim(ReadinessStatus.SUFFICIENT, "invented effects"));
        expect(body(input, List.of(new GobackFact(header(h, h.id(), 0, h.containment(), h.coverage(), sufficientEffects), GobackExit.CURRENT_PROGRAM_INVOCATION, LocalContinuation.NONE)), input.structure(), List.of(), c), Status.INVALID_INPUT, Rule.READINESS);
        var limit = PORT.admit(input, new AdmitInput.Limits(1));
        check(limit.status() == Status.ADMITTED && !limit.diagnosticsTruncated(), "valid input needs no diagnostic budget");
        var manyErrors = body(twice, List.of(input.statements().get(0), duplicate), new Structure(List.of(), List.of()), List.of(), c);
        limit = PORT.admit(manyErrors, new AdmitInput.Limits(1));
        check(limit.status() == Status.IMPLEMENTATION_LIMIT && limit.diagnosticsTruncated(), "diagnostic cap explicit");
        check(PORT.admit(input, LIMITS).equals(PORT.admit(minimal(), LIMITS)), "independent memory constructions deterministic");
        return count;
    }
}
