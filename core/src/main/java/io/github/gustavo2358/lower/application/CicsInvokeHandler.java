package io.github.gustavo2358.lower.application;

import io.github.gustavo2358.air.model.*;
import io.github.gustavo2358.air.model.Ids.*;
import io.github.gustavo2358.lower.domain.SpInput;
import java.util.*;
import static io.github.gustavo2358.lower.application.Admission.Rule;

/** CICS Program Control boundary: source target, partial signature, conservative foreign effects. */
final class CicsInvokeHandler {
    static final Capabilities.Capability NAME = new Capabilities.Capability("cics-ts.program", "1");
    static void validate(SpInput.CicsFact fact, EntryGobackAdmission.Context c) {
        var h=fact.header(); var seen=new HashSet<SpInput.OperandId>();
        c.require(fact.nameProfile().equals("cics-ts.program@1"),Rule.PROFILE_FACT,h.id().handle(),h.provenance(),"Supported CICS name profile required");
        CallAdmission.continuation(fact.ordinaryContinuation(),h,c);
        c.require(fact.localContinuation().statement().isEmpty()||fact.localContinuation().statement().equals(fact.ordinaryContinuation().statement()),Rule.PROFILE_FACT,h.id().handle(),h.provenance(),"CICS local and ordinary continuation agree within a paragraph");
        c.require(coherentConditions(fact),Rule.PROFILE_FACT,h.id().handle(),h.provenance(),"CICS conditions contradict typed options or gaps");
        fact.target().ifPresent(target->{
            if(target instanceof SpInput.DataCallTarget d)CallAdmission.reference(d.reference(),h,seen,c);
            else {
                var l=(SpInput.LiteralCallTarget)target;CallAdmission.operand(l.id(),h,seen,c);c.provenance(l.provenance());
                l.logicalValue().ifPresent(v->{CallAdmission.logical(v,h,c);c.require(v.value().equals(l.text()),Rule.PROFILE_FACT,l.id().handle(),l.provenance(),"Literal agrees with logical value");});
            }
        });
        for(var o:fact.options())o.reference().ifPresent(r->CallAdmission.reference(r,h,seen,c));
        for(var o:fact.options())c.require(o.start()>=0&&o.end()>=o.start()&&o.end()<=fact.rawText().length(),Rule.PROFILE_FACT,h.id().handle(),h.provenance(),"Option span inside preserved payload");
    }
    private static boolean coherentConditions(SpInput.CicsFact fact) {
        if(fact.conditions()==SpInput.CicsConditions.UNKNOWN)return true;
        boolean local=fact.options().stream().anyMatch(o->o.name().equals("RESP")||o.name().equals("NOHANDLE"));
        boolean shape=fact.options().stream().filter(o->o.name().equals("PROGRAM")).count()==1
            &&fact.options().stream().allMatch(o->Set.of("PROGRAM","COMMAREA","LENGTH","CHANNEL","RESP","RESP2","NOHANDLE","INPUTMSG","INPUTMSGLEN","SYSID","SYNCONRETURN","TRANSID","DATALENGTH").contains(o.name())
                &&((o.name().equals("NOHANDLE")||o.name().equals("SYNCONRETURN"))!=o.operand().isPresent())
                &&(fact.command()!=SpInput.CicsCommand.XCTL||!Set.of("SYSID","SYNCONRETURN","TRANSID","DATALENGTH").contains(o.name())));
        var allowed=new HashSet<>(Set.of("CICS_EFFECTS_SIGNATURE_PARTIAL","CICS_HOST_BINDING_UNAVAILABLE","CICS_TARGET_UNKNOWN"));
        if(fact.conditions()==SpInput.CicsConditions.LOCAL_CONDITION)allowed.add("CICS_CONDITION_VALUES_UNKNOWN");
        return shape&&allowed.containsAll(fact.gapCodes())&&(fact.conditions()==SpInput.CicsConditions.LOCAL_CONDITION?local:
            !local&&fact.options().stream().noneMatch(o->o.name().equals("RESP2")));
    }
    static Operations.Invoke translate(SpInput.CicsFact fact, ScalarDataTranslator.Result data,
            LabelId next, UnitId unit, LocalIds ids, SourceOrigins origins, List<LoweringResult.OperandLink> links,
            List<Evidence.CoverageItem> items,List<Evidence.Uncertainty> uncertainties) {
        var key=fact.header().id().handle();
        var operation=new OperationId(unit,ids.id("operation","cics-invoke",unit.localId(),key));
        var origin=origins.source("statement",key,fact.header().provenance());
        var scope=new Scopes.EntityScope(List.of(operation));
        var reason=new UncertaintyId(unit.publication(),ids.id("uncertainty","cics-contract",operation.localId(),key));
        uncertainties.add(new Evidence.Uncertainty(reason,"CONTRACT_UNKNOWN",List.of(Evidence.Dimension.CONTROL,Evidence.Dimension.EFFECTS,Evidence.Dimension.STORAGE,Evidence.Dimension.DEPENDENCIES),scope,
            "Signature/options and foreign effects remain partial; control follows source profile "+fact.conditions()+"; DEFAULT_ENTRY_PREFIX is conditional on the explicitly declared new CICS logical level and a complete canonical MOVE-only entry prefix.",origin));
        var policy=new Interactions.ExtensionName(NAME.name(),NAME.version());
        Interactions.Target target;
        if(fact.target().orElse(null) instanceof SpInput.LiteralCallTarget literal&&literal.logicalValue().isPresent()) {
            var targetOrigin=origins.source("cics-target",literal.id().handle(),literal.provenance());
            target=new Interactions.LiteralTarget("program","cics.program",literal.logicalValue().orElseThrow().value(),policy,targetOrigin);
        } else if(fact.target().orElse(null) instanceof SpInput.DataCallTarget d&&NominalTarget.available(d.reference(),data)) {
            var reference=d.reference();
            var targetOrigin=origins.source("cics-target",reference.id().handle(),reference.provenance());
            var placeId=new OperandId(new OperationOwner(operation),ids.id("operand","cics-target-place",operation.localId(),reference.id().handle()));
            var readId=new OperandId(new OperationOwner(operation),ids.id("operand","cics-target-read",operation.localId(),reference.id().handle()));
            var place=NominalTarget.place(reference,data,new Operand.Header(placeId,Operand.Role.VALUE_READ,targetOrigin),ids);
            if(!nameArea(reference,data)) {
                var gap=new UncertaintyId(unit.publication(),ids.id("uncertainty","cics-logical-target",operation.localId(),key));
                uncertainties.add(new Evidence.Uncertainty(gap,"cobol-lower:CICS_PHYSICAL_NAME_AREA_UNPROVEN",List.of(Evidence.Dimension.VALUES,Evidence.Dimension.DEPENDENCIES),scope,"Whole nominal target retained; physical name area unproved. Interpretation remains open.",targetOrigin));
            }
            target=new Interactions.ComputedTarget("program","cics.program",new Expressions.Read(new Operand.Header(readId,Operand.Role.CALL_TARGET,targetOrigin),place),policy,targetOrigin);
            links.add(new LoweringResult.OperandLink(reference.id(),readId,targetOrigin));links.add(new LoweringResult.OperandLink(reference.id(),placeId,targetOrigin));
            items.add(ScalarEvidence.item(unit.publication(),"operand",reference.id().handle(),targetOrigin,List.of(readId,placeId)));
        } else {
            var missing=new UncertaintyId(unit.publication(),ids.id("uncertainty","cics-name-area",operation.localId(),key));
            uncertainties.add(new Evidence.Uncertainty(missing,"cobol-lower:CICS_NAME_AREA_UNAVAILABLE",List.of(Evidence.Dimension.VALUES,Evidence.Dimension.DEPENDENCIES),scope,"Target absent or an exactly eight-byte IBM1047 physical name area is not proved; no padding or dynamic offset inference.",origin));
            var operand=new OperandId(new OperationOwner(operation),ids.id("operand","cics-name-unknown",operation.localId(),key));
            var unknown=new Expressions.Unknown(new Operand.Header(operand,Operand.Role.CALL_TARGET,origin),Types.known(Types.Builtin.TEXT),List.of(),new Scopes.WithinMemory(new Scopes.AllMemory(unit.publication(),true)),missing);
            target=new Interactions.ComputedTarget("program","cics.program",unknown,policy,origin);
        }
        var effectOperands=new ArrayList<Place>();
        for(var option:fact.options())option.reference().ifPresent(reference->{
            var selected=reference.binding().selected();
            if(reference.regionalAccess().isPresent()&&selected.filter(data.index()::containsKey).isPresent()) {
                var placeId=new OperandId(new OperationOwner(operation),ids.id("operand","cics-option-place",operation.localId(),reference.id().handle()));
                var optionOrigin=origins.source("cics-option",reference.id().handle(),reference.provenance());
                effectOperands.add(RegionalPlaces.place(reference,data.index().get(selected.orElseThrow()),new Operand.Header(placeId,reference.role()==SpInput.OperandRole.WRITE?Operand.Role.VALUE_WRITE:Operand.Role.VALUE_READ,optionOrigin),ids));
                links.add(new LoweringResult.OperandLink(reference.id(),placeId,optionOrigin));
                items.add(ScalarEvidence.item(unit.publication(),"operand",reference.id().handle(),optionOrigin,List.of(placeId)));
            }
        });
        var signature=new Interactions.ExternalSignature(new Interactions.Signature(new Interactions.ParameterInventory(List.of(),new Interactions.UnknownRemainder(reason)),new Interactions.ResultInventory(List.of(),new Interactions.UnknownRemainder(reason)),origin));
        var memory=new Scopes.WithinMemory(new Scopes.AllMemory(unit.publication(),true));
        var effects=new Interactions.EffectBound(new Interactions.ForeignEffects(memory,memory,List.of()),List.of());
        var external=new Scopes.UnitControl(unit,false,false,true,true,false,true);
        Scopes.ControlScope remainder=fact.conditions()==SpInput.CicsConditions.DEFAULT_ENTRY_PREFIX&&(fact.command()!=SpInput.CicsCommand.LINK||next!=null)?external:fact.conditions()==SpInput.CicsConditions.LOCAL_CONDITION&&next!=null
            ?new Scopes.ControlUnion(List.of(new Scopes.LabelsControl(List.of(next)),external))
            :new Scopes.UnitControl(unit,true,true,true,true,true,true);
        var outcomes=new Control.InvocationOutcomes(fact.command()==SpInput.CicsCommand.LINK&&next!=null?List.of(new Control.Normal(next)):List.of(),new Scopes.WithinControl(remainder));
        var open=new Evidence.Claim(scope,Evidence.PrecisionStatus.OPEN,List.of(reason));
        var precision=new Evidence.Precision(open,open,open,new Evidence.Claim(scope,Evidence.PrecisionStatus.NOT_APPLICABLE,List.of()),open);
        return new Operations.Invoke(new Operations.Header(operation,origin,Evidence.CoverageStatus.ABSTRACTED,precision,List.of(reason)),fact.command()==SpInput.CicsCommand.LINK?"call":"execute",target,List.of(),List.of(),signature,effectOperands,effects,outcomes,new Interactions.UnknownContract(reason));
    }
    private static boolean nameArea(SpInput.DataReference reference,ScalarDataTranslator.Result data) {
        if(reference.regionalAccess().isEmpty()||reference.binding().selected().isEmpty())return false;
        var selected=reference.binding().selected().orElseThrow();var view=data.views().get(selected);
        if(view==null||!data.index().containsKey(selected))return false;
        var range=RegionalPlaces.view(view,reference);
        return range.extent().equals(java.math.BigInteger.valueOf(8))&&range.codec().equals(RegionalStorageAdmission.IBM1047);
    }
}
