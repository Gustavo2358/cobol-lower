package io.github.gustavo2358.lower.application;

import java.util.*;
import static io.github.gustavo2358.lower.domain.SpInput.*;
import static io.github.gustavo2358.lower.application.Admission.*;

/** Published integer operand admission; no COBOL spelling or runtime value inference. */
final class PerformCountAdmission {
    static boolean integer(DataFact d) { return d.scalarInteger().filter(n->n.digits()>0).isPresent()&&d.scalarText().isEmpty()&&d.provenance().exact()&&d.coverage()==CoverageStatus.MODELED; }
    static void validate(ProcedurePerformFact p,PerformCount count,EntryGobackAdmission.Context c) {
        c.provenance(count.provenance());
        need(p,c,count.integer().isEmpty()||count.reference().isEmpty(),"one count operand");
        count.reference().ifPresent(r->CallAdmission.reference(r,p.header(),new HashSet<>(),c));
        if(count.profile()==PerformCountProfile.POSITIVE_INTEGER) {
            boolean positive=false;
            try {positive=count.integer().filter(i->new java.math.BigInteger(i).signum()>0&&new java.math.BigInteger(i).toString().equals(i)).isPresent();}
            catch(NumberFormatException ignored) { }
            need(p,c,positive&&count.reference().isEmpty()&&count.provenance().exact(),"canonical positive integer count");
        } else if(count.profile()==PerformCountProfile.INTEGER_ITEM) {
            need(p,c,count.integer().isEmpty()&&count.reference().isPresent()&&count.provenance().exact(),"integer count reference");
            count.reference().ifPresent(r->need(p,c,r.role()==OperandRole.READ&&r.provenance().exact()
                &&r.wholeItemAccess().filter(w->c.data(w.data())!=null&&integer(c.data(w.data()))).isPresent(),"count reads a proved complete integer item"));
        }
    }
    private static void need(ProcedurePerformFact p,EntryGobackAdmission.Context c,boolean value,String detail) {
        c.require(value,Rule.PROFILE_FACT,p.header().id().handle(),p.header().provenance(),detail);
    }
}
