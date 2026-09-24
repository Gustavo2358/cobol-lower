package io.github.gustavo2358.lower.adapters.testing;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.node.*;
import io.github.gustavo2358.lower.adapters.sp.SpJsonDecoder;
import io.github.gustavo2358.lower.adapters.cli.CobolLower;
import io.github.gustavo2358.lower.application.*;
import io.github.gustavo2358.lower.domain.*;
import io.github.gustavo2358.air.model.*;
import java.util.*;

/** New fact authority and frozen historical interpretation cross the same decoder/admission. */
public final class FactDependencyWireSuite {
    private FactDependencyWireSuite() { }
    private static final ObjectMapper JSON=new ObjectMapper();
    private static int checks;
    private static void need(boolean ok,String why){checks++;if(!ok)throw new AssertionError(why);}
    private static SpInput decode(JsonNode node)throws Exception {
        var r=new SpJsonDecoder(CobolLower.INPUT_LIMITS).decode(JSON.writeValueAsBytes(node));
        need(r instanceof SpJsonDecoder.Decoded,"valid producer facts decode: "+r);return ((SpJsonDecoder.Decoded)r).input();
    }
    private static ObjectNode fixture(String name)throws Exception {
        try(var s=FactDependencyWireSuite.class.getResourceAsStream("/sp/fact-dependency-r2/"+name+".json")){
            if(s==null)throw new AssertionError("producer fixture missing");return (ObjectNode)JSON.readTree(s);}
    }
    private static LoweringResult lower(SpInput input){return new CobolLowerer().lower(input,CobolLower.OPTIONS);}
    public static void main(String[] args)throws Exception {
        checks=0;var tree=fixture("mixed-profile-absent");var input=decode(tree);var graph=input.factDependencies().orElseThrow();
        var result=lower(input);need(result.publication().isPresent(),"mixed local facts translate without profile: "+result.status());
        need(result.validation().orElseThrow().isStructurallyValid(),"AIR validator stays active");
        var target=input.dataDeclarations().stream().filter(d->d.canonicalName().equals("TARGET")).findFirst().orElseThrow();
        var link=result.data().stream().filter(d->d.source().equals(target.id())).findFirst().orElseThrow();
        var object=result.publication().orElseThrow().units().getFirst().objects().stream().filter(o->o.id().equals(link.object())).findFirst().orElseThrow();
        need(object.storage() instanceof Memory.CellBinding,"proved local TEXT cell survives absent physical profile");
        need(graph.equals(JSON.readValue(JSON.writeValueAsBytes(graph),FactDependencies.class)),"typed roundtrip");
        var old=tree.deepCopy();old.remove("factDependencies");old.put("contractVersion","2.39.0");
        var legacy=decode(old);need(legacy.factDependencies().isEmpty(),"old wire remains old interpretation");
        need(lower(legacy).status()==LoweringResult.Status.BLOCKED_LOWERING,"historical storage blocker is not retroactively reinterpreted");
        for(var mutation:List.of("missing","null","old-version","missing-proof","missing-bound","missing-alias-dependency","unknown-kind","missing-input-availability","missing-proof-subject","wrong-proof-subject","extra")) {
            var bad=tree.deepCopy();var f=(ObjectNode)bad.path("factDependencies");
            switch(mutation) {
                case "missing" -> bad.remove("factDependencies");
                case "null" -> bad.putNull("factDependencies");
                case "old-version" -> bad.put("contractVersion","2.39.0");
                case "missing-proof" -> ((ArrayNode)f.path("proofs")).remove(0);
                case "missing-bound" -> {var b=(ObjectNode)f.path("bindings").get(0);b.put("exactCell","");b.set("cells",JSON.createArrayNode());b.set("regions",JSON.createArrayNode());}
                case "missing-alias-dependency" -> {for(var fact:f.path("facts"))if(fact.path("kind").asText().equals("LOCAL_CELL")){var deps=(ArrayNode)fact.path("dependencies");for(int i=deps.size()-1;i>=0;i--)if(deps.get(i).asText().startsWith("ALIAS_CLOSURE/"))deps.remove(i);break;}}
                case "unknown-kind" -> ((ObjectNode)f.path("proofs").get(0)).put("kind","PROBABLY_INDEPENDENT");
                case "missing-input-availability" -> ((ObjectNode)f.path("inputs").get(0)).remove("available");
                case "missing-proof-subject" -> ((ObjectNode)f.path("proofs").get(0)).remove("subject");
                case "wrong-proof-subject" -> {for(var proof:f.path("proofs"))if(proof.path("kind").asText().equals("LOGICAL_TYPE")){((ObjectNode)proof).put("subject",proof.path("scope").asText());break;}}
                case "extra" -> f.put("inferFromNames",true);
                default -> throw new AssertionError(mutation);
            }
            need(new SpJsonDecoder(CobolLower.INPUT_LIMITS).decode(JSON.writeValueAsBytes(bad)) instanceof SpJsonDecoder.Rejected,"malformed new contract rejected: "+mutation);
        }
        var facts=new ArrayList<>(graph.facts());var victim=facts.stream().filter(f->f.kind()==FactDependencies.FactKind.LOCAL_CELL).findFirst().orElseThrow();facts.remove(victim);
        facts.add(new FactDependencies.Fact(victim.id(),victim.kind(),victim.subject(),victim.region(),victim.dependencies().stream().filter(d->!d.startsWith("ALIAS_CLOSURE/")).toList()));
        boolean rejected=false;try{new FactDependencies(graph.authority(),graph.inputs(),graph.proofs(),graph.regions(),facts,graph.bindings());}catch(IllegalArgumentException e){rejected=true;}
        need(rejected,"in-memory causal dependency removal rejected");
        for(var name:List.of("open-copy","alias","opaque-include")) {
            var partial=decode(fixture(name));var lowered=lower(partial);
            need(lowered.publication().isPresent(),"bounded partial input remains representable: "+name+" "+lowered.status());
            need(lowered.validation().orElseThrow().isStructurallyValid(),"partial AIR valid: "+name);
            var unknown=lowered.publication().orElseThrow().units().getFirst().objects().stream()
                .filter(o->o.displayName().orElse("").equals("TARGET")).findFirst().orElseThrow();
            need(unknown.storage() instanceof Memory.UnknownBinding,"input/alias uncertainty forbids exact target cell: "+name);
        }
        for(var name:List.of("header-copy","child-header-copy")) {
            var headerTree=fixture(name);var headerInput=decode(headerTree);var header=headerInput.factDependencies().orElseThrow();
            var declaration=headerInput.dataDeclarations().stream().filter(d->d.canonicalName().equals("TARGET")).findFirst().orElseThrow();
            var node=headerInput.storage().orElseThrow().nodes().stream().filter(n->n.data().equals(Optional.of(declaration.id()))).findFirst().orElseThrow().id().handle();
            need(header.facts().stream().filter(f->f.subject().equals(node)&&(f.kind()==FactDependencies.FactKind.LOGICAL_TEXT||f.kind()==FactDependencies.FactKind.LOCAL_CELL)).noneMatch(header::available),"unknown declaration clauses cannot prove logical text/cell: "+name);
            var lowered=lower(headerInput);
            need(lowered.publication().isPresent(),"unknown header remains representable: "+name+" "+lowered.status());
            need(lowered.validation().orElseThrow().isStructurallyValid(),"header gap AIR valid: "+name);
            need(lowered.data().stream().noneMatch(d->d.source().equals(declaration.id())),"legacy data metadata cannot recreate unavailable declaration: "+name);
            var victimInput=header.inputs().stream().filter(i->i.declarationScopes().contains(node)).findFirst().orElseThrow();
            var bad=headerTree.deepCopy();
            for(var i:bad.path("factDependencies").path("inputs"))if(i.path("id").asText().equals(victimInput.id()))((ObjectNode)i).set("declarationScopes",JSON.createArrayNode());
            need(new SpJsonDecoder(CobolLower.INPUT_LIMITS).decode(JSON.writeValueAsBytes(bad)) instanceof SpJsonDecoder.Rejected,"removing input declaration relation rejects before AIR: "+name);
            var changed=header.inputs().stream().map(i->i.equals(victimInput)?new FactDependencies.Input(i.id(),i.kind(),i.available(),i.contextScopes(),i.closureScopes(),List.of(),i.provenance()):i).toList();
            boolean typedRejected=false;try{new FactDependencies(header.authority(),changed,header.proofs(),header.regions(),header.facts(),header.bindings());}catch(IllegalArgumentException e){typedRejected=true;}
            need(typedRejected,"in-memory input relation mutation rejects: "+name);
            bad=headerTree.deepCopy();var proofs=(ArrayNode)bad.path("factDependencies").path("proofs");
            for(int i=proofs.size()-1;i>=0;i--)if(proofs.get(i).path("kind").asText().equals("DECLARATION_CONTEXT")&&proofs.get(i).path("subject").asText().equals(node))proofs.remove(i);
            need(new SpJsonDecoder(CobolLower.INPUT_LIMITS).decode(JSON.writeValueAsBytes(bad)) instanceof SpJsonDecoder.Rejected,"declaration context removal rejects before AIR: "+name);
        }
        var stripped=decode(fixture("removed-diagnostic"));
        need(stripped.factDependencies().orElseThrow().facts().stream().filter(f->f.kind()==FactDependencies.FactKind.LOCAL_CELL)
            .noneMatch(stripped.factDependencies().orElseThrow()::available),"diagnostic deletion does not restore dependent cells");
        need(lower(stripped).publication().isEmpty(),"inconsistent entry input evidence remains rejected before AIR");
        var peer=tree.deepCopy();for(var key:List.of("inputs","proofs","regions","facts","bindings"))reverse((ArrayNode)peer.path("factDependencies").path(key));
        for(var key:List.of("nodes","bases","views"))reverse((ArrayNode)peer.path("storage").path(key));
        reverse((ArrayNode)peer.path("dataDeclarations"));reverse((ArrayNode)peer.path("statements"));
        var reordered=decode(peer);need(graph.equals(reordered.factDependencies().orElseThrow()),"inventory order does not change facts");
        need(result.publication().equals(lower(reordered).publication()),"inventory permutation preserves AIR byte/semantic identity");
        System.out.println("FACT_DEPENDENCY_WIRE_CHECKS="+checks);
    }
    private static void reverse(ArrayNode a){var values=new ArrayList<JsonNode>();a.forEach(values::add);Collections.reverse(values);a.removeAll();values.forEach(a::add);}
}
