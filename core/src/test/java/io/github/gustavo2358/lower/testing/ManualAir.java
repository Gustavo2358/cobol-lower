package io.github.gustavo2358.lower.testing;

import io.github.gustavo2358.air.model.*;
import io.github.gustavo2358.air.model.Ids.*;
import java.util.List;
import java.util.Optional;

/** Synthetic AIR fixture, hand-authored from AIR 2.0 §1–4 and operations §8. No SP producer. */
public final class ManualAir {
    private ManualAir() {}

    public static Publication withProfileObligation() {
        Publication p = create(false, false);
        return new Publication(p.id(), p.airVersion(), new Capabilities.Manifest(List.of(),
                List.of(new Capabilities.Capability("AIR-STRUCTURE", "2"))), p.artifacts(), p.units(),
                p.storage(), p.resources(), p.artifactRelations(), p.origins(), p.coverage(),
                p.uncertainties(), p.premises());
    }

    public static Publication create(boolean dangling, boolean halt) {
        PublicationId publication = new PublicationId("manual-cp0-revision");
        UnitId unit = new UnitId(publication, "manual-unit");
        OriginId origin = new OriginId(publication, "manual-origin");
        LabelId label = new LabelId(unit, "start");
        OperationId operation = new OperationId(unit, "exit");
        Scopes.EntityScope scope = new Scopes.EntityScope(List.of(operation));
        Evidence.Claim exact = new Evidence.Claim(scope, Evidence.PrecisionStatus.EXACT, List.of());
        Evidence.Claim na = new Evidence.Claim(scope, Evidence.PrecisionStatus.NOT_APPLICABLE, List.of());
        Operations.Header header = new Operations.Header(operation, origin, Evidence.CoverageStatus.MODELED,
                new Evidence.Precision(exact, na, na, na, na), List.of());
        Terminator terminal = halt
                ? new Operations.Halt(header, Operations.HaltKind.NORMAL)
                : new Operations.Return(header, List.of());
        Sequence sequence = new Sequence(label, List.of(), terminal, origin);
        Interactions.Signature signature = new Interactions.Signature(
                new Interactions.ParameterInventory(List.of(), Interactions.NoRemainder.INSTANCE),
                new Interactions.ResultInventory(List.of(), Interactions.NoRemainder.INSTANCE), origin);
        Entries.Entry entry = new Entries.Entry(new EntryId(unit, "primary"),
                Optional.of(dangling ? new LabelId(unit, "missing") : label), signature,
                new Entries.EntryState(List.of(), List.of()), origin);
        Evidence.CoverageItem item = new Evidence.CoverageItem("manual-exit", origin,
                Evidence.CoverageStatus.MODELED, List.of(operation), List.of(), Optional.empty());
        Unit body = new Unit(unit, Optional.empty(), List.of(), List.of(), List.of(entry),
                List.of(sequence), List.of(), Unit.BodyAvailability.AVAILABLE, Optional.empty(),
                new Evidence.Coverage(Evidence.InventoryStatus.COMPLETE, new Scopes.UnitScope(unit),
                        List.of(item), List.of()), origin);
        return new Publication(publication, SemanticVersion.AIR_2_0_0,
                new Capabilities.Manifest(List.of(), List.of()), List.of(), List.of(body),
                List.of(), List.of(), List.of(),
                List.of(new Origins.Unavailable(origin, "synthetic hand-authored AIR; no COBOL source")),
                new Evidence.Coverage(Evidence.InventoryStatus.COMPLETE,
                        new Scopes.PublicationScope(publication), List.of(item), List.of()),
                List.of(), List.of());
    }
}
