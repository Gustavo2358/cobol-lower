package io.github.gustavo2358.lower.domain;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/** Closed snapshot of the consumed SP surface; not a semantic validity certificate. */
public record SpInput(UnitKey unit, Policy policy, List<DataFact> dataDeclarations, List<StatementFact> statements, Structure structure, List<Gap> gaps, Coverage coverage, EntryInventory entryInventory) {
    public SpInput {
        Objects.requireNonNull(unit, "unit");
        Objects.requireNonNull(policy, "policy");
        dataDeclarations = List.copyOf(dataDeclarations);
        statements = List.copyOf(statements);
        Objects.requireNonNull(structure, "structure");
        gaps = List.copyOf(gaps);
        Objects.requireNonNull(coverage, "coverage");
        Objects.requireNonNull(entryInventory, "entryInventory");
    }
    /** Non-GOBACK payload semantics are outside this snapshot's capability. Occurrences survive. */
    public sealed interface StatementFact permits GobackFact, OtherStatement { StatementHeader header(); }

    public enum Availability { KNOWN, PARTIAL, UNAVAILABLE, INPUT_MISSING }
    public enum CoverageStatus { MODELED, PARTIAL, UNSUPPORTED, INPUT_MISSING }
    public enum InventoryStatus { COMPLETE, PARTIAL, INPUT_MISSING }
    public enum ReadinessStatus { SUFFICIENT, PARTIAL, BLOCKED, NOT_APPLICABLE }
    public enum Branch { ROOT, THEN, ELSE, UNKNOWN }
    public enum EntryRole { PRIMARY }
    public enum EntryInventoryScope { PRIMARY_ONLY }
    public enum ReturningClause { ABSENT, PRESENT, UNKNOWN }
    public enum GobackExit { CURRENT_PROGRAM_INVOCATION }
    public enum LocalContinuation { NONE }
    public enum Variant { MOVE, CALL, IF, OBSERVED }
    public enum GapScope { RUNTIME_CALL_TARGET, LITERAL_KIND, NOMINAL_BINDING, CONDITION_SEMANTICS, STRUCTURE, CAPABILITY, ANALYSIS_INPUT, ENTRY_START, ENTRY_SIGNATURE }
    public enum QualifyMode { STANDARD, EXTEND, UNSPECIFIED }
    public enum PgmnameMode { COMPAT, LONGUPPER, LONGMIXED, UNSPECIFIED }
    public enum DynamMode { DYNAM, NODYNAM, UNSPECIFIED }
    public enum DllMode { DLL, NODLL, UNSPECIFIED }

    public record UnitKey(String compilationUnitId, List<Integer> structuralPath, String canonicalProgramName) {
        public UnitKey {
            Objects.requireNonNull(compilationUnitId, "compilationUnitId");
            structuralPath = List.copyOf(structuralPath);
            Objects.requireNonNull(canonicalProgramName, "canonicalProgramName");
        }
    }

    public record StatementId(UnitKey unit, String handle) {
        public StatementId {
            Objects.requireNonNull(unit, "unit");
            Objects.requireNonNull(handle, "handle");
        }
    }

    public record EntryId(UnitKey unit, String handle) {
        public EntryId {
            Objects.requireNonNull(unit, "unit");
            Objects.requireNonNull(handle, "handle");
        }
    }

    public record DataId(UnitKey unit, String handle) {
        public DataId {
            Objects.requireNonNull(unit, "unit");
            Objects.requireNonNull(handle, "handle");
        }
    }

    public record Policy(String policyId, String version, QualifyMode qualifyMode, PgmnameMode pgmnameMode, DynamMode dynamMode, DllMode dllMode) {
        public Policy {
            Objects.requireNonNull(policyId, "policyId");
            Objects.requireNonNull(version, "version");
            Objects.requireNonNull(qualifyMode, "qualifyMode");
            Objects.requireNonNull(pgmnameMode, "pgmnameMode");
            Objects.requireNonNull(dynamMode, "dynamMode");
            Objects.requireNonNull(dllMode, "dllMode");
        }
    }

    public record Location(String file, int startLine, int startColumn, int endLine, int endColumn) {
        public Location {
            Objects.requireNonNull(file, "file");
        }
    }

    public record IncludeFrame(String includingFile, String requestedName, String includedFile, int includeLine) {
        public IncludeFrame {
            Objects.requireNonNull(includingFile, "includingFile");
            Objects.requireNonNull(requestedName, "requestedName");
            Objects.requireNonNull(includedFile, "includedFile");
        }
    }

    public record Provenance(Location expanded, Location original, List<IncludeFrame> includeChain, boolean exact) {
        public Provenance {
            Objects.requireNonNull(expanded, "expanded");
            Objects.requireNonNull(original, "original");
            includeChain = List.copyOf(includeChain);
        }
    }

    public record ReadinessClaim(ReadinessStatus status, String scope) {
        public ReadinessClaim {
            Objects.requireNonNull(status, "status");
            Objects.requireNonNull(scope, "scope");
        }
    }

