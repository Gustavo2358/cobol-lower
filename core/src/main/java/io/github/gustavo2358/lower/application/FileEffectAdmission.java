package io.github.gustavo2358.lower.application;
import io.github.gustavo2358.lower.domain.*;
import java.math.BigInteger;
import java.util.*;
import static io.github.gustavo2358.lower.domain.FileFacts.*;
import static io.github.gustavo2358.lower.domain.SpInput.*;

/** Bilateral validation of source-owned effect facts. No COBOL text or name lookup. */
final class FileEffectAdmission {
    private static final class Invalid extends RuntimeException {private static final long serialVersionUID=1L;Invalid(String message){super(message);}}
    private static void require(boolean condition,String message){if(!condition)throw new Invalid(message);}
    static void validate(SpInput input,EntryGobackAdmission.Context c) {
        if(input.fileInventory().operations().uses().stream().noneMatch(u->u.effects().isPresent()))return;
        var references=new HashMap<OperandId,DataReference>();
        for(var statement:input.statements())if(statement instanceof OtherStatement o)for(var ref:o.knownReferences())references.put(ref.id(),ref);
        var files=new HashMap<Candidate,Declaration>();for(var f:input.fileInventory().declarations())files.put(new Candidate(f.id(),f.owner()),f);
        for(var use:input.fileInventory().operations().uses())if(use.effects().isPresent())try {
            var e=use.effects().orElseThrow();c.touch();require(use.surface().isPresent(),"file effects require operand surface");
            require(e.availability()!=Availability.KNOWN||!e.unknownReadBound()&&!e.unknownWriteBound()&&e.gapCodes().isEmpty(),"known file effects have closed bounds and no gap");
            require(e.availability()==Availability.KNOWN||!e.gapCodes().isEmpty(),"partial/unavailable effects need reasons");
            if(e.availability()==Availability.UNAVAILABLE||e.availability()==Availability.INPUT_MISSING) {
                require(e.before().isEmpty()&&e.outcomes().isEmpty()&&e.ioReads().isEmpty()&&e.unknownReadBound()&&e.unknownWriteBound(),"unavailable effects cannot assert a plan");continue;
            }
            require(c.regionalStorage!=null,"file effects require a validated storage inventory");
            var file=use.bindingStatus()==ResolutionStatus.RESOLVED?files.get(use.candidates().getFirst()):null;
            var cases=EnumSet.noneOf(EffectOutcome.class);for(var outcome:e.outcomes())require(cases.add(outcome.outcome()),"duplicate file effect outcome");
            require(cases.equals(EnumSet.allOf(EffectOutcome.class)),"conditional file effect table must preserve every outcome");
            for(var read:e.ioReads())target(read,use,c,references);
            if(!e.unknownReadBound()&&file!=null&&(use.command()==Command.WRITE||use.command()==Command.REWRITE||use.command()==Command.RELEASE))
                for(var record:file.records())require(e.ioReads().stream().anyMatch(t->t.data().filter(record::equals).isPresent()),"output record read omitted");
            if(!e.unknownReadBound()&&file!=null)input.fileInventory().auxiliary().ifPresent(aux->{for(var clause:aux.clauses()){
                boolean selected=clause.fileReferences().stream().anyMatch(f->f.status()==ResolutionStatus.RESOLVED&&f.candidates().contains(new Candidate(file.id(),file.owner())));
                boolean read=clause.kind()==AuxKind.RECORD&&(use.command()==Command.WRITE||use.command()==Command.REWRITE||use.command()==Command.RELEASE)||clause.effect()==AuxEffect.ACCESS_CHECK&&use.command()==Command.OPEN||clause.effect()==AuxEffect.PAGE_CONTROL&&(use.command()==Command.WRITE||use.command()==Command.OPEN&&(use.mode()==OpenMode.OUTPUT||use.mode()==OpenMode.EXTEND));
                if(selected&&read)for(var ref:clause.dataReferences())require(ref.binding().selected().isPresent()&&e.ioReads().stream().anyMatch(t->t.data().equals(ref.binding().selected())),"auxiliary parameter read omitted");
            }});
            for(var step:e.before()){require(step.role()==MemoryRole.FROM_RECORD,"only FROM transfer precedes I/O");step(step,use,null,file,c,references);}
            require(e.unknownReadBound()||e.before().stream().allMatch(s->s.source().isPresent()&&!s.source().orElseThrow().wholeBase()),"unproved FROM address requires an open read bound");
            var surface=use.surface().orElseThrow();
            boolean from=surface.operands().stream().anyMatch(o->o.role()==FileFacts.OperandRole.FROM);
            require(e.before().isEmpty()||from&&(use.command()==Command.WRITE||use.command()==Command.REWRITE||use.command()==Command.RELEASE),"unexpected pre-I/O transfer");
            require(e.unknownWriteBound()||!from||e.before().size()==1,"FROM receiver effect omitted");
            for(var step:e.before())require(hasRole(step.destination(),surface,FileFacts.OperandRole.RECORD),"FROM receiver lacks RECORD operand");
            for(var outcome:e.outcomes()) {
                int order=-1;var roles=EnumSet.noneOf(MemoryRole.class);
                for(var step:outcome.steps()) {
                    require(step.role()==MemoryRole.RECORD||roles.add(step.role()),"duplicate file receiver role");
                    require(step.role()!=MemoryRole.FROM_RECORD,"FROM cannot move after I/O");
                    int rank=switch(step.role()){case RECORD,FROM_RECORD->0;case RELATIVE_KEY->1;case RECORD_LENGTH->2;case FILE_STATUS->3;case ADDITIONAL_STATUS->4;case INTO->5;};
                    require(rank>=order,"file memory steps are out of semantic order");order=rank;
                    if(step.role()==MemoryRole.INTO||step.role()==MemoryRole.RECORD_LENGTH||step.role()==MemoryRole.RELATIVE_KEY)
                        require((use.command()==Command.READ||use.command()==Command.RETURN||FileFacts.aggregate(use))&&outcome.outcome()==EffectOutcome.SUCCESS,"READ receiving effects require success");
                    require(!(use.command()==Command.REWRITE&&outcome.outcome()==EffectOutcome.INVALID_KEY&&step.role()==MemoryRole.RECORD),"REWRITE invalid key does not invalidate record");
                    step(step,use,outcome.outcome(),file,c,references);
                    if(step.role()==MemoryRole.INTO)require(hasRole(step.destination(),surface,FileFacts.OperandRole.INTO),"INTO receiver lacks INTO operand");
                }
                if(!e.unknownWriteBound()&&file!=null) {
                    if(use.command()==Command.READ||use.command()==Command.RETURN||use.command()==Command.CLOSE||FileFacts.aggregate(use))for(var record:file.records())require(outcome.steps().stream().anyMatch(s->s.role()==MemoryRole.RECORD&&s.destination().data().filter(record::equals).isPresent()),"buffer effect omitted");
                    if((use.command()==Command.READ||use.command()==Command.RETURN||FileFacts.aggregate(use))&&outcome.outcome()==EffectOutcome.SUCCESS&&surface.operands().stream().anyMatch(o->o.role()==FileFacts.OperandRole.INTO))
                        require(roles.contains(MemoryRole.INTO),"INTO receiver effect omitted");
                    for(var status:FileFacts.local(use)?List.<Reference>of():file.references())if(status.role()==ReferenceRole.FILE_STATUS||status.role()==ReferenceRole.ADDITIONAL_STATUS) {
                        var role=status.role()==ReferenceRole.FILE_STATUS?MemoryRole.FILE_STATUS:MemoryRole.ADDITIONAL_STATUS;
                        require(status.binding().selected().isPresent()&&outcome.steps().stream().anyMatch(s->s.role()==role&&s.destination().data().equals(status.binding().selected())),"file status effect omitted");
                    }
                }
            }
        } catch(Invalid invalid) {c.require(false,Admission.Rule.PROFILE_FACT,"file-effects",use.provenance(),invalid.getMessage());}
    }
    private static boolean hasRole(MemoryTarget target,Surface surface,FileFacts.OperandRole role) {
        return target.reference().isPresent()&&surface.operands().stream().anyMatch(o->o.role()==role&&o.references().contains(target.reference().orElseThrow()));
    }
    private static void target(MemoryTarget t,Use use,EntryGobackAdmission.Context c,Map<OperandId,DataReference> references) {
        c.touch();c.provenance(t.provenance());require(t.data().isPresent()||t.regional().isPresent(),"effect target lacks identity");
        t.data().ifPresent(id->require(id.unit().equals(c.input.unit())&&c.data.containsKey(id),"effect DATA missing or foreign"));
        t.reference().ifPresent(id->{
            var ref=references.get(id);require(id.statement().equals(use.statement())&&ref!=null,"effect occurrence outside observed statement");
            if(t.data().isPresent()&&ref.binding().selected().isPresent())require(t.data().equals(ref.binding().selected()),"effect target/ref binding mismatch");
        });
        t.regional().ifPresent(access->{
            var index=c.regionalStorage;var view=index.views().get(access.view());var node=index.nodes().get(access.view());
            require(access.view().unit().equals(c.input.unit())&&view!=null&&node!=null,"effect view missing or foreign");
            require(t.data().isEmpty()||node.data().equals(t.data()),"effect DATA/view mismatch");
            require(!t.wholeBase()||access.slice().isEmpty(),"whole-base effect cannot carry an exact slice");
            access.slice().ifPresent(slice->{
                require(view.offset().value().isPresent()&&view.extent().value().isPresent(),"slice needs known view");
                var start=view.offset().value().orElseThrow();require(slice.offset().signum()>=0&&slice.extent().signum()>0&&slice.offset().compareTo(start)>=0&&slice.offset().add(slice.extent()).compareTo(start.add(view.extent().value().orElseThrow()))<=0,"file effect slice outside view");
            });
        });
    }
    private static StorageFacts.View exact(MemoryTarget t,EntryGobackAdmission.Context c) {
        require(!t.wholeBase()&&t.regional().isPresent(),"strong effect requires exact regional receiver");
        var access=t.regional().orElseThrow();var v=c.regionalStorage.views().get(access.view());var base=c.regionalStorage.bases().get(v.base());
        require(v.offset().value().isPresent()&&v.extent().value().filter(n->n.signum()>0).isPresent()&&v.codec().filter(StorageFacts.CODEC::equals).isPresent()
            &&base.extent().value().isPresent(),"strong file effect needs proven text bytes");
        return access.slice().map(s->new StorageFacts.View(v.node(),v.base(),new StorageFacts.Measure(Optional.of(s.offset()),List.of()),new StorageFacts.Measure(Optional.of(s.extent()),List.of()),v.codec(),v.provenance())).orElse(v);
    }
    private static void step(MemoryStep s,Use use,EffectOutcome outcome,Declaration file,EntryGobackAdmission.Context c,Map<OperandId,DataReference> references) {
        c.touch();c.provenance(s.provenance());target(s.destination(),use,c,references);s.source().ifPresent(t->target(t,use,c,references));
        require(s.role()==MemoryRole.FROM_RECORD||s.source().isEmpty(),"only FROM carries a source transfer");
        if(use.effects().orElseThrow().availability()==Availability.KNOWN)require(s.gapCodes().isEmpty(),"known effect plan contains unreported step gap");
        if(s.role()==MemoryRole.RECORD||s.role()==MemoryRole.FROM_RECORD)require(file==null||s.destination().data().isEmpty()||file.records().contains(s.destination().data().orElseThrow()),"record effect has wrong FILE owner");
        if(s.role()==MemoryRole.INTO)require(s.destination().reference().isPresent()&&use.surface().orElseThrow().operands().stream().anyMatch(o->o.role()==FileFacts.OperandRole.INTO&&o.references().contains(s.destination().reference().orElseThrow())),"INTO effect must retain receiving operand");
        if(s.kind()==MemoryKind.MAY_UNKNOWN)return;
        require(use.profile()==SyntaxProfile.N_LR&&file!=null&&file.kind()==FileFacts.expectedKind(use)&&(FileFacts.local(use)||file.assignment().sourceKind()==NameSource.ASSIGNMENT_NAME),"strong native effect needs N-LR file facts");
        require(s.gapCodes().isEmpty(),"strong effect cannot carry an unresolved proof gap");var destination=exact(s.destination(),c);
        if(s.kind()==MemoryKind.MUST_UNKNOWN) {
            require(s.role()==MemoryRole.FILE_STATUS||s.role()==MemoryRole.INTO,"MUST cannot be inferred from a file verb");
            require(s.source().isEmpty(),"unknown write is not a source copy");
            if(s.role()==MemoryRole.FILE_STATUS) {
                require(destination.extent().value().orElseThrow().equals(BigInteger.TWO),"primary file status is two characters");
                require(s.destination().data().isPresent()&&file.references().stream().anyMatch(r->r.role()==ReferenceRole.FILE_STATUS&&r.binding().selected().equals(s.destination().data())),"MUST status lacks declared status target");
            } else {
                require(outcome==EffectOutcome.SUCCESS,"INTO MUST belongs only to success");
                var records=use.effects().orElseThrow().outcomes().stream().filter(o->o.outcome()==EffectOutcome.SUCCESS).flatMap(o->o.steps().stream()).filter(t->t.role()==MemoryRole.RECORD).toList();
                require(!records.isEmpty(),"INTO MUST needs a published record area");
                for(var record:records) {
                    require(record.destination().regional().isPresent(),"INTO MUST needs record storage");
                    var source=c.regionalStorage.views().get(record.destination().regional().orElseThrow().view());
                    require(source!=null&&source.codec().filter(StorageFacts.CODEC::equals).isPresent()&&!source.base().equals(destination.base())&&c.regionalStorage.bases().get(source.base()).allocation().proved()&&c.regionalStorage.bases().get(destination.base()).allocation().proved(),"INTO MUST needs disjoint text record allocation");
                }
            }
        } else {
            require(outcome==null&&s.role()==MemoryRole.FROM_RECORD&&s.source().isPresent(),"precise FROM copy belongs before I/O");
            var source=exact(s.source().orElseThrow(),c);var sourceReference=s.source().orElseThrow().reference();
            require(sourceReference.isPresent()&&use.surface().orElseThrow().operands().stream().anyMatch(o->o.role()==FileFacts.OperandRole.FROM&&o.references().contains(sourceReference.orElseThrow())),"copy source lacks FROM operand");
            boolean separate=source.base().equals(destination.base())?source.offset().value().orElseThrow().add(source.extent().value().orElseThrow()).compareTo(destination.offset().value().orElseThrow())<=0
                ||destination.offset().value().orElseThrow().add(destination.extent().value().orElseThrow()).compareTo(source.offset().value().orElseThrow())<=0:
                c.regionalStorage.bases().get(source.base()).allocation().proved()&&c.regionalStorage.bases().get(destination.base()).allocation().proved();
            require(separate,"precise FROM copy needs disjoint bytes");
            require((s.kind()==MemoryKind.COPY_BYTES)==source.extent().value().equals(destination.extent().value()),"COPY/FIT width proof mismatch");
        }
    }
}
