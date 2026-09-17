package io.github.gustavo2358.lower.application;
import io.github.gustavo2358.lower.domain.*;
import java.util.*;
import static io.github.gustavo2358.lower.domain.FileFacts.*;
import static io.github.gustavo2358.lower.domain.SpInput.*;

/** Closed typed dispatch contract, validated for both wire and in-memory callers. */
final class FileControlAdmission {
    private static final class Invalid extends RuntimeException {private static final long serialVersionUID=1L;Invalid(String s){super(s);}}
    private static void require(boolean test,String message){if(!test)throw new Invalid(message);}
    static void validate(SpInput input,EntryGobackAdmission.Context c) {
        if(input.fileInventory().declaratives().isEmpty()&&input.fileInventory().operations().uses().stream().noneMatch(u->u.control().isPresent()))return;
        try {
            var declarations=new HashMap<String,Declarative>();var members=new HashSet<StatementId>();
            var fileIndex=new HashMap<Candidate,Declaration>();for(var f:input.fileInventory().declarations())fileIndex.put(new Candidate(f.id(),f.owner()),f);
            var childByParent=new HashMap<StatementId,List<StatementId>>();
            for(var s:input.statements())s.header().containment().parent().ifPresent(p->childByParent.computeIfAbsent(p,k->new ArrayList<>()).add(s.header().id()));
            for(var d:input.fileInventory().declaratives()) {
                c.touch();c.provenance(d.provenance());require(d.owner().equals(input.unit())&&declarations.putIfAbsent(d.id(),d)==null,"duplicate/foreign USE declaration");
                require(d.entry().equals(d.roots().isEmpty()?Optional.empty():Optional.of(d.roots().getFirst())),"USE entry is first root");
                require(new HashSet<>(d.roots()).size()==d.roots().size()&&new HashSet<>(d.completions()).size()==d.completions().size(),"duplicate USE body member");
                var body=new HashSet<StatementId>();var pending=new ArrayDeque<>(d.roots());
                for(var root:d.roots())require(c.lookup(root)!=null&&c.lookup(root).header().containment().branch()==Branch.ROOT,"USE roots are published root statements");
                while(!pending.isEmpty()){var id=pending.pop();require(id.unit().equals(input.unit())&&c.lookup(id)!=null,"USE body foreign/absent");if(body.add(id))pending.addAll(childByParent.getOrDefault(id,List.of()));}
                require(body.containsAll(d.completions()),"USE completions belong to body");
                for(var id:body)require(members.add(id),"USE bodies cannot overlap");
                for(var entry:input.entryInventory().entries())require(entry.start().statement().filter(body::contains).isEmpty(),"USE is not a primary executable prefix");
                require(d.kind()!=UseKind.DEBUGGING||!d.gapCodes().isEmpty(),"debugging cannot claim error dispatch");
                require(d.mode()==OpenMode.UNSPECIFIED||d.files().isEmpty(),"USE cannot be simultaneously file/mode selected");
                require(new HashSet<>(d.files()).size()==d.files().size(),"duplicate USE file reference");
                for(var file:d.files())require(fileIndex.containsKey(file)&&fileIndex.get(file).kind()!=Kind.SD,"USE file reference absent or SD");
            }
            var handlerMembers=new HashSet<StatementId>();
            for(var use:input.fileInventory().operations().uses())if(use.control().isPresent()) {
                c.touch();var p=use.control().orElseThrow();require(use.surface().isPresent()&&use.effects().isPresent(),"dispatch requires surface/effects");
                require(p.availability()!=Availability.KNOWN||p.gapCodes().isEmpty()&&!p.routes().isEmpty(),"known control needs routes/no gaps");
                require(p.availability()==Availability.KNOWN||!p.gapCodes().isEmpty(),"incomplete control needs gap");
                p.continuation().ifPresent(id->require(id.unit().equals(input.unit())&&c.lookup(id)!=null&&!id.equals(use.statement()),"file continuation foreign/absent/self"));
                var ordinary=PartialProgramAdmission.ordinaryNext(c.lookup(use.statement()));
                // An intrinsic paragraph completion has no next statement; that absence
                // does not contradict a separately published ordinary FILE continuation.
                // A known intrinsic successor still constrains the outcome plan.
                if(ordinary!=null&&ordinary.statement().isPresent())require(p.continuation().equals(ordinary.statement()),"file event continuation contradicts structural completion");
                var handlers=new EnumMap<HandlerKind,Handler>(HandlerKind.class);
                for(var h:use.surface().orElseThrow().handlers()) {
                    require(handlers.put(h.kind(),h)==null,"duplicate handler");
                    for(var id:h.statements()) {
                        require(c.lookup(id)!=null&&c.lookup(id).header().containment().parent().filter(use.statement()::equals).isPresent()&&c.lookup(id).header().containment().branch()==Branch.FILE_HANDLER,"handler direct member belongs to its I/O");handlerMembers.add(id);
                    }
                }
                if(p.availability()==Availability.UNAVAILABLE||p.availability()==Availability.INPUT_MISSING){require(p.routes().isEmpty(),"unavailable dispatch asserts no routes");continue;}
                boolean local=FileFacts.local(use),knownMode=use.command()==Command.OPEN||FileFacts.aggregate(use);
                var eligible=local?List.<Declarative>of():declarations.values();
                var selected=new HashSet<String>();
                boolean knownFile=use.bindingStatus()==ResolutionStatus.RESOLVED&&use.candidates().size()==1;
                for(var d:eligible)if(d.kind()==UseKind.AFTER_EXCEPTION&&knownFile&&d.files().contains(use.candidates().getFirst()))selected.add(d.id());
                boolean noUsePossible=false;
                if(selected.isEmpty()) {
                    for(var d:eligible)if(d.kind()==UseKind.AFTER_EXCEPTION&&d.mode()!=OpenMode.UNSPECIFIED&&(!knownMode||d.mode()==use.mode()))selected.add(d.id());
                    noUsePossible=!selected.isEmpty()&&!knownMode;
                }
                for(var d:eligible)if(d.kind()==UseKind.AFTER_EXCEPTION&&(!knownFile||d.gapCodes().contains("FILE_USE_BINDING_NOT_PROVEN"))) {selected.add(d.id());noUsePossible=true;}
                noUsePossible|=selected.isEmpty();
                if(noUsePossible&&!selected.isEmpty())require(p.availability()==Availability.PARTIAL,"unproved USE selection cannot claim known dispatch");
                var events=EnumSet.noneOf(ControlEvent.class);
                for(var r:p.routes()) {
                    require(events.add(r.event())&&!r.destinations().isEmpty(),"event duplicate/without destination");
                    require(r.effects()==switch(r.event()){case SUCCESS,END_OF_PAGE->EffectOutcome.SUCCESS;case END->EffectOutcome.END;case INVALID_KEY->EffectOutcome.INVALID_KEY;case OTHER_ERROR->EffectOutcome.OTHER_ERROR;},"event effects mismatch");
                    require(r.criticalExit()==(r.event()==ControlEvent.OTHER_ERROR),"unresolved critical exit cannot be omitted or invented for success");
                    for(var d:r.destinations()) {
                        require(d.handler().isPresent()==(d.kind()==DestinationKind.HANDLER)&&d.declarative().isPresent()==(d.kind()==DestinationKind.USE),"destination shape mismatch");
                        d.handler().ifPresent(h->{require(handlers.containsKey(h),"route names absent handler");require(switch(r.event()) {
                            case END->h==HandlerKind.AT_END;case INVALID_KEY->h==HandlerKind.INVALID_KEY;case END_OF_PAGE->h==HandlerKind.AT_END_OF_PAGE;
                            case SUCCESS->h==HandlerKind.NOT_AT_END||h==HandlerKind.NOT_INVALID_KEY||h==HandlerKind.NOT_AT_END_OF_PAGE;
                            case OTHER_ERROR->use.command()==Command.READ&&h==HandlerKind.NOT_AT_END;
                        },"handler/event mismatch");});
                        d.declarative().ifPresent(id->{var declaration=declarations.get(id);require(declaration!=null&&declaration.kind()==UseKind.AFTER_EXCEPTION,"unknown/nonerror USE route");require(r.event()!=ControlEvent.SUCCESS&&r.event()!=ControlEvent.END_OF_PAGE,"success cannot execute error USE");});
                    }
                    if(r.event()==ControlEvent.END&&handlers.containsKey(HandlerKind.AT_END)||r.event()==ControlEvent.INVALID_KEY&&handlers.containsKey(HandlerKind.INVALID_KEY))
                        require(r.destinations().size()==1&&r.destinations().getFirst().kind()==DestinationKind.HANDLER,"explicit handler takes precedence over USE");
                    else if(r.event()==ControlEvent.END||r.event()==ControlEvent.INVALID_KEY||r.event()==ControlEvent.OTHER_ERROR) {
                        var routed=new HashSet<String>();for(var d:r.destinations())d.declarative().ifPresent(routed::add);
                        require(routed.equals(selected),"USE selection contradicts published file/mode precedence");
                        require(r.destinations().stream().anyMatch(d->d.kind()!=DestinationKind.USE)==noUsePossible,"unproved/no USE alternative missing or invented");
                    }
                }
                require(events.contains(ControlEvent.SUCCESS)&&(local?!events.contains(ControlEvent.OTHER_ERROR):events.contains(ControlEvent.OTHER_ERROR)),"dispatch omits success/error event");
                if(use.command()==Command.RETURN)require(events.equals(EnumSet.of(ControlEvent.SUCCESS,ControlEvent.END)),"RETURN requires success/END only");
                if(use.command()==Command.RELEASE)require(events.equals(EnumSet.of(ControlEvent.SUCCESS)),"RELEASE has no READ outcomes");
                if(use.command()==Command.READ)require(events.contains(ControlEvent.END)||events.contains(ControlEvent.INVALID_KEY),"READ omits exceptional condition");
                if(use.command()==Command.WRITE||use.command()==Command.REWRITE||use.command()==Command.DELETE_RECORD||use.command()==Command.START)require(events.contains(ControlEvent.INVALID_KEY),"native key event omitted");
                if(handlers.containsKey(HandlerKind.AT_END_OF_PAGE)||handlers.containsKey(HandlerKind.NOT_AT_END_OF_PAGE))require(events.contains(ControlEvent.END_OF_PAGE)&&use.command()==Command.WRITE,"EOP must be a WRITE event");
                require(!events.contains(ControlEvent.END)||use.command()==Command.READ||use.command()==Command.RETURN,"EOF is a READ event");
            }
            for(var s:input.statements())if(s.header().containment().branch()==Branch.FILE_HANDLER)require(handlerMembers.contains(s.header().id()),"handler member omitted from surface");
        } catch(Invalid ex) {c.require(false,Admission.Rule.PROFILE_FACT,"file-control",null,ex.getMessage());}
    }
}
