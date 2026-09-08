package io.github.gustavo2358.lower.application;

import io.github.gustavo2358.air.model.*;
import io.github.gustavo2358.air.model.Ids.*;
import io.github.gustavo2358.lower.domain.SpInput;
import java.util.*;

/** One object and one abstract persistent private cell per admitted scalar declaration. */
final class ScalarDataTranslator {
    record Result(List<Memory.ObjectDeclaration> objects, List<Memory.Storage> storage,
                  Map<SpInput.DataId, LoweringResult.DataLink> index) { }
    static Result translate(List<SpInput.DataFact> data, UnitId unit, LocalIds ids, SourceOrigins origins,
            List<Evidence.CoverageItem> items, List<Evidence.Uncertainty> uncertainties) {
        var objects = new ArrayList<Memory.ObjectDeclaration>(); var cells = new ArrayList<Memory.Storage>();
        var index = new LinkedHashMap<SpInput.DataId, LoweringResult.DataLink>();
        for (var d : data) {
            var object = new ObjectId(unit, ids.id("object", "scalar-declaration", unit.localId(), d.id().handle()));
            var cell = new StorageId(unit.publication(), ids.id("storage", "scalar-persistent-cell", unit.localId(), d.id().handle()));
            var origin = origins.source("data", d.id().handle(), d.provenance());
            var objectOrigin = origins.derived(ids.id("origin", "scalar-object", unit.localId(), d.id().handle()), List.of(origin), "scalar-text-move@1/data-object");
            var cellOrigin = origins.derived(ids.id("origin", "scalar-cell", unit.localId(), d.id().handle()), List.of(origin), "scalar-text-move@1/local-working-storage-cell");
            var text = Types.known(Types.Builtin.TEXT);
            var precision = ScalarEvidence.limited(ids, unit.publication(), object, d.id().handle(), origin, Evidence.Dimension.STORAGE, uncertainties);
            objects.add(new Memory.ObjectDeclaration(object, Optional.of(d.canonicalName()), text, new Memory.CellBinding(cell),
                Memory.Visibility.PRIVATE, objectOrigin, Evidence.CoverageStatus.MODELED, precision));
            cells.add(new Memory.Cell(new Memory.StorageHeader(cell, Optional.of(unit), Memory.Lifetime.PERSISTENT, Memory.Visibility.PRIVATE, cellOrigin), text));
            index.put(d.id(), new LoweringResult.DataLink(d.id(), object, cell, origin));
            items.add(ScalarEvidence.item(unit.publication(), "data", d.id().handle(), origin, List.of(object, cell)));
        }
        return new Result(objects, cells, index);
    }
}
