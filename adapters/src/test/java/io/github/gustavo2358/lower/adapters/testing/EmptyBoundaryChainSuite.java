package io.github.gustavo2358.lower.adapters.testing;

import com.fasterxml.jackson.databind.node.*;
import io.github.gustavo2358.air.model.*;
import io.github.gustavo2358.air.model.Ids.*;
import io.github.gustavo2358.lower.adapters.cli.CobolLower;
import io.github.gustavo2358.lower.adapters.sp.SpJsonDecoder;
import io.github.gustavo2358.lower.application.CobolLowerer;
import java.nio.file.*;
import java.util.*;

/** Real SP admission and assembler; the expected chain is independent of AIR construction. */
public final class EmptyBoundaryChainSuite {
    private EmptyBoundaryChainSuite() { }
    private static final String ROOT="region:procedure:13",BODY="region:paragraph:25";
    private static ObjectNode target(String kind,String ref,String proof) {
        var t=CobolControlSuite.J.createObjectNode();t.put("kind",kind);t.put("reference",ref);t.putArray("proofs").add(proof);return t;
    }
    private static ObjectNode fixture(int count)throws Exception {
        ObjectNode tree;
        try(var stream=EmptyBoundaryChainSuite.class.getResourceAsStream("/sp/perform-reentry/sequential-callers.json")) {
            tree=(ObjectNode)CobolControlSuite.J.readTree(Objects.requireNonNull(stream));
        }
        var topology=(ObjectNode)tree.path("controlTopology");
        var regions=(ArrayNode)topology.path("regions");var boundaries=(ArrayNode)topology.path("boundaries");
        var proofs=(ArrayNode)topology.path("proofs");var template=(ObjectNode)proofs.get(0);
        ObjectNode lastDefault=null;
        for(var b:boundaries)if(b.path("region").asText().equals(BODY)) {
            lastDefault=((ObjectNode)b.path("ordinaryDefault")).deepCopy();
            ((ObjectNode)b).set("ordinaryDefault",target("REGION_ENTRY","empty/0","proof:region:paragraph:25"));
        }
        if(lastDefault==null)throw new AssertionError("missing body boundary");
        for(int i=0;i<count;i++) {
            var id="empty/"+i;var proof="proof:empty/"+i;var b="boundary:"+id;
            var p=template.deepCopy();p.put("id",proof);p.put("kind","LOCAL_GRAMMAR");p.put("rule","empty-boundary-"+i);p.putArray("dependencies");proofs.add(p);
            var r=regions.addObject();r.put("id",id);r.put("kind","PARAGRAPH");r.put("parent",ROOT);
            r.set("entry",target("COMPLETE",id,proof));r.putArray("members");r.putArray("regions");r.put("boundary",b);r.putArray("proofs").add(proof);
            var boundary=boundaries.addObject();boundary.put("id",b);boundary.put("region",id);boundary.putArray("proofs").add(proof);
            boundary.set("ordinaryDefault",i+1==count?lastDefault:target("REGION_ENTRY","empty/"+(i+1),proof));
        }
        for(var r:regions)if(r.path("id").asText().equals(ROOT)||r.path("kind").asText().equals("RANGE"))
            for(int i=0;i<count;i++)((ArrayNode)r.path("regions")).add("empty/"+i);
        for(var binding:topology.path("bindings"))((ObjectNode)binding).put("endpoint","boundary:empty/"+(count-1));
        return tree;
    }
    private static Publication lower(ObjectNode tree)throws Exception {
        var decoded=CobolControlSuite.decode(tree);
        if(!(decoded instanceof SpJsonDecoder.Decoded accepted))throw new AssertionError("fixture admission: "+decoded);
        var result=new CobolLowerer().lower(accepted.input(),CobolLower.POSITIVE_OPTIONS);
        if(result.publication().isEmpty()||!result.validation().orElseThrow().isStructurallyValid())throw new AssertionError("real lowering failed: "+result);
        return result.publication().orElseThrow();
    }
    private static void verify(Publication p,int count) {
        var seqs=LocalControlOracle.sequences(p);
        var origins=new HashMap<OriginId,Origins.Origin>();p.origins().forEach(o->origins.put(o.id(),o));
        var boundaries=new HashMap<CompletionPortId,Sequence>();
        for(var seq:seqs.values())if(seq.terminator() instanceof Operations.LocalBoundary b)
            if(boundaries.put(b.port(),seq)!=null)throw new AssertionError("duplicated boundary port");
        if(boundaries.size()!=count+1)throw new AssertionError("lost boundary: "+boundaries.size());
        var starts=new HashSet<LabelId>();for(var seq:boundaries.values())starts.add(seq.label());
        for(var seq:boundaries.values())starts.remove(((Operations.LocalBoundary)seq.terminator()).defaultDestination());
        if(starts.size()!=1)throw new AssertionError("expected one independent chain start");
        var cursor=starts.iterator().next();var seen=new HashSet<LabelId>();Sequence last=null;
        while(seqs.get(cursor).terminator() instanceof Operations.LocalBoundary b) {
            var seq=seqs.get(cursor);if(!seen.add(cursor))throw new AssertionError("boundary cycle");
            var origin=origins.get(seq.origin());
            if(!(origin instanceof Origins.Derived d)||d.inputs().isEmpty())throw new AssertionError("lost boundary proof");
            if(seen.size()>1) {
                var expected="SP2.39/LOCAL_GRAMMAR/empty-boundary-"+(seen.size()-2);
                var pending=new ArrayDeque<OriginId>();pending.add(seq.origin());var visited=new HashSet<OriginId>();boolean found=false;
                while(!pending.isEmpty()) {
                    var id=pending.removeFirst();if(!visited.add(id))continue;
                    if(origins.get(id) instanceof Origins.Derived proof) {
                        if(proof.rule().equals(expected))found=true;pending.addAll(proof.inputs());
                    }
                }
                if(!found)throw new AssertionError("boundary lost its own proof: "+expected);
            }
            last=seq;cursor=b.defaultDestination();
        }
        if(seen.size()!=count+1)throw new AssertionError("missing default-chain member");
        var endpoint=((Operations.LocalBoundary)Objects.requireNonNull(last).terminator()).port();
        var calls=p.units().getFirst().sequences().stream().map(Sequence::terminator).filter(Operations.LocalInvoke.class::isInstance)
            .map(Operations.LocalInvoke.class::cast).filter(c->c.completionPorts().contains(endpoint)).toList();
        if(calls.size()!=2)throw new AssertionError("two caller-specific returns must remain");
        var destinations=new HashSet<LabelId>();
        for(var call:calls) {
            var point=new LocalControlOracle.Point(starts.iterator().next(),List.of(call));
            for(int i=0;i<count;i++) {
                var next=LocalControlOracle.successors(point,seqs);
                if(next.size()!=1||!next.getFirst().stack().equals(point.stack()))throw new AssertionError("nonendpoint popped frame");
                point=next.getFirst();
            }
            var returned=LocalControlOracle.successors(point,seqs).getFirst();
            if(!returned.stack().isEmpty()||!returned.label().equals(call.resume()))throw new AssertionError("wrong endpoint return");
            destinations.add(returned.label());
        }
        if(destinations.size()!=2)throw new AssertionError("caller continuations merged");
        var unframed=new LocalControlOracle.Point(last.label(),List.of());
        if(!LocalControlOracle.successors(unframed,seqs).getFirst().label().equals(cursor))throw new AssertionError("lost ordinary default");
    }
    public static void main(String[] args)throws Exception {
        var folder=Path.of("target/empty-boundary-chain");Files.createDirectories(folder);
        for(int count:new int[]{100,10_000}) {
            var tree=fixture(count);Files.write(folder.resolve("chain-"+count+".sp.json"),CobolControlSuite.J.writeValueAsBytes(tree));
            var p=lower(tree);verify(p,count);
            var codec=new io.github.gustavo2358.air.json.AirJson();var bytes=codec.encode(p);
            Files.write(folder.resolve("chain-"+count+".air.json"),bytes);
            if(count==100) {
                for(var field:List.of("regions","boundaries","proofs")) {
                    var rows=(ArrayNode)tree.path("controlTopology").path(field);var reversed=new ArrayList<com.fasterxml.jackson.databind.JsonNode>();rows.forEach(reversed::add);
                    Collections.reverse(reversed);rows.removeAll();reversed.forEach(rows::add);
                }
                if(!Arrays.equals(bytes,codec.encode(lower(tree))))throw new AssertionError("inventory order changed deterministic emission");
            }
        }
        System.out.println("EMPTY_BOUNDARY_CHAIN=PASS");
    }
}
