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
    /** Typed consumed variants; unsupported occurrences remain explicit. */
    public sealed interface StatementFact permits GobackFact, MoveFact, OtherStatement { StatementHeader header(); }

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

    public record DataFact(DataId id, String canonicalName, Optional<String> picture, Provenance provenance, CoverageStatus coverage, Readiness readiness, Optional<ScalarText> scalarText) {
        public DataFact(DataId id, String canonicalName, Optional<String> picture, Provenance provenance, CoverageStatus coverage, Readiness readiness) {
            this(id, canonicalName, picture, provenance, coverage, readiness, Optional.empty());
        }
        public DataFact {
            Objects.requireNonNull(scalarText, "scalarText");
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

    // Closed 1.2.0 facts. Constructors preserve absence; admission validates semantic coherence.
    public enum LogicalDomain { TEXT }
    public enum StorageClass { WORKING_STORAGE }
    public enum DeclarationScope { LOCAL }
    public enum CopySemantics { FULL_IDENTITY, UNAVAILABLE }
    public enum ContinuationAvailability { KNOWN, UNAVAILABLE, NONE }
    public enum LiteralKind { ALPHANUMERIC, NUMERIC, UNKNOWN }
    public enum OperandRole { READ, WRITE, CALL_TARGET }
    public enum ResolutionStatus { RESOLVED, AMBIGUOUS, UNRESOLVED, INPUT_MISSING }
    public record OperandId(StatementId statement, String handle) {
        public OperandId { Objects.requireNonNull(statement); Objects.requireNonNull(handle); }
    }
    public record ScalarText(LogicalDomain logicalDomain, int logicalExtent, StorageClass storageClass, DeclarationScope declarationScope) {
        public ScalarText { Objects.requireNonNull(logicalDomain); Objects.requireNonNull(storageClass); Objects.requireNonNull(declarationScope); }
    }
    public record LogicalValue(LogicalDomain logicalDomain, String value, int logicalExtent) {
        public LogicalValue { Objects.requireNonNull(logicalDomain); Objects.requireNonNull(value); }
    }
    public record LiteralSource(OperandId id, LiteralKind kind, Optional<LogicalValue> logicalValue, Provenance provenance) {
        public LiteralSource { Objects.requireNonNull(id); Objects.requireNonNull(kind); Objects.requireNonNull(logicalValue); Objects.requireNonNull(provenance); }
    }
    public record Binding(ResolutionStatus status, List<DataId> candidates, Optional<DataId> selected) {
        public Binding { Objects.requireNonNull(status); candidates = List.copyOf(candidates); Objects.requireNonNull(selected); }
    }
    public record WholeItemAccess(DataId data) {
        public WholeItemAccess { Objects.requireNonNull(data); }
    }
    public record DataReference(OperandId id, OperandRole role, Binding binding, Optional<WholeItemAccess> wholeItemAccess, Provenance provenance) {
        public DataReference { Objects.requireNonNull(id); Objects.requireNonNull(role); Objects.requireNonNull(binding); Objects.requireNonNull(wholeItemAccess); Objects.requireNonNull(provenance); }
    }
    public record NormalContinuation(ContinuationAvailability availability, Optional<StatementId> statement, Provenance provenance) {
        public NormalContinuation { Objects.requireNonNull(availability); Objects.requireNonNull(statement); Objects.requireNonNull(provenance); }
    }
    public record MoveFact(StatementHeader header, LiteralSource source, DataReference target, CopySemantics copySemantics, NormalContinuation normalContinuation) implements StatementFact {
        public MoveFact { Objects.requireNonNull(header); Objects.requireNonNull(source); Objects.requireNonNull(target); Objects.requireNonNull(copySemantics); Objects.requireNonNull(normalContinuation); }
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
