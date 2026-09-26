package io.github.gustavo2358.lower.application;
import io.github.gustavo2358.air.model.*;
import io.github.gustavo2358.air.model.Ids.*;
import io.github.gustavo2358.air.model.Ids.OperandId;
import io.github.gustavo2358.lower.domain.SpInput.*;
import java.math.BigInteger;
import java.util.*;
/** C-FC source protocol. Value evaluation remains in the general AIR consumer. */
final class CicsFileInvokeHandler {
    static final Capabilities.Capability NAME=new Capabilities.Capability("cics-ts.file","1");
    private CicsFileInvokeHandler() { }
    static Terminator translate(CicsFileFact f,ScalarDataTranslator.Result data,LabelId next,UnitId unit,LocalIds ids,SourceOrigins origins,
        List<LoweringResult.OperandLink> links,List<Evidence.CoverageItem> items,List<Evidence.Uncertainty> uncertainties) {
        var op=new OperationId(unit,ids.id("operation","cics-file",unit.localId(),f.header().id().handle()));
        var origin=origins.source("statement",f.header().id().handle(),f.header().provenance());var scope=new Scopes.EntityScope(List.of(op));
        var reason=new UncertaintyId(unit.publication(),ids.id("uncertainty","cics-file",op.localId(),"values-outcomes"));
        uncertainties.add(new Evidence.Uncertainty(reason,"cobol-lower:CICS_FILE_OUTCOME_VALUES_UNKNOWN",List.of(Evidence.Dimension.CONTROL,Evidence.Dimension.EFFECTS,Evidence.Dimension.VALUES,Evidence.Dimension.DEPENDENCIES),scope,
            "CICS TS5.6 source-only FILE contract; conditional external outcomes and returned values remain open. Direction="+f.targetMode()+"; source gaps="+f.gapCodes(),origin));
        var context=new Context(data,op,ids,origins,origin,reason,links,items);
        var policy=new Interactions.ExtensionName(NAME.name(),NAME.version());Interactions.Target target;
        if(f.target().orElse(null) instanceof LiteralCallTarget l&&l.logicalValue().isPresent())target=new Interactions.LiteralTarget("file","cics.file",l.logicalValue().orElseThrow().value(),policy,origins.source("cics-file-target",l.id().handle(),l.provenance()));
        else if(f.target().orElse(null) instanceof DataCallTarget d&&context.hasNamePlace(d.reference(),8)) {
            Expression name=context.name(d.reference(),8,"file",Operand.Role.CALL_TARGET);
            target=new Interactions.ComputedTarget("file","cics.file",name,policy,name.header().origin());
        } else if(f.targetMode()!=CicsFileTargetMode.INPUT) {
            // Output-name SPI commands receive the name from the environment.
            Expression name=context.unknown("file",Operand.Role.CALL_TARGET,Types.Builtin.TEXT,origin);
            target=new Interactions.ComputedTarget("file","cics.file",name,policy,name.header().origin());
        } else target=null;
        var args=new ArrayList<Interactions.Argument>();var types=new ArrayList<Types.Builtin>();
        var sys=f.options().stream().filter(o->o.canonicalName().equals("SYSID")).toList();
        args.add(new Interactions.ValueArgument(context.literal("selection",sys.isEmpty()?"DEFAULT":"EXPLICIT")));types.add(Types.Builtin.TEXT);
        Expression system=sys.isEmpty()?context.literal("sysid",""):sys.size()==1?context.option(sys.getFirst(),4,"sysid",Types.Builtin.TEXT):context.unknown("sysid",Operand.Role.ARGUMENT_VALUE,Types.Builtin.TEXT,origin);
        args.add(new Interactions.ValueArgument(system));types.add(Types.Builtin.TEXT);
        if(CicsFileAdmission.BROWSE.contains(f.command())) {
            var req=f.options().stream().filter(o->o.canonicalName().equals("REQID")).toList();
            var value=req.isEmpty()?context.integer("reqid",BigInteger.ZERO):req.size()==1?context.option(req.getFirst(),2,"reqid",Types.Builtin.INT):context.unknown("reqid",Operand.Role.ARGUMENT_VALUE,Types.Builtin.INT,origin);
            args.add(new Interactions.ValueArgument(value));types.add(Types.Builtin.INT);
        }
        var params=new ArrayList<Interactions.Parameter>();for(int n=0;n<args.size();n++)params.add(new Interactions.Parameter(BigInteger.valueOf(n),new Interactions.KnownMode(Interactions.PassingMode.VALUE),Types.known(types.get(n)),Interactions.ExternalBinding.INSTANCE,origin));
        var signature=new Interactions.ExternalSignature(new Interactions.Signature(new Interactions.ParameterInventory(params,Interactions.NoRemainder.INSTANCE),new Interactions.ResultInventory(List.of(),Interactions.NoRemainder.INSTANCE),origin));
        var memory=CicsFileMemory.effects(f,data,context);context.finish();
        if(target==null) {
            var missing=new UncertaintyId(unit.publication(),ids.id("uncertainty","cics-file-target",op.localId(),"unavailable"));
            uncertainties.add(new Evidence.Uncertainty(missing,"cobol-lower:CICS_FILE_NAME_AREA_UNAVAILABLE",
                List.of(Evidence.Dimension.VALUES,Evidence.Dimension.DEPENDENCIES),scope,
                "Input FILE name has no supported literal or nominal value place; option effects and normal continuation remain published",origin));
            var reads=memory.operands().stream().filter(p->p.header().role()==Operand.Role.VALUE_READ).map(p->p.header().id()).toList();
            var writes=memory.operands().stream().filter(p->p.header().role()==Operand.Role.VALUE_WRITE).map(p->p.header().id()).toList();
            var returned=next==null?List.<OperandId>of():memory.bound().perOutcome().stream()
                .filter(e->e.outcome() instanceof Control.NormalOutcome).flatMap(e->e.effects().mustOverwrite().stream()).toList();
            var envelope=new Envelopes.Envelope(new Envelopes.MemoryEnvelope(reads,memory.bound().otherwise().reads(),writes,
                memory.bound().otherwise().writes(),returned),new Control.ControlEnvelope(next==null?List.of():List.of(new Control.JumpAlternative(next)),
                next==null?new Scopes.WithinControl(new Scopes.LabelsControl(List.of())):Scopes.NoControl.INSTANCE),
                new Envelopes.DependencyEnvelope(List.of(),Scopes.NoResources.INSTANCE));
            var open=new Evidence.Claim(scope,Evidence.PrecisionStatus.OPEN,List.of(reason));
            var header=new Operations.Header(op,origin,Evidence.CoverageStatus.ABSTRACTED,
                new Evidence.Precision(open,open,open,new Evidence.Claim(scope,Evidence.PrecisionStatus.NOT_APPLICABLE,List.of()),open),List.of(reason,missing));
            var knownOperands=new ArrayList<Operand>();
            for(var argument:args)if(argument instanceof Interactions.ValueArgument value)knownOperands.add(value.value());
            knownOperands.addAll(memory.operands());
            return new Operations.Opaque(header,"cics-file-target-unavailable/"+f.command().toLowerCase(Locale.ROOT),
                knownOperands,List.of(),envelope);
        }
        var outcomes=new Control.InvocationOutcomes(next==null?List.of():List.of(new Control.Normal(next)),
            next==null?new Scopes.WithinControl(new Scopes.LabelsControl(List.of())):Scopes.NoControl.INSTANCE);
        var open=new Evidence.Claim(scope,Evidence.PrecisionStatus.OPEN,List.of(reason));var exact=new Evidence.Claim(scope,Evidence.PrecisionStatus.EXACT,List.of());
        var precision=new Evidence.Precision(open,open,open,new Evidence.Claim(scope,Evidence.PrecisionStatus.NOT_APPLICABLE,List.of()),exact);
        return new Operations.Invoke(new Operations.Header(op,origin,Evidence.CoverageStatus.ABSTRACTED,precision,List.of(reason)),f.command().toLowerCase(Locale.ROOT),target,args,List.of(),signature,memory.operands(),memory.bound(),outcomes,new Interactions.KnownContract(new Interactions.ContractRef("cics-ts.file-control","1",List.of(origin))));
    }
    static final class Context {
        final Map<DataReference,List<Ids.Id>> covered=new LinkedHashMap<>();
        final Map<DataReference,OriginId> coverageOrigins=new LinkedHashMap<>();
        final ScalarDataTranslator.Result data;final OperationId op;final LocalIds ids;final SourceOrigins origins;final OriginId origin;final UncertaintyId reason;final List<LoweringResult.OperandLink> links;final List<Evidence.CoverageItem> items;
        Context(ScalarDataTranslator.Result data,OperationId op,LocalIds ids,SourceOrigins origins,OriginId origin,UncertaintyId reason,List<LoweringResult.OperandLink> links,List<Evidence.CoverageItem> items){this.data=data;this.op=op;this.ids=ids;this.origins=origins;this.origin=origin;this.reason=reason;this.links=links;this.items=items;}
        Operand.Header header(String key,Operand.Role role,OriginId source){return new Operand.Header(new OperandId(new OperationOwner(op),ids.id("operand","cics-file-"+key,op.localId(),key)),role,source);}
        Expression literal(String key,String text){return new Expressions.Literal(header(key,Operand.Role.ARGUMENT_VALUE,origin),new Values.TextValue(text));}
        Expression integer(String key,BigInteger value){return new Expressions.Literal(header(key,Operand.Role.ARGUMENT_VALUE,origin),new Values.IntValue(value));}
        Expression unknown(String key,Operand.Role role,Types.Builtin type,OriginId source){return new Expressions.Unknown(header(key,role,source),Types.known(type),List.of(),Scopes.NoMemory.INSTANCE,reason);}
        Expression option(CicsFileOption o,int width,String key,Types.Builtin type){
            if(type==Types.Builtin.TEXT&&o.literal().isPresent())return literal(key,o.literal().orElseThrow());
            if(type==Types.Builtin.INT&&o.integer().isPresent())return integer(key,o.integer().orElseThrow());
            if(type==Types.Builtin.TEXT&&o.reference().isPresent())return name(o.reference().orElseThrow(),width,key,Operand.Role.ARGUMENT_VALUE);
            return unknown(key,Operand.Role.ARGUMENT_VALUE,type,origin);
        }
        Expression name(DataReference ref,int width,String key,Operand.Role role){
            var source=origins.source("cics-file-operand",ref.id().handle(),ref.provenance());var view=view(ref);
            if(view==null||!view.extent().equals(BigInteger.valueOf(width))||!view.codec().equals(RegionalStorageAdmission.IBM1047)) {
                boolean logical=role==Operand.Role.ARGUMENT_VALUE&&ref.logicalWholeItem().isPresent()
                    &&ref.logicalWholeItem().filter(data.nominal()::containsKey).isPresent()
                    &&ref.logicalWholeItem().map(data.logicalTextExtents()::get).filter(n->n!=null&&n==width).isPresent();
                if(!logical&&(role!=Operand.Role.CALL_TARGET||!NominalTarget.available(ref,data)))return unknown(key,role,Types.Builtin.TEXT,source);
                var place=logical?new Places.ObjectPlace(header(key+"-place",Operand.Role.VALUE_READ,source),
                    data.nominal().get(ref.logicalWholeItem().orElseThrow()))
                    :NominalTarget.place(ref,data,header(key+"-place",Operand.Role.VALUE_READ,source),ids);
                var read=new Expressions.Read(header(key+"-read",role,source),place);link(ref,List.of(read.header().id(),place.header().id()),source);return read;
            }
            var place=RegionalPlaces.place(ref,data.index().get(ref.binding().selected().orElseThrow()),header(key+"-place",Operand.Role.VALUE_READ,source),ids);
            var read=new Expressions.Read(header(key+"-read",role,source),place);link(ref,List.of(read.header().id(),place.header().id()),source);return read;
        }
        void finish(){covered.forEach((ref,values)->items.add(ScalarEvidence.item(op.unit().publication(),"operand",ids.sourceKey(ref.id().handle()),coverageOrigins.get(ref),values)));}
        Memory.ViewBinding view(DataReference ref){if(ref.regionalAccess().isEmpty()||ref.binding().selected().isEmpty())return null;var base=data.views().get(ref.binding().selected().orElseThrow());return base==null?null:RegionalPlaces.view(base,ref);}
        boolean hasNamePlace(DataReference ref,int width){
            var physical=view(ref);
            return physical!=null&&physical.extent().equals(BigInteger.valueOf(width))
                &&physical.codec().equals(RegionalStorageAdmission.IBM1047)
                &&ref.binding().selected().filter(data.index()::containsKey).isPresent()
                ||NominalTarget.available(ref,data);
        }
        void link(DataReference ref,List<OperandId> operands,OriginId source){for(var id:operands)links.add(new LoweringResult.OperandLink(ref.id(),id,source));covered.computeIfAbsent(ref,k->new ArrayList<>()).addAll(operands);coverageOrigins.putIfAbsent(ref,source);}
    }
}
