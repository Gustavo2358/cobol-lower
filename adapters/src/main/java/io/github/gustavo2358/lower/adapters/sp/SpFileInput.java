package io.github.gustavo2358.lower.adapters.sp;

import io.github.gustavo2358.lower.adapters.transport.JsonFiles;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

/** Caller-owned path, complete read, closed resource. Source provenance is never opened. */
public final class SpFileInput {
    private final SpJsonDecoder decoder;
    private final CompilationJsonDecoder documentDecoder;
    public SpFileInput(SpJsonDecoder.Limits limits) {
        decoder = new SpJsonDecoder(Objects.requireNonNull(limits));
        documentDecoder=new CompilationJsonDecoder(limits);
    }
    public SpJsonDecoder.Result read(Path path) {
        if (path == null) return error();
        try (var input = JsonFiles.input(path)) {
            return decoder.decodeStream(input);
        } catch (IOException ex) { return error(); }
    }
    public CompilationJsonDecoder.Result decodeDocument(byte[] bytes){return documentDecoder.decode(bytes);}
    public CompilationJsonDecoder.Result readDocument(Path path){
        if(path==null)return new CompilationJsonDecoder.Rejected(error().diagnostic());
        try(var input=JsonFiles.input(path)){return documentDecoder.decodeStream(input);}
        catch(IOException e){return new CompilationJsonDecoder.Rejected(error().diagnostic());}
    }
    public record DocumentIdentity(String schema,String version,String sha256) {
        public DocumentIdentity {Objects.requireNonNull(schema);Objects.requireNonNull(version);Objects.requireNonNull(sha256);}
    }
    record DocumentRead(CompilationJsonDecoder.Result result,java.util.Optional<DocumentIdentity> identity) { }
    DocumentRead readSnapshot(Path path) {
        if(path==null)return new DocumentRead(new CompilationJsonDecoder.Rejected(error().diagnostic()),java.util.Optional.empty());
        try {
            var digest=java.security.MessageDigest.getInstance("SHA-256");var header=new String[2];
            try(var input=new java.security.DigestInputStream(JsonFiles.input(path),digest)) {
                var result=documentDecoder.decodeStream(input,(schema,version)->{header[0]=schema;header[1]=version;});
                if(result instanceof CompilationJsonDecoder.Rejected)return new DocumentRead(result,java.util.Optional.empty());
                return new DocumentRead(result,java.util.Optional.of(new DocumentIdentity(header[0],header[1],java.util.HexFormat.of().formatHex(digest.digest()))));
            }
        } catch(java.io.IOException failure){return new DocumentRead(new CompilationJsonDecoder.Rejected(error().diagnostic()),java.util.Optional.empty());}
        catch(java.security.NoSuchAlgorithmException failure){throw new IllegalStateException(failure);}
    }
    private static SpJsonDecoder.Rejected error() {
        return new SpJsonDecoder.Rejected(new SpJsonDecoder.Diagnostic(SpJsonDecoder.Code.INPUT_ERROR, "physical", "$ file I/O"));
    }
}
