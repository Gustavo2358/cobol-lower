package io.github.gustavo2358.lower.domain;

import java.util.*;

/** Closed nominal source facts; no source IO, parsing or runtime operations. */
public final class SourceFacts {
    private SourceFacts() {}
    public enum Kind { COPYBOOK, DCLGEN, SQL_INCLUDE }
    public enum Resolution { RESOLVED, UNRESOLVED, CYCLIC, IO_ERROR }
    public enum Authority { COPY_SYNTAX, CONFIGURED_DCLGEN, CONFIGURED_SQL_INCLUDE, BUILTIN_SQL_INCLUDE, UNKNOWN }
    public record Occurrence(String id,Kind kind,String name,String qualification,Resolution resolution,
            String artifact,Authority authority,SpInput.Provenance provenance) {
        public Occurrence {
            Objects.requireNonNull(id);Objects.requireNonNull(kind);Objects.requireNonNull(name);Objects.requireNonNull(qualification);
            Objects.requireNonNull(resolution);Objects.requireNonNull(artifact);Objects.requireNonNull(authority);Objects.requireNonNull(provenance);
            if(id.isBlank()||name.isBlank()||!name.equals(name.toUpperCase(Locale.ROOT))||!qualification.equals(qualification.toUpperCase(Locale.ROOT)))
                throw new IllegalArgumentException("Source dependency identity must be canonical");
            if((kind==Kind.DCLGEN)!=(authority==Authority.CONFIGURED_DCLGEN)||kind==Kind.DCLGEN&&(name.equals("SQLCA")||name.equals("SQLDA")))throw new IllegalArgumentException("DCLGEN authority absent");
            if((kind==Kind.COPYBOOK)!=(authority==Authority.COPY_SYNTAX))throw new IllegalArgumentException("COPY authority mismatch");
            if((resolution==Resolution.RESOLVED)==artifact.isBlank())throw new IllegalArgumentException("Resolved artifact absent");
            var p=provenance.original();
            if(p.file().isBlank()||p.startLine()<1||p.endLine()<p.startLine()||p.startColumn()<0||p.endColumn()<0)throw new IllegalArgumentException("Source coordinates absent");
        }
    }
    public record Inventory(SpInput.Availability availability,List<Occurrence> occurrences,List<String> gapCodes) {
        public Inventory {
            Objects.requireNonNull(availability);occurrences=List.copyOf(occurrences);gapCodes=List.copyOf(gapCodes);
            if(availability==SpInput.Availability.UNAVAILABLE&&!occurrences.isEmpty()||gapCodes.stream().anyMatch(String::isBlank))throw new IllegalArgumentException("Invalid source inventory absence/gap");
            var ids=new HashSet<String>();for(var occurrence:occurrences)if(!ids.add(occurrence.id()))throw new IllegalArgumentException("Duplicate source occurrence");
            if(availability==SpInput.Availability.KNOWN&&!gapCodes.isEmpty()||availability!=SpInput.Availability.KNOWN&&gapCodes.isEmpty())throw new IllegalArgumentException("Source inventory remainder mismatch");
            for(var f:occurrences)if((f.resolution()!=Resolution.RESOLVED||f.authority()==Authority.UNKNOWN)&&availability==SpInput.Availability.KNOWN)throw new IllegalArgumentException("Unresolved fact requires remainder");
        }
        public static Inventory unavailable(){return new Inventory(SpInput.Availability.UNAVAILABLE,List.of(),List.of("SOURCE_DEPENDENCIES_UNAVAILABLE"));}
    }
}
