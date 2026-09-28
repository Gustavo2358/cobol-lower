package io.github.gustavo2358.lower.adapters.testing;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.node.*;
import io.github.gustavo2358.lower.adapters.sp.SpJsonDecoder;
import io.github.gustavo2358.lower.adapters.cli.CobolLower;
import io.github.gustavo2358.lower.application.CobolLowerer;
import io.github.gustavo2358.lower.domain.SpInput;
import io.github.gustavo2358.lower.domain.SpInput.*;
import io.github.gustavo2358.air.model.*;
import java.util.*;
/** Real producer database facts: unknown outputs never license MUST or guessed dispatch. */
public final class DatabaseControlSuite {
    static final ObjectMapper J=new ObjectMapper();static int cases,mutations;
    static void need(boolean v,String why){if(!v)throw new AssertionError(why);}
    static SpJsonDecoder.Result decode(ObjectNode j)throws Exception{return new SpJsonDecoder(CobolLower.INPUT_LIMITS).decode(J.writeValueAsBytes(j));}
    static void reject(ObjectNode j)throws Exception {
        var d=decode(j);need(d instanceof SpJsonDecoder.Rejected||new CobolLowerer().lower(((SpJsonDecoder.Decoded)d).input(),CobolLower.POSITIVE_OPTIONS).publication().isEmpty(),"malformed effects rejected");mutations++;
    }
    public static void main(String[] args)throws Exception {
        for(var language:List.of("sql","dli"))for(var kind:language.equals("sql")?List.of("select","predicate","update","insert","delete","open","fetch","close"):List.of("chkp","repl","isrt","dlet"))for(var mode:List.of("known","missing")) {
            String name="database-"+language+"-"+kind+"-"+mode;ObjectNode j;
            try(var in=DatabaseControlSuite.class.getResourceAsStream("/sp/database-control/"+name+".json")){j=(ObjectNode)J.readTree(Objects.requireNonNull(in,name));}
            need(j.path("contractVersion").asText().equals("2.55.0"),"new producer contract");
            var d=decode(j);need(d instanceof SpJsonDecoder.Decoded,"database SP admitted "+name);var input=((SpJsonDecoder.Decoded)d).input();
            var result=new CobolLowerer().lower(input,CobolLower.POSITIVE_OPTIONS);var pub=result.publication().orElseThrow();need(result.validation().orElseThrow().isStructurallyValid(),"AIR validity");
            var reached=TerminalSendSuite.reached(pub);var seqs=pub.units().stream().flatMap(u->u.sequences().stream()).toList();
            for(var target:List.of("AFTERP","NODATA","OTHER"))need(seqs.stream().anyMatch(s->reached.contains(s.label())&&s.terminator() instanceof Operations.Invoke v&&v.target() instanceof Interactions.LiteralTarget t&&t.name().equals(target)),"possible status branch/caller "+name+" "+target);
            var fact=input.statements().stream().filter(OtherStatement.class::isInstance).map(OtherStatement.class::cast).filter(o->o.effects().filter(e->e.proof()==EffectProof.SQL_HOST_OPERANDS||e.proof()==EffectProof.DLI_EXTERNAL_OPERANDS).isPresent()).findFirst().orElseThrow();
            var e=fact.effects().orElseThrow();need(e.mustOverwrite().isEmpty()&&e.unknownWriteBound()==EffectBound.ALL,"unknown external write retains prior values");
            var copy=j.deepCopy();copy.put("contractVersion","2.54.0");reject(copy);
            for(var field:List.of("unknownReadBound","unknownWriteBound","unknownExposureBound")) {
                copy=j.deepCopy();for(var ef:copy.path("statementEffects"))if(ef.path("proof").asText().endsWith("HOST_OPERANDS")||ef.path("proof").asText().equals("DLI_EXTERNAL_OPERANDS"))((ObjectNode)ef).put(field,"NONE");reject(copy);
            }
            var bad=new EffectSummary(e.knownReads(),e.mayWrites(),e.mustOverwrite(),e.exposedRegions(),e.unknownReadBound(),EffectBound.NONE,e.unknownExposureBound(),e.environment(),e.values(),e.proof());
            var fs=new ArrayList<>(input.statements());fs.set(fs.indexOf(fact),new OtherStatement(fact.header(),fact.variant(),fact.observedKind(),fact.observedShape(),fact.gapCode(),fact.normalContinuation(),fact.knownReferences(),Optional.of(bad)));
            var malformed=new SpInput(input.unit(),input.policy(),input.dataDeclarations(),fs,input.structure(),input.gaps(),input.coverage(),input.entryInventory(),input.storageIndependence(),input.compositional(),input.storage(),input.fileInventory(),input.sourceDependencies(),input.ordinaryContinuations(),input.controlTopology(),input.factDependencies(),input.nominalValues());
            need(new CobolLowerer().lower(malformed,CobolLower.POSITIVE_OPTIONS).publication().isEmpty(),"typed boundary rejects invented closed footprint");mutations++;cases++;
        }
        System.out.println("PASS DatabaseControlSuite cases="+cases+" mutations="+mutations);
    }
}
