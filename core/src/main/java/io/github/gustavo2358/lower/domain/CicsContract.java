package io.github.gustavo2358.lower.domain;

import java.util.*;
import static io.github.gustavo2358.lower.domain.SpInput.*;

/** Consistency of published input facts only. Never resolves source text or executes an operation. */
final class CicsContract {
    private CicsContract() { }
    static void require(boolean condition, String message) {
        if (!condition) throw new IllegalArgumentException("CICS input: " + message);
    }
    static void handler(StatementHeader h,CicsHandlerAction action,CicsHandlerTargetKind kind,
            Optional<String> syntax,Optional<ResolutionStatus> binding,Optional<CicsHandlerLabelTarget> label,
            Optional<StatementId> entry,Optional<Provenance> entryOrigin,Optional<Provenance> origin,
            Optional<CallTarget> program,String raw,List<CicsOption> options) {
        boolean target=kind==CicsHandlerTargetKind.LABEL||kind==CicsHandlerTargetKind.PROGRAM;
        require(switch(action) {
            case ACTIVATE -> target;
            case CANCEL,RESET -> kind==CicsHandlerTargetKind.NONE;
            case UNAVAILABLE -> kind==CicsHandlerTargetKind.UNAVAILABLE;
        },"action/target kind");
        require((action==CicsHandlerAction.UNAVAILABLE)==(kind==CicsHandlerTargetKind.UNAVAILABLE),"unavailable kind");
        require(syntax.isPresent()==target&&origin.isPresent()==target,"operand syntax and origin presence");
        require(binding.isPresent()==(kind==CicsHandlerTargetKind.LABEL),"LABEL binding status");
        require(label.isPresent()==binding.filter(b->b==ResolutionStatus.RESOLVED).isPresent(),"selected LABEL identity");
        require(program.isEmpty()||kind==CicsHandlerTargetKind.PROGRAM,"PROGRAM target kind");
        program.ifPresent(p->{require(origin.filter(p.provenance()::equals).isPresent(),"PROGRAM operand provenance parity");
            require(p.id().statement().equals(h.id()),"PROGRAM operand owner");});
        require(entry.isPresent()==entryOrigin.isPresent()&&(entry.isEmpty()||label.isPresent()),"entry requires resolved LABEL and provenance");
        label.ifPresent(l->require(l.id().unit().equals(h.id().unit()),"LABEL owner"));
        entry.ifPresent(e->require(e.unit().equals(h.id().unit()),"entry owner"));
        require(h.coverage()!=CoverageStatus.MODELED,"handler execution remains partial");
        for(var o:options) {
            require(!o.name().isBlank()&&o.start()>=0&&o.end()>=o.start()&&o.end()<=raw.length(),"handler option offsets");
            o.reference().ifPresent(r->require(r.id().statement().equals(h.id()),"option owner"));
        }
        if(action==CicsHandlerAction.UNAVAILABLE)return;
        require(!options.isEmpty()&&options.getFirst().name().equals("ABEND"),"ABEND kind option");
        var seen=new HashSet<String>();CicsOption selector=null;
        for(var o:options) {
            require(seen.add(o.name()),"duplicate handler option");
            require(Set.of("ABEND","LABEL","PROGRAM","CANCEL","RESET","RESP","RESP2","NOHANDLE").contains(o.name()),"handler option shape");
            require(Set.of("LABEL","PROGRAM","RESP","RESP2").contains(o.name())==o.operand().isPresent()
                &&o.operand().filter(String::isBlank).isEmpty(),"handler option operand");
            if(Set.of("LABEL","PROGRAM","CANCEL","RESET").contains(o.name())) {
                require(selector==null,"one action selector");selector=o;
            }
        }
        String selected=selector==null?"CANCEL":selector.name();
        require(selected.equals(action==CicsHandlerAction.ACTIVATE?kind.name():action.name()),"action agrees with typed options");
        if(target)require(selector!=null&&syntax.equals(selector.operand()),"target agrees with typed operand");
    }
    static void event(CicsAbendEligibility eligibility,String raw,List<CicsOption> options,List<String> gaps) {
        var names=new HashSet<String>();boolean shape=true;int end=0;
        for(var o:options) {
            require(!o.name().isBlank()&&o.start()>=end&&o.end()>o.start()&&o.end()<=raw.length(),"event option offsets/order");end=o.end();
            require(o.reference().isEmpty(),"event publishes no operand binding");
            shape&=names.add(o.name())&&Set.of("CANCEL","NODUMP","ABCODE").contains(o.name());
            shape&=o.name().equals("ABCODE")?o.operand().filter(v->!v.isBlank()).isPresent():o.operand().isEmpty();
        }
        if(eligibility==CicsAbendEligibility.UNAVAILABLE) {
            require(gaps.stream().anyMatch(g->!g.isBlank()&&!g.equals("CICS_ABEND_DISPATCH_NOT_MODELED")),"unavailable cause");
        } else {
            require(shape,"qualified event option shape/duplicates");
            require((eligibility==CicsAbendEligibility.HANDLERS_BYPASSED)==names.contains("CANCEL"),"eligibility agrees with typed CANCEL");
            require(gaps.stream().allMatch(g->g.equals("CICS_ABEND_DISPATCH_NOT_MODELED")),"qualified event syntax gaps");
        }
    }
    static void command(StatementHeader header,CicsCommandKind kind,CicsCommandSyntaxStatus syntax,String raw,List<CicsOption> options,List<String> gaps) {
        var allowed=new HashSet<>(Set.of("RESP","RESP2","NOHANDLE"));
        if(kind==CicsCommandKind.SEND_TERMINAL)allowed.addAll(Set.of("FROM","LENGTH","ERASE"));
        if(kind==CicsCommandKind.SEND_MAP||kind==CicsCommandKind.RECEIVE_MAP)allowed.addAll(Set.of("MAP","MAPSET",kind==CicsCommandKind.SEND_MAP?"FROM":"INTO"));
        if(kind==CicsCommandKind.SEND_MAP)allowed.addAll(Set.of("CURSOR","ERASE","FREEKB"));
        var names=new HashSet<String>();int end=0;boolean shape=true;
        for(var o:options) {
            require(!o.name().isBlank()&&o.start()>=end&&o.end()>o.start()&&o.end()<=raw.length(),"command offsets/order");end=o.end();
            boolean flag=Set.of("NOHANDLE","CURSOR","ERASE","FREEKB").contains(o.name());
            shape&=names.add(o.name())&&allowed.contains(o.name())&&(flag?o.operand().isEmpty():o.operand().filter(v->!v.isBlank()).isPresent());
            require(o.reference().isEmpty()||o.operand().isPresent()&&!flag,"command reference has operand");
            o.reference().ifPresent(r->{require(r.id().statement().equals(header.id()),"command operand owner");
                require(r.role()==(Set.of("RESP","RESP2","INTO").contains(o.name())?OperandRole.WRITE:OperandRole.READ),"command operand role");});
        }
        require(header.coverage()!=CoverageStatus.MODELED,"command effects remain partial");
        if(syntax==CicsCommandSyntaxStatus.SUPPORTED) {
            require(shape&&(kind==CicsCommandKind.SYNCPOINT||names.contains(kind==CicsCommandKind.SEND_TERMINAL?"FROM":"MAP")),"supported command shape");
            require(gaps.stream().allMatch(g->g.equals("CICS_COMMAND_EFFECTS_NOT_MODELED")),"supported command syntax gaps");
        } else require(gaps.stream().anyMatch(g->!g.isBlank()&&!g.equals("CICS_COMMAND_EFFECTS_NOT_MODELED")),"unavailable command cause");
    }
    static void validateEntries(UnitKey unit,List<StatementFact> statements,List<DataFact> declarations) {
        if(statements.stream().noneMatch(CicsHandlerFact.class::isInstance))return;
        var data=new HashSet<DataId>();declarations.forEach(d->data.add(d.id()));
        var index=new HashMap<StatementId,StatementFact>();
        for(var s:statements)require(index.put(s.header().id(),s)==null,"duplicate statement identity");
        for(var s:statements)if(s instanceof CicsHandlerFact h) {
            require(h.header().id().unit().equals(unit),"handler source owner");
            var references=new ArrayList<DataReference>();
            if(h.programTarget().orElse(null) instanceof DataCallTarget p)references.add(p.reference());
            h.options().forEach(o->o.reference().ifPresent(references::add));
            for(var r:references) {
                var binding=r.binding();var candidates=new HashSet<>(binding.candidates());
                require(r.id().statement().equals(h.header().id())&&data.containsAll(candidates)
                    &&candidates.size()==binding.candidates().size(),"published DATA binding candidates");
                require(binding.status()==ResolutionStatus.RESOLVED
                    ?candidates.size()==1&&binding.selected().filter(candidates::contains).isPresent()
                    :binding.selected().isEmpty(),"DATA selected identity");
                require(binding.reason().isEmpty()||binding.candidateNames().size()==candidates.size(),"DATA candidate evidence");
                r.wholeItemAccess().ifPresent(w->require(binding.selected().equals(Optional.of(w.data())),"whole DATA binding"));
                r.logicalWholeItem().ifPresent(d->require(binding.selected().equals(Optional.of(d)),"logical DATA binding"));
            }
            h.targetEntry().ifPresent(id->{
                var target=index.get(id);
                require(target!=null&&target.header().containment().branch()==Branch.ROOT
                    &&target.header().containment().parent().isEmpty(),"entry is a published local root");
                require(h.entryOrigin().filter(target.header().provenance()::equals).isPresent(),"canonical entry provenance");
            });
        }
    }
}
