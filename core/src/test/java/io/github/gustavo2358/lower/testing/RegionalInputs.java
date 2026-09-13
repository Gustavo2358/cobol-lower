package io.github.gustavo2358.lower.testing;

import io.github.gustavo2358.lower.domain.SpInput;
import io.github.gustavo2358.lower.domain.StorageFacts;
import java.math.BigInteger;
import java.util.*;
import static io.github.gustavo2358.lower.domain.SpInput.*;
import static io.github.gustavo2358.lower.domain.StorageFacts.*;

/** Independent memory-port facts. No JSON, COBOL parser, layout engine or codec is called. */
public final class RegionalInputs {
    private RegionalInputs() { }
    public static Measure known(int n) { return new Measure(Optional.of(BigInteger.valueOf(n)),List.of()); }
    public static SpInput group(int children) {
        var base=CallInputs.create(children+1,1,8,"PROGA",false);var unit=base.unit();
        var data=base.dataDeclarations().stream().map(d->IfInputs.with(d,"scalarText",Optional.empty())).toList();
        var nodes=new ArrayList<Node>();var views=new ArrayList<View>();
        var root=new NodeId(unit,"storage-node:100");var allocation=new BaseId(unit,"storage-base:999");
        for(int i=0;i<data.size();i++) {
            var id=i==0?root:new NodeId(unit,"storage-node:"+(100+i));var extent=known(i==0?8*children:8);
            nodes.add(new Node(id,i==0?Optional.empty():Optional.of(root),i==0?0:i-1,false,i==0?Kind.GROUP:Kind.ELEMENTARY,
                Optional.of(data.get(i).id()),extent,data.get(i).provenance()));
            views.add(new View(id,allocation,known(i==0?0:8*(i-1)),extent,Optional.of(CODEC),data.get(i).provenance()));
        }
        var inventory=new Inventory(Profile.IBM_ENTERPRISE_6_4_FIXED_DISPLAY_1047,Optional.of(PROFILE_ID),Optional.of(CODEC),nodes,
            List.of(new Base(allocation,known(8*children),Allocation.INDEPENDENT_LOCAL_WORKING_STORAGE,data.getFirst().provenance())),views,List.of());
        var old=(MoveFact)base.statements().getFirst();var text="PGM00001".repeat(children);var bytes=new ArrayList<Integer>();
        for(int i=0;i<children;i++)bytes.addAll(List.of(215,199,212,240,240,240,240,241));
        var literal=new LiteralSource(old.source().id(),LiteralKind.ALPHANUMERIC,Optional.of(new LogicalValue(LogicalDomain.TEXT,text,text.length())),old.source().provenance());
        var target=new DataReference(old.target().id(),OperandRole.WRITE,old.target().binding(),Optional.empty(),old.target().provenance(),Optional.of(new Access(root)));
        var move=new MoveFact(old.header(),literal,target,CopySemantics.UNAVAILABLE,old.normalContinuation(),Optional.empty(),Optional.of(new Move(MoveKind.LITERAL_BYTES,bytes,List.of())));
        var call=(CallFact)base.statements().get(1);var read=((DataCallTarget)call.target()).reference();var selected=data.getLast().id();
        var reference=new DataReference(read.id(),OperandRole.CALL_TARGET,new Binding(ResolutionStatus.RESOLVED,List.of(selected),Optional.of(selected)),Optional.empty(),read.provenance(),Optional.of(new Access(nodes.getLast().id())));
        var statements=List.<StatementFact>of(move,CallInputs.target(call,new DataCallTarget(reference)),base.statements().getLast());
        return new SpInput(unit,base.policy(),data,statements,base.structure(),base.gaps(),base.coverage(),base.entryInventory(),Optional.empty(),true,Optional.of(inventory));
    }
}
