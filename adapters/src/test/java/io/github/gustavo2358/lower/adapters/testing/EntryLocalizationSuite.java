package io.github.gustavo2358.lower.adapters.testing;

import io.github.gustavo2358.air.model.Evidence;
import io.github.gustavo2358.air.model.Operations;
import io.github.gustavo2358.lower.adapters.cli.CobolLower;
import io.github.gustavo2358.lower.adapters.sp.SpJsonDecoder;
import io.github.gustavo2358.lower.application.CobolLowerer;
import io.github.gustavo2358.lower.application.LoweringResult;
import io.github.gustavo2358.lower.domain.SpInput;
import java.util.List;
import java.util.Objects;
import static io.github.gustavo2358.lower.testing.CallOracle.check;

/** Real SP1.9 frontend fixtures; no interpretation of source or COPY in the lower. */
public final class EntryLocalizationSuite {
    public static void run() throws Exception {
        for (var name : List.of("leading-entry", "data-independent", "data-unknown", "procedure-unsafe")) {
            try (var in = EntryLocalizationSuite.class.getResourceAsStream("/sp/entry-localization/" + name + ".json")) {
                var decoded = new SpJsonDecoder(CobolLower.INPUT_LIMITS).decode(Objects.requireNonNull(in).readAllBytes());
                check(decoded instanceof SpJsonDecoder.Decoded, "SP1.9 decoder admission: " + decoded);
                var input = ((SpJsonDecoder.Decoded) decoded).input();
                var result = new CobolLowerer().lower(input, CobolLower.OPTIONS);
                if (name.equals("procedure-unsafe")) {
                    check(result.publication().isEmpty(), "unavailable executable region cannot silently disappear");
                    check(result.status() == LoweringResult.Status.BLOCKED_LOWERING, "explicit missing-start blocker");
                    continue;
                }
                check(result.status() == LoweringResult.Status.SUCCESS, "known explicit start admits partial input: " + result);
                var publication = result.publication().orElseThrow();
                check(publication.coverage().inventory() == Evidence.InventoryStatus.PARTIAL, "input is never complete");
                check(publication.units().getFirst().sequences().stream().filter(s -> s.terminator() instanceof Operations.Invoke).count() == 1,
                        "the CALL reaches AIR");
                if (name.startsWith("data-")) {
                    check(input.entryInventory().status() == SpInput.InventoryStatus.INPUT_MISSING, "input gap survives decoder");
                    check(input.entryInventory().entries().getFirst().gaps().stream().anyMatch(g -> g.code().equals("UNRESOLVED_COPY")), "COPY remains explicit");
                    check(input.storageIndependence().orElseThrow().availability() == SpInput.Availability.INPUT_MISSING, "no fabricated storage independence");
                    check(!publication.uncertainties().isEmpty(), "partiality survives AIR");
                }
            }
        }
        // Keep the historical 1.8 decoder and wire semantics available.
        PartialIntegrationSuite.fixture("p1");
        System.out.println("ENTRY_LOCALIZED_INPUT=PASS");
    }
}
