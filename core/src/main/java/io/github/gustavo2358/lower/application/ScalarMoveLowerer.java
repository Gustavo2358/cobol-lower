package io.github.gustavo2358.lower.application;

import io.github.gustavo2358.air.model.*;
import io.github.gustavo2358.air.model.Ids.*;
import io.github.gustavo2358.lower.domain.SpInput;
import java.util.*;

/** Scalar publication assembly; no transport dependencies or universal statement framework. */
final class ScalarMoveLowerer implements LowerInput {
    @Override public LoweringResult lower(SpInput input, Options options) {
        var plan = new ScalarMoveAdmission().plan(input, options.admission()); var admission = plan.admission();
        if (admission.status() != Admission.Status.ADMITTED) {
            var status = switch (admission.status()) {
                case INVALID_INPUT -> LoweringResult.Status.INVALID_INPUT;
                case BLOCKED_LOWERING -> LoweringResult.Status.BLOCKED_LOWERING;
                case UNSUPPORTED_SLICE -> LoweringResult.Status.UNSUPPORTED_SLICE;
                case IMPLEMENTATION_LIMIT -> LoweringResult.Status.IMPLEMENTATION_LIMIT;
                case ADMITTED -> throw new IllegalStateException("already handled");
            };
            return new LoweringResult(status, admission, Optional.empty(), Optional.empty(), List.of(), List.of(), List.of());
        }
        var revision = CanonicalRevision.scalar(input, plan.data(), plan.moves(), plan.terminal().orElseThrow(), options.maximumIdentityCharacters());
        if (revision.isEmpty()) return new LoweringResult(LoweringResult.Status.IMPLEMENTATION_LIMIT, admission, Optional.empty(), Optional.empty(), List.of(), List.of(),
            List.of(new LoweringResult.Limitation(LoweringResult.LimitCode.IDENTITY_LIMIT, "publication", "Compact PublicationId needs 32 characters; no prefix published.")));
        var publication = new PublicationId(revision.orElseThrow()); var unit = new UnitId(publication, "unit"); var ids = new LocalIds();
        var origins = new SourceOrigins(publication, ids); var items = new ArrayList<Evidence.CoverageItem>(); var uncertainties = new ArrayList<Evidence.Uncertainty>();
        var statements = new ArrayList<LoweringResult.StatementLink>(); var operands = new ArrayList<LoweringResult.OperandLink>();
        var sourceEntry = input.entryInventory().entries().getFirst();
        var entryOrigin = origins.source("entry", sourceEntry.id().handle(), sourceEntry.provenance());
        var data = ScalarDataTranslator.translate(plan.data(), unit, ids, origins, items, uncertainties);
        var sequence = ScalarSequenceAssembler.assemble(plan, data, unit, entryOrigin, ids, origins, statements, operands, items, uncertainties);
        var entryId = new EntryId(unit, ids.id("entry", "primary-entry", unit.localId(), sourceEntry.id().handle()));
        var signature = new Interactions.Signature(new Interactions.ParameterInventory(List.of(), Interactions.NoRemainder.INSTANCE),
            new Interactions.ResultInventory(List.of(), Interactions.NoRemainder.INSTANCE), entryOrigin);
        var entry = new Entries.Entry(entryId, Optional.of(sequence.label()), signature, new Entries.EntryState(List.of(), List.of()), entryOrigin);
        var entryLinks = List.of(new LoweringResult.EntryLink(sourceEntry.id(), entryId, sequence.label(), entryOrigin));
        items.add(ScalarEvidence.item(publication, "entry", sourceEntry.id().handle(), entryOrigin, List.of(entryId)));
        var unitOrigin = origins.derived("unit-origin", List.of(entryOrigin, sequence.origin()), "scalar-text-move@1/selected-unit");
        var gaps = new ArrayList<UncertaintyId>();
        for (var code : input.entryInventory().gapCodes()) {
            var gap = new UncertaintyId(publication, ids.id("uncertainty", "scalar-entry-inventory", unit.localId(), Integer.toString(gaps.size()))); gaps.add(gap);
            uncertainties.add(new Evidence.Uncertainty(gap, "cobol-lower:" + code, List.of(Evidence.Dimension.CONTROL), new Scopes.UnitScope(unit),
                "SP PRIMARY_ONLY/PARTIAL preserves this entry inventory gap.", entryOrigin));
        }
        int gapIndex = 0;
        for (var gap : input.gaps()) {
            var key = Integer.toString(gapIndex++); var origin = origins.source("gap", key, gap.provenance());
            var id = new UncertaintyId(publication, ids.id("uncertainty", "scalar-sp-gap", unit.localId(), key)); gaps.add(id);
            uncertainties.add(new Evidence.Uncertainty(id, "cobol-sp:" + gap.code(), List.of(Evidence.Dimension.values()), new Scopes.UnitScope(unit),
                gap.scope().name() + ": " + gap.detail(), origin));
        }
        var unitCoverage = new Evidence.Coverage(Evidence.InventoryStatus.PARTIAL, new Scopes.UnitScope(unit), items, gaps);
        var body = new Unit(unit, Optional.empty(), data.objects(), List.of(), List.of(entry), List.of(sequence), List.of(),
            Unit.BodyAvailability.AVAILABLE, Optional.empty(), unitCoverage, unitOrigin);
        var premises = StoragePremise.available(input, data, unit, ids, origins);
        var output = new Publication(publication, SemanticVersion.AIR_2_0_0, new Capabilities.Manifest(List.of(), List.of()), origins.artifacts(),
            List.of(body), data.storage(), List.of(), List.of(), origins.origins(),
            new Evidence.Coverage(Evidence.InventoryStatus.PARTIAL, new Scopes.PublicationScope(publication), items, gaps), uncertainties, premises);
        var assessment = OutputAssessment.assess(output, options.validation());
        return new LoweringResult(assessment.status(), admission, assessment.publication(), Optional.of(assessment.validation()),
            entryLinks, statements, origins.limitations(), List.copyOf(data.index().values()), operands);
    }
}
