package io.github.gustavo2358.lower.adapters.sp;

import io.github.gustavo2358.lower.domain.SpInput;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.StreamReadConstraints;
import com.fasterxml.jackson.core.StreamReadFeature;
import com.fasterxml.jackson.core.exc.StreamConstraintsException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.databind.cfg.CoercionAction;
import com.fasterxml.jackson.databind.cfg.CoercionInputShape;
import com.fasterxml.jackson.databind.type.LogicalType;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.List;
import java.util.Objects;

/** JSON transport only; success materializes facts, not semantic admission or AIR. */
public final class SpJsonDecoder {
    public record Limits(int maxBytes, int maxDepth, int maxNodes) {
        public Limits {
            if (maxBytes < 1 || maxDepth < 1 || maxNodes < 1) throw new IllegalArgumentException("positive limits required");
        }
    }
    public enum Code { INPUT_ERROR, UNSUPPORTED_CONTRACT, IMPLEMENTATION_LIMIT }
    public record Diagnostic(Code code, String phase, String location) {
        public Diagnostic { Objects.requireNonNull(code); Objects.requireNonNull(phase); Objects.requireNonNull(location); }
    }
    public record UnsupportedVariant(SpInput.StatementId id, SpInput.Variant variant) { }
    public sealed interface Result permits Decoded, Rejected { }
    public record Decoded(SpInput input, List<UnsupportedVariant> unsupportedVariants) implements Result {
        public Decoded { Objects.requireNonNull(input); unsupportedVariants = List.copyOf(unsupportedVariants); }
    }
    public record Rejected(Diagnostic diagnostic) implements Result { }
    public record Statistics(long bytesProcessed, long jsonNodesVisited, long physicalValuesVisited) { }
    public record Measurement(Result result, Statistics statistics) {
        public Measurement { Objects.requireNonNull(result); Objects.requireNonNull(statistics); }
    }

    private final Limits limits;
    private final ObjectMapper mapper;

