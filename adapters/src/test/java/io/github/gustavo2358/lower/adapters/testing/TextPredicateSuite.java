package io.github.gustavo2358.lower.adapters.testing;
import io.github.gustavo2358.lower.adapters.sp.SpJsonDecoder;
import io.github.gustavo2358.lower.adapters.cli.CobolLower;
import io.github.gustavo2358.lower.application.*;
import io.github.gustavo2358.air.model.*;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.node.*;
public final class TextPredicateSuite {
    public static void main(String[] args)throws Exception {
        var json=new ObjectMapper();var tree=(ObjectNode)json.readTree(TextPredicateSuite.class.getResourceAsStream("/sp/recall-locality/text-predicate.json"));
        var decoder=new SpJsonDecoder(CobolLower.INPUT_LIMITS);var decoded=decoder.decode(json.writeValueAsBytes(tree));
        if(!(decoded instanceof SpJsonDecoder.Decoded d))throw new AssertionError(decoded);
        var result=new CobolLowerer().lower(d.input(),CobolLower.OPTIONS);
        if(result.publication().isEmpty()||!result.validation().orElseThrow().isStructurallyValid())throw new AssertionError(result.status()+" "+result.admission().diagnostics());
        var predicate=result.publication().orElseThrow().units().stream().flatMap(u->u.sequences().stream()).map(Sequence::terminator)
            .filter(Operations.Branch.class::isInstance).map(Operations.Branch.class::cast).findFirst().orElseThrow().predicate();
        if(!(predicate instanceof Expressions.Binary b)||b.operator()!=Expressions.BinaryOperator.OR||!(b.left() instanceof Expressions.Binary a)
            ||a.operator()!=Expressions.BinaryOperator.AND||!(a.right() instanceof Expressions.Unknown)||!(b.right() instanceof Expressions.Binary eq)
            ||eq.operator()!=Expressions.BinaryOperator.EQ)throw new AssertionError(predicate);
        var exact=tree.deepCopy();
        var condition=(ObjectNode)exact.path("statements").get(0).path("condition");
        condition.set("textPredicate",condition.path("textPredicate").path("children").get(1).deepCopy());
        var exactDecoded=decoder.decode(json.writeValueAsBytes(exact));
        if(!(exactDecoded instanceof SpJsonDecoder.Decoded ed))throw new AssertionError(exactDecoded);
        var exactResult=new CobolLowerer().lower(ed.input(),CobolLower.OPTIONS);
        if(exactResult.publication().isEmpty()||!exactResult.validation().orElseThrow().isStructurallyValid())throw new AssertionError("exact predicate lost coverage integrity: "+exactResult.status());
        var exactBranch=exactResult.publication().orElseThrow().units().stream().flatMap(u->u.sequences().stream()).map(Sequence::terminator).filter(Operations.Branch.class::isInstance).findFirst().orElseThrow();
        if(exactBranch.header().coverage()!=Evidence.CoverageStatus.MODELED||!exactBranch.header().uncertainties().isEmpty())throw new AssertionError("exact predicate must have modeled coverage");
        var bad=tree.deepCopy();bad.put("contractVersion","2.45.0");
        if(!(decoder.decode(json.writeValueAsBytes(bad)) instanceof SpJsonDecoder.Rejected))throw new AssertionError("new predicate requires SP2.46");
        bad=tree.deepCopy();((ObjectNode)bad.path("statements").get(0).path("condition").path("textPredicate").path("children").get(0)).put("reference","operand:1:9999");
        var forged=decoder.decode(json.writeValueAsBytes(bad));
        if(forged instanceof SpJsonDecoder.Decoded f&&new CobolLowerer().lower(f.input(),CobolLower.OPTIONS).publication().isPresent())throw new AssertionError("unknown predicate reference admitted");
        System.out.println("TEXT_PREDICATE=PASS");
    }
}
