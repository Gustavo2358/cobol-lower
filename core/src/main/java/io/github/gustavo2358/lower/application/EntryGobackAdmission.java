package io.github.gustavo2358.lower.application;

import io.github.gustavo2358.lower.domain.SpInput;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import static io.github.gustavo2358.lower.application.Admission.*;
import static io.github.gustavo2358.lower.domain.SpInput.*;

public final class EntryGobackAdmission implements AdmitInput {
    @Override public Admission admit(SpInput input, Limits limits) {
        var context = new Context(input, Objects.requireNonNull(limits));
        try {
            if (input == null) {
                context.require(false, Rule.INPUT_REQUIRED, "input", null, "A materialized SP input is required");
                return context.result(Status.INVALID_INPUT);
            }
            validate(input, context);
            if (!context.diagnostics.isEmpty()) return context.result(Status.INVALID_INPUT);
            return profile(input, context);
        } catch (LimitReached ex) {
            return context.result(Status.IMPLEMENTATION_LIMIT);
        }
    }

    static void validate(SpInput input, Context c) {
        c.touch();
        var unit = input.unit();
        c.require(!unit.compilationUnitId().isBlank() && !unit.canonicalProgramName().isBlank(), Rule.IDENTITY, "unit", null, "Unit namespace text must be nonblank");
        for (int component : unit.structuralPath()) { c.touch(); c.require(component >= 0, Rule.IDENTITY, "unit", null, "Nonnegative structural path"); }
        c.require(!input.policy().policyId().isBlank() && !input.policy().version().isBlank(), Rule.IDENTITY, "policy", null, "Policy identity/version are explicit");
        var points = new HashSet<Integer>();
        for (var data : input.dataDeclarations()) {
            c.touch(); c.identity(data.id().unit(), data.id().handle(), "data", data.provenance());
            c.require(c.data.putIfAbsent(data.id(), data) == null, Rule.DUPLICATE_ID, data.id().handle(), data.provenance(), "Unique DATA identity");
            c.require(!data.canonicalName().isBlank() && data.picture().map(p -> !p.isBlank()).orElse(true), Rule.IDENTITY, data.id().handle(), data.provenance(), "Nonblank DATA text when present");
            c.provenance(data.provenance()); c.readiness(data.readiness(), data.id().handle(), data.provenance());
        }
        var counts = new EnumMap<CoverageStatus, Long>(CoverageStatus.class);
        int[] weakest = {3, 3, 3}; int previousPoint = -1;
        var expectedRoots = new ArrayList<StatementId>();
        var expectedBranches = new LinkedHashMap<BranchKey, List<StatementId>>();
        for (var statement : input.statements()) {
            c.touch(); var h = statement.header();
            c.identity(h.id().unit(), h.id().handle(), "statement", h.provenance());
            c.require(c.statements.putIfAbsent(h.id(), statement) == null, Rule.DUPLICATE_ID, h.id().handle(), h.provenance(), "Unique statement identity");
            c.require(h.programPoint() >= 0 && (c.unordered ? points.add(h.programPoint()) : h.programPoint() > previousPoint), Rule.PROGRAM_POINT, h.id().handle(), h.provenance(), "Unique increasing structural program points, never execution order");
            previousPoint = h.programPoint();
            c.provenance(h.provenance()); c.readiness(h.readiness(), h.id().handle(), h.provenance());
            counts.merge(h.coverage(), 1L, Long::sum);
            var statuses = List.of(h.readiness().lowering().status(), h.readiness().cfg().status(), h.readiness().effectsDataflow().status());
            for (int i = 0; i < 3; i++) if (rank(statuses.get(i)) >= 0) weakest[i] = Math.min(weakest[i], rank(statuses.get(i)));
            var containment = h.containment();
            boolean child = containment.branch() == Branch.THEN || containment.branch() == Branch.ELSE;
            c.require(containment.parent().isPresent() == child, Rule.CONTAINMENT, h.id().handle(), h.provenance(), "THEN/ELSE require parent; ROOT/UNKNOWN omit it");
            if (containment.branch() == Branch.ROOT) expectedRoots.add(h.id());
            if (statement instanceof OtherStatement other && other.variant() == Variant.IF) {
                expectedBranches.computeIfAbsent(new BranchKey(h.id(), Branch.THEN), ignored -> new ArrayList<>());
                expectedBranches.computeIfAbsent(new BranchKey(h.id(), Branch.ELSE), ignored -> new ArrayList<>());
            }
            if (statement instanceof OtherStatement other && other.variant() == Variant.OBSERVED)
                c.require(h.coverage() != CoverageStatus.MODELED, Rule.COVERAGE, h.id().handle(), h.provenance(), "OBSERVED cannot be MODELED");
            if (statement instanceof GobackFact)
                c.require(h.readiness().effectsDataflow().status() != ReadinessStatus.SUFFICIENT, Rule.READINESS, h.id().handle(), h.provenance(), "GOBACK local exit does not publish sufficient effects");
        }
        var gapScopes = new HashMap<StatementId, Set<GapScope>>();
        for (var gap : input.gaps()) {
            c.touch();
            c.require(gap.statement().unit().equals(unit) && c.lookup(gap.statement()) != null, Rule.GAP, gap.statement().handle(), gap.provenance(), "Gap must reference a published statement in the same unit");
            c.require(!gap.code().isBlank() && !gap.detail().isBlank(), Rule.GAP, gap.statement().handle(), gap.provenance(), "Gap code/detail required");
            c.provenance(gap.provenance());
            gapScopes.computeIfAbsent(gap.statement(), ignored -> new HashSet<>()).add(gap.scope());
        }
        for (var statement : input.statements()) {
            c.touch(); var h = statement.header(); var scopes = gapScopes.getOrDefault(h.id(), Set.of());
            c.require(h.coverage() == CoverageStatus.MODELED || !scopes.isEmpty(), Rule.GAP, h.id().handle(), h.provenance(), "Non-modeled statement retains localized gap");
            if (h.containment().branch() == Branch.UNKNOWN)
                c.require(h.coverage() != CoverageStatus.MODELED && scopes.contains(GapScope.STRUCTURE), Rule.CONTAINMENT, h.id().handle(), h.provenance(), "Unknown containment requires non-modeled coverage and STRUCTURE gap");
            if (h.containment().parent().isPresent()) {
                var parentId = h.containment().parent().orElseThrow(); var parent = c.lookup(parentId);
                c.require(parentId.unit().equals(unit) && parent instanceof OtherStatement other && other.variant() == Variant.IF && parent.header().programPoint() < h.programPoint(), Rule.CONTAINMENT, h.id().handle(), h.provenance(), "Parent must be an earlier published IF in the same unit");
                expectedBranches.computeIfAbsent(new BranchKey(parentId, h.containment().branch()), ignored -> new ArrayList<>()).add(h.id());
            }
        }
        for (var root : input.structure().roots()) { c.touch(); c.require(root.unit().equals(unit) && c.lookup(root) != null, Rule.STRUCTURE, root.handle(), null, "Root belongs to published unit"); }
        c.require((c.unordered ? input.structure().roots().size() == expectedRoots.size() && new HashSet<>(input.structure().roots()).equals(new HashSet<>(expectedRoots)) : input.structure().roots().equals(expectedRoots)), Rule.STRUCTURE, "roots", null, "Roots equal published ROOT containment in structural order");
        var branchKeys = new HashSet<BranchKey>();
        for (var branch : input.structure().branches()) {
            c.touch(); var key = new BranchKey(branch.parent(), branch.branch());
            c.require(branchKeys.add(key) && expectedBranches.containsKey(key), Rule.STRUCTURE, branch.parent().handle(), null, "One branch inventory per published IF/branch");
            c.lookup(branch.parent());
            for (var child : branch.children()) { c.touch(); c.require(c.lookup(child) != null && child.unit().equals(unit), Rule.STRUCTURE, child.handle(), null, "Branch child is published in the same unit"); }
            c.require(branch.children().equals(expectedBranches.get(key)), Rule.STRUCTURE, branch.parent().handle(), null, "Branch children match containment exactly");
        }
        c.require(branchKeys.equals(expectedBranches.keySet()), Rule.STRUCTURE, "branches", null, "All IF branch inventories retained");
        coverage(input.coverage(), input.statements().size(), counts, weakest, c);
        var inventory = input.entryInventory();
        c.require(inventory.status() != InventoryStatus.COMPLETE && !inventory.gapCodes().isEmpty() && inventory.gapCodes().contains("ALTERNATE_ENTRIES_NOT_PROJECTED"), Rule.ENTRY_INVENTORY, "entryInventory", null, "PRIMARY_ONLY cannot close or lose alternate-entry gap");
        for (String code : inventory.gapCodes()) { c.touch(); c.require(!code.isBlank(), Rule.ENTRY_INVENTORY, "entryInventory", null, "Nonblank inventory gap code"); }
        var entries = new HashSet<EntryId>(); var roles = new HashSet<EntryRole>();
        for (var entry : inventory.entries()) {
            c.touch(); c.identity(entry.id().unit(), entry.id().handle(), "entry", entry.provenance());
            c.require(entries.add(entry.id()) && roles.add(entry.role()), Rule.DUPLICATE_ID, entry.id().handle(), entry.provenance(), "Unique entry identity and PRIMARY role");
            entry(entry, c);
        }
    }

