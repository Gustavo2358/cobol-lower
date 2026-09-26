package io.github.gustavo2358.lower.adapters.sp;

import io.github.gustavo2358.lower.application.LowerInput;
import io.github.gustavo2358.lower.application.LoweringResult;
import java.nio.file.Path;
import java.util.Objects;

/** Driving adapter; physical input then the same transport-independent lowering port. */
public final class FileLowering {
    public sealed interface Result permits Lowered, PhysicalFailure { }
    public record Lowered(LoweringResult result, java.util.List<io.github.gustavo2358.lower.domain.SpInput> sources, byte[] sourceBytes) implements Result {
        public Lowered(LoweringResult result){this(result,java.util.List.of(),new byte[0]);}
        public Lowered {Objects.requireNonNull(result);sources=java.util.List.copyOf(sources);sourceBytes=sourceBytes.clone();}
        @Override public byte[] sourceBytes(){return sourceBytes.clone();}
    }
    public record PhysicalFailure(SpJsonDecoder.Diagnostic diagnostic) implements Result { }
    private final SpFileInput reader;
    private final LowerInput port;
    public FileLowering(SpFileInput reader, LowerInput port) { this.reader = Objects.requireNonNull(reader); this.port = Objects.requireNonNull(port); }
    public Result lower(Path path, LowerInput.Options options) {
        return lower(path,options,false);
    }
    public Result lower(Path path, LowerInput.Options options, boolean evidence) {
        byte[] bytes=new byte[0];
        if(evidence)try{bytes=java.nio.file.Files.readAllBytes(path);}catch(java.io.IOException ex){return new PhysicalFailure(new SpJsonDecoder.Diagnostic(SpJsonDecoder.Code.INPUT_ERROR,"physical","$ file I/O"));}
        final byte[] snapshot=bytes;
        return switch (evidence?reader.decodeDocument(snapshot):reader.readDocument(path)) {
            case CompilationJsonDecoder.Single decoded -> new Lowered(port.lower(decoded.input(), options),evidence?java.util.List.of(decoded.input()):java.util.List.of(),snapshot);
            case CompilationJsonDecoder.Compilation decoded -> new Lowered(new io.github.gustavo2358.lower.application.CompilationLowerer().lower(decoded.input(),options),evidence?decoded.input().units().stream().map(io.github.gustavo2358.lower.domain.SpCompilation.UnitProduct::product).toList():java.util.List.of(),snapshot);
            case CompilationJsonDecoder.Rejected failure -> new PhysicalFailure(failure.diagnostic());
        };
    }
}
