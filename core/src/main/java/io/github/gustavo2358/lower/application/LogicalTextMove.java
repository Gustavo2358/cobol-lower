package io.github.gustavo2358.lower.application;

import io.github.gustavo2358.air.model.*;
import io.github.gustavo2358.air.model.Ids.*;
import io.github.gustavo2358.lower.domain.SpInput;
import java.util.*;

/** Constant projection of one logical literal write, using source-owned character coordinates. */
final class LogicalTextMove {
    private LogicalTextMove() { }
    static List<Instruction> translate(SpInput.MoveFact move,LogicalTextIndex logical,ScalarDataTranslator.Result data,
            UnitId unit,LocalIds ids,SourceOrigins origins,List<LoweringResult.OperandLink> links,List<Evidence.CoverageItem> items) {
        var target=logical.byData.get(move.target().logicalWholeItem().orElseThrow());
        var text=((SpInput.LiteralSource)move.source()).logicalValue().orElseThrow().value().codePoints().toArray();
        var statement=origins.source("statement",move.header().id().handle(),move.header().provenance());
        var literal=origins.source("operand",move.source().id().handle(),move.source().provenance());
        var receiver=origins.source("operand",move.target().id().handle(),move.target().provenance());
        var result=new ArrayList<Instruction>();
        for(var leaf:logical.leaves(target)) {
            var node=logical.nodes.get(leaf.node());if(node.data().isEmpty())continue;
            var declaration=node.data().orElseThrow();var link=data.index().get(declaration);
            var operation=new OperationId(unit,ids.id("operation","logical-text-project",unit.localId(),move.header().id().handle()+"/"+leaf.node().handle()));
            var sourceId=new OperandId(new OperationOwner(operation),"logical-source");
            var targetId=new OperandId(new OperationOwner(operation),"logical-target");
            var layout=origins.source("logical-text-view",leaf.node().handle(),node.provenance());
            var origin=origins.derived(ids.id("origin","logical-text-project",unit.localId(),operation.localId()),List.of(statement,literal,receiver,layout),"logical-text@1/fit-parent-then-project-child");
            int start=leaf.start().subtract(target.start()).intValueExact(),length=leaf.length().intValueExact();
            var value=new StringBuilder();
            for(int i=0;i<length;i++)value.appendCodePoint(start+i<text.length?text[start+i]:' ');
            var place=new Places.ObjectPlace(new Operand.Header(targetId,Operand.Role.VALUE_WRITE,receiver),link.object());
            var expression=new Expressions.Literal(new Operand.Header(sourceId,Operand.Role.VALUE_READ,literal),new Values.TextValue(value.toString()));
            result.add(new Operations.Assign(new Operations.Header(operation,origin,Evidence.CoverageStatus.MODELED,ScalarEvidence.assign(operation),List.of()),place,expression));
            links.add(new LoweringResult.OperandLink(move.source().id(),sourceId,literal));
            links.add(new LoweringResult.OperandLink(move.target().id(),targetId,receiver));
            items.add(ScalarEvidence.item(unit.publication(),"operand",ids.sourceKey(move.source().id().handle())+"/"+leaf.node().handle(),literal,List.of(sourceId)));
            items.add(ScalarEvidence.item(unit.publication(),"operand",ids.sourceKey(move.target().id().handle())+"/"+leaf.node().handle(),receiver,List.of(targetId)));
        }
        return List.copyOf(result);
    }
}
