package io.github.gustavo2358.lower.adapters.testing;

import io.github.gustavo2358.air.model.*;
import io.github.gustavo2358.lower.adapters.cli.CobolLower;
import io.github.gustavo2358.lower.adapters.sp.SpJsonDecoder;
import io.github.gustavo2358.lower.application.*;
import io.github.gustavo2358.lower.domain.SpInput;
import io.github.gustavo2358.lower.domain.StorageFacts;
import io.github.gustavo2358.lower.testing.IfInputs;
import io.github.gustavo2358.air.json.AirJson;
import io.github.gustavo2358.air.model.Ids.OriginId;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.math.BigInteger;
import java.util.*;
import static io.github.gustavo2358.lower.testing.CallOracle.check;

/** Independent constants test sharing, extent and clause origin from real SP2.8 files. */
public final class OverlayStorageIntegrationSuite {
    private OverlayStorageIntegrationSuite() { }
    static byte[] fixture(String name) throws Exception {
        try(var in=OverlayStorageIntegrationSuite.class.getResourceAsStream("/sp/storage-28/"+name+".json")) {
            return Objects.requireNonNull(in,"missing SP2.8 fixture").readAllBytes();
        }
    }
    public static void main(String[] args) throws Exception { run(); }
    public static void run() throws Exception {
        var decoder=new SpJsonDecoder(CobolLower.INPUT_LIMITS);var json=new ObjectMapper();
        for(var name:List.of("overlay-root","overlay-reverse","overlay-nested-filler","overlay-chain","overlay-unproved")) {
            var bytes=fixture(name);var tree=json.readTree(bytes);var decoded=decoder.decode(bytes);
            check(decoded instanceof SpJsonDecoder.Decoded,"closed SP2.8 must decode "+name+": "+decoded);
            var input=((SpJsonDecoder.Decoded)decoded).input();
            var result=RegionalTranslationSuite.lower(input);var p=result.publication().orElseThrow();
            if(name.equals("overlay-unproved")) {
                check(p.storage().isEmpty(),"unproved relation cannot invent persistent storage or Cell per name");
                check(p.uncertainties().stream().anyMatch(u->u.code().equals("cobol-lower:STORAGE_RELATION_UNPROVEN")),"unproved relation must retain its own gap");
                continue;
            }
            check(p.storage().size()==1&&p.storage().getFirst() instanceof Memory.Region,"one Region for all descriptions "+name);
            var region=(Memory.Region)p.storage().getFirst();long extent=name.equals("overlay-nested-filler")?12:8;
            check(region.extent().equals(Optional.of(BigInteger.valueOf(extent))),"independent max footprint "+name);
            check(p.units().getFirst().objects().stream().allMatch(o->o.storage() instanceof Memory.ViewBinding v&&v.region().equals(region.header().id())),"all views share declared Region, no exact alias inference");
            for(var relation:tree.path("storage").path("relations")) {
                var handle=relation.path("id").asText();
                var evidence=p.coverage().items().stream().filter(c->c.sourceKey().endsWith("/storage-relation/"+handle)).findFirst().orElseThrow();
                check(evidence.outputs().contains(region.header().id()),"relation coverage including FILLER: "+handle);
                check(origins(p,region.header().origin()).contains(evidence.origin()),"Region origin includes its shared-location proof");
                for(var object:p.units().getFirst().objects())check(origins(p,object.origin()).contains(evidence.origin()),"every view retains relation provenance");
            }
            if(name.equals("overlay-nested-filler")) {
                var objects=p.units().getFirst().objects();check(objects.size()==5,"FILLER is physical evidence without named object");
                check(objects.stream().filter(o->o.storage() instanceof Memory.ViewBinding v&&v.offset().equals(BigInteger.TWO)).count()==2,"RAW and WS-PGM start after prefix");
                check(objects.stream().anyMatch(o->o.storage() instanceof Memory.ViewBinding v&&v.offset().equals(BigInteger.TEN)),"tail starts after larger alternative");
            }
            if(name.equals("overlay-chain")) {
                var lengths=p.units().getFirst().objects().stream().map(o->((Memory.ViewBinding)o.storage()).extent()).collect(java.util.stream.Collectors.toSet());
                check(lengths.equals(Set.of(BigInteger.valueOf(4),BigInteger.valueOf(6),BigInteger.valueOf(8))),"each interpretation retains its own extent");
            }
            if(name.equals("overlay-root")||name.equals("overlay-reverse")) {
                var assign=(Operations.Assign)RegionalTranslationSuite.instructions(p).getFirst();
                check(assign.value() instanceof Expressions.Literal l&&l.value().equals(new Values.BytesValue(List.of(215,199,212,240,240,240,240,241))),"independent exact IBM bytes through either description");
                var call=(SpInput.CallFact)input.statements().get(1);var selected=((SpInput.DataCallTarget)call.target()).reference().binding().selected().orElseThrow();
                var object=result.data().stream().filter(d->d.source().equals(selected)).findFirst().orElseThrow().object();
                var invoke=p.units().getFirst().sequences().stream().map(Sequence::terminator).filter(Operations.Invoke.class::isInstance).map(Operations.Invoke.class::cast).findFirst().orElseThrow();
                check(invoke.target() instanceof Interactions.ComputedTarget t&&t.name() instanceof Expressions.Read r&&r.place() instanceof Places.ObjectPlace o&&o.object().equals(object),"CALL retains selected object interpretation in each direction");
            }
        }
        negativesAndIdentity();
        System.out.println("LOWER_OVERLAY_REAL_FIXTURES=5");
    }
    private static Set<OriginId> origins(Publication p,OriginId start) {
        var map=new HashMap<OriginId,Origins.Origin>();p.origins().forEach(o->map.put(o.id(),o));
        var seen=new HashSet<OriginId>();var pending=new ArrayDeque<OriginId>();pending.push(start);
        while(!pending.isEmpty()) { var id=pending.pop();if(!seen.add(id))continue;var origin=map.get(id);check(origin!=null,"closed origin graph");
            if(origin instanceof Origins.Derived d)pending.addAll(d.inputs()); }
        return seen;
    }
    private static void negativesAndIdentity() throws Exception {
        var json=new ObjectMapper();var decoder=new SpJsonDecoder(CobolLower.INPUT_LIMITS);
        for(var mutation:List.of("missing-relations","null-relations","unknown-field","missing-owner","missing-target","null-id","number-id","status","version","downgrade")) {
            var tree=(ObjectNode)json.readTree(fixture("overlay-chain"));var storage=(ObjectNode)tree.path("storage");var relation=(ObjectNode)storage.path("relations").get(0);
            switch(mutation) {
                case "missing-relations" -> storage.remove("relations");case "null-relations" -> storage.putNull("relations");
                case "unknown-field" -> relation.put("inferredAlias",true);case "missing-owner" -> relation.remove("owner");
                case "missing-target" -> relation.remove("target");case "null-id" -> relation.putNull("id");case "number-id" -> relation.put("id",1);
                case "status" -> relation.put("status","EXACT_ALIAS");case "version" -> storage.put("version","1.0.0");case "downgrade" -> tree.put("contractVersion","2.7.0");
            }
            check(decoder.decode(json.writeValueAsBytes(tree)) instanceof SpJsonDecoder.Rejected,"closed SP2.8 JSON rejects "+mutation);
        }
        check(decoder.decode(RegionalStorageIntegrationSuite.fixture("group-child")) instanceof SpJsonDecoder.Decoded,"unchanged exact SP2.7 reader");
        var input=((SpJsonDecoder.Decoded)decoder.decode(fixture("overlay-chain"))).input();var st=input.storage().orElseThrow();var first=st.relations().getFirst();var second=st.relations().get(1);
        for(var mutation:List.of("duplicate","owner","target","self","forward","no-target","no-reason","foreign")) {
            var relations=new ArrayList<>(st.relations());var missing=new StorageFacts.NodeId(input.unit(),"storage-node:999");
            switch(mutation) {
                case "duplicate" -> relations.add(first);case "owner" -> relations.set(0,IfInputs.with(first,"owner",missing));
                case "target" -> relations.set(0,IfInputs.with(first,"target",Optional.of(missing)));case "self" -> relations.set(0,IfInputs.with(first,"target",Optional.of(first.owner())));
                case "forward" -> relations.set(0,IfInputs.with(first,"target",Optional.of(second.owner())));case "no-target" -> relations.set(0,IfInputs.with(first,"target",Optional.empty()));
                case "no-reason" -> relations.set(0,IfInputs.with(IfInputs.with(first,"target",Optional.empty()),"status",StorageFacts.RelationStatus.UNPROVEN));
                case "foreign" -> relations.set(0,IfInputs.with(first,"id",IfInputs.with(first.id(),"unit",IfInputs.with(input.unit(),"canonicalProgramName","FOREIGN"))));
            }
            invalid(IfInputs.with(input,"storage",Optional.of(IfInputs.with(st,"relations",relations))),mutation);
        }
        var views=new ArrayList<>(st.views());int index=0;while(!views.get(index).node().equals(second.owner()))index++;var view=views.get(index);
        views.set(index,IfInputs.with(view,"offset",IfInputs.with(view.offset(),"value",Optional.of(BigInteger.ONE))));
        invalid(IfInputs.with(input,"storage",Optional.of(IfInputs.with(st,"views",views))),"start differs despite in-bounds shared base");
        var result=RegionalTranslationSuite.lower(input);var before=new AirJson().encode(result.publication().orElseThrow());
        var reversed=new ArrayList<>(st.relations());Collections.reverse(reversed);
        var permuted=IfInputs.with(input,"storage",Optional.of(IfInputs.with(st,"relations",reversed)));
        check(Arrays.equals(before,new AirJson().encode(RegionalTranslationSuite.lower(permuted).publication().orElseThrow())),"relation permutation preserves all AIR bytes");
        var changed=new ArrayList<>(st.relations());changed.set(1,IfInputs.with(second,"target",first.target()));
        var redirected=IfInputs.with(input,"storage",Optional.of(IfInputs.with(st,"relations",changed)));
        check(!result.publication().orElseThrow().id().equals(RegionalTranslationSuite.lower(redirected).publication().orElseThrow().id()),"relation target participates in publication identity");
        var unknown=((SpJsonDecoder.Decoded)decoder.decode(fixture("overlay-unproved"))).input();var us=unknown.storage().orElseThrow();
        var falseIndependent=us.bases().stream().map(b->IfInputs.with(b,"allocation",StorageFacts.Allocation.INDEPENDENT_LOCAL_WORKING_STORAGE)).toList();
        invalid(IfInputs.with(unknown,"storage",Optional.of(IfInputs.with(us,"bases",falseIndependent))),"unproved relation contradicts allocation independence");
        var declarations=new ArrayList<>(unknown.dataDeclarations());
        declarations.set(0,IfInputs.with(declarations.getFirst(),"scalarText",Optional.of(new SpInput.ScalarText(SpInput.LogicalDomain.TEXT,8,SpInput.StorageClass.WORKING_STORAGE,SpInput.DeclarationScope.LOCAL))));
        invalid(IfInputs.with(unknown,"dataDeclarations",declarations),"unproved relation contradicts standalone scalar proof");
        System.out.println("LOWER_OVERLAY_JSON_NEGATIVES=10; MEMORY_NEGATIVES=11; IDENTITY_ORACLES=2");
    }
    private static void invalid(SpInput input,String reason) {
        var admission=new EntryGobackAdmission().admit(input,CobolLower.OPTIONS.admission());
        check(admission.status()==Admission.Status.INVALID_INPUT,"core rejects inconsistent physical relation: "+reason+": "+admission.status());
    }
}