    private static void entry(EntryFact e, Context c) {
        String id = e.id().handle(); var p = e.provenance();
        c.provenance(p); c.readiness(e.readiness(), id, p);
        boolean startKnown = e.start().availability() == Availability.KNOWN;
        c.require(startKnown == e.start().statement().isPresent(), Rule.ENTRY_START, id, p, "Known start iff one explicit statement reference");
        c.require(e.availability() == Availability.KNOWN || !e.start().statement().isPresent(), Rule.ENTRY_STATE, id, p, "Unavailable entry cannot publish a known start");
        e.start().statement().ifPresent(target -> c.require(target.unit().equals(c.input.unit()) && c.lookup(target) != null, Rule.ENTRY_START, id, p, "Start refers to an existing statement in the same unit"));
        var signature = e.signature();
        c.require(signature.parameterCount().map(n -> n >= 0).orElse(true), Rule.SIGNATURE, id, p, "Nonnegative count when known");
        if (signature.availability() == Availability.KNOWN)
            c.require(signature.parameterCount().equals(Optional.of(0)) && signature.returningClause() == ReturningClause.ABSENT, Rule.SIGNATURE, id, p, "KNOWN signature capability is exactly zero/ABSENT");
        if (signature.availability() == Availability.UNAVAILABLE || signature.availability() == Availability.INPUT_MISSING)
            c.require(signature.parameterCount().isEmpty() && signature.returningClause() == ReturningClause.UNKNOWN, Rule.SIGNATURE, id, p, "Unavailable signature preserves null/UNKNOWN");
        var scopes = new HashSet<GapScope>();
        for (var gap : e.gaps()) { c.touch(); scopes.add(gap.scope()); c.provenance(gap.provenance()); c.require(!gap.code().isBlank() && !gap.detail().isBlank(), Rule.GAP, id, gap.provenance(), "Entry gap code/detail required"); }
        boolean complete = e.availability() == Availability.KNOWN && startKnown && signature.availability() == Availability.KNOWN;
        if (!complete) c.require(e.coverage() != CoverageStatus.MODELED && !e.gaps().isEmpty() && e.readiness().lowering().status() != ReadinessStatus.SUFFICIENT, Rule.ENTRY_STATE, id, p, "Incomplete entry must retain gap and lower coverage/readiness");
        if (!startKnown) c.require(scopes.contains(GapScope.ENTRY_START) && e.readiness().cfg().status() != ReadinessStatus.SUFFICIENT, Rule.ENTRY_STATE, id, p, "Unavailable start requires ENTRY_START gap and weaker CFG claim");
        if (signature.availability() != Availability.KNOWN) c.require(scopes.contains(GapScope.ENTRY_SIGNATURE), Rule.ENTRY_STATE, id, p, "Unavailable signature requires ENTRY_SIGNATURE gap");
        c.require(e.readiness().effectsDataflow().status() != ReadinessStatus.SUFFICIENT, Rule.READINESS, id, p, "Entry does not certify effects");
    }

