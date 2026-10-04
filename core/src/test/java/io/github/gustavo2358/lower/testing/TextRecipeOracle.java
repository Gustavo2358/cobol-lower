package io.github.gustavo2358.lower.testing;
import io.github.gustavo2358.air.model.*;
/** Test-only evaluation of literal fitting, keeping the existing handwritten expected strings. */
public final class TextRecipeOracle {
    private TextRecipeOracle() { }
    public static String value(Expression expression) {
        if(expression instanceof Expressions.Literal l&&l.value() instanceof Values.TextValue t)return t.value();
        if(expression instanceof Expressions.FitText f) {
            var points=value(f.value()).codePoints().toArray();var result=new StringBuilder();
            for(int i=0;i<f.length().intValueExact();i++)result.appendCodePoint(i<points.length?points[i]:f.pad().codePointAt(0));
            return result.toString();
        }
        throw new AssertionError("expected literal/fitting: "+expression);
    }
}
