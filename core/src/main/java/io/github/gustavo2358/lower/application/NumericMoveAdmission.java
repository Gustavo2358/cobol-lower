package io.github.gustavo2358.lower.application;
import io.github.gustavo2358.lower.domain.*;
import java.math.BigDecimal;
import java.util.*;
import static io.github.gustavo2358.lower.domain.SpInput.*;
import io.github.gustavo2358.lower.application.Admission.Rule;
/** Validate published certificates; never infer a COBOL numeric conversion. */
final class NumericMoveAdmission {
    static boolean numeric(DataFact d) { return d.scalarNumber().filter(EntryGobackAdmission::validNumber).isPresent()
        &&d.scalarText().isEmpty()&&d.coverage()==CoverageStatus.MODELED; }

    static void validate(MoveFact move,EntryGobackAdmission.Context c) {
        var sources=new HashMap<OperandId,MoveSource>();var targets=new HashMap<OperandId,DataReference>();
        targets.put(move.target().id(),move.target());sources.put(move.target().id(),move.source());
        for(var t:move.additionalTransfers()){targets.put(t.target().id(),t.target());sources.put(t.target().id(),t.source());}
        for(var source:sources.values())if(source instanceof LiteralSource l) {
            c.require(!l.kind().collatingFill()||l.logicalValue().isEmpty()&&l.numericValue().isEmpty(),
                Rule.PROFILE_FACT,move.header().id().handle(),l.provenance(),"Collating figuratives do not certify a concrete logical or numeric value");
            boolean zero=l.kind()==LiteralKind.FIGURATIVE_ZERO;
            c.require(zero?l.numericValue().filter(n->n.signum()==0).isPresent()
                &&l.logicalValue().filter(v->v.value().equals("0")&&v.logicalExtent()==1).isPresent()
                :l.numericValue().isEmpty()||l.kind()==LiteralKind.NUMERIC&&(l.logicalValue().isEmpty()||integerText(l)),
                Rule.PROFILE_FACT,move.header().id().handle(),l.provenance(),"Numeric source has a numeric domain; figurative ZERO has the canonical zero and text seed");
        }
        if(move.numericTransfers().isEmpty())return;
        boolean valid=move.copySemantics()==CopySemantics.UNAVAILABLE&&move.header().provenance().exact();
        var order=new ArrayList<OperandId>();order.add(move.target().id());move.additionalTransfers().forEach(t->order.add(t.target().id()));
        int ordinal=0;var seen=new HashSet<OperandId>();
        for(var proof:move.numericTransfers()) {
            c.touch();var target=targets.get(proof.target());var source=sources.get(proof.target());
            var number=number(target,OperandRole.WRITE,c);
            var text=text(target,OperandRole.WRITE,c);
            boolean supported=seen.add(proof.target())&&(number!=null||text!=null);
            if(source instanceof LiteralSource l&&move.source() instanceof LiteralSource first) {
                supported&=l.provenance().exact()&&literalNumber(l).isPresent()
                    &&l.kind()==first.kind()&&literalNumber(l).equals(literalNumber(first));
                if(supported) supported=number!=null
                    ?literalResult(literalNumber(l).orElseThrow(),number,proof.value())
                    :text.scalarEdit().isPresent()&&l.numericValue().isPresent()
                        &&proof.value().filter(v->literalNumber(l).orElseThrow().compareTo(v)==0).isPresent();
            } else if(source instanceof DataReference r&&move.source() instanceof DataReference first) {
                var input=number(r,OperandRole.READ,c);
                var inputText=text(r,OperandRole.READ,c);
                supported&=proof.value().isEmpty()&&(input!=null||inputText!=null&&inputText.scalarEdit().isEmpty()
                    &&c.input.controlTopology().isPresent()&&number!=null)&&r.wholeItemAccess().equals(first.wholeItemAccess())
                    &&ordinal<order.size()&&proof.target().equals(order.get(ordinal++))
                    &&(exactCell(r,c)&&exactCell(target,c)||c.input.controlTopology().isPresent()&&disjoint(r,target,c));
                if(supported&&input!=null&&number==null)supported=text.scalarEdit().isPresent()||input.scale()<=0;
            } else supported=false;
            valid&=supported;
        }
        c.require(valid,Rule.PROFILE_FACT,move.header().id().handle(),move.header().provenance(),"Numeric MOVE requires typed whole receivers and values satisfying the receiving adjustment");
    }
    private static DataFact text(DataReference reference,OperandRole role,EntryGobackAdmission.Context c) {
        if(reference==null||reference.role()!=role||!reference.provenance().exact()||reference.wholeItemAccess().isEmpty()
                ||reference.binding().status()!=ResolutionStatus.RESOLVED||reference.binding().candidates().size()!=1
                ||!reference.binding().selected().equals(reference.wholeItemAccess().map(WholeItemAccess::data)))return null;
        var declaration=c.data.get(reference.wholeItemAccess().orElseThrow().data());
        return declaration!=null&&CallAdmission.scalar(declaration)&&declaration.coverage()==CoverageStatus.MODELED
            &&declaration.scalarNumber().isEmpty()&&NumericFormatting.valid(declaration)?declaration:null;
    }
    static boolean integerText(LiteralSource l) {
        if(l.numericValue().isEmpty()||l.logicalValue().isEmpty()||l.numericValue().orElseThrow().scale()!=0)return false;
        var text=l.logicalValue().orElseThrow().value();if(text.isEmpty())return false;
        for(int i=0;i<text.length();i++)if(text.charAt(i)<'0'||text.charAt(i)>'9')return false;
        return l.numericValue().orElseThrow().abs().compareTo(new BigDecimal(text))==0;
    }
    private static Optional<BigDecimal> literalNumber(LiteralSource literal) {
        if(literal.kind()==LiteralKind.NUMERIC||literal.kind()==LiteralKind.FIGURATIVE_ZERO)return literal.numericValue();
        if(literal.kind()!=LiteralKind.ALPHANUMERIC||literal.logicalValue().isEmpty())return Optional.empty();
        var text=literal.logicalValue().orElseThrow().value();if(text.isEmpty())return Optional.empty();
        for(int i=0;i<text.length();i++)if(text.charAt(i)<'0'||text.charAt(i)>'9')return Optional.empty();
        return Optional.of(new BigDecimal(text));
    }
    private static boolean literalResult(BigDecimal source,ScalarNumber number,Optional<BigDecimal> result) {
        boolean common=!requiresFit(number)||fits(source,number);
        return common?result.filter(v->fit(source,number).compareTo(v)==0).isPresent():result.isEmpty();
    }
    static boolean requiresFit(ScalarNumber number) {
        return number.representation().equals("BINARY")&&(number.trunc().equals("UNSPECIFIED")||number.trunc().equals("OPT"));
    }
    static boolean fits(BigDecimal value,ScalarNumber target) {
        return value.signum()==0||(long)value.precision()+target.scale()-value.scale()<=target.digits();
    }
    static BigDecimal maximum(ScalarNumber source) {
        java.math.BigInteger coefficient;
        if(source.representation().equals("BINARY")||source.representation().equals("NATIVE_BINARY")) {
            int width=source.digits()<=4?16:source.digits()<=9?32:64;
            coefficient=java.math.BigInteger.ONE.shiftLeft(width-(source.signed()?1:0));
            if(!source.signed())coefficient=coefficient.subtract(java.math.BigInteger.ONE);
        } else coefficient=java.math.BigInteger.TEN.pow(source.digits()).subtract(java.math.BigInteger.ONE);
        return new BigDecimal(coefficient,source.scale());
    }
    static BigDecimal fit(BigDecimal value,ScalarNumber n) {
        boolean nativeBinary=n.representation().equals("NATIVE_BINARY")||n.representation().equals("BINARY")&&n.trunc().equals("BIN");
        int bits=n.digits()<=4?16:n.digits()<=9?32:64;
        var modulus=nativeBinary?java.math.BigInteger.ONE.shiftLeft(bits):java.math.BigInteger.TEN.pow(n.digits());
        long shift=(long)n.scale()-value.scale();var coefficient=value.unscaledValue();
        if(shift>=0)coefficient=coefficient.remainder(modulus)
            .multiply(java.math.BigInteger.TEN.modPow(java.math.BigInteger.valueOf(shift),modulus)).remainder(modulus);
        else coefficient=-shift>=value.precision()?java.math.BigInteger.ZERO:
            coefficient.divide(java.math.BigInteger.TEN.pow((int)-shift)).remainder(modulus);
        if(!n.signed())coefficient=coefficient.abs();
        if(nativeBinary) {
            coefficient=coefficient.mod(modulus);
            if(n.signed()&&coefficient.testBit(bits-1))coefficient=coefficient.subtract(modulus);
        }
        return new BigDecimal(coefficient,n.scale());
    }
    static boolean exactCell(DataReference reference,EntryGobackAdmission.Context c) {
        if(reference==null||reference.wholeItemAccess().isEmpty())return false;
        if(c.regionalStorage==null||c.regionalStorage.facts()==null)return true;
        var view=c.regionalStorage.byData().get(reference.wholeItemAccess().orElseThrow().data());
        return view!=null&&c.regionalStorage.facts().cells.containsKey(view.node().handle());
    }
    private static Set<String> bounds(DataReference reference,EntryGobackAdmission.Context c) {
        if(reference==null||reference.wholeItemAccess().isEmpty()||c.regionalStorage==null||c.regionalStorage.facts()==null)return Set.of();
        var view=c.regionalStorage.byData().get(reference.wholeItemAccess().orElseThrow().data());if(view==null)return Set.of();
        var facts=c.regionalStorage.facts();var binding=facts.bindings.get(view.node().handle());
        if(binding==null||!facts.available(binding.dependencies()))return Set.of();
        var result=new HashSet<>(binding.cells());result.addAll(binding.regions());return result;
    }
    static boolean disjoint(DataReference source,DataReference target,EntryGobackAdmission.Context c) {
        var a=bounds(source,c);var b=bounds(target,c);return !a.isEmpty()&&!b.isEmpty()&&Collections.disjoint(a,b);
    }
    static ScalarNumber number(DataReference r,OperandRole role,EntryGobackAdmission.Context c) {
        if(r==null||r.role()!=role||!r.provenance().exact()||r.wholeItemAccess().isEmpty()
            ||r.binding().status()!=ResolutionStatus.RESOLVED||r.binding().candidates().size()!=1
            ||!r.binding().selected().equals(r.wholeItemAccess().map(WholeItemAccess::data)))return null;
        var data=c.data.get(r.wholeItemAccess().orElseThrow().data());
        if(data==null||data.scalarText().isPresent())return null;
        if(c.regionalStorage!=null&&c.regionalStorage.facts()!=null) {
            var view=c.regionalStorage.byData().get(data.id());var facts=c.regionalStorage.facts();
            if(view==null||!facts.materializable(view.node().handle())||!facts.integers.contains(view.node().handle()))return null;
        }
        return data.scalarNumber().filter(EntryGobackAdmission::validNumber).orElse(null);
    }
}