    private static void coverage(Coverage coverage, int size, Map<CoverageStatus, Long> counts, int[] weakest, Context c) {
        c.touch();
        long modeled = coverage.modeledStatements(), partial = coverage.partialStatements(), unsupported = coverage.unsupportedStatements(), missing = coverage.inputMissingStatements();
        c.require(modeled >= 0 && partial >= 0 && unsupported >= 0 && missing >= 0 && coverage.observedStatements() >= 0 && modeled + partial + unsupported + missing == coverage.observedStatements() && coverage.observedStatements() == size && modeled == counts.getOrDefault(CoverageStatus.MODELED, 0L) && partial == counts.getOrDefault(CoverageStatus.PARTIAL, 0L) && unsupported == counts.getOrDefault(CoverageStatus.UNSUPPORTED, 0L) && missing == counts.getOrDefault(CoverageStatus.INPUT_MISSING, 0L), Rule.COVERAGE, "coverage", null, "Counts reconcile every occurrence and classification without overflow");
        c.readiness(coverage.readiness(), "coverage", null);
        var statuses = List.of(coverage.readiness().lowering().status(), coverage.readiness().cfg().status(), coverage.readiness().effectsDataflow().status());
        for (int i = 0; i < 3; i++) {
            c.require(weakest[i] == 3 || rank(statuses.get(i)) <= weakest[i], Rule.READINESS, "coverage.dimension:" + i, null, "Aggregate cannot exceed weakest fact in the same dimension");
            if (coverage.inventoryStatus() != InventoryStatus.COMPLETE) c.require(statuses.get(i) != ReadinessStatus.SUFFICIENT, Rule.READINESS, "coverage.dimension:" + i, null, "Incomplete inventory cannot claim sufficient aggregate");
        }
    }

