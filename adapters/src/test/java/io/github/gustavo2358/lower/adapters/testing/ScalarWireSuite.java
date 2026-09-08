package io.github.gustavo2358.lower.adapters.testing;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.node.*;
import io.github.gustavo2358.air.json.AirJson;
import io.github.gustavo2358.lower.adapters.sp.SpJsonDecoder;
import io.github.gustavo2358.lower.testing.ScalarSuite;
import io.github.gustavo2358.lower.domain.SpInput;
import java.security.MessageDigest;
import java.util.*;

/** Real upstream fixture, strict version boundaries and independent relational AIR observations. */
public final class ScalarWireSuite {
    private static final ObjectMapper JSON = new ObjectMapper();
    private static final SpJsonDecoder DECODER = new SpJsonDecoder(new SpJsonDecoder.Limits(1_000_000, 64, 100_000));
    private static int checks;
    private ScalarWireSuite() { }
    private static void check(boolean condition, String why) { if (!condition) throw new AssertionError("SCALAR_WIRE " + why); checks++; }
    public static byte[] resource(String file) throws Exception {
        try (var stream = ScalarWireSuite.class.getResourceAsStream("/sp/" + file)) { return stream.readAllBytes(); }
    }
    public static SpInput decode(byte[] bytes) {
        var result = DECODER.decode(bytes);
        check(result instanceof SpJsonDecoder.Decoded, "4C real SP 1.2.0 must decode: " + result);
        check(((SpJsonDecoder.Decoded)result).unsupportedVariants().isEmpty(), "typed MOVE consumed");
        return ((SpJsonDecoder.Decoded)result).input();
    }
    private static void physical(ObjectNode node, String why) throws Exception {
        var result = DECODER.decode(JSON.writeValueAsBytes(node));
        check(result instanceof SpJsonDecoder.Rejected r && r.diagnostic().code() == SpJsonDecoder.Code.INPUT_ERROR, "strict physical rejection " + why);
    }
    private static ObjectNode object(ObjectNode root, String pointer) { return (ObjectNode)root.at(pointer); }
    public static int run() throws Exception {
        checks = 0; byte[] raw = resource("scalar-move-1.2.0.json");
        check(raw.length == 5177 && HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(raw)).equals("468e3207f578e428ace89a311eadbd6e27c670331b739675b761479352adc7af"), "merged upstream fixture digest");
        var input = decode(raw); var result = ScalarSuite.success(input);
        ScalarSuite.oracle(input, result, List.of("PROGA"), List.of(new SpInput.DataId(input.unit(), "data:0")));
        var move = (SpInput.MoveFact)input.statements().getFirst();
        check(move.source().id().handle().equals("operand:0:0") && move.target().id().handle().equals("operand:0:1"), "SP operand identities");
        check(move.source().provenance().original().startColumn() == 16 && move.target().provenance().original().startColumn() == 27, "independent literal target provenance");
        var codec = new AirJson(); var publication = result.publication().orElseThrow(); byte[] air = codec.encode(publication);
        check(codec.decode(air).equals(publication) && Arrays.equals(air, codec.encode(publication)), "shared exact round trip and deterministic bytes");
        var golden = (ObjectNode)JSON.readTree(raw);
        var legacy = golden.deepCopy(); legacy.put("contractVersion", "1.1.0"); physical(legacy, "new fields under 1.1.0");
        for (var path : List.of("/dataDeclarations/0/scalarText", "/statements/0/source/logicalValue", "/statements/0/target/wholeItemAccess",
                "/statements/0/copySemantics", "/statements/0/normalContinuation")) {
            var node = golden.deepCopy(); int slash = path.lastIndexOf('/'); object(node, path.substring(0, slash)).remove(path.substring(slash + 1));
            physical(node, "missing 1.2 field " + path);
        }
        for (var path : List.of("/dataDeclarations/0/scalarText", "/statements/0/source/logicalValue")) {
            var node = golden.deepCopy(); object(node, path).put("logicalDomain", "NUMERIC"); physical(node, "non TEXT logical domain");
        }
        var old = (ObjectNode)JSON.readTree(resource("cobol-semantic-product.json"));
        var previous = ScalarSuite.success(decode(JSON.writeValueAsBytes(old))).publication();
        old.put("contractVersion", "1.2.0");
        check(ScalarSuite.success(decode(JSON.writeValueAsBytes(old))).publication().equals(previous), "CP3 1.1 and 1.2 equivalent typed model");
        for (var path : List.of("/dataDeclarations/0/scalarText", "/statements/0/source/logicalValue", "/statements/0/target/wholeItemAccess", "/statements/0/target/binding/selected")) {
            var node = golden.deepCopy(); int slash = path.lastIndexOf('/'); object(node, path.substring(0, slash)).putNull(path.substring(slash + 1));
            ScalarSuite.rejected(decode(JSON.writeValueAsBytes(node)), "wire absent proof " + path);
        }
        var changed = golden.deepCopy(); object(changed, "/dataDeclarations/0").put("picture", "irrelevant"); object(changed, "/statements/0/source").put("value", "ignored legacy spelling");
        object(changed, "/statements/0/header/readiness/lowering").put("scope", "unrelated display text");
        check(ScalarSuite.success(decode(JSON.writeValueAsBytes(changed))).publication().equals(result.publication()), "no picture/value/readiness text interpretation or hashing");
        var physical = decode(resource("scalar-physical-1.2.0.json")); var pr = ScalarSuite.success(physical);
        ScalarSuite.oracle(physical, pr, List.of("PROGA"), List.of(new SpInput.DataId(physical.unit(), "data:0")));
        byte[] physicalAir = codec.encode(pr.publication().orElseThrow());
        check(physical.statements().getFirst().header().provenance().original().startLine() > 60_000 && physicalAir.length < air.length + 3000, "60k physical lines do not expand AIR identities");
        System.out.println("SCALAR_PHYSICAL lines=60012 sp_bytes=" + resource("scalar-physical-1.2.0.json").length + " air_bytes=" + physicalAir.length);
        System.out.println("SCALAR_AIR bytes=" + air.length + " sha256=" + HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(air)) + " publication=" + publication.id().localId());
        return checks;
    }
}
