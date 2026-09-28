package io.github.gustavo2358.lower.adapters.source;

import java.io.*;
import java.nio.file.*;
import java.security.*;
import java.util.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.gustavo2358.lower.adapters.sp.FileLowering;
import io.github.gustavo2358.lower.application.*;
import io.github.gustavo2358.lower.source.QualifiedSourceDependencies;
import io.github.gustavo2358.lower.source.QualifiedSourceDependencies.*;

/** Physical snapshot correlation. Never opens any source provenance path. */
public final class QualifiedSourceFileOutput {
    public void write(FileLowering.Lowered lowered,LowerInput.Options options,Path air,Path destination) throws IOException {
        var bytes=lowered.sourceBytes();var document=new ObjectMapper().readTree(bytes);
        var units=new ArrayList<UnitEvidence>();
        for(var input:lowered.sources())units.add(lowered.result().admission().input().filter(input::equals).isPresent()
            ?QualifiedSourceProjection.project(input,lowered.result().admission()):QualifiedSourceProjection.admitAndProject(input,options));
        units.sort(Comparator.comparing((UnitEvidence u)->u.unit().compilationUnitId()).thenComparing(u->u.unit().structuralPath().toString()));
        var value=new QualifiedSourceDependencies("qualified-source-dependencies",units.stream().anyMatch(u->u.proofs().stream().anyMatch(p->java.util.Set.of("cics-pgmiderr-condition-event","cics-condition-registration").contains(p.rule())))?"1.6.0":units.stream().anyMatch(u->u.derivations().stream().anyMatch(d->d.authority().equals("ALTERNATE_ENTRY")))?"1.5.0":units.stream().anyMatch(u->u.nominalValues().filter(n->n.facts().authority().equals("NOMINAL_TEXT_SOURCE_V4")).isPresent())?"1.4.0":units.stream().anyMatch(u->u.nominalValues().filter(n->n.facts().authority().equals("NOMINAL_TEXT_SOURCE_V3")).isPresent())?"1.3.0":units.stream().anyMatch(u->u.nodes().stream().anyMatch(n->n.support().cause().equals("SOURCE_REENTRY_UNDEFINED")))?"1.2.0":units.stream().anyMatch(u->!u.nativeFiles().isEmpty()||u.proofs().stream().anyMatch(p->p.kind().equals("CONTROL_POSSIBILITY")))?"1.1.0":"1.0.0","cobol-lower/r7-source-state@1",
            new Document(document.path("schema").asText(),document.path("contractVersion").asText(),sha(bytes)),
            List.of(new AirCorrelation(lowered.result().publication().orElseThrow().id().localId(),sha(Files.readAllBytes(air)))),units);
        var encoded=new QualifiedSourceJson().encode(value);var absolute=destination.toAbsolutePath();
        var temporary=Files.createTempFile(absolute.getParent(),".source-evidence-",".tmp");
        try{Files.write(temporary,encoded);Files.move(temporary,absolute,StandardCopyOption.REPLACE_EXISTING,StandardCopyOption.ATOMIC_MOVE);}finally{Files.deleteIfExists(temporary);}
    }
    private static String sha(byte[] bytes){try{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));}catch(NoSuchAlgorithmException ex){throw new IllegalStateException(ex);}}
}
