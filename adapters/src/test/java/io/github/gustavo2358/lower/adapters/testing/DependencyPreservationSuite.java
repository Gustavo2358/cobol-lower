package io.github.gustavo2358.lower.adapters.testing;
import io.github.gustavo2358.air.model.*;
import io.github.gustavo2358.air.json.AirJson;
import io.github.gustavo2358.lower.adapters.sp.SpJsonDecoder;
import io.github.gustavo2358.lower.adapters.cli.CobolLower;
import io.github.gustavo2358.lower.application.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

public final class DependencyPreservationSuite {
    private DependencyPreservationSuite() { }
    public static void main(String[] args) throws Exception {
        byte[] bytes;try(var in=DependencyPreservationSuite.class.getResourceAsStream("/sp/dependency-preservation/partial.json")){bytes=in.readAllBytes();}
        var decoder=new SpJsonDecoder(CobolLower.INPUT_LIMITS);
        var input=((SpJsonDecoder.Decoded)decoder.decode(bytes)).input();
        var result=new CobolLowerer().lower(input,CobolLower.OPTIONS);
        if(result.status()!=LoweringResult.Status.SUCCESS)throw new AssertionError(result.toString());
        var p=result.publication().orElseThrow();var codec=new AirJson();if(!p.equals(codec.decode(codec.encode(p))))throw new AssertionError("roundtrip");
        var unit=p.units().getFirst();var invoke=unit.sequences().stream().map(Sequence::terminator).filter(Operations.Invoke.class::isInstance).map(Operations.Invoke.class::cast).findFirst().orElseThrow();
        var areaGap=p.uncertainties().stream().filter(u->u.code().equals("cobol-lower:CICS_PHYSICAL_NAME_AREA_UNPROVEN")).findFirst().orElseThrow();
        if(!invoke.header().uncertainties().contains(areaGap.id()))throw new AssertionError("CICS area uncertainty must be linked to Invoke");
        var read=(Expressions.Read)((Interactions.ComputedTarget)invoke.target()).name();var object=((Places.ObjectPlace)read.place()).object();
        var declaration=unit.objects().stream().filter(d->d.id().equals(object)).findFirst().orElseThrow();
        if(!(declaration.storage() instanceof Memory.CellBinding cell)||p.storage().stream().noneMatch(s->s instanceof Memory.Cell c&&c.header().id().equals(cell.storage())))
            throw new AssertionError("supported logical value needs a positive Cell");
        if(!unit.sequences().stream().flatMap(s->s.instructions().stream()).anyMatch(i->i instanceof Operations.Assign a&&a.destination() instanceof Places.ObjectPlace o&&o.object().equals(object)&&a.value() instanceof Expressions.Literal l&&l.value().equals(new Values.TextValue("PROGA   "))))throw new AssertionError("local definition must target same nominal identity");
        var mapper=new ObjectMapper();var old=(ObjectNode)mapper.readTree(bytes);old.put("contractVersion","2.31.0");
        if(decoder.decode(mapper.writeValueAsBytes(old)) instanceof SpJsonDecoder.Decoded)throw new AssertionError("old version accepts possible transfer");
        var malformed=(ObjectNode)mapper.readTree(bytes);((ObjectNode)malformed.path("statements").get(0).path("textAdjustment").path("result")).put("value","BADPGM  ");
        var changed=decoder.decode(mapper.writeValueAsBytes(malformed));
        if(changed instanceof SpJsonDecoder.Decoded d&&new CobolLowerer().lower(d.input(),CobolLower.OPTIONS).status()==LoweringResult.Status.SUCCESS)throw new AssertionError("invented literal accepted");
        byte[] aliasBytes;try(var in=DependencyPreservationSuite.class.getResourceAsStream("/sp/dependency-preservation/redefines.json")){aliasBytes=in.readAllBytes();}
        var aliasInput=((SpJsonDecoder.Decoded)decoder.decode(aliasBytes)).input();
        var aliasPublication=new CobolLowerer().lower(aliasInput,CobolLower.OPTIONS).publication().orElseThrow();
        var aliasObjects=aliasPublication.units().getFirst().objects().stream().filter(o->o.displayName().filter(n->n.equals("A")||n.equals("B")).isPresent()).toList();
        if(aliasObjects.size()!=2||!aliasObjects.stream().allMatch(o->o.storage() instanceof Memory.CellBinding))throw new AssertionError("exact logical views require Cells");
        var shared=((Memory.CellBinding)aliasObjects.get(0).storage()).storage();
        if(!shared.equals(((Memory.CellBinding)aliasObjects.get(1).storage()).storage())||aliasPublication.storage().stream().noneMatch(s->s instanceof Memory.Cell c&&c.header().id().equals(shared)&&c.typeRef().equals(Types.known(Types.Builtin.TEXT))))
            throw new AssertionError("complete equivalent views must share one TEXT Cell");
        if(aliasObjects.get(0).id().equals(aliasObjects.get(1).id()))throw new AssertionError("nominal identities must not be conflated");
        var silent=(ObjectNode)mapper.readTree(aliasBytes);silent.put("contractVersion","2.33.0");
        var silentStorage=(ObjectNode)silent.path("storage");silentStorage.put("version","1.8.0");silentStorage.remove("logicalExactViews");
        var withoutProof=((SpJsonDecoder.Decoded)decoder.decode(mapper.writeValueAsBytes(silent))).input();
        if(new CobolLowerer().lower(withoutProof,CobolLower.OPTIONS).status()!=LoweringResult.Status.BLOCKED_LOWERING)
            throw new AssertionError("silent producer cannot create disconnected Cells for a proved overlay");
        var forged=(ObjectNode)mapper.readTree(aliasBytes);
        ((ObjectNode)forged.path("storage").path("logicalExactViews").get(1)).put("length","4");
        var inconsistent=((SpJsonDecoder.Decoded)decoder.decode(mapper.writeValueAsBytes(forged))).input();
        if(new CobolLowerer().lower(inconsistent,CobolLower.OPTIONS).status()!=LoweringResult.Status.INVALID_INPUT)
            throw new AssertionError("inconsistent local exact view must be rejected");
        System.out.println("DEPENDENCY_PRESERVATION_LOWER=PASS distinct names, shared exact logical Cell, local Assign, version/forgery rejection, roundtrip");
    }
}
