package io.github.gustavo2358.lower.adapters.testing;
import io.github.gustavo2358.air.model.*;
import io.github.gustavo2358.lower.adapters.cli.CobolLower;
import io.github.gustavo2358.lower.adapters.sp.SpJsonDecoder;
import io.github.gustavo2358.lower.application.*;
import io.github.gustavo2358.lower.domain.*;
import io.github.gustavo2358.lower.testing.IfInputs;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.*;
import static io.github.gustavo2358.lower.testing.CallOracle.check;

/** Producer/reader contract, including attempts to invent a candidate or downgrade the wire. */
public final class AmbiguousTargetSuite {
 public static void main(String[] args)throws Exception{run();}
 public static void run()throws Exception {
  byte[] bytes;try(var in=AmbiguousTargetSuite.class.getResourceAsStream("/sp/recall-first/ambiguous.json")){bytes=Objects.requireNonNull(in).readAllBytes();}
  var decoder=new SpJsonDecoder(CobolLower.INPUT_LIMITS);var d=decoder.decode(bytes);check(d instanceof SpJsonDecoder.Decoded,"SP2.18 typed alternatives");
  var input=((SpJsonDecoder.Decoded)d).input();var call=(SpInput.CallFact)input.statements().getFirst();var target=(SpInput.DataCallTarget)call.target();var ref=target.reference();
  check(ref.binding().selected().isEmpty()&&ref.regionalAlternatives().size()==2,"reader neither selects nor discards alternatives");
  var output=RegionalTranslationSuite.lower(input).publication().orElseThrow();
  check(output.capabilities().required().contains(Capabilities.TARGET_POSSIBILITIES),"explicit domain capability");
  var invoke=(Operations.Invoke)output.units().getFirst().sequences().stream().map(Sequence::terminator).filter(Operations.Invoke.class::isInstance).findFirst().orElseThrow();
  var choice=(Places.Choice)((Expressions.Read)((Interactions.ComputedTarget)invoke.target()).name()).place();
  check(choice.candidates().size()==2&&choice.remainder() instanceof Scopes.WithinMemory&&choice.typeRef() instanceof Types.UnknownType,"known regions plus unknown remainder/domain");
  for(var variant:List.of("duplicate","foreign","selected")) {
   var choices=new ArrayList<>(ref.regionalAlternatives());var binding=ref.binding();
   if(variant.equals("duplicate"))choices.add(choices.getFirst());
   if(variant.equals("foreign"))choices.set(0,new StorageFacts.Access(new StorageFacts.NodeId(input.unit(),"storage-node:9999")));
   if(variant.equals("selected"))binding=IfInputs.with(binding,"selected",Optional.of(binding.candidates().getFirst()));
   var updated=IfInputs.with(IfInputs.with(ref,"regionalAlternatives",choices),"binding",binding);
   var statements=new ArrayList<>(input.statements());statements.set(0,IfInputs.with(call,"target",IfInputs.with(target,"reference",updated)));
   check(new EntryGobackAdmission().admit(IfInputs.with(input,"statements",statements),CobolLower.OPTIONS.admission()).status()==Admission.Status.INVALID_INPUT,"hostile alternative "+variant);
  }
  var mapper=new ObjectMapper();var old=(ObjectNode)mapper.readTree(bytes);old.put("contractVersion","2.17.0");
  check(decoder.decode(mapper.writeValueAsBytes(old)) instanceof SpJsonDecoder.Rejected,"older contract cannot silently accept new reference facts");
  var missing=(ObjectNode)mapper.readTree(bytes);((ObjectNode)missing.path("statements").get(0).path("target").path("reference")).remove("regionalAlternatives");
  check(decoder.decode(mapper.writeValueAsBytes(missing)) instanceof SpJsonDecoder.Rejected,"missing alternatives cannot silently become empty");
  System.out.println("AMBIGUOUS_TARGET_CONTRACT=PASS; NEGATIVES=5");
 }
}
