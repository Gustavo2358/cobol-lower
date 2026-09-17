package io.github.gustavo2358.lower.adapters.testing;
import io.github.gustavo2358.lower.adapters.cli.CobolLower;
import io.github.gustavo2358.lower.adapters.sp.SpJsonDecoder;
import io.github.gustavo2358.lower.application.*;
import io.github.gustavo2358.air.model.*;
import java.util.*;
import static io.github.gustavo2358.lower.testing.CallOracle.check;
/** Independent static slice expectations over real SP, with no downstream source interpretation. */
public final class FileStaticSliceSuite {
    public static void main(String[] args)throws Exception{
        var bytes=Objects.requireNonNull(FileStaticSliceSuite.class.getResourceAsStream("/sp/file-dependencies/static.json")).readAllBytes();
        var decoded=new SpJsonDecoder(CobolLower.INPUT_LIMITS).decode(bytes);
        check(decoded instanceof SpJsonDecoder.Decoded,"FD-W1 SP2.22 must decode: "+decoded);
        var input=((SpJsonDecoder.Decoded)decoded).input();
        var result=new CobolLowerer().lower(input,CobolLower.OPTIONS);
        check(result.publication().isPresent(),"FD-W1 publication: "+result);
        var p=result.publication().orElseThrow();
        check(p.resources().size()==1,"one declaration SELECT+FD");
        var r=p.resources().getFirst();var target=(Interactions.LiteralTarget)r.description();
        check(target.namespace().equals("cobol.external-file-name")&&target.name().equals("CLIENTDD"),"external source name exact");
        var invokes=p.units().getFirst().sequences().stream().map(Sequence::terminator).filter(Operations.Invoke.class::isInstance).map(Operations.Invoke.class::cast).toList();
        check(invokes.size()==3,"OPEN READ CLOSE three uses");
        check(invokes.stream().map(Operations.Invoke::action).collect(java.util.stream.Collectors.toSet()).equals(Set.of("open","read","close")),"action set");
        for(var i:invokes){check(i.target() instanceof Interactions.LiteralTarget t&&t.name().equals("CLIENTDD")&&t.category().equals("file"),"FILE target");check(i.effectBound().otherwise().mustOverwrite().isEmpty(),"no speculative MUST");}
        var declaration=r.declaration().orElseThrow();check(declaration.owner().equals(p.units().getFirst().id())&&declaration.name().equals("F")&&declaration.nameSource().equals("cobol.assignment-name"),"owner logical name and source kind");
        check(declaration.objects().size()==1&&declaration.uses().size()==3,"record and three use links preserved");
        check(declaration.uses().stream().anyMatch(u->u.role().equals("open-input")),"OPEN mode survives nominal role");
        var mapper=new com.fasterxml.jackson.databind.ObjectMapper();var base=(com.fasterxml.jackson.databind.node.ObjectNode)mapper.readTree(bytes);
        for(String mutation:List.of("missing-operations","wrong-version","foreign-statement","missing-file","duplicate-use","bad-mode","wrong-profile","unavailable-uses","known-with-gap","blank-candidate")){
            var doc=base.deepCopy();var inv=(com.fasterxml.jackson.databind.node.ObjectNode)doc.path("fileInventory");
            var uses=(com.fasterxml.jackson.databind.node.ArrayNode)inv.path("operations").path("uses");var use=(com.fasterxml.jackson.databind.node.ObjectNode)uses.get(0);
            switch(mutation){
                case "unavailable-uses"->((com.fasterxml.jackson.databind.node.ObjectNode)inv.path("operations")).put("availability","UNAVAILABLE");
                case "known-with-gap"->((com.fasterxml.jackson.databind.node.ObjectNode)inv.path("operations")).put("availability","KNOWN");
                case "blank-candidate"->((com.fasterxml.jackson.databind.node.ObjectNode)use.path("candidates").get(0)).put("id","");
                case "missing-operations"->inv.remove("operations");case "wrong-version"->inv.put("version","1.0.0");
                case "foreign-statement"->use.put("statement","statement:999");
                case "missing-file"->((com.fasterxml.jackson.databind.node.ObjectNode)use.path("candidates").get(0)).put("id","file:999");
                case "duplicate-use"->uses.add(use.deepCopy());case "bad-mode"->use.put("mode","UNSPECIFIED");case "wrong-profile"->use.put("profile","GUESSED");
            }
            var d=new SpJsonDecoder(CobolLower.INPUT_LIMITS).decode(mapper.writeValueAsBytes(doc));
            check(d instanceof SpJsonDecoder.Rejected||new CobolLowerer().lower(((SpJsonDecoder.Decoded)d).input(),CobolLower.OPTIONS).status()==LoweringResult.Status.INVALID_INPUT,"mutation rejected: "+mutation);
        }
        var changed=base.deepCopy();((com.fasterxml.jackson.databind.node.ObjectNode)changed.path("fileInventory").path("declarations").get(0).path("assignment")).put("externalFileName","OTHERDD");
        var changedInput=((SpJsonDecoder.Decoded)new SpJsonDecoder(CobolLower.INPUT_LIMITS).decode(mapper.writeValueAsBytes(changed))).input();
        var changedPublication=new CobolLowerer().lower(changedInput,CobolLower.OPTIONS).publication().orElseThrow();
        check(!changedPublication.id().equals(p.id()),"FILE semantics participate in revision identity");
        var codec=new io.github.gustavo2358.air.json.AirJson();check(codec.decode(codec.encode(p)).equals(p),"roundtrip static slice");
        check(new CobolLowerer().lower(input,CobolLower.OPTIONS).publication().orElseThrow().equals(p),"deterministic lower");
        System.out.println("FD_W1_STATIC=PASS declarations, three literal invokes, no speculative MUST, codec, deterministic");
    }
}
