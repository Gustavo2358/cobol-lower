package io.github.gustavo2358.lower.application;

import io.github.gustavo2358.lower.domain.SpInput;
import io.github.gustavo2358.air.model.*;
import io.github.gustavo2358.air.model.Ids.*;
import java.util.*;

/** Translate a closed source host footprint; runtime/condition effects remain open. */
final class CicsCommandMemory {
    private CicsCommandMemory() { }
    static List<SpInput.DataReference> references(SpInput.CicsCommandFact fact) {
        var refs=new ArrayList<SpInput.DataReference>();
        fact.options().forEach(o->o.reference().ifPresent(refs::add));
        fact.length().filter(e->e.kind()==SpInput.OperandExpressionKind.DATA_REFERENCE).flatMap(SpInput.OperandExpression::reference).ifPresent(refs::add);
        return List.copyOf(refs);
    }
    static boolean ready(SpInput.CicsCommandFact fact,RegionalStorageAdmission.Index storage) {
        if(fact.hostEffects().isEmpty()||storage==null||storage.facts()==null||storage.owner().controlTopology().isEmpty())return false;
        for(var ref:references(fact)) {
            var data=ref.logicalWholeItem().orElse(null);var view=storage.byData().get(data);
            if(view==null||!ref.binding().selected().equals(Optional.ofNullable(data)))return false;
            var binding=storage.facts().bindings.get(view.node().handle());
            if(binding==null||!storage.facts().available(binding.dependencies())
                    ||binding.cells().isEmpty()&&binding.regions().isEmpty())return false;
        }
        return true;
    }
    static Operations.Opaque translate(SpInput.CicsCommandFact fact,ScalarDataTranslator.Result data,UnitId unit,LocalIds ids,
            SourceOrigins origins,List<LoweringResult.OperandLink> links,List<Evidence.Uncertainty> uncertainties,
            List<Control.ControlAlternative> destinations) {
        return translate(fact.header(),"cics-host-command/"+fact.commandKind(),references(fact),data,unit,ids,origins,links,uncertainties,destinations);
    }
    static Operations.Opaque registration(SpInput.CicsHandlerFact fact,ScalarDataTranslator.Result data,UnitId unit,LocalIds ids,
            SourceOrigins origins,List<LoweringResult.OperandLink> links,List<Evidence.Uncertainty> uncertainties,
            List<Control.ControlAlternative> destinations) {
        if(fact.registrationEffects().isEmpty())throw new IllegalArgumentException("registration effect proof required");
        return translate(fact.header(),"cics-handler-registration/"+fact.action(),List.of(),data,unit,ids,origins,links,uncertainties,destinations);
    }
    private static Operations.Opaque translate(SpInput.StatementHeader fact,String kind,List<SpInput.DataReference> references,
            ScalarDataTranslator.Result data,UnitId unit,LocalIds ids,SourceOrigins origins,List<LoweringResult.OperandLink> links,
            List<Evidence.Uncertainty> uncertainties,List<Control.ControlAlternative> destinations) {
        var key=fact.id().handle();var op=new OperationId(unit,ids.id("operation","cics-host-command",unit.localId(),key));
        var source=origins.source("statement",key,fact.provenance());
        var origin=origins.derived(ids.id("origin","cics-host-command",unit.localId(),key),List.of(source),"sp2.46/qualified-application-host-effects");
        var values=new ArrayList<Operand>();var reads=new ArrayList<OperandId>();var writes=new ArrayList<OperandId>();
        for(var ref:references) {
            var object=Objects.requireNonNull(data.nominal().get(ref.logicalWholeItem().orElseThrow()),"admitted host memory must materialize");
            var operand=new OperandId(new OperationOwner(op),ids.id("operand","cics-host",op.localId(),ref.id().handle()));
            var operandOrigin=origins.source("operand",ref.id().handle(),ref.provenance());
            boolean write=ref.role()==SpInput.OperandRole.WRITE;
            values.add(new Places.ObjectPlace(new Operand.Header(operand,write?Operand.Role.VALUE_WRITE:Operand.Role.VALUE_READ,operandOrigin),object));
            (write?writes:reads).add(operand);links.add(new LoweringResult.OperandLink(ref.id(),operand,operandOrigin));
        }
        var gap=new UncertaintyId(unit.publication(),ids.id("uncertainty","cics-runtime-effects",op.localId(),key));
        var scope=new Scopes.EntityScope(List.of(op));
        uncertainties.add(new Evidence.Uncertainty(gap,"CICS_RUNTIME_VALUES_AND_ENVIRONMENT_PARTIAL",
            List.of(Evidence.Dimension.VALUES,Evidence.Dimension.EFFECTS,Evidence.Dimension.DEPENDENCIES),scope,
            "Application host footprint is bounded; runtime response values, task state and external resources remain unknown",origin));
        var open=new Evidence.Claim(scope,Evidence.PrecisionStatus.OPEN,List.of(gap));var exact=new Evidence.Claim(scope,Evidence.PrecisionStatus.EXACT,List.of());
        var header=new Operations.Header(op,origin,Evidence.CoverageStatus.ABSTRACTED,new Evidence.Precision(exact,exact,open,open,open),List.of(gap));
        return new Operations.Opaque(header,kind,values,List.of(),
            new Envelopes.Envelope(new Envelopes.MemoryEnvelope(reads,Scopes.NoMemory.INSTANCE,writes,Scopes.NoMemory.INSTANCE,List.of()),
                new Control.ControlEnvelope(destinations,Scopes.NoControl.INSTANCE),new Envelopes.DependencyEnvelope(List.of(),Scopes.AnyResource.INSTANCE)));
    }
}
