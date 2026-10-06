package io.github.gustavo2358.lower.adapters.sp;
import java.io.*;
import java.nio.charset.*;
/** Strict UTF-8, bounded decoding and historical SP BOM admission; caller retains ownership. */
final class Utf8JsonInput {
    private Utf8JsonInput() { }
    static Reader reader(InputStream input)throws IOException {
        var owned=new FilterInputStream(input){@Override public void close(){ }};
        var decoder=StandardCharsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.REPORT).onUnmappableCharacter(CodingErrorAction.REPORT);
        var reader=new PushbackReader(new InputStreamReader(owned,decoder),1);
        int first=reader.read();if(first!=-1&&first!=0xfeff)reader.unread(first);
        return reader;
    }
}
