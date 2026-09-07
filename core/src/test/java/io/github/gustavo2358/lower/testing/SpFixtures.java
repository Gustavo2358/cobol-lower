package io.github.gustavo2358.lower.testing;

import io.github.gustavo2358.lower.domain.SpInput;
import java.util.List;
import java.util.Optional;
import static io.github.gustavo2358.lower.domain.SpInput.*;

/** Handwritten SP facts, independent of JSON, decoder, validator and translation. */
public final class SpFixtures {
    private SpFixtures() { }
    public static ReadinessClaim claim(ReadinessStatus status, String scope) { return new ReadinessClaim(status, scope); }
    public static Readiness blocked() { return new Readiness(claim(ReadinessStatus.BLOCKED, "synthetic lowering gap"), claim(ReadinessStatus.BLOCKED, "synthetic control gap"), claim(ReadinessStatus.BLOCKED, "synthetic effects gap")); }
    public static SpInput minimal() { return minimal(new UnitKey("SEMANTIC-PRODUCT-ENTRY-GOBACK.CBL", List.of(0), "AIR-FIRST"), "entry:0", "statement:0"); }
    public static SpInput minimal(UnitKey unit, String entryHandle, String statementHandle) {
        var statementId = new StatementId(unit, statementHandle);
        var entryOrigin = new Provenance(new Location("<preprocessed>", 3, 0, 4, 11), new Location("semantic-product-entry-goback.cbl", 3, 7, 4, 18), List.of(), true);
        var statementOrigin = new Provenance(new Location("<preprocessed>", 4, 4, 4, 9), new Location("semantic-product-entry-goback.cbl", 4, 11, 4, 16), List.of(), true);
        var entryReadiness = new Readiness(claim(ReadinessStatus.SUFFICIENT, "primary entry, canonical executable start and published signature availability"), claim(ReadinessStatus.SUFFICIENT, "primary entry start only; no sequence or reachability claim"), claim(ReadinessStatus.BLOCKED, "entry state, storage and effects are not published"));
        var statementReadiness = new Readiness(claim(ReadinessStatus.SUFFICIENT, "GOBACK concludes the current program invocation"), claim(ReadinessStatus.SUFFICIENT, "GOBACK has no local successor"), claim(ReadinessStatus.BLOCKED, "GOBACK runtime result and lifecycle effects are not published"));
        var aggregate = new Readiness(claim(ReadinessStatus.SUFFICIENT, "report binding claim combined with every observed statement fact"), claim(ReadinessStatus.SUFFICIENT, "complete observed inventory bounded by each statement's structural readiness"), claim(ReadinessStatus.BLOCKED, "report dependency claim bounded by each statement's effects readiness"));
        var header = new StatementHeader(statementId, 0, new Containment(Optional.empty(), Branch.ROOT), statementOrigin, CoverageStatus.MODELED, statementReadiness);
        var entry = new EntryFact(new EntryId(unit, entryHandle), EntryRole.PRIMARY, Availability.KNOWN, new ExecutableStart(Availability.KNOWN, Optional.of(statementId)), new EntrySignature(Availability.KNOWN, Optional.of(0), ReturningClause.ABSENT), entryOrigin, CoverageStatus.MODELED, entryReadiness, List.of());
        return new SpInput(unit, new Policy("cobol-explorer/explicit-options", "3.0.0", QualifyMode.UNSPECIFIED, PgmnameMode.UNSPECIFIED, DynamMode.UNSPECIFIED, DllMode.UNSPECIFIED), List.of(), List.of(new GobackFact(header, GobackExit.CURRENT_PROGRAM_INVOCATION, LocalContinuation.NONE)), new Structure(List.of(statementId), List.of()), List.of(), new Coverage(InventoryStatus.COMPLETE, 1, 1, 0, 0, 0, aggregate), new EntryInventory(InventoryStatus.PARTIAL, EntryInventoryScope.PRIMARY_ONLY, List.of(entry), List.of("ALTERNATE_ENTRIES_NOT_PROJECTED")));
    }
    public static SpInput entry(SpInput input, EntryFact entry) {
        return inventory(input, new EntryInventory(input.entryInventory().status(), input.entryInventory().scope(), List.of(entry), input.entryInventory().gapCodes()));
    }
    public static SpInput inventory(SpInput input, EntryInventory inventory) {
        return new SpInput(input.unit(), input.policy(), input.dataDeclarations(), input.statements(), input.structure(), input.gaps(), input.coverage(), inventory);
    }
    public static SpInput body(SpInput input, List<StatementFact> statements, Structure structure, List<Gap> gaps, Coverage coverage) {
        return new SpInput(input.unit(), input.policy(), input.dataDeclarations(), statements, structure, gaps, coverage, input.entryInventory());
    }
    public static EntryFact entryFacts(EntryFact e, ExecutableStart start, EntrySignature signature, CoverageStatus coverage, Readiness readiness, List<EntryGap> gaps) {
        return new EntryFact(e.id(), e.role(), e.availability(), start, signature, e.provenance(), coverage, readiness, gaps);
    }
    public static StatementHeader header(StatementHeader h, StatementId id, int point, Containment containment, CoverageStatus coverage, Readiness readiness) {
        return new StatementHeader(id, point, containment, h.provenance(), coverage, readiness);
    }
}
