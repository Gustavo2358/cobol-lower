package io.github.gustavo2358.lower.application;

import static io.github.gustavo2358.lower.domain.SpInput.*;
import static io.github.gustavo2358.lower.application.Admission.*;

/** In-memory and wire consumers share the same structural checks. */
final class GoToAdmission {
    record CanonicalTarget(StatementId entry,Provenance origin) { }
    static void validate(GoToFact g,EntryGobackAdmission.Context c,java.util.Map<ProcedureId,CanonicalTarget> targets) {
        var h=g.header(); c.provenance(g.referenceOrigin());
        g.target().ifPresent(t -> { c.identity(t.id().unit(),t.id().handle(),"procedure",t.paragraphOrigin()); c.provenance(t.paragraphOrigin()); });
        g.entryOrigin().ifPresent(c::provenance);
        if(g.target().isPresent() && g.targetEntry().isPresent()) {
            var previous=targets.putIfAbsent(g.target().orElseThrow().id(),new CanonicalTarget(g.targetEntry().orElseThrow(),g.target().orElseThrow().paragraphOrigin()));
            c.require(previous==null || previous.entry().equals(g.targetEntry().orElseThrow()) && previous.origin().equals(g.target().orElseThrow().paragraphOrigin()),
                Rule.STRUCTURE,h.id().handle(),h.provenance(),"paragraph identity has one canonical executable entry");
        }
        c.require(g.targetEntry().isPresent()==g.entryOrigin().isPresent(),Rule.STRUCTURE,h.id().handle(),h.provenance(),"GO TO entry and provenance must agree");
        c.require(!g.gapCodes().isEmpty() || g.target().isPresent() && g.targetEntry().isPresent(),Rule.STRUCTURE,h.id().handle(),h.provenance(),"complete GO TO requires target identity and entry");
        g.targetEntry().ifPresent(id -> {
            var target=c.lookup(id);
            c.require(g.target().isPresent() && id.unit().equals(h.id().unit()) && target!=null,Rule.STRUCTURE,h.id().handle(),h.provenance(),"GO TO entry must be published in same unit");
            if(target!=null)c.require(target.header().containment().branch()==Branch.ROOT && g.entryOrigin().filter(target.header().provenance()::equals).isPresent(),Rule.STRUCTURE,h.id().handle(),h.provenance(),"GO TO entry provenance agrees with destination");
        });
    }
    static void validate(ConditionalGoToFact g,EntryGobackAdmission.Context c,java.util.Map<ProcedureId,CanonicalTarget> targets) {
        var h=g.header();c.provenance(g.selectorOrigin());
        g.selector().ifPresent(r->{
            CallAdmission.reference(r,h,new java.util.HashSet<>(),c);
            need(g,c,r.role()==OperandRole.READ&&r.provenance().equals(g.selectorOrigin()),"conditional selector is one source read");
        });
        need(g,c,!g.selectorInteger()||g.selector().filter(r->r.provenance().exact()
            &&r.wholeItemAccess().filter(w->c.data(w.data())!=null&&PerformCountAdmission.integer(c.data(w.data()))).isPresent()).isPresent(),"integer selector requires integer declaration");
        int ordinal=0;
        for(var d:g.destinations()) {
            c.touch();need(g,c,d.ordinal()==ordinal++,"contiguous ordered destination ordinals");
            c.provenance(d.referenceOrigin());d.procedureOrigin().ifPresent(c::provenance);d.entryOrigin().ifPresent(c::provenance);
            need(g,c,d.target().isPresent()==d.procedureOrigin().isPresent(),"destination identity and provenance paired");
            d.target().ifPresent(id->c.identity(id.unit(),id.handle(),"procedure",d.procedureOrigin().orElse(h.provenance())));
            need(g,c,d.targetEntry().isPresent()==d.entryOrigin().isPresent(),"destination entry and provenance paired");
            need(g,c,!d.gapCodes().isEmpty()||d.target().isPresent()&&d.targetEntry().isPresent(),"complete destination requires identity and entry");
            d.targetEntry().ifPresent(id->{
                var entry=c.lookup(id);
                need(g,c,d.target().isPresent()&&id.unit().equals(h.id().unit())&&entry!=null,"conditional destination is published in the same unit");
                if(entry!=null)need(g,c,entry.header().containment().branch()==Branch.ROOT&&d.entryOrigin().filter(entry.header().provenance()::equals).isPresent(),"conditional entry provenance agrees with statement");
                need(g,c,d.referenceOrigin().exact()&&d.procedureOrigin().filter(Provenance::exact).isPresent()
                    &&d.entryOrigin().filter(Provenance::exact).isPresent()&&h.provenance().exact(),"known destination requires exact local proof");
                if(d.target().isPresent()&&d.procedureOrigin().isPresent()) {
                    var target=new CanonicalTarget(id,d.procedureOrigin().orElseThrow());var previous=targets.putIfAbsent(d.target().orElseThrow(),target);
                    need(g,c,previous==null||previous.equals(target),"one canonical entry per procedure identity");
                }
            });
        }
        var next=g.normalContinuation();need(g,c,next.availability()!=ContinuationAvailability.NONE,"conditional GO TO may fall through");
        next.statement().ifPresent(id->{var entry=c.lookup(id);
            need(g,c,entry!=null&&next.provenance().equals(entry.header().provenance()),"conditional fallthrough provenance agrees with statement");
        });
        need(g,c,!g.gapCodes().isEmpty()||precise(g),"complete conditional GO TO requires all control proofs");
    }
    static boolean precise(ConditionalGoToFact g) {
        return g.header().containment().branch()!=Branch.UNKNOWN
            &&g.header().provenance().exact()&&g.selectorOrigin().exact()&&!g.destinations().isEmpty()
            &&g.destinations().stream().allMatch(d->d.targetEntry().isPresent()&&d.referenceOrigin().exact()
                &&d.procedureOrigin().filter(Provenance::exact).isPresent()&&d.entryOrigin().filter(Provenance::exact).isPresent())
            &&g.normalContinuation().statement().isPresent()&&g.normalContinuation().provenance().exact();
    }
    private static void need(ConditionalGoToFact g,EntryGobackAdmission.Context c,boolean value,String detail) {
        c.require(value,Rule.STRUCTURE,g.header().id().handle(),g.header().provenance(),detail);
    }
    static boolean precise(GoToFact g) {
        return g.header().containment().branch()!=Branch.UNKNOWN && g.header().provenance().exact()
            && g.referenceOrigin().exact() && g.target().filter(t->t.paragraphOrigin().exact()).isPresent()
            && g.targetEntry().isPresent() && g.entryOrigin().filter(Provenance::exact).isPresent();
    }
}
