package io.github.gustavo2358.lower.application;

import static io.github.gustavo2358.lower.domain.SpInput.*;
import static io.github.gustavo2358.lower.application.Admission.*;

/** In-memory and wire consumers share the same structural checks. */
final class GoToAdmission {
    static void validate(GoToFact g,EntryGobackAdmission.Context c,java.util.Map<ProcedureId,GoToFact> targets) {
        var h=g.header(); c.provenance(g.referenceOrigin());
        g.target().ifPresent(t -> { c.identity(t.id().unit(),t.id().handle(),"procedure",t.paragraphOrigin()); c.provenance(t.paragraphOrigin()); });
        g.entryOrigin().ifPresent(c::provenance);
        if(g.target().isPresent() && g.targetEntry().isPresent()) {
            var previous=targets.putIfAbsent(g.target().orElseThrow().id(),g);
            c.require(previous==null || previous.targetEntry().equals(g.targetEntry()) && previous.target().equals(g.target()),
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
    static boolean precise(GoToFact g) {
        return g.gapCodes().isEmpty() && g.header().containment().branch()!=Branch.UNKNOWN && g.header().provenance().exact()
            && g.referenceOrigin().exact() && g.target().filter(t->t.paragraphOrigin().exact()).isPresent()
            && g.targetEntry().isPresent() && g.entryOrigin().filter(Provenance::exact).isPresent();
    }
}
