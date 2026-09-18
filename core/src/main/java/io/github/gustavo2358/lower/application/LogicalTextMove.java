package io.github.gustavo2358.lower.application;

import io.github.gustavo2358.air.model.*;
import io.github.gustavo2358.air.model.Ids.*;
import io.github.gustavo2358.lower.domain.SpInput;
import java.math.BigInteger;
import java.util.*;

/** Source-proved character views composed by ordinary AIR expressions, never byte effects. */
final class LogicalTextMove {
    private LogicalTextMove() { }
    private static final class ExpressionsFor {
        final OperationId operation;final OriginId origin;int next;
        ExpressionsFor(OperationId operation,OriginId origin){this.operation=operation;this.origin=origin;}
        Operand.Header h(){return new Operand.Header(new OperandId(new OperationOwner(operation),"logical-"+(next++)),Operand.Role.VALUE_READ,origin);}
        Expression literal(String value){return new Expressions.Literal(h(),new Values.TextValue(value));}
        Expression read(ObjectId object){return new Expressions.Read(h(),new Places.ObjectPlace(h(),object));}
        Expression integer(BigInteger value){return new Expressions.Literal(h(),new Values.IntValue(value));}
        Expression slice(Expression value,BigInteger start,BigInteger length){return new Expressions.SliceText(h(),fit(value,start.add(length)),integer(start),integer(length));}
        Expression fit(Expression value,BigInteger length){return new Expressions.FitText(h(),value,length," ");}
        Expression concat(Expression left,Expression right){return new Expressions.Binary(h(),Expressions.BinaryOperator.CONCAT,left,right);}
    }
    static List<Instruction> initial(LogicalTextIndex logical,ScalarDataTranslator.Result data,UnitId unit,LocalIds ids,SourceOrigins origins) {
        var result=new ArrayList<Instruction>();
        for(var root:logical.views.values().stream().filter(v->v.node().equals(v.root())).sorted(Comparator.comparing(v->v.node().handle())).toList()) {
            if(logical.nodes.get(root.node()).kind()!=io.github.gustavo2358.lower.domain.StorageFacts.Kind.GROUP)continue;
            var rootObject=data.index().get(logical.nodes.get(root.node()).data().orElseThrow()).object();
            var id=new OperationId(unit,ids.id("operation","logical-initial-root",unit.localId(),root.node().handle()));
            var origin=origins.source("logical-initial-root",root.node().handle(),logical.nodes.get(root.node()).provenance());var f=new ExpressionsFor(id,origin);
            Expression value=null;BigInteger cursor=BigInteger.ZERO;
            for(var leaf:logical.leaves(root)) {
                var end=LogicalTextIndex.end(leaf);if(end.compareTo(cursor)<=0)continue;
                var start=leaf.start().max(cursor);var length=end.subtract(start);
                var node=logical.nodes.get(leaf.node());
                Expression part=node.data().filter(data.index()::containsKey).map(d->f.fit(f.read(data.index().get(d).object()),leaf.length()))
                    .orElseGet(()->f.slice(f.read(rootObject),leaf.start(),leaf.length()));
                // An overlapping longer view still contributes its uncovered tail.
                // Omitting it and fitting the shorter concatenation would invent spaces.
                if(start.compareTo(leaf.start())>0)part=f.slice(part,start.subtract(leaf.start()),length);
                value=value==null?part:f.concat(value,part);cursor=end;
            }
            if(value==null)continue;
            var place=new Places.ObjectPlace(new Operand.Header(new OperandId(new OperationOwner(id),"destination"),Operand.Role.VALUE_WRITE,origin),rootObject);
            result.add(new Operations.Assign(new Operations.Header(id,origin,Evidence.CoverageStatus.MODELED,ScalarEvidence.assign(id),List.of()),place,f.fit(value,root.length())));
        }
        return List.copyOf(result);
    }
    static List<Instruction> translate(SpInput.MoveFact move,LogicalTextIndex logical,ScalarDataTranslator.Result data,
            UnitId unit,LocalIds ids,SourceOrigins origins,List<LoweringResult.OperandLink> links,List<Evidence.CoverageItem> items) {
        var target=logical.byData.get(move.target().logicalWholeItem().orElseThrow());var root=logical.views.get(target.root());
        var rootData=logical.nodes.get(root.node()).data().orElseThrow();var rootObject=data.index().get(rootData).object();
        var statement=origins.source("statement",move.header().id().handle(),move.header().provenance());
        var sourceOrigin=origins.source("operand",move.source().id().handle(),move.source().provenance());
        var targetOrigin=origins.source("operand",move.target().id().handle(),move.target().provenance());
        var operation=new OperationId(unit,ids.id("operation","logical-text-copy",unit.localId(),move.header().id().handle()));
        var origin=origins.derived(ids.id("origin","logical-text-copy",unit.localId(),operation.localId()),List.of(statement,sourceOrigin,targetOrigin),"logical-text@2/capture-fit-update-root");
        var f=new ExpressionsFor(operation,origin);Expression source;
        if(move.source() instanceof SpInput.LiteralSource literal)source=f.literal(literal.logicalValue().orElseThrow().value());
        else source=f.read(data.index().get(((SpInput.DataReference)move.source()).logicalWholeItem().orElseThrow()).object());
        Expression value=f.fit(source,target.length());
        if(target.start().signum()>0)value=f.concat(f.slice(f.read(rootObject),BigInteger.ZERO,target.start()),value);
        var end=LogicalTextIndex.end(target);
        if(end.compareTo(root.length())<0)value=f.concat(value,f.slice(f.read(rootObject),end,root.length().subtract(end)));
        value=f.fit(value,root.length());
        var place=new Places.ObjectPlace(new Operand.Header(new OperandId(new OperationOwner(operation),"destination"),Operand.Role.VALUE_WRITE,targetOrigin),rootObject);
        var result=new ArrayList<Instruction>();
        result.add(new Operations.Assign(new Operations.Header(operation,origin,Evidence.CoverageStatus.MODELED,ScalarEvidence.assign(operation),List.of()),place,value));
        links.add(new LoweringResult.OperandLink(move.source().id(),value.header().id(),sourceOrigin));links.add(new LoweringResult.OperandLink(move.target().id(),place.header().id(),targetOrigin));
        for(var view:logical.family(target)) {
            var node=logical.nodes.get(view.node());if(node.data().isEmpty()||view.node().equals(root.node()))continue;
            var object=data.index().get(node.data().orElseThrow());if(object==null)continue;
            var id=new OperationId(unit,ids.id("operation","logical-text-project",unit.localId(),move.header().id().handle()+"/"+view.node().handle()));
            var e=new ExpressionsFor(id,origin);
            var dest=new Places.ObjectPlace(new Operand.Header(new OperandId(new OperationOwner(id),"destination"),Operand.Role.VALUE_WRITE,targetOrigin),object.object());
            result.add(new Operations.Assign(new Operations.Header(id,origin,Evidence.CoverageStatus.MODELED,ScalarEvidence.assign(id),List.of()),dest,e.slice(e.read(rootObject),view.start(),view.length())));
        }
        return List.copyOf(result);
    }
}
