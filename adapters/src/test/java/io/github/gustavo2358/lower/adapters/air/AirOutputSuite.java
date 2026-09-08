package io.github.gustavo2358.lower.adapters.air;

import io.github.gustavo2358.air.json.AirJson;
import io.github.gustavo2358.air.json.AirJsonException;
import io.github.gustavo2358.air.model.Publication;
import io.github.gustavo2358.air.validation.ValidationOptions;
import io.github.gustavo2358.lower.adapters.cli.CobolLower;
import io.github.gustavo2358.lower.adapters.sp.FileLowering;
import io.github.gustavo2358.lower.adapters.sp.SpFileInput;
import io.github.gustavo2358.lower.application.EntryGobackLowerer;
import io.github.gustavo2358.lower.application.LowerInput;
import io.github.gustavo2358.lower.application.LoweringResult;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.CopyOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/** Transport oracles; earlier independent semantic suites still own the meaning of the slice. */
public final class AirOutputSuite {
    private static int assertions;
    private AirOutputSuite() { }
    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError("AIR_OUTPUT " + message);
        assertions++;
    }
    private record Execution(int code, String diagnostic) { }
    private static FileLowering driver(LowerInput port) { return new FileLowering(new SpFileInput(CobolLower.INPUT_LIMITS), port); }
    private static Execution run(String[] args, LowerInput port, AirFileOutput output) {
        var bytes = new ByteArrayOutputStream();
        int code;
        try (var err = new PrintStream(bytes, true, StandardCharsets.UTF_8)) {
            code = CobolLower.run(args, err, driver(port), CobolLower.OPTIONS, output);
        }
        return new Execution(code, bytes.toString(StandardCharsets.UTF_8));
    }
    private static Execution run(Path input, Path output) {
        var bytes = new ByteArrayOutputStream();
        int code;
        try (var err = new PrintStream(bytes, true, StandardCharsets.UTF_8)) {
            code = CobolLower.run(new String[]{input.toString(), output.toString()}, err);
        }
        return new Execution(code, bytes.toString(StandardCharsets.UTF_8));
    }
    private static void failure(Execution result, int code, String diagnostic) {
        check(result.code() == code, "expected exit " + code + ", got " + result);
        check(result.diagnostic().contains(diagnostic), "typed diagnostic " + diagnostic + ": " + result);
        check(!result.diagnostic().contains("\tat "), "expected failure has no stack trace");
    }
    private static void noTemporary(Path dir) throws IOException {
        try (var files = Files.list(dir)) {
            check(files.noneMatch(p -> p.getFileName().toString().startsWith(".cobol-lower-air-")), "no temporary residue");
        }
    }
    private static void unchanged(Path path, byte[] expected) throws IOException {
        check(Arrays.equals(expected, Files.readAllBytes(path)), "old destination preserved on failure");
    }
    private static final class CountingFiles extends AirFileOutput.FileOperations {
        int calls;
        @Override Path temporary(Path directory) throws IOException { calls++; return super.temporary(directory); }
        @Override void write(Path path, byte[] bytes) throws IOException { calls++; super.write(path, bytes); }
        @Override void move(Path source, Path target, CopyOption... options) throws IOException { calls++; super.move(source, target, options); }
        @Override void delete(Path path) throws IOException { calls++; super.delete(path); }
    }
    private static void positive(Path input, Path dir, LoweringResult expected) throws Exception {
        Path output = dir.resolve("actual.air.json"), second = dir.resolve("second.air.json");
        var execution = run(input, output);
        check(execution.code() == 0, "real fixture CLI success: " + execution);
        check(execution.diagnostic().isEmpty(), "success has no error diagnostic");
        check(Files.isRegularFile(output), "success creates AIR file");
        byte[] bytes = Files.readAllBytes(output);
        var codec = new AirJson(); Publication publication = expected.publication().orElseThrow();
        check(Arrays.equals(bytes, codec.encode(publication)), "exact shared codec bytes");
        check(publication.id().localId().matches("[0-9a-f]{32}"), "compact complete XXH3-128 namespace");
        check(java.util.HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256").digest(bytes))
            .equals("46919c1429db4aa310e66fc9df9374eeba53fd98e50a287fdd005c17622f33ad"), "CP3 approved exact bytes preserved");
        IdentityRenamingOracle.verify(publication);
        System.out.println("COMPACT_AIR_BYTES=" + bytes.length);
        check(codec.decode(bytes).equals(publication), "round-trip entire real Publication");
        check(run(input, second).code() == 0 && Arrays.equals(bytes, Files.readAllBytes(second)), "deterministic repeated execution");
        Files.writeString(second, "old file");
        check(run(input, second).code() == 0 && Arrays.equals(bytes, Files.readAllBytes(second)), "existing output replaced exactly");
        noTemporary(dir);
    }
    private static void rejected(Path input, Path dir, LoweringResult valid) throws Exception {
        Path output = dir.resolve("rejected.air.json");
        byte[] old = new AirJson().encode(valid.publication().orElseThrow());
        var files = new CountingFiles(); var writer = new AirFileOutput(new AirJson(), files);
        for (var status : LoweringResult.Status.values()) {
            if (status == LoweringResult.Status.SUCCESS) continue;
            // Typed port failure fixture: every status is guarded, including future failures after admission.
            var result = new LoweringResult(status, valid.admission(), Optional.empty(), valid.validation(), List.of(), List.of(), List.of());
            for (boolean existing : List.of(false, true)) {
                if (existing) Files.write(output, old);
                failure(run(new String[]{input.toString(), output.toString()}, (sp, options) -> result, writer), 4, status.name());
                check(files.calls == 0, "non-SUCCESS never invokes physical output");
                if (existing) { unchanged(output, old); Files.delete(output); }
                else check(!Files.exists(output), "no new file on rejected lowering");
            }
        }
        // A real decoder/materialization/lower rejection, not only a fake port result.
        Path bad = dir.resolve("unsupported.json");
        String raw = Files.readString(input);
        check(raw.contains("\"parameterCount\":0"), "real fixture signature precondition");
        Files.writeString(bad, raw.replace("\"parameterCount\":0", "\"parameterCount\":1"));
        failure(run(bad, output), 4, "INVALID_INPUT");
        check(!Files.exists(output), "real rejection leaves no AIR output");
    }
    private static void physical(Path input, Path dir) throws Exception {
        Path output = dir.resolve("physical.air.json"), bad = dir.resolve("bad.json");
        failure(run(dir.resolve("missing.json"), output), 3, "INPUT_ERROR");
        for (String raw : List.of("{", Files.readString(input).replace("1.1.0", "9.0.0"))) {
            Files.writeString(bad, raw);
            failure(run(bad, output), 3, raw.equals("{") ? "INPUT_ERROR" : "UNSUPPORTED_CONTRACT");
            check(!Files.exists(output), "no output from bad physical SP");
        }
        Path impossible = dir.resolve("missing-parent/air.json");
        failure(run(input, impossible), 6, "AIR output");
        check(!Files.exists(impossible), "impossible destination cannot report success");
        failure(run(input, dir.getRoot()), 6, "AIR output");
        Path occupied = Files.createDirectory(dir.resolve("occupied"));
        Files.writeString(occupied.resolve("keep"), "old");
        failure(run(input, occupied), 6, "AIR output");
        check(Files.readString(occupied.resolve("keep")).equals("old"), "failed move preserves destination directory");
        noTemporary(dir);
    }
    private static void usage(Path input, Path dir) throws Exception {
        int[] calls = {0};
        LowerInput unexpected = (sp, options) -> { calls[0]++; throw new AssertionError("semantic work on usage error"); };
        for (String[] args : List.of(new String[]{}, new String[]{input.toString()}, new String[]{"a","b","c"},
                new String[]{"", dir.resolve("usage.air.json").toString()}, new String[]{"a\0", "b"})) {
            failure(run(args, unexpected, new AirFileOutput()), 2, "Usage: cobol-lower");
        }
        check(calls[0] == 0, "usage performs no semantic work");
        check(!Files.exists(dir.resolve("usage.air.json")), "usage has no output");
        try {
            run(new String[]{input.toString(),dir.resolve("bug.air.json").toString()},
                    (sp, options) -> { throw new IllegalStateException("unexpected lowering bug"); }, new AirFileOutput());
            throw new AssertionError("unexpected bug masked");
        } catch (IllegalStateException ex) { check(ex.getMessage().equals("unexpected lowering bug"), "unexpected bugs retain identity"); }
    }
    private static void publicationFailures(Path input, Path dir, Publication publication) throws Exception {
        Path output = dir.resolve("failed.air.json"); byte[] old = new AirJson().encode(publication);
        var files = new CountingFiles();
        var limited = new AirJson(new AirJson.Limits(1,128), ValidationOptions.defaults());
        String codecPath;
        try { limited.encode(publication); throw new AssertionError("limit precondition"); }
        catch (AirJsonException ex) {
            check(ex.code() == AirJsonException.Code.IMPLEMENTATION_LIMIT, "real typed codec failure"); codecPath = ex.path();
        }
        Files.write(output, old);
        var result = run(new String[]{input.toString(), output.toString()}, new EntryGobackLowerer(), new AirFileOutput(limited, files));
        failure(result, 5, "IMPLEMENTATION_LIMIT");
        check(result.diagnostic().contains(codecPath), "codec path preserved");
        check(files.calls == 0, "encode completes before any physical publication");
        unchanged(output, old); noTemporary(dir);
        Files.delete(output);
        failure(run(new String[]{input.toString(),output.toString()}, new EntryGobackLowerer(),new AirFileOutput(limited,files)),5,"IMPLEMENTATION_LIMIT");
        check(!Files.exists(output) && files.calls == 0, "codec failure without old file leaves nothing");
        Files.write(output, old);
        var interrupted = new AirFileOutput(new AirJson(), new AirFileOutput.FileOperations() {
            @Override void write(Path path, byte[] bytes) throws IOException {
                check(!path.equals(output), "write occurs in temporary file");
                check(path.getParent().equals(output.getParent()), "temporary file in destination directory");
                Files.write(path, Arrays.copyOf(bytes, 17)); throw new IOException("injected partial temporary write");
            }
        });
        failure(run(new String[]{input.toString(),output.toString()},new EntryGobackLowerer(),interrupted),6,"injected partial temporary write");
        unchanged(output, old); noTemporary(dir);
        Files.delete(output);
        failure(run(new String[]{input.toString(),output.toString()},new EntryGobackLowerer(),interrupted),6,"injected partial temporary write");
        check(!Files.exists(output), "partial temporary write creates no final output"); noTemporary(dir);
        int[] moves = {0};
        var fallback = new AirFileOutput(new AirJson(), new AirFileOutput.FileOperations() {
            @Override void move(Path source, Path target, CopyOption... options) throws IOException {
                moves[0]++;
                if (moves[0] == 1) {
                    check(Arrays.asList(options).contains(StandardCopyOption.ATOMIC_MOVE), "atomic move tried first");
                    throw new AtomicMoveNotSupportedException(source.toString(),target.toString(),"test platform");
                }
                check(Arrays.equals(options,new CopyOption[]{StandardCopyOption.REPLACE_EXISTING}), "explicit replace fallback");
                super.move(source,target,options);
            }
        });
        Files.writeString(output,"old"); fallback.write(publication,output);
        check(moves[0] == 2 && Arrays.equals(old,Files.readAllBytes(output)), "fallback exact bytes"); noTemporary(dir);
    }
    public static void main(String[] args) throws Exception {
        Path input = Path.of(AirOutputSuite.class.getResource("/sp/cobol-semantic-product.json").toURI());
        var result = driver(new EntryGobackLowerer()).lower(input, CobolLower.OPTIONS);
        check(result instanceof FileLowering.Lowered, "existing file composition used");
        var lowered = ((FileLowering.Lowered) result).result();
        check(lowered.status() == LoweringResult.Status.SUCCESS && lowered.publication().isPresent(), "real lowering success precondition");
        Path dir = Files.createTempDirectory("lower-air-output-");
        try {
            positive(input,dir,lowered); rejected(input,dir,lowered); physical(input,dir); usage(input,dir);
            publicationFailures(input,dir,lowered.publication().orElseThrow());
        } finally {
            try (var paths = Files.walk(dir)) {
                for (Path path : paths.sorted(java.util.Comparator.reverseOrder()).toList()) Files.delete(path);
            }
        }
        assertions += ScalarOutputSuite.run();
        System.out.println("LOWER_AIR_OUTPUT_TESTS=" + assertions);
    }
}
