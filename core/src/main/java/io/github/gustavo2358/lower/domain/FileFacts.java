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
    public enum Command { OPEN, READ, CLOSE }
    public enum OpenMode { INPUT, OUTPUT, IO, EXTEND, UNSPECIFIED }
    public enum SyntaxProfile { N_LR, UNSUPPORTED }
    public record Candidate(String id,UnitKey owner) {public Candidate {Objects.requireNonNull(id);Objects.requireNonNull(owner);}}
    public record Use(StatementId statement,int ordinal,Command command,OpenMode mode,SyntaxProfile profile,
            ResolutionStatus bindingStatus,List<Candidate> candidates,Provenance provenance,List<String> gapCodes) {
        public Use {Objects.requireNonNull(statement);Objects.requireNonNull(command);Objects.requireNonNull(mode);Objects.requireNonNull(profile);Objects.requireNonNull(bindingStatus);candidates=List.copyOf(candidates);Objects.requireNonNull(provenance);gapCodes=List.copyOf(gapCodes);}
    }
    public record Operations(Availability availability,List<Use> uses,List<String> gapCodes) {
        public Operations {Objects.requireNonNull(availability);uses=List.copyOf(uses);gapCodes=List.copyOf(gapCodes);}
        public static Operations unavailable(){return new Operations(Availability.UNAVAILABLE,List.of(),List.of("FILE_OPERATIONS_UNAVAILABLE"));}
    }
    public record Inventory(Availability availability, List<Declaration> declarations, List<String> gapCodes, Operations operations) {
        public Inventory(Availability availability,List<Declaration> declarations,List<String> gapCodes){this(availability,declarations,gapCodes,Operations.unavailable());}
        public Inventory { Objects.requireNonNull(availability); declarations=List.copyOf(declarations); gapCodes=List.copyOf(gapCodes);Objects.requireNonNull(operations); }
        public static Inventory unavailable() { return new Inventory(Availability.UNAVAILABLE,List.of(),List.of("FILE_INVENTORY_UNAVAILABLE")); }
    }
}
