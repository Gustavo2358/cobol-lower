package io.github.gustavo2358.lower.application;

import io.github.gustavo2358.lower.domain.SpInput.DataFact;
import java.util.Collection;
import java.util.List;

/** Four bounded radix passes on already validated canonical numeric DATA handles: O(D), no names. */
final class ScalarDataOrder {
    private ScalarDataOrder() { }
    static List<DataFact> canonical(Collection<DataFact> data) {
        var values = data.toArray(DataFact[]::new); var scratch = new DataFact[values.length];
        for (int shift = 0; shift < 32; shift += 8) {
            int[] offsets = new int[256];
            for (var d : values) offsets[(number(d) >>> shift) & 255]++;
            int total = 0;
            for (int i = 0; i < offsets.length; i++) { int count = offsets[i]; offsets[i] = total; total += count; }
            for (var d : values) scratch[offsets[(number(d) >>> shift) & 255]++] = d;
            var previous = values; values = scratch; scratch = previous;
        }
        return List.of(values);
    }
    private static int number(DataFact data) { return Integer.parseInt(data.id().handle().substring(5)); }
}
