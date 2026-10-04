package io.github.gustavo2358.lower.application;

import io.github.gustavo2358.lower.domain.*;
import io.github.gustavo2358.air.model.*;
import io.github.gustavo2358.air.model.Ids.*;
import java.math.BigInteger;
import java.util.*;

/** Translation of SP condition-name facts. No COBOL parsing, binding or range enumeration. */
final class ConditionNameLowerer {
    private final SpInput input;
    private final ScalarDataTranslator.Result data;
    private final Map<String,ConditionNames.Definition> definitions=new HashMap<>();
    private final Map<String,ConditionNames.Use> uses=new HashMap<>();
    private final Map<String,ConditionNames.Tree> predicates=new HashMap<>();
    private final Map<String,List<ConditionNames.Assignment>> assignments=new HashMap<>();
    private final Map<String,SpInput.DataReference> references=new HashMap<>();
    private final Map<String,SpInput.StatementFact> statements=new HashMap<>();
    private final Set<String> completeSets;
    private final List<LoweringResult.OperandLink> links;
    private final Map<ObjectId,Memory.ObjectDeclaration> objects=new HashMap<>();
    ConditionNameLowerer(SpInput input,ScalarDataTranslator.Result data,List<LoweringResult.OperandLink> links) {
        this.input=input;this.data=data;this.links=links;
        this.completeSets=input.conditionNames().map(f->ConditionNameContract.completeSets(f,input.statements())).orElse(Set.of());
        input.statements().forEach(f->{statements.put(f.header().id().handle(),f);ConditionNameContract.references(f).forEach(r->references.put(r.id().handle(),r));});
        input.conditionNames().ifPresent(f->{f.definitions().forEach(d->definitions.put(d.id(),d));f.uses().forEach(u->uses.put(u.id(),u));
            f.predicates().forEach(p->predicates.put(key(p.statement(),p.role()),p.tree()));
            f.assignments().forEach(a->assignments.computeIfAbsent(a.statement(),k->new ArrayList<>()).add(a));});
        data.objects().forEach(o->objects.put(o.id(),o));
    }
    private static String key(String statement,String role){return statement+"/"+role;}
    boolean has(String statement,String role){return predicates.containsKey(key(statement,role));}
    Optional<Expression> predicate(String statement,String role,OperationId operation,LocalIds ids,
            SourceOrigins origins,List<Evidence.Uncertainty> uncertainties) {
        var tree=predicates.get(key(statement,role));if(tree==null)return Optional.empty();
        return Optional.of(new Builder(operation,ids,origins,uncertainties,statements.get(statement).header().provenance()).tree(tree));
    }
    Operations.Branch branch(SpInput.StatementFact fact,String role,LabelId whenTrue,LabelId whenFalse,
            UnitId unit,LocalIds ids,SourceOrigins origins,List<Evidence.Uncertainty> uncertainties) {
        var operation=new OperationId(unit,ids.id("operation","condition-name-branch",unit.localId(),key(fact.header().id().handle(),role)));
        int before=uncertainties.size();var value=predicate(fact.header().id().handle(),role,operation,ids,origins,uncertainties).orElseThrow();
        var reasons=uncertainties.subList(before,uncertainties.size()).stream().map(Evidence.Uncertainty::id).toList();
        var origin=origins.source("condition-name-decision",fact.header().id().handle(),fact.header().provenance());
        var exact=new Evidence.Claim(new Scopes.EntityScope(List.of(operation)),Evidence.PrecisionStatus.EXACT,List.of());
        var values=new Evidence.Claim(new Scopes.EntityScope(List.of(value.header().id())),reasons.isEmpty()?Evidence.PrecisionStatus.EXACT:Evidence.PrecisionStatus.OPEN,reasons);
        return new Operations.Branch(new Operations.Header(operation,origin,reasons.isEmpty()?Evidence.CoverageStatus.MODELED:Evidence.CoverageStatus.ABSTRACTED,
            new Evidence.Precision(exact,exact,exact,values,exact),reasons),value,whenTrue,whenFalse);
    }
    boolean hasSet(String statement){return completeSets.contains(statement);}
    List<Instruction> set(SpInput.StatementFact statement,LogicalTextIndex logical,UnitId unit,LocalIds ids,
            SourceOrigins origins,List<Evidence.Uncertainty> uncertainties) {
        var result=new ArrayList<Instruction>();
        for(var assignment:assignments.get(statement.header().id().handle())) {
            var use=uses.get(assignment.use());var definition=definitions.get(use.definition());var reference=references.get(use.operand());
            var origin=origins.derived(ids.id("origin","condition-set-value",statement.header().id().handle(),Integer.toString(assignment.ordinal())),
                List.of(origins.source("condition-name-use",use.id(),use.provenance()),origins.source("condition-name-definition",definition.id(),definition.provenance())),"condition-name@1/first-or-declared-false-value");
            var value=assignment.value();var dataId=new SpInput.DataId(input.unit(),definition.parent());
            String text=value.kind()==ConditionNames.ValueKind.TEXT?value.value():value.kind()==ConditionNames.ValueKind.SPACES?"":null;
            if(use.indices().isEmpty()&&text!=null&&logical.byData.containsKey(dataId)) {
                result.addAll(LogicalTextMove.literal(statement,reference,text,assignment.ordinal(),logical,data,unit,ids,origins,links,origin));continue;
            }
            var op=new OperationId(unit,ids.id("operation","condition-set",statement.header().id().handle(),Integer.toString(assignment.ordinal())));
            int before=uncertainties.size();var builder=new Builder(op,ids,origins,uncertainties,statement.header().provenance());
            var link=data.index().get(dataId);var object=link==null?null:objects.get(link.object());
            var type=definition.domain()==ConditionNames.VariableDomain.INTEGER?Types.Builtin.INT:Types.Builtin.TEXT;
            if (!use.indices().isEmpty() || object == null || definition.domain() == ConditionNames.VariableDomain.UNKNOWN
                    || !(object.typeRef() instanceof Types.Known known) || known.type() != type) {
                // The source assignment is known, but an unlocated receiving item does not
                // authorize an AIR exact write or a known-domain open Choice.
                var reason = new UncertaintyId(unit.publication(), ids.id("uncertainty", "condition-set-location", op.localId(), use.id()));
                var scope = new Scopes.EntityScope(List.of(op));
                uncertainties.add(new Evidence.Uncertainty(reason, "SET_RECEIVER_STORAGE_UNAVAILABLE",
                        List.of(Evidence.Dimension.STORAGE, Evidence.Dimension.VALUES), scope,
                        "The ordered source SET value is retained; its executable receiving location is unavailable.", origin));
                var nominal = data.nominal().get(dataId);
                Scopes.MemoryScope bound = nominal == null ? new Scopes.AllMemory(unit.publication(), true)
                        : new Scopes.ObjectsMemory(List.of(nominal));
                var exact = new Evidence.Claim(scope, Evidence.PrecisionStatus.EXACT, List.of());
                var open = new Evidence.Claim(scope, Evidence.PrecisionStatus.OPEN, List.of(reason));
                result.add(new Operations.HavocMay(new Operations.Header(op, origin, Evidence.CoverageStatus.ABSTRACTED,
                        new Evidence.Precision(exact, open, open, open, exact), List.of(reason)), bound, reason));
                continue;
            }
            Expression source;
            if(type==Types.Builtin.INT&&(value.kind()==ConditionNames.ValueKind.NUMBER||value.kind()==ConditionNames.ValueKind.ZERO)) {
                try{source=new Expressions.Literal(builder.h(origin,Operand.Role.VALUE_READ),new Values.IntValue(value.kind()==ConditionNames.ValueKind.ZERO?BigInteger.ZERO:new java.math.BigDecimal(value.value()).toBigIntegerExact()));}
                catch(ArithmeticException e){source=builder.unknown(type,"SET_NUMERIC_REPRESENTATION_UNAVAILABLE",origin,List.of());}
            } else if(text!=null&&type==Types.Builtin.TEXT) {
                source=new Expressions.Literal(builder.h(origin,Operand.Role.VALUE_READ),new Values.TextValue(text));
                var extent=data.logicalTextExtents().get(dataId);
                source=extent==null?builder.unknown(type,"SET_RECEIVER_EXTENT_UNAVAILABLE",origin,List.of(source)):
                    new Expressions.FitText(builder.h(origin,Operand.Role.VALUE_READ),source,BigInteger.valueOf(extent)," ");
            } else source=builder.unknown(type,"SET_VALUE_REPRESENTATION_UNAVAILABLE",origin,List.of());
            Place destination = new Places.ObjectPlace(builder.h(origin, Operand.Role.VALUE_WRITE), object.id());
            if(reference!=null)links.add(new LoweringResult.OperandLink(reference.id(),destination.header().id(),origin));
            var reasons=uncertainties.subList(before,uncertainties.size()).stream().map(Evidence.Uncertainty::id).toList();
            var exact=new Evidence.Claim(new Scopes.EntityScope(List.of(op)),Evidence.PrecisionStatus.EXACT,List.of());
            var open=new Evidence.Claim(new Scopes.EntityScope(List.of(op)),reasons.isEmpty()?Evidence.PrecisionStatus.EXACT:Evidence.PrecisionStatus.OPEN,reasons);
            result.add(new Operations.Assign(new Operations.Header(op,origin,reasons.isEmpty()?Evidence.CoverageStatus.MODELED:Evidence.CoverageStatus.ABSTRACTED,new Evidence.Precision(exact,open,open,open,exact),reasons),destination,source));
        }
        return List.copyOf(result);
    }
    private final class Builder {
        final OperationId operation;final LocalIds ids;final SourceOrigins origins;final List<Evidence.Uncertainty> uncertainties;final SpInput.Provenance source;int ordinal;
        Builder(OperationId operation,LocalIds ids,SourceOrigins origins,List<Evidence.Uncertainty> uncertainties,SpInput.Provenance source){this.operation=operation;this.ids=ids;this.origins=origins;this.uncertainties=uncertainties;this.source=source;}
        Operand.Header h(OriginId origin,Operand.Role role){return new Operand.Header(new OperandId(new OperationOwner(operation),"condition-name:"+(ordinal++)),role,origin);}
        Expression unknown(Types.Builtin type,String code,OriginId origin,List<Expression> dependencies) {
            var header=h(origin,type==Types.Builtin.BOOL?Operand.Role.PREDICATE:Operand.Role.VALUE_READ);
            var reason=new UncertaintyId(operation.publication(),ids.id("uncertainty","condition-name",operation.localId(),header.id().localId()));
            uncertainties.add(new Evidence.Uncertainty(reason,code,List.of(Evidence.Dimension.VALUES),new Scopes.EntityScope(List.of(header.id())),"The source condition is modeled; this input lacks the runtime value, access or collation proof.",origin));
            return new Expressions.Unknown(header,Types.known(type),dependencies,new Scopes.WithinMemory(new Scopes.AllMemory(operation.publication(),true)),reason);
        }
        Expression tree(ConditionNames.Tree root) {
            record Pending(ConditionNames.Tree tree,boolean finish) { }
            var todo=new ArrayDeque<Pending>();var built=new IdentityHashMap<ConditionNames.Tree,Expression>();todo.push(new Pending(root,false));
            var origin=origins.source("condition-name-expression",operation.localId(),source);
            while(!todo.isEmpty()) {
                var next=todo.pop();var tree=next.tree();
                if(!next.finish()&&!tree.children().isEmpty()){todo.push(new Pending(tree,true));for(int i=tree.children().size()-1;i>=0;i--)todo.push(new Pending(tree.children().get(i),false));continue;}
                Expression result=switch(tree.kind()) {
                    case "TEST" -> membership(uses.get(tree.use()));
                    case "UNKNOWN" -> unknown(Types.Builtin.BOOL,"CONDITION_OTHER_EXPRESSION_UNAVAILABLE",origin,List.of());
                    case "NOT" -> new Expressions.Unary(h(origin,Operand.Role.PREDICATE),Expressions.UnaryOperator.NOT,built.get(tree.children().getFirst()));
                    default -> join(tree.kind().equals("AND")?Expressions.BinaryOperator.AND:Expressions.BinaryOperator.OR,tree.children().stream().map(built::get).toList(),origin);
                };
                built.put(tree,result);
            }
            return built.get(root);
        }
        Expression membership(ConditionNames.Use use) {
            var definition=definitions.get(use.definition());
            var origin=origins.derived(ids.id("origin","condition-name-membership",operation.localId(),use.id()),
                List.of(origins.source("condition-name-use",use.id(),use.provenance()),origins.source("condition-name-definition",definition.id(),definition.provenance())),"SP2.64/condition-variable-membership");
            var alternatives=new ArrayList<Expression>();
            for(var range:definition.ranges()) {
                if(range.last().isEmpty())alternatives.add(compare(use,definition,range.first(),Expressions.BinaryOperator.EQ,origin));
                else alternatives.add(join(Expressions.BinaryOperator.AND,List.of(
                    compare(use,definition,range.first(),Expressions.BinaryOperator.GE,origin),compare(use,definition,range.last().get(),Expressions.BinaryOperator.LE,origin)),origin));
            }
            return join(Expressions.BinaryOperator.OR,alternatives,origin);
        }
        Expression compare(ConditionNames.Use use,ConditionNames.Definition definition,ConditionNames.Value value,Expressions.BinaryOperator operator,OriginId origin) {
            var dataId=new SpInput.DataId(input.unit(),definition.parent());var link=data.index().get(dataId);
            var object=link==null?null:objects.get(link.object());var type=definition.domain()==ConditionNames.VariableDomain.INTEGER?Types.Builtin.INT:Types.Builtin.TEXT;
            Expression left;
            if(use.indices().isEmpty()&&object!=null&&object.typeRef() instanceof Types.Known known&&known.type()==type)
                left=new Expressions.Read(h(origin,Operand.Role.VALUE_READ),new Places.ObjectPlace(h(origin,Operand.Role.VALUE_READ),link.object()));
            else left=unknown(type,"CONDITION_VARIABLE_ACCESS_UNAVAILABLE",origin,List.of());
            if(!use.operand().isEmpty())links.add(new LoweringResult.OperandLink(references.get(use.operand()).id(),left.header().id(),origin));
            Expression right;
            if(type==Types.Builtin.INT&&(value.kind()==ConditionNames.ValueKind.NUMBER||value.kind()==ConditionNames.ValueKind.ZERO)) {
                try{right=new Expressions.Literal(h(origin,Operand.Role.VALUE_READ),new Values.IntValue(value.kind()==ConditionNames.ValueKind.ZERO?BigInteger.ZERO:new java.math.BigDecimal(value.value()).toBigIntegerExact()));}
                catch(ArithmeticException e){return unknown(Types.Builtin.BOOL,"CONDITION_NUMERIC_DOMAIN_UNAVAILABLE",origin,List.of(left));}
            } else if(type==Types.Builtin.TEXT&&(value.kind()==ConditionNames.ValueKind.TEXT||value.kind()==ConditionNames.ValueKind.SPACES)) {
                right=new Expressions.Literal(h(origin,Operand.Role.VALUE_READ),new Values.TextValue(value.kind()==ConditionNames.ValueKind.SPACES?"":value.value()));
                var extent=data.logicalTextExtents().get(dataId);
                if(extent==null||operator!=Expressions.BinaryOperator.EQ)return unknown(Types.Builtin.BOOL,"CONDITION_TEXT_COMPARISON_PROFILE_UNAVAILABLE",origin,List.of(left,right));
                var length=BigInteger.valueOf(Math.max(extent,value.kind()==ConditionNames.ValueKind.SPACES?0:value.value().codePointCount(0,value.value().length())));
                left=new Expressions.FitText(h(origin,Operand.Role.VALUE_READ),left,length," ");right=new Expressions.FitText(h(origin,Operand.Role.VALUE_READ),right,length," ");
            } else if(value.kind()==ConditionNames.ValueKind.HEX) {
                var bytes=new ArrayList<Integer>();for(int i=0;i<value.value().length();i+=2)bytes.add(Integer.parseInt(value.value().substring(i,i+2),16));
                right=new Expressions.Literal(h(origin,Operand.Role.VALUE_READ),new Values.BytesValue(bytes));
                return unknown(Types.Builtin.BOOL,"CONDITION_BYTE_ACCESS_UNAVAILABLE",origin,List.of(left,right));
            } else return unknown(Types.Builtin.BOOL,"CONDITION_FIGURATIVE_COLLATION_UNAVAILABLE",origin,List.of(left));
            return new Expressions.Binary(h(origin,Operand.Role.PREDICATE),operator,left,right);
        }
        /** Balanced joins preserve source grouping with O(n) nodes and O(log n) added depth. */
        Expression join(Expressions.BinaryOperator operator,List<Expression> values,OriginId origin) {
            var level=new ArrayList<>(values);
            while(level.size()>1){var next=new ArrayList<Expression>((level.size()+1)/2);for(int i=0;i<level.size();i+=2)next.add(i+1==level.size()?level.get(i):new Expressions.Binary(h(origin,Operand.Role.PREDICATE),operator,level.get(i),level.get(i+1)));level=next;}
            return level.getFirst();
        }
    }
}
