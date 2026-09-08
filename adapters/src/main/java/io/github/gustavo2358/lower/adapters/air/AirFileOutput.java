package io.github.gustavo2358.lower.adapters.air;

import io.github.gustavo2358.air.json.AirJson;
import io.github.gustavo2358.air.model.Publication;
import java.io.IOException;
import java.nio.file.CopyOption;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Objects;

/** Encodes a whole Publication before touching the destination directory. */
public final class AirFileOutput {
    private final AirJson codec;
    private final FileOperations files;
    public AirFileOutput() { this(new AirJson(), new FileOperations()); }
    AirFileOutput(AirJson codec, FileOperations files) {
        this.codec = Objects.requireNonNull(codec);
        this.files = Objects.requireNonNull(files);
    }
    public void write(Publication publication, Path destination) throws IOException {
        Objects.requireNonNull(destination);
        byte[] bytes = codec.encode(publication);
        Path target = destination.toAbsolutePath();
        Path temporary = files.temporary(target.getParent());
        try {
            files.write(temporary, bytes);
            try {
                files.move(temporary, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException ex) {
                // Complete bytes, but this platform cannot promise an atomic replacement.
                files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException | RuntimeException | Error ex) {
            try { files.delete(temporary); }
            catch (IOException cleanup) { ex.addSuppressed(cleanup); }
            throw ex;
        }
    }
    /** Package-private physical seam: deterministic failures without mocking AIR or its codec. */
    static class FileOperations {
        Path temporary(Path directory) throws IOException {
            if (directory == null) throw new IOException("destination must name a file below a directory");
            return Files.createTempFile(directory, ".cobol-lower-air-", ".tmp");
        }
        void write(Path path, byte[] bytes) throws IOException { Files.write(path, bytes); }
        void move(Path source, Path target, CopyOption... options) throws IOException { Files.move(source, target, options); }
        void delete(Path path) throws IOException { Files.deleteIfExists(path); }
    }
}
