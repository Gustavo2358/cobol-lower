package io.github.gustavo2358.lower.adapters.sp;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.github.gustavo2358.air.json.AirJson;
import io.github.gustavo2358.air.model.Control;
import io.github.gustavo2358.air.model.Evidence;
import io.github.gustavo2358.air.model.Ids.PublicationId;
import io.github.gustavo2358.air.model.Operations;
import io.github.gustavo2358.air.model.Scopes;
import io.github.gustavo2358.lower.adapters.cli.CobolLower;
import io.github.gustavo2358.lower.application.CobolLowerer;
import io.github.gustavo2358.lower.application.LoweringResult;
import io.github.gustavo2358.lower.domain.SpInput;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/** Synthetic SP2.15 witnesses for the descriptive observed-statement boundary. */
public final class ObservedShapeSuite {
    private static final ObjectMapper JSON = new ObjectMapper();
    private static int checks;

    private ObservedShapeSuite() { }

    private record SemanticEnvelope(Evidence.CoverageStatus coverage,
            List<Evidence.PrecisionStatus> precision, boolean readsAllMemory,
            boolean writesAllMemory, boolean includesEnvironment, int knownControl,
            List<Boolean> openControl, int knownDependencies, boolean anyResource) { }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
        checks++;
    }

    private static ObjectNode witness(String shape) throws Exception {
        var resource = ObservedShapeSuite.class.getResourceAsStream("/sp/dvi/invariant.json");
        var root = (ObjectNode) JSON.readTree(Objects.requireNonNull(resource, "SP2.15 base fixture"));
        var previous = (ObjectNode) root.path("statements").get(0);
        var header = (ObjectNode) previous.path("header").deepCopy();
        var readiness = (ObjectNode) header.path("readiness");
        for (String dimension : List.of("lowering", "cfg", "effectsDataflow")) {
            var claim = (ObjectNode) readiness.path(dimension);
            claim.put("status", "BLOCKED").put("scope", "opaque statement semantics are unavailable");
            var aggregate = (ObjectNode) root.path("coverage").path("readiness").path(dimension);
            aggregate.put("status", "BLOCKED").put("scope", "opaque statement bounds aggregate readiness");
        }

        var observed = JSON.createObjectNode();
        observed.put("variant", "OBSERVED");
        observed.set("header", header);
        observed.put("observedKind", "EMBEDDED_LANGUAGE");
        observed.put("observedShape", shape);
        observed.put("gapCode", "OBSERVED_STATEMENT_PARTIAL");
        var continuation = observed.putObject("normalContinuation");
        continuation.put("availability", "UNAVAILABLE").putNull("statement");
        continuation.set("provenance", header.path("provenance"));
        observed.putArray("knownReferences");
        ((ArrayNode) root.path("statements")).set(0, observed);

        var gaps = (ArrayNode) root.path("gaps");
        gaps.removeAll();
        var gap = gaps.addObject();
        gap.put("statement", "statement:0").put("scope", "CAPABILITY")
            .put("code", "OBSERVED_STATEMENT_PARTIAL").put("detail", "synthetic opaque witness");
        gap.set("provenance", header.path("provenance"));
        return root;
    }

    private static SpInput decode(ObjectNode document) throws Exception {
        var bytes = JSON.writeValueAsBytes(document);
        var wire = JSON.readValue(bytes, Wire215.Document.class);
        check(wire.statements().getFirst() instanceof Wire211.ObservedDocument,
            "SP JSON must decode to the typed observed wire variant");
        var observed = (Wire211.ObservedDocument) wire.statements().getFirst();
        check(observed.observedShape().equals(document.path("statements").get(0).path("observedShape").textValue()),
            "wire must retain observedShape exactly");

        var decoded = new SpJsonDecoder(new SpJsonDecoder.Limits(64)).decode(bytes);
        check(decoded instanceof SpJsonDecoder.Decoded, "synthetic SP2.15 witness must decode: " + decoded);
        var statement = ((SpJsonDecoder.Decoded) decoded).input().statements().getFirst();
        check(statement instanceof SpInput.OtherStatement, "observed wire fact must materialize as OtherStatement");
        return ((SpJsonDecoder.Decoded) decoded).input();
    }

    private static Operations.Opaque opaque(LoweringResult result, SpInput.StatementId source) {
        var link = result.statements().stream().filter(value -> value.source().equals(source)).findFirst().orElseThrow();
        var sequence = result.publication().orElseThrow().units().getFirst().sequences().stream()
            .filter(value -> value.label().equals(link.label())).findFirst().orElseThrow();
        check(sequence.instructions().isEmpty(), "opaque witness must not gain semantic instructions or NOP");
        check(sequence.terminator() instanceof Operations.Opaque, "observed witness must lower to AIR opaque");
        return (Operations.Opaque) sequence.terminator();
    }

    private static SemanticEnvelope semanticEnvelope(Operations.Opaque opaque) {
        var memory = opaque.envelope().memory();
        check(memory.knownReads().isEmpty() && memory.knownWrites().isEmpty() && memory.mustOverwrite().isEmpty(),
            "empty known references must not invent reads, writes, or overwrite effects");
        check(memory.otherReads() == Scopes.NoMemory.INSTANCE && memory.otherWrites() == Scopes.NoMemory.INSTANCE,
            "a missing summary cannot invent memory effects");

        var control = opaque.envelope().control();
        check(control.known().isEmpty() && control.remainder() instanceof Scopes.WithinControl,
            "unavailable continuation must not invent fallthrough or known control");
        var controlScope = (Scopes.WithinControl) control.remainder();
        check(controlScope.scope() instanceof Scopes.LabelsControl labels && labels.labels().isEmpty(),
            "unavailable continuation has no modeled destination");

        var dependencies = opaque.envelope().dependencies();
        check(dependencies.known().isEmpty() && dependencies.remainder() == Scopes.NoResources.INSTANCE,
            "a missing summary cannot invent resources");
        check(opaque.knownOperands().isEmpty() && opaque.valueResults().isEmpty(),
            "empty known references must not invent operands or results");
        var precision = opaque.header().precision();
        var result = new SemanticEnvelope(opaque.header().coverage(),
            List.of(precision.control().status(), precision.storage().status(), precision.effects().status(),
                precision.values().status(), precision.dependencies().status()),
            false, false, false,
            control.known().size(), List.of(false, false, false, false, false, false), dependencies.known().size(), false);
        check(result.coverage() == Evidence.CoverageStatus.ABSTRACTED
                && result.precision().equals(List.of(Evidence.PrecisionStatus.OPEN, Evidence.PrecisionStatus.EXACT,
                    Evidence.PrecisionStatus.EXACT, Evidence.PrecisionStatus.OPEN, Evidence.PrecisionStatus.EXACT))
                && !result.readsAllMemory() && !result.writesAllMemory() && !result.includesEnvironment()
                && result.knownControl() == 0 && result.openControl().equals(List.of(false, false, false, false, false, false))
                && result.knownDependencies() == 0 && !result.anyResource(),
            "shape affects diagnostics but adds no executable global effect");
        return result;
    }

    public static int run() throws Exception {
        checks = 0;
        SemanticEnvelope expectedEnvelope = null;
        Set<PublicationId> identities = new HashSet<>();
        for (String shape : List.of("OPAQUE_DLI", "OPAQUE_CICS", "OPAQUE_FUTURE_LANGUAGE")) {
            var input = decode(witness(shape));
            var statement = (SpInput.OtherStatement) input.statements().getFirst();
            check(statement.observedShape().orElseThrow().equals(shape),
                "materialization must retain open observed shape " + shape);
            check(statement.observedKind().equals("EMBEDDED_LANGUAGE")
                    && statement.gapCode().equals("OBSERVED_STATEMENT_PARTIAL"),
                "materialization must retain observed kind and gap independently of shape");
            check(statement.normalContinuation().availability() == SpInput.ContinuationAvailability.UNAVAILABLE
                    && statement.normalContinuation().statement().isEmpty() && statement.knownReferences().isEmpty(),
                "shape transport must not change continuation or known references");
            check(statement.header().readiness().lowering().status() == SpInput.ReadinessStatus.BLOCKED
                    && statement.header().readiness().cfg().status() == SpInput.ReadinessStatus.BLOCKED
                    && statement.header().readiness().effectsDataflow().status() == SpInput.ReadinessStatus.BLOCKED,
                "shape transport must not change readiness");

            var lowered = new CobolLowerer().lower(input, CobolLower.OPTIONS);
            check(lowered.status() == LoweringResult.Status.SUCCESS,
                "opaque witness must lower conservatively: " + lowered.status() + " " + lowered.admission().diagnostics());
            identities.add(lowered.publication().orElseThrow().id());
            check(lowered.operands().isEmpty(), "shape must not invent AIR operand links");
            var opaque = opaque(lowered, statement.header().id());
            check(opaque.observedKind().equals(shape), "AIR opaque descriptive identity must retain observedShape");
            var actualEnvelope = semanticEnvelope(opaque);
            if (expectedEnvelope == null) expectedEnvelope = actualEnvelope;
            else check(actualEnvelope.equals(expectedEnvelope), "different shapes must have identical conservative semantics");

            var roundTrip = new AirJson().decode(new AirJson().encode(lowered.publication().orElseThrow()));
            check(roundTrip.units().getFirst().sequences().stream().map(value -> value.terminator())
                    .filter(Operations.Opaque.class::isInstance).map(Operations.Opaque.class::cast)
                    .anyMatch(value -> value.observedKind().equals(shape)),
                "AIR JSON round-trip must retain opaque descriptive identity");
        }
        check(identities.size() == 3, "observedShape must participate in deterministic publication identity");
        for (String malformed : List.of("missing", "null")) {
            var document = witness("OPAQUE_DLI");
            var observed = (ObjectNode) document.path("statements").get(0);
            if (malformed.equals("missing")) observed.remove("observedShape");
            else observed.putNull("observedShape");
            check(new SpJsonDecoder(new SpJsonDecoder.Limits(64)).decode(JSON.writeValueAsBytes(document))
                    instanceof SpJsonDecoder.Rejected,
                malformed + " observedShape must fail closed");
        }
        return checks;
    }

    public static void main(String[] ignored) throws Exception {
        System.out.println("OBSERVED_SHAPE_TESTS=" + run());
    }
}
