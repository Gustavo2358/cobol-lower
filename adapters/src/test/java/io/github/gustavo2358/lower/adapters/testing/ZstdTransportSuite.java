package io.github.gustavo2358.lower.adapters.testing;

import io.github.gustavo2358.lower.adapters.cli.*;
import io.github.gustavo2358.lower.adapters.transport.JsonFiles;
import java.nio.file.*;
import java.util.*;
import java.io.*;
import java.security.*;

/** Tests real producer snapshots, complete lower output, and physical rejection. */
public final class ZstdTransportSuite {
    private static void need(boolean value,String why){if(!value)throw new AssertionError(why);}
    private static String sha(byte[] bytes)throws Exception{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));}
    public static void main(String[] args)throws Exception {
        Path dir=Files.createTempDirectory("lower-zstd-");
        try {
            byte[] fixture=QualifiedSourceSuite.fixture("ordinary");
            Path sp=dir.resolve("sp.json.zst"), plain=dir.resolve("plain.json"), air=dir.resolve("air.json.zst"), source=dir.resolve("source.json.zst"), bundle=dir.resolve("bundle.json.zst");
            JsonFiles.write(sp,sp,fixture);
            var errors=new ByteArrayOutputStream();var err=new PrintStream(errors);
            need(CobolLower.run(new String[]{sp.toString(),plain.toString()},err)==0,"compressed SP: "+errors);
            need(CobolLower.run(new String[]{sp.toString(),air.toString(),"--source-evidence",source.toString()},err)==0,"AIR/source: "+errors);
            need(Arrays.equals(Files.readAllBytes(plain),JsonFiles.read(air)),"AIR bytes changed");
            var mapper=new com.fasterxml.jackson.databind.ObjectMapper();
            var evidence=mapper.readTree(JsonFiles.read(source));
            need(evidence.toString().contains(sha(Files.readAllBytes(plain))),"AIR logical digest absent");
            need(evidence.toString().contains(sha(fixture)),"SP logical digest absent");
            need(CobolDependencyInput.run(new String[]{sp.toString(),bundle.toString()},err)==0,"bundle: "+errors);
            byte[] first=Files.readAllBytes(bundle);
            var manifest=mapper.readTree(JsonFiles.read(bundle));
            for(String member:List.of("air","qualifiedSource")) {
                var ref=manifest.path(member);String name=ref.path("path").asText();
                need(name.endsWith(".json.zst"),"uncompressed bundle member");
                need(sha(JsonFiles.read(dir.resolve(name))).equals(ref.path("sha256").asText()),"logical hash "+member);
            }
            need(CobolDependencyInput.run(new String[]{sp.toString(),bundle.toString()},err)==0,"repeat bundle");
            need(Arrays.equals(first,Files.readAllBytes(bundle)),"bundle nondeterministic");
            byte[] saved=Files.readAllBytes(air),frame=Files.readAllBytes(sp);
            for(byte[] broken:List.of(Arrays.copyOf(frame,frame.length-1),fixture,new byte[0])) {
                Files.write(sp,broken);
                need(CobolLower.run(new String[]{sp.toString(),air.toString()},err)==CobolLower.INPUT,"bad frame accepted");
                need(Arrays.equals(saved,Files.readAllBytes(air)),"failure overwrote output");
            }
            System.out.println("PASS Zstandard SP/AIR/source/bundle, logical hashes, determinism and rejection");
        } finally {try(var paths=Files.walk(dir)){for(var p:paths.sorted(Comparator.reverseOrder()).toList())Files.delete(p);}}
    }
}
