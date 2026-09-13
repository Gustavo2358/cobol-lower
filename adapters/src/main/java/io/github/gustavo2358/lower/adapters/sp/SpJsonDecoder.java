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
    public record Limits(int maxDepth) {
        public Limits {
            if (maxDepth < 1) throw new IllegalArgumentException("positive limits required");
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

    private final ObjectMapper mapper;

    public SpJsonDecoder(Limits limits) {
        Objects.requireNonNull(limits);
        var factory = JsonFactory.builder().enable(StreamReadFeature.STRICT_DUPLICATE_DETECTION)
            .streamReadConstraints(new ShapeConstraints(limits.maxDepth())).build();
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
                meter.nodes++;
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
                case "1.3.0" -> {
                    var wire = mapper.treeToValue(node, Wire13.Document.class);
                    requirePhysical(wire, "$", meter); requireCoherent13(wire); input = Materialize.input(wire);
                }
                case "1.4.0" -> {
                    var wire = mapper.treeToValue(node, Wire14.Document.class);
                    requirePhysical(wire, "$", meter); requireCoherent14(wire); input = Materialize.input(wire);
                }
                case "1.5.0" -> {
                    var wire = mapper.treeToValue(node, Wire15.Document.class);
                    requirePhysical(wire, "$", meter); requireCoherent15(wire); input = Materialize.input(wire);
                }
                case "1.6.0" -> {
                    var wire = mapper.treeToValue(node, Wire16.Document.class);
                    requirePhysical(wire, "$", meter); requireCoherent16(wire); input = Materialize.input(wire);
                    requireLegacyPerform(input);
                }
                case "1.7.0" -> {
                    // Same typed fields; SP1.7 explicitly generalizes isolated primary composition.
                    var wire = mapper.treeToValue(node, Wire16.Document.class);
                    requirePhysical(wire, "$", meter); requireCoherent16(wire); input = Materialize.input(wire);
                }
                case "1.8.0", "1.9.0" -> {
                    // SP1.9 localizes frontend entry/input completeness using the same typed fields.
                    var wire = mapper.treeToValue(node, Wire18.Document.class);
                    requirePhysical(wire, "$", meter); requireCoherent18(wire); input = Materialize.input(wire);
                }
                case "2.0.0" -> {
                    var wire = mapper.treeToValue(node, Wire20.Document.class);
                    requirePhysical(wire, "$", meter); requireCoherent20(wire); input = Materialize.input(wire);
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
            if (move.copySemantics() == SpInput.CopySemantics.FITTED_TEXT)
                throw new PhysicalShape("$/statements/" + i + "/copySemantics (1.2)");
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

    private static void requireCoherent13(Wire13.Document wire) {
        for (var data : wire.dataDeclarations()) if (data.scalarText() != null && data.scalarText().logicalExtent() <= 0)
            throw new PhysicalShape("$/dataDeclarations/scalarText/logicalExtent");
        for (var statement : wire.statements()) {
            if (statement instanceof Wire13.MoveDocument m && m.source().logicalValue() != null) {
                var value = m.source().logicalValue();
                if (m.source().kind() != SpInput.LiteralKind.ALPHANUMERIC || !m.source().value().equals(value.value()))
                    throw new PhysicalShape("$/statements/source/logicalValue");
                logical13(value);
            }
            if (statement instanceof Wire13.MoveDocument m && m.textAdjustment() != null) logical13(m.textAdjustment().result());
            if (statement instanceof Wire13.CallDocument c && c.target() instanceof Wire13.LiteralTargetDocument l && l.logicalValue() != null) {
                logical13(l.logicalValue());
                if (!l.text().equals(l.logicalValue().value())) throw new PhysicalShape("$/statements/target/text");
            }
        }
    }
    private static void logical13(Wire13.LogicalDocument value) {
        if (value.logicalExtent() < 0 || value.logicalExtent() != value.value().codePointCount(0, value.value().length()))
            throw new PhysicalShape("$/logicalValue/logicalExtent");
    }

    private static void requireCoherent14(Wire14.Document wire) {
        for (var data : wire.dataDeclarations()) if (data.scalarText() != null && data.scalarText().logicalExtent() <= 0)
            throw new PhysicalShape("$/dataDeclarations/scalarText/logicalExtent");
        for (var statement : wire.statements()) {
            if (statement instanceof Wire14.MoveDocument m && m.source().logicalValue() != null) {
                var value = m.source().logicalValue();
                if (m.source().kind() != SpInput.LiteralKind.ALPHANUMERIC || !m.source().value().equals(value.value()))
                    throw new PhysicalShape("$/statements/source/logicalValue");
                logical14(value);
            }
            if (statement instanceof Wire14.MoveDocument m && m.textAdjustment() != null) logical14(m.textAdjustment().result());
            if (statement instanceof Wire14.CallDocument c && c.target() instanceof Wire14.LiteralTargetDocument l && l.logicalValue() != null) {
                logical14(l.logicalValue());
                if (!l.text().equals(l.logicalValue().value())) throw new PhysicalShape("$/statements/target/text");
            }
        }
    }
    private static void logical14(Wire14.LogicalDocument value) {
        if (value.logicalExtent() < 0 || value.logicalExtent() != value.value().codePointCount(0, value.value().length()))
            throw new PhysicalShape("$/logicalValue/logicalExtent");
    }

    private static void requireCoherent15(Wire15.Document wire) {
        for (var data : wire.dataDeclarations()) if (data.scalarText() != null && data.scalarText().logicalExtent() <= 0)
            throw new PhysicalShape("$/dataDeclarations/scalarText/logicalExtent");
        for (var statement : wire.statements()) {
            if (statement instanceof Wire15.MoveDocument m && m.source() instanceof Wire15.LiteralDocument literal && literal.logicalValue() != null) {
                var value = literal.logicalValue();
                if (literal.kind() != SpInput.LiteralKind.ALPHANUMERIC || !literal.value().equals(value.value()))
                    throw new PhysicalShape("$/statements/source/logicalValue");
                logical15(value);
            }
            if (statement instanceof Wire15.MoveDocument m && m.textAdjustment() != null) logical15(m.textAdjustment().result());
            if (statement instanceof Wire15.CallDocument c && c.target() instanceof Wire15.LiteralTargetDocument l && l.logicalValue() != null) {
                logical15(l.logicalValue());
                if (!l.text().equals(l.logicalValue().value())) throw new PhysicalShape("$/statements/target/text");
            }
        }
    }
    private static void logical15(Wire15.LogicalDocument value) {
        if (value.logicalExtent() < 0 || value.logicalExtent() != value.value().codePointCount(0, value.value().length()))
            throw new PhysicalShape("$/logicalValue/logicalExtent");
    }

    private static void requireCoherent16(Wire16.Document wire) {
        for (var s : wire.statements()) if (s instanceof Wire16.PerformDocument p && p.profile() == io.github.gustavo2358.lower.domain.SpInput.PerformProfile.BASIC_PROCEDURE_PERFORM)
            throw new IllegalArgumentException("BASIC intrinsic body semantics require SP 1.8.0");
        for (var data : wire.dataDeclarations()) if (data.scalarText() != null && data.scalarText().logicalExtent() <= 0)
            throw new PhysicalShape("$/dataDeclarations/scalarText/logicalExtent");
        for (var statement : wire.statements()) {
            if (statement instanceof Wire16.MoveDocument m && m.source() instanceof Wire16.LiteralDocument literal && literal.logicalValue() != null) {
                var value = literal.logicalValue();
                if (literal.kind() != SpInput.LiteralKind.ALPHANUMERIC || !literal.value().equals(value.value()))
                    throw new PhysicalShape("$/statements/source/logicalValue");
                logical16(value);
            }
            if (statement instanceof Wire16.MoveDocument m && m.textAdjustment() != null) logical16(m.textAdjustment().result());
            if (statement instanceof Wire16.CallDocument c && c.target() instanceof Wire16.LiteralTargetDocument l && l.logicalValue() != null) {
                logical16(l.logicalValue());
                if (!l.text().equals(l.logicalValue().value())) throw new PhysicalShape("$/statements/target/text");
            }
        }
    }
    private static void logical16(Wire16.LogicalDocument value) {
        if (value.logicalExtent() < 0 || value.logicalExtent() != value.value().codePointCount(0, value.value().length()))
            throw new PhysicalShape("$/logicalValue/logicalExtent");
    }

    private static void requireCoherent18(Wire18.Document wire) {
        for (var data : wire.dataDeclarations()) if (data.scalarText() != null && data.scalarText().logicalExtent() <= 0)
            throw new PhysicalShape("$/dataDeclarations/scalarText/logicalExtent");
        for (var statement : wire.statements()) {
            if (statement instanceof Wire18.MoveDocument m && m.source() instanceof Wire18.LiteralDocument literal && literal.logicalValue() != null) {
                var value = literal.logicalValue();
                if (literal.kind() != SpInput.LiteralKind.ALPHANUMERIC || !literal.value().equals(value.value()))
                    throw new PhysicalShape("$/statements/source/logicalValue");
                logical18(value);
            }
            if (statement instanceof Wire18.MoveDocument m && m.textAdjustment() != null) logical18(m.textAdjustment().result());
            if (statement instanceof Wire18.CallDocument c && c.target() instanceof Wire18.LiteralTargetDocument l && l.logicalValue() != null) {
                logical18(l.logicalValue());
                if (!l.text().equals(l.logicalValue().value())) throw new PhysicalShape("$/statements/target/text");
            }
        }
    }
    private static void logical18(Wire18.LogicalDocument value) {
        if (value.logicalExtent() < 0 || value.logicalExtent() != value.value().codePointCount(0, value.value().length()))
            throw new PhysicalShape("$/logicalValue/logicalExtent");
    }

    private static void requireCoherent20(Wire20.Document wire) {
        for (var data : wire.dataDeclarations()) if (data.scalarText() != null && data.scalarText().logicalExtent() <= 0)
            throw new PhysicalShape("$/dataDeclarations/scalarText/logicalExtent");
        for (var statement : wire.statements()) {
            if (statement instanceof Wire20.EvaluateDocument e) for (var a : e.arms()) {
                if (!(a.selection() instanceof Wire20.LiteralDocument l) || l.kind()!=SpInput.LiteralKind.ALPHANUMERIC
                        || l.logicalValue()==null || !l.value().equals(l.logicalValue().value()))
                    throw new PhysicalShape("$/statements/EVALUATE/arms/selection");
                logical20(l.logicalValue());
            }
            if (statement instanceof Wire20.MoveDocument m && m.source() instanceof Wire20.LiteralDocument literal && literal.logicalValue() != null) {
                var value = literal.logicalValue();
                if (literal.kind() != SpInput.LiteralKind.ALPHANUMERIC || !literal.value().equals(value.value()))
                    throw new PhysicalShape("$/statements/source/logicalValue");
                logical20(value);
            }
            if (statement instanceof Wire20.MoveDocument m && m.textAdjustment() != null) logical20(m.textAdjustment().result());
            if (statement instanceof Wire20.CallDocument c && c.target() instanceof Wire20.LiteralTargetDocument l && l.logicalValue() != null) {
                logical20(l.logicalValue());
                if (!l.text().equals(l.logicalValue().value())) throw new PhysicalShape("$/statements/target/text");
            }
        }
    }
    private static void logical20(Wire20.LogicalDocument value) {
        if (value.logicalExtent() < 0 || value.logicalExtent() != value.value().codePointCount(0, value.value().length()))
            throw new PhysicalShape("$/logicalValue/logicalExtent");
    }

    private static void requireLegacyPerform(SpInput input) {
        var statements = new java.util.HashMap<SpInput.StatementId, SpInput.StatementFact>();
        input.statements().forEach(s -> statements.put(s.header().id(), s));
        for (var s : input.statements()) if (s instanceof SpInput.PerformFact p
                && p.profile() == SpInput.PerformProfile.SIMPLE_SINGLE_CALLSITE_PROCEDURE_PERFORM) {
            var main = p.primaryStatements(); int index = main.indexOf(p.header().id());
            if (index < 0 || main.size() != index + 3 || !(statements.get(main.get(index + 1)) instanceof SpInput.CallFact)
                    || !(statements.get(main.get(index + 2)) instanceof SpInput.GobackFact))
                throw new PhysicalShape("$/statements/PERFORM (SP1.6 primary shape; composition requires SP1.7)");
            for (int n = 0; n < index; n++) if (!(statements.get(main.get(n)) instanceof SpInput.MoveFact))
                throw new PhysicalShape("$/statements/PERFORM (SP1.6 MOVE prefix)");
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
    /** Jackson 2.22 has no document/token cap by default. Keep nesting and lexical
     * number/name guards, but strings are semantic values, not a capacity budget. */
    private static final class ShapeConstraints extends StreamReadConstraints {
        private static final long serialVersionUID = 1L;
        ShapeConstraints(int depth) {
            super(depth, DEFAULT_MAX_DOC_LEN, DEFAULT_MAX_NUM_LEN, DEFAULT_MAX_STRING_LEN,
                DEFAULT_MAX_NAME_LEN, DEFAULT_MAX_TOKEN_COUNT);
        }
        @Override public void validateStringLength(int length) { /* No artificial value-size gate. */ }
    }
    private static final class Meter { long bytes; long nodes; long physical; }
    private static final class PhysicalShape extends RuntimeException {
        private static final long serialVersionUID = 1L;
        PhysicalShape(String location) { super(location); }
    }
}
