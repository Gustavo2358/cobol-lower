package io.github.gustavo2358.lower.adapters.sp;
import java.io.*;
import java.util.*;
/** Physical transport oracle: bounded reads, exact facts, strict UTF-8 and caller ownership. */
public final class StreamingInputSuite {
    private static final class Chunked extends ByteArrayInputStream {
        int largest;boolean closed;
        Chunked(byte[] bytes){super(bytes);}
        @Override public synchronized int read(byte[] buffer,int offset,int length){largest=Math.max(largest,length);return super.read(buffer,offset,Math.min(length,3));}
        @Override public void close(){closed=true;}
    }
    private static void require(boolean value,String message){if(!value)throw new AssertionError(message);}
    public static void main(String[] args)throws Exception {
        var mapper=new com.fasterxml.jackson.databind.ObjectMapper();
        String paragraph="{\"id\":\"Aa\",\"entry\":\"e\",\"statements\":[\"e\"],\"completions\":[\"e\"],\"provenance\":{\"exact\":true}}";
        var rows=new ArrayList<String>();for(int i=0;i<40;i++)rows.add("{\"variant\":\"PERFORM_PROCEDURE\",\"procedures\":["+paragraph+"]}");
        String physical="{\"statements\":["+String.join(",",rows)+"],\"other\":42}";
        var tree=SharedSpTree.read(mapper,new StringReader(physical));
        require(tree.equals(mapper.readTree(physical)),"physical tree changed");
        var first=tree.path("statements").get(0).path("procedures").get(0);
        for(var row:tree.path("statements"))require(row.path("procedures").get(0)==first,"equal paragraph payload retained repeatedly");
        String different=physical.replaceFirst("Aa","BB");
        var second=SharedSpTree.read(mapper,new StringReader(different));
        require(second.equals(mapper.readTree(different)),"distinct payload lost");
        require(second.path("statements").get(0).path("procedures").get(0)!=second.path("statements").get(1).path("procedures").get(0),"hash collision merged different identity");
        var dtoMapper=new com.fasterxml.jackson.databind.ObjectMapper().registerModule(new ParagraphDtoSharing())
            .enable(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_MISSING_CREATOR_PROPERTIES)
            .enable(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        var location=new Wire.LocationDocument("synthetic.cbl",1,0,1,4);
        var origin=new Wire.ProvenanceDocument(location,location,List.of(),true);
        var descriptor=new Wire211.PerformParagraphDocument("opaque", "entry",List.of("entry","end"),List.of("end"),origin);
        var dtoBytes=dtoMapper.writeValueAsBytes(List.of(descriptor,descriptor));
        var descriptors=dtoMapper.readValue(dtoBytes,Wire211.PerformParagraphDocument[].class);
        require(descriptors.length==2&&descriptor.equals(descriptors[0])&&descriptor.equals(descriptors[1]),"typed descriptor facts changed");
        require(descriptors[0]==descriptors[1],"typed descriptor retained repeatedly");
        var missing=dtoMapper.valueToTree(descriptor);((com.fasterxml.jackson.databind.node.ObjectNode)missing).remove("entry");
        boolean missingRejected=false;
        try{dtoMapper.readValue(dtoMapper.writeValueAsBytes(List.of(descriptor,missing)),Wire211.PerformParagraphDocument[].class);}
        catch(com.fasterxml.jackson.databind.JsonMappingException expectedFailure){missingRejected=true;}
        require(missingRejected,"memo bypassed missing field rejection");
        byte[] raw;
        try(var resource=StreamingInputSuite.class.getResourceAsStream("/sp/cobol-semantic-product.json")){raw=resource.readAllBytes();}
        var decoder=new SpJsonDecoder(new SpJsonDecoder.Limits(64));
        var expected=decoder.decode(raw);var input=new Chunked(raw);var actual=decoder.decodeStream(input);
        require(expected.equals(actual),"stream facts and diagnostics differ");
        require(input.largest<=8192&&!input.closed,"unbounded read or closed caller stream");
        var compilation=new CompilationJsonDecoder(new SpJsonDecoder.Limits(64));input=new Chunked(raw);
        require(compilation.decode(raw).equals(compilation.decodeStream(input)),"single envelope facts differ");
        require(!input.closed,"envelope closed caller stream");
        byte[] bom=new byte[raw.length+3];bom[0]=(byte)0xef;bom[1]=(byte)0xbb;bom[2]=(byte)0xbf;System.arraycopy(raw,0,bom,3,raw.length);
        require(expected.equals(decoder.decodeStream(new Chunked(bom))),"historical UTF-8 BOM admission changed");
        for(byte[] tail:List.of(new byte[]{(byte)0xc0,(byte)0xaf},new byte[]{(byte)0xed,(byte)0xa0,(byte)0x80},new byte[]{(byte)0xf4,(byte)0x90,(byte)0x80,(byte)0x80},new byte[]{(byte)0xe2,(byte)0x82},new byte[]{'{','}'})) {
            byte[] invalid=Arrays.copyOf(raw,raw.length+tail.length);System.arraycopy(tail,0,invalid,raw.length,tail.length);
            require(decoder.decodeStream(new Chunked(invalid)) instanceof SpJsonDecoder.Rejected r&&r.diagnostic().code()==SpJsonDecoder.Code.INPUT_ERROR,"invalid trailing bytes admitted");
        }
    }
}
