package io.github.gustavo2358.lower.testing;

import io.github.gustavo2358.air.model.*;
import io.github.gustavo2358.lower.application.*;
import io.github.gustavo2358.lower.domain.SpInput;
import java.util.*;
import static io.github.gustavo2358.lower.domain.SpInput.*;
import static io.github.gustavo2358.lower.testing.CallOracle.check;

public final class MoveDataSuite {
    public static SpInput input() {
        var base = CallInputs.create(2, 2, 8, "PROGA", false);
        var copy = (MoveFact) base.statements().get(1);
        var data = base.dataDeclarations().getFirst().id();
        var read = new DataReference(copy.source().id(), OperandRole.READ,
            new Binding(ResolutionStatus.RESOLVED, List.of(data), Optional.of(data)),
            Optional.of(new WholeItemAccess(data)), copy.source().provenance());
        var statements = new ArrayList<>(base.statements());
        statements.set(1, new MoveFact(copy.header(), read, copy.target(), CopySemantics.FULL_IDENTITY, copy.normalContinuation()));
        var result = ScalarInputs.replace(base, base.dataDeclarations(), statements);
        return IfInputs.proof(result, Optional.of(new IndependentStorageSet(Availability.KNOWN,
            StorageIndependenceRule.INDEPENDENT_WORKING_STORAGE_ROOTS, "IBM_ENTERPRISE_COBOL_6_4_WORKING_STORAGE",
            base.dataDeclarations().stream().map(DataFact::id).toList(), Optional.of(ScalarInputs.provenance(4, 0)), List.of())));
    }
    public static int run() {
        int before = CallOracle.assertions();
        var input = input();
        var result = new CobolLowerer().lower(input, ScalarSuite.OPTIONS);
        check(result.status() == LoweringResult.Status.SUCCESS, "data copy admitted: " + result.admission());
        var p = result.publication().orElseThrow();
        var assigns = p.units().getFirst().sequences().stream().flatMap(s -> s.instructions().stream())
            .filter(Operations.Assign.class::isInstance).map(Operations.Assign.class::cast).toList();
        check(assigns.size() == 2 && assigns.getFirst().value() instanceof Expressions.Literal, "literal remains literal");
        check(((Values.TextValue)((Expressions.Literal)assigns.getFirst().value()).value()).value().equals("PROGA   "), "literal fitting preserved");
        check(assigns.get(1).value() instanceof Expressions.Read, "copy is Read, not constant folding");
        var read = (Expressions.Read) assigns.get(1).value();
        check(read.header().role() == Operand.Role.VALUE_READ && read.place().header().role() == Operand.Role.VALUE_READ, "real read roles");
        check(((Places.ObjectPlace)read.place()).object().equals(((Places.ObjectPlace)assigns.getFirst().destination()).object()), "read source ObjectId");
        check(!read.place().header().origin().equals(assigns.get(1).destination().header().origin()), "source and target origins distinct");
        check(assigns.get(1).destination().header().role() == Operand.Role.VALUE_WRITE, "write role");
        check(p.premises().size() == 1 && p.premises().getFirst().assertion() instanceof Proofs.DisjointStorage, "source-derived linear storage premise");
        var m = (MoveFact)input.statements().get(1); var source = (DataReference)m.source();
        check(result.operands().stream().anyMatch(link -> link.source().equals(m.source().id())
            && link.target().equals(read.header().id()) && link.origin().equals(read.header().origin())), "source occurrence correlation");
        var without = new CobolLowerer().lower(IfInputs.proof(input, Optional.empty()), ScalarSuite.OPTIONS);
        check(without.publication().orElseThrow().premises().isEmpty(), "no fabricated storage premise");
        var changedData = new ArrayList<>(input.dataDeclarations());
        // Only the receiver extent changes: the preceding literal MOVE remains valid.
        var d = changedData.get(1);
        changedData.set(1, new DataFact(d.id(), d.canonicalName(), d.picture(), d.provenance(), d.coverage(), d.readiness(),
            Optional.of(new ScalarText(LogicalDomain.TEXT, 9, StorageClass.WORKING_STORAGE, DeclarationScope.LOCAL))));
        check(new CobolLowerer().lower(ScalarInputs.replace(input, changedData, input.statements()), ScalarSuite.OPTIONS).publication().isEmpty(),
            "incompatible source/target extents refused");
        for (var bad : List.of(
                new DataReference(source.id(), OperandRole.WRITE, source.binding(), source.wholeItemAccess(), source.provenance()),
                new DataReference(source.id(), OperandRole.READ, source.binding(), Optional.empty(), source.provenance()),
                new DataReference(source.id(), OperandRole.READ, new Binding(ResolutionStatus.UNRESOLVED, List.of(), Optional.empty()), Optional.empty(), source.provenance()))) {
            var statements = new ArrayList<>(input.statements());
            statements.set(1,new MoveFact(m.header(),bad,m.target(),m.copySemantics(),m.normalContinuation()));
            check(new CobolLowerer().lower(ScalarInputs.replace(input,input.dataDeclarations(),statements),ScalarSuite.OPTIONS).publication().isEmpty(), "invalid read refused");
        }
        return CallOracle.assertions() - before;
    }
    public static void main(String[] args) { System.out.println("MOVE_DATA_TESTS=" + run()); }
}
