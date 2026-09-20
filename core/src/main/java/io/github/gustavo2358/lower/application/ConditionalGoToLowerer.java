package io.github.gustavo2358.lower.application;

import io.github.gustavo2358.air.model.*;
import io.github.gustavo2358.air.model.Ids.*;
import io.github.gustavo2358.lower.domain.SpInput;
import java.util.*;

/** Existing generic finite control envelope; one selector occurrence, no numeric evaluation. */
final class ConditionalGoToLowerer {
    static Operations.Opaque translate(SpInput.ConditionalGoToFact g,LabelId continuation,ScalarDataTranslator.Result data,
            UnitId unit,LocalIds ids,SourceOrigins origins,List<LoweringResult.OperandLink> operands,List<Evidence.Uncertainty> uncertainties) {
        var operation=new OperationId(unit,ids.id("operation","indexed-transfer",unit.localId(),g.header().id().handle()));
        var source=origins.source("statement",g.header().id().handle(),g.header().provenance());
        var evidence=new LinkedHashSet<OriginId>();evidence.add(source);
        evidence.add(origins.source("selector",g.header().id().handle(),g.selectorOrigin()));
        var alternatives=new LinkedHashSet<Control.ControlAlternative>();
        for(var d:g.destinations()) {
            var occurrence=g.header().id().handle()+"/destination/"+d.ordinal();
            var local=new LinkedHashSet<OriginId>();local.add(source);
            local.add(origins.source("destination-reference",occurrence,d.referenceOrigin()));
            d.procedureOrigin().ifPresent(o->local.add(origins.source("destination-procedure",occurrence,o)));
            d.entryOrigin().ifPresent(o->local.add(origins.source("destination-entry",occurrence,o)));
            evidence.add(origins.derived(ids.id("origin","indexed-destination",unit.localId(),occurrence),List.copyOf(local),
                "indexed-transfer@1/selector-"+(d.ordinal()+1)+"/"+d.target().map(SpInput.ProcedureId::handle).orElse("unresolved")+"/"+d.targetEntry().map(SpInput.StatementId::handle).orElse("entry-unavailable")));
            d.targetEntry().ifPresent(id->alternatives.add(new Control.JumpAlternative(PartialProgramAssembler.label(id,unit,ids))));
        }
        evidence.add(origins.source("normal-continuation",g.header().id().handle(),g.normalContinuation().provenance()));
        if(continuation!=null)alternatives.add(new Control.Normal(continuation));
        var origin=origins.derived(ids.id("origin","indexed-transfer",unit.localId(),g.header().id().handle()),List.copyOf(evidence),
            "indexed-transfer@1/one-selector-finite-alternatives");
        var known=OpaqueOperands.translate(g,operation,data,ids,origins,operands,uncertainties);
        var scope=new Scopes.EntityScope(List.of(operation));
        var valueGap=new UncertaintyId(unit.publication(),ids.id("uncertainty","indexed-selector",operation.localId(),"value"));
        uncertainties.add(new Evidence.Uncertainty(valueGap,"SELECTOR_VALUE_NOT_EVALUATED",List.of(Evidence.Dimension.VALUES),scope,
            "One source selector determines an ordinal destination or normal continuation; no numeric pruning",origin));
        var gaps=new ArrayList<UncertaintyId>();gaps.add(valueGap);
        var exact=new Evidence.Claim(scope,Evidence.PrecisionStatus.EXACT,List.of());
        boolean closed=GoToAdmission.precise(g);var control=exact;
        if(!closed) {
            var gap=new UncertaintyId(unit.publication(),ids.id("uncertainty","indexed-control",operation.localId(),"remainder"));gaps.add(gap);
            uncertainties.add(new Evidence.Uncertainty(gap,"CONDITIONAL_TRANSFER_INCOMPLETE",List.of(Evidence.Dimension.CONTROL),scope,
                "Known ordinal destinations survive local gaps: "+g.gapCodes(),origin));
            control=new Evidence.Claim(scope,Evidence.PrecisionStatus.OPEN,List.of(gap));
        }
        boolean knownRead=g.selector().isPresent()&&known.reads().size()==1;
        var storage=exact;
        if(!knownRead) {
            var gap=new UncertaintyId(unit.publication(),ids.id("uncertainty","indexed-selector",operation.localId(),"storage"));gaps.add(gap);
            uncertainties.add(new Evidence.Uncertainty(gap,"SELECTOR_READ_NOT_PROVEN",List.of(Evidence.Dimension.STORAGE),scope,"Selector occurrence has no modeled memory read",origin));
            storage=new Evidence.Claim(scope,Evidence.PrecisionStatus.OPEN,List.of(gap));
        }
        var header=new Operations.Header(operation,origin,Evidence.CoverageStatus.ABSTRACTED,new Evidence.Precision(control,storage,exact,
            new Evidence.Claim(scope,Evidence.PrecisionStatus.OPEN,List.of(valueGap)),exact),gaps);
        return new Operations.Opaque(header,"finite-indexed-transfer",known.operands(),List.of(),new Envelopes.Envelope(
            new Envelopes.MemoryEnvelope(known.reads(),Scopes.NoMemory.INSTANCE,
                List.of(),Scopes.NoMemory.INSTANCE,List.of()),
            new Control.ControlEnvelope(List.copyOf(alternatives),Scopes.NoControl.INSTANCE),
            new Envelopes.DependencyEnvelope(List.of(),Scopes.NoResources.INSTANCE)));
    }
}
