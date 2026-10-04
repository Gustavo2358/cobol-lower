package io.github.gustavo2358.lower.application;
import io.github.gustavo2358.air.model.*;
import io.github.gustavo2358.lower.domain.SpInput;
import java.math.BigInteger;
import java.util.*;
/** Translate and verify published formatting segments; never parse a source PICTURE. */
final class NumericFormatting {
    static List<DecimalText.Part> parts(SpInput.ScalarEdit edit) {
        return edit.parts().stream().map(p->new DecimalText.Part(DecimalText.Kind.valueOf(p.kind()),BigInteger.valueOf(p.count()),p.text(),p.negative())).toList();
    }
    static Expression literal(java.math.BigDecimal value,SpInput.ScalarEdit edit,Operand.Header header) {
        // A bounded representative has the same rendered digits and original sign.
        // This avoids expanding scientific literals such as 1E+1000000000.
        var n=new SpInput.ScalarNumber(edit.digits(),edit.scale(),true,"DISPLAY","UNSPECIFIED");
        var bounded=NumericMoveAdmission.fit(value,n);
        if(bounded.signum()==0&&value.signum()<0)bounded=new java.math.BigDecimal(BigInteger.ONE.negate(),edit.scale()+1);
        return new Expressions.Literal(header,new Values.DecimalValue(bounded.unscaledValue(),BigInteger.valueOf(bounded.scale())));
    }
    static Expression format(Expression value,SpInput.ScalarNumber source,SpInput.ScalarEdit edit,int length,
            java.util.function.Function<String,Operand.Header> header) {
        if(edit!=null) {
            if(source!=null&&source.scale()==0)value=new Expressions.Unary(header.apply("to-decimal"),Expressions.UnaryOperator.TO_DECIMAL,value);
            return new Expressions.FormatDecimal(header.apply("result"),value,parts(edit));
        }
        if(source==null||source.scale()>0)throw new IllegalArgumentException("integer source required for plain text");
        if(source.scale()!=0)value=new Expressions.Unary(header.apply("to-integer"),Expressions.UnaryOperator.TO_INT,value);
        int width=source.digits();
        if(source.representation().equals("NATIVE_BINARY"))width=width<=4?5:width<=9?10:source.signed()?19:20;
        value=new Expressions.IntegerDigits(header.apply("digits"),value,BigInteger.valueOf((long)width-source.scale()));
        return new Expressions.FitText(header.apply("result"),value,BigInteger.valueOf(length)," ");
    }
    static boolean valid(SpInput.DataFact data) {
        if(data.scalarEdit().isEmpty())return true;
        if(data.scalarNumber().isPresent()||data.scalarText().isEmpty()||data.coverage()!=SpInput.CoverageStatus.MODELED)return false;
        try {
            var edit=data.scalarEdit().orElseThrow();var shape=DecimalText.describe(parts(edit));
            return edit.digits()>0&&edit.digits()<=31&&edit.scale()>=0&&edit.scale()<=edit.digits()
                &&shape.digits().equals(BigInteger.valueOf(edit.digits()))&&shape.scale().equals(BigInteger.valueOf(edit.scale()))
                &&shape.extent().equals(BigInteger.valueOf(edit.extent()))&&edit.extent()==data.scalarText().orElseThrow().logicalExtent();
        } catch(IllegalArgumentException|NullPointerException invalid) {return false;}
    }
}