    public record Readiness(ReadinessClaim lowering, ReadinessClaim cfg, ReadinessClaim effectsDataflow) {
        public Readiness {
            Objects.requireNonNull(lowering, "lowering");
            Objects.requireNonNull(cfg, "cfg");
            Objects.requireNonNull(effectsDataflow, "effectsDataflow");
        }
    }

    public record DataFact(DataId id, String canonicalName, Optional<String> picture, Provenance provenance, CoverageStatus coverage, Readiness readiness) {
        public DataFact {
            Objects.requireNonNull(id, "id");
            Objects.requireNonNull(canonicalName, "canonicalName");
            Objects.requireNonNull(picture, "picture");
            Objects.requireNonNull(provenance, "provenance");
            Objects.requireNonNull(coverage, "coverage");
            Objects.requireNonNull(readiness, "readiness");
        }
    }

    public record Containment(Optional<StatementId> parent, Branch branch) {
        public Containment {
            Objects.requireNonNull(parent, "parent");
            Objects.requireNonNull(branch, "branch");
        }
    }

    public record StatementHeader(StatementId id, int programPoint, Containment containment, Provenance provenance, CoverageStatus coverage, Readiness readiness) {
        public StatementHeader {
            Objects.requireNonNull(id, "id");
            Objects.requireNonNull(containment, "containment");
            Objects.requireNonNull(provenance, "provenance");
            Objects.requireNonNull(coverage, "coverage");
            Objects.requireNonNull(readiness, "readiness");
        }
    }

    public record GobackFact(StatementHeader header, GobackExit exit, LocalContinuation localContinuation) implements StatementFact {
        public GobackFact {
            Objects.requireNonNull(header, "header");
            Objects.requireNonNull(exit, "exit");
            Objects.requireNonNull(localContinuation, "localContinuation");
        }
    }

    public record OtherStatement(StatementHeader header, Variant variant) implements StatementFact {
        public OtherStatement {
            Objects.requireNonNull(header, "header");
            Objects.requireNonNull(variant, "variant");
        }
    }

    public record Gap(StatementId statement, GapScope scope, String code, String detail, Provenance provenance) {
        public Gap {
            Objects.requireNonNull(statement, "statement");
            Objects.requireNonNull(scope, "scope");
            Objects.requireNonNull(code, "code");
            Objects.requireNonNull(detail, "detail");
            Objects.requireNonNull(provenance, "provenance");
        }
    }

    public record EntryGap(GapScope scope, String code, String detail, Provenance provenance) {
        public EntryGap {
            Objects.requireNonNull(scope, "scope");
            Objects.requireNonNull(code, "code");
            Objects.requireNonNull(detail, "detail");
            Objects.requireNonNull(provenance, "provenance");
        }
    }

    public record ExecutableStart(Availability availability, Optional<StatementId> statement) {
        public ExecutableStart {
            Objects.requireNonNull(availability, "availability");
            Objects.requireNonNull(statement, "statement");
        }
    }

    public record EntrySignature(Availability availability, Optional<Integer> parameterCount, ReturningClause returningClause) {
        public EntrySignature {
            Objects.requireNonNull(availability, "availability");
            Objects.requireNonNull(parameterCount, "parameterCount");
            Objects.requireNonNull(returningClause, "returningClause");
        }
    }

    public record EntryFact(EntryId id, EntryRole role, Availability availability, ExecutableStart start, EntrySignature signature, Provenance provenance, CoverageStatus coverage, Readiness readiness, List<EntryGap> gaps) {
        public EntryFact {
            Objects.requireNonNull(id, "id");
            Objects.requireNonNull(role, "role");
            Objects.requireNonNull(availability, "availability");
            Objects.requireNonNull(start, "start");
            Objects.requireNonNull(signature, "signature");
            Objects.requireNonNull(provenance, "provenance");
            Objects.requireNonNull(coverage, "coverage");
            Objects.requireNonNull(readiness, "readiness");
            gaps = List.copyOf(gaps);
        }
    }

    public record EntryInventory(InventoryStatus status, EntryInventoryScope scope, List<EntryFact> entries, List<String> gapCodes) {
        public EntryInventory {
            Objects.requireNonNull(status, "status");
            Objects.requireNonNull(scope, "scope");
            entries = List.copyOf(entries);
            gapCodes = List.copyOf(gapCodes);
        }
    }

    public record BranchChildren(StatementId parent, Branch branch, List<StatementId> children) {
        public BranchChildren {
            Objects.requireNonNull(parent, "parent");
            Objects.requireNonNull(branch, "branch");
            children = List.copyOf(children);
        }
    }

    public record Structure(List<StatementId> roots, List<BranchChildren> branches) {
        public Structure {
            roots = List.copyOf(roots);
            branches = List.copyOf(branches);
        }
    }

    public record Coverage(InventoryStatus inventoryStatus, int observedStatements, int modeledStatements, int partialStatements, int unsupportedStatements, int inputMissingStatements, Readiness readiness) {
        public Coverage {
            Objects.requireNonNull(inventoryStatus, "inventoryStatus");
            Objects.requireNonNull(readiness, "readiness");
        }
    }
}
