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
    /** Only a wholly literal Boolean tree closes control; no short-circuit of reads/effects. */
    private static Optional<Boolean> constantBoolean(ConditionNames.Tree root) {
        record Pending(ConditionNames.Tree tree,boolean finish) { }
        var todo=new ArrayDeque<Pending>();var values=new IdentityHashMap<ConditionNames.Tree,Boolean>();
        todo.push(new Pending(root,false));
        while(!todo.isEmpty()) {
            var next=todo.pop();var tree=next.tree();
            if(!next.finish()&&!tree.children().isEmpty()) {
                todo.push(new Pending(tree,true));
                for(int i=tree.children().size()-1;i>=0;i--)todo.push(new Pending(tree.children().get(i),false));
                continue;
            }
            if(tree.kind().equals("BOOL"))values.put(tree,Boolean.valueOf(tree.use()));
            else if(tree.kind().equals("NOT")&&values.containsKey(tree.children().getFirst()))
                values.put(tree,!values.get(tree.children().getFirst()));
            else if((tree.kind().equals("AND")||tree.kind().equals("OR"))&&tree.children().stream().allMatch(values::containsKey)) {
                boolean value=tree.kind().equals("AND");
                for(var child:tree.children())value=tree.kind().equals("AND")?value&&values.get(child):value||values.get(child);
                values.put(tree,value);
            }
        }
        return Optional.ofNullable(values.get(root));
    }
    Terminator branch(SpInput.StatementFact fact,String role,LabelId whenTrue,LabelId whenFalse,
            UnitId unit,LocalIds ids,SourceOrigins origins,List<Evidence.Uncertainty> uncertainties) {
        var operation=new OperationId(unit,ids.id("operation","condition-name-branch",unit.localId(),key(fact.header().id().handle(),role)));
        var tree=predicates.get(key(fact.header().id().handle(),role));
        var constant=constantBoolean(tree);
        if(constant.isPresent()) {
            var origin=origins.source("condition-name-decision",fact.header().id().handle(),fact.header().provenance());
            var exact=new Evidence.Claim(new Scopes.EntityScope(List.of(operation)),Evidence.PrecisionStatus.EXACT,List.of());
            return new Operations.Jump(new Operations.Header(operation,origin,Evidence.CoverageStatus.MODELED,
                new Evidence.Precision(exact,exact,exact,exact,exact),List.of()),constant.get()?whenTrue:whenFalse);
        }
        int before=uncertainties.size();
        var builder=new Builder(operation,ids,origins,uncertainties,fact.header().provenance());
        var previous=fact instanceof SpInput.IfFact f?TextPredicateLowerer.translate(f,operation,data,ids,origins,links,uncertainties):Optional.<Expression>empty();
        var value=previous.orElseGet(()->builder.tree(tree));
        var reasons=uncertainties.subList(before,uncertainties.size()).stream().map(Evidence.Uncertainty::id).toList();
        var origin=origins.source("condition-name-decision",fact.header().id().handle(),fact.header().provenance());
        var exact=new Evidence.Claim(new Scopes.EntityScope(List.of(operation)),Evidence.PrecisionStatus.EXACT,List.of());
        var values=new Evidence.Claim(new Scopes.EntityScope(List.of(operation)),reasons.isEmpty()?Evidence.PrecisionStatus.EXACT:Evidence.PrecisionStatus.OPEN,reasons);
        if(!builder.executable) {
            var open=new Evidence.Claim(new Scopes.EntityScope(List.of(operation)),Evidence.PrecisionStatus.OPEN,reasons);
            var memory=new Envelopes.MemoryEnvelope(builder.safeReads.stream().filter(r->r.header().role()!=Operand.Role.ADDRESS_READ).map(r->r.place().header().id()).toList(),
                (builder.openExpression||builder.openReads)?new Scopes.WithinMemory(new Scopes.AllMemory(unit.publication(),true)):
                    new Scopes.WithinMemory(new Scopes.ObjectsMemory(List.copyOf(builder.unlocated))),
                List.of(),builder.openExpression?new Scopes.WithinMemory(new Scopes.AllMemory(unit.publication(),true)):Scopes.NoMemory.INSTANCE,List.of());
            return new Operations.Opaque(new Operations.Header(operation,origin,Evidence.CoverageStatus.ABSTRACTED,
                new Evidence.Precision(open,open,builder.openExpression?open:exact,values,exact),reasons),
                "PREDICATE_NOT_EXECUTABLE",builder.opaqueOperands(),List.of(),
                new Envelopes.Envelope(memory,new Control.ControlEnvelope(List.of(new Control.JumpAlternative(whenTrue),new Control.JumpAlternative(whenFalse)),
                    new Scopes.WithinControl(new Scopes.LabelsControl(List.of()))),
                    new Envelopes.DependencyEnvelope(List.of(),builder.openExpression?Scopes.AnyResource.INSTANCE:Scopes.NoResources.INSTANCE)));
        }
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
            uncertainties.add(new Evidence.Uncertainty(reason,code,List.of(Evidence.Dimension.VALUES),new Scopes.EntityScope(List.of(operation)),"The source condition is modeled; this input lacks the runtime value, access or collation proof.",origin));
            return new Expressions.Unknown(header,Types.known(type),dependencies,new Scopes.WithinMemory(new Scopes.AllMemory(operation.publication(),true)),reason);
        }
        boolean executable=true,openExpression=false,openReads=false;
        final List<Expressions.Read> safeReads=new ArrayList<>();final List<Place> unlocatedPlaces=new ArrayList<>();final Set<ObjectId> unlocated=new LinkedHashSet<>();
        List<Operand> opaqueOperands() {var out=new ArrayList<Operand>(safeReads);out.addAll(unlocatedPlaces);return List.copyOf(out);}
        private final Set<String> addressOperands=new HashSet<>();
        private final Map<Expression,Types.Builtin> scalarTypes=new IdentityHashMap<>();
        private final Map<Expression,BigInteger> textLengths=new IdentityHashMap<>();
        Expression literal(ConditionNames.Tree tree,OriginId origin) {
            // The pinned AIR JSON codec carries INT but not BoolValue. Preserve
            // mixed Boolean trees without discarding their other reads/effects.
            if(tree.kind().equals("BOOL"))return new Expressions.Binary(h(origin,Operand.Role.PREDICATE),Expressions.BinaryOperator.EQ,
                new Expressions.Literal(h(origin,Operand.Role.VALUE_READ),new Values.IntValue(BigInteger.ZERO)),
                new Expressions.Literal(h(origin,Operand.Role.VALUE_READ),new Values.IntValue(Boolean.parseBoolean(tree.use())?BigInteger.ZERO:BigInteger.ONE)));
            Values.LiteralValue value;
            if(tree.kind().equals("TEXT")||tree.kind().equals("SPACES"))value=new Values.TextValue(tree.use());
            else {var n=new java.math.BigDecimal(tree.kind().equals("ZERO")?"0":tree.use());
                value=n.scale()<=0?new Values.IntValue(n.toBigIntegerExact()):new Values.DecimalValue(n.unscaledValue(),BigInteger.valueOf(n.scale()));}
            var result=new Expressions.Literal(h(origin,Operand.Role.VALUE_READ),value);
            scalarTypes.put(result,value instanceof Values.TextValue?Types.Builtin.TEXT:value instanceof Values.IntValue?Types.Builtin.INT:Types.Builtin.DECIMAL);
            if(value instanceof Values.TextValue text)textLengths.put(result,BigInteger.valueOf(text.value().codePointCount(0,text.value().length())));
            return result;
        }
        Expression scalarRead(ConditionNames.Tree tree,List<Expression> addressReads,OriginId rootOrigin) {
            var reference=references.get(tree.use());
            var origin=origins.source("predicate-read",reference.id().handle(),reference.provenance());
            var selected=reference.binding().selected().orElseThrow();var link=data.index().get(selected);
            var role=addressOperands.contains(tree.use())?Operand.Role.ADDRESS_READ:Operand.Role.VALUE_READ;
            var objectId=link!=null?link.object():data.nominal().get(selected);var object=objects.get(objectId);
            Types.TypeRef type=object==null?null:object.typeRef();
            Expression result;
            if(addressReads.isEmpty()&&object!=null&&link!=null&&(reference.wholeItemAccess().isPresent()||reference.regionalAccess().isPresent())) {
                var place=RegionalPlaces.place(reference,link,h(origin,role),ids);
                result=new Expressions.Read(h(origin,role),place);
                safeReads.add((Expressions.Read)result);
                if(place instanceof Places.RegionSlice slice)type=slice.typeRef();
                links.add(new LoweringResult.OperandLink(reference.id(),place.header().id(),origin));
            } else {
                executable=false;if(objectId!=null)unlocated.add(objectId);else openReads=true;
                var header=h(origin,role);var reason=new UncertaintyId(operation.publication(),ids.id("uncertainty","predicate-access",operation.localId(),header.id().localId()));
                uncertainties.add(new Evidence.Uncertainty(reason,"PREDICATE_ACCESS_NOT_PROVEN",List.of(Evidence.Dimension.STORAGE,Evidence.Dimension.VALUES),new Scopes.EntityScope(List.of(header.id())),"Nominal data binding does not establish a valid executable address; known address reads, bounded content reads and a non-executable frontier remain.",origin));
                var typeReason=new UncertaintyId(operation.publication(),ids.id("uncertainty","predicate-type",operation.localId(),header.id().localId()));
                uncertainties.add(new Evidence.Uncertainty(typeReason,"TYPE_UNKNOWN",List.of(Evidence.Dimension.VALUES),new Scopes.EntityScope(List.of(header.id())),"The open address occurrence has no same-domain proof; the source object's available type remains on that object.",origin));
                var choiceType=new Types.UnknownType(typeReason);
                if(type==null)type=choiceType;
                unlocatedPlaces.add(new Places.Choice(header,List.of(),objectId==null?new Scopes.WithinMemory(new Scopes.AllMemory(operation.publication(),true)):new Scopes.WithinMemory(new Scopes.ObjectsMemory(List.of(objectId))),choiceType));
                result=new Expressions.Unknown(header,type,addressReads,objectId==null?new Scopes.WithinMemory(new Scopes.AllMemory(operation.publication(),true)):new Scopes.WithinMemory(new Scopes.ObjectsMemory(List.of(objectId))),reason);
            }
            links.add(new LoweringResult.OperandLink(reference.id(),result.header().id(),origin));
            if(type instanceof Types.Known k&&k.type() instanceof Types.Builtin b)scalarTypes.put(result,b);
            var extent=reference.regionalAccess().flatMap(a->a.slice()).map(x->x.extent()).orElseGet(()->Optional.ofNullable(data.logicalTextExtents().get(selected)).map(BigInteger::valueOf).orElse(null));
            if(extent!=null)textLengths.put(result,extent);
            return result;
        }
        Expression relation(String kind,Expression left,Expression right,OriginId origin) {
            var a=scalarTypes.get(left);var b=scalarTypes.get(right);
            if(a==Types.Builtin.TEXT&&b==Types.Builtin.TEXT){
                if(!kind.equals("EQ")&&!kind.equals("NE"))return unknownComplete("PREDICATE_COLLATION_UNKNOWN",origin,List.of(left,right));
                var x=textLengths.get(left);var y=textLengths.get(right);
                if(x==null||y==null)return unknownComplete("PREDICATE_TEXT_EXTENT_UNKNOWN",origin,List.of(left,right));
                var extent=x.max(y);left=new Expressions.FitText(h(origin,Operand.Role.VALUE_READ),left,extent," ");right=new Expressions.FitText(h(origin,Operand.Role.VALUE_READ),right,extent," ");
            } else if((a==Types.Builtin.INT||a==Types.Builtin.DECIMAL)&&(b==Types.Builtin.INT||b==Types.Builtin.DECIMAL)){
                if(a!=b){if(a==Types.Builtin.INT)left=new Expressions.Unary(h(origin,Operand.Role.VALUE_READ),Expressions.UnaryOperator.TO_DECIMAL,left);
                    else right=new Expressions.Unary(h(origin,Operand.Role.VALUE_READ),Expressions.UnaryOperator.TO_DECIMAL,right);}
            } else return unknownComplete("PREDICATE_COMPARISON_DOMAIN_UNAVAILABLE",origin,List.of(left,right));
            return new Expressions.Binary(h(origin,Operand.Role.PREDICATE),Expressions.BinaryOperator.valueOf(kind),left,right);
        }
        Expression unknownComplete(String code,OriginId origin,List<Expression> dependencies) {
            var header=h(origin,Operand.Role.PREDICATE);var reason=new UncertaintyId(operation.publication(),ids.id("uncertainty","scalar-predicate",operation.localId(),header.id().localId()));
            uncertainties.add(new Evidence.Uncertainty(reason,code,List.of(Evidence.Dimension.VALUES),new Scopes.EntityScope(List.of(operation)),"The pure source comparison and its reads are retained; its truth needs the stated domain or interpretation proof.",origin));
            return new Expressions.Unknown(header,Types.known(Types.Builtin.BOOL),dependencies,Scopes.NoMemory.INSTANCE,reason);
        }
        Expression tree(ConditionNames.Tree root) {
            var addressNodes=new ArrayDeque<ConditionNames.Tree>();addressNodes.add(root);
            while(!addressNodes.isEmpty()){var n=addressNodes.removeFirst();if(n.kind().equals("READ")||n.kind().equals("TEST"))n.children().forEach(c->addressOperands.add(c.use()));addressNodes.addAll(n.children());}
            record Pending(ConditionNames.Tree tree,boolean finish) { }
            var todo=new ArrayDeque<Pending>();var built=new IdentityHashMap<ConditionNames.Tree,Expression>();todo.push(new Pending(root,false));
            var origin=origins.source("condition-name-expression",operation.localId(),source);
            while(!todo.isEmpty()) {
                var next=todo.pop();var tree=next.tree();
                if(!next.finish()&&!tree.children().isEmpty()){todo.push(new Pending(tree,true));for(int i=tree.children().size()-1;i>=0;i--)todo.push(new Pending(tree.children().get(i),false));continue;}
                Expression result=switch(tree.kind()) {
                    case "TEST" -> membership(uses.get(tree.use()),tree.children().stream().map(built::get).toList());
                    case "READ" -> scalarRead(tree,tree.children().stream().map(built::get).toList(),origin);
                    case "NUMBER","TEXT","ZERO","SPACES","BOOL" -> literal(tree,origin);
                    case "LOW_VALUES","HIGH_VALUES" -> {
                        var reason=new UncertaintyId(operation.publication(),ids.id("uncertainty","predicate-figurative",operation.localId(),Integer.toString(ordinal)));
                        uncertainties.add(new Evidence.Uncertainty(reason,"PREDICATE_FIGURATIVE_COLLATION_UNKNOWN",List.of(Evidence.Dimension.VALUES),new Scopes.EntityScope(List.of(operation)),"Source figurative character needs collation; it reads no memory.",origin));
                        var value=new Expressions.Unknown(h(origin,Operand.Role.VALUE_READ),Types.known(Types.Builtin.TEXT),List.of(),Scopes.NoMemory.INSTANCE,reason);scalarTypes.put(value,Types.Builtin.TEXT);yield value;
                    }
                    case "EQ","NE","LT","LE","GT","GE" -> relation(tree.kind(),built.get(tree.children().getFirst()),built.get(tree.children().getLast()),origin);
                    case "UNKNOWN" -> {
                        if(!tree.use().equals("PURE")){executable=false;if(tree.use().equals("READS_OPEN"))openReads=true;else openExpression=true;}
                        yield tree.use().equals("PURE")?unknownComplete("CONDITION_OTHER_EXPRESSION_UNAVAILABLE",origin,tree.children().stream().map(built::get).toList()):unknown(Types.Builtin.BOOL,"CONDITION_OTHER_EXPRESSION_UNAVAILABLE",origin,tree.children().stream().map(built::get).toList());
                    }
                    case "NOT" -> new Expressions.Unary(h(origin,Operand.Role.PREDICATE),Expressions.UnaryOperator.NOT,built.get(tree.children().getFirst()));
                    default -> join(tree.kind().equals("AND")?Expressions.BinaryOperator.AND:Expressions.BinaryOperator.OR,tree.children().stream().map(built::get).toList(),origin);
                };
                built.put(tree,result);
            }
            return built.get(root);
        }
        Expression membership(ConditionNames.Use use,List<Expression> addressReads) {
            var definition=definitions.get(use.definition());
            var origin=origins.derived(ids.id("origin","condition-name-membership",operation.localId(),use.id()),
                List.of(origins.source("condition-name-use",use.id(),use.provenance()),origins.source("condition-name-definition",definition.id(),definition.provenance())),"SP2.64/condition-variable-membership");
            var alternatives=new ArrayList<Expression>();
            for(var range:definition.ranges()) {
                if(range.last().isEmpty())alternatives.add(compare(use,definition,range.first(),Expressions.BinaryOperator.EQ,origin,addressReads));
                else alternatives.add(join(Expressions.BinaryOperator.AND,List.of(
                    compare(use,definition,range.first(),Expressions.BinaryOperator.GE,origin,addressReads),compare(use,definition,range.last().get(),Expressions.BinaryOperator.LE,origin,addressReads)),origin));
            }
            return join(Expressions.BinaryOperator.OR,alternatives,origin);
        }
        Expression compare(ConditionNames.Use use,ConditionNames.Definition definition,ConditionNames.Value value,Expressions.BinaryOperator operator,OriginId origin,List<Expression> addressReads) {
            var dataId=new SpInput.DataId(input.unit(),definition.parent());var link=data.index().get(dataId);
            var object=link==null?null:objects.get(link.object());var type=definition.domain()==ConditionNames.VariableDomain.INTEGER?Types.Builtin.INT:Types.Builtin.TEXT;
            Expression left;
            if(!use.indices().isEmpty()&&!use.operand().isEmpty()) {
                left=scalarRead(new ConditionNames.Tree("READ",use.operand(),List.of()),addressReads,origin);
                var indices=new ArrayDeque<ConditionNames.Index>(use.indices());while(!indices.isEmpty()){var i=indices.removeFirst();if(i.kind().equals("UNKNOWN")){executable=false;openExpression=true;}indices.addAll(i.arguments());}
            }
            else if(use.indices().isEmpty()&&object!=null&&object.typeRef() instanceof Types.Known known&&known.type()==type)
                left=new Expressions.Read(h(origin,Operand.Role.VALUE_READ),new Places.ObjectPlace(h(origin,Operand.Role.VALUE_READ),link.object()));
            else left=unknown(type,"CONDITION_VARIABLE_ACCESS_UNAVAILABLE",origin,List.of());
            if(left instanceof Expressions.Read read)safeReads.add(read);
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
