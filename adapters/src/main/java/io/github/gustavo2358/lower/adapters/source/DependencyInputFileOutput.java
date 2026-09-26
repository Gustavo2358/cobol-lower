package io.github.gustavo2358.lower.adapters.source;

import java.io.*;
import java.nio.file.*;
import java.security.*;
import java.util.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.gustavo2358.air.model.Ids.*;
import io.github.gustavo2358.lower.adapters.air.AirFileOutput;
import io.github.gustavo2358.lower.adapters.sp.FileLowering;
import io.github.gustavo2358.lower.application.LowerInput;
import io.github.gustavo2358.lower.source.QualifiedSourceDependencies.*;

/** Explicit manifest output, separate from CLI and the shared AIR codec. */
public final class DependencyInputFileOutput {
    public void write(FileLowering.Lowered lowered,LowerInput.Options options,Path destination)throws IOException {
        var directory=destination.getParent();var temporary=Files.createTempDirectory(directory,".dependency-input-");
        try {
            var air=temporary.resolve("air.json");var source=temporary.resolve("source.json");var output=new AirFileOutput();var result=lowered.result();
            if(result.validation().orElseThrow().status()==io.github.gustavo2358.air.validation.ValidationResult.Status.INCOMPLETE_VALIDATION)output.writePartial(result.publication().orElseThrow(),air);
            else output.write(result.publication().orElseThrow(),air);
            new QualifiedSourceFileOutput().write(lowered,options,air,source);
            var airBytes=Files.readAllBytes(air);var sourceBytes=Files.readAllBytes(source);var evidence=new QualifiedSourceJson().decode(sourceBytes);
            var occurrences=new HashSet<StatementId>();for(var unit:evidence.units())for(var occurrence:unit.occurrences())if(occurrence.namespace().equals("PROGRAM"))occurrences.add(occurrence.id());
            var links=new ArrayList<Map<String,Object>>();
            for(var link:result.statements()) {
                var key=link.source().unit();
                var statement=new StatementId(new io.github.gustavo2358.lower.source.QualifiedSourceDependencies.UnitId(key.compilationUnitId(),key.structuralPath(),key.canonicalProgramName()),link.source().handle());
                if(occurrences.contains(statement))links.add(map("source",QualifiedSourceJson.value(statement),"operation",id(link.target()),"label",id(link.label()),"origin",id(link.origin())));
            }
            var mapper=new ObjectMapper();links.sort(Comparator.comparing(x->x.get("source").toString()+x.get("operation").toString()));
            String airName="air-"+sha(airBytes)+".json",sourceName="source-"+sha(sourceBytes)+".json";
            var manifest=map("schema","dependency-input","version","1.0.0","air",map("path",airName,"sha256",sha(airBytes)),
                "qualifiedSource",map("path",sourceName,"sha256",sha(sourceBytes)),"sourceSha256",evidence.source().sha256(),"correlations",links);
            if(destination.getFileName().toString().equals(airName)||destination.getFileName().toString().equals(sourceName))
                throw new IllegalArgumentException("manifest conflicts with snapshot path");
            // Content-addressed snapshots are installed first; the manifest is the commit point.
            install(air,directory.resolve(airName));install(source,directory.resolve(sourceName));
            var staged=temporary.resolve("manifest.json");Files.write(staged,mapper.writeValueAsBytes(manifest));
            Files.move(staged,destination,StandardCopyOption.REPLACE_EXISTING,StandardCopyOption.ATOMIC_MOVE);
        } finally {try(var files=Files.walk(temporary)){for(var path:files.sorted(Comparator.reverseOrder()).toList())Files.deleteIfExists(path);}}
    }
    private static void install(Path source,Path destination)throws IOException {
        if(Files.exists(destination)) {
            if(!Arrays.equals(Files.readAllBytes(source),Files.readAllBytes(destination)))throw new IOException("snapshot content disagreement");
        } else Files.move(source,destination,StandardCopyOption.ATOMIC_MOVE);
    }
    private static Map<String,Object> id(Id id) {
        var out=map("publication",id.publication().localId(),"localId",id.localId());
        if(id instanceof OperationId operation){out.put("domain","operation");out.put("unit",operation.unit().localId());}
        else if(id instanceof LabelId label){out.put("domain","label");out.put("unit",label.unit().localId());}
        else if(id instanceof OriginId)out.put("domain","origin");else throw new IllegalArgumentException("correlation identity");
        return out;
    }
    private static Map<String,Object> map(Object... fields){var out=new TreeMap<String,Object>();for(int n=0;n<fields.length;n+=2)out.put((String)fields[n],fields[n+1]);return out;}
    private static String sha(byte[] bytes){try{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));}catch(NoSuchAlgorithmException failure){throw new IllegalStateException(failure);}}
}
