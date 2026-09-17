package io.github.gustavo2358.lower.application;

import io.github.gustavo2358.lower.domain.FileFacts;
import io.github.gustavo2358.lower.domain.SpInput;
import java.util.*;
import static io.github.gustavo2358.lower.domain.FileFacts.*;
import static io.github.gustavo2358.lower.domain.SpInput.*;
import static io.github.gustavo2358.lower.application.Admission.*;

/** Validates published structure on either port. It does not resolve names or assign runtime outcomes. */
final class FileOperationAdmission {
    private FileOperationAdmission() { }
    static void validate(SpInput input, EntryGobackAdmission.Context c) {
        var statements=new HashMap<StatementId,StatementFact>();var references=new HashMap<OperandId,DataReference>();
        for(var statement:input.statements()) {
            statements.put(statement.header().id(),statement);
            if(statement instanceof OtherStatement o)for(var ref:o.knownReferences())references.put(ref.id(),ref);
        }
        var owners=new HashMap<DataId,Candidate>();for(var f:input.fileInventory().declarations())for(var record:f.records())owners.put(record,new Candidate(f.id(),f.owner()));
        for(var use:input.fileInventory().operations().uses()) {
            if(use.surface().isEmpty()) {
                require(c,Set.of(Command.OPEN,Command.READ,Command.CLOSE).contains(use.command()),"native command requires SP2.23 surface");continue;
            }
            var surface=use.surface().orElseThrow();var roles=new HashSet<FileFacts.OperandRole>();
            require(c,(use.command()==Command.START)==(surface.keyRelation()!=KeyRelation.UNSPECIFIED),"key relation belongs to START");
            require(c,!surface.explicitTerminator()||use.command()!=Command.OPEN&&use.command()!=Command.CLOSE,"OPEN/CLOSE has no explicit terminator");
            for(var operand:surface.operands()) {
                c.touch();c.provenance(operand.provenance());require(c,roles.add(operand.role()),"duplicate file operand role");
                boolean allowed=switch(operand.role()) {
                    case RECORD,FROM->use.command()==Command.WRITE||use.command()==Command.REWRITE;
                    case INTO->use.command()==Command.READ;case KEY->use.command()==Command.READ||use.command()==Command.START;
                    case ADVANCING->use.command()==Command.WRITE;
                };
                require(c,allowed,"operand role does not belong to command");
                require(c,operand.writtenValue().isPresent()==(operand.form()==OperandForm.LITERAL||operand.form()==OperandForm.MNEMONIC),"operand written value/form mismatch");
                require(c,operand.form()==OperandForm.REFERENCE||operand.references().isEmpty(),"nonreference operand has data refs");
                require(c,operand.form()!=OperandForm.REFERENCE||!operand.references().isEmpty()||!operand.gapCodes().isEmpty(),"unprojected reference needs gap");
                var refs=new HashSet<OperandId>();
                for(var ref:operand.references())require(c,ref.statement().equals(use.statement())&&references.containsKey(ref)&&refs.add(ref),"file operand outside owning observed statement");
                if(operand.role()==FileFacts.OperandRole.RECORD&&use.bindingStatus()==ResolutionStatus.RESOLVED&&!operand.references().isEmpty()) {
                    var ref=references.get(operand.references().getFirst());
                    if(ref!=null&&ref.binding().selected().isPresent())require(c,use.candidates().size()==1&&use.candidates().getFirst().equals(owners.get(ref.binding().selected().orElseThrow())),"record/file candidate ownership mismatch");
                }
            }
            require(c,roles.contains(FileFacts.OperandRole.RECORD)==(use.command()==Command.WRITE||use.command()==Command.REWRITE),"WRITE/REWRITE requires record operand");
            var options=new HashSet<Option>();
            for(var option:surface.options()) {
                c.touch();require(c,options.add(option),"duplicate file option");
                boolean allowed=switch(option) {
                    case NEXT->use.command()==Command.READ;
                    case REVERSED->use.command()==Command.OPEN&&use.mode()==OpenMode.INPUT;
                    case NO_REWIND->use.command()==Command.CLOSE||use.command()==Command.OPEN&&(use.mode()==OpenMode.INPUT||use.mode()==OpenMode.OUTPUT);
                    case LOCK,REEL,UNIT,FOR_REMOVAL->use.command()==Command.CLOSE;
                    case BEFORE_ADVANCING,AFTER_ADVANCING,PAGE->use.command()==Command.WRITE;
                };require(c,allowed,"option does not belong to command/mode");
            }
            require(c,!(options.contains(Option.BEFORE_ADVANCING)&&options.contains(Option.AFTER_ADVANCING)),"conflicting ADVANCING direction");
            require(c,!options.contains(Option.PAGE)||options.contains(Option.BEFORE_ADVANCING)||options.contains(Option.AFTER_ADVANCING),"PAGE without ADVANCING");
            var kinds=new HashSet<HandlerKind>();var bodies=new HashSet<StatementId>();
            for(var handler:surface.handlers()) {
                c.touch();c.provenance(handler.provenance());require(c,kinds.add(handler.kind()),"duplicate handler kind");
                boolean allowed=switch(handler.kind()) {
                    case AT_END,NOT_AT_END->use.command()==Command.READ;
                    case AT_END_OF_PAGE,NOT_AT_END_OF_PAGE->use.command()==Command.WRITE;
                    case INVALID_KEY,NOT_INVALID_KEY->Set.of(Command.READ,Command.WRITE,Command.REWRITE,Command.DELETE_RECORD,Command.START).contains(use.command());
                };require(c,allowed,"handler kind does not belong to command");
                for(var body:handler.statements()){c.touch();require(c,statements.containsKey(body)&&body.unit().equals(input.unit())&&!body.equals(use.statement())&&bodies.add(body),"invalid/duplicate handler body");}
            }
        }
    }
    private static void require(EntryGobackAdmission.Context c,boolean condition,String message){c.require(condition,Rule.PROFILE_FACT,"file-operation",null,message);}
}
