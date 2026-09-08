package io.github.gustavo2358.lower.application;

import io.github.gustavo2358.air.model.Ids.PublicationId;
import io.github.gustavo2358.lower.domain.SpInput;
import io.github.gustavo2358.lower.testing.SpFixtures;
import io.github.gustavo2358.lower.testing.LoweringSuite;
import java.lang.reflect.Modifier;
import java.lang.reflect.Proxy;
import com.dynatrace.hash4j.hashing.Hashing;
import com.dynatrace.hash4j.hashing.HashStream128;
import com.dynatrace.hash4j.hashing.HashValues;
import java.util.List;
import java.util.Optional;

/** Independent identity observations; no expected computed by CanonicalRevision. */
public final class CompactIdentitySuite {
    private static int count;
    private CompactIdentitySuite() { }
    private static void check(boolean value, String message) {
        if (!value) throw new AssertionError("COMPACT_ID " + message);
        count++;
    }
    public static int run() {
        var input = SpFixtures.minimal();
        var id = CanonicalRevision.encode(input, Integer.MAX_VALUE).orElseThrow();
        check(id.matches("[0-9a-f]{32}"), "full XXH3-128 fixed 32 lowercase hex");
        // libxxhash 0.8.3 XXH3_128bits_withSeed(seed=0) of the original fixture's canonical-v1 facts, with xxh3-128-v1 domain.
        check(id.equals("a5fce8cae9328bc4007fc589e1989e37"), "independent domain and all-facts vector");
        check(CanonicalRevision.encode(SpFixtures.minimal(), 32).orElseThrow().equals(id), "independent equivalent instances and final bound");
        check(CanonicalRevision.encode(input, 31).isEmpty(), "final bound never truncates");
        check(CanonicalRevision.token(":😀\ud800").equals("4:003ad83dde00d800"), "SourceKey token UTF-16 unchanged including lone surrogate");
        check(CanonicalRevision.token("").equals("0:"), "SourceKey empty token unchanged");
        var a = withPolicy(input, "a", "bc"); var b = withPolicy(input, "ab", "c");
        check(!CanonicalRevision.encode(a, 32).equals(CanonicalRevision.encode(b, 32)), "adjacent strings have unambiguous lengths");
        check(!CanonicalRevision.encode(withPolicy(input, "\ud800", "x"), 32)
                .equals(CanonicalRevision.encode(withPolicy(input, "\ud801", "x"), 32)), "unpaired surrogates are not replaced by charset conversion");
        var longer = withPolicy(input, input.policy().policyId(), "x".repeat(100_000));
        var longResult = new EntryGobackLowerer().lower(longer,
                new LowerInput.Options(LoweringSuite.OPTIONS.admission(), 32, LoweringSuite.OPTIONS.validation()));
        check(longResult.status() == LoweringResult.Status.SUCCESS, "no hidden canonical blob limit");
        check(!longResult.publication().orElseThrow().id().localId().equals(id), "long semantic fact participates");
        var p = new EntryGobackLowerer().lower(input, LoweringSuite.OPTIONS).publication().orElseThrow();
        check(namespaces(p, p.id()) == 73, "every publication namespace in full AIR graph is consistent");
        knownVectors();
        try {
            // Observe the private encoder without a configurable production hashing API.
            var constructor = CanonicalRevision.class.getDeclaredConstructor(); constructor.setAccessible(true);
            var encoder = constructor.newInstance();
            var field = CanonicalRevision.class.getDeclaredField("stream"); field.setAccessible(true);
            var delegate = (HashStream128) field.get(encoder);
            long[] observed = {0, 0, 0};
            var spy = (HashStream128) Proxy.newProxyInstance(HashStream128.class.getClassLoader(),
                    new Class<?>[]{HashStream128.class}, (proxy, method, args) -> {
                        if (method.getName().equals("putBytes")) {
                            byte[] bytes = (byte[]) args[0]; int length = (Integer) args[2];
                            check(bytes.length <= 256 && length <= 256, "hash update buffer bounded at 256 bytes");
                            observed[0] += length; observed[1]++;
                        }
                        if (method.getName().equals("get")) observed[2]++;
                        return method.invoke(delegate, args);
                    });
            field.set(encoder, spy);
            var append = CanonicalRevision.class.getDeclaredMethod("append", String.class); append.setAccessible(true);
            var facts = CanonicalRevision.class.getDeclaredMethod("input", SpInput.class); facts.setAccessible(true);
            var finish = CanonicalRevision.class.getDeclaredMethod("finish"); finish.setAccessible(true);
            append.invoke(encoder, "minimal-entry-goback@1/AIR2/SP1.1/xxh3-128-v1/local-xxh3-128-v1/");
            facts.invoke(encoder, longer);
            check(finish.invoke(encoder).equals(longResult.publication().orElseThrow().id().localId()), "observed incremental hash agrees with public port");
            check(observed[0] == 5293L + 5 + 4L * (100_000 - 5), "all canonical bytes streamed");
            check(observed[1] > 1000 && observed[2] == 1, "bounded updates with exactly one finalization");
            check(delegate.getState().length < 2048, "hash4j stream state remains bounded for long fact");
        } catch (ReflectiveOperationException ex) { throw new AssertionError(ex); }
        for (var field : CanonicalRevision.class.getDeclaredFields()) {
            if (!Modifier.isStatic(field.getModifiers()))
                check(field.getType() != String.class && field.getType() != StringBuilder.class,
                        "encoder retains no full canonical text");
        }
        System.out.println("COMPACT_ID_TESTS=" + count);
        return count;
    }
    private static SpInput withPolicy(SpInput i, String id, String version) {
        var p = i.policy();
        return new SpInput(i.unit(), new SpInput.Policy(id, version, p.qualifyMode(), p.pgmnameMode(), p.dynamMode(), p.dllMode()),
                i.dataDeclarations(), i.statements(), i.structure(), i.gaps(), i.coverage(), i.entryInventory());
    }
    private static int namespaces(Object value, PublicationId expected) {
        if (value instanceof PublicationId id) { check(id.equals(expected), "typed full namespace agrees"); return 1; }
        if (value instanceof Optional<?> option) return option.map(v -> namespaces(v, expected)).orElse(0);
        if (value instanceof List<?> list) return list.stream().mapToInt(v -> namespaces(v, expected)).sum();
        if (value != null && value.getClass().isRecord()) {
            int total = 0;
            try {
                for (var component : value.getClass().getRecordComponents()) total += namespaces(component.getAccessor().invoke(value), expected);
            } catch (ReflectiveOperationException ex) { throw new AssertionError(ex); }
            return total;
        }
        return 0;
    }
    private static void knownVectors() {
        var stream = Hashing.xxh3_128(0L).hashStream();
        check(HashValues.toHexString(stream.get()).equals("99aa06d3014798d86001c324468d497f"), "reference XXH3-128 empty seed zero");
        stream.putByte((byte) 'a').putByte((byte) 'b').putByte((byte) 'c');
        check(HashValues.toHexString(stream.get()).equals("06b05ab6733a618578af5f94892f3950"), "reference XXH3-128 abc leading zero and canonical byte order");
        stream.reset();
        for (int i = 0; i < 1024; i++) stream.putByte((byte) i);
        check(HashValues.toHexString(stream.get()).equals("83885e853bb6640ca870f92984398d22"), "reference XXH3-128 long streaming vector");
    }
}
