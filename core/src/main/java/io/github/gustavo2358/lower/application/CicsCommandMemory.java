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
        fact.implicitArea().ifPresent(refs::add);
        fact.length().filter(e->e.kind()==SpInput.OperandExpressionKind.DATA_REFERENCE).flatMap(SpInput.OperandExpression::reference).ifPresent(refs::add);
        return List.copyOf(refs);
    }
    /** Strict execution policy retains the existing requirement for a closed, admitted footprint. */
    static boolean ready(SpInput.CicsCommandFact fact,RegionalStorageAdmission.Index storage) {
        return storage!=null&&controlReady(fact,storage.owner())&&fact.hostEffects().isPresent()&&references(fact).stream().allMatch(ref->admitted(ref,storage));
    }
    /** Partial publication can preserve proved control while carrying open MAY memory. */
    static boolean controlReady(SpInput.CicsCommandFact fact,SpInput owner) {
        return fact.syntaxStatus()==SpInput.CicsCommandSyntaxStatus.SUPPORTED&&owner.controlTopology().isPresent();
    }
    static boolean admitted(SpInput.DataReference ref,RegionalStorageAdmission.Index storage) {
        if(storage==null||storage.facts()==null)return false;
        var id=ref.logicalWholeItem().orElse(null);if(id==null)return false;var view=storage.byData().get(id);
        if(view==null||!ref.binding().selected().equals(Optional.ofNullable(id)))return false;
        var binding=storage.facts().bindings.get(view.node().handle());
        return binding!=null&&storage.facts().available(binding.dependencies())&&(!binding.cells().isEmpty()||!binding.regions().isEmpty());
    }
    static Operations.Opaque translate(SpInput.CicsCommandFact fact,RegionalStorageAdmission.Index storage,ScalarDataTranslator.Result data,UnitId unit,LocalIds ids,
            SourceOrigins origins,List<LoweringResult.OperandLink> links,List<Evidence.Uncertainty> uncertainties,
            List<Control.ControlAlternative> destinations) {
        return translate(fact.header(),"cics-host-command/"+fact.commandKind(),references(fact).stream().filter(ref->admitted(ref,storage)).toList(),fact.hostEffects().isEmpty()||references(fact).stream().anyMatch(ref->!admitted(ref,storage)),data,unit,ids,origins,links,uncertainties,destinations);
    }
    static Operations.Opaque registration(SpInput.CicsHandlerFact fact,ScalarDataTranslator.Result data,UnitId unit,LocalIds ids,
            SourceOrigins origins,List<LoweringResult.OperandLink> links,List<Evidence.Uncertainty> uncertainties,
            List<Control.ControlAlternative> destinations) {
        if(fact.registrationEffects().isEmpty())throw new IllegalArgumentException("registration effect proof required");
        return translate(fact.header(),"cics-handler-registration/"+fact.action(),List.of(),false,data,unit,ids,origins,links,uncertainties,destinations);
    }
    private static Operations.Opaque translate(SpInput.StatementHeader fact,String kind,List<SpInput.DataReference> references,boolean openFootprint,
            ScalarDataTranslator.Result data,UnitId unit,LocalIds ids,SourceOrigins origins,List<LoweringResult.OperandLink> links,
            List<Evidence.Uncertainty> uncertainties,List<Control.ControlAlternative> destinations) {
        var key=fact.id().handle();var op=new OperationId(unit,ids.id("operation","cics-host-command",unit.localId(),key));
        var source=origins.source("statement",key,fact.provenance());
        var origin=origins.derived(ids.id("origin","cics-host-command",unit.localId(),key),List.of(source),"sp2.46/qualified-application-host-effects");
        var values=new ArrayList<Operand>();var reads=new ArrayList<OperandId>();var writes=new ArrayList<OperandId>();
        for(var ref:references) {
            var object=ref.logicalWholeItem().map(data.nominal()::get).orElse(null);
            if(object==null){openFootprint=true;continue;}
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
            openFootprint?"Host footprint is unavailable in part; MAY effects remain open without authorizing any overwrite":"Application host footprint is bounded; runtime response values, task state and external resources remain unknown",origin));
        var open=new Evidence.Claim(scope,Evidence.PrecisionStatus.OPEN,List.of(gap));var exact=new Evidence.Claim(scope,Evidence.PrecisionStatus.EXACT,List.of());
        var header=new Operations.Header(op,origin,Evidence.CoverageStatus.ABSTRACTED,new Evidence.Precision(exact,exact,open,open,open),List.of(gap));
        Scopes.MemoryBound other=openFootprint?new Scopes.WithinMemory(new Scopes.VisibleMemory(unit,true)):Scopes.NoMemory.INSTANCE;
        return new Operations.Opaque(header,kind,values,List.of(),
            new Envelopes.Envelope(new Envelopes.MemoryEnvelope(reads,other,writes,other,List.of()),
                new Control.ControlEnvelope(destinations,Scopes.NoControl.INSTANCE),new Envelopes.DependencyEnvelope(List.of(),Scopes.AnyResource.INSTANCE)));
    }
}
