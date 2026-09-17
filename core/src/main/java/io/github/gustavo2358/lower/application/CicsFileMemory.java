package io.github.gustavo2358.lower.application;
import io.github.gustavo2358.air.model.*;
import io.github.gustavo2358.air.model.Ids.*;
import io.github.gustavo2358.air.model.Ids.OperandId;
import io.github.gustavo2358.lower.domain.SpInput.*;
import java.math.BigInteger;
import java.util.*;
/** Source host footprints; every declared write is MAY except proved RESP/RESP2 on return. */
final class CicsFileMemory {
    record Result(List<Place> operands,Interactions.EffectBound bound) { }
    static Result effects(CicsFileFact f,ScalarDataTranslator.Result data,CicsFileInvokeHandler.Context c) {
        var reads=new LinkedHashSet<Scopes.MemoryScope>();var writes=new LinkedHashSet<Scopes.MemoryScope>();
        var operands=new ArrayList<Place>();var returned=new ArrayList<OperandId>();boolean unknown=!CicsFileAdmission.wellFormed(f);
        for(int n=0;n<f.options().size();n++) {
            var option=f.options().get(n);if(option.role()==CicsFileRole.NONE)continue;
            if(option.role()==CicsFileRole.UNKNOWN){unknown=true;continue;}
            boolean read=option.role()==CicsFileRole.READ||option.role()==CicsFileRole.READ_WRITE;
            boolean write=option.role()==CicsFileRole.WRITE||option.role()==CicsFileRole.READ_WRITE;
            var ref=option.reference().orElse(null);if(ref==null){if(write)writes.add(new Scopes.VisibleMemory(c.op.unit(),true));else if(option.literal().isEmpty()&&option.integer().isEmpty()&&!option.canonicalName().equals("FILE"))reads.add(new Scopes.VisibleMemory(c.op.unit(),true));continue;}
            var object=ref.binding().selected().map(data.nominal()::get).orElse(null);var view=c.view(ref);
            var source=c.origins.source("cics-file-option",ref.id().handle(),ref.provenance());
            if(read){var width=readWidth(f,option);boolean bounded=object!=null&&view!=null&&width!=null&&width.signum()>=0&&width.compareTo(view.extent())<=0;
                if(width==null||width.signum()!=0)reads.add(bounded?new Scopes.ObjectsMemory(List.of(object)):new Scopes.VisibleMemory(c.op.unit(),true));}
            if(write) {
                BigInteger width=width(f,option,view);
                boolean bounded=object!=null&&view!=null&&width!=null&&width.signum()>=0&&width.compareTo(view.extent())<=0;
                if(width==null||width.signum()!=0)writes.add(bounded?new Scopes.ObjectsMemory(List.of(object)):new Scopes.VisibleMemory(c.op.unit(),true));
                if(bounded&&Set.of("RESP","RESP2").contains(option.canonicalName())&&width.equals(view.extent())&&data.index().containsKey(ref.binding().selected().orElseThrow())){
                    var place=RegionalPlaces.place(ref,data.index().get(ref.binding().selected().orElseThrow()),c.header("effect-"+n,Operand.Role.VALUE_WRITE,source),c.ids);
                    operands.add(place);returned.add(place.header().id());c.link(ref,List.of(place.header().id()),source);
                }
            }
            if(object!=null&&!(write&&Set.of("RESP","RESP2").contains(option.canonicalName())&&returned.contains(c.header("effect-"+n,Operand.Role.VALUE_WRITE,source).id()))){
                var place=new Places.ObjectPlace(c.header("host-"+n,write?Operand.Role.VALUE_WRITE:Operand.Role.VALUE_READ,source),object);operands.add(place);c.link(ref,List.of(place.header().id()),source);
            }
        }
        if(unknown){reads.add(new Scopes.VisibleMemory(c.op.unit(),true));writes.add(new Scopes.VisibleMemory(c.op.unit(),true));returned.clear();}
        var otherwise=new Interactions.ForeignEffects(bound(reads),bound(writes),List.of());
        var per=returned.isEmpty()?List.<Interactions.OutcomeEffects>of():List.of(new Interactions.OutcomeEffects(Control.NormalOutcome.INSTANCE,new Interactions.ForeignEffects(otherwise.reads(),otherwise.writes(),returned)));
        return new Result(List.copyOf(operands),new Interactions.EffectBound(otherwise,per));
    }
    private static BigInteger readWidth(CicsFileFact f,CicsFileOption option){
        return switch(option.canonicalName()) {
            case "FILE"->BigInteger.valueOf(8);
            case "SYSID","TOKEN"->BigInteger.valueOf(4);
            case "LENGTH","KEYLENGTH","REQID"->BigInteger.valueOf(2);
            case "FROM"->f.options().stream().filter(o->o.canonicalName().equals("LENGTH")).findFirst().flatMap(CicsFileOption::integer).orElse(null);
            case "RIDFLD"->f.options().stream().anyMatch(o->o.canonicalName().equals("XRBA"))?BigInteger.valueOf(8):f.options().stream().anyMatch(o->Set.of("RBA","RRN").contains(o.canonicalName()))?BigInteger.valueOf(4):f.options().stream().filter(o->o.canonicalName().equals("KEYLENGTH")).findFirst().flatMap(CicsFileOption::integer).orElse(null);
            default->null;
        };
    }
    private static BigInteger width(CicsFileFact f,CicsFileOption option,Memory.ViewBinding view){
        String name=option.canonicalName();
        if(Set.of("RESP","RESP2","TOKEN","NUMREC").contains(name))return BigInteger.valueOf(4);
        if(name.equals("LENGTH")&&CicsFileAdmission.READS.contains(f.command()))return BigInteger.valueOf(2);
        if(name.equals("FILE")&&f.targetMode()==CicsFileTargetMode.OUTPUT)return BigInteger.valueOf(8);
        if(name.equals("INTO")){
            var length=f.options().stream().filter(o->o.canonicalName().equals("LENGTH")).toList();
            // Defaulting also depends on the translator NOLENGTH option (API5.6 p9).
            // This source contract publishes no proof of that option, so absence alone is insufficient.
            return length.size()==1?length.getFirst().integer().orElse(null):null;
        }
        if(name.equals("RIDFLD")){
            if(f.command().equals("WRITE")){if(f.options().stream().anyMatch(o->o.canonicalName().equals("XRBA")))return BigInteger.valueOf(8);if(f.options().stream().anyMatch(o->o.canonicalName().equals("RBA")))return BigInteger.valueOf(4);}
            return f.options().stream().filter(o->o.canonicalName().equals("KEYLENGTH")).findFirst().flatMap(CicsFileOption::integer).orElse(null);
        }
        // Pointer width, remote/file key size and SPI attribute layouts need their own proof.
        return null;
    }
    private static Scopes.MemoryBound bound(Set<Scopes.MemoryScope> scopes){return scopes.isEmpty()?Scopes.NoMemory.INSTANCE:new Scopes.WithinMemory(scopes.size()==1?scopes.iterator().next():new Scopes.MemoryUnion(List.copyOf(scopes)));}
}
