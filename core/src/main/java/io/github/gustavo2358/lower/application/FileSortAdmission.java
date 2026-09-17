package io.github.gustavo2358.lower.application;
import io.github.gustavo2358.lower.domain.*;
import java.util.*;
import static io.github.gustavo2358.lower.domain.FileFacts.*;
import static io.github.gustavo2358.lower.domain.SpInput.*;

/** Both ports must preserve complete participant and local procedure inventories. */
final class FileSortAdmission {
    private static final class Invalid extends RuntimeException {private static final long serialVersionUID=1L;Invalid(String s){super(s);}}
    private static void require(boolean test,String message){if(!test)throw new Invalid(message);}
    static void validate(SpInput input,EntryGobackAdmission.Context c) {
        try {
            var byStatement=new HashMap<StatementId,Map<Integer,Use>>();var files=new HashMap<Candidate,Declaration>();
            for(var d:input.fileInventory().declarations())files.put(new Candidate(d.id(),d.owner()),d);
            for(var u:input.fileInventory().operations().uses()) {
                byStatement.computeIfAbsent(u.statement(),ignored->new LinkedHashMap<>()).put(u.ordinal(),u);
                require(aggregate(u)==(u.role()!=Role.DIRECT),"role requires SORT/MERGE");
                require(u.role()!=Role.INPUT||u.mode()==OpenMode.INPUT,"input phase requires input mode");
                require(u.role()!=Role.OUTPUT||u.mode()==OpenMode.OUTPUT,"output phase requires output mode");
                for(var candidate:u.candidates())if(files.containsKey(candidate))require(files.get(candidate).kind()==expectedKind(u)||u.gapCodes().contains("FILE_KIND_NOT_PROVEN"),"FD/SD kind mismatch without gap");
            }
            var inventory=input.fileInventory().sorts();
            if(inventory.isEmpty()) {require(byStatement.values().stream().flatMap(m->m.values().stream()).noneMatch(FileFacts::aggregate),"sort inventory missing");return;}
            var sorts=inventory.orElseThrow();var seen=new HashSet<StatementId>();
            require(sorts.availability()!=Availability.UNAVAILABLE||sorts.plans().isEmpty(),"unavailable sort has plans");
            var children=new HashMap<StatementId,List<StatementId>>();for(var s:input.statements())s.header().containment().parent().ifPresent(p->children.computeIfAbsent(p,k->new ArrayList<>()).add(s.header().id()));
            for(var p:sorts.plans()) {
                c.touch();require(seen.add(p.statement())&&c.lookup(p.statement())!=null,"duplicate/absent sort statement");
                require(p.availability()==Availability.KNOWN?p.gapCodes().isEmpty()&&p.work()>=0:!p.gapCodes().isEmpty(),"sort availability/gaps mismatch");
                var uses=byStatement.getOrDefault(p.statement(),Map.of());require(!uses.isEmpty()&&uses.values().stream().allMatch(FileFacts::aggregate),"sort without uses");
                var listed=new HashSet<Integer>(p.inputs());require(listed.size()==p.inputs().size(),"duplicate input ordinal");
                for(var id:p.outputs())require(listed.add(id),"duplicate output ordinal");if(p.work()>=0)require(listed.add(p.work()),"duplicate work ordinal");
                require(listed.equals(uses.keySet()),"participant lost/added at sort boundary");
                for(var u:uses.values())require(u.role()==(u.ordinal()==p.work()?Role.WORK:p.inputs().contains(u.ordinal())?Role.INPUT:Role.OUTPUT),"participant role disagrees with phase");
                var command=uses.values().iterator().next().command();require(uses.values().stream().allMatch(u->u.command()==command),"mixed sort commands");
                var phases=EnumSet.noneOf(ProcedurePhase.class);
                for(var procedure:p.procedures()) {
                    require(phases.add(procedure.phase()),"duplicate procedure phase");
                    require(!procedure.gapCodes().isEmpty()||procedure.start().isPresent()&&procedure.end().isPresent(),"known procedure lacks endpoints");
                    for(var endpoint:List.of(procedure.start(),procedure.end()))endpoint.ifPresent(t->{require(t.id().unit().equals(input.unit()),"foreign procedure endpoint");c.provenance(t.referenceOrigin());c.provenance(t.paragraphOrigin());});
                    require(procedure.entry().equals(procedure.roots().isEmpty()?Optional.empty():Optional.of(procedure.roots().getFirst())),"procedure entry differs from first root");
                    require(new HashSet<>(procedure.roots()).size()==procedure.roots().size()&&new HashSet<>(procedure.completions()).size()==procedure.completions().size(),"duplicate procedure body/completion");
                    var members=new HashSet<StatementId>();var pending=new ArrayDeque<>(procedure.roots());
                    for(var root:procedure.roots())require(c.lookup(root)!=null&&c.lookup(root).header().containment().branch()==Branch.ROOT,"procedure roots must be source root statements");
                    while(!pending.isEmpty()){var id=pending.pop();require(c.lookup(id)!=null&&id.unit().equals(input.unit()),"foreign/absent procedure member");if(members.add(id))pending.addAll(children.getOrDefault(id,List.of()));}
                    require(members.containsAll(procedure.completions()),"completion outside procedure");
                    var froms=new HashSet<StatementId>(procedure.completions());
                    for(var link:procedure.links())require(members.contains(link.from())&&members.contains(link.to())&&!link.from().equals(link.to())&&froms.add(link.from()),"invalid/duplicate local procedure link");
                    require(p.gapCodes().containsAll(procedure.gapCodes()),"sort omits procedure gap");
                }
                if(p.availability()==Availability.KNOWN) {
                    require(p.inputs().isEmpty()==phases.contains(ProcedurePhase.INPUT)&&p.outputs().isEmpty()==phases.contains(ProcedurePhase.OUTPUT),"sort input/output methods conflict or missing");
                    if(command==Command.MERGE) {
                        require(!phases.contains(ProcedurePhase.INPUT)&&p.inputs().size()>=2,"MERGE cannot use INPUT PROCEDURE or fewer than two inputs");
                        var ids=new HashSet<Candidate>();for(var u:uses.values())require(u.bindingStatus()==ResolutionStatus.RESOLVED&&ids.add(u.candidates().getFirst()),"MERGE duplicate/unresolved participant");
                    }
                }
            }
            for(var entry:byStatement.entrySet())if(entry.getValue().values().stream().anyMatch(FileFacts::aggregate))require(seen.contains(entry.getKey()),"sort plan omitted");
        } catch(Invalid invalid){c.require(false,Admission.Rule.PROFILE_FACT,"file-sort",null,invalid.getMessage());}
    }
}
