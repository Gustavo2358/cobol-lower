package io.github.gustavo2358.lower.adapters.testing;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.github.gustavo2358.lower.adapters.cli.CobolLower;
import io.github.gustavo2358.lower.adapters.sp.SpJsonDecoder;
import io.github.gustavo2358.lower.application.CobolLowerer;
import io.github.gustavo2358.lower.application.LoweringResult;
import io.github.gustavo2358.lower.domain.SpInput;
import io.github.gustavo2358.lower.testing.IfInputs;
import java.util.*;
import static io.github.gustavo2358.lower.testing.CallOracle.check;

/** W5 wire preservation only; executable composition belongs to W6. */
public final class PartialStructuralFactsSuite {
    private static final ObjectMapper JSON=new ObjectMapper();
    private static final SpJsonDecoder DECODER=new SpJsonDecoder(CobolLower.INPUT_LIMITS);
    private static SpInput decode(byte[] bytes) {
        var result=DECODER.decode(bytes);
        check(result instanceof SpJsonDecoder.Decoded,"SP2.36 structural facts decode: "+result);
        return ((SpJsonDecoder.Decoded)result).input();
    }
    public static void main(String[] args) throws Exception {
        for(var name:List.of("call-basic","nested","neutral")) {
            byte[] bytes;
            try(var in=PartialStructuralFactsSuite.class.getResourceAsStream("/sp/partial-structural-facts/"+name+".json")) {
                bytes=Objects.requireNonNull(in,name).readAllBytes();
            }
            var input=decode(bytes);var tree=(ObjectNode)JSON.readTree(bytes);
            var roundTrip=decode(JSON.writeValueAsBytes(tree));
            check(input.equals(roundTrip),"wire tree round-trip preserves all typed facts "+name);
            var facts=input.statements().stream().filter(SpInput.ProcedurePerformFact.class::isInstance)
                .map(SpInput.ProcedurePerformFact.class::cast).toList();
            check(facts.size()==(name.equals("nested")?2:1),"one fact per activation "+name);
            for(var p:facts) {
                check(p.publicationKind()==SpInput.PerformPublicationKind.STRUCTURAL_FACTS,"explicit source facts capability");
                check(p.start().isPresent()&&p.targetEntry().isPresent()&&!p.procedures().isEmpty(),"target, independent entry and membership preserved");
                check(p.procedures().stream().allMatch(r->r.statements().contains(r.entry())&&!r.completions().isEmpty()),"entry and conditional frontiers preserved");
            }
            var result=new CobolLowerer().lower(input,CobolLower.OPTIONS);
            check(result.status()!=LoweringResult.Status.INVALID_INPUT,"valid structural facts are admitted: "+name+" "+result);
            check(result.status()!=LoweringResult.Status.OUTPUT_INVALID,"structural-only input does not fabricate invalid execution "+name);
            var noDiagnostics=input.statements().stream().map(s->s instanceof SpInput.ProcedurePerformFact p
                ?IfInputs.with(p,"gapCodes",List.of()):s).toList();
            var withoutGaps=new CobolLowerer().lower(IfInputs.with(input,"statements",noDiagnostics),CobolLower.OPTIONS);
            check(withoutGaps.status()==result.status(),"diagnostics do not promote structural facts to specialization");
            for(var version:List.of("2.28.0","2.33.0","2.35.0")) {
                var old=tree.deepCopy();old.put("contractVersion",version);
                check(DECODER.decode(JSON.writeValueAsBytes(old)) instanceof SpJsonDecoder.Rejected,"old version rejects new meaning "+version);
            }
            for(var kind:List.of("SPECIALIZATION","UNKNOWN","")) {
                var bad=tree.deepCopy();for(var s:bad.path("statements"))if(s.has("publicationKind"))((ObjectNode)s).put("publicationKind",kind);
                check(DECODER.decode(JSON.writeValueAsBytes(bad)) instanceof SpJsonDecoder.Rejected,"unknown or ambiguous new wire kind refused");
            }
            if(name.equals("call-basic")) {
                var partialRange=tree.deepCopy();
                for(var s:partialRange.path("statements"))if(s.has("publicationKind"))((ObjectNode)s).putArray("procedures");
                var independent=decode(JSON.writeValueAsBytes(partialRange));
                var entryOnly=(SpInput.ProcedurePerformFact)independent.statements().stream().filter(SpInput.ProcedurePerformFact.class::isInstance).findFirst().orElseThrow();
                check(entryOnly.targetEntry().equals(facts.getFirst().targetEntry()),"entry survives unavailable whole range");
                check(new CobolLowerer().lower(independent,CobolLower.OPTIONS).status()!=LoweringResult.Status.INVALID_INPUT,"independent entry is valid without whole range");
            }
            var p=facts.getFirst();var paragraph=p.procedures().getFirst();
            var broken=IfInputs.with(paragraph,"completions",List.of(p.header().id()));
            var paragraphs=new ArrayList<>(p.procedures());paragraphs.set(0,broken);
            var bad=IfInputs.with(p,"procedures",paragraphs);
            var badInput=IfInputs.with(input,"statements",input.statements().stream().map(s->s==p?bad:s).toList());
            check(new CobolLowerer().lower(badInput,CobolLower.OPTIONS).status()==LoweringResult.Status.INVALID_INPUT,"partial does not relax referential consistency");
        }
        System.out.println("LOWER_PARTIAL_STRUCTURAL_FACTS=PASS");
    }
}
