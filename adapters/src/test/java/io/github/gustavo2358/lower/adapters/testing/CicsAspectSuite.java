package io.github.gustavo2358.lower.adapters.testing;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.github.gustavo2358.air.model.*;
import io.github.gustavo2358.lower.adapters.cli.CobolLower;
import io.github.gustavo2358.lower.adapters.sp.SpJsonDecoder;
import io.github.gustavo2358.lower.application.CobolLowerer;
import java.util.*;

/** CICS target materialization and independent local option effects. */
public final class CicsAspectSuite {
    private CicsAspectSuite() { }
    private static void need(boolean yes,String why) { if(!yes)throw new AssertionError(why); }
    private static Terminator cics(byte[] raw) {
        var decoded=new SpJsonDecoder(CobolLower.INPUT_LIMITS).decode(raw);
        need(decoded instanceof SpJsonDecoder.Decoded,"CICS SP decode");
        var publication=new CobolLowerer().lower(((SpJsonDecoder.Decoded)decoded).input(),CobolLower.OPTIONS).publication().orElseThrow();
        return publication.units().stream().flatMap(u->u.sequences().stream()).map(Sequence::terminator)
            .filter(t->t instanceof Operations.Opaque o&&o.observedKind().equals("cics-program-target-unavailable")
                ||t instanceof Operations.Invoke i&&i.target() instanceof Interactions.LiteralTarget l&&l.namespace().equals("cics.program"))
            .findFirst().orElseThrow();
    }
    private static ObjectNode statement(ObjectNode tree) {
        for(var fact:tree.path("statements"))if(fact.path("variant").asText().equals("CICS_PROGRAM_CONTROL"))return (ObjectNode)fact;
        throw new AssertionError("CICS statement missing");
    }
    public static void main(String[] args) throws Exception {
        byte[] raw=Objects.requireNonNull(CicsAspectSuite.class.getResourceAsStream("/sp/w8/cics-options-known.json")).readAllBytes();
        var mapper=new ObjectMapper();
        var known=(Operations.Invoke)cics(raw);
        need(known.target() instanceof Interactions.LiteralTarget,"target known");
        need(known.effectBound().otherwise().reads() instanceof Scopes.WithinMemory r&&r.scope() instanceof Scopes.ObjectsMemory o&&o.objects().size()==1,"COMMAREA read scope local");
        need(known.effectBound().otherwise().writes() instanceof Scopes.WithinMemory w&&w.scope() instanceof Scopes.ObjectsMemory o&&o.objects().size()==1,"RESP write scope local");
        need(known.effectOperands().stream().filter(p->p.header().role()==Operand.Role.VALUE_READ).count()==1,"known read operand");
        need(known.effectOperands().stream().filter(p->p.header().role()==Operand.Role.VALUE_WRITE).count()==1,"known write operand");
        var targetGap=(ObjectNode)mapper.readTree(raw);var c=statement(targetGap);c.putNull("target");c.put("conditions","UNKNOWN");
        ((com.fasterxml.jackson.databind.node.ArrayNode)c.path("gapCodes")).add("CICS_TARGET_UNKNOWN");
        var opaque=(Operations.Opaque)cics(mapper.writeValueAsBytes(targetGap));
        need(opaque.envelope().memory().knownReads().size()==1&&opaque.envelope().memory().knownWrites().size()==1,"target gap retains same option effects");
        need(opaque.envelope().memory().otherReads() instanceof Scopes.NoMemory&&opaque.envelope().memory().otherWrites() instanceof Scopes.NoMemory,"target gap adds no broad memory");
        for(String option:List.of("COMMAREA","RESP")) {
            var tree=(ObjectNode)mapper.readTree(raw);var fact=statement(tree);
            for(var o:fact.path("options"))if(o.path("name").asText().equals(option))((ObjectNode)o.path("reference")).putNull("regionalAccess");
            var mutated=(Operations.Invoke)cics(mapper.writeValueAsBytes(tree));
            need(mutated.target() instanceof Interactions.LiteralTarget,"effect gap must retain target");
            var effects=mutated.effectBound().otherwise();
            need((option.equals("COMMAREA")?effects.reads():effects.writes()) instanceof Scopes.NoMemory,"removed effect proof must disappear: "+option);
            need((option.equals("COMMAREA")?effects.writes():effects.reads()) instanceof Scopes.WithinMemory,"unrelated effect proof survives: "+option);
        }
        System.out.println("CICS_ASPECT=PASS target/effect split, local scopes, 3 mutations");
    }
}
