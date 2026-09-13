package io.github.gustavo2358.lower.application;

import io.github.gustavo2358.air.model.*;
import io.github.gustavo2358.air.model.Ids.*;
import io.github.gustavo2358.lower.domain.SpInput;
import java.util.*;

/** A neutral source interaction, without runtime target resolution or value analysis. */
final class InvokeHandler {
    static Operations.Invoke translate(SpInput.CallFact call, Map<SpInput.DataId, LoweringResult.DataLink> data,
            LabelId normal, OriginId continuation, UnitId unit, LocalIds ids, SourceOrigins origins, List<LoweringResult.OperandLink> links,
            List<Evidence.CoverageItem> items, List<Evidence.Uncertainty> uncertainties) {
        var key = call.header().id().handle();
        var operation = new OperationId(unit, ids.id("operation", "call-invoke", unit.localId(), key));
        var origin = origins.source("statement", key, call.header().provenance());
        var targetOrigin = origins.source("call-target", call.target().id().handle(), call.target().provenance());
        var scope = new Scopes.EntityScope(List.of(operation));
        var runtime = uncertainty("RESOURCE_TARGET_UNKNOWN", "Runtime target remains unresolved; no preceding-MOVE analysis.",
            List.of(Evidence.Dimension.DEPENDENCIES), operation, targetOrigin, ids, uncertainties);
        var name = uncertainty("cobol-lower:RUNTIME_NAME_POLICY_UNKNOWN", "Runtime program-name interpretation is not established by source text.",
            List.of(Evidence.Dimension.DEPENDENCIES), operation, targetOrigin, ids, uncertainties);
        var effects = uncertainty("EFFECT_UNKNOWN", "SP publishes UNKNOWN external effects; all-memory bounds are may-read/may-write.",
            List.of(Evidence.Dimension.EFFECTS, Evidence.Dimension.STORAGE), operation, origin, ids, uncertainties);
        var outcomes = uncertainty("CONTROL_UNKNOWN", "Only the published normal continuation is retained; missing and other invocation outcomes remain OPEN.",
            List.of(Evidence.Dimension.CONTROL), operation, continuation, ids, uncertainties);
        var contract = uncertainty("CONTRACT_UNKNOWN", "External contract authority is not certified.",
            List.of(Evidence.Dimension.CONTROL, Evidence.Dimension.EFFECTS, Evidence.Dimension.DEPENDENCIES), operation, origin, ids, uncertainties);
        var namePolicy = new Interactions.UnknownName(name);
        Interactions.Target target;
        if (call.target() instanceof SpInput.LiteralCallTarget literal && literal.logicalValue().isPresent()) {
            target = new Interactions.LiteralTarget("program", "cobol.program", literal.logicalValue().orElseThrow().value(), namePolicy, targetOrigin);
        } else if(call.target() instanceof SpInput.DataCallTarget d && d.reference().wholeItemAccess().filter(w->data.containsKey(w.data())).isPresent()) {
            var reference = d.reference();
            var object = data.get(reference.wholeItemAccess().orElseThrow().data()).object();
            var owner = new OperationOwner(operation);
            var readId = new OperandId(owner, ids.id("operand", "call-name-read", operation.localId(), reference.id().handle()));
            var placeId = new OperandId(owner, ids.id("operand", "call-name-place", operation.localId(), reference.id().handle()));
            var readOrigin = origins.derived(ids.id("origin", "call-read", operation.localId(), reference.id().handle()),
                List.of(targetOrigin), "cp6-call@1/target-name-read");
            var placeOrigin = origins.derived(ids.id("origin", "call-place", operation.localId(), reference.id().handle()),
                List.of(targetOrigin, data.get(reference.wholeItemAccess().orElseThrow().data()).origin()), "cp6-call@1/published-whole-item-access");
            var place = new Places.ObjectPlace(new Operand.Header(placeId, Operand.Role.VALUE_READ, placeOrigin), object);
            var read = new Expressions.Read(new Operand.Header(readId, Operand.Role.CALL_TARGET, readOrigin), place);
            target = new Interactions.ComputedTarget("program", "cobol.program", read, namePolicy, targetOrigin);
            links.add(new LoweringResult.OperandLink(reference.id(), readId, readOrigin));
            links.add(new LoweringResult.OperandLink(reference.id(), placeId, placeOrigin));
            items.add(ScalarEvidence.item(unit.publication(), "operand", reference.id().handle(), targetOrigin, List.of(readId, placeId)));
        } else {
            var valueReason=uncertainty("CALL_NAME_VALUE_UNAVAILABLE","The published CALL site has an unavailable program-name value.",
                List.of(Evidence.Dimension.VALUES,Evidence.Dimension.DEPENDENCIES),operation,targetOrigin,ids,uncertainties);
            var operand=new OperandId(new OperationOwner(operation),ids.id("operand","call-name-unknown",operation.localId(),call.target().id().handle()));
            var unknown=new Expressions.Unknown(new Operand.Header(operand,Operand.Role.CALL_TARGET,targetOrigin),Types.known(Types.Builtin.TEXT),
                List.of(),new Scopes.WithinMemory(new Scopes.AllMemory(unit.publication(),true)),valueReason);
            target=new Interactions.ComputedTarget("program","cobol.program",unknown,namePolicy,targetOrigin);
            links.add(new LoweringResult.OperandLink(call.target().id(),operand,targetOrigin));
            items.add(ScalarEvidence.item(unit.publication(),"operand",call.target().id().handle(),targetOrigin,List.of(operand)));
        }
        var signatureOrigin = origins.derived(ids.id("origin", "call-signature", operation.localId(), key),
            List.of(origin), "cp6-call@1/published-surface");
        var signature = new Interactions.ExternalSignature(new Interactions.Signature(
            new Interactions.ParameterInventory(List.of(), call.surface().using() == SpInput.ClausePresence.ABSENT ? Interactions.NoRemainder.INSTANCE : new Interactions.UnknownRemainder(contract)),
            new Interactions.ResultInventory(List.of(), call.surface().returning() == SpInput.ClausePresence.ABSENT ? Interactions.NoRemainder.INSTANCE : new Interactions.UnknownRemainder(contract)), signatureOrigin));
        var memory = new Scopes.WithinMemory(new Scopes.AllMemory(unit.publication(), true));
        var bounds = new Interactions.EffectBound(new Interactions.ForeignEffects(memory, memory, List.of()), List.of());
        var alternatives = new Control.InvocationOutcomes(normal == null ? List.of() : List.of(new Control.Normal(normal)),
            new Scopes.WithinControl(new Scopes.AllControl(unit.publication())));
        var precision = new Evidence.Precision(
            new Evidence.Claim(scope, Evidence.PrecisionStatus.OPEN, List.of(outcomes)),
            new Evidence.Claim(scope, Evidence.PrecisionStatus.OPEN, List.of(effects)),
            new Evidence.Claim(scope, Evidence.PrecisionStatus.OPEN, List.of(effects, contract)),
            new Evidence.Claim(scope, Evidence.PrecisionStatus.NOT_APPLICABLE, List.of()),
            new Evidence.Claim(scope, Evidence.PrecisionStatus.OPEN, List.of(runtime, name, contract)));
        var header = new Operations.Header(operation, origin, Evidence.CoverageStatus.ABSTRACTED, precision,
            List.of(runtime, name, effects, outcomes, contract));
        return new Operations.Invoke(header, "call", target, List.of(), List.of(), signature, List.of(), bounds, alternatives,
            new Interactions.UnknownContract(contract));
    }
    private static UncertaintyId uncertainty(String code, String reason, List<Evidence.Dimension> dimensions,
            OperationId operation, OriginId origin, LocalIds ids, List<Evidence.Uncertainty> uncertainties) {
        var id = new UncertaintyId(operation.unit().publication(), ids.id("uncertainty", code, operation.localId(), "call"));
        uncertainties.add(new Evidence.Uncertainty(id, code, dimensions, new Scopes.EntityScope(List.of(operation)), reason, origin));
        return id;
    }
}
