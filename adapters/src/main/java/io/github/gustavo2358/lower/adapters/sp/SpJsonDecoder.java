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
    private record LogicalTextViewDocument(String node,String root,String start,String length) { }
    private record LogicalExactViewDocument(String node,String representative,String length) { }
    private record OrdinaryContinuationDocument(String statement,String destination,Wire.ProvenanceDocument provenance) { }
    private record PendingLogical(String target,String value,int extent) { }
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
            if (!node.path("schema").isTextual() || !node.path("contractVersion").isTextual())
                return reject(Code.INPUT_ERROR, "$/schema or contractVersion");
            if (!node.path("schema").textValue().equals("cobol-semantic-product"))
                return reject(Code.UNSUPPORTED_CONTRACT, "$/schema");
            String receivedVersion=node.path("contractVersion").textValue();
            var profile=SpContractProfile.admitted(receivedVersion);
            if(profile==null)return reject(Code.UNSUPPORTED_CONTRACT,"$/contractVersion");
            for(var effect:node.path("statementEffects"))
                if(java.util.Set.of("DLI_HOST_OPERANDS","CICS_CONDITION_REGISTRATION").contains(effect.path("proof").asText())&&!java.util.Set.of("2.46.0","2.47.0").contains(receivedVersion))
                    throw new PhysicalShape("$/statementEffects/new embedded proof requires SP2.46");
            for(var condition:node.path("storage").path("entryState").path("conditions"))
                if(condition.path("kind").asText().equals("LOGICAL_TEXT")&&!java.util.Set.of("2.46.0","2.47.0").contains(receivedVersion))
                    throw new PhysicalShape("$/storage/entryState/LOGICAL_TEXT requires SP2.46");
            for(var statement:node.path("statements")) {
                String variant=statement.path("variant").asText();
                var condition=variant.equals("IF")?statement.path("condition"):statement.path("loop").path("condition");
                if(condition.isObject()) {
                    if(condition.has("textPredicate")&&!java.util.Set.of("2.46.0","2.47.0").contains(receivedVersion))throw new PhysicalShape("$/condition/textPredicate requires SP2.46");
                    if(!condition.has("textPredicate")&&receivedVersion.startsWith("2.")&&Integer.parseInt(receivedVersion.split("\\.")[1])>=11)
                        ((com.fasterxml.jackson.databind.node.ObjectNode)condition).putNull("textPredicate");
                }
                if(variant.equals("CICS_COMMAND")&&!profile.terminalSend()&&(statement.path("commandKind").asText().equals("SEND_TERMINAL")||statement.has("length")))
                    throw new PhysicalShape("$/statements terminal SEND requires SP2.44");
                if(variant.equals("CICS_HANDLER")&&!profile.handlers()||variant.equals("CICS_ABEND")&&!profile.abend()||variant.equals("CICS_COMMAND")&&!profile.commands())
                    throw new PhysicalShape("$/statements/variant not admitted by "+receivedVersion);
                if(variant.equals("CICS_COMMAND")&&statement.path("commandKind").asText().equals("RETRIEVE")&&!java.util.Set.of("2.46.0","2.47.0").contains(receivedVersion))
                    throw new PhysicalShape("$/statements/RETRIEVE requires SP2.46");
                if(variant.equals("CICS_HANDLER")&&statement.has("registrationEffects")&&!java.util.Set.of("2.46.0","2.47.0").contains(receivedVersion))
                    throw new PhysicalShape("$/statements/registrationEffects requires SP2.46");
                if(variant.equals("CICS_HANDLER")&&!statement.has("registrationEffects"))((com.fasterxml.jackson.databind.node.ObjectNode)statement).putNull("registrationEffects");
                if(variant.equals("CICS_COMMAND")&&statement.has("hostEffects")&&!java.util.Set.of("2.46.0","2.47.0").contains(receivedVersion))
                    throw new PhysicalShape("$/statements/hostEffects requires SP2.46");
                if(variant.equals("CICS_COMMAND")&&!statement.has("hostEffects"))((com.fasterxml.jackson.databind.node.ObjectNode)statement).putNull("hostEffects");
                if(variant.equals("CICS_COMMAND")&&!statement.has("length"))((com.fasterxml.jackson.databind.node.ObjectNode)statement).putNull("length");
            }
            if(!profile.abend())for(var proof:node.path("controlTopology").path("proofs"))
                if(proof.path("rule").asText().equals("cics-handle-abend-ordinary-return"))
                    throw new PhysicalShape("$/controlTopology/proofs/rule requires SP2.42");
            if(!profile.commands())for(var proof:node.path("controlTopology").path("proofs"))
                if(proof.path("rule").asText().startsWith("cics-command-"))
                    throw new PhysicalShape("$/controlTopology/proofs/rule requires SP2.43");
            if(!java.util.Set.of("2.45.0","2.46.0","2.47.0").contains(receivedVersion)&&node.path("controlTopology").has("exceptionalEvents")&&!node.path("controlTopology").path("exceptionalEvents").isEmpty())
                throw new PhysicalShape("$/controlTopology/exceptionalEvents requires SP2.45");
            if(node.path("controlTopology").has("exceptionalEvents")&&!node.path("controlTopology").path("exceptionalEvents").isArray())
                throw new PhysicalShape("$/controlTopology/exceptionalEvents must be an array");
            io.github.gustavo2358.lower.domain.NominalValues nominalValues=null;
            if(node.has("nominalValues")) {
                if(!receivedVersion.equals("2.47.0"))throw new PhysicalShape("$/nominalValues requires SP2.47");
                nominalValues=mapper.treeToValue(node.get("nominalValues"),io.github.gustavo2358.lower.domain.NominalValues.class);
                requirePhysical(nominalValues,"$/nominalValues",meter);
                ((com.fasterxml.jackson.databind.node.ObjectNode)node).remove("nominalValues");
            }
            boolean factContract=profile.factDependencies();
            if(factContract!=node.has("factDependencies")||factContract&&!node.path("factDependencies").isObject())
                throw new PhysicalShape("$/factDependencies requires SP2.40/2.41/2.42/2.43 and is mandatory there");
            io.github.gustavo2358.lower.domain.FactDependencies factDependencies=null;
            if(factContract) {
                factDependencies=mapper.treeToValue(node.path("factDependencies"),io.github.gustavo2358.lower.domain.FactDependencies.class);
                requirePhysical(factDependencies,"$/factDependencies",meter);
                ((com.fasterxml.jackson.databind.node.ObjectNode)node).remove("factDependencies");
            }
            boolean topologyContract=factContract||node.path("contractVersion").asText().equals("2.39.0");
            if(topologyContract!=node.has("controlTopology") || topologyContract&&!node.path("controlTopology").isObject())
                throw new PhysicalShape("$/controlTopology requires SP2.39 and is mandatory there");
            io.github.gustavo2358.lower.domain.ControlTopology topology=null;
            if(topologyContract) {
                if(!node.path("controlTopology").has("exceptionalEvents"))((com.fasterxml.jackson.databind.node.ObjectNode)node.path("controlTopology")).putArray("exceptionalEvents");
                topology=mapper.treeToValue(node.path("controlTopology"),io.github.gustavo2358.lower.domain.ControlTopology.class);
                requirePhysical(topology,"$/controlTopology",meter);
                ((com.fasterxml.jackson.databind.node.ObjectNode)node).remove("controlTopology");
            }
            boolean partialSequenceContract=topologyContract||node.path("contractVersion").asText().equals("2.38.0");
            boolean partialSequence=false;
            for(var statement:node.path("statements"))if(statement.path("variant").asText().equals("MOVE")) {
                partialSequence|=statement.has("logicalTransfers");
                if(statement.path("additionalTransfers").isArray()&&!statement.path("additionalTransfers").isEmpty()) {
                    partialSequence|=statement.path("regionalMove").path("kind").asText().equals("UNAVAILABLE");
                    for(var transfer:statement.path("additionalTransfers"))
                        partialSequence|=transfer.path("effect").path("kind").asText().equals("UNAVAILABLE");
                }
            }
            if(!topologyContract&&partialSequence!=partialSequenceContract)
                throw new PhysicalShape("$/contractVersion partial MOVE sequence requires SP2.38");
            var logicalTransfers=new java.util.LinkedHashMap<String,java.util.List<PendingLogical>>();
            for(var statement:node.path("statements"))if(statement.has("logicalTransfers")) {
                if(!partialSequenceContract||!statement.path("variant").asText().equals("MOVE")
                        ||!statement.path("logicalTransfers").isArray()||statement.path("logicalTransfers").isEmpty())
                    throw new PhysicalShape("$/statements/logicalTransfers requires SP2.38 MOVE");
                var owner=statement.path("header").path("id").asText();
                var facts=new java.util.ArrayList<PendingLogical>();
                var seen=new java.util.HashSet<String>();
                for(var transfer:statement.path("logicalTransfers")) {
                    if(!transfer.isObject()||transfer.size()!=2||!transfer.path("target").isTextual()
                            ||!transfer.path("value").isObject()||transfer.path("value").size()!=3
                            ||!transfer.path("value").path("logicalDomain").asText().equals("TEXT")
                            ||!transfer.path("value").path("value").isTextual()
                            ||!transfer.path("value").path("logicalExtent").canConvertToInt())
                        throw new PhysicalShape("$/statements/logicalTransfers shape");
                    var id=transfer.path("target").asText();var value=transfer.path("value").path("value").asText();
                    var extent=transfer.path("value").path("logicalExtent").asInt();
                    if(!seen.add(id)||extent<=0||extent!=value.codePointCount(0,value.length()))
                        throw new PhysicalShape("$/statements/logicalTransfers value or duplicate target");
                    facts.add(new PendingLogical(id,value,extent));
                }
                logicalTransfers.put(owner,java.util.List.copyOf(facts));
                ((com.fasterxml.jackson.databind.node.ObjectNode)statement).remove("logicalTransfers");
            }
            boolean ordinaryContract=node.path("contractVersion").asText().equals("2.37.0")
                ||partialSequenceContract&&node.has("ordinaryContinuations");
            java.util.List<OrdinaryContinuationDocument> ordinaryRelations=java.util.List.of();
            if(node.has("ordinaryContinuations") && !ordinaryContract)
                throw new PhysicalShape("$/ordinaryContinuations requires SP2.37");
            if(ordinaryContract) {
                if(!node.path("ordinaryContinuations").isArray())throw new PhysicalShape("$/ordinaryContinuations");
                ordinaryRelations=java.util.Arrays.asList(mapper.treeToValue(node.path("ordinaryContinuations"),OrdinaryContinuationDocument[].class));
                requirePhysical(ordinaryRelations,"$/ordinaryContinuations",meter);
                var ordinarySources=new java.util.HashSet<String>();
                for(var relation:ordinaryRelations)if(!ordinarySources.add(relation.statement()))
                    throw new PhysicalShape("$/ordinaryContinuations requires distinct sources and complete relations");
                ((com.fasterxml.jackson.databind.node.ObjectNode)node).remove("ordinaryContinuations");
                boolean hasStructural=false;for(var statement:node.path("statements"))hasStructural|=statement.has("publicationKind");
                ((com.fasterxml.jackson.databind.node.ObjectNode)node).put("contractVersion",hasStructural?"2.36.0":
                    node.path("storage").has("logicalExactViews")?"2.35.0":"2.33.0");
            }
            if(partialSequenceContract&&!ordinaryContract) {
                boolean hasStructural=false;for(var statement:node.path("statements"))hasStructural|=statement.has("publicationKind");
                ((com.fasterxml.jackson.databind.node.ObjectNode)node).put("contractVersion",hasStructural?"2.36.0":
                    node.path("storage").has("logicalExactViews")?"2.35.0":"2.33.0");
            }
            // SP2.36 adds source structural PERFORM facts, not an executable specialization.
            boolean structuralContract=node.path("contractVersion").asText().equals("2.36.0");
            var structuralPerformIds=new java.util.HashSet<String>();
            var structuralEntries=new java.util.HashMap<String,String>();
            for(var statement:node.path("statements"))if(statement.has("publicationKind")) {
                if(!structuralContract || !statement.path("variant").asText().equals("PERFORM_PROCEDURE")
                        || !statement.path("publicationKind").asText().equals("STRUCTURAL_FACTS")
                        || !structuralPerformIds.add(statement.path("header").path("id").asText()))
                    throw new PhysicalShape("$/statements/publicationKind requires SP2.36 structural PERFORM");
                if(statement.has("targetEntry")) {
                    if(!statement.path("targetEntry").isTextual())throw new PhysicalShape("$/statements/targetEntry");
                    structuralEntries.put(statement.path("header").path("id").asText(),statement.path("targetEntry").asText());
                    ((com.fasterxml.jackson.databind.node.ObjectNode)statement).remove("targetEntry");
                }
                ((com.fasterxml.jackson.databind.node.ObjectNode)statement).remove("publicationKind");
            }
            if(structuralContract) {
                if(structuralPerformIds.isEmpty())throw new PhysicalShape("$/statements missing structural PERFORM facts");
                // The rest of 2.36 is the unchanged 2.33/2.35 typed contract.
                ((com.fasterxml.jackson.databind.node.ObjectNode)node).put("contractVersion",
                    node.path("storage").has("logicalExactViews")?"2.35.0":"2.33.0");
            }
            boolean localExact=java.util.Set.of("2.34.0","2.35.0").contains(node.path("contractVersion").asText());
            java.util.List<LogicalExactViewDocument> logicalExactViews=java.util.List.of();
            if(localExact) {
                var storage=node.path("storage");
                if(!storage.path("version").asText().equals(node.path("contractVersion").asText().equals("2.35.0")?"1.11.0":"1.10.0")
                    ||!storage.path("logicalExactViews").isArray()||storage.path("logicalExactViews").isEmpty())
                    throw new PhysicalShape("$/storage/logicalExactViews");
                logicalExactViews=java.util.Arrays.asList(mapper.treeToValue(storage.path("logicalExactViews"),LogicalExactViewDocument[].class));
                if(node.path("contractVersion").asText().equals("2.34.0")) {
                    var counts=new java.util.HashMap<String,Integer>();
                    var kinds=new java.util.HashMap<String,String>();
                    for(var physical:storage.path("nodes"))kinds.put(physical.path("id").asText(),physical.path("kind").asText());
                    for(var view:logicalExactViews) {
                        counts.merge(view.representative(),1,Integer::sum);
                        if(!"ELEMENTARY".equals(kinds.get(view.node())))throw new PhysicalShape("$/storage/logicalExactViews");
                    }
                    if(counts.values().stream().anyMatch(n->n<2))throw new PhysicalShape("$/storage/logicalExactViews");
                }
                ((com.fasterxml.jackson.databind.node.ObjectNode)storage).remove("logicalExactViews");
                ((com.fasterxml.jackson.databind.node.ObjectNode)storage).put("version",storage.path("logicalTextViews").isArray()?"1.9.0":"1.8.0");
                ((com.fasterxml.jackson.databind.node.ObjectNode)node).put("contractVersion","2.33.0");
            }
            io.github.gustavo2358.lower.domain.SourceFacts.Inventory sourceDependencies=null;
            boolean structuredEvaluate=node.path("contractVersion").asText().equals("2.33.0");
            for(var statement:node.path("statements"))if(statement.path("variant").asText().equals("EVALUATE"))
                for(var arm:statement.path("arms")) {
                    boolean unmodeled=arm.path("selection").isNull();
                    if(unmodeled && (!structuredEvaluate || !arm.path("conditionReads").isArray() || !arm.path("conditionOrigin").isObject()))
                        throw new PhysicalShape("$/statements/EVALUATE/arms");
                    if(!unmodeled && (arm.has("conditionReads") || arm.has("conditionOrigin")))
                        throw new PhysicalShape("$/statements/EVALUATE/arms");
                    if(!unmodeled && java.util.Set.of("2.11.0","2.12.0","2.14.0","2.15.0","2.16.0","2.17.0","2.18.0","2.19.0","2.20.0","2.21.0","2.22.0","2.23.0","2.24.0","2.25.0","2.26.0","2.27.0","2.28.0","2.29.0","2.30.0","2.31.0","2.32.0","2.33.0").contains(node.path("contractVersion").asText())) {
                        ((com.fasterxml.jackson.databind.node.ObjectNode)arm).putNull("conditionReads");
                        ((com.fasterxml.jackson.databind.node.ObjectNode)arm).putNull("conditionOrigin");
                    }
                }
            boolean preservation=structuredEvaluate||node.path("contractVersion").asText().equals("2.32.0");
            if(!preservation)for(var statement:node.path("statements"))if(statement.path("copySemantics").asText().equals("POSSIBLE_TEXT"))throw new PhysicalShape("$/statements/copySemantics");
            if(preservation&&!node.path("sourceDependencies").isObject())((com.fasterxml.jackson.databind.node.ObjectNode)node).put("contractVersion",node.path("storage").path("version").asText().equals("1.9.0")?"2.29.0":"2.28.0");
            if(java.util.Set.of("2.30.0","2.31.0","2.32.0","2.33.0").contains(node.path("contractVersion").asText())) {
                if(!node.path("sourceDependencies").isObject())throw new PhysicalShape("$/sourceDependencies");
                if(node.path("contractVersion").asText().equals("2.30.0"))for(var occurrence:node.path("sourceDependencies").path("occurrences")) {
                    if(occurrence.has("operation")||occurrence.has("access")||occurrence.path("kind").asText().equals("DB2_TABLE")||occurrence.path("resolution").asText().equals("NOT_APPLICABLE"))throw new PhysicalShape("$/sourceDependencies/occurrences");
                    ((com.fasterxml.jackson.databind.node.ObjectNode)occurrence).put("operation","NONE").put("access","NONE");
                }
                sourceDependencies=mapper.treeToValue(node.path("sourceDependencies"),io.github.gustavo2358.lower.domain.SourceFacts.Inventory.class);
                requirePhysical(sourceDependencies,"$/sourceDependencies",meter);
                ((com.fasterxml.jackson.databind.node.ObjectNode)node).remove("sourceDependencies");
                ((com.fasterxml.jackson.databind.node.ObjectNode)node).put("contractVersion",node.path("storage").path("version").asText().equals("1.9.0")?"2.29.0":"2.28.0");
            }
            // SP2.29 adds a typed logical-coordinate inventory; the physical contract remains unchanged.
            boolean logicalTextContract=node.path("contractVersion").asText().equals("2.29.0");
            java.util.List<LogicalTextViewDocument> logicalTextViews=java.util.List.of();
            if(logicalTextContract) {
                var storage=node.path("storage");
                if(!storage.path("version").asText().equals("1.9.0")||!storage.path("logicalTextViews").isArray())throw new PhysicalShape("$/storage/logicalTextViews");
                logicalTextViews=java.util.Arrays.asList(mapper.treeToValue(storage.path("logicalTextViews"),LogicalTextViewDocument[].class));
                ((com.fasterxml.jackson.databind.node.ObjectNode)storage).remove("logicalTextViews");
                ((com.fasterxml.jackson.databind.node.ObjectNode)storage).put("version","1.8.0");
                ((com.fasterxml.jackson.databind.node.ObjectNode)node).put("contractVersion","2.28.0");
            }
            var pending = new ArrayDeque<JsonNode>(); pending.push(node);
            while (!pending.isEmpty()) {
                meter.nodes++;
                pending.pop().elements().forEachRemaining(pending::push);
            }
            if (!node.path("schema").isTextual() || !node.path("contractVersion").isTextual()) return reject(Code.INPUT_ERROR, "$/schema,contractVersion");
            if (!node.path("schema").textValue().equals("cobol-semantic-product")) return reject(Code.UNSUPPORTED_CONTRACT, "$/schema");
            if(!node.path("contractVersion").asText().equals("2.28.0"))for(var statement:node.path("statements"))
                if(statement.path("variant").asText().equals("CICS_FILE_CONTROL"))return reject(Code.UNSUPPORTED_CONTRACT,"$/statements/CICS_FILE_CONTROL requires SP2.28");
            if(!java.util.Set.of("2.27.0","2.28.0").contains(node.path("contractVersion").asText())&&node.path("fileInventory").path("declarations").findValues("organization").stream().anyMatch(o->o.asText().equals("LINE_SEQUENTIAL")))return reject(Code.UNSUPPORTED_CONTRACT,"$/fileInventory LINE_SEQUENTIAL requires SP2.27");
            if(!java.util.Set.of("2.26.0","2.27.0","2.28.0").contains(node.path("contractVersion").asText())) {
                if(node.path("statementEffects").findValues("proof").stream().anyMatch(p->p.asText().equals("NO_OP")))return reject(Code.UNSUPPORTED_CONTRACT,"$/statementEffects NO_OP requires SP2.26");
                if(node.path("fileInventory").path("operations").path("uses").findValues("command").stream().anyMatch(c->java.util.Set.of("SORT","MERGE","RELEASE","RETURN").contains(c.asText())))return reject(Code.UNSUPPORTED_CONTRACT,"$/fileInventory SORT family requires SP2.26");
            }
            if(!java.util.Set.of("2.25.0","2.26.0","2.27.0","2.28.0").contains(node.path("contractVersion").asText())&&node.findValues("branch").stream().anyMatch(b->b.asText().equals("FILE_HANDLER")))return reject(Code.UNSUPPORTED_CONTRACT,"$/branch FILE_HANDLER requires SP2.25");
            if(!java.util.Set.of("2.24.0","2.25.0","2.26.0","2.27.0","2.28.0").contains(node.path("contractVersion").asText()))for(var base:node.path("storage").path("bases"))
                if(base.path("allocation").asText().equals("INDEPENDENT_LOCAL_STORAGE"))return reject(Code.UNSUPPORTED_CONTRACT,"$/storage/allocation requires SP 2.24");
            if(!java.util.Set.of("2.14.0","2.15.0","2.16.0","2.17.0","2.18.0","2.19.0","2.20.0","2.21.0","2.22.0","2.23.0","2.24.0","2.25.0","2.26.0","2.27.0","2.28.0").contains(node.path("contractVersion").textValue()))
                for(var statement:node.path("statements"))if(statement.path("variant").asText().equals("CICS_PROGRAM_CONTROL"))return reject(Code.UNSUPPORTED_CONTRACT,"$/statements/CICS_PROGRAM_CONTROL");
            if(!java.util.Set.of("2.20.0","2.21.0","2.22.0","2.23.0","2.24.0","2.25.0","2.26.0","2.27.0","2.28.0").contains(node.path("contractVersion").textValue())&&node.findValues("kind").stream().anyMatch(k->k.asText().equals("LOGICAL_FIT_TEXT")))return reject(Code.UNSUPPORTED_CONTRACT,"$/LOGICAL_FIT_TEXT requires SP 2.20");
            boolean logicalAccessContract=java.util.Set.of("2.19.0","2.20.0","2.21.0","2.22.0","2.23.0","2.24.0","2.25.0","2.26.0","2.27.0","2.28.0").contains(node.path("contractVersion").textValue());
            if(!logicalAccessContract&&!node.findValues("logicalWholeItem").isEmpty())return reject(Code.UNSUPPORTED_CONTRACT,"$/logicalWholeItem requires SP 2.19");
            if(logicalAccessContract)for(var ref:node.findParents("binding"))if(ref.has("role")&&ref.has("id")&&!ref.has("logicalWholeItem"))throw new PhysicalShape("$/reference/logicalWholeItem");
            if(java.util.Set.of("2.11.0","2.12.0","2.14.0","2.15.0","2.16.0","2.17.0","2.18.0").contains(node.path("contractVersion").textValue()))
                for(var ref:node.findParents("binding"))if(ref.has("role")&&ref.has("id"))((com.fasterxml.jackson.databind.node.ObjectNode)ref).putNull("logicalWholeItem");
            for(var condition:node.path("storage").path("entryState").path("conditions")) {
                if(logicalAccessContract&&!condition.has("logicalText"))throw new PhysicalShape("$/storage/entryState/conditions/logicalText");
                if(!logicalAccessContract&&condition.has("logicalText"))return reject(Code.UNSUPPORTED_CONTRACT,"$/logicalText requires SP 2.19");
                if(java.util.Set.of("2.15.0","2.16.0","2.17.0","2.18.0").contains(node.path("contractVersion").textValue()))
                    ((com.fasterxml.jackson.databind.node.ObjectNode)condition).putNull("logicalText");
            }
            var alternatives=node.findValues("regionalAlternatives");
            if(!java.util.Set.of("2.18.0","2.19.0","2.20.0","2.21.0","2.22.0","2.23.0","2.24.0","2.25.0","2.26.0","2.27.0","2.28.0").contains(node.path("contractVersion").textValue())&&!alternatives.isEmpty())
                return reject(Code.UNSUPPORTED_CONTRACT,"$/regionalAlternatives requires SP 2.18");
            if(java.util.Set.of("2.18.0","2.19.0","2.20.0","2.21.0","2.22.0","2.23.0","2.24.0","2.25.0","2.26.0","2.27.0","2.28.0").contains(node.path("contractVersion").textValue())) {
                for(var choices:alternatives)if(!choices.isArray())throw new PhysicalShape("$/regionalAlternatives");
                for(var reference:node.findParents("binding"))if(reference.has("role")&&reference.has("id")&&!reference.path("regionalAlternatives").isArray())
                    throw new PhysicalShape("$/reference/regionalAlternatives");
            }
            // Shared typed reference records also read the prior closed wire profiles.
            // Those contracts had no physical-alternative field; absent means no such proof,
            // not an exhaustive empty binding or a no-effect claim. New-version omission rejects above.
            if(java.util.Set.of("2.11.0","2.12.0","2.14.0","2.15.0","2.16.0","2.17.0").contains(node.path("contractVersion").textValue()))
                for(var reference:node.findParents("binding"))if(reference.has("role")&&reference.has("id"))
                    ((com.fasterxml.jackson.databind.node.ObjectNode)reference).putArray("regionalAlternatives");
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
                case "2.2.0" -> {
                    var wire = mapper.treeToValue(node, Wire22.Document.class);
                    requirePhysical(wire, "$", meter); requireCoherent22(wire); input = Materialize.input(wire);
                }
                case "2.3.0" -> {
                    var wire = mapper.treeToValue(node, Wire23.Document.class);
                    requirePhysical(wire, "$", meter); requireCoherent23(wire); input = Materialize.input(wire);
                }
                case "2.4.0" -> {
                    var wire = mapper.treeToValue(node, Wire24.Document.class);
                    requirePhysical(wire, "$", meter); requireCoherent24(wire); input = Materialize.input(wire);
                }
                case "2.7.0" -> {
                    var wire = mapper.treeToValue(node, Wire27.Document.class);
                    requirePhysical(wire, "$", meter); requireCoherent27(wire); input = Materialize.input(wire);
                }
                case "2.27.0", "2.28.0" -> {
                    var wire=mapper.treeToValue(node,Wire227.Document.class);requirePhysical(wire,"$",meter);
                    if(!wire.storage().version().equals("1.8.0")||!wire.fileInventory().version().equals("1.6.0"))throw new PhysicalShape("$/storage/version,fileInventory/version");
                    requireCoherentFacts211(Wire215.common(Wire217.common(Wire221.common(Wire223.common(Wire224.common(Wire225.common(Wire226.common(Wire227.common(wire)))))))));input=Materialize.input(wire);
                }
                case "2.26.0" -> {
                    var wire=mapper.treeToValue(node,Wire226.Document.class);requirePhysical(wire,"$",meter);
                    if(!wire.storage().version().equals("1.8.0")||!wire.fileInventory().version().equals("1.5.0"))throw new PhysicalShape("$/storage/version,fileInventory/version");
                    requireCoherentFacts211(Wire215.common(Wire217.common(Wire221.common(Wire223.common(Wire224.common(Wire225.common(Wire226.common(wire))))))));input=Materialize.input(wire);
                }
                case "2.25.0" -> {
                    var wire=mapper.treeToValue(node,Wire225.Document.class);requirePhysical(wire,"$",meter);
                    if(!wire.storage().version().equals("1.8.0")||!wire.fileInventory().version().equals("1.4.0"))throw new PhysicalShape("$/storage/version,fileInventory/version");
                    requireCoherentFacts211(Wire215.common(Wire217.common(Wire221.common(Wire223.common(Wire224.common(Wire225.common(wire)))))));input=Materialize.input(wire);
                }
                case "2.24.0" -> {
                    var wire=mapper.treeToValue(node,Wire224.Document.class);requirePhysical(wire,"$",meter);
                    if(!wire.storage().version().equals("1.8.0")||!wire.fileInventory().version().equals("1.3.0"))throw new PhysicalShape("$/storage/version,fileInventory/version");
                    requireCoherentFacts211(Wire215.common(Wire217.common(Wire221.common(Wire223.common(Wire224.common(wire))))));input=Materialize.input(wire);
                }
                case "2.23.0" -> {
                    var wire=mapper.treeToValue(node,Wire223.Document.class);requirePhysical(wire,"$",meter);
                    if(!wire.storage().version().equals("1.7.0")||!wire.fileInventory().version().equals("1.2.0"))throw new PhysicalShape("$/storage/version,fileInventory/version");
                    requireCoherentFacts211(Wire215.common(Wire217.common(Wire221.common(Wire223.common(wire)))));input=Materialize.input(wire);
                }
                case "2.22.0" -> {
                    var wire=mapper.treeToValue(node,Wire222.Document.class);requirePhysical(wire,"$",meter);
                    if(!wire.storage().version().equals("1.7.0")||!wire.fileInventory().version().equals("1.1.0"))throw new PhysicalShape("$/storage/version,fileInventory/version");
                    requireCoherentFacts211(Wire215.common(Wire217.common(Wire221.common(Wire222.common(wire)))));input=Materialize.input(wire);
                }
                case "2.21.0" -> {
                    var wire = mapper.treeToValue(node, Wire221.Document.class); requirePhysical(wire,"$",meter);
                    if (!wire.storage().version().equals("1.7.0") || !wire.fileInventory().version().equals("1.0.0"))
                        throw new PhysicalShape("$/storage/version,fileInventory/version");
                    requireCoherentFacts211(Wire215.common(Wire217.common(Wire221.common(wire)))); input = Materialize.input(wire);
                }
                case "2.17.0","2.18.0","2.19.0","2.20.0" -> {
                    var wire=mapper.treeToValue(node,Wire217.Document.class);requirePhysical(wire,"$",meter);
                    if(!wire.storage().version().equals(node.path("contractVersion").textValue().equals("2.20.0")?"1.7.0":node.path("contractVersion").textValue().equals("2.19.0")?"1.6.0":"1.5.0"))throw new PhysicalShape("$/storage/version");
                    requireCoherentFacts211(Wire215.common(Wire217.common(wire)));input=Materialize.input(wire);
                }
                case "2.15.0", "2.16.0" -> {
                    var wire=mapper.treeToValue(node,Wire215.Document.class);requirePhysical(wire,"$",meter);
                    if(!wire.storage().version().equals(node.path("contractVersion").textValue().equals("2.16.0")?"1.5.0":"1.4.0"))throw new PhysicalShape("$/storage/version");
                    requireCoherentFacts211(Wire215.common(wire));input=Materialize.input(wire);
                }
                case "2.12.0", "2.14.0" -> {
                    var wire=mapper.treeToValue(node,Wire212.Document.class);requirePhysical(wire,"$",meter);
                    if(!wire.storage().version().equals("1.3.0"))throw new PhysicalShape("$/storage/version");
                    requireCoherentFacts211(Wire212.common(wire));input=Materialize.input(wire);
                }
                case "2.11.0" -> {
                    var wire=mapper.treeToValue(node,Wire211.Document.class);
                    requirePhysical(wire,"$",meter);
                    if(!wire.storage().version().equals("1.2.0"))throw new PhysicalShape("$/storage/version");
                    requireCoherentFacts211(wire);input=Materialize.input(wire);
                }
                case "2.10.0" -> {
                    var wire=mapper.treeToValue(node,Wire210.Document.class);
                    requirePhysical(wire,"$",meter);
                    if(!wire.storage().version().equals("1.2.0"))throw new PhysicalShape("$/storage/version");
                    requireCoherentFacts210(wire);input=Materialize.input(wire);
                }
                case "2.9.0" -> {
                    var wire=mapper.treeToValue(node,Wire29.Document.class);
                    requirePhysical(wire,"$",meter);
                    if(!wire.storage().version().equals("1.2.0"))throw new PhysicalShape("$/storage/version");
                    requireCoherentFacts27(Wire28.common(Wire29.common(wire)));input=Materialize.input(wire);
                }
                case "2.8.0" -> {
                    var wire=mapper.treeToValue(node,Wire28.Document.class);
                    requirePhysical(wire,"$",meter);
                    if(!wire.storage().version().equals("1.1.0"))throw new PhysicalShape("$/storage/version");
                    requireCoherentFacts27(Wire28.common(wire));input=Materialize.input(wire);
                }
                case "2.6.0" -> {
                    var wire = mapper.treeToValue(node, Wire26.Document.class);
                    requirePhysical(wire, "$", meter); requireCoherent26(wire); input = Materialize.input(wire);
                }
                case "2.5.0" -> {
                    var wire = mapper.treeToValue(node, Wire25.Document.class);
                    requirePhysical(wire, "$", meter); requireCoherent25(wire); input = Materialize.input(wire);
                }
                case "2.1.0" -> {
                    var wire = mapper.treeToValue(node, Wire21.Document.class);
                    requirePhysical(wire, "$", meter); requireCoherent21(wire); input = Materialize.input(wire);
                }
                default -> { return reject(Code.UNSUPPORTED_CONTRACT, "$/contractVersion"); }
            }
            if(!java.util.Set.of("2.16.0","2.17.0","2.18.0","2.19.0","2.20.0","2.21.0","2.22.0","2.23.0","2.24.0","2.25.0","2.26.0","2.27.0","2.28.0").contains(node.path("contractVersion").textValue())&&input.storage().isPresent())
                for(var condition:input.storage().get().entryState().conditions())
                    if(condition.kind()==io.github.gustavo2358.lower.domain.StorageFacts.InitialKind.POSSIBLE_LITERAL_BYTES
                        ||condition.proof()==io.github.gustavo2358.lower.domain.StorageFacts.InitialProof.DECLARATIVE_POSSIBILITY)
                        throw new PhysicalShape("$/storage/entryState/conditions/possible-requires-SP2.16");
            if (!java.util.Set.of("2.5.0","2.6.0","2.7.0","2.8.0","2.9.0","2.10.0","2.11.0","2.12.0","2.14.0","2.15.0","2.16.0","2.17.0","2.18.0","2.19.0","2.20.0","2.21.0","2.22.0","2.23.0","2.24.0","2.25.0","2.26.0","2.27.0","2.28.0").contains(node.path("contractVersion").textValue())) for (var statement : input.statements()) {
                var predicate = statement instanceof SpInput.IfFact f ? f.predicateGuarantee()
                    : statement instanceof SpInput.ProcedurePerformFact p ? p.loop().map(SpInput.PerformLoop::predicate).orElse(null) : null;
                if (predicate != null && predicate.profile()==SpInput.PredicateProfile.NUMERIC_RELATION)
                    throw new PhysicalShape("$/statements/predicate/profile");
            }
            if(!java.util.Set.of("2.11.0","2.12.0","2.14.0","2.15.0","2.16.0","2.17.0","2.18.0","2.19.0","2.20.0","2.21.0","2.22.0","2.23.0","2.24.0","2.25.0","2.26.0","2.27.0","2.28.0").contains(node.path("contractVersion").textValue()))for(var statement:input.statements()) {
                if(statement instanceof SpInput.MoveFact m&&m.regionalMove().filter(e->e.kind()==io.github.gustavo2358.lower.domain.StorageFacts.MoveKind.FIT_TEXT||e.kind()==io.github.gustavo2358.lower.domain.StorageFacts.MoveKind.FITTED_LITERAL_BYTES).isPresent())
                    throw new PhysicalShape("$/statements/regionalMove/kind/version");
            }
            var variants = input.statements().stream().filter(SpInput.OtherStatement.class::isInstance)
                .map(SpInput.OtherStatement.class::cast).map(v -> new UnsupportedVariant(v.header().id(), v.variant())).toList();
            if(logicalTextContract) {
                var st=input.storage().orElseThrow();var unit=input.unit();
                var logical=logicalTextViews.stream().map(v->new io.github.gustavo2358.lower.domain.StorageFacts.LogicalTextView(
                    new io.github.gustavo2358.lower.domain.StorageFacts.NodeId(unit,v.node()),new io.github.gustavo2358.lower.domain.StorageFacts.NodeId(unit,v.root()),
                    new java.math.BigInteger(v.start()),new java.math.BigInteger(v.length()))).toList();
                var inventory=new io.github.gustavo2358.lower.domain.StorageFacts.Inventory(st.profile(),st.profileId(),st.runtimeCodec(),st.nodes(),st.bases(),st.views(),st.gapCodes(),st.relations(),st.renames(),st.entryState(),logical);
                input=new SpInput(input.unit(),input.policy(),input.dataDeclarations(),input.statements(),input.structure(),input.gaps(),input.coverage(),input.entryInventory(),input.storageIndependence(),input.compositional(),java.util.Optional.of(inventory),input.fileInventory());
            }
            if(localExact) {
                var st=input.storage().orElseThrow();var unit=input.unit();
                var exact=logicalExactViews.stream().map(v->new io.github.gustavo2358.lower.domain.StorageFacts.LogicalExactView(
                    new io.github.gustavo2358.lower.domain.StorageFacts.NodeId(unit,v.node()),new io.github.gustavo2358.lower.domain.StorageFacts.NodeId(unit,v.representative()),
                    new java.math.BigInteger(v.length()))).toList();
                var inventory=new io.github.gustavo2358.lower.domain.StorageFacts.Inventory(st.profile(),st.profileId(),st.runtimeCodec(),st.nodes(),st.bases(),st.views(),st.gapCodes(),st.relations(),st.renames(),st.entryState(),st.logicalTextViews(),exact);
                input=new SpInput(input.unit(),input.policy(),input.dataDeclarations(),input.statements(),input.structure(),input.gaps(),input.coverage(),input.entryInventory(),input.storageIndependence(),input.compositional(),java.util.Optional.of(inventory),input.fileInventory());
            }
            if(sourceDependencies!=null)input=new SpInput(input.unit(),input.policy(),input.dataDeclarations(),input.statements(),input.structure(),input.gaps(),input.coverage(),input.entryInventory(),input.storageIndependence(),input.compositional(),input.storage(),input.fileInventory(),sourceDependencies);
            if(structuralContract) {
                var statements=new java.util.ArrayList<SpInput.StatementFact>();
                for(var fact:input.statements()) {
                    if(structuralPerformIds.contains(fact.header().id().handle())) {
                        if(!(fact instanceof SpInput.ProcedurePerformFact p))throw new PhysicalShape("$/statements/structural PERFORM");
                        fact=new SpInput.ProcedurePerformFact(p.header(),p.start(),p.end(),p.procedures(),p.normalContinuation(),
                            p.loop(),p.times(),p.varying(),p.gapCodes(),SpInput.PerformPublicationKind.STRUCTURAL_FACTS,
                            java.util.Optional.ofNullable(structuralEntries.get(p.header().id().handle())).map(id->new SpInput.StatementId(p.header().id().unit(),id)));
                    }
                    statements.add(fact);
                }
                input=new SpInput(input.unit(),input.policy(),input.dataDeclarations(),statements,input.structure(),input.gaps(),
                    input.coverage(),input.entryInventory(),input.storageIndependence(),input.compositional(),input.storage(),input.fileInventory(),input.sourceDependencies());
            }
            if(ordinaryContract) {
                var relations=new java.util.LinkedHashMap<SpInput.StatementId,SpInput.NormalContinuation>();
                for(var relation:ordinaryRelations)relations.put(new SpInput.StatementId(input.unit(),relation.statement()),
                    new SpInput.NormalContinuation(SpInput.ContinuationAvailability.KNOWN,
                        java.util.Optional.of(new SpInput.StatementId(input.unit(),relation.destination())),Materialize.provenance(relation.provenance(),input.unit())));
                input=new SpInput(input.unit(),input.policy(),input.dataDeclarations(),input.statements(),input.structure(),input.gaps(),
                    input.coverage(),input.entryInventory(),input.storageIndependence(),input.compositional(),input.storage(),input.fileInventory(),input.sourceDependencies(),relations);
            }
            if(!logicalTransfers.isEmpty()) {
                var statements=new java.util.ArrayList<SpInput.StatementFact>();
                var matched=new java.util.HashSet<String>();
                for(var fact:input.statements()) {
                    var logicalPending=logicalTransfers.get(fact.header().id().handle());
                    if(logicalPending!=null) {
                        if(!(fact instanceof SpInput.MoveFact m))throw new PhysicalShape("$/statements/logicalTransfers non-MOVE");
                        matched.add(fact.header().id().handle());
                        var values=logicalPending.stream().map(t->new SpInput.LogicalTransfer(new SpInput.OperandId(m.header().id(),t.target()),
                            new SpInput.LogicalValue(SpInput.LogicalDomain.TEXT,t.value(),t.extent()))).toList();
                        fact=new SpInput.MoveFact(m.header(),m.source(),m.target(),m.copySemantics(),m.normalContinuation(),
                            m.textAdjustment(),m.regionalMove(),m.additionalTransfers(),values);
                    }
                    statements.add(fact);
                }
                if(matched.size()!=logicalTransfers.size())throw new PhysicalShape("$/statements/logicalTransfers owner");
                input=new SpInput(input.unit(),input.policy(),input.dataDeclarations(),statements,input.structure(),input.gaps(),
                    input.coverage(),input.entryInventory(),input.storageIndependence(),input.compositional(),input.storage(),input.fileInventory(),input.sourceDependencies(),input.ordinaryContinuations());
            }
            if(topologyContract)input=new SpInput(input.unit(),input.policy(),input.dataDeclarations(),input.statements(),input.structure(),input.gaps(),
                input.coverage(),input.entryInventory(),input.storageIndependence(),input.compositional(),input.storage(),input.fileInventory(),input.sourceDependencies(),input.ordinaryContinuations(),java.util.Optional.of(topology),java.util.Optional.ofNullable(factDependencies),java.util.Optional.ofNullable(nominalValues));
            return new Decoded(input, variants);
        } catch (StreamConstraintsException ex) {
            return reject(Code.IMPLEMENTATION_LIMIT, "$ limits");
        } catch (JsonProcessingException ex) {
            var location = ex.getLocation();
            return reject(Code.INPUT_ERROR, location == null ? "$ DTO" : "line:" + location.getLineNr() + ",column:" + location.getColumnNr());
        } catch (CharacterCodingException ex) {
            return reject(Code.INPUT_ERROR, "$ UTF-8");
        } catch (Materialize.EffectShape ex) {
            return reject(Code.INPUT_ERROR,"$/statementEffects/"+ex.getMessage());
        } catch (PhysicalShape ex) {
            return reject(Code.INPUT_ERROR, ex.getMessage());
        } catch (IllegalArgumentException ex) {
            return reject(Code.INPUT_ERROR, "$/typed-contract: "+ex.getMessage());
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

    private static void requireCoherent21(Wire21.Document wire) {
        for (var data : wire.dataDeclarations()) if (data.scalarText() != null && data.scalarText().logicalExtent() <= 0)
            throw new PhysicalShape("$/dataDeclarations/scalarText/logicalExtent");
        for (var statement : wire.statements()) {
            if (statement instanceof Wire21.EvaluateDocument e) for (var a : e.arms()) {
                if (!(a.selection() instanceof Wire21.LiteralDocument l) || l.kind()!=SpInput.LiteralKind.ALPHANUMERIC
                        || l.logicalValue()==null || !l.value().equals(l.logicalValue().value()))
                    throw new PhysicalShape("$/statements/EVALUATE/arms/selection");
                logical21(l.logicalValue());
            }
            if (statement instanceof Wire21.MoveDocument m && m.source() instanceof Wire21.LiteralDocument literal && literal.logicalValue() != null) {
                var value = literal.logicalValue();
                if (literal.kind() != SpInput.LiteralKind.ALPHANUMERIC || !literal.value().equals(value.value()))
                    throw new PhysicalShape("$/statements/source/logicalValue");
                logical21(value);
            }
            if (statement instanceof Wire21.MoveDocument m && m.textAdjustment() != null) logical21(m.textAdjustment().result());
            if (statement instanceof Wire21.CallDocument c && c.target() instanceof Wire21.LiteralTargetDocument l && l.logicalValue() != null) {
                logical21(l.logicalValue());
                if (!l.text().equals(l.logicalValue().value())) throw new PhysicalShape("$/statements/target/text");
            }
        }
    }
    private static void logical21(Wire21.LogicalDocument value) {
        if (value.logicalExtent() < 0 || value.logicalExtent() != value.value().codePointCount(0, value.value().length()))
            throw new PhysicalShape("$/logicalValue/logicalExtent");
    }

    private static void requireCoherent22(Wire22.Document wire) {
        for (var data : wire.dataDeclarations()) if (data.scalarText() != null && data.scalarText().logicalExtent() <= 0)
            throw new PhysicalShape("$/dataDeclarations/scalarText/logicalExtent");
        for (var statement : wire.statements()) {
            if (statement instanceof Wire22.EvaluateDocument e) for (var a : e.arms()) {
                if (!(a.selection() instanceof Wire22.LiteralDocument l) || l.kind()!=SpInput.LiteralKind.ALPHANUMERIC
                        || l.logicalValue()==null || !l.value().equals(l.logicalValue().value()))
                    throw new PhysicalShape("$/statements/EVALUATE/arms/selection");
                logical22(l.logicalValue());
            }
            if (statement instanceof Wire22.MoveDocument m && m.source() instanceof Wire22.LiteralDocument literal && literal.logicalValue() != null) {
                var value = literal.logicalValue();
                if (literal.kind() != SpInput.LiteralKind.ALPHANUMERIC || !literal.value().equals(value.value()))
                    throw new PhysicalShape("$/statements/source/logicalValue");
                logical22(value);
            }
            if (statement instanceof Wire22.MoveDocument m && m.textAdjustment() != null) logical22(m.textAdjustment().result());
            if (statement instanceof Wire22.CallDocument c && c.target() instanceof Wire22.LiteralTargetDocument l && l.logicalValue() != null) {
                logical22(l.logicalValue());
                if (!l.text().equals(l.logicalValue().value())) throw new PhysicalShape("$/statements/target/text");
            }
        }
    }
    private static void logical22(Wire22.LogicalDocument value) {
        if (value.logicalExtent() < 0 || value.logicalExtent() != value.value().codePointCount(0, value.value().length()))
            throw new PhysicalShape("$/logicalValue/logicalExtent");
    }

    private static void requireCoherent23(Wire23.Document wire) {
        for (var data : wire.dataDeclarations()) if (data.scalarText() != null && data.scalarText().logicalExtent() <= 0)
            throw new PhysicalShape("$/dataDeclarations/scalarText/logicalExtent");
        for (var statement : wire.statements()) {
            if (statement instanceof Wire23.EvaluateDocument e) for (var a : e.arms()) {
                if (!(a.selection() instanceof Wire23.LiteralDocument l) || l.kind()!=SpInput.LiteralKind.ALPHANUMERIC
                        || l.logicalValue()==null || !l.value().equals(l.logicalValue().value()))
                    throw new PhysicalShape("$/statements/EVALUATE/arms/selection");
                logical23(l.logicalValue());
            }
            if (statement instanceof Wire23.MoveDocument m && m.source() instanceof Wire23.LiteralDocument literal && literal.logicalValue() != null) {
                var value = literal.logicalValue();
                if (literal.kind() != SpInput.LiteralKind.ALPHANUMERIC || !literal.value().equals(value.value()))
                    throw new PhysicalShape("$/statements/source/logicalValue");
                logical23(value);
            }
            if (statement instanceof Wire23.MoveDocument m && m.textAdjustment() != null) logical23(m.textAdjustment().result());
            if (statement instanceof Wire23.CallDocument c && c.target() instanceof Wire23.LiteralTargetDocument l && l.logicalValue() != null) {
                logical23(l.logicalValue());
                if (!l.text().equals(l.logicalValue().value())) throw new PhysicalShape("$/statements/target/text");
            }
        }
    }
    private static void logical23(Wire23.LogicalDocument value) {
        if (value.logicalExtent() < 0 || value.logicalExtent() != value.value().codePointCount(0, value.value().length()))
            throw new PhysicalShape("$/logicalValue/logicalExtent");
    }

    private static void requireCoherent24(Wire24.Document wire) {
        for (var data : wire.dataDeclarations()) if (data.scalarText() != null && data.scalarText().logicalExtent() <= 0)
            throw new PhysicalShape("$/dataDeclarations/scalarText/logicalExtent");
        for (var statement : wire.statements()) {
            if (statement instanceof Wire24.EvaluateDocument e) for (var a : e.arms()) {
                if (!(a.selection() instanceof Wire24.LiteralDocument l) || l.kind()!=SpInput.LiteralKind.ALPHANUMERIC
                        || l.logicalValue()==null || !l.value().equals(l.logicalValue().value()))
                    throw new PhysicalShape("$/statements/EVALUATE/arms/selection");
                logical24(l.logicalValue());
            }
            if (statement instanceof Wire24.MoveDocument m && m.source() instanceof Wire24.LiteralDocument literal && literal.logicalValue() != null) {
                var value = literal.logicalValue();
                if (literal.kind() != SpInput.LiteralKind.ALPHANUMERIC || !literal.value().equals(value.value()))
                    throw new PhysicalShape("$/statements/source/logicalValue");
                logical24(value);
            }
            if (statement instanceof Wire24.MoveDocument m && m.textAdjustment() != null) logical24(m.textAdjustment().result());
            if (statement instanceof Wire24.CallDocument c && c.target() instanceof Wire24.LiteralTargetDocument l && l.logicalValue() != null) {
                logical24(l.logicalValue());
                if (!l.text().equals(l.logicalValue().value())) throw new PhysicalShape("$/statements/target/text");
            }
        }
    }
    private static void logical24(Wire24.LogicalDocument value) {
        if (value.logicalExtent() < 0 || value.logicalExtent() != value.value().codePointCount(0, value.value().length()))
            throw new PhysicalShape("$/logicalValue/logicalExtent");
    }

    private static void requireCoherent25(Wire25.Document wire) {
        for (var data : wire.dataDeclarations()) if (data.scalarText() != null && data.scalarText().logicalExtent() <= 0)
            throw new PhysicalShape("$/dataDeclarations/scalarText/logicalExtent");
        for (var statement : wire.statements()) {
            if (statement instanceof Wire25.EvaluateDocument e) for (var a : e.arms()) {
                if (!(a.selection() instanceof Wire25.LiteralDocument l) || l.kind()!=SpInput.LiteralKind.ALPHANUMERIC
                        || l.logicalValue()==null || !l.value().equals(l.logicalValue().value()))
                    throw new PhysicalShape("$/statements/EVALUATE/arms/selection");
                logical25(l.logicalValue());
            }
            if (statement instanceof Wire25.MoveDocument m && m.source() instanceof Wire25.LiteralDocument literal && literal.logicalValue() != null) {
                var value = literal.logicalValue();
                if (literal.kind() != SpInput.LiteralKind.ALPHANUMERIC || !literal.value().equals(value.value()))
                    throw new PhysicalShape("$/statements/source/logicalValue");
                logical25(value);
            }
            if (statement instanceof Wire25.MoveDocument m && m.textAdjustment() != null) logical25(m.textAdjustment().result());
            if (statement instanceof Wire25.CallDocument c && c.target() instanceof Wire25.LiteralTargetDocument l && l.logicalValue() != null) {
                logical25(l.logicalValue());
                if (!l.text().equals(l.logicalValue().value())) throw new PhysicalShape("$/statements/target/text");
            }
        }
    }
    private static void logical25(Wire25.LogicalDocument value) {
        if (value.logicalExtent() < 0 || value.logicalExtent() != value.value().codePointCount(0, value.value().length()))
            throw new PhysicalShape("$/logicalValue/logicalExtent");
    }

    private static void requireCoherent26(Wire26.Document wire) {
        for (var data : wire.dataDeclarations()) if (data.scalarText() != null && data.scalarText().logicalExtent() <= 0)
            throw new PhysicalShape("$/dataDeclarations/scalarText/logicalExtent");
        for (var statement : wire.statements()) {
            if (statement instanceof Wire26.EvaluateDocument e) for (var a : e.arms()) {
                if (!(a.selection() instanceof Wire26.LiteralDocument l) || l.kind()!=SpInput.LiteralKind.ALPHANUMERIC
                        || l.logicalValue()==null || !l.value().equals(l.logicalValue().value()))
                    throw new PhysicalShape("$/statements/EVALUATE/arms/selection");
                logical26(l.logicalValue());
            }
            if (statement instanceof Wire26.MoveDocument m && m.source() instanceof Wire26.LiteralDocument literal && literal.logicalValue() != null) {
                var value = literal.logicalValue();
                if (literal.kind() != SpInput.LiteralKind.ALPHANUMERIC || !literal.value().equals(value.value()))
                    throw new PhysicalShape("$/statements/source/logicalValue");
                logical26(value);
            }
            if (statement instanceof Wire26.MoveDocument m && m.textAdjustment() != null) logical26(m.textAdjustment().result());
            if (statement instanceof Wire26.CallDocument c && c.target() instanceof Wire26.LiteralTargetDocument l && l.logicalValue() != null) {
                logical26(l.logicalValue());
                if (!l.text().equals(l.logicalValue().value())) throw new PhysicalShape("$/statements/target/text");
            }
        }
    }
    private static void logical26(Wire26.LogicalDocument value) {
        if (value.logicalExtent() < 0 || value.logicalExtent() != value.value().codePointCount(0, value.value().length()))
            throw new PhysicalShape("$/logicalValue/logicalExtent");
    }

    private static void requireCoherent27(Wire27.Document wire) {
        if (!wire.storage().version().equals("1.0.0")) throw new PhysicalShape("$/storage/version");
        requireCoherentFacts27(wire);
    }
    private static void requireCoherentFacts211(Wire211.Document wire) {
        wire.storage().nodes().forEach(n->measure211(n.extent()));
        wire.storage().bases().forEach(b->measure211(b.extent()));
        wire.storage().views().forEach(v->{ measure211(v.offset()); measure211(v.extent()); });
        for (var data : wire.dataDeclarations()) if (data.scalarText() != null && data.scalarText().logicalExtent() <= 0)
            throw new PhysicalShape("$/dataDeclarations/scalarText/logicalExtent");
        for (var statement : wire.statements()) {
            if(statement instanceof Wire211.CicsFileDocument f)for(var option:f.options())
                if(option.integer()!=null&&!option.integer().matches("0|-?[1-9][0-9]*"))throw new PhysicalShape("$/statements/CICS_FILE_CONTROL/options/integer");
            if(statement instanceof Wire211.MoveDocument m)for(var transfer:m.additionalTransfers()) {
                if(transfer.source() instanceof Wire211.LiteralDocument literal&&literal.logicalValue()!=null) {
                    logical211(literal.logicalValue());
                    if(literal.kind()!=SpInput.LiteralKind.ALPHANUMERIC||!literal.value().equals(literal.logicalValue().value()))throw new PhysicalShape("$/statements/additionalTransfers/source/logicalValue");
                }
            }
            if (statement instanceof Wire211.EvaluateDocument e) for (var a : e.arms()) {
                if(a.selection()==null) {
                    if(a.conditionReads()==null || a.conditionOrigin()==null)throw new PhysicalShape("$/statements/EVALUATE/arms/condition");
                    continue;
                }
                if (!(a.selection() instanceof Wire211.LiteralDocument l) || l.kind()!=SpInput.LiteralKind.ALPHANUMERIC
                        || l.logicalValue()==null || !l.value().equals(l.logicalValue().value()))
                    throw new PhysicalShape("$/statements/EVALUATE/arms/selection");
                logical211(l.logicalValue());
            }
            if (statement instanceof Wire211.MoveDocument m && m.source() instanceof Wire211.LiteralDocument literal && literal.logicalValue() != null) {
                var value = literal.logicalValue();
                if (literal.kind() != SpInput.LiteralKind.ALPHANUMERIC || !literal.value().equals(value.value()))
                    throw new PhysicalShape("$/statements/source/logicalValue");
                logical211(value);
            }
            if (statement instanceof Wire211.MoveDocument m && m.textAdjustment() != null) logical211(m.textAdjustment().result());
            if (statement instanceof Wire211.CallDocument c && c.target() instanceof Wire211.LiteralTargetDocument l && l.logicalValue() != null) {
                logical211(l.logicalValue());
                if (!l.text().equals(l.logicalValue().value())) throw new PhysicalShape("$/statements/target/text");
            }
        }
    }
    private static void measure211(Wire211.MeasureDocument m) {
        if (m.value()!=null && !m.value().matches("0|[1-9][0-9]*")) throw new PhysicalShape("$/storage/measure/value");
    }
    private static void logical211(Wire211.LogicalDocument value) {
        if (value.logicalExtent() < 0 || value.logicalExtent() != value.value().codePointCount(0, value.value().length()))
            throw new PhysicalShape("$/logicalValue/logicalExtent");
    }

    private static void requireCoherentFacts210(Wire210.Document wire) {
        wire.storage().nodes().forEach(n->measure210(n.extent()));
        wire.storage().bases().forEach(b->measure210(b.extent()));
        wire.storage().views().forEach(v->{ measure210(v.offset()); measure210(v.extent()); });
        for (var data : wire.dataDeclarations()) if (data.scalarText() != null && data.scalarText().logicalExtent() <= 0)
            throw new PhysicalShape("$/dataDeclarations/scalarText/logicalExtent");
        for (var statement : wire.statements()) {
            if (statement instanceof Wire210.EvaluateDocument e) for (var a : e.arms()) {
                if (!(a.selection() instanceof Wire210.LiteralDocument l) || l.kind()!=SpInput.LiteralKind.ALPHANUMERIC
                        || l.logicalValue()==null || !l.value().equals(l.logicalValue().value()))
                    throw new PhysicalShape("$/statements/EVALUATE/arms/selection");
                logical210(l.logicalValue());
            }
            if (statement instanceof Wire210.MoveDocument m && m.source() instanceof Wire210.LiteralDocument literal && literal.logicalValue() != null) {
                var value = literal.logicalValue();
                if (literal.kind() != SpInput.LiteralKind.ALPHANUMERIC || !literal.value().equals(value.value()))
                    throw new PhysicalShape("$/statements/source/logicalValue");
                logical210(value);
            }
            if (statement instanceof Wire210.MoveDocument m && m.textAdjustment() != null) logical210(m.textAdjustment().result());
            if (statement instanceof Wire210.CallDocument c && c.target() instanceof Wire210.LiteralTargetDocument l && l.logicalValue() != null) {
                logical210(l.logicalValue());
                if (!l.text().equals(l.logicalValue().value())) throw new PhysicalShape("$/statements/target/text");
            }
        }
    }
    private static void measure210(Wire210.MeasureDocument m) {
        if (m.value()!=null && !m.value().matches("0|[1-9][0-9]*")) throw new PhysicalShape("$/storage/measure/value");
    }
    private static void logical210(Wire210.LogicalDocument value) {
        if (value.logicalExtent() < 0 || value.logicalExtent() != value.value().codePointCount(0, value.value().length()))
            throw new PhysicalShape("$/logicalValue/logicalExtent");
    }

    private static void requireCoherentFacts27(Wire27.Document wire) {
        wire.storage().nodes().forEach(n->measure27(n.extent()));
        wire.storage().bases().forEach(b->measure27(b.extent()));
        wire.storage().views().forEach(v->{ measure27(v.offset()); measure27(v.extent()); });
        for (var data : wire.dataDeclarations()) if (data.scalarText() != null && data.scalarText().logicalExtent() <= 0)
            throw new PhysicalShape("$/dataDeclarations/scalarText/logicalExtent");
        for (var statement : wire.statements()) {
            if (statement instanceof Wire27.EvaluateDocument e) for (var a : e.arms()) {
                if (!(a.selection() instanceof Wire27.LiteralDocument l) || l.kind()!=SpInput.LiteralKind.ALPHANUMERIC
                        || l.logicalValue()==null || !l.value().equals(l.logicalValue().value()))
                    throw new PhysicalShape("$/statements/EVALUATE/arms/selection");
                logical27(l.logicalValue());
            }
            if (statement instanceof Wire27.MoveDocument m && m.source() instanceof Wire27.LiteralDocument literal && literal.logicalValue() != null) {
                var value = literal.logicalValue();
                if (literal.kind() != SpInput.LiteralKind.ALPHANUMERIC || !literal.value().equals(value.value()))
                    throw new PhysicalShape("$/statements/source/logicalValue");
                logical27(value);
            }
            if (statement instanceof Wire27.MoveDocument m && m.textAdjustment() != null) logical27(m.textAdjustment().result());
            if (statement instanceof Wire27.CallDocument c && c.target() instanceof Wire27.LiteralTargetDocument l && l.logicalValue() != null) {
                logical27(l.logicalValue());
                if (!l.text().equals(l.logicalValue().value())) throw new PhysicalShape("$/statements/target/text");
            }
        }
    }
    private static void measure27(Wire27.MeasureDocument m) {
        if (m.value()!=null && !m.value().matches("0|[1-9][0-9]*")) throw new PhysicalShape("$/storage/measure/value");
    }
    private static void logical27(Wire27.LogicalDocument value) {
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
        if (value instanceof Wire211.SliceDocument slice) {
            if (slice.offset() == null || !slice.offset().matches("0|[1-9][0-9]*"))
                throw new PhysicalShape(location + "/offset");
            if (slice.extent() == null || !slice.extent().matches("0|[1-9][0-9]*"))
                throw new PhysicalShape(location + "/extent");
        }
        if (value instanceof Wire210.SliceDocument slice) {
            if (slice.offset() == null || !slice.offset().matches("0|[1-9][0-9]*"))
                throw new PhysicalShape(location + "/offset");
            if (slice.extent() == null || !slice.extent().matches("0|[1-9][0-9]*"))
                throw new PhysicalShape(location + "/extent");
        }
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
