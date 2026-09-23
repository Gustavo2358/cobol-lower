package io.github.gustavo2358.lower.adapters.testing;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.github.gustavo2358.lower.adapters.cli.CobolLower;
import io.github.gustavo2358.lower.adapters.sp.SpJsonDecoder;
import io.github.gustavo2358.lower.application.Admission;
import io.github.gustavo2358.lower.application.CobolLowerer;
import io.github.gustavo2358.lower.application.LoweringResult;
import io.github.gustavo2358.lower.domain.SpInput;
import io.github.gustavo2358.lower.testing.IfInputs;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import static io.github.gustavo2358.lower.testing.CallOracle.check;

/** SP2.37 malformed transport and semantic admission use distinct typed failures. */
public final class OrdinaryContinuationBoundarySuite {
    private static final ObjectMapper JSON = new ObjectMapper();
    private static SpJsonDecoder.Result decode(ObjectNode tree) throws Exception {
        return new SpJsonDecoder(CobolLower.INPUT_LIMITS).decode(JSON.writeValueAsBytes(tree));
    }
    private static SpInput input(ObjectNode tree) throws Exception {
        var decoded = decode(tree);
        check(decoded instanceof SpJsonDecoder.Decoded, "well-shaped SP decodes: " + decoded);
        return ((SpJsonDecoder.Decoded) decoded).input();
    }
    private static void physicalRejection(ObjectNode tree, String mutation) throws Exception {
        var decoded = decode(tree); // An escaped exception must fail the test.
        check(decoded instanceof SpJsonDecoder.Rejected, mutation + " rejected physically");
        check(((SpJsonDecoder.Rejected) decoded).diagnostic().code() == SpJsonDecoder.Code.INPUT_ERROR,
            mutation + " is INPUT_ERROR, not an implementation limit");
    }
    private static void semanticRejection(SpInput input, SpInput.StatementId source) {
        var result = new CobolLowerer().lower(input, CobolLower.OPTIONS);
        check(result.status() == LoweringResult.Status.INVALID_INPUT && result.publication().isEmpty()
            && result.validation().isEmpty(), "self relation rejected before AIR publication/validation");
        check(result.admission().diagnostics().stream().anyMatch(d -> d.rule() == Admission.Rule.STRUCTURE
            && d.subject().equals(source.handle())
            && d.requirement().equals("ordinary continuation cannot target its source")),
            "specific self-relation invariant, not an intrinsic-successor conflict");
    }
    private static void cliRejection(ObjectNode tree, int expected, String diagnostic) throws Exception {
        var directory = Files.createTempDirectory("sp237-boundary-");
        var source = directory.resolve("input.json"); var output = directory.resolve("output.air.json");
        try {
            Files.write(source, JSON.writeValueAsBytes(tree));
            for (boolean preexisting : List.of(false, true)) {
                if (preexisting) Files.writeString(output, "caller-owned sentinel");
                var errors = new ByteArrayOutputStream();
                int exit;
                try (var err = new PrintStream(errors, true, StandardCharsets.UTF_8)) {
                    exit = CobolLower.run(new String[]{source.toString(), output.toString()}, err);
                }
                check(exit == expected && errors.toString(StandardCharsets.UTF_8).contains(diagnostic),
                    "CLI typed rejection: " + errors);
                check(preexisting ? Files.readString(output).equals("caller-owned sentinel") : !Files.exists(output),
                    "invalid input neither publishes nor overwrites AIR");
            }
        } finally {
            Files.deleteIfExists(source); Files.deleteIfExists(output); Files.deleteIfExists(directory);
        }
    }
    public static void main(String[] args) throws Exception {
        ObjectNode control;
        try (var stream = OrdinaryContinuationBoundarySuite.class.getResourceAsStream("/sp/control-composition/ordinary-base.json")) {
            control = (ObjectNode) JSON.readTree(Objects.requireNonNull(stream));
        }
        var valid = input(control);
        check(new CobolLowerer().lower(valid, CobolLower.OPTIONS).publication().isPresent(), "unchanged control is executable");
        var nullOnly = control.deepCopy(); nullOnly.putArray("ordinaryContinuations").addNull();
        physicalRejection(nullOnly, "null relation only");
        var appended = control.deepCopy(); ((ArrayNode) appended.path("ordinaryContinuations")).addNull();
        physicalRejection(appended, "null appended to valid relations");
        cliRejection(appended, CobolLower.INPUT, "SP INPUT_ERROR");
        int mutations = 2;
        var relation = "/ordinaryContinuations/0";
        for (var parent : List.of("", relation, relation + "/provenance",
                relation + "/provenance/expanded", relation + "/provenance/original")) {
            var fields = parent.isEmpty() ? List.of("ordinaryContinuations") : parent.equals(relation)
                ? List.of("statement", "destination", "provenance") : parent.endsWith("provenance")
                ? List.of("expanded", "original", "includeChain", "exact")
                : List.of("file", "startLine", "startColumn", "endLine", "endColumn");
            for (var field : fields) for (boolean missing : List.of(false, true)) {
                var mutant = control.deepCopy(); var node = (ObjectNode) mutant.at(parent);
                if (missing) node.remove(field); else node.putNull(field);
                physicalRejection(mutant, parent + "/" + field + (missing ? " missing" : " null")); mutations++;
            }
        }
        var nullFrame = control.deepCopy();
        ((ObjectNode) nullFrame.at(relation + "/provenance")).putArray("includeChain").addNull();
        physicalRejection(nullFrame, "null include frame"); mutations++;
        for (var field : List.of("includingFile", "requestedName", "includedFile", "includeLine")) {
            for (boolean missing : List.of(false, true)) {
                var mutant = control.deepCopy();
                var frame = ((ObjectNode) mutant.at(relation + "/provenance")).putArray("includeChain").addObject();
                frame.put("includingFile", "root.cbl").put("requestedName", "CPY").put("includedFile", "CPY.cpy").put("includeLine", 1);
                if (missing) frame.remove(field); else frame.putNull(field);
                physicalRejection(mutant, "include frame " + field); mutations++;
            }
        }
        var wrongType = control.deepCopy(); ((ObjectNode) wrongType.at(relation)).put("provenance", "malformed");
        physicalRejection(wrongType, "provenance type"); mutations++;

        // Select a MOVE without an intrinsic successor, so no other guard masks R02.
        var source = valid.statements().stream().filter(SpInput.MoveFact.class::isInstance)
            .map(SpInput.MoveFact.class::cast).filter(m -> m.normalContinuation().statement().isEmpty()
                && valid.ordinaryContinuations().containsKey(m.header().id())).findFirst().orElseThrow();
        var id = source.header().id(); var original = valid.ordinaryContinuations().get(id);
        check(!original.statement().equals(Optional.of(id)), "control relation is not self-referential");
        var self = control.deepCopy();
        for (var r : self.path("ordinaryContinuations")) if (r.path("statement").asText().equals(id.handle()))
            ((ObjectNode) r).put("destination", id.handle());
        semanticRejection(input(self), id);
        var relations = new HashMap<>(valid.ordinaryContinuations());
        relations.put(id, IfInputs.with(original, "statement", Optional.of(id)));
        semanticRejection(IfInputs.with(valid, "ordinaryContinuations", relations), id);
        cliRejection(self, CobolLower.LOWERING, "Lowering INVALID_INPUT");
        check(new CobolLowerer().lower(valid, CobolLower.OPTIONS).publication().isPresent(), "negative checks do not mutate valid input");
        System.out.println("ORDINARY_CONTINUATION_BOUNDARY_R1=PASS physicalMutations=" + mutations + " selfRelation=JSON+MEMORY CLI=typed+atomic");
    }
}
