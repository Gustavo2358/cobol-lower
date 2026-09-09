package io.github.gustavo2358.lower.adapters.air;

import io.github.gustavo2358.lower.adapters.cli.CobolLower;
import io.github.gustavo2358.lower.adapters.sp.SpJsonDecoder;
import io.github.gustavo2358.lower.adapters.testing.ScalarWireSuite;
import io.github.gustavo2358.lower.application.CobolLowerer;
import io.github.gustavo2358.lower.application.LoweringResult;
import java.nio.charset.StandardCharsets;

/** A single large, supported descriptive string must not restore the removed byte gate. */
public final class CapacitySafetySuite {
    private static void check(boolean value, String why) {
        if (!value) throw new AssertionError("CAPACITY_SAFETY " + why);
    }
    public static void run() throws Exception {
        var decoder = new SpJsonDecoder(CobolLower.INPUT_LIMITS);
        byte[] golden = ScalarWireSuite.resource("scalar-move-1.2.0.json");
        var small = ((SpJsonDecoder.Decoded) decoder.decode(golden)).input();
        // PICTURE is descriptive input in this profile, never interpreted by lowering.
        byte[] large = new String(golden, StandardCharsets.UTF_8)
            .replace("\"picture\":\"X(5)\"", "\"picture\":\"" + "X".repeat(32 * 1024 * 1024 + 1) + "\"")
            .getBytes(StandardCharsets.UTF_8);
        var decoded = decoder.decode(large);
        check(decoded instanceof SpJsonDecoder.Decoded, "large string decodes: " + decoded);
        var input = ((SpJsonDecoder.Decoded) decoded).input();
        check(input.dataDeclarations().getFirst().picture().orElseThrow().length() == 32 * 1024 * 1024 + 1, "string preserved in full");
        var port = new CobolLowerer(); var result = port.lower(input, CobolLower.OPTIONS);
        check(result.status() == LoweringResult.Status.SUCCESS, "large string stays in supported profile");
        check(result.publication().equals(port.lower(small, CobolLower.OPTIONS).publication()), "descriptive value does not change AIR meaning");
        System.out.println("CAPACITY_STRING bytes=" + large.length + " chars=" + input.dataDeclarations().getFirst().picture().orElseThrow().length() + " PASS");
    }
    public static void main(String[] args) throws Exception { run(); }
}