    private static Admission profile(SpInput input, Context c) {
        c.phase = Phase.ADMISSION;
        c.require(input.coverage().inventoryStatus() == InventoryStatus.COMPLETE && input.coverage().inputMissingStatements() == 0 && input.entryInventory().status() == InventoryStatus.PARTIAL, Rule.PROFILE_FACT, "inventory", null, "Complete observed input is required; alternate entries remain PARTIAL");
        c.require(input.entryInventory().gapCodes().stream().allMatch(code -> code.equals("ALTERNATE_ENTRIES_NOT_PROJECTED")), Rule.PROFILE_FACT, "entryInventory", null, "Only the proven alternate-entry inventory gap is admitted by this profile");
        c.require(!input.entryInventory().entries().isEmpty(), Rule.PROFILE_FACT, "entryInventory", null, "A known PRIMARY entry is required");
        for (var entry : input.entryInventory().entries()) {
            c.touch();
            c.require(entry.availability() == Availability.KNOWN && entry.start().availability() == Availability.KNOWN && entry.signature().availability() == Availability.KNOWN && entry.coverage() == CoverageStatus.MODELED && entry.readiness().lowering().status() == ReadinessStatus.SUFFICIENT && entry.readiness().cfg().status() == ReadinessStatus.SUFFICIENT && entry.gaps().isEmpty(), Rule.PROFILE_FACT, entry.id().handle(), entry.provenance(), "Entry/start/zero signature and local claims must be known, with no unexplained entry gap");
        }
        for (var statement : input.statements()) if (statement instanceof GobackFact goback) {
            c.touch(); var h = goback.header();
            c.require(h.coverage() == CoverageStatus.MODELED && h.readiness().lowering().status() == ReadinessStatus.SUFFICIENT && h.readiness().cfg().status() == ReadinessStatus.SUFFICIENT && h.containment().branch() != Branch.UNKNOWN, Rule.PROFILE_FACT, h.id().handle(), h.provenance(), "GOBACK local exit and containment must be known");
        }
        if (!c.diagnostics.isEmpty()) return c.result(Status.BLOCKED_LOWERING);
        c.require(input.dataDeclarations().isEmpty() && input.statements().size() == 1 && input.entryInventory().entries().size() == 1, Rule.PROFILE_SHAPE, "inventory", null, "minimal-entry-goback@1 requires zero DATA, one entry and one statement; no filtering");
        for (var statement : input.statements()) c.require(statement instanceof GobackFact && statement.header().containment().branch() == Branch.ROOT, Rule.PROFILE_SHAPE, statement.header().id().handle(), statement.header().provenance(), "Only a typed root GOBACK is supported");
        if (!c.diagnostics.isEmpty()) return c.result(Status.UNSUPPORTED_SLICE);
        c.require(input.gaps().isEmpty(), Rule.PROFILE_FACT, "gaps", null, "Additional gap compatibility is not established by this profile");
        if (!c.diagnostics.isEmpty()) return c.result(Status.BLOCKED_LOWERING);
        // The reference was validated against the complete index. No ordering-based start selection.
        return c.result(Status.ADMITTED);
    }

