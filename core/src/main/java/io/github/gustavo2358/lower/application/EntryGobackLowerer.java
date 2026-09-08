package io.github.gustavo2358.lower.application;

import io.github.gustavo2358.lower.domain.SpInput;
import io.github.gustavo2358.air.model.*;
import io.github.gustavo2358.air.model.Ids.*;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Optional;

/** Closed minimal-entry-goback@1 translation from published facts, with no frontend callbacks. */
public final class EntryGobackLowerer implements LowerInput {
    @Override public LoweringResult lower(SpInput input, Options options) {
        var admission = new EntryGobackAdmission().admit(input, options.admission());
        if (admission.status() != Admission.Status.ADMITTED) {
            var status = switch (admission.status()) {
                case INVALID_INPUT -> LoweringResult.Status.INVALID_INPUT;
                case BLOCKED_LOWERING -> LoweringResult.Status.BLOCKED_LOWERING;
                case UNSUPPORTED_SLICE -> LoweringResult.Status.UNSUPPORTED_SLICE;
                case IMPLEMENTATION_LIMIT -> LoweringResult.Status.IMPLEMENTATION_LIMIT;
                case ADMITTED -> throw new IllegalStateException("handled before rejection");
            };
            return new LoweringResult(status, admission, Optional.empty(), Optional.empty(), List.of(), List.of(), List.of());
        }
        return translate(input, options, admission, new LocalIds());
    }
    private LoweringResult translate(SpInput input, Options options, Admission admission, LocalIds ids) {
        var revision = CanonicalRevision.encode(input, options.maximumIdentityCharacters());
        if (revision.isEmpty()) return new LoweringResult(LoweringResult.Status.IMPLEMENTATION_LIMIT, admission,
                Optional.empty(), Optional.empty(), List.of(), List.of(), List.of(new LoweringResult.Limitation(
                        LoweringResult.LimitCode.IDENTITY_LIMIT, "publication", "PublicationId exceeds explicit character limit; no prefix published.")));
        var publication = new PublicationId(revision.orElseThrow()); var unit = new UnitId(publication, "unit");
        var origins = new SourceOrigins(publication, ids);
        var labels = new LinkedHashMap<SpInput.StatementId, LabelId>();
        var statements = new ArrayList<LoweringResult.StatementLink>();
        var entryLinks = new ArrayList<LoweringResult.EntryLink>();
        var sequences = new ArrayList<Sequence>(); var entries = new ArrayList<Entries.Entry>();
        var items = new ArrayList<Evidence.CoverageItem>(); var uncertainties = new ArrayList<Evidence.Uncertainty>();
        var unitInputs = new ArrayList<OriginId>();
        for (var statement : input.statements()) {
            // Admission established typed GOBACK and exact exit/continuation, never textual spelling.
            var source = statement.header(); String key = source.id().handle();
            var origin = origins.source("statement", key, source.provenance());
            var label = new LabelId(unit, ids.id("label", "goback-sequence", unit.localId(), key)); labels.put(source.id(), label);
            var operation = new OperationId(unit, ids.id("operation", "goback-return", unit.localId(), key));
            var scope = new Scopes.EntityScope(List.of(operation));
            var exact = new Evidence.Claim(scope, Evidence.PrecisionStatus.EXACT, List.of());
            var storage = unavailable(ids, publication, operation, origin, Evidence.Dimension.STORAGE, uncertainties);
            var effects = unavailable(ids, publication, operation, origin, Evidence.Dimension.EFFECTS, uncertainties);
            var values = unavailable(ids, publication, operation, origin, Evidence.Dimension.VALUES, uncertainties);
            var dependencies = unavailable(ids, publication, operation, origin, Evidence.Dimension.DEPENDENCIES, uncertainties);
            var reasons = new ArrayList<UncertaintyId>();
            for (var claim : List.of(storage, effects, values, dependencies)) reasons.addAll(claim.reasons());
            var header = new Operations.Header(operation, origin, Evidence.CoverageStatus.MODELED,
                    new Evidence.Precision(exact, storage, effects, values, dependencies), reasons);
            var sequenceOrigin = origins.derived(ids.id("origin", "sequence", unit.localId(), key), List.of(origin), "minimal-entry-goback@1/sequence");
            sequences.add(new Sequence(label, List.of(), new Operations.Return(header, List.of()), sequenceOrigin));
            statements.add(new LoweringResult.StatementLink(source.id(), operation, label, origin));
            items.add(new Evidence.CoverageItem(sourceKey(input.unit(), "statement", source.id().handle()), origin,
                    Evidence.CoverageStatus.MODELED, List.of(operation, label), List.of(), Optional.empty()));
        }
        for (var entry : input.entryInventory().entries()) {
            String key = entry.id().handle();
            var origin = origins.source("entry", key, entry.provenance()); unitInputs.add(origin);
            var id = new EntryId(unit, ids.id("entry", "primary-entry", unit.localId(), key));
            var label = labels.get(entry.start().statement().orElseThrow());
            if (label == null) throw new IllegalStateException("admission/start index invariant violated");
            var signature = new Interactions.Signature(new Interactions.ParameterInventory(List.of(), Interactions.NoRemainder.INSTANCE),
                    new Interactions.ResultInventory(List.of(), Interactions.NoRemainder.INSTANCE), origin);
            entries.add(new Entries.Entry(id, Optional.of(label), signature, new Entries.EntryState(List.of(), List.of()), origin));
            entryLinks.add(new LoweringResult.EntryLink(entry.id(), id, label, origin));
            items.add(new Evidence.CoverageItem(sourceKey(input.unit(), "entry", entry.id().handle()), origin,
                    Evidence.CoverageStatus.MODELED, List.of(id), List.of(), Optional.empty()));
        }
        var unitOrigin = origins.derived("unit-origin", unitInputs, "minimal-entry-goback@1/selected-unit");
        var gap = new UncertaintyId(publication, "entry-inventory/alternate-not-projected");
        uncertainties.add(new Evidence.Uncertainty(gap, "cobol-lower:ALTERNATE_ENTRIES_NOT_PROJECTED",
                List.of(Evidence.Dimension.CONTROL), new Scopes.UnitScope(unit),
                "SP PRIMARY_ONLY/PARTIAL does not enumerate alternate entries; known primary start remains exact.", unitOrigin));
        var unitCoverage = new Evidence.Coverage(Evidence.InventoryStatus.PARTIAL, new Scopes.UnitScope(unit), items, List.of(gap));
        var body = new Unit(unit, Optional.empty(), List.of(), List.of(), entries, sequences, List.of(), Unit.BodyAvailability.AVAILABLE,
                Optional.empty(), unitCoverage, unitOrigin);
        var output = new Publication(publication, SemanticVersion.AIR_2_0_0, new Capabilities.Manifest(List.of(), List.of()),
                origins.artifacts(), List.of(body), List.of(), List.of(), List.of(), origins.origins(),
                new Evidence.Coverage(Evidence.InventoryStatus.PARTIAL, new Scopes.PublicationScope(publication), items, List.of(gap)), uncertainties, List.of());
        var assessment = OutputAssessment.assess(output, options.validation());
        return new LoweringResult(assessment.status(), admission, assessment.publication(), Optional.of(assessment.validation()),
                entryLinks, statements, origins.limitations());
    }
    private static Evidence.Claim unavailable(LocalIds ids, PublicationId publication, OperationId operation, OriginId origin,
            Evidence.Dimension dimension, List<Evidence.Uncertainty> uncertainties) {
        var id = new UncertaintyId(publication, ids.id("uncertainty", "unproved", operation.localId(), dimension.name()));
        var scope = new Scopes.EntityScope(List.of(operation));
        uncertainties.add(new Evidence.Uncertainty(id, "cobol-lower:UNPROVED_" + dimension.name(), List.of(dimension), scope,
                "The local control rule does not certify " + dimension.name() + "; upstream readiness retained separately.", origin));
        return new Evidence.Claim(scope, Evidence.PrecisionStatus.UNAVAILABLE, List.of(id));
    }
    private static String sourceKey(SpInput.UnitKey unit, String kind, String handle) {
        var key = new StringBuilder("sp-unit/").append(CanonicalRevision.token(unit.compilationUnitId()));
        key.append('/').append(unit.structuralPath().size()).append('/');
        for (int part : unit.structuralPath()) key.append(CanonicalRevision.token(Integer.toString(part)));
        return key.append('/').append(CanonicalRevision.token(unit.canonicalProgramName())).append('/')
                .append(kind).append('/').append(CanonicalRevision.token(handle)).toString();
    }
}
