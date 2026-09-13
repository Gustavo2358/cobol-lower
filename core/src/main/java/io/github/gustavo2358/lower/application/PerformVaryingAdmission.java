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
        if(!p.gapCodes().isEmpty())return;
        need(p,c,v.levels()==1 && v.controls().size()==3,"closed single-variable profile");
        for(var role:VaryingOperandRole.values()) {
            var values=v.controls().stream().filter(o->o.level()==1 && o.role()==role).toList();
            need(p,c,values.size()==1,"exactly one occurrence for each varying role");
            if(values.size()!=1)continue;
            var o=values.getFirst();need(p,c,o.provenance().exact(),"exact varying operand origin");
            switch(role) {
                case CONTROL_VARIABLE -> need(p,c,o.integer().isEmpty() && o.references().size()==1
                    && integerReference(o.references().getFirst(),OperandRole.WRITE,c),"whole integer control item write");
                case FROM -> need(p,c,o.integer().isPresent() || o.references().size()==1
                    && integerReference(o.references().getFirst(),OperandRole.READ,c),"proved integer initial operand");
                case BY -> need(p,c,o.integer().filter(i->canonical(i)&&new java.math.BigInteger(i).signum()!=0).isPresent(),"nonzero integer literal increment");
            }
        }
    }
    private static void need(ProcedurePerformFact p,EntryGobackAdmission.Context c,boolean value,String detail) {
        c.require(value,Rule.PROFILE_FACT,p.header().id().handle(),p.header().provenance(),detail);
    }
}
