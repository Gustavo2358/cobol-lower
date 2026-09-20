package io.github.gustavo2358.lower.adapters.testing;
import io.github.gustavo2358.lower.adapters.cli.CobolLower;
import io.github.gustavo2358.lower.adapters.sp.SpJsonDecoder;
import io.github.gustavo2358.lower.application.CobolLowerer;
import io.github.gustavo2358.lower.domain.FileFacts;
import io.github.gustavo2358.lower.domain.SpInput;
import java.util.*;
import static io.github.gustavo2358.lower.testing.CallOracle.check;

/** Real producer inputs, independent source-memory expectations; no lower output oracle. */
public final class FileMemoryEffectsSuite {
    static byte[] bytes(String name)throws Exception {
        return Objects.requireNonNull(FileMemoryEffectsSuite.class.getResourceAsStream("/sp/file-dependencies/w3/"+name+".json")).readAllBytes();
    }
    public static void main(String[] args)throws Exception {
        for(var fixture:List.of("read-effects","from-effects")) {
            var result=new SpJsonDecoder(CobolLower.INPUT_LIMITS).decode(bytes(fixture));
            check(result instanceof SpJsonDecoder.Decoded,"FD-W3 SP2.24 memory facts must decode: "+result);
            var input=((SpJsonDecoder.Decoded)result).input();var effects=input.fileInventory().operations().uses().getFirst().effects().orElseThrow();
            check(effects.outcomes().size()==4,"conditional effect cases preserved");
            check(new CobolLowerer().lower(input,CobolLower.OPTIONS).publication().isPresent(),"valid producer memory facts admitted");
            if(fixture.equals("read-effects")) {
                check(effects.before().isEmpty(),"READ has no pre-I/O INTO transfer");
                var success=effects.outcomes().getFirst();check(success.outcome()==FileFacts.EffectOutcome.SUCCESS,"canonical effect case order");
                check(success.steps().getFirst().kind()==FileFacts.MemoryKind.MAY_UNKNOWN,"buffer READ is not a verb-based MUST");
                check(success.steps().getLast().role()==FileFacts.MemoryRole.INTO,"INTO follows buffer/status");
                for(var outcome:effects.outcomes())if(outcome.outcome()!=FileFacts.EffectOutcome.SUCCESS)check(outcome.steps().stream().noneMatch(s->s.role()==FileFacts.MemoryRole.INTO),"failed READ cannot copy INTO");
            } else check(effects.before().size()==1&&effects.before().getFirst().kind()==FileFacts.MemoryKind.COPY_BYTES,"FROM transfer is before I/O");
        }
        var mapper=new com.fasterxml.jackson.databind.ObjectMapper();
        var withoutIoReads=(com.fasterxml.jackson.databind.node.ObjectNode)mapper.readTree(bytes("from-effects"));
        ((com.fasterxml.jackson.databind.node.ObjectNode)withoutIoReads.path("fileInventory").path("operations").path("uses").get(0).path("effects")).putArray("ioReads");
        var readless=(SpJsonDecoder.Decoded)new SpJsonDecoder(CobolLower.INPUT_LIMITS).decode(mapper.writeValueAsBytes(withoutIoReads));
        check(new CobolLowerer().lower(readless.input(),CobolLower.OPTIONS).publication().isEmpty(),"WRITE/REWRITE cannot omit the record read and claim closed bounds");
        var alias=(SpJsonDecoder.Decoded)new SpJsonDecoder(CobolLower.INPUT_LIMITS).decode(bytes("alias-from-effects"));
        var aliasAir=new CobolLowerer().lower(alias.input(),CobolLower.OPTIONS).publication().orElseThrow();
        var aliasUnit=aliasAir.units().getFirst();var firstLabel=aliasUnit.entries().getFirst().initialLabel().orElseThrow();
        var beforeIo=aliasUnit.sequences().stream().filter(s->s.label().equals(firstLabel)).findFirst().orElseThrow();
        check(beforeIo.terminator() instanceof io.github.gustavo2358.air.model.Operations.Opaque o&&(!o.envelope().memory().knownReads().isEmpty()||!(o.envelope().memory().otherReads() instanceof io.github.gustavo2358.air.model.Scopes.NoMemory)),"unproved FROM still reads its source before I/O");
        var missing=(SpJsonDecoder.Decoded)new SpJsonDecoder(CobolLower.INPUT_LIMITS).decode(bytes("missing-from-effects"));
        check(missing.input().fileInventory().operations().uses().getFirst().effects().orElseThrow().unknownReadBound(),"unresolved FROM retains an open source-read bound");
        check(new CobolLowerer().lower(missing.input(),CobolLower.OPTIONS).publication().isPresent(),"localized FROM input gap must not erase the publication");
        var numeric=new SpJsonDecoder(CobolLower.INPUT_LIMITS).decode(bytes("relative-effects"));
        check(numeric instanceof SpJsonDecoder.Decoded,"relative key/status producer facts decode");
        var numericResult=new CobolLowerer().lower(((SpJsonDecoder.Decoded)numeric).input(),CobolLower.OPTIONS);
        check(numericResult.publication().isPresent(),"numeric receivers use their declared storage kind: "+numericResult.status()+" "+numericResult.validation());
        for(var mutation:List.of("missing-effects","missing-outcomes","foreign-view","must-buffer","into-eof","missing-outcome","old-version","missing-into","missing-status","unbound-into")) {
            var root=(com.fasterxml.jackson.databind.node.ObjectNode)mapper.readTree(bytes("read-effects"));
            var use=(com.fasterxml.jackson.databind.node.ObjectNode)root.path("fileInventory").path("operations").path("uses").get(0);
            var plan=(com.fasterxml.jackson.databind.node.ObjectNode)use.path("effects");var cases=(com.fasterxml.jackson.databind.node.ArrayNode)plan.path("outcomes");
            var steps=(com.fasterxml.jackson.databind.node.ArrayNode)cases.get(0).path("steps");
            switch(mutation) {
                case "missing-effects"->use.remove("effects");case "missing-outcomes"->plan.remove("outcomes");
                case "foreign-view"->((com.fasterxml.jackson.databind.node.ObjectNode)steps.get(0).path("destination").path("regional")).put("view","storage-node:999999");
                case "must-buffer"->{((com.fasterxml.jackson.databind.node.ObjectNode)steps.get(0)).put("kind","MUST_UNKNOWN");((com.fasterxml.jackson.databind.node.ObjectNode)steps.get(0).path("destination")).put("wholeBase",false);}
                case "into-eof"->((com.fasterxml.jackson.databind.node.ArrayNode)cases.get(1).path("steps")).add(steps.get(steps.size()-1).deepCopy());
                case "missing-outcome"->cases.remove(1);
                case "old-version"->root.put("contractVersion","2.23.0");
                case "missing-into"->steps.remove(2);
                case "missing-status"->steps.remove(1);
                case "unbound-into"->((com.fasterxml.jackson.databind.node.ObjectNode)steps.get(2).path("destination")).putNull("reference");
            }
            var decoded=new SpJsonDecoder(CobolLower.INPUT_LIMITS).decode(mapper.writeValueAsBytes(root));
            check(!(decoded instanceof SpJsonDecoder.Decoded d)||new CobolLowerer().lower(d.input(),CobolLower.OPTIONS).publication().isEmpty(),"wire must reject "+mutation);
        }
        var input=((SpJsonDecoder.Decoded)new SpJsonDecoder(CobolLower.INPUT_LIMITS).decode(bytes("read-effects"))).input();
        var plan=input.fileInventory().operations().uses().getFirst().effects().orElseThrow();var cases=new ArrayList<>(plan.outcomes());var success=cases.getFirst();var steps=new ArrayList<>(success.steps());var old=steps.getFirst();var target=old.destination();
        var exact=new FileFacts.MemoryTarget(target.data(),target.regional(),false,target.reference(),target.provenance());
        steps.set(0,new FileFacts.MemoryStep(old.role(),FileFacts.MemoryKind.MUST_UNKNOWN,exact,old.source(),old.gapCodes(),old.provenance()));
        cases.set(0,new FileFacts.OutcomeEffects(success.outcome(),steps));
        var unsafe=new FileFacts.EffectPlan(plan.availability(),plan.ioReads(),plan.before(),cases,plan.unknownReadBound(),plan.unknownWriteBound(),plan.gapCodes());
        check(new CobolLowerer().lower(withPlan(input,unsafe),CobolLower.OPTIONS).publication().isEmpty(),"memory port rejects verb-based buffer MUST");
        System.out.println("FileMemoryEffectsSuite: admission PASS (wire negatives and memory MUST counterexample)");
        var air=new CobolLowerer().lower(input,CobolLower.OPTIONS).publication().orElseThrow();
        var profileGap=new FileFacts.EffectPlan(SpInput.Availability.PARTIAL,plan.ioReads(),plan.before(),plan.outcomes(),
            plan.unknownReadBound(),plan.unknownWriteBound(),List.of("FILE_EFFECT_PROFILE_NOT_PROVEN"));
        var profileInput=withPlan(input,profileGap);var profileInventory=profileInput.fileInventory();var profileUses=new ArrayList<>(profileInventory.operations().uses());
        var originalUse=profileUses.getFirst();profileUses.set(0,new FileFacts.Use(originalUse.statement(),originalUse.ordinal(),originalUse.command(),originalUse.mode(),
            FileFacts.SyntaxProfile.UNSUPPORTED,originalUse.bindingStatus(),originalUse.candidates(),originalUse.provenance(),
            List.of("FILE_SYNTAX_OUTSIDE_N_LR"),originalUse.surface(),originalUse.effects()));
        var profileOperations=new FileFacts.Operations(profileInventory.operations().availability(),profileUses,profileInventory.operations().gapCodes());
        var profileFact=new SpInput(profileInput.unit(),profileInput.policy(),profileInput.dataDeclarations(),profileInput.statements(),profileInput.structure(),
            profileInput.gaps(),profileInput.coverage(),profileInput.entryInventory(),profileInput.storageIndependence(),profileInput.compositional(),profileInput.storage(),
            new FileFacts.Inventory(profileInventory.availability(),profileInventory.declarations(),profileInventory.gapCodes(),profileOperations));
        var profileResult=new CobolLowerer().lower(profileFact,CobolLower.OPTIONS);
        check(profileResult.publication().isPresent(),"orthogonal FILE profile diagnostic preserves admitted conditional effects: "+profileResult.status()+" "+profileResult.admission().diagnostics()+" "+profileResult.validation());
        check(profileResult.publication().orElseThrow().units().getFirst().sequences().stream().flatMap(s->s.instructions().stream())
            .filter(io.github.gustavo2358.air.model.Operations.HavocMust.class::isInstance).count()==5,
            "profile gap cannot downgrade four FILE STATUS and one success-only INTO MUST");
        var read=air.units().getFirst().sequences().stream().map(io.github.gustavo2358.air.model.Sequence::terminator)
            .filter(io.github.gustavo2358.air.model.Operations.Invoke.class::isInstance).map(io.github.gustavo2358.air.model.Operations.Invoke.class::cast)
            .filter(i->i.action().equals("read")).findFirst().orElseThrow();
        check(!(read.effectBound().otherwise().writes() instanceof io.github.gustavo2358.air.model.Scopes.WithinMemory b&&b.scope() instanceof io.github.gustavo2358.air.model.Scopes.VisibleMemory),"bounded READ must not use all visible memory");
        var sequences=air.units().getFirst().sequences();
        for(var sequence:sequences)if(sequence.terminator() instanceof io.github.gustavo2358.air.model.Operations.Opaque o&&o.observedKind().equals("file-outcome-continuation")) {
            var control=o.envelope().control().remainder();
            check(control instanceof io.github.gustavo2358.air.model.Scopes.NoControl
                    ||control instanceof io.github.gustavo2358.air.model.Scopes.WithinControl c
                        &&c.scope() instanceof io.github.gustavo2358.air.model.Scopes.LabelsControl labels&&labels.labels().isEmpty(),
                "missing handler interpretation adds no control destination to conditional FILE effects");
        }
        check(read.results().isEmpty(),"INTO address must not be evaluated as a pre-invoke result place");
        check(sequences.stream().filter(s->s.terminator() instanceof io.github.gustavo2358.air.model.Operations.Branch).count()>=3,"four conditional outcomes use explicit general AIR branches");
        var strong=sequences.stream().flatMap(s->s.instructions().stream()).filter(io.github.gustavo2358.air.model.Operations.HavocMust.class::isInstance).toList();
        check(strong.size()==5,"four primary status updates and success-only INTO; no buffer MUST");
        var fromInput=((SpJsonDecoder.Decoded)new SpJsonDecoder(CobolLower.INPUT_LIMITS).decode(bytes("from-effects"))).input();
        var fromAir=new CobolLowerer().lower(fromInput,CobolLower.OPTIONS).publication().orElseThrow();
        var inventory=fromInput.fileInventory();var declaration=inventory.declarations().getFirst();
        var sourceName=declaration.assignment();
        var missingName=new FileFacts.Assignment(SpInput.Availability.PARTIAL,sourceName.profile(),sourceName.original(),
            FileFacts.NameSource.UNSUPPORTED,Optional.empty(),List.of("FILE_ASSIGNMENT_NAME_UNMODELED"));
        var unresolvedDeclaration=io.github.gustavo2358.lower.testing.IfInputs.with(declaration,"assignment",missingName);
        var unresolvedInventory=new FileFacts.Inventory(inventory.availability(),List.of(unresolvedDeclaration),inventory.gapCodes(),inventory.operations());
        var unresolvedInput=new SpInput(fromInput.unit(),fromInput.policy(),fromInput.dataDeclarations(),fromInput.statements(),fromInput.structure(),
            fromInput.gaps(),fromInput.coverage(),fromInput.entryInventory(),fromInput.storageIndependence(),fromInput.compositional(),fromInput.storage(),unresolvedInventory);
        var unresolvedAir=new CobolLowerer().lower(unresolvedInput,CobolLower.OPTIONS).publication().orElseThrow();
        check(unresolvedAir.units().getFirst().sequences().stream().map(io.github.gustavo2358.air.model.Sequence::terminator)
            .anyMatch(t->t instanceof io.github.gustavo2358.air.model.Operations.Opaque o&&o.observedKind().equals("source-file-target-unavailable/rewrite")
                &&o.envelope().memory().otherReads() instanceof io.github.gustavo2358.air.model.Scopes.WithinMemory),
            "missing assignment name keeps the real REWRITE buffer read without a fabricated runtime target");
        check(unresolvedAir.units().getFirst().sequences().stream().flatMap(s->s.instructions().stream())
            .anyMatch(io.github.gustavo2358.air.model.Operations.CopyBytes.class::isInstance),
            "missing file name does not erase the supported FROM transfer");
        var copySequence=fromAir.units().getFirst().sequences().stream().filter(s->s.instructions().stream().anyMatch(i->i instanceof io.github.gustavo2358.air.model.Operations.CopyBytes)).findFirst().orElseThrow();
        check(copySequence.terminator() instanceof io.github.gustavo2358.air.model.Operations.Jump,"FROM copy precedes a transfer to I/O");
        var afterCopy=((io.github.gustavo2358.air.model.Operations.Jump)copySequence.terminator()).destination();
        check(fromAir.units().getFirst().sequences().stream().anyMatch(s->s.label().equals(afterCopy)&&s.terminator() instanceof io.github.gustavo2358.air.model.Operations.Invoke i&&i.action().equals("rewrite")),"FROM reaches REWRITE after copying");
        var codec=new io.github.gustavo2358.air.json.AirJson();
        for(var publication:List.of(air,fromAir)) {
            var wire=codec.encode(publication);check(publication.equals(codec.decode(wire)),"memory plan AIR codec roundtrip");
            check(Arrays.equals(wire,codec.encode(codec.decode(wire))),"deterministic memory plan wire");
        }
        var output=java.nio.file.Path.of("adapters/target/fd-w3");java.nio.file.Files.createDirectories(output);
        java.nio.file.Files.write(output.resolve("read-effects.air.json"),codec.encode(air));
        java.nio.file.Files.write(output.resolve("from-effects.air.json"),codec.encode(fromAir));
        System.out.println("FileMemoryEffectsSuite: PASS (SP2.24 admission and localized lowering)");
    }
    static SpInput withPlan(SpInput input,FileFacts.EffectPlan plan) {
        var inv=input.fileInventory();var ops=inv.operations();var uses=new ArrayList<>(ops.uses());var u=uses.getFirst();
        uses.set(0,new FileFacts.Use(u.statement(),u.ordinal(),u.command(),u.mode(),u.profile(),u.bindingStatus(),u.candidates(),u.provenance(),u.gapCodes(),u.surface(),Optional.of(plan)));
        return new SpInput(input.unit(),input.policy(),input.dataDeclarations(),input.statements(),input.structure(),input.gaps(),input.coverage(),input.entryInventory(),input.storageIndependence(),input.compositional(),input.storage(),
            new FileFacts.Inventory(inv.availability(),inv.declarations(),inv.gapCodes(),new FileFacts.Operations(ops.availability(),uses,ops.gapCodes())));
    }
}
