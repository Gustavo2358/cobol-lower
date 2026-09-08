package io.github.gustavo2358.lower.application;

import io.github.gustavo2358.air.model.Origins;
import io.github.gustavo2358.air.model.Ids;
import io.github.gustavo2358.lower.domain.SpInput;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Optional;

/** Source evidence conversion only; logical filenames are never opened or normalized. */
final class SourceOrigins {
    private final Ids.PublicationId publication;
    private final LocalIds ids;
    private final LinkedHashMap<String, Origins.Artifact> artifacts = new LinkedHashMap<>();
    private final List<Origins.Origin> origins = new ArrayList<>();
    private final List<LoweringResult.Limitation> limitations = new ArrayList<>();
    SourceOrigins(Ids.PublicationId publication, LocalIds ids) { this.publication = publication; this.ids = ids; }
    List<Origins.Artifact> artifacts() { return List.copyOf(artifacts.values()); }
    List<Origins.Origin> origins() { return List.copyOf(origins); }
    List<LoweringResult.Limitation> limitations() { return List.copyOf(limitations); }
    Ids.OriginId derived(String local, List<Ids.OriginId> inputs, String rule) {
        var id = new Ids.OriginId(publication, local); origins.add(new Origins.Derived(id, inputs, rule)); return id;
    }
    Ids.OriginId source(String kind, String handle, SpInput.Provenance provenance) {
        String local = ids.id("origin", "source", kind, handle);
        List<Origins.IncludeFrame> includes = new ArrayList<>();
        for (var frame : provenance.includeChain()) {
            includes.add(new Origins.IncludeFrame(artifact("original", frame.includingFile()), artifact("original", frame.includedFile()),
                    frame.requestedName(), Optional.empty()));
            limitations.add(new LoweringResult.Limitation(LoweringResult.LimitCode.INCLUDE_SITE_UNAVAILABLE, local,
                    "Only includeLine was published; no complete AIR location. Raw frame retained in source evidence."));
        }
        var original = written(ids.id("origin", "original", kind, handle), "original", provenance.original(), includes, provenance.exact());
        var expanded = written(ids.id("origin", "expanded", kind, handle), "expanded", provenance.expanded(), includes, provenance.exact());
        return derived(local, List.of(original, expanded), "sp-provenance/original-expanded@1");
    }
    private Ids.ArtifactId artifact(String role, String file) {
        String key = ids.id("artifact", role, "publication", file);
        return artifacts.computeIfAbsent(key, k -> new Origins.Artifact(new Ids.ArtifactId(publication, k), file, Optional.empty())).id();
    }
    private Ids.OriginId written(String local, String role, SpInput.Location source, List<Origins.IncludeFrame> includes, boolean exact) {
        var id = new Ids.OriginId(publication, local);
        Optional<Origins.Location> location = Optional.empty();
        boolean available = source.startLine() >= 1 && source.endLine() >= 1
                && source.startColumn() >= 0 && source.endColumn() >= 0
                && (source.startLine() < source.endLine() || (source.startLine() == source.endLine() && source.startColumn() <= source.endColumn()));
        if (available) {
            location = Optional.of(new Origins.LineColumns(new Origins.Span(
                    new Origins.Position(BigInteger.valueOf(source.startLine()), BigInteger.valueOf(source.startColumn())),
                    new Origins.Position(BigInteger.valueOf(source.endLine()), BigInteger.valueOf(source.endColumn())),
                    BigInteger.ONE, BigInteger.ZERO, Origins.ColumnUnit.UNICODE_SCALAR, false)));
        } else limitations.add(new LoweringResult.Limitation(LoweringResult.LimitCode.COORDINATES_UNAVAILABLE, local,
                "Published coordinates do not form an AIR span under SP conventions; raw values retained, no precise span claimed."));
        origins.add(new Origins.Written(id, artifact(role, source.file()), location, includes, exact && available));
        return id;
    }
}
