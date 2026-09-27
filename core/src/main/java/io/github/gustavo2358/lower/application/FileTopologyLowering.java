package io.github.gustavo2358.lower.application;

import io.github.gustavo2358.air.model.*;
import io.github.gustavo2358.air.model.Ids.*;
import io.github.gustavo2358.lower.domain.*;
import static io.github.gustavo2358.lower.domain.ControlTopology.*;
import java.util.*;
import java.util.function.*;

/** Materializes published points; no statement/operand ordering is discovered here. */
final class FileTopologyLowering {
    private FileTopologyLowering() { }
    static LabelId label(UnitId unit,LocalIds ids,String point) {
        return new LabelId(unit,ids.id("label","file-topology-point",unit.localId(),point));
    }
    static FileSortLowering.Layout layout(FileFlow flow,SpInput.StatementFact fact,UnitId unit,LocalIds ids,
            Function<Target,LabelId> destination,BiFunction<String,List<String>,OriginId> evidence,List<Evidence.Uncertainty> gaps) {
        var prefix=new ArrayList<Sequence>();var uses=new HashMap<Integer,FileSortLowering.Step>();
        var entryOrigin=evidence.apply(flow.statement()+"/file-entry",flow.proofs());
        var entryOp=new OperationId(unit,ids.id("operation","file-topology-entry",unit.localId(),flow.statement()));
        prefix.add(new Sequence(PartialProgramAssembler.label(fact.header().id(),unit,ids),List.of(),
            new Operations.Jump(new Operations.Header(entryOp,entryOrigin,Evidence.CoverageStatus.MODELED,ScalarEvidence.assign(entryOp),List.of()),destination.apply(flow.entry())),entryOrigin));
        for(var point:flow.points()) {
            var at=label(unit,ids,point.id());
            if(point.kind()==FilePointKind.USE) {
                uses.put(point.ordinal(),new FileSortLowering.Step(at,destination.apply(point.targets().getFirst())));continue;
            }
            var origin=evidence.apply(point.id(),point.proofs());
            var op=new OperationId(unit,ids.id("operation","file-topology-choice",unit.localId(),point.id()));
            var gap=new UncertaintyId(unit.publication(),ids.id("uncertainty","file-topology-choice",unit.localId(),op.localId()));
            var scope=new Scopes.EntityScope(List.of(op));
            gaps.add(new Evidence.Uncertainty(gap,"FILE_AGGREGATE_ORDER_COUNT_NOT_PROVEN",List.of(Evidence.Dimension.CONTROL),scope,
                "Published aggregate alternatives retain unknown participant order and repetition",origin));
            var exact=new Evidence.Claim(scope,Evidence.PrecisionStatus.EXACT,List.of());var open=new Evidence.Claim(scope,Evidence.PrecisionStatus.OPEN,List.of(gap));
            var header=new Operations.Header(op,origin,Evidence.CoverageStatus.ABSTRACTED,new Evidence.Precision(open,exact,exact,exact,exact),List.of(gap));
            var alternatives=point.targets().stream().map(destination).distinct().<Control.ControlAlternative>map(Control.JumpAlternative::new).toList();
            prefix.add(new Sequence(at,List.of(),new Operations.Opaque(header,"file-aggregate-choice",List.of(),List.of(),new Envelopes.Envelope(
                new Envelopes.MemoryEnvelope(List.of(),Scopes.NoMemory.INSTANCE,List.of(),Scopes.NoMemory.INSTANCE,List.of()),
                new Control.ControlEnvelope(alternatives,Scopes.NoControl.INSTANCE),new Envelopes.DependencyEnvelope(List.of(),Scopes.NoResources.INSTANCE))),origin));
        }
        return new FileSortLowering.Layout(List.copyOf(prefix),Map.copyOf(uses));
    }
}
