package io.github.gustavo2358.lower.testing;

import io.github.gustavo2358.air.model.*;
import io.github.gustavo2358.lower.application.*;
import io.github.gustavo2358.lower.domain.SpInput;
import java.util.*;
import static io.github.gustavo2358.lower.domain.SpInput.*;
import static io.github.gustavo2358.lower.testing.IfInputs.*;

/** Fixed consumer contracts; large scaling is a separate local qualification entrypoint. */
public final class IfSuite {
    private IfSuite() { }
    public static LoweringResult lower(SpInput input) { return new CobolLowerer().lower(input,ScalarSuite.OPTIONS); }
    private static void reject(SpInput input,String name) {
        var r=lower(input);IfOracle.check(r.status()!=LoweringResult.Status.SUCCESS && r.publication().isEmpty() && !r.admission().diagnostics().isEmpty(),"reject " + name + ": " + r.status());
    }
    private static void negatives() {
        var input=create(3,2,2);var f=branch(input);var p=f.predicateGuarantee();
        for(var pair:List.of(new Object[]{"availability",Availability.UNAVAILABLE},new Object[]{"profile",PredicateProfile.UNAVAILABLE},new Object[]{"resultDomain",PredicateDomain.UNKNOWN},new Object[]{"evaluation",PredicateEvaluation.UNKNOWN},new Object[]{"normalCompletion",PredicateCompletion.UNKNOWN},new Object[]{"readsCompleteness",ReadsCompleteness.PARTIAL},new Object[]{"knownReads",List.of()},new Object[]{"gapCodes",List.of("gap")}))
            reject(branch(input,predicate(f,with(p,(String)pair[0],pair[1]))),"predicate " + pair[0]);
        var ref=f.conditionReads().getFirst();
        for(var bad:List.of(with(ref,"wholeItemAccess",Optional.empty()),with(ref,"role",OperandRole.WRITE),with(with(ref,"binding",new Binding(ResolutionStatus.UNRESOLVED,List.of(),Optional.empty())),"wholeItemAccess",Optional.empty()),with(with(ref,"binding",new Binding(ResolutionStatus.AMBIGUOUS,input.dataDeclarations().stream().map(DataFact::id).toList(),Optional.empty())),"wholeItemAccess",Optional.empty())))
            reject(branch(input,with(f,"conditionReads",List.of(bad))),"predicate whole resolved READ");
        for(var field:List.of("thenArm","elseArm")) {
            var arm=field.equals("thenArm")?f.thenArm():f.elseArm();
            reject(branch(input,with(f,field,with(arm,"contentAvailability",Availability.PARTIAL))),field+" partial");
            reject(branch(input,with(f,field,with(arm,"presence",ClausePresence.UNKNOWN))),field+" presence");
            reject(branch(input,with(f,field,with(arm,"entry",new ExecutableStart(Availability.KNOWN,f.continuation())))),field+" entry mismatch");
        }
        reject(branch(input,with(f,"profile",IfProfile.OUTSIDE_SLICE)),"nested profile");
        reject(branch(input,with(f,"explicitlyTerminated",false)),"not explicitly terminated");
        reject(branch(input,with(f,"continuation",Optional.empty())),"structural continuation absent");
        reject(branch(input,with(f,"normalContinuation",with(with(f.normalContinuation(),"availability",ContinuationAvailability.UNAVAILABLE),"statement",Optional.empty()))),"completion absent");
        reject(branch(input,with(f,"normalContinuation",with(f.normalContinuation(),"statement",Optional.of(f.header().id())))),"completion self");
        var proof=input.storageIndependence().orElseThrow();
        reject(proof(input,Optional.empty()),"storage proof absent");
        for(var pair:List.of(new Object[]{"availability",Availability.UNAVAILABLE},new Object[]{"members",proof.members().subList(0,2)},new Object[]{"members",List.of(proof.members().getFirst(),proof.members().getFirst())},new Object[]{"members",List.of(new DataId(input.unit(),"data:99"),proof.members().getFirst())},new Object[]{"gapCodes",List.of("gap")},new Object[]{"provenance",Optional.empty()},new Object[]{"authority","invented"}))
            reject(proof(input,Optional.of(with(proof,(String)pair[0],pair[1]))),"storage " + pair[0]);
        var statements=new ArrayList<>(input.statements());var child=(MoveFact)statements.getFirst();
        statements.set(0,new OtherStatement(child.header(),Variant.OBSERVED));reject(with(input,"statements",statements),"non-MOVE arm child");
        var nested=with(f,"header",with(f.header(),"containment",new Containment(Optional.of(f.header().id()),Branch.THEN)));
        reject(branch(input,nested),"nested containment not flattened");
        var call=(CallFact)input.statements().get(input.statements().size()-2);
        statements=new ArrayList<>(input.statements());statements.set(statements.size()-2,with(call,"surface",with(with(call.surface(),"using",ClausePresence.PRESENT),"argumentCount",Optional.of(1))));reject(with(input,"statements",statements),"CALL USING outside W1C");
        var relations=new ArrayList<>(input.structure().branches());var relation=relations.getFirst();var children=new ArrayList<>(relation.children());Collections.reverse(children);
        relations.set(0,with(relation,"children",children));reject(with(input,"structure",with(input.structure(),"branches",relations)),"membership order disagrees with entry/completion");
        statements=new ArrayList<>(input.statements());statements.set(0,with(child,"normalContinuation",with(child.normalContinuation(),"statement",Optional.of(child.header().id()))));reject(with(input,"statements",statements),"arm completion cycle");
    }
    public static int run() {
        int before=IfOracle.assertions();
        for(var input:List.of(create(2,1,1),create(2,1,0),create(4,3,2))) {
            var result=lower(input);IfOracle.inspect(input,result);
            var statements=new ArrayList<>(input.statements());var data=new ArrayList<>(input.dataDeclarations());var branches=new ArrayList<>(input.structure().branches());var roots=new ArrayList<>(input.structure().roots());
            Collections.reverse(statements);Collections.reverse(data);Collections.reverse(branches);Collections.reverse(roots);
            var permuted=with(with(with(input,"statements",statements),"dataDeclarations",data),"structure",with(with(input.structure(),"branches",branches),"roots",roots));
            var other=lower(permuted);IfOracle.inspect(permuted,other);IfOracle.check(result.publication().equals(other.publication()),"physical input inventory permutation byte identity");
            IfOracle.check(!result.entries().getFirst().start().equals(result.publication().orElseThrow().units().getFirst().sequences().getFirst().label()),"physical first sequence is not entry authority");
            var publication=result.publication().orElseThrow();var body=publication.units().getFirst();var sequences=new ArrayList<>(body.sequences());Collections.reverse(sequences);
            var permutedPublication=with(publication,"units",List.of(with(body,"sequences",sequences)));
            IfOracle.inspect(input,with(result,"publication",Optional.of(permutedPublication)));
            var proof=input.storageIndependence().orElseThrow();var members=new ArrayList<>(proof.members());Collections.reverse(members);
            var reordered=proof(input,Optional.of(with(proof,"members",members)));var reorderedResult=lower(reordered);IfOracle.inspect(reordered,reorderedResult);
            IfOracle.check(!reorderedResult.publication().orElseThrow().id().equals(result.publication().orElseThrow().id()),"ordered proof facts affect publication revision");
        }
        negatives();int count=IfOracle.assertions()-before;System.out.println("LOWER_IF_TESTS="+count);return count;
    }
    public static int scale() {
        int before=IfOracle.assertions();long[] visits=new long[2];long[] nanos=new long[2];
        for(int i=0;i<2;i++) {
            int n=1000*(i+1);var input=create(n,n,n);long start=System.nanoTime();
            var result=lower(input);nanos[i]=System.nanoTime()-start;IfOracle.inspect(input,result);visits[i]=result.admission().statistics().entitiesVisited();
            System.out.println("IF_SCALE n="+n+" nanos="+nanos[i]+" visits="+visits[i]);
        }
        IfOracle.check(visits[1]<=visits[0]*2+100,"indexed linear work N/2N");return IfOracle.assertions()-before;
    }
    public static void main(String[] args) { if(args.length>0 && args[0].equals("scale"))scale();else run(); }
}
