package io.github.gustavo2358.lower.testing;

import io.github.gustavo2358.lower.domain.SpInput;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** Deterministic synthetic inventories, not frontend captures or an expanded lowering capability. */
public final class ScaleInputs {
    private ScaleInputs() { }
    public static SpInput create(int size) {
        if (size < 1) throw new IllegalArgumentException("positive synthetic size");
        var base = SpFixtures.minimal(); var first = (SpInput.GobackFact)base.statements().getFirst();
        var statements = new ArrayList<SpInput.StatementFact>(); var roots = new ArrayList<SpInput.StatementId>();
        for (int i = 0; i < size; i++) {
            var id = new SpInput.StatementId(base.unit(), "statement:" + i); roots.add(id);
            statements.add(new SpInput.GobackFact(SpFixtures.header(first.header(), id, i, first.header().containment(),
                    first.header().coverage(), first.header().readiness()), first.exit(), first.localContinuation()));
        }
        var c = base.coverage(); var e = base.entryInventory().entries().getFirst();
        var body = SpFixtures.body(base, statements, new SpInput.Structure(roots, List.of()), List.of(),
                new SpInput.Coverage(c.inventoryStatus(), size, size, 0, 0, 0, c.readiness()));
        return SpFixtures.entry(body, SpFixtures.entryFacts(e, new SpInput.ExecutableStart(SpInput.Availability.KNOWN,
                Optional.of(roots.getLast())), e.signature(), e.coverage(), e.readiness(), e.gaps()));
    }
}
