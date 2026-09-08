package io.github.gustavo2358.lower.testing;

import io.github.gustavo2358.lower.domain.SpInput;
import java.util.*;
import static io.github.gustavo2358.lower.domain.SpInput.*;

/** Synthetic published facts, no COBOL parser, JSON or lowerer. Names deliberately collide. */
public final class ScalarInputs {
    private ScalarInputs() { }
    public static Provenance provenance(int line, int column) {
        return new Provenance(new Location("<expanded>", line, column, line, column + 3),
            new Location("synthetic.cbl", line, column + 7, line, column + 10), List.of(), true);
    }
    public static SpInput create(int dataCount, int moveCount) {
        var base = SpFixtures.minimal(); var unit = base.unit();
        var readiness = base.statements().getFirst().header().readiness();
        var data = new ArrayList<DataFact>(); var statements = new ArrayList<StatementFact>();
        for (int i = 0; i < dataCount; i++) data.add(new DataFact(new DataId(unit, "data:" + i), "same-display-name", Optional.of("not interpreted"),
            provenance(i + 1, 0), CoverageStatus.MODELED, readiness,
            Optional.of(new ScalarText(LogicalDomain.TEXT, 5, StorageClass.WORKING_STORAGE, DeclarationScope.LOCAL))));
        for (int i = 0; i < moveCount; i++) {
            var id = new StatementId(unit, "statement:" + i); var d = data.get(i % dataCount).id();
            var h = new StatementHeader(id, i, new Containment(Optional.empty(), Branch.ROOT), provenance(i + 100, 0), CoverageStatus.MODELED, readiness);
            statements.add(new MoveFact(h, new LiteralSource(new OperandId(id, "operand:" + i + ":0"), LiteralKind.ALPHANUMERIC,
                Optional.of(new LogicalValue(LogicalDomain.TEXT, i == 0 ? "PROGA" : "BBBBB", 5)), provenance(i + 100, 5)),
                new DataReference(new OperandId(id, "operand:" + i + ":1"), OperandRole.WRITE,
                    new Binding(ResolutionStatus.RESOLVED, List.of(d), Optional.of(d)), Optional.of(new WholeItemAccess(d)), provenance(i + 100, 20)),
                CopySemantics.FULL_IDENTITY, new NormalContinuation(ContinuationAvailability.KNOWN,
                    Optional.of(new StatementId(unit, "statement:" + (i + 1))), provenance(i + 100, 30))));
        }
        var h = new StatementHeader(new StatementId(unit, "statement:" + moveCount), moveCount,
            new Containment(Optional.empty(), Branch.ROOT), provenance(moveCount + 100, 0), CoverageStatus.MODELED, readiness);
        statements.add(new GobackFact(h, GobackExit.CURRENT_PROGRAM_INVOCATION, LocalContinuation.NONE));
        return replace(base, data, statements);
    }
    public static SpInput replace(SpInput base, List<DataFact> data, List<StatementFact> statements) {
        return new SpInput(base.unit(), base.policy(), data, statements,
            new Structure(statements.stream().map(s -> s.header().id()).toList(), List.of()), base.gaps(),
            new Coverage(InventoryStatus.COMPLETE, statements.size(), statements.size(), 0, 0, 0, base.coverage().readiness()), base.entryInventory());
    }
    public static SpInput move(SpInput base, MoveFact move) {
        var statements = new ArrayList<>(base.statements()); statements.set(0, move);
        return replace(base, base.dataDeclarations(), statements);
    }
    public static MoveFact move(MoveFact base, LiteralSource source, DataReference target, CopySemantics copy, NormalContinuation continuation) {
        return new MoveFact(base.header(), source, target, copy, continuation);
    }
    public static DataReference target(MoveFact m, Binding binding, Optional<WholeItemAccess> whole) {
        return new DataReference(m.target().id(), m.target().role(), binding, whole, m.target().provenance());
    }
}
