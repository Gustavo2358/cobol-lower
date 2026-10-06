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
