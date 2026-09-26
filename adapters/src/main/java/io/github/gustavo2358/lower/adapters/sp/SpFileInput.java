package io.github.gustavo2358.lower.adapters.sp;

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
        try (var input = Files.newInputStream(path)) {
            byte[] bytes = input.readAllBytes();
            return decoder.decode(bytes);
        } catch (IOException ex) { return error(); }
    }
    public CompilationJsonDecoder.Result decodeDocument(byte[] bytes){return documentDecoder.decode(bytes);}
    public CompilationJsonDecoder.Result readDocument(Path path){
        if(path==null)return new CompilationJsonDecoder.Rejected(error().diagnostic());
        try(var input=Files.newInputStream(path)){return documentDecoder.decode(input.readAllBytes());}
        catch(IOException e){return new CompilationJsonDecoder.Rejected(error().diagnostic());}
    }
    private static SpJsonDecoder.Rejected error() {
        return new SpJsonDecoder.Rejected(new SpJsonDecoder.Diagnostic(SpJsonDecoder.Code.INPUT_ERROR, "physical", "$ file I/O"));
    }
}
