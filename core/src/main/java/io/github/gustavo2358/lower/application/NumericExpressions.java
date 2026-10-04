package io.github.gustavo2358.lower.application;
import io.github.gustavo2358.air.model.*;
import io.github.gustavo2358.air.model.Ids.*;
import io.github.gustavo2358.lower.domain.SpInput.ScalarNumber;
import java.math.BigInteger;
/** Translate admitted descriptors into pure value operations, without interpreting source text. */
final class NumericExpressions {
    static Expression adjust(Expression value,ScalarNumber source,ScalarNumber target,OperandId root,
                             OperationId operation,OriginId origin,LocalIds ids) {
        java.util.function.Function<String,Operand.Header> h=key->new Operand.Header(
            new OperandId(root.owner(),ids.id("operand","numeric-"+key,operation.localId(),root.localId())),Operand.Role.VALUE_READ,origin);
        var result=new Operand.Header(root,Operand.Role.VALUE_READ,origin);
        if(source.scale()==0)value=new Expressions.Unary(h.apply("decimal-input"),Expressions.UnaryOperator.TO_DECIMAL,value);
        if(!(target.representation().equals("NATIVE_BINARY")||target.representation().equals("BINARY")&&target.trunc().equals("BIN"))) {
            value=new Expressions.FitDecimal(target.scale()==0?h.apply("fit"):result,value,
                BigInteger.valueOf(target.digits()),BigInteger.valueOf(target.scale()),!target.signed());
            return target.scale()==0?new Expressions.Unary(result,Expressions.UnaryOperator.TO_INT,value):value;
        }
        if(target.scale()!=0)value=new Expressions.Binary(h.apply("coefficient"),Expressions.BinaryOperator.MUL,value,powerTen(h.apply("scale-in"),target.scale()));
        value=new Expressions.Unary(h.apply("integral-coefficient"),Expressions.UnaryOperator.TO_INT,value);
        if(!target.signed())value=new Expressions.Unary(h.apply("magnitude"),Expressions.UnaryOperator.ABS,value);
        int width=target.digits()<=4?16:target.digits()<=9?32:64;
        value=new Expressions.WrapInteger(target.scale()==0?result:h.apply("wrap"),value,BigInteger.valueOf(width),target.signed());
        if(target.scale()==0)return value;
        value=new Expressions.Unary(h.apply("decimal-result"),Expressions.UnaryOperator.TO_DECIMAL,value);
        return new Expressions.Binary(result,Expressions.BinaryOperator.MUL,value,powerTen(h.apply("scale-out"),-target.scale()));
    }
    private static Expression powerTen(Operand.Header header,int exponent) {
        return new Expressions.Literal(header,new Values.DecimalValue(BigInteger.TEN.pow(Math.max(0,exponent)),BigInteger.valueOf(Math.max(0,-exponent))));
    }
}
