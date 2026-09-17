package io.github.gustavo2358.lower.adapters.sp;
import java.util.*;
import io.github.gustavo2358.lower.domain.*;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.core.*;
/** Closed envelope decoder; nested unit products still cross the original decoder. */
public final class CompilationJsonDecoder {
    public sealed interface Result permits Single,Compilation,Rejected{}
    public record Single(SpInput input)implements Result{}
    public record Compilation(SpCompilation input)implements Result{}
    public record Rejected(SpJsonDecoder.Diagnostic diagnostic)implements Result{}
    private final ObjectMapper mapper;private final SpJsonDecoder unitDecoder;
    public CompilationJsonDecoder(SpJsonDecoder.Limits limits){
        mapper=JsonMapper.builder(JsonFactory.builder().enable(StreamReadFeature.STRICT_DUPLICATE_DETECTION).streamReadConstraints(StreamReadConstraints.builder().maxNestingDepth(limits.maxDepth()).maxStringLength(Integer.MAX_VALUE).maxNumberLength(Integer.MAX_VALUE).build()).build()).enable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS).build();unitDecoder=new SpJsonDecoder(limits);
    }
    public Result decode(byte[] bytes){
        try {
            if(bytes==null)throw new IllegalArgumentException("null bytes");
            java.nio.charset.StandardCharsets.UTF_8.newDecoder().onMalformedInput(java.nio.charset.CodingErrorAction.REPORT).onUnmappableCharacter(java.nio.charset.CodingErrorAction.REPORT).decode(java.nio.ByteBuffer.wrap(bytes));
            var root=mapper.readTree(bytes);if(root==null||!root.isObject())throw new IllegalArgumentException("object required");
            if(!root.path("schema").asText().equals("cobol-semantic-compilation")){
                var decoded=unitDecoder.decode(bytes);return decoded instanceof SpJsonDecoder.Decoded d?new Single(d.input()):new Rejected(((SpJsonDecoder.Rejected)decoded).diagnostic());
            }
            keys(root,"schema","contractVersion","inventoryStatus","unitInventory","units");
            if(!text(root.get("contractVersion")).equals("1.0.0"))return reject(SpJsonDecoder.Code.UNSUPPORTED_CONTRACT,"compilation version");
            var inventory=new ArrayList<SpInput.UnitKey>();for(var u:array(root.get("unitInventory")))inventory.add(unit(u));
            var units=new ArrayList<SpCompilation.UnitProduct>();
            for(var u:array(root.get("units"))){
                keys(u,"product","parent","ownedData","globalData","dataCaptures","fileCaptures");
                var decoded=unitDecoder.decode(mapper.writeValueAsBytes(u.get("product")));
                if(decoded instanceof SpJsonDecoder.Rejected r)return new Rejected(r.diagnostic());var input=((SpJsonDecoder.Decoded)decoded).input();
                var owned=dataIds(input.unit(),u.get("ownedData"));var globals=dataIds(input.unit(),u.get("globalData"));
                var captures=new ArrayList<SpCompilation.DataCapture>();for(var c:array(u.get("dataCaptures"))){
                    keys(c,"localDataId","sourceUnit","sourceDataId");captures.add(new SpCompilation.DataCapture(new SpInput.DataId(input.unit(),text(c.get("localDataId"))),new SpInput.DataId(unit(c.get("sourceUnit")),text(c.get("sourceDataId")))));
                }
                var files=new ArrayList<FileFacts.Candidate>();for(var f:array(u.get("fileCaptures"))){keys(f,"owner","id");files.add(new FileFacts.Candidate(text(f.get("id")),unit(f.get("owner"))));}
                units.add(new SpCompilation.UnitProduct(input,u.get("parent").isNull()?Optional.empty():Optional.of(unit(u.get("parent"))),owned,globals,captures,files));
            }
            return new Compilation(new SpCompilation(SpInput.InventoryStatus.valueOf(text(root.get("inventoryStatus"))),inventory,units));
        }catch(com.fasterxml.jackson.core.exc.StreamConstraintsException e){return reject(SpJsonDecoder.Code.IMPLEMENTATION_LIMIT,"compilation depth");}
        catch(java.io.IOException|IllegalArgumentException e){return reject(SpJsonDecoder.Code.INPUT_ERROR,"compilation shape");}
    }
    private static List<SpInput.DataId> dataIds(SpInput.UnitKey unit,JsonNode array){var result=new ArrayList<SpInput.DataId>();for(var d:array(array))result.add(new SpInput.DataId(unit,text(d)));return result;}
    private static SpInput.UnitKey unit(JsonNode n){keys(n,"compilationUnitId","structuralPath","canonicalProgramName");var path=new ArrayList<Integer>();for(var i:array(n.get("structuralPath"))){if(!i.isIntegralNumber()||!i.canConvertToInt())throw new IllegalArgumentException("integer path required");path.add(i.intValue());}return new SpInput.UnitKey(text(n.get("compilationUnitId")),path,text(n.get("canonicalProgramName")));}
    private static String text(JsonNode n){if(n==null||!n.isTextual())throw new IllegalArgumentException("text required");return n.textValue();}
    private static JsonNode array(JsonNode n){if(n==null||!n.isArray())throw new IllegalArgumentException("array required");return n;}
    private static void keys(JsonNode n,String... expected){if(n==null||!n.isObject())throw new IllegalArgumentException("object required");var actual=new HashSet<String>();n.fieldNames().forEachRemaining(actual::add);if(!actual.equals(Set.of(expected)))throw new IllegalArgumentException("closed fields required");}
    private static Rejected reject(SpJsonDecoder.Code code,String location){return new Rejected(new SpJsonDecoder.Diagnostic(code,"physical",location));}
}
