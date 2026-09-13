package io.github.gustavo2358.lower.application;

import io.github.gustavo2358.air.model.*;
import io.github.gustavo2358.air.model.Ids.*;
import io.github.gustavo2358.lower.domain.SpInput;
import java.util.*;

/** Preserve proved operands; nominal binding alone is never an exact address/effect proof. */
final class OpaqueOperands {
    record Known(List<Operand> operands,List<OperandId> reads,List<OperandId> writes,List<OriginId> origins) { }
    private OpaqueOperands() { }
    static Known translate(SpInput.StatementFact fact,OperationId operation,ScalarDataTranslator.Result data,
            LocalIds ids,SourceOrigins origins,List<LoweringResult.OperandLink> links,List<Evidence.Uncertainty> uncertainties) {
        var refs=new ArrayList<SpInput.DataReference>();
        if(fact instanceof SpInput.OtherStatement o)refs.addAll(o.knownReferences());
        if(fact instanceof SpInput.IfFact f)refs.addAll(f.conditionReads());
        if(fact instanceof SpInput.MoveFact m) {refs.add(m.target());if(m.source() instanceof SpInput.DataReference r)refs.add(r);}
        var operands=new ArrayList<Operand>();var reads=new ArrayList<OperandId>();var writes=new ArrayList<OperandId>();var evidence=new ArrayList<OriginId>();
        for(var ref:refs) {
            var origin=origins.source("operand",ref.id().handle(),ref.provenance());evidence.add(origin);
            var mapping=ref.wholeItemAccess().map(SpInput.WholeItemAccess::data).map(data.index()::get);
            if(mapping.isEmpty()) {
                var gap=new UncertaintyId(operation.publication(),ids.id("uncertainty","opaque-reference",operation.localId(),ref.id().handle()));
                uncertainties.add(new Evidence.Uncertainty(gap,"NOMINAL_REFERENCE_WITHOUT_ADDRESS_PROOF",List.of(Evidence.Dimension.STORAGE),
                    new Scopes.EntityScope(List.of(operation)),"SP operand "+ref.id().handle()+" has "+ref.binding().status()+" nominal candidates "+ref.binding().candidates().stream().map(SpInput.DataId::handle).toList()+"; memory remains conservatively bounded",origin));
                continue;
            }
            var id=new OperandId(new OperationOwner(operation),ids.id("operand","opaque-reference",operation.localId(),ref.id().handle()));
            var role=ref.role()==SpInput.OperandRole.WRITE?Operand.Role.VALUE_WRITE:Operand.Role.VALUE_READ;
            var place=new Places.ObjectPlace(new Operand.Header(id,role,origin),mapping.orElseThrow().object());
            operands.add(place);if(role==Operand.Role.VALUE_WRITE)writes.add(id);else reads.add(id);
            links.add(new LoweringResult.OperandLink(ref.id(),id,origin));
        }
        return new Known(List.copyOf(operands),List.copyOf(reads),List.copyOf(writes),List.copyOf(evidence));
    }
}
