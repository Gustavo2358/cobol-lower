package io.github.gustavo2358.lower.adapters.testing;

import io.github.gustavo2358.lower.adapters.sp.*;
import io.github.gustavo2358.lower.adapters.cli.CobolLower;
import io.github.gustavo2358.lower.application.*;
import io.github.gustavo2358.lower.domain.*;
import io.github.gustavo2358.air.model.*;
import java.util.*;
import java.util.zip.GZIPInputStream;
import static io.github.gustavo2358.lower.testing.CallOracle.check;

/** Nominal identity and physical allocation are independent published facts. */
public final class StorageBoundarySuite {
    private static final List<String> CASES=List.of("global-fd-record-visibility","global-subordinate-visibility","nested-data-visibility","nested-global-file","nested-global-through-local-data","nested-namespace-shadowing","nested-qualified-global","filler-redefines-owner","renames-structural-binding");
    public static void main(String[] args)throws Exception {
        var failures=new ArrayList<String>();
        for(var name:CASES)try { verify(name); } catch(Exception|AssertionError ex) {failures.add(name+": "+ex);}
        check(failures.isEmpty(),String.join("\n",failures));
        System.out.println("STORAGE_BOUNDARY=PASS 9 producer products, capture identity, unknown bounds, permutations, negative contracts");
    }
    private static void verify(String name)throws Exception {
        byte[] bytes;
        try(var in=new GZIPInputStream(Objects.requireNonNull(StorageBoundarySuite.class.getResourceAsStream("/sp/storage-boundary/"+name+".json.gz")))) {bytes=in.readAllBytes();}
        var decoder=new CompilationJsonDecoder(CobolLower.INPUT_LIMITS);
        var decoded=decoder.decode(bytes);check(decoded instanceof CompilationJsonDecoder.Compilation,"wire admitted "+decoded);
        var input=((CompilationJsonDecoder.Compilation)decoded).input();
        var result=new CompilationLowerer().lower(input,CobolLower.OPTIONS);
        check(result.publication().isPresent(),"AIR publication: "+result.status()+" "+result.admission().diagnostics()+" "+result.validation());
        var p=result.publication().orElseThrow();
        // Match source units through entry links, never through declaration spelling.
        var unitIds=new HashMap<SpInput.UnitKey,io.github.gustavo2358.air.model.Ids.UnitId>();
        for(var link:result.entries())unitIds.put(link.source().unit(),link.target().unit());
        for(var u:input.units()) {
            var unit=p.units().stream().filter(v->v.id().equals(unitIds.get(u.product().unit()))).findFirst().orElseThrow();
            for(var capture:u.dataCaptures()) {
                var sourceUnit=p.units().stream().filter(v->v.id().equals(unitIds.get(capture.sourceData().unit()))).findFirst().orElseThrow();
                var localFact=u.product().dataDeclarations().stream().filter(d->d.id().equals(capture.localData())).findFirst().orElseThrow();
                var sourceFact=input.units().stream().filter(v->v.product().unit().equals(capture.sourceData().unit())).flatMap(v->v.product().dataDeclarations().stream()).filter(d->d.id().equals(capture.sourceData())).findFirst().orElseThrow();
                var local=object(p,unit,localFact);var source=object(p,sourceUnit,sourceFact);
                check(local.storage().equals(new Memory.AliasBinding(source.id())),"capture targets published owner, including shadowing");
                check(local.typeRef().equals(source.typeRef()),"capture shares owner type and uncertainty");
                if(source.storage() instanceof Memory.UnknownBinding b)check(b.scope() instanceof Scopes.ObjectsMemory||b.scope() instanceof Scopes.StorageMemory,"unknown owner has scoped memory bound");
            }
        }
        var reversed=new ArrayList<>(input.units());Collections.reverse(reversed);
        check(new CompilationLowerer().lower(new SpCompilation(input.inventoryStatus(),input.unitInventory(),reversed),CobolLower.OPTIONS).publication().equals(result.publication()),"unit order invariant");
        if(name.equals("nested-global-file")) {
            check(p.units().stream().flatMap(u->u.sequences().stream()).map(Sequence::terminator)
                .filter(Operations.Opaque.class::isInstance).map(Operations.Opaque.class::cast)
                .anyMatch(o->o.observedKind().equals("published-memory-may-write")
                    &&o.envelope().memory().otherWrites() instanceof Scopes.WithinMemory w
                    &&w.scope() instanceof Scopes.VisibleMemory),"unproved receiver retains a visible MAY write, never an empty effect or a must kill");
        }
        var mapper=new com.fasterxml.jackson.databind.ObjectMapper();
        if(name.equals("filler-redefines-owner")) {
            var doc=mapper.readTree(bytes);
            for(var node:doc.path("units").get(0).path("product").path("storage").path("nodes"))if(node.path("filler").asBoolean())((com.fasterxml.jackson.databind.node.ObjectNode)node).put("filler",false);
            reject(decoder,mapper.writeValueAsBytes(doc),"anonymous non-FILLER cannot claim exact nominal identity");
            check(p.units().stream().flatMap(u->u.objects().stream()).noneMatch(o->o.displayName().equals(Optional.of("FILLER"))),"FILLER does not invent nominal data");
        }
        if(name.equals("renames-structural-binding")) {
            var doc=mapper.readTree(bytes);var storage=(com.fasterxml.jackson.databind.node.ObjectNode)doc.path("units").get(0).path("product").path("storage");
            storage.putArray("renames");
            reject(decoder,mapper.writeValueAsBytes(doc),"extra physical child cannot masquerade as nonallocating RENAMES");
        }
    }
    private static Memory.ObjectDeclaration object(Publication p,Unit unit,SpInput.DataFact fact) {
        var origins=p.origins().stream().collect(java.util.stream.Collectors.toMap(Origins.Origin::id,o->o));
        var matches=unit.objects().stream().filter(o->o.displayName().equals(Optional.of(fact.canonicalName()))).filter(o->{
            var pending=new ArrayDeque<io.github.gustavo2358.air.model.Ids.OriginId>();pending.add(o.origin());
            var seen=new HashSet<io.github.gustavo2358.air.model.Ids.OriginId>();
            while(!pending.isEmpty()) {var id=pending.remove();if(!seen.add(id))continue;var origin=origins.get(id);
                if(origin instanceof Origins.Derived d)pending.addAll(d.inputs());
                if(origin instanceof Origins.Written w&&w.location().orElse(null) instanceof Origins.LineColumns loc
                        &&loc.span().start().line().intValueExact()==fact.provenance().original().startLine())return true;
            }
            return false;
        }).toList();
        check(matches.size()==1,"one declaration with the published source identity, even for homonyms");return matches.getFirst();
    }
    private static void reject(CompilationJsonDecoder decoder,byte[] bytes,String reason) {
        var d=decoder.decode(bytes);
        if(d instanceof CompilationJsonDecoder.Compilation c)check(new CompilationLowerer().lower(c.input(),CobolLower.OPTIONS).status()==LoweringResult.Status.INVALID_INPUT,reason);
        else check(d instanceof CompilationJsonDecoder.Rejected,reason);
    }
}
