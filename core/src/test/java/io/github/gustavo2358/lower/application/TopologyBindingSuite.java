package io.github.gustavo2358.lower.application;

import io.github.gustavo2358.lower.domain.ControlTopology;
import io.github.gustavo2358.lower.domain.SpInput;
import static io.github.gustavo2358.lower.domain.ControlTopology.*;
import java.util.*;

/** Algebraic oracles: same occurrence, different active region; no COBOL verbs. */
public final class TopologyBindingSuite {
    private TopologyBindingSuite() { }
    private static final List<String> P=List.of("proof");
    private static Target target(TargetKind kind,String reference){return new Target(kind,reference,P);}
    private static Target next(String id){return target(TargetKind.OCCURRENCE,id);}
    private static Target complete(String id){return target(TargetKind.COMPLETE,id);}
    private static final class Fixture {
        final List<Occurrence> occurrences=new ArrayList<>();final List<Region> regions=new ArrayList<>();
        final List<Boundary> boundaries=new ArrayList<>();final List<Outcome> outcomes=new ArrayList<>();final List<Binding> bindings=new ArrayList<>();
        void occurrence(String id,String region,OutcomeKind kind,Target target,String binding){
            occurrences.add(new Occurrence(id,region,List.of(id+"/out"),P));outcomes.add(new Outcome(id+"/out",id,kind,"normal",target,binding,P));
        }
        void region(String id,RegionKind kind,String parent,List<String> members,List<String> children,Target entry,Target after){
            regions.add(new Region(id,kind,parent,entry,members,children,id+"/end",P));boundaries.add(new Boundary(id+"/end",id,after,P));
        }
        ControlTopology build(){
            var location=new SpInput.Location("oracle.cbl",1,0,1,1);var proof=new Proof("proof",ProofKind.LOCAL_GRAMMAR,"oracle",new SpInput.Provenance(location,location,List.of(),true),List.of());
            return new ControlTopology("FRONTEND_CONTROL_TOPOLOGY_R1",occurrences,regions,boundaries,outcomes,bindings,List.of(proof));
        }
    }
    private static Fixture fixture(){
        var f=new Fixture();
        f.region("root",RegionKind.PROCEDURE,"",List.of("call1","call2","stop"),List.of("A","B","C","single","thru"),next("call1"),target(TargetKind.PROGRAM_RETURN,"root"));
        f.region("A",RegionKind.PARAGRAPH,"root",List.of("a"),List.of(),next("a"),target(TargetKind.REGION_ENTRY,"B"));
        f.region("B",RegionKind.PARAGRAPH,"root",List.of("b"),List.of(),next("b"),target(TargetKind.REGION_ENTRY,"C"));
        f.region("C",RegionKind.PARAGRAPH,"root",List.of("c"),List.of(),next("c"),next("stop"));
        f.region("single",RegionKind.RANGE,"root",List.of(),List.of("A"),target(TargetKind.REGION_ENTRY,"A"),next("call2"));
        f.region("thru",RegionKind.RANGE,"root",List.of(),List.of("A","B","C"),target(TargetKind.REGION_ENTRY,"A"),next("stop"));
        f.bindings.add(new Binding("invoke1","call1","single","A/end",next("call2"),"BODY","RESUME",List.of(),P));
        f.bindings.add(new Binding("invoke2","call2","thru","C/end",next("stop"),"BODY","RESUME",List.of(),P));
        f.occurrence("call1","root",OutcomeKind.LOCAL_INVOKE,target(TargetKind.REGION_ENTRY,"single"),"invoke1");
        f.occurrence("call2","root",OutcomeKind.LOCAL_INVOKE,target(TargetKind.REGION_ENTRY,"thru"),"invoke2");
        f.occurrence("stop","root",OutcomeKind.PROGRAM_RETURN,target(TargetKind.PROGRAM_RETURN,"root"),"");
        for(var id:List.of("a","b","c"))f.occurrence(id,id.toUpperCase(Locale.ROOT),OutcomeKind.NORMAL,complete(id.toUpperCase(Locale.ROOT)),"");
        return f;
    }
    public static int run(){
        var f=fixture();var t=f.build();var binder=new TopologyBinding(t);int n=0;
        need(binder.resolve(complete("A"),null).reference().equals("b"),"ordinary completion follows default");n++;
        need(binder.resolve(complete("A"),binder.binding("invoke1")).kind()==TargetKind.COMPLETE,"single region returns at endpoint");n++;
        need(binder.resolve(complete("A"),binder.binding("invoke2")).reference().equals("b"),"THRU intermediate A must not return");n++;
        need(binder.resolve(complete("B"),binder.binding("invoke2")).reference().equals("c"),"THRU intermediate B must not return");n++;
        need(binder.resolve(complete("C"),binder.binding("invoke2")).kind()==TargetKind.COMPLETE,"only final THRU endpoint returns");n++;
        need(!binder.binding("invoke1").resume().equals(binder.binding("invoke2").resume()),"caller resumes stay distinct");n++;
        need(binder.closure("a",binder.binding("invoke1")).equals(Set.of("a")),"activation closure stops at endpoint");n++;
        need(binder.closure("a",binder.binding("invoke2")).equals(Set.of("a","b","c")),"THRU closure composes paragraph defaults");n++;
        need(binder.closure("stop",null).equals(Set.of("stop")),"return has no fallthrough");n++;
        Collections.reverse(f.occurrences);Collections.reverse(f.regions);Collections.reverse(f.boundaries);Collections.reverse(f.outcomes);Collections.reverse(f.bindings);
        need(t.equals(f.build()),"physical topology inventory permutation is canonical");n++;
        var missing=fixture();missing.occurrences.removeIf(o->o.statement().equals("a"));reject(missing::build,"missing referenced target");n++;
        var wrong=fixture();var call=wrong.bindings.getFirst();wrong.bindings.set(0,new Binding(call.id(),call.caller(),call.region(),"B/end",call.resume(),"BODY","RESUME",List.of(),P));reject(wrong::build,"wrong endpoint");n++;
        var compound=fixture();compound.occurrences.removeIf(o->o.statement().equals("a"));compound.outcomes.removeIf(o->o.statement().equals("a"));
        var region=compound.regions.stream().filter(r->r.id().equals("A")).findFirst().orElseThrow();compound.regions.remove(region);
        compound.regions.add(new Region("A",RegionKind.PARAGRAPH,"root",next("a"),List.of(),List.of("evaluate"),"A/end",P));
        compound.region("evaluate",RegionKind.EVALUATE,"A",List.of(),List.of("arm"),next("a"),complete("A"));
        compound.region("arm",RegionKind.EVALUATE_ARM,"evaluate",List.of("a"),List.of(),next("a"),complete("evaluate"));
        compound.occurrence("a","arm",OutcomeKind.NORMAL,complete("arm"),"");var composed=new TopologyBinding(compound.build());
        need(composed.resolve(complete("arm"),null).reference().equals("b"),"arm-parent-paragraph composition");n++;
        need(composed.resolve(complete("arm"),composed.binding("invoke1")).kind()==TargetKind.COMPLETE,"same rule catches contextual completion");n++;
        var unknown=fixture();unknown.outcomes.set(5,new Outcome("c/out","c",OutcomeKind.UNKNOWN_LOCAL,"normal",target(TargetKind.UNKNOWN_LOCAL,"C"),"",P));
        need(!new TopologyBinding(unknown.build()).unavailableBounds("call1").isEmpty(),"unknown bound is not silently materialized as empty/all labels");n++;
        return n;
    }
    private static void need(boolean value,String message){if(!value)throw new AssertionError(message);}
    private static void reject(Runnable action,String message){try{action.run();}catch(IllegalArgumentException ex){return;}throw new AssertionError(message);}
    public static void main(String[] args){System.out.println("TOPOLOGY_BINDING_TESTS="+run());}
}
