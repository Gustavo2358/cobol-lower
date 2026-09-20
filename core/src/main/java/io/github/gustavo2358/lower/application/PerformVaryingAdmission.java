package io.github.gustavo2358.lower.application;

import java.util.*;
import static io.github.gustavo2358.lower.domain.SpInput.*;
import static io.github.gustavo2358.lower.application.Admission.*;

/** SP2.5 single integer item, exact initialization/update footprint, no numeric evaluation. */
final class PerformVaryingAdmission {
    static boolean canonical(String value) {
        try { return new java.math.BigInteger(value).toString().equals(value); }
        catch(NumberFormatException ignored) { return false; }
    }
    static boolean integerReference(DataReference r,OperandRole role,EntryGobackAdmission.Context c) {
        return r.role()==role && r.provenance().exact() && r.binding().status()==ResolutionStatus.RESOLVED
            && r.binding().candidates().size()==1 && r.wholeItemAccess().filter(w->c.data(w.data())!=null
                && PerformCountAdmission.integer(c.data(w.data())) && r.binding().selected().equals(Optional.of(w.data()))).isPresent();
    }
    static boolean executable(ProcedurePerformFact p,EntryGobackAdmission.Context c) {
        if(p.varying().isEmpty())return true;
        var v=p.varying().orElseThrow();
        if(v.levels()!=1||v.controls().size()!=3)return false;
        var byRole=new EnumMap<VaryingOperandRole,VaryingOperand>(VaryingOperandRole.class);
        for(var o:v.controls())if(o.level()!=1||byRole.putIfAbsent(o.role(),o)!=null)return false;
        if(byRole.size()!=3)return false;
        var variable=byRole.get(VaryingOperandRole.CONTROL_VARIABLE);
        var from=byRole.get(VaryingOperandRole.FROM);
        var by=byRole.get(VaryingOperandRole.BY);
        return variable.provenance().exact()&&variable.integer().isEmpty()&&variable.references().size()==1
            &&integerReference(variable.references().getFirst(),OperandRole.WRITE,c)
            &&from.provenance().exact()&&(from.integer().filter(PerformVaryingAdmission::canonical).isPresent()
                ||from.references().size()==1&&integerReference(from.references().getFirst(),OperandRole.READ,c))
            &&by.provenance().exact()&&by.integer().filter(i->canonical(i)&&new java.math.BigInteger(i).signum()!=0).isPresent();
    }
    static void validate(ProcedurePerformFact p,PerformVarying v,Set<OperandId> seen,EntryGobackAdmission.Context c) {
        need(p,c,p.loop().isPresent() && p.times().isEmpty() && v.levels()>0,"VARYING has a loop and positive typed level count");
        var roles=new HashSet<String>();
        for(var o:v.controls()) {
            c.provenance(o.provenance());
            need(p,c,o.level()>0 && o.level()<=v.levels() && roles.add(o.level()+"/"+o.role()),"unique varying role at a declared level");
            need(p,c,o.integer().isEmpty() || o.references().isEmpty(),"literal and reference cannot describe the same varying operand");
            o.integer().ifPresent(i->need(p,c,canonical(i),"canonical integer operand"));
            for(var r:o.references()) {
                CallAdmission.reference(r,p.header(),seen,c);
                need(p,c,r.role()==OperandRole.READ || o.role()==VaryingOperandRole.CONTROL_VARIABLE && r.role()==OperandRole.WRITE,"typed varying operand role");
            }
        }
    }
    private static void need(ProcedurePerformFact p,EntryGobackAdmission.Context c,boolean value,String detail) {
        c.require(value,Rule.PROFILE_FACT,p.header().id().handle(),p.header().provenance(),detail);
    }
}