    private static int rank(ReadinessStatus status) {
        return switch (status) { case BLOCKED -> 0; case PARTIAL -> 1; case SUFFICIENT -> 2; case NOT_APPLICABLE -> -1; };
    }
    private record BranchKey(StatementId parent, Branch branch) { }
    static final class Context {
        final SpInput input; final Limits limits; final boolean unordered;
        final Map<DataId, DataFact> data = new LinkedHashMap<>();
        final Map<StatementId, StatementFact> statements = new HashMap<>();
        final List<Diagnostic> diagnostics = new ArrayList<>();
        long entities; long references; long components; boolean truncated;
        Phase phase = Phase.INPUT_VALIDATION;
        Context(SpInput input, Limits limits) { this(input, limits, false); }
        Context(SpInput input, Limits limits, boolean unordered) { this.input = input; this.limits = limits; this.unordered = unordered; }
        DataFact data(DataId id) { references++; return data.get(id); }
        void touch() {
            if (++entities > limits.maxEntities()) {
                require(false, Rule.LIMIT, "input", null, "Entity visit limit exceeded; no partial admission");
                throw new LimitReached();
            }
        }
        StatementFact lookup(StatementId id) { references++; return statements.get(id); }
        void require(boolean condition, Rule rule, String subject, Provenance provenance, String requirement) {
            if (condition) return;
            if (diagnostics.size() == limits.maxDiagnostics()) { truncated = true; throw new LimitReached(); }
            diagnostics.add(new Diagnostic(rule, phase, Severity.ERROR, Optional.ofNullable(input).map(SpInput::unit), subject.isBlank() ? "invalid-identity" : subject, Optional.ofNullable(provenance), requirement));
        }
        void identity(UnitKey unit, String handle, String kind, Provenance provenance) {
            boolean valid = false;
            if (handle.startsWith(kind + ":")) {
                String suffix = handle.substring(kind.length() + 1);
                try { int number = Integer.parseInt(suffix); valid = number >= 0 && Integer.toString(number).equals(suffix); }
                catch (NumberFormatException ignored) { /* Invalid published identity, never a guessed handle. */ }
            }
            require(unit.equals(input.unit()) && valid, Rule.IDENTITY, handle, provenance, "Namespaced canonical " + kind + ":n identity required");
        }
        void readiness(Readiness readiness, String subject, Provenance provenance) {
            require(!readiness.lowering().scope().isBlank() && !readiness.cfg().scope().isBlank() && !readiness.effectsDataflow().scope().isBlank(), Rule.READINESS, subject, provenance, "Readiness dimension scopes are explicit");
        }
        void provenance(Provenance provenance) {
            touch(); components++;
            for (var location : List.of(provenance.expanded(), provenance.original())) {
                touch(); components++;
                require(!location.file().isBlank() && location.startLine() >= 0 && location.endLine() >= 0 && location.startColumn() >= 0 && location.endColumn() >= 0, Rule.IDENTITY, "provenance", provenance, "Published location text and nonnegative coordinates required");
            }
            for (var frame : provenance.includeChain()) {
                touch(); components++;
                require(!frame.includingFile().isBlank() && !frame.includedFile().isBlank() && !frame.requestedName().isBlank() && frame.includeLine() >= 0, Rule.IDENTITY, "includeChain", provenance, "Published include frame preserved and well formed");
            }
        }
        Admission result(Status status) { return new Admission(status, Optional.ofNullable(input), diagnostics, new Statistics(entities, references, components), truncated); }
    }
    static final class LimitReached extends RuntimeException { private static final long serialVersionUID = 1L; }
}
