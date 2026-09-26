package io.github.gustavo2358.lower.adapters.testing;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.node.*;
import io.github.gustavo2358.air.json.AirJson;
import io.github.gustavo2358.air.model.*;
import io.github.gustavo2358.lower.adapters.cli.CobolLower;
import io.github.gustavo2358.lower.adapters.sp.SpJsonDecoder;
import io.github.gustavo2358.lower.application.*;
import io.github.gustavo2358.lower.domain.SpInput;
import io.github.gustavo2358.lower.testing.IfInputs;
import java.util.*;
import static io.github.gustavo2358.lower.testing.CallOracle.check;

/** Finite Multiplicity: hundreds of ordered occurrences never trigger count-based fallback. */
public final class ConditionalGoToIntegrationSuite {
    private static final ObjectMapper JSON=new ObjectMapper();
    private static byte[] fixture() throws Exception {
        try(var in=ConditionalGoToIntegrationSuite.class.getResourceAsStream("/sp/goto-depending/d1.json")){return Objects.requireNonNull(in).readAllBytes();}
    }
    private static SpInput decode(byte[] raw) {
        var r=new SpJsonDecoder(CobolLower.INPUT_LIMITS).decode(raw);
        check(r instanceof SpJsonDecoder.Decoded,"SP2.6 typed conditional decode: "+r);return ((SpJsonDecoder.Decoded)r).input();
    }
    private static SpInput.ConditionalGoToFact transfer(SpInput input) {
        return input.statements().stream().filter(SpInput.ConditionalGoToFact.class::isInstance).map(SpInput.ConditionalGoToFact.class::cast).findFirst().orElseThrow();
    }
    private static SpInput replace(SpInput input,SpInput.ConditionalGoToFact g) {
        return IfInputs.with(input,"statements",input.statements().stream().map(s->s.header().id().equals(g.header().id())?g:s).toList());
    }
    private static void invalid(SpInput input,String reason) {
        var r=new CobolLowerer().lower(input,CobolLower.OPTIONS);check(r.status()==LoweringResult.Status.INVALID_INPUT,"reject "+reason+": "+r.status());
    }
    private static Operations.Opaque operation(LoweringResult r,SpInput.ConditionalGoToFact g) {
        var links=r.statements().stream().filter(s->s.source().equals(g.header().id())).toList();check(links.size()==1,"one conditional occurrence, no selector reevaluation");
        return r.publication().orElseThrow().units().getFirst().sequences().stream().filter(s->s.label().equals(links.getFirst().label()))
            .map(s->{check(s.instructions().isEmpty(),"no writes/decision expansion");return (Operations.Opaque)s.terminator();}).findFirst().orElseThrow();
    }
    private static SpInput multiplicity(SpInput base,int n) {
        var g=transfer(base);var facts=new ArrayList<SpInput.StatementFact>();var ids=new ArrayList<SpInput.StatementId>();
        for(int i=0;i<n;i++)ids.add(new SpInput.StatementId(base.unit(),"statement:"+(100+i)));
        for(int i=0;i<n;i++) {
            var h=IfInputs.with(IfInputs.with(g.header(),"id",ids.get(i)),"programPoint",i);
            var ref=IfInputs.with(g.selector().orElseThrow(),"id",new SpInput.OperandId(ids.get(i),"operand:"+(100+i)+":0"));
            var next=i+1<n?new SpInput.NormalContinuation(SpInput.ContinuationAvailability.KNOWN,Optional.of(ids.get(i+1)),g.header().provenance()):g.normalContinuation();
            facts.add(new SpInput.ConditionalGoToFact(h,Optional.of(ref),true,g.selectorOrigin(),g.destinations(),next,g.gapCodes()));
        }
        for(var f:base.statements())if(f!=g)facts.add(IfInputs.with((Record)f,"header",IfInputs.with(f.header(),"programPoint",f.header().programPoint()+n-1)) instanceof SpInput.StatementFact changed?changed:f);
        var roots=new ArrayList<SpInput.StatementId>();for(var id:base.structure().roots())if(id.equals(g.header().id()))roots.addAll(ids);else roots.add(id);
        var entry=base.entryInventory().entries().getFirst();entry=IfInputs.with(entry,"start",new SpInput.ExecutableStart(SpInput.Availability.KNOWN,Optional.of(ids.getFirst())));
        var coverage=IfInputs.with(IfInputs.with(base.coverage(),"observedStatements",base.coverage().observedStatements()+n-1),"modeledStatements",base.coverage().modeledStatements()+n-1);
        return IfInputs.with(IfInputs.with(IfInputs.with(IfInputs.with(base,"statements",facts),"structure",IfInputs.with(base.structure(),"roots",roots)),"entryInventory",IfInputs.with(base.entryInventory(),"entries",List.of(entry))),"coverage",coverage);
    }
    public static void run() throws Exception {
        var raw=fixture();var base=decode(raw);var g=transfer(base);var codec=new AirJson();
        long previousReferences=0;int previousCount=0;
        for(int n:new int[]{1,2,5,40,100,200,255}) {
            var ds=new ArrayList<SpInput.GoToDestination>();
            for(int i=0;i<n;i++)ds.add(IfInputs.with(g.destinations().get(i%g.destinations().size()),"ordinal",i));
            var fact=IfInputs.with(g,"destinations",ds);var input=replace(base,fact);var r=PartialIntegrationSuite.lower(input);var op=operation(r,fact);
            check(op.envelope().control().remainder()==Scopes.NoControl.INSTANCE,"closed regardless of target count "+n);
            check(op.envelope().control().known().size()==Math.min(n,3)+1,"all distinct target labels and normal continuation");
            check(op.envelope().memory().knownReads().size()==1&&op.envelope().memory().knownWrites().isEmpty()
                &&op.envelope().memory().otherWrites()==Scopes.NoMemory.INSTANCE,"one integer read, no writes");
            var references=r.admission().statistics().referencesChecked();
            if(previousCount>0)check(references-previousReferences<=30L*(n-previousCount),"linear indexed admission work, not pair comparisons");
            previousCount=n;previousReferences=references;
            var inventory=new ArrayList<>(input.statements());Collections.reverse(inventory);
            check(Arrays.equals(codec.encode(r.publication().orElseThrow()),codec.encode(PartialIntegrationSuite.lower(IfInputs.with(input,"statements",inventory)).publication().orElseThrow())),"physical statement permutation preserves ordered occurrences");
        }
        for(int n:new int[]{1,2,5,40}) {
            var input=multiplicity(base,n);var result=PartialIntegrationSuite.lower(input);int count=0;
            for(var fact:input.statements())if(fact instanceof SpInput.ConditionalGoToFact c){operation(result,c);count++;}
            check(count==n,"finite multiplicity of source occurrences "+n);
        }
        long diagnosticReferences=-1;
        for(int diagnostics:new int[]{0,1,50}) {
            var codes=java.util.stream.IntStream.range(0,diagnostics).mapToObj(i->"selector-gap-"+i).toList();
            var partial=IfInputs.with(IfInputs.with(g,"selectorInteger",false),"gapCodes",codes);
            var r=PartialIntegrationSuite.lower(replace(base,partial));var op=operation(r,partial);
            check(op.envelope().control().remainder()==Scopes.NoControl.INSTANCE,
                "selector diagnostics do not open the Unit");
            check(op.envelope().control().known().size()==4 && op.envelope().memory().otherReads()==Scopes.NoMemory.INSTANCE,
                "ordinal alternatives and source read are independent of diagnostic count");
            long currentReferences=r.admission().statistics().referencesChecked();
            if(diagnosticReferences<0)diagnosticReferences=currentReferences;
            else check(currentReferences==diagnosticReferences,"diagnostic count does not change reference work");
        }
        var ds=g.destinations();
        for(int ordinal:new int[]{-1,1,9}) {
            var changed=new ArrayList<>(ds);changed.set(0,IfInputs.with(ds.getFirst(),"ordinal",ordinal));
            invalid(replace(base,IfInputs.with(g,"destinations",changed)),"bad ordinal "+ordinal);
        }
        var foreign=IfInputs.with(base.unit(),"structuralPath",List.of(999));
        var target=IfInputs.with(ds.getFirst(),"targetEntry",Optional.of(new SpInput.StatementId(foreign,ds.getFirst().targetEntry().orElseThrow().handle())));
        var changed=new ArrayList<>(ds);changed.set(0,target);invalid(replace(base,IfInputs.with(g,"destinations",changed)),"cross-unit executable entry");
        changed=new ArrayList<>(ds);changed.set(0,IfInputs.with(ds.getFirst(),"entryOrigin",ds.get(1).entryOrigin()));
        invalid(replace(base,IfInputs.with(g,"destinations",changed)),"entry provenance mismatch");
        changed=new ArrayList<>(ds);changed.set(1,IfInputs.with(IfInputs.with(ds.get(1),"target",ds.getFirst().target()),"procedureOrigin",ds.getFirst().procedureOrigin()));
        invalid(replace(base,IfInputs.with(g,"destinations",changed)),"same procedure with inconsistent executable entries");
        var text=base.dataDeclarations().stream().filter(d->d.scalarText().isPresent()).findFirst().orElseThrow();var ref=g.selector().orElseThrow();
        var binding=IfInputs.with(IfInputs.with(ref.binding(),"selected",Optional.of(text.id())),"candidates",List.of(text.id()));
        ref=IfInputs.with(IfInputs.with(ref,"binding",binding),"wholeItemAccess",Optional.of(new SpInput.WholeItemAccess(text.id())));
        invalid(replace(base,IfInputs.with(g,"selector",Optional.of(ref))),"integer selector cannot point to TEXT");
        invalid(replace(base,IfInputs.with(g,"normalContinuation",IfInputs.with(g.normalContinuation(),"statement",ds.getFirst().targetEntry()))),"incorrect fallthrough target and provenance");
        for(String mutation:List.of("missing-ordinal","missing-selector-proof","historical-version")) {
            var tree=JSON.readTree(raw);var wire=(ObjectNode)java.util.stream.StreamSupport.stream(tree.path("statements").spliterator(),false).filter(s->s.path("variant").asText().equals("GO_TO_DEPENDING_ON")).findFirst().orElseThrow();
            if(mutation.equals("missing-ordinal"))((ObjectNode)wire.path("destinations").get(0)).remove("ordinal");
            else if(mutation.equals("missing-selector-proof"))wire.remove("selectorInteger");
            else ((ObjectNode)tree).put("contractVersion","2.5.0");
            var result=new SpJsonDecoder(CobolLower.INPUT_LIMITS).decode(JSON.writeValueAsBytes(tree));
            check(!(result instanceof SpJsonDecoder.Decoded),"no silent wire repair "+mutation);
        }
        System.out.println("PASS: conditional GO TO finite multiplicity, generic control, malformed SP and physical order");
    }
    public static void main(String[] args) throws Exception {run();}
}
