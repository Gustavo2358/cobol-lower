package io.github.gustavo2358.lower.application;

import io.github.gustavo2358.air.model.*;
import io.github.gustavo2358.air.model.Ids.*;
import io.github.gustavo2358.lower.domain.SpInput;
import java.util.*;

/** Integer initialization and increment have a proved footprint, but deliberately open values. */
final class PerformVaryingEffects {
    static Operations.Opaque effect(SpInput.ProcedurePerformFact p,boolean initial,LabelId next,
            ScalarDataTranslator.Result data,UnitId unit,LocalIds ids,SourceOrigins origins,
            List<LoweringResult.OperandLink> links,List<Evidence.Uncertainty> uncertainties) {
        var role=initial?"perform-varying-initialization":"perform-varying-increment";
        var id=new OperationId(unit,ids.id("operation",role,unit.localId(),p.header().id().handle()));
        var controls=p.varying().orElseThrow().controls();
        var variable=controls.stream().filter(o->o.role()==SpInput.VaryingOperandRole.CONTROL_VARIABLE).findFirst().orElseThrow();
        var value=controls.stream().filter(o->o.role()==(initial?SpInput.VaryingOperandRole.FROM:SpInput.VaryingOperandRole.BY)).findFirst().orElseThrow();
        var source=origins.source("statement",p.header().id().handle(),p.header().provenance());
        var targetOrigin=origins.source(role+"-variable",p.header().id().handle(),variable.provenance());
        var valueOrigin=origins.source(role+"-value",p.header().id().handle(),value.provenance());
        var origin=origins.derived(ids.id("origin",role,unit.localId(),p.header().id().handle()),
            List.of(source,targetOrigin,valueOrigin),"perform-varying@1/implicit-whole-item-write");
        var known=new ArrayList<Operand>();var reads=new ArrayList<OperandId>();
        var reference=variable.references().getFirst();
        var write=place(reference,Operand.Role.VALUE_WRITE,"write",id,data,ids,targetOrigin,links);
        known.add(write);
        if(initial) for(var r:value.references()) {
            var place=place(r,Operand.Role.VALUE_READ,"from-read",id,data,ids,valueOrigin,links);
            known.add(place);reads.add(place.header().id());
        } else {
            // This read is implied by the typed BY update, not by spelling or a synthesized expression.
            var readOrigin=origins.derived(ids.id("origin",role+"-read",id.localId(),reference.id().handle()),
                List.of(targetOrigin,valueOrigin),"perform-varying@1/update-reads-current-control-item");
            var place=place(reference,Operand.Role.VALUE_READ,"control-read",id,data,ids,readOrigin,links);
            known.add(place);reads.add(place.header().id());
        }
        var reason=new UncertaintyId(unit.publication(),ids.id("uncertainty",role,id.localId(),"numeric-value"));
        var scope=new Scopes.EntityScope(List.of(id));
        uncertainties.add(new Evidence.Uncertainty(reason,"PERFORM_VARYING_NUMERIC_VALUE_OPEN",List.of(Evidence.Dimension.VALUES),scope,
            "Published integer control item is overwritten; numeric conversion and arithmetic values remain open",origin));
        var exact=new Evidence.Claim(scope,Evidence.PrecisionStatus.EXACT,List.of());
        var open=new Evidence.Claim(scope,Evidence.PrecisionStatus.OPEN,List.of(reason));
        return new Operations.Opaque(new Operations.Header(id,origin,Evidence.CoverageStatus.ABSTRACTED,
            new Evidence.Precision(exact,exact,exact,open,exact),List.of(reason)),role,known,List.of(),
            new Envelopes.Envelope(new Envelopes.MemoryEnvelope(reads,Scopes.NoMemory.INSTANCE,List.of(write.header().id()),
                Scopes.NoMemory.INSTANCE,List.of(write.header().id())),
                new Control.ControlEnvelope(List.of(new Control.JumpAlternative(next)),Scopes.NoControl.INSTANCE),
                new Envelopes.DependencyEnvelope(List.of(),Scopes.NoResources.INSTANCE)));
    }
    private static Places.ObjectPlace place(SpInput.DataReference ref,Operand.Role role,String purpose,OperationId operation,
            ScalarDataTranslator.Result data,LocalIds ids,OriginId origin,List<LoweringResult.OperandLink> links) {
        var operand=new OperandId(new OperationOwner(operation),ids.id("operand",purpose,operation.localId(),ref.id().handle()));
        links.add(new LoweringResult.OperandLink(ref.id(),operand,origin));
        return new Places.ObjectPlace(new Operand.Header(operand,role,origin),data.index().get(ref.wholeItemAccess().orElseThrow().data()).object());
    }
}
