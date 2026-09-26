package io.github.gustavo2358.lower.adapters.testing;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.github.gustavo2358.air.model.*;
import io.github.gustavo2358.lower.adapters.cli.CobolLower;
import io.github.gustavo2358.lower.adapters.sp.SpJsonDecoder;
import io.github.gustavo2358.lower.application.CobolLowerer;
import java.util.*;

/** A proved whole TEXT SYSID survives an unrelated lack of byte layout. */
public final class CicsFileLogicalOptionSuite {
    private CicsFileLogicalOptionSuite() { }
    private static void need(boolean ok,String why){if(!ok)throw new AssertionError(why);}
    private static Expression sysid(byte[] raw) {
        var decoded=new SpJsonDecoder(CobolLower.INPUT_LIMITS).decode(raw);
        need(decoded instanceof SpJsonDecoder.Decoded,"SP decode");
        var pub=new CobolLowerer().lower(((SpJsonDecoder.Decoded)decoded).input(),CobolLower.OPTIONS).publication().orElseThrow();
        var invoke=pub.units().stream().flatMap(u->u.sequences().stream()).map(Sequence::terminator)
            .filter(t->t instanceof Operations.Invoke i&&i.target() instanceof Interactions.LiteralTarget l&&l.namespace().equals("cics.file"))
            .map(Operations.Invoke.class::cast).findFirst().orElseThrow();
        return ((Interactions.ValueArgument)invoke.arguments().get(1)).value();
    }
    public static void main(String[] args) throws Exception {
        byte[] raw=Objects.requireNonNull(CicsFileLogicalOptionSuite.class.getResourceAsStream("/sp/w8/r01-file-sysid.json")).readAllBytes();
        need(sysid(raw) instanceof Expressions.Read r&&r.place() instanceof Places.ObjectPlace,"logical SYSID uses exact whole object");
        var mapper=new ObjectMapper();var tree=(ObjectNode)mapper.readTree(raw);
        for(var fact:tree.path("statements"))if(fact.path("variant").asText().equals("CICS_FILE_CONTROL"))
            for(var option:fact.path("options"))if(option.path("name").asText().equals("SYSID"))
                ((ObjectNode)option.path("reference")).putNull("logicalWholeItem");
        need(sysid(mapper.writeValueAsBytes(tree)) instanceof Expressions.Unknown,"removed logical proof restores localized gap");
        System.out.println("CICS_FILE_LOGICAL_SYSID=PASS physical absence and logical proof mutation");
    }
}
