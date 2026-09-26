package io.github.gustavo2358.lower.application;

import io.github.gustavo2358.lower.domain.SpInput;
import io.github.gustavo2358.air.model.*;
import io.github.gustavo2358.air.model.Ids.*;
import java.util.*;
import java.math.BigInteger;

/** Translation of a published comparison tree; collation-dependent truth stays unknown. */
final class TextPredicateLowerer {
    static void validate(SpInput.IfFact fact,SpInput.TextPredicate p,EntryGobackAdmission.Context c) {
        c.touch();boolean leaf=p.kind().name().startsWith("EQUAL_");
        c.require(leaf?p.reference().isPresent()&&p.children().isEmpty():p.reference().isEmpty()&&!p.children().isEmpty(),Admission.Rule.PROFILE_FACT,fact.header().id().handle(),null,"text predicate shape");
        c.require((p.kind()==SpInput.TextPredicateKind.EQUAL_TEXT)==p.text().isPresent(),Admission.Rule.PROFILE_FACT,fact.header().id().handle(),null,"literal equality payload");
        c.require(p.kind()!=SpInput.TextPredicateKind.NOT||p.children().size()==1,Admission.Rule.PROFILE_FACT,fact.header().id().handle(),null,"NOT arity");
        c.require(p.kind()!=SpInput.TextPredicateKind.AND&&p.kind()!=SpInput.TextPredicateKind.OR||p.children().size()>=2,Admission.Rule.PROFILE_FACT,fact.header().id().handle(),null,"combined predicate arity");
        p.reference().ifPresent(id->c.require(fact.conditionProvenance().exact()&&fact.conditionReads().stream().anyMatch(r->r.id().equals(id)&&r.wholeItemAccess().isPresent()
            &&r.role()==SpInput.OperandRole.READ&&r.provenance().exact()&&r.binding().status()==SpInput.ResolutionStatus.RESOLVED),Admission.Rule.PROFILE_FACT,id.handle(),null,"predicate needs canonical whole read"));
        p.children().forEach(child->validate(fact,child,c));
    }
    static Optional<Expression> translate(SpInput.IfFact fact,OperationId op,ScalarDataTranslator.Result data,LocalIds ids,
            SourceOrigins origins,List<LoweringResult.OperandLink> links,List<Evidence.Uncertainty> uncertainties) {
        if(fact.textPredicate().isEmpty())return Optional.empty();
        var refs=new HashMap<SpInput.OperandId,SpInput.DataReference>();fact.conditionReads().forEach(r->refs.put(r.id(),r));
        var builder=new Builder(fact,op,data,ids,origins,links,uncertainties,refs);
        if(!builder.ready(fact.textPredicate().get()))return Optional.empty();
        return Optional.of(builder.expression(fact.textPredicate().get(),"root"));
    }
    private static final class Builder {
        final SpInput.IfFact fact;final OperationId op;final ScalarDataTranslator.Result data;final LocalIds ids;final SourceOrigins origins;
        final List<LoweringResult.OperandLink> links;final List<Evidence.Uncertainty> uncertainties;final Map<SpInput.OperandId,SpInput.DataReference> refs;
        final Map<ObjectId,BigInteger> extents=new HashMap<>();final OriginId origin;
        Builder(SpInput.IfFact fact,OperationId op,ScalarDataTranslator.Result data,LocalIds ids,SourceOrigins origins,List<LoweringResult.OperandLink> links,List<Evidence.Uncertainty> uncertainties,Map<SpInput.OperandId,SpInput.DataReference> refs) {
            this.fact=fact;this.op=op;this.data=data;this.ids=ids;this.origins=origins;this.links=links;this.uncertainties=uncertainties;this.refs=refs;
            origin=origins.source("text-predicate",fact.header().id().handle(),fact.conditionProvenance());
        }
        boolean ready(SpInput.TextPredicate p) {
            if(p.reference().isPresent()) {
                var ref=refs.get(p.reference().get());if(ref==null||ref.wholeItemAccess().isEmpty())return false;
                var link=data.index().get(ref.wholeItemAccess().get().data());if(link==null)return false;
                var object=data.objects().stream().filter(o->o.id().equals(link.object())).findFirst().orElse(null);
                var extent=data.logicalTextExtents().get(ref.wholeItemAccess().get().data());
                if(object==null||!(object.typeRef() instanceof Types.Known known)||known.type()!=Types.Builtin.TEXT||extent==null||extent<=0)return false;
                extents.put(link.object(),BigInteger.valueOf(extent));
            }
            return p.children().stream().allMatch(this::ready);
        }
        Operand.Header header(String path,Operand.Role role){return new Operand.Header(new OperandId(new OperationOwner(op),ids.id("operand","text-predicate",op.localId(),path)),role,origin);}
        Expression expression(SpInput.TextPredicate p,String path) {
            if(p.kind()==SpInput.TextPredicateKind.NOT)return new Expressions.Unary(header(path,Operand.Role.PREDICATE),Expressions.UnaryOperator.NOT,expression(p.children().getFirst(),path+"/not"));
            if(p.kind()==SpInput.TextPredicateKind.AND||p.kind()==SpInput.TextPredicateKind.OR) {
                Expression result=expression(p.children().getFirst(),path+"/0");
                for(int i=1;i<p.children().size();i++)result=new Expressions.Binary(header(path+"/join-"+i,Operand.Role.PREDICATE),p.kind()==SpInput.TextPredicateKind.AND?Expressions.BinaryOperator.AND:Expressions.BinaryOperator.OR,result,expression(p.children().get(i),path+"/"+i));
                return result;
            }
            var ref=refs.get(p.reference().orElseThrow());var link=data.index().get(ref.wholeItemAccess().orElseThrow().data());var length=extents.get(link.object());
            if(p.kind()==SpInput.TextPredicateKind.EQUAL_TEXT||p.kind()==SpInput.TextPredicateKind.EQUAL_SPACES) {
                var text=p.text().orElse("");var size=length.max(BigInteger.valueOf(text.codePointCount(0,text.length())));
                var left=new Expressions.FitText(header(path+"/left-fit",Operand.Role.VALUE_READ),read(ref,link,path+"/left"),size," ");
                var right=new Expressions.FitText(header(path+"/right-fit",Operand.Role.VALUE_READ),new Expressions.Literal(header(path+"/literal",Operand.Role.VALUE_READ),new Values.TextValue(text)),size," ");
                return new Expressions.Binary(header(path,Operand.Role.PREDICATE),Expressions.BinaryOperator.EQ,left,right);
            }
            var reason=new UncertaintyId(op.publication(),ids.id("uncertainty","figurative-collation",op.localId(),path));
            uncertainties.add(new Evidence.Uncertainty(reason,"FIGURATIVE_COLLATION_UNKNOWN",List.of(Evidence.Dimension.VALUES),new Scopes.EntityScope(List.of(op)),"LOW/HIGH-VALUES depends on source collation; only character uniformity is known",origin));
            Expression unknown=new Expressions.Unknown(header(path+"/collation",Operand.Role.PREDICATE),Types.known(Types.Builtin.BOOL),List.of(read(ref,link,path+"/collation-read")),Scopes.NoMemory.INSTANCE,reason);
            if(length.compareTo(BigInteger.ONE)<=0)return unknown;
            var left=new Expressions.SliceText(header(path+"/prefix",Operand.Role.VALUE_READ),new Expressions.FitText(header(path+"/prefix-fit",Operand.Role.VALUE_READ),read(ref,link,path+"/prefix-read"),length," "),integer(path+"/zero",BigInteger.ZERO),integer(path+"/prefix-length",length.subtract(BigInteger.ONE)));
            var right=new Expressions.SliceText(header(path+"/suffix",Operand.Role.VALUE_READ),new Expressions.FitText(header(path+"/suffix-fit",Operand.Role.VALUE_READ),read(ref,link,path+"/suffix-read"),length," "),integer(path+"/one",BigInteger.ONE),integer(path+"/suffix-length",length.subtract(BigInteger.ONE)));
            var uniform=new Expressions.Binary(header(path+"/uniform",Operand.Role.PREDICATE),Expressions.BinaryOperator.EQ,left,right);
            return new Expressions.Binary(header(path,Operand.Role.PREDICATE),Expressions.BinaryOperator.AND,uniform,unknown);
        }
        Expression integer(String path,BigInteger value){return new Expressions.Literal(header(path,Operand.Role.VALUE_READ),new Values.IntValue(value));}
        Expression read(SpInput.DataReference ref,LoweringResult.DataLink link,String path) {
            var place=new Places.ObjectPlace(header(path+"/place",Operand.Role.VALUE_READ),link.object());var read=new Expressions.Read(header(path,Operand.Role.VALUE_READ),place);
            links.add(new LoweringResult.OperandLink(ref.id(),read.header().id(),origin));links.add(new LoweringResult.OperandLink(ref.id(),place.header().id(),origin));return read;
        }
    }
}
