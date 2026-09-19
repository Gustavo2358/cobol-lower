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
        var read=(Expressions.Read)((Interactions.ComputedTarget)invoke.target()).name();var object=((Places.ObjectPlace)read.place()).object();
        var declaration=unit.objects().stream().filter(d->d.id().equals(object)).findFirst().orElseThrow();
        if(!(declaration.storage() instanceof Memory.UnknownBinding))throw new AssertionError("no invented allocation");
        if(!unit.sequences().stream().flatMap(s->s.instructions().stream()).anyMatch(i->i instanceof Operations.Assign a&&a.destination() instanceof Places.ObjectPlace o&&o.object().equals(object)&&a.value() instanceof Expressions.Literal l&&l.value().equals(new Values.TextValue("PROGA   "))))throw new AssertionError("local definition must target same nominal identity");
        var mapper=new ObjectMapper();var old=(ObjectNode)mapper.readTree(bytes);old.put("contractVersion","2.31.0");
        if(decoder.decode(mapper.writeValueAsBytes(old)) instanceof SpJsonDecoder.Decoded)throw new AssertionError("old version accepts possible transfer");
        var malformed=(ObjectNode)mapper.readTree(bytes);((ObjectNode)malformed.path("statements").get(0).path("textAdjustment").path("result")).put("value","BADPGM  ");
        var changed=decoder.decode(mapper.writeValueAsBytes(malformed));
        if(changed instanceof SpJsonDecoder.Decoded d&&new CobolLowerer().lower(d.input(),CobolLower.OPTIONS).status()==LoweringResult.Status.SUCCESS)throw new AssertionError("invented literal accepted");
        System.out.println("DEPENDENCY_PRESERVATION_LOWER=PASS nominal identity, no allocation, local Assign, version/forgery rejection, roundtrip");
    }
}
