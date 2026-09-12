package io.github.gustavo2358.lower.adapters.testing;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.github.gustavo2358.air.json.AirJson;
import io.github.gustavo2358.air.model.*;
import io.github.gustavo2358.lower.adapters.cli.CobolLower;
import io.github.gustavo2358.lower.adapters.sp.SpJsonDecoder;
import io.github.gustavo2358.lower.application.*;
import io.github.gustavo2358.lower.domain.SpInput;
import static io.github.gustavo2358.lower.testing.CallOracle.check;

public final class MoveDataIntegrationSuite {
    public static void run() throws Exception {
        var json = new ObjectMapper();
        ObjectNode wire;
        try (var in = MoveDataIntegrationSuite.class.getResourceAsStream("/sp/move-data/one-hop.json")) {
            if (in == null) throw new AssertionError("SP1.5 real source fixture missing");
            wire = (ObjectNode) json.readTree(in);
        }
        var decoder = new SpJsonDecoder(CobolLower.INPUT_LIMITS);
        var decoded = decoder.decode(json.writeValueAsBytes(wire));
        check(decoded instanceof SpJsonDecoder.Decoded, "closed SP1.5 reader: " + decoded);
        var input = ((SpJsonDecoder.Decoded) decoded).input();
        check(((SpInput.MoveFact) input.statements().get(0)).source() instanceof SpInput.LiteralSource, "literal variant");
        check(((SpInput.MoveFact) input.statements().get(1)).source() instanceof SpInput.DataReference, "DATA variant");
        var lowered = new CobolLowerer().lower(input, CobolLower.OPTIONS);
        check(lowered.status() == LoweringResult.Status.SUCCESS, "SP1.5 lowers: " + lowered.admission());
        var p = lowered.publication().orElseThrow();
        check(p.units().getFirst().sequences().stream().flatMap(s -> s.instructions().stream())
            .anyMatch(i -> i instanceof Operations.Assign a && a.value() instanceof Expressions.Read), "AIR transports real Read");
        var codec = new AirJson();
        check(p.equals(codec.decode(codec.encode(p))), "Read roundtrip using unchanged AIR codec");
        for (String discriminator : new String[]{"FUTURE", "LITERAL"}) {
            var bad = wire.deepCopy(); ((ObjectNode) bad.at("/statements/1/source")).put("variant", discriminator);
            check(decoder.decode(json.writeValueAsBytes(bad)) instanceof SpJsonDecoder.Rejected, "ambiguous/wrong source shape refused");
        }
        var bad = wire.deepCopy(); ((ObjectNode) bad.at("/statements/1/source")).remove("variant");
        check(decoder.decode(json.writeValueAsBytes(bad)) instanceof SpJsonDecoder.Rejected, "missing discriminant refused");
        System.out.println("MOVE_DATA_INTEGRATION=PASS");
    }
}
