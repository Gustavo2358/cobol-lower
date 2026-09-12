package io.github.gustavo2358.lower.adapters.testing;

import java.util.List;

/** Fixed focal wire/codec contracts, without historical or mutation campaign entrypoints. */
public final class FastAdapterSuite {
    private FastAdapterSuite() { }
    public static void main(String[] args) throws Exception {
        int cases = 0;
        for (String name : List.of("dynamic-x8", "literal")) {
            try (var stream = FastAdapterSuite.class.getResourceAsStream("/sp/cp6/" + name + ".json")) {
                if (stream == null) throw new AssertionError("missing W1 fixture");
                boolean computed = name.equals("dynamic-x8");
                CallIntegrationSuite.positive(stream.readAllBytes(), computed ? List.of("PROGA   ") : List.of(), computed, "PROGA");
                cases++;
            }
        }
        if (cases != 2) throw new AssertionError("fixed W1 focal cases absent");
        IfIntegrationSuite.focal();
        MoveDataIntegrationSuite.run(); PerformIntegrationSuite.run(); MultiCallIntegrationSuite.run();
        System.out.println("LOWER_FAST_ADAPTER_CASES=" + (cases + 2));
    }
}
