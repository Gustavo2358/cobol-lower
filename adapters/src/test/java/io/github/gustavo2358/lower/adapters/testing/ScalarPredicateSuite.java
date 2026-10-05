package io.github.gustavo2358.lower.adapters.testing;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.node.*;
import io.github.gustavo2358.lower.adapters.sp.SpJsonDecoder;
import io.github.gustavo2358.lower.adapters.cli.CobolLower;
import io.github.gustavo2358.lower.application.*;
import io.github.gustavo2358.air.json.AirJson;
import io.github.gustavo2358.air.model.*;
import java.util.*;

/** Independent assertions on source family, executable operand closure and conservative boundaries. */
public final class ScalarPredicateSuite {
    static final ObjectMapper JSON=new ObjectMapper();
    static final SpJsonDecoder DECODER=new SpJsonDecoder(CobolLower.INPUT_LIMITS);
    static ObjectNode source(String name)throws Exception{return (ObjectNode)JSON.readTree(ScalarPredicateSuite.class.getResourceAsStream("/sp/scalar-predicates/"+name+".json"));}
    static LoweringResult lower(ObjectNode source)throws Exception {
        var decoded=DECODER.decode(JSON.writeValueAsBytes(source));
        if(!(decoded instanceof SpJsonDecoder.Decoded d))throw new AssertionError(decoded);
        var result=new CobolLowerer().lower(d.input(),CobolLower.POSITIVE_OPTIONS);
        if(result.publication().isEmpty()||!result.validation().orElseThrow().isStructurallyValid())throw new AssertionError(result);
        var air=result.publication().orElseThrow();var codec=new AirJson();
        if(!air.equals(codec.decode(codec.encode(air))))throw new AssertionError("codec changed predicates");
        return result;
    }
    static List<Expression> predicates(LoweringResult r){return r.publication().orElseThrow().units().stream().flatMap(u->u.sequences().stream()).map(Sequence::terminator).filter(Operations.Branch.class::isInstance).map(Operations.Branch.class::cast).map(Operations.Branch::predicate).toList();}
    static List<Operand> operands(Expression e){var out=new ArrayList<Operand>();var todo=new ArrayDeque<Operand>();todo.add(e);while(!todo.isEmpty()){var o=todo.removeFirst();out.add(o);Operands.children(o).forEach(todo::add);}return out;}
    static void reject(ObjectNode bad)throws Exception{var d=DECODER.decode(JSON.writeValueAsBytes(bad));if(d instanceof SpJsonDecoder.Decoded)throw new AssertionError("invalid predicate accepted");}
    public static void main(String[] args)throws Exception {
        var shared=source("shared");var r=lower(shared);var all=predicates(r).stream().flatMap(e->operands(e).stream()).toList();
        for(var op:List.of(Expressions.BinaryOperator.GT,Expressions.BinaryOperator.GE,Expressions.BinaryOperator.NE,Expressions.BinaryOperator.AND))
            if(all.stream().noneMatch(o->o instanceof Expressions.Binary b&&b.operator()==op))throw new AssertionError("lost operator "+op);
        if(all.stream().noneMatch(o->o instanceof Expressions.Read))throw new AssertionError("lost predicate read");
        if(all.stream().noneMatch(o->o instanceof Expressions.FitText))throw new AssertionError("text comparison did not pad");
        var indexed=lower(source("addresses"));var indexedTerms=indexed.publication().orElseThrow().units().stream().flatMap(u->u.sequences().stream()).map(Sequence::terminator).filter(Operations.Opaque.class::isInstance).map(Operations.Opaque.class::cast).toList();
        if(indexedTerms.stream().noneMatch(o->o.observedKind().equals("PREDICATE_NOT_EXECUTABLE")&&o.envelope().memory().otherReads() instanceof Scopes.WithinMemory
            &&o.knownOperands().stream().anyMatch(p->p instanceof Expressions.Read indexRead&&indexRead.header().role()==Operand.Role.ADDRESS_READ)
            &&o.envelope().control().known().size()==2))throw new AssertionError("unproven address lost index reads, bounded content or control alternatives");
        var untyped=lower(source("untyped"));
        if(untyped.publication().orElseThrow().uncertainties().stream().noneMatch(u->u.code().equals("TYPE_UNKNOWN")))throw new AssertionError("open reference domain lost normative TYPE_UNKNOWN reason");
        var missing=lower(source("missing"));
        if(missing.publication().orElseThrow().units().stream().flatMap(u->u.sequences().stream()).map(Sequence::terminator).noneMatch(t->t instanceof Operations.Opaque o&&o.observedKind().equals("PREDICATE_NOT_EXECUTABLE")&&o.envelope().memory().otherReads() instanceof Scopes.WithinMemory&&o.envelope().memory().otherWrites()==Scopes.NoMemory.INSTANCE&&o.envelope().dependencies().remainder()==Scopes.NoResources.INSTANCE))throw new AssertionError("open read fabricated writes or dependency effects");
        var ordered=lower(source("ordering"));
        if(predicates(ordered).stream().noneMatch(e->e instanceof Expressions.Unknown u&&u.dependencies().size()==2))throw new AssertionError("invented collation");
        if(ordered.publication().orElseThrow().uncertainties().stream().noneMatch(u->u.code().equals("PREDICATE_COLLATION_UNKNOWN")))throw new AssertionError("collation reason lost");
        var old=shared.deepCopy();old.put("contractVersion","2.66.0");reject(old);
        var owner=shared.deepCopy();((ObjectNode)owner.path("conditionNames").path("predicates").get(0).path("tree").path("children").get(0).path("children").get(0)).put("use","operand:999:0");reject(owner);
        var root=shared.deepCopy();((ObjectNode)root.path("conditionNames").path("predicates").get(0).path("tree")).put("kind","READ");reject(root);
        System.out.println("SCALAR_PREDICATES=PASS relations, mixed88, shared contexts, fitting, addresses, collation, codec, closure");
    }
}