    public SpJsonDecoder(Limits limits) {
        this.limits = Objects.requireNonNull(limits);
        var factory = JsonFactory.builder().enable(StreamReadFeature.STRICT_DUPLICATE_DETECTION)
            .streamReadConstraints(StreamReadConstraints.builder().maxNestingDepth(limits.maxDepth())
                .maxStringLength(limits.maxBytes()).build()).build();
        mapper = JsonMapper.builder(factory).disable(MapperFeature.ALLOW_COERCION_OF_SCALARS)
            .disable(DeserializationFeature.ACCEPT_FLOAT_AS_INT)
            .enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            .enable(DeserializationFeature.FAIL_ON_MISSING_CREATOR_PROPERTIES)
            .enable(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES)
            .enable(DeserializationFeature.FAIL_ON_NUMBERS_FOR_ENUMS)
            .enable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS).build();
        // ALLOW_COERCION_OF_SCALARS does not by itself forbid scalar -> String.
        for (var shape : List.of(CoercionInputShape.Integer, CoercionInputShape.Float, CoercionInputShape.Boolean))
            mapper.coercionConfigFor(LogicalType.Textual).setCoercion(shape, CoercionAction.Fail);
    }

    public Result decode(byte[] bytes) {
        return decodeMeasured(bytes).result();
    }

    public Measurement decodeMeasured(byte[] bytes) {
        var meter = new Meter();
        var result = decodePayload(bytes, meter);
        return new Measurement(result, new Statistics(meter.bytes, meter.nodes, meter.physical));
    }

    private Result decodePayload(byte[] bytes, Meter meter) {
        if (bytes == null) return reject(Code.INPUT_ERROR, "$");
        if (bytes.length > limits.maxBytes()) return reject(Code.IMPLEMENTATION_LIMIT, "$");
        meter.bytes = bytes.length;
        try {
            // Reject non-UTF-8 encodings even if the JSON library can autodetect them.
            StandardCharsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.REPORT)
                .onUnmappableCharacter(CodingErrorAction.REPORT).decode(ByteBuffer.wrap(bytes));
            if (bytes.length > 1 && (bytes[0] == 0 || bytes[1] == 0)) return reject(Code.INPUT_ERROR, "$");
            JsonNode node = mapper.readTree(bytes);
            if (node == null || !node.isObject()) return reject(Code.INPUT_ERROR, "$");
            var pending = new ArrayDeque<JsonNode>(); pending.push(node);
            while (!pending.isEmpty()) {
                if (++meter.nodes > limits.maxNodes()) return reject(Code.IMPLEMENTATION_LIMIT, "$");
                pending.pop().elements().forEachRemaining(pending::push);
            }
            if (!node.path("schema").isTextual() || !node.path("contractVersion").isTextual()) return reject(Code.INPUT_ERROR, "$/schema,contractVersion");
            if (!node.path("schema").textValue().equals("cobol-semantic-product")) return reject(Code.UNSUPPORTED_CONTRACT, "$/schema");
            SpInput input;
            switch (node.path("contractVersion").textValue()) {
                case "1.1.0" -> {
                    var wire = mapper.treeToValue(node, Wire.Document.class);
                    requirePhysical(wire, "$", meter); input = Materialize.input(wire);
                }
                case "1.2.0" -> {
                    var wire = mapper.treeToValue(node, Wire12.Document.class);
                    requirePhysical(wire, "$", meter); requireCoherent12(wire); input = Materialize.input(wire);
                }
                default -> { return reject(Code.UNSUPPORTED_CONTRACT, "$/contractVersion"); }
            }
            var variants = input.statements().stream().filter(SpInput.OtherStatement.class::isInstance)
                .map(SpInput.OtherStatement.class::cast).map(v -> new UnsupportedVariant(v.header().id(), v.variant())).toList();
            return new Decoded(input, variants);
        } catch (StreamConstraintsException ex) {
            return reject(Code.IMPLEMENTATION_LIMIT, "$ limits");
        } catch (JsonProcessingException ex) {
            var location = ex.getLocation();
            return reject(Code.INPUT_ERROR, location == null ? "$ DTO" : "line:" + location.getLineNr() + ",column:" + location.getColumnNr());
        } catch (CharacterCodingException ex) {
            return reject(Code.INPUT_ERROR, "$ UTF-8");
        } catch (PhysicalShape ex) {
            return reject(Code.INPUT_ERROR, ex.getMessage());
        } catch (java.io.IOException ex) {
            return reject(Code.INPUT_ERROR, "$ bytes");
        }
    }

    /** Upstream 1.2 typed fact invariants, not COBOL interpretation or input repair. */
    private static void requireCoherent12(Wire12.Document wire) {
        for (int i = 0; i < wire.dataDeclarations().size(); i++) {
            var scalar = wire.dataDeclarations().get(i).scalarText();
            if (scalar != null && scalar.logicalExtent() <= 0)
                throw new PhysicalShape("$/dataDeclarations/" + i + "/scalarText/logicalExtent");
        }
        for (int i = 0; i < wire.statements().size(); i++) {
            if (!(wire.statements().get(i) instanceof Wire12.MoveDocument move)) continue;
            var source = move.source(); var logical = source.logicalValue();
            if (logical == null) continue;
            if (source.kind() != SpInput.LiteralKind.ALPHANUMERIC)
                throw new PhysicalShape("$/statements/" + i + "/source/kind");
            if (!source.value().equals(logical.value()))
                throw new PhysicalShape("$/statements/" + i + "/source/value");
            if (logical.logicalDomain() != SpInput.LogicalDomain.TEXT || logical.logicalExtent() < 0
                    || logical.logicalExtent() != logical.value().codePointCount(0, logical.value().length()))
                throw new PhysicalShape("$/statements/" + i + "/source/logicalValue");
        }
    }

    private static Rejected reject(Code code, String location) {
        return new Rejected(new Diagnostic(code, "physical", location));
    }

    private static void requirePhysical(Object value, String location, Meter meter) {
        if (value == null) throw new PhysicalShape(location);
        meter.physical++;
        if (value instanceof List<?> list) {
            for (int i = 0; i < list.size(); i++) requirePhysical(list.get(i), location + "/" + i, meter);
        } else if (value.getClass().isRecord()) {
            for (var component : value.getClass().getRecordComponents()) {
                try {
                    Object nested = component.getAccessor().invoke(value);
                    if (nested != null || component.getAnnotation(Wire.Nullable.class) == null)
                        requirePhysical(nested, location + "/" + component.getName(), meter);
                } catch (ReflectiveOperationException ex) {
                    throw new IllegalStateException("wire record inaccessible", ex);
                }
            }
        }
    }
    private static final class Meter { long bytes; long nodes; long physical; }
    private static final class PhysicalShape extends RuntimeException {
        private static final long serialVersionUID = 1L;
        PhysicalShape(String location) { super(location); }
    }
}
