package io.github.gustavo2358.lower.adapters.testing;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.github.gustavo2358.air.model.Memory;
import io.github.gustavo2358.air.model.Types;
import io.github.gustavo2358.air.validation.AirValidator;
import io.github.gustavo2358.lower.adapters.cli.CobolLower;
import io.github.gustavo2358.lower.adapters.sp.SpJsonDecoder;
import io.github.gustavo2358.lower.application.CobolLowerer;
import io.github.gustavo2358.lower.application.LoweringResult;
import java.util.stream.Collectors;

/** Frontend-generated SP fixtures; complete logical views never supply physical bytes. */
public final class FileRecordGroundingSuite {
    private FileRecordGroundingSuite() { }
    private static byte[] fixture(String name) throws Exception {
        try(var input=FileRecordGroundingSuite.class.getResourceAsStream("/sp/file-record-grounding/"+name+".json")) {
            if(input==null)throw new AssertionError("missing generated SP fixture "+name);
            return input.readAllBytes();
        }
    }
    private static LoweringResult lower(byte[] bytes) {
        var result=new SpJsonDecoder(CobolLower.INPUT_LIMITS).decode(bytes);
        if(!(result instanceof SpJsonDecoder.Decoded decoded))throw new AssertionError(result);
        return new CobolLowerer().lower(decoded.input(),CobolLower.OPTIONS);
    }
    private static void accepted(LoweringResult result) {
        if(result.status()!=LoweringResult.Status.SUCCESS)throw new AssertionError(result);
        if(!AirValidator.validate(result.publication().orElseThrow()).isStructurallyValid())
            throw new AssertionError("generated grounded AIR must pass the final validator");
    }
    public static void main(String[] args) throws Exception {
        var mapper=new ObjectMapper();
        var pair=fixture("file-w2-composition");
        var pairTree=mapper.readTree(pair);
        if(!pairTree.path("contractVersion").asText().equals("2.35.0")
                ||!pairTree.path("storage").path("version").asText().equals("1.11.0"))
            throw new AssertionError("extended local exact-view contract absent");
        var pairResult=lower(pair);accepted(pairResult);
        var objects=pairResult.publication().orElseThrow().units().getFirst().objects().stream()
            .filter(o->o.displayName().filter(n->n.equals("R")||n.equals("K")||n.equals("BUF")).isPresent())
            .collect(Collectors.toMap(o->o.displayName().orElseThrow(),o->o));
        if(objects.size()!=3||objects.get("R").id().equals(objects.get("K").id()))throw new AssertionError("distinct DATA identities");
        if(!(objects.get("R").storage() instanceof Memory.CellBinding r)
                ||!(objects.get("K").storage() instanceof Memory.CellBinding k)
                ||!(objects.get("BUF").storage() instanceof Memory.CellBinding buf)
                ||!r.storage().equals(k.storage())||r.storage().equals(buf.storage()))
            throw new AssertionError("exact record/child Cell and independent same-sized BUF");
        if(pairResult.publication().orElseThrow().storage().stream().noneMatch(s->s instanceof Memory.Cell cell
                &&cell.header().id().equals(r.storage())&&cell.typeRef().equals(Types.known(Types.Builtin.TEXT))))
            throw new AssertionError("shared TEXT Cell absent");
        var oldSource=fixture("source-w3-composition");var oldResult=lower(oldSource);accepted(oldResult);
        var record=oldResult.publication().orElseThrow().units().getFirst().objects().stream()
            .filter(o->o.displayName().filter("FILE-RECORD"::equals).isPresent()).findFirst().orElseThrow();
        if(!(record.storage() instanceof Memory.CellBinding))throw new AssertionError("standalone supported TEXT record remains ungrounded");
        var forged=(ObjectNode)mapper.readTree(pair);
        ((ObjectNode)forged.path("storage").path("logicalExactViews").get(2)).put("length","4");
        if(lower(mapper.writeValueAsBytes(forged)).status()!=LoweringResult.Status.INVALID_INPUT)
            throw new AssertionError("inconsistent complete view accepted");
        var silent=(ObjectNode)mapper.readTree(pair);
        ((ObjectNode)silent.path("storage")).remove("logicalExactViews");
        if(new SpJsonDecoder(CobolLower.INPUT_LIMITS).decode(mapper.writeValueAsBytes(silent)) instanceof SpJsonDecoder.Decoded)
            throw new AssertionError("new contract accepted silence instead of published exact view");
        System.out.println("FILE_RECORD_GROUNDING=PASS shared group/child Cell, independent storage, standalone record, forged/silent proof rejection");
    }
}
