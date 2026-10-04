package io.github.gustavo2358.lower.application;
import io.github.gustavo2358.lower.domain.*;
import java.math.BigDecimal;
import java.util.*;
import static io.github.gustavo2358.lower.domain.SpInput.*;
import io.github.gustavo2358.lower.application.Admission.Rule;
/** Validate published certificates; never infer a COBOL numeric conversion. */
final class IntegerMoveAdmission {
    static void validate(MoveFact move,EntryGobackAdmission.Context c) {
        var sources=new HashMap<OperandId,MoveSource>();var targets=new HashMap<OperandId,DataReference>();
        targets.put(move.target().id(),move.target());sources.put(move.target().id(),move.source());
        for(var t:move.additionalTransfers()){targets.put(t.target().id(),t.target());sources.put(t.target().id(),t.source());}
        for(var source:sources.values())if(source instanceof LiteralSource l)
            c.require(l.numericValue().isEmpty()||l.kind()==LiteralKind.NUMERIC&&l.logicalValue().isEmpty(),Rule.PROFILE_FACT,move.header().id().handle(),l.provenance(),"Numeric source must have numeric domain only");
        if(move.integerTransfers().isEmpty())return;
        boolean valid=move.copySemantics()==CopySemantics.UNAVAILABLE&&move.header().provenance().exact();
        var order=new ArrayList<OperandId>();order.add(move.target().id());move.additionalTransfers().forEach(t->order.add(t.target().id()));
        int ordinal=0;var seen=new HashSet<OperandId>();
        for(var proof:move.integerTransfers()) {
            c.touch();var target=targets.get(proof.target());var source=sources.get(proof.target());
            int digits=digits(target,OperandRole.WRITE,c);
            boolean supported=seen.add(proof.target())&&digits>0;
            if(source instanceof LiteralSource l&&move.source() instanceof LiteralSource first) {
                supported&=l.provenance().exact()&&l.kind()==LiteralKind.NUMERIC&&l.numericValue().isPresent()
                    &&l.numericValue().equals(first.numericValue())&&proof.value().isPresent();
                if(supported) {var v=proof.value().orElseThrow();supported=v.signum()>=0&&v.toString().length()<=digits
                    &&l.numericValue().orElseThrow().compareTo(new BigDecimal(v))==0;}
            } else if(source instanceof DataReference r&&move.source() instanceof DataReference first) {
                int input=digits(r,OperandRole.READ,c);
                supported&=proof.value().isEmpty()&&input>0&&input<=digits&&r.wholeItemAccess().equals(first.wholeItemAccess())
                    &&ordinal<order.size()&&proof.target().equals(order.get(ordinal++));
            } else supported=false;
            valid&=supported;
        }
        c.require(valid,Rule.PROFILE_FACT,move.header().id().handle(),move.header().provenance(),"Integer MOVE requires distinct whole receivers and exact fitting numeric values or non-narrowing DATA");
    }
    private static int digits(DataReference r,OperandRole role,EntryGobackAdmission.Context c) {
        if(r==null||r.role()!=role||!r.provenance().exact()||r.wholeItemAccess().isEmpty()
            ||r.binding().status()!=ResolutionStatus.RESOLVED||r.binding().candidates().size()!=1
            ||!r.binding().selected().equals(r.wholeItemAccess().map(WholeItemAccess::data)))return 0;
        var data=c.data.get(r.wholeItemAccess().orElseThrow().data());
        if(data==null||!data.provenance().exact()||data.scalarText().isPresent())return 0;
        if(c.regionalStorage!=null&&c.regionalStorage.facts()!=null) {
            var view=c.regionalStorage.byData().get(data.id());var facts=c.regionalStorage.facts();
            if(view==null||!facts.cells.containsKey(view.node().handle())||!facts.integers.contains(view.node().handle()))return 0;
        }
        return data.scalarInteger().map(ScalarInteger::digits).filter(d->d<=31).orElse(0);
    }
}
