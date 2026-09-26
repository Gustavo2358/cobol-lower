package io.github.gustavo2358.lower.application;

import io.github.gustavo2358.air.model.Scopes;
import io.github.gustavo2358.air.model.Ids.PublicationId;
import io.github.gustavo2358.lower.domain.SpInput;
import java.util.*;

/** Exposure alone cannot be translated into a memory write remainder. */
public final class OpaqueExposureSuite {
    private OpaqueExposureSuite() { }
    private static SpInput.OtherStatement fact(SpInput.EffectBound write) {
        var unit=new SpInput.UnitKey("w8",List.of(),"W8");
        var loc=new SpInput.Location("w8.cbl",1,1,1,1);
        var provenance=new SpInput.Provenance(loc,loc,List.of(),true);
        var readiness=new SpInput.Readiness(
            new SpInput.ReadinessClaim(SpInput.ReadinessStatus.PARTIAL,"effect"),
            new SpInput.ReadinessClaim(SpInput.ReadinessStatus.SUFFICIENT,"control"),
            new SpInput.ReadinessClaim(SpInput.ReadinessStatus.PARTIAL,"effect"));
        var header=new SpInput.StatementHeader(new SpInput.StatementId(unit,"statement:0"),0,
            new SpInput.Containment(Optional.empty(),SpInput.Branch.ROOT),provenance,SpInput.CoverageStatus.PARTIAL,readiness);
        var effects=new SpInput.EffectSummary(List.of(),List.of(),List.of(),List.of(),
            SpInput.EffectBound.NONE,write,SpInput.EffectBound.ALL,
            SpInput.EnvironmentEffect.NONE,SpInput.EffectValueTransform.NONE,SpInput.EffectProof.NO_OP);
        return new SpInput.OtherStatement(header,SpInput.Variant.OBSERVED,"EXPOSURE_ONLY",Optional.of("source"),"EXPOSURE_GAP",
            new SpInput.NormalContinuation(SpInput.ContinuationAvailability.NONE,Optional.empty(),provenance),
            List.of(),Optional.of(effects));
    }
    public static void main(String[] args) {
        var known=new OpaqueOperands.Known(List.of(),List.of(),List.of(),List.of());
        var publication=new PublicationId("w8");
        var exposure=OpaqueOperands.memory(fact(SpInput.EffectBound.NONE),known,publication);
        if(!(exposure.otherWrites() instanceof Scopes.NoMemory)||!(exposure.otherReads() instanceof Scopes.NoMemory))
            throw new AssertionError("unknown exposure must not invent unknown write/read");
        var write=OpaqueOperands.memory(fact(SpInput.EffectBound.ALL),known,publication);
        if(!(write.otherWrites() instanceof Scopes.WithinMemory within&&within.scope() instanceof Scopes.AllMemory))
            throw new AssertionError("actual unknown write bound must remain effective");
        System.out.println("OPAQUE_EXPOSURE=PASS exposure and write dimensions independent");
    }
}
