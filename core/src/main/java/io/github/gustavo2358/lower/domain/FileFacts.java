package io.github.gustavo2358.lower.domain;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import static io.github.gustavo2358.lower.domain.SpInput.*;

/** Public SP declaration facts. No source parsing, execution or external resolution. */
public final class FileFacts {
    private FileFacts() { }
    public enum Kind { FD, SD, UNKNOWN }
    public enum Organization { SEQUENTIAL, INDEXED, RELATIVE, UNSPECIFIED, UNSUPPORTED }
    public enum AccessMode { SEQUENTIAL, RANDOM, DYNAMIC, UNSPECIFIED, UNSUPPORTED }
    public enum Visibility { LOCAL, GLOBAL, EXTERNAL, CONFLICTING }
    public enum ReferenceRole { RECORD_KEY, ALTERNATE_RECORD_KEY, RELATIVE_KEY, FILE_STATUS, ADDITIONAL_STATUS }
    public enum NameSource { ASSIGNMENT_NAME, SORT_COMMENT, UNSUPPORTED, ABSENT }
    public record Assignment(Availability availability, String profile, String original, NameSource sourceKind,
            Optional<String> externalFileName, List<String> gapCodes) {
        public Assignment { Objects.requireNonNull(availability); Objects.requireNonNull(profile); Objects.requireNonNull(original);
            Objects.requireNonNull(sourceKind); Objects.requireNonNull(externalFileName); gapCodes=List.copyOf(gapCodes); }
    }
    public record Reference(ReferenceRole role, Binding binding, boolean duplicates, Provenance provenance) {
        public Reference { Objects.requireNonNull(role); Objects.requireNonNull(binding); Objects.requireNonNull(provenance); }
    }
    public record Declaration(String id, UnitKey owner, String logicalFile, Kind kind, Optional<Boolean> optional,
            Assignment assignment, Organization organization, AccessMode accessMode, Visibility visibility,
            List<DataId> records, List<Reference> references, List<Provenance> origins, List<String> gapCodes) {
        public Declaration { Objects.requireNonNull(id); Objects.requireNonNull(owner); Objects.requireNonNull(logicalFile); Objects.requireNonNull(kind);
            Objects.requireNonNull(optional); Objects.requireNonNull(assignment); Objects.requireNonNull(organization); Objects.requireNonNull(accessMode); Objects.requireNonNull(visibility);
            records=List.copyOf(records); references=List.copyOf(references); origins=List.copyOf(origins); gapCodes=List.copyOf(gapCodes); }
    }
    public enum Command { OPEN, READ, WRITE, REWRITE, DELETE_RECORD, START, CLOSE, RELEASE, RETURN, SORT, MERGE }
    public enum Role { DIRECT, WORK, INPUT, OUTPUT }
    public enum ProcedurePhase { INPUT, OUTPUT }
    public static boolean aggregate(Use use){return use.command()==Command.SORT||use.command()==Command.MERGE;}
    public static boolean local(Use use){return use.command()==Command.RELEASE||use.command()==Command.RETURN||use.role()==Role.WORK;}
    public static Kind expectedKind(Use use){return local(use)?Kind.SD:Kind.FD;}
    public record ProcedureLink(StatementId from,StatementId to) {public ProcedureLink {Objects.requireNonNull(from);Objects.requireNonNull(to);}}
    public record ProcedurePlan(ProcedurePhase phase,Optional<PerformTarget> start,Optional<PerformTarget> end,List<StatementId> roots,
            Optional<StatementId> entry,List<StatementId> completions,List<ProcedureLink> links,List<String> gapCodes) {
        public ProcedurePlan {Objects.requireNonNull(phase);Objects.requireNonNull(start);Objects.requireNonNull(end);roots=List.copyOf(roots);Objects.requireNonNull(entry);completions=List.copyOf(completions);links=List.copyOf(links);gapCodes=List.copyOf(gapCodes);}
    }
    public record SortPlan(StatementId statement,Availability availability,int work,List<Integer> inputs,List<Integer> outputs,List<ProcedurePlan> procedures,List<String> gapCodes) {
        public SortPlan {Objects.requireNonNull(statement);Objects.requireNonNull(availability);inputs=List.copyOf(inputs);outputs=List.copyOf(outputs);procedures=List.copyOf(procedures);gapCodes=List.copyOf(gapCodes);}
    }
    public record SortInventory(Availability availability,List<SortPlan> plans) {public SortInventory {Objects.requireNonNull(availability);plans=List.copyOf(plans);}}
    public enum OpenMode { INPUT, OUTPUT, IO, EXTEND, UNSPECIFIED }
    public enum SyntaxProfile { N_LR, UNSUPPORTED }
    public record Candidate(String id,UnitKey owner) {public Candidate {Objects.requireNonNull(id);Objects.requireNonNull(owner);}}
    public enum Option { NEXT, REVERSED, NO_REWIND, LOCK, REEL, UNIT, FOR_REMOVAL, BEFORE_ADVANCING, AFTER_ADVANCING, PAGE }
    public enum KeyRelation { UNSPECIFIED, EQUAL, GREATER, GREATER_OR_EQUAL }
    public enum OperandRole { RECORD, INTO, FROM, KEY, ADVANCING }
    public enum OperandForm { REFERENCE, LITERAL, MNEMONIC, UNSUPPORTED }
    public enum HandlerKind { AT_END, NOT_AT_END, INVALID_KEY, NOT_INVALID_KEY, AT_END_OF_PAGE, NOT_AT_END_OF_PAGE }
    public record Operand(OperandRole role,OperandForm form,List<OperandId> references,Optional<String> writtenValue,Provenance provenance,List<String> gapCodes) {
        public Operand {Objects.requireNonNull(role);Objects.requireNonNull(form);references=List.copyOf(references);Objects.requireNonNull(writtenValue);Objects.requireNonNull(provenance);gapCodes=List.copyOf(gapCodes);}
    }
    public record Handler(HandlerKind kind,List<StatementId> statements,Provenance provenance) {
        public Handler {Objects.requireNonNull(kind);statements=List.copyOf(statements);Objects.requireNonNull(provenance);}
    }
    public record Surface(List<Operand> operands,List<Option> options,KeyRelation keyRelation,boolean explicitTerminator,List<Handler> handlers) {
        public Surface {operands=List.copyOf(operands);options=List.copyOf(options);Objects.requireNonNull(keyRelation);handlers=List.copyOf(handlers);}
    }
    public enum EffectOutcome { SUCCESS, END, INVALID_KEY, OTHER_ERROR }
    public enum MemoryRole { RECORD, INTO, FROM_RECORD, FILE_STATUS, ADDITIONAL_STATUS, RELATIVE_KEY, RECORD_LENGTH }
    public enum MemoryKind { MAY_UNKNOWN, MUST_UNKNOWN, COPY_BYTES, FIT_TEXT }
    public record MemoryTarget(Optional<DataId> data,Optional<StorageFacts.Access> regional,boolean wholeBase,Optional<OperandId> reference,Provenance provenance) {
        public MemoryTarget {Objects.requireNonNull(data);Objects.requireNonNull(regional);Objects.requireNonNull(reference);Objects.requireNonNull(provenance);}
    }
    public record MemoryStep(MemoryRole role,MemoryKind kind,MemoryTarget destination,Optional<MemoryTarget> source,List<String> gapCodes,Provenance provenance) {
        public MemoryStep {Objects.requireNonNull(role);Objects.requireNonNull(kind);Objects.requireNonNull(destination);Objects.requireNonNull(source);gapCodes=List.copyOf(gapCodes);Objects.requireNonNull(provenance);}
    }
    public record OutcomeEffects(EffectOutcome outcome,List<MemoryStep> steps) {
        public OutcomeEffects {Objects.requireNonNull(outcome);steps=List.copyOf(steps);}
    }
    public record EffectPlan(Availability availability,List<MemoryTarget> ioReads,List<MemoryStep> before,List<OutcomeEffects> outcomes,
            boolean unknownReadBound,boolean unknownWriteBound,List<String> gapCodes) {
        public EffectPlan {Objects.requireNonNull(availability);ioReads=List.copyOf(ioReads);before=List.copyOf(before);outcomes=List.copyOf(outcomes);gapCodes=List.copyOf(gapCodes);}
    }
    public enum UseKind { AFTER_EXCEPTION, DEBUGGING }
    public enum ControlEvent { SUCCESS, END, INVALID_KEY, OTHER_ERROR, END_OF_PAGE }
    public enum DestinationKind { CONTINUE, HANDLER, USE }
    public record Declarative(String id,UnitKey owner,UseKind kind,boolean global,OpenMode mode,List<Candidate> files,List<StatementId> roots,Optional<StatementId> entry,List<StatementId> completions,List<String> gapCodes,Provenance provenance) {
        public Declarative {Objects.requireNonNull(id);Objects.requireNonNull(owner);Objects.requireNonNull(kind);Objects.requireNonNull(mode);files=List.copyOf(files);roots=List.copyOf(roots);Objects.requireNonNull(entry);completions=List.copyOf(completions);gapCodes=List.copyOf(gapCodes);Objects.requireNonNull(provenance);}
    }
    public record Destination(DestinationKind kind,Optional<HandlerKind> handler,Optional<String> declarative) {
        public Destination {Objects.requireNonNull(kind);Objects.requireNonNull(handler);Objects.requireNonNull(declarative);}
    }
    public record ControlRoute(ControlEvent event,EffectOutcome effects,List<Destination> destinations,boolean criticalExit) {
        public ControlRoute {Objects.requireNonNull(event);Objects.requireNonNull(effects);destinations=List.copyOf(destinations);}
    }
    public record ControlPlan(Availability availability,Optional<StatementId> continuation,List<ControlRoute> routes,List<String> gapCodes) {
        public ControlPlan {Objects.requireNonNull(availability);Objects.requireNonNull(continuation);routes=List.copyOf(routes);gapCodes=List.copyOf(gapCodes);}
    }
    public record Use(StatementId statement,int ordinal,Command command,OpenMode mode,SyntaxProfile profile,
            ResolutionStatus bindingStatus,List<Candidate> candidates,Provenance provenance,List<String> gapCodes,Optional<Surface> surface,Optional<EffectPlan> effects,Optional<ControlPlan> control,Role role) {
        public Use(StatementId statement,int ordinal,Command command,OpenMode mode,SyntaxProfile profile,ResolutionStatus bindingStatus,List<Candidate> candidates,Provenance provenance,List<String> gapCodes,Optional<Surface> surface,Optional<EffectPlan> effects,Optional<ControlPlan> control){this(statement,ordinal,command,mode,profile,bindingStatus,candidates,provenance,gapCodes,surface,effects,control,Role.DIRECT);}
        public Use(StatementId statement,int ordinal,Command command,OpenMode mode,SyntaxProfile profile,ResolutionStatus bindingStatus,List<Candidate> candidates,Provenance provenance,List<String> gapCodes,Optional<Surface> surface,Optional<EffectPlan> effects) {
            this(statement,ordinal,command,mode,profile,bindingStatus,candidates,provenance,gapCodes,surface,effects,Optional.empty());
        }
        public Use(StatementId statement,int ordinal,Command command,OpenMode mode,SyntaxProfile profile,ResolutionStatus bindingStatus,List<Candidate> candidates,Provenance provenance,List<String> gapCodes,Optional<Surface> surface) {
            this(statement,ordinal,command,mode,profile,bindingStatus,candidates,provenance,gapCodes,surface,Optional.empty());
        }
        public Use(StatementId statement,int ordinal,Command command,OpenMode mode,SyntaxProfile profile,ResolutionStatus bindingStatus,List<Candidate> candidates,Provenance provenance,List<String> gapCodes) {
            this(statement,ordinal,command,mode,profile,bindingStatus,candidates,provenance,gapCodes,Optional.empty());
        }
        public Use {Objects.requireNonNull(statement);Objects.requireNonNull(command);Objects.requireNonNull(mode);Objects.requireNonNull(profile);Objects.requireNonNull(bindingStatus);candidates=List.copyOf(candidates);Objects.requireNonNull(provenance);gapCodes=List.copyOf(gapCodes);Objects.requireNonNull(surface);Objects.requireNonNull(effects);Objects.requireNonNull(control);Objects.requireNonNull(role);}
    }
    public record Operations(Availability availability,List<Use> uses,List<String> gapCodes) {
        public Operations {Objects.requireNonNull(availability);uses=List.copyOf(uses);gapCodes=List.copyOf(gapCodes);}
        public static Operations unavailable(){return new Operations(Availability.UNAVAILABLE,List.of(),List.of("FILE_OPERATIONS_UNAVAILABLE"));}
    }
    public record Inventory(Availability availability, List<Declaration> declarations, List<String> gapCodes, Operations operations,List<Declarative> declaratives,Optional<SortInventory> sorts) {
        public Inventory(Availability availability,List<Declaration> declarations,List<String> gapCodes,Operations operations,List<Declarative> declaratives){this(availability,declarations,gapCodes,operations,declaratives,Optional.empty());}
        public Inventory(Availability availability,List<Declaration> declarations,List<String> gapCodes,Operations operations){this(availability,declarations,gapCodes,operations,List.of());}
        public Inventory(Availability availability,List<Declaration> declarations,List<String> gapCodes){this(availability,declarations,gapCodes,Operations.unavailable());}
        public Inventory { Objects.requireNonNull(availability); declarations=List.copyOf(declarations); gapCodes=List.copyOf(gapCodes);Objects.requireNonNull(operations);declaratives=List.copyOf(declaratives);Objects.requireNonNull(sorts); }
        public static Inventory unavailable() { return new Inventory(Availability.UNAVAILABLE,List.of(),List.of("FILE_INVENTORY_UNAVAILABLE")); }
    }
}
