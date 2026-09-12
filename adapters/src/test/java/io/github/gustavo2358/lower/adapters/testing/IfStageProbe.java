package io.github.gustavo2358.lower.adapters.testing;

import io.github.gustavo2358.lower.adapters.sp.SpJsonDecoder;
import io.github.gustavo2358.lower.application.*;
import io.github.gustavo2358.lower.domain.SpInput;
import java.nio.file.*;

/** Independent development stage probes, not the final consumer oracle. */
public final class IfStageProbe {
    private IfStageProbe() { }
    public static void main(String[] args) throws Exception {
        var decoded=new SpJsonDecoder(new SpJsonDecoder.Limits(128)).decode(Files.readAllBytes(Path.of(args[1])));
        if(!(decoded instanceof SpJsonDecoder.Decoded d))throw new AssertionError("SP14 decoder unavailable: " + decoded);
        System.out.println("SP14 decoded: " + d.input().statements().stream().map(x -> x.getClass().getSimpleName()).toList());
        if(args[0].equals("materialization")) {
            if(d.input().statements().stream().noneMatch(SpInput.IfFact.class::isInstance))throw new AssertionError("IF_NOT_MATERIALIZED: OtherStatement(IF) survives strict Wire14");
        } else if(args[0].equals("linear-admission")) {
            if(d.input().statements().stream().noneMatch(SpInput.IfFact.class::isInstance))throw new AssertionError("probe prerequisite typed IF absent");
            var a=new CallAdmission().admit(d.input(),new AdmitInput.Limits(100));
            System.out.println("linear admission: " + a.status() + " " + a.diagnostics());
            if(a.status()!=Admission.Status.ADMITTED)throw new AssertionError("LINEAR_W1C_ADMISSION_REFUSES_TYPED_IF");
        } else if(args[0].equals("assembly")) {
            var a=new IfAdmission().admit(d.input(),new AdmitInput.Limits(100));
            System.out.println("IF admission: " + a.status() + " " + a.diagnostics());
            if(a.status()!=Admission.Status.ADMITTED)throw new AssertionError("probe prerequisite IF plan rejected");
            var r=new CobolLowerer().lower(d.input(),new LowerInput.Options(new AdmitInput.Limits(1000),32,io.github.gustavo2358.air.validation.ValidationOptions.defaults()));
            System.out.println("assembly: " + r.status());
            if(r.publication().isEmpty())throw new AssertionError("VALID_IF_PLAN_WITHOUT_DIAMOND_ASSEMBLER");
        } else if(args[0].equals("premise")) {
            var r=new CobolLowerer().lower(d.input(),new LowerInput.Options(new AdmitInput.Limits(1000),32,io.github.gustavo2358.air.validation.ValidationOptions.defaults()));
            if(r.status()!=LoweringResult.Status.SUCCESS)throw new AssertionError("probe prerequisite diamond rejected: " + r);
            var p=r.publication().orElseThrow();
            if(p.units().getFirst().sequences().stream().noneMatch(s -> s.terminator() instanceof io.github.gustavo2358.air.model.Operations.Branch))throw new AssertionError("probe prerequisite Branch absent");
            System.out.println("diamond sequences=" + p.units().getFirst().sequences().size() + " premises=" + p.premises().size());
            if(p.premises().isEmpty())throw new AssertionError("DIAMOND_WITHOUT_STORAGE_PREMISE");
        } else throw new IllegalArgumentException("unknown stage");
    }
}
